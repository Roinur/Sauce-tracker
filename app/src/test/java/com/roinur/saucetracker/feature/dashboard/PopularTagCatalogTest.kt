package com.roinur.saucetracker

import org.junit.Assert.assertEquals
import org.junit.Test

class PopularTagCatalogTest {
    @Test
    fun `matching preserves normalized names and distinct categories`() {
        val catalog = listOf(PopularTagRow(1, "  Shared   Name ", "tag", 2, false))
        val local = listOf(
            PopularTagSeed("shared name", "tag", 1),
            PopularTagSeed("SHARED NAME", "artist", 1),
            PopularTagSeed("other", "tag", 1)
        )
        assertEquals(local.drop(1), missingPopularTags(local, catalog))
    }

    @Test
    fun `large catalog normalizes each row once instead of every pair`() {
        val catalog = (1..4460).map { PopularTagRow(it.toLong(), "tag $it", "tag", 1, false) }
        val local = (1..4000).map { PopularTagSeed("tag $it", "tag", 1) }
        var normalizations = 0
        val missing = missingPopularTags(local, catalog) {
            normalizations++
            normalizeTagName(it)
        }
        assertEquals(emptyList<PopularTagSeed>(), missing)
        assertEquals(catalog.size + local.size, normalizations)
    }

    @Test
    fun `empty catalog retains all local rows`() {
        val local = listOf(PopularTagSeed("example", "tag", 1))
        assertEquals(local, missingPopularTags(local, emptyList()))
    }
}
