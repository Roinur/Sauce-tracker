package com.roinur.saucetracker.data.source

import org.junit.Assert.assertEquals
import org.junit.Test

class MangaDexContentRatingTest {
    @Test
    fun safe_scope_only_requests_safe_titles() {
        assertEquals(
            listOf("safe"),
            mangaDexContentRatings(SourceQuery(terms = listOf(SourceQueryTerm(SourceQueryField.CATEGORY, "safe"))))
        )
    }

    @Test
    fun suggestive_scope_includes_safe_and_suggestive_titles() {
        assertEquals(
            listOf("safe", "suggestive"),
            mangaDexContentRatings(SourceQuery(terms = listOf(SourceQueryTerm(SourceQueryField.CATEGORY, "suggestive"))))
        )
    }

    @Test
    fun adult_scope_preserves_all_mangadex_ratings() {
        assertEquals(
            listOf("safe", "suggestive", "erotica", "pornographic"),
            mangaDexContentRatings(SourceQuery(terms = listOf(SourceQueryTerm(SourceQueryField.CATEGORY, "adult"))))
        )
    }
}
