package com.roinur.saucetracker.data.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceAdapterContractTest {
    private val nhentai = NhentaiSourceAdapter()
    private val mangaDex = MangaDexSourceAdapter()

    @Test fun officialAdaptersExposeTheRequiredV2Baseline() {
        listOf(nhentai, mangaDex).forEach { adapter ->
            assertTrue(adapter.contractVersion > 0)
            assertTrue(adapter.supports(SourceCapability.SEARCH))
            assertTrue(adapter.supports(SourceCapability.DETAIL))
            assertTrue(adapter.supports(SourceCapability.IMPORT))
            assertTrue(adapter.supports(SourceCapability.METADATA_REFRESH))
        }
        assertTrue(nhentai.supports(SourceCapability.READER))
        assertTrue(mangaDex.supports(SourceCapability.READER))
        assertTrue(mangaDex.supports(SourceCapability.SUBSCRIPTIONS))
        assertTrue(mangaDex.supports(SourceCapability.CREATOR_NAVIGATION))
        assertTrue(mangaDex.supports(SourceCapability.POPULAR_SORT))
    }

    @Test fun adaptersNormalizeTheirOwnUrlsWithoutCrossProviderCollisions() {
        val uuid = "123e4567-e89b-12d3-a456-426614174000"
        assertEquals("123", nhentai.normalizeRemoteId("https://nhentai.net/g/123/"))
        assertNull(nhentai.normalizeRemoteId("https://mangadex.org/title/$uuid/example"))
        assertEquals(uuid, mangaDex.normalizeRemoteId("https://mangadex.org/title/$uuid/example"))
        assertNull(mangaDex.normalizeRemoteId("123"))
        assertEquals("https://nhentai.net/g/123/", nhentai.canonicalUrl("123"))
        assertEquals("https://mangadex.org/title/$uuid", mangaDex.canonicalUrl(uuid))
    }

    @Test fun sourceFiltersShortCircuitTheWrongProviderWithoutNetworkWork() {
        val nhOnly = SourceQuery(terms = listOf(SourceQueryTerm(SourceQueryField.SOURCE, "nhentai")))
        val mdOnly = SourceQuery(terms = listOf(SourceQueryTerm(SourceQueryField.SOURCE, "mangadex")))
        assertTrue(mangaDex.search(nhOnly).entries.isEmpty())
        assertTrue(nhentai.search(mdOnly).entries.isEmpty())
    }

    @Test fun unsupportedStructuredFieldsNeverDegradeIntoBroadResults() {
        val unsupportedGroup = SourceQuery(terms = listOf(SourceQueryTerm(SourceQueryField.GROUP, "Example")))
        val remoteStatus = SourceQuery(terms = listOf(SourceQueryTerm(SourceQueryField.STATUS, "ongoing")))
        assertTrue(mangaDex.search(unsupportedGroup).entries.isEmpty())
        assertTrue(nhentai.search(remoteStatus).entries.isEmpty())
    }
}
