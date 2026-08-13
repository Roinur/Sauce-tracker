@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.saucetracker.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

internal data class SwipeCommitConfig(
    val minHorizontalSwipePx: Float,
    val minGestureDurationMs: Long = 160L,
    val maxVerticalPerHorizontalRatio: Float,
    val maxSwipeSpeedPxPerMs: Float = 1.15f
)

internal val RATING_STAR_GOLD = Color(0xFFF6C453)
internal val RATING_STAR_MUTED = Color(0xFF98A2B3)

internal fun buildRatingSummaryAnnotatedString(rating: Int): AnnotatedString {
    val safe = rating.coerceIn(0, 5)
    return buildAnnotatedString {
        append("Rating: ")
        val starStart = length
        repeat(5) { index ->
            append(if (index < safe) '★' else '☆')
        }
        addStyle(
            style = SpanStyle(color = RATING_STAR_GOLD),
            start = starStart,
            end = length
        )
        append(" ($safe/5)")
    }
}

@Stable
internal class SwipeCommitTrackerState(
    private val config: SwipeCommitConfig
) {
    var gestureActive by mutableStateOf(false)
        private set
    var durationMs by mutableStateOf(0L)
        private set
    var absDx by mutableStateOf(0f)
        private set
    var absDy by mutableStateOf(0f)
        private set

    val deliberate: Boolean
        get() {
            val swipeSpeedPxPerMs = if (durationMs <= 0L) {
                Float.POSITIVE_INFINITY
            } else {
                absDx / durationMs.toFloat()
            }
            val angleWithinLimit = absDy <= (absDx * config.maxVerticalPerHorizontalRatio)
            return absDx >= config.minHorizontalSwipePx &&
                angleWithinLimit &&
                durationMs >= config.minGestureDurationMs &&
                swipeSpeedPxPerMs <= config.maxSwipeSpeedPxPerMs
        }

    fun onGestureStart() {
        gestureActive = true
        durationMs = 0L
        absDx = 0f
        absDy = 0f
    }

    fun onGestureSample(startUptime: Long, eventUptime: Long, totalDx: Float, totalDy: Float) {
        durationMs = (eventUptime - startUptime).coerceAtLeast(0L)
        absDx = abs(totalDx)
        absDy = abs(totalDy)
    }

    fun advanceDuration(stepMs: Long) {
        durationMs = (durationMs + stepMs).coerceAtLeast(0L)
    }

    fun onGestureEnd(startUptime: Long, endUptime: Long, totalDx: Float, totalDy: Float) {
        gestureActive = false
        durationMs = (endUptime - startUptime).coerceAtLeast(0L)
        absDx = abs(totalDx)
        absDy = abs(totalDy)
    }

    fun commitReadyTarget(
        dismissTarget: SwipeToDismissBoxValue,
        feedbackEnabled: Boolean
    ): SwipeToDismissBoxValue {
        return if (
            feedbackEnabled &&
            dismissTarget != SwipeToDismissBoxValue.Settled &&
            deliberate
        ) {
            dismissTarget
        } else {
            SwipeToDismissBoxValue.Settled
        }
    }
}

@Composable
internal fun rememberSwipeCommitTrackerState(
    key: Any,
    config: SwipeCommitConfig
): SwipeCommitTrackerState {
    return remember(
        key,
        config.minHorizontalSwipePx,
        config.minGestureDurationMs,
        config.maxVerticalPerHorizontalRatio,
        config.maxSwipeSpeedPxPerMs
    ) {
        SwipeCommitTrackerState(config)
    }
}

@Composable
internal fun trackSwipeCommitFeedback(
    key: Any,
    tracker: SwipeCommitTrackerState,
    dismissTarget: SwipeToDismissBoxValue,
    feedbackEnabled: Boolean,
    haptic: HapticFeedback
): SwipeToDismissBoxValue {
    LaunchedEffect(key, tracker.gestureActive) {
        if (!tracker.gestureActive) return@LaunchedEffect
        while (tracker.gestureActive) {
            tracker.advanceDuration(16L)
            delay(16L)
        }
    }

    val commitReadyTarget = tracker.commitReadyTarget(
        dismissTarget = dismissTarget,
        feedbackEnabled = feedbackEnabled
    )
    var feedbackTarget by remember(key) { mutableStateOf(SwipeToDismissBoxValue.Settled) }
    LaunchedEffect(key, commitReadyTarget) {
        if (commitReadyTarget == SwipeToDismissBoxValue.Settled) {
            feedbackTarget = SwipeToDismissBoxValue.Settled
        } else if (commitReadyTarget != feedbackTarget) {
            feedbackTarget = commitReadyTarget
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    return commitReadyTarget
}

internal fun Modifier.trackSwipeCommitGestures(
    gestureKey: Any,
    tracker: SwipeCommitTrackerState
): Modifier = pointerInput(gestureKey, tracker) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val startUptime = down.uptimeMillis
        tracker.onGestureStart()
        var endUptime = startUptime
        var totalDx = 0f
        var totalDy = 0f
        var lastX = down.position.x
        var lastY = down.position.y
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            val dx = change.position.x - lastX
            val dy = change.position.y - lastY
            totalDx += dx
            totalDy += dy
            lastX = change.position.x
            lastY = change.position.y
            endUptime = change.uptimeMillis
            tracker.onGestureSample(startUptime, endUptime, totalDx, totalDy)
            if (!change.pressed) break
        }
        tracker.onGestureEnd(startUptime, endUptime, totalDx, totalDy)
    }
}

@Composable
internal fun SwipeCommitReadySwoosh(
    commitReadyTarget: SwipeToDismissBoxValue,
    tint: Color,
    modifier: Modifier = Modifier
) {
    var displayTarget by remember { mutableStateOf(SwipeToDismissBoxValue.Settled) }
    LaunchedEffect(commitReadyTarget) {
        if (commitReadyTarget != SwipeToDismissBoxValue.Settled) {
            displayTarget = commitReadyTarget
        } else {
            delay(180L)
            if (commitReadyTarget == SwipeToDismissBoxValue.Settled) {
                displayTarget = SwipeToDismissBoxValue.Settled
            }
        }
    }
    val visibilityAlpha by animateFloatAsState(
        targetValue = if (commitReadyTarget == SwipeToDismissBoxValue.Settled) 0f else 1f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "swipeCommitSwooshVisibility"
    )
    if (displayTarget == SwipeToDismissBoxValue.Settled && visibilityAlpha <= 0.001f) return

    val progress = rememberSynchronizedLoopProgress(
        active = commitReadyTarget != SwipeToDismissBoxValue.Settled || visibilityAlpha > 0.001f,
        durationMillis = 720
    )
    val stripeEnvelope = sin(progress * PI).toFloat().coerceAtLeast(0f)
    val activeTarget = if (commitReadyTarget != SwipeToDismissBoxValue.Settled) {
        commitReadyTarget
    } else {
        displayTarget
    }
    Canvas(modifier = modifier) {
        drawRect(tint.copy(alpha = 0.16f * visibilityAlpha))

        val stripeHalfWidth = size.width * 0.18f
        val travel = size.width + (stripeHalfWidth * 2f)
        val centerX = if (activeTarget == SwipeToDismissBoxValue.StartToEnd) {
            -stripeHalfWidth + (travel * progress)
        } else {
            size.width + stripeHalfWidth - (travel * progress)
        }
        val topLeft = Offset(centerX - stripeHalfWidth, -size.height * 0.18f)
        val stripeSize = Size(stripeHalfWidth * 2f, size.height * 1.36f)
        rotate(
            degrees = if (activeTarget == SwipeToDismissBoxValue.StartToEnd) -12f else 12f,
            pivot = Offset(centerX, size.height / 2f)
        ) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        tint.copy(alpha = 0.78f * visibilityAlpha * stripeEnvelope),
                        Color.Transparent
                    ),
                    startX = topLeft.x,
                    endX = topLeft.x + stripeSize.width
                ),
                topLeft = topLeft,
                size = stripeSize
            )
        }
    }
}

@Composable
private fun rememberTriggeredPulse(triggerKey: Any?): Float {
    val pulse = remember { Animatable(0f) }
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(triggerKey) {
        if (!initialized) {
            initialized = true
            return@LaunchedEffect
        }
        pulse.snapTo(1f)
        pulse.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 820,
                easing = FastOutSlowInEasing
            )
        )
    }
    return pulse.value
}

@Composable
internal fun AccentPulseOverlay(
    triggerKey: Any?,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val pulse = rememberTriggeredPulse(triggerKey = triggerKey)
    if (pulse <= 0.001f) return

    Canvas(modifier = modifier) {
        val baseAlpha = 0.22f * pulse
        drawRect(tint.copy(alpha = baseAlpha))

        val glowWidth = size.width * (0.24f + ((1f - pulse) * 0.48f))
        val centerX = (size.width * 0.2f) + ((1f - pulse) * size.width * 0.65f)
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    tint.copy(alpha = 0.32f * pulse),
                    Color.Transparent
                ),
                startX = centerX - glowWidth,
                endX = centerX + glowWidth
            ),
            topLeft = Offset.Zero,
            size = size
        )
    }
}

@Composable
internal fun AttentionBorderSweep(
    active: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val visibilityAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "attentionBorderSweepVisibility"
    )
    if (!active && visibilityAlpha <= 0.001f) return

    val progress = rememberSynchronizedLoopProgress(
        active = active || visibilityAlpha > 0.001f,
        durationMillis = 1200
    )
    val stripeEnvelope = sin(progress * PI).toFloat().coerceAtLeast(0f)
    Canvas(modifier = modifier) {
        val strokeWidth = 1.5.dp.toPx()
        drawRect(
            color = tint.copy(alpha = 0.36f * visibilityAlpha),
            style = Stroke(width = strokeWidth)
        )

        val stripeHalfWidth = size.width * 0.12f
        val centerX = -stripeHalfWidth + ((size.width + (stripeHalfWidth * 2f)) * progress)
        rotate(degrees = -10f, pivot = Offset(centerX, size.height / 2f)) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        tint.copy(alpha = 0.78f * visibilityAlpha * stripeEnvelope),
                        Color.Transparent
                    ),
                    startX = centerX - stripeHalfWidth,
                    endX = centerX + stripeHalfWidth
                ),
                topLeft = Offset(centerX - stripeHalfWidth, -size.height * 0.2f),
                size = Size(stripeHalfWidth * 2f, size.height * 1.4f)
            )
        }
    }
}

@Composable
internal fun LoadingShimmerOverlay(
    active: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val visibilityAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "loadingShimmerVisibility"
    )
    if (!active && visibilityAlpha <= 0.001f) return

    val progress = rememberSynchronizedLoopProgress(
        active = active || visibilityAlpha > 0.001f,
        durationMillis = 950
    )
    val stripeEnvelope = sin(progress * PI).toFloat().coerceAtLeast(0f)
    Canvas(modifier = modifier) {
        drawRect(tint.copy(alpha = 0.12f * visibilityAlpha))

        val stripeHalfWidth = size.width * 0.2f
        val centerX = -stripeHalfWidth + ((size.width + (stripeHalfWidth * 2f)) * progress)
        rotate(degrees = -14f, pivot = Offset(centerX, size.height / 2f)) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        tint.copy(alpha = 0.45f * visibilityAlpha * stripeEnvelope),
                        tint.copy(alpha = 0.18f * visibilityAlpha * stripeEnvelope),
                        Color.Transparent
                    ),
                    startX = centerX - stripeHalfWidth,
                    endX = centerX + stripeHalfWidth
                ),
                topLeft = Offset(centerX - stripeHalfWidth, -size.height * 0.18f),
                size = Size(stripeHalfWidth * 2f, size.height * 1.36f)
            )
        }
    }
}

@Composable
internal fun TriggeredShimmerOverlay(
    triggerKey: Any?,
    tint: Color,
    modifier: Modifier = Modifier,
    holdDurationMs: Long = 780L
) {
    var active by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(triggerKey) {
        if (!initialized) {
            initialized = true
            return@LaunchedEffect
        }
        active = true
        delay(holdDurationMs)
        if (triggerKey != null) {
            active = false
        }
    }
    LoadingShimmerOverlay(
        active = active,
        tint = tint,
        modifier = modifier
    )
}

@Composable
internal fun BreathingSelectionOverlay(
    active: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val visibilityAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "breathingSelectionVisibility"
    )
    if (!active && visibilityAlpha <= 0.001f) return

    val transition = rememberInfiniteTransition(label = "breathingSelection")
    val alpha by transition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingSelectionAlpha"
    )
    Canvas(modifier = modifier) {
        drawRect(tint.copy(alpha = alpha * visibilityAlpha))
    }
}

@Composable
internal fun PinnedCornerBleedGlow(
    visible: Boolean,
    tint: Color,
    cornerRadius: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val visibilityAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "pinnedCornerGlowVisibility"
    )
    if (!visible && visibilityAlpha <= 0.001f) return

    Canvas(modifier = modifier) {
        val cornerCenter = Offset(size.width * 1.04f, -size.height * 0.05f)
        val radius = size.minDimension * 0.95f
        val drawCornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    tint.copy(alpha = 0.26f * visibilityAlpha),
                    tint.copy(alpha = 0.12f * visibilityAlpha),
                    Color.Transparent
                ),
                center = cornerCenter,
                radius = radius
            ),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = drawCornerRadius
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    tint.copy(alpha = 0.11f * visibilityAlpha),
                    Color.Transparent
                ),
                start = Offset(size.width * 0.52f, 0f),
                end = Offset(size.width, size.height * 0.58f)
            ),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = drawCornerRadius
        )
    }
}

@Composable
internal fun SelectedCardEdgeGlow(
    active: Boolean,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val visibilityAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "selectedCardEdgeGlowVisibility"
    )
    if (!active && visibilityAlpha <= 0.001f) return

    Canvas(modifier = modifier) {
        val strokeWidth = 1.25.dp.toPx()
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    tint.copy(alpha = 0.62f * visibilityAlpha),
                    tint.copy(alpha = 0.24f * visibilityAlpha),
                    Color.Transparent
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height * 0.86f)
            ),
            style = Stroke(width = strokeWidth)
        )
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    tint.copy(alpha = 0.18f * visibilityAlpha),
                    Color.Transparent
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width * 0.55f, size.height * 0.42f)
            ),
            topLeft = Offset.Zero,
            size = size
        )
    }
}

@Composable
private fun rememberSynchronizedLoopProgress(
    active: Boolean,
    durationMillis: Int
): Float {
    if (!active) return 0f
    val durationNanos = durationMillis.coerceAtLeast(1).toLong() * 1_000_000L
    val progress by produceState(initialValue = 0f, active, durationMillis) {
        while (active) {
            withFrameNanos { frameTimeNanos ->
                val normalizedNanos = frameTimeNanos.mod(durationNanos)
                value = normalizedNanos.toFloat() / durationNanos.toFloat()
            }
        }
    }
    return progress
}
