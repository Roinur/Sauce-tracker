package com.example.saucetracker.feature.browser

import com.example.saucetracker.*
import com.example.saucetracker.data.backup.*
import com.example.saucetracker.data.remote.GalleryUrls
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
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.min
internal class BrowserViewModel : androidx.lifecycle.ViewModel() {
    private val client: OkHttpClient = HttpClientFactory.create(HttpClientProfile.BROWSER)
    private val commentsByCode = ConcurrentHashMap<Int, List<BrowserGalleryComment>>()
    private val relatedByCode = ConcurrentHashMap<Int, List<BrowserGallerySummary>>()

    fun clearSession() {
        runCatching { client.dispatcher.cancelAll() }
        runCatching { client.connectionPool.evictAll() }
        commentsByCode.clear()
        relatedByCode.clear()
    }

    override fun onCleared() {
        clearSession()
    }

    fun searchGalleries(query: String, page: Int, sortMode: BrowserSearchSortMode): BrowserSearchPage {
        val safePage = page.coerceAtLeast(1)
        val trimmed = query.trim()
        val url = if (trimmed.isBlank()) {
            val sortValue = sortMode.searchSortValue.trim()
            if (safePage <= 1 && sortValue.isBlank()) {
                "https://nhentai.net/"
            } else if (sortValue.isNotBlank()) {
                "https://nhentai.net/search/?q=&sort=${Uri.encode(sortValue)}&page=$safePage"
            } else {
                "https://nhentai.net/?page=$safePage"
            }
        } else {
            val encodedQuery = Uri.encode(trimmed)
            val sortPart = sortMode.searchSortValue.trim().takeIf { it.isNotBlank() }
                ?.let { "&sort=${Uri.encode(it)}" }
                .orEmpty()
            "https://nhentai.net/search/?q=$encodedQuery$sortPart&page=$safePage"
        }
        val html = requestHtml(url)
        val galleries = parseGallerySummariesFromHtml(html)
        val hasMore = galleries.isNotEmpty() && (
            html.contains("?page=${safePage + 1}") ||
                html.contains("&page=${safePage + 1}") ||
                galleries.size >= 20
            )
        return BrowserSearchPage(
            results = galleries,
            page = safePage,
            hasMore = hasMore
        )
    }

    fun searchCreatorGalleries(
        creatorType: String,
        creatorSlug: String,
        page: Int,
        sortMode: BrowserSearchSortMode
    ): BrowserSearchPage {
        val safePage = page.coerceAtLeast(1)
        val normalizedType = normalizeBrowserRouteType(creatorType)
        if (normalizedType.isBlank()) {
            return BrowserSearchPage(results = emptyList(), page = safePage, hasMore = false)
        }
        val cleanedSlug = creatorSlug.trim().trim('/')
        if (cleanedSlug.isBlank()) {
            return BrowserSearchPage(results = emptyList(), page = safePage, hasMore = false)
        }
        val encodedSlug = Uri.encode(cleanedSlug)
        val base = "https://nhentai.net/$normalizedType/$encodedSlug/"
        // Creator pages keep the sort mode in the query string; appending it as a
        // path segment produces a valid-looking URL which nhentai does not paginate.
        val sortQuery = sortMode.searchSortValue.trim().takeIf { it.isNotBlank() }
            ?.let { "sort=${Uri.encode(it)}" }
        val queryParts = buildList {
            sortQuery?.let(::add)
            if (safePage > 1) add("page=$safePage")
        }
        val url = if (queryParts.isEmpty()) base else "$base?${queryParts.joinToString("&")}" 
        val html = requestHtml(url)
        val galleries = parseGallerySummariesFromHtml(html)
        val hasMore = galleries.isNotEmpty() && (
            html.contains("?page=${safePage + 1}") ||
                html.contains("&page=${safePage + 1}") ||
                galleries.size >= 20
            )
        return BrowserSearchPage(
            results = galleries,
            page = safePage,
            hasMore = hasMore
        )
    }

    fun fetchGalleryDetail(code: Int): BrowserGalleryDetail {
        if (code <= 0) throw IOException("Invalid code.")
        val apiDetail = runCatching {
            val url = "https://nhentai.net/api/gallery/$code"
            val body = requestBody(url)
            val root = try {
                JSONObject(body)
            } catch (_: Exception) {
                throw IOException("Gallery API returned invalid JSON.")
            }
            // The API payload already contains the metadata and page information needed to
            // display the detail pane. Comments are intentionally loaded after the pane is
            // visible so a slow or malformed comments section cannot delay browser opening.
            parseGalleryDetail(root, galleryHtml = "", commentsJson = null)
                ?: throw IOException("Could not parse gallery metadata.")
        }

        apiDetail.getOrNull()?.let { return it }

        val pageHtml = runCatching { requestHtml("https://nhentai.net/g/$code/") }.getOrDefault("")
        parseGalleryDetailFromHtml(pageHtml, code, includeComments = false)?.let { return it }
        throw (apiDetail.exceptionOrNull() ?: IOException("Could not parse gallery metadata."))
    }

    fun fetchGalleryComments(code: Int): List<BrowserGalleryComment> {
        if (code <= 0) return emptyList()
        commentsByCode[code]?.let { return it }
        val apiComments = runCatching {
            requestBody(GalleryUrls.comments(code))
        }.getOrNull()
        parseGalleryCommentsFromApiJson(apiComments).takeIf { it.isNotEmpty() }?.let { comments ->
            commentsByCode[code] = comments
            return comments
        }

        val pageHtml = runCatching { requestHtml("https://nhentai.net/g/$code/") }.getOrDefault("")
        return parseGalleryCommentsFromHtml(pageHtml).also { commentsByCode[code] = it }
    }

    fun fetchRelatedGalleries(code: Int): List<BrowserGallerySummary> {
        if (code <= 0) return emptyList()
        relatedByCode[code]?.let { return it }
        val payload = sequenceOf(
            GalleryUrls.relatedV2(code),
            GalleryUrls.relatedLegacy(code)
        ).mapNotNull { url -> runCatching { requestBody(url) }.getOrNull() }
            .firstOrNull { parseRelatedGallerySummaries(it).isNotEmpty() }
        return parseRelatedGallerySummaries(payload)
            .filterNot { it.code == code }
            .take(5)
            .also { relatedByCode[code] = it }
    }

    private fun requestBody(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
            )
            .header("Accept", "application/json")
            .header("Referer", "https://nhentai.net/")
            .header("Origin", "https://nhentai.net")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (exc: IOException) {
            throw IOException("Network error: ${exc.message ?: "unknown error"}")
        }

        response.use { rsp ->
            if (!rsp.isSuccessful) {
                throw IOException("HTTP ${rsp.code} from API.")
            }
            return rsp.body?.string()
                ?: throw IOException("Server returned an empty response.")
        }
    }

    private fun requestHtml(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
            )
            .header("Accept", "text/html,application/xhtml+xml,application/xml")
            .header("Referer", "https://nhentai.net/")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (exc: IOException) {
            throw IOException("Network error: ${exc.message ?: "unknown error"}")
        }

        response.use { rsp ->
            if (!rsp.isSuccessful) {
                throw IOException("HTTP ${rsp.code} from API.")
            }
            return rsp.body?.string()
                ?: throw IOException("Server returned an empty response.")
        }
    }
}
