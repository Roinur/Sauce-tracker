package com.roinur.saucetracker.feature.heatmap

import androidx.compose.ui.graphics.ImageBitmap
import com.roinur.saucetracker.TagGraphEntryNode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.sqrt

internal fun centeredThumbnailZoneAxisFraction(percent: Int): Float =
    sqrt(percent.coerceIn(1, 100) / 100f)

internal fun isInsideCenteredThumbnailZone(
    x: Float,
    y: Float,
    left: Float,
    right: Float,
    top: Float,
    bottom: Float,
    percent: Int
): Boolean {
    val fraction = centeredThumbnailZoneAxisFraction(percent)
    val centerX = (left + right) * 0.5f
    val centerY = (top + bottom) * 0.5f
    val halfWidth = ((right - left).coerceAtLeast(0f) * fraction) * 0.5f
    val halfHeight = ((bottom - top).coerceAtLeast(0f) * fraction) * 0.5f
    return x in (centerX - halfWidth)..(centerX + halfWidth) &&
        y in (centerY - halfHeight)..(centerY + halfHeight)
}

internal object HeatmapThumbnailLoader {
    suspend fun load(
        entries: List<TagGraphEntryNode>,
        parallelism: Int,
        fetch: suspend (TagGraphEntryNode) -> ImageBitmap?
    ): Map<Int, ImageBitmap> {
        if (entries.isEmpty()) return emptyMap()
        val ordered = entries.sortedWith(
            compareByDescending<TagGraphEntryNode> { it.isRead }
                .thenByDescending { it.pinned }
                .thenBy { it.code }
        )
        val resolved = LinkedHashMap<Int, ImageBitmap>()
        ordered.chunked(parallelism.coerceIn(1, 8)).forEach { chunk ->
            val batch = coroutineScope {
                chunk.map { entry ->
                    async { entry.code to fetch(entry) }
                }.awaitAll()
            }
            batch.forEach { (code, bitmap) -> if (bitmap != null) resolved[code] = bitmap }
        }
        return resolved.toMap()
    }
}
