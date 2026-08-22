package com.roinur.saucetracker.data.database

import com.roinur.saucetracker.core.media.*
import com.roinur.saucetracker.core.diagnostics.GitHubMediaSession
import com.roinur.saucetracker.core.time.UserCalendar

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.roinur.saucetracker.*
import com.roinur.saucetracker.data.database.dao.CreatorDao
import com.roinur.saucetracker.data.database.dao.EntryDao
import com.roinur.saucetracker.data.database.dao.HeatmapCacheDao
import com.roinur.saucetracker.data.database.dao.HistoryDao
import com.roinur.saucetracker.data.database.dao.SqliteCreatorDao
import com.roinur.saucetracker.data.database.dao.SqliteEntryDao
import com.roinur.saucetracker.data.database.dao.SqliteHeatmapCacheDao
import com.roinur.saucetracker.data.database.dao.SqliteHistoryDao
import com.roinur.saucetracker.data.database.dao.SqliteSubscriptionDao
import com.roinur.saucetracker.data.database.dao.SqliteTagDao
import com.roinur.saucetracker.data.database.dao.SubscriptionDao
import com.roinur.saucetracker.data.database.dao.TagDao
import com.roinur.saucetracker.data.database.entity.RelatedEntryEntity
import com.roinur.saucetracker.feature.heatmap.TrendBucketGranularity
import com.roinur.saucetracker.feature.heatmap.TrendBucketMode
import com.roinur.saucetracker.feature.heatmap.TrendPoint
import com.roinur.saucetracker.feature.heatmap.TrendRequest
import com.roinur.saucetracker.feature.heatmap.TrendSeries
import com.roinur.saucetracker.feature.heatmap.TrendSnapshot
import com.roinur.saucetracker.feature.heatmap.TrendTarget
import com.roinur.saucetracker.feature.heatmap.TrendTargetKind
import com.roinur.saucetracker.feature.heatmap.thirtyDayRateFactor
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class SauceTrackerDatabase(
    private val appContext: Context,
    private val databaseNameOverride: String? = null
) : SQLiteOpenHelper(
    appContext,
    databaseNameOverride ?: GitHubMediaSession.databaseName(),
    null,
    DatabaseSchema.VERSION
) {
    internal val entryDao: EntryDao by lazy { SqliteEntryDao(this) }
    internal val tagDao: TagDao by lazy { SqliteTagDao(this) }
    internal val creatorDao: CreatorDao by lazy { SqliteCreatorDao(this) }
    internal val historyDao: HistoryDao by lazy { SqliteHistoryDao(this) }
    internal val subscriptionDao: SubscriptionDao by lazy { SqliteSubscriptionDao(this) }
    internal val heatmapCacheDao: HeatmapCacheDao by lazy { SqliteHeatmapCacheDao(this) }
    init {
        migrateSchema(writableDatabase)
        if (databaseNameOverride == null) {
            GitHubMediaSession.populateFromProductionIfNeeded(appContext, writableDatabase)
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS entries (
                code INTEGER PRIMARY KEY,
                title TEXT NOT NULL,
                subtitle TEXT NOT NULL DEFAULT '',
                source_url TEXT NOT NULL,
                num_pages INTEGER NOT NULL DEFAULT 0,
                upload_date TEXT NOT NULL DEFAULT '',
                media_id INTEGER NOT NULL DEFAULT 0,
                cover_ext TEXT NOT NULL DEFAULT '',
                rating INTEGER NOT NULL DEFAULT 0,
                read_state INTEGER NOT NULL DEFAULT 0,
                read_at TEXT NOT NULL DEFAULT '',
                pinned INTEGER NOT NULL DEFAULT 0,
                fetched_at TEXT NOT NULL,
                added_at TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                normalized_name TEXT NOT NULL,
                pinned INTEGER NOT NULL DEFAULT 0,
                source_url TEXT NOT NULL DEFAULT '',
                UNIQUE(normalized_name, type)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS entry_tags (
                entry_code INTEGER NOT NULL,
                tag_id INTEGER NOT NULL,
                PRIMARY KEY (entry_code, tag_id),
                FOREIGN KEY (entry_code) REFERENCES entries(code) ON DELETE CASCADE,
                FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS popular_tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                normalized_name TEXT NOT NULL,
                tag_count INTEGER NOT NULL DEFAULT 0,
                blocked INTEGER NOT NULL DEFAULT 0,
                UNIQUE(normalized_name, type)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS daily_read_activity (
                activity_date TEXT PRIMARY KEY,
                pages_read INTEGER NOT NULL DEFAULT 0,
                entries_read INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS reading_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                started_at TEXT NOT NULL,
                ended_at TEXT NOT NULL,
                day_key TEXT NOT NULL,
                entry_code INTEGER NOT NULL,
                pages_viewed INTEGER NOT NULL DEFAULT 0,
                seconds_elapsed INTEGER NOT NULL DEFAULT 0,
                rating INTEGER NOT NULL DEFAULT 0,
                is_reread INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS entry_heatmap_cache (
                slot_id INTEGER PRIMARY KEY CHECK (slot_id = 1),
                cache_key TEXT NOT NULL DEFAULT '',
                payload_json TEXT NOT NULL DEFAULT '',
                updated_at TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS subscriptions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                route_name TEXT NOT NULL,
                route_type TEXT NOT NULL,
                route_key TEXT NOT NULL,
                notifications_enabled INTEGER NOT NULL DEFAULT 1,
                notification_dot_enabled INTEGER NOT NULL DEFAULT 1,
                initialized INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL DEFAULT '',
                last_checked_at TEXT NOT NULL DEFAULT '',
                UNIQUE(route_key)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS subscription_seen_codes (
                subscription_id INTEGER NOT NULL,
                code INTEGER NOT NULL,
                seen_at TEXT NOT NULL DEFAULT '',
                PRIMARY KEY (subscription_id, code),
                FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS subscription_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                subscription_id INTEGER NOT NULL,
                code INTEGER NOT NULL,
                title TEXT NOT NULL DEFAULT '',
                thumbnail_url TEXT NOT NULL DEFAULT '',
                num_pages INTEGER NOT NULL DEFAULT 0,
                upload_date TEXT NOT NULL DEFAULT '',
                source_url TEXT NOT NULL DEFAULT '',
                discovered_at TEXT NOT NULL DEFAULT '',
                dismissed INTEGER NOT NULL DEFAULT 0,
                pinned INTEGER NOT NULL DEFAULT 0,
                UNIQUE(subscription_id, code),
                FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tags_type ON tags(type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tags_name ON tags(name)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_entry_tags_tag_id ON entry_tags(tag_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_popular_tags_type ON popular_tags(type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_popular_tags_name ON popular_tags(name)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_popular_tags_count ON popular_tags(tag_count)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_reading_sessions_day_key ON reading_sessions(day_key)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_reading_sessions_entry_code ON reading_sessions(entry_code)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_subscriptions_route_type ON subscriptions(route_type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_subscription_events_subscription_id ON subscription_events(subscription_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_subscription_events_dismissed ON subscription_events(dismissed, pinned, discovered_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Schema version 1 only.
    }

    private fun migrateSchema(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS popular_tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                normalized_name TEXT NOT NULL,
                tag_count INTEGER NOT NULL DEFAULT 0,
                blocked INTEGER NOT NULL DEFAULT 0,
                UNIQUE(normalized_name, type)
            )
            """.trimIndent()
        )
        if (!hasColumn(db, "popular_tags", "blocked")) {
            db.execSQL("ALTER TABLE popular_tags ADD COLUMN blocked INTEGER NOT NULL DEFAULT 0")
        }
        if (!hasColumn(db, "popular_tags", "tag_count")) {
            db.execSQL("ALTER TABLE popular_tags ADD COLUMN tag_count INTEGER NOT NULL DEFAULT 0")
        }
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_popular_tags_type ON popular_tags(type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_popular_tags_name ON popular_tags(name)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_popular_tags_count ON popular_tags(tag_count)")

        if (!hasColumn(db, "tags", "pinned")) {
            db.execSQL("ALTER TABLE tags ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
        }
        if (!hasColumn(db, "tags", "source_url")) {
            db.execSQL("ALTER TABLE tags ADD COLUMN source_url TEXT NOT NULL DEFAULT ''")
        }
        if (!hasColumn(db, "entries", "media_id")) {
            db.execSQL("ALTER TABLE entries ADD COLUMN media_id INTEGER NOT NULL DEFAULT 0")
        }
        if (!hasColumn(db, "entries", "cover_ext")) {
            db.execSQL("ALTER TABLE entries ADD COLUMN cover_ext TEXT NOT NULL DEFAULT ''")
        }
        if (!hasColumn(db, "entries", "pinned")) {
            db.execSQL("ALTER TABLE entries ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
        }
        if (!hasColumn(db, "entries", "read_state")) {
            db.execSQL("ALTER TABLE entries ADD COLUMN read_state INTEGER NOT NULL DEFAULT 0")
        }
        if (!hasColumn(db, "entries", "read_at")) {
            db.execSQL("ALTER TABLE entries ADD COLUMN read_at TEXT NOT NULL DEFAULT ''")
        }
        db.execSQL(
            """
            UPDATE tags
            SET pinned = CASE
                WHEN pinned IS NULL THEN 0
                WHEN pinned = 0 THEN 0
                ELSE 1
            END
            """.trimIndent()
        )
        db.execSQL("UPDATE tags SET source_url = '' WHERE source_url IS NULL")
        db.execSQL("UPDATE entries SET media_id = 0 WHERE media_id IS NULL")
        db.execSQL("UPDATE entries SET cover_ext = '' WHERE cover_ext IS NULL")
        db.execSQL(
            """
            UPDATE entries
            SET pinned = CASE
                WHEN pinned IS NULL THEN 0
                WHEN pinned = 0 THEN 0
                ELSE 1
            END
            """.trimIndent()
        )
        db.execSQL(
            """
            UPDATE entries
            SET read_state = CASE
                WHEN read_state IS NULL THEN 0
                WHEN read_state = 0 THEN 0
                ELSE 1
            END
            """.trimIndent()
        )
        db.execSQL("UPDATE entries SET read_at = '' WHERE read_at IS NULL")
        db.execSQL(
            """
            UPDATE entries
            SET read_at = COALESCE(NULLIF(added_at, ''), '')
            WHERE COALESCE(read_state, 0) = 1
              AND COALESCE(read_at, '') = ''
            """.trimIndent()
        )
        db.execSQL(
            """
            UPDATE popular_tags
            SET blocked = CASE
                WHEN blocked IS NULL THEN 0
                WHEN blocked = 0 THEN 0
                ELSE 1
            END
            """.trimIndent()
        )
        db.execSQL("UPDATE popular_tags SET tag_count = 0 WHERE tag_count IS NULL")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS daily_read_activity (
                activity_date TEXT PRIMARY KEY,
                pages_read INTEGER NOT NULL DEFAULT 0,
                entries_read INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS reading_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                started_at TEXT NOT NULL,
                ended_at TEXT NOT NULL,
                day_key TEXT NOT NULL,
                entry_code INTEGER NOT NULL,
                pages_viewed INTEGER NOT NULL DEFAULT 0,
                seconds_elapsed INTEGER NOT NULL DEFAULT 0,
                rating INTEGER NOT NULL DEFAULT 0,
                is_reread INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        if (!hasColumn(db, "reading_sessions", "rating")) {
            db.execSQL("ALTER TABLE reading_sessions ADD COLUMN rating INTEGER NOT NULL DEFAULT 0")
        }
        if (!hasColumn(db, "reading_sessions", "is_reread")) {
            db.execSQL("ALTER TABLE reading_sessions ADD COLUMN is_reread INTEGER NOT NULL DEFAULT 0")
        }
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS entry_heatmap_cache (
                slot_id INTEGER PRIMARY KEY CHECK (slot_id = 1),
                cache_key TEXT NOT NULL DEFAULT '',
                payload_json TEXT NOT NULL DEFAULT '',
                updated_at TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS subscriptions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                route_name TEXT NOT NULL,
                route_type TEXT NOT NULL,
                route_key TEXT NOT NULL DEFAULT '',
                notifications_enabled INTEGER NOT NULL DEFAULT 1,
                notification_dot_enabled INTEGER NOT NULL DEFAULT 1,
                initialized INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL DEFAULT '',
                last_checked_at TEXT NOT NULL DEFAULT '',
                UNIQUE(route_key)
            )
            """.trimIndent()
        )
        if (!hasColumn(db, "subscriptions", "route_key")) {
            db.execSQL("ALTER TABLE subscriptions ADD COLUMN route_key TEXT NOT NULL DEFAULT ''")
        }
        if (!hasColumn(db, "subscriptions", "notifications_enabled")) {
            db.execSQL("ALTER TABLE subscriptions ADD COLUMN notifications_enabled INTEGER NOT NULL DEFAULT 1")
        }
        if (!hasColumn(db, "subscriptions", "notification_dot_enabled")) {
            db.execSQL("ALTER TABLE subscriptions ADD COLUMN notification_dot_enabled INTEGER NOT NULL DEFAULT 1")
        }
        if (!hasColumn(db, "subscriptions", "initialized")) {
            db.execSQL("ALTER TABLE subscriptions ADD COLUMN initialized INTEGER NOT NULL DEFAULT 0")
        }
        if (!hasColumn(db, "subscriptions", "created_at")) {
            db.execSQL("ALTER TABLE subscriptions ADD COLUMN created_at TEXT NOT NULL DEFAULT ''")
        }
        if (!hasColumn(db, "subscriptions", "last_checked_at")) {
            db.execSQL("ALTER TABLE subscriptions ADD COLUMN last_checked_at TEXT NOT NULL DEFAULT ''")
        }
        db.execSQL(
            """
            UPDATE subscriptions
            SET route_key = LOWER(COALESCE(route_type, '')) || '|' || LOWER(COALESCE(route_name, ''))
            WHERE COALESCE(route_key, '') = ''
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS subscription_seen_codes (
                subscription_id INTEGER NOT NULL,
                code INTEGER NOT NULL,
                seen_at TEXT NOT NULL DEFAULT '',
                PRIMARY KEY (subscription_id, code),
                FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS subscription_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                subscription_id INTEGER NOT NULL,
                code INTEGER NOT NULL,
                title TEXT NOT NULL DEFAULT '',
                thumbnail_url TEXT NOT NULL DEFAULT '',
                num_pages INTEGER NOT NULL DEFAULT 0,
                upload_date TEXT NOT NULL DEFAULT '',
                source_url TEXT NOT NULL DEFAULT '',
                discovered_at TEXT NOT NULL DEFAULT '',
                dismissed INTEGER NOT NULL DEFAULT 0,
                pinned INTEGER NOT NULL DEFAULT 0,
                UNIQUE(subscription_id, code),
                FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_subscriptions_route_type ON subscriptions(route_type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_subscription_events_subscription_id ON subscription_events(subscription_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_subscription_events_dismissed ON subscription_events(dismissed, pinned, discovered_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_reading_sessions_day_key ON reading_sessions(day_key)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_reading_sessions_entry_code ON reading_sessions(entry_code)")
        backfillDailyReadActivityIfNeeded(db)
    }

    private fun backfillDailyReadActivityIfNeeded(db: SQLiteDatabase) {
        val activityRows = db.rawQuery("SELECT COUNT(*) FROM daily_read_activity", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
        if (activityRows > 0) return

        db.rawQuery(
            """
            SELECT
                substr(COALESCE(NULLIF(read_at, ''), added_at), 1, 10) AS activity_date,
                COALESCE(SUM(CASE WHEN num_pages > 0 THEN num_pages ELSE 0 END), 0) AS pages_read,
                COUNT(*) AS entries_read
            FROM entries
            WHERE COALESCE(read_state, 0) = 1
            GROUP BY activity_date
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxDate = cursor.getColumnIndexOrThrow("activity_date")
            val idxPages = cursor.getColumnIndexOrThrow("pages_read")
            val idxEntries = cursor.getColumnIndexOrThrow("entries_read")
            while (cursor.moveToNext()) {
                val dayKey = normalizeDayKey(cursor.getString(idxDate))
                if (dayKey == null) continue
                upsertDailyReadActivity(
                    db = db,
                    dayKey = dayKey,
                    pagesRead = cursor.getInt(idxPages).coerceAtLeast(0),
                    entriesRead = cursor.getInt(idxEntries).coerceAtLeast(0)
                )
            }
        }
    }

    private fun normalizeDayKey(raw: String?): String? {
        val token = raw?.trim().orEmpty()
        if (token.length >= 10) {
            val candidate = token.substring(0, 10)
            val parsed = runCatching { LocalDate.parse(candidate, UPLOAD_DATE_FORMAT) }.getOrNull()
            if (parsed != null) {
                return parsed.format(UPLOAD_DATE_FORMAT)
            }
        }
        return null
    }

    private fun dayKeyFromTimestamp(timestamp: String?): String {
        return UserCalendar.dayForUtcTimestamp(timestamp, UTC_TIMESTAMP_FORMAT)
            ?.format(UPLOAD_DATE_FORMAT)
            ?: normalizeDayKey(timestamp)
            ?: UserCalendar.today().format(UPLOAD_DATE_FORMAT)
    }

    private fun upsertDailyReadActivity(
        db: SQLiteDatabase,
        dayKey: String,
        pagesRead: Int,
        entriesRead: Int
    ) {
        val values = ContentValues().apply {
            put("activity_date", dayKey)
            put("pages_read", pagesRead.coerceAtLeast(0))
            put("entries_read", entriesRead.coerceAtLeast(0))
        }
        db.insertWithOnConflict("daily_read_activity", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun adjustDailyReadActivity(
        db: SQLiteDatabase,
        dayKey: String,
        pagesDelta: Int,
        entriesDelta: Int
    ) {
        if (pagesDelta == 0 && entriesDelta == 0) return
        db.rawQuery(
            """
            SELECT pages_read, entries_read
            FROM daily_read_activity
            WHERE activity_date = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(dayKey)
        ).use { cursor ->
            val hasRow = cursor.moveToFirst()
            val currentPages = if (hasRow) {
                cursor.getInt(cursor.getColumnIndexOrThrow("pages_read")).coerceAtLeast(0)
            } else {
                0
            }
            val currentEntries = if (hasRow) {
                cursor.getInt(cursor.getColumnIndexOrThrow("entries_read")).coerceAtLeast(0)
            } else {
                0
            }
            val nextPages = (currentPages + pagesDelta).coerceAtLeast(0)
            val nextEntries = (currentEntries + entriesDelta).coerceAtLeast(0)
            if (nextPages == 0 && nextEntries == 0) {
                db.delete("daily_read_activity", "activity_date = ?", arrayOf(dayKey))
            } else {
                upsertDailyReadActivity(
                    db = db,
                    dayKey = dayKey,
                    pagesRead = nextPages,
                    entriesRead = nextEntries
                )
            }
        }
    }

    fun upsertGallery(gallery: GalleryData): Boolean {
        val db = writableDatabase
        val now = utcNowString()
        var insertedNew = false

        db.beginTransaction()
        try {
            val exists = entryExists(db, gallery.code)
            if (exists) {
                val values = ContentValues().apply {
                    put("title", gallery.title)
                    put("subtitle", gallery.subtitle)
                    put("source_url", gallery.sourceUrl)
                    put("num_pages", gallery.numPages.coerceAtLeast(0))
                    put("upload_date", gallery.uploadDate)
                    put("media_id", gallery.mediaId.coerceAtLeast(0L))
                    put("cover_ext", parseCoverExtension(gallery.coverExt))
                    put("fetched_at", now)
                }
                db.update("entries", values, "code = ?", arrayOf(gallery.code.toString()))
            } else {
                insertedNew = true
                val values = ContentValues().apply {
                    put("code", gallery.code)
                    put("title", gallery.title)
                    put("subtitle", gallery.subtitle)
                    put("source_url", gallery.sourceUrl)
                    put("num_pages", gallery.numPages.coerceAtLeast(0))
                    put("upload_date", gallery.uploadDate)
                    put("media_id", gallery.mediaId.coerceAtLeast(0L))
                    put("cover_ext", parseCoverExtension(gallery.coverExt))
                    put("rating", 0)
                    put("read_state", 0)
                    put("read_at", "")
                    put("fetched_at", now)
                    put("added_at", now)
                }
                db.insertOrThrow("entries", null, values)
            }

            val deduped = LinkedHashMap<Pair<String, String>, GalleryTag>()
            gallery.tags.forEach { tag ->
                val normalized = normalizeTagName(tag.name)
                if (normalized.isBlank()) return@forEach
                val type = tag.type.trim().lowercase(Locale.US).ifBlank { "tag" }
                deduped[normalized to type] = GalleryTag(name = tag.name.trim(), type = type)
            }

            // A partial Browser/API response can legitimately contain metadata but no tags.
            // Never let that transient response erase the local entry's complete tag set.
            if (deduped.isNotEmpty()) {
                db.delete("entry_tags", "entry_code = ?", arrayOf(gallery.code.toString()))
                deduped.forEach { (key, tag) ->
                    val normalizedName = key.first
                    val type = key.second

                    val tagValues = ContentValues().apply {
                        put("name", tag.name)
                        put("type", type)
                        put("normalized_name", normalizedName)
                    }
                    db.insertWithOnConflict("tags", null, tagValues, SQLiteDatabase.CONFLICT_IGNORE)

                    val tagId = findTagId(db, normalizedName, type) ?: return@forEach
                    val linkValues = ContentValues().apply {
                        put("entry_code", gallery.code)
                        put("tag_id", tagId)
                    }
                    db.insertWithOnConflict("entry_tags", null, linkValues, SQLiteDatabase.CONFLICT_IGNORE)
                }
            }

            cleanupOrphanTags(db)
            db.delete("entry_heatmap_cache", null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return insertedNew
    }

    fun deleteEntry(code: Int) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("entries", "code = ?", arrayOf(code.toString()))
            cleanupOrphanTags(db)
            db.delete("entry_heatmap_cache", null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun clearAllEntries(): ClearAllResult {
        val db = writableDatabase
        var entryTotal = 0
        var creatorTotal = 0
        db.beginTransaction()
        try {
            db.rawQuery("SELECT COUNT(*) FROM entries", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    entryTotal = cursor.getInt(0)
                }
            }
            db.rawQuery(
                """
                SELECT COUNT(*)
                FROM tags
                WHERE type IN ('artist', 'group')
                """.trimIndent(),
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    creatorTotal = cursor.getInt(0)
                }
            }
            db.delete("entries", null, null)
            db.delete("tags", "type IN ('artist', 'group')", null)
            cleanupOrphanTags(db)
            db.delete("entry_heatmap_cache", null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return ClearAllResult(
            entriesCleared = entryTotal,
            creatorsCleared = creatorTotal
        )
    }

    fun getSavedStats(): SavedStats {
        var entriesTotal = 0
        var artistsTotal = 0
        var groupsTotal = 0
        var readTotal = 0

        readableDatabase.rawQuery("SELECT COUNT(*) FROM entries", null).use { cursor ->
            if (cursor.moveToFirst()) {
                entriesTotal = cursor.getInt(0)
            }
        }
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM entries WHERE COALESCE(read_state, 0) = 1",
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                readTotal = cursor.getInt(0)
            }
        }

        readableDatabase.rawQuery(
            """
            SELECT type, COUNT(*) AS total
            FROM tags
            WHERE type IN ('artist', 'group')
            GROUP BY type
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxType = cursor.getColumnIndexOrThrow("type")
            val idxTotal = cursor.getColumnIndexOrThrow("total")
            while (cursor.moveToNext()) {
                when ((cursor.getString(idxType) ?: "").lowercase(Locale.US)) {
                    "artist" -> artistsTotal = cursor.getInt(idxTotal)
                    "group" -> groupsTotal = cursor.getInt(idxTotal)
                }
            }
        }

        return SavedStats(
            entries = entriesTotal,
            artists = artistsTotal,
            groups = groupsTotal,
            readEntries = readTotal
        )
    }

    fun getReadAnalyticsSnapshot(
        tagLimit: Int = 5,
        creatorLimit: Int = 5
    ): ReadAnalyticsSnapshot {
        backfillDailyReadActivityIfNeeded(writableDatabase)
        val safeTagLimit = tagLimit.coerceIn(1, 20)
        val safeCreatorLimit = creatorLimit.coerceIn(1, 20)
        val readCounts = linkedMapOf<StatsRange, Int>()
        val pagesRead = linkedMapOf<StatsRange, Int>()
        val averageRatings = linkedMapOf<StatsRange, Float>()
        val topTags = linkedMapOf<StatsRange, List<AnalyticsCountRow>>()
        val topCreators = linkedMapOf<StatsRange, List<AnalyticsCountRow>>()
        val dailyActivity = linkedMapOf<StatsRange, List<DailyActivityPoint>>()
        val readingSpeed = linkedMapOf<StatsRange, ReadingSpeedStats>()
        val readBreakdowns = linkedMapOf<StatsRange, ReadCountBreakdown>()

        StatsRange.entries.forEach { range ->
            val breakdown = queryReadCountBreakdown(range)
            readBreakdowns[range] = breakdown
            readCounts[range] = breakdown.total
            pagesRead[range] = queryPagesRead(range)
            averageRatings[range] = queryAverageReadRating(range)
            topTags[range] = queryTopReadTags(range, safeTagLimit)
            topCreators[range] = queryTopReadCreators(range, safeCreatorLimit)
            dailyActivity[range] = queryDailyReadActivity(range)
            readingSpeed[range] = queryReadingSpeedStats(range)
        }

        return ReadAnalyticsSnapshot(
            readCounts = readCounts,
            pagesRead = pagesRead,
            averageRatings = averageRatings,
            topTags = topTags,
            topCreators = topCreators,
            dailyActivity = dailyActivity,
            readingSpeed = readingSpeed,
            readBreakdowns = readBreakdowns
        )
    }

    private fun readDateRange(range: StatsRange): Pair<String, String>? {
        val today = UserCalendar.today()
        val start = when (range) {
            StatsRange.TODAY -> today
            StatsRange.WEEK -> today.minusDays(6)
            StatsRange.MONTH -> today.withDayOfMonth(1)
            StatsRange.YEAR -> today.withDayOfYear(1)
            StatsRange.ALL_TIME -> null
        } ?: return null
        return start.format(UPLOAD_DATE_FORMAT) to today.format(UPLOAD_DATE_FORMAT)
    }

    private fun readRangeClause(range: StatsRange, alias: String = "e"): Pair<String, List<String>> {
        val bounds = readDateRange(range) ?: return "" to emptyList()
        val dateExpr = localCalendarDateSql("COALESCE(NULLIF($alias.read_at, ''), $alias.added_at)")
        return " AND $dateExpr BETWEEN ? AND ?" to listOf(bounds.first, bounds.second)
    }

    private fun queryReadCountBreakdown(range: StatsRange): ReadCountBreakdown {
        val (entryRangeSql, entryRangeArgs) = readRangeClause(range, alias = "e")
        val (sessionRangeSql, sessionRangeArgs) = readRangeClauseForUtcTimestamp(range, "s.started_at")
        val sql = """
            SELECT
                (SELECT COUNT(*) FROM entries e
                 WHERE COALESCE(e.read_state, 0) = 1$entryRangeSql) AS unique_entries,
                (SELECT COUNT(*) FROM reading_sessions s
                 WHERE COALESCE(s.is_reread, 0) = 1$sessionRangeSql) AS rereads
        """.trimIndent()
        readableDatabase.rawQuery(sql, (entryRangeArgs + sessionRangeArgs).toTypedArray()).use { cursor ->
            if (cursor.moveToFirst()) {
                return ReadCountBreakdown(
                    uniqueEntries = cursor.getInt(cursor.getColumnIndexOrThrow("unique_entries")).coerceAtLeast(0),
                    rereads = cursor.getInt(cursor.getColumnIndexOrThrow("rereads")).coerceAtLeast(0)
                )
            }
        }
        return ReadCountBreakdown()
    }

    private fun queryPagesRead(range: StatsRange): Int {
        val (entryRangeSql, entryRangeArgs) = readRangeClause(range, alias = "e")
        val (sessionRangeSql, sessionRangeArgs) = readRangeClauseForUtcTimestamp(range, "s.started_at")
        val sql = """
            SELECT COALESCE(SUM(pages_read), 0)
            FROM (
                SELECT CASE WHEN e.num_pages > 0 THEN e.num_pages ELSE 0 END AS pages_read
                FROM entries e
                WHERE COALESCE(e.read_state, 0) = 1$entryRangeSql
                UNION ALL
                SELECT CASE WHEN s.pages_viewed > 0 THEN s.pages_viewed ELSE 0 END AS pages_read
                FROM reading_sessions s
                WHERE COALESCE(s.is_reread, 0) = 1$sessionRangeSql
            ) reads
        """.trimIndent()
        readableDatabase.rawQuery(sql, (entryRangeArgs + sessionRangeArgs).toTypedArray()).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getInt(0).coerceAtLeast(0)
            }
        }
        return 0
    }

    private fun queryAverageReadRating(range: StatsRange): Float {
        val (rangeSql, rangeArgs) = readRangeClause(range, alias = "e")
        val sql = """
            SELECT AVG(CASE WHEN e.rating > 0 THEN CAST(e.rating AS REAL) END)
            FROM entries e
            WHERE COALESCE(e.read_state, 0) = 1$rangeSql
        """.trimIndent()
        readableDatabase.rawQuery(sql, rangeArgs.toTypedArray()).use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                return cursor.getDouble(0).toFloat().coerceIn(0f, 5f)
            }
        }
        return 0f
    }

    private fun queryTopReadTags(range: StatsRange, limit: Int): List<AnalyticsCountRow> {
        val safeLimit = limit.coerceIn(1, 50)
        val (rangeSql, rangeArgs) = readRangeClause(range, alias = "e")
        val sql = """
            SELECT t.name, t.type, COUNT(*) AS entry_count
            FROM entries e
            JOIN entry_tags et ON et.entry_code = e.code
            JOIN tags t ON t.id = et.tag_id
            WHERE COALESCE(e.read_state, 0) = 1
              AND t.type NOT IN ('artist', 'group')$rangeSql
            GROUP BY t.id, t.name, t.type
            ORDER BY entry_count DESC, LOWER(t.name) ASC
            LIMIT ?
        """.trimIndent()
        val args = rangeArgs.toMutableList().apply { add(safeLimit.toString()) }
        val rows = mutableListOf<AnalyticsCountRow>()
        readableDatabase.rawQuery(sql, args.toTypedArray()).use { cursor ->
            val idxName = cursor.getColumnIndexOrThrow("name")
            val idxType = cursor.getColumnIndexOrThrow("type")
            val idxCount = cursor.getColumnIndexOrThrow("entry_count")
            while (cursor.moveToNext()) {
                rows += AnalyticsCountRow(
                    name = cursor.getString(idxName)?.trim().orEmpty(),
                    type = cursor.getString(idxType)?.trim().orEmpty(),
                    count = cursor.getInt(idxCount).coerceAtLeast(0)
                )
            }
        }
        return rows
    }

    private fun queryTopReadCreators(range: StatsRange, limit: Int): List<AnalyticsCountRow> {
        val safeLimit = limit.coerceIn(1, 50)
        val (rangeSql, rangeArgs) = readRangeClause(range, alias = "e")
        val sql = """
            SELECT t.name, t.type, COUNT(*) AS entry_count
            FROM entries e
            JOIN entry_tags et ON et.entry_code = e.code
            JOIN tags t ON t.id = et.tag_id
            WHERE COALESCE(e.read_state, 0) = 1
              AND t.type IN ('artist', 'group')$rangeSql
            GROUP BY t.id, t.name, t.type
            ORDER BY entry_count DESC, LOWER(t.name) ASC
            LIMIT ?
        """.trimIndent()
        val args = rangeArgs.toMutableList().apply { add(safeLimit.toString()) }
        val rows = mutableListOf<AnalyticsCountRow>()
        readableDatabase.rawQuery(sql, args.toTypedArray()).use { cursor ->
            val idxName = cursor.getColumnIndexOrThrow("name")
            val idxType = cursor.getColumnIndexOrThrow("type")
            val idxCount = cursor.getColumnIndexOrThrow("entry_count")
            while (cursor.moveToNext()) {
                rows += AnalyticsCountRow(
                    name = cursor.getString(idxName)?.trim().orEmpty(),
                    type = cursor.getString(idxType)?.trim().orEmpty(),
                    count = cursor.getInt(idxCount).coerceAtLeast(0)
                )
            }
        }
        return rows
    }

    fun listTrendTargets(kind: TrendTargetKind, includeMisc: Boolean): List<TrendTarget> {
        val typeClause = trendTargetClause(kind, includeMisc)
        val targetIdExpression = trendTargetIdExpression(kind)
        val targetNameExpression = if (kind == TrendTargetKind.CREATORS) "MIN(t.name)" else "t.name"
        val targetTypeExpression = if (kind == TrendTargetKind.CREATORS) "'creator'" else "t.type"
        val targetGroupExpression = trendTargetGroupExpression(kind)
        val sql = """
            SELECT $targetIdExpression AS id, $targetNameExpression AS name,
                   $targetTypeExpression AS type, COUNT(DISTINCT et.entry_code) AS entry_count
            FROM tags t
            JOIN entry_tags et ON et.tag_id = t.id
            WHERE $typeClause
            GROUP BY $targetGroupExpression
            ORDER BY entry_count DESC, LOWER(name) ASC
        """.trimIndent()
        val rows = mutableListOf<TrendTarget>()
        readableDatabase.rawQuery(sql, null).use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow("id")
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val typeIndex = cursor.getColumnIndexOrThrow("type")
            val countIndex = cursor.getColumnIndexOrThrow("entry_count")
            while (cursor.moveToNext()) {
                rows += TrendTarget(
                    id = cursor.getLong(idIndex),
                    name = cursor.getString(nameIndex)?.trim().orEmpty(),
                    type = cursor.getString(typeIndex)?.trim().orEmpty(),
                    entryCount = cursor.getInt(countIndex).coerceAtLeast(0)
                )
            }
        }
        return rows
    }

    fun getTrendSnapshot(request: TrendRequest): TrendSnapshot {
        val targetIds = request.targetIds.asSequence().filter { it > 0L }.distinct().take(5).toList()
        val granularity = trendGranularity(request.range, request.bucketMode)
        if (!request.viewAll && targetIds.isEmpty()) {
            return TrendSnapshot(
                range = request.range,
                granularity = granularity,
                buckets = emptyList(),
                series = emptyList()
            )
        }

        val readEventsCte = """
            WITH read_events AS (
                SELECT 'entry:' || e.code AS event_id,
                       e.code AS entry_code,
                       COALESCE(NULLIF(e.read_at, ''), e.added_at) AS read_timestamp,
                       COALESCE(e.rating, 0) AS rating
                FROM entries e
                WHERE COALESCE(e.read_state, 0) = 1

                UNION ALL

                SELECT 'reread:' || s.id AS event_id,
                       s.entry_code AS entry_code,
                       s.started_at AS read_timestamp,
                       COALESCE(s.rating, 0) AS rating
                FROM reading_sessions s
                JOIN entries e ON e.code = s.entry_code
                WHERE COALESCE(s.is_reread, 0) = 1
            )
        """.trimIndent()
        val timestampExpression = "re.read_timestamp"
        val dateExpression = localCalendarDateSql(timestampExpression)
        val bucketSourceExpression = if (granularity == TrendBucketGranularity.FOUR_HOURS) {
            "datetime($timestampExpression, 'localtime')"
        } else {
            dateExpression
        }
        val bucketExpression = trendBucketSql(bucketSourceExpression, granularity)
        val rangeBounds = readDateRange(request.range)
        val rangeSql = if (rangeBounds == null) "" else " AND $dateExpression BETWEEN ? AND ?"
        val rangeArgs = rangeBounds?.let { listOf(it.first, it.second) }.orEmpty()
        val targetPlaceholders = targetIds.joinToString(",") { "?" }
        val targetIdExpression = trendTargetIdExpression(request.targetKind)
        val targetGroupExpression = trendTargetGroupExpression(request.targetKind)
        val targetFilterSql = if (request.viewAll) {
            trendTargetClause(request.targetKind, request.includeMisc)
        } else {
            "$targetIdExpression IN ($targetPlaceholders)"
        }
        val targetFilterArgs = if (request.viewAll) emptyList() else targetIds.map(Long::toString)

        val totalsByBucket = linkedMapOf<String, Int>()
        val totalsSql = """
            $readEventsCte
            SELECT $bucketExpression AS bucket_key, COUNT(DISTINCT re.event_id) AS total_reads
            FROM read_events re
            WHERE 1 = 1$rangeSql
            GROUP BY bucket_key
            ORDER BY bucket_key ASC
        """.trimIndent()
        readableDatabase.rawQuery(totalsSql, rangeArgs.toTypedArray()).use { cursor ->
            val bucketIndex = cursor.getColumnIndexOrThrow("bucket_key")
            val totalIndex = cursor.getColumnIndexOrThrow("total_reads")
            while (cursor.moveToNext()) {
                val key = cursor.getString(bucketIndex)?.trim().orEmpty()
                if (key.isNotBlank()) totalsByBucket[key] = cursor.getInt(totalIndex).coerceAtLeast(0)
            }
        }

        val targetMetadataSql = """
            SELECT $targetIdExpression AS id,
                   ${if (request.targetKind == TrendTargetKind.CREATORS) "MIN(t.name)" else "t.name"} AS name,
                   ${if (request.targetKind == TrendTargetKind.CREATORS) "'creator'" else "t.type"} AS type,
                   COUNT(DISTINCT et.entry_code) AS entry_count
            FROM tags t
            JOIN entry_tags et ON et.tag_id = t.id
            WHERE $targetFilterSql
            GROUP BY $targetGroupExpression
            ORDER BY entry_count DESC, LOWER(name) ASC
        """.trimIndent()
        val targetOrder = mutableListOf<Long>()
        val targetsById = linkedMapOf<Long, TrendTarget>()
        readableDatabase.rawQuery(targetMetadataSql, targetFilterArgs.toTypedArray()).use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow("id")
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val typeIndex = cursor.getColumnIndexOrThrow("type")
            val countIndex = cursor.getColumnIndexOrThrow("entry_count")
            while (cursor.moveToNext()) {
                val target = TrendTarget(
                    id = cursor.getLong(idIndex),
                    name = cursor.getString(nameIndex)?.trim().orEmpty(),
                    type = cursor.getString(typeIndex)?.trim().orEmpty(),
                    entryCount = cursor.getInt(countIndex).coerceAtLeast(0)
                )
                targetsById[target.id] = target
                targetOrder += target.id
            }
        }
        val pointsByTarget = mutableMapOf<Long, LinkedHashMap<String, TrendPoint>>()
        if (!request.viewAll) {
            targetIds.forEach { pointsByTarget[it] = linkedMapOf() }
        }
        val seriesSql = """
            $readEventsCte
            SELECT
                $targetIdExpression AS target_id,
                $bucketExpression AS bucket_key,
                COUNT(DISTINCT re.event_id) AS matching_reads,
                COUNT(DISTINCT CASE WHEN re.rating >= 4 THEN re.event_id END) AS positive_ratings,
                COUNT(DISTINCT CASE WHEN re.rating > 0 THEN re.event_id END) AS rated_entries,
                SUM(CASE WHEN re.rating > 0 THEN CAST(re.rating AS REAL) ELSE 0 END) AS rating_sum,
                AVG(CASE WHEN re.rating > 0 THEN CAST(re.rating AS REAL) END) AS average_rating,
                COUNT(DISTINCT CASE WHEN re.rating = 1 THEN re.event_id END) AS rating_1_count,
                COUNT(DISTINCT CASE WHEN re.rating = 2 THEN re.event_id END) AS rating_2_count,
                COUNT(DISTINCT CASE WHEN re.rating = 3 THEN re.event_id END) AS rating_3_count,
                COUNT(DISTINCT CASE WHEN re.rating = 4 THEN re.event_id END) AS rating_4_count,
                COUNT(DISTINCT CASE WHEN re.rating = 5 THEN re.event_id END) AS rating_5_count
            FROM read_events re
            JOIN entry_tags et ON et.entry_code = re.entry_code
            JOIN tags t ON t.id = et.tag_id
            WHERE $targetFilterSql$rangeSql
            GROUP BY $targetGroupExpression, bucket_key
            ORDER BY bucket_key ASC
        """.trimIndent()
        val seriesArgs = targetFilterArgs + rangeArgs
        readableDatabase.rawQuery(seriesSql, seriesArgs.toTypedArray()).use { cursor ->
            val targetIndex = cursor.getColumnIndexOrThrow("target_id")
            val bucketIndex = cursor.getColumnIndexOrThrow("bucket_key")
            val matchingIndex = cursor.getColumnIndexOrThrow("matching_reads")
            val positiveIndex = cursor.getColumnIndexOrThrow("positive_ratings")
            val ratedIndex = cursor.getColumnIndexOrThrow("rated_entries")
            val ratingSumIndex = cursor.getColumnIndexOrThrow("rating_sum")
            val averageIndex = cursor.getColumnIndexOrThrow("average_rating")
            val rating1Index = cursor.getColumnIndexOrThrow("rating_1_count")
            val rating2Index = cursor.getColumnIndexOrThrow("rating_2_count")
            val rating3Index = cursor.getColumnIndexOrThrow("rating_3_count")
            val rating4Index = cursor.getColumnIndexOrThrow("rating_4_count")
            val rating5Index = cursor.getColumnIndexOrThrow("rating_5_count")
            while (cursor.moveToNext()) {
                val targetId = cursor.getLong(targetIndex)
                val bucket = cursor.getString(bucketIndex)?.trim().orEmpty()
                if (bucket.isBlank()) continue
                pointsByTarget.getOrPut(targetId) { linkedMapOf() }[bucket] =
                    TrendPoint(
                        bucketKey = bucket,
                        matchingReads = cursor.getInt(matchingIndex).coerceAtLeast(0),
                        totalReads = totalsByBucket[bucket] ?: 0,
                        positiveRatings = cursor.getInt(positiveIndex).coerceAtLeast(0),
                        ratedEntries = cursor.getInt(ratedIndex).coerceAtLeast(0),
                        ratingSum = if (cursor.isNull(ratingSumIndex)) 0f else cursor.getFloat(ratingSumIndex).coerceAtLeast(0f),
                        averageRating = if (cursor.isNull(averageIndex)) 0f else cursor.getFloat(averageIndex).coerceIn(0f, 5f),
                        rating1Count = cursor.getInt(rating1Index).coerceAtLeast(0),
                        rating2Count = cursor.getInt(rating2Index).coerceAtLeast(0),
                        rating3Count = cursor.getInt(rating3Index).coerceAtLeast(0),
                        rating4Count = cursor.getInt(rating4Index).coerceAtLeast(0),
                        rating5Count = cursor.getInt(rating5Index).coerceAtLeast(0)
                    )
            }
        }

        val buckets = continuousTrendBuckets(request.range, granularity, totalsByBucket.keys)
        val earliestReadDate = if (request.range == StatsRange.ALL_TIME) earliestReadCalendarDate() else null
        val resolvedTargetOrder = if (request.viewAll) {
            targetOrder.filter { pointsByTarget.containsKey(it) }
        } else {
            targetIds
        }
        val series = resolvedTargetOrder.mapNotNull { targetId ->
            val target = targetsById[targetId] ?: return@mapNotNull null
            val recorded = pointsByTarget[targetId].orEmpty()
            TrendSeries(
                target = target,
                points = buckets.map { bucket ->
                    (recorded[bucket] ?: TrendPoint(
                        bucketKey = bucket,
                        matchingReads = 0,
                        totalReads = totalsByBucket[bucket] ?: 0,
                        positiveRatings = 0,
                        ratedEntries = 0,
                        ratingSum = 0f,
                        averageRating = 0f,
                        rating1Count = 0,
                        rating2Count = 0,
                        rating3Count = 0,
                        rating4Count = 0,
                        rating5Count = 0
                    )).copy(
                        readNormalizationFactor = trendReadNormalizationFactor(
                            bucket = bucket,
                            granularity = granularity,
                            earliestReadDate = earliestReadDate
                        )
                    )
                }
            )
        }
        return TrendSnapshot(
            range = request.range,
            granularity = granularity,
            buckets = buckets,
            series = series
        )
    }

    private fun trendTargetClause(kind: TrendTargetKind, includeMisc: Boolean): String = when (kind) {
        TrendTargetKind.TAGS -> if (includeMisc) {
            "t.type NOT IN ('artist', 'group')"
        } else {
            "t.type = 'tag'"
        }
        TrendTargetKind.CREATORS -> "t.type IN ('artist', 'group')"
    }

    private fun trendTargetNormalizedNameExpression(alias: String): String =
        "COALESCE(NULLIF($alias.normalized_name, ''), LOWER(TRIM($alias.name)))"

    private fun trendTargetIdExpression(kind: TrendTargetKind): String = when (kind) {
        TrendTargetKind.TAGS -> "t.id"
        TrendTargetKind.CREATORS -> """
            (SELECT MIN(t2.id)
             FROM tags t2
             WHERE t2.type IN ('artist', 'group')
               AND ${trendTargetNormalizedNameExpression("t2")} = ${trendTargetNormalizedNameExpression("t")})
        """.trimIndent()
    }

    private fun trendTargetGroupExpression(kind: TrendTargetKind): String = when (kind) {
        TrendTargetKind.TAGS -> "t.id, t.name, t.type"
        TrendTargetKind.CREATORS -> trendTargetNormalizedNameExpression("t")
    }

    private fun trendGranularity(
        range: StatsRange,
        mode: TrendBucketMode
    ): TrendBucketGranularity {
        if (mode == TrendBucketMode.LEGACY) {
            return when (range) {
                StatsRange.TODAY, StatsRange.WEEK, StatsRange.MONTH -> TrendBucketGranularity.DAY
                StatsRange.YEAR, StatsRange.ALL_TIME -> TrendBucketGranularity.MONTH
            }
        }
        return when (range) {
            StatsRange.TODAY -> TrendBucketGranularity.FOUR_HOURS
            StatsRange.WEEK -> TrendBucketGranularity.DAY
            StatsRange.MONTH -> TrendBucketGranularity.WEEK
            StatsRange.YEAR -> TrendBucketGranularity.MONTH
            StatsRange.ALL_TIME -> adaptiveAllTimeGranularity()
        }
    }

    private fun adaptiveAllTimeGranularity(): TrendBucketGranularity {
        val earliest = earliestReadCalendarDate() ?: return TrendBucketGranularity.MONTH
        val months = ChronoUnit.MONTHS.between(
            YearMonth.from(earliest),
            YearMonth.from(UserCalendar.today())
        ) + 1L
        return when {
            months <= 18L -> TrendBucketGranularity.MONTH
            months <= 48L -> TrendBucketGranularity.QUARTER
            months <= 96L -> TrendBucketGranularity.HALF_YEAR
            else -> TrendBucketGranularity.YEAR
        }
    }

    private fun earliestReadCalendarDate(): LocalDate? {
        val entryDateExpression = localCalendarDateSql("COALESCE(NULLIF(read_at, ''), added_at)")
        val sessionDateExpression = localCalendarDateSql("started_at")
        return readableDatabase.rawQuery(
            """
                SELECT MIN(read_date)
                FROM (
                    SELECT $entryDateExpression AS read_date
                    FROM entries
                    WHERE COALESCE(read_state, 0) = 1
                    UNION ALL
                    SELECT $sessionDateExpression AS read_date
                    FROM reading_sessions
                    WHERE COALESCE(is_reread, 0) = 1
                )
            """.trimIndent(),
            emptyArray()
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            cursor.getString(0)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        }
    }

    private fun trendBucketSql(dateExpression: String, granularity: TrendBucketGranularity): String = when (granularity) {
        TrendBucketGranularity.FOUR_HOURS ->
            "printf('%s %02d', date($dateExpression), (CAST(strftime('%H', $dateExpression) AS INTEGER) / 4) * 4)"
        TrendBucketGranularity.DAY -> "strftime('%Y-%m-%d', $dateExpression)"
        TrendBucketGranularity.WEEK ->
            "date($dateExpression, '-' || ((CAST(strftime('%w', $dateExpression) AS INTEGER) + 6) % 7) || ' days')"
        TrendBucketGranularity.MONTH -> "strftime('%Y-%m', $dateExpression)"
        TrendBucketGranularity.QUARTER ->
            "printf('%04d-Q%d', CAST(strftime('%Y', $dateExpression) AS INTEGER), ((CAST(strftime('%m', $dateExpression) AS INTEGER) - 1) / 3) + 1)"
        TrendBucketGranularity.HALF_YEAR ->
            "printf('%04d-H%d', CAST(strftime('%Y', $dateExpression) AS INTEGER), ((CAST(strftime('%m', $dateExpression) AS INTEGER) - 1) / 6) + 1)"
        TrendBucketGranularity.YEAR -> "strftime('%Y', $dateExpression)"
    }

    private fun continuousTrendBuckets(
        range: StatsRange,
        granularity: TrendBucketGranularity,
        recordedBuckets: Collection<String>
    ): List<String> {
        val today = UserCalendar.today()
        return when (granularity) {
            TrendBucketGranularity.FOUR_HOURS -> (0..20 step 4).map { hour ->
                "${today.format(UPLOAD_DATE_FORMAT)} ${hour.toString().padStart(2, '0')}"
            }
            TrendBucketGranularity.DAY -> {
                val start = when (range) {
                    StatsRange.TODAY -> today
                    StatsRange.WEEK -> today.minusDays(6)
                    StatsRange.MONTH -> today.withDayOfMonth(1)
                    StatsRange.YEAR -> today.withDayOfYear(1)
                    StatsRange.ALL_TIME -> recordedBuckets.minOrNull()?.let(LocalDate::parse) ?: today
                }
                generateSequence(start) { current -> current.plusDays(1).takeIf { it <= today } }
                    .map { it.format(UPLOAD_DATE_FORMAT) }
                    .toList()
            }
            TrendBucketGranularity.WEEK -> {
                val firstOfMonth = today.withDayOfMonth(1)
                val start = firstOfMonth.minusDays(((firstOfMonth.dayOfWeek.value - 1) % 7).toLong())
                generateSequence(start) { current -> current.plusWeeks(1).takeIf { it <= today } }
                    .map { it.format(UPLOAD_DATE_FORMAT) }
                    .toList()
            }
            TrendBucketGranularity.MONTH -> {
                val currentMonth = today.withDayOfMonth(1)
                val start = when (range) {
                    StatsRange.YEAR -> today.withDayOfYear(1).withDayOfMonth(1)
                    StatsRange.ALL_TIME -> recordedBuckets.minOrNull()
                        ?.let { runCatching { LocalDate.parse("$it-01") }.getOrNull() }
                        ?: currentMonth
                    else -> currentMonth
                }
                generateSequence(start) { current -> current.plusMonths(1).takeIf { it <= currentMonth } }
                    .map { it.format(DateTimeFormatter.ofPattern("yyyy-MM", Locale.US)) }
                    .toList()
            }
            TrendBucketGranularity.QUARTER -> {
                val first = recordedBuckets.minOrNull()?.let(::parseQuarterStart)
                    ?: today.withDayOfYear(1)
                val current = LocalDate.of(today.year, ((today.monthValue - 1) / 3) * 3 + 1, 1)
                generateSequence(first) { value -> value.plusMonths(3).takeIf { it <= current } }
                    .map { value -> "${value.year}-Q${((value.monthValue - 1) / 3) + 1}" }
                    .toList()
            }
            TrendBucketGranularity.HALF_YEAR -> {
                val first = recordedBuckets.minOrNull()?.let(::parseHalfYearStart)
                    ?: today.withDayOfYear(1)
                val current = LocalDate.of(today.year, if (today.monthValue <= 6) 1 else 7, 1)
                generateSequence(first) { value -> value.plusMonths(6).takeIf { it <= current } }
                    .map { value -> "${value.year}-H${if (value.monthValue <= 6) 1 else 2}" }
                    .toList()
            }
            TrendBucketGranularity.YEAR -> {
                val firstYear = recordedBuckets.minOrNull()?.toIntOrNull() ?: today.year
                (firstYear..today.year).map(Int::toString)
            }
        }
    }

    private fun parseQuarterStart(key: String): LocalDate? = runCatching {
        val parts = key.split("-Q")
        LocalDate.of(parts[0].toInt(), (parts[1].toInt() - 1) * 3 + 1, 1)
    }.getOrNull()

    private fun parseHalfYearStart(key: String): LocalDate? = runCatching {
        val parts = key.split("-H")
        LocalDate.of(parts[0].toInt(), if (parts[1].toInt() == 1) 1 else 7, 1)
    }.getOrNull()

    private fun trendReadNormalizationFactor(
        bucket: String,
        granularity: TrendBucketGranularity,
        earliestReadDate: LocalDate?
    ): Float {
        val (bucketStart, bucketEndExclusive) = when (granularity) {
            TrendBucketGranularity.QUARTER -> {
                val start = parseQuarterStart(bucket) ?: return 1f
                start to start.plusMonths(3)
            }
            TrendBucketGranularity.HALF_YEAR -> {
                val start = parseHalfYearStart(bucket) ?: return 1f
                start to start.plusMonths(6)
            }
            TrendBucketGranularity.YEAR -> {
                val year = bucket.toIntOrNull() ?: return 1f
                val start = LocalDate.of(year, 1, 1)
                start to start.plusYears(1)
            }
            else -> return 1f
        }
        return thirtyDayRateFactor(
            bucketStart = bucketStart,
            bucketEndExclusive = bucketEndExclusive,
            earliestObservedDate = earliestReadDate,
            today = UserCalendar.today()
        )
    }

    fun getTagGraphDataSnapshot(): TagGraphDataSnapshot {
        val totalEntries = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM entries",
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0).coerceAtLeast(0) else 0
        }
        val totalRatedEntries = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM entries WHERE rating > 0",
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0).coerceAtLeast(0) else 0
        }
        val totalPopularTagUsage = readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(tag_count), 0) FROM popular_tags WHERE type = 'tag'",
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0).coerceAtLeast(0L) else 0L
        }

        data class MutableSeed(
            val name: String,
            val normalizedName: String,
            val localCount: Int,
            val popularCount: Int,
            val ratedSignalSum: Float,
            val ratedMentionCount: Int,
            val entryCodes: MutableList<Int> = mutableListOf()
        )

        data class MutableEntrySeed(
            val code: Int,
            val title: String,
            val thumbnailUrl: String,
            val rating: Int,
            val isRead: Boolean,
            val pinned: Boolean,
            val tagNames: MutableList<String> = mutableListOf()
        )

        val seedsByName = linkedMapOf<String, MutableSeed>()
        val entrySeedsByCode = linkedMapOf<Int, MutableEntrySeed>()
        val statsSql = """
            SELECT
                t.name AS name,
                t.normalized_name AS normalized_name,
                COUNT(DISTINCT et.entry_code) AS local_count,
                COALESCE(MAX(pt.tag_count), 0) AS popular_count,
                COALESCE(SUM(
                    CASE
                        WHEN e.rating = 5 THEN 3.0
                        WHEN e.rating = 4 THEN 2.0
                        WHEN e.rating = 3 THEN 1.0
                        WHEN e.rating = 1 THEN -1.0
                        ELSE 0.0
                    END
                ), 0.0) AS rated_signal_sum,
                COALESCE(SUM(CASE WHEN e.rating > 0 THEN 1 ELSE 0 END), 0) AS rated_mention_count
            FROM tags t
            LEFT JOIN entry_tags et ON et.tag_id = t.id
            LEFT JOIN entries e ON e.code = et.entry_code
            LEFT JOIN popular_tags pt
                ON pt.normalized_name = t.normalized_name
               AND pt.type = 'tag'
            WHERE t.type = 'tag'
            GROUP BY t.id, t.name, t.normalized_name
            HAVING local_count > 0
            ORDER BY local_count DESC, LOWER(t.name) ASC
        """.trimIndent()
        readableDatabase.rawQuery(statsSql, emptyArray()).use { cursor ->
            val idxName = cursor.getColumnIndexOrThrow("name")
            val idxNormalized = cursor.getColumnIndexOrThrow("normalized_name")
            val idxLocal = cursor.getColumnIndexOrThrow("local_count")
            val idxPopular = cursor.getColumnIndexOrThrow("popular_count")
            val idxRatedSignal = cursor.getColumnIndexOrThrow("rated_signal_sum")
            val idxRatedMentions = cursor.getColumnIndexOrThrow("rated_mention_count")
            while (cursor.moveToNext()) {
                val normalizedName = cursor.getString(idxNormalized)?.trim().orEmpty()
                if (normalizedName.isBlank()) continue
                seedsByName[normalizedName] = MutableSeed(
                    name = cursor.getString(idxName)?.trim().orEmpty().ifBlank { normalizedName },
                    normalizedName = normalizedName,
                    localCount = cursor.getInt(idxLocal).coerceAtLeast(0),
                    popularCount = cursor.getInt(idxPopular).coerceAtLeast(0),
                    ratedSignalSum = cursor.getDouble(idxRatedSignal).toFloat(),
                    ratedMentionCount = cursor.getInt(idxRatedMentions).coerceAtLeast(0)
                )
            }
        }

        readableDatabase.rawQuery(
            """
            SELECT code, title, COALESCE(media_id, 0) AS media_id, COALESCE(cover_ext, '') AS cover_ext, rating, read_state, COALESCE(pinned, 0) AS pinned
            FROM entries
            ORDER BY added_at DESC, code DESC
            """.trimIndent(),
            emptyArray()
        ).use { cursor ->
            val idxCode = cursor.getColumnIndexOrThrow("code")
            val idxTitle = cursor.getColumnIndexOrThrow("title")
            val idxMediaId = cursor.getColumnIndexOrThrow("media_id")
            val idxCoverExt = cursor.getColumnIndexOrThrow("cover_ext")
            val idxRating = cursor.getColumnIndexOrThrow("rating")
            val idxRead = cursor.getColumnIndexOrThrow("read_state")
            val idxPinned = cursor.getColumnIndexOrThrow("pinned")
            while (cursor.moveToNext()) {
                val code = cursor.getInt(idxCode).coerceAtLeast(0)
                if (code <= 0) continue
                val title = cursor.getString(idxTitle)?.trim().orEmpty().ifBlank { "Gallery $code" }
                val mediaId = cursor.getLong(idxMediaId).coerceAtLeast(0L)
                val coverExt = parseCoverExtension(cursor.getString(idxCoverExt).orEmpty())
                entrySeedsByCode[code] = MutableEntrySeed(
                    code = code,
                    title = title,
                    thumbnailUrl = buildThumbnailUrl(mediaId, coverExt),
                    rating = cursor.getInt(idxRating).coerceIn(0, 5),
                    isRead = cursor.getInt(idxRead) != 0,
                    pinned = cursor.getInt(idxPinned) != 0
                )
            }
        }

        val linkSql = """
            SELECT t.normalized_name, et.entry_code
            FROM tags t
            JOIN entry_tags et ON et.tag_id = t.id
            WHERE t.type = 'tag'
        """.trimIndent()
        readableDatabase.rawQuery(linkSql, emptyArray()).use { cursor ->
            val idxNormalized = cursor.getColumnIndexOrThrow("normalized_name")
            val idxEntryCode = cursor.getColumnIndexOrThrow("entry_code")
            while (cursor.moveToNext()) {
                val normalizedName = cursor.getString(idxNormalized)?.trim().orEmpty()
                val entryCode = cursor.getInt(idxEntryCode)
                if (normalizedName.isBlank() || entryCode <= 0) continue
                seedsByName[normalizedName]?.entryCodes?.add(entryCode)
                entrySeedsByCode[entryCode]?.tagNames?.add(normalizedName)
            }
        }

        return TagGraphDataSnapshot(
            totalEntries = totalEntries,
            totalRatedEntries = totalRatedEntries,
            totalPopularTagUsage = totalPopularTagUsage,
            seeds = seedsByName.values.map { seed ->
                TagGraphSeed(
                    name = seed.name,
                    normalizedName = seed.normalizedName,
                    localCount = seed.localCount,
                    popularCount = seed.popularCount,
                    ratedSignalSum = seed.ratedSignalSum,
                    ratedMentionCount = seed.ratedMentionCount,
                    entryCodes = seed.entryCodes.distinct().sorted().toIntArray()
                )
            },
            entrySeeds = entrySeedsByCode.values.map { entry ->
                TagGraphEntrySeed(
                    code = entry.code,
                    title = entry.title,
                    thumbnailUrl = entry.thumbnailUrl,
                    rating = entry.rating,
                    isRead = entry.isRead,
                    pinned = entry.pinned,
                    tagNames = entry.tagNames.distinct().sorted()
                )
            }
        )
    }

    private fun localCalendarDateSql(timestampExpr: String): String {
        return "date($timestampExpr, 'localtime')"
    }

    private fun readRangeClauseForUtcTimestamp(
        range: StatsRange,
        timestampExpr: String
    ): Pair<String, List<String>> {
        val bounds = readDateRange(range) ?: return "" to emptyList()
        val dateExpr = localCalendarDateSql(timestampExpr)
        return " AND $dateExpr BETWEEN ? AND ?" to listOf(bounds.first, bounds.second)
    }

    private fun queryDailyReadActivity(range: StatsRange): List<DailyActivityPoint> {
        val (entryRangeSql, entryRangeArgs) = readRangeClause(range, alias = "e")
        val (sessionRangeSql, sessionRangeArgs) = readRangeClauseForUtcTimestamp(range, "s.started_at")
        val entryDateExpr = localCalendarDateSql("COALESCE(NULLIF(e.read_at, ''), e.added_at)")
        val sessionDateExpr = localCalendarDateSql("s.started_at")
        val sql = """
            SELECT activity_date,
                   COALESCE(SUM(pages_read), 0) AS pages_read,
                   COALESCE(SUM(entries_read), 0) AS entries_read
            FROM (
                SELECT $entryDateExpr AS activity_date,
                       CASE WHEN e.num_pages > 0 THEN e.num_pages ELSE 0 END AS pages_read,
                       1 AS entries_read
                FROM entries e
                WHERE COALESCE(e.read_state, 0) = 1$entryRangeSql
                UNION ALL
                SELECT $sessionDateExpr AS activity_date,
                       CASE WHEN s.pages_viewed > 0 THEN s.pages_viewed ELSE 0 END AS pages_read,
                       1 AS entries_read
                FROM reading_sessions s
                WHERE COALESCE(s.is_reread, 0) = 1$sessionRangeSql
            ) activity
            GROUP BY activity_date
            ORDER BY activity_date ASC
        """.trimIndent()
        val rows = mutableListOf<DailyActivityPoint>()
        readableDatabase.rawQuery(sql, (entryRangeArgs + sessionRangeArgs).toTypedArray()).use { cursor ->
            val idxDate = cursor.getColumnIndexOrThrow("activity_date")
            val idxPages = cursor.getColumnIndexOrThrow("pages_read")
            val idxEntries = cursor.getColumnIndexOrThrow("entries_read")
            while (cursor.moveToNext()) {
                val day = normalizeDayKey(cursor.getString(idxDate)) ?: continue
                val parsedDate = runCatching { LocalDate.parse(day, UPLOAD_DATE_FORMAT) }.getOrNull() ?: continue
                rows += DailyActivityPoint(
                    date = parsedDate,
                    pagesRead = cursor.getInt(idxPages).coerceAtLeast(0),
                    entriesRead = cursor.getInt(idxEntries).coerceAtLeast(0)
                )
            }
        }
        return rows
    }

    fun listReadEntriesForDay(day: LocalDate): List<DayReadEntryRow> {
        val dayKey = day.format(UPLOAD_DATE_FORMAT)
        val rows = mutableListOf<DayReadEntryRow>()
        val entryDateExpr = localCalendarDateSql("COALESCE(NULLIF(e.read_at, ''), e.added_at)")
        val sessionDateExpr = localCalendarDateSql("s.started_at")
        val sql = """
            SELECT
                'entry:' || e.code AS row_key,
                e.code,
                e.title,
                COALESCE(e.media_id, 0) AS media_id,
                COALESCE(e.cover_ext, '') AS cover_ext,
                COALESCE(NULLIF(e.read_at, ''), e.added_at, '') AS read_at,
                CASE WHEN e.num_pages > 0 THEN e.num_pages ELSE 0 END AS pages_viewed,
                0 AS seconds_elapsed,
                0 AS session_count,
                0 AS is_reread
            FROM entries e
            WHERE COALESCE(e.read_state, 0) = 1
              AND $entryDateExpr = ?
            UNION ALL
            SELECT
                'session:' || s.id AS row_key,
                e.code,
                e.title,
                COALESCE(e.media_id, 0) AS media_id,
                COALESCE(e.cover_ext, '') AS cover_ext,
                COALESCE(s.ended_at, s.started_at, '') AS read_at,
                CASE WHEN s.pages_viewed > 0 THEN s.pages_viewed ELSE 0 END AS pages_viewed,
                CASE WHEN s.seconds_elapsed > 0 THEN s.seconds_elapsed ELSE 0 END AS seconds_elapsed,
                1 AS session_count,
                1 AS is_reread
            FROM reading_sessions s
            JOIN entries e ON e.code = s.entry_code
            WHERE $sessionDateExpr = ?
              AND COALESCE(s.is_reread, 0) = 1
            ORDER BY read_at DESC, e.code DESC
        """.trimIndent()
        readableDatabase.rawQuery(sql, arrayOf(dayKey, dayKey)).use { cursor ->
            val idxRowKey = cursor.getColumnIndexOrThrow("row_key")
            val idxCode = cursor.getColumnIndexOrThrow("code")
            val idxTitle = cursor.getColumnIndexOrThrow("title")
            val idxMediaId = cursor.getColumnIndexOrThrow("media_id")
            val idxCoverExt = cursor.getColumnIndexOrThrow("cover_ext")
            val idxReadAt = cursor.getColumnIndexOrThrow("read_at")
            val idxPages = cursor.getColumnIndexOrThrow("pages_viewed")
            val idxSeconds = cursor.getColumnIndexOrThrow("seconds_elapsed")
            val idxSessions = cursor.getColumnIndexOrThrow("session_count")
            val idxReread = cursor.getColumnIndexOrThrow("is_reread")
            while (cursor.moveToNext()) {
                val code = cursor.getInt(idxCode)
                val mediaId = cursor.getLong(idxMediaId)
                val coverExt = cursor.getString(idxCoverExt).orEmpty()
                rows += DayReadEntryRow(
                    rowKey = cursor.getString(idxRowKey).orEmpty(),
                    code = code,
                    title = cursor.getString(idxTitle).orEmpty(),
                    thumbnailUrl = buildThumbnailUrl(mediaId, coverExt),
                    readAt = cursor.getString(idxReadAt).orEmpty(),
                    pagesViewed = cursor.getInt(idxPages).coerceAtLeast(0),
                    secondsElapsed = cursor.getLong(idxSeconds).coerceAtLeast(0L),
                    sessionCount = cursor.getInt(idxSessions).coerceAtLeast(0),
                    isReread = cursor.getInt(idxReread) != 0
                )
            }
        }
        return rows
    }

    private fun queryReadingSpeedStats(range: StatsRange): ReadingSpeedStats {
        val (rangeSql, rangeArgs) = readRangeClauseForUtcTimestamp(range, "s.started_at")
        val sql = """
            SELECT
                COALESCE(SUM(CASE WHEN s.pages_viewed > 0 THEN s.pages_viewed ELSE 0 END), 0) AS total_pages_viewed,
                COALESCE(SUM(CASE WHEN s.seconds_elapsed > 0 THEN s.seconds_elapsed ELSE 0 END), 0) AS total_seconds_elapsed
            FROM reading_sessions s
            WHERE 1 = 1$rangeSql
        """.trimIndent()
        readableDatabase.rawQuery(sql, rangeArgs.toTypedArray()).use { cursor ->
            if (cursor.moveToFirst()) {
                val totalPages = cursor.getInt(cursor.getColumnIndexOrThrow("total_pages_viewed")).coerceAtLeast(0)
                val totalSeconds = cursor.getLong(cursor.getColumnIndexOrThrow("total_seconds_elapsed")).coerceAtLeast(0L)
                val pagesPerMinute = if (totalSeconds > 0L) {
                    totalPages.toFloat() / (totalSeconds.toFloat() / 60f)
                } else {
                    0f
                }
                return ReadingSpeedStats(
                    totalPagesViewed = totalPages,
                    totalSecondsElapsed = totalSeconds,
                    pagesPerMinute = pagesPerMinute
                )
            }
        }
        return ReadingSpeedStats()
    }

    fun addCreator(name: String, creatorType: String, sourceUrl: String): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val added = upsertCreatorTag(db, name = name, creatorType = creatorType, sourceUrl = sourceUrl)
            cleanupOrphanTags(db)
            db.setTransactionSuccessful()
            added
        } finally {
            db.endTransaction()
        }
    }

    fun listCreators(
        textFilter: String,
        tagFilterIds: List<Long>,
        sortField: CreatorSortField,
        sortDirection: SortDirection
    ): List<CreatorRow> {
        val filterQuery = buildEntryFilterQuery(textFilter, tagFilterIds)
        val hasEntryFilter = filterQuery.whereClauses.isNotEmpty()

        val orderBy = when (sortField) {
            CreatorSortField.NAME -> "LOWER(t.name) ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, t.type ASC, entry_count DESC"
            CreatorSortField.TYPE -> "t.type ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, LOWER(t.name) ASC, entry_count DESC"
            CreatorSortField.COUNT -> "entry_count ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, t.type ASC, LOWER(t.name) ASC"
        }

        val sql = if (hasEntryFilter) {
            val entryWhere = filterQuery.whereClauses.joinToString(" AND ")
            """
                WITH filtered_entries AS (
                    SELECT e.code
                    FROM entries e
                    WHERE $entryWhere
                )
                SELECT
                    t.id,
                    t.name,
                    t.type,
                    COUNT(DISTINCT fe.code) AS entry_count
                FROM tags t
                LEFT JOIN entry_tags et ON et.tag_id = t.id
                LEFT JOIN filtered_entries fe ON fe.code = et.entry_code
                WHERE t.type IN ('artist', 'group')
                  AND fe.code IS NOT NULL
                GROUP BY t.id, t.name, t.type
                ORDER BY $orderBy
            """.trimIndent()
        } else {
            """
                SELECT
                    t.id,
                    t.name,
                    t.type,
                    COUNT(DISTINCT et.entry_code) AS entry_count
                FROM tags t
                LEFT JOIN entry_tags et ON et.tag_id = t.id
                WHERE t.type IN ('artist', 'group')
                GROUP BY t.id, t.name, t.type
                ORDER BY $orderBy
            """.trimIndent()
        }

        val rows = mutableListOf<CreatorRow>()
        val args = if (hasEntryFilter) filterQuery.args.toTypedArray() else emptyArray()
        readableDatabase.rawQuery(sql, args).use { cursor ->
            val idxId = cursor.getColumnIndexOrThrow("id")
            val idxName = cursor.getColumnIndexOrThrow("name")
            val idxType = cursor.getColumnIndexOrThrow("type")
            val idxCount = cursor.getColumnIndexOrThrow("entry_count")
            while (cursor.moveToNext()) {
                rows += CreatorRow(
                    id = cursor.getLong(idxId),
                    name = cursor.getString(idxName) ?: "",
                    type = cursor.getString(idxType) ?: "",
                    entryCount = cursor.getInt(idxCount)
                )
            }
        }
        return rows
    }

    fun listEntriesForCreator(
        tagId: Long,
        textFilter: String,
        tagFilterIds: List<Long>
    ): List<CreatorEntryRow> {
        val filterQuery = buildEntryFilterQuery(textFilter, tagFilterIds)
        val args = mutableListOf(tagId.toString())
        args += filterQuery.args

        val sql = buildString {
            append(
                """
                SELECT e.code, e.title
                FROM entries e
                JOIN entry_tags et ON et.entry_code = e.code
                WHERE et.tag_id = ?
                """.trimIndent()
            )
            if (filterQuery.whereClauses.isNotEmpty()) {
                append(" AND ")
                append(filterQuery.whereClauses.joinToString(" AND "))
            }
            append(" ORDER BY e.added_at DESC, e.code DESC")
        }

        val rows = mutableListOf<CreatorEntryRow>()
        readableDatabase.rawQuery(sql, args.toTypedArray()).use { cursor ->
            val idxCode = cursor.getColumnIndexOrThrow("code")
            val idxTitle = cursor.getColumnIndexOrThrow("title")
            while (cursor.moveToNext()) {
                val code = cursor.getInt(idxCode)
                val title = cursor.getString(idxTitle)?.trim().orEmpty().ifBlank { "Gallery $code" }
                rows += CreatorEntryRow(code = code, title = title)
            }
        }
        return rows
    }

    fun listAllEntryCodes(): List<Int> {
        val codes = mutableListOf<Int>()
        readableDatabase.rawQuery(
            """
            SELECT code
            FROM entries
            ORDER BY added_at DESC, code DESC
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxCode = cursor.getColumnIndexOrThrow("code")
            while (cursor.moveToNext()) {
                codes += cursor.getInt(idxCode)
            }
        }
        return codes
    }

    fun listSeriesCandidates(): List<SeriesCandidateRow> {
        val rows = mutableListOf<SeriesCandidateRow>()
        readableDatabase.rawQuery(
            """
            SELECT e.code, e.title, e.subtitle, e.media_id, e.cover_ext, e.num_pages,
                   COALESCE(GROUP_CONCAT(t.normalized_name, '|'), '') AS creator_keys
            FROM entries e
            LEFT JOIN entry_tags et ON et.entry_code = e.code
            LEFT JOIN tags t ON t.id = et.tag_id AND t.type IN ('artist', 'group')
            GROUP BY e.code, e.title, e.subtitle, e.media_id, e.cover_ext, e.num_pages
            ORDER BY added_at DESC, code DESC
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxCode = cursor.getColumnIndexOrThrow("code")
            val idxTitle = cursor.getColumnIndexOrThrow("title")
            val idxSubtitle = cursor.getColumnIndexOrThrow("subtitle")
            val idxMediaId = cursor.getColumnIndexOrThrow("media_id")
            val idxCoverExt = cursor.getColumnIndexOrThrow("cover_ext")
            val idxNumPages = cursor.getColumnIndexOrThrow("num_pages")
            val idxCreatorKeys = cursor.getColumnIndexOrThrow("creator_keys")
            while (cursor.moveToNext()) {
                val creatorKeys = cursor.getString(idxCreatorKeys)
                    ?.split('|')
                    ?.asSequence()
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?.toSet()
                    .orEmpty()
                rows += SeriesCandidateRow(
                    code = cursor.getInt(idxCode),
                    title = cursor.getString(idxTitle)?.trim().orEmpty(),
                    subtitle = cursor.getString(idxSubtitle)?.trim().orEmpty(),
                    creatorKeys = creatorKeys,
                    thumbnailUrl = buildThumbnailUrl(
                        cursor.getLong(idxMediaId).coerceAtLeast(0L),
                        parseCoverExtension(cursor.getString(idxCoverExt).orEmpty())
                    ),
                    numPages = cursor.getInt(idxNumPages).coerceAtLeast(0)
                )
            }
        }
        return rows
    }

    fun listRelatedEntryPreviews(code: Int, limit: Int): List<RelatedEntryEntity> {
        if (code <= 0 || limit <= 0) return emptyList()
        val rows = mutableListOf<RelatedEntryEntity>()
        readableDatabase.rawQuery(
            """
            WITH current_tags AS (
                SELECT t.id, t.type
                FROM entry_tags current_et
                JOIN tags t ON t.id = current_et.tag_id
                WHERE current_et.entry_code = ?
                  AND t.type NOT IN ('artist', 'group')
            )
            SELECT e.code, e.title, e.subtitle, e.num_pages, e.media_id, e.cover_ext,
                   SUM(
                       CASE current_tags.type
                           WHEN 'tag' THEN 100
                           WHEN 'parody' THEN 42
                           WHEN 'character' THEN 34
                           WHEN 'category' THEN 24
                           WHEN 'language' THEN 42
                           ELSE 28
                       END
                   ) AS similarity_score,
                   COUNT(*) AS shared_tag_count
            FROM current_tags
            JOIN entry_tags candidate_et ON candidate_et.tag_id = current_tags.id
            JOIN entries e ON e.code = candidate_et.entry_code
            WHERE e.code != ?
            GROUP BY e.code, e.title, e.subtitle, e.num_pages, e.media_id, e.cover_ext, e.added_at
            ORDER BY similarity_score DESC,
                     shared_tag_count DESC,
                     ABS(e.num_pages - COALESCE((SELECT num_pages FROM entries WHERE code = ?), e.num_pages)) ASC,
                     e.added_at DESC,
                     e.code DESC
            LIMIT ?
            """.trimIndent(),
            arrayOf(code.toString(), code.toString(), code.toString(), limit.coerceAtMost(200).toString())
        ).use { cursor ->
            val idxCode = cursor.getColumnIndexOrThrow("code")
            val idxTitle = cursor.getColumnIndexOrThrow("title")
            val idxSubtitle = cursor.getColumnIndexOrThrow("subtitle")
            val idxPages = cursor.getColumnIndexOrThrow("num_pages")
            val idxMediaId = cursor.getColumnIndexOrThrow("media_id")
            val idxCoverExt = cursor.getColumnIndexOrThrow("cover_ext")
            while (cursor.moveToNext()) {
                val entryCode = cursor.getInt(idxCode)
                val mediaId = cursor.getLong(idxMediaId).coerceAtLeast(0L)
                val coverExt = parseCoverExtension(cursor.getString(idxCoverExt).orEmpty())
                rows += RelatedEntryEntity(
                    code = entryCode,
                    title = cursor.getString(idxTitle)?.trim().orEmpty().ifBlank { "Gallery $entryCode" },
                    subtitle = cursor.getString(idxSubtitle)?.trim().orEmpty(),
                    thumbnailUrl = buildThumbnailUrl(mediaId, coverExt),
                    numPages = cursor.getInt(idxPages).coerceAtLeast(0)
                )
            }
        }
        return rows
    }

    fun listSameArtistEntryPreviews(code: Int, limit: Int): List<RelatedEntryEntity> {
        if (code <= 0 || limit <= 0) return emptyList()
        val rows = mutableListOf<RelatedEntryEntity>()
        readableDatabase.rawQuery(
            """
            WITH current_artists AS (
                SELECT current_et.tag_id
                FROM entry_tags current_et
                JOIN tags t ON t.id = current_et.tag_id
                WHERE current_et.entry_code = ?
                  AND t.type = 'artist'
            )
            SELECT e.code, e.title, e.subtitle, e.num_pages, e.media_id, e.cover_ext,
                   COUNT(*) AS shared_artist_count
            FROM current_artists
            JOIN entry_tags candidate_et ON candidate_et.tag_id = current_artists.tag_id
            JOIN entries e ON e.code = candidate_et.entry_code
            WHERE e.code != ?
            GROUP BY e.code, e.title, e.subtitle, e.num_pages, e.media_id, e.cover_ext, e.added_at
            ORDER BY shared_artist_count DESC, e.added_at DESC, e.code DESC
            LIMIT ?
            """.trimIndent(),
            arrayOf(code.toString(), code.toString(), limit.coerceAtMost(200).toString())
        ).use { cursor ->
            val idxCode = cursor.getColumnIndexOrThrow("code")
            val idxTitle = cursor.getColumnIndexOrThrow("title")
            val idxSubtitle = cursor.getColumnIndexOrThrow("subtitle")
            val idxPages = cursor.getColumnIndexOrThrow("num_pages")
            val idxMediaId = cursor.getColumnIndexOrThrow("media_id")
            val idxCoverExt = cursor.getColumnIndexOrThrow("cover_ext")
            while (cursor.moveToNext()) {
                val entryCode = cursor.getInt(idxCode)
                val mediaId = cursor.getLong(idxMediaId).coerceAtLeast(0L)
                val coverExt = parseCoverExtension(cursor.getString(idxCoverExt).orEmpty())
                rows += RelatedEntryEntity(
                    code = entryCode,
                    title = cursor.getString(idxTitle)?.trim().orEmpty().ifBlank { "Gallery $entryCode" },
                    subtitle = cursor.getString(idxSubtitle)?.trim().orEmpty(),
                    thumbnailUrl = buildThumbnailUrl(mediaId, coverExt),
                    numPages = cursor.getInt(idxPages).coerceAtLeast(0)
                )
            }
        }
        return rows
    }

    fun listSubscriptions(): List<SubscriptionRow> {
        val rows = mutableListOf<SubscriptionRow>()
        readableDatabase.rawQuery(
            """
            SELECT id, route_name, route_type,
                   COALESCE(notifications_enabled, 1) AS notifications_enabled,
                   COALESCE(notification_dot_enabled, 1) AS notification_dot_enabled,
                   COALESCE(initialized, 0) AS initialized,
                   COALESCE(created_at, '') AS created_at,
                   COALESCE(last_checked_at, '') AS last_checked_at
            FROM subscriptions
            ORDER BY LOWER(route_type) ASC, LOWER(route_name) ASC
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxId = cursor.getColumnIndexOrThrow("id")
            val idxName = cursor.getColumnIndexOrThrow("route_name")
            val idxType = cursor.getColumnIndexOrThrow("route_type")
            val idxNotifications = cursor.getColumnIndexOrThrow("notifications_enabled")
            val idxDot = cursor.getColumnIndexOrThrow("notification_dot_enabled")
            val idxInitialized = cursor.getColumnIndexOrThrow("initialized")
            val idxCreatedAt = cursor.getColumnIndexOrThrow("created_at")
            val idxLastCheckedAt = cursor.getColumnIndexOrThrow("last_checked_at")
            while (cursor.moveToNext()) {
                rows += SubscriptionRow(
                    id = cursor.getLong(idxId),
                    routeName = cursor.getString(idxName)?.trim().orEmpty(),
                    routeType = cursor.getString(idxType)?.trim().orEmpty(),
                    notificationsEnabled = cursor.getInt(idxNotifications) != 0,
                    notificationDotEnabled = cursor.getInt(idxDot) != 0,
                    initialized = cursor.getInt(idxInitialized) != 0,
                    createdAt = cursor.getString(idxCreatedAt).orEmpty(),
                    lastCheckedAt = cursor.getString(idxLastCheckedAt).orEmpty()
                )
            }
        }
        return rows
    }

    fun upsertSubscription(routeType: String, routeName: String): SubscriptionRow? {
        val normalizedType = normalizeSubscriptionRouteType(routeType)
        val normalizedName = normalizeSubscriptionRouteName(normalizedType, routeName)
        val routeKey = subscriptionRouteKey(normalizedType, normalizedName)
        if (normalizedType.isBlank() || normalizedName.isBlank() || routeKey.isBlank()) return null
        val now = utcNowString()
        val values = ContentValues().apply {
            put("route_name", normalizedName)
            put("route_type", normalizedType)
            put("route_key", routeKey)
            put("created_at", now)
        }
        val db = writableDatabase
        db.insertWithOnConflict("subscriptions", null, values, SQLiteDatabase.CONFLICT_IGNORE)
        return findSubscriptionByKey(routeKey)
    }

    fun removeSubscription(subscriptionId: Long) {
        if (subscriptionId <= 0L) return
        writableDatabase.delete("subscriptions", "id = ?", arrayOf(subscriptionId.toString()))
    }

    fun updateSubscriptionSettings(
        subscriptionId: Long,
        notificationsEnabled: Boolean,
        notificationDotEnabled: Boolean
    ) {
        if (subscriptionId <= 0L) return
        val values = ContentValues().apply {
            put("notifications_enabled", if (notificationsEnabled) 1 else 0)
            put("notification_dot_enabled", if (notificationDotEnabled) 1 else 0)
        }
        writableDatabase.update("subscriptions", values, "id = ?", arrayOf(subscriptionId.toString()))
    }

    fun markSubscriptionInitialized(subscriptionId: Long) {
        if (subscriptionId <= 0L) return
        val values = ContentValues().apply {
            put("initialized", 1)
            put("last_checked_at", utcNowString())
        }
        writableDatabase.update("subscriptions", values, "id = ?", arrayOf(subscriptionId.toString()))
    }

    fun markSubscriptionChecked(subscriptionId: Long) {
        if (subscriptionId <= 0L) return
        val values = ContentValues().apply {
            put("last_checked_at", utcNowString())
        }
        writableDatabase.update("subscriptions", values, "id = ?", arrayOf(subscriptionId.toString()))
    }

    fun addSeenCodesForSubscription(subscriptionId: Long, codes: Collection<Int>) {
        if (subscriptionId <= 0L || codes.isEmpty()) return
        val db = writableDatabase
        val now = utcNowString()
        db.beginTransaction()
        try {
            codes.asSequence()
                .filter { it > 0 }
                .distinct()
                .forEach { code ->
                    val values = ContentValues().apply {
                        put("subscription_id", subscriptionId)
                        put("code", code)
                        put("seen_at", now)
                    }
                    db.insertWithOnConflict("subscription_seen_codes", null, values, SQLiteDatabase.CONFLICT_IGNORE)
                }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun listSeenCodesForSubscription(subscriptionId: Long): Set<Int> {
        if (subscriptionId <= 0L) return emptySet()
        val codes = linkedSetOf<Int>()
        readableDatabase.rawQuery(
            """
            SELECT code
            FROM subscription_seen_codes
            WHERE subscription_id = ?
            """.trimIndent(),
            arrayOf(subscriptionId.toString())
        ).use { cursor ->
            val idxCode = cursor.getColumnIndexOrThrow("code")
            while (cursor.moveToNext()) {
                val code = cursor.getInt(idxCode)
                if (code > 0) codes += code
            }
        }
        return codes
    }

    fun insertSubscriptionEvents(subscriptionId: Long, galleries: List<GalleryData>): Int {
        if (subscriptionId <= 0L || galleries.isEmpty()) return 0
        val db = writableDatabase
        val discoveredAt = utcNowString()
        var inserted = 0
        db.beginTransaction()
        try {
            galleries.forEach { gallery ->
                if (gallery.code <= 0) return@forEach
                val values = ContentValues().apply {
                    put("subscription_id", subscriptionId)
                    put("code", gallery.code)
                    put("title", gallery.title)
                    put("thumbnail_url", buildGalleryCoverThumbnailUrl(gallery))
                    put("num_pages", gallery.numPages.coerceAtLeast(0))
                    put("upload_date", gallery.uploadDate)
                    put("source_url", gallery.sourceUrl)
                    put("discovered_at", discoveredAt)
                }
                val rowId = db.insertWithOnConflict("subscription_events", null, values, SQLiteDatabase.CONFLICT_IGNORE)
                if (rowId != -1L) {
                    inserted += 1
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return inserted
    }

    fun listSubscriptionEvents(includeDismissed: Boolean = false): List<SubscriptionEventRow> {
        val rows = mutableListOf<SubscriptionEventRow>()
        val whereClause = if (includeDismissed) {
            ""
        } else {
            """
            WHERE COALESCE(e.dismissed, 0) = 0
              AND NOT EXISTS (SELECT 1 FROM entries imported WHERE imported.code = e.code)
            """.trimIndent()
        }
        readableDatabase.rawQuery(
            """
            SELECT
                e.id,
                e.subscription_id,
                s.route_name,
                s.route_type,
                e.code,
                e.title,
                COALESCE(e.thumbnail_url, '') AS thumbnail_url,
                COALESCE(e.num_pages, 0) AS num_pages,
                COALESCE(e.upload_date, '') AS upload_date,
                COALESCE(e.source_url, '') AS source_url,
                COALESCE(e.discovered_at, '') AS discovered_at,
                COALESCE(e.dismissed, 0) AS dismissed,
                COALESCE(e.pinned, 0) AS pinned
            FROM subscription_events e
            JOIN subscriptions s ON s.id = e.subscription_id
            $whereClause
            ORDER BY COALESCE(e.pinned, 0) DESC, COALESCE(e.discovered_at, '') DESC, e.id DESC
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxId = cursor.getColumnIndexOrThrow("id")
            val idxSubscriptionId = cursor.getColumnIndexOrThrow("subscription_id")
            val idxRouteName = cursor.getColumnIndexOrThrow("route_name")
            val idxRouteType = cursor.getColumnIndexOrThrow("route_type")
            val idxCode = cursor.getColumnIndexOrThrow("code")
            val idxTitle = cursor.getColumnIndexOrThrow("title")
            val idxThumb = cursor.getColumnIndexOrThrow("thumbnail_url")
            val idxPages = cursor.getColumnIndexOrThrow("num_pages")
            val idxUpload = cursor.getColumnIndexOrThrow("upload_date")
            val idxSourceUrl = cursor.getColumnIndexOrThrow("source_url")
            val idxDiscoveredAt = cursor.getColumnIndexOrThrow("discovered_at")
            val idxDismissed = cursor.getColumnIndexOrThrow("dismissed")
            val idxPinned = cursor.getColumnIndexOrThrow("pinned")
            while (cursor.moveToNext()) {
                rows += SubscriptionEventRow(
                    id = cursor.getLong(idxId),
                    subscriptionId = cursor.getLong(idxSubscriptionId),
                    routeName = cursor.getString(idxRouteName)?.trim().orEmpty(),
                    routeType = cursor.getString(idxRouteType)?.trim().orEmpty(),
                    code = cursor.getInt(idxCode),
                    title = cursor.getString(idxTitle)?.trim().orEmpty(),
                    thumbnailUrl = cursor.getString(idxThumb).orEmpty(),
                    numPages = cursor.getInt(idxPages).coerceAtLeast(0),
                    uploadDate = cursor.getString(idxUpload).orEmpty(),
                    sourceUrl = cursor.getString(idxSourceUrl).orEmpty(),
                    discoveredAt = cursor.getString(idxDiscoveredAt).orEmpty(),
                    dismissed = cursor.getInt(idxDismissed) != 0,
                    pinned = cursor.getInt(idxPinned) != 0
                )
            }
        }
        return rows
    }

    fun dismissSubscriptionEvent(eventId: Long) {
        if (eventId <= 0L) return
        val values = ContentValues().apply {
            put("dismissed", 1)
        }
        writableDatabase.update("subscription_events", values, "id = ?", arrayOf(eventId.toString()))
    }

    fun toggleSubscriptionEventPinned(eventId: Long) {
        if (eventId <= 0L) return
        writableDatabase.execSQL(
            """
            UPDATE subscription_events
            SET pinned = CASE WHEN COALESCE(pinned, 0) = 0 THEN 1 ELSE 0 END
            WHERE id = ?
            """.trimIndent(),
            arrayOf(eventId)
        )
    }

    fun countSubscriptionEventsForBadge(): Int {
        readableDatabase.rawQuery(
            """
            SELECT COUNT(*)
            FROM subscription_events e
            JOIN subscriptions s ON s.id = e.subscription_id
            WHERE COALESCE(e.dismissed, 0) = 0
              AND COALESCE(s.notification_dot_enabled, 1) = 1
              AND NOT EXISTS (SELECT 1 FROM entries imported WHERE imported.code = e.code)
            """.trimIndent(),
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getInt(0).coerceAtLeast(0)
            }
        }
        return 0
    }

    fun countSubscriptionEventsForNotification(): Int {
        readableDatabase.rawQuery(
            """
            SELECT COUNT(*)
            FROM subscription_events e
            JOIN subscriptions s ON s.id = e.subscription_id
            WHERE COALESCE(e.dismissed, 0) = 0
              AND COALESCE(s.notifications_enabled, 1) = 1
              AND NOT EXISTS (SELECT 1 FROM entries imported WHERE imported.code = e.code)
            """.trimIndent(),
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getInt(0).coerceAtLeast(0)
            }
        }
        return 0
    }

    fun findSubscription(routeType: String, routeName: String): SubscriptionRow? {
        val routeKey = subscriptionRouteKey(routeType, routeName)
        if (routeKey.isBlank()) return null
        return findSubscriptionByKey(routeKey)
    }

    private fun findSubscriptionByKey(routeKey: String): SubscriptionRow? {
        if (routeKey.isBlank()) return null
        readableDatabase.rawQuery(
            """
            SELECT id, route_name, route_type,
                   COALESCE(notifications_enabled, 1) AS notifications_enabled,
                   COALESCE(notification_dot_enabled, 1) AS notification_dot_enabled,
                   COALESCE(initialized, 0) AS initialized,
                   COALESCE(created_at, '') AS created_at,
                   COALESCE(last_checked_at, '') AS last_checked_at
            FROM subscriptions
            WHERE route_key = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(routeKey)
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return SubscriptionRow(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                routeName = cursor.getString(cursor.getColumnIndexOrThrow("route_name"))?.trim().orEmpty(),
                routeType = cursor.getString(cursor.getColumnIndexOrThrow("route_type"))?.trim().orEmpty(),
                notificationsEnabled = cursor.getInt(cursor.getColumnIndexOrThrow("notifications_enabled")) != 0,
                notificationDotEnabled = cursor.getInt(cursor.getColumnIndexOrThrow("notification_dot_enabled")) != 0,
                initialized = cursor.getInt(cursor.getColumnIndexOrThrow("initialized")) != 0,
                createdAt = cursor.getString(cursor.getColumnIndexOrThrow("created_at")).orEmpty(),
                lastCheckedAt = cursor.getString(cursor.getColumnIndexOrThrow("last_checked_at")).orEmpty()
            )
        }
    }

    fun setEntryRating(code: Int, rating: Int) {
        val safeRating = rating.coerceIn(0, 5)
        val values = ContentValues().apply {
            put("rating", safeRating)
        }
        writableDatabase.update("entries", values, "code = ?", arrayOf(code.toString()))
    }

    fun setEntryRead(code: Int, isRead: Boolean) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.rawQuery(
                """
                SELECT
                    COALESCE(read_state, 0) AS read_state,
                    COALESCE(read_at, '') AS read_at,
                    COALESCE(num_pages, 0) AS num_pages
                FROM entries
                WHERE code = ?
                LIMIT 1
                """.trimIndent(),
                arrayOf(code.toString())
            ).use { cursor ->
                if (!cursor.moveToFirst()) return@use
                val wasRead = cursor.getInt(cursor.getColumnIndexOrThrow("read_state")) != 0
                val previousReadAt = cursor.getString(cursor.getColumnIndexOrThrow("read_at")).orEmpty()
                val numPages = cursor.getInt(cursor.getColumnIndexOrThrow("num_pages")).coerceAtLeast(0)

                val transitioned = wasRead != isRead
                val resolvedReadAt = when {
                    isRead && previousReadAt.isBlank() -> utcNowString()
                    isRead -> previousReadAt
                    else -> ""
                }

                if (transitioned || (isRead && previousReadAt.isBlank())) {
                    val values = ContentValues().apply {
                        put("read_state", if (isRead) 1 else 0)
                        put("read_at", resolvedReadAt)
                    }
                    db.update("entries", values, "code = ?", arrayOf(code.toString()))
                }

                if (transitioned) {
                    // Keep daily aggregates reversible so read/unread toggles stay accurate over time.
                    if (isRead) {
                        val dayKey = dayKeyFromTimestamp(resolvedReadAt)
                        adjustDailyReadActivity(
                            db = db,
                            dayKey = dayKey,
                            pagesDelta = numPages,
                            entriesDelta = 1
                        )
                    } else {
                        val dayKey = dayKeyFromTimestamp(previousReadAt)
                        adjustDailyReadActivity(
                            db = db,
                            dayKey = dayKey,
                            pagesDelta = -numPages,
                            entriesDelta = -1
                        )
                    }
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun insertReadingSession(
        entryCode: Int,
        startedAtMillisUtc: Long,
        endedAtMillisUtc: Long,
        pagesViewed: Int,
        secondsElapsed: Long,
        rating: Int = 0,
        isReread: Boolean = false
    ) {
        if (entryCode <= 0) return
        val startMs = startedAtMillisUtc.coerceAtLeast(0L)
        val endMs = endedAtMillisUtc.coerceAtLeast(startMs)
        val safeSeconds = secondsElapsed.coerceAtLeast(1L)
        val safePagesViewed = pagesViewed.coerceAtLeast(1)
        val safeRating = rating.coerceIn(0, 5)
        val startedAt = Instant.ofEpochMilli(startMs)
            .atOffset(ZoneOffset.UTC)
            .toLocalDateTime()
            .format(UTC_TIMESTAMP_FORMAT)
        val endedAt = Instant.ofEpochMilli(endMs)
            .atOffset(ZoneOffset.UTC)
            .toLocalDateTime()
            .format(UTC_TIMESTAMP_FORMAT)
        val dayKey = UserCalendar.dayForInstant(Instant.ofEpochMilli(startMs))
            .format(UPLOAD_DATE_FORMAT)

        val values = ContentValues().apply {
            put("started_at", startedAt)
            put("ended_at", endedAt)
            put("day_key", dayKey)
            put("entry_code", entryCode)
            put("pages_viewed", safePagesViewed)
            put("seconds_elapsed", safeSeconds)
            put("rating", safeRating)
            put("is_reread", if (isReread) 1 else 0)
        }
        writableDatabase.insert("reading_sessions", null, values)
    }

    fun recordEntryRatingSession(code: Int, rating: Int, isReread: Boolean) {
        if (code <= 0) return
        val now = System.currentTimeMillis()
        insertReadingSession(
            entryCode = code,
            startedAtMillisUtc = now,
            endedAtMillisUtc = now,
            pagesViewed = 1,
            secondsElapsed = 1L,
            rating = rating.coerceIn(0, 5),
            isReread = isReread
        )
    }

    fun updateRatingHistoryRow(code: Int, row: EntryRatingHistoryRow, rating: Int) {
        if (code <= 0) return
        val safeRating = rating.coerceIn(0, 5)
        if (row.isEntrySummary) {
            setEntryRating(code, safeRating)
            setEntryRead(code, true)
            return
        }
        val sessionId = row.sessionId ?: return
        val values = ContentValues().apply {
            put("rating", safeRating)
        }
        writableDatabase.update(
            "reading_sessions",
            values,
            "id = ? AND entry_code = ?",
            arrayOf(sessionId.toString(), code.toString())
        )
    }

    fun deleteRatingHistoryRow(code: Int, row: EntryRatingHistoryRow) {
        if (code <= 0) return
        if (row.isEntrySummary) {
            setEntryRating(code, 0)
            setEntryRead(code, false)
            return
        }
        val sessionId = row.sessionId ?: return
        writableDatabase.delete(
            "reading_sessions",
            "id = ? AND entry_code = ?",
            arrayOf(sessionId.toString(), code.toString())
        )
    }

    fun getEntryRatingHistory(code: Int): List<EntryRatingHistoryRow> {
        if (code <= 0) return emptyList()
        val rows = mutableListOf<EntryRatingHistoryRow>()
        readableDatabase.rawQuery(
            """
            SELECT
                COALESCE(read_at, '') AS read_at,
                COALESCE(rating, 0) AS rating,
                COALESCE(num_pages, 0) AS num_pages
            FROM entries
            WHERE code = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(code.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                val readAt = cursor.getString(cursor.getColumnIndexOrThrow("read_at")).orEmpty()
                val rating = cursor.getInt(cursor.getColumnIndexOrThrow("rating")).coerceIn(0, 5)
                if (readAt.isNotBlank() || rating > 0) {
                    rows += EntryRatingHistoryRow(
                        sessionId = null,
                        readAt = readAt.ifBlank { "-" },
                        rating = rating,
                        isReread = false,
                        isEntrySummary = true,
                        pagesViewed = cursor.getInt(cursor.getColumnIndexOrThrow("num_pages")).coerceAtLeast(0),
                        secondsElapsed = 0L
                    )
                }
            }
        }
        readableDatabase.rawQuery(
            """
            SELECT
                id,
                COALESCE(ended_at, started_at) AS read_at,
                COALESCE(rating, 0) AS rating,
                COALESCE(is_reread, 0) AS is_reread,
                COALESCE(pages_viewed, 0) AS pages_viewed,
                COALESCE(seconds_elapsed, 0) AS seconds_elapsed
            FROM reading_sessions
            WHERE entry_code = ?
            ORDER BY read_at ASC, id ASC
            """.trimIndent(),
            arrayOf(code.toString())
        ).use { cursor ->
            val idxId = cursor.getColumnIndexOrThrow("id")
            val idxReadAt = cursor.getColumnIndexOrThrow("read_at")
            val idxRating = cursor.getColumnIndexOrThrow("rating")
            val idxIsReread = cursor.getColumnIndexOrThrow("is_reread")
            val idxPages = cursor.getColumnIndexOrThrow("pages_viewed")
            val idxSeconds = cursor.getColumnIndexOrThrow("seconds_elapsed")
            while (cursor.moveToNext()) {
                val readAt = cursor.getString(idxReadAt).orEmpty()
                val rating = cursor.getInt(idxRating).coerceIn(0, 5)
                if (rating <= 0) continue
                rows += EntryRatingHistoryRow(
                    sessionId = cursor.getLong(idxId),
                    readAt = readAt.ifBlank { "-" },
                    rating = rating,
                    isReread = cursor.getInt(idxIsReread) != 0,
                    isEntrySummary = false,
                    pagesViewed = cursor.getInt(idxPages).coerceAtLeast(0),
                    secondsElapsed = cursor.getLong(idxSeconds).coerceAtLeast(0L)
                )
            }
        }
        val distinctRows = rows.distinctBy { listOf(it.sessionId, it.readAt, it.rating, it.isReread, it.isEntrySummary) }
        val hasEntryFirstReadSummary = distinctRows.any { !it.isReread && it.secondsElapsed == 0L }
        return distinctRows
            .filter { row -> row.isReread || !hasEntryFirstReadSummary || row.secondsElapsed == 0L }
            .sortedBy { it.readAt }
    }

    fun getAverageEntryRating(code: Int): Float? {
        val ratings = getEntryRatingHistory(code)
            .map { it.rating }
            .filter { it > 0 }
        if (ratings.isEmpty()) return null
        return ratings.average().toFloat().coerceIn(0f, 5f)
    }

    fun listDuplicateSeeds(): List<LocalDuplicateSeed> {
        data class MutableSeed(
            val code: Int,
            val titleKey: String,
            val numPages: Int,
            val uploadDate: String,
            val mediaId: Long,
            val thumbnailUrl: String,
            val creatorKeys: MutableSet<String>,
            val artistKeys: MutableSet<String>,
            val groupKeys: MutableSet<String>,
            val tagKeys: MutableSet<String>
        )

        val seedMap = linkedMapOf<Int, MutableSeed>()
        readableDatabase.rawQuery(
            """
            SELECT code, title, subtitle, COALESCE(num_pages, 0) AS num_pages,
                   COALESCE(upload_date, '') AS upload_date,
                   COALESCE(media_id, 0) AS media_id,
                   COALESCE(cover_ext, '') AS cover_ext
            FROM entries
            ORDER BY code ASC
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxCode = cursor.getColumnIndexOrThrow("code")
            val idxTitle = cursor.getColumnIndexOrThrow("title")
            val idxSubtitle = cursor.getColumnIndexOrThrow("subtitle")
            val idxPages = cursor.getColumnIndexOrThrow("num_pages")
            val idxUpload = cursor.getColumnIndexOrThrow("upload_date")
            val idxMedia = cursor.getColumnIndexOrThrow("media_id")
            val idxCoverExt = cursor.getColumnIndexOrThrow("cover_ext")
            while (cursor.moveToNext()) {
                val code = cursor.getInt(idxCode)
                if (code <= 0) continue
                val title = cursor.getString(idxTitle).orEmpty()
                val subtitle = cursor.getString(idxSubtitle).orEmpty()
                val titleKey = normalizeDuplicateTitleKey("$title $subtitle")
                val mediaId = cursor.getLong(idxMedia).coerceAtLeast(0L)
                val coverExt = parseCoverExtension(cursor.getString(idxCoverExt).orEmpty())
                seedMap[code] = MutableSeed(
                    code = code,
                    titleKey = titleKey,
                    numPages = cursor.getInt(idxPages).coerceAtLeast(0),
                    uploadDate = cursor.getString(idxUpload)?.trim().orEmpty(),
                    mediaId = mediaId,
                    thumbnailUrl = buildThumbnailUrl(mediaId, coverExt),
                    creatorKeys = linkedSetOf(),
                    artistKeys = linkedSetOf(),
                    groupKeys = linkedSetOf(),
                    tagKeys = linkedSetOf()
                )
            }
        }

        if (seedMap.isEmpty()) return emptyList()

        readableDatabase.rawQuery(
            """
            SELECT et.entry_code AS code, t.name AS name, t.type AS type
            FROM entry_tags et
            JOIN tags t ON t.id = et.tag_id
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxCode = cursor.getColumnIndexOrThrow("code")
            val idxName = cursor.getColumnIndexOrThrow("name")
            val idxType = cursor.getColumnIndexOrThrow("type")
            while (cursor.moveToNext()) {
                val code = cursor.getInt(idxCode)
                val seed = seedMap[code] ?: continue
                val key = normalizeTagName(cursor.getString(idxName).orEmpty())
                if (key.isBlank()) continue
                when (cursor.getString(idxType)?.trim()?.lowercase(Locale.US).orEmpty()) {
                    "artist" -> {
                        seed.creatorKeys += key
                        seed.artistKeys += key
                    }
                    "group" -> {
                        seed.creatorKeys += key
                        seed.groupKeys += key
                    }
                    else -> seed.tagKeys += key
                }
            }
        }

        return seedMap.values.map { seed ->
            LocalDuplicateSeed(
                code = seed.code,
                titleKey = seed.titleKey,
                numPages = seed.numPages,
                uploadDate = seed.uploadDate,
                mediaId = seed.mediaId,
                creatorKeys = seed.creatorKeys,
                tagKeys = seed.tagKeys,
                artistKeys = seed.artistKeys,
                groupKeys = seed.groupKeys,
                thumbnailUrl = seed.thumbnailUrl
            )
        }
    }

    fun setEntryPinned(code: Int, pinned: Boolean) {
        val values = ContentValues().apply {
            put("pinned", if (pinned) 1 else 0)
        }
        writableDatabase.update("entries", values, "code = ?", arrayOf(code.toString()))
    }

    fun isEntryPinned(code: Int): Boolean {
        if (code <= 0) return false
        return readableDatabase.rawQuery(
            """
            SELECT COALESCE(pinned, 0) AS pinned
            FROM entries
            WHERE code = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(code.toString())
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use false
            cursor.getInt(cursor.getColumnIndexOrThrow("pinned")) != 0
        }
    }

    fun getBrowserLibraryStates(codes: List<Int>): Map<Int, BrowserLibraryStateRow> {
        val safeCodes = codes.filter { it > 0 }.distinct()
        if (safeCodes.isEmpty()) return emptyMap()
        val out = linkedMapOf<Int, BrowserLibraryStateRow>()
        safeCodes.chunked(500).forEach { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            readableDatabase.rawQuery(
                """
                SELECT code,
                       COALESCE(rating, 0) AS rating,
                       COALESCE(read_state, 0) AS read_state,
                       COALESCE(pinned, 0) AS pinned
                FROM entries
                WHERE code IN ($placeholders)
                """.trimIndent(),
                chunk.map { it.toString() }.toTypedArray()
            ).use { cursor ->
                val idxCode = cursor.getColumnIndexOrThrow("code")
                val idxRating = cursor.getColumnIndexOrThrow("rating")
                val idxRead = cursor.getColumnIndexOrThrow("read_state")
                val idxPinned = cursor.getColumnIndexOrThrow("pinned")
                while (cursor.moveToNext()) {
                    val code = cursor.getInt(idxCode)
                    out[code] = BrowserLibraryStateRow(
                        code = code,
                        rating = cursor.getInt(idxRating).coerceIn(0, 5),
                        isRead = cursor.getInt(idxRead) != 0,
                        pinned = cursor.getInt(idxPinned) != 0
                    )
                }
            }
        }
        return out
    }

    private data class EntryFilterQuery(
        val whereClauses: List<String>,
        val args: List<String>
    )

    private fun buildEntryFilterQuery(
        textFilter: String,
        tagFilterIds: List<Long>
    ): EntryFilterQuery {
        val parsedQuery = parseSearchQuery(textFilter)
        val whereClauses = mutableListOf<String>()
        val args = mutableListOf<String>()

        val trimmedFilter = parsedQuery.freeText
        if (trimmedFilter.isNotEmpty()) {
            val freeTerms = extractSearchEverythingBrowserTerms(trimmedFilter)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .ifEmpty { listOf(trimmedFilter) }

            val tokenClauses = mutableListOf<String>()
            val tokenArgs = mutableListOf<String>()

            freeTerms.forEach { term ->
                val searchClauses = mutableListOf<String>()
                val searchArgs = mutableListOf<String>()
                val likeTerm = "%$term%"

                searchClauses += "CAST(e.code AS TEXT) LIKE ?"
                searchArgs += likeTerm

                parseCode(term)?.let { parsedCode ->
                    searchClauses += "e.code = ?"
                    searchArgs += parsedCode.toString()
                }

                searchClauses += "e.title LIKE ?"
                searchArgs += likeTerm
                searchClauses += "e.subtitle LIKE ?"
                searchArgs += likeTerm
                searchClauses += "e.upload_date LIKE ?"
                searchArgs += likeTerm
                searchClauses += "e.fetched_at LIKE ?"
                searchArgs += likeTerm
                searchClauses += "e.added_at LIKE ?"
                searchArgs += likeTerm
                searchClauses += "e.source_url LIKE ?"
                searchArgs += likeTerm
                searchClauses += """
                    EXISTS (
                        SELECT 1
                        FROM entry_tags etf
                        JOIN tags tf ON tf.id = etf.tag_id
                        WHERE etf.entry_code = e.code
                          AND (tf.name LIKE ? OR tf.type LIKE ?)
                    )
                """.trimIndent()
                searchArgs += likeTerm
                searchArgs += likeTerm

                tokenClauses += "(${searchClauses.joinToString("\n OR ")})"
                tokenArgs += searchArgs
            }

            if (tokenClauses.isNotEmpty()) {
                whereClauses += tokenClauses.joinToString("\n AND ")
                args += tokenArgs
            }
        }

        parsedQuery.filters.forEach { filter ->
            val value = filter.value.trim()
            if (value.isBlank()) return@forEach
            when (filter.key) {
                "code" -> {
                    val cleaned = value.removePrefix("#").trim()
                    val parsedCode = cleaned.toIntOrNull()
                    if (parsedCode != null) {
                        whereClauses += "e.code = ?"
                        args += parsedCode.toString()
                    } else {
                        whereClauses += "CAST(e.code AS TEXT) LIKE ?"
                        args += "%$value%"
                    }
                }
                "title" -> {
                    whereClauses += "e.title LIKE ?"
                    args += "%$value%"
                }
                "subtitle" -> {
                    whereClauses += "e.subtitle LIKE ?"
                    args += "%$value%"
                }
                "pages" -> {
                    val pageNumbers = extractNumericTokens(value)
                    when {
                        pageNumbers.size >= 2 -> {
                            val start = minOf(pageNumbers[0], pageNumbers[1])
                            val end = maxOf(pageNumbers[0], pageNumbers[1])
                            whereClauses += "e.num_pages BETWEEN ? AND ?"
                            args += start.toString()
                            args += end.toString()
                        }
                        pageNumbers.size == 1 -> {
                            whereClauses += "e.num_pages = ?"
                            args += pageNumbers.first().toString()
                        }
                        else -> {
                            whereClauses += "CAST(e.num_pages AS TEXT) LIKE ?"
                            args += "%$value%"
                        }
                    }
                }
                "upload" -> {
                    val dateRange = parseDateRange(value)
                    when {
                        dateRange != null -> {
                            whereClauses += "e.upload_date BETWEEN ? AND ?"
                            args += dateRange.first.format(UPLOAD_DATE_FORMAT)
                            args += dateRange.second.format(UPLOAD_DATE_FORMAT)
                        }
                        else -> {
                            val singleDate = parseFirstDate(value)
                            if (singleDate != null) {
                                whereClauses += "e.upload_date = ?"
                                args += singleDate.format(UPLOAD_DATE_FORMAT)
                            } else {
                                whereClauses += "e.upload_date LIKE ?"
                                args += "%$value%"
                            }
                        }
                    }
                }
                "rating" -> {
                    val ratingNumbers = extractNumericTokens(value)
                    when {
                        ratingNumbers.size >= 2 -> {
                            val start = minOf(ratingNumbers[0], ratingNumbers[1]).coerceIn(0, 5)
                            val end = maxOf(ratingNumbers[0], ratingNumbers[1]).coerceIn(0, 5)
                            whereClauses += "e.rating BETWEEN ? AND ?"
                            args += start.toString()
                            args += end.toString()
                        }
                        ratingNumbers.size == 1 -> {
                            whereClauses += "e.rating = ?"
                            args += ratingNumbers.first().coerceIn(0, 5).toString()
                        }
                        else -> {
                            whereClauses += "CAST(e.rating AS TEXT) LIKE ?"
                            args += "%$value%"
                        }
                    }
                }
                "fetched" -> {
                    whereClauses += "e.fetched_at LIKE ?"
                    args += "%$value%"
                }
                "added" -> {
                    val dateRange = parseDateRange(value)
                    when {
                        dateRange != null -> {
                            whereClauses += "substr(e.added_at, 1, 10) BETWEEN ? AND ?"
                            args += dateRange.first.format(UPLOAD_DATE_FORMAT)
                            args += dateRange.second.format(UPLOAD_DATE_FORMAT)
                        }
                        else -> {
                            val singleDate = parseFirstDate(value)
                            if (singleDate != null) {
                                whereClauses += "substr(e.added_at, 1, 10) = ?"
                                args += singleDate.format(UPLOAD_DATE_FORMAT)
                            } else {
                                whereClauses += "e.added_at LIKE ?"
                                args += "%$value%"
                            }
                        }
                    }
                }
                "url" -> {
                    whereClauses += "e.source_url LIKE ?"
                    args += "%$value%"
                }
                "tag" -> {
                    whereClauses += """
                        EXISTS (
                            SELECT 1
                            FROM entry_tags etf
                            JOIN tags tf ON tf.id = etf.tag_id
                            WHERE etf.entry_code = e.code
                              AND (tf.name LIKE ? OR tf.type LIKE ?)
                        )
                    """.trimIndent()
                    val term = "%$value%"
                    args += term
                    args += term
                }
                "anytag" -> {
                    val names = value.split('|').map { it.trim() }.filter { it.isNotBlank() }.distinct()
                    if (names.isNotEmpty()) {
                        whereClauses += """
                            EXISTS (
                                SELECT 1 FROM entry_tags etf
                                JOIN tags tf ON tf.id = etf.tag_id
                                WHERE etf.entry_code = e.code
                                  AND (${names.joinToString(" OR ") { "tf.name LIKE ?" }})
                            )
                        """.trimIndent()
                        names.forEach { args += "%$it%" }
                    }
                }
                "anytagid" -> {
                    val ids = value.split('|').mapNotNull { it.trim().toLongOrNull()?.takeIf { id -> id > 0L } }.distinct()
                    if (ids.isNotEmpty()) {
                        whereClauses += """
                            EXISTS (
                                SELECT 1 FROM entry_tags etf
                                WHERE etf.entry_code = e.code
                                  AND etf.tag_id IN (${ids.joinToString(",") { "?" }})
                            )
                        """.trimIndent()
                        ids.forEach { args += it.toString() }
                    }
                }
                "excludetag" -> {
                    val names = value.split('|').map { it.trim() }.filter { it.isNotBlank() }.distinct()
                    if (names.isNotEmpty()) {
                        whereClauses += """
                            NOT EXISTS (
                                SELECT 1 FROM entry_tags etf
                                JOIN tags tf ON tf.id = etf.tag_id
                                WHERE etf.entry_code = e.code
                                  AND (${names.joinToString(" OR ") { "tf.name LIKE ?" }})
                            )
                        """.trimIndent()
                        names.forEach { args += "%$it%" }
                    }
                }
                "excludetagid" -> {
                    val ids = value.split('|').mapNotNull { it.trim().toLongOrNull()?.takeIf { id -> id > 0L } }.distinct()
                    if (ids.isNotEmpty()) {
                        whereClauses += """
                            NOT EXISTS (
                                SELECT 1 FROM entry_tags etf
                                WHERE etf.entry_code = e.code
                                  AND etf.tag_id IN (${ids.joinToString(",") { "?" }})
                            )
                        """.trimIndent()
                        ids.forEach { args += it.toString() }
                    }
                }
                "type" -> {
                    whereClauses += """
                        EXISTS (
                            SELECT 1
                            FROM entry_tags etf
                            JOIN tags tf ON tf.id = etf.tag_id
                            WHERE etf.entry_code = e.code
                              AND tf.type LIKE ?
                        )
                    """.trimIndent()
                    args += "%$value%"
                }
                "artist", "group", "parody", "character", "category", "language" -> {
                    whereClauses += """
                        EXISTS (
                            SELECT 1
                            FROM entry_tags etf
                            JOIN tags tf ON tf.id = etf.tag_id
                            WHERE etf.entry_code = e.code
                              AND tf.type = ?
                              AND tf.name LIKE ?
                        )
                    """.trimIndent()
                    args += filter.key
                    args += "%$value%"
                }
            }
        }

        val uniqueTagIds = tagFilterIds
            .mapNotNull { it.takeIf { value -> value > 0L } }
            .distinct()

        if (uniqueTagIds.isNotEmpty()) {
            uniqueTagIds.forEach { tagId ->
                whereClauses += """
                    EXISTS (
                        SELECT 1
                        FROM entry_tags etg
                        WHERE etg.entry_code = e.code
                          AND etg.tag_id = ?
                    )
                """.trimIndent()
                args += tagId.toString()
            }
        }

        return EntryFilterQuery(
            whereClauses = whereClauses,
            args = args
        )
    }

    fun listEntries(
        textFilter: String,
        tagFilterIds: List<Long>,
        sortField: EntrySortField?,
        sortDirection: SortDirection,
        readFilter: EntryReadFilterMode,
        prioritizePinned: Boolean = true
    ): List<EntryRow> {
        val filterQuery = buildEntryFilterQuery(textFilter, tagFilterIds)
        val whereClauses = filterQuery.whereClauses.toMutableList()
        val args = filterQuery.args.toMutableList()

        when (readFilter) {
            EntryReadFilterMode.ALL -> Unit
            EntryReadFilterMode.READ -> {
                whereClauses += "COALESCE(e.read_state, 0) = 1"
            }
            EntryReadFilterMode.UNREAD -> {
                whereClauses += "COALESCE(e.read_state, 0) = 0"
            }
            EntryReadFilterMode.DOWNLOADED -> Unit
        }

        val baseOrderBy = when (sortField) {
            EntrySortField.RATING -> "e.rating DESC, e.added_at DESC, e.code DESC"
            EntrySortField.CODE -> "e.code ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}"
            EntrySortField.TITLE -> "LOWER(e.title) ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, e.code DESC"
            EntrySortField.PAGES -> "e.num_pages ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, e.code DESC"
            EntrySortField.UPLOAD ->
                "CASE WHEN e.upload_date = '' THEN 1 ELSE 0 END ASC, e.upload_date ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, e.code DESC"
            EntrySortField.ADDED -> "e.added_at ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, e.code DESC"
            EntrySortField.READ ->
                "CASE WHEN COALESCE(e.read_at, '') = '' THEN 1 ELSE 0 END ASC, e.read_at ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, e.code DESC"
            null -> "e.added_at DESC, e.code DESC"
        }
        val orderBy = if (prioritizePinned) {
            "COALESCE(e.pinned, 0) DESC, $baseOrderBy"
        } else {
            baseOrderBy
        }

        val sql = buildString {
            append(
                """
                SELECT
                    e.code,
                    e.title,
                    e.num_pages,
                    e.upload_date,
                    e.added_at,
                    e.rating,
                    COALESCE(
                        (
                            SELECT AVG(CAST(rating_value AS REAL))
                            FROM (
                                SELECT e.rating AS rating_value
                                WHERE COALESCE(e.rating, 0) > 0
                                UNION ALL
                                SELECT rs.rating AS rating_value
                                FROM reading_sessions rs
                                WHERE rs.entry_code = e.code
                                  AND COALESCE(rs.rating, 0) > 0
                            )
                        ),
                        CAST(COALESCE(e.rating, 0) AS REAL)
                    ) AS average_rating,
                    e.read_state,
                    e.pinned,
                    e.fetched_at,
                    e.source_url,
                    e.media_id,
                    e.cover_ext,
                    COALESCE(
                        (
                            SELECT GROUP_CONCAT(t.name, ', ')
                            FROM entry_tags et
                            JOIN tags t ON t.id = et.tag_id
                            WHERE et.entry_code = e.code
                        ),
                        ''
                    ) AS tags
                FROM entries e
                """.trimIndent()
            )
            if (whereClauses.isNotEmpty()) {
                append(" WHERE ")
                append(whereClauses.joinToString(" AND "))
            }
            append(" ORDER BY ")
            append(orderBy)
        }

        val rows = mutableListOf<EntryRow>()
        readableDatabase.rawQuery(sql, args.toTypedArray()).use { cursor ->
            val idxCode = cursor.getColumnIndexOrThrow("code")
            val idxTitle = cursor.getColumnIndexOrThrow("title")
            val idxPages = cursor.getColumnIndexOrThrow("num_pages")
            val idxUpload = cursor.getColumnIndexOrThrow("upload_date")
            val idxAdded = cursor.getColumnIndexOrThrow("added_at")
            val idxRating = cursor.getColumnIndexOrThrow("rating")
            val idxAverageRating = cursor.getColumnIndexOrThrow("average_rating")
            val idxRead = cursor.getColumnIndexOrThrow("read_state")
            val idxPinned = cursor.getColumnIndexOrThrow("pinned")
            val idxFetched = cursor.getColumnIndexOrThrow("fetched_at")
            val idxSourceUrl = cursor.getColumnIndexOrThrow("source_url")
            val idxMediaId = cursor.getColumnIndexOrThrow("media_id")
            val idxCoverExt = cursor.getColumnIndexOrThrow("cover_ext")
            val idxTags = cursor.getColumnIndexOrThrow("tags")

            while (cursor.moveToNext()) {
                val mediaId = cursor.getLong(idxMediaId).coerceAtLeast(0L)
                val coverExt = parseCoverExtension(cursor.getString(idxCoverExt) ?: "")
                rows += EntryRow(
                    code = cursor.getInt(idxCode),
                    title = cursor.getString(idxTitle) ?: "",
                    numPages = cursor.getInt(idxPages),
                    uploadDate = cursor.getString(idxUpload) ?: "",
                    addedAt = cursor.getString(idxAdded) ?: "",
                    rating = cursor.getInt(idxRating).coerceIn(0, 5),
                    averageRating = cursor.getFloat(idxAverageRating).coerceIn(0f, 5f),
                    isRead = cursor.getInt(idxRead) != 0,
                    pinned = cursor.getInt(idxPinned) != 0,
                    fetchedAt = cursor.getString(idxFetched) ?: "",
                    sourceUrl = cursor.getString(idxSourceUrl) ?: "",
                    thumbnailUrl = buildThumbnailUrl(mediaId, coverExt),
                    tags = cursor.getString(idxTags) ?: ""
                )
            }
        }
        return rows
    }

    fun listTagCounts(
        textFilter: String,
        sortField: TagSortField,
        sortDirection: SortDirection,
        visibleEntryCodes: Collection<Int>? = null
    ): List<TagCountRow> {
        if (visibleEntryCodes != null && visibleEntryCodes.isEmpty()) return emptyList()
        val parsedQuery = parseSearchQuery(textFilter)
        val args = mutableListOf<String>()
        val whereClauses = mutableListOf<String>()

        fun appendNameOrTypeLike(value: String) {
            val trimmed = value.trim()
            if (trimmed.isBlank()) return
            val term = "%$trimmed%"
            whereClauses += "(t.name LIKE ? OR t.type LIKE ?)"
            args += term
            args += term
        }

        if (parsedQuery.freeText.isNotBlank()) {
            appendNameOrTypeLike(parsedQuery.freeText)
        }

        parsedQuery.filters.forEach { filter ->
            val value = filter.value.trim()
            if (value.isBlank()) return@forEach
            when (filter.key) {
                "tag" -> appendNameOrTypeLike(value)
                "type" -> {
                    whereClauses += "t.type LIKE ?"
                    args += "%$value%"
                }
                "artist", "group", "parody", "character", "category", "language" -> {
                    whereClauses += "(t.type = ? AND t.name LIKE ?)"
                    args += filter.key
                    args += "%$value%"
                }
            }
        }

        visibleEntryCodes?.let { codes ->
            // Codes originate from the typed local entry query. Embedding the validated integers
            // avoids SQLite's bind-variable limit for libraries containing more than 999 entries.
            val safeCodes = codes.asSequence().filter { it > 0 }.distinct().toList()
            if (safeCodes.isEmpty()) return emptyList()
            whereClauses += "et.entry_code IN (${safeCodes.joinToString(",")})"
        }

        val orderBy = when (sortField) {
            TagSortField.NAME -> "t.name ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, t.type ASC, entry_count DESC"
            TagSortField.TYPE -> "t.type ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, t.name ASC, entry_count DESC"
            TagSortField.COUNT -> "entry_count ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, t.type ASC, t.name ASC"
        }

        val sql = """
            SELECT
                t.id,
                t.name,
                t.type,
                COUNT(et.entry_code) AS entry_count
            FROM tags t
            JOIN entry_tags et ON et.tag_id = t.id
            ${if (whereClauses.isNotEmpty()) "WHERE ${whereClauses.joinToString(" AND ")}" else ""}
            GROUP BY t.id, t.name, t.type
            ORDER BY $orderBy
        """.trimIndent()

        val rows = mutableListOf<TagCountRow>()
        readableDatabase.rawQuery(sql, args.toTypedArray()).use { cursor ->
            val idxId = cursor.getColumnIndexOrThrow("id")
            val idxName = cursor.getColumnIndexOrThrow("name")
            val idxType = cursor.getColumnIndexOrThrow("type")
            val idxCount = cursor.getColumnIndexOrThrow("entry_count")
            while (cursor.moveToNext()) {
                rows += TagCountRow(
                    id = cursor.getLong(idxId),
                    name = cursor.getString(idxName) ?: "",
                    type = cursor.getString(idxType) ?: "",
                    count = cursor.getInt(idxCount)
                )
            }
        }
        return rows
    }

    fun listPopularTags(
        sortField: TagSortField,
        sortDirection: SortDirection
    ): List<PopularTagRow> {
        val orderBy = when (sortField) {
            TagSortField.NAME -> "name ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, type ASC, tag_count DESC"
            TagSortField.TYPE -> "type ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, name ASC, tag_count DESC"
            TagSortField.COUNT -> "tag_count ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, type ASC, name ASC"
        }

        val rows = mutableListOf<PopularTagRow>()
        readableDatabase.rawQuery(
            """
            SELECT id, name, type, tag_count, blocked
            FROM popular_tags
            ORDER BY $orderBy
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxId = cursor.getColumnIndexOrThrow("id")
            val idxName = cursor.getColumnIndexOrThrow("name")
            val idxType = cursor.getColumnIndexOrThrow("type")
            val idxCount = cursor.getColumnIndexOrThrow("tag_count")
            val idxBlocked = cursor.getColumnIndexOrThrow("blocked")
            while (cursor.moveToNext()) {
                rows += PopularTagRow(
                    id = cursor.getLong(idxId),
                    name = cursor.getString(idxName) ?: "",
                    type = cursor.getString(idxType) ?: "",
                    count = cursor.getInt(idxCount).coerceAtLeast(0),
                    blocked = cursor.getInt(idxBlocked) != 0
                )
            }
        }
        return rows
    }

    fun replacePopularTags(rows: List<PopularTagSeed>) {
        val deduped = linkedMapOf<Pair<String, String>, PopularTagSeed>()
        rows.forEach { row ->
            val normalizedName = normalizeTagName(row.name)
            val type = row.type.trim().lowercase(Locale.US)
            if (normalizedName.isBlank() || type.isBlank()) return@forEach
            val key = normalizedName to type
            val existing = deduped[key]
            if (existing == null || row.count > existing.count) {
                deduped[key] = PopularTagSeed(
                    name = row.name.trim(),
                    type = type,
                    count = row.count.coerceAtLeast(0)
                )
            }
        }

        val db = writableDatabase
        db.beginTransaction()
        try {
            val blockedByKey = linkedMapOf<Pair<String, String>, Int>()
            db.rawQuery(
                "SELECT normalized_name, type, blocked FROM popular_tags",
                null
            ).use { cursor ->
                val idxName = cursor.getColumnIndexOrThrow("normalized_name")
                val idxType = cursor.getColumnIndexOrThrow("type")
                val idxBlocked = cursor.getColumnIndexOrThrow("blocked")
                while (cursor.moveToNext()) {
                    val key = (cursor.getString(idxName) ?: "") to (cursor.getString(idxType) ?: "")
                    blockedByKey[key] = if (cursor.getInt(idxBlocked) != 0) 1 else 0
                }
            }

            db.delete("popular_tags", null, null)
            deduped.forEach { (key, row) ->
                val values = ContentValues().apply {
                    put("name", row.name)
                    put("type", key.second)
                    put("normalized_name", key.first)
                    put("tag_count", row.count.coerceAtLeast(0))
                    put("blocked", blockedByKey[key] ?: 0)
                }
                db.insertOrThrow("popular_tags", null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun setPopularTagBlocked(tagId: Long, blocked: Boolean) {
        if (tagId <= 0L) return
        val values = ContentValues().apply {
            put("blocked", if (blocked) 1 else 0)
        }
        writableDatabase.update("popular_tags", values, "id = ?", arrayOf(tagId.toString()))
    }

    fun clearAllBlockedPopularTags() {
        val values = ContentValues().apply {
            put("blocked", 0)
        }
        writableDatabase.update("popular_tags", values, null, null)
    }

    fun listBlockedPopularTagNames(): List<String> {
        val rows = mutableListOf<String>()
        readableDatabase.rawQuery(
            """
            SELECT MIN(name) AS name
            FROM popular_tags
            WHERE blocked = 1
            GROUP BY normalized_name
            ORDER BY LOWER(name) ASC
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxName = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                val name = cursor.getString(idxName)?.trim().orEmpty()
                if (name.isNotBlank()) {
                    rows += name
                }
            }
        }
        return rows
    }

    fun getEntryDetail(code: Int): EntryDetail? {
        val entry = readableDatabase.rawQuery(
            """
            SELECT code, title, subtitle, source_url, num_pages, upload_date, rating, read_state, read_at, fetched_at, added_at, media_id, cover_ext
            FROM entries
            WHERE code = ?
            """.trimIndent(),
            arrayOf(code.toString())
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                null
            } else {
                EntryDetail(
                    code = cursor.getInt(cursor.getColumnIndexOrThrow("code")),
                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")) ?: "",
                    subtitle = cursor.getString(cursor.getColumnIndexOrThrow("subtitle")) ?: "",
                    sourceUrl = cursor.getString(cursor.getColumnIndexOrThrow("source_url")) ?: "",
                    mediaId = cursor.getLong(cursor.getColumnIndexOrThrow("media_id")).coerceAtLeast(0L),
                    coverExt = parseCoverExtension(cursor.getString(cursor.getColumnIndexOrThrow("cover_ext")) ?: ""),
                    numPages = cursor.getInt(cursor.getColumnIndexOrThrow("num_pages")),
                    uploadDate = cursor.getString(cursor.getColumnIndexOrThrow("upload_date")) ?: "",
                    rating = cursor.getInt(cursor.getColumnIndexOrThrow("rating")).coerceIn(0, 5),
                    isRead = cursor.getInt(cursor.getColumnIndexOrThrow("read_state")) != 0,
                    readAt = cursor.getString(cursor.getColumnIndexOrThrow("read_at")) ?: "",
                    fetchedAt = cursor.getString(cursor.getColumnIndexOrThrow("fetched_at")) ?: "",
                    addedAt = cursor.getString(cursor.getColumnIndexOrThrow("added_at")) ?: "",
                    thumbnailUrl = buildThumbnailUrl(
                        cursor.getLong(cursor.getColumnIndexOrThrow("media_id")).coerceAtLeast(0L),
                        parseCoverExtension(cursor.getString(cursor.getColumnIndexOrThrow("cover_ext")) ?: "")
                    ),
                    tagsByType = emptyMap()
                )
            }
        } ?: return null

        val tagsByType = linkedMapOf<String, MutableList<String>>()
        readableDatabase.rawQuery(
            """
            SELECT t.type, t.name
            FROM entry_tags et
            JOIN tags t ON t.id = et.tag_id
            WHERE et.entry_code = ?
            ORDER BY t.type ASC, t.name ASC
            """.trimIndent(),
            arrayOf(code.toString())
        ).use { cursor ->
            val idxType = cursor.getColumnIndexOrThrow("type")
            val idxName = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                val type = cursor.getString(idxType) ?: ""
                val name = cursor.getString(idxName) ?: ""
                tagsByType.getOrPut(type) { mutableListOf() }.add(name)
            }
        }

        return entry.copy(tagsByType = tagsByType)
    }

    fun getEntryDetails(codes: List<Int>): List<EntryDetail> {
        val requestedCodes = codes
            .asSequence()
            .filter { it > 0 }
            .distinct()
            .toList()
        if (requestedCodes.isEmpty()) return emptyList()

        val detailsByCode = linkedMapOf<Int, EntryDetail>()
        requestedCodes.chunked(300).forEach { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            readableDatabase.rawQuery(
                """
                SELECT code, title, subtitle, source_url, num_pages, upload_date, rating, read_state, read_at, fetched_at, added_at, media_id, cover_ext
                FROM entries
                WHERE code IN ($placeholders)
                """.trimIndent(),
                chunk.map(Int::toString).toTypedArray()
            ).use { cursor ->
                val idxCode = cursor.getColumnIndexOrThrow("code")
                val idxTitle = cursor.getColumnIndexOrThrow("title")
                val idxSubtitle = cursor.getColumnIndexOrThrow("subtitle")
                val idxSourceUrl = cursor.getColumnIndexOrThrow("source_url")
                val idxMediaId = cursor.getColumnIndexOrThrow("media_id")
                val idxCoverExt = cursor.getColumnIndexOrThrow("cover_ext")
                val idxNumPages = cursor.getColumnIndexOrThrow("num_pages")
                val idxUploadDate = cursor.getColumnIndexOrThrow("upload_date")
                val idxRating = cursor.getColumnIndexOrThrow("rating")
                val idxReadState = cursor.getColumnIndexOrThrow("read_state")
                val idxReadAt = cursor.getColumnIndexOrThrow("read_at")
                val idxFetchedAt = cursor.getColumnIndexOrThrow("fetched_at")
                val idxAddedAt = cursor.getColumnIndexOrThrow("added_at")
                while (cursor.moveToNext()) {
                    val code = cursor.getInt(idxCode)
                    val mediaId = cursor.getLong(idxMediaId).coerceAtLeast(0L)
                    val coverExt = parseCoverExtension(cursor.getString(idxCoverExt) ?: "")
                    detailsByCode[code] = EntryDetail(
                        code = code,
                        title = cursor.getString(idxTitle) ?: "",
                        subtitle = cursor.getString(idxSubtitle) ?: "",
                        sourceUrl = cursor.getString(idxSourceUrl) ?: "",
                        mediaId = mediaId,
                        coverExt = coverExt,
                        numPages = cursor.getInt(idxNumPages),
                        uploadDate = cursor.getString(idxUploadDate) ?: "",
                        rating = cursor.getInt(idxRating).coerceIn(0, 5),
                        isRead = cursor.getInt(idxReadState) != 0,
                        readAt = cursor.getString(idxReadAt) ?: "",
                        fetchedAt = cursor.getString(idxFetchedAt) ?: "",
                        addedAt = cursor.getString(idxAddedAt) ?: "",
                        thumbnailUrl = buildThumbnailUrl(mediaId, coverExt),
                        tagsByType = emptyMap()
                    )
                }
            }
        }

        val tagsByCode = mutableMapOf<Int, LinkedHashMap<String, MutableList<String>>>()
        requestedCodes.chunked(300).forEach { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            readableDatabase.rawQuery(
                """
                SELECT et.entry_code AS entry_code, t.type AS type, t.name AS name
                FROM entry_tags et
                JOIN tags t ON t.id = et.tag_id
                WHERE et.entry_code IN ($placeholders)
                ORDER BY et.entry_code ASC, t.type ASC, t.name ASC
                """.trimIndent(),
                chunk.map(Int::toString).toTypedArray()
            ).use { cursor ->
                val idxCode = cursor.getColumnIndexOrThrow("entry_code")
                val idxType = cursor.getColumnIndexOrThrow("type")
                val idxName = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    val code = cursor.getInt(idxCode)
                    val type = cursor.getString(idxType) ?: ""
                    val name = cursor.getString(idxName) ?: ""
                    val tagMap = tagsByCode.getOrPut(code) { linkedMapOf() }
                    tagMap.getOrPut(type) { mutableListOf() }.add(name)
                }
            }
        }

        return requestedCodes.mapNotNull { code ->
            detailsByCode[code]?.copy(tagsByType = tagsByCode[code] ?: linkedMapOf())
        }
    }

    fun clearEntryHeatmapCache() {
        writableDatabase.delete("entry_heatmap_cache", null, null)
    }

    fun saveEntryHeatmapCache(
        cacheKey: String,
        layout: TagGraphEntryLayoutResult
    ) {
        if (cacheKey.isBlank()) return
        val payload = JSONObject().apply {
            put(
                "nodes",
                JSONArray().apply {
                    layout.nodes.forEach { node ->
                        put(
                            JSONObject()
                                .put("code", node.code)
                                .put("dominant_circle_tags", JSONArray(node.dominantCircleTags))
                                .put("boundary_center_x", node.boundaryCenterX.toDouble())
                                .put("boundary_center_y", node.boundaryCenterY.toDouble())
                                .put("boundary_radius_px", node.boundaryRadiusPx.toDouble())
                                .put("x", node.x.toDouble())
                                .put("y", node.y.toDouble())
                        )
                    }
                }
            )
            put(
                "circles",
                JSONArray().apply {
                    layout.familyCircles.forEach { circle ->
                        put(
                            JSONObject()
                                .put("tag_name", circle.tagName)
                                .put("label", circle.label)
                                .put("center_x", circle.centerX.toDouble())
                                .put("center_y", circle.centerY.toDouble())
                                .put("radius_px", circle.radiusPx.toDouble())
                                .put("entry_count", circle.entryCount)
                        )
                    }
                }
            )
        }
        val values = ContentValues().apply {
            put("slot_id", 1)
            put("cache_key", cacheKey)
            put("payload_json", payload.toString())
            put("updated_at", utcNowString())
        }
        writableDatabase.insertWithOnConflict(
            "entry_heatmap_cache",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun loadEntryHeatmapCache(
        cacheKey: String,
        snapshot: TagGraphSnapshot
    ): TagGraphEntryLayoutResult? {
        if (cacheKey.isBlank()) return null
        val payloadRaw = readableDatabase.rawQuery(
            """
            SELECT payload_json
            FROM entry_heatmap_cache
            WHERE slot_id = 1
              AND cache_key = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(cacheKey)
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow("payload_json"))?.trim().orEmpty()
            } else {
                ""
            }
        }
        if (payloadRaw.isBlank()) return null
        val payload = runCatching { JSONObject(payloadRaw) }.getOrNull() ?: return null
        val snapshotNodesByCode = snapshot.entryNodes.associateBy { it.code }
        val nodes = buildList {
            val rows = payload.optJSONArray("nodes") ?: JSONArray()
            for (index in 0 until rows.length()) {
                val obj = rows.optJSONObject(index) ?: continue
                val code = when (val raw = obj.opt("code")) {
                    is Number -> raw.toInt()
                    is String -> raw.trim().toIntOrNull() ?: 0
                    else -> 0
                }
                if (code <= 0) continue
                val snapshotNode = snapshotNodesByCode[code] ?: continue
                val dominantCircleTags = buildList {
                    val tags = obj.optJSONArray("dominant_circle_tags") ?: JSONArray()
                    for (tagIndex in 0 until tags.length()) {
                        val tag = tags.optString(tagIndex, "").trim()
                        if (tag.isNotBlank()) add(tag)
                    }
                }
                add(
                    snapshotNode.copy(
                        dominantCircleTags = dominantCircleTags,
                        boundaryCenterX = obj.optDouble("boundary_center_x", snapshotNode.boundaryCenterX.toDouble()).toFloat(),
                        boundaryCenterY = obj.optDouble("boundary_center_y", snapshotNode.boundaryCenterY.toDouble()).toFloat(),
                        boundaryRadiusPx = obj.optDouble("boundary_radius_px", snapshotNode.boundaryRadiusPx.toDouble()).toFloat(),
                        x = obj.optDouble("x", snapshotNode.x.toDouble()).toFloat(),
                        y = obj.optDouble("y", snapshotNode.y.toDouble()).toFloat()
                    )
                )
            }
        }
        if (nodes.isEmpty()) return null
        val familyCircles = buildList {
            val rows = payload.optJSONArray("circles") ?: JSONArray()
            for (index in 0 until rows.length()) {
                val obj = rows.optJSONObject(index) ?: continue
                val tagName = obj.optString("tag_name", "").trim()
                if (tagName.isBlank()) continue
                add(
                    TagGraphEntryFamilyCircle(
                        tagName = tagName,
                        label = obj.optString("label", "").trim().ifBlank { tagName },
                        centerX = obj.optDouble("center_x", 0.5).toFloat(),
                        centerY = obj.optDouble("center_y", 0.5).toFloat(),
                        radiusPx = obj.optDouble("radius_px", 0.0).toFloat().coerceAtLeast(0f),
                        entryCount = obj.optInt("entry_count", 0).coerceAtLeast(0)
                    )
                )
            }
        }
        return TagGraphEntryLayoutResult(
            nodes = nodes,
            familyCircles = familyCircles
        )
    }

    fun exportEntryHeatmapCacheSnapshot(): JSONArray {
        val rows = JSONArray()
        readableDatabase.rawQuery(
            """
            SELECT cache_key, payload_json, updated_at
            FROM entry_heatmap_cache
            WHERE slot_id = 1
            LIMIT 1
            """.trimIndent(),
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                val cacheKey = cursor.getString(cursor.getColumnIndexOrThrow("cache_key"))?.trim().orEmpty()
                val payloadRaw = cursor.getString(cursor.getColumnIndexOrThrow("payload_json"))?.trim().orEmpty()
                val updatedAt = cursor.getString(cursor.getColumnIndexOrThrow("updated_at"))?.trim().orEmpty()
                if (cacheKey.isNotBlank() && payloadRaw.isNotBlank()) {
                    val payload = runCatching { JSONObject(payloadRaw) }.getOrNull()
                    if (payload != null) {
                        rows.put(
                            JSONObject()
                                .put("cache_key", cacheKey)
                                .put("updated_at", updatedAt)
                                .put("payload", payload)
                        )
                    }
                }
            }
        }
        return rows
    }

    fun getEntryHeatmapCacheRecord(): EntryHeatmapCacheRecord? {
        return readableDatabase.rawQuery(
            """
            SELECT cache_key, updated_at
            FROM entry_heatmap_cache
            WHERE slot_id = 1
            LIMIT 1
            """.trimIndent(),
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val cacheKey = cursor.getString(cursor.getColumnIndexOrThrow("cache_key"))?.trim().orEmpty()
            val updatedAt = cursor.getString(cursor.getColumnIndexOrThrow("updated_at"))?.trim().orEmpty()
            if (cacheKey.isBlank()) null else EntryHeatmapCacheRecord(cacheKey = cacheKey, updatedAt = updatedAt)
        }
    }

    private fun replaceEntryHeatmapCacheFromSnapshot(
        db: SQLiteDatabase,
        rows: JSONArray?
    ): Int {
        db.delete("entry_heatmap_cache", null, null)
        if (rows == null) return 0
        for (idx in 0 until rows.length()) {
            val obj = rows.optJSONObject(idx) ?: continue
            val cacheKey = obj.optString("cache_key", "").trim()
            val payload = obj.optJSONObject("payload") ?: continue
            if (cacheKey.isBlank()) continue
            val values = ContentValues().apply {
                put("slot_id", 1)
                put("cache_key", cacheKey)
                put("payload_json", payload.toString())
                put("updated_at", obj.optString("updated_at", "").trim())
            }
            db.insertWithOnConflict(
                "entry_heatmap_cache",
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
            return 1
        }
        return 0
    }

    fun exportSnapshot(): JSONObject {
        val entriesArray = JSONArray()
        val creatorsArray = JSONArray()
        val tagsByCode = mutableMapOf<Int, MutableList<JSONObject>>()

        readableDatabase.rawQuery(
            """
            SELECT et.entry_code AS code, t.name AS name, t.type AS type
            FROM entry_tags et
            JOIN tags t ON t.id = et.tag_id
            ORDER BY et.entry_code ASC, t.type ASC, t.name ASC
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxCode = cursor.getColumnIndexOrThrow("code")
            val idxName = cursor.getColumnIndexOrThrow("name")
            val idxType = cursor.getColumnIndexOrThrow("type")
            while (cursor.moveToNext()) {
                val code = cursor.getInt(idxCode)
                val tagObj = JSONObject()
                    .put("name", cursor.getString(idxName) ?: "")
                    .put("type", cursor.getString(idxType) ?: "tag")
                tagsByCode.getOrPut(code) { mutableListOf() }.add(tagObj)
            }
        }

        readableDatabase.rawQuery(
            """
            SELECT
                code, title, subtitle, source_url, num_pages, upload_date, rating, read_state, read_at, pinned, fetched_at, added_at, media_id, cover_ext
            FROM entries
            ORDER BY added_at DESC, code DESC
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxCode = cursor.getColumnIndexOrThrow("code")
            val idxTitle = cursor.getColumnIndexOrThrow("title")
            val idxSubtitle = cursor.getColumnIndexOrThrow("subtitle")
            val idxSource = cursor.getColumnIndexOrThrow("source_url")
            val idxPages = cursor.getColumnIndexOrThrow("num_pages")
            val idxUpload = cursor.getColumnIndexOrThrow("upload_date")
            val idxRating = cursor.getColumnIndexOrThrow("rating")
            val idxRead = cursor.getColumnIndexOrThrow("read_state")
            val idxReadAt = cursor.getColumnIndexOrThrow("read_at")
            val idxPinned = cursor.getColumnIndexOrThrow("pinned")
            val idxFetched = cursor.getColumnIndexOrThrow("fetched_at")
            val idxAdded = cursor.getColumnIndexOrThrow("added_at")
            val idxMediaId = cursor.getColumnIndexOrThrow("media_id")
            val idxCoverExt = cursor.getColumnIndexOrThrow("cover_ext")

            while (cursor.moveToNext()) {
                val code = cursor.getInt(idxCode)
                val tags = JSONArray()
                tagsByCode[code].orEmpty().forEach { tags.put(it) }
                val entry = JSONObject()
                    .put("code", code)
                    .put("title", cursor.getString(idxTitle) ?: "Gallery $code")
                    .put("subtitle", cursor.getString(idxSubtitle) ?: "")
                    .put("source_url", cursor.getString(idxSource) ?: "https://nhentai.net/g/$code/")
                    .put("num_pages", cursor.getInt(idxPages).coerceAtLeast(0))
                    .put("upload_date", cursor.getString(idxUpload) ?: "")
                    .put("rating", cursor.getInt(idxRating).coerceIn(0, 5))
                    .put("read", if (cursor.getInt(idxRead) != 0) 1 else 0)
                    .put("read_at", cursor.getString(idxReadAt) ?: "")
                    .put("pinned", if (cursor.getInt(idxPinned) != 0) 1 else 0)
                    .put("fetched_at", cursor.getString(idxFetched) ?: "")
                    .put("added_at", cursor.getString(idxAdded) ?: "")
                    .put("media_id", cursor.getLong(idxMediaId).coerceAtLeast(0L))
                    .put("cover_ext", parseCoverExtension(cursor.getString(idxCoverExt) ?: ""))
                    .put("tags", tags)
                entriesArray.put(entry)
            }
        }

        readableDatabase.rawQuery(
            """
            SELECT name, type, COALESCE(source_url, '') AS source_url, COALESCE(pinned, 0) AS pinned
            FROM tags
            WHERE type IN ('artist', 'group')
            ORDER BY type ASC, name ASC
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxName = cursor.getColumnIndexOrThrow("name")
            val idxType = cursor.getColumnIndexOrThrow("type")
            val idxSourceUrl = cursor.getColumnIndexOrThrow("source_url")
            val idxPinned = cursor.getColumnIndexOrThrow("pinned")
            while (cursor.moveToNext()) {
                creatorsArray.put(
                    JSONObject()
                        .put("name", cursor.getString(idxName) ?: "")
                        .put("type", cursor.getString(idxType) ?: "")
                        .put("source_url", cursor.getString(idxSourceUrl) ?: "")
                        .put("pinned", if (cursor.getInt(idxPinned) != 0) 1 else 0)
                )
            }
        }

        return JSONObject()
            .put("version", 5)
            .put("entries", entriesArray)
            .put("creators", creatorsArray)
            .put("subscriptions", exportSubscriptionsSnapshot())
            .put("subscription_seen_codes", exportSubscriptionSeenCodesSnapshot())
            .put("subscription_events", exportSubscriptionEventsSnapshot())
            .put("entry_heatmap_cache", exportEntryHeatmapCacheSnapshot())
    }

    /**
     * Builds the small, read-only subset used by the recommendation profile.
     * Unlike a backup export this never materializes unread/unrated entries,
     * creator exports, settings, sessions, or backup metadata.
     */
    fun exportSuggestionProfileSnapshot(): JSONObject {
        val entriesArray = JSONArray()
        val rowsByCode = linkedMapOf<Int, JSONObject>()
        readableDatabase.rawQuery(
            """
            SELECT code, title, num_pages, rating, read_state, media_id, cover_ext
            FROM entries
            WHERE COALESCE(read_state, 0) != 0 OR COALESCE(rating, 0) > 0
            ORDER BY code ASC
            """.trimIndent(),
            null
        ).use { cursor ->
            val codeIndex = cursor.getColumnIndexOrThrow("code")
            val titleIndex = cursor.getColumnIndexOrThrow("title")
            val pagesIndex = cursor.getColumnIndexOrThrow("num_pages")
            val ratingIndex = cursor.getColumnIndexOrThrow("rating")
            val readIndex = cursor.getColumnIndexOrThrow("read_state")
            val mediaIdIndex = cursor.getColumnIndexOrThrow("media_id")
            val coverExtIndex = cursor.getColumnIndexOrThrow("cover_ext")
            while (cursor.moveToNext()) {
                val code = cursor.getInt(codeIndex)
                val row = JSONObject()
                    .put("code", code)
                    .put("title", cursor.getString(titleIndex) ?: "Gallery $code")
                    .put("num_pages", cursor.getInt(pagesIndex).coerceAtLeast(0))
                    .put("rating", cursor.getInt(ratingIndex).coerceIn(0, 5))
                    .put("read", if (cursor.getInt(readIndex) != 0) 1 else 0)
                    .put("media_id", cursor.getLong(mediaIdIndex).coerceAtLeast(0L))
                    .put("cover_ext", parseCoverExtension(cursor.getString(coverExtIndex) ?: ""))
                    .put("tags", JSONArray())
                rowsByCode[code] = row
                entriesArray.put(row)
            }
        }
        if (rowsByCode.isNotEmpty()) {
            readableDatabase.rawQuery(
                """
                SELECT et.entry_code AS code, t.name AS name, t.type AS type
                FROM entry_tags et
                JOIN tags t ON t.id = et.tag_id
                JOIN entries e ON e.code = et.entry_code
                WHERE COALESCE(e.read_state, 0) != 0 OR COALESCE(e.rating, 0) > 0
                ORDER BY et.entry_code ASC, t.type ASC, t.name ASC
                """.trimIndent(),
                null
            ).use { cursor ->
                val codeIndex = cursor.getColumnIndexOrThrow("code")
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                val typeIndex = cursor.getColumnIndexOrThrow("type")
                while (cursor.moveToNext()) {
                    rowsByCode[cursor.getInt(codeIndex)]
                        ?.getJSONArray("tags")
                        ?.put(
                            JSONObject()
                                .put("name", cursor.getString(nameIndex) ?: "")
                                .put("type", cursor.getString(typeIndex) ?: "tag")
                        )
                }
            }
        }
        return JSONObject().put("entries", entriesArray)
    }

    fun suggestionLibraryRevision(): String {
        readableDatabase.rawQuery(
            """
            SELECT
                COUNT(*) AS entry_count,
                COALESCE(SUM(code), 0) AS code_sum,
                COALESCE(SUM(COALESCE(rating, 0)), 0) AS rating_sum,
                COALESCE(SUM(CASE WHEN COALESCE(read_state, 0) != 0 THEN 1 ELSE 0 END), 0) AS read_count,
                COALESCE(MAX(COALESCE(fetched_at, '')), '') AS latest_fetch
            FROM entries
            """.trimIndent(),
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return "0:0:0:0:"
            return listOf(
                cursor.getLong(cursor.getColumnIndexOrThrow("entry_count")),
                cursor.getLong(cursor.getColumnIndexOrThrow("code_sum")),
                cursor.getLong(cursor.getColumnIndexOrThrow("rating_sum")),
                cursor.getLong(cursor.getColumnIndexOrThrow("read_count")),
                cursor.getString(cursor.getColumnIndexOrThrow("latest_fetch")) ?: ""
            ).joinToString(":")
        }
    }

    fun exportSubscriptionsSnapshot(): JSONArray {
        val rows = JSONArray()
        readableDatabase.rawQuery(
            """
            SELECT
                route_name,
                route_type,
                COALESCE(notifications_enabled, 1) AS notifications_enabled,
                COALESCE(notification_dot_enabled, 1) AS notification_dot_enabled,
                COALESCE(initialized, 0) AS initialized,
                COALESCE(created_at, '') AS created_at,
                COALESCE(last_checked_at, '') AS last_checked_at
            FROM subscriptions
            ORDER BY LOWER(route_type) ASC, LOWER(route_name) ASC
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxName = cursor.getColumnIndexOrThrow("route_name")
            val idxType = cursor.getColumnIndexOrThrow("route_type")
            val idxNotifications = cursor.getColumnIndexOrThrow("notifications_enabled")
            val idxDot = cursor.getColumnIndexOrThrow("notification_dot_enabled")
            val idxInitialized = cursor.getColumnIndexOrThrow("initialized")
            val idxCreatedAt = cursor.getColumnIndexOrThrow("created_at")
            val idxLastCheckedAt = cursor.getColumnIndexOrThrow("last_checked_at")
            while (cursor.moveToNext()) {
                rows.put(
                    JSONObject()
                        .put("route_name", cursor.getString(idxName)?.trim().orEmpty())
                        .put("route_type", cursor.getString(idxType)?.trim().orEmpty())
                        .put("notifications_enabled", if (cursor.getInt(idxNotifications) != 0) 1 else 0)
                        .put("notification_dot_enabled", if (cursor.getInt(idxDot) != 0) 1 else 0)
                        .put("initialized", if (cursor.getInt(idxInitialized) != 0) 1 else 0)
                        .put("created_at", cursor.getString(idxCreatedAt).orEmpty())
                        .put("last_checked_at", cursor.getString(idxLastCheckedAt).orEmpty())
                )
            }
        }
        return rows
    }

    fun exportSubscriptionSeenCodesSnapshot(): JSONArray {
        val rows = JSONArray()
        readableDatabase.rawQuery(
            """
            SELECT
                s.route_name,
                s.route_type,
                sc.code,
                COALESCE(sc.seen_at, '') AS seen_at
            FROM subscription_seen_codes sc
            JOIN subscriptions s ON s.id = sc.subscription_id
            ORDER BY LOWER(s.route_type) ASC, LOWER(s.route_name) ASC, sc.code ASC
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxName = cursor.getColumnIndexOrThrow("route_name")
            val idxType = cursor.getColumnIndexOrThrow("route_type")
            val idxCode = cursor.getColumnIndexOrThrow("code")
            val idxSeenAt = cursor.getColumnIndexOrThrow("seen_at")
            while (cursor.moveToNext()) {
                rows.put(
                    JSONObject()
                        .put("route_name", cursor.getString(idxName)?.trim().orEmpty())
                        .put("route_type", cursor.getString(idxType)?.trim().orEmpty())
                        .put("code", cursor.getInt(idxCode))
                        .put("seen_at", cursor.getString(idxSeenAt).orEmpty())
                )
            }
        }
        return rows
    }

    fun exportSubscriptionEventsSnapshot(): JSONArray {
        val rows = JSONArray()
        readableDatabase.rawQuery(
            """
            SELECT
                s.route_name,
                s.route_type,
                e.code,
                COALESCE(e.title, '') AS title,
                COALESCE(e.thumbnail_url, '') AS thumbnail_url,
                COALESCE(e.num_pages, 0) AS num_pages,
                COALESCE(e.upload_date, '') AS upload_date,
                COALESCE(e.source_url, '') AS source_url,
                COALESCE(e.discovered_at, '') AS discovered_at,
                COALESCE(e.dismissed, 0) AS dismissed,
                COALESCE(e.pinned, 0) AS pinned
            FROM subscription_events e
            JOIN subscriptions s ON s.id = e.subscription_id
            ORDER BY COALESCE(e.discovered_at, '') ASC, e.code ASC
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxName = cursor.getColumnIndexOrThrow("route_name")
            val idxType = cursor.getColumnIndexOrThrow("route_type")
            val idxCode = cursor.getColumnIndexOrThrow("code")
            val idxTitle = cursor.getColumnIndexOrThrow("title")
            val idxThumb = cursor.getColumnIndexOrThrow("thumbnail_url")
            val idxPages = cursor.getColumnIndexOrThrow("num_pages")
            val idxUpload = cursor.getColumnIndexOrThrow("upload_date")
            val idxSource = cursor.getColumnIndexOrThrow("source_url")
            val idxDiscovered = cursor.getColumnIndexOrThrow("discovered_at")
            val idxDismissed = cursor.getColumnIndexOrThrow("dismissed")
            val idxPinned = cursor.getColumnIndexOrThrow("pinned")
            while (cursor.moveToNext()) {
                rows.put(
                    JSONObject()
                        .put("route_name", cursor.getString(idxName)?.trim().orEmpty())
                        .put("route_type", cursor.getString(idxType)?.trim().orEmpty())
                        .put("code", cursor.getInt(idxCode))
                        .put("title", cursor.getString(idxTitle).orEmpty())
                        .put("thumbnail_url", cursor.getString(idxThumb).orEmpty())
                        .put("num_pages", cursor.getInt(idxPages).coerceAtLeast(0))
                        .put("upload_date", cursor.getString(idxUpload).orEmpty())
                        .put("source_url", cursor.getString(idxSource).orEmpty())
                        .put("discovered_at", cursor.getString(idxDiscovered).orEmpty())
                        .put("dismissed", if (cursor.getInt(idxDismissed) != 0) 1 else 0)
                        .put("pinned", if (cursor.getInt(idxPinned) != 0) 1 else 0)
                )
            }
        }
        return rows
    }

    fun exportDailyReadActivitySnapshot(): JSONArray {
        val rows = JSONArray()
        readableDatabase.rawQuery(
            """
            SELECT activity_date, pages_read, entries_read
            FROM daily_read_activity
            ORDER BY activity_date ASC
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxDate = cursor.getColumnIndexOrThrow("activity_date")
            val idxPages = cursor.getColumnIndexOrThrow("pages_read")
            val idxEntries = cursor.getColumnIndexOrThrow("entries_read")
            while (cursor.moveToNext()) {
                rows.put(
                    JSONObject()
                        .put("activity_date", cursor.getString(idxDate)?.trim().orEmpty())
                        .put("pages_read", cursor.getInt(idxPages).coerceAtLeast(0))
                        .put("entries_read", cursor.getInt(idxEntries).coerceAtLeast(0))
                )
            }
        }
        return rows
    }

    fun exportReadingSessionsSnapshot(): JSONArray {
        val rows = JSONArray()
        readableDatabase.rawQuery(
            """
            SELECT started_at, ended_at, day_key, entry_code, pages_viewed, seconds_elapsed,
                   COALESCE(rating, 0) AS rating,
                   COALESCE(is_reread, 0) AS is_reread
            FROM reading_sessions
            ORDER BY started_at ASC, id ASC
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxStartedAt = cursor.getColumnIndexOrThrow("started_at")
            val idxEndedAt = cursor.getColumnIndexOrThrow("ended_at")
            val idxDayKey = cursor.getColumnIndexOrThrow("day_key")
            val idxEntryCode = cursor.getColumnIndexOrThrow("entry_code")
            val idxPages = cursor.getColumnIndexOrThrow("pages_viewed")
            val idxSeconds = cursor.getColumnIndexOrThrow("seconds_elapsed")
            val idxRating = cursor.getColumnIndexOrThrow("rating")
            val idxIsReread = cursor.getColumnIndexOrThrow("is_reread")
            while (cursor.moveToNext()) {
                rows.put(
                    JSONObject()
                        .put("started_at", cursor.getString(idxStartedAt)?.trim().orEmpty())
                        .put("ended_at", cursor.getString(idxEndedAt)?.trim().orEmpty())
                        .put("day_key", cursor.getString(idxDayKey)?.trim().orEmpty())
                        .put("entry_code", cursor.getInt(idxEntryCode).coerceAtLeast(0))
                        .put("pages_viewed", cursor.getInt(idxPages).coerceAtLeast(0))
                        .put("seconds_elapsed", cursor.getLong(idxSeconds).coerceAtLeast(0L))
                        .put("rating", cursor.getInt(idxRating).coerceIn(0, 5))
                        .put("is_reread", if (cursor.getInt(idxIsReread) != 0) 1 else 0)
                )
            }
        }
        return rows
    }

    fun exportPopularTagsSnapshot(): JSONArray {
        val rows = JSONArray()
        readableDatabase.rawQuery(
            """
            SELECT name, type, normalized_name, tag_count, blocked
            FROM popular_tags
            ORDER BY type ASC, tag_count DESC, name ASC
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxName = cursor.getColumnIndexOrThrow("name")
            val idxType = cursor.getColumnIndexOrThrow("type")
            val idxNormalized = cursor.getColumnIndexOrThrow("normalized_name")
            val idxCount = cursor.getColumnIndexOrThrow("tag_count")
            val idxBlocked = cursor.getColumnIndexOrThrow("blocked")
            while (cursor.moveToNext()) {
                rows.put(
                    JSONObject()
                        .put("name", cursor.getString(idxName)?.trim().orEmpty())
                        .put("type", cursor.getString(idxType)?.trim().orEmpty())
                        .put("normalized_name", cursor.getString(idxNormalized)?.trim().orEmpty())
                        .put("tag_count", cursor.getInt(idxCount).coerceAtLeast(0))
                        .put("blocked", if (cursor.getInt(idxBlocked) != 0) 1 else 0)
                )
            }
        }
        return rows
    }

    private data class SubscriptionImportCounts(
        val subscriptions: Int,
        val seenCodes: Int,
        val events: Int
    )

    private fun replaceSubscriptionsFromSnapshot(
        db: SQLiteDatabase,
        subscriptions: JSONArray?,
        seenCodes: JSONArray?,
        events: JSONArray?
    ): SubscriptionImportCounts {
        db.delete("subscription_events", null, null)
        db.delete("subscription_seen_codes", null, null)
        db.delete("subscriptions", null, null)

        if (subscriptions == null && seenCodes == null && events == null) {
            return SubscriptionImportCounts(0, 0, 0)
        }

        val routeIdByKey = mutableMapOf<String, Long>()
        var importedSubscriptions = 0
        var importedSeenCodes = 0
        var importedEvents = 0

        fun ensureSubscription(routeTypeRaw: String, routeNameRaw: String): Long? {
            val routeType = normalizeSubscriptionRouteType(routeTypeRaw)
            val routeName = normalizeSubscriptionRouteName(routeType, routeNameRaw)
            val routeKey = subscriptionRouteKey(routeType, routeName)
            if (routeType.isBlank() || routeName.isBlank() || routeKey.isBlank()) return null
            routeIdByKey[routeKey]?.let { return it }
            val existing = findSubscriptionByKey(routeKey)
            if (existing != null) {
                routeIdByKey[routeKey] = existing.id
                return existing.id
            }
            val values = ContentValues().apply {
                put("route_name", routeName)
                put("route_type", routeType)
                put("route_key", routeKey)
                put("created_at", utcNowString())
            }
            val rowId = db.insertWithOnConflict("subscriptions", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            val resolvedId = if (rowId != -1L) rowId else findSubscriptionByKey(routeKey)?.id
            if (resolvedId != null) {
                routeIdByKey[routeKey] = resolvedId
            }
            return resolvedId
        }

        for (idx in 0 until (subscriptions?.length() ?: 0)) {
            val obj = subscriptions?.optJSONObject(idx) ?: continue
            val routeType = obj.optString("route_type", "")
            val routeName = obj.optString("route_name", "")
            val subscriptionId = ensureSubscription(routeType, routeName) ?: continue
            val values = ContentValues().apply {
                put("notifications_enabled", if (obj.optInt("notifications_enabled", 1) != 0) 1 else 0)
                put("notification_dot_enabled", if (obj.optInt("notification_dot_enabled", 1) != 0) 1 else 0)
                put("initialized", if (obj.optInt("initialized", 0) != 0) 1 else 0)
                put("created_at", obj.optString("created_at", "").trim())
                put("last_checked_at", obj.optString("last_checked_at", "").trim())
            }
            db.update("subscriptions", values, "id = ?", arrayOf(subscriptionId.toString()))
            importedSubscriptions += 1
        }

        for (idx in 0 until (seenCodes?.length() ?: 0)) {
            val obj = seenCodes?.optJSONObject(idx) ?: continue
            val subscriptionId = ensureSubscription(
                obj.optString("route_type", ""),
                obj.optString("route_name", "")
            ) ?: continue
            val code = obj.optInt("code", 0).coerceAtLeast(0)
            if (code <= 0) continue
            val values = ContentValues().apply {
                put("subscription_id", subscriptionId)
                put("code", code)
                put("seen_at", obj.optString("seen_at", "").trim())
            }
            db.insertWithOnConflict("subscription_seen_codes", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            importedSeenCodes += 1
        }

        for (idx in 0 until (events?.length() ?: 0)) {
            val obj = events?.optJSONObject(idx) ?: continue
            val subscriptionId = ensureSubscription(
                obj.optString("route_type", ""),
                obj.optString("route_name", "")
            ) ?: continue
            val code = obj.optInt("code", 0).coerceAtLeast(0)
            if (code <= 0) continue
            val values = ContentValues().apply {
                put("subscription_id", subscriptionId)
                put("code", code)
                put("title", obj.optString("title", "").trim())
                put("thumbnail_url", obj.optString("thumbnail_url", "").trim())
                put("num_pages", obj.optInt("num_pages", 0).coerceAtLeast(0))
                put("upload_date", obj.optString("upload_date", "").trim())
                put("source_url", obj.optString("source_url", "").trim())
                put("discovered_at", obj.optString("discovered_at", "").trim())
                put("dismissed", if (obj.optInt("dismissed", 0) != 0) 1 else 0)
                put("pinned", if (obj.optInt("pinned", 0) != 0) 1 else 0)
            }
            db.insertWithOnConflict("subscription_events", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            importedEvents += 1
        }

        return SubscriptionImportCounts(
            subscriptions = importedSubscriptions,
            seenCodes = importedSeenCodes,
            events = importedEvents
        )
    }

    private fun replaceDailyReadActivityFromSnapshot(
        db: SQLiteDatabase,
        rows: JSONArray
    ): Int {
        db.delete("daily_read_activity", null, null)
        var imported = 0
        for (idx in 0 until rows.length()) {
            val obj = rows.optJSONObject(idx) ?: continue
            val dayKeyRaw = obj.optString("activity_date", "").trim()
                .ifBlank { obj.optString("date", "").trim() }
            val dayKey = normalizeDayKey(dayKeyRaw) ?: continue
            val pagesRead = when (val raw = obj.opt("pages_read")) {
                is Number -> raw.toInt()
                is String -> raw.trim().toIntOrNull() ?: 0
                else -> obj.optInt("pages_read", 0)
            }.coerceAtLeast(0)
            val entriesRead = when (val raw = obj.opt("entries_read")) {
                is Number -> raw.toInt()
                is String -> raw.trim().toIntOrNull() ?: 0
                else -> obj.optInt("entries_read", 0)
            }.coerceAtLeast(0)
            if (pagesRead == 0 && entriesRead == 0) continue
            upsertDailyReadActivity(
                db = db,
                dayKey = dayKey,
                pagesRead = pagesRead,
                entriesRead = entriesRead
            )
            imported += 1
        }
        return imported
    }

    private fun replaceReadingSessionsFromSnapshot(
        db: SQLiteDatabase,
        rows: JSONArray
    ): Int {
        db.delete("reading_sessions", null, null)
        var imported = 0
        for (idx in 0 until rows.length()) {
            val obj = rows.optJSONObject(idx) ?: continue
            val entryCode = when (val raw = obj.opt("entry_code")) {
                is Number -> raw.toInt()
                is String -> raw.trim().toIntOrNull() ?: 0
                else -> obj.optInt("entry_code", 0)
            }.coerceAtLeast(0)
            if (entryCode <= 0) continue

            val startedAtRaw = obj.optString("started_at", "").trim()
            val endedAtRaw = obj.optString("ended_at", "").trim()
            val dayKeyRaw = obj.optString("day_key", "").trim()
            val dayKey = normalizeDayKey(dayKeyRaw)
                ?: normalizeDayKey(startedAtRaw)
                ?: normalizeDayKey(endedAtRaw)
                ?: continue

            val pagesViewed = when (val raw = obj.opt("pages_viewed")) {
                is Number -> raw.toInt()
                is String -> raw.trim().toIntOrNull() ?: 0
                else -> obj.optInt("pages_viewed", 0)
            }.coerceAtLeast(1)
            val secondsElapsed = when (val raw = obj.opt("seconds_elapsed")) {
                is Number -> raw.toLong()
                is String -> raw.trim().toLongOrNull() ?: 0L
                else -> obj.optLong("seconds_elapsed", 0L)
            }.coerceAtLeast(1L)
            val rating = when (val raw = obj.opt("rating")) {
                is Number -> raw.toInt()
                is String -> raw.trim().toIntOrNull() ?: 0
                else -> obj.optInt("rating", 0)
            }.coerceIn(0, 5)
            val isReread = when (val raw = obj.opt("is_reread")) {
                is Boolean -> raw
                is Number -> raw.toInt() != 0
                is String -> raw.trim().lowercase(Locale.US) in setOf("true", "1", "yes", "on")
                else -> obj.optInt("is_reread", 0) != 0
            }
            val startedAt = startedAtRaw.ifBlank { "$dayKey 00:00:00" }
            val endedAt = endedAtRaw.ifBlank { startedAt }

            val values = ContentValues().apply {
                put("started_at", startedAt)
                put("ended_at", endedAt)
                put("day_key", dayKey)
                put("entry_code", entryCode)
                put("pages_viewed", pagesViewed)
                put("seconds_elapsed", secondsElapsed)
                put("rating", rating)
                put("is_reread", if (isReread) 1 else 0)
            }
            db.insert("reading_sessions", null, values)
            imported += 1
        }
        return imported
    }

    private fun replacePopularTagsFromSnapshot(
        db: SQLiteDatabase,
        rows: JSONArray
    ): Int {
        db.delete("popular_tags", null, null)
        var imported = 0
        for (idx in 0 until rows.length()) {
            val obj = rows.optJSONObject(idx) ?: continue
            val name = obj.optString("name", "").trim()
            val type = obj.optString("type", "").trim().lowercase(Locale.US)
            val normalized = obj.optString("normalized_name", "").trim().ifBlank { normalizeTagName(name) }
            if (name.isBlank() || normalized.isBlank() || type.isBlank()) continue
            val count = when (val raw = obj.opt("tag_count")) {
                is Number -> raw.toInt()
                is String -> raw.trim().toIntOrNull() ?: 0
                else -> obj.optInt("tag_count", 0)
            }.coerceAtLeast(0)
            val blocked = if (obj.optInt("blocked", 0) != 0) 1 else 0
            val values = ContentValues().apply {
                put("name", name)
                put("type", type)
                put("normalized_name", normalized)
                put("tag_count", count)
                put("blocked", blocked)
            }
            db.insertWithOnConflict("popular_tags", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            imported += 1
        }
        return imported
    }

    fun importSnapshot(
        entries: JSONArray,
        creators: JSONArray = JSONArray(),
        popularTags: JSONArray? = null,
        entryHeatmapCache: JSONArray? = null,
        subscriptions: JSONArray? = null,
        subscriptionSeenCodes: JSONArray? = null,
        subscriptionEvents: JSONArray? = null,
        dailyReadActivity: JSONArray? = null,
        readingSessions: JSONArray? = null
    ): ImportResult {
        val db = writableDatabase
        val now = utcNowString()

        var processed = 0
        var imported = 0
        var inserted = 0
        var updated = 0
        var skipped = 0
        val insertedCodes = mutableListOf<Int>()
        var creatorsProcessed = 0
        var creatorsAdded = 0
        var creatorsSkipped = 0
        var creatorsDuplicates = 0
        var artistsProcessed = 0
        var artistsAdded = 0
        var artistsDuplicates = 0
        var artistsSkipped = 0
        var groupsProcessed = 0
        var groupsAdded = 0
        var groupsDuplicates = 0
        var groupsSkipped = 0
        var importedPopularTagRows: Int? = null
        var importedEntryHeatmapCacheRows: Int? = null
        var importedSubscriptionRows: Int? = null
        var importedSubscriptionSeenRows: Int? = null
        var importedSubscriptionEventRows: Int? = null
        var importedDailyReadActivityRows: Int? = null
        var importedReadingSessionRows: Int? = null

        db.beginTransaction()
        try {
            for (idx in 0 until entries.length()) {
                processed += 1
                val raw = entries.opt(idx)
                val obj = raw as? JSONObject
                if (obj == null) {
                    skipped += 1
                    continue
                }

                val code = obj.optInt("code", 0)
                if (code <= 0) {
                    skipped += 1
                    continue
                }

                val title = obj.optString("title", "").trim().ifBlank { "Gallery $code" }
                val subtitle = obj.optString("subtitle", "")
                val sourceUrl = obj.optString("source_url", "").ifBlank { "https://nhentai.net/g/$code/" }
                val numPages = obj.optInt("num_pages", 0).coerceAtLeast(0)
                val uploadDate = obj.optString("upload_date", "")
                val fetchedAt = obj.optString("fetched_at", "").ifBlank { now }
                val addedAt = obj.optString("added_at", "").ifBlank { now }
                val rating = obj.optInt("rating", 0).coerceIn(0, 5)
                val hasReadState = obj.has("read")
                val readState = when {
                    hasReadState -> if (obj.optInt("read", 0) != 0) 1 else 0
                    rating > 0 -> 1
                    else -> 0
                }
                val rawReadAt = obj.optString("read_at", "").trim()
                val readAt = when {
                    readState != 0 -> rawReadAt.ifBlank { addedAt }
                    else -> ""
                }
                val hasPinned = obj.has("pinned")
                val pinned = if (obj.optInt("pinned", 0) != 0) 1 else 0
                val mediaId = parseMediaId(obj.opt("media_id"))
                val coverExt = parseCoverExtension(obj.optString("cover_ext", ""))

                val exists = entryExists(db, code)

                if (exists) {
                    val values = ContentValues().apply {
                        put("title", title)
                        put("subtitle", subtitle)
                        put("source_url", sourceUrl)
                        put("num_pages", numPages)
                        put("upload_date", uploadDate)
                        put("rating", rating)
                        if (hasReadState || rating > 0) {
                            put("read_state", readState)
                            put("read_at", readAt)
                        } else if (rawReadAt.isNotBlank()) {
                            put("read_at", readAt)
                        }
                        if (hasPinned) {
                            put("pinned", pinned)
                        }
                        put("fetched_at", fetchedAt)
                        put("added_at", addedAt)
                        put("media_id", mediaId)
                        put("cover_ext", coverExt)
                    }
                    db.update("entries", values, "code = ?", arrayOf(code.toString()))
                } else {
                    val values = ContentValues().apply {
                        put("code", code)
                        put("title", title)
                        put("subtitle", subtitle)
                        put("source_url", sourceUrl)
                        put("num_pages", numPages)
                        put("upload_date", uploadDate)
                        put("rating", rating)
                        put("read_state", readState)
                        put("read_at", readAt)
                        put("pinned", pinned)
                        put("fetched_at", fetchedAt)
                        put("added_at", addedAt)
                        put("media_id", mediaId)
                        put("cover_ext", coverExt)
                    }
                    db.insertOrThrow("entries", null, values)
                }

                db.delete("entry_tags", "entry_code = ?", arrayOf(code.toString()))

                val deduped = LinkedHashMap<Pair<String, String>, Pair<String, String>>()
                val tags = obj.optJSONArray("tags") ?: JSONArray()
                for (tagIdx in 0 until tags.length()) {
                    val tagRaw = tags.opt(tagIdx)
                    val (tagName, tagType) = when (tagRaw) {
                        is JSONObject -> {
                            val name = tagRaw.optString("name", "").trim()
                            val type = tagRaw.optString("type", "tag").trim().lowercase(Locale.US).ifBlank { "tag" }
                            name to type
                        }
                        else -> {
                            val name = tagRaw?.toString()?.trim().orEmpty()
                            name to "tag"
                        }
                    }
                    val normalized = normalizeTagName(tagName)
                    if (normalized.isBlank()) continue
                    deduped[normalized to tagType] = tagName to tagType
                }

                deduped.forEach { (key, value) ->
                    val normalized = key.first
                    val tagType = key.second
                    val tagName = value.first

                    val tagValues = ContentValues().apply {
                        put("name", tagName)
                        put("type", tagType)
                        put("normalized_name", normalized)
                    }
                    db.insertWithOnConflict("tags", null, tagValues, SQLiteDatabase.CONFLICT_IGNORE)

                    val tagId = findTagId(db, normalized, tagType) ?: return@forEach
                    val linkValues = ContentValues().apply {
                        put("entry_code", code)
                        put("tag_id", tagId)
                    }
                    db.insertWithOnConflict("entry_tags", null, linkValues, SQLiteDatabase.CONFLICT_IGNORE)
                }

                imported += 1
                if (exists) {
                    updated += 1
                } else {
                    inserted += 1
                    insertedCodes += code
                }
            }

            for (idx in 0 until creators.length()) {
                creatorsProcessed += 1
                val raw = creators.opt(idx)
                val obj = raw as? JSONObject
                if (obj == null) {
                    creatorsSkipped += 1
                    continue
                }

                val name = obj.optString("name", "").trim()
                val creatorType = obj.optString("type", "").trim().lowercase(Locale.US)
                val sourceUrl = obj.optString("source_url", "").trim()
                val hasSourceUrl = obj.has("source_url")
                val hasPinned = obj.has("pinned")
                val pinned = if (obj.optInt("pinned", 0) != 0) 1 else 0
                if (name.isBlank() || !isCreatorType(creatorType)) {
                    creatorsSkipped += 1
                    continue
                }

                if (creatorType == "artist") {
                    artistsProcessed += 1
                } else if (creatorType == "group") {
                    groupsProcessed += 1
                }

                when (
                    upsertCreatorTagFromImport(
                        db = db,
                        name = name,
                        creatorType = creatorType,
                        sourceUrl = sourceUrl,
                        hasSourceUrl = hasSourceUrl,
                        pinned = pinned,
                        hasPinned = hasPinned
                    )
                ) {
                    CreatorImportUpsertResult.INSERTED -> {
                        creatorsAdded += 1
                        if (creatorType == "artist") {
                            artistsAdded += 1
                        } else if (creatorType == "group") {
                            groupsAdded += 1
                        }
                    }
                    CreatorImportUpsertResult.DUPLICATE_OR_UPDATED -> {
                        creatorsDuplicates += 1
                        if (creatorType == "artist") {
                            artistsDuplicates += 1
                        } else if (creatorType == "group") {
                            groupsDuplicates += 1
                        }
                    }
                    CreatorImportUpsertResult.SKIPPED -> {
                        creatorsSkipped += 1
                        if (creatorType == "artist") {
                            artistsSkipped += 1
                        } else if (creatorType == "group") {
                            groupsSkipped += 1
                        }
                    }
                }
            }

            if (popularTags != null) {
                importedPopularTagRows = replacePopularTagsFromSnapshot(
                    db = db,
                    rows = popularTags
                )
            }

            importedEntryHeatmapCacheRows = replaceEntryHeatmapCacheFromSnapshot(
                db = db,
                rows = entryHeatmapCache
            )

            if (subscriptions != null || subscriptionSeenCodes != null || subscriptionEvents != null) {
                val importedSubscriptionCounts = replaceSubscriptionsFromSnapshot(
                    db = db,
                    subscriptions = subscriptions,
                    seenCodes = subscriptionSeenCodes,
                    events = subscriptionEvents
                )
                importedSubscriptionRows = importedSubscriptionCounts.subscriptions
                importedSubscriptionSeenRows = importedSubscriptionCounts.seenCodes
                importedSubscriptionEventRows = importedSubscriptionCounts.events
            }

            if (dailyReadActivity != null) {
                importedDailyReadActivityRows = replaceDailyReadActivityFromSnapshot(
                    db = db,
                    rows = dailyReadActivity
                )
            }
            if (readingSessions != null) {
                importedReadingSessionRows = replaceReadingSessionsFromSnapshot(
                    db = db,
                    rows = readingSessions
                )
            }

            cleanupOrphanTags(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        return ImportResult(
            processed = processed,
            imported = imported,
            inserted = inserted,
            updated = updated,
            skipped = skipped,
            insertedCodes = insertedCodes,
            creatorsProcessed = creatorsProcessed,
            creatorsAdded = creatorsAdded,
            creatorsSkipped = creatorsSkipped,
            creatorsDuplicates = creatorsDuplicates,
            artistsProcessed = artistsProcessed,
            artistsAdded = artistsAdded,
            artistsDuplicates = artistsDuplicates,
            artistsSkipped = artistsSkipped,
            groupsProcessed = groupsProcessed,
            groupsAdded = groupsAdded,
            groupsDuplicates = groupsDuplicates,
            groupsSkipped = groupsSkipped,
            popularTagRows = importedPopularTagRows,
            entryHeatmapCacheRows = importedEntryHeatmapCacheRows,
            subscriptionRows = importedSubscriptionRows,
            subscriptionSeenRows = importedSubscriptionSeenRows,
            subscriptionEventRows = importedSubscriptionEventRows,
            dailyReadActivityRows = importedDailyReadActivityRows,
            readingSessionRows = importedReadingSessionRows
        )
    }

    private enum class CreatorImportUpsertResult {
        INSERTED,
        DUPLICATE_OR_UPDATED,
        SKIPPED
    }

    private fun upsertCreatorTagFromImport(
        db: SQLiteDatabase,
        name: String,
        creatorType: String,
        sourceUrl: String,
        hasSourceUrl: Boolean,
        pinned: Int,
        hasPinned: Boolean
    ): CreatorImportUpsertResult {
        val cleanedName = name.trim().replace(Regex("\\s+"), " ")
        val normalizedName = normalizeTagName(cleanedName)
        val normalizedType = creatorType.trim().lowercase(Locale.US)
        val cleanedUrl = sourceUrl.trim()
        val safePinned = if (pinned != 0) 1 else 0
        if (normalizedName.isBlank() || !isCreatorType(normalizedType)) {
            return CreatorImportUpsertResult.SKIPPED
        }

        var existingId: Long? = null
        var existingPinned = 0
        var existingSourceUrl = ""
        db.rawQuery(
            """
            SELECT id, COALESCE(pinned, 0) AS pinned, COALESCE(source_url, '') AS source_url
            FROM tags
            WHERE normalized_name = ? AND type = ?
            """.trimIndent(),
            arrayOf(normalizedName, normalizedType)
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                existingId = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                existingPinned = cursor.getInt(cursor.getColumnIndexOrThrow("pinned"))
                existingSourceUrl = cursor.getString(cursor.getColumnIndexOrThrow("source_url")) ?: ""
            }
        }

        if (existingId != null) {
            val updates = ContentValues()
            if (hasPinned && existingPinned != safePinned) {
                updates.put("pinned", safePinned)
            } else if (!hasPinned && existingPinned == 0) {
                // Backward compatibility: old exports only had pinned creators.
                updates.put("pinned", 1)
            }
            if (hasSourceUrl) {
                if (existingSourceUrl != cleanedUrl) {
                    updates.put("source_url", cleanedUrl)
                }
            } else if (cleanedUrl.isNotBlank() && existingSourceUrl.isBlank()) {
                updates.put("source_url", cleanedUrl)
            }
            if (updates.size() > 0) {
                db.update("tags", updates, "id = ?", arrayOf(existingId.toString()))
            }
            return CreatorImportUpsertResult.DUPLICATE_OR_UPDATED
        }

        val values = ContentValues().apply {
            put("name", cleanedName)
            put("type", normalizedType)
            put("normalized_name", normalizedName)
            put("pinned", if (hasPinned) safePinned else 1)
            put("source_url", cleanedUrl)
        }
        db.insertOrThrow("tags", null, values)
        return CreatorImportUpsertResult.INSERTED
    }

    private fun upsertCreatorTag(
        db: SQLiteDatabase,
        name: String,
        creatorType: String,
        sourceUrl: String
    ): Boolean {
        val cleanedName = name.trim().replace(Regex("\\s+"), " ")
        val normalizedName = normalizeTagName(cleanedName)
        val normalizedType = creatorType.trim().lowercase(Locale.US)
        val cleanedUrl = sourceUrl.trim()
        if (normalizedName.isBlank() || !isCreatorType(normalizedType)) {
            return false
        }

        var existingId: Long? = null
        var existingPinned = 0
        var existingSourceUrl = ""
        db.rawQuery(
            """
            SELECT id, COALESCE(pinned, 0) AS pinned, COALESCE(source_url, '') AS source_url
            FROM tags
            WHERE normalized_name = ? AND type = ?
            """.trimIndent(),
            arrayOf(normalizedName, normalizedType)
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                existingId = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                existingPinned = cursor.getInt(cursor.getColumnIndexOrThrow("pinned"))
                existingSourceUrl = cursor.getString(cursor.getColumnIndexOrThrow("source_url")) ?: ""
            }
        }

        if (existingId != null) {
            val updates = ContentValues()
            if (existingPinned == 0) {
                updates.put("pinned", 1)
            }
            if (cleanedUrl.isNotBlank() && existingSourceUrl.isBlank()) {
                updates.put("source_url", cleanedUrl)
            }
            if (updates.size() > 0) {
                db.update("tags", updates, "id = ?", arrayOf(existingId.toString()))
            }
            return false
        }

        val values = ContentValues().apply {
            put("name", cleanedName)
            put("type", normalizedType)
            put("normalized_name", normalizedName)
            put("pinned", 1)
            put("source_url", cleanedUrl)
        }
        db.insertOrThrow("tags", null, values)
        return true
    }

    fun getTagName(tagId: Long): String? {
        readableDatabase.rawQuery(
            "SELECT name FROM tags WHERE id = ?",
            arrayOf(tagId.toString())
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return cursor.getString(0)
        }
    }

    fun findTagId(type: String, name: String): Long? {
        val normalizedType = type.trim().lowercase(Locale.US)
        val normalizedName = normalizeTagName(name)
        if (normalizedType.isBlank() || normalizedName.isBlank()) return null
        return findTagId(readableDatabase, normalizedName, normalizedType)
    }

    fun getTagRouteRef(tagId: Long): TagRouteRef? {
        if (tagId <= 0L) return null
        readableDatabase.rawQuery(
            "SELECT name, type FROM tags WHERE id = ? LIMIT 1",
            arrayOf(tagId.toString())
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            val name = cursor.getString(0)?.trim().orEmpty()
            val type = cursor.getString(1)?.trim().orEmpty().lowercase(Locale.US)
            if (name.isBlank() || type.isBlank()) return null
            return TagRouteRef(name = name, type = type)
        }
    }

    fun findDirectRouteTagByName(rawName: String): TagRouteRef? {
        val normalized = normalizeTagName(rawName)
        if (normalized.isBlank()) return null
        val allowedTypes = listOf("group", "artist", "language", "character", "parody", "category", "tag")
        val placeholders = allowedTypes.joinToString(",") { "?" }
        val args = mutableListOf(normalized).apply { addAll(allowedTypes) }
        val sql = """
            SELECT t.name, t.type, COUNT(et.entry_code) AS entry_count
            FROM tags t
            LEFT JOIN entry_tags et ON et.tag_id = t.id
            WHERE t.normalized_name = ?
              AND t.type IN ($placeholders)
            GROUP BY t.id, t.name, t.type
            ORDER BY
                CASE t.type
                    WHEN 'group' THEN 0
                    WHEN 'artist' THEN 1
                    WHEN 'language' THEN 2
                    WHEN 'character' THEN 3
                    WHEN 'parody' THEN 4
                    WHEN 'category' THEN 5
                    WHEN 'tag' THEN 6
                    ELSE 99
                END ASC,
                entry_count DESC,
                t.id ASC
            LIMIT 1
        """.trimIndent()
        readableDatabase.rawQuery(sql, args.toTypedArray()).use { cursor ->
            if (!cursor.moveToFirst()) return null
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))?.trim().orEmpty()
            val type = cursor.getString(cursor.getColumnIndexOrThrow("type"))?.trim().orEmpty().lowercase(Locale.US)
            if (name.isBlank() || type.isBlank()) return null
            return TagRouteRef(name = name, type = type)
        }
    }

    fun findCreatorId(type: String, name: String): Long? {
        val normalizedType = type.trim().lowercase(Locale.US)
        if (!isCreatorType(normalizedType)) return null
        val normalizedName = normalizeTagName(name)
        if (normalizedName.isBlank()) return null

        readableDatabase.rawQuery(
            """
            SELECT id
            FROM tags
            WHERE normalized_name = ? AND type = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(normalizedName, normalizedType)
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return cursor.getLong(0)
        }
    }

    private fun cleanupOrphanTags(db: SQLiteDatabase) {
        db.execSQL(
            """
            DELETE FROM tags
            WHERE id NOT IN (SELECT DISTINCT tag_id FROM entry_tags)
              AND COALESCE(pinned, 0) = 0
            """.trimIndent()
        )
    }

    private fun hasColumn(db: SQLiteDatabase, tableName: String, columnName: String): Boolean {
        db.rawQuery("PRAGMA table_info($tableName)", null).use { cursor ->
            val idxName = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                if ((cursor.getString(idxName) ?: "") == columnName) {
                    return true
                }
            }
        }
        return false
    }

    private fun isCreatorType(type: String): Boolean {
        return type == "artist" || type == "group"
    }

    private fun entryExists(db: SQLiteDatabase, code: Int): Boolean {
        db.rawQuery("SELECT 1 FROM entries WHERE code = ?", arrayOf(code.toString())).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun findTagId(db: SQLiteDatabase, normalizedName: String, type: String): Long? {
        db.rawQuery(
            "SELECT id FROM tags WHERE normalized_name = ? AND type = ?",
            arrayOf(normalizedName, type)
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return cursor.getLong(0)
        }
    }
}

