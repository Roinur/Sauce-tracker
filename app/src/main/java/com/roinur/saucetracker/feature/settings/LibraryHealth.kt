package com.roinur.saucetracker.feature.settings

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.roinur.saucetracker.core.preferences.KEY_AUTO_BACKUP_TREE_URI
import com.roinur.saucetracker.core.preferences.KEY_ENTRY_PIN_PRIORITY
import com.roinur.saucetracker.core.preferences.KEY_TAG_PRESETS
import com.roinur.saucetracker.core.preferences.KEY_TASTE_TRAINING_FEEDBACK
import com.roinur.saucetracker.data.backup.BackupImporter
import com.roinur.saucetracker.data.backup.BackupHistoryValidation
import com.roinur.saucetracker.data.backup.BackupSerializer
import com.roinur.saucetracker.data.backup.BackupSnapshotExport
import com.roinur.saucetracker.ParsedImportPayload
import com.roinur.saucetracker.data.backup.PortablePreferences
import com.roinur.saucetracker.data.backup.readCurrentProceduralBackupTextOrNull
import com.roinur.saucetracker.data.database.SauceTrackerDatabase
import com.roinur.saucetracker.data.downloads.loadGalleryDownloadTreeUri
import com.roinur.saucetracker.core.diagnostics.GitHubMediaSession
import com.roinur.saucetracker.feature.saucefinder.SauceFinderIndexStore
import org.json.JSONArray
import org.json.JSONObject

private fun canonicalBackupJson(value: Any?): String = when (value) {
    null, JSONObject.NULL -> "null"
    is JSONObject -> value.keys().asSequence().toList().sorted().joinToString(",", "{", "}") { key ->
        "${JSONObject.quote(key)}:${canonicalBackupJson(value.opt(key))}"
    }
    is JSONArray -> (0 until value.length()).map { canonicalBackupJson(value.opt(it)) }.sorted().joinToString(",", "[", "]")
    is Number, is Boolean -> value.toString()
    else -> JSONObject.quote(value.toString())
}

private fun JSONArray.countSourceRows(sourceId: String): Int =
    (0 until length()).count { index -> optJSONObject(index)?.optString("source_id") == sourceId }

internal enum class LibraryHealthLevel { HEALTHY, ATTENTION, ACTION_REQUIRED }

internal data class LibraryHealthCheck(
    val title: String,
    val detail: String,
    val level: LibraryHealthLevel
)

internal data class LibraryHealthReport(
    val checks: List<LibraryHealthCheck>,
    val scannedAtMillis: Long
) {
    val level: LibraryHealthLevel = when {
        checks.any { it.level == LibraryHealthLevel.ACTION_REQUIRED } -> LibraryHealthLevel.ACTION_REQUIRED
        checks.any { it.level == LibraryHealthLevel.ATTENTION } -> LibraryHealthLevel.ATTENTION
        else -> LibraryHealthLevel.HEALTHY
    }
}

internal object LibraryHealthScanner {
    fun scan(context: Context, db: SauceTrackerDatabase, preferences: SharedPreferences): LibraryHealthReport {
        val checks = mutableListOf<LibraryHealthCheck>()
        val sql = db.readableDatabase
        val quickCheck = sql.rawQuery("PRAGMA quick_check", null).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0).orEmpty()) }
        }
        checks += LibraryHealthCheck(
            "SQLite integrity",
            if (quickCheck.size == 1 && quickCheck.first().equals("ok", true)) "PRAGMA quick_check returned ok." else quickCheck.joinToString(),
            if (quickCheck.size == 1 && quickCheck.first().equals("ok", true)) LibraryHealthLevel.HEALTHY else LibraryHealthLevel.ACTION_REQUIRED
        )

        fun count(query: String): Int = sql.rawQuery(query, null).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        val entries = count("SELECT COUNT(*) FROM entries")
        val tags = count("SELECT COUNT(*) FROM tags")
        val creators = count("SELECT COUNT(*) FROM tags WHERE type IN ('artist','group')")
        val subscriptions = count("SELECT COUNT(*) FROM subscriptions")
        checks += LibraryHealthCheck("Library records", "$entries entries, $tags tags, $creators artists/groups, $subscriptions subscriptions.", LibraryHealthLevel.HEALTHY)

        val sourceEntries = count("SELECT COUNT(*) FROM source_entries")
        val mainProfiles = count("SELECT COUNT(*) FROM profiles WHERE id='main' AND is_main=1")
        val activeProfiles = count("SELECT COUNT(*) FROM app_state a JOIN profiles p ON p.id=a.active_profile_id WHERE a.slot_id=1")
        val missingMigratedCodes = count("SELECT COUNT(*) FROM entries e LEFT JOIN source_entries se ON se.source_id='nhentai' AND se.remote_id=CAST(e.code AS TEXT) WHERE se.id IS NULL")
        val orphanProfileState = count("SELECT COUNT(*) FROM profile_entries pe LEFT JOIN profiles p ON p.id=pe.profile_id LEFT JOIN source_entries se ON se.id=pe.source_entry_id WHERE p.id IS NULL OR se.id IS NULL")
        val sourceHealthy = mainProfiles == 1 && activeProfiles == 1 && missingMigratedCodes == 0 && orphanProfileState == 0
        checks += LibraryHealthCheck(
            "2.0 sources and profiles",
            "$sourceEntries source entries; Default profile ${if (mainProfiles == 1) "present" else "invalid"}; $missingMigratedCodes legacy entries missing source identity; $orphanProfileState orphan profile states.",
            if (sourceHealthy) LibraryHealthLevel.HEALTHY else LibraryHealthLevel.ACTION_REQUIRED
        )

        val orphanLinks = count("SELECT COUNT(*) FROM entry_tags et LEFT JOIN entries e ON e.code=et.entry_code LEFT JOIN tags t ON t.id=et.tag_id WHERE e.code IS NULL OR t.id IS NULL")
        checks += LibraryHealthCheck("Tag relations", if (orphanLinks == 0) "No orphan relations." else "$orphanLinks orphan relations found.", if (orphanLinks == 0) LibraryHealthLevel.HEALTHY else LibraryHealthLevel.ACTION_REQUIRED)

        val invalidRatings = count("SELECT COUNT(*) FROM profile_entries WHERE rating < 0 OR rating > 5")
        checks += LibraryHealthCheck("Ratings", if (invalidRatings == 0) "All ratings are within 0–5." else "$invalidRatings invalid ratings found.", if (invalidRatings == 0) LibraryHealthLevel.HEALTHY else LibraryHealthLevel.ACTION_REQUIRED)

        val missingSessionEntries = count("SELECT COUNT(*) FROM reading_sessions rs LEFT JOIN source_entries se ON se.source_id=rs.source_id AND se.remote_id=rs.remote_id WHERE se.id IS NULL")
        val unratedMissingSessions = count("SELECT COUNT(*) FROM reading_sessions rs LEFT JOIN source_entries se ON se.source_id=rs.source_id AND se.remote_id=rs.remote_id WHERE se.id IS NULL AND COALESCE(rs.rating,0)=0")
        val invalidSessionOwners = count("SELECT COUNT(*) FROM reading_sessions rs LEFT JOIN profiles p ON p.id=rs.profile_id LEFT JOIN sources s ON s.id=rs.source_id WHERE p.id IS NULL OR s.id IS NULL OR TRIM(rs.remote_id)='' OR rs.remote_id='0'")
        val readWithoutSession = count("SELECT COUNT(*) FROM profile_entries pe JOIN source_entries se ON se.id=pe.source_entry_id WHERE pe.read_state=1 AND NOT EXISTS (SELECT 1 FROM reading_sessions rs WHERE rs.profile_id=pe.profile_id AND rs.source_id=se.source_id AND rs.remote_id=se.remote_id)")
        checks += readingHistoryHealthChecks(
            missingSessionEntries, unratedMissingSessions, invalidSessionOwners, readWithoutSession
        )

        val trainingValid = validJsonArray(preferences.getString(KEY_TASTE_TRAINING_FEEDBACK, "").orEmpty())
        val presetsValid = validJsonArray(preferences.getString(KEY_TAG_PRESETS, "").orEmpty())
        checks += LibraryHealthCheck(
            "1.9 local models",
            "Training feedback ${if (trainingValid) "valid" else "invalid"}; tag presets ${if (presetsValid) "valid" else "invalid"}.",
            if (trainingValid && presetsValid) LibraryHealthLevel.HEALTHY else LibraryHealthLevel.ACTION_REQUIRED
        )

        val persisted = context.contentResolver.persistedUriPermissions.map { it.uri.toString() }.toSet()
        val backupUri = preferences.getString(KEY_AUTO_BACKUP_TREE_URI, "").orEmpty()
        val permissionOk = backupUri.isBlank() || backupUri in persisted
        checks += LibraryHealthCheck(
            "Document access",
            when {
                backupUri.isBlank() -> "No procedural backup folder configured."
                permissionOk -> "Procedural backup folder permission is persisted."
                else -> "Procedural backup folder permission is missing."
            },
            if (!permissionOk) LibraryHealthLevel.ACTION_REQUIRED else if (backupUri.isBlank()) LibraryHealthLevel.ATTENTION else LibraryHealthLevel.HEALTHY
        )

        checks += verifyCurrentDataSnapshot(context, db, preferences)
            .copy(title = "Verified restore (current data)")
        if (backupUri.isNotBlank() && permissionOk) {
            checks += verifyCurrentBackup(context, Uri.parse(backupUri))
                .copy(title = "Verified restore (procedural backup)")
        }

        val downloadUri = loadGalleryDownloadTreeUri(context)
        val downloadPermissionOk = downloadUri.isBlank() || downloadUri in persisted
        checks += LibraryHealthCheck(
            "Gallery downloads",
            when {
                downloadUri.isBlank() -> "No gallery download folder configured."
                downloadPermissionOk -> "Gallery download folder permission is persisted."
                else -> "Gallery download folder permission is missing."
            },
            if (downloadPermissionOk) LibraryHealthLevel.HEALTHY else LibraryHealthLevel.ACTION_REQUIRED
        )

        val libraryCodes = mutableSetOf<Int>()
        sql.rawQuery("SELECT code FROM entries", null).use { cursor -> while (cursor.moveToNext()) libraryCodes += cursor.getInt(0) }
        val sauceIndexName = if (GitHubMediaSession.active) "sauce_finder_index_github.db" else "sauce_finder_index.db"
        if (context.getDatabasePath(sauceIndexName).isFile) {
            SauceFinderIndexStore(context).use { indexStore ->
                val stats = indexStore.stats()
                val orphanRows = indexStore.all().count { it.entryCode !in libraryCodes }
                checks += LibraryHealthCheck(
                    "Sauce Finder index",
                    "${stats.images} pages across ${stats.entries} entries; $orphanRows orphan rows.",
                    if (orphanRows == 0) LibraryHealthLevel.HEALTHY else LibraryHealthLevel.ATTENTION
                )
            }
        } else {
            checks += LibraryHealthCheck("Sauce Finder index", "Index has not been built yet.", LibraryHealthLevel.HEALTHY)
        }

        val heatmapCacheRows = count("SELECT COUNT(*) FROM entry_heatmap_cache WHERE payload_json <> ''")
        checks += LibraryHealthCheck(
            "Rebuildable caches",
            "Entry Heatmap has $heatmapCacheRows cached layout(s). Suggestion caches are disposable and fingerprinted by library plus training revision.",
            LibraryHealthLevel.HEALTHY
        )
        return LibraryHealthReport(checks, System.currentTimeMillis())
    }

    private fun validJsonArray(raw: String): Boolean = raw.isBlank() || runCatching { JSONArray(raw) }.isSuccess

    private fun verifyCurrentBackup(context: Context, uri: Uri): LibraryHealthCheck {
        val text = readCurrentProceduralBackupTextOrNull(context, uri)
            ?: return LibraryHealthCheck("Verified restore", "No current procedural backup could be read.", LibraryHealthLevel.ATTENTION)
        return runCatching {
            verifyPayload(context, BackupImporter().parse(text))
        }.getOrElse {
            LibraryHealthCheck("Verified restore", "Backup validation failed: ${it.message ?: "unknown error"}", LibraryHealthLevel.ACTION_REQUIRED)
        }
    }

    private fun verifyCurrentDataSnapshot(
        context: Context,
        db: SauceTrackerDatabase,
        preferences: SharedPreferences
    ): LibraryHealthCheck = runCatching {
        val importer = BackupImporter()
        val snapshot = BackupSnapshotExport.buildSnapshotWithSettings(
            db = db,
            prefs = preferences,
            backupImporter = importer,
            entryPinPriorityEnabled = preferences.getBoolean(KEY_ENTRY_PIN_PRIORITY, false)
        )
        verifyPayload(context, importer.parse(BackupSerializer.serialize(snapshot)))
    }.getOrElse {
        LibraryHealthCheck("Verified restore", "Current-data backup validation failed: ${it.message ?: "unknown error"}", LibraryHealthLevel.ACTION_REQUIRED)
    }

    private fun verifyPayload(context: Context, payload: ParsedImportPayload): LibraryHealthCheck {
        return runCatching {
            val tempName = "library_health_restore_${System.currentTimeMillis()}.db"
            val tempPrefsName = "library_health_restore_preferences_${System.currentTimeMillis()}"
            val tempDb = SauceTrackerDatabase(context.applicationContext, tempName)
            val tempPreferences = context.getSharedPreferences(tempPrefsName, Context.MODE_PRIVATE)
            try {
                fun restore() = tempDb.importSnapshot(
                    entries = payload.entries,
                    creators = payload.creators,
                    popularTags = payload.popularTags,
                    entryHeatmapCache = payload.entryHeatmapCache,
                    subscriptions = payload.subscriptions,
                    subscriptionSeenCodes = payload.subscriptionSeenCodes,
                    subscriptionEvents = payload.subscriptionEvents,
                    dailyReadActivity = payload.dailyReadActivity,
                    readingSessions = payload.readingSessions,
                    sourcePlatform = payload.sourcePlatform
                )
                fun fingerprint(): String = buildString {
                    append(tempDb.exportSnapshot().toString())
                    append('|').append(tempDb.exportDailyReadActivitySnapshot().toString())
                    append('|').append(tempDb.exportReadingSessionsSnapshot().toString())
                    append('|').append(tempDb.exportPopularTagsSnapshot().toString())
                    append('|').append(canonicalBackupJson(tempDb.exportSourcePlatformSnapshot()))
                    append('|').append(canonicalBackupJson(tempDb.exportSubscriptionsSnapshot()))
                    append('|').append(canonicalBackupJson(tempDb.exportSubscriptionSeenCodesSnapshot()))
                    append('|').append(canonicalBackupJson(tempDb.exportSubscriptionEventsSnapshot()))
                }
                val first = restore()
                val once = fingerprint()
                val sourcePlatformRoundTrip = payload.sourcePlatform?.let { expected ->
                    canonicalBackupJson(expected) == canonicalBackupJson(tempDb.exportSourcePlatformSnapshot())
                } ?: true
                val readingSessionsRoundTrip = payload.readingSessions?.let { expected ->
                    BackupHistoryValidation.canonicalRows(expected.toString(), "reading_sessions") ==
                        BackupHistoryValidation.canonicalRows(tempDb.exportReadingSessionsSnapshot().toString(), "reading_sessions")
                } ?: true
                val subscriptionsRoundTrip = payload.subscriptions?.let { expected ->
                    BackupHistoryValidation.canonicalRows(expected.toString(), "subscriptions") ==
                        BackupHistoryValidation.canonicalRows(tempDb.exportSubscriptionsSnapshot().toString(), "subscriptions")
                } ?: true
                val seenCodesRoundTrip = payload.subscriptionSeenCodes?.let { expected ->
                    BackupHistoryValidation.canonicalRows(expected.toString(), "subscription_seen_codes") ==
                        BackupHistoryValidation.canonicalRows(tempDb.exportSubscriptionSeenCodesSnapshot().toString(), "subscription_seen_codes")
                } ?: true
                val eventsRoundTrip = payload.subscriptionEvents?.let { expected ->
                    BackupHistoryValidation.canonicalRows(expected.toString(), "subscription_events") ==
                        BackupHistoryValidation.canonicalRows(tempDb.exportSubscriptionEventsSnapshot().toString(), "subscription_events")
                } ?: true
                val dailyActivityRoundTrip = payload.dailyReadActivity?.let { expected ->
                    canonicalBackupJson(expected) == canonicalBackupJson(tempDb.exportDailyReadActivitySnapshot())
                } ?: true
                val second = restore()
                val twice = fingerprint()
                val integrityHealthy = tempDb.readableDatabase.rawQuery("PRAGMA integrity_check", null).use {
                    it.moveToFirst() && it.getString(0) == "ok"
                }
                val idempotent = once == twice && second.inserted == 0 && second.skipped == 0 && integrityHealthy
                val portableSnapshot = payload.portablePreferences
                val portableSettingsPresent = portableSnapshot != null
                val portableSettingsValid = PortablePreferences.isValidSnapshot(portableSnapshot)
                val expectedPortableSettings = if (portableSettingsValid) {
                    PortablePreferences.decode(portableSnapshot)
                } else {
                    emptyMap()
                }
                val appliedPortableSettings = if (portableSettingsValid) {
                    PortablePreferences.apply(tempPreferences, portableSnapshot)
                } else {
                    0
                }
                val restoredPortableSettings = if (portableSettingsValid) {
                    PortablePreferences.decode(PortablePreferences.encode(tempPreferences))
                } else {
                    emptyMap()
                }
                val portableSettingsRoundTrip = portableSettingsValid &&
                    appliedPortableSettings == expectedPortableSettings.size &&
                    restoredPortableSettings == expectedPortableSettings
                val mangaDexEntries = payload.sourcePlatform
                    ?.optJSONArray("source_entries")
                    ?.countSourceRows("mangadex") ?: 0
                val mangaDexReaderProgress = payload.sourcePlatform
                    ?.optJSONArray("source_reader_progress")
                    ?.countSourceRows("mangadex") ?: 0
                val mangaDexChapterProgress = payload.sourcePlatform
                    ?.optJSONArray("source_chapter_progress")
                    ?.countSourceRows("mangadex") ?: 0
                val mangaDexSessions = payload.readingSessions?.countSourceRows("mangadex") ?: 0
                val mangaDexSubscriptions = payload.subscriptions?.countSourceRows("mangadex") ?: 0
                val sourceDataRoundTrip = sourcePlatformRoundTrip && readingSessionsRoundTrip && subscriptionsRoundTrip &&
                    seenCodesRoundTrip && eventsRoundTrip && dailyActivityRoundTrip
                val failedRoundTrips = listOfNotNull(
                    "source platform".takeUnless { sourcePlatformRoundTrip },
                    "reading sessions".takeUnless { readingSessionsRoundTrip },
                    "subscriptions".takeUnless { subscriptionsRoundTrip },
                    "seen codes".takeUnless { seenCodesRoundTrip },
                    "subscription events".takeUnless { eventsRoundTrip },
                    "daily activity".takeUnless { dailyActivityRoundTrip }
                )
                val restoreHealthy = first.skipped == 0 && idempotent && portableSettingsRoundTrip && sourceDataRoundTrip
                LibraryHealthCheck(
                    "Verified restore",
                    "Restored ${first.imported}/${first.processed} entries into an isolated temporary database. " +
                        "Second restore ${if (idempotent) "was idempotent" else "changed the result"}; ${first.skipped} rows skipped. " +
                        "Source platform, sessions and subscriptions ${if (sourceDataRoundTrip) "passed lossless comparison" else "did not restore losslessly (${failedRoundTrips.joinToString()})"}. " +
                        "MangaDex audit: $mangaDexEntries entries, $mangaDexSessions sessions, " +
                        "$mangaDexReaderProgress reader positions, $mangaDexChapterProgress chapter progress rows and " +
                        "$mangaDexSubscriptions subscriptions. " +
                        when {
                            portableSettingsRoundTrip -> "Portable settings also passed a lossless isolated round trip."
                            !portableSettingsPresent -> "This backup does not contain portable settings."
                            else -> "Portable settings failed isolated validation."
                        },
                    when {
                        restoreHealthy -> LibraryHealthLevel.HEALTHY
                        first.skipped > 0 || !idempotent || !sourceDataRoundTrip || portableSettingsPresent -> LibraryHealthLevel.ACTION_REQUIRED
                        else -> LibraryHealthLevel.ATTENTION
                    }
                )
            } finally {
                tempDb.close()
                context.deleteDatabase(tempName)
                context.deleteSharedPreferences(tempPrefsName)
            }
        }.getOrElse {
            LibraryHealthCheck("Verified restore", "Backup validation failed: ${it.message ?: "unknown error"}", LibraryHealthLevel.ACTION_REQUIRED)
        }
    }
}
