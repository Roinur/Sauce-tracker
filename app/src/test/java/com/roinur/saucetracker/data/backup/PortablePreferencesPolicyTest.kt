package com.roinur.saucetracker.data.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PortablePreferencesPolicyTest {
    @Test
    fun normalSettingsArePortable() {
        assertTrue(PortablePreferences.isPortable("theme_mode"))
        assertTrue(PortablePreferences.isPortable("app_lock_pin_hash"))
        assertTrue(PortablePreferences.isPortable("slideshow_reading_mode"))
        assertTrue(PortablePreferences.isPortable("future_setting_added_later"))
    }

    @Test
    fun installBoundAndDisposableValuesStayLocal() {
        assertFalse(PortablePreferences.isPortable("app_lock_grace_until"))
        assertFalse(PortablePreferences.isPortable("auto_backup_tree_uri"))
        assertFalse(PortablePreferences.isPortable("backup_thumbnail_archive_enabled"))
        assertFalse(PortablePreferences.isPortable("gallery_download_tree_uri"))
        assertFalse(PortablePreferences.isPortable("gallery_download_skip_prompt"))
        assertFalse(PortablePreferences.isPortable("suggestion_result_cache_v1"))
        assertFalse(PortablePreferences.isPortable("suggestion_gallery_cache_v1"))
    }
}
