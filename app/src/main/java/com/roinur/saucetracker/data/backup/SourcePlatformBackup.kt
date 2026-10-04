package com.roinur.saucetracker.data.backup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/** Portable source payload version, independent of the SQLite schema version. */
internal object SourcePlatformBackup {
    const val VERSION = 5
    val columns = linkedMapOf(
        "sources" to listOf("id", "display_name", "adapter_version", "enabled"),
        "profiles" to listOf("id", "name", "kind", "created_at", "last_used_at", "is_main"),
        "profile_sources" to listOf("profile_id", "source_id"),
        "source_entries" to listOf("id", "source_id", "remote_id", "legacy_code", "title", "alternate_titles", "canonical_url", "thumbnail_url", "unit_count", "unit_label", "published_at", "status", "fetched_at", "provider_revision", "source_payload"),
        "source_entry_tags" to listOf("source_entry_id", "name", "type", "normalized_name", "remote_id"),
        "source_entry_creators" to listOf("source_entry_id", "name", "type", "normalized_name", "remote_id", "source_url"),
        "profile_entries" to listOf("profile_id", "source_entry_id", "read_state", "rating", "pinned", "pin_priority", "read_at", "added_at", "fetched_at"),
        "profile_entry_local_tags" to listOf("profile_id", "source_entry_id", "name", "normalized_name"),
        "profile_preferences" to listOf("profile_id", "preference_key", "value_json", "updated_at"),
        "source_chapter_cache" to listOf("source_id", "remote_id", "chapter_count", "payload_json", "fetched_at_epoch_ms"),
        "source_reader_progress" to listOf("profile_id", "source_id", "remote_id", "chapter_id", "page_index", "updated_at"),
        "source_chapter_progress" to listOf("profile_id", "source_id", "remote_id", "chapter_id", "furthest_page_index", "page_count", "completed", "completed_at", "updated_at"),
        "app_state" to listOf("slot_id", "active_profile_id")
    )
    val deleteOrder = listOf("source_chapter_progress", "source_reader_progress", "source_chapter_cache", "profile_entry_local_tags", "profile_entries", "source_entry_creators", "source_entry_tags", "profile_preferences", "profile_sources", "app_state", "source_entries", "profiles", "sources")

    /** Read-only preflight: do not silently skip broken rows or install dangling parents. */
    fun validate(raw: String) {
        val root = Json.parseToJsonElement(raw) as? JsonObject
            ?: throw IllegalArgumentException("Invalid source platform backup object.")
        val version = (root["schema_version"] as? JsonPrimitive)?.intOrNull
        require(version != null && version in 2..VERSION) { "Unsupported source platform backup schema." }
        fun rows(table: String): List<JsonObject> {
            val introduced = when (table) { "source_reader_progress" -> 3; "source_chapter_cache" -> 4; "source_chapter_progress" -> 5; else -> 2 }
            val value = root[table]
            if (value == null && version < introduced) return emptyList()
            require(value is JsonArray) { "Invalid source backup: missing $table array." }
            return value.map { row ->
                require(row is JsonObject) { "Invalid source backup: non-object row in $table." }
                columns.getValue(table).forEach { column ->
                    val cell = row[column]
                    require(cell == null || cell is JsonPrimitive) { "Invalid source backup: nonscalar $table.$column." }
                }
                row
            }
        }
        val data = columns.keys.associateWith(::rows)
        fun text(row: JsonObject, key: String): String {
            val cell = row[key] as? JsonPrimitive
            require(cell != null && cell.isString && cell.content.isNotBlank()) { "Invalid source backup identity: $key." }
            return cell.content
        }
        fun number(row: JsonObject, key: String): Long {
            val cell = row[key] as? JsonPrimitive
            val n = cell?.longOrNull
            require(cell != null && !cell.isString && n != null) { "Invalid source backup number: $key." }
            return n
        }
        fun unique(table: String, keys: List<String>) {
            val seen = hashSetOf<List<String>>()
            data.getValue(table).forEach { row ->
                val key = keys.map { column ->
                    val cell = row[column]
                    require(cell is JsonPrimitive && cell != JsonNull) { "Invalid source backup key in $table." }
                    cell.toString()
                }
                require(seen.add(key)) { "Duplicate source backup row in $table." }
            }
        }
        val keys = mapOf(
            "sources" to listOf("id"), "profiles" to listOf("id"), "profile_sources" to listOf("profile_id", "source_id"),
            "source_entries" to listOf("id"), "source_entry_tags" to listOf("source_entry_id", "type", "normalized_name"),
            "source_entry_creators" to listOf("source_entry_id", "type", "normalized_name"), "profile_entries" to listOf("profile_id", "source_entry_id"),
            "profile_entry_local_tags" to listOf("profile_id", "source_entry_id", "normalized_name"), "profile_preferences" to listOf("profile_id", "preference_key"),
            "source_chapter_cache" to listOf("source_id", "remote_id"), "source_reader_progress" to listOf("profile_id", "source_id", "remote_id"),
            "source_chapter_progress" to listOf("profile_id", "source_id", "remote_id", "chapter_id"), "app_state" to listOf("slot_id")
        )
        keys.forEach { (table, identity) -> unique(table, identity) }
        unique("source_entries", listOf("source_id", "remote_id"))
        val sourceIds = data.getValue("sources").map { text(it, "id") }.toSet()
        val profileIds = data.getValue("profiles").map { text(it, "id") }.toSet()
        val entryIds = data.getValue("source_entries").map { number(it, "id") }.toSet()
        require(profileIds.isNotEmpty() && sourceIds.isNotEmpty()) { "Source backup contains no valid profiles or sources." }
        fun parent(table: String, column: String, allowed: Set<String>) = data.getValue(table).forEach {
            require(text(it, column) in allowed) { "Invalid source backup: missing parent for $table.$column." }
        }
        listOf("profile_sources", "source_entries", "source_chapter_cache", "source_reader_progress", "source_chapter_progress").forEach { parent(it, "source_id", sourceIds) }
        listOf("profile_sources", "profile_entries", "profile_entry_local_tags", "profile_preferences", "source_reader_progress", "source_chapter_progress").forEach { parent(it, "profile_id", profileIds) }
        listOf("source_entry_tags", "source_entry_creators", "profile_entries", "profile_entry_local_tags").forEach { table ->
            data.getValue(table).forEach { require(number(it, "source_entry_id") in entryIds) { "Invalid source backup: missing entry in $table." } }
        }
        data.getValue("source_entries").forEach { row ->
            text(row, "remote_id")
            val titles = (row["alternate_titles"] as? JsonPrimitive)?.takeIf { it.isString }?.content
            val parsed = titles?.let { runCatching { Json.parseToJsonElement(it) }.getOrNull() }
            require(parsed is JsonArray && parsed.all { it is JsonPrimitive && it.isString }) { "Invalid source backup alternate titles." }
        }
        val appState = data.getValue("app_state")
        require(appState.size == 1 && number(appState.single(), "slot_id") == 1L) { "Invalid source backup active-profile state." }
        require(text(appState.single(), "active_profile_id") in profileIds) { "Source backup active profile is missing." }
        data.getValue("source_reader_progress").forEach { require(number(it, "page_index") >= 0) { "Invalid saved reader page." } }
        data.getValue("source_chapter_progress").forEach { row ->
            val page = number(row, "furthest_page_index"); val count = number(row, "page_count")
            require(page >= 0 && count >= 0 && (count == 0L || page < count)) { "Invalid saved chapter progress." }
        }
    }
}
