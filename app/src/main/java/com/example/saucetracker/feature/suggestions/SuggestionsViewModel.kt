package com.example.saucetracker.feature.suggestions

import com.example.saucetracker.GalleryTag
import com.example.saucetracker.SuggestedEntryRow

internal class SuggestionsViewModel {
    fun tagTypeWeight(type: String): Float = suggestionTagTypeWeight(type)

    fun ratingWeight(rating: Int, isRead: Boolean): Float =
        suggestionWeightForRating(rating, isRead)

    fun scoreCandidate(
        candidateNumPages: Int,
        tags: List<GalleryTag>,
        tagWeights: Map<String, Float>,
        tagThemeWeights: Map<String, Float>,
        creatorWeights: Map<String, Float>,
        averageNumPages: Float,
        numPagesDeviation: Float,
        lengthWeight: Float,
        blockedTags: Set<String>
    ): SuggestionScoreBreakdown = scoreSuggestionCandidate(
        candidateNumPages = candidateNumPages,
        tags = tags,
        tagWeights = tagWeights,
        tagThemeWeights = tagThemeWeights,
        creatorWeights = creatorWeights,
        averageNumPages = averageNumPages,
        numPagesDeviation = numPagesDeviation,
        lengthWeight = lengthWeight,
        blockedTags = blockedTags
    )

    fun themeWeights(
        tagWeights: Map<String, Float>,
        localTagEntryCodes: Map<String, Set<Int>>,
        themeStrength: Float
    ): Map<String, Float> = buildSuggestionTagThemeWeights(
        tagWeights = tagWeights,
        localTagEntryCodes = localTagEntryCodes,
        themeStrength = themeStrength
    )

    fun excludeImported(
        suggestions: List<SuggestedEntryRow>,
        importedCodes: Set<Int>
    ): List<SuggestedEntryRow> = filterOutImportedSuggestions(suggestions, importedCodes)
}
