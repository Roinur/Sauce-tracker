package com.roinur.saucetracker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roinur.saucetracker.core.ui.privacy.privacyObfuscate
import com.roinur.saucetracker.feature.dashboard.DashboardViewModel
import com.roinur.saucetracker.feature.dashboard.normalizeHeatmapOverviewPageOrder
import com.roinur.saucetracker.feature.heatmap.HeatmapCanvas
import com.roinur.saucetracker.feature.heatmap.HeatmapLayoutCache
import com.roinur.saucetracker.feature.heatmap.TrendOverTimePanel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DashboardHeatmapSection(
    vm: DashboardViewModel,
    snapshot: TagGraphSnapshot?,
    displayMode: TagHeatmapDisplayMode,
    onDisplayModeChange: (TagHeatmapDisplayMode) -> Unit,
    onPressStart: () -> Unit,
    runOnPressWhen: () -> Boolean,
    screenHeightDp: Int,
    onTagSelected: (TagGraphNode) -> Unit,
    onEntrySelected: (TagGraphEntryNode, List<String>) -> Unit,
    entryLayoutSessionCache: HeatmapLayoutCache,
    legacyCollapsed: Boolean,
    onLegacyCollapsedChange: (Boolean) -> Unit,
    pageOrder: List<HeatmapOverviewPage> = listOf(
        HeatmapOverviewPage.HEATMAP,
        HeatmapOverviewPage.READING_TRENDS
    ),
    overviewPage: Int = 0,
    onOverviewPageChange: (Int) -> Unit = {},
    onSearchVisibilityChange: (Boolean) -> Unit = {},
    onOverviewPageProgressChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!vm.legacyHomeUi) {
        val normalizedPageOrder = remember(pageOrder) {
            normalizeHeatmapOverviewPageOrder(pageOrder)
        }
        val safeOverviewPage = overviewPage.coerceIn(normalizedPageOrder.indices)
        val pagerState = rememberPagerState(
            initialPage = safeOverviewPage,
            pageCount = { normalizedPageOrder.size }
        )
        var searchRevealCommitted by remember {
            mutableStateOf(normalizedPageOrder[safeOverviewPage] == HeatmapOverviewPage.HEATMAP)
        }
        LaunchedEffect(pagerState) {
            pagerState.scrollToPage(safeOverviewPage)
            snapshotFlow { pagerState.settledPage }
                .drop(1)
                .collect { settledPage ->
                    onOverviewPageChange(settledPage)
                }
        }
        val settledContent = normalizedPageOrder.getOrElse(pagerState.settledPage) {
            HeatmapOverviewPage.HEATMAP
        }
        LaunchedEffect(settledContent, pagerState.isScrollInProgress) {
            if (settledContent != HeatmapOverviewPage.HEATMAP) {
                searchRevealCommitted = false
                onSearchVisibilityChange(false)
            } else if (pagerState.isScrollInProgress && !searchRevealCommitted) {
                // The user reversed direction before the search reveal became visible.
                // Cancel only that unfinished reveal; established cards still exit slowly.
                onSearchVisibilityChange(false)
            } else if (!pagerState.isScrollInProgress && !searchRevealCommitted) {
                onSearchVisibilityChange(true)
                // Match the deliberately prominent search-card reveal. Only after it has
                // completed should a new swipe use the normal slow exit animation.
                delay(300)
                if (
                    normalizedPageOrder.getOrNull(pagerState.settledPage) == HeatmapOverviewPage.HEATMAP &&
                    !pagerState.isScrollInProgress
                ) {
                    searchRevealCommitted = true
                }
            } else {
                // An established Heatmap page keeps Search Everything visible throughout
                // the drag and starts the intentional slow exit only after Trends settles.
                onSearchVisibilityChange(true)
            }
        }
        LaunchedEffect(pagerState) {
            snapshotFlow {
                (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                    .coerceIn(0f, 1f)
            }.collect(onOverviewPageProgressChange)
        }
        LaunchedEffect(overviewPage) {
            val targetPage = overviewPage.coerceIn(normalizedPageOrder.indices)
            if (pagerState.currentPage != targetPage) {
                pagerState.animateScrollToPage(targetPage)
            }
        }
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                HorizontalPager(
                    state = pagerState,
                    pageSpacing = 16.dp,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (normalizedPageOrder[page]) {
                    HeatmapOverviewPage.HEATMAP -> Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Heatmap Overview",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${vm.entries.size} filtered entries • ${vm.tags.size} visible tags",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.privacyObfuscate(
                                        enabled = vm.incognitoModeEnabled,
                                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                            alpha = INCOGNITO_OVERLAY_ALPHA
                                        )
                                    )
                                )
                            }
                            HeatmapDisplayModeActions(
                                displayMode = displayMode,
                                onDisplayModeChange = onDisplayModeChange,
                                onPressStart = onPressStart,
                                runOnPressWhen = runOnPressWhen
                            )
                        }

                        DashboardHeatmapCanvas(
                            vm = vm,
                            snapshot = snapshot,
                            displayMode = displayMode,
                            screenHeightDp = screenHeightDp,
                            onTagSelected = onTagSelected,
                            onEntrySelected = onEntrySelected,
                            entryLayoutSessionCache = entryLayoutSessionCache
                        )
                    }

                    HeatmapOverviewPage.READING_TRENDS -> Column(modifier = Modifier.fillMaxSize()) {
                        TrendOverTimePanel(
                            incognitoModeEnabled = vm.incognitoModeEnabled,
                            targetProvider = vm::trendTargets,
                            snapshotProvider = vm::trendSnapshot,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                    }
                }
            }

        }
        return
    }

    androidx.compose.material3.Card {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Heatmap Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeatmapDisplayModeActions(
                        displayMode = displayMode,
                        onDisplayModeChange = onDisplayModeChange,
                        onPressStart = onPressStart,
                        runOnPressWhen = runOnPressWhen
                    )
                    ImmediateActionText(
                        label = if (legacyCollapsed) "Expand" else "Collapse",
                        onAction = { onLegacyCollapsedChange(!legacyCollapsed) },
                        onPressStart = onPressStart,
                        runOnPressWhen = runOnPressWhen,
                        textStyle = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (!legacyCollapsed) {
                DashboardHeatmapCanvas(
                    vm = vm,
                    snapshot = snapshot,
                    displayMode = displayMode,
                    screenHeightDp = screenHeightDp,
                    onTagSelected = onTagSelected,
                    onEntrySelected = onEntrySelected,
                    entryLayoutSessionCache = entryLayoutSessionCache
                )
            }
        }
    }
}

@Composable
private fun HeatmapDisplayModeActions(
    displayMode: TagHeatmapDisplayMode,
    onDisplayModeChange: (TagHeatmapDisplayMode) -> Unit,
    onPressStart: () -> Unit,
    runOnPressWhen: () -> Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ImmediateActionText(
            label = "Tags",
            onAction = { onDisplayModeChange(TagHeatmapDisplayMode.TAGS) },
            onPressStart = onPressStart,
            runOnPressWhen = runOnPressWhen,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                color = if (displayMode == TagHeatmapDisplayMode.TAGS) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            ),
            fontWeight = if (displayMode == TagHeatmapDisplayMode.TAGS) {
                FontWeight.SemiBold
            } else {
                FontWeight.Medium
            }
        )
        ImmediateActionText(
            label = "Entries",
            onAction = { onDisplayModeChange(TagHeatmapDisplayMode.ENTRIES) },
            onPressStart = onPressStart,
            runOnPressWhen = runOnPressWhen,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                color = if (displayMode == TagHeatmapDisplayMode.ENTRIES) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            ),
            fontWeight = if (displayMode == TagHeatmapDisplayMode.ENTRIES) {
                FontWeight.SemiBold
            } else {
                FontWeight.Medium
            }
        )
    }
}

@Composable
private fun DashboardHeatmapCanvas(
    vm: DashboardViewModel,
    snapshot: TagGraphSnapshot?,
    displayMode: TagHeatmapDisplayMode,
    screenHeightDp: Int,
    onTagSelected: (TagGraphNode) -> Unit,
    onEntrySelected: (TagGraphEntryNode, List<String>) -> Unit,
    entryLayoutSessionCache: HeatmapLayoutCache
) {
    if (snapshot == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((screenHeightDp.dp * 0.66f).coerceIn(420.dp, 640.dp)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (snapshot.nodes.isEmpty()) {
        Text(
            text = "No entries match the current search/filter for heatmap overview.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        HeatmapCanvas(
            snapshot = snapshot,
            selectedTab = TagGraphTab.HEATMAP,
            selectedHeatmapDisplayMode = displayMode,
            incognitoModeEnabled = vm.incognitoModeEnabled,
            onTagSelected = onTagSelected,
            onEntrySelected = onEntrySelected,
            entryLayoutSessionCache = entryLayoutSessionCache,
            referenceEntryLayoutSnapshot = vm.tagGraphSnapshot,
            persistentEntryLayoutProvider = vm::loadEntryHeatmapLayoutForSnapshot,
            graphViewportHeight = (screenHeightDp.dp * 0.60f).coerceIn(380.dp, 580.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
