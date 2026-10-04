package com.roinur.saucetracker.data.source

import com.roinur.saucetracker.*
import java.util.Locale

/** The same Search Everything grammar, translated to the shared library rather than a provider API. */
internal data class SourceLibraryFilter(val clauses: List<String>, val arguments: List<String>)

internal fun sourceLibraryFilter(raw: String, resolveTag: (Long) -> TagRouteRef?): SourceLibraryFilter {
    val clauses = mutableListOf<String>()
    val args = mutableListOf<String>()
    fun like(column: String, value: String): String {
        args += "%${value.trim().trim('"').lowercase(Locale.US)}%"
        return "LOWER($column) LIKE ?"
    }
    fun tag(value: String, type: String? = null, exact: Boolean = false): String {
        val name = value.trim().trim('"').lowercase(Locale.US)
        if (type == "source") {
            args += normalizeSourceName(name)
            return "se.source_id=?"
        }
        val creator = type in setOf("artist", "author", "group", "creator")
        val tables = if (creator) listOf("source_entry_creators") else if (type == null) listOf("source_entry_tags", "source_entry_creators") else listOf("source_entry_tags")
        return tables.joinToString(" OR ", "(", ")") { table ->
            val typeClause = if (type != null && type != "creator") { args += type; " AND st.type=?" } else ""
            args += if (exact) name else "%$name%"
            "EXISTS(SELECT 1 FROM $table st WHERE st.source_entry_id=se.id$typeClause AND st.normalized_name ${if (exact) "=" else "LIKE"} ?)"
        }
    }
    fun universal(value: String): String = listOf(
        like("se.title", value), like("se.alternate_titles", value), like("se.remote_id", value),
        like("se.canonical_url", value), tag(value)
    ).joinToString(" OR ", "(", ")")
    fun numeric(column: String, value: String): String {
        val numbers = extractNumericTokens(value)
        return when {
            numbers.size >= 2 -> { args += minOf(numbers[0], numbers[1]).toString(); args += maxOf(numbers[0], numbers[1]).toString(); "$column BETWEEN ? AND ?" }
            numbers.size == 1 -> { args += numbers[0].toString(); "$column=?" }
            else -> like("CAST($column AS TEXT)", value)
        }
    }
    val parsed = parseSearchQuery(raw)
    Regex("\"([^\"]+)\"|(\\S+)").findAll(parsed.freeText).forEach { match ->
        clauses += universal(match.groupValues[1].ifBlank { match.groupValues[2] })
    }
    parsed.filters.forEach { filter ->
        val value = filter.value.trim().trim('"')
        clauses += when (filter.key) {
            "code" -> like("se.remote_id", value.removePrefix("#"))
            "title" -> like("se.title", value)
            "subtitle" -> like("se.alternate_titles", value)
            "pages", "chapters" -> "(se.unit_label='${filter.key}' AND ${numeric("se.unit_count", value)})"
            "rating" -> numeric("pe.rating", value)
            "upload", "added", "fetched" -> {
                val column = when (filter.key) { "upload" -> "se.published_at"; "added" -> "pe.added_at"; else -> "pe.fetched_at" }
                val range = parseDateRange(value)
                if (range != null) { args += range.first.toString(); args += range.second.toString(); "SUBSTR($column,1,10) BETWEEN ? AND ?" }
                else like(column, value)
            }
            "url" -> if (normalizeSourceName(value) in setOf("nhentai", "mangadex")) tag(value, "source") else like("se.canonical_url", value)
            "tag" -> tag(value)
            "anytag", "excludetag", "anytagid", "excludetagid" -> {
                val alternatives = value.split('|').mapNotNull { term ->
                    if (filter.key.endsWith("id")) term.trim().toLongOrNull()?.let(resolveTag)?.let { tag(it.name, it.type, exact = true) }
                    else term.trim().takeIf(String::isNotBlank)?.let { tag(it) }
                }
                val sql = alternatives.joinToString(" OR ", "(", ")").takeIf { alternatives.isNotEmpty() } ?: "0"
                if (filter.key.startsWith("exclude")) "NOT $sql" else sql
            }
            "type" -> {
                args += "%${value.lowercase(Locale.US)}%"; args += "%${value.lowercase(Locale.US)}%"
                "(EXISTS(SELECT 1 FROM source_entry_tags st WHERE st.source_entry_id=se.id AND LOWER(st.type) LIKE ?) OR EXISTS(SELECT 1 FROM source_entry_creators st WHERE st.source_entry_id=se.id AND LOWER(st.type) LIKE ?))"
            }
            "status" -> like("se.status", value)
            "artist", "author", "group", "parody", "character", "category", "language" -> tag(value, filter.key)
            else -> universal(value)
        }
    }
    return SourceLibraryFilter(clauses, args)
}

internal fun normalizeSourceName(value: String): String = normalizeSourceText(value).replace(" ", "")

internal fun <T> readAllLibraryPages(maxEntries: Int = Int.MAX_VALUE, fetchPage: (offset: Int, limit: Int) -> List<T>): List<T> = buildList {
    var offset = 0
    while (size < maxEntries) {
        val limit = minOf(500, maxEntries - size)
        val page = fetchPage(offset, limit)
        addAll(page)
        offset += page.size
        if (page.size < limit) break
    }
}
