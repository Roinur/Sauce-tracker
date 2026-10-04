package com.roinur.saucetracker.feature.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserSourceTranslationTest {
    @Test fun nhentaiKeepsLegacyBrowserIdentityAndCoverResolution() {
        val row = BrowserGallerySummary(123, "Title", "", 456, "jpg", 20, "")
        assertTrue(row.isNhentai)
        assertEquals("123", row.displayId)
        assertTrue(row.coverUrls.isNotEmpty())
    }

    @Test fun mangaDexUsesRemoteIdentityAndAdapterCoverInsideSameBrowserModel() {
        val uuid = "123e4567-e89b-12d3-a456-426614174000"
        val cover = "https://uploads.mangadex.org/covers/$uuid/cover.jpg.512.jpg"
        val row = BrowserGallerySummary(
            code = 91,
            title = "Title",
            subtitle = "",
            mediaId = 0,
            coverExt = "jpg",
            numPages = 12,
            uploadDate = "2026",
            sourceId = "mangadex",
            remoteId = uuid,
            thumbnailUrl = cover,
            unitLabel = "chapters"
        )
        assertFalse(row.isNhentai)
        assertEquals(uuid, row.displayId)
        assertEquals(listOf(cover), row.coverUrls)
        assertEquals("chapters", row.unitLabel)
    }
}
