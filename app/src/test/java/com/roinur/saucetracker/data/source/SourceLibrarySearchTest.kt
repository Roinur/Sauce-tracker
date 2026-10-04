package com.roinur.saucetracker.data.source

import com.roinur.saucetracker.TagRouteRef
import org.junit.Assert.*
import org.junit.Test

class SourceLibrarySearchTest {
    @Test fun structuredSearchIsCompiledBeforePagination() {
        val query = sourceLibraryFilter("title:Prisma artist:TYPE-MOON language:English rating:4 chapters:10-40") { null }
        assertEquals(listOf("%prisma%", "artist", "%type-moon%", "language", "%english%", "4", "10", "40"), query.arguments)
        assertTrue(query.clauses.joinToString().contains("pe.rating=?"))
        assertTrue(query.clauses.joinToString().contains("se.unit_count BETWEEN ? AND ?"))
        assertEquals(query.arguments.size, query.clauses.sumOf { clause -> clause.count { it == '?' } })
    }
    @Test fun mixedSourcePresetsResolveIdsToSemanticTagsNotProviderIds() {
        val refs = mapOf(4000000001L to TagRouteRef("English", "language"), 12L to TagRouteRef("incest", "tag"))
        val query = sourceLibraryFilter("anytagid:4000000001|12 excludetagid:12", refs::get)
        assertTrue(query.clauses[0].contains(" OR "))
        assertTrue(query.clauses[1].startsWith("NOT "))
        assertEquals(listOf("language", "english", "tag", "incest", "tag", "incest"), query.arguments)
    }
    @Test fun humanSourceNamesAndQuotedFreeTextAreNotLiteralPrefixSearches() {
        val query = sourceLibraryFilter("\"summer memory\" source:Manga Dex") { null }
        assertTrue(query.clauses.first().contains("se.title"))
        assertEquals("mangadex", query.arguments.last())
        assertFalse(query.arguments.any { it.contains("source:") })
    }
    @Test fun completeLocalLibraryHasNoThousandEntryCeiling() {
        val items = (1..2507).toList()
        val calls = mutableListOf<Pair<Int, Int>>()
        val loaded = readAllLibraryPages { offset, limit -> calls += offset to limit; items.drop(offset).take(limit) }
        assertEquals(items, loaded)
        assertEquals(listOf(0, 500, 1000, 1500, 2000, 2500), calls.map { it.first })
        assertTrue(calls.all { it.second == 500 })
    }
    @Test fun exactPageBoundaryAndEmptyLibraryTerminate() {
        assertEquals(1000, readAllLibraryPages { offset, limit -> (1..1000).drop(offset).take(limit) }.size)
        assertTrue(readAllLibraryPages<Int> { _, _ -> emptyList() }.isEmpty())
    }
    @Test fun paginatedChooserCanGoBeyondAThousandWithoutLoadingEverything() {
        val calls = mutableListOf<Pair<Int, Int>>()
        val loaded = readAllLibraryPages(1201) { offset, limit -> calls += offset to limit; (1..5000).drop(offset).take(limit) }
        assertEquals(1201, loaded.size)
        assertEquals(listOf(0 to 500, 500 to 500, 1000 to 201), calls)
    }
    @Test fun browserFiltersRetainTheirCategories() {
        val query = sourceBrowserQuery("title", listOf(TagRouteRef("English", "language"), TagRouteRef("TYPE-MOON", "artist"), TagRouteRef("incest", "tag")))
        val parsed = SourceQueryParser.parse(query)
        assertEquals("title", parsed.freeText)
        assertEquals(listOf(SourceQueryField.LANGUAGE, SourceQueryField.ARTIST, SourceQueryField.TAG), parsed.terms.map { it.field })
    }
    @Test fun creatorPresetUsesTheSharedCreatorRelation() {
        val query = sourceLibraryFilter("anytagid:4000000001") { TagRouteRef("TYPE-MOON", "creator") }
        assertTrue(query.clauses.single().contains("source_entry_creators"))
        assertFalse(query.clauses.single().contains("st.type=?"))
        assertEquals(listOf("type-moon"), query.arguments)
    }
    @Test fun languageTranslationAcceptsHumanNamesAndProviderCodes() {
        assertEquals("en", mangaDexLanguageCode("English"))
        assertEquals("ja", mangaDexLanguageCode("Japanese"))
        assertEquals("pt-br", mangaDexLanguageCode("pt-br"))
        assertNull(mangaDexLanguageCode("unknown language"))
    }
}
