package com.roinur.saucetracker.data.database.dao

import com.roinur.saucetracker.PopularTagRow
import com.roinur.saucetracker.PopularTagSeed
import com.roinur.saucetracker.SortDirection
import com.roinur.saucetracker.TagCountRow
import com.roinur.saucetracker.TagRouteRef
import com.roinur.saucetracker.TagSortField
import com.roinur.saucetracker.data.database.SauceTrackerDatabase

internal interface TagDao {
    fun counts(
        textFilter: String,
        sortField: TagSortField,
        sortDirection: SortDirection,
        visibleEntryCodes: Collection<Int>? = null
    ): List<TagCountRow>
    fun popular(sortField: TagSortField, sortDirection: SortDirection): List<PopularTagRow>
    fun replacePopular(rows: List<PopularTagSeed>)
    fun route(tagId: Long): TagRouteRef?
    fun setBlocked(tagId: Long, blocked: Boolean)
    fun clearBlocked()
}

internal class SqliteTagDao(private val database: SauceTrackerDatabase) : TagDao {
    override fun counts(
        textFilter: String,
        sortField: TagSortField,
        sortDirection: SortDirection,
        visibleEntryCodes: Collection<Int>?
    ) = database.listTagCounts(textFilter, sortField, sortDirection, visibleEntryCodes)
    override fun popular(sortField: TagSortField, sortDirection: SortDirection) =
        database.listPopularTags(sortField, sortDirection)
    override fun replacePopular(rows: List<PopularTagSeed>) = database.replacePopularTags(rows)
    override fun route(tagId: Long): TagRouteRef? = database.getTagRouteRef(tagId)
    override fun setBlocked(tagId: Long, blocked: Boolean) = database.setPopularTagBlocked(tagId, blocked)
    override fun clearBlocked() = database.clearAllBlockedPopularTags()
}
