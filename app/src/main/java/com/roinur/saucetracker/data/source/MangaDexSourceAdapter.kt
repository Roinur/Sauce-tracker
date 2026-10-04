package com.roinur.saucetracker.data.source

import android.net.Uri
import com.roinur.saucetracker.core.network.HttpClientFactory
import com.roinur.saucetracker.core.network.HttpClientProfile
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

class MangaDexSourceAdapter : SourceAdapter {
    override val id = SourceId("mangadex")
    override val displayName: String = "MangaDex"
    override val contractVersion: Int = 1
    override val capabilities: Set<SourceCapability> = setOf(
        SourceCapability.SEARCH,
        SourceCapability.BROWSE,
        SourceCapability.DETAIL,
        SourceCapability.IMPORT,
        SourceCapability.METADATA_REFRESH,
        SourceCapability.READER,
        SourceCapability.SUBSCRIPTIONS,
        SourceCapability.CREATOR_NAVIGATION,
        SourceCapability.POPULAR_SORT,
        SourceCapability.RECENT_SORT
    )
    private val client = HttpClientFactory.create(HttpClientProfile.GALLERY_METADATA)
    private val tagCache = AtomicReference<Map<String, String>>(emptyMap())
    private val volumeCoverCache = ConcurrentHashMap<String, Map<String, String>>()
    private val aggregateChapterCache = ConcurrentHashMap<String, List<ReadableChapter>>()
    private val chapterById = ConcurrentHashMap<String, ReadableChapter>()
    private val readerContentCache = ConcurrentHashMap<String, CachedReaderContent>()
    private val invalidatedReaderChapters = ConcurrentHashMap.newKeySet<String>()

    override fun recognizes(input: String): Boolean = normalizeRemoteId(input) != null

    override fun normalizeRemoteId(input: String): String? {
        val trimmed = input.trim()
        val fromUrl = Regex("(?i)mangadex\\.org/title/([0-9a-f]{8}-[0-9a-f-]{27,})").find(trimmed)?.groupValues?.get(1)
        val candidate = fromUrl ?: trimmed.takeIf {
            UUID_PATTERN.matches(mangaDexBaseRemoteId(it)) &&
                (mangaDexLanguage(it).isBlank() || mangaDexLanguage(it).matches(Regex("[a-z]{2,3}(?:-[a-z0-9]{2,8})?")))
        }
        return candidate?.lowercase(Locale.US)
    }

    override fun canonicalUrl(remoteId: String): String = "https://mangadex.org/title/${requireId(remoteId)}"

    override fun fetchEntry(remoteId: String): SourceEntry {
        val safeId = requireId(remoteId)
        val url = HttpUrl.Builder()
            .scheme("https").host("api.mangadex.org")
            .addPathSegment("manga").addPathSegment(safeId)
            .addQueryParameter("includes[]", "author")
            .addQueryParameter("includes[]", "artist")
            .addQueryParameter("includes[]", "cover_art")
            .build()
        val root = requestJson(url)
        val data = root.optJSONObject("data")
            ?: throw SourceException(SourceFailureKind.INVALID_RESPONSE, id, "MangaDex response has no manga data.")
        return parseEntry(data).let { entry ->
            mangaDexLanguage(remoteId).takeIf(String::isNotBlank)?.let(entry::withMangaDexLanguage) ?: entry
        }
    }

    override fun search(query: SourceQuery, offset: Int, limit: Int): SourceSearchPage {
        val safeLimit = limit.coerceIn(1, 100)
        val safeOffset = offset.coerceAtLeast(0)
        val sourceTerms = query.terms.filter { it.field == SourceQueryField.SOURCE }
        if (sourceTerms.any { (!it.excluded && normalizeSourceText(it.value) != id.value) || (it.excluded && normalizeSourceText(it.value) == id.value) }) {
            return SourceSearchPage(id, emptyList(), safeOffset, safeLimit, 0, false)
        }
        val unsupported = query.terms.any { term ->
            !supportsQueryField(term.field) ||
                (term.field == SourceQueryField.CATEGORY && normalizeSourceText(term.value) !in setOf("safe", "suggestive", "adult", "nsfw"))
        }
        if (unsupported) return SourceSearchPage(id, emptyList(), safeOffset, safeLimit, 0, false)
        val builder = HttpUrl.Builder()
            .scheme("https").host("api.mangadex.org").addPathSegment("manga")
            .addQueryParameter("limit", safeLimit.toString())
            .addQueryParameter("offset", safeOffset.toString())
            .addQueryParameter("includes[]", "author")
            .addQueryParameter("includes[]", "artist")
            .addQueryParameter("includes[]", "cover_art")
        mangaDexContentRatings(query).forEach { rating ->
            builder.addQueryParameter("contentRating[]", rating)
        }
        if (query.sort == SourceSortMode.POPULAR) {
            builder.addQueryParameter("order[followedCount]", "desc")
        } else {
            builder.addQueryParameter("order[latestUploadedChapter]", "desc")
        }
        query.freeText.takeIf(String::isNotBlank)?.let { builder.addQueryParameter("title", it) }
        val tags = query.terms.filter { it.field == SourceQueryField.TAG }
        if (tags.isNotEmpty()) {
            val ids = loadTagIds()
            if (tags.any { !it.excluded && normalizeSourceText(it.value) !in ids }) return SourceSearchPage(id, emptyList(), safeOffset, safeLimit, 0, false)
            tags.forEach { term ->
                ids[normalizeSourceText(term.value)]?.let { tagId ->
                    builder.addQueryParameter(if (term.excluded) "excludedTags[]" else "includedTags[]", tagId)
                }
            }
        }
        query.terms.filter { it.field == SourceQueryField.STATUS && !it.excluded }.forEach {
            builder.addQueryParameter("status[]", normalizeSourceText(it.value))
        }
        query.terms.filter { it.field == SourceQueryField.LANGUAGE && !it.excluded }.forEach {
            mangaDexLanguageCode(it.value)
                ?.let { language -> builder.addQueryParameter("availableTranslatedLanguage[]", language) }
                ?: return SourceSearchPage(id, emptyList(), safeOffset, safeLimit, 0, false)
        }
        query.terms.filter { it.field in setOf(SourceQueryField.ARTIST, SourceQueryField.AUTHOR) }.forEach { term ->
            if (term.excluded) return SourceSearchPage(id, emptyList(), safeOffset, safeLimit, 0, false)
            resolveCreatorId(term.value)?.let { builder.addQueryParameter("authorOrArtist", it) }
                ?: return SourceSearchPage(id, emptyList(), safeOffset, safeLimit, 0, false)
        }
        val root = requestJson(builder.build())
        val data = root.optJSONArray("data") ?: JSONArray()
        val entries = buildList {
            for (index in 0 until data.length()) data.optJSONObject(index)?.let { add(parseEntry(it)) }
        }
        val total = root.optInt("total", -1).takeIf { it >= 0 }
        val returnedOffset = root.optInt("offset", safeOffset)
        return SourceSearchPage(id, entries, returnedOffset, safeLimit, total, total?.let { returnedOffset + entries.size < it } ?: (entries.size >= safeLimit))
    }

    override fun supportsQueryField(field: SourceQueryField): Boolean = field in setOf(
        SourceQueryField.FREE_TEXT, SourceQueryField.TAG, SourceQueryField.STATUS,
        SourceQueryField.LANGUAGE, SourceQueryField.ARTIST, SourceQueryField.AUTHOR,
        SourceQueryField.CATEGORY, SourceQueryField.SOURCE
    )

    override fun fetchTagCatalog(): List<SourceTag> = loadTagIds().map { (name, id) -> SourceTag(name, "tag", id) }

    override fun fetchChapters(remoteId: String, offset: Int, limit: Int): SourceChapterPage {
        val mangaId = requireId(remoteId)
        val language = mangaDexLanguage(remoteId).ifBlank { "en" }
        val safeOffset = offset.coerceAtLeast(0)
        val safeLimit = limit.coerceIn(1, 100)
        // Aggregate already contains the complete logical chapter list. Loading paginated feed
        // pages here used to turn a 200-chapter title into four serial network round-trips.
        // Aggregate and covers are independent, so the cold path is now one parallel round-trip.
        val coverFuture = CompletableFuture.supplyAsync({ fetchVolumeCoverUrls(mangaId) }, chapterMetadataExecutor)
        val chapterFuture = CompletableFuture.supplyAsync({ fetchAggregateChapters(mangaId, language) }, chapterMetadataExecutor)
        val volumeCovers = runCatching { coverFuture.get() }.getOrDefault(emptyMap())
        val aggregateChapters = runCatching { chapterFuture.get() }.getOrDefault(emptyList())
        val page = if (aggregateChapters.isNotEmpty()) {
            ChapterCandidatePage(
                chapters = aggregateChapters.drop(safeOffset).take(safeLimit),
                total = aggregateChapters.size
            )
        } else {
            val preferred = fetchChapterCandidates(mangaId, language, safeOffset, safeLimit)
            if (preferred.chapters.isEmpty() && safeOffset == 0) {
                fetchChapterCandidates(mangaId, null, safeOffset, safeLimit)
            } else {
                preferred
            }
        }
        val newestVolumeCover = volumeCovers.entries
            .maxByOrNull { (volume, _) -> volume.toDoubleOrNull() ?: Double.NEGATIVE_INFINITY }
            ?.value.orEmpty()
        page.chapters.forEach { chapter -> chapterById[chapter.id] = chapter }
        return SourceChapterPage(
            entryKey = SourceEntryKey(id, remoteId),
            chapters = page.chapters.map { chapter ->
                chapter.toSourceChapter(
                    previewUrl = volumeCovers[chapter.volume]
                        ?: newestVolumeCover
                )
            },
            offset = safeOffset,
            limit = safeLimit,
            total = page.total,
            hasMore = page.total?.let { safeOffset + page.chapters.size < it }
                ?: (page.chapters.size >= safeLimit)
        )
    }

    private fun fetchVolumeCoverUrls(mangaId: String): Map<String, String> {
        volumeCoverCache[mangaId]?.let { return it }
        val covers = linkedMapOf<String, String>()
        var offset = 0
        var pagesRemaining = 5
        while (pagesRemaining-- > 0) {
            val url = HttpUrl.Builder()
                .scheme("https").host("api.mangadex.org")
                .addPathSegment("cover")
                .addQueryParameter("manga[]", mangaId)
                .addQueryParameter("limit", "100")
                .addQueryParameter("offset", offset.toString())
                .addQueryParameter("order[volume]", "desc")
                .build()
            val root = requestJson(url)
            val data = root.optJSONArray("data") ?: JSONArray()
            for (index in 0 until data.length()) {
                val attributes = data.optJSONObject(index)?.optJSONObject("attributes") ?: continue
                val fileName = attributes.optionalString("fileName")
                if (fileName.isBlank()) continue
                val coverUrl = "https://uploads.mangadex.org/covers/$mangaId/$fileName.256.jpg"
                val volume = attributes.optionalString("volume")
                if (volume.isNotBlank()) covers.putIfAbsent(volume, coverUrl)
            }
            val total = root.optInt("total", data.length()).coerceAtLeast(0)
            offset += data.length()
            if (data.length() == 0 || offset >= total) break
        }
        return covers.toMap().also { volumeCoverCache[mangaId] = it }
    }

    private fun fetchAggregateChapters(mangaId: String, language: String): List<ReadableChapter> {
        val cacheKey = "$mangaId@$language"
        aggregateChapterCache[cacheKey]?.let { return it }
        val url = HttpUrl.Builder()
            .scheme("https").host("api.mangadex.org")
            .addPathSegment("manga").addPathSegment(mangaId).addPathSegment("aggregate")
            .addQueryParameter("translatedLanguage[]", language)
            .build()
        val volumes = requestJson(url).optJSONObject("volumes") ?: JSONObject()
        val mapped = buildList {
            val volumeKeys = volumes.keys()
            while (volumeKeys.hasNext()) {
                val volumeKey = volumeKeys.next()
                val chapters = volumes.optJSONObject(volumeKey)?.optJSONObject("chapters") ?: continue
                val chapterKeys = chapters.keys()
                while (chapterKeys.hasNext()) {
                    val chapterKey = chapterKeys.next()
                    val chapter = chapters.optJSONObject(chapterKey) ?: continue
                    if (chapter.optBoolean("isUnavailable", false)) continue
                    val chapterId = chapter.optString("id", "")
                    if (!UUID_PATTERN.matches(chapterId)) continue
                    val volume = volumeKey.takeUnless { it.equals("none", ignoreCase = true) }.orEmpty()
                    add(
                        ReadableChapter(
                            id = chapterId,
                            label = buildList {
                                volume.takeIf(String::isNotBlank)?.let { add("Vol. $it") }
                                add(if (chapterKey.isBlank()) "Oneshot" else "Chapter $chapterKey")
                            }.joinToString(" · "),
                            number = chapterKey,
                            volume = volume,
                            title = "",
                            language = language,
                            groups = emptyList(),
                            publishedAt = "",
                            pageCount = 0
                        )
                    )
                }
            }
        }.distinctBy { it.number.ifBlank { it.id } }
            .sortedWith(compareByDescending<ReadableChapter> { it.number.toDoubleOrNull() ?: Double.NEGATIVE_INFINITY }
                .thenByDescending { it.number })
        return mapped.also { aggregateChapterCache[cacheKey] = it }
    }

    override fun fetchReaderContent(remoteId: String, chapterId: String?): SourceReaderContent {
        val mangaId = requireId(remoteId)
        val language = mangaDexLanguage(remoteId).ifBlank { "en" }
        val requestedChapterId = chapterId?.trim()?.lowercase(Locale.US).orEmpty()
        if (requestedChapterId.isNotBlank() && !UUID_PATTERN.matches(requestedChapterId)) {
            throw SourceException(SourceFailureKind.NOT_FOUND, id, "Invalid MangaDex chapter ID.")
        }
        if (requestedChapterId.isNotBlank() && requestedChapterId !in invalidatedReaderChapters) {
            readerContentCache[requestedChapterId]
                ?.takeIf { System.currentTimeMillis() - it.fetchedAtEpochMs <= READER_CONTENT_TTL_MS }
                ?.let { return it.content }
        }
        val candidates = if (chapterId.isNullOrBlank()) {
            // A cover tap without saved progress means "start reading", not "open the newest
            // upload". Aggregate is complete and sorted newest-first, so its last item is the
            // earliest logical chapter. Explicit chapter taps and saved progress are unchanged.
            val firstChapter = runCatching { fetchAggregateChapters(mangaId, language).lastOrNull() }.getOrNull()
            if (firstChapter != null) {
                listOf(firstChapter)
            } else {
                val preferred = fetchChapterCandidates(mangaId, language, 0, 20).chapters
                (preferred + if (preferred.isEmpty()) fetchChapterCandidates(mangaId, null, 0, 20).chapters else emptyList())
                    .distinctBy { it.id }
            }
        } else {
            // The chapter list already supplied everything needed to ask At-Home for pages.
            // Reusing it removes the chapter + 100-row feed round-trips from every normal tap.
            val primary = chapterById[requestedChapterId] ?: ReadableChapter(
                id = requestedChapterId,
                label = "Chapter",
                number = "",
                volume = "",
                title = "",
                language = language,
                groups = emptyList(),
                publishedAt = "",
                pageCount = 0
            )
            if (requestedChapterId in invalidatedReaderChapters) {
                val hydrated = runCatching { fetchChapter(requestedChapterId) }.getOrDefault(primary)
                val alternates = runCatching { fetchChapterCandidates(mangaId, language, 0, 100).chapters }
                    .getOrDefault(emptyList())
                    .filter { it.id != hydrated.id && hydrated.number.isNotBlank() && it.number == hydrated.number }
                (listOf(hydrated) + alternates).distinctBy { it.id }
            } else {
                listOf(primary)
            }
        }
        if (candidates.isEmpty()) {
            throw SourceException(SourceFailureKind.NOT_FOUND, id, "No readable MangaDex chapters were found.")
        }
        var lastFailure: Throwable? = null
        candidates.take(3).forEach { chapter ->
            val delivery = runCatching { requestJson(atHomeUrl(chapter.id)) }
                .onFailure { lastFailure = it }
                .getOrNull() ?: return@forEach
            val baseUrl = delivery.optString("baseUrl", "").trim().trimEnd('/')
            val payload = delivery.optJSONObject("chapter") ?: return@forEach
            val hash = payload.optString("hash", "").trim()
            if (baseUrl.isBlank() || hash.isBlank()) return@forEach
            val original = payload.optJSONArray("data").toStringList()
            val dataSaver = payload.optJSONArray("dataSaver").toStringList()
            if (original.isEmpty() && dataSaver.isEmpty()) return@forEach
            val originalPageUrls = original.map { "$baseUrl/data/$hash/$it" }
            val saverPageUrls = dataSaver.map { "$baseUrl/data-saver/$hash/$it" }
            // Data-saver pages are MangaDex's reader-optimized representation. Use them for
            // display and retain originals as the per-page fallback/raw export source. Page
            // loading already retries the central host, so probing page 1 here only added
            // serial latency and duplicated the reader's reliability path.
            val displayPages = saverPageUrls.ifEmpty { originalPageUrls }
            val originalFallback = if (saverPageUrls.isNotEmpty()) originalPageUrls else emptyList()
            val content = SourceReaderContent(
                entryKey = SourceEntryKey(id, remoteId),
                chapterId = chapter.id,
                chapterLabel = chapter.label,
                chapterTitle = chapter.title,
                language = chapter.language,
                scanlationGroups = chapter.groups,
                pageUrls = displayPages,
                dataSaverPageUrls = originalFallback
            )
            readerContentCache[chapter.id] = CachedReaderContent(content, System.currentTimeMillis())
            invalidatedReaderChapters.remove(chapter.id)
            return content
        }
        throw SourceException(
            SourceFailureKind.TEMPORARY,
            id,
            "MangaDex could not provide pages for the latest readable chapter.",
            cause = lastFailure
        )
    }

    fun invalidateReaderContent(chapterId: String) {
        val safeId = chapterId.trim().lowercase(Locale.US)
        if (safeId.isBlank()) return
        readerContentCache.remove(safeId)
        invalidatedReaderChapters += safeId
    }

    private fun fetchChapterCandidates(
        mangaId: String,
        language: String?,
        offset: Int,
        limit: Int
    ): ChapterCandidatePage {
        val builder = HttpUrl.Builder()
            .scheme("https").host("api.mangadex.org")
            .addPathSegment("manga").addPathSegment(mangaId).addPathSegment("feed")
            .addQueryParameter("limit", limit.coerceIn(1, 100).toString())
            .addQueryParameter("offset", offset.coerceAtLeast(0).toString())
            .addQueryParameter("order[chapter]", "desc")
            .addQueryParameter("includes[]", "scanlation_group")
            .addQueryParameter("includeExternalUrl", "0")
        language?.let { builder.addQueryParameter("translatedLanguage[]", it) }
        val root = requestJson(builder.build())
        val data = root.optJSONArray("data") ?: JSONArray()
        val chapters = buildList {
            for (index in 0 until data.length()) {
                val item = data.optJSONObject(index) ?: continue
                parseReadableChapter(item)?.let(::add)
            }
        }
        chapters.forEach { chapter -> chapterById[chapter.id] = chapter }
        return ChapterCandidatePage(
            chapters = chapters,
            total = root.optInt("total", -1).takeIf { it >= 0 }
        )
    }

    private fun fetchChapter(chapterId: String): ReadableChapter {
        val safeId = chapterId.trim().lowercase(Locale.US)
        if (!UUID_PATTERN.matches(safeId)) {
            throw SourceException(SourceFailureKind.NOT_FOUND, id, "Invalid MangaDex chapter ID.")
        }
        val url = HttpUrl.Builder()
            .scheme("https").host("api.mangadex.org")
            .addPathSegment("chapter").addPathSegment(safeId)
            .addQueryParameter("includes[]", "scanlation_group")
            .build()
        val item = requestJson(url).optJSONObject("data")
            ?: throw SourceException(SourceFailureKind.NOT_FOUND, id, "MangaDex chapter was not found.")
        return parseReadableChapter(item)
            ?: throw SourceException(SourceFailureKind.NOT_FOUND, id, "MangaDex chapter is not readable.")
    }

    private fun parseReadableChapter(item: JSONObject): ReadableChapter? {
        val chapterId = item.optString("id", "")
        if (!UUID_PATTERN.matches(chapterId)) return null
        val attributes = item.optJSONObject("attributes") ?: return null
        if (attributes.optBoolean("isUnavailable", false)) return null
        if (!attributes.isNull("externalUrl") && attributes.optString("externalUrl", "").isNotBlank()) return null
        if (attributes.optInt("pages", 0) <= 0) return null
        val chapterNumber = attributes.optionalString("chapter")
        val volume = attributes.optionalString("volume")
        val label = buildList {
            volume.takeIf(String::isNotBlank)?.let { add("Vol. $it") }
            add(if (chapterNumber.isBlank()) "Oneshot" else "Chapter $chapterNumber")
        }.joinToString(" · ")
        val groups = buildList {
            val relations = item.optJSONArray("relationships") ?: JSONArray()
            for (relationIndex in 0 until relations.length()) {
                val relation = relations.optJSONObject(relationIndex) ?: continue
                if (relation.optString("type") != "scanlation_group") continue
                relation.optJSONObject("attributes")?.optString("name", "")
                    ?.trim()?.takeIf(String::isNotBlank)?.let(::add)
            }
        }.distinct()
        return ReadableChapter(
            id = chapterId,
            label = label,
            number = chapterNumber,
            volume = volume,
            title = attributes.optionalString("title"),
            language = attributes.optionalString("translatedLanguage"),
            groups = groups,
            publishedAt = attributes.optionalString("publishAt"),
            pageCount = attributes.optInt("pages", 0)
        )
    }

    private fun atHomeUrl(chapterId: String): HttpUrl = HttpUrl.Builder()
        .scheme("https").host("api.mangadex.org")
        .addPathSegment("at-home").addPathSegment("server").addPathSegment(chapterId)
        .build()

    private fun resolveCreatorId(value: String): String? {
        value.trim().takeIf(UUID_PATTERN::matches)?.let { return it.lowercase(Locale.US) }
        val name = value.trim()
        if (name.isBlank()) return null
        val root = requestJson(
            HttpUrl.Builder().scheme("https").host("api.mangadex.org")
                .addPathSegment("author").addQueryParameter("name", name).addQueryParameter("limit", "20").build()
        )
        val data = root.optJSONArray("data") ?: return null
        var fallback: String? = null
        for (index in 0 until data.length()) {
            val author = data.optJSONObject(index) ?: continue
            val authorId = author.optString("id", "").takeIf(UUID_PATTERN::matches) ?: continue
            val authorName = author.optJSONObject("attributes")?.optString("name", "").orEmpty()
            if (fallback == null) fallback = authorId
            if (authorName.equals(name, ignoreCase = true)) return authorId
        }
        return fallback
    }

    private fun loadTagIds(): Map<String, String> {
        tagCache.get().takeIf { it.isNotEmpty() }?.let { return it }
        val root = requestJson("https://api.mangadex.org/manga/tag".toHttpUrl())
        val data = root.optJSONArray("data") ?: JSONArray()
        val mapped = buildMap {
            for (index in 0 until data.length()) {
                val tag = data.optJSONObject(index) ?: continue
                val name = localizedText(tag.optJSONObject("attributes")?.optJSONObject("name"))
                val tagId = tag.optString("id", "")
                if (name.isNotBlank() && UUID_PATTERN.matches(tagId)) put(normalizeSourceText(name), tagId)
            }
        }
        tagCache.compareAndSet(emptyMap(), mapped)
        return tagCache.get()
    }

    private fun requestJson(url: HttpUrl): JSONObject {
        val request = Request.Builder().url(url).header("Accept", "application/json").header("User-Agent", "SauceTracker/2.0").build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.code == 404) throw SourceException(SourceFailureKind.NOT_FOUND, id, "MangaDex entry was not found.")
                if (response.code == 429) throw SourceException(SourceFailureKind.RATE_LIMITED, id, "MangaDex rate limit reached.", response.header("Retry-After")?.toLongOrNull())
                if (!response.isSuccessful) throw SourceException(SourceFailureKind.TEMPORARY, id, "MangaDex returned HTTP ${response.code}.")
                return runCatching { JSONObject(response.body?.string().orEmpty()) }
                    .getOrElse { throw SourceException(SourceFailureKind.INVALID_RESPONSE, id, "MangaDex returned invalid JSON.", cause = it) }
            }
        } catch (error: SourceException) {
            throw error
        } catch (error: IOException) {
            throw SourceException(SourceFailureKind.OFFLINE, id, "Could not reach MangaDex.", cause = error)
        }
    }

    private fun parseEntry(data: JSONObject): SourceEntry {
        val remoteId = requireId(data.optString("id", ""))
        val attributes = data.optJSONObject("attributes") ?: JSONObject()
        val titleObject = attributes.optJSONObject("title")
        val title = localizedText(titleObject).ifBlank { "MangaDex $remoteId" }
        val alternateTitles = buildList {
            val raw = attributes.optJSONArray("altTitles") ?: JSONArray()
            for (index in 0 until raw.length()) localizedText(raw.optJSONObject(index)).takeIf(String::isNotBlank)?.let(::add)
        }.distinct().filterNot { it.equals(title, true) }
        val tags = buildList {
            val raw = attributes.optJSONArray("tags") ?: JSONArray()
            for (index in 0 until raw.length()) {
                val tag = raw.optJSONObject(index) ?: continue
                val name = localizedText(tag.optJSONObject("attributes")?.optJSONObject("name"))
                if (name.isNotBlank()) add(SourceTag(name, "tag", tag.optString("id", "")))
            }
        }
        var coverFile = ""
        val creators = mutableListOf<SourceCreator>()
        val relationships = data.optJSONArray("relationships") ?: JSONArray()
        for (index in 0 until relationships.length()) {
            val relationship = relationships.optJSONObject(index) ?: continue
            val relationId = relationship.optString("id", "")
            val type = relationship.optString("type", "")
            val relationAttributes = relationship.optJSONObject("attributes")
            when (type) {
                "cover_art" -> coverFile = relationAttributes?.optString("fileName", "").orEmpty()
                "author", "artist" -> {
                    val name = relationAttributes?.optString("name", "").orEmpty().trim()
                    if (name.isNotBlank()) creators += SourceCreator(name, type, relationId, "https://mangadex.org/author/$relationId")
                }
            }
        }
        val lastChapter = attributes.optString("lastChapter", "").substringBefore('.').toIntOrNull() ?: 0
        return SourceEntry(
            key = SourceEntryKey(id, remoteId),
            title = title,
            alternateTitles = alternateTitles,
            canonicalUrl = canonicalUrl(remoteId),
            thumbnailUrl = coverFile.takeIf(String::isNotBlank)?.let { "https://uploads.mangadex.org/covers/$remoteId/$it.512.jpg" }.orEmpty(),
            unitCount = lastChapter.coerceAtLeast(0),
            unitLabel = "chapters",
            publishedAt = attributes.optString("year", ""),
            status = attributes.optString("status", ""),
            tags = tags,
            creators = creators,
            sourcePayload = attributes.toString()
        )
    }

    private fun localizedText(obj: JSONObject?): String {
        if (obj == null) return ""
        listOf("en", "ja-ro", "ja", "ko-ro", "ko", "zh-ro", "zh").forEach { key ->
            obj.optString(key, "").trim().takeIf(String::isNotBlank)?.let { return it }
        }
        val keys = obj.keys()
        while (keys.hasNext()) obj.optString(keys.next(), "").trim().takeIf(String::isNotBlank)?.let { return it }
        return ""
    }

    private fun requireId(remoteId: String): String = mangaDexBaseRemoteId(remoteId).takeIf(UUID_PATTERN::matches)
        ?: throw SourceException(SourceFailureKind.UNSUPPORTED, id, "Invalid MangaDex UUID.")

    private companion object {
        val chapterMetadataExecutor = Executors.newFixedThreadPool(2) { runnable ->
            Thread(runnable, "mangadex-chapter-metadata").apply { isDaemon = true }
        }

        data class ReadableChapter(
            val id: String,
            val label: String,
            val number: String,
            val volume: String,
            val title: String,
            val language: String,
            val groups: List<String>,
            val publishedAt: String,
            val pageCount: Int
        ) {
            fun toSourceChapter(previewUrl: String = ""): SourceChapter = SourceChapter(
                id = id,
                label = label,
                number = number,
                volume = volume,
                title = title,
                language = language,
                scanlationGroups = groups,
                publishedAt = publishedAt,
                pageCount = pageCount,
                previewUrl = previewUrl
            )
        }

        data class ChapterCandidatePage(
            val chapters: List<ReadableChapter>,
            val total: Int?
        )

        data class CachedReaderContent(
            val content: SourceReaderContent,
            val fetchedAtEpochMs: Long
        )

        const val READER_CONTENT_TTL_MS = 5L * 60L * 1000L

        val UUID_PATTERN = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}")

        fun JSONArray?.toStringList(): List<String> {
            if (this == null) return emptyList()
            return buildList {
                for (index in 0 until length()) optString(index, "").trim().takeIf(String::isNotBlank)?.let(::add)
            }
        }

        fun JSONObject.optionalString(key: String): String {
            if (isNull(key)) return ""
            return optString(key, "").trim().takeUnless { it.equals("null", ignoreCase = true) }.orEmpty()
        }
    }
}

internal fun mangaDexContentRatings(query: SourceQuery): List<String> {
    val scope = query.terms
        .lastOrNull { it.field == SourceQueryField.CATEGORY && !it.excluded }
        ?.value?.trim()?.lowercase(Locale.US)
    return when (scope) {
        "safe" -> listOf("safe")
        "suggestive" -> listOf("safe", "suggestive")
        "adult", "nsfw" -> listOf("safe", "suggestive", "erotica", "pornographic")
        else -> listOf("safe", "suggestive", "erotica", "pornographic")
    }
}
