package com.roinur.saucetracker.data.source

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.roinur.saucetracker.data.database.SauceTrackerDatabase
import org.json.JSONArray
import org.json.JSONObject

data class CachedSourceChapters(
    val chapters: List<SourceChapter>,
    val chapterCount: Int,
    val fetchedAtEpochMs: Long,
    val formatVersion: Int
)

class SourceChapterCacheStore(
    private val database: SauceTrackerDatabase
) {
    fun fresh(
        sourceId: String,
        remoteId: String,
        maxAgeMs: Long = DEFAULT_MAX_AGE_MS,
        nowEpochMs: Long = System.currentTimeMillis()
    ): CachedSourceChapters? = load(sourceId, remoteId)?.takeIf {
        it.formatVersion >= CACHE_FORMAT_VERSION &&
            nowEpochMs - it.fetchedAtEpochMs in 0..maxAgeMs
    }

    fun load(sourceId: String, remoteId: String): CachedSourceChapters? =
        database.readableDatabase.rawQuery(
            "SELECT chapter_count,payload_json,fetched_at_epoch_ms FROM source_chapter_cache WHERE source_id=? AND remote_id=?",
            arrayOf(sourceId.trim(), remoteId.trim())
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val parsed = parseChapters(cursor.getString(1)) ?: return@use null
            CachedSourceChapters(
                chapters = parsed.chapters,
                chapterCount = cursor.getInt(0).coerceAtLeast(parsed.chapters.size),
                fetchedAtEpochMs = cursor.getLong(2),
                formatVersion = parsed.formatVersion
            )
        }

    fun save(
        sourceId: String,
        remoteId: String,
        chapters: List<SourceChapter>,
        fetchedAtEpochMs: Long = System.currentTimeMillis()
    ) {
        val distinct = chapters.distinctBy(SourceChapter::id)
        database.writableDatabase.insertWithOnConflict(
            "source_chapter_cache",
            null,
            ContentValues().apply {
                put("source_id", sourceId.trim())
                put("remote_id", remoteId.trim())
                put("chapter_count", distinct.size)
                put("payload_json", serializeChapters(distinct))
                put("fetched_at_epoch_ms", fetchedAtEpochMs)
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
        database.writableDatabase.update(
            "source_entries",
            ContentValues().apply {
                put("unit_count", distinct.size)
                put("unit_label", "chapters")
            },
            "source_id=? AND remote_id=?",
            arrayOf(sourceId.trim(), remoteId.trim())
        )
    }

    private fun serializeChapters(chapters: List<SourceChapter>): String = JSONObject()
        .put("format_version", CACHE_FORMAT_VERSION)
        .put("chapters", JSONArray().apply {
            chapters.forEach { chapter ->
                put(JSONObject()
                    .put("id", chapter.id)
                    .put("label", chapter.label)
                    .put("number", chapter.number)
                    .put("volume", chapter.volume)
                    .put("title", chapter.title)
                    .put("language", chapter.language)
                    .put("groups", JSONArray(chapter.scanlationGroups))
                    .put("published_at", chapter.publishedAt)
                    .put("page_count", chapter.pageCount)
                    .put("preview_url", chapter.previewUrl)
                )
            }
        }).toString()

    private fun parseChapters(payload: String): ParsedChapterPayload? = runCatching {
        val trimmed = payload.trim()
        val formatVersion: Int
        val rows: JSONArray
        if (trimmed.startsWith("{")) {
            val root = JSONObject(trimmed)
            formatVersion = root.optInt("format_version", 1)
            rows = root.optJSONArray("chapters") ?: JSONArray()
        } else {
            // Version 1 was a bare array. It remains a valid stale fallback, but is refreshed
            // once so the new volume-cover mapping is written without a schema migration.
            formatVersion = 1
            rows = JSONArray(trimmed)
        }
        ParsedChapterPayload(formatVersion, buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val chapterId = row.optString("id", "").trim()
                if (chapterId.isBlank()) continue
                val groups = buildList {
                    val raw = row.optJSONArray("groups") ?: JSONArray()
                    for (groupIndex in 0 until raw.length()) {
                        raw.optString(groupIndex, "").trim().takeIf(String::isNotBlank)?.let(::add)
                    }
                }
                add(
                    SourceChapter(
                        id = chapterId,
                        label = row.optString("label", ""),
                        number = row.optString("number", ""),
                        volume = row.optString("volume", ""),
                        title = row.optString("title", ""),
                        language = row.optString("language", ""),
                        scanlationGroups = groups,
                        publishedAt = row.optString("published_at", ""),
                        pageCount = row.optInt("page_count", 0),
                        previewUrl = row.optString("preview_url", "")
                    )
                )
            }
        })
    }.getOrNull()

    companion object {
        private const val CACHE_FORMAT_VERSION = 3
        const val DEFAULT_MAX_AGE_MS = 6L * 60L * 60L * 1000L
    }

    private data class ParsedChapterPayload(
        val formatVersion: Int,
        val chapters: List<SourceChapter>
    )
}
