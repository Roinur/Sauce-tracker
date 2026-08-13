package com.example.saucetracker.data.database.dao

import com.example.saucetracker.DayReadEntryRow
import com.example.saucetracker.EntryRatingHistoryRow
import com.example.saucetracker.ReadAnalyticsSnapshot
import com.example.saucetracker.data.database.SauceTrackerDatabase
import java.time.LocalDate

internal interface HistoryDao {
    fun analytics(): ReadAnalyticsSnapshot
    fun entriesForDay(day: LocalDate): List<DayReadEntryRow>
    fun ratings(code: Int): List<EntryRatingHistoryRow>
    fun recordRating(code: Int, rating: Int, isReread: Boolean)
}

internal class SqliteHistoryDao(private val database: SauceTrackerDatabase) : HistoryDao {
    override fun analytics(): ReadAnalyticsSnapshot = database.getReadAnalyticsSnapshot()
    override fun entriesForDay(day: LocalDate): List<DayReadEntryRow> = database.listReadEntriesForDay(day)
    override fun ratings(code: Int): List<EntryRatingHistoryRow> = database.getEntryRatingHistory(code)
    override fun recordRating(code: Int, rating: Int, isReread: Boolean) =
        database.recordEntryRatingSession(code, rating, isReread)
}
