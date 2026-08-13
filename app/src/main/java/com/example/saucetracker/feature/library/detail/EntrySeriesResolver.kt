package com.example.saucetracker.feature.library.detail

import com.example.saucetracker.EntryDetail
import com.example.saucetracker.SeriesCandidateRow
import com.example.saucetracker.SeriesEntryPreview
import com.example.saucetracker.SeriesNeighbors
import com.example.saucetracker.normalizeTagName
import java.util.Locale

internal object EntrySeriesResolver {
    private data class SeriesTitleAnalysis(
        val baseKey: String,
        val tokens: Set<String>,
        val sequence: Int?,
        val explicitSequenceHint: Boolean
    )

    fun resolve(
        current: EntryDetail,
        candidates: List<SeriesCandidateRow>
    ): SeriesNeighbors {
        val currentAnalysis = analyzeSeriesTitle(current.title, current.subtitle)
        if (currentAnalysis.baseKey.isBlank() || currentAnalysis.tokens.isEmpty()) {
            return SeriesNeighbors()
        }
        val currentCreatorKeys = (current.tagsByType["artist"].orEmpty() + current.tagsByType["group"].orEmpty())
            .asSequence()
            .map { normalizeTagName(it) }
            .filter { it.isNotBlank() }
            .toSet()

        data class ScoredCandidate(
            val preview: SeriesEntryPreview,
            val analysis: SeriesTitleAnalysis,
            val tokenOverlap: Int,
            val trigramScore: Float,
            val creatorOverlap: Int,
            val familyHint: Boolean
        )

        val scored = candidates.asSequence()
            .filter { it.code != current.code }
            .mapNotNull { candidate ->
                val analysis = analyzeSeriesTitle(candidate.title, candidate.subtitle)
                if (analysis.baseKey.isBlank() || analysis.tokens.isEmpty()) return@mapNotNull null

                val tokenOverlap = tokenIntersection(currentAnalysis.tokens, analysis.tokens)
                val trigramScore = trigramDice(currentAnalysis.baseKey, analysis.baseKey)
                val creatorOverlap = tokenIntersection(currentCreatorKeys, candidate.creatorKeys)
                val creatorBonus = when {
                    creatorOverlap >= 2 -> 0.18f
                    creatorOverlap == 1 -> 0.12f
                    else -> 0f
                }
                val sequenceSignal = currentAnalysis.sequence != null || analysis.sequence != null
                val explicitFamilyMatch =
                    currentAnalysis.baseKey == analysis.baseKey &&
                        (currentAnalysis.explicitSequenceHint || analysis.explicitSequenceHint)
                val familyHint =
                    explicitFamilyMatch ||
                        tokenOverlap >= 2 ||
                        (creatorOverlap > 0 && trigramScore >= 0.62f)
                val score = (seriesSimilarity(currentAnalysis, analysis) + creatorBonus)
                    .coerceIn(0f, 1f)
                val minimumThreshold = when {
                    explicitFamilyMatch -> 0f
                    creatorOverlap > 0 && sequenceSignal -> 0.58f
                    currentAnalysis.tokens.size <= 2 || analysis.tokens.size <= 2 -> 0.82f
                    else -> 0.72f
                }

                if (!explicitFamilyMatch && score < minimumThreshold) return@mapNotNull null
                if (!explicitFamilyMatch) {
                    val passesGate = when {
                        tokenOverlap >= 1 -> true
                        creatorOverlap > 0 && sequenceSignal && trigramScore >= 0.62f -> true
                        trigramScore >= 0.92f -> true
                        else -> false
                    }
                    if (!passesGate) return@mapNotNull null
                }
                if (!explicitFamilyMatch &&
                    creatorOverlap <= 0 &&
                    tokenOverlap < 2 &&
                    trigramScore < 0.75f
                ) {
                    return@mapNotNull null
                }

                ScoredCandidate(
                    preview = SeriesEntryPreview(
                        code = candidate.code,
                        title = candidate.title.ifBlank { "Gallery ${candidate.code}" },
                        sequence = analysis.sequence,
                        score = score,
                        thumbnailUrl = candidate.thumbnailUrl,
                        numPages = candidate.numPages
                    ),
                    analysis = analysis,
                    tokenOverlap = tokenOverlap,
                    trigramScore = trigramScore,
                    creatorOverlap = creatorOverlap,
                    familyHint = familyHint
                )
            }
            .sortedWith(
                compareByDescending<ScoredCandidate> { it.preview.score }
                    .thenBy { it.preview.code }
            )
            .toList()

        if (scored.isEmpty()) return SeriesNeighbors()

        val currentSequence = currentAnalysis.sequence
        val sequenceFamily = scored
            .filter {
                it.preview.sequence != null &&
                    (
                        it.familyHint ||
                            it.analysis.baseKey == currentAnalysis.baseKey ||
                            it.creatorOverlap > 0
                        )
            }
            .map { it.preview }
            .sortedBy { it.sequence ?: Int.MAX_VALUE }

        val previous = if (currentSequence != null) {
            sequenceFamily.lastOrNull { (it.sequence ?: Int.MAX_VALUE) < currentSequence }
        } else {
            scored.firstOrNull()?.preview
        }

        val next = if (currentSequence != null) {
            sequenceFamily.firstOrNull { (it.sequence ?: Int.MIN_VALUE) > currentSequence }
        } else {
            scored.firstOrNull { it.preview.code != previous?.code }?.preview
        }

        if (previous == null && next == null) return SeriesNeighbors()
        val currentPreview = SeriesEntryPreview(
            code = current.code,
            title = current.title.ifBlank { "Gallery ${current.code}" },
            sequence = currentSequence,
            score = 1f,
            thumbnailUrl = current.thumbnailUrl,
            numPages = current.numPages
        )
        val parts = if (currentSequence != null) {
            (sequenceFamily + currentPreview)
                .distinctBy { it.code }
                .sortedWith(compareBy<SeriesEntryPreview> { it.sequence ?: Int.MAX_VALUE }.thenBy { it.code })
        } else {
            listOfNotNull(previous, currentPreview, next).distinctBy { it.code }
        }
        return SeriesNeighbors(
            previous = previous,
            next = next,
            parts = parts,
            currentPartIndex = parts.indexOfFirst { it.code == current.code }
        )
    }

    private fun analyzeSeriesTitle(title: String, subtitle: String): SeriesTitleAnalysis {
        val merged = listOf(title.trim(), subtitle.trim())
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .lowercase(Locale.US)
            .replace("&", " and ")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (merged.isBlank()) {
            return SeriesTitleAnalysis(baseKey = "", tokens = emptySet(), sequence = null, explicitSequenceHint = false)
        }

        var working = merged
        var sequence: Int? = null
        var explicitHint = false

        val markerAfter = Regex("\\b(part|pt|chapter|ch|volume|vol|episode|ep|book)\\s*([0-9]{1,3}|[ivxlcdm]{1,6})\\b")
        val markerBefore = Regex("\\b([0-9]{1,3}|[ivxlcdm]{1,6})\\s*(part|pt|chapter|ch|volume|vol|episode|ep|book)\\b")
        val markerMatch = markerAfter.find(working) ?: markerBefore.find(working)
        if (markerMatch != null) {
            val numberGroup = markerMatch.groupValues
                .drop(1)
                .firstOrNull { it.matches(Regex("[0-9]{1,3}|[ivxlcdm]{1,6}")) }
                .orEmpty()
            sequence = parseSeriesNumberToken(numberGroup)
            explicitHint = sequence != null
            if (explicitHint) {
                working = working.removeRange(markerMatch.range).trim()
            }
        }

        if (sequence == null) {
            val trailing = Regex("(.+?)(?:\\s+|\\s*[-:_]\\s*)([0-9]{1,2}|[ivxlcdm]{1,6})\\s*$").find(working)
            if (trailing != null) {
                val parsed = parseSeriesNumberToken(trailing.groupValues.getOrNull(2).orEmpty())
                if (parsed != null) {
                    sequence = parsed
                    explicitHint = true
                    working = trailing.groupValues.getOrNull(1).orEmpty().trim()
                }
            }
        }

        if (sequence == null) {
            // Handle compact titles like "Series2" where number is attached.
            val attachedTrailingDigits = Regex("(.+?)([0-9]{1,2})\\s*$").find(working)
            if (attachedTrailingDigits != null) {
                val parsed = parseSeriesNumberToken(attachedTrailingDigits.groupValues.getOrNull(2).orEmpty())
                val stem = attachedTrailingDigits.groupValues.getOrNull(1).orEmpty().trim()
                if (parsed != null && stem.any { it.isLetter() }) {
                    sequence = parsed
                    explicitHint = true
                    working = stem
                }
            }
        }

        if (sequence == null) {
            // Generic infix parsing: detect a part-number token between title words.
            val words = working.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.size >= 3) {
                val index = (1 until words.lastIndex)
                    .lastOrNull { parseSeriesNumberToken(words[it]) != null }
                if (index != null) {
                    val parsed = parseSeriesNumberToken(words[index])
                    val prefixWords = words.subList(0, index)
                    val suffixWords = words.subList(index + 1, words.size)
                    val prefix = prefixWords.joinToString(" ").trim()
                    val prefixLetterTokens = prefixWords.count { token -> token.any { ch -> ch.isLetter() } }
                    val suffixHasLetterToken = suffixWords.any { token -> token.any { ch -> ch.isLetter() } }
                    if (parsed != null &&
                        suffixWords.isNotEmpty() &&
                        prefixLetterTokens >= 1 &&
                        suffixHasLetterToken &&
                        prefix.any { it.isLetter() }
                    ) {
                        sequence = parsed
                        explicitHint = true
                        working = prefix
                    }
                }
            }
        }

        val stopWords = setOf(
            "the", "a", "an", "of", "to", "for", "in", "on", "at", "and",
            "part", "pt", "chapter", "ch", "volume", "vol", "episode", "ep", "book"
        )
        val rawTokens = working.split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toCollection(linkedSetOf())
        val tokens = rawTokens
            .filterNot { token ->
                token in stopWords ||
                    token.matches(Regex("\\d{1,2}"))
            }
            .toCollection(linkedSetOf())
        val normalizedTokens = if (tokens.isNotEmpty()) {
            tokens
        } else {
            rawTokens
                .filterNot { token -> token.matches(Regex("\\d{1,2}")) }
                .toCollection(linkedSetOf())
        }

        val baseKey = normalizedTokens.joinToString(" ")
        return SeriesTitleAnalysis(
            baseKey = baseKey,
            tokens = normalizedTokens,
            sequence = sequence,
            explicitSequenceHint = explicitHint
        )
    }

    private fun parseSeriesNumberToken(raw: String): Int? {
        val cleaned = raw.trim().lowercase(Locale.US)
        if (cleaned.isBlank()) return null
        val numeric = cleaned.toIntOrNull()
            ?: romanToInt(cleaned)
            ?: return null
        return numeric.takeIf { it in 1..40 }
    }

    private fun romanToInt(raw: String): Int? {
        val roman = raw.trim().uppercase(Locale.US)
        if (!roman.matches(Regex("[IVXLCDM]+"))) return null
        val values = mapOf(
            'I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000
        )
        var total = 0
        var prev = 0
        for (ch in roman.reversed()) {
            val value = values[ch] ?: return null
            if (value < prev) total -= value else total += value
            prev = value
        }
        return total
    }

    private fun seriesSimilarity(a: SeriesTitleAnalysis, b: SeriesTitleAnalysis): Float {
        if (a.baseKey.isBlank() || b.baseKey.isBlank()) return 0f
        if (a.baseKey == b.baseKey) return 1f

        val tokenScore = tokenJaccard(a.tokens, b.tokens)
        val ngramScore = trigramDice(a.baseKey, b.baseKey)
        val containmentBonus = if (
            a.baseKey.contains(b.baseKey) || b.baseKey.contains(a.baseKey)
        ) {
            0.08f
        } else {
            0f
        }
        return (0.58f * tokenScore + 0.42f * ngramScore + containmentBonus)
            .coerceIn(0f, 1f)
    }

    private fun tokenIntersection(a: Set<String>, b: Set<String>): Int {
        if (a.isEmpty() || b.isEmpty()) return 0
        return a.count { it in b }
    }

    private fun tokenJaccard(a: Set<String>, b: Set<String>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val intersection = a.count { it in b }.toFloat()
        val union = (a.size + b.size - intersection).coerceAtLeast(1f)
        return (intersection / union).coerceIn(0f, 1f)
    }

    private fun trigramDice(left: String, right: String): Float {
        val a = ngramSet(left, n = 3)
        val b = ngramSet(right, n = 3)
        if (a.isEmpty() || b.isEmpty()) return 0f
        val overlap = a.count { it in b }.toFloat()
        return ((2f * overlap) / (a.size + b.size).toFloat()).coerceIn(0f, 1f)
    }

    private fun ngramSet(value: String, n: Int): Set<String> {
        val normalized = value.trim().lowercase(Locale.US).replace(Regex("\\s+"), " ")
        if (normalized.length < n) {
            return if (normalized.isBlank()) emptySet() else setOf(normalized)
        }
        val out = linkedSetOf<String>()
        for (idx in 0..(normalized.length - n)) {
            out += normalized.substring(idx, idx + n)
        }
        return out
    }
}
