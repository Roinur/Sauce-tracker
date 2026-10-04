package com.roinur.saucetracker.data.source

import org.junit.Assert.assertEquals
import org.junit.Test

class SourceChapterOrderTest {
    private fun chapter(id: String, number: String, volume: String = "") =
        SourceChapter(id = id, label = "Chapter $number", number = number, volume = volume)

    @Test fun sortsNumericallyIncludingFractionalChapters() {
        val chapters = listOf(chapter("ten", "10"), chapter("two", "2"), chapter("extra", "2.5"), chapter("one", "1"))
        assertEquals(listOf("one", "two", "extra", "ten"), orderedSourceChapters(chapters, true).map { it.id })
        assertEquals(listOf("ten", "extra", "two", "one"), orderedSourceChapters(chapters, false).map { it.id })
        assertEquals("ten", chapters.first().id) // Does not mutate the reader's source list.
    }

    @Test fun preservesProviderOrderForAlternateGroupsWithSameNumberAndVolume() {
        val chapters = listOf(chapter("groupA", "7", "2"), chapter("groupB", "7", "2"))
        for (ascending in listOf(true, false)) {
            assertEquals(chapters, orderedSourceChapters(chapters, ascending))
        }
    }

    @Test fun handlesMissingAndNonFiniteNumbersWithoutDroppingRows() {
        val chapters = listOf(chapter("extra", ""), chapter("nan", "NaN"), chapter("inf", "Infinity"), chapter("one", "1"))
        assertEquals(listOf("extra", "nan", "inf", "one"), orderedSourceChapters(chapters, true).map { it.id })
        assertEquals(listOf("one", "extra", "nan", "inf"), orderedSourceChapters(chapters, false).map { it.id })
    }

    @Test fun usesNumericVolumeToBreakChapterNumberTies() {
        val chapters = listOf(chapter("v10", "1", "10"), chapter("v2", "1", "2"))
        assertEquals(listOf("v2", "v10"), orderedSourceChapters(chapters, true).map { it.id })
        assertEquals(emptyList<SourceChapter>(), orderedSourceChapters(emptyList(), false))
    }

    @Test fun resumesExactMiddleChapterInBothOrders() {
        val chapters = (0..300).map { chapter("chapter-$it", "$it") }
        for (ascending in listOf(true, false)) {
            val ordered = orderedSourceChapters(chapters, ascending)
            assertEquals(150, resumeChapterIndex(ordered, "chapter-150"))
            assertEquals(if (ascending) 149 else 151, ordered[149].number.toInt())
        }
    }

    @Test fun resumeUsesIdNotNumberOrFurthestReadChapter() {
        val chapters = listOf(chapter("groupA", "150"), chapter("groupB", "150"), chapter("last", "300"))
        for (ascending in listOf(true, false)) {
            val ordered = orderedSourceChapters(chapters, ascending)
            assertEquals("groupB", ordered[resumeChapterIndex(ordered, "groupB")].id)
            assertEquals(-1, resumeChapterIndex(ordered, "deleted-chapter"))
            assertEquals(-1, resumeChapterIndex(ordered, null))
        }
        assertEquals(-1, resumeChapterIndex(emptyList(), "chapter-150"))
    }

    @Test fun centeringUsesMeasuredRowAndViewportRatherThanFixedDensityOrRowCount() {
        assertEquals(-180f, chapterCenterScrollDelta(0, 80, 0, 440), 0f)
        assertEquals(0f, chapterCenterScrollDelta(180, 80, 0, 440), 0f)
        assertEquals(130f, chapterCenterScrollDelta(250, 120, 20, 340), 0f)
    }
}
