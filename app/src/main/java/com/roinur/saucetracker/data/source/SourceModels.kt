package com.roinur.saucetracker.data.source

import java.util.Locale

@JvmInline
value class SourceId(val value: String) {
    init {
        require(value.matches(Regex("[a-z0-9][a-z0-9_-]{1,31}"))) { "Invalid source id." }
    }

    override fun toString(): String = value
}

data class SourceEntryKey(
    val sourceId: SourceId,
    val remoteId: String
) {
    init {
        require(remoteId.isNotBlank()) { "Remote id must not be blank." }
        require(remoteId.length <= 160) { "Remote id is too long." }
    }

    val storageKey: String get() = "${sourceId.value}:${remoteId.trim()}"
    val displayId: String get() = if (sourceId.value == "nhentai") "#${remoteId.trim()}" else remoteId.trim()
}

/** Stable positive UI identity for legacy Int-keyed Compose lists. Persistence always uses SourceEntryKey. */
fun SourceEntryKey.uiCode(): Int {
    if (sourceId.value == "nhentai") return remoteId.toIntOrNull()?.takeIf { it > 0 } ?: 0
    val bucket = (storageKey.hashCode() and Int.MAX_VALUE) % 900_000_000
    return 1_000_000_000 + bucket
}

enum class SourceCapability {
    SEARCH,
    BROWSE,
    DETAIL,
    IMPORT,
    METADATA_REFRESH,
    READER,
    DOWNLOADS,
    SUBSCRIPTIONS,
    CREATOR_NAVIGATION,
    POPULAR_SORT,
    RECENT_SORT,
    COMMENTS,
    PAGE_OVERVIEW
}

enum class SourceQueryField {
    FREE_TEXT,
    TAG,
    ARTIST,
    AUTHOR,
    GROUP,
    CHARACTER,
    PARODY,
    LANGUAGE,
    CATEGORY,
    STATUS,
    SOURCE
}

data class SourceQueryTerm(
    val field: SourceQueryField,
    val value: String,
    val excluded: Boolean = false
)

data class SourceQuery(
    val freeText: String = "",
    val terms: List<SourceQueryTerm> = emptyList(),
    val sort: SourceSortMode = SourceSortMode.RECENT
)

enum class SourceSortMode {
    RECENT,
    POPULAR
}

/** Keeps source-aware tag/creator ids separate from legacy SQLite tag ids in existing UI models. */
const val SOURCE_TERM_UI_ID_BASE: Long = 4_000_000_000L

fun sourceTermUiId(storageId: Long): Long = SOURCE_TERM_UI_ID_BASE + storageId.coerceAtLeast(1L)

fun sourceTermStorageId(uiId: Long): Long? =
    (uiId - SOURCE_TERM_UI_ID_BASE).takeIf { uiId > SOURCE_TERM_UI_ID_BASE && it > 0L }

data class SourceTag(
    val name: String,
    val type: String,
    val remoteId: String = ""
)

/**
 * Provider-independent tag identity used by combined profiles.
 * The provider's remote id remains metadata; only an exact normalized type/name pair is shared.
 */
data class SourceSemanticTagKey(
    val type: String,
    val normalizedName: String
)

fun SourceTag.semanticKey(): SourceSemanticTagKey = SourceSemanticTagKey(
    type = normalizeSourceText(type).ifBlank { "tag" },
    normalizedName = normalizeSourceText(name)
)

data class SourceCreator(
    val name: String,
    val type: String,
    val remoteId: String = "",
    val url: String = ""
)

data class SourceEntry(
    val key: SourceEntryKey,
    val title: String,
    val alternateTitles: List<String> = emptyList(),
    val canonicalUrl: String,
    val thumbnailUrl: String = "",
    val unitCount: Int = 0,
    val unitLabel: String = "pages",
    val publishedAt: String = "",
    val status: String = "",
    val tags: List<SourceTag> = emptyList(),
    val creators: List<SourceCreator> = emptyList(),
    val sourcePayload: String = ""
)

data class SourceSearchPage(
    val sourceId: SourceId,
    val entries: List<SourceEntry>,
    val offset: Int,
    val limit: Int,
    val total: Int?,
    val hasMore: Boolean
)

data class SourceChapter(
    val id: String,
    val label: String,
    val number: String = "",
    val volume: String = "",
    val title: String = "",
    val language: String = "",
    val scanlationGroups: List<String> = emptyList(),
    val publishedAt: String = "",
    val pageCount: Int = 0,
    val previewUrl: String = ""
) {
    val displayTitle: String
        get() = listOf(label, title)
            .map(String::trim)
            .filter(String::isNotBlank)
            .joinToString(" · ")
}

data class SourceChapterPage(
    val entryKey: SourceEntryKey,
    val chapters: List<SourceChapter>,
    val offset: Int,
    val limit: Int,
    val total: Int?,
    val hasMore: Boolean
)

data class SourceReaderContent(
    val entryKey: SourceEntryKey,
    val chapterId: String,
    val chapterLabel: String,
    val chapterTitle: String = "",
    val language: String = "",
    val scanlationGroups: List<String> = emptyList(),
    val pageUrls: List<String>,
    val dataSaverPageUrls: List<String> = emptyList()
) {
    val displayTitle: String
        get() = listOf(chapterLabel, chapterTitle)
            .map(String::trim)
            .filter(String::isNotBlank)
            .joinToString(" · ")
}

enum class SourceFailureKind {
    OFFLINE,
    RATE_LIMITED,
    NOT_FOUND,
    INVALID_RESPONSE,
    UNSUPPORTED,
    TEMPORARY
}

class SourceException(
    val kind: SourceFailureKind,
    val sourceId: SourceId,
    message: String,
    val retryAfterSeconds: Long? = null,
    cause: Throwable? = null
) : Exception(message, cause)

interface SourceAdapter {
    val id: SourceId
    val displayName: String
    val contractVersion: Int
    val capabilities: Set<SourceCapability>

    fun recognizes(input: String): Boolean
    fun normalizeRemoteId(input: String): String?
    fun canonicalUrl(remoteId: String): String
    fun search(query: SourceQuery, offset: Int = 0, limit: Int = 20): SourceSearchPage
    fun fetchEntry(remoteId: String): SourceEntry
    fun fetchTagCatalog(): List<SourceTag> = emptyList()
    fun supportsQueryField(field: SourceQueryField): Boolean = field != SourceQueryField.STATUS
    fun fetchChapters(remoteId: String, offset: Int = 0, limit: Int = 100): SourceChapterPage {
        throw SourceException(SourceFailureKind.UNSUPPORTED, id, "$displayName does not provide chapters.")
    }
    fun fetchReaderContent(remoteId: String, chapterId: String? = null): SourceReaderContent {
        throw SourceException(SourceFailureKind.UNSUPPORTED, id, "$displayName does not provide reader content.")
    }

    fun supports(capability: SourceCapability): Boolean = capability in capabilities
}

fun sourceQueryField(type: String): SourceQueryField? = when (normalizeSourceText(type)) {
    "tag" -> SourceQueryField.TAG
    "artist" -> SourceQueryField.ARTIST
    "author", "creator" -> SourceQueryField.AUTHOR
    "group" -> SourceQueryField.GROUP
    "character" -> SourceQueryField.CHARACTER
    "parody", "series" -> SourceQueryField.PARODY
    "language" -> SourceQueryField.LANGUAGE
    "category" -> SourceQueryField.CATEGORY
    "status" -> SourceQueryField.STATUS
    "source" -> SourceQueryField.SOURCE
    else -> null
}

fun sourceBrowserQuery(raw: String, selected: List<com.roinur.saucetracker.TagRouteRef>): String =
    (listOf(raw.trim()).filter(String::isNotBlank) + selected.mapNotNull { ref ->
        sourceQueryField(ref.type)?.let { field -> "${field.name.lowercase()}:\"${ref.name.replace("\"", "") }\"" }
    }).joinToString(" ")

internal fun normalizeSourceText(value: String): String = value
    .trim()
    .lowercase(Locale.US)
    .replace(Regex("\\s+"), " ")
