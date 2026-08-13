package com.example.saucetracker.core.diagnostics

import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

internal object PerformanceMetrics {
    private val thumbnailCacheHitCounter = AtomicLong()
    private val thumbnailCacheMissCounter = AtomicLong()
    private val thumbnailLoadCounter = AtomicLong()
    private val duplicateCheckCounter = AtomicLong()
    private val suggestionSearchRequestCounter = AtomicLong()
    private val suggestionMetadataCacheHitCounter = AtomicLong()
    private val suggestionMetadataNetworkCounter = AtomicLong()
    private val suggestionProfileMillisValue = AtomicLong()
    private val suggestionSearchMillisValue = AtomicLong()
    private val suggestionFirstVisibleMillisValue = AtomicLong()
    private val suggestionTotalMillisValue = AtomicLong()
    private val suggestionCandidateCountValue = AtomicLong()
    private val suggestionDuplicateMillisValue = AtomicLong()

    val thumbnailCacheHits: Long get() = thumbnailCacheHitCounter.get()
    val thumbnailCacheMisses: Long get() = thumbnailCacheMissCounter.get()
    val thumbnailLoadsCompleted: Long get() = thumbnailLoadCounter.get()
    val duplicateChecksStarted: Long get() = duplicateCheckCounter.get()
    val suggestionSearchRequests: Long get() = suggestionSearchRequestCounter.get()
    val suggestionMetadataCacheHits: Long get() = suggestionMetadataCacheHitCounter.get()
    val suggestionMetadataNetworkFetches: Long get() = suggestionMetadataNetworkCounter.get()
    val suggestionProfileMillis: Long get() = suggestionProfileMillisValue.get()
    val suggestionSearchMillis: Long get() = suggestionSearchMillisValue.get()
    val suggestionFirstVisibleMillis: Long get() = suggestionFirstVisibleMillisValue.get()
    val suggestionTotalMillis: Long get() = suggestionTotalMillisValue.get()
    val suggestionCandidateCount: Long get() = suggestionCandidateCountValue.get()
    val suggestionDuplicateMillis: Long get() = suggestionDuplicateMillisValue.get()

    fun recordThumbnailCacheHit() { thumbnailCacheHitCounter.incrementAndGet() }
    fun recordThumbnailCacheMiss() { thumbnailCacheMissCounter.incrementAndGet() }
    fun recordThumbnailLoadCompleted() { thumbnailLoadCounter.incrementAndGet() }
    fun recordDuplicateCheckStarted() { duplicateCheckCounter.incrementAndGet() }
    fun recordSuggestionSearchRequest() { suggestionSearchRequestCounter.incrementAndGet() }
    fun recordSuggestionMetadataCacheHit() { suggestionMetadataCacheHitCounter.incrementAndGet() }
    fun recordSuggestionMetadataNetworkFetch() { suggestionMetadataNetworkCounter.incrementAndGet() }
    fun recordSuggestionProfileMillis(value: Long) { suggestionProfileMillisValue.set(value.coerceAtLeast(0L)) }
    fun recordSuggestionSearchMillis(value: Long) { suggestionSearchMillisValue.set(value.coerceAtLeast(0L)) }
    fun recordSuggestionFirstVisibleMillis(value: Long) { suggestionFirstVisibleMillisValue.set(value.coerceAtLeast(0L)) }
    fun recordSuggestionTotalMillis(value: Long) { suggestionTotalMillisValue.set(value.coerceAtLeast(0L)) }
    fun recordSuggestionCandidateCount(value: Int) { suggestionCandidateCountValue.set(value.coerceAtLeast(0).toLong()) }
    fun recordSuggestionDuplicateMillis(value: Long) { suggestionDuplicateMillisValue.set(value.coerceAtLeast(0L)) }

    fun thumbnailCacheHitRatePercent(): Int {
        val hits = thumbnailCacheHits
        val total = hits + thumbnailCacheMisses
        if (total <= 0L) return 0
        return ((hits.toDouble() / total.toDouble()) * 100.0).roundToInt()
    }
}
