package com.example.saucetracker.data.database.dao

import com.example.saucetracker.EntryHeatmapCacheRecord
import com.example.saucetracker.TagGraphEntryLayoutResult
import com.example.saucetracker.TagGraphSnapshot
import com.example.saucetracker.data.database.SauceTrackerDatabase

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
