package com.roinur.saucetracker.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ExtraDarkColorsTest {
    @Test fun disabledKeepsExistingDarkSchemeUnchanged() {
        val original = darkColorScheme()
        assertSame(original, applyExtraDarkMode(original, isDark = true, enabled = false))
    }

    @Test fun enabledDoesNotChangeLightMode() {
        val original = lightColorScheme()
        assertSame(original, applyExtraDarkMode(original, isDark = false, enabled = true))
    }

    @Test fun darkSurfacesMatchBookTracker() {
        val dark = applyExtraDarkMode(darkColorScheme(), isDark = true, enabled = true)
        assertEquals(Color(0xFF0B0A12), dark.background)
        assertEquals(Color(0xFF14121E), dark.surface)
        assertEquals(Color(0xFF201D2C), dark.surfaceVariant)
        assertEquals(Color(0xFF09080F), dark.surfaceContainerLowest)
        assertEquals(Color(0xFF11101A), dark.surfaceContainerLow)
        assertEquals(Color(0xFF181620), dark.surfaceContainer)
        assertEquals(Color(0xFF211E2B), dark.surfaceContainerHigh)
        assertEquals(Color(0xFF2A2735), dark.surfaceContainerHighest)
        assertEquals(Color(0xFF302B40), dark.outlineVariant)
        assertEquals(Color(0xFFE5E1EF), dark.onSurface)
    }

    @Test fun wallpaperAccentAndErrorArePreserved() {
        val original = darkColorScheme(primary = Color.Green, secondary = Color.Cyan, tertiary = Color.Yellow)
        val dark = applyExtraDarkMode(original, isDark = true, enabled = true)
        assertEquals(original.primary, dark.primary)
        assertEquals(original.secondary, dark.secondary)
        assertEquals(original.tertiary, dark.tertiary)
        assertEquals(original.error, dark.error)
    }

    @Test fun customAccentDoesNotUndoExtraDarkSurfaces() {
        val dark = applyExtraDarkMode(darkColorScheme(), isDark = true, enabled = true)
        val accented = applyAccentMode(dark, AccentMode.BLUE, isDark = true)
        assertEquals(accentColorForMode(AccentMode.BLUE), accented.primary)
        assertEquals(dark.background, accented.background)
        assertEquals(dark.surfaceContainer, accented.surfaceContainer)
    }
}
