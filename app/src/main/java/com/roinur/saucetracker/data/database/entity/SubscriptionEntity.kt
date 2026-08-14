package com.roinur.saucetracker.data.database.entity

data class SubscriptionEntity(
    val id: Long,
    val routeName: String,
    val routeType: String,
    val notificationsEnabled: Boolean,
    val notificationDotEnabled: Boolean,
    val initialized: Boolean,
    val createdAt: String,
    val lastCheckedAt: String
)

