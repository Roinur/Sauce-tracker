package com.example.saucetracker.core.network

import okhttp3.ConnectionPool
import okhttp3.CookieJar
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Cache
import java.util.concurrent.TimeUnit

internal enum class HttpClientProfile {
    GALLERY_METADATA,
    SUGGESTIONS,
    BROWSER,
    THUMBNAIL,
    BROWSER_IMAGE,
    SLIDESHOW,
    DOWNLOAD
}

internal object HttpClientFactory {
    fun create(profile: HttpClientProfile, cache: Cache? = null): OkHttpClient {
        val builder = OkHttpClient.Builder()
        if (cache != null) {
            builder.cache(cache)
            if (profile == HttpClientProfile.THUMBNAIL) {
                // Gallery cover URLs are content-addressed by media id and safe to retain across
                // process deaths and app updates even when the CDN omits useful cache headers.
                builder.addNetworkInterceptor { chain ->
                    val response = chain.proceed(chain.request())
                    val isImage = response.header("Content-Type")
                        ?.startsWith("image/", ignoreCase = true) == true
                    if (response.isSuccessful && isImage) {
                        response.newBuilder()
                            .header("Cache-Control", "public, max-age=2592000")
                            .build()
                    } else {
                        response
                    }
                }
            }
        }
        when (profile) {
            HttpClientProfile.GALLERY_METADATA -> builder
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
            HttpClientProfile.SUGGESTIONS -> builder
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .callTimeout(15, TimeUnit.SECONDS)
            HttpClientProfile.BROWSER -> builder
                .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
                .cookieJar(CookieJar.NO_COOKIES)
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .callTimeout(15, TimeUnit.SECONDS)
            HttpClientProfile.THUMBNAIL -> builder
                .dispatcher(dispatcher(maxRequests = 64, maxRequestsPerHost = 12))
                .connectionPool(ConnectionPool(12, 5, TimeUnit.MINUTES))
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .callTimeout(12, TimeUnit.SECONDS)
            HttpClientProfile.BROWSER_IMAGE -> builder
                .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
                .cookieJar(CookieJar.NO_COOKIES)
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .callTimeout(14, TimeUnit.SECONDS)
            HttpClientProfile.SLIDESHOW -> builder
                .dispatcher(dispatcher(maxRequests = 64, maxRequestsPerHost = 12))
                .connectionPool(ConnectionPool(12, 5, TimeUnit.MINUTES))
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .callTimeout(16, TimeUnit.SECONDS)
            HttpClientProfile.DOWNLOAD -> builder
                .dispatcher(dispatcher(maxRequests = 24, maxRequestsPerHost = 8))
                .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(16, TimeUnit.SECONDS)
                .callTimeout(24, TimeUnit.SECONDS)
        }
        return builder.build()
    }

    private fun dispatcher(maxRequests: Int, maxRequestsPerHost: Int): Dispatcher =
        Dispatcher().apply {
            this.maxRequests = maxRequests
            this.maxRequestsPerHost = maxRequestsPerHost
        }
}
