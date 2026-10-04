package com.roinur.saucetracker.core.diagnostics

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import com.roinur.saucetracker.HomeSurface
import com.roinur.saucetracker.ThemeMode
import com.roinur.saucetracker.core.preferences.KEY_INCOGNITO_MODE_ENABLED
import com.roinur.saucetracker.core.preferences.KEY_THEME_MODE
import org.json.JSONObject

internal object GitHubMediaSession {
    const val EXTRA_CONFIG_BASE64 = "github_media_config_b64"
    const val DATABASE_NAME = "sauce_tracker_github_media.db"
    const val LAUNCHER_COMPONENT_CLASS = "com.roinur.saucetracker.app.GitHubMediaLauncher"
    private const val MEDIA_PREFERENCES_SUFFIX = "_github_media"
    private const val MAX_ENCODED_CONFIG = 64 * 1024

    @Volatile
    var active: Boolean = false
        private set
    @Volatile
    var themeOverride: ThemeMode? = null
        private set
    @Volatile
    var privacyMaskEnabled: Boolean = false
        private set
    @Volatile
    var initialIncognitoEnabled: Boolean = false
        private set
    @Volatile
    var initialSurface: HomeSurface = HomeSurface.DASHBOARD
        private set

    private var databasePopulated = false

    fun activateIfRequested(context: Context, intent: Intent?): Boolean {
        val encoded = intent?.getStringExtra(EXTRA_CONFIG_BASE64) ?: return false
        require(encoded.length <= MAX_ENCODED_CONFIG) { "GitHub media config is too large." }
        val raw = String(Base64.decode(encoded, Base64.NO_WRAP), Charsets.UTF_8)
        val json = JSONObject(raw)
        themeOverride = when (json.optString("theme", "system").lowercase()) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
        require(json.optBoolean("privacyMask", true)) {
            "GitHub media mode requires privacyMask=true."
        }
        privacyMaskEnabled = true
        initialIncognitoEnabled = json.optBoolean("incognito", false)
        initialSurface = when (json.optString("surface", "dashboard").lowercase()) {
            "entries" -> HomeSurface.ENTRIES
            "tags" -> HomeSurface.TAGS
            "suggestions" -> HomeSurface.SUGGESTED
            "subscriptions" -> HomeSurface.SUBSCRIPTIONS
            "creators" -> HomeSurface.CREATORS
            "heatmap" -> HomeSurface.HEATMAP
            "history" -> HomeSurface.HISTORY
            else -> HomeSurface.DASHBOARD
        }
        active = true
        databasePopulated = false
        copyPreferencesForSession(context, "nhtagbook_prefs")
        context.deleteDatabase(DATABASE_NAME)
        return true
    }

    fun databaseName(): String = if (active) DATABASE_NAME else "tagbook.db"

    fun preferencesName(productionName: String): String =
        if (active) productionName + MEDIA_PREFERENCES_SUFFIX else productionName

    fun shouldApplyPrivacyMask(requestedByIncognito: Boolean): Boolean =
        requestedByIncognito || active

    fun shouldStrengthenPrivacyMask(): Boolean = active

    @Synchronized
    fun deactivate() {
        active = false
        themeOverride = null
        privacyMaskEnabled = false
        initialIncognitoEnabled = false
        initialSurface = HomeSurface.DASHBOARD
        databasePopulated = false
    }

    fun encodedLaunchConfig(
        themeMode: ThemeMode,
        surface: HomeSurface = HomeSurface.DASHBOARD,
        incognitoEnabled: Boolean = false
    ): String {
        val config = JSONObject()
            .put("surface", surface.name.lowercase())
            .put("theme", themeMode.name.lowercase())
            .put("privacyMask", true)
            .put("incognito", incognitoEnabled)
            .toString()
        return Base64.encodeToString(config.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private fun copyPreferencesForSession(context: Context, productionName: String) {
        val source = context.getSharedPreferences(productionName, Context.MODE_PRIVATE)
        val target = context.getSharedPreferences(
            productionName + MEDIA_PREFERENCES_SUFFIX,
            Context.MODE_PRIVATE
        )
        val editor = target.edit().clear()
        source.all.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Float -> editor.putFloat(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is String -> editor.putString(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }
        editor.putBoolean(KEY_INCOGNITO_MODE_ENABLED, initialIncognitoEnabled)
        themeOverride?.let { editor.putString(KEY_THEME_MODE, it.name) }
        editor.apply()
    }

    @Synchronized
    fun populateFromProductionIfNeeded(context: Context, database: SQLiteDatabase) {
        if (!active || databasePopulated) return
        val production = context.getDatabasePath("tagbook.db")
        if (!production.isFile) {
            databasePopulated = true
            return
        }

        val quotedPath = production.absolutePath.replace("'", "''")
        database.execSQL("ATTACH DATABASE '$quotedPath' AS production")
        try {
            val sourceTables = mutableSetOf<String>()
            database.rawQuery(
                "SELECT name FROM production.sqlite_master WHERE type = 'table'",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) sourceTables += cursor.getString(0)
            }
            val destinationTables = mutableListOf<String>()
            database.rawQuery(
                "SELECT name FROM main.sqlite_master WHERE type = 'table' ORDER BY rowid",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) destinationTables += cursor.getString(0)
            }
            val copiedTables = destinationTables.filterNot {
                it == "android_metadata" || it.startsWith("sqlite_")
            }
            val parents = copiedTables.associateWith { table ->
                val identifier = table.replace("\"", "\"\"")
                database.rawQuery("PRAGMA main.foreign_key_list(\"$identifier\")", null).use { cursor ->
                    buildSet {
                        val parentColumn = cursor.getColumnIndexOrThrow("table")
                        while (cursor.moveToNext()) add(cursor.getString(parentColumn))
                    }
                }
            }
            val copyOrder = DatabaseCopyOrder.parentFirst(parents)
            database.beginTransaction()
            try {
                // Newly created media databases contain default source/profile rows.
                // REPLACE would delete those parents while RESTRICT children still exist.
                // Only clear the isolated destination, then insert parents before children.
                copyOrder.asReversed().forEach { table ->
                    val identifier = table.replace("\"", "\"\"")
                    database.execSQL("DELETE FROM main.\"$identifier\"")
                }
                copyOrder
                    .filter { it in sourceTables }
                    .forEach { table ->
                        val identifier = table.replace("\"", "\"\"")
                        database.execSQL(
                            "INSERT INTO main.\"$identifier\" SELECT * FROM production.\"$identifier\""
                        )
                    }
                database.rawQuery("PRAGMA main.foreign_key_check", null).use { cursor ->
                    check(!cursor.moveToFirst()) { "Invalid relations in the isolated media database." }
                }
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
            databasePopulated = true
        } finally {
            database.execSQL("DETACH DATABASE production")
        }
    }
}
