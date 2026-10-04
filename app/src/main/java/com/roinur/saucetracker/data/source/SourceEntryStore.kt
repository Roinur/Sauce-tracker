package com.roinur.saucetracker.data.source

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.roinur.saucetracker.CreatorEntryRow
import com.roinur.saucetracker.CreatorRow
import com.roinur.saucetracker.CreatorSortField
import com.roinur.saucetracker.SortDirection
import com.roinur.saucetracker.TagCountRow
import com.roinur.saucetracker.TagRouteRef
import com.roinur.saucetracker.TagSortField
import com.roinur.saucetracker.data.database.SauceTrackerDatabase
import com.roinur.saucetracker.data.profile.ProfileEntryState
import org.json.JSONArray
import java.time.Instant
import java.util.Locale

data class ProfileSourceEntry(
    val entry: SourceEntry,
    val state: ProfileEntryState
)

internal class SourceEntryStore(private val database: SauceTrackerDatabase) {
    fun upsert(entry: SourceEntry, profileId: String, applyIncomingState: ProfileEntryState? = null): Boolean {
        val db = database.writableDatabase
        var inserted = false
        db.beginTransaction()
        try {
            val previousId = findId(db, entry.key)
            val now = Instant.now().toString()
            val values = ContentValues().apply {
                put("source_id", entry.key.sourceId.value)
                put("remote_id", entry.key.remoteId)
                put("title", entry.title)
                put("alternate_titles", JSONArray(entry.alternateTitles).toString())
                put("canonical_url", entry.canonicalUrl)
                put("thumbnail_url", entry.thumbnailUrl)
                put("unit_count", entry.unitCount.coerceAtLeast(0))
                put("unit_label", entry.unitLabel)
                put("published_at", entry.publishedAt)
                put("status", entry.status)
                put("fetched_at", now)
                put("source_payload", entry.sourcePayload)
                if (entry.key.sourceId.value == "nhentai") put("legacy_code", entry.key.remoteId.toIntOrNull())
            }
            val entryId = if (previousId == null) {
                db.insertOrThrow("source_entries", null, values)
            } else {
                db.update("source_entries", values, "id=?", arrayOf(previousId.toString()))
                previousId
            }
            inserted = previousId == null
            if (entry.tags.isNotEmpty()) {
                db.delete("source_entry_tags", "source_entry_id = ?", arrayOf(entryId.toString()))
                entry.tags.distinctBy(SourceTag::semanticKey).forEach { tag ->
                    val semanticKey = tag.semanticKey()
                    val normalizedName = semanticKey.normalizedName
                    val normalizedType = semanticKey.type
                    db.insertOrThrow("source_entry_tags", null, ContentValues().apply {
                        put("source_entry_id", entryId); put("name", tag.name.trim()); put("type", normalizedType)
                        put("normalized_name", normalizedName); put("remote_id", tag.remoteId)
                    })
                    db.insertWithOnConflict("source_terms", null, ContentValues().apply {
                        put("kind", "tag"); put("type", normalizedType); put("normalized_name", normalizedName); put("display_name", tag.name.trim())
                    }, SQLiteDatabase.CONFLICT_IGNORE)
                }
            }
            if (entry.creators.isNotEmpty()) {
                db.delete("source_entry_creators", "source_entry_id = ?", arrayOf(entryId.toString()))
                entry.creators.distinctBy { normalize(it.type) to normalize(it.name) }.forEach { creator ->
                    val normalizedName = normalize(creator.name)
                    db.insertOrThrow("source_entry_creators", null, ContentValues().apply {
                        put("source_entry_id", entryId); put("name", creator.name.trim()); put("type", normalize(creator.type))
                        put("normalized_name", normalizedName); put("remote_id", creator.remoteId); put("source_url", creator.url)
                    })
                    db.insertWithOnConflict("source_terms", null, ContentValues().apply {
                        put("kind", "creator"); put("type", "creator"); put("normalized_name", normalizedName); put("display_name", creator.name.trim())
                    }, SQLiteDatabase.CONFLICT_IGNORE)
                }
            }
            val existing = stateById(db, profileId, entryId)
            val state = when {
                existing != null && applyIncomingState == null -> existing
                existing != null && applyIncomingState != null -> applyIncomingState.copy(
                    profileId = profileId,
                    isRead = existing.isRead || applyIncomingState.isRead,
                    rating = if (existing.rating > 0) existing.rating else applyIncomingState.rating,
                    pinned = existing.pinned || applyIncomingState.pinned,
                    addedAt = existing.addedAt.ifBlank { applyIncomingState.addedAt }
                )
                applyIncomingState != null -> applyIncomingState.copy(profileId = profileId)
                else -> ProfileEntryState(profileId, entry.key.sourceId, entry.key.remoteId, false, 0, false, 0, "", now, now)
            }
            db.insertWithOnConflict("profile_entries", null, ContentValues().apply {
                put("profile_id", profileId); put("source_entry_id", entryId); put("read_state", if (state.isRead) 1 else 0)
                put("rating", state.rating.coerceIn(0, 5)); put("pinned", if (state.pinned) 1 else 0); put("pin_priority", state.pinPriority)
                put("read_at", state.readAt); put("added_at", state.addedAt.ifBlank { now }); put("fetched_at", now)
            }, SQLiteDatabase.CONFLICT_REPLACE)
            if (entry.key.sourceId.value == "nhentai") mirrorNhentaiMetadata(db, entry, now)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        return inserted
    }

    fun entries(profileId: String, sourceScope: Set<SourceId> = emptySet(), text: String = "", limit: Int = 500, offset: Int = 0, stateFilter: String = "all", sort: String = "default"): List<ProfileSourceEntry> {
        val where = mutableListOf("pe.profile_id = ?")
        val args = mutableListOf(profileId)
        when (stateFilter) {
            "read" -> where += "pe.read_state<>0"
            "unread" -> where += "COALESCE(pe.read_state,0)=0"
            "pinned" -> where += "pe.pinned<>0"
        }
        val order = when (sort) {
            "title" -> "LOWER(se.title), se.id"
            "rating" -> "pe.rating DESC, LOWER(se.title), se.id"
            "added" -> "pe.added_at DESC, se.id DESC"
            else -> "pe.pinned DESC, LOWER(se.title), se.remote_id"
        }
        if (sourceScope.isNotEmpty()) {
            where += "se.source_id IN (${sourceScope.joinToString(",") { "?" }})"
            args += sourceScope.map { it.value }
        }
        val search = sourceLibraryFilter(text, database.tagDao::route)
        where += search.clauses
        args += search.arguments
        args += listOf(limit.coerceIn(1, 1000).toString(), offset.coerceAtLeast(0).toString())
        return database.readableDatabase.rawQuery(
            """
            SELECT se.id,se.source_id,se.remote_id,se.title,se.alternate_titles,se.canonical_url,se.thumbnail_url,
                   se.unit_count,se.unit_label,se.published_at,se.status,se.source_payload,
                   pe.read_state,pe.rating,pe.pinned,pe.pin_priority,pe.read_at,pe.added_at,pe.fetched_at
            FROM profile_entries pe JOIN source_entries se ON se.id=pe.source_entry_id
            WHERE ${where.joinToString(" AND ")}
            ORDER BY $order,se.id LIMIT ? OFFSET ?
            """.trimIndent(), args.toTypedArray()
        ).use { cursor ->
            val ids = buildList { while (cursor.moveToNext()) add(cursor.getLong(0)) }
            cursor.moveToPosition(-1)
            val tagRows = linkedMapOf<Long, MutableList<SourceTag>>()
            val creatorRows = linkedMapOf<Long, MutableList<SourceCreator>>()
            ids.chunked(500).forEach { batch ->
                val placeholders = batch.joinToString(",") { "?" }
                val parameters = batch.map(Long::toString).toTypedArray()
                database.readableDatabase.rawQuery("SELECT source_entry_id,name,type,remote_id FROM source_entry_tags WHERE source_entry_id IN ($placeholders) ORDER BY type,LOWER(name)", parameters).use { c ->
                    while (c.moveToNext()) tagRows.getOrPut(c.getLong(0)) { mutableListOf() }.add(SourceTag(c.getString(1), c.getString(2), c.getString(3)))
                }
                database.readableDatabase.rawQuery("SELECT source_entry_id,name,type,remote_id,source_url FROM source_entry_creators WHERE source_entry_id IN ($placeholders) ORDER BY type,LOWER(name)", parameters).use { c ->
                    while (c.moveToNext()) creatorRows.getOrPut(c.getLong(0)) { mutableListOf() }.add(SourceCreator(c.getString(1), c.getString(2), c.getString(3), c.getString(4)))
                }
            }
            buildList {
                while (cursor.moveToNext()) {
                    val entryId = cursor.getLong(0)
                    val sourceId = SourceId(cursor.getString(1)); val remoteId = cursor.getString(2)
                    val tags = tagRows[entryId].orEmpty(); val creators = creatorRows[entryId].orEmpty()
                    val alt = runCatching { JSONArray(cursor.getString(4)) }.getOrNull()?.let { array -> buildList { for (i in 0 until array.length()) array.optString(i).takeIf(String::isNotBlank)?.let(::add) } }.orEmpty()
                    val entry = SourceEntry(SourceEntryKey(sourceId, remoteId), cursor.getString(3), alt, cursor.getString(5), cursor.getString(6), cursor.getInt(7), cursor.getString(8), cursor.getString(9), cursor.getString(10), tags, creators, cursor.getString(11))
                    val state = ProfileEntryState(profileId, sourceId, remoteId, cursor.getInt(12) != 0, cursor.getInt(13), cursor.getInt(14) != 0, cursor.getInt(15), cursor.getString(16), cursor.getString(17), cursor.getString(18))
                    add(ProfileSourceEntry(entry, state))
                }
            }
        }
    }

    /** Local pagination is a memory bound, never a cap on the user's library or an API request. */
    fun allEntries(profileId: String, sourceScope: Set<SourceId> = emptySet(), text: String = "", maxEntries: Int = Int.MAX_VALUE): List<ProfileSourceEntry> =
        readAllLibraryPages(maxEntries) { offset, limit -> entries(profileId, sourceScope, text, limit = limit, offset = offset) }

    fun tagCounts(
        rows: List<ProfileSourceEntry>,
        sortField: TagSortField,
        sortDirection: SortDirection,
        includeSourceTags: Boolean
    ): List<TagCountRow> {
        data class Seed(val name: String, val type: String, val entryKeys: MutableSet<String>)
        val seeds = linkedMapOf<Pair<String, String>, Seed>()
        rows.forEach { row ->
            if (includeSourceTags) {
                val sourceName = sourceDisplayName(row.entry.key.sourceId)
                val normalizedSource = normalize(row.entry.key.sourceId.value)
                if (normalizedSource.isNotBlank()) {
                    seeds.getOrPut("source" to normalizedSource) {
                        Seed(sourceName, "source", linkedSetOf())
                    }.entryKeys += row.entry.key.storageKey
                }
            }
            for (tag in row.entry.tags.distinctBy(SourceTag::semanticKey)) {
                val semanticKey = tag.semanticKey()
                val type = semanticKey.type
                val name = tag.name.trim()
                val normalizedName = semanticKey.normalizedName
                if (normalizedName.isBlank()) continue
                seeds.getOrPut(type to normalizedName) { Seed(name, type, linkedSetOf()) }
                    .entryKeys += row.entry.key.storageKey
            }
        }
        if (includeSourceTags) ensureSourceFilterTerms(rows)
        val terms = sourceTerms("tag")
        val result = seeds.mapNotNull { (key, seed) ->
            val storageId = terms[key] ?: return@mapNotNull null
            TagCountRow(sourceTermUiId(storageId), seed.name, seed.type, seed.entryKeys.size)
        }
        val comparator = when (sortField) {
            TagSortField.NAME -> compareBy<TagCountRow> { it.name.lowercase(Locale.US) }.thenBy { it.type }
            TagSortField.TYPE -> compareBy<TagCountRow> { it.type }.thenBy { it.name.lowercase(Locale.US) }
            TagSortField.COUNT -> compareBy<TagCountRow> { it.count }.thenBy { it.type }.thenBy { it.name.lowercase(Locale.US) }
        }
        return result.sortedWith(if (sortDirection == SortDirection.ASC) comparator else comparator.reversed())
    }

    private fun ensureSourceFilterTerms(rows: List<ProfileSourceEntry>) {
        val sourceIds = rows.asSequence().map { it.entry.key.sourceId }.distinctBy { it.value }.toList()
        if (sourceIds.isEmpty()) return
        val db = database.writableDatabase
        sourceIds.forEach { sourceId ->
            val normalized = normalize(sourceId.value)
            if (normalized.isBlank()) return@forEach
            db.insertWithOnConflict("source_terms", null, ContentValues().apply {
                put("kind", "tag")
                put("type", "source")
                put("normalized_name", normalized)
                put("display_name", sourceDisplayName(sourceId))
            }, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    private fun sourceDisplayName(sourceId: SourceId): String = when (sourceId.value.lowercase(Locale.US)) {
        "nhentai" -> "NHentai"
        "mangadex" -> "MangaDex"
        else -> sourceId.value.replaceFirstChar { it.uppercaseChar() }
    }

    fun creators(
        rows: List<ProfileSourceEntry>,
        sortField: CreatorSortField,
        sortDirection: SortDirection
    ): List<CreatorRow> {
        data class Seed(val name: String, val types: MutableSet<String>, val entryKeys: MutableSet<String>)
        val seeds = linkedMapOf<String, Seed>()
        rows.forEach { row ->
            for (creator in row.entry.creators.distinctBy { normalize(it.name) }) {
                val normalizedName = normalize(creator.name)
                if (normalizedName.isBlank()) continue
                seeds.getOrPut(normalizedName) { Seed(creator.name.trim(), linkedSetOf(), linkedSetOf()) }.apply {
                    types += normalize(creator.type)
                    entryKeys += row.entry.key.storageKey
                }
            }
        }
        val terms = sourceTerms("creator")
        val result = seeds.mapNotNull { (normalizedName, seed) ->
            val storageId = terms["creator" to normalizedName] ?: return@mapNotNull null
            val displayType = when {
                "artist" in seed.types -> "artist"
                "group" in seed.types -> "group"
                "author" in seed.types -> "author"
                else -> seed.types.firstOrNull().orEmpty().ifBlank { "creator" }
            }
            CreatorRow(sourceTermUiId(storageId), seed.name, displayType, seed.entryKeys.size)
        }
        val comparator = when (sortField) {
            CreatorSortField.NAME -> compareBy<CreatorRow> { it.name.lowercase(Locale.US) }.thenBy { it.type }
            CreatorSortField.TYPE -> compareBy<CreatorRow> { it.type }.thenBy { it.name.lowercase(Locale.US) }
            CreatorSortField.COUNT -> compareBy<CreatorRow> { it.entryCount }.thenBy { it.type }.thenBy { it.name.lowercase(Locale.US) }
        }
        return result.sortedWith(if (sortDirection == SortDirection.ASC) comparator else comparator.reversed())
    }

    fun creatorEntries(
        rows: List<ProfileSourceEntry>,
        creatorUiId: Long,
        uiCodesByKey: Map<SourceEntryKey, Int>
    ): List<CreatorEntryRow> {
        val ref = termRef(creatorUiId, expectedKind = "creator") ?: return emptyList()
        val normalizedName = normalize(ref.name)
        return rows.filter { row -> row.entry.creators.any { normalize(it.name) == normalizedName } }
            .mapNotNull { row -> uiCodesByKey[row.entry.key]?.let { CreatorEntryRow(it, row.entry.title) } }
            .distinctBy(CreatorEntryRow::code)
    }

    fun termRef(uiId: Long, expectedKind: String? = null): TagRouteRef? {
        val storageId = sourceTermStorageId(uiId) ?: return null
        return database.readableDatabase.rawQuery(
            "SELECT kind,type,display_name FROM source_terms WHERE id=? LIMIT 1",
            arrayOf(storageId.toString())
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val kind = cursor.getString(0).orEmpty()
            if (expectedKind != null && kind != expectedKind) return@use null
            TagRouteRef(
                name = cursor.getString(2).orEmpty(),
                type = if (kind == "creator") "creator" else cursor.getString(1).orEmpty()
            )
        }
    }

    private fun sourceTerms(kind: String): Map<Pair<String, String>, Long> = database.readableDatabase.rawQuery(
        "SELECT id,type,normalized_name FROM source_terms WHERE kind=?",
        arrayOf(kind)
    ).use { cursor ->
        buildMap {
            while (cursor.moveToNext()) put(cursor.getString(1) to cursor.getString(2), cursor.getLong(0))
        }
    }

    fun legacyCodes(profileId: String): Set<Int> = database.readableDatabase.rawQuery(
        "SELECT se.remote_id FROM profile_entries pe JOIN source_entries se ON se.id=pe.source_entry_id WHERE pe.profile_id=? AND se.source_id='nhentai'",
        arrayOf(profileId)
    ).use { cursor -> buildSet { while (cursor.moveToNext()) cursor.getString(0).toIntOrNull()?.let(::add) } }

    fun statesForSource(profileId: String, sourceId: SourceId): Map<String, ProfileEntryState> = database.readableDatabase.rawQuery(
        "SELECT se.remote_id,pe.read_state,pe.rating,pe.pinned,pe.pin_priority,pe.read_at,pe.added_at,pe.fetched_at FROM profile_entries pe JOIN source_entries se ON se.id=pe.source_entry_id WHERE pe.profile_id=? AND se.source_id=?",
        arrayOf(profileId, sourceId.value)
    ).use { cursor ->
        buildMap {
            while (cursor.moveToNext()) {
                val remoteId = cursor.getString(0)
                put(remoteId, ProfileEntryState(profileId, sourceId, remoteId, cursor.getInt(1) != 0, cursor.getInt(2), cursor.getInt(3) != 0, cursor.getInt(4), cursor.getString(5), cursor.getString(6), cursor.getString(7)))
            }
        }
    }

    fun entry(key: SourceEntryKey): SourceEntry? {
        val id = findId(database.readableDatabase, key) ?: return null
        return database.readableDatabase.rawQuery(
            "SELECT title,alternate_titles,canonical_url,thumbnail_url,unit_count,unit_label,published_at,status,source_payload FROM source_entries WHERE id=?",
            arrayOf(id.toString())
        ).use { cursor ->
            if (!cursor.moveToFirst()) null else {
                val alternateTitles = runCatching { JSONArray(cursor.getString(1)) }.getOrNull()?.let { array ->
                    buildList { for (index in 0 until array.length()) array.optString(index).takeIf(String::isNotBlank)?.let(::add) }
                }.orEmpty()
                SourceEntry(key, cursor.getString(0), alternateTitles, cursor.getString(2), cursor.getString(3), cursor.getInt(4), cursor.getString(5), cursor.getString(6), cursor.getString(7), tags(id), creators(id), cursor.getString(8))
            }
        }
    }

    fun localTags(profileId: String, key: SourceEntryKey): List<String> {
        val id = findId(database.readableDatabase, key) ?: return emptyList()
        return database.readableDatabase.rawQuery(
            "SELECT name FROM profile_entry_local_tags WHERE profile_id=? AND source_entry_id=? ORDER BY LOWER(name)",
            arrayOf(profileId, id.toString())
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }
    }

    fun mergeLocalTags(profileId: String, key: SourceEntryKey, names: Collection<String>) {
        val id = findId(database.readableDatabase, key) ?: return
        names.map(String::trim).filter(String::isNotBlank).distinctBy(::normalize).take(12).forEach { name ->
            database.writableDatabase.insertWithOnConflict("profile_entry_local_tags", null, ContentValues().apply {
                put("profile_id", profileId); put("source_entry_id", id); put("name", name.take(60)); put("normalized_name", normalize(name).take(60))
            }, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    fun updateState(profileId: String, key: SourceEntryKey, read: Boolean? = null, rating: Int? = null, pinned: Boolean? = null): Boolean {
        val entryId = findId(database.readableDatabase, key) ?: return false
        val values = ContentValues().apply {
            read?.let { value ->
                put("read_state", if (value) 1 else 0)
                put("read_at", if (value) Instant.now().toString() else "")
            }
            rating?.let { put("rating", it.coerceIn(0, 5)) }
            pinned?.let { put("pinned", if (it) 1 else 0) }
        }
        return values.size() > 0 && database.writableDatabase.update("profile_entries", values, "profile_id=? AND source_entry_id=?", arrayOf(profileId, entryId.toString())) == 1
    }

    fun removeMembership(profileId: String, key: SourceEntryKey): Boolean {
        val entryId = findId(database.readableDatabase, key) ?: return false
        return database.writableDatabase.delete(
            "profile_entries",
            "profile_id=? AND source_entry_id=?",
            arrayOf(profileId, entryId.toString())
        ) == 1
    }

    fun membershipCount(key: SourceEntryKey): Int {
        val entryId = findId(database.readableDatabase, key) ?: return 0
        return database.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM profile_entries WHERE source_entry_id=?",
            arrayOf(entryId.toString())
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0).coerceAtLeast(0) else 0 }
    }

    fun deleteSharedMetadataIfUnused(key: SourceEntryKey): Boolean {
        val db = database.writableDatabase
        val entryId = findId(db, key) ?: return false
        val memberships = db.rawQuery(
            "SELECT COUNT(*) FROM profile_entries WHERE source_entry_id=?",
            arrayOf(entryId.toString())
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }
        if (memberships != 0) return false
        if (key.sourceId.value == "nhentai") {
            key.remoteId.toIntOrNull()?.takeIf { it > 0 }?.let { code ->
                db.delete("entries", "code=?", arrayOf(code.toString()))
            }
        }
        return db.delete("source_entries", "id=?", arrayOf(entryId.toString())) == 1
    }

    private fun findId(db: SQLiteDatabase, key: SourceEntryKey): Long? = db.rawQuery("SELECT id FROM source_entries WHERE source_id=? AND remote_id=?", arrayOf(key.sourceId.value, key.remoteId)).use { if (it.moveToFirst()) it.getLong(0) else null }

    private fun mirrorNhentaiMetadata(db: SQLiteDatabase, entry: SourceEntry, now: String) {
        val code = entry.key.remoteId.toIntOrNull()?.takeIf { it > 0 } ?: return
        val exists = db.rawQuery("SELECT 1 FROM entries WHERE code=?", arrayOf(code.toString())).use { it.moveToFirst() }
        val mediaMatch = Regex("/galleries/(\\d+)/cover\\.([a-zA-Z0-9]+)").find(entry.thumbnailUrl)
        val metadata = ContentValues().apply {
            put("title", entry.title); put("subtitle", entry.alternateTitles.firstOrNull().orEmpty()); put("source_url", entry.canonicalUrl)
            put("num_pages", entry.unitCount.coerceAtLeast(0)); put("upload_date", entry.publishedAt); put("fetched_at", now)
            mediaMatch?.groupValues?.getOrNull(1)?.toLongOrNull()?.let { put("media_id", it) }
            mediaMatch?.groupValues?.getOrNull(2)?.lowercase(Locale.US)?.let { put("cover_ext", if (it == "jpeg") "jpg" else it) }
        }
        if (exists) db.update("entries", metadata, "code=?", arrayOf(code.toString()))
        else db.insertOrThrow("entries", null, ContentValues(metadata).apply {
            put("code", code); put("rating", 0); put("read_state", 0); put("read_at", ""); put("pinned", 0); put("added_at", now)
            if (!containsKey("media_id")) put("media_id", 0); if (!containsKey("cover_ext")) put("cover_ext", "jpg")
        })
        if (entry.tags.isEmpty()) return
        db.delete("entry_tags", "entry_code=?", arrayOf(code.toString()))
        for (tag in entry.tags.distinctBy(SourceTag::semanticKey)) {
            val semanticKey = tag.semanticKey()
            val type = semanticKey.type; val name = tag.name.trim(); val normalized = semanticKey.normalizedName
            if (normalized.isBlank()) continue
            db.insertWithOnConflict("tags", null, ContentValues().apply { put("name", name); put("type", type); put("normalized_name", normalized) }, SQLiteDatabase.CONFLICT_IGNORE)
            val tagId = db.rawQuery("SELECT id FROM tags WHERE normalized_name=? AND type=? LIMIT 1", arrayOf(normalized, type)).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null } ?: continue
            db.insertWithOnConflict("entry_tags", null, ContentValues().apply { put("entry_code", code); put("tag_id", tagId) }, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }
    private fun stateById(db: SQLiteDatabase, profileId: String, id: Long): ProfileEntryState? = db.rawQuery("SELECT se.source_id,se.remote_id,pe.read_state,pe.rating,pe.pinned,pe.pin_priority,pe.read_at,pe.added_at,pe.fetched_at FROM profile_entries pe JOIN source_entries se ON se.id=pe.source_entry_id WHERE pe.profile_id=? AND se.id=?", arrayOf(profileId,id.toString())).use {
        if (!it.moveToFirst()) null else ProfileEntryState(profileId,SourceId(it.getString(0)),it.getString(1),it.getInt(2)!=0,it.getInt(3),it.getInt(4)!=0,it.getInt(5),it.getString(6),it.getString(7),it.getString(8))
    }
    private fun tags(entryId: Long): List<SourceTag> = database.readableDatabase.rawQuery("SELECT name,type,remote_id FROM source_entry_tags WHERE source_entry_id=? ORDER BY type,LOWER(name)", arrayOf(entryId.toString())).use { c -> buildList { while(c.moveToNext()) add(SourceTag(c.getString(0),c.getString(1),c.getString(2))) } }
    private fun creators(entryId: Long): List<SourceCreator> = database.readableDatabase.rawQuery("SELECT name,type,remote_id,source_url FROM source_entry_creators WHERE source_entry_id=? ORDER BY type,LOWER(name)", arrayOf(entryId.toString())).use { c -> buildList { while(c.moveToNext()) add(SourceCreator(c.getString(0),c.getString(1),c.getString(2),c.getString(3))) } }
    private fun normalize(value: String): String = value.trim().lowercase(Locale.US).replace(Regex("\\s+"), " ")
}
