package com.roinur.saucetracker.core.ui.components

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import com.roinur.saucetracker.data.source.SourceChapter
import com.roinur.saucetracker.data.source.chapterCenterScrollDelta
import com.roinur.saucetracker.data.source.resumeChapterIndex
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull

internal data class ChapterListPosition(val state: LazyListState, val ready: Boolean)

/** Prepare before revealing the list; do not keep snapping back as the user scrolls. */
@Composable
internal fun rememberResumeChapterListPosition(
    identity: String,
    chapters: List<SourceChapter>,
    resumeChapterId: String?,
    loading: Boolean
): ChapterListPosition {
    val targetIndex = resumeChapterIndex(chapters, resumeChapterId)
    val state = remember(identity) { LazyListState(firstVisibleItemIndex = targetIndex.coerceAtLeast(0)) }
    val chapterIds = remember(chapters) { chapters.map { it.id } }
    var ready by remember(identity, chapterIds, resumeChapterId, loading) {
        mutableStateOf(!loading && targetIndex < 0)
    }
    LaunchedEffect(identity, chapterIds, resumeChapterId, loading) {
        if (loading) return@LaunchedEffect
        if (targetIndex >= 0) {
            state.scrollToItem(targetIndex)
            val delta = snapshotFlow {
                val layout = state.layoutInfo
                layout.visibleItemsInfo.firstOrNull { it.index == targetIndex }
                    ?.takeIf { layout.viewportEndOffset > layout.viewportStartOffset }
                    ?.let { chapterCenterScrollDelta(it.offset, it.size, layout.viewportStartOffset, layout.viewportEndOffset) }
            }.filterNotNull().first()
            state.scrollBy(delta)
        }
        ready = true
    }
    return ChapterListPosition(state, ready)
}
