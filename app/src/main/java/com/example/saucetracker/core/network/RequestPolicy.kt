package com.example.saucetracker.core.network

internal data class RequestPolicy(
    val maximumAttempts: Int,
    val retryDelaysMillis: List<Long>,
    val retryStatusCodes: Set<Int>
) {
    init {
        require(maximumAttempts >= 1)
        require(retryDelaysMillis.all { it >= 0L })
    }

    fun shouldRetry(statusCode: Int): Boolean = statusCode in retryStatusCodes

    fun delayAfterAttempt(attemptIndex: Int): Long =
        retryDelaysMillis.getOrElse(attemptIndex) { retryDelaysMillis.lastOrNull() ?: 0L }
}

internal val WEBSITE_REQUEST_POLICY = RequestPolicy(
    maximumAttempts = 3,
    retryDelaysMillis = listOf(250L, 750L),
    retryStatusCodes = setOf(408, 429, 500, 502, 503, 504)
)

internal fun shouldRetryWebsiteRequest(statusCode: Int): Boolean =
    WEBSITE_REQUEST_POLICY.shouldRetry(statusCode)
