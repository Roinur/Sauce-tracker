package com.roinur.saucetracker.feature.saucefinder

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.roinur.saucetracker.core.diagnostics.GitHubMediaSession

internal class SauceFinderIndexStore(context: Context) :
    SQLiteOpenHelper(
        context.applicationContext,
        if (GitHubMediaSession.active) GITHUB_DATABASE_NAME else DATABASE_NAME,
        null,
        DATABASE_VERSION
    ) {
    private val appContext = context.applicationContext
    private val databaseName = if (GitHubMediaSession.active) GITHUB_DATABASE_NAME else DATABASE_NAME

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE sauce_image_hashes (
                entry_code INTEGER NOT NULL,
                page_number INTEGER NOT NULL,
                source TEXT NOT NULL,
                hash_0 INTEGER NOT NULL,
                hash_1 INTEGER NOT NULL,
                hash_2 INTEGER NOT NULL,
                hash_3 INTEGER NOT NULL,
                hash_4 INTEGER NOT NULL,
                indexed_at INTEGER NOT NULL,
                PRIMARY KEY(entry_code, page_number)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX sauce_image_hash_entry_idx ON sauce_image_hashes(entry_code)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS sauce_image_hashes")
        onCreate(db)
    }

    @Synchronized
    fun put(record: SauceFinderIndexRecord) {
        val normalized = LongArray(HASH_COUNT) { index ->
            record.fingerprint.hashes.getOrElse(index) { record.fingerprint.hashes.first() }
        }
        writableDatabase.insertWithOnConflict(
            "sauce_image_hashes",
            null,
            ContentValues().apply {
                put("entry_code", record.entryCode)
                put("page_number", record.pageNumber)
                put("source", record.source)
                normalized.forEachIndexed { index, hash -> put("hash_$index", hash) }
                put("indexed_at", System.currentTimeMillis())
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun contains(entryCode: Int, pageNumber: Int): Boolean = readableDatabase.rawQuery(
        "SELECT 1 FROM sauce_image_hashes WHERE entry_code = ? AND page_number = ? LIMIT 1",
        arrayOf(entryCode.toString(), pageNumber.toString())
    ).use { it.moveToFirst() }

    fun all(): List<SauceFinderIndexRecord> {
        val rows = mutableListOf<SauceFinderIndexRecord>()
        readableDatabase.rawQuery(
            "SELECT entry_code, page_number, source, hash_0, hash_1, hash_2, hash_3, hash_4 FROM sauce_image_hashes",
            emptyArray()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                rows += SauceFinderIndexRecord(
                    entryCode = cursor.getInt(0),
                    pageNumber = cursor.getInt(1),
                    source = cursor.getString(2).orEmpty(),
                    fingerprint = SauceImageFingerprint(LongArray(HASH_COUNT) { cursor.getLong(3 + it) })
                )
            }
        }
        return rows
    }

    fun stats(): SauceFinderIndexStats {
        val counts = readableDatabase.rawQuery(
        "SELECT COUNT(*), COUNT(DISTINCT entry_code) FROM sauce_image_hashes",
        emptyArray()
    ).use { cursor ->
        if (cursor.moveToFirst()) cursor.getInt(0) to cursor.getInt(1) else 0 to 0
    }
        val databasePath = appContext.getDatabasePath(databaseName)
        val bytes = listOf(
            databasePath,
            java.io.File(databasePath.path + "-wal"),
            java.io.File(databasePath.path + "-shm")
        ).sumOf { file -> file.takeIf { it.isFile }?.length() ?: 0L }
        return SauceFinderIndexStats(counts.first, counts.second, bytes)
    }

    companion object {
        private const val DATABASE_NAME = "sauce_finder_index.db"
        private const val GITHUB_DATABASE_NAME = "sauce_finder_index_github.db"
        private const val DATABASE_VERSION = 1
        private const val HASH_COUNT = 5
    }
}
