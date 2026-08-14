package com.example.saucetracker

import com.example.saucetracker.data.backup.*
import com.example.saucetracker.data.downloads.*
import com.example.saucetracker.core.ui.components.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.saucetracker.core.media.*
import com.example.saucetracker.feature.library.creators.*
import com.example.saucetracker.feature.library.detail.*
import com.example.saucetracker.feature.library.history.*
import com.example.saucetracker.feature.library.tags.*
import com.example.saucetracker.feature.settings.*
import com.example.saucetracker.feature.subscriptions.*
import com.example.saucetracker.feature.suggestions.*
import kotlin.math.roundToInt

@Composable
internal fun ModernHomeDashboard(
    vm: com.example.saucetracker.feature.dashboard.DashboardViewModel,
    dashboardVisitNonce: Long,
    onOpenEntries: () -> Unit,
    onOpenTags: () -> Unit,
    onOpenCreators: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onOpenSubscriptionsList: () -> Unit,
    onOpenHeatmap: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSuggestions: () -> Unit,
    onOpenEntry: (Int) -> Unit,
    onOpenRandomEntry: (Int) -> Unit,
    onEntriesLongPress: (() -> Unit)? = null,
    onTagsLongPress: (() -> Unit)? = null,
    onCreatorsLongPress: (() -> Unit)? = null
) {
    val recentEntries = remember(vm.entries) {
        vm.entries
            .sortedByDescending { it.addedAt.ifBlank { it.fetchedAt } }
            .take(8)
    }
    val suggestionPreview = remember(vm.suggestedEntries, vm.entries) {
        vm.suggestedEntries
            .map { suggestion ->
                EntryRow(
                    code = suggestion.code,
                    title = suggestion.title,
                    numPages = suggestion.numPages,
                    uploadDate = "",
                    addedAt = "",
                    rating = 0,
                    averageRating = 0f,
                    isRead = false,
                    pinned = false,
                    fetchedAt = "",
                    sourceUrl = "",
                    thumbnailUrl = suggestion.thumbnailUrl,
                    tags = suggestion.topTags.joinToString(", ")
                )
            }
            .take(8)
    }
    val randomPreview = remember(vm.entries, dashboardVisitNonce) {
        vm.entries.shuffled().take(8)
    }
    var randomPreviewInitialIndex by remember { mutableStateOf<Int?>(null) }
    val sauceFinderState by vm.sauceFinderState.collectAsState()
    val sauceImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(vm::findSauce)
    }
    val playDashboardEntrance = remember { vm.consumeDashboardEntrance() }
    var dashboardVisible by remember { mutableStateOf(!playDashboardEntrance) }
    LaunchedEffect(playDashboardEntrance) {
        if (playDashboardEntrance) {
            dashboardVisible = true
        }
        vm.ensureReadAnalyticsLoaded(forceRefresh = false)
    }

    randomPreviewInitialIndex?.let { initialIndex ->
        RandomEntryPreviewDialog(
            entries = randomPreview,
            initialIndex = initialIndex,
            incognitoModeEnabled = vm.incognitoModeEnabled,
            onDismiss = { randomPreviewInitialIndex = null },
            onOpenEntry = { code ->
                randomPreviewInitialIndex = null
                onOpenRandomEntry(code)
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModernMetricTile(
                label = "Entries",
                value = vm.entries.size,
                caption = "Filtered library",
                glyph = "▣",
                onClick = onOpenEntries,
                onLongClick = onEntriesLongPress,
                modifier = Modifier
                    .weight(1f)
                    .dashboardEntrance(dashboardVisible, delayMillis = 0, label = "dashboardEntriesEntrance")
            )
            ModernMetricTile(
                label = "Tags",
                value = vm.tags.size,
                caption = "Visible tags",
                glyph = "◆",
                onClick = onOpenTags,
                onLongClick = onTagsLongPress,
                modifier = Modifier
                    .weight(1f)
                    .dashboardEntrance(dashboardVisible, delayMillis = 48, label = "dashboardTagsEntrance")
            )
            ModernMetricTile(
                label = "Artists / Groups",
                value = vm.creators.size,
                caption = "Matched",
                glyph = "♚",
                onClick = onOpenCreators,
                onLongClick = onCreatorsLongPress,
                modifier = Modifier
                    .weight(1f)
                    .dashboardEntrance(dashboardVisible, delayMillis = 96, label = "dashboardCreatorsEntrance")
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .dashboardEntrance(dashboardVisible, delayMillis = 144, label = "dashboardWidgetsEntrance"),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SuggestedRandomPreviewPanel(
                suggestedEntries = suggestionPreview,
                randomEntries = randomPreview,
                suggestedSubtitle = if (vm.suggestedEntriesLoading) "Refreshing..." else "For you to discover",
                suggestedEmptyMessage = if (vm.suggestedEntriesLoading) {
                    "Loading suggested entries..."
                } else {
                    "Open suggested entries to load recommendations."
                },
                incognitoModeEnabled = vm.incognitoModeEnabled,
                onOpenSuggestions = onOpenSuggestions,
                onSuggestedEntryClick = onOpenEntry,
                onRandomEntryClick = { entry ->
                    val index = randomPreview.indexOfFirst { it.code == entry.code }.coerceAtLeast(0)
                    randomPreviewInitialIndex = index
                },
                sauceFinderState = sauceFinderState,
                onPrepareSauceFinder = vm::prepareSauceFinderLocalIndex,
                onChooseSauceImage = { sauceImagePicker.launch("image/*") },
                onBuildSauceIndex = vm::buildFullSauceFinderIndex,
                onPauseSauceIndex = vm::pauseFullSauceFinderIndex,
                onOpenSauceMatch = onOpenRandomEntry,
                modifier = Modifier
                    .weight(2f)
                    .height(184.dp)
            )
            SubscriptionHeatmapPreviewPanel(
                subscriptionCount = vm.subscriptions.size,
                updateCount = vm.visibleSubscriptionEvents.size,
                entryCount = vm.entries.size,
                tagCount = vm.tags.size,
                analyticsSnapshot = vm.readAnalytics,
                analyticsLoading = vm.readAnalyticsLoading,
                onOpenUpdates = onOpenSubscriptions,
                onOpenList = onOpenSubscriptionsList,
                onOpenHeatmap = onOpenHeatmap,
                onOpenHistory = onOpenHistory,
                modifier = Modifier
                    .weight(1f)
                    .height(184.dp)
            )
        }

        ModernPreviewPanel(
            title = "Continue where you left off",
            subtitle = "Most recently imported",
            entries = recentEntries,
            incognitoModeEnabled = vm.incognitoModeEnabled,
            onHeaderClick = onOpenEntries,
            onEntryClick = onOpenRandomEntry,
            modifier = Modifier.dashboardEntrance(
                dashboardVisible,
                delayMillis = 192,
                label = "dashboardContinueEntrance"
            )
        )
    }
}

internal fun Modifier.adaptiveDashboardViewport(scale: Float): Modifier = layout { measurable, constraints ->
    if (!constraints.hasBoundedWidth || !constraints.hasBoundedHeight) {
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    } else {
        val safeScale = scale.coerceIn(0.58f, 1.24f)
        val virtualWidth = (constraints.maxWidth / safeScale).roundToInt().coerceAtLeast(1)
        val virtualHeight = (constraints.maxHeight / safeScale).roundToInt().coerceAtLeast(1)
        val placeable = measurable.measure(Constraints.fixed(virtualWidth, virtualHeight))
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.placeWithLayer(0, 0) {
                scaleX = safeScale
                scaleY = safeScale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}

internal fun adaptiveDashboardScale(screenHeightDp: Int): Float {
    val rawScale = screenHeightDp / 933f
    return if (rawScale in 0.94f..1.06f) 1f else rawScale.coerceIn(0.58f, 1.24f)
}

@Composable
internal fun Modifier.dashboardEntrance(
    visible: Boolean,
    delayMillis: Int,
    label: String
): Modifier {
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 210,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing
        ),
        label = "${label}Alpha"
    )
    val offsetY by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 0f else 10f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 230,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing
        ),
        label = "${label}Offset"
    )
    return graphicsLayer {
        this.alpha = alpha
        translationY = offsetY
    }
}
