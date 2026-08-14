package com.roinur.saucetracker.feature.library.privacy

internal object LibraryIncognitoPolicy {
    fun shouldRenderEntries(legacyHomeUi: Boolean, entriesCardCollapsed: Boolean): Boolean {
        return !legacyHomeUi || !entriesCardCollapsed
    }
}
