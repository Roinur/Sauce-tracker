package com.roinur.saucetracker.feature.library.entries

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roinur.saucetracker.READ_STATE_COLOR
import com.roinur.saucetracker.UNREAD_STATE_COLOR
import com.roinur.saucetracker.core.ui.components.*
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun EntrySwipeDismissContainer(
    code: Int,
    isPinned: Boolean,
    isRead: Boolean,
    incognitoModeEnabled: Boolean,
    onTogglePinned: (Int) -> Unit,
    onToggleRead: (Int) -> Unit,
    backgroundShape: Shape? = null,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val haptic: HapticFeedback = LocalHapticFeedback.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val swipeCommitTracker = rememberSwipeCommitTrackerState(
        key = code,
        config = SwipeCommitConfig(
            minHorizontalSwipePx = with(density) { 88.dp.toPx() },
            minGestureDurationMs = 160L,
            maxVerticalPerHorizontalRatio = 0.176327f,
            maxSwipeSpeedPxPerMs = 1.15f
        )
    )
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * 0.45f },
        confirmValueChange = { target ->
            if (target != SwipeToDismissBoxValue.Settled && !swipeCommitTracker.deliberate) {
                return@rememberSwipeToDismissBoxState false
            }
            when (target) {
                SwipeToDismissBoxValue.StartToEnd -> onTogglePinned(code)
                SwipeToDismissBoxValue.EndToStart -> onToggleRead(code)
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        }
    )
    val commitReadyTarget = trackSwipeCommitFeedback(
        key = code,
        tracker = swipeCommitTracker,
        dismissTarget = dismissState.targetValue,
        feedbackEnabled = !incognitoModeEnabled,
        haptic = haptic
    )
    val direction = dismissState.dismissDirection
    val progress = dismissState.progress
    // Material3 reports progress == 1f when the state is fully settled too.
    // Using progress alone therefore leaves the swipe background visible behind
    // rounded cards at rest, where its different corner radius leaks through.
    val swipeVisualActive = direction != SwipeToDismissBoxValue.Settled
    var swipeSnapshotPinned by remember(code) { mutableStateOf(isPinned) }
    var swipeSnapshotRead by remember(code) { mutableStateOf(isRead) }
    var swipeSnapshotCaptured by remember(code) { mutableStateOf(false) }
    LaunchedEffect(swipeCommitTracker.gestureActive, isPinned, isRead) {
        if (swipeCommitTracker.gestureActive && !swipeSnapshotCaptured) {
            swipeSnapshotPinned = isPinned
            swipeSnapshotRead = isRead
            swipeSnapshotCaptured = true
        } else if (!swipeCommitTracker.gestureActive) {
            swipeSnapshotCaptured = false
        }
    }
    val visualDirection = if (swipeVisualActive && direction != SwipeToDismissBoxValue.Settled) {
        direction
    } else {
        SwipeToDismissBoxValue.Settled
    }
    val visualPinnedState = if (swipeCommitTracker.gestureActive && swipeSnapshotCaptured) swipeSnapshotPinned else isPinned
    val visualReadState = if (swipeCommitTracker.gestureActive && swipeSnapshotCaptured) swipeSnapshotRead else isRead
    val backgroundSpec = when (visualDirection) {
        SwipeToDismissBoxValue.StartToEnd -> SwipeBackgroundSpec(
            label = if (visualPinnedState) "Unpin" else "Pin",
            glyph = "\uD83D\uDCCC",
            tint = if (visualPinnedState) UNREAD_STATE_COLOR else READ_STATE_COLOR
        )
        SwipeToDismissBoxValue.EndToStart -> SwipeBackgroundSpec(
            label = if (visualReadState) "Unread" else "Read",
            glyph = if (visualReadState) "○" else "✓",
            tint = if (visualReadState) UNREAD_STATE_COLOR else READ_STATE_COLOR
        )
        SwipeToDismissBoxValue.Settled -> SwipeBackgroundSpec("", "", MaterialTheme.colorScheme.onSurfaceVariant)
    }
    val backgroundAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (!swipeVisualActive) 0f else (0.42f + (progress * 0.58f)).coerceIn(0f, 1f),
        label = "entrySwipeAlpha"
    )
    val contentScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 180),
        label = "entrySwipeContentScale"
    )
    val contentAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = 1f - (progress * 0.035f),
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 180),
        label = "entrySwipeContentAlpha"
    )
    val backgroundColor by animateColorAsState(
        targetValue = when (visualDirection) {
            SwipeToDismissBoxValue.StartToEnd -> if (visualPinnedState) UNREAD_STATE_COLOR else MaterialTheme.colorScheme.primaryContainer
            SwipeToDismissBoxValue.EndToStart -> if (visualReadState) UNREAD_STATE_COLOR else MaterialTheme.colorScheme.primaryContainer
            SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "entrySwipeColor"
    )
    val resolvedBackgroundShape = backgroundShape ?: MaterialTheme.shapes.medium

    SwipeToDismissBox(
        modifier = modifier.trackSwipeCommitGestures(gestureKey = code, tracker = swipeCommitTracker),
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(resolvedBackgroundShape)
                    .background(backgroundColor.copy(alpha = backgroundAlpha))
            ) {
                SwipeCommitReadySwoosh(
                    commitReadyTarget = commitReadyTarget,
                    tint = backgroundSpec.tint,
                    modifier = Modifier.matchParentSize()
                )
                if (backgroundSpec.label.isNotBlank()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (incognitoModeEnabled) "\u26D4" else backgroundSpec.glyph,
                            style = MaterialTheme.typography.titleMedium,
                            color = backgroundSpec.tint
                        )
                        Text(
                            text = if (incognitoModeEnabled) "Blocked" else backgroundSpec.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = backgroundSpec.tint,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        content = swipeContent@{
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = contentScale
                        scaleY = contentScale
                        alpha = contentAlpha
                    }
            ) {
                this@swipeContent.content()
            }
        }
    )
}

private data class SwipeBackgroundSpec(
    val label: String,
    val glyph: String,
    val tint: Color
)
