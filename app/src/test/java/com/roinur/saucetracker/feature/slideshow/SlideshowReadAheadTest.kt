package com.roinur.saucetracker.feature.slideshow

import org.junit.Assert.assertEquals
import org.junit.Test

class SlideshowReadAheadTest {
    @Test
    fun warmup_prioritizes_current_then_five_forward_and_one_back() {
        assertEquals(
            listOf(4, 5, 6, 7, 8, 9, 3),
            explicitGalleryPrefetchIndexes(pageCount = 20, currentIndex = 4, includeCurrent = true)
        )
    }

    @Test
    fun active_reader_queues_forward_pages_without_duplicate_current() {
        assertEquals(
            listOf(1, 2, 3),
            explicitGalleryPrefetchIndexes(pageCount = 4, currentIndex = 0, includeCurrent = false)
        )
    }

    @Test
    fun read_ahead_is_clamped_at_chapter_edges() {
        assertEquals(
            listOf(2, 1),
            explicitGalleryPrefetchIndexes(pageCount = 3, currentIndex = 2, includeCurrent = true)
        )
    }
}
