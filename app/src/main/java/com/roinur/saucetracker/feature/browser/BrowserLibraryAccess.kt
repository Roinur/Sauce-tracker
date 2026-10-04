package com.roinur.saucetracker.feature.browser

import com.roinur.saucetracker.GalleryData
import com.roinur.saucetracker.GalleryTag
import com.roinur.saucetracker.data.database.SauceTrackerDatabase
import com.roinur.saucetracker.data.profile.ProfileStore
import com.roinur.saucetracker.data.source.SourceEntryKey
import com.roinur.saucetracker.data.source.SourceEntryStore
import com.roinur.saucetracker.data.source.SourceId

/** Keeps the existing browser UX independent of provider-specific persistence. */
internal class BrowserLibraryAccess(private val database: SauceTrackerDatabase) {
    private val sourceStore = SourceEntryStore(database)
    private val profileId: String get() = ProfileStore(database).activeProfileId()

    fun state(summary: BrowserGallerySummary): BrowserLocalLibraryState {
        if (summary.isNhentai) {
            val local = database.getEntryDetail(summary.code, profileId)
            return if (local == null) emptyState() else BrowserLocalLibraryState(
                exists = true,
                rating = local.rating.coerceIn(0, 5),
                isRead = local.isRead,
                pinned = database.isEntryPinned(summary.code, profileId)
            )
        }
        val state = sourceStore.statesForSource(profileId, SourceId(summary.sourceId))[summary.remoteId]
            ?: return emptyState()
        return BrowserLocalLibraryState(true, state.rating.coerceIn(0, 5), state.isRead, state.pinned)
    }

    fun states(rows: List<BrowserGallerySummary>): Map<Int, BrowserLocalLibraryState> {
        val nhRows = rows.filter(BrowserGallerySummary::isNhentai)
        val result = mutableMapOf<Int, BrowserLocalLibraryState>()
        if (nhRows.isNotEmpty()) {
            val codes = nhRows.map { it.code }.distinct()
            val batch = database.getBrowserLibraryStates(codes, profileId)
            nhRows.forEach { row ->
                val local = batch[row.code]
                result[row.code] = if (local == null) emptyState() else BrowserLocalLibraryState(
                    true, local.rating.coerceIn(0, 5), local.isRead, local.pinned
                )
            }
        }
        rows.filterNot(BrowserGallerySummary::isNhentai).groupBy { it.sourceId }.forEach { (source, sourceRows) ->
            val states = sourceStore.statesForSource(profileId, SourceId(source))
            sourceRows.forEach { row ->
                val state = states[row.remoteId]
                result[row.code] = if (state == null) emptyState() else BrowserLocalLibraryState(
                    true, state.rating.coerceIn(0, 5), state.isRead, state.pinned
                )
            }
        }
        return result
    }

    fun import(detail: BrowserGalleryDetail): Boolean {
        val summary = detail.summary
        if (summary.isNhentai) {
            database.upsertGallery(detail.toGalleryData())
            return database.getEntryDetail(summary.code, profileId) != null
        }
        val entry = summary.sourceEntry ?: return false
        sourceStore.upsert(entry, profileId)
        return state(summary).exists
    }

    fun apply(summary: BrowserGallerySummary, action: BrowserPendingLibraryAction): BrowserLocalLibraryState {
        val current = state(summary)
        if (!current.exists) return current
        if (summary.isNhentai) {
            when (action) {
                BrowserPendingLibraryAction.ImportOnly -> Unit
                is BrowserPendingLibraryAction.SetRating -> {
                    database.setEntryRating(summary.code, action.rating.coerceIn(0, 5), profileId)
                    database.setEntryRead(summary.code, true, profileId)
                }
                is BrowserPendingLibraryAction.SetRead -> database.setEntryRead(summary.code, action.isRead, profileId)
                is BrowserPendingLibraryAction.SetPinned -> database.setEntryPinned(summary.code, action.pinned, profileId)
                BrowserPendingLibraryAction.ToggleRead -> database.setEntryRead(summary.code, !current.isRead, profileId)
                BrowserPendingLibraryAction.TogglePinned -> database.setEntryPinned(summary.code, !current.pinned, profileId)
            }
        } else {
            val key = SourceEntryKey(SourceId(summary.sourceId), summary.remoteId)
            when (action) {
                BrowserPendingLibraryAction.ImportOnly -> Unit
                is BrowserPendingLibraryAction.SetRating -> sourceStore.updateState(profileId, key, read = true, rating = action.rating)
                is BrowserPendingLibraryAction.SetRead -> sourceStore.updateState(profileId, key, read = action.isRead)
                is BrowserPendingLibraryAction.SetPinned -> sourceStore.updateState(profileId, key, pinned = action.pinned)
                BrowserPendingLibraryAction.ToggleRead -> sourceStore.updateState(profileId, key, read = !current.isRead)
                BrowserPendingLibraryAction.TogglePinned -> sourceStore.updateState(profileId, key, pinned = !current.pinned)
            }
        }
        return state(summary)
    }

    fun remove(summary: BrowserGallerySummary): BrowserLocalLibraryState {
        if (summary.isNhentai) database.deleteEntry(summary.code)
        else sourceStore.removeMembership(profileId, SourceEntryKey(SourceId(summary.sourceId), summary.remoteId))
        return emptyState()
    }

    private fun BrowserGalleryDetail.toGalleryData(): GalleryData {
        val summary = summary
        return GalleryData(
            code = summary.code,
            title = summary.title,
            subtitle = summary.subtitle,
            numPages = summary.numPages,
            uploadDate = summary.uploadDate,
            sourceUrl = summary.canonicalUrl.ifBlank { "https://nhentai.net/g/${summary.code}/" },
            mediaId = summary.mediaId,
            coverExt = summary.coverExt,
            tags = tagsByType.flatMap { (type, names) -> names.map { GalleryTag(it, type) } }
        )
    }

    private fun emptyState() = BrowserLocalLibraryState(false, 0, false, false)
}
