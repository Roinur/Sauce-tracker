package com.example.saucetracker.data.database.entity

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
    val tags: String
)

