package com.example.saucetracker.feature.suggestions

import com.example.saucetracker.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

internal data class SuggestionCreatorToken(
    val name: String,
    val type: String
) {
    fun isNotBlank(): Boolean = name.isNotBlank()
}

internal data class SuggestionTagToken(
    val name: String,
    val type: String
) {
    fun isNotBlank(): Boolean = name.isNotBlank()
}

internal data class SuggestionPreferenceProfile(
        val tagWeights: Map<String, Float>,
        val tagThemeWeights: Map<String, Float>,
        val tagTypeByName: Map<String, String>,
        val creatorWeights: Map<String, Float>,
        val creatorTypeByName: Map<String, String>,
        val creatorSignalEntries: Int,
        val averageNumPages: Float,
        val numPagesDeviation: Float
    )

internal fun buildSuggestionSearchQuery(
        includeTags: List<SuggestionTagToken>,
        includeCreators: List<SuggestionCreatorToken>,
        blockedTags: List<String>
    ): String {
        val tokens = mutableListOf<String>()
        includeCreators.forEach { creator ->
            val type = creator.type.trim().lowercase(Locale.US).ifBlank { "artist" }
            tokens += "$type:${toSuggestionQueryTerm(creator.name)}"
        }
        includeTags.forEach { tag ->
            val normalizedName = normalizeTagName(tag.name)
            if (normalizedName.isBlank() || normalizedName in IGNORED_SUGGESTION_TAG_NAMES) return@forEach
            val type = tag.type.trim().lowercase(Locale.US)
            val term = toSuggestionQueryTerm(normalizedName)
            if (term.isBlank()) return@forEach
            tokens += if (type == "language") {
                "language:$term"
            } else {
                term
            }
        }
        blockedTags.forEach { blocked ->
            tokens += "-tag:${toSuggestionQueryTerm(blocked)}"
        }
        return tokens.joinToString(" ").trim()
    }

private fun toSuggestionQueryTerm(raw: String): String {
        val normalized = raw.trim().replace(Regex("\\s+"), " ")
        if (normalized.isBlank()) return ""
        return if (normalized.contains(' ')) "\"$normalized\"" else normalized
    }

internal fun buildSuggestionProfile(
        snapshot: JSONObject,
        blockedTags: Set<String>,
        categoryWeights: Map<SuggestionWeightCategory, Float>,
        themeStrength: Float,
        suggestionsViewModel: SuggestionsViewModel
    ): SuggestionPreferenceProfile {
        val tagWeights = linkedMapOf<String, Float>()
        val tagTypeByName = linkedMapOf<String, String>()
        val creatorWeights = linkedMapOf<String, Float>()
        val creatorTypeScores = linkedMapOf<String, MutableMap<String, Float>>()
        val localTagEntryCodes = linkedMapOf<String, MutableSet<Int>>()
        var weightedNumPagesTotal = 0f
        var weightedNumPagesSignal = 0f
        val weightedPageSamples = mutableListOf<Pair<Float, Float>>()
        var creatorSignalEntries = 0
        val entries = snapshot.optJSONArray("entries") ?: JSONArray()
        for (idx in 0 until entries.length()) {
            val row = entries.optJSONObject(idx) ?: continue
            val rating = row.optInt("rating", 0).coerceIn(0, 5)
            val isRead = row.optInt("read", if (rating > 0) 1 else 0) != 0
            val numPages = row.optInt("num_pages", 0).coerceAtLeast(0)
            val weight = suggestionsViewModel.ratingWeight(rating = rating, isRead = isRead)
            val positiveSignal = isRead || rating > 0
            if (numPages > 0 && positiveSignal && weight > 0f) {
                weightedNumPagesTotal += numPages.toFloat() * weight
                weightedNumPagesSignal += weight
                weightedPageSamples += numPages.toFloat() to weight
            }
            val tags = row.optJSONArray("tags") ?: continue
            val entryCode = row.optInt("code", idx + 1)
            var hasCreatorSignal = false
            for (tagIdx in 0 until tags.length()) {
                val tagObj = tags.optJSONObject(tagIdx) ?: continue
                val name = normalizeTagName(tagObj.optString("name", ""))
                if (name.isBlank() || name in blockedTags || name in IGNORED_SUGGESTION_TAG_NAMES) continue
                val type = tagObj.optString("type", "tag").trim().lowercase(Locale.US)
                tagTypeByName.putIfAbsent(name, type)
                if (type == "tag") {
                    localTagEntryCodes.getOrPut(name) { linkedSetOf() }.add(entryCode)
                }
                if (!positiveSignal || weight == 0f) continue
                val category = SuggestionWeightCategory.fromTagType(type)
                val categoryScale = (categoryWeights[category] ?: 1f).coerceIn(0f, 2f)
                if (categoryScale <= 0f) continue
                if (category == SuggestionWeightCategory.CREATOR) {
                    creatorWeights[name] = (creatorWeights[name] ?: 0f) + (weight * 1.15f * categoryScale)
                    val typeScores = creatorTypeScores.getOrPut(name) { linkedMapOf() }
                    typeScores[type] = (typeScores[type] ?: 0f) + abs(weight)
                    hasCreatorSignal = true
                } else {
                    val typeWeight = suggestionsViewModel.tagTypeWeight(type) * categoryScale
                    if (typeWeight == 0f) continue
                    tagWeights[name] = (tagWeights[name] ?: 0f) + (weight * typeWeight)
                }
            }
            if (hasCreatorSignal) {
                creatorSignalEntries += 1
            }
        }
        val creatorTypeByName = creatorTypeScores.mapValues { (_, scores) ->
            scores.maxByOrNull { it.value }?.key ?: "artist"
        }
        val tagThemeWeights = suggestionsViewModel.themeWeights(
            tagWeights = tagWeights,
            localTagEntryCodes = localTagEntryCodes,
            themeStrength = themeStrength
        )
        val averageNumPages = if (weightedNumPagesSignal > 0f) {
            weightedNumPagesTotal / weightedNumPagesSignal
        } else {
            0f
        }
        val numPagesDeviation = if (weightedPageSamples.isNotEmpty() && averageNumPages > 0f) {
            val variance = weightedPageSamples.sumOf { (pages, sampleWeight) ->
                val delta = (pages - averageNumPages).toDouble()
                (delta * delta) * sampleWeight.toDouble()
            } / weightedNumPagesSignal.toDouble().coerceAtLeast(1.0)
            sqrt(variance).toFloat().coerceAtLeast(6f)
        } else {
            0f
        }
        return SuggestionPreferenceProfile(
            tagWeights = tagWeights,
            tagThemeWeights = tagThemeWeights,
            tagTypeByName = tagTypeByName,
            creatorWeights = creatorWeights,
            creatorTypeByName = creatorTypeByName,
            creatorSignalEntries = creatorSignalEntries,
            averageNumPages = averageNumPages,
            numPagesDeviation = numPagesDeviation
        )
    }

internal fun GalleryData.matchesSuggestionFilters(
    requiredTagFilters: Set<String>,
    parsedSearch: ParsedSearchQuery
): Boolean {
    val normalizedTitle = normalizeTagName(title)
    val normalizedSubtitle = normalizeTagName(subtitle)
    val normalizedUploadDate = uploadDate.trim().lowercase(Locale.US)
    val normalizedSourceUrl = sourceUrl.trim().lowercase(Locale.US)
    val codeText = code.toString()
    val pagesText = numPages.toString()
    val galleryUploadDate = parseFirstDate(uploadDate)
    val normalizedTags = tags
        .asSequence()
        .map { normalizeTagName(it.name) to it.type.trim().lowercase(Locale.US) }
        .filter { it.first.isNotBlank() }
        .toList()
    val tagNames = normalizedTags.asSequence().map { it.first }.toSet()

    if (requiredTagFilters.isNotEmpty() && requiredTagFilters.any { it !in tagNames }) {
        return false
    }

    fun matchesUniversalTerm(rawTerm: String): Boolean {
        val normalizedTerm = normalizeTagName(rawTerm)
        if (normalizedTerm.isBlank()) return true
        parseCode(rawTerm)?.let { parsed ->
            if (parsed == code) return true
        }
        if (codeText.contains(normalizedTerm)) return true
        if (pagesText.contains(normalizedTerm)) return true
        if (normalizedTitle.contains(normalizedTerm)) return true
        if (normalizedSubtitle.contains(normalizedTerm)) return true
        if (normalizedUploadDate.contains(normalizedTerm)) return true
        if (normalizedSourceUrl.contains(normalizedTerm)) return true
        return normalizedTags.any { (name, type) ->
            name.contains(normalizedTerm) || type.contains(normalizedTerm)
        }
    }

    fun matchesFieldFilter(key: String, rawValue: String): Boolean {
        val value = rawValue.trim()
        val normalizedValue = normalizeTagName(value)
        if (value.isBlank()) return true
        return when (key) {
            "code" -> {
                val cleaned = value.removePrefix("#").trim()
                val parsedCode = cleaned.toIntOrNull()
                if (parsedCode != null) {
                    code == parsedCode
                } else {
                    normalizedValue.isNotBlank() && codeText.contains(normalizedValue)
                }
            }
            "title" -> normalizedValue.isNotBlank() && normalizedTitle.contains(normalizedValue)
            "subtitle" -> normalizedValue.isNotBlank() && normalizedSubtitle.contains(normalizedValue)
            "pages" -> {
                val pageNumbers = extractNumericTokens(value)
                when {
                    pageNumbers.size >= 2 -> {
                        val start = minOf(pageNumbers[0], pageNumbers[1])
                        val end = maxOf(pageNumbers[0], pageNumbers[1])
                        numPages in start..end
                    }
                    pageNumbers.size == 1 -> numPages == pageNumbers.first()
                    else -> normalizedValue.isNotBlank() && pagesText.contains(normalizedValue)
                }
            }
            "upload" -> {
                val dateRange = parseDateRange(value)
                when {
                    dateRange != null -> {
                        galleryUploadDate != null &&
                            galleryUploadDate >= dateRange.first &&
                            galleryUploadDate <= dateRange.second
                    }
                    else -> {
                        val singleDate = parseFirstDate(value)
                        if (singleDate != null) {
                            galleryUploadDate == singleDate
                        } else {
                            normalizedValue.isNotBlank() && normalizedUploadDate.contains(normalizedValue)
                        }
                    }
                }
            }
            "url" -> normalizedValue.isNotBlank() && normalizedSourceUrl.contains(normalizedValue)
            "tag" -> {
                normalizedValue.isNotBlank() &&
                    normalizedTags.any { (name, type) ->
                        name.contains(normalizedValue) || type.contains(normalizedValue)
                    }
            }
            "type" -> {
                normalizedValue.isNotBlank() &&
                    normalizedTags.any { (_, type) -> type.contains(normalizedValue) }
            }
            "artist", "group", "parody", "character", "category", "language" -> {
                normalizedValue.isNotBlank() &&
                    normalizedTags.any { (name, type) ->
                        type == key && name.contains(normalizedValue)
                    }
            }
            "rating", "fetched", "added" -> matchesUniversalTerm(value)
            else -> matchesUniversalTerm(value)
        }
    }

    val freeTerms = extractSearchEverythingBrowserTerms(parsedSearch.freeText)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .ifEmpty { parsedSearch.freeText.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList() }
    if (freeTerms.any { term -> !matchesUniversalTerm(term) }) {
        return false
    }

    for (filter in parsedSearch.filters) {
        if (!matchesFieldFilter(filter.key, filter.value)) {
            return false
        }
    }

    return true
}
