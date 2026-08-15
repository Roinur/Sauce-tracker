package com.roinur.saucetracker

import com.roinur.saucetracker.data.backup.*
import com.roinur.saucetracker.data.downloads.*
import com.roinur.saucetracker.core.ui.components.*
import com.roinur.saucetracker.core.time.UserCalendar
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
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
import java.time.LocalDate

@Composable
internal fun LocalEntryHoldPopup(
    code: Int,
    rating: Int,
    modifier: Modifier = Modifier
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val maxWidth = (screenWidth - 24.dp).coerceAtLeast(220.dp)
    val popupWidth = (screenWidth * 0.94f).coerceIn(220.dp, maxWidth)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.width(popupWidth)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Drag to rate #$code",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (0..5).forEach { value ->
                    val selected = value == rating
                    val holdButtonShape = RoundedCornerShape(14.dp)
                    Box(
                        modifier = Modifier
                            .weight(if (value == 0) 1.7f else 1f)
                            .clip(holdButtonShape)
                            .background(
                                when (value) {
                                    0 -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                                        alpha = if (selected) 0.96f else 0.72f
                                    )
                                    else -> MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = if (selected) 0.68f else 0.34f
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingShimmerOverlay(
                            active = selected,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .matchParentSize()
                                .clip(holdButtonShape)
                        )
                        Text(
                            text = if (value == 0) "Cancel" else "★",
                            style = if (value == 0) {
                                MaterialTheme.typography.labelMedium
                            } else {
                                MaterialTheme.typography.titleMedium
                            },
                            color = if (selected) {
                                if (value == 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    RATING_STAR_GOLD
                                }
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                            },
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun LocalSuggestedHoldOverlay(
    code: Int,
    action: SuggestedDragAction,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.86f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Suggested entry #$code",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val cancelSelected = action == SuggestedDragAction.CANCEL
                val hideSelected = action == SuggestedDragAction.HIDE
                val holdButtonShape = RoundedCornerShape(14.dp)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(holdButtonShape)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                                alpha = if (cancelSelected) 0.96f else 0.72f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingShimmerOverlay(
                        active = cancelSelected,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(holdButtonShape)
                    )
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (cancelSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                        },
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(holdButtonShape)
                        .background(
                            UNREAD_STATE_COLOR.copy(
                                alpha = if (hideSelected) 0.24f else 0.14f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingShimmerOverlay(
                        active = hideSelected,
                        tint = UNREAD_STATE_COLOR,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(holdButtonShape)
                    )
                    Text(
                        text = "Don't show again",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (hideSelected) {
                            UNREAD_STATE_COLOR
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                        },
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

internal data class HeatmapCell(
    val date: LocalDate,
    val pagesRead: Int,
    val entriesRead: Int,
    val inRange: Boolean
)

@Composable
internal fun ActivityHeatmap(
    range: StatsRange,
    points: List<DailyActivityPoint>,
    onDaySelected: (DailyActivityPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = UserCalendar.today()
    val startDate = when (range) {
        StatsRange.TODAY -> today
        StatsRange.WEEK -> today.minusDays(6)
        StatsRange.MONTH -> today.withDayOfMonth(1)
        StatsRange.YEAR -> today.withDayOfYear(1)
        StatsRange.ALL_TIME -> points.minByOrNull { it.date }?.date ?: today
    }
    val endDate = today
    val alignedStart = startDate.minusDays((startDate.dayOfWeek.value - 1).toLong())
    val alignedEnd = endDate.plusDays((7 - endDate.dayOfWeek.value).toLong())

    val pointsByDate = points
        .groupBy { it.date }
        .mapValues { (_, items) ->
            DailyActivityPoint(
                date = items.first().date,
                pagesRead = items.sumOf { it.pagesRead }.coerceAtLeast(0),
                entriesRead = items.sumOf { it.entriesRead }.coerceAtLeast(0)
            )
        }

    val cells = buildList {
        var cursor = alignedStart
        while (!cursor.isAfter(alignedEnd)) {
            val point = pointsByDate[cursor]
            add(
                HeatmapCell(
                    date = cursor,
                    pagesRead = point?.pagesRead ?: 0,
                    entriesRead = point?.entriesRead ?: 0,
                    inRange = !cursor.isBefore(startDate) && !cursor.isAfter(endDate)
                )
            )
            cursor = cursor.plusDays(1)
        }
    }
    val weekColumns = cells.chunked(7)
    val maxPages = cells
        .asSequence()
        .filter { it.inRange }
        .maxOfOrNull { it.pagesRead }
        ?.coerceAtLeast(0)
        ?: 0

    fun activityLevel(pagesRead: Int): Int {
        if (pagesRead <= 0 || maxPages <= 0) return 0
        val ratio = pagesRead.toFloat() / maxPages.toFloat()
        return when {
            ratio >= 0.85f -> 4
            ratio >= 0.6f -> 3
            ratio >= 0.35f -> 2
            else -> 1
        }
    }

    val emptyColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
    val levelColors = listOf(
        emptyColor,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.46f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.63f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.80f)
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (weekColumns.isEmpty()) {
            Text(
                text = "No activity yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Top
            ) {
                items(weekColumns.size, key = { index -> weekColumns[index].firstOrNull()?.date.toString() }) { columnIndex ->
                    val column = weekColumns[columnIndex]
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        column.forEach { cell ->
                            val level = activityLevel(cell.pagesRead).coerceIn(0, 4)
                            val color = if (cell.inRange) {
                                levelColors[level]
                            } else {
                                emptyColor.copy(alpha = 0.12f)
                            }
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color)
                                    .border(
                                        width = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                                    .clickable(enabled = cell.inRange) {
                                        onDaySelected(
                                            DailyActivityPoint(
                                                date = cell.date,
                                                pagesRead = cell.pagesRead,
                                                entriesRead = cell.entriesRead
                                            )
                                        )
                                    }
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Less",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                levelColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Text(
                    text = "More",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
