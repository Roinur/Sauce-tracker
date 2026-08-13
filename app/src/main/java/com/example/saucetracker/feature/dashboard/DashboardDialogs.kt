package com.example.saucetracker

import com.example.saucetracker.data.backup.*
import com.example.saucetracker.data.downloads.*
import com.example.saucetracker.core.ui.components.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.saucetracker.core.media.*
import com.example.saucetracker.feature.library.creators.*
import com.example.saucetracker.feature.library.detail.*
import com.example.saucetracker.feature.library.history.*
import com.example.saucetracker.feature.library.tags.*
import com.example.saucetracker.feature.settings.*
import com.example.saucetracker.feature.subscriptions.*
import com.example.saucetracker.feature.suggestions.*
import kotlin.math.max

@Composable
fun BrowserDuplicateCheckModeDialog(
    title: String,
    currentMode: BrowserDuplicateCheckMode,
    defaultMode: BrowserDuplicateCheckMode,
    temporary: Boolean,
    coverSystemBars: Boolean = false,
    onSelect: (BrowserDuplicateCheckMode) -> Unit,
    onReset: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    AnimatedOverlayCard(
        onDismissRequest = onDismiss,
        modifier = Modifier.heightIn(max = 520.dp),
        coverSystemBars = coverSystemBars,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    ImmediateActionText(
                        label = "Close",
                        onAction = onDismiss,
                        textStyle = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = if (temporary) {
                        "This only changes duplicate checking for the current browser session. Default: ${defaultMode.label}."
                    } else {
                        "Choose how aggressively the built-in browser should calculate duplicate hints by default."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BrowserDuplicateCheckMode.entries.forEach { mode: BrowserDuplicateCheckMode ->
                    val selected = mode == currentMode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                            .clickable { onSelect(mode) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selected) {
                            Text(
                                text = "Current",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                onReset?.let { resetAction ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = resetAction) {
                        Text(if (temporary) "Use Default" else "Set To Default")
                    }
                }
        }
    }
}
}

@Composable
internal fun <T> SelectionDialog(
    title: String,
    options: List<T>,
    selectedKey: String,
    optionKey: (T) -> String,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedOverlayCard(
        onDismissRequest = onDismiss,
        modifier = Modifier.heightIn(max = 520.dp),
        coverSystemBars = true,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    ImmediateActionText(
                        label = "Close",
                        onAction = onDismiss,
                        textStyle = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                options.forEach { option: T ->
                    val selected = optionKey(option) == selectedKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                            .clickable { onSelect(option) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = optionLabel(option),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (selected) {
                            Text(
                                text = "Selected",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onReset) {
                        Text("Set To Default")
                    }
                }
        }
    }
}

internal data class GraphTagPrevalenceStats(
    val label: String,
    val websitePercent: Float?,
    val libraryPercent: Float,
    val relativeFactor: Float?
)

internal fun buildGraphTagPrevalenceStats(
    node: TagGraphNode,
    snapshot: TagGraphSnapshot
): GraphTagPrevalenceStats {
    val libraryShare = node.localCount.toFloat() / snapshot.totalEntries.coerceAtLeast(1).toFloat()
    val websiteShare = if (node.popularCount > 0 && snapshot.totalPopularTagUsage > 0L) {
        node.popularCount.toFloat() / snapshot.totalPopularTagUsage.toFloat()
    } else {
        null
    }
    val relativeFactor = websiteShare?.let { web ->
        if (web <= 0f) null else libraryShare / web
    }
    return GraphTagPrevalenceStats(
        label = node.name,
        websitePercent = websiteShare?.times(100f),
        libraryPercent = libraryShare * 100f,
        relativeFactor = relativeFactor
    )
}
