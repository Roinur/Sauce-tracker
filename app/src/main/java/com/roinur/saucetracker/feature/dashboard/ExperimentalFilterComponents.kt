package com.roinur.saucetracker

import com.roinur.saucetracker.data.backup.*
import com.roinur.saucetracker.data.downloads.*
import com.roinur.saucetracker.core.ui.components.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints
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
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun ExperimentalFilterStatusStrip(
    searchText: String,
    activeTagIds: List<Long>,
    tags: List<TagCountRow>,
    readFilter: EntryReadFilterMode,
    incognitoModeEnabled: Boolean,
    onClearSearch: () -> Unit,
    onClearTag: (Long) -> Unit,
    onClearAllTags: () -> Unit,
    onClearReadFilter: () -> Unit
) {
    val activeTags = remember(activeTagIds, tags) {
        activeTagIds.mapNotNull { tagId ->
            tags.firstOrNull { it.id == tagId }?.let { tagId to it.name }
        }
    }
    val hasSearch = searchText.isNotBlank()
    val hasReadFilter = readFilter != EntryReadFilterMode.ALL
    if (!hasSearch && activeTags.isEmpty() && !hasReadFilter) return

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Active",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            if (hasSearch) {
                ExperimentalFilterChip(
                    label = if (incognitoModeEnabled) "Search: •••••" else "Search: ${searchText.trim()}",
                    onClear = onClearSearch,
                    enabled = !incognitoModeEnabled
                )
            }
            activeTags.forEach { (tagId, tagName) ->
                ExperimentalFilterChip(
                    label = if (incognitoModeEnabled) "Tag: •••••" else "Tag: $tagName",
                    onClear = { onClearTag(tagId) },
                    enabled = !incognitoModeEnabled
                )
            }
            if (activeTags.size > 1) {
                ExperimentalFilterChip(
                    label = "Clear tags",
                    onClear = onClearAllTags,
                    enabled = !incognitoModeEnabled
                )
            }
            if (hasReadFilter) {
                ExperimentalFilterChip(
                    label = readableEntryReadFilterModeLabel(readFilter),
                    onClear = onClearReadFilter
                )
            }
        }
    }
}

@Composable
internal fun ExperimentalFilterChip(
    label: String,
    onClear: () -> Unit,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(999.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (enabled) 1f else 0.55f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.clip(shape)
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            TextButton(
                onClick = onClear,
                enabled = enabled,
                modifier = Modifier.heightIn(min = 0.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) {
                Text("x", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun ExperimentalTagFilterChipField(
    chips: List<Pair<Long, String>>,
    showingCount: Int,
    incognitoModeEnabled: Boolean,
    onRemoveTag: (Long) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasChips = chips.isNotEmpty()
    val fieldValue = when {
        hasChips -> " "
        incognitoModeEnabled -> "••••••••••••••"
        else -> "No tag selected"
    }
    val shape = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = 14.dp,
        bottomEnd = 14.dp
    )
    Layout(
        modifier = modifier,
        content = {
            OutlinedTextField(
                value = fieldValue,
                onValueChange = {},
                readOnly = true,
                label = { Text("Tag filter") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = if (hasChips) Color.Transparent else MaterialTheme.colorScheme.onSurface
                ),
                trailingIcon = null,
                shape = shape,
                modifier = Modifier.fillMaxSize()
            )
            if (hasChips) {
                chips.forEach { (tagId, tagName) ->
                    ExperimentalInlineTagChip(
                        label = if (incognitoModeEnabled) "•••••" else tagName,
                        enabled = !incognitoModeEnabled,
                        onRemove = { onRemoveTag(tagId) }
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Showing:$showingCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(
                        onClick = onClearAll,
                        enabled = !incognitoModeEnabled,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_clear_circle_24),
                            contentDescription = "Clear tag filter",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    ) { measurables, constraints ->
        val minHeightPx = 64.dp.roundToPx()
        val leftPaddingPx = 16.dp.roundToPx()
        val rightPaddingPx = 10.dp.roundToPx()
        val topPaddingPx = 24.dp.roundToPx()
        val rowGapPx = 4.dp.roundToPx()
        val chipGapPx = 4.dp.roundToPx()
        val contentWidth = (constraints.maxWidth - leftPaddingPx - rightPaddingPx).coerceAtLeast(0)
        val fieldMeasurable = measurables.first()
        val statusMeasurable = if (hasChips) measurables.last() else null
        val chipMeasurables = if (hasChips) measurables.drop(1).dropLast(1) else emptyList()
        val statusPlaceable = statusMeasurable?.measure(
            constraints.copy(minWidth = 0, minHeight = 0)
        )
        val chipPlaceables = chipMeasurables.map { measurable ->
            measurable.measure(
                constraints.copy(minWidth = 0, maxWidth = contentWidth, minHeight = 0)
            )
        }

        val firstRowLimit = (contentWidth - (statusPlaceable?.width ?: 0) - if (statusPlaceable != null) chipGapPx else 0)
            .coerceAtLeast(0)
        val rows = mutableListOf<MutableList<Int>>()
        val rowWidths = mutableListOf<Int>()
        val rowHeights = mutableListOf<Int>()
        var currentRow = mutableListOf<Int>()
        var currentWidth = 0
        var currentHeight = 0

        fun activeLimit(): Int = if (rows.isEmpty()) firstRowLimit else contentWidth
        fun pushRow() {
            if (currentRow.isNotEmpty()) {
                rows += currentRow
                rowWidths += currentWidth
                rowHeights += currentHeight
            }
            currentRow = mutableListOf()
            currentWidth = 0
            currentHeight = 0
        }

        chipPlaceables.forEachIndexed { index, placeable ->
            val proposedWidth = if (currentRow.isEmpty()) {
                placeable.width
            } else {
                currentWidth + chipGapPx + placeable.width
            }
            if (currentRow.isNotEmpty() && proposedWidth > activeLimit()) {
                pushRow()
            }
            currentRow += index
            currentWidth = if (currentWidth == 0) placeable.width else currentWidth + chipGapPx + placeable.width
            currentHeight = max(currentHeight, placeable.height)
        }
        pushRow()

        val chipRowsHeight = rowHeights.sum() + (rowGapPx * (rowHeights.size - 1).coerceAtLeast(0))
        val layoutHeight = max(minHeightPx, topPaddingPx + chipRowsHeight + 10.dp.roundToPx())
        val fieldPlaceable = fieldMeasurable.measure(
            constraints.copy(
                minWidth = constraints.maxWidth,
                maxWidth = constraints.maxWidth,
                minHeight = layoutHeight,
                maxHeight = layoutHeight
            )
        )

        layout(width = constraints.maxWidth, height = layoutHeight) {
            fieldPlaceable.place(0, 0)
            var y = topPaddingPx
            rows.forEachIndexed { rowIndex, row ->
                var x = leftPaddingPx
                row.forEach { chipIndex ->
                    val placeable = chipPlaceables[chipIndex]
                    placeable.place(x, y + ((rowHeights[rowIndex] - placeable.height) / 2))
                    x += placeable.width + chipGapPx
                }
                if (rowIndex == 0 && statusPlaceable != null) {
                    statusPlaceable.place(
                        x = constraints.maxWidth - rightPaddingPx - statusPlaceable.width,
                        y = y + ((rowHeights[rowIndex].coerceAtLeast(statusPlaceable.height) - statusPlaceable.height) / 2)
                    )
                }
                y += rowHeights[rowIndex] + rowGapPx
            }
        }
    }
}

@Composable
internal fun ExperimentalInlineTagChip(
    label: String,
    enabled: Boolean,
    onRemove: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    Surface(
        shape = shape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)),
        modifier = Modifier
            .clip(shape)
            .height(30.dp)
            .clickable(enabled = enabled, onClick = onRemove)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
