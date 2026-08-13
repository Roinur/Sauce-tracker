package com.example.saucetracker.data.database.dao

import com.example.saucetracker.CreatorEntryRow
import com.example.saucetracker.CreatorRow
import com.example.saucetracker.CreatorSortField
import com.example.saucetracker.SortDirection
import com.example.saucetracker.data.database.SauceTrackerDatabase

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
