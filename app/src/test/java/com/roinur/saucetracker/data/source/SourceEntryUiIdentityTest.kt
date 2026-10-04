package com.roinur.saucetracker.data.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceEntryUiIdentityTest {
    @Test fun nhentaiKeepsItsVisibleNumericIdentity() {
        val key = SourceEntryKey(SourceId("nhentai"), "123456")

        assertEquals(123456, key.uiCode())
        assertEquals("#123456", key.displayId)
    }

    @Test fun mangadexGetsAStableUiIdentityWithoutReplacingItsRemoteId() {
        val remoteId = "7f30dfc3-0b80-4dcc-a3b9-0cd746fac005"
        val key = SourceEntryKey(SourceId("mangadex"), remoteId)

        assertEquals(key.uiCode(), key.uiCode())
        assertTrue(key.uiCode() >= 1_000_000_000)
        assertEquals(remoteId, key.displayId)
    }

    @Test fun sourceRemainsPartOfTheIdentity() {
        val remoteId = "same-id"

        assertNotEquals(
            SourceEntryKey(SourceId("mangadex"), remoteId).uiCode(),
            SourceEntryKey(SourceId("example"), remoteId).uiCode()
        )
    }
}
