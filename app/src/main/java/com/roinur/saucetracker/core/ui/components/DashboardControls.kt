package com.roinur.saucetracker

import com.roinur.saucetracker.data.backup.*
import com.roinur.saucetracker.data.downloads.*
import com.roinur.saucetracker.core.ui.theme.AccentMode
import com.roinur.saucetracker.core.ui.components.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import com.roinur.saucetracker.core.media.*
import com.roinur.saucetracker.feature.library.creators.*
import com.roinur.saucetracker.feature.library.detail.*
import com.roinur.saucetracker.feature.library.history.*
import com.roinur.saucetracker.feature.library.tags.*
import com.roinur.saucetracker.feature.settings.*
import com.roinur.saucetracker.feature.subscriptions.*
import com.roinur.saucetracker.feature.suggestions.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun ImmediateActionText(
    label: String,
    onAction: () -> Unit,
    enabled: Boolean = true,
    onPressStart: () -> Unit = {},
    runOnPressWhen: () -> Boolean = { false },
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.labelLarge,
    fontWeight: FontWeight? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
) {
    var firedOnPress by remember { mutableStateOf(false) }
    val actionScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    TextButton(
        onClick = {
            if (!enabled) return@TextButton
            if (firedOnPress) {
                firedOnPress = false
            } else {
                onAction()
            }
        },
        enabled = enabled,
        interactionSource = interactionSource,
        contentPadding = contentPadding,
        modifier = modifier.pointerInput(enabled, onPressStart, runOnPressWhen) {
            awaitEachGesture {
                if (!enabled) return@awaitEachGesture
                firedOnPress = false
                val down = awaitFirstDown(requireUnconsumed = false)
                val shouldRunOnPress = runOnPressWhen()
                onPressStart()

                if (shouldRunOnPress) {
                    firedOnPress = true
                    val press = PressInteraction.Press(down.position)
                    actionScope.launch {
                        interactionSource.emit(press)
                    }
                    actionScope.launch {
                        // Keep a short delay so the press animation is visible.
                        delay(32)
                        onAction()
                    }
                    val up = waitForUpOrCancellation()
                    actionScope.launch {
                        if (up == null) {
                            interactionSource.emit(PressInteraction.Cancel(press))
                        } else {
                            interactionSource.emit(PressInteraction.Release(press))
                        }
                    }
                } else {
                    waitForUpOrCancellation()
                }
            }
        }
    ) {
        Text(
            text = label,
            style = textStyle,
            fontWeight = fontWeight
        )
    }
}

@Composable
internal fun EntrySortChip(
    label: String,
    selected: Boolean,
    activeDirection: SortDirection? = null,
    onClick: () -> Unit,
    onPressStart: () -> Unit = {},
    runOnPressWhen: () -> Boolean = { false },
    modifier: Modifier = Modifier
) {
    var firedOnPress by remember { mutableStateOf(false) }
    val actionScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val descAccent = Color(0xFF22C55E)
    val ascAccent = Color(0xFFEF4444)
    val selectedAccent = when (activeDirection) {
        SortDirection.DESC -> descAccent
        SortDirection.ASC -> ascAccent
        null -> MaterialTheme.colorScheme.primary
    }
    val selectedContainer = selectedAccent.copy(alpha = 0.20f)
    val inactiveBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val inactiveLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f)
    val chipBorder = if (selected) {
        BorderStroke(1.3.dp, selectedAccent.copy(alpha = 0.90f))
    } else {
        BorderStroke(1.2.dp, inactiveBorderColor)
    }
    FilterChip(
        modifier = modifier.pointerInput(onPressStart, runOnPressWhen) {
            awaitEachGesture {
                firedOnPress = false
                val down = awaitFirstDown(requireUnconsumed = false)
                val shouldRunOnPress = runOnPressWhen()
                onPressStart()

                if (shouldRunOnPress) {
                    firedOnPress = true
                    val press = PressInteraction.Press(down.position)
                    actionScope.launch {
                        interactionSource.emit(press)
                    }
                    actionScope.launch {
                        delay(32)
                        onClick()
                    }
                    val up = waitForUpOrCancellation()
                    actionScope.launch {
                        if (up == null) {
                            interactionSource.emit(PressInteraction.Cancel(press))
                        } else {
                            interactionSource.emit(PressInteraction.Release(press))
                        }
                    }
                } else {
                    waitForUpOrCancellation()
                }
            }
        },
        selected = selected,
        onClick = {
            if (firedOnPress) {
                firedOnPress = false
            } else {
                onClick()
            }
        },
        interactionSource = interactionSource,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = inactiveLabelColor,
            selectedContainerColor = selectedContainer,
            selectedLabelColor = selectedAccent
        ),
        border = chipBorder,
        label = { Text(label) }
    )
}

@Composable
internal fun ThemeToggleWithAccentPicker(
    themeMode: ThemeMode,
    accentMode: AccentMode,
    incognitoModeEnabled: Boolean,
    cunnyModeActive: Boolean,
    onCycleThemeMode: () -> Unit,
    onAccentModeSelected: (AccentMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = ACCENT_PICKER_OPTIONS
    var pickerVisible by remember { mutableStateOf(false) }
    var highlightedIndex by remember { mutableStateOf<Int?>(null) }

    val iconSize = 40.dp
    val chipSize = 22.dp
    val chipSpacing = 8.dp
    val pillPadding = 10.dp
    val pillGap = 8.dp
    val pillHeight = 34.dp
    val pillWidth =
        (pillPadding * 2) + (chipSize * options.size) + (chipSpacing * (options.size - 1))
    val expandedWidth = iconSize + pillGap + pillWidth

    val density = androidx.compose.ui.platform.LocalDensity.current
    fun indexForX(x: Float): Int? {
        val iconPx = with(density) { iconSize.toPx() }
        val gapPx = with(density) { pillGap.toPx() }
        val paddingPx = with(density) { pillPadding.toPx() }
        val chipPx = with(density) { chipSize.toPx() }
        val slotPx = with(density) { (chipSize + chipSpacing).toPx() }

        val start = iconPx + gapPx + paddingPx
        val end = start + ((options.size - 1) * slotPx) + chipPx
        if (x < start || x > end) return null
        val idx = ((x - start) / slotPx).toInt()
        return idx.coerceIn(0, options.lastIndex)
    }

    Box(
        modifier = modifier
            .width(if (pickerVisible && !incognitoModeEnabled) expandedWidth else iconSize)
            .height(iconSize)
            .pointerInput(incognitoModeEnabled, accentMode, cunnyModeActive) {
                awaitEachGesture {
                    if (cunnyModeActive) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        waitForUpOrCancellation()
                        return@awaitEachGesture
                    }
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (incognitoModeEnabled) {
                        waitForUpOrCancellation()
                        return@awaitEachGesture
                    }

                    val longPress = awaitLongPressOrCancellation(down.id)
                    if (longPress == null) {
                        onCycleThemeMode()
                        return@awaitEachGesture
                    }

                    pickerVisible = true
                    highlightedIndex = indexForX(longPress.position.x)
                        ?: options.indexOfFirst { it.mode == accentMode }.takeIf { it >= 0 }

                    var released = false
                    while (!released) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: event.changes.firstOrNull()
                        if (change == null) {
                            released = true
                        } else if (!change.pressed) {
                            released = true
                        } else {
                            highlightedIndex = indexForX(change.position.x) ?: highlightedIndex
                        }
                    }

                    val picked = highlightedIndex
                    if (picked != null && picked in options.indices) {
                        onAccentModeSelected(options[picked].mode)
                    }
                    pickerVisible = false
                    highlightedIndex = null
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        if (cunnyModeActive) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\uD83E\uDD80",
                    style = MaterialTheme.typography.titleLarge,
                    cunnyExempt = true
                )
            }
            return@Box
        }
        if (pickerVisible && !incognitoModeEnabled) {
            Box(
                modifier = Modifier
                    .offset(x = iconSize + pillGap)
                    .height(pillHeight)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = pillPadding),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(chipSpacing)
                ) {
                    options.forEachIndexed { index, option ->
                        val selected = accentMode == option.mode
                        val hovered = highlightedIndex == index
                        val ringColor = when {
                            hovered -> MaterialTheme.colorScheme.primary
                            selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                        if (option.color == null) {
                            val autoCircleColor = if (hovered || selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            }
                            Canvas(
                                modifier = Modifier
                                    .size(chipSize)
                                    .padding(1.dp)
                            ) {
                                val stroke = if (hovered || selected) 3f else 2f
                                val radius = (size.minDimension / 2f) - stroke
                                drawCircle(
                                    color = autoCircleColor,
                                    radius = radius,
                                    style = Stroke(width = stroke)
                                )
                                drawLine(
                                    color = autoCircleColor,
                                    start = Offset(size.width * 0.72f, size.height * 0.18f),
                                    end = Offset(size.width * 0.28f, size.height * 0.82f),
                                    strokeWidth = stroke
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(chipSize)
                                    .clip(CircleShape)
                                    .background(option.color)
                                    .border(
                                        width = if (hovered || selected) 2.2.dp else 1.2.dp,
                                        color = ringColor,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(CircleShape)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (incognitoModeEnabled) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_incognito_24),
                    contentDescription = "Incognito mode enabled",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = themeModeSymbol(themeMode),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

internal fun themeModeSymbol(mode: ThemeMode): String {
    return when (mode) {
        ThemeMode.LIGHT -> "☀"
        ThemeMode.DARK -> "☾"
        ThemeMode.SYSTEM -> "◐"
    }
}

internal data class AccentPickerOption(
    val mode: AccentMode,
    val color: Color?
)

private val ACCENT_PICKER_OPTIONS = listOf(
    AccentPickerOption(AccentMode.AUTO, null),
    AccentPickerOption(AccentMode.RED, Color(0xFFE53935)),
    AccentPickerOption(AccentMode.ORANGE, Color(0xFFFB8C00)),
    AccentPickerOption(AccentMode.AMBER, Color(0xFFF9A825)),
    AccentPickerOption(AccentMode.GREEN, Color(0xFF43A047)),
    AccentPickerOption(AccentMode.TEAL, Color(0xFF00897B)),
    AccentPickerOption(AccentMode.BLUE, Color(0xFF1E88E5)),
    AccentPickerOption(AccentMode.INDIGO, Color(0xFF5E35B1)),
    AccentPickerOption(AccentMode.PINK, Color(0xFFD81B60))
)


internal fun renderStars(rating: Int): String {
    val safe = rating.coerceIn(0, 5)
    return buildString {
        repeat(5) { idx ->
            append(if (idx < safe) '\u2605' else '\u2606')
        }
    }
}

internal fun formatRatingValue(rating: Float): String {
    val safe = rating.coerceIn(0f, 5f)
    return if (abs(safe - safe.roundToInt()) < 0.05f) {
        safe.roundToInt().toString()
    } else {
        String.format(Locale.US, "%.1f", safe)
    }
}
