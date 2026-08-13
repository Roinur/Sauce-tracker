package com.example.saucetracker.feature.subscriptions

import com.example.saucetracker.GalleryData
import com.example.saucetracker.NhentaiApiClient
import com.example.saucetracker.SubscriptionRow
import com.example.saucetracker.data.repository.SubscriptionRepository
import com.example.saucetracker.data.repository.SuggestionsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class SubscriptionSyncUseCase(
    private val subscriptions: SubscriptionRepository,
    private val suggestions: SuggestionsRepository,
    private val galleryClient: NhentaiApiClient,
    private val routeFetchPages: Int = 2,
    private val maximumNewCodes: Int = 24
) {
    suspend fun initialize(subscription: SubscriptionRow) {
        val codes = withContext(Dispatchers.IO) {
            suggestions.routeCodes(subscription.routeType, subscription.routeName, routeFetchPages)
        }
        withContext(Dispatchers.IO) {
            if (codes.isNotEmpty()) subscriptions.addSeenCodes(subscription.id, codes)
            subscriptions.markInitialized(subscription.id)
        }
    }

    suspend fun refresh(subscription: SubscriptionRow): Int {
        val codes = withContext(Dispatchers.IO) {
            suggestions.routeCodes(subscription.routeType, subscription.routeName, routeFetchPages)
        }
        if (!subscription.initialized) {
            withContext(Dispatchers.IO) {
                if (codes.isNotEmpty()) subscriptions.addSeenCodes(subscription.id, codes)
                subscriptions.markInitialized(subscription.id)
            }
            return 0
        }

        val seenCodes = withContext(Dispatchers.IO) { subscriptions.seenCodes(subscription.id) }
        val unseenCodes = codes.asSequence()
            .filter { it > 0 && it !in seenCodes }
            .take(maximumNewCodes)
            .toList()
        if (unseenCodes.isEmpty()) {
            withContext(Dispatchers.IO) { subscriptions.markChecked(subscription.id) }
            return 0
        }

        val galleries = mutableListOf<GalleryData>()
        unseenCodes.forEach { code ->
            val fetched = withContext(Dispatchers.IO) {
                runCatching { galleryClient.fetchGallery(code) }.getOrNull()
            }
            if (fetched != null) galleries += fetched
        }
        return withContext(Dispatchers.IO) {
            val inserted = subscriptions.insertEvents(subscription.id, galleries)
            subscriptions.addSeenCodes(subscription.id, unseenCodes)
            subscriptions.markChecked(subscription.id)
            inserted
        }
    }
}
