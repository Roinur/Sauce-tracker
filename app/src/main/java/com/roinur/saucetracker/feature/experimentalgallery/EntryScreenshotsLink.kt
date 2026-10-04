package com.roinur.saucetracker.feature.experimentalgallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roinur.saucetracker.core.ui.privacy.privacyObfuscate
import com.roinur.saucetracker.data.source.SourceChapter

/** A text link into the existing gallery, not a second screenshot browser. */
@Composable
internal fun EntryScreenshotsLink(
    sourceId: String,
    remoteId: String,
    title: String,
    chapters: List<SourceChapter> = emptyList(),
    incognitoModeEnabled: Boolean,
    labelStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    linkStyle: TextStyle = MaterialTheme.typography.titleMedium
) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.privacyObfuscate(
            enabled = incognitoModeEnabled,
            overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        )
    ) {
        Text("Saved:", style = labelStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.clip(MaterialTheme.shapes.small)
            .clickable(enabled = !incognitoModeEnabled) {
                context.startActivity(ExperimentalGalleryActivity.createEntryIntent(
                    context, sourceId, remoteId, title,
                    chapters.map { it.id.take(12).lowercase() }.distinct().take(8_000)
                ))
            }
            .heightIn(min = 32.dp)
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("Screenshots", style = linkStyle, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}
