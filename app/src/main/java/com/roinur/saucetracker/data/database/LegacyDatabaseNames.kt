package com.roinur.saucetracker.data.database

internal object LegacyDatabaseNames {
    // Never change this value without a real SQLite migration: all 1.6 installs use it.
    const val PRIMARY = "tagbook.db"
}
