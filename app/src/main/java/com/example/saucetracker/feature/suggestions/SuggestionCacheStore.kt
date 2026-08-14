package com.example.saucetracker.feature.suggestions

import android.content.SharedPreferences
import com.example.saucetracker.SuggestedEntryRow
import com.example.saucetracker.data.remote.GalleryData
import com.example.saucetracker.data.remote.GalleryTag
import org.json.JSONArray
import org.json.JSONObject

internal data class CachedSuggestionRows(
    val fingerprint: String,
    val rows: List<SuggestedEntryRow>,
    val savedAtMillis: Long
)

internal class SuggestionCacheStore(private val preferences: SharedPreferences) {
    fun loadRows(fingerprint: String): CachedSuggestionRows? {
        val root = loadRowsRoot() ?: return null
        if (root.optString("fingerprint") != fingerprint) return null
        return decodeCachedRows(root)
    }

    fun loadLatestRows(): CachedSuggestionRows? = loadRowsRoot()?.let(::decodeCachedRows)

    fun saveRows(fingerprint: String, rows: List<SuggestedEntryRow>) {
        if (rows.isEmpty()) return
        val root = JSONObject()
            .put("version", 1)
            .put("fingerprint", fingerprint)
            .put("savedAtMillis", System.currentTimeMillis())
            .put("rows", encodeRows(rows.take(MAX_RESULT_ROWS)))
        preferences.edit().putString(KEY_ROWS, root.toString()).apply()
    }

    private fun loadRowsRoot(): JSONObject? = preferences.getString(KEY_ROWS, null)
        ?.takeIf { it.isNotBlank() }
        ?.let { runCatching { JSONObject(it) }.getOrNull() }

    private fun decodeCachedRows(root: JSONObject): CachedSuggestionRows? {
        val rows = decodeRows(root.optJSONArray("rows") ?: JSONArray())
        if (rows.isEmpty()) return null
        return CachedSuggestionRows(
            fingerprint = root.optString("fingerprint"),
            rows = rows,
            savedAtMillis = root.optLong("savedAtMillis", 0L)
        )
    }

    fun loadGalleryMetadata(nowMillis: Long = System.currentTimeMillis()): Map<Int, GalleryData> {
        val root = preferences.getString(KEY_GALLERIES, null)
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { JSONObject(it) }.getOrNull() }
            ?: return emptyMap()
        val rows = root.optJSONArray("rows") ?: return emptyMap()
        val out = linkedMapOf<Int, GalleryData>()
        for (index in 0 until rows.length()) {
            val item = rows.optJSONObject(index) ?: continue
            val savedAt = item.optLong("savedAtMillis", 0L)
            if (savedAt <= 0L || nowMillis - savedAt > GALLERY_CACHE_MAX_AGE_MS) continue
            decodeGallery(item.optJSONObject("gallery") ?: continue)?.let { gallery ->
                out[gallery.code] = gallery
            }
        }
        return out
    }

    fun saveGalleryMetadata(galleries: Collection<GalleryData>) {
        val now = System.currentTimeMillis()
        val rows = JSONArray()
        galleries
            .asSequence()
            .filter { it.code > 0 }
            .distinctBy { it.code }
            .toList()
            .takeLast(MAX_GALLERY_ROWS)
            .forEach { gallery ->
                rows.put(
                    JSONObject()
                        .put("savedAtMillis", now)
                        .put("gallery", encodeGallery(gallery))
                )
            }
        preferences.edit().putString(
            KEY_GALLERIES,
            JSONObject().put("version", 1).put("rows", rows).toString()
        ).apply()
    }

    companion object {
        private const val KEY_ROWS = "suggestion_result_cache_v1"
        private const val KEY_GALLERIES = "suggestion_gallery_cache_v1"
        private const val MAX_RESULT_ROWS = 48
        private const val MAX_GALLERY_ROWS = 160
        private const val GALLERY_CACHE_MAX_AGE_MS = 14L * 24L * 60L * 60L * 1000L

        internal fun encodeRows(rows: List<SuggestedEntryRow>): JSONArray = JSONArray().apply {
            rows.forEach { row ->
                put(
                    JSONObject()
                        .put("code", row.code)
                        .put("title", row.title)
                        .put("numPages", row.numPages)
                        .put("uploadDate", row.uploadDate)
                        .put("thumbnailUrl", row.thumbnailUrl)
                        .put("topTags", JSONArray(row.topTags))
                        .put("score", row.score.toDouble())
                        .put("whySuggestedReason", row.whySuggestedReason)
                )
            }
        }

        internal fun decodeRows(array: JSONArray): List<SuggestedEntryRow> = buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val code = item.optInt("code", 0)
                if (code <= 0) continue
                val tags = item.optJSONArray("topTags") ?: JSONArray()
                add(
                    SuggestedEntryRow(
                        code = code,
                        title = item.optString("title", "Gallery $code"),
                        numPages = item.optInt("numPages", 0).coerceAtLeast(0),
                        uploadDate = item.optString("uploadDate", ""),
                        thumbnailUrl = item.optString("thumbnailUrl", ""),
                        topTags = buildList {
                            for (tagIndex in 0 until tags.length()) {
                                tags.optString(tagIndex).takeIf { it.isNotBlank() }?.let(::add)
                            }
                        },
                        score = item.optDouble("score", 0.0).toFloat(),
                        whySuggestedReason = item.optString("whySuggestedReason", ""),
                        duplicateHint = null
                    )
                )
            }
        }

        private fun encodeGallery(gallery: GalleryData): JSONObject = JSONObject()
            .put("code", gallery.code)
            .put("title", gallery.title)
            .put("subtitle", gallery.subtitle)
            .put("numPages", gallery.numPages)
            .put("uploadDate", gallery.uploadDate)
            .put("sourceUrl", gallery.sourceUrl)
            .put("mediaId", gallery.mediaId)
            .put("coverExt", gallery.coverExt)
            .put("tags", JSONArray().apply {
                gallery.tags.forEach { tag ->
                    put(JSONObject().put("name", tag.name).put("type", tag.type))
                }
            })

        private fun decodeGallery(item: JSONObject): GalleryData? {
            val code = item.optInt("code", 0)
            if (code <= 0) return null
            val tagRows = item.optJSONArray("tags") ?: JSONArray()
            val tags = buildList {
                for (index in 0 until tagRows.length()) {
                    val tag = tagRows.optJSONObject(index) ?: continue
                    val name = tag.optString("name", "").trim()
                    if (name.isNotBlank()) add(GalleryTag(name, tag.optString("type", "tag")))
                }
            }
            return GalleryData(
                code = code,
                title = item.optString("title", "Gallery $code"),
                subtitle = item.optString("subtitle", ""),
                numPages = item.optInt("numPages", 0).coerceAtLeast(0),
                uploadDate = item.optString("uploadDate", ""),
                sourceUrl = item.optString("sourceUrl", ""),
                mediaId = item.optLong("mediaId", 0L).coerceAtLeast(0L),
                coverExt = item.optString("coverExt", "jpg"),
                tags = tags
            )
        }
    }
}
