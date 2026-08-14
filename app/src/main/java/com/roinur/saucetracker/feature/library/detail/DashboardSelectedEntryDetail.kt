package com.roinur.saucetracker

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
) {
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
        downloadButtonLabel = if (detail != null && vm.selectedDetail?.code == detail.code) {
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
        showThumbnails = vm.showThumbnails,
        incognitoModeEnabled = vm.incognitoModeEnabled,
        experimentalLazyMetadata = vm.experimentalLazyEntryDetail,
        headerCenterText = headerCenterText,
        compactContent = compactContent,
        modifier = modifier
    )
}
