package com.roinur.saucetracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.roinur.saucetracker.data.source.SourceChapter
import com.roinur.saucetracker.data.database.SourceChapterReadingState
import com.roinur.saucetracker.feature.dashboard.DashboardViewModel
import com.roinur.saucetracker.feature.library.detail.SelectedEntryDetailCard

internal data class DashboardEntryDetailActions(
    val onOpenCreatorFromDetail: (String, String) -> Unit,
    val onRefetch: (Int) -> Unit,
    val onDownload: (EntryDetail) -> Unit,
    val onRedownload: (EntryDetail) -> Unit,
    val onDelete: (Int) -> Unit,
    val onOpenRelatedEntry: (Int) -> Unit,
    val onThumbnailClick: (Int, String, String) -> Unit
)

@Composable
internal fun DashboardSelectedEntryDetail(
    vm: DashboardViewModel,
    code: Int,
    selectedEntryDownloaded: Boolean,
    actions: DashboardEntryDetailActions,
    detail: EntryDetail? = vm.selectedDetail?.takeIf { it.code == code },
    modifier: Modifier = Modifier,
    compactContent: Boolean = false,
    enableLibraryRelatedNavigation: Boolean = true,
    seriesNeighbors: SeriesNeighbors = vm.selectedSeriesNeighbors,
    onOpenInBrowser: () -> Unit = vm::openSelectedInBrowser,
    headerCenterText: String? = null
) = key(vm.activeProfileId, detail?.sourceId, detail?.remoteId) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var readerReturnNonce by remember { mutableLongStateOf(0L) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) readerReturnNonce++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val chapters by produceState<List<SourceChapter>?>(
        initialValue = if (detail?.isNhentai == false) null else emptyList(),
        detail?.sourceId,
        detail?.remoteId
    ) {
        value = if (detail?.isNhentai == false) {
            runCatching { vm.fetchSourceChapters(detail) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
    }
    val readingState by produceState<SourceChapterReadingState?>(
        initialValue = null, detail?.sourceId, detail?.remoteId, vm.activeProfileId, readerReturnNonce
    ) {
        value = if (detail?.isNhentai == false) vm.fetchSourceChapterReadingState(detail, vm.activeProfileId) else null
    }
    SelectedEntryDetailCard(
        detail = detail,
        summary = vm.selectedSummary?.takeIf { it.code == code },
        detailLoading = vm.selectedDetailLoading && vm.selectedCode == code,
        analyticsSnapshot = vm.readAnalytics,
        onOpenInBrowser = onOpenInBrowser,
        onOpenCreatorFromDetail = actions.onOpenCreatorFromDetail,
        onCopyCode = vm::copyCodeToClipboard,
        onToggleReadStatus = vm::toggleEntryRead,
        onSetRating = vm::setEntryRating,
        onResetRating = { targetCode -> vm.setEntryRating(targetCode, 0) },
        ratingHistoryProvider = vm::getEntryRatingHistory,
        averageRatingProvider = vm::getAverageEntryRating,
        onUpdateRatingHistory = vm::updateRatingHistoryRow,
        onDeleteRatingHistory = vm::deleteRatingHistoryRow,
        onRefetch = actions.onRefetch,
        downloadButtonLabel = if (detail?.isNhentai == true && vm.selectedDetail?.code == detail.code) {
            if (selectedEntryDownloaded) "Local" else "Download"
        } else {
            null
        },
        downloadProgressLabel = vm.entryDownloadProgressState
            ?.takeIf { detail != null && it.code == detail.code }
            ?.label,
        downloadProgressFraction = vm.entryDownloadProgressState
            ?.takeIf { detail != null && it.code == detail.code }
            ?.fraction,
        onDownloadAction = actions.onDownload,
        onRedownloadAction = actions.onRedownload,
        onDelete = actions.onDelete,
        seriesNeighbors = seriesNeighbors,
        onOpenSeriesEntry = actions.onOpenRelatedEntry,
        enableLibraryRelatedNavigation = enableLibraryRelatedNavigation,
        relatedEntriesState = vm.selectedEntryRelatedUiState,
        relatedEntryMode = vm.selectedRelatedEntryMode,
        onRelatedEntryModeChange = vm::selectRelatedEntryMode,
        onOpenRelatedEntry = actions.onOpenRelatedEntry,
        onOpenCreatorInBrowser = vm::openCreatorPreviewInBrowser,
        onSelectedThumbnailClick = actions.onThumbnailClick,
        chapters = chapters.orEmpty(),
        chapterProgress = readingState?.chapters.orEmpty(),
        resumeChapterId = readingState?.resume?.chapterId,
        chapterResumeLoading = detail?.isNhentai == false && readingState == null,
        chaptersLoading = detail?.isNhentai == false && chapters == null,
        onOpenChapter = { chapterId -> vm.openSourceChapter(code, chapterId) },
        showThumbnails = vm.showThumbnails,
        incognitoModeEnabled = vm.incognitoModeEnabled,
        experimentalLazyMetadata = vm.experimentalLazyEntryDetail,
        headerCenterText = headerCenterText,
        compactContent = compactContent,
        modifier = modifier
    )
}
