package com.roinur.saucetracker.core.security

import android.content.Context
import com.roinur.saucetracker.core.preferences.KEY_APP_LOCK_GRACE_UNTIL
import com.roinur.saucetracker.core.preferences.SaucePreferences

internal class AppLockController private constructor(
    private val preferences: SaucePreferences,
    private val pins: PinStorage,
    private val graceDurationMillis: Long
) {
    val graceUntilMillis: Long
        get() = preferences.long(KEY_APP_LOCK_GRACE_UNTIL).coerceAtLeast(0L)

    fun isConfigured(): Boolean = pins.current().configured

    fun storePin(pinInput: String): String? = pins.store(pinInput)

    fun verifyPin(pinInput: String): Boolean = pins.verify(pinInput)

    fun clearCredentials() {
        pins.clear()
        clearGrace()
    }

    fun shouldLock(enabled: Boolean, nowMillis: Long = System.currentTimeMillis()): Boolean =
        enabled && isConfigured() && nowMillis >= graceUntilMillis

    fun grantGrace(nowMillis: Long = System.currentTimeMillis()): Long =
        setGraceUntil(nowMillis + graceDurationMillis)

    fun clearGrace(): Long = setGraceUntil(0L)

    fun setGraceUntil(untilMillis: Long): Long {
        val safeValue = untilMillis.coerceAtLeast(0L)
        preferences.raw.edit().putLong(KEY_APP_LOCK_GRACE_UNTIL, safeValue).apply()
        return safeValue
    }

    companion object {
        fun from(context: Context, graceDurationMillis: Long): AppLockController = AppLockController(
            preferences = SaucePreferences.from(context),
            pins = PinStorage.from(context),
            graceDurationMillis = graceDurationMillis.coerceAtLeast(0L)
        )
    }
}
