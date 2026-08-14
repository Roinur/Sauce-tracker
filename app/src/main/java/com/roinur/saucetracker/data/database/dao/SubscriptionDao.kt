package com.roinur.saucetracker.data.database.dao

import com.roinur.saucetracker.GalleryData
import com.roinur.saucetracker.SubscriptionEventRow
import com.roinur.saucetracker.SubscriptionRow
import com.roinur.saucetracker.data.database.SauceTrackerDatabase

internal interface SubscriptionDao {
    fun list(): List<SubscriptionRow>
    fun find(routeType: String, routeName: String): SubscriptionRow?
    fun upsert(routeType: String, routeName: String): SubscriptionRow?
    fun remove(id: Long)
    fun updateSettings(id: Long, notificationsEnabled: Boolean, notificationDotEnabled: Boolean)
    fun markInitialized(id: Long)
    fun markChecked(id: Long)
    fun seenCodes(id: Long): Set<Int>
    fun addSeenCodes(id: Long, codes: Collection<Int>)
    fun insertEvents(id: Long, galleries: List<GalleryData>): Int
    fun events(includeDismissed: Boolean = false): List<SubscriptionEventRow>
    fun dismissEvent(id: Long)
    fun toggleEventPinned(id: Long)
}

internal class SqliteSubscriptionDao(private val database: SauceTrackerDatabase) : SubscriptionDao {
    override fun list() = database.listSubscriptions()
    override fun find(routeType: String, routeName: String) = database.findSubscription(routeType, routeName)
    override fun upsert(routeType: String, routeName: String) = database.upsertSubscription(routeType, routeName)
    override fun remove(id: Long) = database.removeSubscription(id)
    override fun updateSettings(id: Long, notificationsEnabled: Boolean, notificationDotEnabled: Boolean) =
        database.updateSubscriptionSettings(id, notificationsEnabled, notificationDotEnabled)
    override fun markInitialized(id: Long) = database.markSubscriptionInitialized(id)
    override fun markChecked(id: Long) = database.markSubscriptionChecked(id)
    override fun seenCodes(id: Long) = database.listSeenCodesForSubscription(id)
    override fun addSeenCodes(id: Long, codes: Collection<Int>) = database.addSeenCodesForSubscription(id, codes)
    override fun insertEvents(id: Long, galleries: List<GalleryData>) = database.insertSubscriptionEvents(id, galleries)
    override fun events(includeDismissed: Boolean) = database.listSubscriptionEvents(includeDismissed)
    override fun dismissEvent(id: Long) = database.dismissSubscriptionEvent(id)
    override fun toggleEventPinned(id: Long) = database.toggleSubscriptionEventPinned(id)
}
