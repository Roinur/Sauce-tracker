package com.roinur.saucetracker.feature.library.presets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.roinur.saucetracker.TagCountRow
import com.roinur.saucetracker.core.ui.privacy.privacyObfuscate
import com.roinur.saucetracker.feature.dashboard.DashboardViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TagPresetsDialog(vm: DashboardViewModel, onDismiss: () -> Unit) {
    var editing by remember { mutableStateOf<TagPreset?>(null) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            if (editing == null) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Tag presets", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                            Text("Reusable filters, never library tags", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { editing = emptyPreset() }) { Text("New") }
                    }
                    if (vm.tagPresets.isEmpty()) {
                        Text("No presets yet. Add tags to include, match either, or hide.")
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            itemsIndexed(vm.tagPresets, key = { _, preset -> preset.id }) { index, preset ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { vm.applyTagPreset(preset); onDismiss() }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            preset.name,
                                            modifier = Modifier.privacyObfuscate(
                                                enabled = false,
                                                overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                                                cornerRadius = 7.dp
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                preset.terms.count { it.role == TagPresetRole.INCLUDE }.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            PresetStatusGlyph(role = TagPresetRole.INCLUDE)
                                            Text(
                                                preset.terms.count { it.role == TagPresetRole.EITHER }.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            PresetStatusGlyph(role = TagPresetRole.EITHER)
                                            Text(
                                                preset.terms.count { it.role == TagPresetRole.HIDE }.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            PresetStatusGlyph(role = TagPresetRole.HIDE)
                                        }
                                    }
                                    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                                        TextButton(
                                            onClick = { editing = preset },
                                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)
                                        ) { Text("Edit") }
                                        TextButton(
                                            onClick = { vm.moveTagPreset(preset.id, -1) },
                                            enabled = index > 0,
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                        ) { Text("↑") }
                                        TextButton(
                                            onClick = { vm.moveTagPreset(preset.id, 1) },
                                            enabled = index < vm.tagPresets.lastIndex,
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                        ) { Text("↓") }
                                        TextButton(
                                            onClick = { vm.deleteTagPreset(preset.id) },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                        ) { Text("×", style = MaterialTheme.typography.titleLarge) }
                                    }
                                }
                            }
                        }
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Done") }
                }
            } else {
                TagPresetEditor(
                    initial = editing!!,
                    availableTags = vm.tags,
                    onCancel = { editing = null },
                    onSave = { vm.saveTagPreset(it); editing = null }
                )
            }
        }
    }
}

@Composable
private fun PresetStatusGlyph(role: TagPresetRole) {
    val color = when (role) {
        TagPresetRole.INCLUDE -> Color(0xFF43A047)
        TagPresetRole.EITHER -> Color(0xFFFFA000)
        TagPresetRole.HIDE -> Color(0xFFE53935)
    }
    Canvas(modifier = Modifier.size(11.dp)) {
        val stroke = 1.8.dp.toPx()
        when (role) {
            TagPresetRole.INCLUDE -> {
            drawLine(
                color = color,
                start = Offset(size.width * 0.08f, size.height * 0.54f),
                end = Offset(size.width * 0.38f, size.height * 0.84f),
                strokeWidth = stroke,
                cap = StrokeCap.Square
            )
            drawLine(
                color = color,
                start = Offset(size.width * 0.38f, size.height * 0.84f),
                end = Offset(size.width * 0.94f, size.height * 0.12f),
                strokeWidth = stroke,
                cap = StrokeCap.Square
            )
            }
            TagPresetRole.EITHER -> {
                val junction = Offset(size.width * 0.50f, size.height * 0.48f)
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.50f, size.height * 0.94f),
                    end = junction,
                    strokeWidth = stroke,
                    cap = StrokeCap.Square
                )
                drawLine(
                    color = color,
                    start = junction,
                    end = Offset(size.width * 0.12f, size.height * 0.10f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Square
                )
                drawLine(
                    color = color,
                    start = junction,
                    end = Offset(size.width * 0.88f, size.height * 0.10f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Square
                )
            }
            TagPresetRole.HIDE -> {
            drawLine(
                color = color,
                start = Offset(size.width * 0.14f, size.height * 0.14f),
                end = Offset(size.width * 0.86f, size.height * 0.86f),
                strokeWidth = stroke,
                cap = StrokeCap.Square
            )
            drawLine(
                color = color,
                start = Offset(size.width * 0.86f, size.height * 0.14f),
                end = Offset(size.width * 0.14f, size.height * 0.86f),
                strokeWidth = stroke,
                cap = StrokeCap.Square
            )
            }
        }
    }
}

@Composable
private fun TagPresetEditor(
    initial: TagPreset,
    availableTags: List<TagCountRow>,
    onCancel: () -> Unit,
    onSave: (TagPreset) -> Unit
) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var search by remember(initial.id) { mutableStateOf("") }
    var roles by remember(initial.id) {
        mutableStateOf(initial.terms.associate { it.key to it.role })
    }
    val visible = remember(availableTags, search) {
        val needle = search.trim().lowercase()
        availableTags.filter { needle.isBlank() || it.name.lowercase().contains(needle) || it.type.lowercase().contains(needle) }.take(400)
    }
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Edit preset", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier
                .fillMaxWidth()
                .privacyObfuscate(
                    enabled = false,
                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                    cornerRadius = 12.dp
                )
        )
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Find tag or creator") },
            modifier = Modifier
                .fillMaxWidth()
                .privacyObfuscate(
                    enabled = false,
                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                    cornerRadius = 12.dp
                )
        )
        Text(
            "Include: every selected tag must be present. Either: at least one selected tag must be present. Hide: matching entries are removed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text("Tap the active rule again to remove it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(visible, key = { it.id }) { tag ->
                val key = "${tag.type.lowercase()}|${tag.name.lowercase()}"
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            tag.name,
                            modifier = Modifier.privacyObfuscate(
                                enabled = false,
                                overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                                cornerRadius = 7.dp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(tag.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TagPresetRole.entries.forEach { role ->
                        FilterChip(
                            selected = roles[key] == role,
                            onClick = { roles = roles.toMutableMap().apply { if (this[key] == role) remove(key) else put(key, role) } },
                            label = { Text(role.displayLabel()) }
                        )
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            TextButton(enabled = name.isNotBlank() && roles.isNotEmpty(), onClick = {
                val now = System.currentTimeMillis()
                val tagsByKey = availableTags.associateBy { "${it.type.lowercase()}|${it.name.lowercase()}" }
                onSave(
                    initial.copy(
                        name = name.trim(),
                        terms = roles.mapNotNull { (key, role) -> tagsByKey[key]?.let { TagPresetTerm(it.name, it.type, role) } },
                        createdAtMillis = initial.createdAtMillis.takeIf { it > 0 } ?: now,
                        updatedAtMillis = now
                    )
                )
            }) { Text("Save") }
        }
    }
}

private fun emptyPreset(): TagPreset = TagPreset(
    id = UUID.randomUUID().toString(),
    name = "",
    terms = emptyList(),
    createdAtMillis = 0L,
    updatedAtMillis = 0L
)

private fun TagPresetRole.displayLabel(): String = when (this) {
    TagPresetRole.INCLUDE -> "Include"
    TagPresetRole.EITHER -> "Either"
    TagPresetRole.HIDE -> "Hide"
}
