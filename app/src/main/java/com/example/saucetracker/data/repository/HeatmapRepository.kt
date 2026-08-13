package com.example.saucetracker.data.repository

import com.example.saucetracker.EntryHeatmapCacheRecord
import com.example.saucetracker.TagGraphDataSnapshot
import com.example.saucetracker.TagGraphEntryLayoutResult
import com.example.saucetracker.TagGraphSnapshot
import com.example.saucetracker.data.database.SauceTrackerDatabase

internal class HeatmapRepository(
    private val database: SauceTrackerDatabase
) {
    private val cache = database.heatmapCacheDao

    fun graphData(): TagGraphDataSnapshot = database.getTagGraphDataSnapshot()
    fun cacheRecord(): EntryHeatmapCacheRecord? = cache.record()
    fun load(cacheKey: String, snapshot: TagGraphSnapshot): TagGraphEntryLayoutResult? =
        cache.load(cacheKey, snapshot)
    fun save(cacheKey: String, layout: TagGraphEntryLayoutResult) = cache.save(cacheKey, layout)
    fun clear() = cache.clear()
}
