package com.roinur.saucetracker.feature.heatmap

import com.roinur.saucetracker.EntryHeatmapRecalculationSummary
import com.roinur.saucetracker.TagGraphSnapshot

data class HeatmapUiState(
    val snapshot: TagGraphSnapshot? = null,
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val recalculationRunning: Boolean = false,
    val cacheStatusText: String = "Entry heatmap cache: Not calculated",
    val progressLabel: String = "Preparing entry heatmap recalculation...",
    val progressFraction: Float? = null,
    val completionSummary: EntryHeatmapRecalculationSummary? = null,
    val cacheNonce: Long = 0L
)
