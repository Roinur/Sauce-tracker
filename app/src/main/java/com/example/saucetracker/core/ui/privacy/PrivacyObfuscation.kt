package com.example.saucetracker.core.ui.privacy

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.saucetracker.core.diagnostics.GitHubMediaSession

internal fun Modifier.privacyObfuscate(
    enabled: Boolean,
    overlayColor: Color,
    blurRadius: Dp = 7.dp,
    expandHorizontal: Dp = 2.dp,
    expandVertical: Dp = 1.dp,
    cornerRadius: Dp = 12.dp
): Modifier {
    if (!GitHubMediaSession.shouldApplyPrivacyMask(enabled)) return this
    val strengthenForGitHub = GitHubMediaSession.shouldStrengthenPrivacyMask()
    val effectiveBlurRadius = if (strengthenForGitHub) maxOf(blurRadius, 29.dp) else blurRadius
    val effectiveOverlayColor = if (strengthenForGitHub) {
        overlayColor.copy(alpha = maxOf(overlayColor.alpha, 0.98f))
    } else {
        overlayColor
    }
    val shape = RoundedCornerShape(cornerRadius)
    return padding(horizontal = expandHorizontal, vertical = expandVertical)
        .clearAndSetSemantics { }
        .clip(shape)
        .blur(effectiveBlurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        .drawWithContent {
            drawContent()
            val radius = cornerRadius.toPx()
            drawRoundRect(color = effectiveOverlayColor, cornerRadius = CornerRadius(radius, radius))
            drawRoundRect(
                color = effectiveOverlayColor.copy(
                    alpha = (effectiveOverlayColor.alpha + 0.16f).coerceAtMost(1f)
                ),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        .clip(shape)
}
