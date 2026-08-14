package com.example.saucetracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.saucetracker.core.ui.privacy.privacyObfuscate
import com.example.saucetracker.feature.dashboard.DashboardViewModel
import com.example.saucetracker.feature.heatmap.HeatmapCanvas
import com.example.saucetracker.feature.heatmap.HeatmapLayoutCache

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
    onLegacyCollapsedChange: (Boolean) -> Unit
) {
    if (!vm.legacyHomeUi) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
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
