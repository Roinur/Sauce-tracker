package com.roinur.saucetracker.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/** Book Tracker's opt-in dark surfaces; wallpaper/custom accents stay intact. */
internal fun applyExtraDarkMode(
    baseScheme: ColorScheme,
    isDark: Boolean,
    enabled: Boolean
): ColorScheme = if (isDark && enabled) baseScheme.copy(
    background = Color(0xFF0B0A12),
    onBackground = Color(0xFFE5E1EF),
    surface = Color(0xFF14121E),
    onSurface = Color(0xFFE5E1EF),
    onSurfaceVariant = Color(0xFFB9B3CC),
    surfaceVariant = Color(0xFF201D2C),
    surfaceContainerLowest = Color(0xFF09080F),
    surfaceContainerLow = Color(0xFF11101A),
    surfaceContainer = Color(0xFF181620),
    surfaceContainerHigh = Color(0xFF211E2B),
    surfaceContainerHighest = Color(0xFF2A2735),
    outline = Color(0xFF716A83),
    outlineVariant = Color(0xFF302B40)
) else baseScheme
