package com.example.saucetracker.app

import com.example.saucetracker.background.EXTRA_OPEN_SUBSCRIPTIONS

import android.content.Intent

internal class AppNavigator(
    private val openSubscriptions: () -> Unit
) {
    fun route(intent: Intent?) {
        val incoming = intent ?: return
        if (!incoming.getBooleanExtra(EXTRA_OPEN_SUBSCRIPTIONS, false)) return
        incoming.removeExtra(EXTRA_OPEN_SUBSCRIPTIONS)
        openSubscriptions()
    }
}
