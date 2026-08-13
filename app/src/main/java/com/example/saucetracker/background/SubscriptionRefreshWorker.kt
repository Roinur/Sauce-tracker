package com.example.saucetracker.background

import com.example.saucetracker.core.network.TemporaryWebsiteException
import com.example.saucetracker.data.database.SauceTrackerDatabase

import com.example.saucetracker.*

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.saucetracker.app.MainActivity
import java.util.concurrent.TimeUnit

private const val SUBSCRIPTION_REFRESH_WORK_NAME = "subscription_refresh_periodic"
private const val SUBSCRIPTION_PREFS_NAME = "nhtagbook_prefs"
private const val SUBSCRIPTION_KEY_REFRESH_INTERVAL_HOURS = "subscription_refresh_interval_hours"
private const val SUBSCRIPTION_NOTIFICATION_CHANNEL_ID = "subscription_updates"
private const val SUBSCRIPTION_NOTIFICATION_ID = 12041
private const val SUBSCRIPTION_ROUTE_FETCH_PAGES = 2
internal const val EXTRA_OPEN_SUBSCRIPTIONS = "extra_open_subscriptions"

internal fun syncSubscriptionBackgroundWork(
    context: Context,
    hasSubscriptions: Boolean,
    intervalHours: Int
) {
    val appContext = context.applicationContext
    val workManager = runCatching { WorkManager.getInstance(appContext) }.getOrNull() ?: return
    if (!hasSubscriptions) {
        workManager.cancelUniqueWork(SUBSCRIPTION_REFRESH_WORK_NAME)
        return
    }
    val safeIntervalHours = intervalHours.coerceIn(1, 24).toLong()
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    val request = PeriodicWorkRequestBuilder<SubscriptionRefreshWorker>(
        safeIntervalHours,
        TimeUnit.HOURS
    )
        .setConstraints(constraints)
        .build()
    workManager.enqueueUniquePeriodicWork(
        SUBSCRIPTION_REFRESH_WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

internal fun syncSubscriptionNotificationSummaryForContext(
    appContext: Context,
    db: SauceTrackerDatabase
) {
    val notificationCount = db.countSubscriptionEventsForNotification()
    val badgeCount = db.countSubscriptionEventsForBadge()
    if (notificationCount <= 0 && badgeCount <= 0) {
        NotificationManagerCompat.from(appContext).cancel(SUBSCRIPTION_NOTIFICATION_ID)
        return
    }
    ensureSubscriptionNotificationChannel(appContext)
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    val builder = NotificationCompat.Builder(appContext, SUBSCRIPTION_NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle(
            if (notificationCount > 0) {
                "$notificationCount new subscription update${if (notificationCount == 1) "" else "s"}"
            } else {
                "Subscription updates available"
            }
        )
        .setContentText("Open Sauce Tracker to review your subscribed tags and creators.")
        .setContentIntent(
            PendingIntent.getActivity(
                appContext,
                SUBSCRIPTION_NOTIFICATION_ID,
                Intent(appContext, MainActivity::class.java).apply {
                    action = "com.example.saucetracker.OPEN_SUBSCRIPTIONS"
                    putExtra(EXTRA_OPEN_SUBSCRIPTIONS, true)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .setAutoCancel(true)
        .setOnlyAlertOnce(true)
        .setNumber(badgeCount.coerceAtLeast(0))
        .setSilent(notificationCount <= 0)
        .setPriority(
            if (notificationCount > 0) {
                NotificationCompat.PRIORITY_DEFAULT
            } else {
                NotificationCompat.PRIORITY_LOW
            }
        )
    NotificationManagerCompat.from(appContext).notify(SUBSCRIPTION_NOTIFICATION_ID, builder.build())
}

private fun ensureSubscriptionNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < 26) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
    val existing = manager.getNotificationChannel(SUBSCRIPTION_NOTIFICATION_CHANNEL_ID)
    if (existing != null) return
    val channel = NotificationChannel(
        SUBSCRIPTION_NOTIFICATION_CHANNEL_ID,
        "Subscription updates",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "Notifications for subscribed tags and creators."
        setShowBadge(true)
    }
    manager.createNotificationChannel(channel)
}

class SubscriptionRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val db = SauceTrackerDatabase(applicationContext)
        val subscriptions = db.listSubscriptions()
        if (subscriptions.isEmpty()) {
            syncSubscriptionNotificationSummaryForContext(applicationContext, db)
            return Result.success()
        }

        val suggestionApi = SuggestionApiClient()
        val client = NhentaiApiClient()
        var hadTemporaryFailure = false

        subscriptions.forEach { subscription ->
            try {
                val codes = suggestionApi.fetchDirectRouteCodes(
                    routeType = subscription.routeType,
                    routeName = subscription.routeName,
                    pages = SUBSCRIPTION_ROUTE_FETCH_PAGES
                )
                if (!subscription.initialized) {
                    if (codes.isNotEmpty()) {
                        db.addSeenCodesForSubscription(subscription.id, codes)
                    }
                    db.markSubscriptionInitialized(subscription.id)
                    return@forEach
                }

                val seenCodes = db.listSeenCodesForSubscription(subscription.id)
                val unseenCodes = codes
                    .asSequence()
                    .filter { it > 0 && it !in seenCodes }
                    .take(24)
                    .toList()
                if (unseenCodes.isEmpty()) {
                    db.markSubscriptionChecked(subscription.id)
                    return@forEach
                }

                val galleries = mutableListOf<GalleryData>()
                var detailTemporaryFailure = false
                unseenCodes.forEach { code ->
                    try {
                        galleries += client.fetchGallery(code)
                    } catch (_: TemporaryWebsiteException) {
                        detailTemporaryFailure = true
                    } catch (_: Exception) {
                        // A permanent per-gallery failure should not block the full subscription route.
                    }
                }
                if (detailTemporaryFailure) {
                    hadTemporaryFailure = true
                    return@forEach
                }
                db.insertSubscriptionEvents(subscription.id, galleries)
                db.addSeenCodesForSubscription(subscription.id, unseenCodes)
                db.markSubscriptionChecked(subscription.id)
            } catch (_: TemporaryWebsiteException) {
                hadTemporaryFailure = true
            } catch (_: Exception) {
                db.markSubscriptionChecked(subscription.id)
            }
        }

        syncSubscriptionNotificationSummaryForContext(applicationContext, db)
        return if (hadTemporaryFailure) Result.retry() else Result.success()
    }
}
