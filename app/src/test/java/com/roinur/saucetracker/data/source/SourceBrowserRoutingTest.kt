package com.roinur.saucetracker.data.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SourceBrowserRoutingTest {
    private val nhentai = SourceId("nhentai")
    private val mangadex = SourceId("mangadex")
    private val both = setOf(nhentai, mangadex)

    @Test fun singleSourceOpensDirectly() {
        assertEquals(mangadex, resolveBrowserSource(setOf(mangadex), "romance"))
    }

    @Test fun knownIdsAndUrlsChooseTheirProvider() {
        assertEquals(nhentai, resolveBrowserSource(both, "#123456"))
        assertEquals(mangadex, resolveBrowserSource(both, "https://mangadex.org/title/7f30dfc3-0b80-4dcc-a3b9-0cd746fac005"))
    }

    @Test fun explicitSourcePrefixWinsAndAmbiguousTextPrompts() {
        assertEquals(mangadex, resolveBrowserSource(both, "source:mangadex romance"))
        assertNull(resolveBrowserSource(both, "romance"))
    }
}
