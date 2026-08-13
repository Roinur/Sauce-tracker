package com.example.saucetracker.data.repository

import com.example.saucetracker.*
import com.example.saucetracker.data.database.SauceTrackerDatabase
import com.example.saucetracker.data.database.entity.RelatedEntryEntity

internal class LibraryRepository(
    private val database: SauceTrackerDatabase
) {
    private val entries = database.entryDao
    private val tags = database.tagDao
    private val creators = database.creatorDao

    fun entries(
        textFilter: String,
        tagFilterIds: List<Long>,
        sortField: EntrySortField?,
        sortDirection: SortDirection,
        readFilter: EntryReadFilterMode,
        prioritizePinned: Boolean = true
    ): List<EntryRow> = entries.list(
        textFilter,
        tagFilterIds,
        sortField,
        sortDirection,
        readFilter,
        prioritizePinned
    )

    fun entryDetail(code: Int): EntryDetail? = entries.detail(code)
    fun entryRow(code: Int): EntryRow? {
        if (code <= 0) return null
        return entries.list(
            textFilter = "code:$code",
            tagFilterIds = emptyList(),
            sortField = null,
            sortDirection = SortDirection.DESC,
            readFilter = EntryReadFilterMode.ALL,
            prioritizePinned = false
        ).firstOrNull { it.code == code }
    }
    fun entryDetails(codes: List<Int>): List<EntryDetail> = entries.details(codes)
    fun allEntryCodes(): List<Int> = entries.allCodes()
    fun browserStates(codes: List<Int>): Map<Int, BrowserLibraryStateRow> = entries.browserStates(codes)
    fun relatedEntries(code: Int, limit: Int = 18): List<RelatedEntryEntity> =
        entries.relatedEntries(code, limit)
    fun sameArtistEntries(code: Int, limit: Int = 18): List<RelatedEntryEntity> =
        entries.sameArtistEntries(code, limit)
    fun deleteEntry(code: Int) = entries.delete(code)
    fun setEntryRating(code: Int, rating: Int) = entries.setRating(code, rating)
    fun setEntryRead(code: Int, isRead: Boolean) = entries.setRead(code, isRead)
    fun setEntryPinned(code: Int, pinned: Boolean) = entries.setPinned(code, pinned)

    fun tags(textFilter: String, sortField: TagSortField, sortDirection: SortDirection): List<TagCountRow> =
        tags.counts(textFilter, sortField, sortDirection)
    fun tagRoute(tagId: Long): TagRouteRef? = tags.route(tagId)
    fun setPopularTagBlocked(tagId: Long, blocked: Boolean) = tags.setBlocked(tagId, blocked)
    fun clearBlockedPopularTags() = tags.clearBlocked()

    fun creators(
        textFilter: String,
        tagFilterIds: List<Long>,
        sortField: CreatorSortField,
        sortDirection: SortDirection
    ): List<CreatorRow> = creators.list(textFilter, tagFilterIds, sortField, sortDirection)
    fun creatorEntries(tagId: Long, textFilter: String, tagFilterIds: List<Long>): List<CreatorEntryRow> =
        creators.entries(tagId, textFilter, tagFilterIds)
    fun addCreator(name: String, creatorType: String, sourceUrl: String): Boolean =
        creators.add(name, creatorType, sourceUrl)

    fun upsertGallery(gallery: GalleryData): Boolean = database.upsertGallery(gallery)
}
