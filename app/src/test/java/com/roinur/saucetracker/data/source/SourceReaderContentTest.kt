package com.roinur.saucetracker.data.source

import org.junit.Assert.assertEquals
import org.junit.Test

class SourceReaderContentTest {
    @Test fun chapterIdentityStaysSeparateFromItsDisplayLabel() {
        val reader = SourceReaderContent(
            entryKey = SourceEntryKey(SourceId("mangadex"), "manga-id"),
            chapterId = "chapter-id",
            chapterLabel = "Chapter 55",
            chapterTitle = "Hanei Rin (4)",
            language = "en",
            pageUrls = listOf("https://node.example/page-1.jpg")
        )

        assertEquals("Chapter 55 · Hanei Rin (4)", reader.displayTitle)
        assertEquals("chapter-id", reader.chapterId)
    }
}
