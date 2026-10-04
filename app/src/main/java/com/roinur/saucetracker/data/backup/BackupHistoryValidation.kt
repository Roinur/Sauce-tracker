package com.roinur.saucetracker.data.backup

import kotlinx.serialization.json.*
import java.util.UUID

/** Legacy defaults are additions, not data loss. Explicitly supplied values stay strict. */
internal object BackupHistoryValidation {
    fun canonicalRows(raw: String, table: String): String {
        val rows = Json.parseToJsonElement(raw) as? JsonArray
            ?: throw IllegalArgumentException("Invalid $table backup array.")
        val normalized = rows.map { value ->
            val row = (value as? JsonObject)?.toMutableMap()
                ?: throw IllegalArgumentException("Invalid $table backup row.")
            row.putIfAbsent("profile_id", JsonPrimitive("main"))
            row.putIfAbsent("source_id", JsonPrimitive("nhentai"))
            if (table == "reading_sessions") {
                fun text(key: String) = (row[key] as? JsonPrimitive)?.content.orEmpty()
                row.putIfAbsent("remote_id", JsonPrimitive(text("entry_code")))
                row.putIfAbsent("chapter_id", JsonPrimitive(""))
                val reread = text("is_reread").lowercase() in setOf("true", "1", "yes", "on")
                val seed = "${text("source_id")}|${text("remote_id")}|${text("started_at")}|${text("ended_at")}|${text("entry_code")}|${text("pages_viewed")}|${text("seconds_elapsed")}|$reread"
                row.putIfAbsent("session_key", JsonPrimitive(UUID.nameUUIDFromBytes(seed.toByteArray(Charsets.UTF_8)).toString()))
            }
            canonical(JsonObject(row))
        }
        return normalized.sorted().joinToString(",", "[", "]")
    }

    private fun canonical(value: JsonElement): String = when (value) {
        is JsonObject -> value.keys.sorted().joinToString(",", "{", "}") { "${JsonPrimitive(it)}:${canonical(value.getValue(it))}" }
        is JsonArray -> value.map(::canonical).sorted().joinToString(",", "[", "]")
        else -> value.toString()
    }

    /** Browser-only reading history need not have a library entry, but must have an owner. */
    fun validateOwners(platformRaw: String?, arrays: List<String?>) {
        val platform = platformRaw?.let { Json.parseToJsonElement(it).jsonObject }
        val profiles = platform?.get("profiles")?.jsonArray?.map { it.jsonObject.getValue("id").jsonPrimitive.content }?.toSet()
        val sources = platform?.get("sources")?.jsonArray?.map { it.jsonObject.getValue("id").jsonPrimitive.content }?.toSet()
        arrays.filterNotNull().forEach { raw ->
            Json.parseToJsonElement(raw).jsonArray.forEach { value ->
                val row = value as? JsonObject ?: throw IllegalArgumentException("Invalid history/subscription backup row.")
                val source = (row["source_id"] as? JsonPrimitive)?.content?.ifBlank { "nhentai" } ?: "nhentai"
                val profile = (row["profile_id"] as? JsonPrimitive)?.content?.ifBlank { "main" } ?: "main"
                if (platform == null) {
                    require(source == "nhentai" && profile == "main") {
                        "Incomplete multi-source backup: profiles/source metadata are missing. Use a complete 2.0 export; the original file is unchanged."
                    }
                } else {
                    require(profile in profiles.orEmpty() && source in sources.orEmpty()) {
                        "Backup history/subscriptions reference an unknown profile or source."
                    }
                }
            }
        }
    }
}
