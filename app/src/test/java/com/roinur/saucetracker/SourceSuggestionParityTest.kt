package com.roinur.saucetracker

import com.roinur.saucetracker.data.source.*
import com.roinur.saucetracker.feature.suggestions.*
import org.junit.Assert.*
import org.junit.Test

class SourceSuggestionParityTest {
    private fun entry() = SourceEntry(SourceEntryKey(SourceId("mangadex"), "test@en"), "Prisma", canonicalUrl = "https://mangadex.org/title/test",
        tags = listOf(SourceTag("English", "language"), SourceTag("Incest", "tag")), creators = listOf(SourceCreator("TYPE-MOON", "author")))
    @Test fun creatorAndLanguageFiltersApplyToMangaDexCandidates() {
        assertTrue(entry().matchesSourceSuggestionSearch("author:TYPE-MOON language:English", emptyList()) { null })
        assertFalse(entry().matchesSourceSuggestionSearch("language:Japanese", emptyList()) { null })
        assertFalse(entry().matchesSourceSuggestionSearch("excludetag:incest", emptyList()) { null })
    }
    @Test fun presetExclusionsAreNotRelaxedToGetMoreSuggestions() {
        val refs = mapOf(1L to TagRouteRef("incest", "tag"), 2L to TagRouteRef("English", "language"))
        assertTrue(entry().matchesSourceSuggestionSearch("anytagid:1|2", emptyList(), refs::get))
        assertFalse(entry().matchesSourceSuggestionSearch("excludetagid:1", emptyList(), refs::get))
    }
    @Test fun authorReceivesTheSameCreatorScoringAsAnArtist() {
        fun score(type: String) = scoreSuggestionCandidate(0, listOf(GalleryTag("TYPE-MOON", type)), emptyMap(), emptyMap(), mapOf("type-moon" to 3f), 0f, 0f, 1f, emptySet()).score
        assertEquals(score("artist"), score("author"), 0.001f)
        assertEquals(SuggestionWeightCategory.CREATOR, SuggestionWeightCategory.fromTagType("author"))
    }
    @Test fun mixedProfileCannotBeFilledEntirelyByNHentai() {
        fun row(code: Int, score: Float) = SuggestedEntryRow(code, "Entry", 0, "", "", emptyList(), score)
        val rows = (1..20).map { row(it, 10f) } + (1000000001..1000000010).map { row(it, 1f) }
        val visible = balancedSourceSuggestions(rows, { if (it >= 1000000000) "mangadex" else "nhentai" }, 10)
        assertEquals(5, visible.count { it.code >= 1000000000 })
        assertEquals(10, visible.map { it.code }.distinct().size)
    }
    @Test fun remoteQueryTranslatesPresetIdsAndPreservesHardExclusions() {
        val refs = mapOf(1L to TagRouteRef("incest", "tag"), 2L to TagRouteRef("English", "language"))
        val queries = sourceSuggestionSearchQueries("title:Prisma anytagid:1|2 excludetag:comedy", emptyList(), refs::get)
        assertEquals(2, queries.size)
        assertTrue(queries.all { it.freeText == "Prisma" })
        assertTrue(queries.all { it.terms.any { term -> term.field == SourceQueryField.TAG && term.value == "comedy" && term.excluded } })
        assertTrue(queries.any { it.terms.any { term -> term.field == SourceQueryField.LANGUAGE && term.value == "English" } })
        assertTrue(queries.none { it.freeText.contains("tagid:") })
    }
    @Test fun chipIdentityIsExactAndDoesNotConflateCreatorsWithTags() {
        assertFalse(entry().matchesSourceSuggestionSearch("", listOf(TagRouteRef("moon", "author"))) { null })
        assertFalse(entry().matchesSourceSuggestionSearch("", listOf(TagRouteRef("TYPE-MOON", "tag"))) { null })
        assertTrue(entry().matchesSourceSuggestionSearch("", listOf(TagRouteRef("TYPE-MOON", "creator"))) { null })
    }
    @Test fun chapterCountsAreNotPresentedOrFilteredAsPages() {
        val manga = entry().copy(unitCount = 31, unitLabel = "chapters")
        assertTrue(manga.matchesSourceSuggestionSearch("chapters:30-40", emptyList()) { null })
        assertFalse(manga.matchesSourceSuggestionSearch("pages:31", emptyList()) { null })
        assertFalse(entry().copy(unitCount = 31, unitLabel = "pages").matchesSourceSuggestionSearch("chapters:31", emptyList()) { null })
    }
    @Test fun republishingMixedSuggestionsDoesNotRestoreHiddenSkippedOrImportedManga() {
        val rows = (1..5).map { code -> SuggestedEntryRow(code, "Entry", 10, "", "", emptyList(), 1f, sourceId = "mangadex", remoteId = "series-$code") }
        val visible = visibleSourceSuggestionRows(rows, setOf(1), setOf(2), setOf("mangadex:series-3"), setOf("mangadex"))
        assertEquals(listOf(4, 5), visible.map { it.code })
        assertTrue(visibleSourceSuggestionRows(rows, emptySet(), emptySet(), emptySet(), setOf("nhentai")).isEmpty())
    }
}
