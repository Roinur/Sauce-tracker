package com.roinur.saucetracker.data.profile

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.roinur.saucetracker.data.database.SauceTrackerDatabase
import com.roinur.saucetracker.data.source.SourceEntryKey
import com.roinur.saucetracker.data.source.SourceId
import java.time.Instant
import java.util.Locale
import java.util.UUID

internal class ProfileStore(private val database: SauceTrackerDatabase) {
    fun activeProfileId(): String = database.readableDatabase.rawQuery(
        "SELECT active_profile_id FROM app_state WHERE slot_id = 1",
        null
    ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0).orEmpty().ifBlank { MAIN_PROFILE_ID } else MAIN_PROFILE_ID }

    fun setActiveProfile(profileId: String): Boolean {
        if (profile(profileId) == null) return false
        val now = now()
        val db = database.writableDatabase
        db.beginTransaction()
        return try {
            db.execSQL("UPDATE app_state SET active_profile_id = ? WHERE slot_id = 1", arrayOf(profileId))
            db.execSQL("UPDATE profiles SET last_used_at = ? WHERE id = ?", arrayOf(now, profileId))
            db.setTransactionSuccessful()
            true
        } finally {
            db.endTransaction()
        }
    }

    fun profiles(): List<LibraryProfile> {
        val sourceMap = linkedMapOf<String, MutableSet<SourceId>>()
        database.readableDatabase.rawQuery("SELECT profile_id, source_id FROM profile_sources ORDER BY source_id", null).use { cursor ->
            while (cursor.moveToNext()) sourceMap.getOrPut(cursor.getString(0)) { linkedSetOf() }.add(SourceId(cursor.getString(1)))
        }
        return database.readableDatabase.rawQuery(
            "SELECT id, name, kind, created_at, last_used_at, is_main FROM profiles ORDER BY is_main DESC, LOWER(name), id",
            null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(
                    LibraryProfile(
                        id = cursor.getString(0),
                        name = cursor.getString(1),
                        kind = runCatching { ProfileKind.valueOf(cursor.getString(2)) }.getOrDefault(ProfileKind.COMBINED),
                        sourceIds = sourceMap[cursor.getString(0)].orEmpty(),
                        createdAt = cursor.getString(3),
                        lastUsedAt = cursor.getString(4),
                        isMain = cursor.getInt(5) != 0
                    )
                )
            }
        }
    }

    fun profile(profileId: String): LibraryProfile? = profiles().firstOrNull { it.id == profileId }

    fun create(name: String, kind: ProfileKind, sources: Set<SourceId>): LibraryProfile {
        val cleanName = name.trim().take(60).ifBlank { "Profile" }
        require(sources.isNotEmpty()) { "A profile needs at least one source." }
        require(kind != ProfileKind.SOURCE_LOCKED || sources.size == 1) { "A source-locked profile needs exactly one source." }
        val id = UUID.randomUUID().toString()
        val timestamp = now()
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("id", id); put("name", cleanName); put("kind", kind.name)
                put("created_at", timestamp); put("last_used_at", timestamp); put("is_main", 0)
            }
            db.insertOrThrow("profiles", null, values)
            sources.forEach { sourceId ->
                db.insertOrThrow("profile_sources", null, ContentValues().apply {
                    put("profile_id", id); put("source_id", sourceId.value)
                })
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return requireNotNull(profile(id))
    }

    fun rename(profileId: String, name: String): Boolean {
        val clean = name.trim().take(60)
        if (clean.isBlank()) return false
        return database.writableDatabase.update("profiles", ContentValues().apply { put("name", clean) }, "id = ?", arrayOf(profileId)) == 1
    }

    fun replaceSources(profileId: String, sources: Set<SourceId>): Boolean {
        val existing = profile(profileId) ?: return false
        if (sources.isEmpty() || (existing.kind == ProfileKind.SOURCE_LOCKED && sources.size != 1)) return false
        val db = database.writableDatabase
        db.beginTransaction()
        return try {
            db.delete("profile_sources", "profile_id = ?", arrayOf(profileId))
            sources.forEach { source -> db.insertOrThrow("profile_sources", null, ContentValues().apply { put("profile_id", profileId); put("source_id", source.value) }) }
            db.setTransactionSuccessful()
            true
        } finally { db.endTransaction() }
    }

    fun state(profileId: String, key: SourceEntryKey): ProfileEntryState? = database.readableDatabase.rawQuery(
        """
        SELECT pe.profile_id, se.source_id, se.remote_id, pe.read_state, pe.rating, pe.pinned,
               pe.pin_priority, pe.read_at, pe.added_at, pe.fetched_at
        FROM profile_entries pe JOIN source_entries se ON se.id = pe.source_entry_id
        WHERE pe.profile_id = ? AND se.source_id = ? AND se.remote_id = ? LIMIT 1
        """.trimIndent(),
        arrayOf(profileId, key.sourceId.value, key.remoteId)
    ).use { cursor ->
        if (!cursor.moveToFirst()) null else ProfileEntryState(
            cursor.getString(0), SourceId(cursor.getString(1)), cursor.getString(2), cursor.getInt(3) != 0,
            cursor.getInt(4), cursor.getInt(5) != 0, cursor.getInt(6), cursor.getString(7), cursor.getString(8), cursor.getString(9)
        )
    }

    fun copyOrMove(keys: Collection<SourceEntryKey>, fromProfileId: String, toProfileId: String, move: Boolean): ProfileTransferResult {
        val targetProfile = profile(toProfileId)
        if (fromProfileId == toProfileId || profile(fromProfileId) == null || targetProfile == null) return ProfileTransferResult(0, 0, 0, keys.size)
        val db = database.writableDatabase
        var copied = 0; var merged = 0; var removed = 0; var failed = 0
        db.beginTransaction()
        try {
            keys.distinct().forEach { key ->
                if (key.sourceId !in targetProfile.sourceIds) { failed += 1; return@forEach }
                val entryId = sourceEntryId(db, key)
                if (entryId == null) { failed += 1; return@forEach }
                val source = readState(db, fromProfileId, entryId)
                if (source == null) { failed += 1; return@forEach }
                val target = readState(db, toProfileId, entryId)
                val mergedState = if (target == null) source else source.copy(
                    read = source.read || target.read,
                    rating = if (target.rating > 0) target.rating else source.rating,
                    pinned = source.pinned || target.pinned,
                    pinPriority = maxOf(source.pinPriority, target.pinPriority),
                    readAt = target.readAt.ifBlank { source.readAt },
                    addedAt = target.addedAt.ifBlank { source.addedAt },
                    fetchedAt = maxOf(source.fetchedAt, target.fetchedAt)
                )
                upsertState(db, toProfileId, entryId, mergedState)
                copyLocalTags(db, fromProfileId, toProfileId, entryId)
                copyReadingSessions(db, fromProfileId, toProfileId, key)
                copyReaderProgress(db, fromProfileId, toProfileId, key)
                if (target == null) copied += 1 else merged += 1
                if (move) {
                    db.delete("source_reader_progress", "profile_id=? AND source_id=? AND remote_id=?", arrayOf(fromProfileId, key.sourceId.value, key.remoteId))
                    db.delete("source_chapter_progress", "profile_id=? AND source_id=? AND remote_id=?", arrayOf(fromProfileId, key.sourceId.value, key.remoteId))
                    db.delete("reading_sessions", "profile_id=? AND source_id=? AND remote_id=?", arrayOf(fromProfileId, key.sourceId.value, key.remoteId))
                    db.delete("profile_entry_local_tags", "profile_id = ? AND source_entry_id = ?", arrayOf(fromProfileId, entryId.toString()))
                    db.delete("profile_entries", "profile_id = ? AND source_entry_id = ?", arrayOf(fromProfileId, entryId.toString()))
                    removed += 1
                }
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        return ProfileTransferResult(copied, merged, removed, failed)
    }

    fun deleteProfile(profileId: String): Boolean {
        val current = profile(profileId) ?: return false
        if (current.isMain || profiles().size <= 1 || activeProfileId() == profileId) return false
        val db = database.writableDatabase
        db.beginTransaction()
        return try {
            // These legacy tables gained profile columns, not profile foreign keys.
            db.delete("reading_sessions", "profile_id=?", arrayOf(profileId))
            db.delete("subscriptions", "profile_id=?", arrayOf(profileId))
            val deleted = db.delete("profiles", "id = ?", arrayOf(profileId)) == 1
            check(deleted) { "Profile deletion failed." }
            db.setTransactionSuccessful()
            true
        } finally { db.endTransaction() }
    }

    private data class RawState(val read: Boolean, val rating: Int, val pinned: Boolean, val pinPriority: Int, val readAt: String, val addedAt: String, val fetchedAt: String)
    private fun sourceEntryId(db: SQLiteDatabase, key: SourceEntryKey): Long? = db.rawQuery("SELECT id FROM source_entries WHERE source_id = ? AND remote_id = ?", arrayOf(key.sourceId.value, key.remoteId)).use { if (it.moveToFirst()) it.getLong(0) else null }
    private fun readState(db: SQLiteDatabase, profileId: String, entryId: Long): RawState? = db.rawQuery("SELECT read_state,rating,pinned,pin_priority,read_at,added_at,fetched_at FROM profile_entries WHERE profile_id=? AND source_entry_id=?", arrayOf(profileId, entryId.toString())).use {
        if (!it.moveToFirst()) null else RawState(it.getInt(0) != 0, it.getInt(1), it.getInt(2) != 0, it.getInt(3), it.getString(4), it.getString(5), it.getString(6))
    }
    private fun upsertState(db: SQLiteDatabase, profileId: String, entryId: Long, state: RawState) {
        db.insertWithOnConflict("profile_entries", null, ContentValues().apply {
            put("profile_id", profileId); put("source_entry_id", entryId); put("read_state", if (state.read) 1 else 0)
            put("rating", state.rating.coerceIn(0, 5)); put("pinned", if (state.pinned) 1 else 0); put("pin_priority", state.pinPriority)
            put("read_at", state.readAt); put("added_at", state.addedAt); put("fetched_at", state.fetchedAt)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }
    private fun copyLocalTags(db: SQLiteDatabase, from: String, to: String, entryId: Long) {
        db.execSQL("INSERT OR IGNORE INTO profile_entry_local_tags(profile_id,source_entry_id,name,normalized_name) SELECT ?,source_entry_id,name,normalized_name FROM profile_entry_local_tags WHERE profile_id=? AND source_entry_id=?", arrayOf(to, from, entryId))
    }
    private fun copyReadingSessions(db: SQLiteDatabase, from: String, to: String, key: SourceEntryKey) {
        db.execSQL(
            """
            INSERT OR IGNORE INTO reading_sessions(started_at,ended_at,day_key,entry_code,pages_viewed,seconds_elapsed,rating,is_reread,profile_id,source_id,remote_id,session_key,chapter_id)
            SELECT started_at,ended_at,day_key,entry_code,pages_viewed,seconds_elapsed,rating,is_reread,?,source_id,remote_id,session_key,chapter_id
            FROM reading_sessions WHERE profile_id=? AND source_id=? AND remote_id=?
            """.trimIndent(),
            arrayOf(to, from, key.sourceId.value, key.remoteId)
        )
    }
    private fun copyReaderProgress(db: SQLiteDatabase, from: String, to: String, key: SourceEntryKey) {
        val args = arrayOf(to, from, key.sourceId.value, key.remoteId)
        // Preserve an existing target resume position; do not unexpectedly move its reader.
        db.execSQL("""
            INSERT OR IGNORE INTO source_reader_progress(profile_id,source_id,remote_id,chapter_id,page_index,updated_at)
            SELECT ?,source_id,remote_id,chapter_id,page_index,updated_at FROM source_reader_progress
            WHERE profile_id=? AND source_id=? AND remote_id=?
        """.trimIndent(), args)
        db.execSQL("""
            INSERT OR REPLACE INTO source_chapter_progress(profile_id,source_id,remote_id,chapter_id,furthest_page_index,page_count,completed,completed_at,updated_at)
            SELECT ?,src.source_id,src.remote_id,src.chapter_id,
                MAX(src.furthest_page_index,COALESCE(dst.furthest_page_index,0)),
                MAX(src.page_count,COALESCE(dst.page_count,0)),MAX(src.completed,COALESCE(dst.completed,0)),
                CASE WHEN COALESCE(dst.completed_at,'')='' THEN src.completed_at
                    WHEN src.completed_at='' THEN dst.completed_at ELSE MIN(src.completed_at,dst.completed_at) END,
                MAX(src.updated_at,COALESCE(dst.updated_at,''))
            FROM source_chapter_progress src LEFT JOIN source_chapter_progress dst
                ON dst.profile_id=? AND dst.source_id=src.source_id AND dst.remote_id=src.remote_id AND dst.chapter_id=src.chapter_id
            WHERE src.profile_id=? AND src.source_id=? AND src.remote_id=?
        """.trimIndent(), arrayOf(to, to, from, key.sourceId.value, key.remoteId))
    }
    private fun now(): String = Instant.now().toString()

    companion object { const val MAIN_PROFILE_ID = "main" }
}
