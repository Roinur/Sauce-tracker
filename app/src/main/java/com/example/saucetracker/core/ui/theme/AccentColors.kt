package com.example.saucetracker.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

internal fun accentColorForMode(mode: AccentMode): Color? = when (mode) {
    AccentMode.AUTO -> null
    AccentMode.RED -> Color(0xFFE53935)
    AccentMode.ORANGE -> Color(0xFFFB8C00)
    AccentMode.AMBER -> Color(0xFFF9A825)
    AccentMode.GREEN -> Color(0xFF43A047)
    AccentMode.TEAL -> Color(0xFF00897B)
    AccentMode.BLUE -> Color(0xFF1E88E5)
    AccentMode.INDIGO -> Color(0xFF5E35B1)
    AccentMode.PINK -> Color(0xFFD81B60)
}

internal fun preferredOnAccent(color: Color): Color {
    val luminance = (0.299f * color.red) + (0.587f * color.green) + (0.114f * color.blue)
    return if (luminance >= 0.62f) Color(0xFF111111) else Color.White
}

internal fun applyAccentMode(
    baseScheme: ColorScheme,
    accentMode: AccentMode,
    isDark: Boolean
): ColorScheme {
    val accent = accentColorForMode(accentMode) ?: return baseScheme
    val container = accent.copy(alpha = if (isDark) 0.34f else 0.22f)
    return baseScheme.copy(
        primary = accent,
        onPrimary = preferredOnAccent(accent),
        secondary = accent,
        tertiary = accent,
        primaryContainer = container,
        secondaryContainer = container,
        tertiaryContainer = container
    )
}
