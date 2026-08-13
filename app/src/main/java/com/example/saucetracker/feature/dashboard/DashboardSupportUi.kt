package com.example.saucetracker

import com.example.saucetracker.data.backup.*
import com.example.saucetracker.data.downloads.*
import com.example.saucetracker.core.ui.components.*
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.key
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
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
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun EntryDetailSkeletonLines(
    compactContent: Boolean,
    alpha: Float = 0.44f
) {
    val lineColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha)
    val heights = if (compactContent) listOf(9.dp, 9.dp, 9.dp) else listOf(11.dp, 11.dp, 11.dp, 11.dp)
    val widths = listOf(0.92f, 0.72f, 0.84f, 0.58f)
    Column(verticalArrangement = Arrangement.spacedBy(if (compactContent) 5.dp else 7.dp)) {
        heights.forEachIndexed { index, height ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(widths.getOrElse(index) { 0.7f })
                    .height(height)
                    .clip(RoundedCornerShape(999.dp))
                    .background(lineColor)
            )
        }
    }
}

internal object ThumbnailBitmapCache {
    private val maxBytes = (Runtime.getRuntime().maxMemory() / 16L)
        .coerceIn(12L * 1024L * 1024L, 32L * 1024L * 1024L)
    private val cache = BitmapMemoryCache<String, ImageBitmap>(
        maximumBytes = maxBytes,
        sizeOf = { bitmap ->
            bitmap.width.toLong().coerceAtLeast(1L) *
                bitmap.height.toLong().coerceAtLeast(1L) * 4L
        }
    )

    private fun key(url: String, lowRes: Boolean): String {
        return if (lowRes) "low:$url" else "full:$url"
    }

    fun get(url: String, lowRes: Boolean = false): ImageBitmap? {
        if (url.isBlank()) return null
        val bitmap = cache[key(url, lowRes)]
        if (bitmap != null) {
            PerformanceMetrics.recordThumbnailCacheHit()
        } else {
            PerformanceMetrics.recordThumbnailCacheMiss()
        }
        return bitmap
    }

    fun put(url: String, bitmap: ImageBitmap, lowRes: Boolean = false) {
        if (url.isBlank()) return
        cache.put(key(url, lowRes), bitmap)
    }

    fun clear() {
        cache.clear()
    }
}

@Composable
fun PerformanceOverlay(enabled: Boolean) {
    if (!enabled) return

    var fps by remember { mutableStateOf(0) }
    var thumbnailLoadsPerSecond by remember { mutableStateOf(0L) }
    var duplicateChecksPerSecond by remember { mutableStateOf(0L) }
    var cacheHitRate by remember { mutableStateOf(PerformanceMetrics.thumbnailCacheHitRatePercent()) }

    LaunchedEffect(enabled) {
        var frames = 0
        var lastSampleNanos = withFrameNanos { it }
        var lastThumbnailLoads = PerformanceMetrics.thumbnailLoadsCompleted
        var lastDuplicateChecks = PerformanceMetrics.duplicateChecksStarted
        while (true) {
            val now = withFrameNanos { it }
            frames += 1
            if (now - lastSampleNanos >= 1_000_000_000L) {
                val currentThumbnailLoads = PerformanceMetrics.thumbnailLoadsCompleted
                val currentDuplicateChecks = PerformanceMetrics.duplicateChecksStarted
                fps = frames
                thumbnailLoadsPerSecond = currentThumbnailLoads - lastThumbnailLoads
                duplicateChecksPerSecond = currentDuplicateChecks - lastDuplicateChecks
                cacheHitRate = PerformanceMetrics.thumbnailCacheHitRatePercent()
                frames = 0
                lastSampleNanos = now
                lastThumbnailLoads = currentThumbnailLoads
                lastDuplicateChecks = currentDuplicateChecks
            }
        }
    }

    Popup(
        alignment = Alignment.TopStart,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.72f),
            contentColor = Color.White,
            shape = RoundedCornerShape(10.dp),
            shadowElevation = 4.dp,
            modifier = Modifier.padding(start = 10.dp, top = 34.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text("FPS $fps", style = MaterialTheme.typography.labelSmall)
                Text("Thumb/s $thumbnailLoadsPerSecond", style = MaterialTheme.typography.labelSmall)
                Text("Dup/s $duplicateChecksPerSecond", style = MaterialTheme.typography.labelSmall)
                Text("Cache $cacheHitRate%", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun CreatorJumpRow(
    label: String,
    creatorType: String,
    creatorName: String,
    onOpenCreator: (String, String) -> Unit,
    onOpenCreatorInBrowser: (String, String) -> Unit,
    incognitoModeEnabled: Boolean
) {
    val creatorInteraction = remember { MutableInteractionSource() }
    val privacyOverlay = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$label:",
            modifier = Modifier.privacyObfuscate(
                enabled = incognitoModeEnabled,
                overlayColor = privacyOverlay
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .combinedClickable(
                    interactionSource = creatorInteraction,
                    indication = rememberRipple(
                        bounded = true,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                    ),
                    enabled = !incognitoModeEnabled,
                    onClick = { onOpenCreator(creatorType, creatorName) },
                    onLongClick = { onOpenCreatorInBrowser(creatorType, creatorName) }
                )
                .heightIn(min = 32.dp)
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .privacyObfuscate(
                    enabled = incognitoModeEnabled,
                    overlayColor = privacyOverlay
                )
        ) {
            Text(
                text = creatorName,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
internal fun AnimatedOverlayCard(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 640.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    border: BorderStroke? = BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
    ),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    coverSystemBars: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val dismissInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }
    var entered by remember { mutableStateOf(false) }
    val progress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "overlayCardProgress"
    )
    val scrimAlpha = 0.68f * progress
    val cardScale = 0.96f + (0.04f * progress)

    LaunchedEffect(Unit) {
        entered = true
    }

    val overlayContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = dismissInteraction,
                    indication = null,
                    onClick = onDismissRequest
                )
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .widthIn(max = maxWidth)
                    .graphicsLayer {
                        alpha = progress
                        scaleX = cardScale
                        scaleY = cardScale
                    }
                    .clickable(
                        interactionSource = cardInteraction,
                        indication = null,
                        onClick = {}
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = containerColor
                ),
                border = border
            ) {
                Column(
                    modifier = Modifier.padding(contentPadding),
                    verticalArrangement = verticalArrangement,
                    content = content
                )
            }
        }
    }
    if (coverSystemBars) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            overlayContent()
        }
    } else {
        Popup(
            alignment = Alignment.Center,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                clippingEnabled = false
            )
        ) {
            overlayContent()
        }
    }
}
