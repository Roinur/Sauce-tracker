package com.roinur.saucetracker.app

import com.roinur.saucetracker.background.EXTRA_OPEN_SUBSCRIPTIONS
import com.roinur.saucetracker.background.EXTRA_SUBSCRIPTION_PROFILE_ID

import android.content.Intent

internal class AppNavigator(
    private val activateProfile: (String) -> Unit,
    private val openSubscriptions: () -> Unit
) {
    fun route(intent: Intent?) {
        val incoming = intent ?: return
        if (!incoming.getBooleanExtra(EXTRA_OPEN_SUBSCRIPTIONS, false)) return
        incoming.removeExtra(EXTRA_OPEN_SUBSCRIPTIONS)
        incoming.getStringExtra(EXTRA_SUBSCRIPTION_PROFILE_ID)?.takeIf(String::isNotBlank)?.let(activateProfile)
        incoming.removeExtra(EXTRA_SUBSCRIPTION_PROFILE_ID)
        openSubscriptions()
    }
}
