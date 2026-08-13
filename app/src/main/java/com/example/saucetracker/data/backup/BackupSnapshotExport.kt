package com.example.saucetracker.data.backup

import android.content.SharedPreferences
import com.example.saucetracker.HiddenSuggestedEntryState
import com.example.saucetracker.SavedStats
import com.example.saucetracker.SuggestionWeightCategory
import com.example.saucetracker.UTC_TIMESTAMP_FORMAT
import com.example.saucetracker.csvEscape
import com.example.saucetracker.core.preferences.KEY_SUGGESTION_HIDDEN_CODES
import com.example.saucetracker.core.preferences.KEY_SUGGESTION_HIDDEN_ENTRIES
import com.example.saucetracker.core.preferences.KEY_SUGGESTION_WEIGHT_PREFIX
import com.example.saucetracker.data.database.SauceTrackerDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

internal object BackupSnapshotExport {
    private fun copyJsonObject(source: JSONObject): JSONObject {
        return JSONObject(source.toString())
    }

    private fun mergeJsonArrayKeepingPrimaryOrder(
        primary: JSONArray?,
        fallback: JSONArray?,
        keySelector: (JSONObject) -> String?
    ): JSONArray {
        val merged = LinkedHashMap<String, JSONObject>()

        fun appendRows(array: JSONArray?, replaceExisting: Boolean) {
            if (array == null) return
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val key = keySelector(obj) ?: continue
                if (!replaceExisting && merged.containsKey(key)) continue
                merged[key] = copyJsonObject(obj)
            }
        }

        appendRows(primary, replaceExisting = true)
        appendRows(fallback, replaceExisting = false)

        return JSONArray().apply {
            merged.values.forEach { put(it) }
        }
    }

    private fun mergeJsonObjectKeepingPrimaryValues(
        primary: JSONObject?,
        fallback: JSONObject?
    ): JSONObject? {
        if (primary == null && fallback == null) return null
        val merged = JSONObject()
        fallback?.keys()?.forEach { key -> merged.put(key, fallback.opt(key)) }
        primary?.keys()?.forEach { key -> merged.put(key, primary.opt(key)) }
        return merged
    }

    fun mergeProceduralSnapshots(
        latestSnapshot: JSONObject,
        existingSnapshot: JSONObject?
    ): JSONObject {
        if (existingSnapshot == null) {
            return JSONObject(latestSnapshot.toString())
        }

        fun normalizeTextKey(vararg parts: String): String? {
            val normalized = parts
                .map { it.trim().lowercase(Locale.US) }
                .filter { it.isNotBlank() }
            return if (normalized.isEmpty()) null else normalized.joinToString("|")
        }

        val mergedHiddenEntries = mergeJsonArrayKeepingPrimaryOrder(
            primary = latestSnapshot.optJSONArray("hidden_suggested_entries"),
            fallback = existingSnapshot.optJSONArray("hidden_suggested_entries")
        ) { obj ->
            obj.optInt("code", 0).takeIf { it > 0 }?.toString()
        }
        val mergedHiddenCodes = JSONArray().apply {
            for (index in 0 until mergedHiddenEntries.length()) {
                val code = mergedHiddenEntries.optJSONObject(index)?.optInt("code", 0) ?: 0
                if (code > 0) put(code)
            }
        }

        return JSONObject(latestSnapshot.toString()).apply {
            put(
                "version",
                latestSnapshot.optInt("version", 5)
                    .coerceAtLeast(existingSnapshot.optInt("version", 5))
                    .coerceAtLeast(9)
            )
            put(
                "entries",
                mergeJsonArrayKeepingPrimaryOrder(
                    primary = latestSnapshot.optJSONArray("entries"),
                    fallback = existingSnapshot.optJSONArray("entries")
                ) { obj ->
                    obj.optInt("code", 0).takeIf { it > 0 }?.toString()
                }
            )
            put(
                "creators",
                mergeJsonArrayKeepingPrimaryOrder(
                    primary = latestSnapshot.optJSONArray("creators"),
                    fallback = existingSnapshot.optJSONArray("creators")
                ) { obj ->
                    normalizeTextKey(
                        obj.optString("type", ""),
                        obj.optString("name", "")
                    )
                }
            )
            put(
                "subscriptions",
                mergeJsonArrayKeepingPrimaryOrder(
                    primary = latestSnapshot.optJSONArray("subscriptions"),
                    fallback = existingSnapshot.optJSONArray("subscriptions")
                ) { obj ->
                    normalizeTextKey(
                        obj.optString("route_type", ""),
                        obj.optString("route_name", "")
                    )
                }
            )
            put(
                "subscription_seen_codes",
                mergeJsonArrayKeepingPrimaryOrder(
                    primary = latestSnapshot.optJSONArray("subscription_seen_codes"),
                    fallback = existingSnapshot.optJSONArray("subscription_seen_codes")
                ) { obj ->
                    val routeKey = normalizeTextKey(
                        obj.optString("route_type", ""),
                        obj.optString("route_name", "")
                    ) ?: return@mergeJsonArrayKeepingPrimaryOrder null
                    val code = obj.optInt("code", 0).takeIf { it > 0 } ?: return@mergeJsonArrayKeepingPrimaryOrder null
                    "$routeKey|$code"
                }
            )
            put(
                "subscription_events",
                mergeJsonArrayKeepingPrimaryOrder(
                    primary = latestSnapshot.optJSONArray("subscription_events"),
                    fallback = existingSnapshot.optJSONArray("subscription_events")
                ) { obj ->
                    val routeKey = normalizeTextKey(
                        obj.optString("route_type", ""),
                        obj.optString("route_name", "")
                    ) ?: return@mergeJsonArrayKeepingPrimaryOrder null
                    val code = obj.optInt("code", 0).takeIf { it > 0 } ?: return@mergeJsonArrayKeepingPrimaryOrder null
                    "$routeKey|$code"
                }
            )
            put(
                "entry_heatmap_cache",
                mergeJsonArrayKeepingPrimaryOrder(
                    primary = latestSnapshot.optJSONArray("entry_heatmap_cache"),
                    fallback = existingSnapshot.optJSONArray("entry_heatmap_cache")
                ) { obj ->
                    normalizeTextKey(obj.optString("cache_key", ""))
                }
            )
            put(
                "popular_tags",
                mergeJsonArrayKeepingPrimaryOrder(
                    primary = latestSnapshot.optJSONArray("popular_tags"),
                    fallback = existingSnapshot.optJSONArray("popular_tags")
                ) { obj ->
                    normalizeTextKey(
                        obj.optString("type", ""),
                        obj.optString("normalized_name", "").ifBlank { obj.optString("name", "") }
                    )
                }
            )
            put(
                "daily_read_activity",
                mergeJsonArrayKeepingPrimaryOrder(
                    primary = latestSnapshot.optJSONArray("daily_read_activity"),
                    fallback = existingSnapshot.optJSONArray("daily_read_activity")
                ) { obj ->
                    normalizeTextKey(
                        obj.optString("activity_date", "").ifBlank { obj.optString("date", "") }
                    )
                }
            )
            put(
                "reading_sessions",
                mergeJsonArrayKeepingPrimaryOrder(
                    primary = latestSnapshot.optJSONArray("reading_sessions"),
                    fallback = existingSnapshot.optJSONArray("reading_sessions")
                ) { obj ->
                    val entryCode = obj.optInt("entry_code", 0).takeIf { it > 0 } ?: return@mergeJsonArrayKeepingPrimaryOrder null
                    normalizeTextKey(
                        obj.optString("started_at", ""),
                        obj.optString("ended_at", ""),
                        obj.optString("day_key", ""),
                        entryCode.toString(),
                        obj.opt("pages_viewed")?.toString().orEmpty(),
                        obj.opt("seconds_elapsed")?.toString().orEmpty()
                    )
                }
            )
            put("hidden_suggested_entries", mergedHiddenEntries)
            put("hidden_suggested_codes", mergedHiddenCodes)
            mergeJsonObjectKeepingPrimaryValues(
                primary = latestSnapshot.optJSONObject("suggestion_category_weights"),
                fallback = existingSnapshot.optJSONObject("suggestion_category_weights")
            )?.let { put("suggestion_category_weights", it) }

            when {
                latestSnapshot.has("entry_pin_priority_enabled") ->
                    put("entry_pin_priority_enabled", latestSnapshot.optBoolean("entry_pin_priority_enabled"))
                existingSnapshot.has("entry_pin_priority_enabled") ->
                    put("entry_pin_priority_enabled", existingSnapshot.optBoolean("entry_pin_priority_enabled"))
            }
        }
    }

    fun parseHiddenSuggestionCodeList(raw: String): List<Int> {
        if (raw.isBlank()) return emptyList()
        return raw
            .split(',')
            .asSequence()
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it > 0 }
            .toList()
    }

    fun parseHiddenSuggestionEntries(
        raw: String,
        backupImporter: BackupImporter
    ): List<HiddenSuggestedEntryState> {
        if (raw.isBlank()) return emptyList()
        val payload = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return backupImporter.parseHiddenSuggestionEntries(payload).orEmpty()
    }

    fun buildSnapshotWithSettings(
        db: SauceTrackerDatabase,
        prefs: SharedPreferences,
        backupImporter: BackupImporter,
        entryPinPriorityEnabled: Boolean
    ): JSONObject {
        val hiddenEntries = loadHiddenSuggestionEntriesFromPrefs(prefs, backupImporter)
        val snapshot = db.exportSnapshot()
        snapshot.put("daily_read_activity", db.exportDailyReadActivitySnapshot())
        snapshot.put("reading_sessions", db.exportReadingSessionsSnapshot())
        snapshot.put("popular_tags", db.exportPopularTagsSnapshot())

        val hiddenCodesJson = JSONArray()
        hiddenEntries
            .asSequence()
            .map { it.code }
            .filter { it > 0 }
            .forEach { code -> hiddenCodesJson.put(code) }
        val hiddenEntriesJson = JSONArray()
        hiddenEntries.forEach { entry ->
            hiddenEntriesJson.put(
                JSONObject()
                    .put("code", entry.code)
                    .put("hidden_at_ms", entry.hiddenAtMillis)
                    .put(
                        "hidden_at",
                        runCatching {
                            Instant.ofEpochMilli(entry.hiddenAtMillis)
                                .atOffset(ZoneOffset.UTC)
                                .toLocalDateTime()
                                .format(UTC_TIMESTAMP_FORMAT)
                        }.getOrDefault("")
                    )
            )
        }

        snapshot.put("hidden_suggested_codes", hiddenCodesJson)
        snapshot.put("hidden_suggested_entries", hiddenEntriesJson)
        snapshot.put("suggestion_category_weights", suggestionCategoryWeightsForExport(prefs))
        snapshot.put("entry_pin_priority_enabled", entryPinPriorityEnabled)
        snapshot.put("version", snapshot.optInt("version", 5).coerceAtLeast(9))
        return snapshot
    }

    private fun suggestionCategoryWeightsForExport(prefs: SharedPreferences): JSONObject {
        return JSONObject().apply {
            SuggestionWeightCategory.entries.forEach { category ->
                val key = "$KEY_SUGGESTION_WEIGHT_PREFIX${category.storageKey}"
                val value = prefs.getFloat(key, 1f).coerceIn(0f, 2f)
                put(category.storageKey, value.toDouble())
            }
        }
    }

    private fun loadHiddenSuggestionEntriesFromPrefs(
        prefs: SharedPreferences,
        backupImporter: BackupImporter
    ): List<HiddenSuggestedEntryState> {
        val rawCodes = prefs.getString(KEY_SUGGESTION_HIDDEN_CODES, "").orEmpty()
        val rawEntries = prefs.getString(KEY_SUGGESTION_HIDDEN_ENTRIES, "").orEmpty()
        val codes = parseHiddenSuggestionCodeList(rawCodes)
        val entriesByCode = parseHiddenSuggestionEntries(rawEntries, backupImporter)
            .associateBy { it.code }
        val fallbackBase = 1L
        return codes
            .asSequence()
            .filter { it > 0 }
            .distinct()
            .mapIndexed { index, code ->
                HiddenSuggestedEntryState(
                    code = code,
                    hiddenAtMillis = entriesByCode[code]?.hiddenAtMillis ?: (fallbackBase + index)
                )
            }
            .toList()
    }

    fun toCsv(snapshot: JSONObject, stats: SavedStats): String {
        val header = listOf(
            "record_type",
            "group",
            "code",
            "title",
            "subtitle",
            "source_url",
            "num_pages",
            "upload_date",
            "rating",
            "read",
            "read_at",
            "pinned",
            "fetched_at",
            "added_at",
            "media_id",
            "cover_ext",
            "tags",
            "name",
            "type",
            "activity_date",
            "pages_read",
            "entries_read",
            "started_at",
            "ended_at",
            "seconds_elapsed",
            "pages_viewed",
            "count",
            "key",
            "value"
        )
        fun row(vararg values: Any?): String = values.joinToString(",") { csvEscape(it) }

        return buildString {
            appendLine(row(*header.toTypedArray()))
            appendLine(
                row(
                    "summary",
                    "stats",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "entries",
                    stats.entries
                )
            )
            appendLine(row("summary", "stats", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "artists", stats.artists))
            appendLine(row("summary", "stats", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "groups", stats.groups))
            appendLine(row("summary", "stats", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "read_entries", stats.readEntries))

            snapshot.optJSONArray("entries")?.let { entries ->
                for (index in 0 until entries.length()) {
                    val obj = entries.optJSONObject(index) ?: continue
                    val tags = buildList {
                        val tagsArray = obj.optJSONArray("tags")
                        if (tagsArray != null) {
                            for (tagIndex in 0 until tagsArray.length()) {
                                val tagObj = tagsArray.optJSONObject(tagIndex) ?: continue
                                val type = tagObj.optString("type").ifBlank { "tag" }
                                val name = tagObj.optString("name")
                                add("$type:$name")
                            }
                        }
                    }.joinToString(" | ")
                    appendLine(
                        row(
                            "entry",
                            "library",
                            obj.optInt("code"),
                            obj.optString("title"),
                            obj.optString("subtitle"),
                            obj.optString("source_url"),
                            obj.optInt("num_pages"),
                            obj.optString("upload_date"),
                            obj.optInt("rating"),
                            obj.optInt("read"),
                            obj.optString("read_at"),
                            obj.optInt("pinned"),
                            obj.optString("fetched_at"),
                            obj.optString("added_at"),
                            obj.optLong("media_id"),
                            obj.optString("cover_ext"),
                            tags
                        )
                    )
                }
            }

            snapshot.optJSONArray("creators")?.let { creators ->
                for (index in 0 until creators.length()) {
                    val obj = creators.optJSONObject(index) ?: continue
                    appendLine(
                        row(
                            "creator",
                            "library",
                            null,
                            null,
                            null,
                            obj.optString("source_url"),
                            null,
                            null,
                            null,
                            null,
                            null,
                            obj.optInt("pinned"),
                            null,
                            null,
                            null,
                            null,
                            null,
                            obj.optString("name"),
                            obj.optString("type")
                        )
                    )
                }
            }

            snapshot.optJSONArray("daily_read_activity")?.let { activity ->
                for (index in 0 until activity.length()) {
                    val obj = activity.optJSONObject(index) ?: continue
                    appendLine(
                        row(
                            "activity",
                            "daily_read_activity",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            obj.optString("date"),
                            obj.optInt("pages_read"),
                            obj.optInt("entries_read")
                        )
                    )
                }
            }

            snapshot.optJSONArray("reading_sessions")?.let { sessions ->
                for (index in 0 until sessions.length()) {
                    val obj = sessions.optJSONObject(index) ?: continue
                    appendLine(
                        row(
                            "session",
                            "reading_sessions",
                            obj.optInt("entry_code"),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            obj.optString("started_at"),
                            obj.optString("ended_at"),
                            obj.optLong("seconds_elapsed"),
                            obj.optInt("pages_viewed")
                        )
                    )
                }
            }

            snapshot.optJSONArray("popular_tags")?.let { tags ->
                for (index in 0 until tags.length()) {
                    val obj = tags.optJSONObject(index) ?: continue
                    appendLine(
                        row(
                            "tag_stat",
                            "popular_tags",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            obj.optString("name"),
                            obj.optString("type"),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            obj.optLong("count")
                        )
                    )
                }
            }

            snapshot.optJSONArray("hidden_suggested_entries")?.let { hidden ->
                for (index in 0 until hidden.length()) {
                    val obj = hidden.optJSONObject(index) ?: continue
                    appendLine(
                        row(
                            "hidden_suggestion",
                            "hidden_suggested_entries",
                            obj.optInt("code"),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            obj.optString("hidden_at"),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "hidden_at_ms",
                            obj.optLong("hidden_at_ms")
                        )
                    )
                }
            }
        }
    }
}
