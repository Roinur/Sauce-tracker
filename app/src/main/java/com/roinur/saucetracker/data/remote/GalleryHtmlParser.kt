package com.roinur.saucetracker.data.remote

import android.net.Uri
import android.text.Html
import com.roinur.saucetracker.CreatorLink
import com.roinur.saucetracker.HTML_TAG_PATTERN
import com.roinur.saucetracker.POPULAR_TAG_ANCHOR_PATTERN
import com.roinur.saucetracker.POPULAR_TAG_COUNT_SPAN_PATTERN
import com.roinur.saucetracker.POPULAR_TAG_NAME_SPAN_PATTERN
import com.roinur.saucetracker.UPLOAD_DATE_FORMAT
import com.roinur.saucetracker.normalizeTagName
import com.roinur.saucetracker.parseCoverExtension
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import kotlin.math.roundToInt

internal object GalleryHtmlParser {
    private val typedCreatorPattern = Regex("(?i)^(artist|group)\\s*:\\s*(.+)$")
    private val creatorLinkPattern = Regex(
        "(?i)(?:https?://)?(?:www\\.)?nhentai\\.net/(artist|group)/([^/\\s?#]+)(?:/)?(?:[?#][^\\s]*)?"
    )
    private const val trailingPunctuation = ".,;:!?)]}'\""

    fun parseTypedCreatorInput(raw: String): Pair<String, String>? {
        val match = typedCreatorPattern.matchEntire(raw.trim()) ?: return null
        val type = match.groupValues.getOrNull(1).orEmpty().trim().lowercase(Locale.US)
        if (type != "artist" && type != "group") return null
        val value = match.groupValues.getOrNull(2).orEmpty().trim()
        return value.takeIf { it.isNotBlank() }?.let { type to it }
    }

    fun parseCreatorSlug(rawSlug: String): String {
        var cleaned = rawSlug.trim()
        while (cleaned.isNotEmpty() && trailingPunctuation.contains(cleaned.last())) {
            cleaned = cleaned.dropLast(1)
        }
        if (cleaned.isBlank()) return ""
        return Uri.decode(cleaned)
            .replace("+", " ")
            .replace("-", " ")
            .replace("_", " ")
            .trim()
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
            .joinToString(" ")
    }

    fun buildCreatorSlugCandidates(rawInput: String): List<String> {
        var cleaned = rawInput.trim().trim('/')
        while (cleaned.isNotEmpty() && trailingPunctuation.contains(cleaned.last())) {
            cleaned = cleaned.dropLast(1)
        }
        if (cleaned.isBlank()) return emptyList()
        val tokens = parseCreatorSlug(cleaned).split(Regex("\\s+")).filter(String::isNotBlank)
        return linkedSetOf<String>().apply {
            fun addIfPresent(value: String) {
                value.trim().trim('/').takeIf(String::isNotBlank)?.let(::add)
            }
            addIfPresent(cleaned)
            addIfPresent(cleaned.lowercase(Locale.US))
            if (tokens.isNotEmpty()) {
                addIfPresent(tokens.joinToString("-"))
                addIfPresent(tokens.joinToString("_"))
                addIfPresent(tokens.joinToString("+"))
            }
        }.toList()
    }

    fun parseCreatorLink(raw: String): CreatorLink? {
        val match = creatorLinkPattern.matchEntire(raw.trim()) ?: return null
        val type = match.groupValues.getOrNull(1).orEmpty().trim().lowercase(Locale.US)
        if (type != "artist" && type != "group") return null
        var slug = match.groupValues.getOrNull(2).orEmpty().trim()
        while (slug.isNotEmpty() && trailingPunctuation.contains(slug.last())) slug = slug.dropLast(1)
        val name = parseCreatorSlug(slug)
        if (slug.isBlank() || name.isBlank()) return null
        return CreatorLink(type, name, "https://nhentai.net/$type/$slug/")
    }

    fun creatorMatchScore(targetNormalized: String, candidateNormalized: String): Int {
        if (targetNormalized.isBlank() || candidateNormalized.isBlank()) return 0
        if (targetNormalized == candidateNormalized) return 3
        if (candidateNormalized.contains(targetNormalized) || targetNormalized.contains(candidateNormalized)) return 2
        val targetTokens = targetNormalized.split(Regex("\\s+")).filter(String::isNotBlank)
        val candidateTokens = candidateNormalized.split(Regex("\\s+")).filter(String::isNotBlank).toSet()
        return if (targetTokens.isNotEmpty() && targetTokens.all(candidateTokens::contains)) 1 else 0
    }

    fun toCreatorUrlSlug(name: String): String = parseCreatorSlug(name)
        .takeIf(String::isNotBlank)
        ?.replace(Regex("\\s+"), "-")
        ?.lowercase(Locale.US)
        .orEmpty()

    fun parseGallery(code: Int, html: String): GalleryData? {
        if (html.isBlank()) return null
        extractEmbeddedPayload(html, code)?.let { payload ->
            return GalleryJsonParser.parse(code, payload, embedded = true).takeIf { it.mediaId > 0L }
        }

        val titleBlockRegex = Regex(
            """<div[^>]+id=["']info["'][^>]*>.*?<h1[^>]*>(.*?)</h1>.*?(?:<h2[^>]*>(.*?)</h2>)?""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val metaTitleRegex = Regex(
            """<meta[^>]+property=["']og:title["'][^>]+content=["'](.*?)["'][^>]*>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val headingRegex = Regex(
            """<h[1-3][^>]*>(.*?)</h[1-3]>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val mediaIdRegex = Regex("""["']media_id["']\s*:\s*["']?(\d+)["']?""", RegexOption.IGNORE_CASE)
        val uploadTimestampRegex = Regex("""["']upload_date["']\s*:\s*(\d{5,})""", RegexOption.IGNORE_CASE)
        val timeRegex = Regex("""<time[^>]+datetime=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val pageCountRegexes = listOf(
            Regex("""["']num_pages["']\s*:\s*(\d{1,5})""", RegexOption.IGNORE_CASE),
            Regex("""Pages?</[^>]*>\s*<[^>]*class=["'][^"']*name[^"']*["'][^>]*>(\d{1,5})</""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
            Regex(""">(\d{1,5})\s+pages?<""", RegexOption.IGNORE_CASE)
        )
        val thumbRegex = Regex(
            """(?:https?:)?//[^"' ]*/galleries/(\d+)/(?:cover|(\d+)t)\.([a-z0-9]+)""",
            RegexOption.IGNORE_CASE
        )
        val tagLinkRegex = Regex(
            """<a[^>]+href=["']/(tag|artist|group|parody|character|language|category)/([^"']+)["'][^>]*>(.*?)</a>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val nameRegex = Regex(
            """class=["'][^"']*name[^"']*["'][^>]*>(.*?)</""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

        val titleBlock = titleBlockRegex.find(html)
        val title = cleanSnippet(
            titleBlock?.groupValues?.getOrNull(1)
                ?: metaTitleRegex.find(html)?.groupValues?.getOrNull(1)
                ?: headingRegex.find(html)?.groupValues?.getOrNull(1).orEmpty()
        ).removeSuffix(" | nhentai").removeSuffix(" | nHentai").ifBlank { "Gallery $code" }
        val subtitle = cleanSnippet(titleBlock?.groupValues?.getOrNull(2).orEmpty())
            .takeIf { it.isNotBlank() && !it.equals(title, ignoreCase = true) }
            .orEmpty()

        val thumbMatches = thumbRegex.findAll(html).toList()
        val mediaId = mediaIdRegex.find(html)?.groupValues?.getOrNull(1)?.toLongOrNull()?.coerceAtLeast(0L)
            ?: thumbMatches.firstNotNullOfOrNull { it.groupValues.getOrNull(1)?.toLongOrNull()?.coerceAtLeast(0L) }
            ?: 0L
        val coverExt = thumbMatches.firstOrNull { it.groupValues.getOrNull(2).isNullOrBlank() }
            ?.groupValues?.getOrNull(3).orEmpty().let(::parseCoverExtension)
        val numPages = pageCountRegexes.firstNotNullOfOrNull { regex ->
            regex.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
        }?.coerceAtLeast(0) ?: thumbMatches.maxOfOrNull {
            it.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
        } ?: 0
        val uploadDate = uploadTimestampRegex.find(html)?.groupValues?.getOrNull(1)?.toLongOrNull()?.let {
            Instant.ofEpochSecond(it).atZone(ZoneOffset.UTC).toLocalDate().format(UPLOAD_DATE_FORMAT)
        } ?: timeRegex.find(html)?.groupValues?.getOrNull(1)?.let(::parseUploadDate).orEmpty()

        val tags = buildList {
            tagLinkRegex.findAll(html).forEach { match ->
                val type = match.groupValues.getOrNull(1).orEmpty().trim().lowercase(Locale.US).ifBlank { "tag" }
                val inner = match.groupValues.getOrNull(3).orEmpty()
                val name = cleanSnippet(nameRegex.find(inner)?.groupValues?.getOrNull(1).orEmpty())
                    .ifBlank { cleanSnippet(inner) }
                if (name.isNotBlank()) add(GalleryTag(name, type))
            }
        }.distinctBy { "${it.type}:${it.name.lowercase(Locale.US)}" }

        if (title.isBlank() || mediaId <= 0L) return null
        return GalleryData(
            code = code,
            title = title,
            subtitle = subtitle,
            numPages = numPages,
            uploadDate = uploadDate,
            sourceUrl = GalleryUrls.gallery(code),
            mediaId = mediaId,
            coverExt = coverExt,
            tags = tags
        )
    }

    fun parsePopularTags(html: String): List<com.roinur.saucetracker.PopularTagSeed> {
        val rows = mutableListOf<com.roinur.saucetracker.PopularTagSeed>()
        POPULAR_TAG_ANCHOR_PATTERN.findAll(html).forEach { match ->
            val type = match.groupValues.getOrNull(2).orEmpty().trim().lowercase(Locale.US)
            if (type.isBlank()) return@forEach
            val slug = match.groupValues.getOrNull(3).orEmpty()
            val innerHtml = match.groupValues.getOrNull(4).orEmpty()
            val nameHtml = POPULAR_TAG_NAME_SPAN_PATTERN.find(innerHtml)?.groupValues?.getOrNull(1).orEmpty()
            val countHtml = POPULAR_TAG_COUNT_SPAN_PATTERN.find(innerHtml)?.groupValues?.getOrNull(1).orEmpty()
            val name = decodeSnippet(nameHtml).ifBlank { slugToName(slug) }
            if (name.isNotBlank()) {
                rows += com.roinur.saucetracker.PopularTagSeed(name, type, parseCompactCount(countHtml))
            }
        }
        return rows.distinctBy { normalizeTagName(it.name) to it.type }
    }

    private fun extractEmbeddedPayload(html: String, code: Int): JSONObject? {
        val scriptRegex = Regex(
            """<script[^>]+data-sveltekit-fetched[^>]+data-url=["']/api/v2/galleries/$code[^"']*["'][^>]*>(.*?)</script>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val scriptBody = scriptRegex.find(html)?.groupValues?.getOrNull(1).orEmpty().trim()
        if (scriptBody.isBlank()) return null
        val envelope = runCatching { JSONObject(scriptBody) }.getOrNull() ?: return null
        val payload = envelope.optString("body", "").trim()
        return payload.takeIf { it.isNotBlank() }?.let { runCatching { JSONObject(it) }.getOrNull() }
    }

    private fun cleanSnippet(raw: String): String = if (raw.isBlank()) "" else {
        Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString().replace(Regex("\\s+"), " ").trim()
    }

    private fun parseUploadDate(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""
        Regex("""(\d{4}-\d{2}-\d{2})""").find(trimmed)?.groupValues?.getOrNull(1)?.let { return it }
        return trimmed.toLongOrNull()?.let {
            Instant.ofEpochSecond(it).atZone(ZoneOffset.UTC).toLocalDate().format(UPLOAD_DATE_FORMAT)
        }.orEmpty()
    }

    private fun decodeSnippet(raw: String): String {
        val stripped = raw.replace(HTML_TAG_PATTERN, " ")
        return Html.fromHtml(stripped, Html.FROM_HTML_MODE_LEGACY)
            .toString().replace(Regex("\\s+"), " ").trim()
    }

    private fun slugToName(slug: String): String = Uri.decode(slug.trim())
        .replace(Regex("[-_]+"), " ").replace(Regex("\\s+"), " ").trim()

    private fun parseCompactCount(raw: String): Int {
        val cleaned = decodeSnippet(raw).replace(",", "").replace(" ", "").lowercase(Locale.US).trim()
        if (cleaned.isBlank()) return 0
        val match = Regex("^(\\d+(?:\\.\\d+)?)([kmb])?\\+?$", RegexOption.IGNORE_CASE).find(cleaned)
        if (match != null) {
            val base = match.groupValues.getOrNull(1)?.toDoubleOrNull() ?: return 0
            val multiplier = when (match.groupValues.getOrNull(2).orEmpty().lowercase(Locale.US)) {
                "k" -> 1_000.0
                "m" -> 1_000_000.0
                "b" -> 1_000_000_000.0
                else -> 1.0
            }
            return (base * multiplier).roundToInt().coerceAtLeast(0)
        }
        return cleaned.filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(0) ?: 0
    }
}
