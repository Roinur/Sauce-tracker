package com.example.saucetracker.feature.saucefinder

internal data class SauceImageFingerprint(
    val hashes: LongArray
) {
    init {
        require(hashes.isNotEmpty())
    }

    override fun equals(other: Any?): Boolean =
        other is SauceImageFingerprint && hashes.contentEquals(other.hashes)

    override fun hashCode(): Int = hashes.contentHashCode()
}

internal data class SauceFinderIndexRecord(
    val entryCode: Int,
    val pageNumber: Int,
    val source: String,
    val fingerprint: SauceImageFingerprint
)

internal data class SauceFinderIndexStats(
    val images: Int,
    val entries: Int,
    val bytes: Long
)

internal data class SauceFinderMatch(
    val entryCode: Int,
    val title: String,
    val pageNumber: Int,
    val similarity: Float,
    val confidence: String,
    val thumbnailUrl: String
)

internal data class SauceFinderUiState(
    val indexedImages: Int = 0,
    val indexedEntries: Int = 0,
    val indexBytes: Long = 0L,
    val indexing: Boolean = false,
    val matching: Boolean = false,
    val progress: Float? = null,
    val message: String = "Choose or share an image to search your library.",
    val match: SauceFinderMatch? = null,
    val alternatives: List<SauceFinderMatch> = emptyList(),
    val openRequestNonce: Long = 0L
)

internal fun sauceFingerprintDistance(
    query: SauceImageFingerprint,
    candidate: SauceImageFingerprint
): Float {
    val distances = query.hashes.map { queryHash ->
        candidate.hashes.minOf { candidateHash -> java.lang.Long.bitCount(queryHash xor candidateHash) }
    }.sorted()
    val sampleCount = distances.size.coerceAtMost(3)
    return distances.take(sampleCount).average().toFloat()
}

internal fun sauceFingerprintSimilarity(
    query: SauceImageFingerprint,
    candidate: SauceImageFingerprint
): Float = (1f - sauceFingerprintDistance(query, candidate) / 64f).coerceIn(0f, 1f)

internal fun sauceConfidenceLabel(similarity: Float): String = when {
    similarity >= 0.90f -> "Strong match"
    similarity >= 0.82f -> "Likely match"
    else -> "Possible match"
}
