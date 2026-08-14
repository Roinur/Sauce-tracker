package com.roinur.saucetracker.core.network

import com.roinur.saucetracker.GalleryFetchException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

internal class TemporaryWebsiteException(message: String) : GalleryFetchException(message)

internal fun executeWebsiteRequestWithRetry(
    client: OkHttpClient,
    request: Request,
    operation: String
): Response {
    var lastNetworkError: IOException? = null
    repeat(WEBSITE_REQUEST_POLICY.maximumAttempts) { attempt ->
        try {
            val response = client.newCall(request).execute()
            if (!WEBSITE_REQUEST_POLICY.shouldRetry(response.code) ||
                attempt == WEBSITE_REQUEST_POLICY.maximumAttempts - 1
            ) {
                return response
            }
            response.close()
        } catch (error: IOException) {
            lastNetworkError = error
            if (attempt == WEBSITE_REQUEST_POLICY.maximumAttempts - 1) {
                throw TemporaryWebsiteException(
                    "Network problem while $operation. Check your connection and try again."
                )
            }
        }
        Thread.sleep(WEBSITE_REQUEST_POLICY.delayAfterAttempt(attempt))
    }
    throw TemporaryWebsiteException(
        "Network problem while $operation: ${lastNetworkError?.message ?: "connection failed"}"
    )
}

internal fun websiteHttpFailure(operation: String, statusCode: Int): GalleryFetchException {
    val failure = NetworkErrorMapper.http(operation, statusCode)
    return if (failure.kind == NetworkFailureKind.TEMPORARY) {
        TemporaryWebsiteException(failure.message)
    } else {
        GalleryFetchException(failure.message)
    }
}

internal fun websiteHttpFailureMessage(operation: String, statusCode: Int): String {
    return NetworkErrorMapper.http(operation, statusCode).message
}

internal fun invalidGalleryResponseMessage(body: String, contentType: String?): String {
    return NetworkErrorMapper.invalidPayload(body, contentType).message
}
