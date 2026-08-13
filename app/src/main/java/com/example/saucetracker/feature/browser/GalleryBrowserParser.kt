package com.example.saucetracker.feature.browser

import com.example.saucetracker.*
import com.example.saucetracker.core.media.*
import com.example.saucetracker.core.ui.theme.AccentMode
import com.example.saucetracker.core.ui.theme.applyAccentMode
import com.example.saucetracker.core.ui.components.*
import com.example.saucetracker.data.backup.*
import com.example.saucetracker.data.database.SauceTrackerDatabase
import com.example.saucetracker.feature.slideshow.GallerySlideshowActivity

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.text.Html
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.example.saucetracker.core.diagnostics.PerformanceMetrics
import com.example.saucetracker.core.media.BitmapMemoryCache
import com.example.saucetracker.core.media.computeDHash64
import com.example.saucetracker.core.network.HttpClientFactory
import com.example.saucetracker.core.network.HttpClientProfile
import com.example.saucetracker.core.preferences.KEY_BROWSER_DUPLICATE_CHECK_MODE
import com.example.saucetracker.core.preferences.KEY_PERFORMANCE_OVERLAY_ENABLED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min


internal fun parseGallerySummary(obj: JSONObject): BrowserGallerySummary? {
    val code = obj.optInt("id", 0).takeIf { it > 0 } ?: return null
    val mediaId = parseApiMediaId(obj.opt("media_id"))
    val titleObj = obj.optJSONObject("title") ?: JSONObject()
    val (title, subtitle) = resolveBrowserGalleryTitles(
        code = code,
        titleCandidates = listOf(
        titleObj.optString("english", "").trim(),
        titleObj.optString("japanese", "").trim(),
        titleObj.optString("pretty", "").trim(),
        obj.optString("english_title", "").trim(),
        obj.optString("japanese_title", "").trim(),
        obj.optString("pretty_title", "").trim(),
        obj.opt("title")?.takeIf { it is String }?.toString()?.trim().orEmpty()
        ),
        subtitleCandidates = listOf(
        titleObj.optString("pretty", "").trim(),
        obj.optString("pretty_title", "").trim(),
        obj.optString("japanese_title", "").trim()
        )
    )
    val uploadDate = parseApiUploadDate(obj.optLong("upload_date", 0L))
    val numPages = obj.optInt("num_pages", 0).coerceAtLeast(0)
    val coverExt = parseApiImageExtension(
        obj.optJSONObject("images")
            ?.optJSONObject("cover")
            ?.optString("t", "")
    )
    return BrowserGallerySummary(
        code = code,
        title = title,
        subtitle = subtitle,
        mediaId = mediaId,
        coverExt = coverExt,
        numPages = numPages,
        uploadDate = uploadDate
    )
}

internal fun resolveBrowserGalleryTitles(
    code: Int,
    titleCandidates: List<String>,
    subtitleCandidates: List<String>
): Pair<String, String> {
    val title = titleCandidates.firstOrNull { it.isNotBlank() } ?: "Gallery $code"
    val subtitle = subtitleCandidates.firstOrNull { it.isNotBlank() && it != title }.orEmpty()
    return title to subtitle
}

internal fun parseRelatedGallerySummaries(payload: String?): List<BrowserGallerySummary> {
    if (payload.isNullOrBlank()) return emptyList()
    val array = runCatching {
        val trimmed = payload.trim()
        if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else {
            val root = JSONObject(trimmed)
            root.optJSONArray("result")
                ?: root.optJSONArray("results")
                ?: root.optJSONArray("galleries")
                ?: root.optJSONObject("data")?.let { data ->
                    data.optJSONArray("result")
                        ?: data.optJSONArray("results")
                        ?: data.optJSONArray("galleries")
                }
                ?: JSONArray()
        }
    }.getOrElse { return emptyList() }

    return buildList {
        for (index in 0 until array.length()) {
            val summary = array.optJSONObject(index)?.let(::parseGallerySummary) ?: continue
            if (none { it.code == summary.code }) add(summary)
            if (size == 5) break
        }
    }
}

internal fun parseGalleryDetail(
    obj: JSONObject,
    galleryHtml: String = "",
    commentsJson: String? = null
): BrowserGalleryDetail? {
    val summary = parseGallerySummary(obj) ?: return null

    val tagsByType = linkedMapOf<String, MutableList<String>>()
    val tagCountsByKey = linkedMapOf<String, Int>()
    val rawTags = obj.optJSONArray("tags") ?: JSONArray()
    for (idx in 0 until rawTags.length()) {
        val tagObj = rawTags.optJSONObject(idx) ?: continue
        val rawName = tagObj.optString("name", "").trim()
        if (rawName.isBlank()) continue
        val count = tagObj.optInt("count", -1).takeIf { it >= 0 }?.coerceAtLeast(0)
        val type = tagObj.optString("type", "tag")
            .trim()
            .lowercase(Locale.US)
            .ifBlank { "tag" }
        val name = if (type == "artist" || type == "group") {
            normalizeCreatorDisplayName(rawName)
        } else {
            rawName
        }
        if (name.isBlank()) continue
        tagsByType.getOrPut(type) { mutableListOf() }.add(name)
        if (count != null) {
            val key = browserTagLookupKey(type = type, name = name)
            val previous = tagCountsByKey[key]
            if (previous == null || count > previous) {
                tagCountsByKey[key] = count
            }
        }
    }
    val sortedTags = tagsByType
        .toSortedMap(compareBy<String> { if (it == "tag") 1 else 0 }.thenBy { it })
        .mapValues { it.value.distinct() }

    val pagesArray = obj.optJSONObject("images")?.optJSONArray("pages")
    val fallbackExt = summary.coverExt.ifBlank { "jpg" }
    val pageThumbs = buildList {
        for (page in 1..summary.numPages.coerceAtLeast(0)) {
            val ext = parseApiImageExtension(
                pagesArray
                    ?.optJSONObject(page - 1)
                    ?.optString("t", "")
            ).ifBlank { fallbackExt }
            add(
                BrowserPageThumb(
                    pageNumber = page,
                    thumbnailUrls = buildPageThumbnailUrls(
                        mediaId = summary.mediaId,
                        pageNumber = page,
                        preferredExt = ext
                    )
                )
            )
        }
    }
    val comments = parseGalleryCommentsFromApiJson(commentsJson).ifEmpty {
        parseGalleryCommentsFromHtml(galleryHtml)
    }

    return BrowserGalleryDetail(
        summary = summary,
        tagsByType = sortedTags,
        tagCountsByKey = tagCountsByKey.toMap(),
        pageThumbs = pageThumbs,
        comments = comments
    )
}

internal fun parseGalleryDetailFromHtml(
    html: String,
    code: Int,
    includeComments: Boolean = true
): BrowserGalleryDetail? {
    if (html.isBlank() || code <= 0) return null

    val summary = parseGallerySummaryFromHtmlDetail(html, code) ?: return null
    val (tagsByType, tagCountsByKey) = parseGalleryTagsFromHtml(html)
    val pageThumbs = parseGalleryPageThumbsFromHtml(html, summary)
    val comments = if (includeComments) parseGalleryCommentsFromHtml(html) else emptyList()

    return BrowserGalleryDetail(
        summary = summary,
        tagsByType = tagsByType,
        tagCountsByKey = tagCountsByKey,
        pageThumbs = pageThumbs,
        comments = comments
    )
}

internal fun parseGallerySummaryFromHtmlDetail(
    html: String,
    code: Int
): BrowserGallerySummary? {
    extractEmbeddedBrowserGalleryPayload(html, code)?.let { payload ->
        parseGallerySummaryFromEmbeddedPayload(code, payload)?.let { return it }
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

    val titleBlock = titleBlockRegex.find(html)
    val title = cleanHtmlText(
        titleBlock?.groupValues?.getOrNull(1)
            ?: metaTitleRegex.find(html)?.groupValues?.getOrNull(1)
            ?: headingRegex.find(html)?.groupValues?.getOrNull(1).orEmpty()
    )
        .removeSuffix(" | nhentai")
        .removeSuffix(" | nHentai")
        .ifBlank { "Gallery $code" }
    val subtitle = cleanHtmlText(titleBlock?.groupValues?.getOrNull(2).orEmpty())
        .takeIf { it.isNotBlank() && !it.equals(title, ignoreCase = true) }
        .orEmpty()

    val allThumbMatches = thumbRegex.findAll(html).toList()
    val mediaId = mediaIdRegex.find(html)?.groupValues?.getOrNull(1)?.toLongOrNull()?.coerceAtLeast(0L)
        ?: allThumbMatches.firstNotNullOfOrNull { it.groupValues.getOrNull(1)?.toLongOrNull()?.coerceAtLeast(0L) }
        ?: 0L
    val coverExt = allThumbMatches
        .firstOrNull { it.groupValues.getOrNull(2).isNullOrBlank() }
        ?.groupValues
        ?.getOrNull(3)
        .orEmpty()
        .let(::parseApiImageExtension)
    val numPages = pageCountRegexes.firstNotNullOfOrNull { regex ->
        regex.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }?.coerceAtLeast(0)
        ?: allThumbMatches.maxOfOrNull { it.groupValues.getOrNull(2)?.toIntOrNull() ?: 0 }
        ?: 0
    val uploadDate = uploadTimestampRegex.find(html)?.groupValues?.getOrNull(1)?.toLongOrNull()?.let(::parseApiUploadDate)
        ?: timeRegex.find(html)?.groupValues?.getOrNull(1)?.let(::parseHtmlUploadDate).orEmpty()

    return BrowserGallerySummary(
        code = code,
        title = title,
        subtitle = subtitle,
        mediaId = mediaId,
        coverExt = coverExt,
        numPages = numPages,
        uploadDate = uploadDate
    )
}

internal fun extractEmbeddedBrowserGalleryPayload(
    html: String,
    code: Int
): JSONObject? {
    val scriptRegex = Regex(
        """<script[^>]+data-sveltekit-fetched[^>]+data-url=["']/api/v2/galleries/$code[^"']*["'][^>]*>(.*?)</script>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    val scriptBody = scriptRegex.find(html)?.groupValues?.getOrNull(1).orEmpty().trim()
    if (scriptBody.isBlank()) return null
    val envelope = runCatching { JSONObject(scriptBody) }.getOrNull() ?: return null
    val payload = envelope.optString("body", "").trim()
    if (payload.isBlank()) return null
    return runCatching { JSONObject(payload) }.getOrNull()
}

internal fun parseGallerySummaryFromEmbeddedPayload(
    code: Int,
    payload: JSONObject
): BrowserGallerySummary? {
    val titleObj = payload.optJSONObject("title") ?: JSONObject()
    val title = listOf(
        titleObj.optString("english", "").trim(),
        titleObj.optString("japanese", "").trim(),
        titleObj.optString("pretty", "").trim()
    ).firstOrNull { it.isNotBlank() } ?: "Gallery $code"
    val subtitle = titleObj.optString("pretty", "").trim()
        .takeIf { it.isNotBlank() && !it.equals(title, ignoreCase = true) }
        .orEmpty()
    val mediaId = parseApiMediaId(payload.opt("media_id"))
    if (mediaId <= 0L) return null
    val uploadDate = payload.optLong("upload_date", 0L)
        .takeIf { it > 0L }
        ?.let(::parseApiUploadDate)
        .orEmpty()
    val coverExt = parseApiImageExtension(
        payload.optJSONObject("cover")
            ?.optString("path", "")
            ?.substringAfterLast('.', "")
    )
    return BrowserGallerySummary(
        code = code,
        title = title,
        subtitle = subtitle,
        mediaId = mediaId,
        coverExt = coverExt,
        numPages = payload.optInt("num_pages", 0).coerceAtLeast(0),
        uploadDate = uploadDate
    )
}

internal fun parseGalleryTagsFromHtml(
    html: String
): Pair<Map<String, List<String>>, Map<String, Int>> {
    val tagLinkRegex = Regex(
        """<a[^>]+href=["']/(tag|artist|group|parody|character|language|category)/([^"']+)["'][^>]*>(.*?)</a>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    val nameRegex = Regex(
        """class=["'][^"']*name[^"']*["'][^>]*>(.*?)</""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    val countRegex = Regex(
        """class=["'][^"']*count[^"']*["'][^>]*>(.*?)</""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    val tagsByType = linkedMapOf<String, MutableList<String>>()
    val tagCountsByKey = linkedMapOf<String, Int>()

    tagLinkRegex.findAll(html).forEach { match ->
        val type = normalizeBrowserRouteType(match.groupValues.getOrNull(1).orEmpty())
            .ifBlank { "tag" }
        val inner = match.groupValues.getOrNull(3).orEmpty()
        val explicitName = cleanHtmlText(
            nameRegex.find(inner)?.groupValues?.getOrNull(1).orEmpty()
        )
        val rawCountLabel = cleanHtmlText(
            countRegex.find(inner)?.groupValues?.getOrNull(1).orEmpty()
        )
        val rawName = explicitName.ifBlank {
            removeTrailingTagCount(
                displayText = cleanHtmlText(inner),
                countLabel = rawCountLabel
            )
        }
        val name = if (type == "artist" || type == "group") {
            normalizeCreatorDisplayName(rawName)
        } else {
            rawName
        }
        if (name.isBlank()) return@forEach
        tagsByType.getOrPut(type) { mutableListOf() }.add(name)

        val count = parseGalleryHtmlCompactCount(rawCountLabel)
        if (count != null) {
            val key = browserTagLookupKey(type = type, name = name)
            val previous = tagCountsByKey[key]
            if (previous == null || count > previous) {
                tagCountsByKey[key] = count
            }
        }
    }

    val sortedTags = tagsByType
        .toSortedMap(compareBy<String> { if (it == "tag") 1 else 0 }.thenBy { it })
        .mapValues { (_, values) -> values.distinct() }
    return sortedTags to tagCountsByKey.toMap()
}

internal fun removeTrailingTagCount(displayText: String, countLabel: String): String {
    val cleanedDisplay = displayText.trim()
    val cleanedCount = countLabel.trim()
    if (cleanedDisplay.isBlank() || cleanedCount.isBlank()) return cleanedDisplay
    return cleanedDisplay.replace(
        Regex(
            pattern = "(?:\\s*\\|\\s*|\\s+)${Regex.escape(cleanedCount)}\\s*$",
            option = RegexOption.IGNORE_CASE
        ),
        ""
    ).trim()
}

internal fun parseGalleryPageThumbsFromHtml(
    html: String,
    summary: BrowserGallerySummary
): List<BrowserPageThumb> {
    val thumbRegex = Regex(
        """(?:https?:)?//[^"' ]*/galleries/(\d+)/(\d+)t\.([a-z0-9]+)""",
        RegexOption.IGNORE_CASE
    )
    val matchedByPage = linkedMapOf<Int, MutableList<String>>()

    thumbRegex.findAll(html).forEach { match ->
        val mediaId = match.groupValues.getOrNull(1)?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        val pageNumber = match.groupValues.getOrNull(2)?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val ext = parseApiImageExtension(match.groupValues.getOrNull(3).orEmpty())
        if (mediaId <= 0L || pageNumber <= 0) return@forEach
        val preferredMediaId = summary.mediaId.takeIf { it > 0L }
        if (preferredMediaId != null && mediaId != preferredMediaId) return@forEach
        matchedByPage.getOrPut(pageNumber) { mutableListOf() }.addAll(
            buildPageThumbnailUrls(mediaId = mediaId, pageNumber = pageNumber, preferredExt = ext)
        )
    }

    if (matchedByPage.isNotEmpty()) {
        return matchedByPage.entries
            .sortedBy { it.key }
            .map { (pageNumber, urls) ->
                BrowserPageThumb(pageNumber = pageNumber, thumbnailUrls = urls.distinct())
            }
    }

    if (summary.mediaId <= 0L || summary.numPages <= 0) return emptyList()
    val fallbackExt = summary.coverExt.ifBlank { "jpg" }
    return buildList {
        for (page in 1..summary.numPages) {
            add(
                BrowserPageThumb(
                    pageNumber = page,
                    thumbnailUrls = buildPageThumbnailUrls(
                        mediaId = summary.mediaId,
                        pageNumber = page,
                        preferredExt = fallbackExt
                    )
                )
            )
        }
    }
}

internal fun parseGallerySummariesFromHtml(html: String): List<BrowserGallerySummary> {
    val codeRegex = Regex("""href="/g/(\d{1,8})/"""", RegexOption.IGNORE_CASE)
    val thumbRegex = Regex(
        """(?:data-src|src)="([^"]*?/galleries/(\d+)/(?:thumb|cover)\.([a-z0-9]+)[^"]*)"""",
        RegexOption.IGNORE_CASE
    )
    val captionRegex = Regex(
        """<div\s+class="caption">\s*(.*?)\s*</div>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    val out = mutableListOf<BrowserGallerySummary>()
    val seen = linkedSetOf<Int>()
    codeRegex.findAll(html).forEach { match ->
        val code = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return@forEach
        if (!seen.add(code)) return@forEach

        val start = match.range.first.coerceAtLeast(0)
        val end = (start + 1700).coerceAtMost(html.length)
        val window = html.substring(start, end)

        val thumb = thumbRegex.find(window)
        val mediaId = thumb?.groupValues?.getOrNull(2)?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        val ext = parseApiImageExtension(thumb?.groupValues?.getOrNull(3).orEmpty())
        val captionRaw = captionRegex.find(window)?.groupValues?.getOrNull(1).orEmpty()
        val caption = Html.fromHtml(captionRaw, Html.FROM_HTML_MODE_LEGACY)
            .toString()
            .replace(Regex("\\s+"), " ")
            .trim()

        out += BrowserGallerySummary(
            code = code,
            title = caption.ifBlank { "Gallery $code" },
            subtitle = "",
            mediaId = mediaId,
            coverExt = ext,
            numPages = 0,
            uploadDate = ""
        )
    }
    return out
}

internal fun parseGalleryCommentsFromApiJson(payload: String?): List<BrowserGalleryComment> {
    if (payload.isNullOrBlank()) return emptyList()
    val parsed = runCatching { JSONObject(payload) }.getOrNull()
    val array = when {
        parsed != null -> {
            parsed.optJSONArray("comments")
                ?: parsed.optJSONArray("result")
                ?: parsed.optJSONArray("data")
                ?: parsed.optJSONArray("items")
        }
        else -> runCatching { JSONArray(payload) }.getOrNull()
    } ?: return emptyList()

    val comments = mutableListOf<BrowserGalleryComment>()
    for (index in 0 until array.length()) {
        val obj = array.optJSONObject(index) ?: continue
        val author = extractCommentAuthorFromJson(obj)

        val text = listOf(
            obj.optString("body", ""),
            obj.optString("content", ""),
            obj.optString("text", ""),
            obj.optString("message", "")
        ).map { cleanHtmlText(it) }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()

        if (!isValidBrowserComment(author, text)) continue
        comments += BrowserGalleryComment(author = author, text = text)
    }
    return comments.distinctBy { "${it.author}\u0000${it.text}" }.take(80)
}

internal fun extractCommentAuthorFromJson(obj: JSONObject): String {
    fun fromObject(j: JSONObject?): String {
        if (j == null) return ""
        return listOf(
            j.optString("username", ""),
            j.optString("name", ""),
            j.optString("display_name", ""),
            j.optString("nickname", "")
        ).map { cleanHtmlText(it) }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
    }

    fun fromAny(value: Any?): String {
        return when (value) {
            is JSONObject -> fromObject(value)
            is String -> {
                val raw = value.trim()
                if (raw.isBlank()) return ""
                if (raw.startsWith("{") && raw.endsWith("}")) {
                    val parsed = runCatching { JSONObject(raw) }.getOrNull()
                    val fromJson = fromObject(parsed)
                    if (fromJson.isNotBlank()) return fromJson
                }
                val regexUsername = Regex("""["']username["']\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                val regexName = Regex("""["'](?:name|display_name|nickname)["']\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                val extracted = regexUsername.find(raw)?.groupValues?.getOrNull(1).orEmpty()
                    .ifBlank { regexName.find(raw)?.groupValues?.getOrNull(1).orEmpty() }
                if (extracted.isNotBlank()) {
                    cleanHtmlText(extracted)
                } else {
                    cleanHtmlText(raw)
                        .takeIf {
                            it.isNotBlank() &&
                                !it.startsWith("{") &&
                                !it.contains(":") &&
                                Regex("""^[\p{L}\p{N} _.'\-]{1,64}$""").matches(it)
                        }
                        .orEmpty()
                }
            }
            else -> ""
        }
    }

    val candidates = buildList {
        add(fromAny(obj.opt("poster")))
        add(fromObject(obj.optJSONObject("poster")))
        add(fromAny(obj.opt("user")))
        add(fromObject(obj.optJSONObject("user")))
        add(cleanHtmlText(obj.optString("username", "")))
        add(cleanHtmlText(obj.optString("author", "")))
        add(cleanHtmlText(obj.optString("name", "")))
    }
    return candidates.firstOrNull { it.isNotBlank() }.orEmpty()
}

private val browserCommentScriptRegex = Regex(
    """<script\b[^>]*>.*?</script>""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)
private val browserCommentStyleRegex = Regex(
    """<style\b[^>]*>.*?</style>""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)
private val browserCommentAuthorPatterns = listOf(
    Regex("""class=["'][^"']*(?:comment-author|username|author|poster|commenter)[^"']*["'][^>]*>(.*?)</""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
    Regex("""<h[1-6][^>]*>(.*?)</h[1-6]>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
    Regex("""<a[^>]*>(.*?)</a>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
)
private val browserCommentBodyPatterns = listOf(
    Regex("""class=["'][^"']*(?:comment-text|comment-body|comment-content|content|body)[^"']*["'][^>]*>(.*?)</""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
    Regex("""<p[^>]*>(.*?)</p>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
)
private val browserCommentBlockRegex = Regex(
    """<div[^>]*class=["'][^"']*comment[^"']*["'][^>]*>(.*?)</div>""",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)

internal fun parseGalleryCommentsFromHtml(html: String): List<BrowserGalleryComment> {
    if (html.isBlank()) return emptyList()
    val commentsSection = extractCommentsSection(html)
    if (commentsSection.isBlank()) return emptyList()

    val section = commentsSection
        .replace(browserCommentScriptRegex, " ")
        .replace(browserCommentStyleRegex, " ")

    fun findFirstText(source: String, patterns: List<Regex>): String {
        patterns.forEach { regex ->
            val matched = regex.find(source)?.groupValues?.getOrNull(1).orEmpty()
            val cleaned = cleanHtmlText(matched)
            if (cleaned.isNotBlank()) return cleaned
        }
        return ""
    }

    val found = mutableListOf<BrowserGalleryComment>()
    browserCommentBlockRegex.findAll(section).forEach { match ->
        val block = match.groupValues.getOrNull(1).orEmpty()
        if (block.isBlank()) return@forEach
        val author = findFirstText(block, browserCommentAuthorPatterns)
        val text = findFirstText(block, browserCommentBodyPatterns)
        if (!isValidBrowserComment(author, text)) return@forEach
        found += BrowserGalleryComment(author = author, text = text)
    }

    if (found.isNotEmpty()) {
        return found.distinctBy { "${it.author}\u0000${it.text}" }.take(60)
    }

    val authorList = browserCommentAuthorPatterns
        .asSequence()
        .flatMap { pattern ->
            pattern.findAll(section).mapNotNull { result ->
                cleanHtmlText(result.groupValues.getOrNull(1).orEmpty()).takeIf { it.isNotBlank() }
            }
        }
        .toList()
    val bodyList = browserCommentBodyPatterns
        .asSequence()
        .flatMap { pattern ->
            pattern.findAll(section).mapNotNull { result ->
                cleanHtmlText(result.groupValues.getOrNull(1).orEmpty()).takeIf { it.isNotBlank() }
            }
        }
        .toList()

    val fallbackCount = min(authorList.size, bodyList.size)
    if (fallbackCount <= 0) return emptyList()
    return buildList {
        for (index in 0 until fallbackCount) {
            val author = authorList[index]
            val text = bodyList[index]
            if (!isValidBrowserComment(author, text)) continue
            add(BrowserGalleryComment(author = author, text = text))
        }
    }.distinctBy { "${it.author}\u0000${it.text}" }.take(60)
}

internal fun extractCommentsSection(html: String): String {
    val markers = listOf("id=\"comments\"", "id='comments'", "class=\"comments\"")
    val markerIndex = markers
        .asSequence()
        .map { html.indexOf(it, ignoreCase = true) }
        .filter { it >= 0 }
        .minOrNull() ?: return ""

    val start = html.lastIndexOf("<section", markerIndex, ignoreCase = true)
        .takeIf { it >= 0 }
        ?: html.lastIndexOf("<div", markerIndex, ignoreCase = true).takeIf { it >= 0 }
        ?: markerIndex
    val endLimit = min(html.length, start + 220_000)
    val chunk = html.substring(start, endLimit)
    val lowerChunk = chunk.lowercase(Locale.US)

    var depth = 0
    var cursor = 0
    var closedAt = -1
    while (cursor < lowerChunk.length) {
        val tagStart = lowerChunk.indexOf('<', cursor)
        if (tagStart < 0) break
        val tagEnd = lowerChunk.indexOf('>', tagStart + 1)
        if (tagEnd < 0) break
        val tag = lowerChunk.substring(tagStart + 1, tagEnd).trimStart()

        when {
            tag.startsWith("div") && tag.getOrNull(3)?.let { it.isWhitespace() || it == '/' } != false -> {
                depth += 1
            }
            tag.startsWith("section") && tag.getOrNull(7)?.let { it.isWhitespace() || it == '/' } != false -> {
                depth += 1
            }
            tag.startsWith("/div") -> {
                depth = (depth - 1).coerceAtLeast(0)
                if (depth == 0 && tagStart > markerIndex - start) {
                    closedAt = tagEnd + 1
                    break
                }
            }
            tag.startsWith("/section") -> {
                depth = (depth - 1).coerceAtLeast(0)
                if (depth == 0 && tagStart > markerIndex - start) {
                    closedAt = tagEnd + 1
                    break
                }
            }
        }
        cursor = tagEnd + 1
    }

    return if (closedAt > 0) {
        chunk.substring(0, closedAt)
    } else {
        chunk
    }
}

internal fun cleanHtmlText(raw: String): String {
    if (raw.isBlank()) return ""
    val plain = Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString()
    return plain.replace(Regex("\\s+"), " ").trim()
}

internal fun parseHtmlUploadDate(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return ""
    val dateMatch = Regex("""(\d{4}-\d{2}-\d{2})""").find(trimmed)?.groupValues?.getOrNull(1)
    if (!dateMatch.isNullOrBlank()) return dateMatch
    val timestamp = trimmed.toLongOrNull()
    return if (timestamp != null) parseApiUploadDate(timestamp) else ""
}

internal fun parseGalleryHtmlCompactCount(raw: String): Int? {
    val cleaned = raw.trim().lowercase(Locale.US).replace(",", "")
    if (cleaned.isBlank()) return null
    val match = Regex("""^(\d+(?:\.\d+)?)\s*([km])?$""").matchEntire(cleaned) ?: return cleaned.toIntOrNull()
    val value = match.groupValues.getOrNull(1)?.toFloatOrNull() ?: return null
    val suffix = match.groupValues.getOrNull(2).orEmpty()
    val multiplier = when (suffix) {
        "k" -> 1_000f
        "m" -> 1_000_000f
        else -> 1f
    }
    return (value * multiplier).toInt().coerceAtLeast(0)
}

internal fun parseApiMediaId(raw: Any?): Long {
    val parsed = when (raw) {
        is Number -> raw.toLong()
        is String -> raw.trim().toLongOrNull() ?: 0L
        else -> 0L
    }
    return parsed.coerceAtLeast(0L)
}

internal fun parseApiUploadDate(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return runCatching {
        Instant.ofEpochSecond(timestamp)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .format(GALLERY_BROWSER_UPLOAD_DATE_FORMAT)
    }.getOrDefault("")
}

internal fun parseApiImageExtension(raw: String?): String {
    return when (raw?.trim()?.lowercase(Locale.US).orEmpty()) {
        "j", "jpg", "jpeg" -> "jpg"
        "p", "png" -> "png"
        "w", "webp" -> "webp"
        "g", "gif" -> "gif"
        else -> ""
    }
}

internal fun buildCoverThumbnailUrls(mediaId: Long, preferredExt: String): List<String> {
    if (mediaId <= 0L) return emptyList()
    val extOrder = buildImageExtensionOrder(preferredExt)
    return extOrder.map { ext -> "https://t.nhentai.net/galleries/$mediaId/cover.$ext" }
}

internal fun browserMapDragPositionToRating(localX: Float, widthPx: Float): Int {
    val safeWidth = widthPx.coerceAtLeast(1f)
    val activeWidth = (safeWidth * GALLERY_BROWSER_DRAG_RATING_ACTIVE_WIDTH_FRACTION).coerceAtLeast(1f)
    val startX = ((safeWidth - activeWidth) / 2f).coerceAtLeast(0f)
    val normalized = ((localX - startX) / activeWidth).coerceIn(0f, 1f)
    return (normalized * 7f).toInt().coerceIn(0, GALLERY_BROWSER_HOLD_ACTION_HIDE)
}

internal fun buildPageThumbnailUrls(
    mediaId: Long,
    pageNumber: Int,
    preferredExt: String
): List<String> {
    if (mediaId <= 0L || pageNumber <= 0) return emptyList()
    val extOrder = buildImageExtensionOrder(preferredExt)
    return extOrder.map { ext -> "https://t.nhentai.net/galleries/$mediaId/${pageNumber}t.$ext" }
}

internal fun buildImageExtensionOrder(preferredExt: String): List<String> {
    val preferred = parseApiImageExtension(preferredExt)
    return buildList {
        if (preferred.isNotBlank()) add(preferred)
        add("jpg")
        add("png")
        add("webp")
        add("gif")
    }.distinct()
}
