package com.roinur.saucetracker.data.backup

import java.io.File
import kotlinx.serialization.json.*
import org.junit.Assert.assertThrows
import org.junit.Test

class SourcePlatformBackupTest {
    private fun fixture(): JsonObject {
        val relative = "tools/fixtures/mixed-v2-source-platform.json"
        val file = listOf(File(relative), File("../$relative")).first { it.isFile }
        return Json.parseToJsonElement(file.readText()).jsonObject
    }
    private fun changed(table: String, transform: (MutableList<JsonElement>) -> Unit): String {
        val root = fixture().toMutableMap()
        val rows = root.getValue(table).jsonArray.toMutableList()
        transform(rows); root[table] = JsonArray(rows)
        return JsonObject(root).toString()
    }
    private fun edit(row: JsonElement, key: String, value: JsonElement): JsonObject =
        JsonObject(row.jsonObject.toMutableMap().apply { put(key, value) })

    @Test fun acceptsMixedSourcesLanguagesAndIndependentProfileProgress() {
        SourcePlatformBackup.validate(fixture().toString())
    }
    @Test fun rejectsMissingParentBeforeRestore() {
        for (table in listOf("profile_sources", "profile_entries", "profile_preferences", "source_reader_progress", "source_chapter_progress")) {
            val raw = changed(table) { it[0] = edit(it[0], "profile_id", JsonPrimitive("missing")) }
            assertThrows(IllegalArgumentException::class.java) { SourcePlatformBackup.validate(raw) }
        }
        val raw = changed("source_entry_tags") { it[0] = edit(it[0], "source_entry_id", JsonPrimitive(999)) }
        assertThrows(IllegalArgumentException::class.java) { SourcePlatformBackup.validate(raw) }
    }
    @Test fun rejectsDuplicateIdentitiesAndMalformedRows() {
        for (table in SourcePlatformBackup.columns.keys) {
            val raw = changed(table) { it.add(it.first()) }
            assertThrows(IllegalArgumentException::class.java) { SourcePlatformBackup.validate(raw) }
        }
        val raw = changed("source_entries") { it[0] = JsonPrimitive("broken row") }
        assertThrows(IllegalArgumentException::class.java) { SourcePlatformBackup.validate(raw) }
    }
    @Test fun rejectsMissingTablesAndActiveProfile() {
        val root = fixture().toMutableMap().apply { remove("profile_entries") }
        assertThrows(IllegalArgumentException::class.java) { SourcePlatformBackup.validate(JsonObject(root).toString()) }
        val raw = changed("app_state") { it[0] = edit(it[0], "active_profile_id", JsonPrimitive("missing")) }
        assertThrows(IllegalArgumentException::class.java) { SourcePlatformBackup.validate(raw) }
    }
    @Test fun rejectsInvalidTitlesAndReaderProgress() {
        val titles = changed("source_entries") { it[0] = edit(it[0], "alternate_titles", JsonPrimitive("[broken")) }
        assertThrows(IllegalArgumentException::class.java) { SourcePlatformBackup.validate(titles) }
        val negative = changed("source_reader_progress") { it[0] = edit(it[0], "page_index", JsonPrimitive(-1)) }
        assertThrows(IllegalArgumentException::class.java) { SourcePlatformBackup.validate(negative) }
        val outside = changed("source_chapter_progress") { it[0] = edit(it[0], "furthest_page_index", JsonPrimitive(40)) }
        assertThrows(IllegalArgumentException::class.java) { SourcePlatformBackup.validate(outside) }
    }
    @Test fun acceptsEarlierPayloadVersionsWithoutLaterProgressTables() {
        for (version in 2..4) {
            val root = fixture().toMutableMap().apply {
                put("schema_version", JsonPrimitive(version))
                if (version < 3) remove("source_reader_progress")
                if (version < 4) remove("source_chapter_cache")
                remove("source_chapter_progress")
            }
            SourcePlatformBackup.validate(JsonObject(root).toString())
        }
    }
    @Test fun acceptsEarlierV5WithoutCompletionTimestamp() {
        val raw = changed("source_chapter_progress") { rows ->
            rows.indices.forEach { rows[it] = JsonObject(rows[it].jsonObject.toMutableMap().apply { remove("completed_at") }) }
        }
        SourcePlatformBackup.validate(raw)
    }
}
