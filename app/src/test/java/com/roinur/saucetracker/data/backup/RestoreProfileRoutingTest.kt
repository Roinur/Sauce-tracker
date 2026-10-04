package com.roinur.saucetracker.data.backup

import org.junit.Assert.assertEquals
import org.junit.Test

class RestoreProfileRoutingTest {
    @Test fun `legacy backup goes to NHentai even while MangaDex is active`() {
        assertEquals("main", RestoreProfileRouting.targetProfileId(false, null, "mangadex-default"))
    }

    @Test fun `legacy backup cannot be redirected by a selected profile`() {
        assertEquals("main", RestoreProfileRouting.targetProfileId(false, "custom", "mangadex-default"))
    }

    @Test fun `V2 selected profile restore retains its explicit destination`() {
        assertEquals("custom", RestoreProfileRouting.targetProfileId(true, "custom", "mangadex-default"))
    }

    @Test fun `V2 full restore retains normal active profile handling`() {
        assertEquals("mangadex-default", RestoreProfileRouting.targetProfileId(true, null, "mangadex-default"))
    }
}
