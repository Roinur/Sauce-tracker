package com.roinur.saucetracker.data.source

import android.net.Uri
import android.text.Html
import com.roinur.saucetracker.GalleryFetchException
import com.roinur.saucetracker.GalleryNotFoundException
import com.roinur.saucetracker.core.network.HttpClientFactory
import com.roinur.saucetracker.core.network.HttpClientProfile
import com.roinur.saucetracker.data.remote.GalleryApi
import com.roinur.saucetracker.data.remote.GalleryData
import com.roinur.saucetracker.data.remote.GalleryJsonParser
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class NhentaiSourceAdapter(
    private val galleryApi: GalleryApi = GalleryApi()
) : SourceAdapter {
    override val id = SourceId("nhentai")
    override val displayName: String = "NHentai"
    override val contractVersion: Int = 1
    override val capabilities: Set<SourceCapability> = setOf(
        SourceCapability.SEARCH,
        SourceCapability.BROWSE,
        SourceCapability.DETAIL,
        SourceCapability.IMPORT,
        SourceCapability.METADATA_REFRESH,
        SourceCapability.READER,
        SourceCapability.DOWNLOADS,
        SourceCapability.SUBSCRIPTIONS,
        SourceCapability.CREATOR_NAVIGATION,
        SourceCapability.POPULAR_SORT,
        SourceCapability.RECENT_SORT,
        SourceCapability.COMMENTS,
        SourceCapability.PAGE_OVERVIEW
    )
    private val client = HttpClientFactory.create(HttpClientProfile.BROWSER)

    override fun recognizes(input: String): Boolean =
        normalizeRemoteId(input) != null || input.contains("nhentai.net", ignoreCase = true)

    override fun normalizeRemoteId(input: String): String? {
        val trimmed = input.trim()
        val fromUrl = Regex("(?i)nhentai\\.net/(?:g|api/gallery)/(\\d{1,8})").find(trimmed)?.groupValues?.get(1)
        val candidate = fromUrl ?: Regex("^#?(\\d{1,8})$").matchEntire(trimmed)?.groupValues?.get(1)
        return candidate?.toIntOrNull()?.takeIf { it > 0 }?.toString()
    }

    override fun canonicalUrl(remoteId: String): String = "https://nhentai.net/g/${requireCode(remoteId)}/"

    override fun fetchEntry(remoteId: String): SourceEntry = try {
        galleryApi.fetchGallery(requireCode(remoteId)).toSourceEntry()
    } catch (error: GalleryNotFoundException) {
        throw SourceException(SourceFailureKind.NOT_FOUND, id, error.message ?: "Gallery not found.", cause = error)
    } catch (error: GalleryFetchException) {
        throw SourceException(SourceFailureKind.TEMPORARY, id, error.message ?: "Could not fetch gallery.", cause = error)
    }

    override fun search(query: SourceQuery, offset: Int, limit: Int): SourceSearchPage {
        val safeLimit = limit.coerceIn(1, 25)
        val safeOffset = offset.coerceAtLeast(0)
        val sourceTerms = query.terms.filter { it.field == SourceQueryField.SOURCE }
        if (sourceTerms.any { (!it.excluded && normalizeSourceText(it.value) != id.value) || (it.excluded && normalizeSourceText(it.value) == id.value) } ||
            query.terms.any { it.field == SourceQueryField.STATUS }) {
            return SourceSearchPage(id, emptyList(), safeOffset, safeLimit, 0, false)
        }
        val page = (safeOffset / safeLimit) + 1
        val translated = buildList {
            if (query.freeText.isNotBlank()) add(query.freeText)
            query.terms.forEach { term ->
                if (term.field == SourceQueryField.SOURCE) return@forEach
                val field = when (term.field) {
                    SourceQueryField.TAG -> "tag"
                    SourceQueryField.ARTIST, SourceQueryField.AUTHOR -> "artist"
                    SourceQueryField.GROUP -> "group"
                    SourceQueryField.CHARACTER -> "character"
                    SourceQueryField.PARODY -> "parody"
                    SourceQueryField.LANGUAGE -> "language"
                    SourceQueryField.CATEGORY -> "category"
                    SourceQueryField.STATUS -> return@forEach
                    else -> return@forEach
                }
                val value = if (term.field == SourceQueryField.LANGUAGE && term.value.matches(Regex("[a-z]{2,3}(?:-[a-z0-9]{2,8})?"))) mangaDexLanguageDisplayName(term.value) else term.value
                add("${if (term.excluded) "-" else ""}$field:\"$value\"")
            }
        }.joinToString(" ")
        try {
            return searchV2(query, translated, page, safeOffset, safeLimit)
        } catch (error: SourceException) {
            if (error.kind == SourceFailureKind.OFFLINE || error.kind == SourceFailureKind.RATE_LIMITED) throw error
        }
        val url = "https://nhentai.net/api/galleries/search?query=${Uri.encode(translated)}&page=$page"
        val request = browserRequest(url, "application/json")
        try {
            client.newCall(request).execute().use { response ->
                if (response.code == 429) throw SourceException(SourceFailureKind.RATE_LIMITED, id, "NHentai rate limit reached.")
                if (response.code == 403) {
                    return searchHtml(translated, query.sort, page, safeOffset, safeLimit)
                }
                if (!response.isSuccessful) throw SourceException(SourceFailureKind.TEMPORARY, id, "NHentai returned HTTP ${response.code}.")
                val root = JSONObject(response.body?.string().orEmpty())
                val rows = root.optJSONArray("result")
                val parsed = buildList {
                    if (rows != null) for (index in 0 until rows.length()) {
                        val raw = rows.optJSONObject(index) ?: continue
                        val code = raw.optInt("id", 0)
                        if (code > 0) add(GalleryJsonParser.parse(code, raw, embedded = true).toSourceEntry())
                    }
                }.take(safeLimit)
                val pages = root.optInt("num_pages", page)
                return SourceSearchPage(id, parsed, offset.coerceAtLeast(0), safeLimit, null, page < pages)
            }
        } catch (error: SourceException) {
            throw error
        } catch (error: IOException) {
            throw SourceException(SourceFailureKind.OFFLINE, id, "Could not reach NHentai.", cause = error)
        } catch (error: Exception) {
            throw SourceException(SourceFailureKind.INVALID_RESPONSE, id, "NHentai returned an invalid response.", cause = error)
        }
    }

    private fun searchV2(
        query: SourceQuery,
        searchable: String,
        page: Int,
        offset: Int,
        limit: Int
    ): SourceSearchPage {
        val sort = if (query.sort == SourceSortMode.POPULAR) "popular" else "date"
        val url = if (searchable.isBlank()) {
            "https://nhentai.net/api/v2/galleries?sort=$sort&page=$page&per_page=$limit"
        } else {
            "https://nhentai.net/api/v2/search?query=${Uri.encode(searchable)}&sort=$sort&page=$page&per_page=$limit"
        }
        try {
            client.newCall(browserRequest(url, "application/json")).execute().use { response ->
                if (response.code == 429) throw SourceException(SourceFailureKind.RATE_LIMITED, id, "NHentai rate limit reached.")
                if (!response.isSuccessful) throw SourceException(SourceFailureKind.TEMPORARY, id, "NHentai v2 returned HTTP ${response.code}.")
                val root = JSONObject(response.body?.string().orEmpty())
                val rows = root.optJSONArray("result")
                val entries = buildList {
                    if (rows != null) for (index in 0 until rows.length()) {
                        val raw = rows.optJSONObject(index) ?: continue
                        val code = raw.optInt("id", 0)
                        if (code <= 0) continue
                        val mediaId = raw.optString("media_id", "").trim()
                        val thumbnailPath = raw.optString("thumbnail", "").trim()
                            .replace(".webp.webp", ".webp", ignoreCase = true)
                        val english = raw.optString("english_title", "").trim().takeUnless { it.equals("null", true) }.orEmpty()
                        val japanese = raw.optString("japanese_title", "").trim().takeUnless { it.equals("null", true) }.orEmpty()
                        add(
                            SourceEntry(
                                key = SourceEntryKey(id, code.toString()),
                                title = english.ifBlank { japanese }.ifBlank { "Gallery $code" },
                                alternateTitles = listOf(japanese).filter { it.isNotBlank() && !it.equals(english, true) },
                                canonicalUrl = canonicalUrl(code.toString()),
                                thumbnailUrl = when {
                                    thumbnailPath.startsWith("http") -> thumbnailPath
                                    thumbnailPath.isNotBlank() -> "https://t.nhentai.net/${thumbnailPath.trimStart('/')}"
                                    mediaId.isNotBlank() -> "https://t.nhentai.net/galleries/$mediaId/thumb.webp"
                                    else -> ""
                                },
                                unitCount = raw.optInt("num_pages", 0).coerceAtLeast(0),
                                unitLabel = "pages",
                                sourcePayload = raw.toString()
                            )
                        )
                    }
                }
                val total = root.optInt("total", -1).takeIf { it >= 0 }
                val pages = root.optInt("num_pages", page)
                return SourceSearchPage(id, entries, offset, limit, total, page < pages)
            }
        } catch (error: SourceException) {
            throw error
        } catch (error: IOException) {
            throw SourceException(SourceFailureKind.OFFLINE, id, "Could not reach NHentai v2.", cause = error)
        } catch (error: Exception) {
            throw SourceException(SourceFailureKind.INVALID_RESPONSE, id, "NHentai v2 returned an invalid response.", cause = error)
        }
    }

    /**
     * NHentai intermittently rejects its JSON search endpoint while the same public browser
     * page remains available. The mobile browser already relies on that page, so Bridge and
     * every other SourceAdapter consumer get the same resilient fallback here.
     */
    private fun searchHtml(
        translated: String,
        sort: SourceSortMode,
        page: Int,
        offset: Int,
        limit: Int
    ): SourceSearchPage {
        val sortPart = if (sort == SourceSortMode.POPULAR) "&sort=popular" else ""
        val url = if (translated.isBlank() && sort == SourceSortMode.RECENT && page == 1) {
            "https://nhentai.net/"
        } else {
            "https://nhentai.net/search/?q=${Uri.encode(translated)}$sortPart&page=$page"
        }
        client.newCall(browserRequest(url, "text/html,application/xhtml+xml,application/xml")).execute().use { response ->
            if (response.code == 429) throw SourceException(SourceFailureKind.RATE_LIMITED, id, "NHentai rate limit reached.")
            if (!response.isSuccessful) {
                throw SourceException(SourceFailureKind.TEMPORARY, id, "NHentai browser returned HTTP ${response.code}.")
            }
            val html = response.body?.string().orEmpty()
            val entries = parseBrowserEntries(html).take(limit)
            val hasMore = entries.isNotEmpty() && (
                html.contains("?page=${page + 1}") ||
                    html.contains("&page=${page + 1}") ||
                    entries.size >= limit
                )
            return SourceSearchPage(id, entries, offset, limit, null, hasMore)
        }
    }

    private fun parseBrowserEntries(html: String): List<SourceEntry> {
        val codeRegex = Regex("""href="/g/(\d{1,8})/"""", RegexOption.IGNORE_CASE)
        val thumbRegex = Regex(
            """(?:data-src|src)="([^"]*?/galleries/(\d+)/(?:thumb|cover)\.([a-z0-9]+)[^"]*)""",
            RegexOption.IGNORE_CASE
        )
        val captionRegex = Regex(
            """<div\s+class="caption">\s*(.*?)\s*</div>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val seen = linkedSetOf<Int>()
        return buildList {
            codeRegex.findAll(html).forEach { match ->
                val code = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return@forEach
                if (!seen.add(code)) return@forEach
                val start = match.range.first.coerceAtLeast(0)
                val window = html.substring(start, (start + 1700).coerceAtMost(html.length))
                val thumb = thumbRegex.find(window)
                val mediaId = thumb?.groupValues?.getOrNull(2)?.toLongOrNull() ?: 0L
                val extension = when (thumb?.groupValues?.getOrNull(3)?.lowercase()) {
                    "png" -> "png"
                    "webp" -> "webp"
                    "gif" -> "gif"
                    else -> "jpg"
                }
                val captionRaw = captionRegex.find(window)?.groupValues?.getOrNull(1).orEmpty()
                val title = Html.fromHtml(captionRaw, Html.FROM_HTML_MODE_LEGACY)
                    .toString().replace(Regex("\\s+"), " ").trim()
                add(
                    SourceEntry(
                        key = SourceEntryKey(id, code.toString()),
                        title = title.ifBlank { "Gallery $code" },
                        canonicalUrl = canonicalUrl(code.toString()),
                        thumbnailUrl = if (mediaId > 0) {
                            "https://t.nhentai.net/galleries/$mediaId/cover.$extension"
                        } else {
                            thumb?.groupValues?.getOrNull(1).orEmpty().let { raw ->
                                when {
                                    raw.startsWith("//") -> "https:$raw"
                                    raw.startsWith("/") -> "https://t.nhentai.net$raw"
                                    else -> raw
                                }
                            }
                        },
                        unitLabel = "pages"
                    )
                )
            }
        }
    }

    private fun browserRequest(url: String, accept: String): Request {
        val builder = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
            )
            .header("Accept", accept)
            .header("Referer", "https://nhentai.net/")
            .header("Accept-Language", "en-US,en;q=0.9")
        if (accept == "application/json") builder.header("Origin", "https://nhentai.net")
        return builder.build()
    }

    private fun requireCode(remoteId: String): Int = remoteId.trim().toIntOrNull()?.takeIf { it > 0 }
        ?: throw SourceException(SourceFailureKind.UNSUPPORTED, id, "Invalid NHentai code.")

    private fun GalleryData.toSourceEntry(): SourceEntry = SourceEntry(
        key = SourceEntryKey(id, code.toString()),
        title = title,
        alternateTitles = listOf(subtitle).filter(String::isNotBlank),
        canonicalUrl = sourceUrl.ifBlank { canonicalUrl(code.toString()) },
        thumbnailUrl = if (mediaId > 0L && coverExt.isNotBlank()) "https://t.nhentai.net/galleries/$mediaId/cover.$coverExt" else "",
        unitCount = numPages,
        unitLabel = "pages",
        publishedAt = uploadDate,
        tags = tags.map { SourceTag(it.name, it.type) }
    )
}
