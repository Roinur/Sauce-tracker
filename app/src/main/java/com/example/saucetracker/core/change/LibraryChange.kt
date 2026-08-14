package com.example.saucetracker.core.change

/** A logical library mutation and the smallest set of projections it invalidates. */
data class LibraryChange(
    val reason: LibraryChangeReason,
    val impacts: Set<LibraryChangeImpact>,
    val selectCode: Int? = null
) {
    companion object {
        fun fullRefresh(reason: LibraryChangeReason, selectCode: Int? = null) = LibraryChange(
            reason = reason,
            impacts = LibraryChangeImpact.entriesAndDerivedData,
            selectCode = selectCode
        )

        fun entryContentChanged(reason: LibraryChangeReason, code: Int) = LibraryChange(
            reason = reason,
            impacts = setOf(
                LibraryChangeImpact.ENTRIES,
                LibraryChangeImpact.TAGS,
                LibraryChangeImpact.RELATED_ENTRIES,
                LibraryChangeImpact.READ_ANALYTICS
            ),
            selectCode = code
        )

        fun pinChanged(code: Int) = LibraryChange(
            reason = LibraryChangeReason.PIN_CHANGED,
            impacts = setOf(LibraryChangeImpact.ENTRIES),
            selectCode = code
        )

        fun tagFilterChanged() = LibraryChange(
            reason = LibraryChangeReason.TAG_FILTER_CHANGED,
            impacts = setOf(
                LibraryChangeImpact.ENTRIES,
                LibraryChangeImpact.TAGS,
                LibraryChangeImpact.CREATORS
            )
        )

        fun tagFilterSuggestionsChanged() = LibraryChange(
            reason = LibraryChangeReason.TAG_FILTER_CHANGED,
            impacts = setOf(LibraryChangeImpact.SUGGESTIONS_REFRESH)
        )
    }
}

enum class LibraryChangeReason {
    ENTRY_IMPORTED,
    ENTRY_UPDATED,
    ENTRY_DELETED,
    LIBRARY_CLEARED,
    LIBRARY_RESTORED,
    RATING_CHANGED,
    READ_STATE_CHANGED,
    PIN_CHANGED,
    RATING_HISTORY_CHANGED,
    TAG_FILTER_CHANGED,
    CREATOR_CHANGED,
    BATCH_CHANGED,
    FALLBACK_REFRESH
}

enum class LibraryChangeImpact {
    ENTRIES,
    TAGS,
    CREATORS,
    SAVED_STATS,
    SUGGESTIONS_EXCLUDE_IMPORTED,
    SUGGESTIONS_REFRESH,
    READ_ANALYTICS,
    READ_ANALYTICS_REFRESH,
    TAG_GRAPH,
    ENTRY_HEATMAP,
    RELATED_ENTRIES;

    companion object {
        val entriesAndDerivedData: Set<LibraryChangeImpact> = entriesAndDerivedData(
            refreshSuggestions = false
        )

        fun entriesAndDerivedData(refreshSuggestions: Boolean): Set<LibraryChangeImpact> = buildSet {
            add(ENTRIES)
            add(TAGS)
            add(CREATORS)
            add(SAVED_STATS)
            add(SUGGESTIONS_EXCLUDE_IMPORTED)
            add(READ_ANALYTICS)
            add(TAG_GRAPH)
            add(ENTRY_HEATMAP)
            add(RELATED_ENTRIES)
            if (refreshSuggestions) add(SUGGESTIONS_REFRESH)
        }
    }
}

data class LibraryChangeBatch(
    val reasons: Set<LibraryChangeReason>,
    val impacts: Set<LibraryChangeImpact>,
    val selectCode: Int?
)

/**
 * Combines mutations that happen in the same interaction burst. A single drain therefore causes
 * at most one reload of each affected projection, even when several tags or fields changed.
 */
class LibraryChangeAccumulator {
    private val reasons = linkedSetOf<LibraryChangeReason>()
    private val impacts = linkedSetOf<LibraryChangeImpact>()
    private var selectCode: Int? = null

    val hasPendingChanges: Boolean
        get() = impacts.isNotEmpty()

    fun record(change: LibraryChange) {
        reasons += change.reason
        impacts += change.impacts
        if (change.selectCode != null) selectCode = change.selectCode
    }

    fun drain(): LibraryChangeBatch? {
        if (!hasPendingChanges) return null
        return LibraryChangeBatch(
            reasons = reasons.toSet(),
            impacts = impacts.toSet(),
            selectCode = selectCode
        ).also {
            reasons.clear()
            impacts.clear()
            selectCode = null
        }
    }
}
