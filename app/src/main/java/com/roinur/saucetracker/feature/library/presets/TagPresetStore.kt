package com.roinur.saucetracker.feature.library.presets

import android.content.SharedPreferences
import com.roinur.saucetracker.core.preferences.KEY_TAG_PRESETS
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

/**
 * Rules are deliberately explicit: Include is an AND group, Either is an OR
 * group, and Hide removes matching entries after the positive rules match.
 */
internal enum class TagPresetRole { INCLUDE, EITHER, HIDE }

internal data class TagPresetTerm(
    val name: String,
    val type: String,
    val role: TagPresetRole
) {
    val key: String get() = "${type.trim().lowercase(Locale.US)}|${name.trim().lowercase(Locale.US)}"
}

internal data class TagPreset(
    val id: String,
    val name: String,
    val terms: List<TagPresetTerm>,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

internal class TagPresetStore(private val preferences: SharedPreferences) {
    fun load(): List<TagPreset> {
        val raw = preferences.getString(KEY_TAG_PRESETS, "").orEmpty()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val row = array.optJSONObject(index) ?: continue
                val id = row.optString("id").ifBlank { UUID.randomUUID().toString() }
                val name = row.optString("name").trim()
                if (name.isBlank()) continue
                val terms = buildList {
                    val values = row.optJSONArray("terms") ?: JSONArray()
                    for (termIndex in 0 until values.length()) {
                        val term = values.optJSONObject(termIndex) ?: continue
                        val termName = term.optString("name").trim()
                        val type = term.optString("type", "tag").trim().lowercase(Locale.US)
                        val role = when (term.optString("role").trim().uppercase(Locale.US)) {
                            "INCLUDE", "ALL" -> TagPresetRole.INCLUDE
                            "ANY", "ONE_OF", "ONEOF", "EITHER" -> TagPresetRole.EITHER
                            // EXCLUDE is kept as a read alias so existing presets survive the rename.
                            "EXCLUDE", "HIDE" -> TagPresetRole.HIDE
                            else -> null
                        }
                        if (termName.isNotBlank() && role != null) add(TagPresetTerm(termName, type, role))
                    }
                }.distinctBy { "${it.role}|${it.key}" }
                add(
                    TagPreset(
                        id = id,
                        name = name,
                        terms = terms,
                        createdAtMillis = row.optLong("created_at_ms", 0L),
                        updatedAtMillis = row.optLong("updated_at_ms", 0L)
                    )
                )
            }
        }
    }

    fun upsert(preset: TagPreset) {
        val current = load().toMutableList()
        val index = current.indexOfFirst { it.id == preset.id }
        if (index >= 0) current[index] = preset else current += preset
        persist(current)
    }

    fun delete(id: String) = persist(load().filterNot { it.id == id })

    fun move(id: String, delta: Int) {
        val rows = load().toMutableList()
        val from = rows.indexOfFirst { it.id == id }
        if (from < 0) return
        val to = (from + delta).coerceIn(0, rows.lastIndex)
        if (to == from) return
        val item = rows.removeAt(from)
        rows.add(to, item)
        persist(rows)
    }

    private fun persist(rows: List<TagPreset>) {
        val array = JSONArray()
        rows.forEach { row ->
            array.put(
                JSONObject()
                    .put("id", row.id)
                    .put("name", row.name)
                    .put("created_at_ms", row.createdAtMillis)
                    .put("updated_at_ms", row.updatedAtMillis)
                    .put("terms", JSONArray().apply {
                        row.terms.forEach { term ->
                            put(JSONObject().put("name", term.name).put("type", term.type).put("role", term.role.name))
                        }
                    })
            )
        }
        preferences.edit().putString(KEY_TAG_PRESETS, array.toString()).apply()
    }
}
