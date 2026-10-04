package com.roinur.saucetracker.feature.settings

import org.junit.Assert.*
import org.junit.Test

class ReadingHistoryHealthTest {
    @Test fun unimportedUnratedAndLegacyManualReadsAreHealthy() {
        val checks = readingHistoryHealthChecks(60, 60, 0, 62)
        assertEquals(LibraryHealthLevel.HEALTHY, LibraryHealthReport(checks, 0).level)
        assertEquals("Reading outside library", checks.last().title)
        assertTrue(checks.last().detail.contains("60 unrated"))
        assertTrue(checks.last().detail.contains("removed later"))
    }

    @Test fun removedRatedEntriesAlsoRetainValidHistory() {
        assertTrue(readingHistoryHealthChecks(2, 0, 0, 0).all { it.level == LibraryHealthLevel.HEALTHY })
    }

    @Test fun invalidOwnersStillFailEvenWithUnimportedReads() {
        assertEquals(LibraryHealthLevel.ACTION_REQUIRED,
            LibraryHealthReport(readingHistoryHealthChecks(60, 60, 1, 62), 0).level)
    }

    @Test fun noOutsideLibraryLabelWhenNoneExist() {
        assertEquals(listOf("Read history"), readingHistoryHealthChecks(0, 0, 0, 0).map { it.title })
    }
}
