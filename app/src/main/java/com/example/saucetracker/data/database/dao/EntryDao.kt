package com.example.saucetracker.data.database.dao

import com.example.saucetracker.BrowserLibraryStateRow
import com.example.saucetracker.EntryDetail
import com.example.saucetracker.EntryReadFilterMode
import com.example.saucetracker.EntryRow
import com.example.saucetracker.EntrySortField
import com.example.saucetracker.SortDirection
import com.example.saucetracker.data.database.SauceTrackerDatabase
import com.example.saucetracker.data.database.entity.RelatedEntryEntity

internal interface EntryDao {
    fun list(
        textFilter: String,
        tagFilterIds: List<Long>,
        sortField: EntrySortField?,
        sortDirection: SortDirection,
        readFilter: EntryReadFilterMode,
        prioritizePinned: Boolean = true
    ): List<EntryRow>

    fun detail(code: Int): EntryDetail?
    fun details(codes: List<Int>): List<EntryDetail>
    fun allCodes(): List<Int>
    fun browserStates(codes: List<Int>): Map<Int, BrowserLibraryStateRow>
    fun relatedEntries(code: Int, limit: Int): List<RelatedEntryEntity>
    fun sameArtistEntries(code: Int, limit: Int): List<RelatedEntryEntity>
    fun delete(code: Int)
    fun setRating(code: Int, rating: Int)
    fun setRead(code: Int, isRead: Boolean)
    fun setPinned(code: Int, pinned: Boolean)
}

internal class SqliteEntryDao(
    private val database: SauceTrackerDatabase
) : EntryDao {
    override fun list(
        textFilter: String,
        tagFilterIds: List<Long>,
        sortField: EntrySortField?,
        sortDirection: SortDirection,
        readFilter: EntryReadFilterMode,
        prioritizePinned: Boolean
    ): List<EntryRow> = database.listEntries(
        textFilter,
        tagFilterIds,
        sortField,
        sortDirection,
        readFilter,
        prioritizePinned
    )

    override fun detail(code: Int): EntryDetail? = database.getEntryDetail(code)
    override fun details(codes: List<Int>): List<EntryDetail> = database.getEntryDetails(codes)
    override fun allCodes(): List<Int> = database.listAllEntryCodes()
    override fun browserStates(codes: List<Int>): Map<Int, BrowserLibraryStateRow> =
        database.getBrowserLibraryStates(codes)
    override fun relatedEntries(code: Int, limit: Int): List<RelatedEntryEntity> =
        database.listRelatedEntryPreviews(code, limit)
    override fun sameArtistEntries(code: Int, limit: Int): List<RelatedEntryEntity> =
        database.listSameArtistEntryPreviews(code, limit)
    override fun delete(code: Int) = database.deleteEntry(code)
    override fun setRating(code: Int, rating: Int) = database.setEntryRating(code, rating)
    override fun setRead(code: Int, isRead: Boolean) = database.setEntryRead(code, isRead)
    override fun setPinned(code: Int, pinned: Boolean) = database.setEntryPinned(code, pinned)
}
