package com.roinur.saucetracker.feature.slideshow

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SlideshowVolumeNavigationTest {
    @Test
    fun verticalVolumeTargetsUseEdgesOnlyForFirstAndLastPage() {
        assertEquals(
            20f,
            verticalPageAlignmentDelta(0, 4, 10, 1010, 30, 1400)
        )
        assertEquals(
            100f,
            verticalPageAlignmentDelta(2, 4, 10, 1010, 210, 800)
        )
        assertEquals(
            -60f,
            verticalPageAlignmentDelta(4, 4, 10, 1010, 250, 700)
        )
    }

    @Test
    fun volumeButtonsPreserveTheirPhysicalIdentity() {
        assertEquals(
            SlideshowVolumeNavigation.VOLUME_UP,
            slideshowVolumeNavigationForKey(KeyEvent.KEYCODE_VOLUME_UP)
        )
        assertEquals(
            SlideshowVolumeNavigation.VOLUME_DOWN,
            slideshowVolumeNavigationForKey(KeyEvent.KEYCODE_VOLUME_DOWN)
        )
    }

    @Test
    fun unrelatedHardwareKeysAreNotCaptured() {
        assertTrue(isSlideshowVolumeKey(KeyEvent.KEYCODE_VOLUME_UP))
        assertTrue(isSlideshowVolumeKey(KeyEvent.KEYCODE_VOLUME_DOWN))
        assertFalse(isSlideshowVolumeKey(KeyEvent.KEYCODE_POWER))
        assertNull(slideshowVolumeNavigationForKey(KeyEvent.KEYCODE_BACK))
    }
}
