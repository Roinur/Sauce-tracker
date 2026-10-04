package com.roinur.saucetracker.core.change

/** Compared only on the same live SQLite helper (not across processes/connections). */
internal data class DatabaseChangeToken(val externalVersion: Long, val localChanges: Long)

internal class NavigationRefreshGate {
    private var baseline: DatabaseChangeToken? = null

    fun remember(token: DatabaseChangeToken?) {
        baseline = token
    }

    // Unknown/error states must refresh, never silently retain stale library data.
    fun needsRefresh(token: DatabaseChangeToken?): Boolean =
        token == null || baseline == null || token != baseline
}

/** Linear visibility lookup, shared by tags/creators without scanning the UI list per entry. */
internal fun <K : Any> visibleEntryKeys(codes: Iterable<Int>, keysByCode: Map<Int, K>): Set<K> =
    codes.mapNotNullTo(HashSet()) { keysByCode[it] }
