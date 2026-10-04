package com.roinur.saucetracker.feature.suggestions

import com.roinur.saucetracker.*
import com.roinur.saucetracker.data.source.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import org.json.JSONArray
import org.json.JSONObject

/** Adapter translation only: the established taste model and scorer remain shared. */
internal fun sourceSuggestionSnapshot(rows: List<ProfileSourceEntry>, sessionKeys: Set<String>, lengthUnit: String? = null): JSONObject {
    val array = JSONArray()
    rows.forEach { row ->
        val entry = row.entry
        val read = row.state.isRead || entry.key.storageKey in sessionKeys
        if (!read && row.state.rating == 0) return@forEach
        val tags = JSONArray()
        entry.tags.forEach { tags.put(JSONObject().put("name", it.name).put("type", it.type)) }
        entry.creators.forEach { tags.put(JSONObject().put("name", it.name).put("type", it.type)) }
        array.put(JSONObject().put("code", entry.key.uiCode()).put("source_key", entry.key.storageKey)
            .put("title", entry.title).put("thumbnail_url", entry.thumbnailUrl)
            .put("rating", row.state.rating).put("read", if (read) 1 else 0)
            .put("num_pages", if (lengthUnit == null || entry.unitLabel == lengthUnit) entry.unitCount else 0)
            .put("tags", tags))
    }
    return JSONObject().put("entries", array)
}

internal fun SourceEntry.suggestionTags(): List<GalleryTag> =
    (tags.map { GalleryTag(it.name, it.type) } + creators.map { GalleryTag(it.name, it.type) } +
        mangaDexAvailableLanguages().map { GalleryTag(mangaDexLanguageDisplayName(it), "language") })
        .distinctBy { normalizeTagName(it.name) to it.type }

internal fun SourceEntry.matchesSourceSuggestionSearch(raw: String, selected: List<TagRouteRef>, resolveTag: (Long) -> TagRouteRef?): Boolean {
    val allTags = suggestionTags()
    fun matches(ref: TagRouteRef, exact: Boolean = true): Boolean {
        if (ref.type == "source") return normalizeSourceName(ref.name) == key.sourceId.value
        return allTags.any { tag ->
            (ref.type == "creator" && tag.type in setOf("artist", "author", "group") || tag.type == ref.type) &&
                (if (exact) normalizeTagName(tag.name) == normalizeTagName(ref.name) else normalizeTagName(tag.name).contains(normalizeTagName(ref.name)))
        }
    }
    if (selected.any { !matches(it) }) return false
    val parsed = parseSearchQuery(raw)
    if (parsed.filters.any { it.key in setOf("pages", "chapters") && it.key != unitLabel }) return false
    val filters = parsed.filters.map { filter ->
        if (filter.key.endsWith("tagid")) {
            val refs = filter.value.split('|').mapNotNull { it.trim().toLongOrNull()?.let(resolveTag) }
            val any = refs.any { matches(it) }
            if (filter.key.startsWith("exclude") && any || filter.key.startsWith("any") && !any) return false
            null
        } else if (filter.key == "url" && normalizeSourceName(filter.value) in setOf("nhentai", "mangadex")) {
            if (normalizeSourceName(filter.value) != key.sourceId.value) return false
            null
        } else if (filter.key in setOf("author", "status", "chapters")) {
            val okay = when (filter.key) {
                "author" -> matches(TagRouteRef(filter.value, "author"), exact = false)
                "status" -> status.contains(filter.value.trim('"'), true)
                else -> extractNumericTokens(filter.value).let { nums -> nums.isNotEmpty() && unitCount in (nums.min()..nums.max()) }
            }
            if (!okay) return false
            null
        } else filter
    }.filterNotNull()
    return GalleryData(key.uiCode(), title, alternateTitles.joinToString(" "), unitCount, publishedAt,
        canonicalUrl, 0, "jpg", allTags).matchesSuggestionFilters(emptySet(), parsed.copy(filters = filters))
}

internal data class ScoredSourceSuggestion(val entry: SourceEntry, val breakdown: SuggestionScoreBreakdown)
internal data class SourceSuggestionResult(val candidates: List<ScoredSourceSuggestion>, val errors: Map<SourceId, String>)

internal fun sourceSuggestionSearchQueries(raw: String, selected: List<TagRouteRef>, resolveTag: (Long) -> TagRouteRef?): List<SourceQuery> {
    val parsed = parseSearchQuery(raw)
    val terms = selected.mapNotNull { ref -> sourceQueryField(ref.type)?.let { SourceQueryTerm(it, ref.name) } }.toMutableList()
    val free = mutableListOf(parsed.freeText)
    val alternatives = mutableListOf<SourceQueryTerm>()
    parsed.filters.forEach { filter ->
        val value = filter.value.trim().trim('"')
        when (filter.key) {
            "title", "subtitle" -> free += value
            "url" -> if (normalizeSourceName(value) in setOf("nhentai", "mangadex")) terms += SourceQueryTerm(SourceQueryField.SOURCE, normalizeSourceName(value))
            "anytagid", "excludetagid", "anytag", "excludetag" -> {
                val refs = value.split('|').mapNotNull { item ->
                    if (filter.key.endsWith("id")) item.trim().toLongOrNull()?.let(resolveTag) else TagRouteRef(item.trim(), "tag")
                }
                refs.mapNotNull { ref -> sourceQueryField(ref.type)?.let { SourceQueryTerm(it, ref.name, filter.key.startsWith("exclude")) } }.forEach {
                    if (filter.key.startsWith("any")) alternatives += it else terms += it
                }
            }
            else -> sourceQueryField(filter.key)?.let { terms += SourceQueryTerm(it, value) }
        }
    }
    val base = SourceQuery(free.filter(String::isNotBlank).joinToString(" "), terms.distinct())
    return if (alternatives.isEmpty()) listOf(base) else alternatives.take(8).map { base.copy(terms = base.terms + it) }
}

internal suspend fun collectSourceSuggestions(
    adapters: List<SourceAdapter>, library: List<ProfileSourceEntry>, sessionKeys: Set<String>,
    blocked: Set<String>, weights: Map<SuggestionWeightCategory, Float>, themeStrength: Float,
    adjustments: Map<String, Float>, mode: SuggestionMode, search: String, selected: List<TagRouteRef>,
    hidden: Set<Int>, excluded: Set<Int>, resolveTag: (Long) -> TagRouteRef?
): SourceSuggestionResult = coroutineScope {
    val existing = library.mapTo(hashSetOf()) { it.entry.key.storageKey }
    val existingManga = library.filter { it.entry.key.sourceId.value == "mangadex" }.mapTo(hashSetOf()) { mangaDexBaseRemoteId(it.entry.key.remoteId) }
    val results = adapters.map { adapter -> async(Dispatchers.IO) {
        val profile = buildSuggestionProfile(sourceSuggestionSnapshot(library, sessionKeys, if (adapter.id.value == "mangadex") "chapters" else "pages"),
            blocked, weights, themeStrength, SuggestionsViewModel(), adjustments)
        val topTags = profile.tagWeights.entries.filter { it.value > 0 }.sortedByDescending { it.value }.take(8).mapNotNull {
            sourceQueryField(profile.tagTypeByName[it.key] ?: "tag")?.let { field -> SourceQueryTerm(field, it.key) }
        }
        val topCreators = profile.creatorWeights.entries.filter { it.value > 0 }.sortedByDescending { it.value }.take(3).mapNotNull {
            sourceQueryField(profile.creatorTypeByName[it.key] ?: "artist")?.let { field -> SourceQueryTerm(field, it.key) }
        }
        val preferred = when (mode) { SuggestionMode.TAGS -> topTags; SuggestionMode.CREATORS -> topCreators; else -> topTags.take(5) + topCreators.take(2) }
            .filter { adapter.supportsQueryField(it.field) }
        val seed = preferred.ifEmpty { (topTags + topCreators).filter { adapter.supportsQueryField(it.field) } }
        val explicitQueries = sourceSuggestionSearchQueries(search, selected, resolveTag)
        val explicit = explicitQueries.first()
        val base = explicit.terms.filter { adapter.supportsQueryField(it.field) }
        val negative = blocked.map { SourceQueryTerm(SourceQueryField.TAG, it, excluded = true) }
        val queries = buildList {
            if (explicit.freeText.isNotBlank() || base.isNotEmpty()) explicitQueries.forEach { query -> add(query.copy(terms = query.terms.filter { adapter.supportsQueryField(it.field) } + negative)) }
            if (seed.isNotEmpty()) {
                add(SourceQuery(explicit.freeText, base + seed.take(2) + negative))
                seed.forEach { add(SourceQuery(explicit.freeText, base + it + negative)) }
            }
        }.distinct().take(10)
        val found = linkedMapOf<SourceEntryKey, ScoredSourceSuggestion>()
        var error: String? = null
        // Sequential rounds, at most two requests concurrently. Relax preference seeds, never user's filters/blocking.
        for (batch in queries.chunked(2)) {
            ensureActive()
            if (found.size >= 30) break
            val outcomes = batch.map { query -> async {
                try { adapter.search(query, 0, 25) to null }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) { null to failure }
            } }.awaitAll()
            val pages = outcomes.mapNotNull { (page, failure) ->
                if (failure != null) error = failure.message ?: "Provider unavailable"
                page
            }
            pages.filterNotNull().flatMap { it.entries }.forEach { entry ->
                if (entry.key.storageKey in existing || entry.key.uiCode() in hidden || entry.key.uiCode() in excluded) return@forEach
                if (entry.key.sourceId.value == "mangadex" && mangaDexBaseRemoteId(entry.key.remoteId) in existingManga) return@forEach
                if (entry.suggestionTags().any { normalizeTagName(it.name) in blocked }) return@forEach
                if (!entry.matchesSourceSuggestionSearch(search, selected, resolveTag)) return@forEach
                val breakdown = scoreSuggestionCandidate(entry.unitCount, entry.suggestionTags(), profile.tagWeights,
                    profile.tagThemeWeights, profile.creatorWeights, profile.averageNumPages, profile.numPagesDeviation,
                    weights[SuggestionWeightCategory.LENGTH] ?: 1f, blocked)
                // Same near-match fallback as the existing reader/library recommendation flow.
                if (breakdown.score > -1.25f) found[entry.key] = ScoredSourceSuggestion(entry, breakdown)
            }
            if (outcomes.any { it.second is SourceException && (it.second as SourceException).kind == SourceFailureKind.RATE_LIMITED }) break
        }
        found.values.sortedByDescending { it.breakdown.score }.take(30) to error
    } }.awaitAll()
    SourceSuggestionResult(results.flatMap { it.first }, adapters.indices.mapNotNull { index -> results[index].second?.let { adapters[index].id to it } }.toMap())
}

/** Apply visibility again when merging an asynchronously refreshed provider bucket. */
internal fun visibleSourceSuggestionRows(rows: List<SuggestedEntryRow>, hidden: Set<Int>, skipped: Set<Int>, imported: Set<String>, sources: Set<String>): List<SuggestedEntryRow> =
    rows.filter { it.sourceId in sources && it.code !in hidden && it.code !in skipped && "${it.sourceId}:${it.remoteId}" !in imported }

/** Round-robin sorted provider buckets prevents one fast provider occupying every visible slot. */
internal fun balancedSourceSuggestions(rows: List<SuggestedEntryRow>, sourceForCode: (Int) -> String, limit: Int): List<SuggestedEntryRow> {
    val buckets = rows.distinctBy { it.code }.groupBy { sourceForCode(it.code) }.values.map { it.sortedByDescending { row -> row.score }.iterator() }
    return buildList { while (size < limit && buckets.any { it.hasNext() }) { buckets.forEach { if (size < limit && it.hasNext()) add(it.next()) } } }
}
