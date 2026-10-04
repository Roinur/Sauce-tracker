package com.roinur.saucetracker.data.database

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

/** Preserve valid provider title variants; repair only missing or malformed legacy JSON. */
internal fun legacyAlternateTitlesReplacement(currentJson: String, subtitle: String): String? {
    val current = runCatching { Json.parseToJsonElement(currentJson) }.getOrNull()
    if (current is JsonArray && current.all { it is JsonPrimitive && it.isString }) {
        if (current.isNotEmpty() || subtitle.isEmpty()) return null
    }
    return JsonArray(if (subtitle.isEmpty()) emptyList() else listOf(JsonPrimitive(subtitle))).toString()
}
