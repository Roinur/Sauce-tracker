package com.roinur.saucetracker.data.source

private val structuredTerm = Regex(
    pattern = "(?i)(^|\\s)(-?)(title|tag|artist|author|group|character|parody|series|language|lang|category|type|status|source):(?:\\\"([^\\\"]+)\\\"|(\\S+))"
)

object SourceQueryParser {
    fun parse(raw: String): SourceQuery {
        val terms = mutableListOf<SourceQueryTerm>()
        val consumed = BooleanArray(raw.length)
        structuredTerm.findAll(raw).forEach { match ->
            for (index in match.range) consumed[index] = true
            val key = match.groupValues[3].lowercase()
            val value = (match.groupValues[4].ifBlank { match.groupValues[5] }).trim()
            if (value.isBlank()) return@forEach
            val field = when (key) {
                "title" -> SourceQueryField.FREE_TEXT
                "tag" -> SourceQueryField.TAG
                "artist" -> SourceQueryField.ARTIST
                "author" -> SourceQueryField.AUTHOR
                "group" -> SourceQueryField.GROUP
                "character" -> SourceQueryField.CHARACTER
                "parody", "series" -> SourceQueryField.PARODY
                "language", "lang" -> SourceQueryField.LANGUAGE
                "category", "type" -> SourceQueryField.CATEGORY
                "status" -> SourceQueryField.STATUS
                "source" -> SourceQueryField.SOURCE
                else -> SourceQueryField.TAG
            }
            terms += SourceQueryTerm(field, value, excluded = match.groupValues[2] == "-")
        }
        val freeText = raw.indices
            .filterNot { consumed[it] }
            .map { raw[it] }
            .joinToString("")
            .replace(Regex("\\s+"), " ")
            .trim()
        return SourceQuery(freeText = (listOf(freeText) + terms.filter { it.field == SourceQueryField.FREE_TEXT && !it.excluded }.map { it.value }).filter(String::isNotBlank).joinToString(" "), terms = terms.filter { it.field != SourceQueryField.FREE_TEXT })
    }
}

