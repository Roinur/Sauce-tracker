package com.example.saucetracker.feature.dashboard

import android.content.SharedPreferences
import com.example.saucetracker.*
import com.example.saucetracker.core.preferences.*
import com.example.saucetracker.core.ui.theme.AccentMode
import com.example.saucetracker.data.backup.KEY_BACKUP_THUMBNAIL_ARCHIVE_ENABLED

internal class DashboardPreferenceReader(
    private val prefs: SharedPreferences
) {
    fun loadThemeMode(): ThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.SYSTEM
    }

    fun loadAccentMode(): AccentMode {
        val raw = prefs.getString(KEY_ACCENT_MODE, AccentMode.AUTO.name)
        return AccentMode.entries.firstOrNull { it.name == raw } ?: AccentMode.AUTO
    }

    fun loadShowThumbnails(): Boolean = prefs.getBoolean(KEY_SHOW_THUMBNAILS, true)

    fun loadAdaptiveScrollThumbnails(): Boolean =
        prefs.getBoolean(KEY_ADAPTIVE_SCROLL_THUMBNAILS, true)

    fun loadPerformanceOverlayEnabled(): Boolean =
        prefs.getBoolean(KEY_PERFORMANCE_OVERLAY_ENABLED, false)

    fun loadPureGalleryMode(): Boolean = prefs.getBoolean(KEY_PURE_GALLERY_MODE, true)

    fun loadGalleryColumns(): Int = prefs.getInt(KEY_GALLERY_COLUMNS, 2).coerceIn(1, 10)

    fun loadLegacyHomeUi(): Boolean = prefs.getBoolean(KEY_LEGACY_HOME_UI, false)

    fun loadExperimentalLazyEntryDetail(): Boolean = true

    fun loadExperimentalSubscriptionInbox(): Boolean = true

    fun loadExperimentalFilterStatusStrip(): Boolean = true

    fun loadExperimentalDashboardLongPress(): Boolean = true

    fun loadEntryPinPriorityEnabled(): Boolean = prefs.getBoolean(KEY_ENTRY_PIN_PRIORITY, true)

    fun defaultEntryReadFilterCycle(): List<EntryReadFilterMode> = listOf(
        EntryReadFilterMode.ALL,
        EntryReadFilterMode.READ,
        EntryReadFilterMode.UNREAD,
        EntryReadFilterMode.DOWNLOADED
    )

    fun normalizedEntryReadFilterCycle(order: List<EntryReadFilterMode>): List<EntryReadFilterMode> {
        val normalized = order.distinct()
        return if (normalized.isEmpty()) defaultEntryReadFilterCycle() else normalized
    }

    fun initialEntryReadFilterForCycle(order: List<EntryReadFilterMode>): EntryReadFilterMode =
        normalizedEntryReadFilterCycle(order).first()

    fun loadEntryReadFilterCycleOrder(): List<EntryReadFilterMode> {
        val raw = prefs.getString(KEY_ENTRY_FILTER_CYCLE_ORDER, "").orEmpty()
        val parsed = raw
            .split(',')
            .mapNotNull { token ->
                EntryReadFilterMode.entries.firstOrNull {
                    it.name.equals(token.trim(), ignoreCase = true)
                }
            }
        return normalizedEntryReadFilterCycle(parsed)
    }

    fun defaultHomeSectionOrder(): List<HomeSection> = listOf(
        HomeSection.TAGS,
        HomeSection.ENTRIES,
        HomeSection.SUGGESTED,
        HomeSection.SUBSCRIPTIONS,
        HomeSection.CREATORS,
        HomeSection.HEATMAP
    )

    fun normalizedHomeSectionOrder(order: List<HomeSection>): List<HomeSection> = order.distinct()

    fun loadHomeSectionOrder(): List<HomeSection> {
        val raw = prefs.getString(KEY_HOME_SECTION_ORDER, null) ?: return defaultHomeSectionOrder()
        if (raw.isBlank()) return emptyList()
        val parsed = raw
            .split(',')
            .mapNotNull { token ->
                HomeSection.entries.firstOrNull { it.name.equals(token.trim(), ignoreCase = true) }
            }
        return normalizedHomeSectionOrder(parsed)
    }

    fun loadDefaultEntrySortField(): EntrySortField? {
        val raw = prefs.getString(KEY_DEFAULT_ENTRY_SORT_FIELD, "NONE").orEmpty()
        if (raw.equals("NONE", ignoreCase = true)) return null
        return EntrySortField.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
    }

    fun loadDefaultEntrySortDirection(field: EntrySortField?): SortDirection {
        val fallback = when (field) {
            EntrySortField.TITLE -> SortDirection.ASC
            else -> SortDirection.DESC
        }
        val raw = prefs.getString(KEY_DEFAULT_ENTRY_SORT_DIRECTION, fallback.name).orEmpty()
        return SortDirection.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: fallback
    }

    fun loadDefaultTagSortField(): TagSortField {
        val raw = prefs.getString(KEY_DEFAULT_TAG_SORT_FIELD, TagSortField.COUNT.name).orEmpty()
        return TagSortField.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
            ?: TagSortField.COUNT
    }

    fun loadDefaultTagSortDirection(field: TagSortField): SortDirection {
        val fallback = when (field) {
            TagSortField.COUNT -> SortDirection.DESC
            TagSortField.NAME, TagSortField.TYPE -> SortDirection.ASC
        }
        val raw = prefs.getString(KEY_DEFAULT_TAG_SORT_DIRECTION, fallback.name).orEmpty()
        return SortDirection.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: fallback
    }

    fun loadDefaultCreatorSortField(): CreatorSortField {
        val raw = prefs.getString(KEY_DEFAULT_CREATOR_SORT_FIELD, CreatorSortField.COUNT.name).orEmpty()
        return CreatorSortField.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
            ?: CreatorSortField.COUNT
    }

    fun loadDefaultCreatorSortDirection(field: CreatorSortField): SortDirection {
        val fallback = when (field) {
            CreatorSortField.COUNT -> SortDirection.DESC
            CreatorSortField.NAME, CreatorSortField.TYPE -> SortDirection.ASC
        }
        val raw = prefs.getString(KEY_DEFAULT_CREATOR_SORT_DIRECTION, fallback.name).orEmpty()
        return SortDirection.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: fallback
    }

    fun loadDefaultBrowserDuplicateCheckMode(): BrowserDuplicateCheckMode {
        val raw = prefs.getString(
            KEY_BROWSER_DUPLICATE_CHECK_MODE,
            BrowserDuplicateCheckMode.AGGRESSIVE.name
        ).orEmpty()
        return BrowserDuplicateCheckMode.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
            ?: BrowserDuplicateCheckMode.AGGRESSIVE
    }

    fun loadApplyBlockedTagsToHome(): Boolean =
        prefs.getBoolean(KEY_APPLY_BLOCKED_TAGS_HOME, false)

    fun loadApplyBlockedTagsToSearchTerms(): Boolean =
        prefs.getBoolean(KEY_APPLY_BLOCKED_TAGS_SEARCH, true)

    fun loadPreloadOnLaunch(): Boolean = prefs.getBoolean(KEY_PRELOAD_ON_LAUNCH, false)

    fun loadSubscriptionRefreshIntervalHours(): Int =
        prefs.getInt(KEY_SUBSCRIPTION_REFRESH_INTERVAL_HOURS, 6).coerceIn(1, 24)

    fun defaultSuggestionCategoryWeights(): Map<SuggestionWeightCategory, Float> =
        SuggestionWeightCategory.entries.associateWith { 1f }

    fun loadSuggestionCategoryWeights(): Map<SuggestionWeightCategory, Float> =
        defaultSuggestionCategoryWeights().toMutableMap().apply {
            SuggestionWeightCategory.entries.forEach { category ->
                val key = "$KEY_SUGGESTION_WEIGHT_PREFIX${category.storageKey}"
                this[category] = prefs.getFloat(key, 1f).coerceIn(0f, 2f)
            }
        }

    fun loadSuggestionThemeStrength(): Float =
        prefs.getFloat(KEY_SUGGESTION_THEME_STRENGTH, 1f).coerceIn(0f, 2f)

    fun loadIncognitoMode(): Boolean = prefs.getBoolean(KEY_INCOGNITO_MODE_ENABLED, false)

    fun loadPreloadPercent(): Int = prefs.getInt(KEY_PRELOAD_PERCENT, 35).coerceIn(0, 100)

    fun loadAutoBackupTreeUri(): String =
        prefs.getString(KEY_AUTO_BACKUP_TREE_URI, "").orEmpty()

    fun loadBackupThumbnailArchiveEnabled(): Boolean =
        prefs.getBoolean(KEY_BACKUP_THUMBNAIL_ARCHIVE_ENABLED, false)

    fun loadDesktopBridgeEnabled(): Boolean =
        prefs.getBoolean(KEY_DESKTOP_BRIDGE_ENABLED, false)

    fun loadDesktopBridgePort(): Int =
        prefs.getInt(KEY_DESKTOP_BRIDGE_PORT, DESKTOP_BRIDGE_DEFAULT_PORT).coerceIn(1024, 65535)

    fun loadAppLockEnabled(): Boolean = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)

    fun loadAppLockBiometricEnabled(): Boolean =
        prefs.getBoolean(KEY_APP_LOCK_BIOMETRIC_ENABLED, true)

    private companion object {
        const val DESKTOP_BRIDGE_DEFAULT_PORT = 17366
    }
}
