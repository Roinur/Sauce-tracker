package com.roinur.saucetracker

import com.roinur.saucetracker.feature.suggestions.filterOutImportedSuggestions
import com.roinur.saucetracker.feature.suggestions.scoreSuggestionCandidate
import com.roinur.saucetracker.feature.suggestions.suggestionTagTypeWeight
import com.roinur.saucetracker.feature.suggestions.suggestionWeightForRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionScoringTest {

    @Test
    fun `rating zero is neutral and not treated as dislike`() {
        assertEquals(0f, suggestionWeightForRating(rating = 0, isRead = false))
        assertEquals(1f, suggestionWeightForRating(rating = 0, isRead = true))
        assertEquals(-1f, suggestionWeightForRating(rating = 1, isRead = true))
    }

    @Test
    fun `blocked tag zeroes score`() {
        val (score, rankedTags) = scoreSuggestionCandidate(
            candidateNumPages = 0,
            tags = listOf(
                GalleryTag(name = "romance", type = "tag"),
                GalleryTag(name = "forbidden", type = "tag")
            ),
            tagWeights = mapOf("romance" to 2f, "forbidden" to 3f),
            tagThemeWeights = emptyMap(),
            creatorWeights = emptyMap(),
            averageNumPages = 0f,
            numPagesDeviation = 0f,
            lengthWeight = 0f,
            blockedTags = setOf("forbidden")
        )
        assertEquals(0f, score)
        assertTrue(rankedTags.isEmpty())
    }

    @Test
    fun `filter removes imported suggestions`() {
        val suggestions = listOf(
            SuggestedEntryRow(
                code = 10,
                title = "A",
                numPages = 1,
                uploadDate = "",
                thumbnailUrl = "",
                topTags = emptyList(),
                score = 1f
            ),
            SuggestedEntryRow(
                code = 20,
                title = "B",
                numPages = 1,
                uploadDate = "",
                thumbnailUrl = "",
                topTags = emptyList(),
                score = 1f
            )
        )
        val filtered = filterOutImportedSuggestions(suggestions, setOf(20))
        assertEquals(listOf(10), filtered.map { it.code })
    }

    @Test
    fun `language and translation tags are weighted lower than normal tags`() {
        assertTrue(suggestionTagTypeWeight("tag") > suggestionTagTypeWeight("language"))
        assertTrue(suggestionTagTypeWeight("tag") > suggestionTagTypeWeight("translation"))
    }
}
