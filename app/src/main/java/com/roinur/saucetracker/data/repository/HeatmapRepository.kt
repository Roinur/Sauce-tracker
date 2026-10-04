package com.roinur.saucetracker.data.repository

import com.roinur.saucetracker.EntryHeatmapCacheRecord
import com.roinur.saucetracker.TagGraphDataSnapshot
import com.roinur.saucetracker.TagGraphEntryLayoutResult
import com.roinur.saucetracker.TagGraphSnapshot
import com.roinur.saucetracker.data.database.SauceTrackerDatabase
import com.roinur.saucetracker.feature.heatmap.TrendRequest
import com.roinur.saucetracker.feature.heatmap.TrendSnapshot
import com.roinur.saucetracker.feature.heatmap.TrendTarget
import com.roinur.saucetracker.feature.heatmap.TrendTargetKind

internal class HeatmapRepository(
    private val database: SauceTrackerDatabase
) {
    private val cache = database.heatmapCacheDao

    fun graphData(profileId: String, sourceScope: Set<String>): TagGraphDataSnapshot =
        database.getTagGraphDataSnapshot(profileId, sourceScope)
    fun trendTargets(kind: TrendTargetKind, includeMisc: Boolean, profileId: String, sourceScope: Set<String>): List<TrendTarget> =
        database.listTrendTargets(kind, includeMisc, profileId, sourceScope)
    fun trendSnapshot(request: TrendRequest, profileId: String, sourceScope: Set<String>): TrendSnapshot = database.getTrendSnapshot(request, profileId, sourceScope)
    fun cacheRecord(): EntryHeatmapCacheRecord? = cache.record()
    fun load(cacheKey: String, snapshot: TagGraphSnapshot): TagGraphEntryLayoutResult? =
        cache.load(cacheKey, snapshot)
    fun save(cacheKey: String, layout: TagGraphEntryLayoutResult) = cache.save(cacheKey, layout)
    fun clear() = cache.clear()
}
