package com.roinur.saucetracker.core.preferences

import android.content.Context
import android.content.SharedPreferences
import com.roinur.saucetracker.core.diagnostics.GitHubMediaSession

internal class SaucePreferences private constructor(
    val raw: SharedPreferences
) {
    fun string(key: String, fallback: String = ""): String =
        raw.getString(key, fallback).orEmpty()

    fun boolean(key: String, fallback: Boolean = false): Boolean =
        raw.getBoolean(key, fallback)

    fun integer(key: String, fallback: Int = 0): Int =
        raw.getInt(key, fallback)

    fun long(key: String, fallback: Long = 0L): Long =
        raw.getLong(key, fallback)

    companion object {
        fun from(context: Context): SaucePreferences {
            val appContext = context.applicationContext
            return SaucePreferences(
                appContext.getSharedPreferences(
                    GitHubMediaSession.preferencesName(SAUCE_PREFERENCES_NAME),
                    Context.MODE_PRIVATE
                )
            )
        }
    }
}
