package com.example.saucetracker

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.min

private const val GALLERY_BROWSER_PREFS_NAME = "nhtagbook_prefs"
private const val GALLERY_BROWSER_KEY_THEME_MODE = "theme_mode"
private const val GALLERY_BROWSER_KEY_ACCENT_MODE = "accent_mode"
private const val GALLERY_BROWSER_URL_TRAILING_PUNCT = ".,;:!?)]}'\""
private const val GALLERY_BROWSER_INCOGNITO_OVERLAY_ALPHA = 0.82f
private val GALLERY_BROWSER_UPLOAD_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

private data class BrowserGallerySummary(
    val code: Int,
    val title: String,
    val subtitle: String,
    val mediaId: Long,
    val coverExt: String,
    val numPages: Int,
    val uploadDate: String
)

private data class BrowserPageThumb(
    val pageNumber: Int,
    val thumbnailUrls: List<String>
)

private data class BrowserGalleryComment(
    val author: String,
    val text: String
)

private data class BrowserGalleryDetail(
    val summary: BrowserGallerySummary,
    val tagsByType: Map<String, List<String>>,
    val tagCountsByKey: Map<String, Int>,
    val pageThumbs: List<BrowserPageThumb>,
    val comments: List<BrowserGalleryComment>
)

private data class BrowserSearchPage(
    val results: List<BrowserGallerySummary>,
    val page: Int,
    val hasMore: Boolean
)

private data class BrowserCreatorRef(
    val type: String,
    val name: String,
    val slug: String
)

private data class BrowserNavSnapshot(
    val searchInput: String,
    val activeSearchTerm: String,
    val rows: List<BrowserGallerySummary>,
    val selectedDetail: BrowserGalleryDetail?,
    val currentPage: Int,
    val hasMorePages: Boolean,
    val activeCreator: BrowserCreatorRef?,
    val sortMode: BrowserSearchSortMode
)

private enum class BrowserSearchSortMode(
    val label: String,
    val searchSortValue: String,
    val creatorPathValue: String
) {
    RECENT(label = "Recent", searchSortValue = "", creatorPathValue = ""),
    POPULAR_TODAY(label = "Today", searchSortValue = "popular-today", creatorPathValue = "popular-today"),
    POPULAR_WEEK(label = "Week", searchSortValue = "popular-week", creatorPathValue = "popular-week"),
    POPULAR_ALL_TIME(label = "All Time", searchSortValue = "popular", creatorPathValue = "popular")
}

private data class BrowserExitRatingPromptState(
    val code: Int,
    val title: String,
    val rating: Int,
    val closeAfter: Boolean
)

private data class BrowserLocalLibraryState(
    val exists: Boolean,
    val rating: Int,
    val isRead: Boolean
)

private sealed interface BrowserPendingLibraryAction {
    data class SetRating(val rating: Int) : BrowserPendingLibraryAction
    data class SetRead(val isRead: Boolean) : BrowserPendingLibraryAction
}

private class GalleryCodeBrowserApi {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
        .cookieJar(CookieJar.NO_COOKIES)
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    fun clearSession() {
        runCatching { client.dispatcher.cancelAll() }
        runCatching { client.connectionPool.evictAll() }
    }

    fun searchGalleries(query: String, page: Int, sortMode: BrowserSearchSortMode): BrowserSearchPage {
        val safePage = page.coerceAtLeast(1)
        val trimmed = query.trim()
        val url = if (trimmed.isBlank()) {
            if (safePage <= 1) {
                "https://nhentai.net/"
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
        val sortPath = sortMode.creatorPathValue.trim().takeIf { it.isNotBlank() }
            .orEmpty()
        val url = if (safePage <= 1) {
            "$base$sortPath"
        } else {
            "$base$sortPath?page=$safePage"
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

    fun fetchGalleryDetail(code: Int): BrowserGalleryDetail {
        if (code <= 0) throw IOException("Invalid code.")
        val url = "https://nhentai.net/api/gallery/$code"
        val body = requestBody(url)
        val root = try {
            JSONObject(body)
        } catch (_: Exception) {
            throw IOException("Gallery API returned invalid JSON.")
        }
        val pageHtml = runCatching { requestHtml("https://nhentai.net/g/$code/") }.getOrDefault("")
        val commentPayload = runCatching { requestBody("https://nhentai.net/api/gallery/$code/comments") }.getOrNull()
        return parseGalleryDetail(root, pageHtml, commentPayload)
            ?: throw IOException("Could not parse gallery metadata.")
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

class GalleryCodeBrowserActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_INITIAL_CODE = "extra_initial_code"
        private const val EXTRA_INITIAL_QUERY = "extra_initial_query"
        private const val EXTRA_INITIAL_CREATOR_TYPE = "extra_initial_creator_type"
        private const val EXTRA_INITIAL_CREATOR_NAME = "extra_initial_creator_name"
        private const val EXTRA_BLOCKED_TAGS = "extra_blocked_tags"
        private const val EXTRA_INCOGNITO_MODE = "extra_incognito_mode"

        fun createIntent(
            context: Context,
            initialCode: Int? = null,
            initialQuery: String = "",
            initialCreatorType: String? = null,
            initialCreatorName: String? = null,
            incognitoModeEnabled: Boolean = false,
            blockedTags: List<String> = emptyList()
        ): Intent {
            return Intent(context, GalleryCodeBrowserActivity::class.java).apply {
                if (initialCode != null && initialCode > 0) {
                    putExtra(EXTRA_INITIAL_CODE, initialCode)
                }
                putExtra(EXTRA_INITIAL_QUERY, initialQuery)
                if (!initialCreatorType.isNullOrBlank() && !initialCreatorName.isNullOrBlank()) {
                    putExtra(EXTRA_INITIAL_CREATOR_TYPE, initialCreatorType.trim())
                    putExtra(EXTRA_INITIAL_CREATOR_NAME, initialCreatorName.trim())
                }
                putExtra(EXTRA_INCOGNITO_MODE, incognitoModeEnabled)
                putStringArrayListExtra(EXTRA_BLOCKED_TAGS, ArrayList(blockedTags))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialCode = intent?.getIntExtra(EXTRA_INITIAL_CODE, 0)?.takeIf { it > 0 }
        val initialQuery = intent?.getStringExtra(EXTRA_INITIAL_QUERY).orEmpty().trim()
        val initialCreatorType = intent?.getStringExtra(EXTRA_INITIAL_CREATOR_TYPE).orEmpty().trim()
            .ifBlank { null }
        val initialCreatorName = intent?.getStringExtra(EXTRA_INITIAL_CREATOR_NAME).orEmpty().trim()
            .ifBlank { null }
        val incognitoModeEnabled = intent?.getBooleanExtra(EXTRA_INCOGNITO_MODE, false) == true
        val blockedTags = intent?.getStringArrayListExtra(EXTRA_BLOCKED_TAGS)?.toList().orEmpty()
        val themeMode = loadThemeMode()
        val accentMode = loadAccentMode()

        setContent {
            GalleryCodeBrowserTheme(
                themeMode = themeMode,
                accentMode = accentMode
            ) {
                GalleryCodeBrowserApplySystemBars()
                GalleryCodeBrowserScreen(
                    initialCode = initialCode,
                    initialQuery = initialQuery,
                    initialCreatorType = initialCreatorType,
                    initialCreatorName = initialCreatorName,
                    incognitoModeEnabled = incognitoModeEnabled,
                    blockedTags = blockedTags,
                    onDone = ::finish
                )
            }
        }
    }

    override fun onDestroy() {
        clearGalleryBrowserPrivacyArtifacts(null)
        super.onDestroy()
    }

    private fun loadThemeMode(): ThemeMode {
        val prefs = getSharedPreferences(GALLERY_BROWSER_PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(GALLERY_BROWSER_KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.SYSTEM
    }

    private fun loadAccentMode(): AccentMode {
        val prefs = getSharedPreferences(GALLERY_BROWSER_PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(GALLERY_BROWSER_KEY_ACCENT_MODE, AccentMode.AUTO.name)
        return AccentMode.entries.firstOrNull { it.name == raw } ?: AccentMode.AUTO
    }
}

@Composable
private fun GalleryCodeBrowserTheme(
    themeMode: ThemeMode,
    accentMode: AccentMode,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val fallbackScheme = if (useDark) {
        darkColorScheme(
            primary = Color(0xFF8BC1FF),
            onPrimary = Color(0xFF002B52),
            secondary = Color(0xFF8CC8A8),
            background = Color(0xFF1D2127),
            onBackground = Color(0xFFE9EDF2),
            surface = Color(0xFF292E36),
            onSurface = Color(0xFFE9EDF2),
            onSurfaceVariant = Color(0xFFB4BEC8),
            error = Color(0xFFFF8A8A)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF1F63D8),
            onPrimary = Color.White,
            secondary = Color(0xFF0D8F4F),
            background = Color(0xFFF6F8FB),
            onBackground = Color(0xFF1F2935),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1F2935),
            onSurfaceVariant = Color(0xFF5C6470),
            error = Color(0xFFB00020)
        )
    }

    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        applyGalleryBrowserAccentMode(
            baseScheme = if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context),
            accentMode = accentMode,
            isDark = useDark
        )
    } else {
        applyGalleryBrowserAccentMode(
            baseScheme = fallbackScheme,
            accentMode = accentMode,
            isDark = useDark
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        content()
    }
}

@Composable
private fun GalleryCodeBrowserApplySystemBars() {
    val view = LocalView.current
    if (view.isInEditMode) return

    val color = MaterialTheme.colorScheme.background.toArgb()
    val darkContent = MaterialTheme.colorScheme.background.luminance() > 0.5f
    SideEffect {
        val activity = view.context as? Activity ?: return@SideEffect
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = color
        window.navigationBarColor = color
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = darkContent
            isAppearanceLightNavigationBars = darkContent
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryCodeBrowserScreen(
    initialCode: Int?,
    initialQuery: String,
    initialCreatorType: String?,
    initialCreatorName: String?,
    incognitoModeEnabled: Boolean,
    blockedTags: List<String>,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { GalleryCodeBrowserApi() }
    val db = remember { TagBookDatabase(context.applicationContext) }
    val blocked = remember(blockedTags) {
        blockedTags
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.US) }
    }

    var searchInput by remember { mutableStateOf(initialQuery) }
    var activeSearchTerm by remember { mutableStateOf(initialQuery) }
    var listRows by remember { mutableStateOf<List<BrowserGallerySummary>>(emptyList()) }
    var selectedDetail by remember { mutableStateOf<BrowserGalleryDetail?>(null) }
    var loadingList by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var loadingDetailCode by remember { mutableStateOf<Int?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentPage by remember { mutableStateOf(0) }
    var hasMorePages by remember { mutableStateOf(false) }
    var activeCreator by remember { mutableStateOf<BrowserCreatorRef?>(null) }
    var searchSortMode by remember { mutableStateOf(BrowserSearchSortMode.RECENT) }
    var clipboardImportPrompt by remember { mutableStateOf<String?>(null) }
    var ratingPromptState by remember { mutableStateOf<BrowserExitRatingPromptState?>(null) }
    var pendingSlideshowRatingCode by remember { mutableStateOf<Int?>(null) }
    var detailLoadRequestId by remember { mutableStateOf(0L) }
    var listLibraryStates by remember { mutableStateOf<Map<Int, BrowserLocalLibraryState>>(emptyMap()) }
    var listLibraryRequestId by remember { mutableStateOf(0L) }
    val navStack = remember { mutableStateListOf<BrowserNavSnapshot>() }

    fun closeBrowserSecurely() {
        clearGalleryBrowserPrivacyArtifacts(api)
        onDone()
    }

    fun toGalleryData(detail: BrowserGalleryDetail): GalleryData {
        val summary = detail.summary
        val tags = detail.tagsByType
            .entries
            .flatMap { (type, names) ->
                names
                    .asSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .map { GalleryTag(name = it, type = type) }
                    .toList()
            }
        return GalleryData(
            code = summary.code,
            title = summary.title,
            subtitle = summary.subtitle,
            numPages = summary.numPages,
            uploadDate = summary.uploadDate,
            sourceUrl = "https://nhentai.net/g/${summary.code}/",
            mediaId = summary.mediaId,
            coverExt = summary.coverExt,
            tags = tags
        )
    }

    fun promptRatingForCode(code: Int, fallbackTitle: String, closeAfter: Boolean) {
        if (code <= 0) return
        scope.launch {
            val detail = withContext(Dispatchers.IO) { db.getEntryDetail(code) }
            val initial = detail?.rating?.coerceIn(0, 5) ?: 0
            ratingPromptState = BrowserExitRatingPromptState(
                code = code,
                title = detail?.title?.ifBlank { fallbackTitle } ?: fallbackTitle,
                rating = initial,
                closeAfter = closeAfter
            )
        }
    }

    val slideshowLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val code = pendingSlideshowRatingCode
        pendingSlideshowRatingCode = null
        if (code != null && code > 0) {
            val title = selectedDetail?.takeIf { it.summary.code == code }?.summary?.title
                ?.ifBlank { "Gallery $code" }
                ?: "Gallery $code"
            promptRatingForCode(code = code, fallbackTitle = title, closeAfter = false)
        }
    }

    fun buildEffectiveQuery(term: String): String {
        val trimmed = term.trim()
        val blockedPart = buildApiTagSearchQuery(includeTagNames = emptyList(), excludeTagNames = blocked)
        return when {
            trimmed.isBlank() && blockedPart.isBlank() -> ""
            trimmed.isBlank() -> blockedPart
            blockedPart.isBlank() -> trimmed
            else -> "$trimmed $blockedPart"
        }.trim()
    }

    fun refreshListLibraryStates(rows: List<BrowserGallerySummary> = listRows) {
        val codes = rows.map { it.code }.distinct()
        if (codes.isEmpty()) {
            listLibraryStates = emptyMap()
            return
        }
        val requestId = System.nanoTime()
        listLibraryRequestId = requestId
        scope.launch {
            val states = withContext(Dispatchers.IO) {
                codes.associateWith { code ->
                    val local = db.getEntryDetail(code)
                    if (local != null) {
                        BrowserLocalLibraryState(
                            exists = true,
                            rating = local.rating.coerceIn(0, 5),
                            isRead = local.isRead
                        )
                    } else {
                        BrowserLocalLibraryState(exists = false, rating = 0, isRead = false)
                    }
                }
            }
            if (listLibraryRequestId == requestId) {
                listLibraryStates = states
            }
        }
    }

    fun loadPage(reset: Boolean) {
        scope.launch {
            if (reset) {
                loadingList = true
                errorMessage = null
                currentPage = 0
                hasMorePages = false
                if (selectedDetail == null) {
                    listRows = emptyList()
                    listLibraryStates = emptyMap()
                }
            } else {
                if (loadingMore || loadingList || !hasMorePages) return@launch
                loadingMore = true
            }

            val pageToLoad = if (reset) 1 else (currentPage + 1).coerceAtLeast(1)
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val creatorRef = activeCreator
                    if (creatorRef != null) {
                        api.searchCreatorGalleries(
                            creatorType = creatorRef.type,
                            creatorSlug = creatorRef.slug,
                            page = pageToLoad,
                            sortMode = searchSortMode
                        )
                    } else {
                        val effectiveQuery = buildEffectiveQuery(activeSearchTerm)
                        api.searchGalleries(
                            query = effectiveQuery,
                            page = pageToLoad,
                            sortMode = searchSortMode
                        )
                    }
                }
            }

            result.onSuccess { page ->
                val nextRows = if (reset) {
                    page.results
                } else {
                    listRows + page.results
                }
                listRows = nextRows
                refreshListLibraryStates(nextRows)
                currentPage = page.page
                hasMorePages = page.hasMore
            }.onFailure { exc ->
                if (reset || listRows.isEmpty()) {
                    errorMessage = exc.message ?: "Could not load homepage data."
                }
            }

            loadingList = false
            loadingMore = false
        }
    }

    fun captureNavSnapshot(): BrowserNavSnapshot {
        return BrowserNavSnapshot(
            searchInput = searchInput,
            activeSearchTerm = activeSearchTerm,
            rows = listRows,
            selectedDetail = selectedDetail,
            currentPage = currentPage,
            hasMorePages = hasMorePages,
            activeCreator = activeCreator,
            sortMode = searchSortMode
        )
    }

    fun pushNavSnapshot() {
        navStack.add(captureNavSnapshot())
    }

    fun applyNavSnapshot(snapshot: BrowserNavSnapshot) {
        detailLoadRequestId = System.nanoTime()
        loadingDetailCode = null
        loadingMore = false
        loadingList = false

        searchInput = snapshot.searchInput
        activeSearchTerm = snapshot.activeSearchTerm
        listRows = snapshot.rows
        selectedDetail = snapshot.selectedDetail
        currentPage = snapshot.currentPage
        hasMorePages = snapshot.hasMorePages
        activeCreator = snapshot.activeCreator
        refreshListLibraryStates(snapshot.rows)
        errorMessage = null

        val reloadRecentNeeded =
            snapshot.sortMode != BrowserSearchSortMode.RECENT &&
                snapshot.selectedDetail == null &&
                (snapshot.activeSearchTerm.isNotBlank() || snapshot.activeCreator != null)
        searchSortMode = BrowserSearchSortMode.RECENT
        if (reloadRecentNeeded) {
            loadPage(reset = true)
        }
    }

    fun navigateBackOrClose() {
        val previous = navStack.lastOrNull()
        if (previous != null) {
            navStack.removeAt(navStack.lastIndex)
            applyNavSnapshot(previous)
            return
        }
        closeBrowserSecurely()
    }

    fun openDetail(code: Int, pushHistory: Boolean = true) {
        if (code <= 0) return
        if (pushHistory) {
            pushNavSnapshot()
        }
        val requestId = System.nanoTime()
        detailLoadRequestId = requestId
        scope.launch {
            loadingDetailCode = code
            errorMessage = null
            val result = withContext(Dispatchers.IO) {
                runCatching { api.fetchGalleryDetail(code) }
            }
            result.onSuccess { detail ->
                if (detailLoadRequestId == requestId) {
                    selectedDetail = detail
                }
            }.onFailure { exc ->
                if (detailLoadRequestId == requestId) {
                    errorMessage = exc.message ?: "Could not load gallery details."
                }
            }
            if (detailLoadRequestId == requestId) {
                loadingDetailCode = null
            }
        }
    }

    fun openSlideshow(detail: BrowserGalleryDetail, startPage: Int = 1) {
        val summary = detail.summary
        if (summary.mediaId <= 0L || summary.numPages <= 0) return
        pendingSlideshowRatingCode = summary.code
        val intent = GallerySlideshowActivity.createIntent(
            context = context,
            code = summary.code,
            title = summary.title,
            mediaId = summary.mediaId,
            coverExt = summary.coverExt,
            numPages = summary.numPages,
            startPage = startPage.coerceIn(1, summary.numPages.coerceAtLeast(1))
        )
        slideshowLauncher.launch(intent)
    }

    fun openCreator(type: String, name: String) {
        val normalizedType = normalizeBrowserRouteType(type)
        val cleanName = when (normalizedType) {
            "artist", "group" -> normalizeCreatorDisplayName(name).ifBlank { name.trim() }
            else -> parseCreatorSlug(name).ifBlank { name.trim() }
        }
        if (normalizedType.isBlank() || cleanName.isBlank()) return
        pushNavSnapshot()
        searchSortMode = BrowserSearchSortMode.RECENT
        val slug = toBrowserRouteSlug(normalizedType, cleanName).ifBlank { cleanName }
        activeCreator = BrowserCreatorRef(
            type = normalizedType,
            name = cleanName,
            slug = slug
        )
        selectedDetail = null
        errorMessage = null
        loadPage(reset = true)
    }

    fun runSearchFromInput() {
        pushNavSnapshot()
        searchSortMode = BrowserSearchSortMode.RECENT
        activeSearchTerm = searchInput.trim()
        activeCreator = null
        selectedDetail = null
        loadPage(reset = true)
    }

    fun runTagSearch(tagName: String) {
        val encodedTag = encodeTagSearchTerm(tagName)
        if (encodedTag.isBlank()) return
        pushNavSnapshot()
        searchSortMode = BrowserSearchSortMode.RECENT
        searchInput = encodedTag
        activeSearchTerm = encodedTag
        activeCreator = null
        selectedDetail = null
        loadPage(reset = true)
    }

    fun handleDoneAction() {
        val currentDetail = selectedDetail
        if (currentDetail != null) {
            promptRatingForCode(
                code = currentDetail.summary.code,
                fallbackTitle = currentDetail.summary.title.ifBlank { "Gallery ${currentDetail.summary.code}" },
                closeAfter = true
            )
            return
        }
        closeBrowserSecurely()
    }

    DisposableEffect(Unit) {
        onDispose {
            clearGalleryBrowserPrivacyArtifacts(api)
        }
    }

    LaunchedEffect(initialCode, initialCreatorType, initialCreatorName) {
        if (initialCode != null && initialCode > 0) {
            openDetail(initialCode, pushHistory = false)
            loadPage(reset = true)
        } else if (!initialCreatorType.isNullOrBlank() && !initialCreatorName.isNullOrBlank()) {
            val normalizedType = normalizeBrowserRouteType(initialCreatorType)
            val cleanName = when (normalizedType) {
                "artist", "group" -> normalizeCreatorDisplayName(initialCreatorName).ifBlank { initialCreatorName.trim() }
                else -> parseCreatorSlug(initialCreatorName).ifBlank { initialCreatorName.trim() }
            }
            if (normalizedType.isNotBlank() && cleanName.isNotBlank()) {
                searchSortMode = BrowserSearchSortMode.RECENT
                searchInput = ""
                activeSearchTerm = ""
                activeCreator = BrowserCreatorRef(
                    type = normalizedType,
                    name = cleanName,
                    slug = toBrowserRouteSlug(normalizedType, cleanName).ifBlank { cleanName }
                )
                selectedDetail = null
                errorMessage = null
                loadPage(reset = true)
            } else {
                loadPage(reset = true)
            }
        } else {
            loadPage(reset = true)
        }
    }

    BackHandler(enabled = true) {
        navigateBackOrClose()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    val creatorRef = activeCreator
                    val defaultTitle = if (creatorRef != null) {
                        val typeLabel = creatorRef.type.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
                        }
                        "$typeLabel: ${creatorRef.name}"
                    } else {
                        "nhentai.net"
                    }
                    Text(
                        text = selectedDetail?.summary?.title?.ifBlank { defaultTitle } ?: defaultTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    TextButton(onClick = ::navigateBackOrClose) {
                        Text(
                            text = if (navStack.isNotEmpty()) "Back" else "Close",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                actions = {
                    TextButton(onClick = ::handleDoneAction) {
                        Text("Done", fontWeight = FontWeight.SemiBold)
                    }
                }
            )

            if (selectedDetail == null && activeCreator == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            label = { Text("Search galleries") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSearch = { runSearchFromInput() }
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = ::runSearchFromInput) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_open_in_new_24),
                                contentDescription = "Search"
                            )
                        }
                    }
                    if (blocked.isNotEmpty()) {
                        Text(
                            text = "Blocked tags applied: ${blocked.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (activeSearchTerm.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = searchSortMode == BrowserSearchSortMode.RECENT,
                                onClick = {
                                    if (searchSortMode != BrowserSearchSortMode.RECENT) {
                                        searchSortMode = BrowserSearchSortMode.RECENT
                                        loadPage(reset = true)
                                    }
                                },
                                label = { Text("Recent") }
                            )
                            Text(
                                text = "Popular",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FilterChip(
                                selected = searchSortMode == BrowserSearchSortMode.POPULAR_TODAY,
                                onClick = {
                                    if (searchSortMode != BrowserSearchSortMode.POPULAR_TODAY) {
                                        searchSortMode = BrowserSearchSortMode.POPULAR_TODAY
                                        loadPage(reset = true)
                                    }
                                },
                                label = { Text("Today") }
                            )
                            FilterChip(
                                selected = searchSortMode == BrowserSearchSortMode.POPULAR_WEEK,
                                onClick = {
                                    if (searchSortMode != BrowserSearchSortMode.POPULAR_WEEK) {
                                        searchSortMode = BrowserSearchSortMode.POPULAR_WEEK
                                        loadPage(reset = true)
                                    }
                                },
                                label = { Text("Week") }
                            )
                            FilterChip(
                                selected = searchSortMode == BrowserSearchSortMode.POPULAR_ALL_TIME,
                                onClick = {
                                    if (searchSortMode != BrowserSearchSortMode.POPULAR_ALL_TIME) {
                                        searchSortMode = BrowserSearchSortMode.POPULAR_ALL_TIME
                                        loadPage(reset = true)
                                    }
                                },
                                label = { Text("All Time") }
                            )
                        }
                    }
                }
            } else if (selectedDetail == null && activeCreator != null) {
                activeCreator?.let { creatorRef ->
                    val creatorLabel = creatorRef.type.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Showing $creatorLabel page for ${creatorRef.name}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = searchSortMode == BrowserSearchSortMode.RECENT,
                                onClick = {
                                    if (searchSortMode != BrowserSearchSortMode.RECENT) {
                                        searchSortMode = BrowserSearchSortMode.RECENT
                                        loadPage(reset = true)
                                    }
                                },
                                label = { Text("Recent") }
                            )
                            Text(
                                text = "Popular",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FilterChip(
                                selected = searchSortMode == BrowserSearchSortMode.POPULAR_TODAY,
                                onClick = {
                                    if (searchSortMode != BrowserSearchSortMode.POPULAR_TODAY) {
                                        searchSortMode = BrowserSearchSortMode.POPULAR_TODAY
                                        loadPage(reset = true)
                                    }
                                },
                                label = { Text("Today") }
                            )
                            FilterChip(
                                selected = searchSortMode == BrowserSearchSortMode.POPULAR_WEEK,
                                onClick = {
                                    if (searchSortMode != BrowserSearchSortMode.POPULAR_WEEK) {
                                        searchSortMode = BrowserSearchSortMode.POPULAR_WEEK
                                        loadPage(reset = true)
                                    }
                                },
                                label = { Text("Week") }
                            )
                            FilterChip(
                                selected = searchSortMode == BrowserSearchSortMode.POPULAR_ALL_TIME,
                                onClick = {
                                    if (searchSortMode != BrowserSearchSortMode.POPULAR_ALL_TIME) {
                                        searchSortMode = BrowserSearchSortMode.POPULAR_ALL_TIME
                                        loadPage(reset = true)
                                    }
                                },
                                label = { Text("All Time") }
                            )
                        }
                    }
                }
            }

            when {
                selectedDetail != null -> {
                    val detail = selectedDetail ?: return@Column
                    GalleryDetailPane(
                        detail = detail,
                        incognitoModeEnabled = incognitoModeEnabled,
                        loading = loadingDetailCode == detail.summary.code,
                        onOpenSlideshow = { page -> openSlideshow(detail, page) },
                        onOpenCode = { code -> openDetail(code, pushHistory = false) },
                        onOpenCreator = ::openCreator,
                        onSearchTag = ::runTagSearch,
                        onCopyCandidateDetected = { candidate ->
                            clipboardImportPrompt = extractImportCandidateFromClipboard(candidate)
                        }
                    )
                }

                loadingList && listRows.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null && listRows.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.error
                            )
                            TextButton(onClick = { loadPage(reset = true) }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                else -> {
                    val showHomepageSectionSplit =
                        activeCreator == null &&
                            activeSearchTerm.isBlank() &&
                            blocked.isEmpty()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(listRows, key = { _, row -> row.code }) { index, row ->
                            if (showHomepageSectionSplit && index == 0) {
                                GalleryListSectionHeader(
                                    title = "Popular Now",
                                    subtitle = if (listRows.size > 5) "Top 5 galleries" else null
                                )
                            }
                            if (showHomepageSectionSplit && index == 5) {
                                GalleryListSectionHeader(
                                    title = "Recently Added",
                                    subtitle = "Newest updates"
                                )
                            }
                            GallerySummaryCard(
                                row = row,
                                incognitoModeEnabled = incognitoModeEnabled,
                                localLibraryState = listLibraryStates[row.code]
                                    ?: BrowserLocalLibraryState(exists = false, rating = 0, isRead = false),
                                loading = loadingDetailCode == row.code,
                                onOpen = { openDetail(row.code) },
                                onOpenSlideshow = { openDetail(row.code) }
                            )
                        }

                        item {
                            when {
                                loadingMore -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }

                                hasMorePages -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Button(onClick = { loadPage(reset = false) }) {
                                            Text("Load More")
                                        }
                                    }
                                }

                                listRows.isEmpty() -> {
                                    Text(
                                        text = "No galleries found.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        clipboardImportPrompt?.let { candidate ->
            AlertDialog(
                onDismissRequest = { clipboardImportPrompt = null },
                title = { Text("Import copied text?") },
                text = {
                    Text(
                        text = "Add/update this in Sauce Tracker?\n\n${candidate.take(140)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            BrowserImportBridge.submit(candidate)
                            clipboardImportPrompt = null
                        }
                    ) {
                        Text("Import")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { clipboardImportPrompt = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        ratingPromptState?.let { prompt ->
            AlertDialog(
                onDismissRequest = {
                    ratingPromptState = null
                    if (prompt.closeAfter) closeBrowserSecurely()
                },
                title = { Text("What do you rate this sauce?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${prompt.code} - ${prompt.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (index in 1..5) {
                                val filled = index <= prompt.rating
                                Text(
                                    text = if (filled) "★" else "☆",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (filled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            ratingPromptState = prompt.copy(rating = index)
                                        }
                                        .padding(top = 2.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val savePrompt = ratingPromptState ?: return@TextButton
                            val code = savePrompt.code
                            val safeRating = savePrompt.rating.coerceIn(0, 5)
                            val selectedSnapshot = selectedDetail?.takeIf { it.summary.code == code }
                            val shouldClose = savePrompt.closeAfter
                            scope.launch {
                                val saved = withContext(Dispatchers.IO) {
                                    var entryExists = db.getEntryDetail(code) != null
                                    if (!entryExists) {
                                        val detailForImport = selectedSnapshot
                                            ?: runCatching { api.fetchGalleryDetail(code) }.getOrNull()
                                        if (detailForImport != null) {
                                            db.upsertGallery(toGalleryData(detailForImport))
                                            entryExists = db.getEntryDetail(code) != null
                                        }
                                    }
                                    if (entryExists) {
                                        db.setEntryRating(code, safeRating)
                                        db.setEntryRead(code, true)
                                    }
                                    entryExists
                                }
                                ratingPromptState = null
                                if (!saved) {
                                    Toast.makeText(
                                        context,
                                        "Could not save rating: failed to import entry $code.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                if (shouldClose) closeBrowserSecurely()
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            val shouldClose = prompt.closeAfter
                            ratingPromptState = null
                            if (shouldClose) closeBrowserSecurely()
                        }
                    ) {
                        Text("Skip")
                    }
                }
            )
        }
    }
}

@Composable
private fun GalleryListSectionHeader(
    title: String,
    subtitle: String? = null
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GallerySummaryCard(
    row: BrowserGallerySummary,
    incognitoModeEnabled: Boolean,
    localLibraryState: BrowserLocalLibraryState,
    loading: Boolean,
    onOpen: () -> Unit,
    onOpenSlideshow: () -> Unit
) {
    val privacyOverlay = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = GALLERY_BROWSER_INCOGNITO_OVERLAY_ALPHA)
    val cardShape = RoundedCornerShape(12.dp)
    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            RemoteThumbnail(
                urls = buildCoverThumbnailUrls(row.mediaId, row.coverExt),
                contentDescription = "Cover for code ${row.code}",
                onClick = onOpenSlideshow,
                modifier = Modifier
                    .width(98.dp)
                    .height(130.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = row.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpen)
                )
                Text(
                    text = "Code: ${row.code}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (row.numPages > 0) {
                    Text(
                        text = "Pages: ${row.numPages}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (row.uploadDate.isNotBlank()) {
                    Text(
                        text = "Uploaded: ${row.uploadDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val shownRating = localLibraryState.rating.coerceIn(0, 5)
                val starText = buildString {
                    for (index in 1..5) {
                        append(if (index <= shownRating) "\u2605" else "\u2606")
                    }
                }
                Text(
                    text = "Read: ${if (localLibraryState.isRead) "Read" else "Unread"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (localLibraryState.isRead) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.browserPrivacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    )
                )
                Text(
                    text = "Rating: $starText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.browserPrivacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    )
                )
                if (loading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Loading details...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GalleryDetailPane(
    detail: BrowserGalleryDetail,
    incognitoModeEnabled: Boolean,
    loading: Boolean,
    onOpenSlideshow: (Int) -> Unit,
    onOpenCode: (Int) -> Unit,
    onOpenCreator: (String, String) -> Unit,
    onSearchTag: (String) -> Unit,
    onCopyCandidateDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember(context) { TagBookDatabase(context.applicationContext) }
    val privacyOverlay = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = GALLERY_BROWSER_INCOGNITO_OVERLAY_ALPHA)
    val summary = detail.summary
    val thumbRows = remember(detail.pageThumbs) { detail.pageThumbs.chunked(3) }
    var localLibraryState by remember(summary.code) {
        mutableStateOf(BrowserLocalLibraryState(exists = false, rating = 0, isRead = false))
    }
    var localLibraryLoading by remember(summary.code) { mutableStateOf(true) }
    var pendingImportAction by remember(summary.code) { mutableStateOf<BrowserPendingLibraryAction?>(null) }

    fun buildImportGalleryData(): GalleryData {
        val tags = detail.tagsByType
            .entries
            .flatMap { (type, names) ->
                names
                    .asSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .map { GalleryTag(name = it, type = type) }
                    .toList()
            }
        return GalleryData(
            code = summary.code,
            title = summary.title,
            subtitle = summary.subtitle,
            numPages = summary.numPages,
            uploadDate = summary.uploadDate,
            sourceUrl = "https://nhentai.net/g/${summary.code}/",
            mediaId = summary.mediaId,
            coverExt = summary.coverExt,
            tags = tags
        )
    }

    fun readLocalLibraryState(): BrowserLocalLibraryState {
        val local = db.getEntryDetail(summary.code)
        return if (local != null) {
            BrowserLocalLibraryState(
                exists = true,
                rating = local.rating.coerceIn(0, 5),
                isRead = local.isRead
            )
        } else {
            BrowserLocalLibraryState(exists = false, rating = 0, isRead = false)
        }
    }

    fun applyLibraryActionInDb(action: BrowserPendingLibraryAction) {
        when (action) {
            is BrowserPendingLibraryAction.SetRating -> {
                db.setEntryRating(summary.code, action.rating.coerceIn(0, 5))
                db.setEntryRead(summary.code, true)
            }
            is BrowserPendingLibraryAction.SetRead -> {
                db.setEntryRead(summary.code, action.isRead)
            }
        }
    }

    fun refreshLocalLibraryState() {
        scope.launch {
            localLibraryLoading = true
            localLibraryState = withContext(Dispatchers.IO) { readLocalLibraryState() }
            localLibraryLoading = false
        }
    }

    fun requestOrApplyLibraryAction(action: BrowserPendingLibraryAction) {
        if (localLibraryState.exists) {
            scope.launch {
                localLibraryLoading = true
                localLibraryState = withContext(Dispatchers.IO) {
                    applyLibraryActionInDb(action)
                    readLocalLibraryState()
                }
                localLibraryLoading = false
            }
        } else {
            pendingImportAction = action
        }
    }

    LaunchedEffect(summary.code) {
        refreshLocalLibraryState()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = summary.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (summary.subtitle.isNotBlank() && summary.subtitle != summary.title) {
                        Text(
                            text = summary.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Code: ${summary.code}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                            copyTextToClipboard(
                                context = context,
                                label = "Sauce code",
                                value = summary.code.toString(),
                                successMessage = "Copied code ${summary.code}."
                            )
                            onCopyCandidateDetected(summary.code.toString())
                        }
                    )
                    Text("Pages: ${summary.numPages}")
                    Text("Uploaded: ${summary.uploadDate.ifBlank { "-" }}")
                    if (!incognitoModeEnabled) {
                        Text(
                            text = if (localLibraryLoading) {
                                "Library: checking..."
                            } else if (localLibraryState.exists) {
                                "Library: imported"
                            } else {
                                "Library: not imported"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.browserPrivacyObfuscate(
                            enabled = incognitoModeEnabled,
                            overlayColor = privacyOverlay
                        )
                    ) {
                        Text(
                            text = "Read:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            enabled = !incognitoModeEnabled,
                            onClick = {
                                requestOrApplyLibraryAction(
                                    BrowserPendingLibraryAction.SetRead(!localLibraryState.isRead)
                                )
                            }
                        ) {
                            Text(
                                text = if (localLibraryState.isRead) "Read" else "Unread",
                                color = if (localLibraryState.isRead) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.browserPrivacyObfuscate(
                            enabled = incognitoModeEnabled,
                            overlayColor = privacyOverlay
                        )
                    ) {
                        Text(
                            text = "Rating:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        for (index in 1..5) {
                            val filled = index <= localLibraryState.rating
                            Text(
                                text = if (filled) "\u2605" else "\u2606",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (filled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .let { base ->
                                        if (incognitoModeEnabled) {
                                            base
                                        } else {
                                            base.clickable {
                                                requestOrApplyLibraryAction(
                                                    BrowserPendingLibraryAction.SetRating(index)
                                                )
                                            }
                                        }
                                    }
                                    .padding(top = 1.dp)
                            )
                        }
                        TextButton(
                            enabled = !incognitoModeEnabled,
                            onClick = {
                                if (localLibraryState.exists && localLibraryState.rating != 0) {
                                    requestOrApplyLibraryAction(
                                        BrowserPendingLibraryAction.SetRating(0)
                                    )
                                }
                            }
                        ) {
                            Text("Reset")
                        }
                    }
                    if (loading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Loading...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onOpenSlideshow(1) }) {
                            Text("Open Slideshow")
                        }
                        TextButton(onClick = { onOpenCode(summary.code) }) {
                            Text("Refresh")
                        }
                    }
                }
            }
        }

        item {
            RemoteThumbnail(
                urls = buildCoverThumbnailUrls(summary.mediaId, summary.coverExt),
                contentDescription = "Cover for code ${summary.code}",
                onClick = { onOpenSlideshow(1) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 300.dp)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Tags",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (detail.tagsByType.isEmpty()) {
                        Text(
                            text = "(none)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        detail.tagsByType.entries.forEach { (type, names) ->
                            val normalizedType = type.trim().lowercase(Locale.US)
                            val cleanedNames = names
                                .map { rawName ->
                                    if (normalizedType == "artist" || normalizedType == "group") {
                                        normalizeCreatorDisplayName(rawName)
                                    } else {
                                        rawName.trim()
                                    }
                                }
                                .filter { it.isNotBlank() }
                                .distinctBy { it.lowercase(Locale.US) }
                            if (cleanedNames.isEmpty()) return@forEach

                            Text(
                                text = "${type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }}:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                cleanedNames.forEach { name ->
                                    val clickableCreator = normalizedType == "artist" || normalizedType == "group"
                                    val localCount = detail.tagCountsByKey[browserTagLookupKey(type = normalizedType, name = name)]
                                    BrowserDetailTagChip(
                                        name = name,
                                        count = localCount,
                                        onClick = {
                                            if (clickableCreator) {
                                                onOpenCreator(normalizedType, name)
                                            } else {
                                                onSearchTag(name)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Gallery",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(thumbRows, key = { row -> row.firstOrNull()?.pageNumber ?: -1 }) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { thumb ->
                    Box(modifier = Modifier.weight(1f)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            RemoteThumbnail(
                                urls = thumb.thumbnailUrls,
                                contentDescription = "Page ${thumb.pageNumber} thumbnail",
                                onClick = { onOpenSlideshow(thumb.pageNumber) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp)
                            )
                            Text(
                                text = "Page ${thumb.pageNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (row.size < 3) {
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Comments",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (detail.comments.isEmpty()) {
                        Text(
                            text = "(none found)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        detail.comments.forEach { comment ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = comment.author.ifBlank { "Anonymous" },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = comment.text.ifBlank { "(empty)" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingImportAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingImportAction = null },
            title = { Text("Import Required") },
            text = {
                Text(
                    text = "You must import this sauce to change rating or read status. Import now?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val confirmedAction = action
                        pendingImportAction = null
                        scope.launch {
                            localLibraryLoading = true
                            localLibraryState = withContext(Dispatchers.IO) {
                                db.upsertGallery(buildImportGalleryData())
                                if (db.getEntryDetail(summary.code) != null) {
                                    applyLibraryActionInDb(confirmedAction)
                                }
                                readLocalLibraryState()
                            }
                            localLibraryLoading = false
                            if (!localLibraryState.exists) {
                                Toast.makeText(
                                    context,
                                    "Could not import code ${summary.code}.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingImportAction = null }) {
                    Text("No")
                }
            }
        )
    }
}

@Composable
private fun BrowserDetailTagChip(
    name: String,
    count: Int?,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val countLabel = count?.takeIf { it >= 0 }?.let(::formatCompactTagCount)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                shape = shape
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.78f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        if (!countLabel.isNullOrBlank()) {
            Text(
                text = countLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RemoteThumbnail(
    urls: List<String>,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbShape = RoundedCornerShape(12.dp)
    val candidates = remember(urls) {
        urls.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }
    val cacheKey = remember(candidates) { candidates.firstOrNull().orEmpty() }
    val initialBitmap = remember(candidates) {
        candidates.firstNotNullOfOrNull { GalleryBrowserThumbnailCache.get(it) }
    }
    val bitmap by produceState<ImageBitmap?>(initialValue = initialBitmap, candidates) {
        if (candidates.isEmpty()) {
            value = null
            return@produceState
        }
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            fetchGalleryBrowserThumbnail(candidates)
        }?.also { loaded ->
            if (cacheKey.isNotBlank()) {
                GalleryBrowserThumbnailCache.put(cacheKey, loaded)
            }
        }
    }

    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = thumbShape
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f),
                shape = thumbShape
            )
            .clip(thumbShape)
            .clickable(onClick = onClick)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap ?: return@Box,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Loading",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private object GalleryBrowserThumbnailCache {
    private const val MAX_ITEMS = 220
    private val map = object : LinkedHashMap<String, ImageBitmap>(MAX_ITEMS, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean {
            return size > MAX_ITEMS
        }
    }

    @Synchronized
    fun get(url: String): ImageBitmap? = map[url]

    @Synchronized
    fun put(url: String, bitmap: ImageBitmap) {
        if (url.isBlank()) return
        map[url] = bitmap
    }

    @Synchronized
    fun clear() {
        map.clear()
    }
}

private val galleryBrowserImageClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
        .cookieJar(CookieJar.NO_COOKIES)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(14, TimeUnit.SECONDS)
        .build()
}

private fun clearGalleryBrowserPrivacyArtifacts(
    api: GalleryCodeBrowserApi?
) {
    runCatching { api?.clearSession() }
    runCatching { galleryBrowserImageClient.dispatcher.cancelAll() }
    runCatching { galleryBrowserImageClient.connectionPool.evictAll() }
    runCatching {
        val manager = CookieManager.getInstance()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            manager.removeAllCookies(null)
            manager.flush()
        } else {
            @Suppress("DEPRECATION")
            manager.removeAllCookie()
        }
    }
    runCatching { WebStorage.getInstance().deleteAllData() }
    GalleryBrowserThumbnailCache.clear()
}

private fun fetchGalleryBrowserThumbnail(urls: List<String>): ImageBitmap? {
    urls.forEach { url ->
        GalleryBrowserThumbnailCache.get(url)?.let { return it }
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
            )
            .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
            .header("Referer", "https://nhentai.net/")
            .build()
        val fetched = runCatching {
            galleryBrowserImageClient.newCall(request).execute().use { rsp ->
                if (!rsp.isSuccessful) return@use null
                val bytes = rsp.body?.bytes() ?: return@use null
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
            }
        }.getOrNull()
        if (fetched != null) {
            urls.forEach { candidate -> GalleryBrowserThumbnailCache.put(candidate, fetched) }
            return fetched
        }
    }
    return null
}

private fun parseGallerySummary(obj: JSONObject): BrowserGallerySummary? {
    val code = obj.optInt("id", 0).takeIf { it > 0 } ?: return null
    val mediaId = parseApiMediaId(obj.opt("media_id"))
    val titleObj = obj.optJSONObject("title") ?: JSONObject()
    val title = listOf(
        titleObj.optString("english", "").trim(),
        titleObj.optString("japanese", "").trim(),
        titleObj.optString("pretty", "").trim()
    ).firstOrNull { it.isNotBlank() } ?: "Gallery $code"
    val subtitle = titleObj.optString("pretty", "").trim()
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

private fun parseGalleryDetail(
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

private fun parseGallerySummariesFromHtml(html: String): List<BrowserGallerySummary> {
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

private fun parseGalleryCommentsFromApiJson(payload: String?): List<BrowserGalleryComment> {
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

        if (author.isBlank() || text.isBlank()) continue
        comments += BrowserGalleryComment(author = author, text = text)
    }
    return comments.distinctBy { "${it.author}\u0000${it.text}" }.take(80)
}

private fun extractCommentAuthorFromJson(obj: JSONObject): String {
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

private fun parseGalleryCommentsFromHtml(html: String): List<BrowserGalleryComment> {
    if (html.isBlank()) return emptyList()
    val commentsSection = extractCommentsSection(html)
    if (commentsSection.isBlank()) return emptyList()

    val section = commentsSection
        .replace(
            Regex(
                """<script\b[^>]*>.*?</script>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            ),
            " "
        )
        .replace(
            Regex(
                """<style\b[^>]*>.*?</style>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            ),
            " "
        )

    val authorPatterns = listOf(
        Regex("""class=["'][^"']*(?:comment-author|username|author|poster|commenter)[^"']*["'][^>]*>(.*?)</""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
        Regex("""<h[1-6][^>]*>(.*?)</h[1-6]>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
        Regex("""<a[^>]*>(.*?)</a>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    )
    val bodyPatterns = listOf(
        Regex("""class=["'][^"']*(?:comment-text|comment-body|comment-content|content|body)[^"']*["'][^>]*>(.*?)</""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
        Regex("""<p[^>]*>(.*?)</p>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    )

    fun findFirstText(source: String, patterns: List<Regex>): String {
        patterns.forEach { regex ->
            val matched = regex.find(source)?.groupValues?.getOrNull(1).orEmpty()
            val cleaned = cleanHtmlText(matched)
            if (cleaned.isNotBlank()) return cleaned
        }
        return ""
    }

    val found = mutableListOf<BrowserGalleryComment>()
    val blockRegex = Regex(
        """<div[^>]*class=["'][^"']*comment[^"']*["'][^>]*>(.*?)</div>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    blockRegex.findAll(section).forEach { match ->
        val block = match.groupValues.getOrNull(1).orEmpty()
        if (block.isBlank()) return@forEach
        val author = findFirstText(block, authorPatterns)
        val text = findFirstText(block, bodyPatterns)
        if (author.isBlank() || text.isBlank()) return@forEach
        found += BrowserGalleryComment(author = author, text = text)
    }

    if (found.isNotEmpty()) {
        return found.distinctBy { "${it.author}\u0000${it.text}" }.take(60)
    }

    val authorList = authorPatterns
        .asSequence()
        .flatMap { pattern ->
            pattern.findAll(section).mapNotNull { result ->
                cleanHtmlText(result.groupValues.getOrNull(1).orEmpty()).takeIf { it.isNotBlank() }
            }
        }
        .toList()
    val bodyList = bodyPatterns
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
            if (author.isBlank() || text.isBlank()) continue
            add(BrowserGalleryComment(author = author, text = text))
        }
    }.distinctBy { "${it.author}\u0000${it.text}" }.take(60)
}

private fun extractCommentsSection(html: String): String {
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

    var depth = 0
    var cursor = 0
    var closedAt = -1
    while (cursor < chunk.length) {
        val openDiv = chunk.indexOf("<div", cursor, ignoreCase = true)
        val closeDiv = chunk.indexOf("</div>", cursor, ignoreCase = true)
        val openSec = chunk.indexOf("<section", cursor, ignoreCase = true)
        val closeSec = chunk.indexOf("</section>", cursor, ignoreCase = true)

        val next = listOf(openDiv, closeDiv, openSec, closeSec)
            .filter { it >= 0 }
            .minOrNull() ?: break

        when (next) {
            openDiv, openSec -> {
                depth += 1
                cursor = next + 4
            }
            closeDiv -> {
                depth = (depth - 1).coerceAtLeast(0)
                cursor = next + 6
                if (depth == 0 && next > markerIndex - start) {
                    closedAt = cursor
                    break
                }
            }
            closeSec -> {
                depth = (depth - 1).coerceAtLeast(0)
                cursor = next + 10
                if (depth == 0 && next > markerIndex - start) {
                    closedAt = cursor
                    break
                }
            }
        }
    }

    return if (closedAt > 0) {
        chunk.substring(0, closedAt)
    } else {
        chunk
    }
}

private fun cleanHtmlText(raw: String): String {
    if (raw.isBlank()) return ""
    val plain = Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString()
    return plain.replace(Regex("\\s+"), " ").trim()
}

private fun parseApiMediaId(raw: Any?): Long {
    val parsed = when (raw) {
        is Number -> raw.toLong()
        is String -> raw.trim().toLongOrNull() ?: 0L
        else -> 0L
    }
    return parsed.coerceAtLeast(0L)
}

private fun parseApiUploadDate(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return runCatching {
        Instant.ofEpochSecond(timestamp)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .format(GALLERY_BROWSER_UPLOAD_DATE_FORMAT)
    }.getOrDefault("")
}

private fun parseApiImageExtension(raw: String?): String {
    return when (raw?.trim()?.lowercase(Locale.US).orEmpty()) {
        "j", "jpg", "jpeg" -> "jpg"
        "p", "png" -> "png"
        "w", "webp" -> "webp"
        "g", "gif" -> "gif"
        else -> ""
    }
}

private fun buildCoverThumbnailUrls(mediaId: Long, preferredExt: String): List<String> {
    if (mediaId <= 0L) return emptyList()
    val extOrder = buildImageExtensionOrder(preferredExt)
    return extOrder.map { ext -> "https://t.nhentai.net/galleries/$mediaId/cover.$ext" }
}

private fun buildPageThumbnailUrls(
    mediaId: Long,
    pageNumber: Int,
    preferredExt: String
): List<String> {
    if (mediaId <= 0L || pageNumber <= 0) return emptyList()
    val extOrder = buildImageExtensionOrder(preferredExt)
    return extOrder.map { ext -> "https://t.nhentai.net/galleries/$mediaId/${pageNumber}t.$ext" }
}

private fun buildImageExtensionOrder(preferredExt: String): List<String> {
    val preferred = parseApiImageExtension(preferredExt)
    return buildList {
        if (preferred.isNotBlank()) add(preferred)
        add("jpg")
        add("png")
        add("webp")
        add("gif")
    }.distinct()
}

private fun galleryBrowserAccentColorForMode(mode: AccentMode): Color? {
    return when (mode) {
        AccentMode.AUTO -> null
        AccentMode.RED -> Color(0xFFE53935)
        AccentMode.ORANGE -> Color(0xFFFB8C00)
        AccentMode.AMBER -> Color(0xFFF9A825)
        AccentMode.GREEN -> Color(0xFF43A047)
        AccentMode.TEAL -> Color(0xFF00897B)
        AccentMode.BLUE -> Color(0xFF1E88E5)
        AccentMode.INDIGO -> Color(0xFF5E35B1)
        AccentMode.PINK -> Color(0xFFD81B60)
    }
}

private fun galleryBrowserPreferredOnAccent(color: Color): Color {
    val lum = (0.299f * color.red) + (0.587f * color.green) + (0.114f * color.blue)
    return if (lum >= 0.62f) Color(0xFF111111) else Color.White
}

private fun applyGalleryBrowserAccentMode(
    baseScheme: androidx.compose.material3.ColorScheme,
    accentMode: AccentMode,
    isDark: Boolean
): androidx.compose.material3.ColorScheme {
    val accent = galleryBrowserAccentColorForMode(accentMode) ?: return baseScheme
    val onAccent = galleryBrowserPreferredOnAccent(accent)
    val container = accent.copy(alpha = if (isDark) 0.34f else 0.22f)
    return baseScheme.copy(
        primary = accent,
        onPrimary = onAccent,
        secondary = accent,
        tertiary = accent,
        primaryContainer = container,
        secondaryContainer = container,
        tertiaryContainer = container
    )
}

private fun Modifier.browserPrivacyObfuscate(
    enabled: Boolean,
    overlayColor: Color,
    blurRadius: Dp = 7.dp,
    expandHorizontal: Dp = 2.dp,
    expandVertical: Dp = 1.dp,
    cornerRadius: Dp = 12.dp
): Modifier {
    if (!enabled) return this
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .padding(horizontal = expandHorizontal, vertical = expandVertical)
        .clip(shape)
        .blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        .drawWithContent {
            drawContent()
            val radius = cornerRadius.toPx()
            drawRoundRect(
                color = overlayColor,
                cornerRadius = CornerRadius(radius, radius)
            )
            drawRoundRect(
                color = overlayColor.copy(alpha = (overlayColor.alpha + 0.16f).coerceAtMost(1f)),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        .clip(shape)
}

private fun copyTextToClipboard(
    context: Context,
    label: String,
    value: String,
    successMessage: String = "Copied to clipboard."
) {
    val cleaned = value.trim()
    if (cleaned.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, cleaned))
    Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
}

private fun extractImportCandidateFromClipboard(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return null
    val firstLine = trimmed.lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
    if (firstLine.isBlank()) return null

    val codePattern = Regex("^#?\\d{1,7}$")
    val creatorTypedPattern = Regex("^(artist|group)\\s*:\\s*.+$", RegexOption.IGNORE_CASE)
    val creatorLinkPattern = Regex("^https?://(?:www\\.)?nhentai\\.net/(artist|group)/[^\\s/]+/?$", RegexOption.IGNORE_CASE)
    val galleryLinkPattern = Regex("^https?://(?:www\\.)?nhentai\\.net/g/\\d+/?$", RegexOption.IGNORE_CASE)

    if (
        codePattern.matches(firstLine) ||
        creatorTypedPattern.matches(firstLine) ||
        creatorLinkPattern.matches(firstLine) ||
        galleryLinkPattern.matches(firstLine)
    ) {
        return firstLine
    }

    if (firstLine.length <= 80) {
        val tokenCount = firstLine.split(Regex("\\s+")).count { it.isNotBlank() }
        if (tokenCount in 1..6) return firstLine
    }
    return null
}

private fun browserTagLookupKey(type: String, name: String): String {
    val normalizedType = type.trim().lowercase(Locale.US).ifBlank { "tag" }
    val normalizedName = if (normalizedType == "artist" || normalizedType == "group") {
        normalizeCreatorDisplayName(name)
    } else {
        name.trim()
    }.lowercase(Locale.US)
        .replace(Regex("\\s+"), " ")
        .trim()
    return "$normalizedType::$normalizedName"
}

private fun formatCompactTagCount(count: Int): String {
    val safe = count.coerceAtLeast(0)
    return when {
        safe >= 1_000_000 -> "${safe / 1_000_000}m"
        safe >= 1_000 -> "${safe / 1_000}k"
        else -> safe.toString()
    }
}

private fun encodeTagSearchTerm(tagName: String): String {
    val words = tagName.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    if (words.isEmpty()) return ""
    return if (words.size == 1) words.first() else "\"${words.joinToString(" ")}\""
}

private fun normalizeCreatorDisplayName(raw: String): String {
    val normalized = raw
        .replace("｜", "|")
        .replace(Regex("\\s*\\|\\s*"), "|")
        .trim()
    if (normalized.isBlank()) return ""
    val parts = normalized.split("|")
        .map { parseCreatorSlug(it) }
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (parts.isEmpty()) return ""
    return if (parts.size == 1) {
        parts.first()
    } else {
        parts.joinToString("|")
    }
}

private fun parseCreatorSlug(rawSlug: String): String {
    var cleaned = rawSlug.trim().trim('/')
    while (cleaned.isNotEmpty() && GALLERY_BROWSER_URL_TRAILING_PUNCT.contains(cleaned.last())) {
        cleaned = cleaned.dropLast(1)
    }
    if (cleaned.isBlank()) return ""
    val decoded = Uri.decode(cleaned)
        .replace("+", " ")
        .replace("-", " ")
        .replace("_", " ")
    return decoded
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" ")
}

private fun toCreatorUrlSlug(name: String): String {
    val normalizedDisplay = normalizeCreatorDisplayName(name)
    if (normalizedDisplay.isBlank()) return ""
    val parts = normalizedDisplay.split("|")
        .map { parseCreatorSlug(it) }
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (parts.isEmpty()) return ""
    return parts.joinToString("-") { part ->
        part.replace(Regex("\\s+"), "-").lowercase(Locale.US)
    }
}

private fun normalizeBrowserRouteType(rawType: String): String {
    return when (rawType.trim().lowercase(Locale.US)) {
        "artist" -> "artist"
        "group" -> "group"
        "tag", "tags" -> "tag"
        "language" -> "language"
        "character" -> "character"
        "parody" -> "parody"
        "category" -> "category"
        else -> ""
    }
}

private fun toBrowserRouteSlug(routeType: String, name: String): String {
    val normalizedType = normalizeBrowserRouteType(routeType)
    if (normalizedType.isBlank()) return ""
    return if (normalizedType == "artist" || normalizedType == "group") {
        toCreatorUrlSlug(name)
    } else {
        parseCreatorSlug(name)
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString("-") { it.lowercase(Locale.US) }
    }
}

private fun buildApiTagSearchQuery(
    includeTagNames: List<String>,
    excludeTagNames: List<String> = emptyList()
): String {
    fun normalize(raw: String): List<String> {
        return raw.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
    }

    fun encodeTerm(rawName: String, excluded: Boolean): String? {
        val words = normalize(rawName)
        if (words.isEmpty()) return null
        val quoted = if (words.size == 1) {
            words.first()
        } else {
            "\"${words.joinToString(" ")}\""
        }
        return if (excluded) "-$quoted" else quoted
    }

    val includeTerms = includeTagNames
        .asSequence()
        .mapNotNull { encodeTerm(it, excluded = false) }
        .toList()
    val excludeTerms = excludeTagNames
        .asSequence()
        .mapNotNull { encodeTerm(it, excluded = true) }
        .toList()
    return (includeTerms + excludeTerms).joinToString(" ").trim()
}
