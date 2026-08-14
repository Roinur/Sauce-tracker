package com.example.saucetracker

import com.example.saucetracker.data.backup.*
import com.example.saucetracker.data.downloads.*
import com.example.saucetracker.core.ui.components.*
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.saucetracker.core.diagnostics.PerformanceMetrics
import com.example.saucetracker.core.media.*
import com.example.saucetracker.core.ui.privacy.privacyObfuscate
import com.example.saucetracker.feature.library.creators.*
import com.example.saucetracker.feature.library.detail.*
import com.example.saucetracker.feature.library.history.*
import com.example.saucetracker.feature.library.tags.*
import com.example.saucetracker.feature.settings.*
import com.example.saucetracker.feature.subscriptions.*
import com.example.saucetracker.feature.suggestions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun GraphTagPrevalenceBar(
    nodes: List<TagGraphNode>,
    snapshot: TagGraphSnapshot,
    modifier: Modifier = Modifier
) {
    if (nodes.isEmpty()) return
    val stats = remember(nodes, snapshot) {
        nodes.map { buildGraphTagPrevalenceStats(it, snapshot) }
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (stats.size == 1) {
                Text(
                    text = stats.first().label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = stats.joinToString(" • ") { it.label },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val websiteAverageShare = stats
                    .mapNotNull { it.websitePercent?.div(100f) }
                    .average()
                    .takeIf { !it.isNaN() }
                val libraryAverageShare = stats
                    .map { it.libraryPercent / 100f }
                    .average()
                    .takeIf { !it.isNaN() }
                val websiteAverage = websiteAverageShare?.times(100f)
                val libraryAverage = (libraryAverageShare ?: 0.0) * 100.0
                val relativeAverage = if (websiteAverageShare != null && websiteAverageShare > 0.0 && libraryAverageShare != null) {
                    libraryAverageShare / websiteAverageShare
                } else {
                    null
                }
                GraphPrevalenceMetric(
                    label = "Website",
                    value = websiteAverage?.let { "%.1f%%".format(Locale.US, it) } ?: "n/a",
                    modifier = Modifier.weight(1f)
                )
                GraphPrevalenceMetric(
                    label = "Library",
                    value = "%.1f%%".format(Locale.US, libraryAverage),
                    modifier = Modifier.weight(1f)
                )
                GraphPrevalenceMetric(
                    label = "Relative",
                    value = relativeAverage?.let { "%.1fx".format(Locale.US, it) } ?: "n/a",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
internal fun GraphPrevalenceMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun GraphCompactEntryRow(
    detail: EntryDetail,
    incognitoModeEnabled: Boolean,
    averageRating: Float? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rowShape = RoundedCornerShape(16.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(rowShape)
            .clickable(onClick = onClick),
        shape = rowShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThumbnailImage(
                thumbnailUrl = detail.thumbnailUrl,
                backupCode = detail.code,
                contentDescription = "Cover for code ${detail.code}",
                obscure = incognitoModeEnabled,
                modifier = Modifier
                    .size(width = 72.dp, height = 96.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                EntryCodeLine(
                    code = detail.code,
                    showSessionNewBadge = false,
                    incognitoModeEnabled = incognitoModeEnabled,
                    textStyle = MaterialTheme.typography.bodySmall,
                    codeColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                    )
                )
                RatingSummaryLine(
                    rating = averageRating ?: detail.rating.toFloat(),
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                    ),
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (detail.isRead) "Read" else "Unread",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (detail.isRead) READ_STATE_COLOR else UNREAD_STATE_COLOR,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                    )
                )
            }
        }
    }
}

@Composable
internal fun ThumbnailImage(
    thumbnailUrl: String,
    backupCode: Int? = null,
    contentDescription: String,
    obscure: Boolean = false,
    preferLowRes: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val initialFullBitmap = thumbnailUrl.takeIf { it.isNotBlank() }
        ?.let { ThumbnailBitmapCache.get(it, lowRes = false) }
    val initialLowBitmap = thumbnailUrl.takeIf { it.isNotBlank() }
        ?.let { ThumbnailBitmapCache.get(it, lowRes = true) }
    val initialBitmap = if (preferLowRes) {
        initialLowBitmap ?: initialFullBitmap
    } else {
        initialFullBitmap ?: initialLowBitmap
    }
    val initialLoadFinished = thumbnailUrl.isBlank() ||
        (if (preferLowRes) initialBitmap != null else initialFullBitmap != null)
    val thumbnailState by produceState(
        initialValue = initialBitmap to initialLoadFinished,
        thumbnailUrl,
        backupCode,
        preferLowRes
    ) {
        if (thumbnailUrl.isBlank()) {
            value = null to true
            return@produceState
        }

        val fullCached = ThumbnailBitmapCache.get(thumbnailUrl, lowRes = false)
        val lowCached = ThumbnailBitmapCache.get(thumbnailUrl, lowRes = true)
        if (preferLowRes) {
            val cached = lowCached ?: fullCached
            if (cached != null) {
                value = cached to true
                return@produceState
            }
        } else {
            if (fullCached != null) {
                value = fullCached to true
                return@produceState
            }
            if (lowCached != null) {
                value = lowCached to false
            } else {
                value = null to false
            }
        }

        val fetched = withContext(Dispatchers.IO) {
            fetchThumbnailBitmap(
                context = context.applicationContext,
                url = thumbnailUrl,
                backupCode = backupCode,
                lowRes = preferLowRes
            )
        }
        if (fetched != null) {
            ThumbnailBitmapCache.put(thumbnailUrl, fetched, lowRes = preferLowRes)
            PerformanceMetrics.recordThumbnailLoadCompleted()
        }
        var resolved = fetched ?: value.first
        if (resolved == null) {
            // A cold-process startup preload may be fetching the same cover concurrently. Its
            // memory-cache write is intentionally not global Compose state, so briefly observe
            // that key before settling on "No preview" instead of requiring a screen remount.
            var cacheChecks = 0
            while (resolved == null && cacheChecks < 5) {
                resolved = ThumbnailBitmapCache.get(thumbnailUrl, lowRes = preferLowRes)
                    ?: ThumbnailBitmapCache.get(thumbnailUrl, lowRes = !preferLowRes)
                cacheChecks += 1
                if (resolved == null) delay(200L)
            }
        }
        if (resolved == null) {
            delay(600L)
            resolved = withContext(Dispatchers.IO) {
                fetchThumbnailBitmap(
                    context = context.applicationContext,
                    url = thumbnailUrl,
                    backupCode = backupCode,
                    lowRes = preferLowRes
                )
            }
            if (resolved != null) {
                ThumbnailBitmapCache.put(thumbnailUrl, requireNotNull(resolved), lowRes = preferLowRes)
                PerformanceMetrics.recordThumbnailLoadCompleted()
            }
        }
        value = resolved to true
    }

    val boxModifier = modifier
        .clip(MaterialTheme.shapes.small)
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .privacyObfuscate(
            enabled = obscure,
            overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
        )
        .let { base ->
            if (onClick != null) {
                base.clickable(onClick = onClick)
            } else {
                base
            }
        }

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        val imageBitmap = thumbnailState.first
        val loadFinished = thumbnailState.second
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else if (!loadFinished) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 1.75.dp
            )
        }
    }
}

@Composable
internal fun GraphEntryThumbnail(
    entry: TagGraphEntryNode,
    graphEntrySessionBitmaps: MutableMap<Int, ImageBitmap>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val initialBitmap = graphEntrySessionBitmaps[entry.code] ?: ThumbnailBitmapCache.get(entry.thumbnailUrl)
    val thumbnailState by produceState(
        initialValue = initialBitmap to (initialBitmap != null || entry.thumbnailUrl.isBlank()),
        entry.code,
        entry.thumbnailUrl
    ) {
        if (entry.thumbnailUrl.isBlank()) {
            value = null to true
            return@produceState
        }

        val sessionCached = graphEntrySessionBitmaps[entry.code]
        if (sessionCached != null) {
            value = sessionCached to true
            return@produceState
        }

        val globalCached = ThumbnailBitmapCache.get(entry.thumbnailUrl)
        if (globalCached != null) {
            graphEntrySessionBitmaps[entry.code] = globalCached
            value = globalCached to true
            return@produceState
        }

        value = null to false
        val fetched = withContext(Dispatchers.IO) {
            fetchGraphThumbnailBitmap(
                context = context.applicationContext,
                url = entry.thumbnailUrl,
                backupCode = entry.code
            )
        }
        if (fetched != null) {
            graphEntrySessionBitmaps[entry.code] = fetched
        }
        value = fetched to true
    }

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val imageBitmap = thumbnailState.first
        val loadFinished = thumbnailState.second
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = entry.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (!loadFinished) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 1.75.dp
            )
        } else {
            Text(
                text = "No preview",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun ThumbnailPreviewDialog(
    thumbnailUrl: String,
    contentDescription: String,
    obscure: Boolean,
    onOpenInBrowser: () -> Unit,
    onDismiss: () -> Unit
) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.86f)
        .coerceIn(260.dp, 860.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThumbnailImage(
                        thumbnailUrl = thumbnailUrl,
                        contentDescription = contentDescription,
                        obscure = obscure,
                        onClick = onOpenInBrowser,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = maxHeight)
                    )
                }
            }
        }
    }
}

internal fun Modifier.heldSelectionMask(
    enabled: Boolean,
    overlayColor: Color
): Modifier {
    if (!enabled) return this
    return this.drawWithContent {
        drawContent()
        drawRect(color = overlayColor.copy(alpha = 0.22f))
    }
}
