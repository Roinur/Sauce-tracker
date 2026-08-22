package com.roinur.saucetracker

import org.junit.Assert.assertEquals
import org.junit.Test

class TagPresetSearchParsingTest {
    @Test
    fun presetAnyAndExcludeRulesRemainSeparateStructuredFilters() {
        val parsed = parseSearchQuery("anytagid:11|22 excludetagid:33|44")

        assertEquals("", parsed.freeText)
        assertEquals(
            listOf(
                SearchFieldFilter("anytagid", "11|22"),
                SearchFieldFilter("excludetagid", "33|44")
            ),
            parsed.filters
        )
    }
}
