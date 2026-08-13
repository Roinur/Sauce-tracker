package com.example.saucetracker.data.remote

import com.example.saucetracker.core.network.executeWebsiteRequestWithRetry
import com.example.saucetracker.core.network.invalidGalleryResponseMessage
import com.example.saucetracker.core.network.websiteHttpFailure

import android.net.Uri
import android.text.Html
import com.example.saucetracker.*
import com.example.saucetracker.core.network.HttpClientFactory
import com.example.saucetracker.core.network.HttpClientProfile
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import kotlin.math.roundToInt

class GalleryApi {
    private val client: OkHttpClient = HttpClientFactory.create(HttpClientProfile.GALLERY_METADATA)

    fun fetchGallery(code: Int): GalleryData {
        if (code <= 0) {
            throw IllegalArgumentException("Code must be a positive integer.")
        }
        val apiResult = runCatching { fetchGalleryFromApi(code) }
        apiResult.getOrNull()?.let { return it }

        val htmlResult = runCatching { requestGalleryHtml(code) }
        htmlResult.getOrNull()?.let { html ->
            GalleryHtmlParser.parseGallery(code, html)?.let { return it }
        }

        val apiError = apiResult.exceptionOrNull()
        val htmlError = htmlResult.exceptionOrNull()
        when {
            apiError is GalleryNotFoundException && htmlError == null -> throw GalleryFetchException("Could not parse gallery HTML for code $code.")
            apiError is GalleryNotFoundException && htmlError is GalleryNotFoundException -> throw apiError
            apiError is GalleryFetchException -> throw apiError
            htmlError is GalleryNotFoundException -> throw htmlError
            htmlError is GalleryFetchException -> throw htmlError
            else -> throw GalleryFetchException("Could not fetch code $code.")
        }
    }

    private fun fetchGalleryFromApi(code: Int): GalleryData {
        val request = Request.Builder()
            .url(GalleryUrls.api(code))
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
            )
            .header("Accept", "application/json")
            .build()

        val response = executeWebsiteRequestWithRetry(client, request, "fetching code $code")

        response.use { rsp ->
            if (rsp.code == 404) {
                throw GalleryNotFoundException("Code $code does not exist on nhentai.")
            }
            if (!rsp.isSuccessful) {
                throw websiteHttpFailure("fetching code $code", rsp.code)
            }

            val contentType = rsp.header("Content-Type")
            val bodyText = rsp.body?.string()
                ?: throw GalleryFetchException("The website returned an empty response while fetching code $code.")

            val json = try {
                JSONObject(bodyText)
            } catch (_: Exception) {
                throw GalleryFetchException(invalidGalleryResponseMessage(bodyText, contentType))
            }

            return GalleryJsonParser.parse(code, json)
        }
    }

    private fun requestGalleryHtml(code: Int): String {
        val request = Request.Builder()
            .url(GalleryUrls.gallery(code))
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
            )
            .header("Accept", "text/html,application/xhtml+xml,application/xml")
            .build()

        val response = executeWebsiteRequestWithRetry(client, request, "loading the gallery page for code $code")

        response.use { rsp ->
            if (rsp.code == 404) {
                throw GalleryNotFoundException("Code $code does not exist on nhentai.")
            }
            if (!rsp.isSuccessful) {
                throw websiteHttpFailure("loading the gallery page for code $code", rsp.code)
            }
            return rsp.body?.string()
                ?: throw GalleryFetchException("The website returned an empty gallery page for code $code.")
        }
    }

    fun resolveCreatorByName(nameInput: String): CreatorLink? {
        val typedInput = GalleryHtmlParser.parseTypedCreatorInput(nameInput)
        val rawName = typedInput?.second ?: nameInput
        val forcedType = typedInput?.first

        val displayName = GalleryHtmlParser.parseCreatorSlug(rawName)
        if (displayName.isBlank()) return null

        val slugCandidates = GalleryHtmlParser.buildCreatorSlugCandidates(rawName)
        if (slugCandidates.isEmpty()) return null

        val creatorTypes = if (forcedType != null) {
            listOf(forcedType)
        } else {
            listOf("artist", "group")
        }

        val seen = linkedSetOf<String>()
        for (creatorType in creatorTypes) {
            for (slug in slugCandidates) {
                val key = "$creatorType/${slug.lowercase(Locale.US)}"
                if (!seen.add(key)) continue
                val resolved = probeCreatorLink(creatorType, slug)
                if (resolved != null) {
                    return resolved
                }
            }
        }

        for (creatorType in creatorTypes) {
            val resolvedFromApi = probeCreatorBySearchApi(
                creatorType = creatorType,
                displayName = displayName,
                slugCandidates = slugCandidates
            )
            if (resolvedFromApi != null) {
                return resolvedFromApi
            }
        }

        return null
    }

    private fun probeCreatorLink(creatorType: String, slug: String): CreatorLink? {
        val cleanSlug = slug.trim().trim('/')
        if (cleanSlug.isBlank()) return null

        val encodedSlug = Uri.encode(cleanSlug)
        val url = "https://nhentai.net/$creatorType/$encodedSlug/"
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
            )
            .header("Accept", "text/html,application/xhtml+xml,application/xml")
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (_: IOException) {
            return null
        }

        response.use { rsp ->
            if (rsp.code == 404 || !rsp.isSuccessful) return null

            val resolvedFromFinalUrl = GalleryHtmlParser.parseCreatorLink(rsp.request.url.toString())
            if (resolvedFromFinalUrl != null) {
                return resolvedFromFinalUrl
            }

            val name = GalleryHtmlParser.parseCreatorSlug(cleanSlug)
            if (name.isBlank()) return null
            return CreatorLink(
                type = creatorType,
                name = name,
                sourceUrl = "https://nhentai.net/$creatorType/$cleanSlug/"
            )
        }
    }

    private fun probeCreatorBySearchApi(
        creatorType: String,
        displayName: String,
        slugCandidates: List<String>
    ): CreatorLink? {
        val normalizedTarget = normalizeTagName(displayName)
        val queryCandidates = linkedSetOf<String>()

        queryCandidates += "$creatorType:\"$displayName\""
        slugCandidates.forEach { slug ->
            val slugName = GalleryHtmlParser.parseCreatorSlug(slug)
            if (slugName.isNotBlank()) {
                queryCandidates += "$creatorType:\"$slugName\""
                queryCandidates += "$creatorType:$slugName"
            }
            queryCandidates += "$creatorType:$slug"
        }

        queryCandidates.take(10).forEach { query ->
            val result = queryCreatorBySearchApi(creatorType, query, normalizedTarget)
            if (result != null) return result
        }

        return null
    }

    private fun queryCreatorBySearchApi(
        creatorType: String,
        query: String,
        normalizedTarget: String
    ): CreatorLink? {
        val url = "https://nhentai.net/api/galleries/search?query=${Uri.encode(query)}&page=1"
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
            )
            .header("Accept", "application/json")
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (_: IOException) {
            return null
        }

        response.use { rsp ->
            if (!rsp.isSuccessful) return null
            val bodyText = rsp.body?.string() ?: return null
            val payload = runCatching { JSONObject(bodyText) }.getOrNull() ?: return null
            val resultArray = payload.optJSONArray("result") ?: return null

            var bestLink: CreatorLink? = null
            var bestScore = 0
            for (index in 0 until resultArray.length()) {
                val gallery = resultArray.optJSONObject(index) ?: continue
                val tags = gallery.optJSONArray("tags") ?: continue
                for (tagIdx in 0 until tags.length()) {
                    val tag = tags.optJSONObject(tagIdx) ?: continue
                    val tagType = tag.optString("type", "").trim().lowercase(Locale.US)
                    if (tagType != creatorType) continue
                    val candidateName = tag.optString("name", "").trim()
                    if (candidateName.isBlank()) continue

                    val candidateNorm = normalizeTagName(candidateName)
                    val score = GalleryHtmlParser.creatorMatchScore(
                        targetNormalized = normalizedTarget,
                        candidateNormalized = candidateNorm
                    )
                    if (score > bestScore) {
                        bestScore = score
                        val slug = GalleryHtmlParser.toCreatorUrlSlug(candidateName)
                        val encodedSlug = Uri.encode(slug)
                        bestLink = CreatorLink(
                            type = creatorType,
                            name = candidateName,
                            sourceUrl = "https://nhentai.net/$creatorType/$encodedSlug/"
                        )
                        if (bestScore >= 3) {
                            return bestLink
                        }
                    }
                }
            }
            return bestLink
        }
    }

    fun fetchAllPopularTags(): PopularTagFetchResult {
        val deduped = linkedMapOf<Pair<String, String>, PopularTagSeed>()
        var pagesFetched = 0

        for (page in 1..POPULAR_TAG_FETCH_MAX_PAGES) {
            val pageRows = fetchPopularTagsPage(page)
            if (pageRows.isEmpty()) break
            pagesFetched = page
            pageRows.forEach { row ->
                val normalized = normalizeTagName(row.name)
                if (normalized.isBlank()) return@forEach
                val key = normalized to row.type
                val existing = deduped[key]
                if (existing == null || row.count > existing.count) {
                    deduped[key] = row.copy(name = row.name.trim())
                }
            }
        }

        return PopularTagFetchResult(
            tags = deduped.values.toList(),
            pagesFetched = pagesFetched
        )
    }

    private fun fetchPopularTagsPage(page: Int): List<PopularTagSeed> {
        if (page <= 0) return emptyList()
        val request = Request.Builder()
            .url(GalleryUrls.popularTags(page))
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
            )
            .header("Accept", "text/html,application/xhtml+xml,application/xml")
            .build()

        val response = executeWebsiteRequestWithRetry(client, request, "loading popular tags page $page")

        response.use { rsp ->
            if (!rsp.isSuccessful) {
                if (page > 1 && (rsp.code == 404 || rsp.code == 410)) {
                    return emptyList()
                }
                throw websiteHttpFailure("loading popular tags page $page", rsp.code)
            }
            val bodyText = rsp.body?.string().orEmpty()
            if (bodyText.isBlank()) return emptyList()
            return GalleryHtmlParser.parsePopularTags(bodyText)
        }
    }

}

