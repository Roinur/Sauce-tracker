package com.roinur.saucetracker.feature.slideshow

import org.junit.Assert.assertEquals
import org.junit.Test

class SlideshowHoldChoiceTest {
    private val width = 1260f
    private val height = 2800f
    private val density = 3f

    @Test
    fun `mode cards and screenshot target map to three exclusive choices`() {
        assertEquals(
            SlideshowHoldChoice.HORIZONTAL,
            mapSlideshowHoldChoice(400f, height / 2f, width, height, density)
        )
        assertEquals(
            SlideshowHoldChoice.VERTICAL,
            mapSlideshowHoldChoice(860f, height / 2f, width, height, density)
        )
        assertEquals(
            SlideshowHoldChoice.SCREENSHOT,
            mapSlideshowHoldChoice(width / 2f, (height / 2f) + (190f * density), width, height, density)
        )
    }

    @Test
    fun `incognito excludes screenshot choice`() {
        val choice = mapSlideshowHoldChoice(
            x = width / 2f,
            y = (height / 2f) + (190f * density),
            screenWidthPx = width,
            screenHeightPx = height,
            density = density,
            screenshotEnabled = false
        )
        assertEquals(SlideshowHoldChoice.HORIZONTAL, choice)
    }

    @Test
    fun `screenshot pickup is limited to one and a half visible radii`() {
        val centerX = width / 2f
        val centerY = (height / 2f) + (190f * density)

        assertEquals(
            SlideshowHoldChoice.SCREENSHOT,
            mapSlideshowHoldChoice(
                x = centerX + (57f * density),
                y = centerY,
                screenWidthPx = width,
                screenHeightPx = height,
                density = density
            )
        )
        assertEquals(
            SlideshowHoldChoice.VERTICAL,
            mapSlideshowHoldChoice(
                x = centerX + (57.1f * density),
                y = centerY,
                screenWidthPx = width,
                screenHeightPx = height,
                density = density
            )
        )
    }
}
