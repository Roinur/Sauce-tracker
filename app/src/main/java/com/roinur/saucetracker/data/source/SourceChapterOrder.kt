package com.roinur.saucetracker.data.source

/** Presentation order only: reader next/previous chapter navigation stays provider-owned. */
internal fun orderedSourceChapters(chapters: List<SourceChapter>, ascending: Boolean): List<SourceChapter> {
    val order = compareBy<SourceChapter> {
        it.number.toDoubleOrNull()?.takeIf(Double::isFinite) ?: Double.NEGATIVE_INFINITY
    }.thenBy { it.volume.toDoubleOrNull()?.takeIf(Double::isFinite) ?: Double.NEGATIVE_INFINITY }
    return chapters.sortedWith(if (ascending) order else order.reversed())
}

/** Identify the exact saved chapter, not a possibly duplicated chapter number. */
internal fun resumeChapterIndex(chapters: List<SourceChapter>, chapterId: String?): Int =
    if (chapterId.isNullOrBlank()) -1 else chapters.indexOfFirst { it.id == chapterId }

internal fun chapterCenterScrollDelta(itemOffset: Int, itemSize: Int, viewportStart: Int, viewportEnd: Int): Float =
    itemOffset + itemSize / 2f - (viewportStart + viewportEnd) / 2f
