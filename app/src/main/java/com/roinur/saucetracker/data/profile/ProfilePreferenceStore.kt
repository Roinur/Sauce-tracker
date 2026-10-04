package com.roinur.saucetracker.data.profile

import android.content.ContentValues
import android.content.SharedPreferences
import com.roinur.saucetracker.data.database.SauceTrackerDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Keeps taste/recommendation/subscription preferences isolated while legacy UI still uses SharedPreferences. */
internal class ProfilePreferenceStore(private val database: SauceTrackerDatabase) {
    fun ensureSeed(profileId: String, preferences: SharedPreferences) {
        val count = database.readableDatabase.rawQuery("SELECT COUNT(*) FROM profile_preferences WHERE profile_id=?", arrayOf(profileId)).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        if (count == 0 && profileId == ProfileStore.MAIN_PROFILE_ID) capture(profileId, preferences)
    }

    fun capture(profileId: String, preferences: SharedPreferences) {
        val scoped = preferences.all.filterKeys(::isProfileKey)
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            db.delete("profile_preferences", "profile_id=?", arrayOf(profileId))
            scoped.forEach { (key, value) ->
                db.insertOrThrow("profile_preferences", null, ContentValues().apply {
                    put("profile_id", profileId); put("preference_key", key); put("value_json", encode(value)); put("updated_at", Instant.now().toString())
                })
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun apply(profileId: String, preferences: SharedPreferences) {
        val rows = linkedMapOf<String, String>()
        database.readableDatabase.rawQuery("SELECT preference_key,value_json FROM profile_preferences WHERE profile_id=?", arrayOf(profileId)).use { cursor ->
            while (cursor.moveToNext()) rows[cursor.getString(0)] = cursor.getString(1)
        }
        val editor = preferences.edit()
        preferences.all.keys.filter(::isProfileKey).forEach(editor::remove)
        rows.forEach { (key, encoded) -> decodeInto(editor, key, encoded) }
        check(editor.commit()) { "Could not activate profile preferences." }
    }

    /** Read-only view for Bridge scoring. Never activates or captures a profile. */
    fun snapshot(profileId: String): Map<String, Any?> = database.readableDatabase.rawQuery(
        "SELECT preference_key,value_json FROM profile_preferences WHERE profile_id=?", arrayOf(profileId)
    ).use { cursor -> buildMap {
        while (cursor.moveToNext()) {
            val row = runCatching { JSONObject(cursor.getString(1)) }.getOrNull() ?: continue
            put(cursor.getString(0), row.opt("value"))
        }
    } }

    private fun isProfileKey(key: String): Boolean = key.startsWith("suggestion_") ||
        key.startsWith("taste_training_") || key.startsWith("tag_presets_") ||
        key == "subscription_refresh_interval_hours"

    private fun encode(value: Any?): String = when (value) {
        is String -> JSONObject().put("type", "string").put("value", value).toString()
        is Boolean -> JSONObject().put("type", "boolean").put("value", value).toString()
        is Int -> JSONObject().put("type", "int").put("value", value).toString()
        is Long -> JSONObject().put("type", "long").put("value", value).toString()
        is Float -> JSONObject().put("type", "float").put("value", value.toDouble()).toString()
        is Set<*> -> JSONObject().put("type", "strings").put("value", JSONArray(value.filterIsInstance<String>().sorted())).toString()
        else -> JSONObject().put("type", "string").put("value", value?.toString().orEmpty()).toString()
    }

    private fun decodeInto(editor: SharedPreferences.Editor, key: String, encoded: String) {
        val row = JSONObject(encoded)
        when (row.optString("type")) {
            "boolean" -> editor.putBoolean(key, row.optBoolean("value"))
            "int" -> editor.putInt(key, row.optInt("value"))
            "long" -> editor.putLong(key, row.optLong("value"))
            "float" -> editor.putFloat(key, row.optDouble("value").toFloat())
            "strings" -> {
                val array = row.optJSONArray("value") ?: JSONArray()
                editor.putStringSet(key, buildSet { for (index in 0 until array.length()) array.optString(index).takeIf(String::isNotBlank)?.let(::add) })
            }
            else -> editor.putString(key, row.optString("value"))
        }
    }
}
