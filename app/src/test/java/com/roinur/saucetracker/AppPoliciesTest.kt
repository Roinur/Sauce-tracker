package com.roinur.saucetracker

import com.roinur.saucetracker.core.network.shouldRetryWebsiteRequest
import com.roinur.saucetracker.core.network.TemporaryWebsiteException
import com.roinur.saucetracker.core.network.invalidGalleryResponseMessage
import com.roinur.saucetracker.core.network.websiteHttpFailure
import com.roinur.saucetracker.core.change.LibraryChange
import com.roinur.saucetracker.core.change.LibraryChangeAccumulator
import com.roinur.saucetracker.core.change.LibraryChangeImpact
import com.roinur.saucetracker.core.change.LibraryChangeReason
import com.roinur.saucetracker.feature.heatmap.isInsideCenteredThumbnailZone
import com.roinur.saucetracker.feature.library.privacy.LibraryIncognitoPolicy
import com.roinur.saucetracker.feature.browser.extractCommentsSection
import com.roinur.saucetracker.feature.browser.isValidBrowserComment
import com.roinur.saucetracker.feature.browser.removeTrailingTagCount
import com.roinur.saucetracker.feature.browser.resolveBrowserGalleryTitles
import com.roinur.saucetracker.data.remote.GalleryUrls
import com.roinur.saucetracker.data.database.entity.RelatedEntryEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.roinur.saucetracker.feature.library.detail.RelatedEntryMode
import com.roinur.saucetracker.feature.library.detail.SelectedEntryRelatedUiState
import com.roinur.saucetracker.feature.library.detail.availableRelatedEntryModes
import com.roinur.saucetracker.feature.library.detail.filterRelatedEntriesByReadState
import com.roinur.saucetracker.feature.library.detail.resolvedRelatedEntryMode
import com.roinur.saucetracker.feature.library.detail.showReadRelatedEntries
import com.roinur.saucetracker.feature.dashboard.includeDirectNavigationEntry
import com.roinur.saucetracker.feature.dashboard.suggestionRefreshErrorMessage
import com.roinur.saucetracker.feature.dashboard.normalizeDashboardDiscoveryPageOrder
import com.roinur.saucetracker.feature.dashboard.normalizeDashboardInsightPageOrder
import kotlinx.coroutines.CancellationException

class AppPoliciesTest {
    @Test
    fun `dashboard pager order preserves choice and restores missing pages`() {
        assertEquals(
            listOf(
                DashboardDiscoveryPage.SAUCE_FINDER,
                DashboardDiscoveryPage.RANDOM,
                DashboardDiscoveryPage.SUGGESTED
            ),
            normalizeDashboardDiscoveryPageOrder(
                listOf(
                    DashboardDiscoveryPage.SAUCE_FINDER,
                    DashboardDiscoveryPage.RANDOM,
                    DashboardDiscoveryPage.SAUCE_FINDER
                )
            )
        )
        assertEquals(
            listOf(
                DashboardInsightPage.HISTORY,
                DashboardInsightPage.SUBSCRIPTIONS,
                DashboardInsightPage.HEATMAP
            ),
            normalizeDashboardInsightPageOrder(listOf(DashboardInsightPage.HISTORY))
        )
    }

    @Test
    fun `library changes coalesce impacts and keep latest explicit selection`() {
        val changes = LibraryChangeAccumulator()
        changes.record(LibraryChange.tagFilterChanged())
        changes.record(LibraryChange.tagFilterSuggestionsChanged())
        changes.record(
            LibraryChange.entryContentChanged(
                reason = LibraryChangeReason.RATING_CHANGED,
                code = 42
            )
        )
        changes.record(LibraryChange.pinChanged(84))

        val batch = changes.drain()

        assertEquals(84, batch?.selectCode)
        assertTrue(LibraryChangeImpact.ENTRIES in requireNotNull(batch).impacts)
        assertTrue(LibraryChangeImpact.CREATORS in batch.impacts)
        assertTrue(LibraryChangeImpact.SUGGESTIONS_REFRESH in batch.impacts)
        assertTrue(LibraryChangeImpact.READ_ANALYTICS in batch.impacts)
        assertFalse(changes.hasPendingChanges)
        assertNull(changes.drain())
    }

    @Test
    fun `rating and pin changes invalidate only their dependent projections`() {
        val rating = LibraryChange.entryContentChanged(LibraryChangeReason.RATING_CHANGED, 7)
        assertEquals(
            setOf(
                LibraryChangeImpact.ENTRIES,
                LibraryChangeImpact.TAGS,
                LibraryChangeImpact.RELATED_ENTRIES,
                LibraryChangeImpact.READ_ANALYTICS
            ),
            rating.impacts
        )

        val pin = LibraryChange.pinChanged(7)
        assertEquals(setOf(LibraryChangeImpact.ENTRIES), pin.impacts)
    }

    @Test
    fun `full library changes invalidate every derived library projection once`() {
        val changes = LibraryChangeAccumulator()
        changes.record(LibraryChange.fullRefresh(LibraryChangeReason.ENTRY_IMPORTED, 17))
        changes.record(LibraryChange.fullRefresh(LibraryChangeReason.ENTRY_UPDATED, 17))

        val batch = requireNotNull(changes.drain())
        assertEquals(LibraryChangeImpact.entriesAndDerivedData, batch.impacts)
        assertEquals(setOf(LibraryChangeReason.ENTRY_IMPORTED, LibraryChangeReason.ENTRY_UPDATED), batch.reasons)
    }

    @Test
    fun `modern entries always render after activity recreation`() {
        assertTrue(LibraryIncognitoPolicy.shouldRenderEntries(legacyHomeUi = false, entriesCardCollapsed = true))
        assertTrue(LibraryIncognitoPolicy.shouldRenderEntries(legacyHomeUi = false, entriesCardCollapsed = false))
    }

    @Test
    fun `legacy entries still respect collapse control`() {
        assertFalse(LibraryIncognitoPolicy.shouldRenderEntries(legacyHomeUi = true, entriesCardCollapsed = true))
        assertTrue(LibraryIncognitoPolicy.shouldRenderEntries(legacyHomeUi = true, entriesCardCollapsed = false))
    }

    @Test
    fun `only temporary website statuses retry`() {
        listOf(408, 429, 500, 502, 503, 504).forEach { assertTrue(shouldRetryWebsiteRequest(it)) }
        listOf(400, 401, 403, 404, 410).forEach { assertFalse(shouldRetryWebsiteRequest(it)) }
    }

    @Test
    fun `html response is described as website problem rather than api error`() {
        assertEquals(
            "The website returned an HTML page instead of gallery data. This is usually a temporary block or service problem.",
            invalidGalleryResponseMessage("<html>blocked</html>", "text/html")
        )
    }

    @Test
    fun `temporary status creates retryable failure but not found does not`() {
        assertTrue(websiteHttpFailure("testing", 503) is TemporaryWebsiteException)
        assertFalse(websiteHttpFailure("testing", 404) is TemporaryWebsiteException)
    }

    @Test
    fun `thumbnail percentage describes centered viewport area`() {
        assertTrue(isInsideCenteredThumbnailZone(50f, 50f, 0f, 100f, 0f, 100f, 10))
        assertFalse(isInsideCenteredThumbnailZone(5f, 5f, 0f, 100f, 0f, 100f, 10))
        assertTrue(isInsideCenteredThumbnailZone(5f, 5f, 0f, 100f, 0f, 100f, 100))
    }

    @Test
    fun `dashboard keeps reference phone unchanged and scales shorter phones`() {
        assertEquals(1f, adaptiveDashboardScale(933), 0.0001f)
        assertEquals(1f, adaptiveDashboardScale(900), 0.0001f)
        assertTrue(adaptiveDashboardScale(720) < 1f)
        assertTrue(adaptiveDashboardScale(1100) > 1f)
    }

    @Test
    fun `comment section scan stops after the matching nested container`() {
        val html = """
            <main><section id="comments"><div><div>First</div></div></section>
            <section id="unrelated">Do not include</section></main>
        """.trimIndent()

        val comments = extractCommentsSection(html)

        assertTrue(comments.contains("First"))
        assertFalse(comments.contains("Do not include"))
    }

    @Test
    fun `gallery comments use the current v2 endpoint`() {
        assertEquals(
            "https://nhentai.net/api/v2/galleries/123/comments",
            GalleryUrls.comments(123)
        )
    }

    @Test
    fun `browser related galleries stay on their own endpoints`() {
        assertEquals(
            "https://nhentai.net/api/v2/galleries/123/related",
            GalleryUrls.relatedV2(123)
        )
        assertEquals(
            "https://nhentai.net/api/gallery/123/related",
            GalleryUrls.relatedLegacy(123)
        )
    }

    @Test
    fun `v2 related gallery list uses its real title instead of code fallback`() {
        val (title, subtitle) = resolveBrowserGalleryTitles(
            code = 123,
            titleCandidates = listOf("", "Actual gallery title", "Japanese title"),
            subtitleCandidates = listOf("", "Japanese title")
        )

        assertEquals("Actual gallery title", title)
        assertEquals("Japanese title", subtitle)
    }

    @Test
    fun `comment author is never accepted as its own message`() {
        assertFalse(isValidBrowserComment("ReaderName", "ReaderName"))
        assertTrue(isValidBrowserComment("ReaderName", "A different message"))
    }

    @Test
    fun `fallback browser tag label does not repeat its separate count`() {
        assertEquals("example tag", removeTrailingTagCount("example tag 162", "162"))
        assertEquals("example tag", removeTrailingTagCount("example tag | 1.2K", "1.2K"))
        assertEquals("route 66", removeTrailingTagCount("route 66", "162"))
    }

    @Test
    fun `related entry modes follow parts recommendations artist priority`() {
        val parts = listOf(
            SeriesEntryPreview(code = 1, title = "One", sequence = 1, score = 1f),
            SeriesEntryPreview(code = 2, title = "Two", sequence = 2, score = 1f)
        )
        val preview = RelatedEntryEntity(
            code = 3,
            title = "Three",
            subtitle = "",
            thumbnailUrl = "",
            numPages = 10
        )
        val modes = availableRelatedEntryModes(
            SeriesNeighbors(parts = parts, currentPartIndex = 0),
            SelectedEntryRelatedUiState(moreLikeThis = listOf(preview), sameArtist = listOf(preview))
        )

        assertEquals(
            listOf(RelatedEntryMode.PARTS, RelatedEntryMode.MORE_LIKE_THIS, RelatedEntryMode.SAME_ARTIST),
            modes
        )
        assertEquals(RelatedEntryMode.PARTS, resolvedRelatedEntryMode(null, modes))
    }

    @Test
    fun `related entry mode persists while available and falls back safely`() {
        val available = listOf(RelatedEntryMode.MORE_LIKE_THIS, RelatedEntryMode.SAME_ARTIST)

        assertEquals(
            RelatedEntryMode.SAME_ARTIST,
            resolvedRelatedEntryMode(RelatedEntryMode.SAME_ARTIST, available)
        )
        assertEquals(
            RelatedEntryMode.MORE_LIKE_THIS,
            resolvedRelatedEntryMode(RelatedEntryMode.PARTS, available)
        )
        assertNull(resolvedRelatedEntryMode(RelatedEntryMode.PARTS, emptyList()))
    }

    @Test
    fun `only read filter exposes read related recommendations`() {
        assertTrue(showReadRelatedEntries(EntryReadFilterMode.READ))
        assertFalse(showReadRelatedEntries(EntryReadFilterMode.ALL))
        assertFalse(showReadRelatedEntries(EntryReadFilterMode.UNREAD))
        assertFalse(showReadRelatedEntries(EntryReadFilterMode.DOWNLOADED))
    }

    @Test
    fun `related recommendations filter after candidates are loaded`() {
        val unread = RelatedEntryEntity(101, "Unread", "", "", 10)
        val read = RelatedEntryEntity(202, "Read", "", "", 20)
        val candidates = listOf(unread, read)
        val states = mapOf(101 to false, 202 to true)

        assertEquals(
            listOf(101),
            filterRelatedEntriesByReadState(candidates, states, showReadEntries = false).map { it.code }
        )
        assertEquals(
            listOf(202),
            filterRelatedEntriesByReadState(candidates, states, showReadEntries = true).map { it.code }
        )
    }

    @Test
    fun `direct related navigation includes a target hidden by current filters`() {
        val visible = listOf(entryRow(code = 101))
        val hiddenTarget = entryRow(code = 202)

        val result = includeDirectNavigationEntry(visible, hiddenTarget)

        assertEquals(listOf(101, 202), result.map { it.code })
    }

    @Test
    fun `direct related navigation does not duplicate an already visible target`() {
        val visible = listOf(entryRow(code = 101), entryRow(code = 202))

        val result = includeDirectNavigationEntry(visible, entryRow(code = 202))

        assertEquals(listOf(101, 202), result.map { it.code })
    }

    @Test
    fun `cancelled suggestion refresh is normal control flow and not a user error`() {
        assertNull(suggestionRefreshErrorMessage(CancellationException("StandaloneCoroutine was cancelled")))
        assertEquals(
            "Could not refresh suggestions:\nnetwork unavailable",
            suggestionRefreshErrorMessage(IllegalStateException("network unavailable"))
        )
    }

    private fun entryRow(code: Int) = EntryRow(
        code = code,
        title = "Entry $code",
        numPages = 10,
        uploadDate = "",
        addedAt = "",
        rating = 0,
        averageRating = 0f,
        isRead = false,
        pinned = false,
        fetchedAt = "",
        sourceUrl = "",
        thumbnailUrl = "",
        tags = ""
    )
}
