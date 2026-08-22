package com.roinur.saucetracker.feature.suggestions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.roinur.saucetracker.core.ui.privacy.privacyObfuscate
import com.roinur.saucetracker.feature.dashboard.DashboardViewModel
import com.roinur.saucetracker.ThumbnailImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TasteTrainingDialog(vm: DashboardViewModel, onDismiss: () -> Unit) {
    val prompt = vm.tasteTrainingPrompts.firstOrNull()
    var selected by remember(prompt?.code) { mutableStateOf(emptySet<String>()) }
    var notMetadata by remember(prompt?.code) { mutableStateOf(false) }
    var normallyLike by remember(prompt?.code) { mutableStateOf(false) }
    var reviewSaved by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Train your model", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(
                    "Adds a small explicit signal to Suggested entries. Your existing ratings, tuning and recommendation engine remain the main model.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (vm.tasteTrainingFeedback.isNotEmpty()) {
                    TextButton(onClick = { reviewSaved = !reviewSaved }) {
                        Text(if (reviewSaved) "Back to training" else "Review saved (${vm.tasteTrainingFeedback.size})")
                    }
                }
                if (reviewSaved) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(vm.tasteTrainingFeedback, key = { it.code }) { feedback ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "#${feedback.code} • ${feedback.rating}★",
                                        modifier = Modifier.privacyObfuscate(
                                            enabled = false,
                                            overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                                            cornerRadius = 7.dp
                                        ),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        when {
                                            feedback.notAboutMetadata -> "Not about metadata"
                                            feedback.normallyLikeButNotThisEntry -> "Reason not listed"
                                            else -> "${feedback.selectedDriverKeys.size} selected drivers"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = { vm.deleteTasteTrainingFeedback(feedback.code) }) { Text("Edit") }
                            }
                        }
                    }
                    Text("Edit removes the saved answer and returns that entry to the training queue.", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Done") }
                    return@Column
                }
                if (prompt == null) {
                    Text("No unreviewed high or low ratings remain.")
                    Text("${vm.tasteTrainingFeedbackCount} answers saved locally and included in backups.")
                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Done") }
                } else {
                    Text(
                        "You gave this a ${prompt.rating}. Why?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        ThumbnailImage(
                            thumbnailUrl = prompt.thumbnailUrl,
                            backupCode = prompt.code,
                            contentDescription = "Cover for training entry ${prompt.code}",
                            obscure = false,
                            preferLowRes = false,
                            onClick = { vm.openSuggestedEntryInBrowser(prompt.code) },
                            modifier = Modifier.width(104.dp).height(138.dp)
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                "#${prompt.code}",
                                modifier = Modifier.privacyObfuscate(
                                    enabled = false,
                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                                    cornerRadius = 7.dp
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                prompt.title,
                                modifier = Modifier.privacyObfuscate(
                                    enabled = false,
                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                                    cornerRadius = 7.dp
                                ),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("${prompt.drivers.size} possible reasons", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (vm.tasteTrainingPromptLoading) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(72.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(prompt.drivers, key = { it.key }) { driver ->
                                val checked = driver.key in selected
                                val rowShape = RoundedCornerShape(12.dp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(rowShape)
                                        .clickable {
                                            selected = if (checked) selected - driver.key else selected + driver.key
                                            notMetadata = false
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.padding(start = 10.dp)) {
                                        Text(
                                            driver.name,
                                            modifier = Modifier.privacyObfuscate(
                                                enabled = false,
                                                overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                                                cornerRadius = 7.dp
                                            ),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(driver.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    TrainingEscapeRow("Not about metadata", notMetadata) {
                        notMetadata = !notMetadata
                        if (notMetadata) selected = emptySet()
                    }
                    TrainingEscapeRow("Reason not listed", normallyLike) {
                        normallyLike = !normallyLike
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { vm.skipTasteTrainingPrompt(prompt.code) }) { Text("Skip") }
                        TextButton(
                            enabled = selected.isNotEmpty() || notMetadata || normallyLike,
                            onClick = {
                                vm.saveTasteTrainingFeedback(prompt, selected, notMetadata, normallyLike)
                            }
                        ) { Text("Save and next") }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainingEscapeRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
