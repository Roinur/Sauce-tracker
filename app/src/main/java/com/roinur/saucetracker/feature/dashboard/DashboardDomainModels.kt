package com.roinur.saucetracker

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class BrowserDuplicateCheckMode(
    val label: String,
    val description: String
) {
    AGGRESSIVE(
        label = "Check every update",
        description = "Re-check browser duplicate hints whenever local duplicate data changes."
    ),
    ONCE_PER_SESSION(
        label = "Check once per session",
        description = "Only check each browser code once until you close and reopen the browser."
    ),
    OFF(
        label = "Off",
        description = "Disable browser duplicate hint checking."
    )
}

internal val CODE_PATTERN = Regex("(?<!\\d)#?(\\d{1,8})(?!\\d)")
internal val SPLIT_CODE_PATTERN = Regex("(?<![#\\d])#?(\\d{1,3}(?:[ \\t]+\\d{1,3})+)(?!\\d)")
internal val GALLERY_LINK_PATTERN = Regex(
    "(?i)(?:https?://)?(?:www\\.)?nhentai\\.net/(?:g|api/gallery)/(\\d{1,8})(?:/)?(?:[?#][^\\s]*)?"
)
internal val NHENTAI_HOME_PATTERN = Regex("(?i)^(?:https?://)?(?:www\\.)?nhentai\\.net/?$")
internal val CREATOR_LINK_PATTERN = Regex(
    "(?i)(?:https?://)?(?:www\\.)?nhentai\\.net/(artist|group)/([^/\\s?#]+)(?:/)?(?:[?#][^\\s]*)?"
)
internal val DIRECT_ROUTE_LINK_PATTERN = Regex(
    "(?i)(?:https?://)?(?:www\\.)?nhentai\\.net/(tag|language|category|parody|character|artist|group)/([^/\\s?#]+)(?:/(?:popular|popular-week|popular-today))?(?:/)?(?:[?#][^\\s]*)?"
)
internal val SEARCH_GALLERY_CODE_PATTERN = Regex("/g/(\\d{1,8})/")
internal val CREATOR_TYPED_INPUT_PATTERN = Regex("(?i)^(artist|group)\\s*:\\s*(.+)$")
internal val CREATOR_NAME_LINE_PATTERN = Regex("^[\\p{L}\\p{N} _.'()\\-]{2,80}$")
internal const val URL_TRAILING_PUNCT = ".,;:!?)]}"
internal val SEARCH_FIELD_PATTERN = Regex(
    "(?i)\\b(code|title|subtitle|pages?|num pages|upload(?: date)?|rating|fetched(?: at)?|added(?: at)?|url|source(?: url)?|link|tags?|artist|group|parody|character|category|language|lang|type)\\s*:\\s*"
)
internal val DATE_TOKEN_PATTERN = Regex("\\d{4}-\\d{2}-\\d{2}")
internal val POPULAR_TAG_ANCHOR_PATTERN = Regex(
    "<a([^>]*?)href=[\"']/(tag|language|category|parody|character|artist|group)/([^\"'/?#]+)(?:/)?[^\"']*[\"'][^>]*>(.*?)</a>",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)
internal val POPULAR_TAG_NAME_SPAN_PATTERN = Regex(
    "<span[^>]*class=[\"'][^\"']*\\bname\\b[^\"']*[\"'][^>]*>(.*?)</span>",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)
internal val POPULAR_TAG_COUNT_SPAN_PATTERN = Regex(
    "<span[^>]*class=[\"'][^\"']*\\bcount\\b[^\"']*[\"'][^>]*>(.*?)</span>",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)
internal val HTML_TAG_PATTERN = Regex("<[^>]+>")

internal val UTC_TIMESTAMP_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)
internal val EXPORT_FILENAME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.US)
internal val UPLOAD_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

enum class ThemeMode(val label: String) {
    SYSTEM("Auto"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class EntrySortField {
    CODE,
    TITLE,
    PAGES,
    UPLOAD,
    ADDED,
    READ,
    RATING
}

enum class EntryReadFilterMode {
    ALL,
    READ,
    UNREAD,
    DOWNLOADED
}

enum class TagSortField {
    NAME,
    TYPE,
    COUNT
}

enum class CreatorSortField {
    NAME,
    TYPE,
    COUNT
}

internal enum class PersonalizationSortTarget {
    ENTRIES,
    TAGS,
    CREATORS
}

enum class HomeSection {
    TAGS,
    ENTRIES,
    SUGGESTED,
    SUBSCRIPTIONS,
    CREATORS,
    HEATMAP
}

enum class DashboardDiscoveryPage {
    SUGGESTED,
    RANDOM,
    SAUCE_FINDER
}

enum class DashboardInsightPage {
    SUBSCRIPTIONS,
    HEATMAP,
    HISTORY
}

internal enum class HomeSurface {
    DASHBOARD,
    ENTRIES,
    TAGS,
    SUGGESTED,
    SUBSCRIPTIONS,
    CREATORS,
    HEATMAP,
    HISTORY
}

internal data class EntrySortPreset(
    val label: String,
    val field: EntrySortField?,
    val direction: SortDirection
)

internal data class TagSortPreset(
    val label: String,
    val field: TagSortField,
    val direction: SortDirection
)

internal data class CreatorSortPreset(
    val label: String,
    val field: CreatorSortField,
    val direction: SortDirection
)

enum class SortDirection {
    ASC,
    DESC
}

enum class StatsRange(val label: String) {
    TODAY("Today"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
    ALL_TIME("All Time")
}

enum class SuggestionMode(val label: String) {
    MIXED("Mixed"),
    TAGS("Top tags"),
    CREATORS("Top creators")
}

enum class SuggestionWeightCategory(
    val storageKey: String,
    val label: String,
    val supportedTypes: Set<String>
) {
    LENGTH("length", "Length / Pages", emptySet()),
    TAG("tag", "General Tags", setOf("tag")),
    PARODY("parody", "Parody", setOf("parody")),
    CHARACTER("character", "Character", setOf("character")),
    CATEGORY("category", "Category", setOf("category")),
    LANGUAGE("language", "Language", setOf("language")),
    CREATOR("creator", "Artist / Group", setOf("artist", "group")),
    OTHER("other", "Other", emptySet());

    companion object {
        fun fromTagType(type: String): SuggestionWeightCategory {
            val normalized = type.trim().lowercase(Locale.US)
            return entries.firstOrNull { normalized in it.supportedTypes } ?: OTHER
        }
    }
}

internal enum class InAppBackActionType {
    ENTRY_SELECTION,
    TAGS_CARD_COLLAPSE,
    ENTRIES_CARD_COLLAPSE,
    CREATORS_CARD_COLLAPSE,
    CREATOR_ROW_EXPANDED
}

internal data class InAppBackAction(
    val type: InAppBackActionType,
    val previousCode: Int? = null,
    val previousBoolean: Boolean? = null,
    val creatorId: Long? = null
)

internal enum class SelectionAnchorContext {
    ENTRY,
    CREATOR_LINK
}

internal data class SelectionAnchor(
    val context: SelectionAnchorContext,
    val code: Int,
    val creatorId: Long? = null,
    val yInRoot: Float
)

typealias GalleryTag = com.roinur.saucetracker.data.remote.GalleryTag
typealias GalleryData = com.roinur.saucetracker.data.remote.GalleryData

typealias EntryRow = com.roinur.saucetracker.data.database.entity.EntryEntity

typealias TagCountRow = com.roinur.saucetracker.data.database.entity.TagEntity

data class TagRouteRef(
    val name: String,
    val type: String
)

data class PopularTagRow(
    val id: Long,
    val name: String,
    val type: String,
    val count: Int,
    val blocked: Boolean
)

data class PopularTagSeed(
    val name: String,
    val type: String,
    val count: Int
)

data class PopularTagFetchResult(
    val tags: List<PopularTagSeed>,
    val pagesFetched: Int
)

data class EntryDetail(
    val code: Int,
    val title: String,
    val subtitle: String,
    val sourceUrl: String,
    val mediaId: Long,
    val coverExt: String,
    val numPages: Int,
    val uploadDate: String,
    val rating: Int,
    val isRead: Boolean,
    val readAt: String,
    val fetchedAt: String,
    val addedAt: String,
    val thumbnailUrl: String,
    val tagsByType: Map<String, List<String>>
)

data class BrowserLibraryStateRow(
    val code: Int,
    val rating: Int,
    val isRead: Boolean,
    val pinned: Boolean
)

data class EntryRatingHistoryRow(
    val sessionId: Long?,
    val readAt: String,
    val rating: Int,
    val isReread: Boolean,
    val isEntrySummary: Boolean,
    val pagesViewed: Int,
    val secondsElapsed: Long
)

data class SeriesCandidateRow(
    val code: Int,
    val title: String,
    val subtitle: String,
    val creatorKeys: Set<String> = emptySet(),
    val thumbnailUrl: String = "",
    val numPages: Int = 0
)

data class SeriesEntryPreview(
    val code: Int,
    val title: String,
    val sequence: Int?,
    val score: Float,
    val thumbnailUrl: String = "",
    val numPages: Int = 0
)

data class SeriesNeighbors(
    val previous: SeriesEntryPreview? = null,
    val next: SeriesEntryPreview? = null,
    val parts: List<SeriesEntryPreview> = emptyList(),
    val currentPartIndex: Int = -1
)

data class ImportResult(
    val processed: Int,
    val imported: Int,
    val inserted: Int,
    val updated: Int,
    val skipped: Int,
    val insertedCodes: List<Int> = emptyList(),
    val creatorsProcessed: Int = 0,
    val creatorsAdded: Int = 0,
    val creatorsSkipped: Int = 0,
    val creatorsDuplicates: Int = 0,
    val artistsProcessed: Int = 0,
    val artistsAdded: Int = 0,
    val artistsDuplicates: Int = 0,
    val artistsSkipped: Int = 0,
    val groupsProcessed: Int = 0,
    val groupsAdded: Int = 0,
    val groupsDuplicates: Int = 0,
    val groupsSkipped: Int = 0,
    val popularTagRows: Int? = null,
    val entryHeatmapCacheRows: Int? = null,
    val subscriptionRows: Int? = null,
    val subscriptionSeenRows: Int? = null,
    val subscriptionEventRows: Int? = null,
    val dailyReadActivityRows: Int? = null,
    val readingSessionRows: Int? = null
)

data class ClearAllResult(
    val entriesCleared: Int,
    val creatorsCleared: Int
)

data class SavedStats(
    val entries: Int,
    val artists: Int,
    val groups: Int,
    val readEntries: Int
)

data class AnalyticsCountRow(
    val name: String,
    val type: String,
    val count: Int
)

data class DailyActivityPoint(
    val date: LocalDate,
    val pagesRead: Int,
    val entriesRead: Int
)

data class DayReadEntryRow(
    val rowKey: String,
    val code: Int,
    val title: String,
    val thumbnailUrl: String,
    val readAt: String,
    val pagesViewed: Int,
    val secondsElapsed: Long,
    val sessionCount: Int,
    val isReread: Boolean
)

data class ReadingSpeedStats(
    val totalPagesViewed: Int = 0,
    val totalSecondsElapsed: Long = 0L,
    val pagesPerMinute: Float = 0f
) {
    val hasEnoughData: Boolean
        get() = totalSecondsElapsed >= 120L && totalPagesViewed >= 20 && pagesPerMinute > 0f
}

data class ReadAnalyticsSnapshot(
    val readCounts: Map<StatsRange, Int> = emptyMap(),
    val pagesRead: Map<StatsRange, Int> = emptyMap(),
    val averageRatings: Map<StatsRange, Float> = emptyMap(),
    val topTags: Map<StatsRange, List<AnalyticsCountRow>> = emptyMap(),
    val topCreators: Map<StatsRange, List<AnalyticsCountRow>> = emptyMap(),
    val dailyActivity: Map<StatsRange, List<DailyActivityPoint>> = emptyMap(),
    val readingSpeed: Map<StatsRange, ReadingSpeedStats> = emptyMap()
)
