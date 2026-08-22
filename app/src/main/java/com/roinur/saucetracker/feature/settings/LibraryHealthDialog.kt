package com.roinur.saucetracker.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.roinur.saucetracker.feature.dashboard.DashboardViewModel

@Composable
internal fun LibraryHealthDialog(vm: DashboardViewModel, onDismiss: () -> Unit) {
    LaunchedEffect(Unit) { vm.runLibraryHealthScan() }
    var confirmCacheClear by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth(0.94f), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Library Health", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("Read-only diagnostics. Nothing here deletes or rewrites library records.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (vm.libraryHealthScanning) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    vm.libraryHealthReport?.let { report ->
                        Text(
                            when (report.level) {
                                LibraryHealthLevel.HEALTHY -> "Healthy"
                                LibraryHealthLevel.ATTENTION -> "Attention"
                                LibraryHealthLevel.ACTION_REQUIRED -> "Action required"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = when (report.level) {
                                LibraryHealthLevel.HEALTHY -> MaterialTheme.colorScheme.primary
                                LibraryHealthLevel.ATTENTION -> MaterialTheme.colorScheme.tertiary
                                LibraryHealthLevel.ACTION_REQUIRED -> MaterialTheme.colorScheme.error
                            }
                        )
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(report.checks) { check ->
                                Column {
                                    Text(check.title, fontWeight = FontWeight.Bold)
                                    Text(check.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = vm::runLibraryHealthScan, enabled = !vm.libraryHealthScanning) { Text("Scan again") }
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
                if (confirmCacheClear) {
                    Text(
                        "This removes only the suggestion result/metadata cache and the precalculated Entry Heatmap layout. Library entries, ratings, history, presets, training data, downloads, and backups are untouched.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { confirmCacheClear = false }) { Text("Cancel") }
                        TextButton(onClick = { confirmCacheClear = false; vm.clearRebuildableCachesFromHealth() }) { Text("Clear caches") }
                    }
                } else {
                    TextButton(onClick = { confirmCacheClear = true }, modifier = Modifier.align(Alignment.End)) {
                        Text("Rebuildable cache actions")
                    }
                }
            }
        }
    }
}
