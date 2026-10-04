package com.roinur.saucetracker.core.change

import org.junit.Assert.*
import org.junit.Test

class NavigationRefreshTest {
    @Test fun unchangedReturnRetainsLibrary() {
        val gate = NavigationRefreshGate()
        gate.remember(DatabaseChangeToken(4, 100))
        repeat(20) { assertFalse(gate.needsRefresh(DatabaseChangeToken(4, 100))) }
    }

    @Test fun otherConnectionWritesRefreshReaderBrowserBridgeAndWorkerData() {
        val gate = NavigationRefreshGate()
        gate.remember(DatabaseChangeToken(4, 100))
        assertTrue(gate.needsRefresh(DatabaseChangeToken(5, 100)))
    }

    @Test fun sameConnectionBackgroundWritesAlsoRefresh() {
        val gate = NavigationRefreshGate()
        gate.remember(DatabaseChangeToken(4, 100))
        assertTrue(gate.needsRefresh(DatabaseChangeToken(4, 101)))
    }

    @Test fun failedProbeNeverHidesChangesAndCanRecover() {
        val gate = NavigationRefreshGate()
        assertTrue(gate.needsRefresh(DatabaseChangeToken(4, 100)))
        gate.remember(DatabaseChangeToken(4, 100))
        assertTrue(gate.needsRefresh(null))
        gate.remember(null)
        assertTrue(gate.needsRefresh(DatabaseChangeToken(4, 100)))
        gate.remember(DatabaseChangeToken(5, 120))
        assertFalse(gate.needsRefresh(DatabaseChangeToken(5, 120)))
    }

    @Test fun linearLookupPreservesOnlyVisibleSourceIdentities() {
        val keys = mapOf(1 to "nhentai:1", 2 to "mangadex:uuid:en", 3 to "mangadex:uuid:ja")
        assertEquals(setOf("nhentai:1", "mangadex:uuid:ja"), visibleEntryKeys(listOf(1, 3, 99), keys))
        assertTrue(visibleEntryKeys(emptyList(), keys).isEmpty())
    }

    @Test fun largeLibraryDoesOneMapLookupPerVisibleEntry() {
        val backing = (1..30000).associateWith { "nhentai:$it" }
        var lookups = 0
        val counted = object : Map<Int, String> by backing {
            override fun get(key: Int): String? { lookups++; return backing[key] }
        }
        assertEquals(30000, visibleEntryKeys(1..30000, counted).size)
        assertEquals(30000, lookups)
    }
}
