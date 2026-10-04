package com.roinur.saucetracker.data.database

internal object DatabaseSchema {
    const val VERSION = 8

    object Tables {
        const val ENTRIES = "entries"
        const val TAGS = "tags"
        const val ENTRY_TAGS = "entry_tags"
        const val SUBSCRIPTIONS = "subscriptions"
        const val SUBSCRIPTION_SEEN_CODES = "subscription_seen_codes"
        const val SUBSCRIPTION_EVENTS = "subscription_events"
        const val DAILY_READ_ACTIVITY = "daily_read_activity"
        const val READING_SESSIONS = "reading_sessions"
        const val POPULAR_TAGS = "popular_tags"
        const val ENTRY_HEATMAP_CACHE = "entry_heatmap_cache"
        const val SOURCES = "sources"
        const val PROFILES = "profiles"
        const val SOURCE_ENTRIES = "source_entries"
        const val PROFILE_ENTRIES = "profile_entries"
        const val SOURCE_CHAPTER_CACHE = "source_chapter_cache"
    }
}
