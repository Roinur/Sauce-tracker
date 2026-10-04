package com.roinur.saucetracker.data.profile

import com.roinur.saucetracker.data.source.SourceId

enum class ProfileKind { SOURCE_LOCKED, COMBINED }

data class LibraryProfile(
    val id: String,
    val name: String,
    val kind: ProfileKind,
    val sourceIds: Set<SourceId>,
    val createdAt: String,
    val lastUsedAt: String,
    val isMain: Boolean
)

data class ProfileEntryState(
    val profileId: String,
    val sourceId: SourceId,
    val remoteId: String,
    val isRead: Boolean,
    val rating: Int,
    val pinned: Boolean,
    val pinPriority: Int,
    val readAt: String,
    val addedAt: String,
    val fetchedAt: String
)

data class ProfileTransferResult(
    val copied: Int,
    val merged: Int,
    val removedFromSource: Int,
    val failed: Int
)

