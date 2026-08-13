package com.example.saucetracker.data.repository

import com.example.saucetracker.SuggestionApiClient

internal class SuggestionsRepository(
    private val remote: SuggestionApiClient
) {
    fun searchCodes(query: String, page: Int): List<Int> = remote.searchCodes(query, page)

    fun routeCodes(routeType: String, routeName: String, pages: Int = 1): List<Int> =
        remote.fetchDirectRouteCodes(routeType, routeName, pages)
}
