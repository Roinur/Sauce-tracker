package com.roinur.saucetracker

import com.roinur.saucetracker.core.media.duplicateCandidatePassesWithArtistMismatch
import com.roinur.saucetracker.core.media.duplicateLooksLikeCreatorOnlyFalsePositive
import com.roinur.saucetracker.core.media.duplicateMetadataPrefilter
import com.roinur.saucetracker.core.media.duplicateThumbnailSimilarity

import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil

internal fun parseSearchQuery(raw: String): ParsedSearchQuery {
    val source = raw.trim()
    if (source.isBlank()) return ParsedSearchQuery(freeText = "", filters = emptyList())

    val matches = SEARCH_FIELD_PATTERN.findAll(source).toList()
    if (matches.isEmpty()) {
        return ParsedSearchQuery(freeText = source, filters = emptyList())
    }

    val freeParts = mutableListOf<String>()
    val filters = mutableListOf<SearchFieldFilter>()
    var cursor = 0
    matches.forEachIndexed { index, match ->
        val fieldStart = match.range.first
        val fieldEndExclusive = match.range.last + 1
        if (fieldStart > cursor) {
            val freeTextSlice = source.substring(cursor, fieldStart).trim()
            if (freeTextSlice.isNotBlank()) {
                freeParts += freeTextSlice
            }
        }

        val nextStart = matches.getOrNull(index + 1)?.range?.first ?: source.length
        val rawKey = match.groupValues.getOrNull(1).orEmpty()
        val key = canonicalSearchField(rawKey)
        val value = source.substring(fieldEndExclusive, nextStart).trim()
        if (key.isNotBlank() && value.isNotBlank()) {
            filters += SearchFieldFilter(key = key, value = value)
        }
        cursor = nextStart
    }

    if (cursor < source.length) {
        val tail = source.substring(cursor).trim()
        if (tail.isNotBlank()) {
            freeParts += tail
        }
    }

    return ParsedSearchQuery(
        freeText = freeParts.joinToString(" ").trim(),
        filters = filters
    )
}

internal fun canonicalSearchField(rawKey: String): String {
    val normalized = rawKey.trim().lowercase(Locale.US).replace(Regex("\\s+"), " ")
    return when (normalized) {
        "code" -> "code"
        "title" -> "title"
        "subtitle" -> "subtitle"
        "page", "pages", "num pages" -> "pages"
        "upload", "upload date" -> "upload"
        "rating" -> "rating"
        "fetched", "fetched at" -> "fetched"
        "added", "added at" -> "added"
        "url", "source", "source url", "link" -> "url"
        "tag", "tags" -> "tag"
        "artist" -> "artist"
        "group" -> "group"
        "parody" -> "parody"
        "character" -> "character"
        "category" -> "category"
        "language", "lang" -> "language"
        "type" -> "type"
        else -> ""
    }
}

internal fun parseDateRange(raw: String): Pair<LocalDate, LocalDate>? {
    val dates = DATE_TOKEN_PATTERN.findAll(raw)
        .mapNotNull { match ->
            runCatching {
                LocalDate.parse(match.value, UPLOAD_DATE_FORMAT)
            }.getOrNull()
        }
        .toList()
    if (dates.size < 2) return null
    val first = dates[0]
    val second = dates[1]
    return if (first <= second) {
        first to second
    } else {
        second to first
    }
}

internal fun parseFirstDate(raw: String): LocalDate? {
    val token = DATE_TOKEN_PATTERN.find(raw)?.value ?: return null
    return runCatching {
        LocalDate.parse(token, UPLOAD_DATE_FORMAT)
    }.getOrNull()
}

internal fun formatDurationFromSeconds(totalSeconds: Long): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0L)
    val hours = safeSeconds / 3600L
    val minutes = (safeSeconds % 3600L) / 60L
    val seconds = safeSeconds % 60L
    return when {
        hours > 0L -> String.format(Locale.US, "%dh %dm", hours, minutes)
        minutes > 0L -> String.format(Locale.US, "%dm %ds", minutes, seconds)
        else -> String.format(Locale.US, "%ds", seconds)
    }
}

internal fun buildEtaTextForEntry(numPages: Int, analyticsSnapshot: ReadAnalyticsSnapshot): String {
    if (numPages <= 0) {
        return "ETA unavailable (read more to calibrate)."
    }

    val monthly = analyticsSnapshot.readingSpeed[StatsRange.MONTH]
    val allTime = analyticsSnapshot.readingSpeed[StatsRange.ALL_TIME]
    val baseline = when {
        monthly?.hasEnoughData == true -> StatsRange.MONTH to monthly
        allTime?.hasEnoughData == true -> StatsRange.ALL_TIME to allTime
        else -> null
    } ?: return "ETA unavailable (read more to calibrate)."

    val pagesPerMinute = baseline.second.pagesPerMinute
    if (pagesPerMinute <= 0f) {
        return "ETA unavailable (read more to calibrate)."
    }

    val etaMinutes = ceil(numPages.toDouble() / pagesPerMinute.toDouble()).toInt().coerceAtLeast(1)
    val etaText = if (etaMinutes >= 60) {
        val h = etaMinutes / 60
        val m = etaMinutes % 60
        if (m == 0) "${h}h" else "${h}h ${m}m"
    } else {
        "${etaMinutes}m"
    }
    val basisLabel = if (baseline.first == StatsRange.MONTH) "month" else "all time"
    return "Estimated time to finish: $etaText (based on $basisLabel speed)"
}

internal fun normalizeDuplicateTitleKey(raw: String): String {
    return raw
        .trim()
        .lowercase(Locale.US)
        .replace("&", " and ")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

internal fun duplicateTrigramSet(value: String): Set<String> {
    val compact = value.trim()
    if (compact.isBlank()) return emptySet()
    if (compact.length < 3) return setOf(compact)
    val set = linkedSetOf<String>()
    for (index in 0..(compact.length - 3)) {
        set += compact.substring(index, index + 3)
    }
    return set
}

internal fun duplicateTitleSimilarity(left: String, right: String): Float {
    val a = normalizeDuplicateTitleKey(left)
    val b = normalizeDuplicateTitleKey(right)
    if (a.isBlank() || b.isBlank()) return 0f
    if (a == b) return 1f
    val leftSet = duplicateTrigramSet(a)
    val rightSet = duplicateTrigramSet(b)
    if (leftSet.isEmpty() || rightSet.isEmpty()) return 0f
    val overlap = leftSet.intersect(rightSet).size.toFloat()
    return ((2f * overlap) / (leftSet.size + rightSet.size).toFloat()).coerceIn(0f, 1f)
}

internal fun duplicateJaccard(left: Set<String>, right: Set<String>): Float {
    if (left.isEmpty() || right.isEmpty()) return 0f
    val union = left.union(right)
    if (union.isEmpty()) return 0f
    val intersection = left.intersect(right)
    return (intersection.size.toFloat() / union.size.toFloat()).coerceIn(0f, 1f)
}

internal fun computeLocalDuplicateSeedVersion(seeds: List<LocalDuplicateSeed>): Int {
    var result = 17
    seeds.forEach { seed ->
        result = (31 * result) + seed.code
        result = (31 * result) + seed.numPages
        result = (31 * result) + seed.uploadDate.hashCode()
        result = (31 * result) + seed.mediaId.hashCode()
    }
    return result
}

internal fun buildLocalDuplicateSeedIndex(seeds: List<LocalDuplicateSeed>): LocalDuplicateSeedIndex {
    if (seeds.isEmpty()) {
        return LocalDuplicateSeedIndex(
            allSeeds = emptyList(),
            byCode = emptyMap(),
            byMediaId = emptyMap(),
            byPageCount = emptyMap(),
            byUploadDate = emptyMap(),
            byTitleKey = emptyMap(),
            byTitleTrigram = emptyMap()
        )
    }
    val dedupedSeeds = seeds
        .asSequence()
        .filter { it.code > 0 }
        .distinctBy { it.code }
        .toList()
    return LocalDuplicateSeedIndex(
        allSeeds = dedupedSeeds,
        byCode = dedupedSeeds.associateBy { it.code },
        byMediaId = dedupedSeeds
            .asSequence()
            .filter { it.mediaId > 0L }
            .groupBy { it.mediaId },
        byPageCount = dedupedSeeds
            .asSequence()
            .filter { it.numPages > 0 }
            .groupBy { it.numPages },
        byUploadDate = dedupedSeeds
            .asSequence()
            .filter { it.uploadDate.isNotBlank() }
            .groupBy { it.uploadDate },
        byTitleKey = dedupedSeeds
            .asSequence()
            .filter { it.titleKey.isNotBlank() }
            .groupBy { it.titleKey },
        byTitleTrigram = buildMap {
            dedupedSeeds.forEach { seed ->
                duplicateTrigramSet(seed.titleKey).forEach trigramLoop@{ trigram ->
                    if (trigram.isBlank()) return@trigramLoop
                    put(trigram, (get(trigram).orEmpty() + seed))
                }
            }
        }
    )
}

internal fun collectLocalDuplicateCandidateSeeds(
    index: LocalDuplicateSeedIndex,
    candidateCode: Int,
    candidateTitle: String,
    candidateNumPages: Int,
    candidateUploadDate: String,
    candidateMediaId: Long
): List<LocalDuplicateSeed> {
    if (index.allSeeds.isEmpty()) return emptyList()
    val out = linkedMapOf<Int, LocalDuplicateSeed>()

    fun addSeeds(seeds: Iterable<LocalDuplicateSeed>) {
        seeds.forEach { seed ->
            if (seed.code <= 0 || seed.code == candidateCode) return@forEach
            out.putIfAbsent(seed.code, seed)
        }
    }

    if (candidateMediaId > 0L) {
        addSeeds(index.byMediaId[candidateMediaId].orEmpty())
    }
    if (candidateNumPages > 0) {
        for (pages in (candidateNumPages - 2)..(candidateNumPages + 2)) {
            if (pages <= 0) continue
            addSeeds(index.byPageCount[pages].orEmpty())
        }
    }
    val uploadDateKey = candidateUploadDate.trim()
    if (uploadDateKey.isNotBlank()) {
        addSeeds(index.byUploadDate[uploadDateKey].orEmpty())
    }
    val titleKey = normalizeDuplicateTitleKey(candidateTitle)
    if (titleKey.isNotBlank()) {
        addSeeds(index.byTitleKey[titleKey].orEmpty())
        val trigramScores = linkedMapOf<Int, Int>()
        duplicateTrigramSet(titleKey).forEach { trigram ->
            index.byTitleTrigram[trigram].orEmpty().forEach seedLoop@{ seed ->
                if (seed.code <= 0 || seed.code == candidateCode) return@seedLoop
                trigramScores[seed.code] = (trigramScores[seed.code] ?: 0) + 1
            }
        }
        trigramScores.entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .forEach { entry ->
                index.byCode[entry.key]?.let { seed -> out.putIfAbsent(seed.code, seed) }
            }
    }

    return out.values.toList()
}

internal fun findLikelyDuplicateHint(
    candidateCode: Int,
    candidateTitle: String,
    candidateNumPages: Int,
    candidateUploadDate: String,
    candidateMediaId: Long,
    candidateArtistKeys: Set<String>,
    candidateGroupKeys: Set<String>,
    candidateTagKeys: Set<String>,
    candidateThumbnailUrl: String,
    localSeeds: List<LocalDuplicateSeed>
): DuplicateHint? {
    if (localSeeds.isEmpty()) return null
    val candidateTitleKey = normalizeDuplicateTitleKey(candidateTitle)
    if (candidateTitleKey.isBlank() &&
        candidateArtistKeys.isEmpty() &&
        candidateGroupKeys.isEmpty() &&
        candidateTagKeys.isEmpty() &&
        candidateMediaId <= 0L &&
        candidateThumbnailUrl.isBlank()
    ) {
        return null
    }

    var bestHint: DuplicateHint? = null
    localSeeds.forEach { seed ->
        if (seed.code <= 0 || seed.code == candidateCode) return@forEach
        val sameMedia = candidateMediaId > 0L && seed.mediaId > 0L && candidateMediaId == seed.mediaId
        val titleSimilarity = duplicateTitleSimilarity(candidateTitleKey, seed.titleKey)
        val artistOverlap = if (candidateArtistKeys.isEmpty() || seed.artistKeys.isEmpty()) {
            0
        } else {
            candidateArtistKeys.intersect(seed.artistKeys).size
        }
        val groupOverlap = if (candidateGroupKeys.isEmpty() || seed.groupKeys.isEmpty()) {
            0
        } else {
            candidateGroupKeys.intersect(seed.groupKeys).size
        }
        val tagSimilarity = duplicateJaccard(candidateTagKeys, seed.tagKeys)
        val samePagesExact = candidateNumPages > 0 && seed.numPages > 0 && candidateNumPages == seed.numPages
        val pagesClose = candidateNumPages > 0 && seed.numPages > 0 &&
            abs(candidateNumPages - seed.numPages) <= 2
        val sameUploadDate = candidateUploadDate.isNotBlank() &&
            seed.uploadDate.isNotBlank() &&
            candidateUploadDate == seed.uploadDate
        val artistMismatchNoGroup =
            candidateArtistKeys.isNotEmpty() &&
                seed.artistKeys.isNotEmpty() &&
                artistOverlap == 0 &&
                groupOverlap == 0
        val shouldCompareThumbnail = duplicateMetadataPrefilter(
            titleSimilarity = titleSimilarity,
            tagSimilarity = tagSimilarity,
            artistOverlap = artistOverlap,
            groupOverlap = groupOverlap,
            samePagesExact = samePagesExact,
            pagesClose = pagesClose,
            sameUploadDate = sameUploadDate,
            sameMedia = sameMedia,
            candidateHasThumbnail = candidateThumbnailUrl.isNotBlank(),
            seedHasThumbnail = seed.thumbnailUrl.isNotBlank()
        )
        val thumbnailSimilarity = if (shouldCompareThumbnail) {
            duplicateThumbnailSimilarity(
                candidateThumbnailUrl = candidateThumbnailUrl,
                seedThumbnailUrl = seed.thumbnailUrl,
                candidateCode = candidateCode,
                seedCode = seed.code
            )
        } else {
            0f
        }

        var score = 0f
        if (sameMedia) score += 0.22f
        score += when {
            thumbnailSimilarity >= 0.95f -> 0.88f
            thumbnailSimilarity >= 0.90f -> 0.74f
            thumbnailSimilarity >= 0.84f -> 0.52f
            thumbnailSimilarity >= 0.76f -> 0.30f
            else -> 0f
        }
        score += when {
            titleSimilarity >= 0.94f -> 0.20f
            titleSimilarity >= 0.86f -> 0.14f
            titleSimilarity >= 0.76f -> 0.08f
            else -> 0f
        }
        score += when {
            artistOverlap >= 2 -> 0.08f
            artistOverlap == 1 -> 0.05f
            else -> 0f
        }
        score += when {
            groupOverlap >= 2 -> 0.16f
            groupOverlap == 1 -> 0.11f
            else -> 0f
        }
        score += when {
            tagSimilarity >= 0.72f -> 0.16f
            tagSimilarity >= 0.52f -> 0.11f
            tagSimilarity >= 0.36f -> 0.06f
            else -> 0f
        }
        if (samePagesExact) {
            score += 0.08f
        } else if (pagesClose) {
            score += 0.04f
        }
        if (sameUploadDate) score += 0.05f
        if (artistMismatchNoGroup) score -= 0.42f

        val corroborationCount = listOf(
            sameMedia,
            titleSimilarity >= 0.56f,
            tagSimilarity >= 0.30f,
            samePagesExact,
            pagesClose,
            sameUploadDate,
            artistOverlap > 0,
            groupOverlap > 0
        ).count { it }
        val creatorOnlyFalsePositive = duplicateLooksLikeCreatorOnlyFalsePositive(
            artistOverlap = artistOverlap,
            groupOverlap = groupOverlap,
            titleSimilarity = titleSimilarity,
            tagSimilarity = tagSimilarity,
            thumbnailSimilarity = thumbnailSimilarity
        )
        val passesArtistMismatch = duplicateCandidatePassesWithArtistMismatch(
            artistMismatchNoGroup = artistMismatchNoGroup,
            thumbnailSimilarity = thumbnailSimilarity,
            tagSimilarity = tagSimilarity,
            titleSimilarity = titleSimilarity
        )

        val strongThumbnail = thumbnailSimilarity >= 0.93f
        val mediumThumbnail = thumbnailSimilarity >= 0.86f
        val weakThumbnail = thumbnailSimilarity >= 0.76f
        val strictMetadataFallback =
            titleSimilarity >= 0.95f &&
                tagSimilarity >= 0.72f &&
                samePagesExact &&
                (artistOverlap > 0 || groupOverlap > 0 || sameUploadDate) &&
                !artistMismatchNoGroup

        val likelyDuplicate = when {
            strongThumbnail -> passesArtistMismatch && !creatorOnlyFalsePositive && corroborationCount >= 1
            mediumThumbnail -> passesArtistMismatch && !creatorOnlyFalsePositive && corroborationCount >= 2
            weakThumbnail -> passesArtistMismatch &&
                !creatorOnlyFalsePositive &&
                corroborationCount >= 3 &&
                (titleSimilarity >= 0.56f || tagSimilarity >= 0.32f || samePagesExact || sameMedia)
            sameMedia -> strictMetadataFallback
            else -> strictMetadataFallback && score >= 0.70f
        }
        if (!likelyDuplicate || score < 0.48f) return@forEach

        val reasons = buildList {
            if (thumbnailSimilarity >= 0.90f) add("thumbnail match")
            else if (thumbnailSimilarity >= 0.84f) add("thumbnail similar")
            if (sameMedia) add("same media id")
            if (artistOverlap > 0) add("artist overlap")
            if (groupOverlap > 0) add("group overlap")
            if (titleSimilarity >= 0.82f) add("title match")
            if (tagSimilarity >= 0.52f) add("tag overlap")
            if (samePagesExact) add("same pages")
            if (sameUploadDate) add("same upload date")
        }
        val hint = DuplicateHint(
            matchedCode = seed.code,
            score = score.coerceIn(0f, 1.5f),
            reason = reasons.take(2).joinToString(", ").ifBlank { "metadata similarity" }
        )
        if (bestHint == null || hint.score > bestHint!!.score) {
            bestHint = hint
        }
    }

    return bestHint
}

internal fun extractNumericTokens(raw: String): List<Int> {
    return Regex("\\d+").findAll(raw)
        .mapNotNull { match -> match.value.toIntOrNull() }
        .toList()
}
