package com.roinur.saucetracker.data.database.entity

data class EntryEntity(
    val code: Int,
    val title: String,
    val numPages: Int,
    val uploadDate: String,
    val addedAt: String,
    val rating: Int,
    val averageRating: Float,
    val isRead: Boolean,
    val pinned: Boolean,
    val fetchedAt: String,
    val sourceUrl: String,
    val thumbnailUrl: String,
    val tags: String,
    val sourceId: String = "nhentai",
    val remoteId: String = code.toString(),
    val unitLabel: String = "pages"
) {
    val isNhentai: Boolean get() = sourceId == "nhentai"
    val displayId: String get() = if (isNhentai) code.toString() else remoteId
}

