package com.roinur.saucetracker.data.source

import org.json.JSONObject
import java.util.Locale

private val mangaDexUuid = Regex("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

fun mangaDexBaseRemoteId(remoteId: String): String =
    remoteId.trim().substringBefore('@').lowercase(Locale.US)

fun mangaDexLanguage(remoteId: String): String =
    remoteId.trim().substringAfter('@', "").lowercase(Locale.US)

fun mangaDexQualifiedRemoteId(remoteId: String, language: String): String {
    val base = mangaDexBaseRemoteId(remoteId)
    require(mangaDexUuid.matches(base)) { "Invalid MangaDex UUID." }
    val normalized = language.trim().lowercase(Locale.US)
    require(normalized.matches(Regex("[a-z]{2,3}(?:-[a-z0-9]{2,8})?"))) { "Invalid MangaDex language." }
    return "$base@$normalized"
}

fun mangaDexLanguageDisplayName(language: String): String {
    val normalized = language.trim().lowercase(Locale.US)
    if (normalized.isBlank()) return ""
    return Locale.forLanguageTag(normalized).getDisplayLanguage(Locale.ENGLISH)
        .takeIf { it.isNotBlank() && !it.equals(normalized, true) }
        ?.replaceFirstChar { it.titlecase(Locale.ENGLISH) }
        ?: normalized.uppercase(Locale.US)
}

/** UI uses human language names; the API uses ISO/BCP-47 values. Exact mapping, never fuzzy. */
fun mangaDexLanguageCode(value: String): String? {
    val normalized = value.trim().lowercase(Locale.US)
    if (normalized.matches(Regex("[a-z]{2,3}(?:-[a-z0-9]{2,8})?"))) return normalized
    return Locale.getISOLanguages().firstOrNull { language ->
        Locale.forLanguageTag(language).getDisplayLanguage(Locale.ENGLISH).equals(normalized, true)
    } ?: mapOf("brazilian portuguese" to "pt-br", "latin american spanish" to "es-la", "traditional chinese" to "zh-hk")[normalized]
}

fun SourceEntry.mangaDexAvailableLanguages(): List<String> {
    if (key.sourceId.value != "mangadex") return emptyList()
    val fromPayload = runCatching {
        val array = JSONObject(sourcePayload).optJSONArray("availableTranslatedLanguages")
        buildList {
            if (array != null) for (index in 0 until array.length()) {
                array.optString(index).trim().lowercase(Locale.US).takeIf(String::isNotBlank)?.let(::add)
            }
        }
    }.getOrDefault(emptyList())
    return (listOfNotNull(mangaDexLanguage(key.remoteId).takeIf(String::isNotBlank)) + fromPayload)
        .distinct()
        .sortedBy(::mangaDexLanguageDisplayName)
}

fun SourceEntry.withMangaDexLanguage(language: String): SourceEntry {
    if (key.sourceId.value != "mangadex") return this
    val normalized = language.trim().lowercase(Locale.US)
    val languageTag = SourceTag(mangaDexLanguageDisplayName(normalized), "language", normalized)
    return copy(
        key = SourceEntryKey(key.sourceId, mangaDexQualifiedRemoteId(key.remoteId, normalized)),
        canonicalUrl = "https://mangadex.org/title/${mangaDexBaseRemoteId(key.remoteId)}",
        tags = tags.filterNot { it.type.equals("language", true) } + languageTag
    )
}
