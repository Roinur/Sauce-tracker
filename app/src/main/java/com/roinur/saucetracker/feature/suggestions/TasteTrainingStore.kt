package com.roinur.saucetracker.feature.suggestions

import android.content.SharedPreferences
import com.roinur.saucetracker.core.preferences.KEY_TASTE_TRAINING_FEEDBACK
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs

internal data class TasteDriver(
    val name: String,
    val type: String
) {
    val key: String get() = "${type.trim().lowercase(Locale.US)}|${name.trim().lowercase(Locale.US)}"
}

internal data class TasteTrainingPrompt(
    val code: Int,
    val title: String,
    val rating: Int,
    val thumbnailUrl: String,
    val drivers: List<TasteDriver>
)

internal data class TasteTrainingFeedback(
    val code: Int,
    val rating: Int,
    val selectedDriverKeys: Set<String>,
    val notAboutMetadata: Boolean,
    val normallyLikeButNotThisEntry: Boolean,
    val updatedAtMillis: Long
)

internal class TasteTrainingStore(private val preferences: SharedPreferences) {
    fun load(): List<TasteTrainingFeedback> {
        val raw = preferences.getString(KEY_TASTE_TRAINING_FEEDBACK, "").orEmpty()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val row = array.optJSONObject(index) ?: continue
                val code = row.optInt("code", 0)
                val rating = row.optInt("rating", 0).coerceIn(0, 5)
                if (code <= 0 || rating !in 1..5) continue
                val keys = buildSet {
                    val values = row.optJSONArray("drivers") ?: JSONArray()
                    for (keyIndex in 0 until values.length()) {
                        values.optString(keyIndex).trim().lowercase(Locale.US)
                            .takeIf { key ->
                                key.substringBefore('|') in TASTE_TRAINING_DRIVER_TYPES &&
                                    key.substringAfter('|', "").isNotBlank()
                            }
                            ?.let(::add)
                    }
                }
                add(
                    TasteTrainingFeedback(
                        code = code,
                        rating = rating,
                        selectedDriverKeys = keys,
                        notAboutMetadata = row.optBoolean("not_about_metadata", false),
                        normallyLikeButNotThisEntry = row.optBoolean("normally_like_but_not_entry", false),
                        updatedAtMillis = row.optLong("updated_at_ms", 0L).coerceAtLeast(0L)
                    )
                )
            }
        }.distinctBy { it.code }.sortedByDescending { it.updatedAtMillis }
    }

    fun save(feedback: TasteTrainingFeedback) {
        val merged = (load().filterNot { it.code == feedback.code } + feedback)
            .sortedByDescending { it.updatedAtMillis }
        persist(merged)
    }

    fun delete(code: Int) = persist(load().filterNot { it.code == code })

    /**
     * A deliberately bounded complement to the existing inferred profile. Explicit feedback can
     * clarify a signal, but cannot take over ranking by itself.
     */
    fun boundedDriverAdjustments(): Map<String, Float> {
        val raw = linkedMapOf<String, Float>()
        load().forEach { feedback ->
            if (feedback.notAboutMetadata || feedback.normallyLikeButNotThisEntry) return@forEach
            val direction = when (feedback.rating) {
                5 -> 0.70f
                4 -> 0.42f
                2 -> -0.42f
                1 -> -0.70f
                else -> 0f
            }
            if (direction == 0f) return@forEach
            feedback.selectedDriverKeys.forEach { key ->
                raw[key] = ((raw[key] ?: 0f) + direction).coerceIn(-2.25f, 2.25f)
            }
        }
        val totalMagnitude = raw.values.sumOf { abs(it).toDouble() }.toFloat()
        if (totalMagnitude <= 12f) return raw
        val scale = 12f / totalMagnitude
        return raw.mapValues { (_, value) -> value * scale }
    }

    fun revision(): Int = preferences.getString(KEY_TASTE_TRAINING_FEEDBACK, "").orEmpty().hashCode()

    private fun persist(rows: List<TasteTrainingFeedback>) {
        val array = JSONArray()
        rows.forEach { row ->
            array.put(
                JSONObject()
                    .put("code", row.code)
                    .put("rating", row.rating)
                    .put("drivers", JSONArray(row.selectedDriverKeys.sorted()))
                    .put("not_about_metadata", row.notAboutMetadata)
                    .put("normally_like_but_not_entry", row.normallyLikeButNotThisEntry)
                    .put("updated_at_ms", row.updatedAtMillis)
            )
        }
        preferences.edit().putString(KEY_TASTE_TRAINING_FEEDBACK, array.toString()).apply()
    }
}

internal val TASTE_TRAINING_DRIVER_TYPES = setOf("tag", "artist", "group")
