package com.roinur.saucetracker.data.database.dao

import com.roinur.saucetracker.CreatorEntryRow
import com.roinur.saucetracker.CreatorRow
import com.roinur.saucetracker.CreatorSortField
import com.roinur.saucetracker.SortDirection
import com.roinur.saucetracker.data.database.SauceTrackerDatabase

internal interface CreatorDao {
    fun add(name: String, type: String, sourceUrl: String): Boolean
    fun list(
        textFilter: String,
        tagFilterIds: List<Long>,
        sortField: CreatorSortField,
        sortDirection: SortDirection
    ): List<CreatorRow>
    fun entries(creatorId: Long, textFilter: String, tagFilterIds: List<Long>): List<CreatorEntryRow>
}

internal class SqliteCreatorDao(private val database: SauceTrackerDatabase) : CreatorDao {
    override fun add(name: String, type: String, sourceUrl: String) = database.addCreator(name, type, sourceUrl)
    override fun list(
        textFilter: String,
        tagFilterIds: List<Long>,
        sortField: CreatorSortField,
        sortDirection: SortDirection
    ) = database.listCreators(textFilter, tagFilterIds, sortField, sortDirection)
    override fun entries(creatorId: Long, textFilter: String, tagFilterIds: List<Long>) =
        database.listEntriesForCreator(creatorId, textFilter, tagFilterIds)
}
