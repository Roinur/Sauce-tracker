package com.roinur.saucetracker

import com.roinur.saucetracker.data.backup.*
import com.roinur.saucetracker.data.downloads.*
import com.roinur.saucetracker.core.ui.components.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.roinur.saucetracker.core.media.*
import com.roinur.saucetracker.feature.library.creators.*
import com.roinur.saucetracker.feature.library.detail.*
import com.roinur.saucetracker.feature.library.history.*
import com.roinur.saucetracker.feature.library.tags.*
import com.roinur.saucetracker.feature.settings.*
import com.roinur.saucetracker.feature.subscriptions.*
import com.roinur.saucetracker.feature.suggestions.*
import kotlin.math.min

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun ModernMetricTile(
    label: String,
    value: Int,
    caption: String,
    glyph: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tileShape = RoundedCornerShape(18.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.968f else 1f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "modernMetricTilePressScale"
    )
    val pressContainerColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (pressed) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "modernMetricTilePressColor"
    )
    val pressShadow by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (pressed) 6.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "modernMetricTilePressShadow"
    )
    val displayedValue by androidx.compose.animation.core.animateIntAsState(
        targetValue = value,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "modernMetricTileValue"
    )
    Card(
        shape = tileShape,
        colors = CardDefaults.cardColors(
            containerColor = pressContainerColor
        ),
        border = BorderStroke(
            1.dp,
            if (pressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.62f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .heightIn(min = 118.dp)
            .shadow(pressShadow, tileShape, clip = false)
            .clip(tileShape)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .combinedClickable(
                interactionSource = interactionSource,
                // The card's own press depth is shape-accurate; avoid a rectangular ripple overlay.
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = glyph,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = displayedValue.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun SubscriptionHeatmapPreviewPanel(
    pageOrder: List<DashboardInsightPage>,
    subscriptionCount: Int,
    updateCount: Int,
    entryCount: Int,
    tagCount: Int,
    analyticsSnapshot: ReadAnalyticsSnapshot,
    analyticsLoading: Boolean,
    onOpenUpdates: () -> Unit,
    onOpenList: () -> Unit,
    onOpenHeatmap: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val normalizedPageOrder = remember(pageOrder) {
        val defaults = listOf(
            DashboardInsightPage.SUBSCRIPTIONS,
            DashboardInsightPage.HEATMAP,
            DashboardInsightPage.HISTORY
        )
        pageOrder.distinct().filter { it in defaults } + defaults.filterNot { it in pageOrder }
    }
    val cardShape = RoundedCornerShape(18.dp)
    val panelShape = RoundedCornerShape(14.dp)
    val pagerState = rememberPagerState(pageCount = { normalizedPageOrder.size })
    val subscriptionInteraction = remember { MutableInteractionSource() }
    val heatmapInteraction = remember { MutableInteractionSource() }
    val historyInteraction = remember { MutableInteractionSource() }
    val subscriptionPressed by subscriptionInteraction.collectIsPressedAsState()
    val heatmapPressed by heatmapInteraction.collectIsPressedAsState()
    val historyPressed by historyInteraction.collectIsPressedAsState()
    val widgetPressed = when (normalizedPageOrder.getOrNull(pagerState.currentPage)) {
        DashboardInsightPage.SUBSCRIPTIONS -> subscriptionPressed
        DashboardInsightPage.HEATMAP -> heatmapPressed
        DashboardInsightPage.HISTORY, null -> historyPressed
    }
    val widgetScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (widgetPressed) 0.968f else 1f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "subscriptionWidgetCardPressScale"
    )
    val widgetColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (widgetPressed) MaterialTheme.colorScheme.surfaceContainerHighest
        else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "subscriptionWidgetCardPressColor"
    )
    val widgetShadow by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (widgetPressed) 6.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "subscriptionWidgetCardPressShadow"
    )
    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = widgetColor),
        border = BorderStroke(
            1.dp,
            if (widgetPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.62f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .shadow(widgetShadow, cardShape, clip = false)
            .clip(cardShape)
            .graphicsLayer {
                scaleX = widgetScale
                scaleY = widgetScale
            }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                pageSpacing = 14.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (normalizedPageOrder[page]) {
                    DashboardInsightPage.SUBSCRIPTIONS -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(panelShape)
                                .combinedClickable(
                                    interactionSource = subscriptionInteraction,
                                    indication = null,
                                    onClick = onOpenUpdates,
                                    onLongClick = onOpenList
                                )
                                .padding(2.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("◔", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                            }
                            Text("Subscriptions", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Text(updateCount.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                            Text(
                                "$subscriptionCount active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    DashboardInsightPage.HEATMAP -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(panelShape)
                                .clickable(
                                    interactionSource = heatmapInteraction,
                                    indication = null,
                                    onClick = onOpenHeatmap
                                )
                                .padding(2.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("▦", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                            }
                            Text("Heatmap", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Text(entryCount.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                            Text(
                                "$tagCount tags",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    DashboardInsightPage.HISTORY -> {
                        ReadingHistoryPreviewPanel(
                            analyticsSnapshot = analyticsSnapshot,
                            analyticsLoading = analyticsLoading,
                            interactionSource = historyInteraction,
                            onOpenHistory = onOpenHistory,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(panelShape)
                                .padding(2.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(normalizedPageOrder.size) { index ->
                    val selected = pagerState.currentPage == index
                    val indicatorWidth by androidx.compose.animation.core.animateDpAsState(
                        targetValue = if (selected) 18.dp else 7.dp,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 180, easing = FastOutSlowInEasing),
                        label = "subscriptionHeatmapIndicatorWidth"
                    )
                    val indicatorColor by androidx.compose.animation.animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 180, easing = FastOutSlowInEasing),
                        label = "subscriptionHeatmapIndicatorColor"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = indicatorWidth, height = 7.dp)
                            .background(
                                indicatorColor,
                                RoundedCornerShape(999.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
internal fun ModernHeatmapOverviewCard(
    entryCount: Int,
    tagCount: Int,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Heatmap Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("$entryCount entries • $tagCount tags", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(4) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(24) { col ->
                            val active = ((row * 17 + col * 11 + entryCount + tagCount) % 7) < 3
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .background(
                                        if (active) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.70f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
                                        },
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
