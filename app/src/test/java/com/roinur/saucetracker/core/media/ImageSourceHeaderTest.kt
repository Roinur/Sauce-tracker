package com.roinur.saucetracker.core.media

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageSourceHeaderTest {
    @Test fun mangadexCoversUseMangadexReferer() {
        val url = "https://uploads.mangadex.org/covers/manga-id/cover.jpg.512.jpg"
        val request = Request.Builder().url(url).applySourceImageHeaders(url).build()

        assertEquals("https://mangadex.org/", request.header("Referer"))
    }

    @Test fun mangadexAtHomePagesDoNotLeakAnotherProvidersReferer() {
        val url = "https://example-node.mangadex.network/data/hash/page.jpg"
        val request = Request.Builder().url(url).applySourceImageHeaders(url).build()

        assertNull(request.header("Referer"))
    }

    @Test fun nhentaiImagesKeepTheirExistingReferer() {
        val url = "https://t.nhentai.net/galleries/123/cover.jpg"
        val request = Request.Builder().url(url).applySourceImageHeaders(url).build()

        assertEquals("https://nhentai.net/", request.header("Referer"))
    }
}
