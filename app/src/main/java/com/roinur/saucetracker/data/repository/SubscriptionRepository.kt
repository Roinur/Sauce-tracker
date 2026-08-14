package com.roinur.saucetracker.data.repository

import com.roinur.saucetracker.GalleryData
import com.roinur.saucetracker.SubscriptionEventRow
import com.roinur.saucetracker.SubscriptionRow
import com.roinur.saucetracker.data.database.SauceTrackerDatabase

internal class SubscriptionRepository(database: SauceTrackerDatabase) {
    private val subscriptions = database.subscriptionDao

    fun list(): List<SubscriptionRow> = subscriptions.list()
    fun find(routeType: String, routeName: String): SubscriptionRow? = subscriptions.find(routeType, routeName)
    fun upsert(routeType: String, routeName: String): SubscriptionRow? = subscriptions.upsert(routeType, routeName)
    fun remove(id: Long) = subscriptions.remove(id)
    fun updateSettings(
        subscriptionId: Long,
        notificationsEnabled: Boolean,
        notificationDotEnabled: Boolean
    ) = subscriptions.updateSettings(subscriptionId, notificationsEnabled, notificationDotEnabled)
    fun markInitialized(id: Long) = subscriptions.markInitialized(id)
    fun markChecked(id: Long) = subscriptions.markChecked(id)
    fun seenCodes(id: Long): Set<Int> = subscriptions.seenCodes(id)
    fun addSeenCodes(id: Long, codes: Collection<Int>) = subscriptions.addSeenCodes(id, codes)
    fun insertEvents(id: Long, galleries: List<GalleryData>): Int = subscriptions.insertEvents(id, galleries)
    fun events(includeDismissed: Boolean = false): List<SubscriptionEventRow> = subscriptions.events(includeDismissed)
    fun dismissEvent(id: Long) = subscriptions.dismissEvent(id)
    fun toggleEventPinned(id: Long) = subscriptions.toggleEventPinned(id)
}
