package com.roinur.saucetracker.feature.subscriptions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.roinur.saucetracker.SubscriptionEventRow
import com.roinur.saucetracker.SubscriptionRow

internal data class SubscriptionUiState(
    val subscriptions: List<SubscriptionRow> = emptyList(),
    val events: List<SubscriptionEventRow> = emptyList(),
    val refreshRunning: Boolean = false
)

internal class SubscriptionsViewModel {
    var uiState by mutableStateOf(SubscriptionUiState())
        private set

    fun update(transform: (SubscriptionUiState) -> SubscriptionUiState) {
        uiState = transform(uiState)
    }
}
