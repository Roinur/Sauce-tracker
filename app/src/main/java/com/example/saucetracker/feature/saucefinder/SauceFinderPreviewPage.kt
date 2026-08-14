package com.example.saucetracker.feature.saucefinder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.saucetracker.core.ui.privacy.privacyObfuscate

@Composable
internal fun SauceFinderPreviewPage(
    state: SauceFinderUiState,
    incognitoModeEnabled: Boolean,
    onChooseImage: () -> Unit,
    onBuildIndex: () -> Unit,
    onPauseIndex: () -> Unit,
    onOpenMatch: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sauce finder",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black
            )
            Text(
                text = formatSauceFinderSize(state.indexBytes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${state.indexedImages} images  •  ${state.indexedEntries} entries",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (state.indexing && state.progress != null) {
            LinearProgressIndicator(
                progress = { state.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        } else {
            Spacer(modifier = Modifier.height(5.dp))
        }

        val match = state.match
        if (match != null) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                        blurRadius = 9.dp,
                        cornerRadius = 10.dp
                    ),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = match.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append("#${match.entryCode}")
                        if (match.pageNumber > 0) append("  •  page ${match.pageNumber}")
                        append("  •  ${(match.similarity * 100).toInt()}%")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
                Text(
                    text = match.confidence,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = state.message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onChooseImage, enabled = !state.matching) {
                Text(if (state.matching) "Searching..." else "Pick image")
            }
            if (match != null) {
                TextButton(
                    onClick = { onOpenMatch(match.entryCode) },
                    enabled = !incognitoModeEnabled
                ) {
                    Text("Open")
                }
            } else {
                TextButton(onClick = if (state.indexing) onPauseIndex else onBuildIndex) {
                    Text(if (state.indexing) "Pause index" else if (state.indexedImages > 0) "Resume index" else "Build index")
                }
            }
        }
    }
}

private fun formatSauceFinderSize(bytes: Long): String = when {
    bytes <= 0L -> "Index empty"
    bytes < 1024L * 1024L -> "${(bytes / 1024L).coerceAtLeast(1L)} KB index"
    else -> String.format(java.util.Locale.US, "%.1f MB index", bytes / (1024.0 * 1024.0))
}
