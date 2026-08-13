package com.example.saucetracker

import android.net.Uri
import java.util.Locale

internal fun parseTypedCreatorInput(raw: String): Pair<String, String>? {
    val match = CREATOR_TYPED_INPUT_PATTERN.matchEntire(raw.trim()) ?: return null
    val creatorType = match.groupValues.getOrNull(1).orEmpty().trim().lowercase(Locale.US)
    if (creatorType != "artist" && creatorType != "group") return null
    val value = match.groupValues.getOrNull(2).orEmpty().trim()
    if (value.isBlank()) return null
    return creatorType to value
}

internal fun parseAmbiguousTwoWordCreatorInput(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return null
    if (parseCode(trimmed) != null) return null
    if (parseCreatorLink(trimmed) != null) return null
    if (parseTypedCreatorInput(trimmed) != null) return null
    if (!CREATOR_NAME_LINE_PATTERN.matches(trimmed)) return null

    val normalized = parseCreatorSlug(trimmed)
    if (!isTwoWordCreatorName(normalized)) return null
    return normalized
}

internal fun isTwoWordCreatorName(value: String): Boolean {
    val normalized = parseCreatorSlug(value)
    if (normalized.isBlank()) return false
    val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
    return tokens.size == 2
}

internal fun toHyphenatedTwoWordCreatorName(value: String): String {
    val tokens = parseCreatorSlug(value)
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    if (tokens.size != 2) return value.trim()
    return "${tokens[0]}-${tokens[1]}"
}

internal fun splitTwoWordCreatorName(value: String): List<String> {
    val tokens = parseCreatorSlug(value)
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    return if (tokens.size == 2) tokens else listOf(value.trim()).filter { it.isNotBlank() }
}

internal fun toCreatorUrlSlug(name: String): String {
    val normalized = parseCreatorSlug(name)
    if (normalized.isBlank()) return ""
    return normalized
        .replace(Regex("\\s+"), "-")
        .lowercase(Locale.US)
}

internal fun creatorMatchScore(targetNormalized: String, candidateNormalized: String): Int {
    if (targetNormalized.isBlank() || candidateNormalized.isBlank()) return 0
    if (targetNormalized == candidateNormalized) return 3
    if (candidateNormalized.contains(targetNormalized) || targetNormalized.contains(candidateNormalized)) return 2

    val targetTokens = targetNormalized.split(Regex("\\s+")).filter { it.isNotBlank() }
    val candidateTokens = candidateNormalized.split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
    if (targetTokens.isNotEmpty() && targetTokens.all { it in candidateTokens }) return 1
    return 0
}

internal fun creatorStrictIdentityKey(raw: String): String {
    return parseCreatorSlug(raw)
        .lowercase(Locale.US)
        .let { normalized ->
            Regex("[\\p{L}\\p{N}]+")
                .findAll(normalized)
                .map { it.value }
                .filter { it.isNotBlank() }
                .joinToString(" ")
        }
        .trim()
}

internal fun isStrictCreatorNameMatch(input: String, resolvedName: String): Boolean {
    val inputKey = creatorStrictIdentityKey(input)
    val resolvedKey = creatorStrictIdentityKey(resolvedName)
    return inputKey.isNotBlank() && inputKey == resolvedKey
}

internal fun parseCreatorSlug(rawSlug: String): String {
    var cleaned = rawSlug.trim()
    while (cleaned.isNotEmpty() && URL_TRAILING_PUNCT.contains(cleaned.last())) {
        cleaned = cleaned.dropLast(1)
    }
    if (cleaned.isBlank()) return ""
    val decoded = Uri.decode(cleaned)
        .replace("+", " ")
        .replace("-", " ")
        .replace("_", " ")
    return decoded
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" ")
}

internal fun parseCreatorLink(raw: String): CreatorLink? {
    val match = CREATOR_LINK_PATTERN.matchEntire(raw.trim()) ?: return null
    val creatorType = match.groupValues.getOrNull(1).orEmpty().trim().lowercase(Locale.US)
    if (creatorType != "artist" && creatorType != "group") return null

    var slug = match.groupValues.getOrNull(2).orEmpty().trim()
    while (slug.isNotEmpty() && URL_TRAILING_PUNCT.contains(slug.last())) {
        slug = slug.dropLast(1)
    }
    if (slug.isBlank()) return null

    val creatorName = parseCreatorSlug(slug)
    if (creatorName.isBlank()) return null
    return CreatorLink(
        type = creatorType,
        name = creatorName,
        sourceUrl = "https://nhentai.net/$creatorType/$slug/"
    )
}

internal fun extractCreatorLinks(text: String): Pair<List<CreatorLink>, String> {
    val creators = mutableListOf<CreatorLink>()
    val seen = linkedSetOf<String>()
    CREATOR_LINK_PATTERN.findAll(text).forEach { match ->
        val creatorType = match.groupValues.getOrNull(1).orEmpty().trim().lowercase(Locale.US)
        if (creatorType != "artist" && creatorType != "group") return@forEach
        var slug = match.groupValues.getOrNull(2).orEmpty().trim()
        while (slug.isNotEmpty() && URL_TRAILING_PUNCT.contains(slug.last())) {
            slug = slug.dropLast(1)
        }
        if (slug.isBlank()) return@forEach

        val creatorName = parseCreatorSlug(slug)
        if (creatorName.isBlank()) return@forEach
        val dedupeKey = "$creatorType:${normalizeTagName(creatorName)}"
        if (!seen.add(dedupeKey)) return@forEach

        creators += CreatorLink(
            type = creatorType,
            name = creatorName,
            sourceUrl = "https://nhentai.net/$creatorType/$slug/"
        )
    }
    val codeSourceText = CREATOR_LINK_PATTERN.replace(text, " ")
    return creators to codeSourceText
}

internal fun buildCreatorSlugCandidates(rawInput: String): List<String> {
    var cleaned = rawInput.trim().trim('/')
    while (cleaned.isNotEmpty() && URL_TRAILING_PUNCT.contains(cleaned.last())) {
        cleaned = cleaned.dropLast(1)
    }
    if (cleaned.isBlank()) return emptyList()

    val tokens = parseCreatorSlug(cleaned)
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    val candidates = linkedSetOf<String>()

    fun addCandidate(value: String) {
        val candidate = value.trim().trim('/').trim()
        if (candidate.isBlank()) return
        candidates += candidate
    }

    addCandidate(cleaned)
    addCandidate(cleaned.lowercase(Locale.US))
    if (tokens.isNotEmpty()) {
        addCandidate(tokens.joinToString("-"))
        addCandidate(tokens.joinToString("_"))
        addCandidate(tokens.joinToString("+"))
    }
    return candidates.toList()
}

internal fun extractCreatorNameCandidates(text: String): List<String> {
    val names = linkedSetOf<String>()
    text.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isBlank()) return@forEach
        if (line.length > 60) return@forEach
        val lower = line.lowercase(Locale.US)
        if (lower.startsWith("sauce exported date") || lower.startsWith("format:")) return@forEach
        if (parseCode(line) != null) return@forEach
        if (parseCreatorLink(line) != null) return@forEach
        if (!CREATOR_NAME_LINE_PATTERN.matches(line)) return@forEach
        val normalized = parseCreatorSlug(line)
        val tokenCount = normalized.split(Regex("\\s+")).count { it.isNotBlank() }
        if (normalized.isNotBlank() && tokenCount in 1..6) {
            names += normalized
        }
    }
    return names.toList()
}

