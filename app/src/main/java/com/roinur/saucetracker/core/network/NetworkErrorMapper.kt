package com.roinur.saucetracker.core.network

internal enum class NetworkFailureKind {
    TEMPORARY,
    BLOCKED,
    PERMANENT,
    INVALID_PAYLOAD
}

internal data class NetworkFailure(
    val kind: NetworkFailureKind,
    val message: String
)

internal object NetworkErrorMapper {
    fun http(operation: String, statusCode: Int): NetworkFailure {
        return when {
            WEBSITE_REQUEST_POLICY.shouldRetry(statusCode) -> NetworkFailure(
                NetworkFailureKind.TEMPORARY,
                "The website is temporarily unavailable (HTTP $statusCode) while $operation. Please try again shortly."
            )
            statusCode == 403 -> NetworkFailure(
                NetworkFailureKind.BLOCKED,
                "The website blocked this request (HTTP 403) while $operation. Try again later or use Browser."
            )
            else -> NetworkFailure(
                NetworkFailureKind.PERMANENT,
                "The website returned HTTP $statusCode while $operation."
            )
        }
    }

    fun invalidPayload(body: String, contentType: String?): NetworkFailure {
        val looksLikeHtml = contentType.orEmpty().contains("html", ignoreCase = true) ||
            body.trimStart().startsWith("<")
        return NetworkFailure(
            NetworkFailureKind.INVALID_PAYLOAD,
            if (looksLikeHtml) {
                "The website returned an HTML page instead of gallery data. This is usually a temporary block or service problem."
            } else {
                "The website returned gallery data in an unexpected format."
            }
        )
    }
}
