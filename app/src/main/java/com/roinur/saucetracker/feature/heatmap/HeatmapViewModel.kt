package com.roinur.saucetracker.feature.heatmap

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class HeatmapViewModel {
    var uiState by mutableStateOf(HeatmapUiState())
        private set

    fun update(transform: (HeatmapUiState) -> HeatmapUiState) {
        uiState = transform(uiState)
    }
}
