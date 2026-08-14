package com.roinur.saucetracker.data.database.dao

import com.roinur.saucetracker.EntryHeatmapCacheRecord
import com.roinur.saucetracker.TagGraphEntryLayoutResult
import com.roinur.saucetracker.TagGraphSnapshot
import com.roinur.saucetracker.data.database.SauceTrackerDatabase

internal interface HeatmapCacheDao {
    fun clear()
    fun save(cacheKey: String, layout: TagGraphEntryLayoutResult)
    fun load(cacheKey: String, snapshot: TagGraphSnapshot): TagGraphEntryLayoutResult?
    fun record(): EntryHeatmapCacheRecord?
}

internal class SqliteHeatmapCacheDao(private val database: SauceTrackerDatabase) : HeatmapCacheDao {
    override fun clear() = database.clearEntryHeatmapCache()
    override fun save(cacheKey: String, layout: TagGraphEntryLayoutResult) =
        database.saveEntryHeatmapCache(cacheKey, layout)
    override fun load(cacheKey: String, snapshot: TagGraphSnapshot) =
        database.loadEntryHeatmapCache(cacheKey, snapshot)
    override fun record() = database.getEntryHeatmapCacheRecord()
}
