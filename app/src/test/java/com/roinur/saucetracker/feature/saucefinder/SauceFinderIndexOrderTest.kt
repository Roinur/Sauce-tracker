package com.roinur.saucetracker.feature.saucefinder

import org.junit.Assert.assertEquals
import org.junit.Test

class SauceFinderIndexOrderTest {
    private data class Entry(val id: Int, val downloaded: Boolean, val read: Boolean)
    @Test fun downloadedUnreadStillComesBeforeReadRemoteAndRemainingEntries() {
        val entries = listOf(Entry(1, false, false), Entry(2, false, true), Entry(3, true, false), Entry(4, true, true), Entry(5, false, true), Entry(6, false, false))
        assertEquals(listOf(3, 4, 2, 5, 1, 6), sauceFinderIndexOrder(entries, Entry::downloaded, Entry::read).map { it.id })
    }
    @Test fun orderWithinEachTierIsStableAndEveryEntryIsKept() {
        val entries = (1..2401).map { Entry(it, it % 7 == 0, it % 3 == 0) }
        val ordered = sauceFinderIndexOrder(entries, Entry::downloaded, Entry::read)
        assertEquals(entries.filter { it.downloaded }, ordered.takeWhile { it.downloaded })
        assertEquals(entries.map { it.id }.toSet(), ordered.map { it.id }.toSet())
        assertEquals(entries.size, ordered.size)
    }
}
