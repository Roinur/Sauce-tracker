package com.roinur.saucetracker.data.backup

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

internal object PortablePreferences {
    const val SNAPSHOT_KEY = "portable_preferences"

    private const val FORMAT_VERSION = 1
    private const val KEY_FORMAT_VERSION = "version"
    private const val KEY_VALUES = "values"
    private const val KEY_TYPE = "type"
    private const val KEY_VALUE = "value"

    // These values are tied to an Android install, a granted document tree, or a disposable cache.
    // They must not be copied to another application id.
    private val excludedKeys = setOf(
        "app_lock_grace_until",
        "auto_backup_tree_uri",
        "backup_thumbnail_archive_enabled",
        "gallery_download_tree_uri",
        "gallery_download_skip_prompt",
        "suggestion_result_cache_v1",
        "suggestion_gallery_cache_v1"
    )

    fun encode(preferences: SharedPreferences): JSONObject = encode(preferences.all)

    internal fun encode(values: Map<String, *>): JSONObject {
        val encodedValues = JSONObject()
        values.toSortedMap().forEach { (key, rawValue) ->
            if (!isPortable(key)) return@forEach
            encodeValue(rawValue)?.let { encodedValues.put(key, it) }
        }
        return JSONObject()
            .put(KEY_FORMAT_VERSION, FORMAT_VERSION)
            .put(KEY_VALUES, encodedValues)
    }

    fun apply(preferences: SharedPreferences, snapshot: JSONObject?): Int {
        val decoded = decode(snapshot)
        if (decoded.isEmpty()) return 0
        val editor = preferences.edit()
        decoded.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Float -> editor.putFloat(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is String -> editor.putString(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }
        check(editor.commit()) { "Could not persist imported app settings." }
        return decoded.size
    }

    internal fun decode(snapshot: JSONObject?): Map<String, Any> {
        if (snapshot == null) return emptyMap()
        if (snapshot.optInt(KEY_FORMAT_VERSION, 0) != FORMAT_VERSION) return emptyMap()
        val values = snapshot.optJSONObject(KEY_VALUES) ?: return emptyMap()
        val decoded = linkedMapOf<String, Any>()
        val keys = values.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (!isPortable(key)) continue
            decodeValue(values.optJSONObject(key))?.let { decoded[key] = it }
        }
        return decoded
    }

    internal fun isValidSnapshot(snapshot: JSONObject?): Boolean {
        if (snapshot == null) return false
        if (snapshot.optInt(KEY_FORMAT_VERSION, 0) != FORMAT_VERSION) return false
        val values = snapshot.optJSONObject(KEY_VALUES) ?: return false
        val keys = values.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val encoded = values.optJSONObject(key) ?: return false
            if (isPortable(key) && decodeValue(encoded) == null) return false
        }
        return true
    }

    internal fun isPortable(key: String): Boolean = key.isNotBlank() && key !in excludedKeys

    private fun encodeValue(value: Any?): JSONObject? = when (value) {
        is Boolean -> typed("boolean", value)
        is Float -> typed("float", value.toDouble())
        is Int -> typed("int", value)
        is Long -> typed("long", value)
        is String -> typed("string", value)
        is Set<*> -> {
            val strings = value.filterIsInstance<String>()
            if (strings.size != value.size) null else typed("string_set", JSONArray(strings.sorted()))
        }
        else -> null
    }

    private fun typed(type: String, value: Any): JSONObject = JSONObject()
        .put(KEY_TYPE, type)
        .put(KEY_VALUE, value)

    private fun decodeValue(encoded: JSONObject?): Any? {
        if (encoded == null || !encoded.has(KEY_VALUE)) return null
        return when (encoded.optString(KEY_TYPE)) {
            "boolean" -> encoded.opt(KEY_VALUE) as? Boolean
            "float" -> (encoded.opt(KEY_VALUE) as? Number)?.toFloat()
            "int" -> (encoded.opt(KEY_VALUE) as? Number)?.toInt()
            "long" -> (encoded.opt(KEY_VALUE) as? Number)?.toLong()
            "string" -> encoded.opt(KEY_VALUE) as? String
            "string_set" -> {
                val array = encoded.optJSONArray(KEY_VALUE) ?: return null
                buildSet {
                    for (index in 0 until array.length()) {
                        val item = array.opt(index) as? String ?: return null
                        add(item)
                    }
                }
            }
            else -> null
        }
    }
}
