package com.example.saucetracker

import org.json.JSONArray

internal val IGNORED_SUGGESTION_TAG_NAMES = setOf("translated", "translation")

data class DuplicateHint(
    val matchedCode: Int,
    val score: Float,
    val reason: String
)

data class LocalDuplicateSeed(
    val code: Int,
    val titleKey: String,
    val numPages: Int,
    val uploadDate: String,
    val mediaId: Long,
    val creatorKeys: Set<String>,
    val tagKeys: Set<String>,
    val artistKeys: Set<String> = emptySet(),
    val groupKeys: Set<String> = emptySet(),
    val thumbnailUrl: String = ""
)

data class SuggestedEntryRow(
    val code: Int,
    val title: String,
    val numPages: Int,
    val uploadDate: String,
    val thumbnailUrl: String,
    val topTags: List<String>,
    val score: Float,
    val whySuggestedReason: String = "",
    val duplicateHint: DuplicateHint? = null
)

internal data class SuggestedDuplicateComparisonState(
    val suggestion: SuggestedEntryRow,
    val hint: DuplicateHint
)

internal data class LocalDuplicateSeedIndex(
    val allSeeds: List<LocalDuplicateSeed>,
    val byCode: Map<Int, LocalDuplicateSeed>,
    val byMediaId: Map<Long, List<LocalDuplicateSeed>>,
    val byPageCount: Map<Int, List<LocalDuplicateSeed>>,
    val byUploadDate: Map<String, List<LocalDuplicateSeed>>,
    val byTitleKey: Map<String, List<LocalDuplicateSeed>>,
    val byTitleTrigram: Map<String, List<LocalDuplicateSeed>>
)

data class BrowserRatingPromptState(
    val code: Int,
    val title: String,
    val rating: Int,
    val wasReadBefore: Boolean = false,
    val isReread: Boolean = false
)

data class PinTogglePromptState(
    val code: Int,
    val targetPinned: Boolean
)

data class SplitSequence(
    val start: Int,
    val endExclusive: Int,
    val raw: String,
    val merged: String
)

data class BatchProgressState(
    val total: Int,
    val processed: Int,
    val saved: Int,
    val notFound: Int,
    val failed: Int,
    val currentCode: Int?
)

data class StartupPreloadState(
    val phase: String,
    val completedSteps: Int,
    val totalSteps: Int,
    val thumbsDone: Int = 0,
    val thumbsTotal: Int = 0
)

data class BackupProgressState(
    val label: String,
    val processed: Int,
    val total: Int,
    val written: Int,
    val reused: Int,
    val failed: Int
)

data class EntryDownloadProgressState(
    val code: Int,
    val label: String,
    val fraction: Float?
)

enum class EntryDownloadBatchMode {
    DOWNLOAD,
    REDOWNLOAD
}

data class EntryDownloadBatchProgressState(
    val mode: EntryDownloadBatchMode,
    val processed: Int,
    val total: Int,
    val currentCode: Int?,
    val label: String,
    val itemFraction: Float?
)

data class ThumbnailPreviewState(
    val code: Int,
    val thumbnailUrl: String,
    val contentDescription: String
)

internal data class LocalEntryHoldPopupState(
    val code: Int,
    val rating: Int
)

internal enum class SuggestedDragAction {
    CANCEL,
    HIDE
}

data class SplitPromptState(
    val count: Int,
    val preview: String
)

data class ShortPromptState(
    val count: Int,
    val preview: String
)

data class ManualCreatorPromptState(
    val phrase: String
)

data class BatchCreatorPromptState(
    val count: Int,
    val preview: String
)

typealias CreatorRow = com.example.saucetracker.data.database.entity.CreatorEntity

data class CreatorEntryRow(
    val code: Int,
    val title: String
)

data class CreatorLink(
    val type: String,
    val name: String,
    val sourceUrl: String
)

typealias SubscriptionRow = com.example.saucetracker.data.database.entity.SubscriptionEntity

data class SubscriptionEventRow(
    val id: Long,
    val subscriptionId: Long,
    val routeName: String,
    val routeType: String,
    val code: Int,
    val title: String,
    val thumbnailUrl: String,
    val numPages: Int,
    val uploadDate: String,
    val sourceUrl: String,
    val discoveredAt: String,
    val dismissed: Boolean,
    val pinned: Boolean
)

data class SearchFieldFilter(
    val key: String,
    val value: String
)

data class ParsedSearchQuery(
    val freeText: String,
    val filters: List<SearchFieldFilter>
)

data class ParsedImportPayload(
    val entries: JSONArray,
    val creators: JSONArray,
    val popularTags: JSONArray? = null,
    val entryHeatmapCache: JSONArray? = null,
    val subscriptions: JSONArray? = null,
    val subscriptionSeenCodes: JSONArray? = null,
    val subscriptionEvents: JSONArray? = null,
    val hiddenSuggestedCodes: Set<Int>? = null,
    val hiddenSuggestedEntries: List<HiddenSuggestedEntryState>? = null,
    val suggestionCategoryWeights: Map<SuggestionWeightCategory, Float>? = null,
    val dailyReadActivity: JSONArray? = null,
    val readingSessions: JSONArray? = null,
    val entryPinPriorityEnabled: Boolean? = null
)

data class HiddenSuggestedEntryState(
    val code: Int,
    val hiddenAtMillis: Long
)

class GalleryNotFoundException(message: String) : Exception(message)
open class GalleryFetchException(message: String) : Exception(message)

typealias NhentaiApiClient = com.example.saucetracker.data.remote.GalleryApi

