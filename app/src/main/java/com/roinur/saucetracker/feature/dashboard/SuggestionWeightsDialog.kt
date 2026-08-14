package com.roinur.saucetracker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.roinur.saucetracker.feature.dashboard.DashboardViewModel
import kotlin.math.roundToInt

@Composable
internal fun SuggestionWeightsDialog(
    vm: DashboardViewModel,
    maxHeight: Dp,
    onDismiss: () -> Unit,
    onPressStart: () -> Unit,
    runOnPressWhen: () -> Boolean
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .widthIn(max = 640.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tune suggestions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                    ImmediateActionText(
                        label = "Close",
                        onAction = onDismiss,
                        onPressStart = onPressStart,
                        runOnPressWhen = runOnPressWhen,
                        textStyle = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "Choose how strongly each part of your library shapes future recommendations.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "0% ignores a signal, 100% is balanced, and 200% gives it maximum influence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (vm.incognitoModeEnabled) {
                    Text(
                        text = "Weight changes are disabled while incognito mode is enabled.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SuggestionWeightCategory.entries.forEach { category ->
                    val weight = (vm.suggestionCategoryWeights[category] ?: 1f).coerceIn(0f, 2f)
                    val percent = (weight * 100f).roundToInt().coerceIn(0, 200)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = category.label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "$percent%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Slider(
                            value = weight,
                            enabled = !vm.incognitoModeEnabled,
                            onValueChange = { next ->
                                val snapped = (next * 10f).roundToInt() / 10f
                                vm.setSuggestionCategoryWeight(category, snapped)
                            },
                            onValueChangeFinished = {
                                if (!vm.incognitoModeEnabled && !vm.suggestedEntriesCollapsed) {
                                    vm.refreshSuggestedEntries(force = true)
                                }
                            },
                            valueRange = 0f..2f,
                            steps = 19
                        )
                    }
                }
                val themeStrength = vm.suggestionThemeStrength.coerceIn(0f, 2f)
                val themePercent = (themeStrength * 100f).roundToInt().coerceIn(0, 200)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Theme strength",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "$themePercent%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Slider(
                        value = themeStrength,
                        enabled = !vm.incognitoModeEnabled,
                        onValueChange = { next ->
                            val snapped = (next * 10f).roundToInt() / 10f
                            vm.updateSuggestionThemeStrength(snapped)
                        },
                        onValueChangeFinished = {
                            if (!vm.incognitoModeEnabled && !vm.suggestedEntriesCollapsed) {
                                vm.refreshSuggestedEntries(force = true)
                            }
                        },
                        valueRange = 0f..2f,
                        steps = 19
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = vm::resetSuggestionCategoryWeights,
                        enabled = !vm.incognitoModeEnabled
                    ) {
                        Text("Reset to defaults")
                    }
                }
            }
        }
    }
}
