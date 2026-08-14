package com.roinur.saucetracker.app

import com.roinur.saucetracker.background.EXTRA_OPEN_SUBSCRIPTIONS

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
