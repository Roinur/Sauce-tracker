package com.roinur.saucetracker.data.downloads

import com.roinur.saucetracker.data.source.mangaDexBaseRemoteId
import com.roinur.saucetracker.data.source.mangaDexLanguage

/** New captures use explicit source/entry identity. Legacy captures are never rewritten. */
internal data class EntryScreenshotScope(
    val sourceId: String,
    val remoteId: String,
    val title: String,
    val legacyChapterPrefixes: Set<String> = emptySet()
) {
    fun matches(photoSourceId: String, photoRemoteId: String, displayName: String): Boolean {
        if (photoSourceId.isNotBlank() || photoRemoteId.isNotBlank()) {
            if (photoSourceId != sourceId) return false
            return if (sourceId == "mangadex" && mangaDexLanguage(remoteId).isBlank()) {
                mangaDexBaseRemoteId(photoRemoteId) == mangaDexBaseRemoteId(remoteId)
            } else photoRemoteId == remoteId
        }
        val legacy = legacyRawPage.matchEntire(displayName) ?: return false
        val chapterPrefix = legacy.groupValues[2].lowercase()
        return when (sourceId) {
            "mangadex" -> chapterPrefix.isNotBlank() && chapterPrefix in legacyChapterPrefixes
            "nhentai" -> chapterPrefix.isBlank() && legacy.groupValues[1] == title.take(48)
            else -> false
        }
    }

    companion object {
        private val legacyRawPage = Regex("^(.*?)(?:_chapter_([0-9a-fA-F-]{12}))?_page_([1-9][0-9]*)$", RegexOption.DOT_MATCHES_ALL)
    }
}
