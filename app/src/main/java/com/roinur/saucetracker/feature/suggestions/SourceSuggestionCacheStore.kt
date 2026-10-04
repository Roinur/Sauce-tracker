package com.roinur.saucetracker.feature.suggestions

import android.content.SharedPreferences
import com.roinur.saucetracker.data.source.*
import org.json.JSONArray
import org.json.JSONObject

/** Stored under profile-scoped preference keys, like the existing recommendation cache. Metadata only. */
internal class SourceSuggestionCacheStore(private val preferences: SharedPreferences) {
    fun load(fingerprint: String): SourceSuggestionResult? = runCatching {
        val root = JSONObject(preferences.getString(KEY, "").orEmpty())
        if (root.optString("fingerprint") != fingerprint || System.currentTimeMillis() - root.optLong("saved_at") > MAX_AGE) return null
        val rows = root.getJSONArray("rows")
        SourceSuggestionResult(buildList {
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                val entry = SourceEntry(
                    key = SourceEntryKey(SourceId(row.getString("source")), row.getString("remote")),
                    title = row.getString("title"), canonicalUrl = row.getString("url"),
                    thumbnailUrl = row.optString("thumbnail"), unitCount = row.optInt("count"),
                    unitLabel = row.optString("unit"), publishedAt = row.optString("published"), status = row.optString("status"),
                    alternateTitles = strings(row.optJSONArray("alternate")), sourcePayload = row.optString("payload"),
                    tags = objects(row.optJSONArray("tags")).map { SourceTag(it.optString("name"), it.optString("type"), it.optString("id")) },
                    creators = objects(row.optJSONArray("creators")).map { SourceCreator(it.optString("name"), it.optString("type"), it.optString("id"), it.optString("url")) }
                )
                add(ScoredSourceSuggestion(entry, SuggestionScoreBreakdown(row.optDouble("score").toFloat(), strings(row.optJSONArray("top_tags")).map { it to 1f }, row.optString("reason"))))
            }
        }, emptyMap())
    }.getOrNull()

    fun save(fingerprint: String, result: SourceSuggestionResult) {
        if (result.errors.isNotEmpty()) return
        val rows = JSONArray()
        result.candidates.take(60).forEach { candidate ->
            val entry = candidate.entry
            rows.put(JSONObject().put("source", entry.key.sourceId.value).put("remote", entry.key.remoteId)
                .put("title", entry.title).put("alternate", JSONArray(entry.alternateTitles)).put("url", entry.canonicalUrl)
                .put("thumbnail", entry.thumbnailUrl).put("count", entry.unitCount).put("unit", entry.unitLabel)
                .put("published", entry.publishedAt).put("status", entry.status).put("payload", entry.sourcePayload)
                .put("tags", JSONArray(entry.tags.map { JSONObject().put("name", it.name).put("type", it.type).put("id", it.remoteId) }))
                .put("creators", JSONArray(entry.creators.map { JSONObject().put("name", it.name).put("type", it.type).put("id", it.remoteId).put("url", it.url) }))
                .put("score", candidate.breakdown.score.toDouble()).put("reason", candidate.breakdown.whySuggestedReason)
                .put("top_tags", JSONArray(candidate.breakdown.rankedTags.sortedByDescending { it.second }.map { it.first }.take(4))))
        }
        preferences.edit().putString(KEY, JSONObject().put("fingerprint", fingerprint).put("saved_at", System.currentTimeMillis()).put("rows", rows).toString()).apply()
    }

    private fun objects(array: JSONArray?): List<JSONObject> = if (array == null) emptyList() else (0 until array.length()).mapNotNull(array::optJSONObject)
    private fun strings(array: JSONArray?): List<String> = if (array == null) emptyList() else (0 until array.length()).map(array::optString)

    companion object {
        private const val KEY = "suggestion_source_result_cache_v2"
        private const val MAX_AGE = 24 * 60 * 60 * 1000L
    }
}
