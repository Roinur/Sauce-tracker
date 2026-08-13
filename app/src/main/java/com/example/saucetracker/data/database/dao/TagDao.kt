package com.example.saucetracker.data.database.dao

import com.example.saucetracker.PopularTagRow
import com.example.saucetracker.PopularTagSeed
import com.example.saucetracker.SortDirection
import com.example.saucetracker.TagCountRow
import com.example.saucetracker.TagRouteRef
import com.example.saucetracker.TagSortField
import com.example.saucetracker.data.database.SauceTrackerDatabase

internal interface TagDao {
    fun counts(textFilter: String, sortField: TagSortField, sortDirection: SortDirection): List<TagCountRow>
    fun popular(sortField: TagSortField, sortDirection: SortDirection): List<PopularTagRow>
    fun replacePopular(rows: List<PopularTagSeed>)
    fun route(tagId: Long): TagRouteRef?
    fun setBlocked(tagId: Long, blocked: Boolean)
    fun clearBlocked()
}

internal class SqliteTagDao(private val database: SauceTrackerDatabase) : TagDao {
    override fun counts(textFilter: String, sortField: TagSortField, sortDirection: SortDirection) =
        database.listTagCounts(textFilter, sortField, sortDirection)
    override fun popular(sortField: TagSortField, sortDirection: SortDirection) =
        database.listPopularTags(sortField, sortDirection)
    override fun replacePopular(rows: List<PopularTagSeed>) = database.replacePopularTags(rows)
    override fun route(tagId: Long): TagRouteRef? = database.getTagRouteRef(tagId)
    override fun setBlocked(tagId: Long, blocked: Boolean) = database.setPopularTagBlocked(tagId, blocked)
    override fun clearBlocked() = database.clearAllBlockedPopularTags()
}
