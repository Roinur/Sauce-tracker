package com.roinur.saucetracker.data.repository

import com.roinur.saucetracker.EntryHeatmapCacheRecord
import com.roinur.saucetracker.TagGraphDataSnapshot
import com.roinur.saucetracker.TagGraphEntryLayoutResult
import com.roinur.saucetracker.TagGraphSnapshot
import com.roinur.saucetracker.data.database.SauceTrackerDatabase

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
