package com.roinur.saucetracker.data.downloads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryScreenshotScopeTest {
    @Test fun usesIdentityRatherThanTitleForNewCaptures() {
        val scope = EntryScreenshotScope("nhentai", "123456", "Renamed title")
        assertTrue(scope.matches("nhentai", "123456", "Old title_page_20"))
        assertFalse(scope.matches("nhentai", "654321", "Renamed title_page_20"))
        assertFalse(scope.matches("mangadex", "123456", "Renamed title_page_20"))
        assertFalse(scope.matches("nhentai", "", "Renamed title_page_20"))
    }

    @Test fun keepsImportedMangaLanguagesSeparateButBrowserCanShowWholeSeries() {
        val english = EntryScreenshotScope("mangadex", "series@en", "Series")
        val browser = EntryScreenshotScope("mangadex", "series", "Series")
        assertTrue(english.matches("mangadex", "series@en", "anything"))
        assertFalse(english.matches("mangadex", "series@ja", "anything"))
        assertFalse(english.matches("mangadex", "series", "anything"))
        assertTrue(browser.matches("mangadex", "series@en", "anything"))
        assertTrue(browser.matches("mangadex", "series@ja", "anything"))
        assertFalse(browser.matches("mangadex", "other@en", "anything"))
    }

    @Test fun recognizesLegacyMangaCapturesOnlyThroughKnownChapterIds() {
        val scope = EntryScreenshotScope("mangadex", "series@en", "Series", setOf("12345678-abc"))
        assertTrue(scope.matches("", "", "Series — Chapter 7_chapter_12345678-abc_page_20"))
        assertFalse(scope.matches("", "", "Series — Chapter 7_chapter_98765432-abc_page_20"))
        assertFalse(scope.matches("", "", "Series_page_20"))
        assertFalse(scope.matches("", "", "ordinary imported image"))
    }

    @Test fun recognizesLegacyNhentaiNameAndRejectsMangaOrOtherNames() {
        val title = "A".repeat(60)
        val scope = EntryScreenshotScope("nhentai", "123456", title)
        assertTrue(scope.matches("", "", "${title.take(48)}_page_15"))
        assertFalse(scope.matches("", "", "Different title_page_15"))
        assertFalse(scope.matches("", "", "${title.take(48)}_chapter_12345678-abc_page_15"))
        assertFalse(scope.matches("", "", "${title.take(48)}_page_0"))
    }
}
