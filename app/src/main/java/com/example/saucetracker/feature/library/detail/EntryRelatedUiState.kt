package com.example.saucetracker.feature.library.detail

import com.example.saucetracker.SeriesNeighbors
import com.example.saucetracker.EntryReadFilterMode
import com.example.saucetracker.data.database.entity.RelatedEntryEntity

internal enum class RelatedEntryMode(val label: String) {
    PARTS("Parts"),
    MORE_LIKE_THIS("More like this"),
    SAME_ARTIST("Same artist")
}

internal data class SelectedEntryRelatedUiState(
    val code: Int? = null,
    val loading: Boolean = false,
    val moreLikeThis: List<RelatedEntryEntity> = emptyList(),
    val sameArtist: List<RelatedEntryEntity> = emptyList()
)

internal fun availableRelatedEntryModes(
    seriesNeighbors: SeriesNeighbors,
    state: SelectedEntryRelatedUiState
): List<RelatedEntryMode> = buildList {
    if (seriesNeighbors.parts.size > 1) add(RelatedEntryMode.PARTS)
    if (state.moreLikeThis.isNotEmpty()) add(RelatedEntryMode.MORE_LIKE_THIS)
    if (state.sameArtist.isNotEmpty()) add(RelatedEntryMode.SAME_ARTIST)
}

internal fun resolvedRelatedEntryMode(
    requested: RelatedEntryMode?,
    available: List<RelatedEntryMode>
): RelatedEntryMode? = requested?.takeIf { it in available } ?: available.firstOrNull()

internal fun showReadRelatedEntries(readFilter: EntryReadFilterMode): Boolean =
    readFilter == EntryReadFilterMode.READ

internal fun filterRelatedEntriesByReadState(
    entries: List<RelatedEntryEntity>,
    readStateByCode: Map<Int, Boolean>,
    showReadEntries: Boolean,
    limit: Int = 18
): List<RelatedEntryEntity> = entries
    .asSequence()
    .filter { entry -> readStateByCode[entry.code] == showReadEntries }
    .take(limit.coerceAtLeast(0))
    .toList()
