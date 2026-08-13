package com.example.saucetracker

import com.example.saucetracker.data.backup.*
import com.example.saucetracker.data.downloads.*
import com.example.saucetracker.core.ui.components.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
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
import kotlin.math.cos
import kotlin.math.sin

private const val DRAG_RATING_ACTIVE_WIDTH_FRACTION = 0.68f

@Composable
internal fun FractionalRatingStars(
    rating: Float,
    starSize: Dp,
    modifier: Modifier = Modifier
) {
    val safe = rating.coerceIn(0f, 5f)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (index in 0 until 5) {
            val fill = (safe - index).coerceIn(0f, 1f)
            FractionalRatingStar(fill = fill, starSize = starSize)
        }
    }
}

@Composable
internal fun RatingSummaryLine(
    rating: Float,
    modifier: Modifier = Modifier,
    starSize: Dp = 12.dp,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = "Rating:",
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
        FractionalRatingStars(
            rating = rating,
            starSize = starSize
        )
        Text(
            text = "(${formatRatingValue(rating)}/5)",
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
    }
}

@Composable
internal fun FractionalRatingStar(
    fill: Float,
    starSize: Dp,
    modifier: Modifier = Modifier
) {
    val safeFill = fill.coerceIn(0f, 1f)
    Canvas(modifier = modifier.size(starSize)) {
        val star = Path()
        val outerRadius = size.minDimension * 0.48f
        val innerRadius = outerRadius * 0.46f
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        repeat(10) { point ->
            val angle = (-90.0 + point * 36.0) * Math.PI / 180.0
            val radius = if (point % 2 == 0) outerRadius else innerRadius
            val x = centerX + cos(angle).toFloat() * radius
            val y = centerY + sin(angle).toFloat() * radius
            if (point == 0) {
                star.moveTo(x, y)
            } else {
                star.lineTo(x, y)
            }
        }
        star.close()
        drawPath(star, RATING_STAR_MUTED)
        clipRect(right = size.width * safeFill) {
            drawPath(star, RATING_STAR_GOLD)
        }
    }
}

internal fun mapDragPositionToRating(localX: Float, rowWidthPx: Float): Int {
    val safeWidth = rowWidthPx.coerceAtLeast(1f)
    val activeWidth = (safeWidth * DRAG_RATING_ACTIVE_WIDTH_FRACTION).coerceAtLeast(1f)
    val startX = ((safeWidth - activeWidth) / 2f).coerceAtLeast(0f)
    val normalized = ((localX - startX) / activeWidth).coerceIn(0f, 1f)
    return (normalized * 6f).toInt().coerceIn(0, 5)
}

internal fun mapAbsoluteDragPositionToRating(
    absoluteX: Float,
    screenWidthPx: Float,
    popupWidthPx: Float
): Int {
    val safeScreenWidth = screenWidthPx.coerceAtLeast(1f)
    val safePopupWidth = popupWidthPx.coerceIn(1f, safeScreenWidth)
    val popupStartX = ((safeScreenWidth - safePopupWidth) / 2f).coerceAtLeast(0f)
    return mapDragPositionToRating(localX = absoluteX - popupStartX, rowWidthPx = safePopupWidth)
}

internal fun mapDragPositionToSuggestedAction(localX: Float, rowWidthPx: Float): SuggestedDragAction {
    val safeWidth = rowWidthPx.coerceAtLeast(1f)
    val activeWidth = (safeWidth * DRAG_RATING_ACTIVE_WIDTH_FRACTION).coerceAtLeast(1f)
    val startX = ((safeWidth - activeWidth) / 2f).coerceAtLeast(0f)
    val normalized = ((localX - startX) / activeWidth).coerceIn(0f, 1f)
    return if (normalized >= 0.5f) SuggestedDragAction.HIDE else SuggestedDragAction.CANCEL
}
