package com.example.saucetracker

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.text.Html
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.roundToInt

private const val APP_TITLE = "Sauce Tracker"
private const val EXPORT_PREFIX = "Sauce exported Date"
private const val EXPORT_FORMAT = "NH_TAGBOOK_EXPORT_V1"
private const val PREFS_NAME = "nhtagbook_prefs"
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_ACCENT_MODE = "accent_mode"
private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
private const val KEY_APP_LOCK_BIOMETRIC_ENABLED = "app_lock_biometric_enabled"
private const val KEY_APP_LOCK_PIN_HASH = "app_lock_pin_hash"
private const val KEY_APP_LOCK_PIN_SALT = "app_lock_pin_salt"
private const val KEY_APP_LOCK_GRACE_UNTIL = "app_lock_grace_until"
private const val KEY_SHOW_THUMBNAILS = "show_thumbnails"
private const val KEY_PURE_GALLERY_MODE = "pure_gallery_mode"
private const val KEY_GALLERY_COLUMNS = "gallery_columns"
private const val KEY_PRELOAD_ON_LAUNCH = "preload_on_launch"
private const val KEY_PRELOAD_PERCENT = "preload_percent"
private const val KEY_AUTO_BACKUP_TREE_URI = "auto_backup_tree_uri"
private const val KEY_APPLY_BLOCKED_TAGS_HOME = "apply_blocked_tags_home"
private const val KEY_APPLY_BLOCKED_TAGS_SEARCH = "apply_blocked_tags_search"
private const val PROCEDURAL_BACKUP_FILENAME = "procedural_backup.txt"
private const val THUMB_PRELOAD_MIN_PARALLEL = 4
private const val THUMB_PRELOAD_MAX_PARALLEL = 10
private const val APP_LOCK_GRACE_MS = 30_000L
private const val INCOGNITO_OVERLAY_ALPHA = 0.82f
private const val POPULAR_TAG_FETCH_MAX_PAGES = 500
const val EXTRA_BROWSER_IMPORT_INPUT = "extra_browser_import_input"
private val READ_STATE_COLOR = Color(0xFF22C55E)
private val UNREAD_STATE_COLOR = Color(0xFFEF4444)

private val CODE_PATTERN = Regex("(?<!\\d)#?(\\d{1,8})(?!\\d)")
private val SPLIT_CODE_PATTERN = Regex("(?<![#\\d])#?(\\d{1,3}(?:[ \\t]+\\d{1,3})+)(?!\\d)")
private val GALLERY_LINK_PATTERN = Regex(
    "(?i)(?:https?://)?(?:www\\.)?nhentai\\.net/(?:g|api/gallery)/(\\d{1,8})(?:/)?(?:[?#][^\\s]*)?"
)
private val NHENTAI_HOME_PATTERN = Regex("(?i)^(?:https?://)?(?:www\\.)?nhentai\\.net/?$")
private val CREATOR_LINK_PATTERN = Regex(
    "(?i)(?:https?://)?(?:www\\.)?nhentai\\.net/(artist|group)/([^/\\s?#]+)(?:/)?(?:[?#][^\\s]*)?"
)
private val DIRECT_ROUTE_LINK_PATTERN = Regex(
    "(?i)(?:https?://)?(?:www\\.)?nhentai\\.net/(tag|language|category|parody|character|artist|group)/([^/\\s?#]+)(?:/(?:popular|popular-week|popular-today))?(?:/)?(?:[?#][^\\s]*)?"
)
private val CREATOR_TYPED_INPUT_PATTERN = Regex("(?i)^(artist|group)\\s*:\\s*(.+)$")
private val CREATOR_NAME_LINE_PATTERN = Regex("^[\\p{L}\\p{N} _.'()\\-]{2,80}$")
private const val URL_TRAILING_PUNCT = ".,;:!?)]}"
private val SEARCH_FIELD_PATTERN = Regex(
    "(?i)\\b(code|title|subtitle|pages?|num pages|upload(?: date)?|rating|fetched(?: at)?|added(?: at)?|url|source(?: url)?|link|tags?|artist|group|parody|character|category|language|lang|type)\\s*:\\s*"
)
private val DATE_TOKEN_PATTERN = Regex("\\d{4}-\\d{2}-\\d{2}")
private val POPULAR_TAG_ANCHOR_PATTERN = Regex(
    "<a([^>]*?)href=[\"']/(tag|language|category|parody|character|artist|group)/([^\"'/?#]+)(?:/)?[^\"']*[\"'][^>]*>(.*?)</a>",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)
private val POPULAR_TAG_NAME_SPAN_PATTERN = Regex(
    "<span[^>]*class=[\"'][^\"']*\\bname\\b[^\"']*[\"'][^>]*>(.*?)</span>",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)
private val POPULAR_TAG_COUNT_SPAN_PATTERN = Regex(
    "<span[^>]*class=[\"'][^\"']*\\bcount\\b[^\"']*[\"'][^>]*>(.*?)</span>",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)
private val HTML_TAG_PATTERN = Regex("<[^>]+>")

private val UTC_TIMESTAMP_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)
private val EXPORT_FILENAME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.US)
private val UPLOAD_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

class MainActivity : ComponentActivity() {
    private lateinit var activityVm: TagBookViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityVm = ViewModelProvider(this)[TagBookViewModel::class.java]
        BrowserImportBridge.setListener { raw ->
            runOnUiThread {
                if (::activityVm.isInitialized) {
                    activityVm.importFromBrowserClipboard(raw)
                }
            }
        }

        window.attributes = window.attributes.apply {
            preferredRefreshRate = 120f
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            TagBookApp(activityVm)
        }

        handleIncomingShareIntent(intent)
        handleIncomingImportIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingShareIntent(intent)
        handleIncomingImportIntent(intent)
    }

    override fun onDestroy() {
        if (!isChangingConfigurations) {
            BrowserImportBridge.setListener(null)
        }
        super.onDestroy()
    }

    override fun onStop() {
        if (!isChangingConfigurations && ::activityVm.isInitialized) {
            activityVm.onHostStopped()
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (::activityVm.isInitialized) {
            activityVm.onHostResumed()
        }
    }

    private fun handleIncomingImportIntent(incoming: Intent?) {
        if (!::activityVm.isInitialized) return
        val raw = incoming?.getStringExtra(EXTRA_BROWSER_IMPORT_INPUT)?.trim().orEmpty()
        if (raw.isBlank()) return
        activityVm.importFromBrowserClipboard(raw)
        incoming?.removeExtra(EXTRA_BROWSER_IMPORT_INPUT)
    }

    private fun handleIncomingShareIntent(incoming: Intent?) {
        if (!::activityVm.isInitialized) return
        val intent = incoming ?: return
        if (!Intent.ACTION_SEND.equals(intent.action, ignoreCase = true)) return

        val sharedText = buildList {
            val direct = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
            if (direct.isNotBlank()) add(direct)
            val clipText = runCatching {
                intent.clipData
                    ?.takeIf { it.itemCount > 0 }
                    ?.getItemAt(0)
                    ?.coerceToText(this@MainActivity)
                    ?.toString()
                    ?.trim()
                    .orEmpty()
            }.getOrDefault("")
            if (clipText.isNotBlank()) add(clipText)
        }.firstOrNull { it.isNotBlank() }.orEmpty()

        if (sharedText.isBlank()) return
        activityVm.queueIncomingShareText(sharedText)

        intent.removeExtra(Intent.EXTRA_TEXT)
    }
}

enum class ThemeMode(val label: String) {
    SYSTEM("Auto"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class AccentMode(val label: String) {
    AUTO("Wallpaper"),
    RED("Red"),
    ORANGE("Orange"),
    AMBER("Amber"),
    GREEN("Green"),
    TEAL("Teal"),
    BLUE("Blue"),
    INDIGO("Indigo"),
    PINK("Pink")
}

enum class EntrySortField {
    CODE,
    TITLE,
    PAGES,
    UPLOAD,
    ADDED,
    RATING
}

enum class EntryReadFilterMode {
    ALL,
    READ,
    UNREAD
}

enum class TagSortField {
    NAME,
    TYPE,
    COUNT
}

enum class CreatorSortField {
    NAME,
    TYPE,
    COUNT
}

enum class SortDirection {
    ASC,
    DESC
}

enum class StatsRange(val label: String) {
    TODAY("Today"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
    ALL_TIME("All Time")
}

private enum class InAppBackActionType {
    ENTRY_SELECTION,
    TAGS_CARD_COLLAPSE,
    ENTRIES_CARD_COLLAPSE,
    CREATORS_CARD_COLLAPSE,
    CREATOR_ROW_EXPANDED
}

private data class InAppBackAction(
    val type: InAppBackActionType,
    val previousCode: Int? = null,
    val previousBoolean: Boolean? = null,
    val creatorId: Long? = null
)

private enum class SelectionAnchorContext {
    ENTRY,
    CREATOR_LINK
}

private data class SelectionAnchor(
    val context: SelectionAnchorContext,
    val code: Int,
    val creatorId: Long? = null,
    val yInRoot: Float
)

data class GalleryTag(val name: String, val type: String)

data class GalleryData(
    val code: Int,
    val title: String,
    val subtitle: String,
    val numPages: Int,
    val uploadDate: String,
    val sourceUrl: String,
    val mediaId: Long,
    val coverExt: String,
    val tags: List<GalleryTag>
)

data class EntryRow(
    val code: Int,
    val title: String,
    val numPages: Int,
    val uploadDate: String,
    val addedAt: String,
    val rating: Int,
    val isRead: Boolean,
    val pinned: Boolean,
    val fetchedAt: String,
    val sourceUrl: String,
    val thumbnailUrl: String,
    val tags: String
)

data class TagCountRow(
    val id: Long,
    val name: String,
    val type: String,
    val count: Int
)

data class TagRouteRef(
    val name: String,
    val type: String
)

data class PopularTagRow(
    val id: Long,
    val name: String,
    val type: String,
    val count: Int,
    val blocked: Boolean
)

data class PopularTagSeed(
    val name: String,
    val type: String,
    val count: Int
)

data class PopularTagFetchResult(
    val tags: List<PopularTagSeed>,
    val pagesFetched: Int
)

data class EntryDetail(
    val code: Int,
    val title: String,
    val subtitle: String,
    val sourceUrl: String,
    val mediaId: Long,
    val coverExt: String,
    val numPages: Int,
    val uploadDate: String,
    val rating: Int,
    val isRead: Boolean,
    val fetchedAt: String,
    val addedAt: String,
    val thumbnailUrl: String,
    val tagsByType: Map<String, List<String>>
)

data class SeriesCandidateRow(
    val code: Int,
    val title: String,
    val subtitle: String,
    val creatorKeys: Set<String> = emptySet()
)

data class SeriesEntryPreview(
    val code: Int,
    val title: String,
    val sequence: Int?,
    val score: Float
)

data class SeriesNeighbors(
    val previous: SeriesEntryPreview? = null,
    val next: SeriesEntryPreview? = null
)

data class ImportResult(
    val processed: Int,
    val imported: Int,
    val inserted: Int,
    val updated: Int,
    val skipped: Int,
    val creatorsProcessed: Int = 0,
    val creatorsAdded: Int = 0,
    val creatorsSkipped: Int = 0
)

data class ClearAllResult(
    val entriesCleared: Int,
    val creatorsCleared: Int
)

data class SavedStats(
    val entries: Int,
    val artists: Int,
    val groups: Int,
    val readEntries: Int
)

data class AnalyticsCountRow(
    val name: String,
    val type: String,
    val count: Int
)

data class ReadAnalyticsSnapshot(
    val readCounts: Map<StatsRange, Int> = emptyMap(),
    val pagesRead: Map<StatsRange, Int> = emptyMap(),
    val averageRatings: Map<StatsRange, Float> = emptyMap(),
    val topTags: Map<StatsRange, List<AnalyticsCountRow>> = emptyMap(),
    val topCreators: Map<StatsRange, List<AnalyticsCountRow>> = emptyMap()
)

data class BrowserRatingPromptState(
    val code: Int,
    val title: String,
    val rating: Int
)

data class PinTogglePromptState(
    val code: Int,
    val targetPinned: Boolean
)

data class SplitSequence(
    val start: Int,
    val endExclusive: Int,
    val raw: String,
    val merged: String
)

data class BatchProgressState(
    val total: Int,
    val processed: Int,
    val saved: Int,
    val notFound: Int,
    val failed: Int,
    val currentCode: Int?
)

data class StartupPreloadState(
    val phase: String,
    val completedSteps: Int,
    val totalSteps: Int,
    val thumbsDone: Int = 0,
    val thumbsTotal: Int = 0
)

data class ThumbnailPreviewState(
    val thumbnailUrl: String,
    val contentDescription: String
)

data class SplitPromptState(
    val count: Int,
    val preview: String
)

data class ShortPromptState(
    val count: Int,
    val preview: String
)

data class ManualCreatorPromptState(
    val phrase: String
)

data class BatchCreatorPromptState(
    val count: Int,
    val preview: String
)

data class CreatorRow(
    val id: Long,
    val name: String,
    val type: String,
    val entryCount: Int
)

data class CreatorEntryRow(
    val code: Int,
    val title: String
)

data class CreatorLink(
    val type: String,
    val name: String,
    val sourceUrl: String
)

data class SearchFieldFilter(
    val key: String,
    val value: String
)

data class ParsedSearchQuery(
    val freeText: String,
    val filters: List<SearchFieldFilter>
)

data class ParsedImportPayload(
    val entries: JSONArray,
    val creators: JSONArray
)

class GalleryNotFoundException(message: String) : Exception(message)
class GalleryFetchException(message: String) : Exception(message)

class NhentaiApiClient {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun fetchGallery(code: Int): GalleryData {
        if (code <= 0) {
            throw IllegalArgumentException("Code must be a positive integer.")
        }

        val request = Request.Builder()
            .url("https://nhentai.net/api/gallery/$code")
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
            )
            .header("Accept", "application/json")
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (exc: IOException) {
            throw GalleryFetchException("Network error while fetching code $code: ${exc.message ?: "unknown error"}")
        }

        response.use { rsp ->
            if (rsp.code == 404) {
                throw GalleryNotFoundException("Code $code does not exist on nhentai.")
            }
            if (!rsp.isSuccessful) {
                throw GalleryFetchException("HTTP ${rsp.code} while fetching code $code.")
            }

            val bodyText = rsp.body?.string()
                ?: throw GalleryFetchException("Server returned an empty response.")

            val json = try {
                JSONObject(bodyText)
            } catch (_: Exception) {
                throw GalleryFetchException("Server returned invalid JSON.")
            }

            val titleObj = json.optJSONObject("title") ?: JSONObject()
            val title = listOf(
                titleObj.optString("english", "").trim(),
                titleObj.optString("japanese", "").trim(),
                titleObj.optString("pretty", "").trim(),
            ).firstOrNull { it.isNotBlank() } ?: "Gallery $code"

            val subtitle = titleObj.optString("pretty", "").trim()
            val uploadTimestamp = json.optLong("upload_date", 0L)
            val uploadDate = if (uploadTimestamp > 0L) {
                Instant.ofEpochSecond(uploadTimestamp)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate()
                    .format(UPLOAD_DATE_FORMAT)
            } else {
                ""
            }

            val tags = mutableListOf<GalleryTag>()
            val rawTags = json.optJSONArray("tags") ?: JSONArray()
            for (index in 0 until rawTags.length()) {
                val rawTag = rawTags.optJSONObject(index) ?: continue
                val name = rawTag.optString("name", "").trim()
                val tagType = rawTag.optString("type", "tag").trim().lowercase(Locale.US)
                if (name.isNotBlank()) {
                    tags += GalleryTag(name = name, type = if (tagType.isBlank()) "tag" else tagType)
                }
            }
            val mediaId = parseMediaId(json.opt("media_id"))
            val coverExt = parseCoverExtension(
                json.optJSONObject("images")
                    ?.optJSONObject("cover")
                    ?.optString("t", "")
            )

            return GalleryData(
                code = code,
                title = title,
                subtitle = subtitle,
                numPages = json.optInt("num_pages", 0).coerceAtLeast(0),
                uploadDate = uploadDate,
                sourceUrl = "https://nhentai.net/g/$code/",
                mediaId = mediaId,
                coverExt = coverExt,
                tags = tags
            )
        }
    }

    fun resolveCreatorByName(nameInput: String): CreatorLink? {
        val typedInput = parseTypedCreatorInput(nameInput)
        val rawName = typedInput?.second ?: nameInput
        val forcedType = typedInput?.first

        val displayName = parseCreatorSlug(rawName)
        if (displayName.isBlank()) return null

        val slugCandidates = buildCreatorSlugCandidates(rawName)
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

            val resolvedFromFinalUrl = parseCreatorLink(rsp.request.url.toString())
            if (resolvedFromFinalUrl != null) {
                return resolvedFromFinalUrl
            }

            val name = parseCreatorSlug(cleanSlug)
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
            val slugName = parseCreatorSlug(slug)
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
                    val score = creatorMatchScore(
                        targetNormalized = normalizedTarget,
                        candidateNormalized = candidateNorm
                    )
                    if (score > bestScore) {
                        bestScore = score
                        val slug = toCreatorUrlSlug(candidateName)
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
            .url("https://nhentai.net/tags/popular?page=$page")
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
            )
            .header("Accept", "text/html,application/xhtml+xml,application/xml")
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (exc: IOException) {
            throw GalleryFetchException("Network error while fetching popular tags page $page: ${exc.message ?: "unknown error"}")
        }

        response.use { rsp ->
            if (!rsp.isSuccessful) {
                if (page > 1 && (rsp.code == 404 || rsp.code == 410)) {
                    return emptyList()
                }
                throw GalleryFetchException("HTTP ${rsp.code} while fetching popular tags page $page.")
            }
            val bodyText = rsp.body?.string().orEmpty()
            if (bodyText.isBlank()) return emptyList()
            return parsePopularTagsHtml(bodyText)
        }
    }

    private fun parsePopularTagsHtml(html: String): List<PopularTagSeed> {
        val rows = mutableListOf<PopularTagSeed>()

        POPULAR_TAG_ANCHOR_PATTERN.findAll(html).forEach { match ->
            val type = match.groupValues.getOrNull(2).orEmpty().trim().lowercase(Locale.US)
            if (type.isBlank()) return@forEach

            val slug = match.groupValues.getOrNull(3).orEmpty()
            val innerHtml = match.groupValues.getOrNull(4).orEmpty()

            val nameHtml = POPULAR_TAG_NAME_SPAN_PATTERN.find(innerHtml)?.groupValues?.getOrNull(1).orEmpty()
            val countHtml = POPULAR_TAG_COUNT_SPAN_PATTERN.find(innerHtml)?.groupValues?.getOrNull(1).orEmpty()

            val name = decodeHtmlSnippet(nameHtml).ifBlank {
                slugToName(slug)
            }
            if (name.isBlank()) return@forEach

            val count = parseCompactCount(countHtml)
            rows += PopularTagSeed(
                name = name,
                type = type,
                count = count.coerceAtLeast(0)
            )
        }

        return rows.distinctBy { normalizeTagName(it.name) to it.type }
    }

    private fun decodeHtmlSnippet(raw: String): String {
        val stripped = raw.replace(HTML_TAG_PATTERN, " ")
        val decoded = Html.fromHtml(stripped, Html.FROM_HTML_MODE_LEGACY).toString()
        return decoded.replace(Regex("\\s+"), " ").trim()
    }

    private fun slugToName(slug: String): String {
        val decoded = Uri.decode(slug.trim())
        return decoded
            .replace(Regex("[-_]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun parseCompactCount(raw: String): Int {
        val cleaned = decodeHtmlSnippet(raw)
            .replace(",", "")
            .replace(" ", "")
            .lowercase(Locale.US)
            .trim()
        if (cleaned.isBlank()) return 0

        val match = Regex("^(\\d+(?:\\.\\d+)?)([kmb])?\\+?$", RegexOption.IGNORE_CASE).find(cleaned)
        if (match != null) {
            val base = match.groupValues.getOrNull(1)?.toDoubleOrNull() ?: return 0
            val suffix = match.groupValues.getOrNull(2).orEmpty().lowercase(Locale.US)
            val multiplier = when (suffix) {
                "k" -> 1_000.0
                "m" -> 1_000_000.0
                "b" -> 1_000_000_000.0
                else -> 1.0
            }
            return (base * multiplier).roundToInt().coerceAtLeast(0)
        }

        return cleaned.filter { it.isDigit() }.toIntOrNull()?.coerceAtLeast(0) ?: 0
    }
}

class TagBookDatabase(context: Context) : SQLiteOpenHelper(context, "tagbook.db", null, 1) {
    init {
        migrateSchema(writableDatabase)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS entries (
                code INTEGER PRIMARY KEY,
                title TEXT NOT NULL,
                subtitle TEXT NOT NULL DEFAULT '',
                source_url TEXT NOT NULL,
                num_pages INTEGER NOT NULL DEFAULT 0,
                upload_date TEXT NOT NULL DEFAULT '',
                media_id INTEGER NOT NULL DEFAULT 0,
                cover_ext TEXT NOT NULL DEFAULT '',
                rating INTEGER NOT NULL DEFAULT 0,
                read_state INTEGER NOT NULL DEFAULT 0,
                read_at TEXT NOT NULL DEFAULT '',
                pinned INTEGER NOT NULL DEFAULT 0,
                fetched_at TEXT NOT NULL,
                added_at TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                normalized_name TEXT NOT NULL,
                pinned INTEGER NOT NULL DEFAULT 0,
                source_url TEXT NOT NULL DEFAULT '',
                UNIQUE(normalized_name, type)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS entry_tags (
                entry_code INTEGER NOT NULL,
                tag_id INTEGER NOT NULL,
                PRIMARY KEY (entry_code, tag_id),
                FOREIGN KEY (entry_code) REFERENCES entries(code) ON DELETE CASCADE,
                FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS popular_tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                normalized_name TEXT NOT NULL,
                tag_count INTEGER NOT NULL DEFAULT 0,
                blocked INTEGER NOT NULL DEFAULT 0,
                UNIQUE(normalized_name, type)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tags_type ON tags(type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tags_name ON tags(name)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_entry_tags_tag_id ON entry_tags(tag_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_popular_tags_type ON popular_tags(type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_popular_tags_name ON popular_tags(name)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_popular_tags_count ON popular_tags(tag_count)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Schema version 1 only.
    }

    private fun migrateSchema(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS popular_tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                normalized_name TEXT NOT NULL,
                tag_count INTEGER NOT NULL DEFAULT 0,
                blocked INTEGER NOT NULL DEFAULT 0,
                UNIQUE(normalized_name, type)
            )
            """.trimIndent()
        )
        if (!hasColumn(db, "popular_tags", "blocked")) {
            db.execSQL("ALTER TABLE popular_tags ADD COLUMN blocked INTEGER NOT NULL DEFAULT 0")
        }
        if (!hasColumn(db, "popular_tags", "tag_count")) {
            db.execSQL("ALTER TABLE popular_tags ADD COLUMN tag_count INTEGER NOT NULL DEFAULT 0")
        }
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_popular_tags_type ON popular_tags(type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_popular_tags_name ON popular_tags(name)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_popular_tags_count ON popular_tags(tag_count)")

        if (!hasColumn(db, "tags", "pinned")) {
            db.execSQL("ALTER TABLE tags ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
        }
        if (!hasColumn(db, "tags", "source_url")) {
            db.execSQL("ALTER TABLE tags ADD COLUMN source_url TEXT NOT NULL DEFAULT ''")
        }
        if (!hasColumn(db, "entries", "media_id")) {
            db.execSQL("ALTER TABLE entries ADD COLUMN media_id INTEGER NOT NULL DEFAULT 0")
        }
        if (!hasColumn(db, "entries", "cover_ext")) {
            db.execSQL("ALTER TABLE entries ADD COLUMN cover_ext TEXT NOT NULL DEFAULT ''")
        }
        if (!hasColumn(db, "entries", "pinned")) {
            db.execSQL("ALTER TABLE entries ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
        }
        if (!hasColumn(db, "entries", "read_state")) {
            db.execSQL("ALTER TABLE entries ADD COLUMN read_state INTEGER NOT NULL DEFAULT 0")
        }
        if (!hasColumn(db, "entries", "read_at")) {
            db.execSQL("ALTER TABLE entries ADD COLUMN read_at TEXT NOT NULL DEFAULT ''")
        }
        db.execSQL(
            """
            UPDATE tags
            SET pinned = CASE
                WHEN pinned IS NULL THEN 0
                WHEN pinned = 0 THEN 0
                ELSE 1
            END
            """.trimIndent()
        )
        db.execSQL("UPDATE tags SET source_url = '' WHERE source_url IS NULL")
        db.execSQL("UPDATE entries SET media_id = 0 WHERE media_id IS NULL")
        db.execSQL("UPDATE entries SET cover_ext = '' WHERE cover_ext IS NULL")
        db.execSQL(
            """
            UPDATE entries
            SET pinned = CASE
                WHEN pinned IS NULL THEN 0
                WHEN pinned = 0 THEN 0
                ELSE 1
            END
            """.trimIndent()
        )
        db.execSQL(
            """
            UPDATE entries
            SET read_state = CASE
                WHEN read_state IS NULL THEN 0
                WHEN read_state = 0 THEN 0
                ELSE 1
            END
            """.trimIndent()
        )
        db.execSQL("UPDATE entries SET read_at = '' WHERE read_at IS NULL")
        db.execSQL(
            """
            UPDATE entries
            SET read_at = COALESCE(NULLIF(added_at, ''), '')
            WHERE COALESCE(read_state, 0) = 1
              AND COALESCE(read_at, '') = ''
            """.trimIndent()
        )
        db.execSQL(
            """
            UPDATE popular_tags
            SET blocked = CASE
                WHEN blocked IS NULL THEN 0
                WHEN blocked = 0 THEN 0
                ELSE 1
            END
            """.trimIndent()
        )
        db.execSQL("UPDATE popular_tags SET tag_count = 0 WHERE tag_count IS NULL")
    }

    fun upsertGallery(gallery: GalleryData) {
        val db = writableDatabase
        val now = utcNowString()

        db.beginTransaction()
        try {
            val exists = entryExists(db, gallery.code)
            if (exists) {
                val values = ContentValues().apply {
                    put("title", gallery.title)
                    put("subtitle", gallery.subtitle)
                    put("source_url", gallery.sourceUrl)
                    put("num_pages", gallery.numPages.coerceAtLeast(0))
                    put("upload_date", gallery.uploadDate)
                    put("media_id", gallery.mediaId.coerceAtLeast(0L))
                    put("cover_ext", parseCoverExtension(gallery.coverExt))
                    put("fetched_at", now)
                }
                db.update("entries", values, "code = ?", arrayOf(gallery.code.toString()))
            } else {
                val values = ContentValues().apply {
                    put("code", gallery.code)
                    put("title", gallery.title)
                    put("subtitle", gallery.subtitle)
                    put("source_url", gallery.sourceUrl)
                    put("num_pages", gallery.numPages.coerceAtLeast(0))
                    put("upload_date", gallery.uploadDate)
                    put("media_id", gallery.mediaId.coerceAtLeast(0L))
                    put("cover_ext", parseCoverExtension(gallery.coverExt))
                    put("rating", 0)
                    put("read_state", 0)
                    put("read_at", "")
                    put("fetched_at", now)
                    put("added_at", now)
                }
                db.insertOrThrow("entries", null, values)
            }

            db.delete("entry_tags", "entry_code = ?", arrayOf(gallery.code.toString()))

            val deduped = LinkedHashMap<Pair<String, String>, GalleryTag>()
            gallery.tags.forEach { tag ->
                val normalized = normalizeTagName(tag.name)
                if (normalized.isBlank()) return@forEach
                val type = tag.type.trim().lowercase(Locale.US).ifBlank { "tag" }
                deduped[normalized to type] = GalleryTag(name = tag.name.trim(), type = type)
            }

            deduped.forEach { (key, tag) ->
                val normalizedName = key.first
                val type = key.second

                val tagValues = ContentValues().apply {
                    put("name", tag.name)
                    put("type", type)
                    put("normalized_name", normalizedName)
                }
                db.insertWithOnConflict("tags", null, tagValues, SQLiteDatabase.CONFLICT_IGNORE)

                val tagId = findTagId(db, normalizedName, type) ?: return@forEach
                val linkValues = ContentValues().apply {
                    put("entry_code", gallery.code)
                    put("tag_id", tagId)
                }
                db.insertWithOnConflict("entry_tags", null, linkValues, SQLiteDatabase.CONFLICT_IGNORE)
            }

            cleanupOrphanTags(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteEntry(code: Int) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("entries", "code = ?", arrayOf(code.toString()))
            cleanupOrphanTags(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun clearAllEntries(): ClearAllResult {
        val db = writableDatabase
        var entryTotal = 0
        var creatorTotal = 0
        db.beginTransaction()
        try {
            db.rawQuery("SELECT COUNT(*) FROM entries", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    entryTotal = cursor.getInt(0)
                }
            }
            db.rawQuery(
                """
                SELECT COUNT(*)
                FROM tags
                WHERE type IN ('artist', 'group')
                """.trimIndent(),
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    creatorTotal = cursor.getInt(0)
                }
            }
            db.delete("entries", null, null)
            db.delete("tags", "type IN ('artist', 'group')", null)
            cleanupOrphanTags(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return ClearAllResult(
            entriesCleared = entryTotal,
            creatorsCleared = creatorTotal
        )
    }

    fun getSavedStats(): SavedStats {
        var entriesTotal = 0
        var artistsTotal = 0
        var groupsTotal = 0
        var readTotal = 0

        readableDatabase.rawQuery("SELECT COUNT(*) FROM entries", null).use { cursor ->
            if (cursor.moveToFirst()) {
                entriesTotal = cursor.getInt(0)
            }
        }
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM entries WHERE COALESCE(read_state, 0) = 1",
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                readTotal = cursor.getInt(0)
            }
        }

        readableDatabase.rawQuery(
            """
            SELECT type, COUNT(*) AS total
            FROM tags
            WHERE type IN ('artist', 'group')
            GROUP BY type
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxType = cursor.getColumnIndexOrThrow("type")
            val idxTotal = cursor.getColumnIndexOrThrow("total")
            while (cursor.moveToNext()) {
                when ((cursor.getString(idxType) ?: "").lowercase(Locale.US)) {
                    "artist" -> artistsTotal = cursor.getInt(idxTotal)
                    "group" -> groupsTotal = cursor.getInt(idxTotal)
                }
            }
        }

        return SavedStats(
            entries = entriesTotal,
            artists = artistsTotal,
            groups = groupsTotal,
            readEntries = readTotal
        )
    }

    fun getReadAnalyticsSnapshot(
        tagLimit: Int = 5,
        creatorLimit: Int = 5
    ): ReadAnalyticsSnapshot {
        val safeTagLimit = tagLimit.coerceIn(1, 20)
        val safeCreatorLimit = creatorLimit.coerceIn(1, 20)
        val readCounts = linkedMapOf<StatsRange, Int>()
        val pagesRead = linkedMapOf<StatsRange, Int>()
        val averageRatings = linkedMapOf<StatsRange, Float>()
        val topTags = linkedMapOf<StatsRange, List<AnalyticsCountRow>>()
        val topCreators = linkedMapOf<StatsRange, List<AnalyticsCountRow>>()

        StatsRange.entries.forEach { range ->
            readCounts[range] = queryReadCount(range)
            pagesRead[range] = queryPagesRead(range)
            averageRatings[range] = queryAverageReadRating(range)
            topTags[range] = queryTopReadTags(range, safeTagLimit)
            topCreators[range] = queryTopReadCreators(range, safeCreatorLimit)
        }

        return ReadAnalyticsSnapshot(
            readCounts = readCounts,
            pagesRead = pagesRead,
            averageRatings = averageRatings,
            topTags = topTags,
            topCreators = topCreators
        )
    }

    private fun readDateRange(range: StatsRange): Pair<String, String>? {
        val today = LocalDate.now(ZoneOffset.UTC)
        val start = when (range) {
            StatsRange.TODAY -> today
            StatsRange.WEEK -> today.minusDays(6)
            StatsRange.MONTH -> today.withDayOfMonth(1)
            StatsRange.YEAR -> today.withDayOfYear(1)
            StatsRange.ALL_TIME -> null
        } ?: return null
        return start.format(UPLOAD_DATE_FORMAT) to today.format(UPLOAD_DATE_FORMAT)
    }

    private fun readRangeClause(range: StatsRange, alias: String = "e"): Pair<String, List<String>> {
        val bounds = readDateRange(range) ?: return "" to emptyList()
        val dateExpr = "substr(COALESCE(NULLIF($alias.read_at, ''), $alias.added_at), 1, 10)"
        return " AND $dateExpr BETWEEN ? AND ?" to listOf(bounds.first, bounds.second)
    }

    private fun queryReadCount(range: StatsRange): Int {
        val (rangeSql, rangeArgs) = readRangeClause(range, alias = "e")
        val sql = """
            SELECT COUNT(*)
            FROM entries e
            WHERE COALESCE(e.read_state, 0) = 1$rangeSql
        """.trimIndent()
        readableDatabase.rawQuery(sql, rangeArgs.toTypedArray()).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getInt(0)
            }
        }
        return 0
    }

    private fun queryPagesRead(range: StatsRange): Int {
        val (rangeSql, rangeArgs) = readRangeClause(range, alias = "e")
        val sql = """
            SELECT COALESCE(SUM(CASE WHEN e.num_pages > 0 THEN e.num_pages ELSE 0 END), 0)
            FROM entries e
            WHERE COALESCE(e.read_state, 0) = 1$rangeSql
        """.trimIndent()
        readableDatabase.rawQuery(sql, rangeArgs.toTypedArray()).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getInt(0).coerceAtLeast(0)
            }
        }
        return 0
    }

    private fun queryAverageReadRating(range: StatsRange): Float {
        val (rangeSql, rangeArgs) = readRangeClause(range, alias = "e")
        val sql = """
            SELECT AVG(CASE WHEN e.rating > 0 THEN CAST(e.rating AS REAL) END)
            FROM entries e
            WHERE COALESCE(e.read_state, 0) = 1$rangeSql
        """.trimIndent()
        readableDatabase.rawQuery(sql, rangeArgs.toTypedArray()).use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                return cursor.getDouble(0).toFloat().coerceIn(0f, 5f)
            }
        }
        return 0f
    }

    private fun queryTopReadTags(range: StatsRange, limit: Int): List<AnalyticsCountRow> {
        val safeLimit = limit.coerceIn(1, 50)
        val (rangeSql, rangeArgs) = readRangeClause(range, alias = "e")
        val sql = """
            SELECT t.name, t.type, COUNT(*) AS entry_count
            FROM entries e
            JOIN entry_tags et ON et.entry_code = e.code
            JOIN tags t ON t.id = et.tag_id
            WHERE COALESCE(e.read_state, 0) = 1
              AND t.type NOT IN ('artist', 'group')$rangeSql
            GROUP BY t.id, t.name, t.type
            ORDER BY entry_count DESC, LOWER(t.name) ASC
            LIMIT ?
        """.trimIndent()
        val args = rangeArgs.toMutableList().apply { add(safeLimit.toString()) }
        val rows = mutableListOf<AnalyticsCountRow>()
        readableDatabase.rawQuery(sql, args.toTypedArray()).use { cursor ->
            val idxName = cursor.getColumnIndexOrThrow("name")
            val idxType = cursor.getColumnIndexOrThrow("type")
            val idxCount = cursor.getColumnIndexOrThrow("entry_count")
            while (cursor.moveToNext()) {
                rows += AnalyticsCountRow(
                    name = cursor.getString(idxName)?.trim().orEmpty(),
                    type = cursor.getString(idxType)?.trim().orEmpty(),
                    count = cursor.getInt(idxCount).coerceAtLeast(0)
                )
            }
        }
        return rows
    }

    private fun queryTopReadCreators(range: StatsRange, limit: Int): List<AnalyticsCountRow> {
        val safeLimit = limit.coerceIn(1, 50)
        val (rangeSql, rangeArgs) = readRangeClause(range, alias = "e")
        val sql = """
            SELECT t.name, t.type, COUNT(*) AS entry_count
            FROM entries e
            JOIN entry_tags et ON et.entry_code = e.code
            JOIN tags t ON t.id = et.tag_id
            WHERE COALESCE(e.read_state, 0) = 1
              AND t.type IN ('artist', 'group')$rangeSql
            GROUP BY t.id, t.name, t.type
            ORDER BY entry_count DESC, LOWER(t.name) ASC
            LIMIT ?
        """.trimIndent()
        val args = rangeArgs.toMutableList().apply { add(safeLimit.toString()) }
        val rows = mutableListOf<AnalyticsCountRow>()
        readableDatabase.rawQuery(sql, args.toTypedArray()).use { cursor ->
            val idxName = cursor.getColumnIndexOrThrow("name")
            val idxType = cursor.getColumnIndexOrThrow("type")
            val idxCount = cursor.getColumnIndexOrThrow("entry_count")
            while (cursor.moveToNext()) {
                rows += AnalyticsCountRow(
                    name = cursor.getString(idxName)?.trim().orEmpty(),
                    type = cursor.getString(idxType)?.trim().orEmpty(),
                    count = cursor.getInt(idxCount).coerceAtLeast(0)
                )
            }
        }
        return rows
    }

    fun addCreator(name: String, creatorType: String, sourceUrl: String): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val added = upsertCreatorTag(db, name = name, creatorType = creatorType, sourceUrl = sourceUrl)
            cleanupOrphanTags(db)
            db.setTransactionSuccessful()
            added
        } finally {
            db.endTransaction()
        }
    }

    fun listCreators(
        textFilter: String,
        tagFilterIds: List<Long>,
        sortField: CreatorSortField,
        sortDirection: SortDirection
    ): List<CreatorRow> {
        val filterQuery = buildEntryFilterQuery(textFilter, tagFilterIds)
        val hasEntryFilter = filterQuery.whereClauses.isNotEmpty()

        val orderBy = when (sortField) {
            CreatorSortField.NAME -> "LOWER(t.name) ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, t.type ASC, entry_count DESC"
            CreatorSortField.TYPE -> "t.type ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, LOWER(t.name) ASC, entry_count DESC"
            CreatorSortField.COUNT -> "entry_count ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, t.type ASC, LOWER(t.name) ASC"
        }

        val sql = if (hasEntryFilter) {
            val entryWhere = filterQuery.whereClauses.joinToString(" AND ")
            """
                WITH filtered_entries AS (
                    SELECT e.code
                    FROM entries e
                    WHERE $entryWhere
                )
                SELECT
                    t.id,
                    t.name,
                    t.type,
                    COUNT(DISTINCT fe.code) AS entry_count
                FROM tags t
                LEFT JOIN entry_tags et ON et.tag_id = t.id
                LEFT JOIN filtered_entries fe ON fe.code = et.entry_code
                WHERE t.type IN ('artist', 'group')
                  AND fe.code IS NOT NULL
                GROUP BY t.id, t.name, t.type
                ORDER BY $orderBy
            """.trimIndent()
        } else {
            """
                SELECT
                    t.id,
                    t.name,
                    t.type,
                    COUNT(DISTINCT et.entry_code) AS entry_count
                FROM tags t
                LEFT JOIN entry_tags et ON et.tag_id = t.id
                WHERE t.type IN ('artist', 'group')
                GROUP BY t.id, t.name, t.type
                ORDER BY $orderBy
            """.trimIndent()
        }

        val rows = mutableListOf<CreatorRow>()
        val args = if (hasEntryFilter) filterQuery.args.toTypedArray() else emptyArray()
        readableDatabase.rawQuery(sql, args).use { cursor ->
            val idxId = cursor.getColumnIndexOrThrow("id")
            val idxName = cursor.getColumnIndexOrThrow("name")
            val idxType = cursor.getColumnIndexOrThrow("type")
            val idxCount = cursor.getColumnIndexOrThrow("entry_count")
            while (cursor.moveToNext()) {
                rows += CreatorRow(
                    id = cursor.getLong(idxId),
                    name = cursor.getString(idxName) ?: "",
                    type = cursor.getString(idxType) ?: "",
                    entryCount = cursor.getInt(idxCount)
                )
            }
        }
        return rows
    }

    fun listEntriesForCreator(
        tagId: Long,
        textFilter: String,
        tagFilterIds: List<Long>
    ): List<CreatorEntryRow> {
        val filterQuery = buildEntryFilterQuery(textFilter, tagFilterIds)
        val args = mutableListOf(tagId.toString())
        args += filterQuery.args

        val sql = buildString {
            append(
                """
                SELECT e.code, e.title
                FROM entries e
                JOIN entry_tags et ON et.entry_code = e.code
                WHERE et.tag_id = ?
                """.trimIndent()
            )
            if (filterQuery.whereClauses.isNotEmpty()) {
                append(" AND ")
                append(filterQuery.whereClauses.joinToString(" AND "))
            }
            append(" ORDER BY e.added_at DESC, e.code DESC")
        }

        val rows = mutableListOf<CreatorEntryRow>()
        readableDatabase.rawQuery(sql, args.toTypedArray()).use { cursor ->
            val idxCode = cursor.getColumnIndexOrThrow("code")
            val idxTitle = cursor.getColumnIndexOrThrow("title")
            while (cursor.moveToNext()) {
                val code = cursor.getInt(idxCode)
                val title = cursor.getString(idxTitle)?.trim().orEmpty().ifBlank { "Gallery $code" }
                rows += CreatorEntryRow(code = code, title = title)
            }
        }
        return rows
    }

    fun listAllEntryCodes(): List<Int> {
        val codes = mutableListOf<Int>()
        readableDatabase.rawQuery(
            """
            SELECT code
            FROM entries
            ORDER BY added_at DESC, code DESC
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxCode = cursor.getColumnIndexOrThrow("code")
            while (cursor.moveToNext()) {
                codes += cursor.getInt(idxCode)
            }
        }
        return codes
    }

    fun listSeriesCandidates(): List<SeriesCandidateRow> {
        val rows = mutableListOf<SeriesCandidateRow>()
        readableDatabase.rawQuery(
            """
            SELECT e.code, e.title, e.subtitle,
                   COALESCE(GROUP_CONCAT(t.normalized_name, '|'), '') AS creator_keys
            FROM entries e
            LEFT JOIN entry_tags et ON et.entry_code = e.code
            LEFT JOIN tags t ON t.id = et.tag_id AND t.type IN ('artist', 'group')
            GROUP BY e.code, e.title, e.subtitle
            ORDER BY added_at DESC, code DESC
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxCode = cursor.getColumnIndexOrThrow("code")
            val idxTitle = cursor.getColumnIndexOrThrow("title")
            val idxSubtitle = cursor.getColumnIndexOrThrow("subtitle")
            val idxCreatorKeys = cursor.getColumnIndexOrThrow("creator_keys")
            while (cursor.moveToNext()) {
                val creatorKeys = cursor.getString(idxCreatorKeys)
                    ?.split('|')
                    ?.asSequence()
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?.toSet()
                    .orEmpty()
                rows += SeriesCandidateRow(
                    code = cursor.getInt(idxCode),
                    title = cursor.getString(idxTitle)?.trim().orEmpty(),
                    subtitle = cursor.getString(idxSubtitle)?.trim().orEmpty(),
                    creatorKeys = creatorKeys
                )
            }
        }
        return rows
    }

    fun setEntryRating(code: Int, rating: Int) {
        val safeRating = rating.coerceIn(0, 5)
        val values = ContentValues().apply {
            put("rating", safeRating)
        }
        writableDatabase.update("entries", values, "code = ?", arrayOf(code.toString()))
    }

    fun setEntryRead(code: Int, isRead: Boolean) {
        val values = ContentValues().apply {
            put("read_state", if (isRead) 1 else 0)
            put("read_at", if (isRead) utcNowString() else "")
        }
        writableDatabase.update("entries", values, "code = ?", arrayOf(code.toString()))
    }

    fun setEntryPinned(code: Int, pinned: Boolean) {
        val values = ContentValues().apply {
            put("pinned", if (pinned) 1 else 0)
        }
        writableDatabase.update("entries", values, "code = ?", arrayOf(code.toString()))
    }

    private data class EntryFilterQuery(
        val whereClauses: List<String>,
        val args: List<String>
    )

    private fun buildEntryFilterQuery(
        textFilter: String,
        tagFilterIds: List<Long>
    ): EntryFilterQuery {
        val parsedQuery = parseSearchQuery(textFilter)
        val whereClauses = mutableListOf<String>()
        val args = mutableListOf<String>()

        val trimmedFilter = parsedQuery.freeText
        if (trimmedFilter.isNotEmpty()) {
            val freeTerms = extractSearchEverythingBrowserTerms(trimmedFilter)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .ifEmpty { listOf(trimmedFilter) }

            val tokenClauses = mutableListOf<String>()
            val tokenArgs = mutableListOf<String>()

            freeTerms.forEach { term ->
                val searchClauses = mutableListOf<String>()
                val searchArgs = mutableListOf<String>()
                val likeTerm = "%$term%"

                searchClauses += "CAST(e.code AS TEXT) LIKE ?"
                searchArgs += likeTerm

                parseCode(term)?.let { parsedCode ->
                    searchClauses += "e.code = ?"
                    searchArgs += parsedCode.toString()
                }

                searchClauses += "e.title LIKE ?"
                searchArgs += likeTerm
                searchClauses += "e.subtitle LIKE ?"
                searchArgs += likeTerm
                searchClauses += "e.upload_date LIKE ?"
                searchArgs += likeTerm
                searchClauses += "e.fetched_at LIKE ?"
                searchArgs += likeTerm
                searchClauses += "e.added_at LIKE ?"
                searchArgs += likeTerm
                searchClauses += "e.source_url LIKE ?"
                searchArgs += likeTerm
                searchClauses += """
                    EXISTS (
                        SELECT 1
                        FROM entry_tags etf
                        JOIN tags tf ON tf.id = etf.tag_id
                        WHERE etf.entry_code = e.code
                          AND (tf.name LIKE ? OR tf.type LIKE ?)
                    )
                """.trimIndent()
                searchArgs += likeTerm
                searchArgs += likeTerm

                tokenClauses += "(${searchClauses.joinToString("\n OR ")})"
                tokenArgs += searchArgs
            }

            if (tokenClauses.isNotEmpty()) {
                whereClauses += tokenClauses.joinToString("\n AND ")
                args += tokenArgs
            }
        }

        parsedQuery.filters.forEach { filter ->
            val value = filter.value.trim()
            if (value.isBlank()) return@forEach
            when (filter.key) {
                "code" -> {
                    val cleaned = value.removePrefix("#").trim()
                    val parsedCode = cleaned.toIntOrNull()
                    if (parsedCode != null) {
                        whereClauses += "e.code = ?"
                        args += parsedCode.toString()
                    } else {
                        whereClauses += "CAST(e.code AS TEXT) LIKE ?"
                        args += "%$value%"
                    }
                }
                "title" -> {
                    whereClauses += "e.title LIKE ?"
                    args += "%$value%"
                }
                "subtitle" -> {
                    whereClauses += "e.subtitle LIKE ?"
                    args += "%$value%"
                }
                "pages" -> {
                    val pageNumbers = extractNumericTokens(value)
                    when {
                        pageNumbers.size >= 2 -> {
                            val start = minOf(pageNumbers[0], pageNumbers[1])
                            val end = maxOf(pageNumbers[0], pageNumbers[1])
                            whereClauses += "e.num_pages BETWEEN ? AND ?"
                            args += start.toString()
                            args += end.toString()
                        }
                        pageNumbers.size == 1 -> {
                            whereClauses += "e.num_pages = ?"
                            args += pageNumbers.first().toString()
                        }
                        else -> {
                            whereClauses += "CAST(e.num_pages AS TEXT) LIKE ?"
                            args += "%$value%"
                        }
                    }
                }
                "upload" -> {
                    val dateRange = parseDateRange(value)
                    when {
                        dateRange != null -> {
                            whereClauses += "e.upload_date BETWEEN ? AND ?"
                            args += dateRange.first.format(UPLOAD_DATE_FORMAT)
                            args += dateRange.second.format(UPLOAD_DATE_FORMAT)
                        }
                        else -> {
                            val singleDate = parseFirstDate(value)
                            if (singleDate != null) {
                                whereClauses += "e.upload_date = ?"
                                args += singleDate.format(UPLOAD_DATE_FORMAT)
                            } else {
                                whereClauses += "e.upload_date LIKE ?"
                                args += "%$value%"
                            }
                        }
                    }
                }
                "rating" -> {
                    val ratingNumbers = extractNumericTokens(value)
                    when {
                        ratingNumbers.size >= 2 -> {
                            val start = minOf(ratingNumbers[0], ratingNumbers[1]).coerceIn(0, 5)
                            val end = maxOf(ratingNumbers[0], ratingNumbers[1]).coerceIn(0, 5)
                            whereClauses += "e.rating BETWEEN ? AND ?"
                            args += start.toString()
                            args += end.toString()
                        }
                        ratingNumbers.size == 1 -> {
                            whereClauses += "e.rating = ?"
                            args += ratingNumbers.first().coerceIn(0, 5).toString()
                        }
                        else -> {
                            whereClauses += "CAST(e.rating AS TEXT) LIKE ?"
                            args += "%$value%"
                        }
                    }
                }
                "fetched" -> {
                    whereClauses += "e.fetched_at LIKE ?"
                    args += "%$value%"
                }
                "added" -> {
                    val dateRange = parseDateRange(value)
                    when {
                        dateRange != null -> {
                            whereClauses += "substr(e.added_at, 1, 10) BETWEEN ? AND ?"
                            args += dateRange.first.format(UPLOAD_DATE_FORMAT)
                            args += dateRange.second.format(UPLOAD_DATE_FORMAT)
                        }
                        else -> {
                            val singleDate = parseFirstDate(value)
                            if (singleDate != null) {
                                whereClauses += "substr(e.added_at, 1, 10) = ?"
                                args += singleDate.format(UPLOAD_DATE_FORMAT)
                            } else {
                                whereClauses += "e.added_at LIKE ?"
                                args += "%$value%"
                            }
                        }
                    }
                }
                "url" -> {
                    whereClauses += "e.source_url LIKE ?"
                    args += "%$value%"
                }
                "tag" -> {
                    whereClauses += """
                        EXISTS (
                            SELECT 1
                            FROM entry_tags etf
                            JOIN tags tf ON tf.id = etf.tag_id
                            WHERE etf.entry_code = e.code
                              AND (tf.name LIKE ? OR tf.type LIKE ?)
                        )
                    """.trimIndent()
                    val term = "%$value%"
                    args += term
                    args += term
                }
                "type" -> {
                    whereClauses += """
                        EXISTS (
                            SELECT 1
                            FROM entry_tags etf
                            JOIN tags tf ON tf.id = etf.tag_id
                            WHERE etf.entry_code = e.code
                              AND tf.type LIKE ?
                        )
                    """.trimIndent()
                    args += "%$value%"
                }
                "artist", "group", "parody", "character", "category", "language" -> {
                    whereClauses += """
                        EXISTS (
                            SELECT 1
                            FROM entry_tags etf
                            JOIN tags tf ON tf.id = etf.tag_id
                            WHERE etf.entry_code = e.code
                              AND tf.type = ?
                              AND tf.name LIKE ?
                        )
                    """.trimIndent()
                    args += filter.key
                    args += "%$value%"
                }
            }
        }

        val uniqueTagIds = tagFilterIds
            .mapNotNull { it.takeIf { value -> value > 0L } }
            .distinct()

        if (uniqueTagIds.isNotEmpty()) {
            uniqueTagIds.forEach { tagId ->
                whereClauses += """
                    EXISTS (
                        SELECT 1
                        FROM entry_tags etg
                        WHERE etg.entry_code = e.code
                          AND etg.tag_id = ?
                    )
                """.trimIndent()
                args += tagId.toString()
            }
        }

        return EntryFilterQuery(
            whereClauses = whereClauses,
            args = args
        )
    }

    fun listEntries(
        textFilter: String,
        tagFilterIds: List<Long>,
        sortField: EntrySortField?,
        sortDirection: SortDirection,
        readFilter: EntryReadFilterMode
    ): List<EntryRow> {
        val filterQuery = buildEntryFilterQuery(textFilter, tagFilterIds)
        val whereClauses = filterQuery.whereClauses.toMutableList()
        val args = filterQuery.args.toMutableList()

        when (readFilter) {
            EntryReadFilterMode.ALL -> Unit
            EntryReadFilterMode.READ -> {
                whereClauses += "COALESCE(e.read_state, 0) = 1"
            }
            EntryReadFilterMode.UNREAD -> {
                whereClauses += "COALESCE(e.read_state, 0) = 0"
            }
        }

        val baseOrderBy = when (sortField) {
            EntrySortField.RATING -> "e.rating DESC, e.added_at DESC, e.code DESC"
            EntrySortField.CODE -> "e.code ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}"
            EntrySortField.TITLE -> "LOWER(e.title) ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, e.code DESC"
            EntrySortField.PAGES -> "e.num_pages ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, e.code DESC"
            EntrySortField.UPLOAD ->
                "CASE WHEN e.upload_date = '' THEN 1 ELSE 0 END ASC, e.upload_date ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, e.code DESC"
            EntrySortField.ADDED -> "e.added_at ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, e.code DESC"
            null -> "e.added_at DESC, e.code DESC"
        }
        val orderBy = "COALESCE(e.pinned, 0) DESC, $baseOrderBy"

        val sql = buildString {
            append(
                """
                SELECT
                    e.code,
                    e.title,
                    e.num_pages,
                    e.upload_date,
                    e.added_at,
                    e.rating,
                    e.read_state,
                    e.pinned,
                    e.fetched_at,
                    e.source_url,
                    e.media_id,
                    e.cover_ext,
                    COALESCE(
                        (
                            SELECT GROUP_CONCAT(t.name, ', ')
                            FROM entry_tags et
                            JOIN tags t ON t.id = et.tag_id
                            WHERE et.entry_code = e.code
                        ),
                        ''
                    ) AS tags
                FROM entries e
                """.trimIndent()
            )
            if (whereClauses.isNotEmpty()) {
                append(" WHERE ")
                append(whereClauses.joinToString(" AND "))
            }
            append(" ORDER BY ")
            append(orderBy)
        }

        val rows = mutableListOf<EntryRow>()
        readableDatabase.rawQuery(sql, args.toTypedArray()).use { cursor ->
            val idxCode = cursor.getColumnIndexOrThrow("code")
            val idxTitle = cursor.getColumnIndexOrThrow("title")
            val idxPages = cursor.getColumnIndexOrThrow("num_pages")
            val idxUpload = cursor.getColumnIndexOrThrow("upload_date")
            val idxAdded = cursor.getColumnIndexOrThrow("added_at")
            val idxRating = cursor.getColumnIndexOrThrow("rating")
            val idxRead = cursor.getColumnIndexOrThrow("read_state")
            val idxPinned = cursor.getColumnIndexOrThrow("pinned")
            val idxFetched = cursor.getColumnIndexOrThrow("fetched_at")
            val idxSourceUrl = cursor.getColumnIndexOrThrow("source_url")
            val idxMediaId = cursor.getColumnIndexOrThrow("media_id")
            val idxCoverExt = cursor.getColumnIndexOrThrow("cover_ext")
            val idxTags = cursor.getColumnIndexOrThrow("tags")

            while (cursor.moveToNext()) {
                val mediaId = cursor.getLong(idxMediaId).coerceAtLeast(0L)
                val coverExt = parseCoverExtension(cursor.getString(idxCoverExt) ?: "")
                rows += EntryRow(
                    code = cursor.getInt(idxCode),
                    title = cursor.getString(idxTitle) ?: "",
                    numPages = cursor.getInt(idxPages),
                    uploadDate = cursor.getString(idxUpload) ?: "",
                    addedAt = cursor.getString(idxAdded) ?: "",
                    rating = cursor.getInt(idxRating).coerceIn(0, 5),
                    isRead = cursor.getInt(idxRead) != 0,
                    pinned = cursor.getInt(idxPinned) != 0,
                    fetchedAt = cursor.getString(idxFetched) ?: "",
                    sourceUrl = cursor.getString(idxSourceUrl) ?: "",
                    thumbnailUrl = buildThumbnailUrl(mediaId, coverExt),
                    tags = cursor.getString(idxTags) ?: ""
                )
            }
        }
        return rows
    }

    fun listTagCounts(
        textFilter: String,
        sortField: TagSortField,
        sortDirection: SortDirection
    ): List<TagCountRow> {
        val parsedQuery = parseSearchQuery(textFilter)
        val args = mutableListOf<String>()
        val whereClauses = mutableListOf<String>()

        fun appendNameOrTypeLike(value: String) {
            val trimmed = value.trim()
            if (trimmed.isBlank()) return
            val term = "%$trimmed%"
            whereClauses += "(t.name LIKE ? OR t.type LIKE ?)"
            args += term
            args += term
        }

        if (parsedQuery.freeText.isNotBlank()) {
            appendNameOrTypeLike(parsedQuery.freeText)
        }

        parsedQuery.filters.forEach { filter ->
            val value = filter.value.trim()
            if (value.isBlank()) return@forEach
            when (filter.key) {
                "tag" -> appendNameOrTypeLike(value)
                "type" -> {
                    whereClauses += "t.type LIKE ?"
                    args += "%$value%"
                }
                "artist", "group", "parody", "character", "category", "language" -> {
                    whereClauses += "(t.type = ? AND t.name LIKE ?)"
                    args += filter.key
                    args += "%$value%"
                }
            }
        }

        val orderBy = when (sortField) {
            TagSortField.NAME -> "t.name ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, t.type ASC, entry_count DESC"
            TagSortField.TYPE -> "t.type ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, t.name ASC, entry_count DESC"
            TagSortField.COUNT -> "entry_count ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, t.type ASC, t.name ASC"
        }

        val sql = """
            SELECT
                t.id,
                t.name,
                t.type,
                COUNT(et.entry_code) AS entry_count
            FROM tags t
            JOIN entry_tags et ON et.tag_id = t.id
            ${if (whereClauses.isNotEmpty()) "WHERE ${whereClauses.joinToString(" AND ")}" else ""}
            GROUP BY t.id, t.name, t.type
            ORDER BY $orderBy
        """.trimIndent()

        val rows = mutableListOf<TagCountRow>()
        readableDatabase.rawQuery(sql, args.toTypedArray()).use { cursor ->
            val idxId = cursor.getColumnIndexOrThrow("id")
            val idxName = cursor.getColumnIndexOrThrow("name")
            val idxType = cursor.getColumnIndexOrThrow("type")
            val idxCount = cursor.getColumnIndexOrThrow("entry_count")
            while (cursor.moveToNext()) {
                rows += TagCountRow(
                    id = cursor.getLong(idxId),
                    name = cursor.getString(idxName) ?: "",
                    type = cursor.getString(idxType) ?: "",
                    count = cursor.getInt(idxCount)
                )
            }
        }
        return rows
    }

    fun listPopularTags(
        sortField: TagSortField,
        sortDirection: SortDirection
    ): List<PopularTagRow> {
        val orderBy = when (sortField) {
            TagSortField.NAME -> "name ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, type ASC, tag_count DESC"
            TagSortField.TYPE -> "type ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, name ASC, tag_count DESC"
            TagSortField.COUNT -> "tag_count ${if (sortDirection == SortDirection.ASC) "ASC" else "DESC"}, type ASC, name ASC"
        }

        val rows = mutableListOf<PopularTagRow>()
        readableDatabase.rawQuery(
            """
            SELECT id, name, type, tag_count, blocked
            FROM popular_tags
            ORDER BY $orderBy
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxId = cursor.getColumnIndexOrThrow("id")
            val idxName = cursor.getColumnIndexOrThrow("name")
            val idxType = cursor.getColumnIndexOrThrow("type")
            val idxCount = cursor.getColumnIndexOrThrow("tag_count")
            val idxBlocked = cursor.getColumnIndexOrThrow("blocked")
            while (cursor.moveToNext()) {
                rows += PopularTagRow(
                    id = cursor.getLong(idxId),
                    name = cursor.getString(idxName) ?: "",
                    type = cursor.getString(idxType) ?: "",
                    count = cursor.getInt(idxCount).coerceAtLeast(0),
                    blocked = cursor.getInt(idxBlocked) != 0
                )
            }
        }
        return rows
    }

    fun replacePopularTags(rows: List<PopularTagSeed>) {
        val deduped = linkedMapOf<Pair<String, String>, PopularTagSeed>()
        rows.forEach { row ->
            val normalizedName = normalizeTagName(row.name)
            val type = row.type.trim().lowercase(Locale.US)
            if (normalizedName.isBlank() || type.isBlank()) return@forEach
            val key = normalizedName to type
            val existing = deduped[key]
            if (existing == null || row.count > existing.count) {
                deduped[key] = PopularTagSeed(
                    name = row.name.trim(),
                    type = type,
                    count = row.count.coerceAtLeast(0)
                )
            }
        }

        val db = writableDatabase
        db.beginTransaction()
        try {
            val blockedByKey = linkedMapOf<Pair<String, String>, Int>()
            db.rawQuery(
                "SELECT normalized_name, type, blocked FROM popular_tags",
                null
            ).use { cursor ->
                val idxName = cursor.getColumnIndexOrThrow("normalized_name")
                val idxType = cursor.getColumnIndexOrThrow("type")
                val idxBlocked = cursor.getColumnIndexOrThrow("blocked")
                while (cursor.moveToNext()) {
                    val key = (cursor.getString(idxName) ?: "") to (cursor.getString(idxType) ?: "")
                    blockedByKey[key] = if (cursor.getInt(idxBlocked) != 0) 1 else 0
                }
            }

            db.delete("popular_tags", null, null)
            deduped.forEach { (key, row) ->
                val values = ContentValues().apply {
                    put("name", row.name)
                    put("type", key.second)
                    put("normalized_name", key.first)
                    put("tag_count", row.count.coerceAtLeast(0))
                    put("blocked", blockedByKey[key] ?: 0)
                }
                db.insertOrThrow("popular_tags", null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun setPopularTagBlocked(tagId: Long, blocked: Boolean) {
        if (tagId <= 0L) return
        val values = ContentValues().apply {
            put("blocked", if (blocked) 1 else 0)
        }
        writableDatabase.update("popular_tags", values, "id = ?", arrayOf(tagId.toString()))
    }

    fun clearAllBlockedPopularTags() {
        val values = ContentValues().apply {
            put("blocked", 0)
        }
        writableDatabase.update("popular_tags", values, null, null)
    }

    fun listBlockedPopularTagNames(): List<String> {
        val rows = mutableListOf<String>()
        readableDatabase.rawQuery(
            """
            SELECT MIN(name) AS name
            FROM popular_tags
            WHERE blocked = 1
            GROUP BY normalized_name
            ORDER BY LOWER(name) ASC
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxName = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                val name = cursor.getString(idxName)?.trim().orEmpty()
                if (name.isNotBlank()) {
                    rows += name
                }
            }
        }
        return rows
    }

    fun getEntryDetail(code: Int): EntryDetail? {
        val entry = readableDatabase.rawQuery(
            """
            SELECT code, title, subtitle, source_url, num_pages, upload_date, rating, read_state, fetched_at, added_at, media_id, cover_ext
            FROM entries
            WHERE code = ?
            """.trimIndent(),
            arrayOf(code.toString())
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                null
            } else {
                EntryDetail(
                    code = cursor.getInt(cursor.getColumnIndexOrThrow("code")),
                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")) ?: "",
                    subtitle = cursor.getString(cursor.getColumnIndexOrThrow("subtitle")) ?: "",
                    sourceUrl = cursor.getString(cursor.getColumnIndexOrThrow("source_url")) ?: "",
                    mediaId = cursor.getLong(cursor.getColumnIndexOrThrow("media_id")).coerceAtLeast(0L),
                    coverExt = parseCoverExtension(cursor.getString(cursor.getColumnIndexOrThrow("cover_ext")) ?: ""),
                    numPages = cursor.getInt(cursor.getColumnIndexOrThrow("num_pages")),
                    uploadDate = cursor.getString(cursor.getColumnIndexOrThrow("upload_date")) ?: "",
                    rating = cursor.getInt(cursor.getColumnIndexOrThrow("rating")).coerceIn(0, 5),
                    isRead = cursor.getInt(cursor.getColumnIndexOrThrow("read_state")) != 0,
                    fetchedAt = cursor.getString(cursor.getColumnIndexOrThrow("fetched_at")) ?: "",
                    addedAt = cursor.getString(cursor.getColumnIndexOrThrow("added_at")) ?: "",
                    thumbnailUrl = buildThumbnailUrl(
                        cursor.getLong(cursor.getColumnIndexOrThrow("media_id")).coerceAtLeast(0L),
                        parseCoverExtension(cursor.getString(cursor.getColumnIndexOrThrow("cover_ext")) ?: "")
                    ),
                    tagsByType = emptyMap()
                )
            }
        } ?: return null

        val tagsByType = linkedMapOf<String, MutableList<String>>()
        readableDatabase.rawQuery(
            """
            SELECT t.type, t.name
            FROM entry_tags et
            JOIN tags t ON t.id = et.tag_id
            WHERE et.entry_code = ?
            ORDER BY t.type ASC, t.name ASC
            """.trimIndent(),
            arrayOf(code.toString())
        ).use { cursor ->
            val idxType = cursor.getColumnIndexOrThrow("type")
            val idxName = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                val type = cursor.getString(idxType) ?: ""
                val name = cursor.getString(idxName) ?: ""
                tagsByType.getOrPut(type) { mutableListOf() }.add(name)
            }
        }

        return entry.copy(tagsByType = tagsByType)
    }

    fun exportSnapshot(): JSONObject {
        val entriesArray = JSONArray()
        val creatorsArray = JSONArray()
        val tagsByCode = mutableMapOf<Int, MutableList<JSONObject>>()

        readableDatabase.rawQuery(
            """
            SELECT et.entry_code AS code, t.name AS name, t.type AS type
            FROM entry_tags et
            JOIN tags t ON t.id = et.tag_id
            ORDER BY et.entry_code ASC, t.type ASC, t.name ASC
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxCode = cursor.getColumnIndexOrThrow("code")
            val idxName = cursor.getColumnIndexOrThrow("name")
            val idxType = cursor.getColumnIndexOrThrow("type")
            while (cursor.moveToNext()) {
                val code = cursor.getInt(idxCode)
                val tagObj = JSONObject()
                    .put("name", cursor.getString(idxName) ?: "")
                    .put("type", cursor.getString(idxType) ?: "tag")
                tagsByCode.getOrPut(code) { mutableListOf() }.add(tagObj)
            }
        }

        readableDatabase.rawQuery(
            """
            SELECT
                code, title, subtitle, source_url, num_pages, upload_date, rating, read_state, read_at, pinned, fetched_at, added_at, media_id, cover_ext
            FROM entries
            ORDER BY added_at DESC, code DESC
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxCode = cursor.getColumnIndexOrThrow("code")
            val idxTitle = cursor.getColumnIndexOrThrow("title")
            val idxSubtitle = cursor.getColumnIndexOrThrow("subtitle")
            val idxSource = cursor.getColumnIndexOrThrow("source_url")
            val idxPages = cursor.getColumnIndexOrThrow("num_pages")
            val idxUpload = cursor.getColumnIndexOrThrow("upload_date")
            val idxRating = cursor.getColumnIndexOrThrow("rating")
            val idxRead = cursor.getColumnIndexOrThrow("read_state")
            val idxReadAt = cursor.getColumnIndexOrThrow("read_at")
            val idxPinned = cursor.getColumnIndexOrThrow("pinned")
            val idxFetched = cursor.getColumnIndexOrThrow("fetched_at")
            val idxAdded = cursor.getColumnIndexOrThrow("added_at")
            val idxMediaId = cursor.getColumnIndexOrThrow("media_id")
            val idxCoverExt = cursor.getColumnIndexOrThrow("cover_ext")

            while (cursor.moveToNext()) {
                val code = cursor.getInt(idxCode)
                val tags = JSONArray()
                tagsByCode[code].orEmpty().forEach { tags.put(it) }
                val entry = JSONObject()
                    .put("code", code)
                    .put("title", cursor.getString(idxTitle) ?: "Gallery $code")
                    .put("subtitle", cursor.getString(idxSubtitle) ?: "")
                    .put("source_url", cursor.getString(idxSource) ?: "https://nhentai.net/g/$code/")
                    .put("num_pages", cursor.getInt(idxPages).coerceAtLeast(0))
                    .put("upload_date", cursor.getString(idxUpload) ?: "")
                    .put("rating", cursor.getInt(idxRating).coerceIn(0, 5))
                    .put("read", if (cursor.getInt(idxRead) != 0) 1 else 0)
                    .put("read_at", cursor.getString(idxReadAt) ?: "")
                    .put("pinned", if (cursor.getInt(idxPinned) != 0) 1 else 0)
                    .put("fetched_at", cursor.getString(idxFetched) ?: "")
                    .put("added_at", cursor.getString(idxAdded) ?: "")
                    .put("media_id", cursor.getLong(idxMediaId).coerceAtLeast(0L))
                    .put("cover_ext", parseCoverExtension(cursor.getString(idxCoverExt) ?: ""))
                    .put("tags", tags)
                entriesArray.put(entry)
            }
        }

        readableDatabase.rawQuery(
            """
            SELECT name, type, COALESCE(source_url, '') AS source_url
            FROM tags
            WHERE type IN ('artist', 'group')
              AND COALESCE(pinned, 0) = 1
            ORDER BY type ASC, name ASC
            """.trimIndent(),
            null
        ).use { cursor ->
            val idxName = cursor.getColumnIndexOrThrow("name")
            val idxType = cursor.getColumnIndexOrThrow("type")
            val idxSourceUrl = cursor.getColumnIndexOrThrow("source_url")
            while (cursor.moveToNext()) {
                creatorsArray.put(
                    JSONObject()
                        .put("name", cursor.getString(idxName) ?: "")
                        .put("type", cursor.getString(idxType) ?: "")
                        .put("source_url", cursor.getString(idxSourceUrl) ?: "")
                )
            }
        }

        return JSONObject()
            .put("version", 5)
            .put("entries", entriesArray)
            .put("creators", creatorsArray)
    }

    fun importSnapshot(entries: JSONArray, creators: JSONArray = JSONArray()): ImportResult {
        val db = writableDatabase
        val now = utcNowString()

        var processed = 0
        var imported = 0
        var inserted = 0
        var updated = 0
        var skipped = 0
        var creatorsProcessed = 0
        var creatorsAdded = 0
        var creatorsSkipped = 0

        db.beginTransaction()
        try {
            for (idx in 0 until entries.length()) {
                processed += 1
                val raw = entries.opt(idx)
                val obj = raw as? JSONObject
                if (obj == null) {
                    skipped += 1
                    continue
                }

                val code = obj.optInt("code", 0)
                if (code <= 0) {
                    skipped += 1
                    continue
                }

                val title = obj.optString("title", "").trim().ifBlank { "Gallery $code" }
                val subtitle = obj.optString("subtitle", "")
                val sourceUrl = obj.optString("source_url", "").ifBlank { "https://nhentai.net/g/$code/" }
                val numPages = obj.optInt("num_pages", 0).coerceAtLeast(0)
                val uploadDate = obj.optString("upload_date", "")
                val fetchedAt = obj.optString("fetched_at", "").ifBlank { now }
                val addedAt = obj.optString("added_at", "").ifBlank { now }
                val rating = obj.optInt("rating", 0).coerceIn(0, 5)
                val hasReadState = obj.has("read")
                val readState = when {
                    hasReadState -> if (obj.optInt("read", 0) != 0) 1 else 0
                    rating > 0 -> 1
                    else -> 0
                }
                val rawReadAt = obj.optString("read_at", "").trim()
                val readAt = when {
                    readState != 0 -> rawReadAt.ifBlank { addedAt }
                    else -> ""
                }
                val hasPinned = obj.has("pinned")
                val pinned = if (obj.optInt("pinned", 0) != 0) 1 else 0
                val mediaId = parseMediaId(obj.opt("media_id"))
                val coverExt = parseCoverExtension(obj.optString("cover_ext", ""))

                val exists = entryExists(db, code)

                if (exists) {
                    val values = ContentValues().apply {
                        put("title", title)
                        put("subtitle", subtitle)
                        put("source_url", sourceUrl)
                        put("num_pages", numPages)
                        put("upload_date", uploadDate)
                        put("rating", rating)
                        if (hasReadState || rating > 0) {
                            put("read_state", readState)
                            put("read_at", readAt)
                        } else if (rawReadAt.isNotBlank()) {
                            put("read_at", readAt)
                        }
                        if (hasPinned) {
                            put("pinned", pinned)
                        }
                        put("fetched_at", fetchedAt)
                        put("added_at", addedAt)
                        put("media_id", mediaId)
                        put("cover_ext", coverExt)
                    }
                    db.update("entries", values, "code = ?", arrayOf(code.toString()))
                } else {
                    val values = ContentValues().apply {
                        put("code", code)
                        put("title", title)
                        put("subtitle", subtitle)
                        put("source_url", sourceUrl)
                        put("num_pages", numPages)
                        put("upload_date", uploadDate)
                        put("rating", rating)
                        put("read_state", readState)
                        put("read_at", readAt)
                        put("pinned", pinned)
                        put("fetched_at", fetchedAt)
                        put("added_at", addedAt)
                        put("media_id", mediaId)
                        put("cover_ext", coverExt)
                    }
                    db.insertOrThrow("entries", null, values)
                }

                db.delete("entry_tags", "entry_code = ?", arrayOf(code.toString()))

                val deduped = LinkedHashMap<Pair<String, String>, Pair<String, String>>()
                val tags = obj.optJSONArray("tags") ?: JSONArray()
                for (tagIdx in 0 until tags.length()) {
                    val tagRaw = tags.opt(tagIdx)
                    val (tagName, tagType) = when (tagRaw) {
                        is JSONObject -> {
                            val name = tagRaw.optString("name", "").trim()
                            val type = tagRaw.optString("type", "tag").trim().lowercase(Locale.US).ifBlank { "tag" }
                            name to type
                        }
                        else -> {
                            val name = tagRaw?.toString()?.trim().orEmpty()
                            name to "tag"
                        }
                    }
                    val normalized = normalizeTagName(tagName)
                    if (normalized.isBlank()) continue
                    deduped[normalized to tagType] = tagName to tagType
                }

                deduped.forEach { (key, value) ->
                    val normalized = key.first
                    val tagType = key.second
                    val tagName = value.first

                    val tagValues = ContentValues().apply {
                        put("name", tagName)
                        put("type", tagType)
                        put("normalized_name", normalized)
                    }
                    db.insertWithOnConflict("tags", null, tagValues, SQLiteDatabase.CONFLICT_IGNORE)

                    val tagId = findTagId(db, normalized, tagType) ?: return@forEach
                    val linkValues = ContentValues().apply {
                        put("entry_code", code)
                        put("tag_id", tagId)
                    }
                    db.insertWithOnConflict("entry_tags", null, linkValues, SQLiteDatabase.CONFLICT_IGNORE)
                }

                imported += 1
                if (exists) {
                    updated += 1
                } else {
                    inserted += 1
                }
            }

            for (idx in 0 until creators.length()) {
                creatorsProcessed += 1
                val raw = creators.opt(idx)
                val obj = raw as? JSONObject
                if (obj == null) {
                    creatorsSkipped += 1
                    continue
                }

                val name = obj.optString("name", "").trim()
                val creatorType = obj.optString("type", "").trim().lowercase(Locale.US)
                val sourceUrl = obj.optString("source_url", "").trim()
                if (name.isBlank() || !isCreatorType(creatorType)) {
                    creatorsSkipped += 1
                    continue
                }

                if (upsertCreatorTag(db, name = name, creatorType = creatorType, sourceUrl = sourceUrl)) {
                    creatorsAdded += 1
                } else {
                    creatorsSkipped += 1
                }
            }

            cleanupOrphanTags(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        return ImportResult(
            processed = processed,
            imported = imported,
            inserted = inserted,
            updated = updated,
            skipped = skipped,
            creatorsProcessed = creatorsProcessed,
            creatorsAdded = creatorsAdded,
            creatorsSkipped = creatorsSkipped
        )
    }

    private fun upsertCreatorTag(
        db: SQLiteDatabase,
        name: String,
        creatorType: String,
        sourceUrl: String
    ): Boolean {
        val cleanedName = name.trim().replace(Regex("\\s+"), " ")
        val normalizedName = normalizeTagName(cleanedName)
        val normalizedType = creatorType.trim().lowercase(Locale.US)
        val cleanedUrl = sourceUrl.trim()
        if (normalizedName.isBlank() || !isCreatorType(normalizedType)) {
            return false
        }

        var existingId: Long? = null
        var existingPinned = 0
        var existingSourceUrl = ""
        db.rawQuery(
            """
            SELECT id, COALESCE(pinned, 0) AS pinned, COALESCE(source_url, '') AS source_url
            FROM tags
            WHERE normalized_name = ? AND type = ?
            """.trimIndent(),
            arrayOf(normalizedName, normalizedType)
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                existingId = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                existingPinned = cursor.getInt(cursor.getColumnIndexOrThrow("pinned"))
                existingSourceUrl = cursor.getString(cursor.getColumnIndexOrThrow("source_url")) ?: ""
            }
        }

        if (existingId != null) {
            val updates = ContentValues()
            if (existingPinned == 0) {
                updates.put("pinned", 1)
            }
            if (cleanedUrl.isNotBlank() && existingSourceUrl.isBlank()) {
                updates.put("source_url", cleanedUrl)
            }
            if (updates.size() > 0) {
                db.update("tags", updates, "id = ?", arrayOf(existingId.toString()))
            }
            return false
        }

        val values = ContentValues().apply {
            put("name", cleanedName)
            put("type", normalizedType)
            put("normalized_name", normalizedName)
            put("pinned", 1)
            put("source_url", cleanedUrl)
        }
        db.insertOrThrow("tags", null, values)
        return true
    }

    fun getTagName(tagId: Long): String? {
        readableDatabase.rawQuery(
            "SELECT name FROM tags WHERE id = ?",
            arrayOf(tagId.toString())
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return cursor.getString(0)
        }
    }

    fun getTagRouteRef(tagId: Long): TagRouteRef? {
        if (tagId <= 0L) return null
        readableDatabase.rawQuery(
            "SELECT name, type FROM tags WHERE id = ? LIMIT 1",
            arrayOf(tagId.toString())
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            val name = cursor.getString(0)?.trim().orEmpty()
            val type = cursor.getString(1)?.trim().orEmpty().lowercase(Locale.US)
            if (name.isBlank() || type.isBlank()) return null
            return TagRouteRef(name = name, type = type)
        }
    }

    fun findDirectRouteTagByName(rawName: String): TagRouteRef? {
        val normalized = normalizeTagName(rawName)
        if (normalized.isBlank()) return null
        val allowedTypes = listOf("group", "artist", "language", "character", "parody", "category", "tag")
        val placeholders = allowedTypes.joinToString(",") { "?" }
        val args = mutableListOf(normalized).apply { addAll(allowedTypes) }
        val sql = """
            SELECT t.name, t.type, COUNT(et.entry_code) AS entry_count
            FROM tags t
            LEFT JOIN entry_tags et ON et.tag_id = t.id
            WHERE t.normalized_name = ?
              AND t.type IN ($placeholders)
            GROUP BY t.id, t.name, t.type
            ORDER BY
                CASE t.type
                    WHEN 'group' THEN 0
                    WHEN 'artist' THEN 1
                    WHEN 'language' THEN 2
                    WHEN 'character' THEN 3
                    WHEN 'parody' THEN 4
                    WHEN 'category' THEN 5
                    WHEN 'tag' THEN 6
                    ELSE 99
                END ASC,
                entry_count DESC,
                t.id ASC
            LIMIT 1
        """.trimIndent()
        readableDatabase.rawQuery(sql, args.toTypedArray()).use { cursor ->
            if (!cursor.moveToFirst()) return null
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))?.trim().orEmpty()
            val type = cursor.getString(cursor.getColumnIndexOrThrow("type"))?.trim().orEmpty().lowercase(Locale.US)
            if (name.isBlank() || type.isBlank()) return null
            return TagRouteRef(name = name, type = type)
        }
    }

    fun findCreatorId(type: String, name: String): Long? {
        val normalizedType = type.trim().lowercase(Locale.US)
        if (!isCreatorType(normalizedType)) return null
        val normalizedName = normalizeTagName(name)
        if (normalizedName.isBlank()) return null

        readableDatabase.rawQuery(
            """
            SELECT id
            FROM tags
            WHERE normalized_name = ? AND type = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(normalizedName, normalizedType)
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return cursor.getLong(0)
        }
    }

    private fun cleanupOrphanTags(db: SQLiteDatabase) {
        db.execSQL(
            """
            DELETE FROM tags
            WHERE id NOT IN (SELECT DISTINCT tag_id FROM entry_tags)
              AND COALESCE(pinned, 0) = 0
            """.trimIndent()
        )
    }

    private fun hasColumn(db: SQLiteDatabase, tableName: String, columnName: String): Boolean {
        db.rawQuery("PRAGMA table_info($tableName)", null).use { cursor ->
            val idxName = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                if ((cursor.getString(idxName) ?: "") == columnName) {
                    return true
                }
            }
        }
        return false
    }

    private fun isCreatorType(type: String): Boolean {
        return type == "artist" || type == "group"
    }

    private fun entryExists(db: SQLiteDatabase, code: Int): Boolean {
        db.rawQuery("SELECT 1 FROM entries WHERE code = ?", arrayOf(code.toString())).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun findTagId(db: SQLiteDatabase, normalizedName: String, type: String): Long? {
        db.rawQuery(
            "SELECT id FROM tags WHERE normalized_name = ? AND type = ?",
            arrayOf(normalizedName, type)
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return cursor.getLong(0)
        }
    }
}

class TagBookViewModel(application: Application) : AndroidViewModel(application) {
    private val db = TagBookDatabase(application)
    private val client = NhentaiApiClient()
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(loadThemeMode())
        private set
    var accentMode by mutableStateOf(loadAccentMode())
        private set
    var showThumbnails by mutableStateOf(loadShowThumbnails())
        private set
    var pureGalleryMode by mutableStateOf(loadPureGalleryMode())
        private set
    var galleryColumns by mutableStateOf(loadGalleryColumns())
        private set
    var applyBlockedTagsToHome by mutableStateOf(loadApplyBlockedTagsToHome())
        private set
    var applyBlockedTagsToSearchTerms by mutableStateOf(loadApplyBlockedTagsToSearchTerms())
        private set
    var preloadOnLaunch by mutableStateOf(loadPreloadOnLaunch())
        private set
    var preloadPercent by mutableStateOf(loadPreloadPercent())
        private set
    var autoBackupTreeUri by mutableStateOf(loadAutoBackupTreeUri())
        private set
    var incognitoModeEnabled by mutableStateOf(false)
        private set
    var appLockEnabled by mutableStateOf(loadAppLockEnabled())
        private set
    var appLockBiometricEnabled by mutableStateOf(loadAppLockBiometricEnabled())
        private set
    var appLocked by mutableStateOf(false)
        private set
    var appLockNonce by mutableStateOf(0L)
        private set
    var incognitoToggleAuthPending by mutableStateOf(false)
        private set
    var incognitoToggleAuthNonce by mutableStateOf(0L)
        private set
    private var appLockPinHash: String = loadAppLockPinHash()
    private var appLockPinSalt: String = loadAppLockPinSalt()
    private var appLockGraceUntilMs: Long = loadAppLockGraceUntilMs()

    var codeInput by mutableStateOf("")
        private set
    var entrySearch by mutableStateOf("")
        private set
    var statusMessage by mutableStateOf("Ready.")
        private set

    var entries by mutableStateOf<List<EntryRow>>(emptyList())
        private set
    var tags by mutableStateOf<List<TagCountRow>>(emptyList())
        private set
    var popularTags by mutableStateOf<List<PopularTagRow>>(emptyList())
        private set
    var entryLayoutPreviewSamples by mutableStateOf<List<EntryRow>>(emptyList())
        private set
    var creators by mutableStateOf<List<CreatorRow>>(emptyList())
        private set
    var savedStats by mutableStateOf(SavedStats(0, 0, 0, 0))
        private set
    var readAnalytics by mutableStateOf(ReadAnalyticsSnapshot())
        private set
    var readAnalyticsLoading by mutableStateOf(false)
        private set
    private var readAnalyticsLoaded = false
    var creatorEntriesById by mutableStateOf<Map<Long, List<CreatorEntryRow>>>(emptyMap())
        private set

    val activeTagFilterIds = mutableStateListOf<Long>()
    val expandedCreatorIds = mutableStateListOf<Long>()

    var selectedCode by mutableStateOf<Int?>(null)
        private set
    var selectedDetail by mutableStateOf<EntryDetail?>(null)
        private set
    var selectedSeriesNeighbors by mutableStateOf(SeriesNeighbors())
        private set
    var pendingEntryJumpCode by mutableStateOf<Int?>(null)
        private set
    var pendingCreatorJumpId by mutableStateOf<Long?>(null)
        private set

    var sortField by mutableStateOf<EntrySortField?>(null)
        private set
    var sortDirection by mutableStateOf(SortDirection.DESC)
        private set
    var entryReadFilter by mutableStateOf(EntryReadFilterMode.ALL)
        private set

    var tagSortField by mutableStateOf(TagSortField.COUNT)
        private set
    var tagSortDirection by mutableStateOf(SortDirection.DESC)
        private set
    var creatorSortField by mutableStateOf(CreatorSortField.COUNT)
        private set
    var creatorSortDirection by mutableStateOf(SortDirection.DESC)
        private set
    var blockedTagSortField by mutableStateOf(TagSortField.COUNT)
        private set
    var blockedTagSortDirection by mutableStateOf(SortDirection.DESC)
        private set
    var tagsCardCollapsed by mutableStateOf(true)
        private set
    var entriesCardCollapsed by mutableStateOf(true)
        private set
    var creatorsCardCollapsed by mutableStateOf(true)
        private set

    var infoDialogMessage by mutableStateOf<String?>(null)
        private set
    var errorDialogMessage by mutableStateOf<String?>(null)
        private set
    var browserRatingPromptState by mutableStateOf<BrowserRatingPromptState?>(null)
        private set
    var pinTogglePromptState by mutableStateOf<PinTogglePromptState?>(null)
        private set

    var splitPromptState by mutableStateOf<SplitPromptState?>(null)
        private set
    var shortPromptState by mutableStateOf<ShortPromptState?>(null)
        private set
    var manualCreatorPromptState by mutableStateOf<ManualCreatorPromptState?>(null)
        private set
    var batchCreatorPromptState by mutableStateOf<BatchCreatorPromptState?>(null)
        private set

    var batchProgressState by mutableStateOf<BatchProgressState?>(null)
        private set
    var batchDialogTitle by mutableStateOf("Batch Add/Update")
        private set
    var startupPreloadState by mutableStateOf<StartupPreloadState?>(null)
        private set

    private var pendingBatchText: String? = null
    private var pendingSplitSequences: List<SplitSequence> = emptyList()
    private var pendingCandidates: List<Pair<Int, Int>> = emptyList()
    private var pendingCreatorAddedCount: Int = 0
    private var pendingCreatorSkippedCount: Int = 0
    private var pendingCreatorUnresolvedCount: Int = 0
    private var pendingBatchCreatorLinks: List<CreatorLink> = emptyList()
    private var pendingBatchCreatorBaseNames: List<String> = emptyList()
    private var pendingBatchCreatorTwoWordNames: List<String> = emptyList()
    private var pendingBatchCodeSourceText: String = ""
    private val tagNameCache = linkedMapOf<Long, String>()
    private val tagRouteCache = linkedMapOf<Long, TagRouteRef>()
    private val creatorLoadJobs = mutableMapOf<Long, Job>()
    private var seriesNeighborsJob: Job? = null
    private val loadingCreatorIds = mutableStateListOf<Long>()
    private var creatorEntryFilterKey: String = ""
    private val autoBackupInFlight = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val inAppBackActions = ArrayDeque<InAppBackAction>()
    private var applyingInAppBackAction = false
    private var lastAutoBackupAttemptMs: Long = 0L
    private var pendingBrowserRatingCode: Int? = null
    private var awaitingBrowserRatingPrompt: Boolean = false
    private var pendingIncomingShareText: String? = null
    var popularTagsFetchInProgress by mutableStateOf(false)
        private set
    @Volatile
    private var batchCancelRequested: Boolean = false

    init {
        if (appLockEnabled && !isAppLockConfigured()) {
            appLockEnabled = false
            appLocked = false
            prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, false).apply()
        }
        if (pureGalleryMode && !showThumbnails) {
            showThumbnails = true
            prefs.edit().putBoolean(KEY_SHOW_THUMBNAILS, true).apply()
        }
        if (galleryColumns !in 1..10) {
            galleryColumns = galleryColumns.coerceIn(1, 10)
            prefs.edit().putInt(KEY_GALLERY_COLUMNS, galleryColumns).apply()
        }
        if (appLockEnabled && isAppLockConfigured()) {
            appLocked = System.currentTimeMillis() >= appLockGraceUntilMs
        } else {
            appLocked = false
        }
        if (preloadOnLaunch) {
            preloadAllOnLaunch()
        } else {
            refreshAll(null)
        }
    }

    fun updateUnifiedInput(value: String) {
        val changed = codeInput != value || entrySearch != value
        codeInput = value
        entrySearch = value
        if (!changed) return
        loadEntries(selectedCode)
        loadTags()
        loadCreators()
    }

    fun updateCodeInput(value: String) {
        updateUnifiedInput(value)
    }

    fun updateEntrySearch(value: String) {
        updateUnifiedInput(value)
    }

    fun clearEntrySearch() {
        if (entrySearch.isBlank()) return
        updateUnifiedInput("")
        setStatus("Search everything cleared.")
    }

    fun setStatus(message: String) {
        statusMessage = message
    }

    fun queueIncomingShareText(sharedText: String) {
        val normalized = sharedText.trim()
        if (normalized.isBlank()) return
        pendingIncomingShareText = normalized
        enforceLockIfRequiredNow()
        consumePendingShareTextIfUnlocked()
    }

    private fun isLockRequiredNow(): Boolean {
        if (!appLockEnabled || !isAppLockConfigured()) return false
        return System.currentTimeMillis() >= appLockGraceUntilMs
    }

    private fun enforceLockIfRequiredNow() {
        if (!isLockRequiredNow()) return
        if (!appLocked) {
            appLocked = true
            appLockNonce = System.currentTimeMillis()
        }
    }

    private fun consumePendingShareTextIfUnlocked() {
        enforceLockIfRequiredNow()
        if (appLockEnabled && appLocked) {
            setStatus("Shared text queued. Unlock to paste.")
            return
        }
        val pending = pendingIncomingShareText ?: return
        pendingIncomingShareText = null
        updateUnifiedInput(pending)
        setStatus("Pasted shared text into Search everything.")
    }

    fun dismissInfoDialog() {
        infoDialogMessage = null
    }

    fun dismissErrorDialog() {
        errorDialogMessage = null
    }

    fun cycleThemeMode() {
        themeMode = when (themeMode) {
            ThemeMode.SYSTEM -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.SYSTEM
        }
        prefs.edit().putString(KEY_THEME_MODE, themeMode.name).apply()
        setStatus("Theme mode set to ${themeMode.label}.")
    }

    fun chooseAccentMode(mode: AccentMode) {
        if (accentMode == mode) return
        accentMode = mode
        prefs.edit().putString(KEY_ACCENT_MODE, mode.name).apply()
        setStatus(
            if (mode == AccentMode.AUTO) {
                "Accent color set to wallpaper/system."
            } else {
                "Accent color set to ${mode.label.lowercase(Locale.US)}."
            }
        )
    }

    fun setOrChangeAppLockPin(pinInput: String, enableIfDisabled: Boolean = true): String? {
        val normalized = normalizePinInput(pinInput)
        if (normalized.isBlank()) {
            return "PIN cannot be empty."
        }
        if (normalized.length > 20) {
            return "PIN cannot be longer than 20 digits."
        }

        val salt = generatePinSalt()
        val hash = hashPin(normalized, salt)
        appLockPinSalt = salt
        appLockPinHash = hash
        val editor = prefs.edit()
            .putString(KEY_APP_LOCK_PIN_SALT, salt)
            .putString(KEY_APP_LOCK_PIN_HASH, hash)

        if (enableIfDisabled && !appLockEnabled) {
            appLockEnabled = true
            editor.putBoolean(KEY_APP_LOCK_ENABLED, true)
            setStatus("App lock enabled.")
        } else {
            setStatus("App lock PIN updated.")
        }
        editor.apply()
        if (!appLockEnabled) {
            appLocked = false
        }
        val now = System.currentTimeMillis()
        setAppLockGraceUntil(now + APP_LOCK_GRACE_MS)
        appLocked = false
        appLockNonce = System.currentTimeMillis()
        return null
    }

    fun disableAppLock() {
        appLockEnabled = false
        appLocked = false
        incognitoToggleAuthPending = false
        appLockPinHash = ""
        appLockPinSalt = ""
        appLockNonce = System.currentTimeMillis()
        setAppLockGraceUntil(0L)
        prefs.edit()
            .putBoolean(KEY_APP_LOCK_ENABLED, false)
            .putString(KEY_APP_LOCK_PIN_HASH, "")
            .putString(KEY_APP_LOCK_PIN_SALT, "")
            .apply()
        setStatus("App lock disabled.")
    }

    fun chooseAppLockBiometricEnabled(enabled: Boolean) {
        if (appLockBiometricEnabled == enabled) return
        appLockBiometricEnabled = enabled
        prefs.edit().putBoolean(KEY_APP_LOCK_BIOMETRIC_ENABLED, enabled).apply()
        setStatus(
            if (enabled) {
                "Biometric unlock enabled."
            } else {
                "Biometric unlock disabled."
            }
        )
    }

    fun scheduleAppLockAfterClose() {
        if (!appLockEnabled || !isAppLockConfigured()) return
        if (appLocked) {
            // If the app is already locked, do not grant a new grace window on close/reopen.
            setAppLockGraceUntil(0L)
            return
        }
        val now = System.currentTimeMillis()
        setAppLockGraceUntil(now + APP_LOCK_GRACE_MS)
        appLocked = false
    }

    fun refreshAppLockOnResume() {
        if (!appLockEnabled || !isAppLockConfigured()) {
            appLocked = false
            return
        }
        val now = System.currentTimeMillis()
        val shouldLock = now >= appLockGraceUntilMs
        val changed = appLocked != shouldLock
        appLocked = shouldLock
        if (shouldLock && changed) {
            appLockNonce = now
        }
    }

    fun unlockAppFromBiometric() {
        if (!appLockEnabled) return
        val now = System.currentTimeMillis()
        setAppLockGraceUntil(now + APP_LOCK_GRACE_MS)
        appLocked = false
        appLockNonce = now
        consumePendingShareTextIfUnlocked()
        if (!completePendingIncognitoToggleIfAny()) {
            setStatus("Unlocked.")
        }
    }

    fun tryUnlockWithPin(pinInput: String): Boolean {
        if (!appLockEnabled || !isAppLockConfigured()) return false
        val normalized = normalizePinInput(pinInput)
        if (normalized.isBlank()) return false
        val expected = hashPin(normalized, appLockPinSalt)
        if (expected != appLockPinHash) return false
        val now = System.currentTimeMillis()
        setAppLockGraceUntil(now + APP_LOCK_GRACE_MS)
        appLocked = false
        appLockNonce = now
        consumePendingShareTextIfUnlocked()
        if (!completePendingIncognitoToggleIfAny()) {
            setStatus("Unlocked.")
        }
        return true
    }

    private fun applyIncognitoModeToggle() {
        incognitoModeEnabled = !incognitoModeEnabled
        setStatus(
            if (incognitoModeEnabled) {
                "Incognito privacy mode enabled."
            } else {
                "Incognito privacy mode disabled."
            }
        )
    }

    private fun completePendingIncognitoToggleIfAny(): Boolean {
        if (!incognitoToggleAuthPending) return false
        incognitoToggleAuthPending = false
        applyIncognitoModeToggle()
        return true
    }

    fun toggleIncognitoMode() {
        if (!appLockEnabled || !isAppLockConfigured()) {
            applyIncognitoModeToggle()
            return
        }
        incognitoToggleAuthPending = true
        val now = System.currentTimeMillis()
        incognitoToggleAuthNonce = now
        appLocked = true
        appLockNonce = now
        setStatus("Unlock to toggle incognito mode.")
    }

    fun toggleThumbnailsEnabled() {
        showThumbnails = !showThumbnails
        prefs.edit().putBoolean(KEY_SHOW_THUMBNAILS, showThumbnails).apply()
        setStatus(
            if (showThumbnails) {
                "Entry thumbnails enabled."
            } else {
                "Entry thumbnails disabled for better performance."
            }
        )
    }

    fun togglePureGalleryMode() {
        pureGalleryMode = !pureGalleryMode
        prefs.edit().putBoolean(KEY_PURE_GALLERY_MODE, pureGalleryMode).apply()
        setStatus(
            if (pureGalleryMode) {
                "Pure Gallery Mode enabled (2-column entries grid)."
            } else {
                "Pure Gallery Mode disabled (standard entries layout)."
            }
        )
    }

    fun applyEntryLayout(modeGallery: Boolean, columns: Int) {
        val safeColumns = columns.coerceIn(1, 10)
        pureGalleryMode = modeGallery
        galleryColumns = safeColumns
        prefs.edit()
            .putBoolean(KEY_PURE_GALLERY_MODE, pureGalleryMode)
            .putInt(KEY_GALLERY_COLUMNS, galleryColumns)
            .apply()
        setStatus(
            if (pureGalleryMode) {
                "Gallery layout enabled ($galleryColumns across)."
            } else {
                "Normal entry layout enabled."
            }
        )
    }

    fun loadEntryLayoutPreviewSamples() {
        val all = db.listEntries(
            textFilter = "",
            tagFilterIds = emptyList(),
            sortField = EntrySortField.ADDED,
            sortDirection = SortDirection.DESC,
            readFilter = EntryReadFilterMode.ALL
        )
        if (all.isEmpty()) {
            entryLayoutPreviewSamples = emptyList()
            return
        }
        val base = all.shuffled().take(10)
        entryLayoutPreviewSamples = if (base.size >= 10) base else buildList {
            addAll(base)
            var idx = 0
            while (size < minOf(10, all.size)) {
                add(all[idx % all.size])
                idx++
            }
        }
    }

    fun toggleApplyBlockedTagsToHome() {
        applyBlockedTagsToHome = !applyBlockedTagsToHome
        prefs.edit().putBoolean(KEY_APPLY_BLOCKED_TAGS_HOME, applyBlockedTagsToHome).apply()
        setStatus(
            if (applyBlockedTagsToHome) {
                "Blocked tags now apply to Home browser open."
            } else {
                "Blocked tags no longer apply to Home browser open."
            }
        )
    }

    fun toggleApplyBlockedTagsToSearchTerms() {
        applyBlockedTagsToSearchTerms = !applyBlockedTagsToSearchTerms
        prefs.edit().putBoolean(KEY_APPLY_BLOCKED_TAGS_SEARCH, applyBlockedTagsToSearchTerms).apply()
        setStatus(
            if (applyBlockedTagsToSearchTerms) {
                "Blocked tags now apply to combined search browser open."
            } else {
                "Blocked tags no longer apply to combined search browser open."
            }
        )
    }

    fun togglePreloadOnLaunch() {
        preloadOnLaunch = !preloadOnLaunch
        prefs.edit().putBoolean(KEY_PRELOAD_ON_LAUNCH, preloadOnLaunch).apply()
        setStatus(
            if (preloadOnLaunch) {
                "Launch preload enabled."
            } else {
                "Launch preload disabled."
            }
        )
    }

    fun updatePreloadPercent(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        if (preloadPercent == clamped) return
        preloadPercent = clamped
        prefs.edit().putInt(KEY_PRELOAD_PERCENT, preloadPercent).apply()
    }

    fun setAutoBackupFolder(uri: Uri?) {
        val normalized = uri?.toString().orEmpty()
        autoBackupTreeUri = normalized
        prefs.edit().putString(KEY_AUTO_BACKUP_TREE_URI, normalized).apply()
        setStatus(
            if (normalized.isBlank()) {
                "Procedural backup folder cleared."
            } else {
                "Procedural backup folder set."
            }
        )
        if (normalized.isNotBlank()) {
            triggerProceduralBackup(ignoreThrottle = true, reportStatus = true)
        }
    }

    fun autoBackupFolderLabel(): String {
        if (autoBackupTreeUri.isBlank()) return "Not set"
        return runCatching {
            val treeId = DocumentsContract.getTreeDocumentId(Uri.parse(autoBackupTreeUri))
            if (treeId.isBlank()) "Selected folder" else treeId
        }.getOrDefault("Selected folder")
    }

    fun onHostStopped() {
        triggerProceduralBackup(ignoreThrottle = false, reportStatus = false)
        scheduleAppLockAfterClose()
    }

    fun onHostResumed() {
        refreshAppLockOnResume()
        consumePendingShareTextIfUnlocked()
        if (!awaitingBrowserRatingPrompt) return
        awaitingBrowserRatingPrompt = false
        val code = pendingBrowserRatingCode ?: return

        val detail = db.getEntryDetail(code)
        browserRatingPromptState = BrowserRatingPromptState(
            code = code,
            title = detail?.title?.ifBlank { "Gallery $code" } ?: "Gallery $code",
            rating = detail?.rating?.coerceIn(0, 5) ?: 0
        )
    }

    fun backupNow() {
        triggerProceduralBackup(ignoreThrottle = true, reportStatus = true)
    }

    private fun triggerProceduralBackup(ignoreThrottle: Boolean, reportStatus: Boolean) {
        if (autoBackupTreeUri.isBlank()) {
            if (reportStatus) {
                setStatus("Set procedural backup folder first.")
            }
            return
        }
        val now = System.currentTimeMillis()
        if (!ignoreThrottle && now - lastAutoBackupAttemptMs < 5000L) return
        if (!autoBackupInFlight.compareAndSet(false, true)) return

        lastAutoBackupAttemptMs = now
        val treeUriValue = autoBackupTreeUri
        Thread {
            val result = runCatching {
                val treeUri = Uri.parse(treeUriValue)
                val snapshot = db.exportSnapshot()
                val exportText = buildExportText(snapshot)
                val backupUri = resolveOrCreateProceduralBackupUri(treeUri)
                writeTextToUri(backupUri, exportText)
                val entryCount = snapshot.optJSONArray("entries")?.length() ?: 0
                val creatorCount = snapshot.optJSONArray("creators")?.length() ?: 0
                entryCount to creatorCount
            }
            autoBackupInFlight.set(false)
            if (reportStatus) {
                result.onSuccess { (entriesCount, creatorsCount) ->
                    mainHandler.post {
                        setStatus("Procedural backup updated ($entriesCount entries, $creatorsCount creators/groups).")
                    }
                }.onFailure { exc ->
                    mainHandler.post {
                        setStatus("Procedural backup failed: ${exc.message ?: "unknown error"}")
                    }
                }
            }
        }.apply {
            name = "ProceduralBackupWriter"
            isDaemon = true
            start()
        }
    }

    fun activeFilterLabel(): String {
        if (activeTagFilterIds.isEmpty()) return "No tag selected"
        val names = activeTagFilterIds.mapNotNull { tagId ->
            tagNameCache[tagId] ?: db.getTagName(tagId)?.also { resolved ->
                tagNameCache[tagId] = resolved
            }
        }
        return if (names.isEmpty()) "No tag selected" else names.joinToString(", ")
    }

    private fun activeTagFilterNames(): List<String> {
        return activeTagFilterIds.mapNotNull { tagId ->
            tagNameCache[tagId] ?: db.getTagName(tagId)?.also { resolved ->
                tagNameCache[tagId] = resolved
            }
        }.map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.isNotBlank() }
    }

    private fun normalizeDirectRouteType(rawType: String): String {
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

    private fun normalizeCreatorRouteName(raw: String): String {
        val normalized = raw
            .replace("ï½œ", "|")
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

    private fun parseDirectRouteLinkInput(rawInput: String): TagRouteRef? {
        val match = DIRECT_ROUTE_LINK_PATTERN.matchEntire(rawInput.trim()) ?: return null
        val routeType = normalizeDirectRouteType(match.groupValues.getOrNull(1).orEmpty())
        if (routeType.isBlank()) return null
        var slug = match.groupValues.getOrNull(2).orEmpty().trim()
        while (slug.isNotEmpty() && URL_TRAILING_PUNCT.contains(slug.last())) {
            slug = slug.dropLast(1)
        }
        if (slug.isBlank()) return null
        val routeName = normalizeDirectRouteName(routeType, slug)
        if (routeName.isBlank()) return null
        return TagRouteRef(name = routeName, type = routeType)
    }

    private fun normalizeDirectRouteName(type: String, rawName: String): String {
        val routeType = normalizeDirectRouteType(type)
        if (routeType.isBlank()) return ""
        return when (routeType) {
            "artist", "group" -> normalizeCreatorRouteName(rawName).ifBlank { rawName.trim() }
            else -> parseCreatorSlug(rawName).ifBlank { rawName.trim() }
        }.trim().replace(Regex("\\s+"), " ")
    }

    private fun getTagRouteRef(tagId: Long): TagRouteRef? {
        if (tagId <= 0L) return null
        val cached = tagRouteCache[tagId]
        if (cached != null) return cached
        val fromDb = db.getTagRouteRef(tagId) ?: return null
        tagRouteCache[tagId] = fromDb
        tagNameCache[tagId] = fromDb.name
        return fromDb
    }

    private fun resolveSingleDirectRouteFromSearch(searchText: String): TagRouteRef? {
        val parsed = parseSearchQuery(searchText)
        if (parsed.freeText.isBlank() && parsed.filters.size == 1) {
            val filter = parsed.filters.first()
            val normalizedType = normalizeDirectRouteType(filter.key)
            if (normalizedType.isNotBlank()) {
                val normalizedName = normalizeDirectRouteName(normalizedType, filter.value)
                if (normalizedName.isNotBlank()) {
                    return TagRouteRef(name = normalizedName, type = normalizedType)
                }
            }
            if (filter.key == "tag") {
                return db.findDirectRouteTagByName(filter.value)?.let { resolved ->
                    val routeType = normalizeDirectRouteType(resolved.type)
                    val routeName = normalizeDirectRouteName(routeType, resolved.name)
                    if (routeType.isBlank() || routeName.isBlank()) null else TagRouteRef(routeName, routeType)
                }
            }
            return null
        }

        if (parsed.filters.isNotEmpty()) return null

        val terms = extractSearchEverythingBrowserTerms(parsed.freeText)
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.isNotBlank() }
        if (terms.size != 1) return null

        return db.findDirectRouteTagByName(terms.first())?.let { resolved ->
            val routeType = normalizeDirectRouteType(resolved.type)
            val routeName = normalizeDirectRouteName(routeType, resolved.name)
            if (routeType.isBlank() || routeName.isBlank()) null else TagRouteRef(routeName, routeType)
        }
    }

    private fun resolveSingleDirectRouteTarget(searchText: String): TagRouteRef? {
        val trimmedSearch = searchText.trim()
        if (activeTagFilterIds.size == 1 && trimmedSearch.isBlank()) {
            val routeRef = getTagRouteRef(activeTagFilterIds.first()) ?: return null
            val routeType = normalizeDirectRouteType(routeRef.type)
            val routeName = normalizeDirectRouteName(routeType, routeRef.name)
            if (routeType.isBlank() || routeName.isBlank()) return null
            return TagRouteRef(name = routeName, type = routeType)
        }
        if (activeTagFilterIds.isEmpty()) {
            parseDirectRouteLinkInput(trimmedSearch)?.let { return it }
            return resolveSingleDirectRouteFromSearch(trimmedSearch)
        }
        return null
    }

    private fun openDirectRouteInBrowser(target: TagRouteRef): Boolean {
        val normalizedType = normalizeDirectRouteType(target.type)
        val normalizedName = normalizeDirectRouteName(normalizedType, target.name)
        if (normalizedType.isBlank() || normalizedName.isBlank()) return false
        val labelType = normalizedType.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
        }
        return openGalleryCodeBrowser(
            initialCode = null,
            initialQuery = "",
            initialCreatorType = normalizedType,
            initialCreatorName = normalizedName,
            blockedTags = emptyList(),
            successStatus = "Opened $labelType '$normalizedName' preview in browser."
        )
    }

    private fun openCombinedSearchInBrowser(
        searchText: String,
        showEmptyPrompt: Boolean
    ): Boolean {
        resolveSingleDirectRouteTarget(searchText)?.let { routeTarget ->
            return openDirectRouteInBrowser(routeTarget)
        }

        val tagNames = activeTagFilterNames()
        val searchTerms = extractSearchEverythingBrowserTerms(searchText)
        val allTerms = buildList {
            addAll(tagNames)
            addAll(searchTerms)
        }
        val blockedTerms = if (applyBlockedTagsToSearchTerms) {
            blockedTagNamesForBrowser()
        } else {
            emptyList()
        }

        if (allTerms.isEmpty() && blockedTerms.isEmpty()) {
            if (showEmptyPrompt) {
                infoDialogMessage = "Type something or select at least one tag first."
            }
            return false
        }

        val encodedQuery = buildNhentaiTagSearchQuery(
            includeTagNames = allTerms,
            excludeTagNames = emptyList()
        )
        if (encodedQuery.isBlank() && blockedTerms.isEmpty()) {
            if (showEmptyPrompt) {
                infoDialogMessage = "Could not build a valid search query."
            }
            return false
        }

        val plainQuery = Uri.decode(encodedQuery.replace("+", " ")).trim()
        openGalleryCodeBrowser(
            initialCode = null,
            initialQuery = plainQuery,
            initialCreatorType = null,
            initialCreatorName = null,
            blockedTags = blockedTerms,
            successStatus = "Opened combined filters/search in browser."
        )
        return true
    }

    private fun blockedTagNamesForBrowser(): List<String> {
        val fromState = popularTags
            .asSequence()
            .filter { it.blocked }
            .map { it.name.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { normalizeTagName(it) }
            .toList()
        if (fromState.isNotEmpty() || popularTags.isNotEmpty()) {
            return fromState
        }
        return db.listBlockedPopularTagNames()
    }

    fun clearTagFilter() {
        activeTagFilterIds.clear()
        loadEntries(null)
        loadCreators()
        setStatus("Tag filter cleared.")
    }

    fun openTagFilterInBrowser() {
        openCombinedSearchInBrowser(
            searchText = codeInput,
            showEmptyPrompt = true
        )
    }

    fun openUnifiedInputInBrowser() {
        val rawInput = codeInput.trim()
        val hasTagFilter = activeTagFilterIds.isNotEmpty()

        if ((rawInput.isBlank() || NHENTAI_HOME_PATTERN.matches(rawInput)) && !hasTagFilter) {
            openHomeInBrowser()
            return
        }

        if (rawInput.isNotBlank()) {
            val directRouteLink = parseDirectRouteLinkInput(rawInput)
            if (directRouteLink != null) {
                openDirectRouteInBrowser(directRouteLink)
                return
            }

            val creatorLink = parseCreatorLink(rawInput)
            if (creatorLink != null) {
                openGalleryCodeBrowser(
                    initialCode = null,
                    initialQuery = "",
                    initialCreatorType = creatorLink.type,
                    initialCreatorName = creatorLink.name,
                    blockedTags = emptyList(),
                    successStatus = "Opened ${creatorLink.type} '${creatorLink.name}' preview in browser."
                )
                return
            }

            val typedCreator = parseTypedCreatorInput(rawInput)
            if (typedCreator != null) {
                openGalleryCodeBrowser(
                    initialCode = null,
                    initialQuery = "",
                    initialCreatorType = typedCreator.first,
                    initialCreatorName = typedCreator.second,
                    blockedTags = emptyList(),
                    successStatus = "Opened ${typedCreator.first} '${typedCreator.second}' preview in browser."
                )
                return
            }

            val code = parseCode(rawInput)
            if (code != null) {
                openGalleryCodeBrowser(
                    initialCode = code,
                    initialQuery = "",
                    blockedTags = emptyList(),
                    successStatus = "Opened code $code preview in gallery browser."
                )
                return
            }

            if (!hasTagFilter) {
                val directRoute = resolveSingleDirectRouteFromSearch(rawInput)
                if (directRoute != null) {
                    openDirectRouteInBrowser(directRoute)
                    return
                }
            }

            viewModelScope.launch {
                val resolvedCreator = withContext(Dispatchers.IO) {
                    runCatching { client.resolveCreatorByName(rawInput) }.getOrNull()
                }

                if (resolvedCreator != null && isStrictCreatorNameMatch(rawInput, resolvedCreator.name)) {
                    openGalleryCodeBrowser(
                        initialCode = null,
                        initialQuery = "",
                        initialCreatorType = resolvedCreator.type,
                        initialCreatorName = resolvedCreator.name,
                        blockedTags = emptyList(),
                        successStatus = "Opened ${resolvedCreator.type} '${resolvedCreator.name}' preview in browser."
                    )
                } else {
                    openCombinedSearchInBrowser(
                        searchText = rawInput,
                        showEmptyPrompt = false
                    )
                }
            }
            return
        }

        openCombinedSearchInBrowser(searchText = "", showEmptyPrompt = false)
    }

    fun hasInAppBackAction(): Boolean {
        return inAppBackActions.isNotEmpty()
    }

    fun handleInAppBackAction(): Boolean {
        val action = inAppBackActions.removeLastOrNull() ?: return false
        applyingInAppBackAction = true
        try {
            when (action.type) {
                InAppBackActionType.ENTRY_SELECTION -> {
                    selectEntry(action.previousCode)
                    setStatus(
                        if (action.previousCode == null) {
                            "Selection cleared."
                        } else {
                            "Restored selection: ${action.previousCode}."
                        }
                    )
                }

                InAppBackActionType.TAGS_CARD_COLLAPSE -> {
                    tagsCardCollapsed = action.previousBoolean ?: tagsCardCollapsed
                    setStatus(
                        if (tagsCardCollapsed) "Tags collapsed." else "Tags expanded."
                    )
                }

                InAppBackActionType.ENTRIES_CARD_COLLAPSE -> {
                    entriesCardCollapsed = action.previousBoolean ?: entriesCardCollapsed
                    setStatus(
                        if (entriesCardCollapsed) "Entries collapsed." else "Entries expanded."
                    )
                }

                InAppBackActionType.CREATORS_CARD_COLLAPSE -> {
                    creatorsCardCollapsed = action.previousBoolean ?: creatorsCardCollapsed
                    setStatus(
                        if (creatorsCardCollapsed) "Artists/groups collapsed." else "Artists/groups expanded."
                    )
                }

                InAppBackActionType.CREATOR_ROW_EXPANDED -> {
                    val creatorId = action.creatorId ?: return false
                    val shouldBeExpanded = action.previousBoolean == true
                    val currentlyExpanded = expandedCreatorIds.contains(creatorId)
                    if (shouldBeExpanded && !currentlyExpanded) {
                        expandedCreatorIds.add(creatorId)
                        ensureCreatorEntriesLoaded(creatorId, forceRefresh = false)
                    } else if (!shouldBeExpanded && currentlyExpanded) {
                        expandedCreatorIds.remove(creatorId)
                        loadingCreatorIds.remove(creatorId)
                        creatorLoadJobs.remove(creatorId)?.cancel()
                    }
                    setStatus(
                        if (shouldBeExpanded) {
                            "Restored expanded artist/group."
                        } else {
                            "Restored collapsed artist/group."
                        }
                    )
                }
            }
        } finally {
            applyingInAppBackAction = false
        }
        return true
    }

    private fun pushInAppBackAction(action: InAppBackAction) {
        if (applyingInAppBackAction) return
        if (inAppBackActions.size >= 120) {
            inAppBackActions.removeFirstOrNull()
        }
        inAppBackActions.addLast(action)
    }

    private fun selectEntryFromUser(nextCode: Int?) {
        if (selectedCode == nextCode) return
        pushInAppBackAction(
            InAppBackAction(
                type = InAppBackActionType.ENTRY_SELECTION,
                previousCode = selectedCode
            )
        )
        selectEntry(nextCode)
    }

    fun toggleEntriesCardCollapsed() {
        pushInAppBackAction(
            InAppBackAction(
                type = InAppBackActionType.ENTRIES_CARD_COLLAPSE,
                previousBoolean = entriesCardCollapsed
            )
        )
        entriesCardCollapsed = !entriesCardCollapsed
    }

    fun toggleTagsCardCollapsed() {
        pushInAppBackAction(
            InAppBackAction(
                type = InAppBackActionType.TAGS_CARD_COLLAPSE,
                previousBoolean = tagsCardCollapsed
            )
        )
        tagsCardCollapsed = !tagsCardCollapsed
    }

    fun toggleCreatorsCardCollapsed() {
        pushInAppBackAction(
            InAppBackAction(
                type = InAppBackActionType.CREATORS_CARD_COLLAPSE,
                previousBoolean = creatorsCardCollapsed
            )
        )
        creatorsCardCollapsed = !creatorsCardCollapsed
    }

    fun toggleCreatorExpanded(tagId: Long) {
        if (tagId <= 0L) return
        val wasExpanded = expandedCreatorIds.contains(tagId)
        pushInAppBackAction(
            InAppBackAction(
                type = InAppBackActionType.CREATOR_ROW_EXPANDED,
                previousBoolean = wasExpanded,
                creatorId = tagId
            )
        )
        if (expandedCreatorIds.contains(tagId)) {
            expandedCreatorIds.remove(tagId)
            loadingCreatorIds.remove(tagId)
            creatorLoadJobs.remove(tagId)?.cancel()
            return
        }
        expandedCreatorIds.add(tagId)
        ensureCreatorEntriesLoaded(tagId, forceRefresh = false)
    }

    fun creatorEntriesFor(tagId: Long): List<CreatorEntryRow> {
        return creatorEntriesById[tagId].orEmpty()
    }

    fun isCreatorLoading(tagId: Long): Boolean {
        return loadingCreatorIds.contains(tagId)
    }

    fun consumePendingCreatorJump() {
        pendingCreatorJumpId = null
    }

    fun consumePendingEntryJump() {
        pendingEntryJumpCode = null
    }

    fun onEntryCardClicked(code: Int) {
        if (selectedCode == code) {
            selectEntryFromUser(null)
            setStatus("Collapsed code $code.")
            return
        }
        selectEntryFromUser(code)
    }

    fun selectEntryFromCreator(code: Int) {
        if (selectedCode == code) {
            selectEntryFromUser(null)
            setStatus("Collapsed code $code from artist/group card.")
            return
        }
        val exists = entries.any { it.code == code }
        selectEntryFromUser(code)
        if (exists) {
            setStatus("Selected code $code from artist/group card.")
        } else {
            setStatus("Selected code $code from artist/group card (not visible under current filters).")
        }
    }

    fun toggleTagFilter(tagId: Long) {
        if (tagId <= 0L) return
        if (incognitoModeEnabled) {
            setStatus("Tag filter selection is disabled in incognito mode.")
            return
        }
        if (activeTagFilterIds.contains(tagId)) {
            activeTagFilterIds.remove(tagId)
        } else {
            activeTagFilterIds.add(tagId)
        }
        loadEntries(null)
        loadCreators()
        if (activeTagFilterIds.isEmpty()) {
            setStatus("Tag filter cleared.")
        } else {
            setStatus("Applied ${activeTagFilterIds.size} tag filters.")
        }
    }

    fun onEntrySortClicked(field: EntrySortField) {
        if (field == EntrySortField.RATING) {
            toggleRatingSort()
            return
        }

        if (sortField == field) {
            sortDirection = if (sortDirection == SortDirection.DESC) SortDirection.ASC else SortDirection.DESC
        } else {
            sortField = field
            sortDirection = when (field) {
                EntrySortField.CODE -> SortDirection.DESC
                EntrySortField.TITLE -> SortDirection.ASC
                EntrySortField.PAGES -> SortDirection.DESC
                EntrySortField.UPLOAD -> SortDirection.DESC
                EntrySortField.ADDED -> SortDirection.DESC
                EntrySortField.RATING -> SortDirection.DESC
            }
        }
        loadEntries(selectedCode, autoSelectFirst = false)
        val order = if (sortDirection == SortDirection.ASC) "ascending" else "descending"
        setStatus("Sorted by ${entrySortLabel(field)} ($order).")
    }

    fun toggleRatingSort() {
        if (sortField == EntrySortField.RATING) {
            sortField = null
            sortDirection = SortDirection.DESC
            setStatus("Rating sort disabled.")
        } else {
            sortField = EntrySortField.RATING
            sortDirection = SortDirection.DESC
            setStatus("Sorting entries by rating (high to low).")
        }
        loadEntries(selectedCode, autoSelectFirst = false)
    }

    fun entryReadFilterLabel(): String {
        return when (entryReadFilter) {
            EntryReadFilterMode.ALL -> "Show All"
            EntryReadFilterMode.READ -> "Show Read"
            EntryReadFilterMode.UNREAD -> "Show Unread"
        }
    }

    fun cycleEntryReadFilter() {
        entryReadFilter = when (entryReadFilter) {
            EntryReadFilterMode.ALL -> EntryReadFilterMode.READ
            EntryReadFilterMode.READ -> EntryReadFilterMode.UNREAD
            EntryReadFilterMode.UNREAD -> EntryReadFilterMode.ALL
        }
        loadEntries(selectedCode)
        val message = when (entryReadFilter) {
            EntryReadFilterMode.ALL -> "Showing all entries."
            EntryReadFilterMode.READ -> "Showing read entries only."
            EntryReadFilterMode.UNREAD -> "Showing unread entries only."
        }
        setStatus(message)
    }

    fun onTagSortClicked(field: TagSortField) {
        if (tagSortField == field) {
            tagSortDirection = if (tagSortDirection == SortDirection.DESC) SortDirection.ASC else SortDirection.DESC
        } else {
            tagSortField = field
            tagSortDirection = when (field) {
                TagSortField.COUNT -> SortDirection.DESC
                TagSortField.NAME -> SortDirection.ASC
                TagSortField.TYPE -> SortDirection.ASC
            }
        }
        loadTags()
        val order = if (tagSortDirection == SortDirection.ASC) "ascending" else "descending"
        setStatus("Sorted tags by ${tagSortLabel(field)} ($order).")
    }

    fun onBlockedTagSortClicked(field: TagSortField) {
        if (blockedTagSortField == field) {
            blockedTagSortDirection = if (blockedTagSortDirection == SortDirection.DESC) SortDirection.ASC else SortDirection.DESC
        } else {
            blockedTagSortField = field
            blockedTagSortDirection = when (field) {
                TagSortField.COUNT -> SortDirection.DESC
                TagSortField.NAME -> SortDirection.ASC
                TagSortField.TYPE -> SortDirection.ASC
            }
        }
        loadPopularTags()
        val order = if (blockedTagSortDirection == SortDirection.ASC) "ascending" else "descending"
        setStatus("Sorted block-tag list by ${tagSortLabel(field)} ($order).")
    }

    fun ensurePopularTagsLoaded(fetchIfEmpty: Boolean) {
        loadPopularTags()
        if (fetchIfEmpty && popularTags.isEmpty()) {
            fetchAllPopularTags()
        }
    }

    fun fetchAllPopularTags() {
        if (popularTagsFetchInProgress) return
        popularTagsFetchInProgress = true
        viewModelScope.launch {
            setStatus("Fetching popular tags from nhentai...")
            val result = withContext(Dispatchers.IO) {
                runCatching { client.fetchAllPopularTags() }
            }
            val payload = result.getOrNull()
            if (payload != null) {
                withContext(Dispatchers.IO) {
                    db.replacePopularTags(payload.tags)
                }
                loadPopularTags()
                setStatus(
                    if (payload.tags.isEmpty()) {
                        "No popular tags were found on nhentai."
                    } else {
                        "Fetched ${payload.tags.size} tags across ${payload.pagesFetched} page(s)."
                    }
                )
            } else {
                val exc = result.exceptionOrNull()
                errorDialogMessage = exc?.message ?: "Failed to fetch popular tags."
                setStatus("Fetching popular tags failed.")
            }
            popularTagsFetchInProgress = false
        }
    }

    fun togglePopularTagBlocked(tagId: Long) {
        val row = popularTags.firstOrNull { it.id == tagId } ?: return
        val next = !row.blocked
        db.setPopularTagBlocked(tagId, next)
        loadPopularTags()
        val action = if (next) "Blocked" else "Unblocked"
        setStatus("$action '${row.name}'.")
    }

    fun clearBlockedTags() {
        db.clearAllBlockedPopularTags()
        loadPopularTags()
        setStatus("Blocked tag filter reset.")
    }

    fun blockedTagsSummary(): String {
        val names = popularTags
            .asSequence()
            .filter { it.blocked }
            .map { it.name.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { normalizeTagName(it) }
            .toList()
        return if (names.isEmpty()) "None" else names.joinToString(", ")
    }

    fun onCreatorSortClicked(field: CreatorSortField) {
        if (creatorSortField == field) {
            creatorSortDirection = if (creatorSortDirection == SortDirection.DESC) SortDirection.ASC else SortDirection.DESC
        } else {
            creatorSortField = field
            creatorSortDirection = when (field) {
                CreatorSortField.COUNT -> SortDirection.DESC
                CreatorSortField.NAME -> SortDirection.ASC
                CreatorSortField.TYPE -> SortDirection.ASC
            }
        }
        loadCreators()
        val order = if (creatorSortDirection == SortDirection.ASC) "ascending" else "descending"
        setStatus("Sorted artists/groups by ${creatorSortLabel(field)} ($order).")
    }

    fun selectEntry(code: Int?) {
        selectedCode = code
        selectedDetail = code?.let { db.getEntryDetail(it) }
        scheduleSeriesNeighborComputation(code, selectedDetail)
    }

    fun openSeriesEntry(code: Int) {
        if (code <= 0) return
        pendingEntryJumpCode = code
        if (selectedCode != code) {
            selectEntry(code)
        }
        setStatus("Opened related entry $code.")
    }

    fun openCreatorFromDetail(creatorType: String, creatorName: String) {
        val type = creatorType.trim().lowercase(Locale.US)
        val cleanName = creatorName.trim()
        if (cleanName.isBlank() || (type != "artist" && type != "group")) return

        val normalizedTarget = normalizeTagName(cleanName)
        val currentListId = creators.firstOrNull { creator ->
            creator.type.equals(type, ignoreCase = true) &&
                normalizeTagName(creator.name) == normalizedTarget
        }?.id
        val creatorId = currentListId ?: db.findCreatorId(type, cleanName)

        if (creatorId == null) {
            setStatus(
                "${type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }} '$cleanName' is not tracked."
            )
            return
        }

        if (!entriesCardCollapsed) {
            pushInAppBackAction(
                InAppBackAction(
                    type = InAppBackActionType.ENTRIES_CARD_COLLAPSE,
                    previousBoolean = entriesCardCollapsed
                )
            )
            entriesCardCollapsed = true
        }
        if (creatorsCardCollapsed) {
            pushInAppBackAction(
                InAppBackAction(
                    type = InAppBackActionType.CREATORS_CARD_COLLAPSE,
                    previousBoolean = creatorsCardCollapsed
                )
            )
            creatorsCardCollapsed = false
        }
        if (!expandedCreatorIds.contains(creatorId)) {
            pushInAppBackAction(
                InAppBackAction(
                    type = InAppBackActionType.CREATOR_ROW_EXPANDED,
                    previousBoolean = false,
                    creatorId = creatorId
                )
            )
            expandedCreatorIds.add(creatorId)
        }
        ensureCreatorEntriesLoaded(creatorId, forceRefresh = false)
        pendingCreatorJumpId = creatorId
        setStatus("Opened $type '$cleanName' in artists/groups.")
    }

    fun openCreatorPreviewInBrowser(creatorType: String, creatorName: String) {
        if (incognitoModeEnabled) {
            setStatus("Open in browser is disabled in incognito mode.")
            return
        }
        val type = creatorType.trim().lowercase(Locale.US)
        val cleanName = creatorName.trim()
        if (cleanName.isBlank() || (type != "artist" && type != "group")) return
        openGalleryCodeBrowser(
            initialCode = null,
            initialQuery = "",
            initialCreatorType = type,
            initialCreatorName = cleanName,
            blockedTags = emptyList(),
            successStatus = "Opened $type '$cleanName' preview in browser."
        )
    }

    fun importFromBrowserClipboard(rawInput: String) {
        val candidate = resolveBrowserClipboardCandidate(rawInput)
        if (candidate.isNullOrBlank()) {
            setStatus("Copied text was not recognized as code/artist/group input.")
            return
        }
        updateUnifiedInput(candidate)
        addOrUpdateByInput()
    }

    private fun resolveBrowserClipboardCandidate(rawInput: String): String? {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return null
        val firstLine = trimmed.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        val candidates = listOf(trimmed, firstLine)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val strongMatch = candidates.firstOrNull { candidate ->
            parseCreatorLink(candidate) != null ||
                parseCode(candidate) != null ||
                parseTypedCreatorInput(candidate) != null ||
                parseAmbiguousTwoWordCreatorInput(candidate) != null
        }
        if (strongMatch != null) return strongMatch

        if (firstLine.isNotBlank() && firstLine.length <= 80) {
            val tokenCount = firstLine.split(Regex("\\s+")).count { it.isNotBlank() }
            if (tokenCount in 1..6) return firstLine
        }
        return null
    }

    fun openSelectedInBrowser() {
        val detail = selectedDetail
        if (detail == null) {
            infoDialogMessage = "Select an entry first."
            return
        }
        viewModelScope.launch {
            var currentDetail: EntryDetail = detail
            if (currentDetail.mediaId <= 0L || currentDetail.numPages <= 0) {
                setStatus("Refreshing image metadata for code ${detail.code}...")
                val result = withContext(Dispatchers.IO) {
                    runCatching { client.fetchGallery(detail.code) }
                }
                result.onSuccess { gallery ->
                    db.upsertGallery(gallery)
                    loadEntries(detail.code)
                    val refreshed = db.getEntryDetail(detail.code)
                    if (refreshed != null) {
                        currentDetail = refreshed
                        selectedCode = refreshed.code
                        selectedDetail = refreshed
                        scheduleSeriesNeighborComputation(refreshed.code, refreshed)
                    }
                }.onFailure { exc ->
                    when (exc) {
                        is GalleryNotFoundException -> {
                            errorDialogMessage = exc.message ?: "Code not found."
                            setStatus("Could not refresh metadata: code not found.")
                        }
                        is GalleryFetchException -> {
                            errorDialogMessage = exc.message ?: "Network/server error."
                            setStatus("Could not refresh metadata: network or server error.")
                        }
                        else -> {
                            errorDialogMessage = exc.message ?: "Unexpected error."
                            setStatus("Could not refresh metadata: unexpected error.")
                        }
                    }
                }
            }

            val finalDetail = currentDetail
            if (finalDetail.mediaId <= 0L || finalDetail.numPages <= 0) {
                errorDialogMessage = "Could not open slideshow because image metadata is unavailable for code ${detail.code}."
                setStatus("Slideshow failed: missing image metadata.")
                pendingBrowserRatingCode = null
                awaitingBrowserRatingPrompt = false
                return@launch
            }

            val opened = openInAppSlideshow(
                code = finalDetail.code,
                title = finalDetail.title,
                mediaId = finalDetail.mediaId,
                coverExt = finalDetail.coverExt,
                numPages = finalDetail.numPages
            )
            if (opened) {
                pendingBrowserRatingCode = finalDetail.code
                awaitingBrowserRatingPrompt = true
            } else {
                pendingBrowserRatingCode = null
                awaitingBrowserRatingPrompt = false
            }
        }
    }

    fun openInputOrHomeInBrowser() {
        val rawInput = codeInput.trim()
        if (rawInput.isBlank() || NHENTAI_HOME_PATTERN.matches(rawInput)) {
            openHomeInBrowser()
            return
        }

        val directRouteLink = parseDirectRouteLinkInput(rawInput)
        if (directRouteLink != null) {
            openDirectRouteInBrowser(directRouteLink)
            return
        }

        val creatorLink = parseCreatorLink(rawInput)
        if (creatorLink != null) {
            openGalleryCodeBrowser(
                initialCode = null,
                initialQuery = "",
                initialCreatorType = creatorLink.type,
                initialCreatorName = creatorLink.name,
                blockedTags = emptyList(),
                successStatus = "Opened ${creatorLink.type} '${creatorLink.name}' preview in browser."
            )
            return
        }

        val typedCreator = parseTypedCreatorInput(rawInput)
        if (typedCreator != null) {
            openGalleryCodeBrowser(
                initialCode = null,
                initialQuery = "",
                initialCreatorType = typedCreator.first,
                initialCreatorName = typedCreator.second,
                blockedTags = emptyList(),
                successStatus = "Opened ${typedCreator.first} '${typedCreator.second}' preview in browser."
            )
            return
        }

        val code = parseCode(rawInput)
        if (code != null) {
            openGalleryCodeBrowser(
                initialCode = code,
                initialQuery = "",
                blockedTags = emptyList(),
                successStatus = "Opened code $code preview in gallery browser."
            )
            return
        }

        val directRoute = resolveSingleDirectRouteFromSearch(rawInput)
        if (directRoute != null) {
            openDirectRouteInBrowser(directRoute)
            return
        }

        viewModelScope.launch {
            setStatus("Resolving preview for '$rawInput'...")
            val result = withContext(Dispatchers.IO) {
                runCatching { client.resolveCreatorByName(rawInput) }
            }
            result.onSuccess { resolved ->
                if (resolved != null) {
                    openGalleryCodeBrowser(
                        initialCode = null,
                        initialQuery = "",
                        initialCreatorType = resolved.type,
                        initialCreatorName = resolved.name,
                        blockedTags = emptyList(),
                        successStatus = "Opened ${resolved.type} '${resolved.name}' preview in browser."
                    )
                } else {
                    errorDialogMessage =
                        "Input was not recognized as a code and no matching artist/group page was found."
                    setStatus("Preview failed: no matching code or artist/group found.")
                }
            }.onFailure { exc ->
                errorDialogMessage = exc.message ?: "Unexpected error while resolving preview input."
                setStatus("Preview failed: unexpected error.")
            }
        }
    }

    private fun openGalleryCodeBrowser(
        initialCode: Int? = null,
        initialQuery: String = "",
        initialCreatorType: String? = null,
        initialCreatorName: String? = null,
        incognitoModeEnabled: Boolean = this.incognitoModeEnabled,
        blockedTags: List<String> = emptyList(),
        successStatus: String
    ): Boolean {
        val app = getApplication<Application>()
        val result = runCatching {
            val intent = GalleryCodeBrowserActivity.createIntent(
                context = app,
                initialCode = initialCode,
                initialQuery = initialQuery,
                initialCreatorType = initialCreatorType,
                initialCreatorName = initialCreatorName,
                incognitoModeEnabled = incognitoModeEnabled,
                blockedTags = blockedTags
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(intent)
        }.onSuccess {
            setStatus(successStatus)
        }.onFailure { exc ->
            errorDialogMessage = "Could not open gallery browser:\n${exc.message ?: "unknown error"}"
            setStatus("Could not open gallery browser.")
        }
        return result.isSuccess
    }

    private fun openInAppSlideshow(
        code: Int,
        title: String,
        mediaId: Long,
        coverExt: String,
        numPages: Int
    ): Boolean {
        val app = getApplication<Application>()
        val result = runCatching {
            val intent = GallerySlideshowActivity.createIntent(
                context = app,
                code = code,
                title = title,
                mediaId = mediaId,
                coverExt = coverExt,
                numPages = numPages
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(intent)
        }.onSuccess {
            setStatus("Opened code $code in slideshow.")
        }.onFailure { exc ->
            errorDialogMessage = "Could not open slideshow:\n${exc.message ?: "unknown error"}"
            setStatus("Could not open slideshow.")
        }
        return result.isSuccess
    }

    fun openHomeInBrowser() {
        val blockedTerms = if (applyBlockedTagsToHome) {
            blockedTagNamesForBrowser()
        } else {
            emptyList()
        }
        openGalleryCodeBrowser(
            initialCode = null,
            initialQuery = "",
            blockedTags = blockedTerms,
            successStatus = if (blockedTerms.isNotEmpty()) {
                "Opened homepage browser with blocked tags applied."
            } else {
                "Opened homepage browser."
            }
        )
    }

    fun addOrUpdateByInput() {
        val rawInput = codeInput.trim()
        if (rawInput.isBlank()) {
            errorDialogMessage = "Enter a gallery code, artist/group link, or artist/group name."
            return
        }

        val creatorLink = parseCreatorLink(rawInput)
        if (creatorLink != null) {
            addCreatorLink(creatorLink)
            return
        }

        val code = parseCode(rawInput)
        if (code != null) {
            viewModelScope.launch {
                setStatus("Fetching code $code...")
                val result = withContext(Dispatchers.IO) {
                    runCatching { client.fetchGallery(code) }
                }
                result.onSuccess { gallery ->
                    db.upsertGallery(gallery)
                    updateUnifiedInput("")
                    refreshAll(code)
                    setStatus("Saved/updated code $code.")
                }.onFailure { exc ->
                    when (exc) {
                        is GalleryNotFoundException -> {
                            errorDialogMessage = exc.message ?: "Code not found."
                            setStatus("Lookup failed: code not found.")
                        }
                        is GalleryFetchException -> {
                            errorDialogMessage = exc.message ?: "Network/server error."
                            setStatus("Lookup failed: network or server error.")
                        }
                        else -> {
                            errorDialogMessage = exc.message ?: "Unexpected error."
                            setStatus("Lookup failed: unexpected error.")
                        }
                    }
                }
            }
            return
        }

        val ambiguousTwoWord = parseAmbiguousTwoWordCreatorInput(rawInput)
        if (ambiguousTwoWord != null) {
            manualCreatorPromptState = ManualCreatorPromptState(ambiguousTwoWord)
            return
        }

        viewModelScope.launch {
            setStatus("Resolving artist/group '$rawInput'...")
            val result = withContext(Dispatchers.IO) {
                runCatching { client.resolveCreatorByName(rawInput) }
            }
            result.onSuccess { resolved ->
                if (resolved != null) {
                    addCreatorLink(resolved)
                } else {
                    errorDialogMessage =
                        "Input was not recognized as a code and no matching artist/group page was found."
                    setStatus("Lookup failed: no matching artist/group found.")
                }
            }.onFailure { exc ->
                errorDialogMessage = exc.message ?: "Unexpected error while resolving artist/group."
                setStatus("Lookup failed: unexpected error.")
            }
        }
    }

    private fun addCreatorLink(creatorLink: CreatorLink) {
        val added = db.addCreator(
            name = creatorLink.name,
            creatorType = creatorLink.type,
            sourceUrl = creatorLink.sourceUrl
        )
        updateUnifiedInput("")
        refreshAll(selectedCode)
        setStatus(
            if (added) {
                "Added ${creatorLink.type} '${creatorLink.name}'."
            } else {
                "${creatorLink.type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }} '${creatorLink.name}' is already tracked."
            }
        )
    }

    fun onManualCreatorPromptAnswered(treatAsSingleEntry: Boolean) {
        val phrase = manualCreatorPromptState?.phrase ?: return
        manualCreatorPromptState = null

        val candidates = if (treatAsSingleEntry) {
            listOf(toHyphenatedTwoWordCreatorName(phrase))
        } else {
            splitTwoWordCreatorName(phrase)
        }
        resolveAndSaveCreatorCandidates(candidates, fromAmbiguousPrompt = true)
    }

    fun cancelManualCreatorPrompt() {
        manualCreatorPromptState = null
        setStatus("Creator lookup cancelled.")
    }

    private fun resolveAndSaveCreatorCandidates(
        rawCandidates: List<String>,
        fromAmbiguousPrompt: Boolean
    ) {
        val candidates = rawCandidates
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.US) }
        if (candidates.isEmpty()) {
            errorDialogMessage = "No valid creator name candidates to resolve."
            return
        }

        viewModelScope.launch {
            setStatus("Resolving ${candidates.size} artist/group name(s)...")
            val (resolved, unresolved) = withContext(Dispatchers.IO) {
                val links = mutableListOf<CreatorLink>()
                var unresolvedCount = 0
                candidates.forEach { candidate ->
                    val link = runCatching { client.resolveCreatorByName(candidate) }.getOrNull()
                    if (link == null) {
                        unresolvedCount += 1
                    } else {
                        links += link
                    }
                }
                links to unresolvedCount
            }

            if (resolved.isEmpty()) {
                errorDialogMessage = if (fromAmbiguousPrompt) {
                    "Could not resolve that two-word creator input.\nTry artist:<name> or group:<name>."
                } else {
                    "Input was not recognized as a code and no matching artist/group page was found."
                }
                setStatus("Lookup failed: no matching artist/group found.")
                return@launch
            }

            val deduped = linkedMapOf<String, CreatorLink>()
            resolved.forEach { link ->
                val key = "${link.type}:${normalizeTagName(link.name)}"
                deduped[key] = link
            }

            val (added, skipped) = withContext(Dispatchers.IO) {
                var addedCount = 0
                var skippedCount = 0
                deduped.values.forEach { link ->
                    if (db.addCreator(link.name, link.type, link.sourceUrl)) {
                        addedCount += 1
                    } else {
                        skippedCount += 1
                    }
                }
                addedCount to skippedCount
            }

            updateUnifiedInput("")
            refreshAll(selectedCode)
            if (unresolved > 0) {
                infoDialogMessage = "Resolved ${deduped.size} creator/group item(s), $unresolved could not be resolved."
            }
            setStatus("Added $added creator/group item(s), skipped $skipped, unresolved $unresolved.")
        }
    }

    fun refetchCode(code: Int) {
        if (code <= 0) return
        viewModelScope.launch {
            setStatus("Re-fetching code $code...")
            val result = withContext(Dispatchers.IO) {
                runCatching { client.fetchGallery(code) }
            }
            result.onSuccess { gallery ->
                db.upsertGallery(gallery)
                refreshAll(code)
                setStatus("Re-fetched code $code.")
            }.onFailure { exc ->
                when (exc) {
                    is GalleryNotFoundException -> {
                        errorDialogMessage = exc.message ?: "Code not found."
                        setStatus("Re-fetch failed: code not found.")
                    }
                    is GalleryFetchException -> {
                        errorDialogMessage = exc.message ?: "Network/server error."
                        setStatus("Re-fetch failed: network or server error.")
                    }
                    else -> {
                        errorDialogMessage = exc.message ?: "Unexpected error."
                        setStatus("Re-fetch failed: unexpected error.")
                    }
                }
            }
        }
    }

    fun deleteSelected() {
        val code = selectedCode
        if (code == null) {
            infoDialogMessage = "Select an entry first."
            return
        }

        db.deleteEntry(code)
        refreshAll(null)
        setStatus("Deleted code $code.")
    }

    fun clearAllWithoutExport() {
        val deleted = db.clearAllEntries()
        refreshAll(null)
        infoDialogMessage = "Cleared ${deleted.entriesCleared} entries and ${deleted.creatorsCleared} artists/groups."
        setStatus("Cleared ${deleted.entriesCleared} entries and ${deleted.creatorsCleared} artists/groups.")
    }

    fun setEntryRating(code: Int, rating: Int) {
        val safe = rating.coerceIn(0, 5)
        db.setEntryRating(code, safe)
        db.setEntryRead(code, true)
        readAnalyticsLoaded = false
        loadEntries(code)
        selectEntry(code)
        setStatus("Set rating for $code to $safe/5 and marked as read.")
    }

    fun toggleEntryRead(code: Int) {
        if (incognitoModeEnabled) {
            setStatus("Read/unread toggle is disabled in incognito mode.")
            return
        }
        val current = entries.firstOrNull { it.code == code }?.isRead
            ?: selectedDetail?.takeIf { it.code == code }?.isRead
            ?: db.getEntryDetail(code)?.isRead
            ?: false
        val next = !current
        db.setEntryRead(code, next)
        readAnalyticsLoaded = false
        loadEntries(code)
        selectEntry(code)
        setStatus(
            if (next) {
                "Marked code $code as read."
            } else {
                "Marked code $code as unread."
            }
        )
    }

    fun updateBrowserRatingSelection(rating: Int) {
        val prompt = browserRatingPromptState ?: return
        browserRatingPromptState = prompt.copy(rating = rating.coerceIn(0, 5))
    }

    fun saveBrowserRatingPrompt() {
        val prompt = browserRatingPromptState ?: return
        db.setEntryRating(prompt.code, prompt.rating.coerceIn(0, 5))
        db.setEntryRead(prompt.code, true)
        readAnalyticsLoaded = false
        browserRatingPromptState = null
        pendingBrowserRatingCode = null
        loadEntries(prompt.code)
        selectEntry(prompt.code)
        setStatus("Saved rating for ${prompt.code} and marked as read.")
    }

    fun skipBrowserRatingPrompt() {
        browserRatingPromptState = null
        pendingBrowserRatingCode = null
        setStatus("Skipped browser-exit rating prompt.")
    }

    fun copyCodeToClipboard(code: Int) {
        if (code <= 0) return
        val app = getApplication<Application>()
        val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard == null) {
            setStatus("Could not access clipboard.")
            return
        }
        clipboard.setPrimaryClip(
            ClipData.newPlainText("Sauce Tracker code", code.toString())
        )
        setStatus("Copied code $code to clipboard.")
    }

    fun copyCreatorNameToClipboard(creatorType: String, creatorName: String) {
        val cleanName = creatorName.trim()
        if (cleanName.isBlank()) return
        val app = getApplication<Application>()
        val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard == null) {
            setStatus("Could not access clipboard.")
            return
        }
        clipboard.setPrimaryClip(
            ClipData.newPlainText("Sauce Tracker $creatorType", cleanName)
        )
        setStatus("Copied $creatorType '$cleanName' to clipboard.")
    }

    fun pasteCodeInputFromClipboard() {
        val app = getApplication<Application>()
        val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard == null) {
            setStatus("Could not access clipboard.")
            return
        }
        val pasted = clipboard.primaryClip
            ?.getItemAt(0)
            ?.coerceToText(app)
            ?.toString()
            .orEmpty()
            .trim()
        if (pasted.isBlank()) {
            setStatus("Clipboard is empty.")
            return
        }
        updateUnifiedInput(pasted)
        setStatus("Pasted input from clipboard.")
    }

    fun requestToggleEntryPinned(code: Int) {
        if (code <= 0) return
        if (incognitoModeEnabled) {
            setStatus("Pin/unpin is disabled in incognito mode.")
            return
        }
        val currentlyPinned = entries.firstOrNull { it.code == code }?.pinned ?: false
        pinTogglePromptState = PinTogglePromptState(
            code = code,
            targetPinned = !currentlyPinned
        )
    }

    fun dismissPinTogglePrompt() {
        pinTogglePromptState = null
    }

    fun confirmPinToggle() {
        val prompt = pinTogglePromptState ?: return
        pinTogglePromptState = null
        if (incognitoModeEnabled) {
            setStatus("Pin/unpin is disabled in incognito mode.")
            return
        }
        val code = prompt.code
        val newPinned = prompt.targetPinned
        db.setEntryPinned(code, newPinned)
        loadEntries(code)
        selectEntry(code)
        setStatus(
            if (newPinned) {
                "Pinned code $code."
            } else {
                "Unpinned code $code."
            }
        )
    }

    fun onBatchFileChosen(uri: Uri) {
        viewModelScope.launch {
            val text = runCatching { readTextFromUri(uri) }
            text.onFailure { exc ->
                errorDialogMessage = "Could not read file:\n${exc.message ?: "unknown error"}"
                return@launch
            }

            val fileText = text.getOrNull().orEmpty()
            pendingCreatorAddedCount = 0
            pendingCreatorSkippedCount = 0
            pendingCreatorUnresolvedCount = 0
            batchCreatorPromptState = null
            pendingBatchCreatorLinks = emptyList()
            pendingBatchCreatorBaseNames = emptyList()
            pendingBatchCreatorTwoWordNames = emptyList()
            pendingBatchCodeSourceText = ""
            val (creatorLinks, codeSourceText) = extractCreatorLinks(fileText)
            val creatorNameCandidates = extractCreatorNameCandidates(fileText)
            val twoWordNames = creatorNameCandidates.filter { isTwoWordCreatorName(it) }
            if (twoWordNames.isNotEmpty()) {
                pendingBatchCreatorLinks = creatorLinks
                pendingBatchCodeSourceText = codeSourceText
                pendingBatchCreatorBaseNames = creatorNameCandidates.filterNot { isTwoWordCreatorName(it) }
                pendingBatchCreatorTwoWordNames = twoWordNames
                val preview = twoWordNames.take(5).joinToString(", ") { "'$it'" } +
                    if (twoWordNames.size > 5) ", ..." else ""
                batchCreatorPromptState = BatchCreatorPromptState(
                    count = twoWordNames.size,
                    preview = preview
                )
                return@launch
            }

            processBatchCreatorsAndContinue(
                creatorLinks = creatorLinks,
                creatorNameCandidates = creatorNameCandidates,
                codeSourceText = codeSourceText
            )
        }
    }

    fun onBatchCreatorPromptAnswered(treatAsSingleEntry: Boolean) {
        val creatorLinks = pendingBatchCreatorLinks
        val codeSourceText = pendingBatchCodeSourceText
        val baseNames = pendingBatchCreatorBaseNames
        val twoWordNames = pendingBatchCreatorTwoWordNames

        batchCreatorPromptState = null
        pendingBatchCreatorLinks = emptyList()
        pendingBatchCodeSourceText = ""
        pendingBatchCreatorBaseNames = emptyList()
        pendingBatchCreatorTwoWordNames = emptyList()

        val expandedNames = if (treatAsSingleEntry) {
            twoWordNames.map { toHyphenatedTwoWordCreatorName(it) }
        } else {
            twoWordNames.flatMap { splitTwoWordCreatorName(it) }
        }

        val mergedNames = (baseNames + expandedNames)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.US) }

        viewModelScope.launch {
            processBatchCreatorsAndContinue(
                creatorLinks = creatorLinks,
                creatorNameCandidates = mergedNames,
                codeSourceText = codeSourceText
            )
        }
    }

    fun cancelBatchCreatorPrompt() {
        batchCreatorPromptState = null
        pendingBatchCreatorLinks = emptyList()
        pendingBatchCodeSourceText = ""
        pendingBatchCreatorBaseNames = emptyList()
        pendingBatchCreatorTwoWordNames = emptyList()
        pendingBatchText = null
        pendingSplitSequences = emptyList()
        pendingCandidates = emptyList()
        pendingCreatorAddedCount = 0
        pendingCreatorSkippedCount = 0
        pendingCreatorUnresolvedCount = 0
        setStatus("Batch add/update cancelled.")
    }

    private suspend fun processBatchCreatorsAndContinue(
        creatorLinks: List<CreatorLink>,
        creatorNameCandidates: List<String>,
        codeSourceText: String
    ) {
        val allCreators = linkedMapOf<String, CreatorLink>()
        creatorLinks.forEach { creator ->
            val key = "${creator.type}:${normalizeTagName(creator.name)}"
            allCreators[key] = creator
        }

        if (creatorNameCandidates.isNotEmpty()) {
            val uniqueNames = creatorNameCandidates
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase(Locale.US) }
            if (uniqueNames.isNotEmpty()) {
                setStatus("Resolving ${uniqueNames.size} artist/group name(s)...")
                val resolvedByName = withContext(Dispatchers.IO) {
                    val resolved = mutableListOf<CreatorLink>()
                    var unresolved = 0
                    uniqueNames.forEach { name ->
                        val byName = runCatching { client.resolveCreatorByName(name) }.getOrNull()
                        if (byName == null) {
                            unresolved += 1
                        } else {
                            resolved += byName
                        }
                    }
                    resolved to unresolved
                }
                pendingCreatorUnresolvedCount = resolvedByName.second
                resolvedByName.first.forEach { creator ->
                    val key = "${creator.type}:${normalizeTagName(creator.name)}"
                    allCreators[key] = creator
                }
            }
        }

        if (allCreators.isNotEmpty()) {
            val (added, skipped) = withContext(Dispatchers.IO) {
                var addedCount = 0
                var skippedCount = 0
                allCreators.values.forEach { creator ->
                    if (db.addCreator(creator.name, creator.type, creator.sourceUrl)) {
                        addedCount += 1
                    } else {
                        skippedCount += 1
                    }
                }
                addedCount to skippedCount
            }
            pendingCreatorAddedCount = added
            pendingCreatorSkippedCount = skipped
        }

        val splitSequences = findSplitCodeSequences(codeSourceText)
        pendingBatchText = codeSourceText
        pendingSplitSequences = splitSequences

        if (splitSequences.isNotEmpty()) {
            val preview = splitSequences.take(5).joinToString(", ") { "'${it.raw}'" } +
                if (splitSequences.size > 5) ", ..." else ""
            splitPromptState = SplitPromptState(
                count = splitSequences.size,
                preview = preview
            )
            return
        }

        val candidates = extractCandidates(codeSourceText, splitSequences, combineSplitCodes = false)
        continueBatchAfterExtraction(candidates)
    }

    fun onSplitPromptAnswered(combine: Boolean) {
        splitPromptState = null
        val text = pendingBatchText.orEmpty()
        val candidates = extractCandidates(text, pendingSplitSequences, combine)
        continueBatchAfterExtraction(candidates)
    }

    fun onShortPromptAnswered(skipShortCodes: Boolean) {
        shortPromptState = null
        var candidates = pendingCandidates
        if (skipShortCodes) {
            candidates = candidates.filter { it.second >= 5 }
        }

        if (candidates.isEmpty()) {
            refreshAll(selectedCode)
            if (pendingCreatorAddedCount > 0 || pendingCreatorSkippedCount > 0 || pendingCreatorUnresolvedCount > 0) {
                infoDialogMessage = """
                    All detected codes were skipped based on your choice.
                    Added creators/groups: $pendingCreatorAddedCount
                    Skipped creators/groups: $pendingCreatorSkippedCount
                    Unresolved creator names: $pendingCreatorUnresolvedCount
                """.trimIndent()
                setStatus(
                    "Skipped all codes. Added $pendingCreatorAddedCount creator/group item(s), " +
                        "skipped $pendingCreatorSkippedCount, unresolved names $pendingCreatorUnresolvedCount."
                )
            } else {
                infoDialogMessage = "All detected codes were skipped based on your choice."
            }
            pendingCreatorAddedCount = 0
            pendingCreatorSkippedCount = 0
            pendingCreatorUnresolvedCount = 0
            return
        }

        runBatch(candidates.map { it.first }, operationName = "Batch Add/Update")
    }

    fun cancelBatch() {
        batchCancelRequested = true
        setStatus("Cancel requested. Finishing current request...")
    }

    fun exportToUri(uri: Uri, clearAfterExport: Boolean) {
        viewModelScope.launch {
            val snapshot = runCatching { db.exportSnapshot() }
            snapshot.onFailure { exc ->
                errorDialogMessage = "Export failed:\n${exc.message ?: "unknown error"}"
                return@launch
            }

            val json = snapshot.getOrNull() ?: JSONObject()
            val exportText = buildExportText(json)
            val writeResult = runCatching { writeTextToUri(uri, exportText) }
            writeResult.onFailure { exc ->
                errorDialogMessage = "Export failed:\n${exc.message ?: "unknown error"}"
                return@launch
            }

            val entryCount = json.optJSONArray("entries")?.length() ?: 0
            val creatorCount = json.optJSONArray("creators")?.length() ?: 0
            if (clearAfterExport) {
                val deleted = db.clearAllEntries()
                refreshAll(null)
                infoDialogMessage =
                    "Exported $entryCount entries and $creatorCount creators/groups, then cleared ${deleted.entriesCleared} entries and ${deleted.creatorsCleared} artists/groups."
                setStatus(
                    "Exported and cleared ${deleted.entriesCleared} entries and ${deleted.creatorsCleared} artists/groups."
                )
            } else {
                infoDialogMessage = "Exported $entryCount entries and $creatorCount creators/groups."
                setStatus("Exported $entryCount entries and $creatorCount creators/groups.")
            }
        }
    }

    fun refetchAllEntries() {
        if (batchProgressState != null) {
            infoDialogMessage = "A batch operation is already running."
            return
        }

        viewModelScope.launch {
            val codes = withContext(Dispatchers.IO) { db.listAllEntryCodes() }
            if (codes.isEmpty()) {
                infoDialogMessage = "No saved entries to re-fetch."
                setStatus("Re-fetch all skipped: no saved entries.")
                return@launch
            }

            pendingCreatorAddedCount = 0
            pendingCreatorSkippedCount = 0
            pendingCreatorUnresolvedCount = 0
            setStatus("Starting re-fetch for ${codes.size} entries. This can take a while.")
            runBatch(codes, operationName = "Re-fetch All Entries")
        }
    }

    fun onExportCancelled(clearAfterExport: Boolean) {
        if (clearAfterExport) {
            setStatus("Clear all cancelled (export not completed).")
        } else {
            setStatus("Export cancelled.")
        }
    }

    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            val textResult = runCatching { readTextFromUri(uri) }
            textResult.onFailure { exc ->
                errorDialogMessage = "Import failed:\nCould not read file:\n${exc.message ?: "unknown error"}"
                return@launch
            }

            val text = textResult.getOrNull().orEmpty()
            val payload = try {
                parseImportText(text)
            } catch (exc: Exception) {
                errorDialogMessage = "Import failed:\n${exc.message ?: "Invalid import file."}"
                return@launch
            }

            val result = runCatching { db.importSnapshot(payload.entries, payload.creators) }
            result.onFailure { exc ->
                errorDialogMessage = "Import failed:\n${exc.message ?: "unknown error"}"
                return@launch
            }

            val import = result.getOrNull() ?: return@launch
            refreshAll(null)
            infoDialogMessage = """
                Processed: ${import.processed}
                Imported: ${import.imported}
                Inserted: ${import.inserted}
                Updated: ${import.updated}
                Skipped: ${import.skipped}
                Creators processed: ${import.creatorsProcessed}
                Creators added: ${import.creatorsAdded}
                Creators skipped: ${import.creatorsSkipped}
            """.trimIndent()
            setStatus(
                "Import complete. Imported ${import.imported} entries " +
                    "(${import.inserted} inserted, ${import.updated} updated, ${import.skipped} skipped), " +
                    "added ${import.creatorsAdded} creator/group item(s)."
            )
        }
    }

    fun defaultExportFilename(): String {
        return "sauce_export_${LocalDateTime.now().format(EXPORT_FILENAME_FORMAT)}.txt"
    }

    private fun continueBatchAfterExtraction(candidates: List<Pair<Int, Int>>) {
        if (candidates.isEmpty()) {
            refreshAll(selectedCode)
            if (pendingCreatorAddedCount > 0 || pendingCreatorSkippedCount > 0 || pendingCreatorUnresolvedCount > 0) {
                infoDialogMessage = """
                    Added creators/groups: $pendingCreatorAddedCount
                    Skipped creators/groups: $pendingCreatorSkippedCount
                    Unresolved creator names: $pendingCreatorUnresolvedCount
                    No numeric gallery codes were found in that .txt file.
                """.trimIndent()
                setStatus(
                    "Added $pendingCreatorAddedCount creator/group item(s), " +
                        "skipped $pendingCreatorSkippedCount, unresolved names $pendingCreatorUnresolvedCount; no codes found."
                )
            } else {
                infoDialogMessage = "No numeric gallery codes were found in that .txt file."
            }
            pendingCreatorAddedCount = 0
            pendingCreatorSkippedCount = 0
            pendingCreatorUnresolvedCount = 0
            return
        }

        val shortCodes = candidates.filter { it.second < 5 }.map { it.first }
        if (shortCodes.isNotEmpty()) {
            pendingCandidates = candidates
            val preview = shortCodes.take(10).joinToString(", ") + if (shortCodes.size > 10) ", ..." else ""
            shortPromptState = ShortPromptState(
                count = shortCodes.size,
                preview = preview
            )
            return
        }

        runBatch(candidates.map { it.first }, operationName = "Batch Add/Update")
    }

    private fun runBatch(codes: List<Int>, operationName: String = "Batch Add/Update") {
        batchDialogTitle = operationName
        if (codes.isEmpty()) {
            if (pendingCreatorAddedCount > 0 || pendingCreatorSkippedCount > 0 || pendingCreatorUnresolvedCount > 0) {
                refreshAll(selectedCode)
                infoDialogMessage = """
                    No codes to process.
                    Added creators/groups: $pendingCreatorAddedCount
                    Skipped creators/groups: $pendingCreatorSkippedCount
                    Unresolved creator names: $pendingCreatorUnresolvedCount
                """.trimIndent()
                setStatus(
                    "No codes to process. Added $pendingCreatorAddedCount creator/group item(s), " +
                        "skipped $pendingCreatorSkippedCount, unresolved names $pendingCreatorUnresolvedCount."
                )
                pendingCreatorAddedCount = 0
                pendingCreatorSkippedCount = 0
                pendingCreatorUnresolvedCount = 0
            } else {
                infoDialogMessage = "No codes to process."
                pendingCreatorUnresolvedCount = 0
            }
            return
        }

        viewModelScope.launch {
            val creatorsAdded = pendingCreatorAddedCount
            val creatorsSkipped = pendingCreatorSkippedCount
            batchCancelRequested = false
            val total = codes.size
            var processed = 0
            var saved = 0
            var notFound = 0
            var failed = 0
            var lastSaved: Int? = null
            val failedItems = mutableListOf<Pair<Int, String>>()
            val notFoundCodes = mutableListOf<Int>()

            batchProgressState = BatchProgressState(
                total = total,
                processed = 0,
                saved = 0,
                notFound = 0,
                failed = 0,
                currentCode = null
            )

            for ((index, code) in codes.withIndex()) {
                if (batchCancelRequested) {
                    break
                }

                batchProgressState = batchProgressState?.copy(currentCode = code)
                setStatus("Batch ${index + 1}/$total: fetching code $code...")

                val fetchResult = withContext(Dispatchers.IO) {
                    runCatching { client.fetchGallery(code) }
                }

                fetchResult.onSuccess { gallery ->
                    db.upsertGallery(gallery)
                    saved += 1
                    lastSaved = code
                }.onFailure { exc ->
                    when (exc) {
                        is GalleryNotFoundException -> {
                            notFound += 1
                            notFoundCodes += code
                        }
                        else -> {
                            failed += 1
                            failedItems += (code to (exc.message ?: "unknown error"))
                        }
                    }
                }

                processed += 1
                batchProgressState = BatchProgressState(
                    total = total,
                    processed = processed,
                    saved = saved,
                    notFound = notFound,
                    failed = failed,
                    currentCode = code
                )
            }

            val cancelled = batchCancelRequested && processed < total
            batchProgressState = null

            refreshAll(lastSaved)

            val summary = buildString {
                appendLine("Requested: $total")
                appendLine("Processed: $processed")
                appendLine("Saved/updated: $saved")
                appendLine("Not found: $notFound")
                appendLine("Failed: $failed")
                appendLine("Creators/groups added: $creatorsAdded")
                appendLine("Creators/groups skipped: $creatorsSkipped")
                appendLine("Creator names unresolved: $pendingCreatorUnresolvedCount")
                if (cancelled) appendLine("Cancelled: yes")
                if (notFoundCodes.isNotEmpty()) {
                    val preview = notFoundCodes.take(12).joinToString(", ") + if (notFoundCodes.size > 12) ", ..." else ""
                    appendLine("Not found codes: $preview")
                }
                if (failedItems.isNotEmpty()) {
                    val first = failedItems.first()
                    appendLine("First error: ${first.first} -> ${first.second}")
                }
            }

            infoDialogMessage = summary.trim()
            val stateWord = if (cancelled) "cancelled" else "complete"
            setStatus(
                "Batch $stateWord. Saved/updated $saved of $processed processed code(s) " +
                    "($notFound not found, $failed failed), creators/groups added $creatorsAdded, unresolved names $pendingCreatorUnresolvedCount."
            )
            pendingCreatorAddedCount = 0
            pendingCreatorSkippedCount = 0
            pendingCreatorUnresolvedCount = 0
        }
    }

    private fun refreshAll(selectCode: Int?) {
        loadTags()
        loadEntries(selectCode)
        loadCreators()
        loadSavedStats()
        readAnalyticsLoaded = false
    }

    fun ensureReadAnalyticsLoaded(forceRefresh: Boolean = false) {
        if (readAnalyticsLoading) return
        if (readAnalyticsLoaded && !forceRefresh) return

        readAnalyticsLoading = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { db.getReadAnalyticsSnapshot() }
            }
            result.onSuccess { snapshot ->
                readAnalytics = snapshot
                readAnalyticsLoaded = true
            }.onFailure { exc ->
                setStatus("Could not load stats: ${exc.message ?: "unknown error"}")
            }
            readAnalyticsLoading = false
        }
    }

    private fun preloadAllOnLaunch() {
        viewModelScope.launch {
            try {
                val totalSteps = if (showThumbnails) 4 else 3
                startupPreloadState = StartupPreloadState(
                    phase = "Loading entries...",
                    completedSteps = 0,
                    totalSteps = totalSteps
                )

                val textSnapshot = entrySearch
                val tagSnapshot = activeTagFilterIds.toList()
                val loadedEntries = withContext(Dispatchers.IO) {
                    db.listEntries(
                        textFilter = textSnapshot,
                        tagFilterIds = tagSnapshot,
                        sortField = sortField,
                        sortDirection = sortDirection,
                        readFilter = entryReadFilter
                    )
                }

                val targetCode = when {
                    selectedCode != null && loadedEntries.any { it.code == selectedCode } -> selectedCode
                    loadedEntries.size == 1 -> loadedEntries.first().code
                    else -> null
                }
                val detail = withContext(Dispatchers.IO) {
                    targetCode?.let { db.getEntryDetail(it) }
                }

                entries = loadedEntries
                selectedCode = targetCode
                selectedDetail = detail
                scheduleSeriesNeighborComputation(targetCode, detail)
                startupPreloadState = StartupPreloadState(
                    phase = "Loading tags...",
                    completedSteps = 1,
                    totalSteps = totalSteps
                )

                val loadedTags = withContext(Dispatchers.IO) {
                    db.listTagCounts(
                        textFilter = textSnapshot,
                        sortField = tagSortField,
                        sortDirection = tagSortDirection
                    )
                }
                tags = loadedTags
                loadedTags.forEach { tag ->
                    tagNameCache[tag.id] = tag.name
                    tagRouteCache[tag.id] = TagRouteRef(name = tag.name, type = tag.type)
                }

                val filtered = activeTagFilterIds.filter { tagId ->
                    val ref = db.getTagRouteRef(tagId)
                    if (ref != null) {
                        tagNameCache[tagId] = ref.name
                        tagRouteCache[tagId] = ref
                        true
                    } else {
                        false
                    }
                }
                if (filtered.size != activeTagFilterIds.size) {
                    activeTagFilterIds.clear()
                    activeTagFilterIds.addAll(filtered)
                }

                startupPreloadState = StartupPreloadState(
                    phase = "Loading artists/groups...",
                    completedSteps = 2,
                    totalSteps = totalSteps
                )

                val loadedCreators = withContext(Dispatchers.IO) {
                    db.listCreators(
                        textFilter = entrySearch,
                        tagFilterIds = activeTagFilterIds.toList(),
                        sortField = creatorSortField,
                        sortDirection = creatorSortDirection
                    )
                }
                creators = loadedCreators
                val validIds = loadedCreators.map { it.id }.toSet()
                if (expandedCreatorIds.any { it !in validIds }) {
                    val removed = expandedCreatorIds.filter { it !in validIds }
                    removed.forEach { removedId ->
                        creatorLoadJobs.remove(removedId)?.cancel()
                        loadingCreatorIds.remove(removedId)
                    }
                    val retained = expandedCreatorIds.filter { it in validIds }
                    expandedCreatorIds.clear()
                    expandedCreatorIds.addAll(retained)
                }
                creatorEntriesById = creatorEntriesById.filterKeys { it in validIds && it in expandedCreatorIds }
                loadingCreatorIds.retainAll(validIds)
                if (expandedCreatorIds.isEmpty()) {
                    creatorEntriesById = emptyMap()
                    loadingCreatorIds.clear()
                    creatorLoadJobs.values.forEach { it.cancel() }
                    creatorLoadJobs.clear()
                    creatorEntryFilterKey = buildCreatorEntryFilterKey()
                }

                savedStats = withContext(Dispatchers.IO) { db.getSavedStats() }

                if (showThumbnails) {
                    val allUrls = loadedEntries
                        .map { it.thumbnailUrl.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                    val selectedCount = ((allUrls.size * (preloadPercent / 100f)).roundToInt())
                        .coerceIn(0, allUrls.size)
                    val urls = allUrls.take(selectedCount)
                    val missingUrls = urls.filter { ThumbnailBitmapCache.get(it) == null }
                    val thumbsTotal = missingUrls.size
                    if (thumbsTotal > 0) {
                        val parallelism = Runtime.getRuntime().availableProcessors()
                            .coerceIn(THUMB_PRELOAD_MIN_PARALLEL, THUMB_PRELOAD_MAX_PARALLEL)
                        var done = 0
                        startupPreloadState = StartupPreloadState(
                            phase = "Preloading thumbnails...",
                            completedSteps = 3,
                            totalSteps = totalSteps,
                            thumbsDone = 0,
                            thumbsTotal = thumbsTotal
                        )
                        missingUrls.chunked(parallelism).forEach { batch ->
                            batch.map { url ->
                                async(Dispatchers.IO) {
                                    runCatching { fetchThumbnailBitmap(url) }
                                        .getOrNull()
                                        ?.let { fetched -> ThumbnailBitmapCache.put(url, fetched) }
                                }
                            }.awaitAll()

                            done += batch.size
                            startupPreloadState = StartupPreloadState(
                                phase = "Preloading thumbnails...",
                                completedSteps = 3,
                                totalSteps = totalSteps,
                                thumbsDone = done.coerceAtMost(thumbsTotal),
                                thumbsTotal = thumbsTotal
                            )
                        }
                    }
                }

                setStatus(
                    if (showThumbnails) {
                        "Launch preload complete: entries, tags, artists/groups, and thumbnails."
                    } else {
                        "Launch preload complete: entries, tags, and artists/groups."
                    }
                )
            } catch (_: OutOfMemoryError) {
                ThumbnailBitmapCache.clear()
                setStatus("Launch preload skipped thumbnail warmup due memory pressure.")
                refreshAll(selectedCode)
            } catch (exc: Throwable) {
                setStatus("Launch preload fallback: ${exc.message ?: "unexpected error"}")
                refreshAll(selectedCode)
            } finally {
                startupPreloadState = null
            }
        }
    }

    private fun loadEntries(selectCode: Int?, autoSelectFirst: Boolean = true) {
        entries = db.listEntries(
            textFilter = entrySearch,
            tagFilterIds = activeTagFilterIds.toList(),
            sortField = sortField,
            sortDirection = sortDirection,
            readFilter = entryReadFilter
        )

        val targetCode = when {
            selectCode != null && entries.any { it.code == selectCode } -> selectCode
            selectedCode != null && entries.any { it.code == selectedCode } -> selectedCode
            autoSelectFirst && entries.size == 1 -> entries.first().code
            else -> null
        }

        selectedCode = targetCode
        selectedDetail = targetCode?.let { db.getEntryDetail(it) }
        scheduleSeriesNeighborComputation(targetCode, selectedDetail)
    }

    private fun loadTags() {
        tags = db.listTagCounts(
            textFilter = entrySearch,
            sortField = tagSortField,
            sortDirection = tagSortDirection
        )

        tags.forEach { tag ->
            tagNameCache[tag.id] = tag.name
            tagRouteCache[tag.id] = TagRouteRef(name = tag.name, type = tag.type)
        }

        // Keep active filters even if hidden by current search text.
        // Only drop filters if the tag no longer exists in the database.
        val filtered = activeTagFilterIds.filter { tagId ->
            val ref = db.getTagRouteRef(tagId)
            if (ref != null) {
                tagNameCache[tagId] = ref.name
                tagRouteCache[tagId] = ref
                true
            } else {
                false
            }
        }
        if (filtered.size != activeTagFilterIds.size) {
            activeTagFilterIds.clear()
            activeTagFilterIds.addAll(filtered)
            loadEntries(selectedCode)
            loadCreators()
        }
    }

    private fun loadPopularTags() {
        popularTags = db.listPopularTags(
            sortField = blockedTagSortField,
            sortDirection = blockedTagSortDirection
        )
    }

    private fun loadCreators() {
        creators = db.listCreators(
            textFilter = entrySearch,
            tagFilterIds = activeTagFilterIds.toList(),
            sortField = creatorSortField,
            sortDirection = creatorSortDirection
        )
        val validIds = creators.map { it.id }.toSet()
        if (expandedCreatorIds.any { it !in validIds }) {
            val removed = expandedCreatorIds.filter { it !in validIds }
            removed.forEach { removedId ->
                creatorLoadJobs.remove(removedId)?.cancel()
                loadingCreatorIds.remove(removedId)
            }
            val retained = expandedCreatorIds.filter { it in validIds }
            expandedCreatorIds.clear()
            expandedCreatorIds.addAll(retained)
        }

        creatorEntriesById = creatorEntriesById.filterKeys { it in validIds && it in expandedCreatorIds }
        loadingCreatorIds.retainAll(validIds)

        if (expandedCreatorIds.isEmpty()) {
            creatorEntriesById = emptyMap()
            loadingCreatorIds.clear()
            creatorLoadJobs.values.forEach { it.cancel() }
            creatorLoadJobs.clear()
            creatorEntryFilterKey = buildCreatorEntryFilterKey()
            return
        }

        val currentFilterKey = buildCreatorEntryFilterKey()
        val forceRefresh = currentFilterKey != creatorEntryFilterKey
        creatorEntryFilterKey = currentFilterKey
        expandedCreatorIds.forEach { tagId ->
            ensureCreatorEntriesLoaded(tagId, forceRefresh = forceRefresh)
        }
    }

    private fun buildCreatorEntryFilterKey(): String {
        val tags = activeTagFilterIds.toList().sorted().joinToString(",")
        return "${entrySearch.trim()}|$tags"
    }

    private fun ensureCreatorEntriesLoaded(tagId: Long, forceRefresh: Boolean) {
        if (tagId <= 0L || !expandedCreatorIds.contains(tagId)) return
        if (!forceRefresh && creatorEntriesById.containsKey(tagId)) return

        creatorLoadJobs.remove(tagId)?.cancel()
        if (!loadingCreatorIds.contains(tagId)) {
            loadingCreatorIds.add(tagId)
        }

        val searchSnapshot = entrySearch
        val tagFilterSnapshot = activeTagFilterIds.toList()
        creatorLoadJobs[tagId] = viewModelScope.launch {
            val rows = withContext(Dispatchers.IO) {
                db.listEntriesForCreator(
                    tagId = tagId,
                    textFilter = searchSnapshot,
                    tagFilterIds = tagFilterSnapshot
                )
            }

            if (expandedCreatorIds.contains(tagId)) {
                creatorEntriesById = creatorEntriesById + (tagId to rows)
            }
            loadingCreatorIds.remove(tagId)
            creatorLoadJobs.remove(tagId)
        }
    }

    private fun loadThemeMode(): ThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.SYSTEM
    }

    private fun loadAccentMode(): AccentMode {
        val raw = prefs.getString(KEY_ACCENT_MODE, AccentMode.AUTO.name)
        return AccentMode.entries.firstOrNull { it.name == raw } ?: AccentMode.AUTO
    }

    private fun loadShowThumbnails(): Boolean {
        return prefs.getBoolean(KEY_SHOW_THUMBNAILS, false)
    }

    private fun loadPureGalleryMode(): Boolean {
        return prefs.getBoolean(KEY_PURE_GALLERY_MODE, false)
    }

    private fun loadGalleryColumns(): Int {
        return prefs.getInt(KEY_GALLERY_COLUMNS, 2).coerceIn(1, 10)
    }

    private fun loadApplyBlockedTagsToHome(): Boolean {
        return prefs.getBoolean(KEY_APPLY_BLOCKED_TAGS_HOME, false)
    }

    private fun loadApplyBlockedTagsToSearchTerms(): Boolean {
        return prefs.getBoolean(KEY_APPLY_BLOCKED_TAGS_SEARCH, true)
    }

    private fun loadPreloadOnLaunch(): Boolean {
        return prefs.getBoolean(KEY_PRELOAD_ON_LAUNCH, false)
    }

    private fun loadPreloadPercent(): Int {
        return prefs.getInt(KEY_PRELOAD_PERCENT, 35).coerceIn(0, 100)
    }

    private fun loadAutoBackupTreeUri(): String {
        return prefs.getString(KEY_AUTO_BACKUP_TREE_URI, "").orEmpty()
    }

    private fun loadAppLockEnabled(): Boolean {
        return prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
    }

    private fun loadAppLockBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_APP_LOCK_BIOMETRIC_ENABLED, true)
    }

    private fun loadAppLockPinHash(): String {
        return prefs.getString(KEY_APP_LOCK_PIN_HASH, "").orEmpty()
    }

    private fun loadAppLockPinSalt(): String {
        return prefs.getString(KEY_APP_LOCK_PIN_SALT, "").orEmpty()
    }

    private fun loadAppLockGraceUntilMs(): Long {
        return prefs.getLong(KEY_APP_LOCK_GRACE_UNTIL, 0L)
    }

    private fun setAppLockGraceUntil(untilMs: Long) {
        appLockGraceUntilMs = untilMs.coerceAtLeast(0L)
        prefs.edit().putLong(KEY_APP_LOCK_GRACE_UNTIL, appLockGraceUntilMs).apply()
    }

    private fun normalizePinInput(value: String): String {
        return value.filter { it.isDigit() }.take(20)
    }

    private fun isAppLockConfigured(): Boolean {
        return appLockPinHash.isNotBlank() && appLockPinSalt.isNotBlank()
    }

    private fun generatePinSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val payload = "$salt:$pin".toByteArray(Charsets.UTF_8)
        val hash = digest.digest(payload)
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    private fun loadSavedStats() {
        savedStats = db.getSavedStats()
    }

    private fun resolveOrCreateProceduralBackupUri(treeUri: Uri): Uri {
        val resolver = getApplication<Application>().contentResolver
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        if (treeDocId.isBlank()) {
            throw IOException("Invalid backup folder URI.")
        }

        val treeDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)

        resolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            if (idIdx >= 0 && nameIdx >= 0) {
                while (cursor.moveToNext()) {
                    val displayName = cursor.getString(nameIdx).orEmpty()
                    if (displayName == PROCEDURAL_BACKUP_FILENAME) {
                        val docId = cursor.getString(idIdx).orEmpty()
                        if (docId.isNotBlank()) {
                            return DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        }
                    }
                }
            }
        }

        return DocumentsContract.createDocument(
            resolver,
            treeDocumentUri,
            "text/plain",
            PROCEDURAL_BACKUP_FILENAME
        ) ?: throw IOException("Could not create procedural backup file in selected folder.")
    }

    private fun readTextFromUri(uri: Uri): String {
        val resolver = getApplication<Application>().contentResolver
        val rawBytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Could not open selected file.")

        if (rawBytes.size >= 3 &&
            rawBytes[0] == 0xEF.toByte() &&
            rawBytes[1] == 0xBB.toByte() &&
            rawBytes[2] == 0xBF.toByte()
        ) {
            return rawBytes.copyOfRange(3, rawBytes.size).toString(Charsets.UTF_8)
        }

        val utf8 = decodeStrict(rawBytes, Charsets.UTF_8)
        if (utf8 != null) return utf8

        val cp1252 = decodeStrict(rawBytes, Charset.forName("windows-1252"))
        if (cp1252 != null) return cp1252

        val latin1 = decodeStrict(rawBytes, Charsets.ISO_8859_1)
        if (latin1 != null) return latin1

        return rawBytes.toString(Charsets.UTF_8)
    }

    private fun writeTextToUri(uri: Uri, text: String) {
        val resolver = getApplication<Application>().contentResolver
        val out = resolver.openOutputStream(uri, "wt")
            ?: resolver.openOutputStream(uri, "w")
            ?: throw IOException("Could not open destination file.")
        out.use {
            it.write(text.toByteArray(Charsets.UTF_8))
            it.flush()
        }
    }

    private fun buildExportText(snapshot: JSONObject): String {
        val exportedAt = LocalDateTime.now().format(UTC_TIMESTAMP_FORMAT)
        return buildString {
            append(EXPORT_PREFIX)
            append(' ')
            append(exportedAt)
            append('\n')
            append("Format: ")
            append(EXPORT_FORMAT)
            append('\n')
            append(snapshot.toString(2))
            append('\n')
        }
    }

    private fun parseImportText(text: String): ParsedImportPayload {
        val lines = text.lineSequence().toList()
        if (lines.isEmpty() || !lines.first().startsWith(EXPORT_PREFIX)) {
            throw IllegalArgumentException("Invalid backup header. Expected a file starting with 'Sauce exported Date ...'.")
        }

        val jsonStart = text.indexOf('{')
        if (jsonStart < 0) {
            throw IllegalArgumentException("Invalid backup file. Could not find JSON payload.")
        }

        val payload = try {
            JSONObject(text.substring(jsonStart))
        } catch (exc: Exception) {
            throw IllegalArgumentException("Invalid backup JSON: ${exc.message ?: "unknown error"}")
        }

        val entries = payload.optJSONArray("entries")
            ?: throw IllegalArgumentException("Invalid backup structure. Missing 'entries' array.")
        val creators = payload.optJSONArray("creators") ?: JSONArray()
        return ParsedImportPayload(entries = entries, creators = creators)
    }

    private data class SeriesTitleAnalysis(
        val baseKey: String,
        val tokens: Set<String>,
        val sequence: Int?,
        val explicitSequenceHint: Boolean
    )

    private fun scheduleSeriesNeighborComputation(code: Int?, detailHint: EntryDetail?) {
        seriesNeighborsJob?.cancel()
        if (code == null || code <= 0) {
            selectedSeriesNeighbors = SeriesNeighbors()
            return
        }

        seriesNeighborsJob = viewModelScope.launch {
            val neighbors = withContext(Dispatchers.IO) {
                val current = detailHint ?: db.getEntryDetail(code)
                if (current == null) {
                    return@withContext SeriesNeighbors()
                }
                val candidates = db.listSeriesCandidates()
                computeSeriesNeighbors(current, candidates)
            }
            if (selectedCode == code) {
                selectedSeriesNeighbors = neighbors
            }
        }
    }

    private fun computeSeriesNeighbors(
        current: EntryDetail,
        candidates: List<SeriesCandidateRow>
    ): SeriesNeighbors {
        val currentAnalysis = analyzeSeriesTitle(current.title, current.subtitle)
        if (currentAnalysis.baseKey.isBlank() || currentAnalysis.tokens.isEmpty()) {
            return SeriesNeighbors()
        }
        val currentCreatorKeys = (current.tagsByType["artist"].orEmpty() + current.tagsByType["group"].orEmpty())
            .asSequence()
            .map { normalizeTagName(it) }
            .filter { it.isNotBlank() }
            .toSet()

        data class ScoredCandidate(
            val preview: SeriesEntryPreview,
            val analysis: SeriesTitleAnalysis,
            val tokenOverlap: Int,
            val trigramScore: Float,
            val creatorOverlap: Int,
            val familyHint: Boolean
        )

        val scored = candidates.asSequence()
            .filter { it.code != current.code }
            .mapNotNull { candidate ->
                val analysis = analyzeSeriesTitle(candidate.title, candidate.subtitle)
                if (analysis.baseKey.isBlank() || analysis.tokens.isEmpty()) return@mapNotNull null

                val tokenOverlap = tokenIntersection(currentAnalysis.tokens, analysis.tokens)
                val trigramScore = trigramDice(currentAnalysis.baseKey, analysis.baseKey)
                val creatorOverlap = tokenIntersection(currentCreatorKeys, candidate.creatorKeys)
                val creatorBonus = when {
                    creatorOverlap >= 2 -> 0.18f
                    creatorOverlap == 1 -> 0.12f
                    else -> 0f
                }
                val sequenceSignal = currentAnalysis.sequence != null || analysis.sequence != null
                val explicitFamilyMatch =
                    currentAnalysis.baseKey == analysis.baseKey &&
                        (currentAnalysis.explicitSequenceHint || analysis.explicitSequenceHint)
                val familyHint =
                    explicitFamilyMatch ||
                        tokenOverlap >= 2 ||
                        (creatorOverlap > 0 && trigramScore >= 0.62f)
                val score = (seriesSimilarity(currentAnalysis, analysis) + creatorBonus)
                    .coerceIn(0f, 1f)
                val minimumThreshold = when {
                    explicitFamilyMatch -> 0f
                    creatorOverlap > 0 && sequenceSignal -> 0.58f
                    currentAnalysis.tokens.size <= 2 || analysis.tokens.size <= 2 -> 0.82f
                    else -> 0.72f
                }

                if (!explicitFamilyMatch && score < minimumThreshold) return@mapNotNull null
                if (!explicitFamilyMatch) {
                    val passesGate = when {
                        tokenOverlap >= 1 -> true
                        creatorOverlap > 0 && sequenceSignal && trigramScore >= 0.62f -> true
                        trigramScore >= 0.92f -> true
                        else -> false
                    }
                    if (!passesGate) return@mapNotNull null
                }
                if (!explicitFamilyMatch &&
                    creatorOverlap <= 0 &&
                    tokenOverlap < 2 &&
                    trigramScore < 0.75f
                ) {
                    return@mapNotNull null
                }

                ScoredCandidate(
                    preview = SeriesEntryPreview(
                        code = candidate.code,
                        title = candidate.title.ifBlank { "Gallery ${candidate.code}" },
                        sequence = analysis.sequence,
                        score = score
                    ),
                    analysis = analysis,
                    tokenOverlap = tokenOverlap,
                    trigramScore = trigramScore,
                    creatorOverlap = creatorOverlap,
                    familyHint = familyHint
                )
            }
            .sortedWith(
                compareByDescending<ScoredCandidate> { it.preview.score }
                    .thenBy { it.preview.code }
            )
            .toList()

        if (scored.isEmpty()) return SeriesNeighbors()

        val currentSequence = currentAnalysis.sequence
        val sequenceFamily = scored
            .filter {
                it.preview.sequence != null &&
                    (
                        it.familyHint ||
                            it.analysis.baseKey == currentAnalysis.baseKey ||
                            it.creatorOverlap > 0
                        )
            }
            .map { it.preview }
            .sortedBy { it.sequence ?: Int.MAX_VALUE }

        val previous = if (currentSequence != null) {
            sequenceFamily.lastOrNull { (it.sequence ?: Int.MAX_VALUE) < currentSequence }
        } else {
            scored.firstOrNull()?.preview
        }

        val next = if (currentSequence != null) {
            sequenceFamily.firstOrNull { (it.sequence ?: Int.MIN_VALUE) > currentSequence }
        } else {
            scored.firstOrNull { it.preview.code != previous?.code }?.preview
        }

        if (previous == null && next == null) return SeriesNeighbors()
        return SeriesNeighbors(previous = previous, next = next)
    }

    private fun analyzeSeriesTitle(title: String, subtitle: String): SeriesTitleAnalysis {
        val merged = listOf(title.trim(), subtitle.trim())
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .lowercase(Locale.US)
            .replace("&", " and ")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (merged.isBlank()) {
            return SeriesTitleAnalysis(baseKey = "", tokens = emptySet(), sequence = null, explicitSequenceHint = false)
        }

        var working = merged
        var sequence: Int? = null
        var explicitHint = false

        val markerAfter = Regex("\\b(part|pt|chapter|ch|volume|vol|episode|ep|book)\\s*([0-9]{1,3}|[ivxlcdm]{1,6})\\b")
        val markerBefore = Regex("\\b([0-9]{1,3}|[ivxlcdm]{1,6})\\s*(part|pt|chapter|ch|volume|vol|episode|ep|book)\\b")
        val markerMatch = markerAfter.find(working) ?: markerBefore.find(working)
        if (markerMatch != null) {
            val numberGroup = markerMatch.groupValues
                .drop(1)
                .firstOrNull { it.matches(Regex("[0-9]{1,3}|[ivxlcdm]{1,6}")) }
                .orEmpty()
            sequence = parseSeriesNumberToken(numberGroup)
            explicitHint = sequence != null
            if (explicitHint) {
                working = working.removeRange(markerMatch.range).trim()
            }
        }

        if (sequence == null) {
            val trailing = Regex("(.+?)(?:\\s+|\\s*[-:_]\\s*)([0-9]{1,2}|[ivxlcdm]{1,6})\\s*$").find(working)
            if (trailing != null) {
                val parsed = parseSeriesNumberToken(trailing.groupValues.getOrNull(2).orEmpty())
                if (parsed != null) {
                    sequence = parsed
                    explicitHint = true
                    working = trailing.groupValues.getOrNull(1).orEmpty().trim()
                }
            }
        }

        if (sequence == null) {
            // Handle compact titles like "Series2" where number is attached.
            val attachedTrailingDigits = Regex("(.+?)([0-9]{1,2})\\s*$").find(working)
            if (attachedTrailingDigits != null) {
                val parsed = parseSeriesNumberToken(attachedTrailingDigits.groupValues.getOrNull(2).orEmpty())
                val stem = attachedTrailingDigits.groupValues.getOrNull(1).orEmpty().trim()
                if (parsed != null && stem.any { it.isLetter() }) {
                    sequence = parsed
                    explicitHint = true
                    working = stem
                }
            }
        }

        if (sequence == null) {
            // Generic infix parsing: detect a part-number token between title words.
            val words = working.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.size >= 3) {
                val index = (1 until words.lastIndex)
                    .lastOrNull { parseSeriesNumberToken(words[it]) != null }
                if (index != null) {
                    val parsed = parseSeriesNumberToken(words[index])
                    val prefixWords = words.subList(0, index)
                    val suffixWords = words.subList(index + 1, words.size)
                    val prefix = prefixWords.joinToString(" ").trim()
                    val prefixLetterTokens = prefixWords.count { token -> token.any { ch -> ch.isLetter() } }
                    val suffixHasLetterToken = suffixWords.any { token -> token.any { ch -> ch.isLetter() } }
                    if (parsed != null &&
                        suffixWords.isNotEmpty() &&
                        prefixLetterTokens >= 1 &&
                        suffixHasLetterToken &&
                        prefix.any { it.isLetter() }
                    ) {
                        sequence = parsed
                        explicitHint = true
                        working = prefix
                    }
                }
            }
        }

        val stopWords = setOf(
            "the", "a", "an", "of", "to", "for", "in", "on", "at", "and",
            "part", "pt", "chapter", "ch", "volume", "vol", "episode", "ep", "book"
        )
        val rawTokens = working.split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toCollection(linkedSetOf())
        val tokens = rawTokens
            .filterNot { token ->
                token in stopWords ||
                    token.matches(Regex("\\d{1,2}"))
            }
            .toCollection(linkedSetOf())
        val normalizedTokens = if (tokens.isNotEmpty()) {
            tokens
        } else {
            rawTokens
                .filterNot { token -> token.matches(Regex("\\d{1,2}")) }
                .toCollection(linkedSetOf())
        }

        val baseKey = normalizedTokens.joinToString(" ")
        return SeriesTitleAnalysis(
            baseKey = baseKey,
            tokens = normalizedTokens,
            sequence = sequence,
            explicitSequenceHint = explicitHint
        )
    }

    private fun parseSeriesNumberToken(raw: String): Int? {
        val cleaned = raw.trim().lowercase(Locale.US)
        if (cleaned.isBlank()) return null
        val numeric = cleaned.toIntOrNull()
            ?: romanToInt(cleaned)
            ?: return null
        return numeric.takeIf { it in 1..40 }
    }

    private fun romanToInt(raw: String): Int? {
        val roman = raw.trim().uppercase(Locale.US)
        if (!roman.matches(Regex("[IVXLCDM]+"))) return null
        val values = mapOf(
            'I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000
        )
        var total = 0
        var prev = 0
        for (ch in roman.reversed()) {
            val value = values[ch] ?: return null
            if (value < prev) total -= value else total += value
            prev = value
        }
        return total
    }

    private fun seriesSimilarity(a: SeriesTitleAnalysis, b: SeriesTitleAnalysis): Float {
        if (a.baseKey.isBlank() || b.baseKey.isBlank()) return 0f
        if (a.baseKey == b.baseKey) return 1f

        val tokenScore = tokenJaccard(a.tokens, b.tokens)
        val ngramScore = trigramDice(a.baseKey, b.baseKey)
        val containmentBonus = if (
            a.baseKey.contains(b.baseKey) || b.baseKey.contains(a.baseKey)
        ) {
            0.08f
        } else {
            0f
        }
        return (0.58f * tokenScore + 0.42f * ngramScore + containmentBonus)
            .coerceIn(0f, 1f)
    }

    private fun tokenIntersection(a: Set<String>, b: Set<String>): Int {
        if (a.isEmpty() || b.isEmpty()) return 0
        return a.count { it in b }
    }

    private fun tokenJaccard(a: Set<String>, b: Set<String>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val intersection = a.count { it in b }.toFloat()
        val union = (a.size + b.size - intersection).coerceAtLeast(1f)
        return (intersection / union).coerceIn(0f, 1f)
    }

    private fun trigramDice(left: String, right: String): Float {
        val a = ngramSet(left, n = 3)
        val b = ngramSet(right, n = 3)
        if (a.isEmpty() || b.isEmpty()) return 0f
        val overlap = a.count { it in b }.toFloat()
        return ((2f * overlap) / (a.size + b.size).toFloat()).coerceIn(0f, 1f)
    }

    private fun ngramSet(value: String, n: Int): Set<String> {
        val normalized = value.trim().lowercase(Locale.US).replace(Regex("\\s+"), " ")
        if (normalized.length < n) {
            return if (normalized.isBlank()) emptySet() else setOf(normalized)
        }
        val out = linkedSetOf<String>()
        for (idx in 0..(normalized.length - n)) {
            out += normalized.substring(idx, idx + n)
        }
        return out
    }

    override fun onCleared() {
        super.onCleared()
        seriesNeighborsJob?.cancel()
        creatorLoadJobs.values.forEach { it.cancel() }
        creatorLoadJobs.clear()
        loadingCreatorIds.clear()
        db.close()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagBookApp(vm: TagBookViewModel) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val useDark = when (vm.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val useIncognitoPalette = vm.incognitoModeEnabled

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

    val incognitoScheme = darkColorScheme(
        primary = Color(0xFF9EA9FF),
        onPrimary = Color(0xFF161A33),
        secondary = Color(0xFFC2A8FF),
        onSecondary = Color(0xFF24183E),
        tertiary = Color(0xFF88B7FF),
        onTertiary = Color(0xFF10213B),
        background = Color(0xFF141B2C),
        onBackground = Color(0xFFE8ECFA),
        surface = Color(0xFF1C2438),
        onSurface = Color(0xFFE8ECFA),
        surfaceVariant = Color(0xFF27324D),
        onSurfaceVariant = Color(0xFFC0C9EC),
        outline = Color(0xFF7C88B8),
        error = Color(0xFFFF97A8)
    )

    val colorScheme = when {
        useIncognitoPalette -> incognitoScheme
        else -> {
            val baseScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                fallbackScheme
            }
            applyAccentMode(baseScheme, vm.accentMode, useDark)
        }
    }

        MaterialTheme(colorScheme = colorScheme) {
            key(vm.themeMode, vm.accentMode, useDark, vm.incognitoModeEnabled) {
                ApplySystemBars(
                    darkContent = if (useIncognitoPalette) false else !useDark,
                    barColor = colorScheme.background.toArgb()
                )
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        TagBookScreen(vm)
                    }
                    if (vm.appLockEnabled && vm.appLocked) {
                        AppLockOverlay(vm = vm)
                    }
                }
            }
        }
}

@Composable
private fun AppLockOverlay(vm: TagBookViewModel) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    var imeBottomPx by remember { mutableStateOf(0) }
    DisposableEffect(view) {
        val visibleRect = android.graphics.Rect()
        val listener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
            view.getWindowVisibleDisplayFrame(visibleRect)
            val rootHeight = view.rootView.height.coerceAtLeast(0)
            val visibleHeight = (visibleRect.bottom - visibleRect.top).coerceAtLeast(0)
            val diff = (rootHeight - visibleHeight).coerceAtLeast(0)
            imeBottomPx = if (rootHeight > 0 && diff > (rootHeight * 0.15f)) diff else 0
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            if (view.viewTreeObserver.isAlive) {
                view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
            }
        }
    }
    val imeVisible = imeBottomPx > 0
    val imeBottomDp = with(density) { imeBottomPx.toDp() }
    val focusRequester = remember(vm.appLockNonce) { FocusRequester() }
    val biometricManager = remember(context) { BiometricManager.from(context) }
    val biometricAvailable = remember(biometricManager) {
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }
    var pinInput by remember(vm.appLockNonce) { mutableStateOf("") }
    var helperMessage by remember(vm.appLockNonce) { mutableStateOf<String?>(null) }
    var biometricPromptInFlight by remember(vm.appLockNonce) { mutableStateOf(false) }
    var autoPromptedNonce by remember { mutableStateOf<Long?>(null) }

    fun submitPin() {
        val normalized = pinInput.filter { it.isDigit() }.take(20)
        pinInput = normalized
        if (normalized.isBlank()) {
            helperMessage = "Enter PIN."
            return
        }
        val unlocked = vm.tryUnlockWithPin(normalized)
        if (unlocked) {
            keyboardController?.hide()
            helperMessage = null
        } else {
            helperMessage = "Incorrect PIN."
        }
    }

    fun startBiometricPrompt() {
        if (!vm.appLocked || !vm.appLockEnabled || !vm.appLockBiometricEnabled) return
        if (!biometricAvailable || activity == null || biometricPromptInFlight) return

        biometricPromptInFlight = true
        helperMessage = null
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    biometricPromptInFlight = false
                    helperMessage = null
                    vm.unlockAppFromBiometric()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    biometricPromptInFlight = false
                    helperMessage = if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        "Use your PIN to unlock."
                    } else {
                        errString.toString().ifBlank { "Biometric unlock failed." }
                    }
                }

                override fun onAuthenticationFailed() {
                    biometricPromptInFlight = false
                    helperMessage = "Biometric not recognized. Try again or use PIN."
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Sauce Tracker")
            .setSubtitle("Use fingerprint or face unlock")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButtonText("Use PIN")
            .build()

        prompt.authenticate(promptInfo)
    }

    LaunchedEffect(vm.appLockNonce, vm.appLocked, vm.appLockBiometricEnabled, biometricAvailable) {
        if (!vm.appLocked || !vm.appLockEnabled) return@LaunchedEffect
        if (!vm.appLockBiometricEnabled || !biometricAvailable) return@LaunchedEffect
        if (autoPromptedNonce == vm.appLockNonce) return@LaunchedEffect
        autoPromptedNonce = vm.appLockNonce
        startBiometricPrompt()
    }

    LaunchedEffect(vm.appLockNonce, vm.appLocked) {
        if (!vm.appLocked || !vm.appLockEnabled) return@LaunchedEffect
        delay(120)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    BackHandler(enabled = vm.appLocked) {
        // Keep lock screen in place until unlock succeeds.
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {},
            contentAlignment = if (imeVisible) Alignment.BottomCenter else Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
                modifier = if (imeVisible) {
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .padding(bottom = imeBottomDp + 8.dp, top = 12.dp)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 22.dp)
                }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Sauce Tracker Locked",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (biometricAvailable && vm.appLockBiometricEnabled) {
                            "Use fingerprint/face unlock or enter your PIN."
                        } else {
                            "Enter your PIN to unlock."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { raw ->
                            pinInput = raw.filter { it.isDigit() }.take(20)
                            helperMessage = null
                        },
                        label = { Text("PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitPin() },
                            onGo = { submitPin() },
                            onSend = { submitPin() },
                            onSearch = { submitPin() }
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { pinInput = ""; helperMessage = null }) {
                            Text("Clear")
                        }
                        if (biometricAvailable && vm.appLockBiometricEnabled) {
                            TextButton(onClick = ::startBiometricPrompt) {
                                Text("Use biometric")
                            }
                        }
                    }
                    Text(
                        text = helperMessage ?: "Press Enter/Done on keyboard to unlock with PIN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (helperMessage != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ApplySystemBars(
    darkContent: Boolean,
    barColor: Int
) {
    val view = LocalView.current
    if (view.isInEditMode) return

    SideEffect {
        val activity = view.context as? Activity ?: return@SideEffect
        val window = activity.window
        window.statusBarColor = barColor
        window.navigationBarColor = barColor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = darkContent
            isAppearanceLightNavigationBars = darkContent
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagBookScreen(vm: TagBookViewModel) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showClearAllPrompt by remember { mutableStateOf(false) }
    var showClearUnsafePrompt by remember { mutableStateOf(false) }
    var showRefetchAllPrompt by remember { mutableStateOf(false) }
    var pendingClearAfterExport by remember { mutableStateOf(false) }
    var showSettingsTab by remember { mutableStateOf(false) }
    var settingsDataExpanded by remember { mutableStateOf(false) }
    var settingsDisplayExpanded by remember { mutableStateOf(false) }
    var settingsSecurityExpanded by remember { mutableStateOf(false) }
    var settingsStatsExpanded by remember { mutableStateOf(false) }
    var selectedStatsRange by remember { mutableStateOf(StatsRange.WEEK) }
    var showBlockedTagsManager by remember { mutableStateOf(false) }
    var showEntryLayoutDialog by remember { mutableStateOf(false) }
    var pendingEntryLayoutApplyMode by remember { mutableStateOf<Boolean?>(null) }
    var pendingEntryLayoutApplyColumns by remember { mutableStateOf(2) }
    var thumbnailPreview by remember { mutableStateOf<ThumbnailPreviewState?>(null) }
    var showEnableAppLockDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showDisableAppLockPrompt by remember { mutableStateOf(false) }
    var showEnableGalleryNeedsThumbnailsPrompt by remember { mutableStateOf(false) }
    var showDisableThumbnailsForGalleryPrompt by remember { mutableStateOf(false) }
    var searchFieldFocused by remember { mutableStateOf(false) }
    var blockedTagsSearchQuery by remember { mutableStateOf("") }
    val entryItemYByCode = remember { mutableStateMapOf<Int, Float>() }
    val creatorLinkYByKey = remember { mutableStateMapOf<String, Float>() }
    var pendingSelectionAnchor by remember { mutableStateOf<SelectionAnchor?>(null) }
    val screenScope = rememberCoroutineScope()
    val rootListState = rememberLazyListState()
    val tagsListState = rememberLazyListState()
    val creatorsListState = rememberLazyListState()
    val searchEverythingShowingCount = vm.tags.size + vm.creators.size + vm.entries.size
    val tagFilterShowingEntriesCount = vm.entries.size
    val creatorsListMaxHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.72f)
        .coerceIn(320.dp, 720.dp)
    val isAnyListScrolling = {
        rootListState.isScrollInProgress ||
            tagsListState.isScrollInProgress ||
            creatorsListState.isScrollInProgress
    }

    fun stopActiveScrolls() {
        screenScope.launch {
            rootListState.stopScroll()
            tagsListState.stopScroll()
            creatorsListState.stopScroll()
        }
    }

    fun creatorLinkKey(creatorId: Long, code: Int): String {
        return "$creatorId:$code"
    }

    val entriesSectionIndex = if (showSettingsTab) 0 else 4
    val creatorsSectionIndex = if (showSettingsTab) {
        0
    } else {
        val entryItems = if (vm.entriesCardCollapsed) {
            0
        } else {
            val columns = vm.galleryColumns.coerceIn(1, 10)
            (vm.entries.size + columns - 1) / columns
        }
        entriesSectionIndex + entryItems
    }

    LaunchedEffect(vm.selectedCode, pendingSelectionAnchor) {
        val anchor = pendingSelectionAnchor ?: return@LaunchedEffect
        if (vm.selectedCode != anchor.code) {
            pendingSelectionAnchor = null
            return@LaunchedEffect
        }
        val deltaThresholdPx = 3f

        when (anchor.context) {
            SelectionAnchorContext.ENTRY -> {
                val newY = entryItemYByCode[anchor.code]
                if (newY == null) {
                    pendingSelectionAnchor = null
                    return@LaunchedEffect
                }
                val delta = newY - anchor.yInRoot
                if (abs(delta) > deltaThresholdPx) {
                    rootListState.scrollBy(delta)
                }
            }

            SelectionAnchorContext.CREATOR_LINK -> {
                val creatorId = anchor.creatorId ?: return@LaunchedEffect
                val key = creatorLinkKey(creatorId, anchor.code)
                val newY = creatorLinkYByKey[key]
                if (newY == null) {
                    pendingSelectionAnchor = null
                    return@LaunchedEffect
                }
                val delta = newY - anchor.yInRoot
                if (abs(delta) > deltaThresholdPx) {
                    creatorsListState.scrollBy(delta)
                }
            }
        }

        pendingSelectionAnchor = null
    }

    LaunchedEffect(
        vm.pendingEntryJumpCode,
        vm.entries,
        vm.entriesCardCollapsed,
        vm.galleryColumns,
        showSettingsTab
    ) {
        val targetCode = vm.pendingEntryJumpCode ?: return@LaunchedEffect
        val targetIndex = vm.entries.indexOfFirst { it.code == targetCode }
        if (targetIndex < 0) {
            vm.consumePendingEntryJump()
            return@LaunchedEffect
        }

        if (showSettingsTab) {
            showSettingsTab = false
            delay(60)
        }
        if (vm.entriesCardCollapsed) {
            vm.toggleEntriesCardCollapsed()
            delay(60)
        }

        val columns = vm.galleryColumns.coerceIn(1, 10)
        val targetRowIndex = (targetIndex / columns).coerceAtLeast(0)
        val totalItems = rootListState.layoutInfo.totalItemsCount
        if (totalItems > 0) {
            val targetRootIndex = (entriesSectionIndex + targetRowIndex).coerceIn(0, totalItems - 1)
            rootListState.scrollToItem(targetRootIndex)
        }
        vm.consumePendingEntryJump()
    }

    LaunchedEffect(vm.pendingCreatorJumpId, vm.creators, creatorsSectionIndex, showSettingsTab) {
        val targetId = vm.pendingCreatorJumpId ?: return@LaunchedEffect
        val targetIndex = vm.creators.indexOfFirst { it.id == targetId }
        if (targetIndex < 0) return@LaunchedEffect

        if (showSettingsTab) {
            showSettingsTab = false
            delay(60)
        }

        val totalItems = rootListState.layoutInfo.totalItemsCount
        if (totalItems > 0) {
            val targetRootIndex = creatorsSectionIndex.coerceIn(0, totalItems - 1)
            rootListState.scrollToItem(targetRootIndex)
        }
        creatorsListState.scrollToItem(targetIndex)
        vm.consumePendingCreatorJump()
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            vm.importFromUri(uri)
        }
    }

    val batchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            vm.onBatchFileChosen(uri)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val clearAfter = pendingClearAfterExport
        pendingClearAfterExport = false
        if (uri != null) {
            vm.exportToUri(uri, clearAfterExport = clearAfter)
        } else {
            vm.onExportCancelled(clearAfter)
        }
    }

    val backupFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val persisted = runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }.isSuccess
            vm.setAutoBackupFolder(uri)
            if (!persisted) {
                vm.setStatus("Folder picked, but persistent permission was not granted by Android. Backup may fail after app restarts.")
            }
        } else {
            vm.setStatus("Backup folder selection cancelled.")
        }
    }

    BackHandler(
        enabled = thumbnailPreview != null ||
            showRefetchAllPrompt ||
            showClearUnsafePrompt ||
            showClearAllPrompt ||
            showDeleteConfirm ||
            showEnableGalleryNeedsThumbnailsPrompt ||
            showDisableThumbnailsForGalleryPrompt ||
            showEntryLayoutDialog ||
            showBlockedTagsManager ||
            showSettingsTab ||
            vm.startupPreloadState != null ||
            vm.manualCreatorPromptState != null ||
            vm.batchCreatorPromptState != null ||
            vm.hasInAppBackAction()
    ) {
        when {
            thumbnailPreview != null -> {
                thumbnailPreview = null
            }
            vm.startupPreloadState != null -> {
                // Ignore back while launch preload is running.
            }
            vm.manualCreatorPromptState != null -> {
                vm.cancelManualCreatorPrompt()
            }
            vm.batchCreatorPromptState != null -> {
                vm.cancelBatchCreatorPrompt()
            }
            showRefetchAllPrompt -> {
                showRefetchAllPrompt = false
                vm.setStatus("Re-fetch all cancelled.")
            }
            showClearUnsafePrompt -> {
                showClearUnsafePrompt = false
                vm.setStatus("Clear all cancelled.")
            }
            showClearAllPrompt -> {
                showClearAllPrompt = false
                vm.setStatus("Clear all cancelled.")
            }
            showDeleteConfirm -> {
                showDeleteConfirm = false
            }
            showEnableGalleryNeedsThumbnailsPrompt -> {
                showEnableGalleryNeedsThumbnailsPrompt = false
                pendingEntryLayoutApplyMode = null
            }
            showDisableThumbnailsForGalleryPrompt -> {
                showDisableThumbnailsForGalleryPrompt = false
            }
            showEntryLayoutDialog -> {
                showEntryLayoutDialog = false
            }
            showBlockedTagsManager -> {
                showBlockedTagsManager = false
            }
            showSettingsTab -> {
                showSettingsTab = false
            }
            vm.hasInAppBackAction() -> {
                vm.handleInAppBackAction()
            }
        }
    }

    thumbnailPreview?.let { preview ->
        ThumbnailPreviewDialog(
            thumbnailUrl = preview.thumbnailUrl,
            contentDescription = preview.contentDescription,
            obscure = vm.incognitoModeEnabled,
            onDismiss = { thumbnailPreview = null }
        )
    }

    if (vm.errorDialogMessage != null) {
        AlertDialog(
            onDismissRequest = vm::dismissErrorDialog,
            title = { Text("Error") },
            text = { Text(vm.errorDialogMessage.orEmpty()) },
            confirmButton = {
                TextButton(onClick = vm::dismissErrorDialog) {
                    Text("OK")
                }
            }
        )
    }

    if (vm.infoDialogMessage != null) {
        AlertDialog(
            onDismissRequest = vm::dismissInfoDialog,
            title = { Text("Info") },
            text = { Text(vm.infoDialogMessage.orEmpty()) },
            confirmButton = {
                TextButton(onClick = vm::dismissInfoDialog) {
                    Text("OK")
                }
            }
        )
    }

    if (showEnableGalleryNeedsThumbnailsPrompt) {
        AlertDialog(
            onDismissRequest = { showEnableGalleryNeedsThumbnailsPrompt = false },
            title = { Text("Enable thumbnails first?") },
            text = {
                Text("To use Pure Gallery Mode you must turn on thumbnails. Do you wish to proceed?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEnableGalleryNeedsThumbnailsPrompt = false
                        if (!vm.showThumbnails) {
                            vm.toggleThumbnailsEnabled()
                        }
                        val pendingMode = pendingEntryLayoutApplyMode
                        if (pendingMode != null) {
                            vm.applyEntryLayout(
                                modeGallery = pendingMode,
                                columns = pendingEntryLayoutApplyColumns
                            )
                            pendingEntryLayoutApplyMode = null
                        } else if (!vm.pureGalleryMode) {
                            vm.togglePureGalleryMode()
                        }
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEnableGalleryNeedsThumbnailsPrompt = false
                        pendingEntryLayoutApplyMode = null
                    }
                ) {
                    Text("No")
                }
            }
        )
    }

    if (showDisableThumbnailsForGalleryPrompt) {
        AlertDialog(
            onDismissRequest = { showDisableThumbnailsForGalleryPrompt = false },
            title = { Text("Disable gallery mode too?") },
            text = {
                Text("Pure Gallery Mode requires thumbnails. Turning thumbnails off will also turn off Pure Gallery Mode. Do you want to proceed?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisableThumbnailsForGalleryPrompt = false
                        if (vm.pureGalleryMode) {
                            vm.togglePureGalleryMode()
                        }
                        if (vm.showThumbnails) {
                            vm.toggleThumbnailsEnabled()
                        }
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableThumbnailsForGalleryPrompt = false }) {
                    Text("No")
                }
            }
        )
    }

    if (showEntryLayoutDialog) {
        var draftModeGallery by remember(showEntryLayoutDialog) { mutableStateOf(vm.pureGalleryMode) }
        var draftColumnsFloat by remember(showEntryLayoutDialog) { mutableStateOf(vm.galleryColumns.toFloat()) }
        val draftColumns = draftColumnsFloat.roundToInt().coerceIn(1, 10)

        AlertDialog(
            onDismissRequest = { showEntryLayoutDialog = false },
            title = { Text("Set Entry Layout") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Mode",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !draftModeGallery,
                            onClick = { draftModeGallery = false },
                            label = { Text("Normal") }
                        )
                        FilterChip(
                            selected = draftModeGallery,
                            onClick = { draftModeGallery = true },
                            label = { Text("Gallery") }
                        )
                    }

                    Text(
                        text = "Preview (${if (draftModeGallery) "Gallery" else "Normal"})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    val previewEntries = vm.entryLayoutPreviewSamples
                    if (previewEntries.isEmpty()) {
                        Text(
                            text = "No saved entries to preview.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val clampedColumns = draftColumns.coerceAtLeast(1)
                        val shownEntries = previewEntries.take(minOf(previewEntries.size, clampedColumns))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            shownEntries.forEach { sample ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (draftModeGallery) {
                                        ThumbnailImage(
                                            thumbnailUrl = sample.thumbnailUrl,
                                            contentDescription = "Preview thumbnail ${sample.code}",
                                            obscure = vm.incognitoModeEnabled,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(74.dp)
                                        )
                                    } else {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = sample.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Code: ${sample.code}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                            if (shownEntries.size < clampedColumns) {
                                repeat(clampedColumns - shownEntries.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Text(
                        text = "Entries across: $draftColumns",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = draftColumnsFloat,
                        onValueChange = { draftColumnsFloat = it.coerceIn(1f, 10f) },
                        valueRange = 1f..10f
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (draftModeGallery && !vm.showThumbnails) {
                            pendingEntryLayoutApplyMode = true
                            pendingEntryLayoutApplyColumns = draftColumns
                            showEntryLayoutDialog = false
                            showEnableGalleryNeedsThumbnailsPrompt = true
                        } else {
                            vm.applyEntryLayout(
                                modeGallery = draftModeGallery,
                                columns = draftColumns
                            )
                            showEntryLayoutDialog = false
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEntryLayoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEnableAppLockDialog) {
        var pinInput by remember { mutableStateOf("") }
        var confirmInput by remember { mutableStateOf("") }
        var useBiometric by remember { mutableStateOf(true) }
        var validationMessage by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showEnableAppLockDialog = false },
            title = { Text("Enable App Lock") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it.filter { ch -> ch.isDigit() }.take(20) },
                        label = { Text("PIN (max 20 digits)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmInput,
                        onValueChange = { confirmInput = it.filter { ch -> ch.isDigit() }.take(20) },
                        label = { Text("Confirm PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Biometric unlock",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = { useBiometric = !useBiometric }) {
                            Text(if (useBiometric) "On" else "Off")
                        }
                    }
                    Text(
                        text = "Unlock opens immediately once the full correct PIN is entered.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    validationMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pinInput != confirmInput) {
                            validationMessage = "PIN values do not match."
                            return@TextButton
                        }
                        val error = vm.setOrChangeAppLockPin(pinInput, enableIfDisabled = true)
                        if (error != null) {
                            validationMessage = error
                            return@TextButton
                        }
                        vm.chooseAppLockBiometricEnabled(useBiometric)
                        showEnableAppLockDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEnableAppLockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showChangePinDialog) {
        var pinInput by remember { mutableStateOf("") }
        var confirmInput by remember { mutableStateOf("") }
        var validationMessage by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Change App Lock PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it.filter { ch -> ch.isDigit() }.take(20) },
                        label = { Text("New PIN (max 20 digits)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmInput,
                        onValueChange = { confirmInput = it.filter { ch -> ch.isDigit() }.take(20) },
                        label = { Text("Confirm new PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    validationMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pinInput != confirmInput) {
                            validationMessage = "PIN values do not match."
                            return@TextButton
                        }
                        val error = vm.setOrChangeAppLockPin(pinInput, enableIfDisabled = false)
                        if (error != null) {
                            validationMessage = error
                            return@TextButton
                        }
                        showChangePinDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDisableAppLockPrompt) {
        AlertDialog(
            onDismissRequest = { showDisableAppLockPrompt = false },
            title = { Text("Disable App Lock?") },
            text = {
                Text("This removes PIN and biometric protection until you set it up again.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.disableAppLock()
                        showDisableAppLockPrompt = false
                    }
                ) {
                    Text("Disable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableAppLockPrompt = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    vm.browserRatingPromptState?.let { prompt ->
        AlertDialog(
            onDismissRequest = vm::skipBrowserRatingPrompt,
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
                            val starInteraction = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable(
                                        interactionSource = starInteraction,
                                        indication = null
                                    ) { vm.updateBrowserRatingSelection(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (filled) "★" else "☆",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (filled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = vm::saveBrowserRatingPrompt) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = vm::skipBrowserRatingPrompt) {
                    Text("Skip")
                }
            }
        )
    }

    vm.splitPromptState?.let { prompt ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Split numbers detected") },
            text = {
                Text(
                    "Found ${prompt.count} split number groups.\n" +
                        "Examples: ${prompt.preview}\n\n" +
                        "Do you want to combine them into contiguous codes?\n" +
                        "Yes = combine (e.g. '00 00 0' -> '00000')\n" +
                        "No = skip split groups"
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.onSplitPromptAnswered(true) }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.onSplitPromptAnswered(false) }) {
                    Text("No")
                }
            }
        )
    }

    vm.shortPromptState?.let { prompt ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Short codes detected") },
            text = {
                Text(
                    "Found ${prompt.count} codes with fewer than 5 digits.\n" +
                        "Examples: ${prompt.preview}\n\n" +
                        "Skip these short codes?"
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.onShortPromptAnswered(true) }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.onShortPromptAnswered(false) }) {
                    Text("No")
                }
            }
        )
    }

    vm.manualCreatorPromptState?.let { prompt ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Two-word creator name") },
            text = {
                Text(
                    "Input '${prompt.phrase}' has two words.\n\n" +
                        "Treat this as:\n" +
                        "One entry: '${toHyphenatedTwoWordCreatorName(prompt.phrase)}'\n" +
                        "Two entries: '${splitTwoWordCreatorName(prompt.phrase).joinToString("', '")}'"
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.onManualCreatorPromptAnswered(true) }) {
                    Text("One entry")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { vm.onManualCreatorPromptAnswered(false) }) {
                        Text("Two entries")
                    }
                    TextButton(onClick = vm::cancelManualCreatorPrompt) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    vm.batchCreatorPromptState?.let { prompt ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Two-word names in batch") },
            text = {
                Text(
                    "Found ${prompt.count} two-word creator line(s).\n" +
                        "Examples: ${prompt.preview}\n\n" +
                        "Should these be treated as one entry per line (hyphenated) or two separate entries?"
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.onBatchCreatorPromptAnswered(true) }) {
                    Text("One entry")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { vm.onBatchCreatorPromptAnswered(false) }) {
                        Text("Two entries")
                    }
                    TextButton(onClick = vm::cancelBatchCreatorPrompt) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    vm.batchProgressState?.let { state ->
        val progress = if (state.total > 0) {
            state.processed.toFloat() / state.total.toFloat()
        } else {
            0f
        }

        AlertDialog(
            onDismissRequest = {},
            title = { Text(vm.batchDialogTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (state.currentCode != null) {
                            "Fetching code ${state.currentCode}..."
                        } else {
                            "Starting..."
                        }
                    )
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text("${state.processed} / ${state.total}")
                    Text("Saved: ${state.saved}  |  Not found: ${state.notFound}  |  Failed: ${state.failed}")
                }
            },
            confirmButton = {
                TextButton(onClick = vm::cancelBatch) {
                    Text("Cancel")
                }
            }
        )
    }

    vm.startupPreloadState?.takeIf { !vm.appLocked }?.let { state ->
        val progress = if (state.totalSteps > 0) {
            val base = state.completedSteps.toFloat()
            val thumbProgress = if (state.thumbsTotal > 0) {
                state.thumbsDone.toFloat() / state.thumbsTotal.toFloat()
            } else {
                0f
            }
            ((base + thumbProgress) / state.totalSteps.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Loading data...") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.phase)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text("${state.completedSteps} / ${state.totalSteps} steps")
                    if (state.thumbsTotal > 0) {
                        Text("Thumbnails: ${state.thumbsDone} / ${state.thumbsTotal}")
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showRefetchAllPrompt) {
        AlertDialog(
            onDismissRequest = {
                showRefetchAllPrompt = false
                vm.setStatus("Re-fetch all cancelled.")
            },
            title = { Text("Re-fetch all entries") },
            text = {
                Text(
                    "This will re-fetch every saved entry from the network and update local metadata.\n\n" +
                        "It can take a while and may feel laggy during the batch, especially with many entries.\n\n" +
                        "Proceed?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRefetchAllPrompt = false
                        vm.refetchAllEntries()
                    }
                ) {
                    Text("Start")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRefetchAllPrompt = false
                        vm.setStatus("Re-fetch all cancelled.")
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    vm.pinTogglePromptState?.let { prompt ->
        AlertDialog(
            onDismissRequest = vm::dismissPinTogglePrompt,
            title = {
                Text(
                    if (prompt.targetPinned) {
                        "Pin entry"
                    } else {
                        "Unpin entry"
                    }
                )
            },
            text = {
                Text(
                    if (prompt.targetPinned) {
                        "Pin code ${prompt.code}? Pinned entries stay at the top of the list."
                    } else {
                        "Unpin code ${prompt.code}? It will return to normal list ordering."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = vm::confirmPinToggle) {
                    Text(if (prompt.targetPinned) "Pin" else "Unpin")
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissPinTogglePrompt) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete entry") },
            text = {
                Text(
                    vm.selectedCode?.let { "Delete code $it and its tag links from your local database?" }
                        ?: "Select an entry first."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        vm.deleteSelected()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearAllPrompt) {
        AlertDialog(
            onDismissRequest = {
                showClearAllPrompt = false
                vm.setStatus("Clear all cancelled.")
            },
            title = { Text("Clear all entries") },
            text = {
                Text(
                    "Do you wish to export before you clear all entries?\n\n" +
                        "Yes: Export metadata, then clear all entries.\n" +
                        "No: Continue without exporting.\n" +
                        "Cancel: Keep everything unchanged."
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showClearAllPrompt = false
                            pendingClearAfterExport = true
                            exportLauncher.launch(vm.defaultExportFilename())
                        }
                    ) {
                        Text("Yes")
                    }
                    TextButton(
                        onClick = {
                            showClearAllPrompt = false
                            vm.setStatus("Clear all cancelled.")
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearAllPrompt = false
                        showClearUnsafePrompt = true
                    }
                ) {
                    Text("No")
                }
            }
        )
    }

    if (showClearUnsafePrompt) {
        AlertDialog(
            onDismissRequest = {
                showClearUnsafePrompt = false
                vm.setStatus("Clear all cancelled.")
            },
            title = { Text("Proceed without export?") },
            text = {
                Text(
                    "Clearing without saving can result in unintended loss of data.\n\n" +
                        "Do you wish to proceed?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearUnsafePrompt = false
                        vm.clearAllWithoutExport()
                    }
                ) {
                    Text("Proceed")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearUnsafePrompt = false
                        vm.setStatus("Clear all cancelled.")
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.background,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground
            ),
            title = {
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .pointerInput(showSettingsTab, vm.incognitoModeEnabled) {
                            detectTapGestures(
                                onPress = {
                                    screenScope.launch {
                                        rootListState.scrollToItem(0)
                                        tagsListState.scrollToItem(0)
                                        creatorsListState.scrollToItem(0)
                                    }
                                    tryAwaitRelease()
                                },
                                onDoubleTap = {
                                    vm.toggleIncognitoMode()
                                }
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = if (showSettingsTab) "Settings" else APP_TITLE)
                }
            },
            navigationIcon = {
                ThemeToggleWithAccentPicker(
                    themeMode = vm.themeMode,
                    accentMode = vm.accentMode,
                    incognitoModeEnabled = vm.incognitoModeEnabled,
                    onCycleThemeMode = vm::cycleThemeMode,
                    onAccentModeSelected = vm::chooseAccentMode,
                    modifier = Modifier.padding(start = 8.dp)
                )
            },
            actions = {
                IconButton(
                    onClick = {
                        val next = !showSettingsTab
                        showSettingsTab = next
                        if (next) {
                            settingsDataExpanded = false
                            settingsDisplayExpanded = false
                            settingsSecurityExpanded = false
                            settingsStatsExpanded = false
                            showBlockedTagsManager = false
                        }
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "⚙",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = rootListState,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showSettingsTab) {
                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Data",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                ImmediateActionText(
                                    label = if (settingsDataExpanded) "Collapse" else "Expand",
                                    onAction = { settingsDataExpanded = !settingsDataExpanded },
                                    textStyle = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (settingsDataExpanded) {
                            Button(
                                onClick = {
                                    importLauncher.launch(arrayOf("text/plain", "text/*"))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Import Data")
                            }
                            Button(
                                onClick = {
                                    pendingClearAfterExport = false
                                    exportLauncher.launch(vm.defaultExportFilename())
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Export Data")
                            }
                            Button(
                                onClick = { backupFolderLauncher.launch(null) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Set Procedural Backup Folder")
                            }
                            Button(
                                onClick = vm::backupNow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Backup Now")
                            }
                            Text(
                                text = "Backup folder: ${vm.autoBackupFolderLabel()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "On app exit, $PROCEDURAL_BACKUP_FILENAME is updated in this folder.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { showClearAllPrompt = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Clear All Entries")
                            }
                            Button(
                                onClick = { showRefetchAllPrompt = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Re-fetch All Entries")
                            }
                            }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                                thickness = 1.dp
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Display",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                ImmediateActionText(
                                    label = if (settingsDisplayExpanded) "Collapse" else "Expand",
                                    onAction = { settingsDisplayExpanded = !settingsDisplayExpanded },
                                    textStyle = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (settingsDisplayExpanded) {
                            Button(
                                onClick = {
                                    if (vm.showThumbnails && vm.pureGalleryMode) {
                                        showDisableThumbnailsForGalleryPrompt = true
                                    } else {
                                        vm.toggleThumbnailsEnabled()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (vm.showThumbnails) {
                                        "Thumbnails: On"
                                    } else {
                                        "Thumbnails: Off"
                                    }
                                )
                            }
                            Button(
                                onClick = {
                                    vm.loadEntryLayoutPreviewSamples()
                                    showEntryLayoutDialog = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Set Entry Layout"
                                )
                            }
                            Text(
                                text = if (vm.pureGalleryMode) {
                                    "Current layout: Gallery (${vm.galleryColumns} across)"
                                } else {
                                    "Current layout: Normal (${vm.galleryColumns} across)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    showBlockedTagsManager = !showBlockedTagsManager
                                    if (showBlockedTagsManager) {
                                        vm.ensurePopularTagsLoaded(fetchIfEmpty = true)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (showBlockedTagsManager) "Hide Block Tags" else "Block Tags")
                            }
                            if (showBlockedTagsManager) {
                                val blockedTagsSearchTrimmed = blockedTagsSearchQuery.trim()
                                val blockedTagsSearchTokens = blockedTagsSearchTrimmed
                                    .lowercase(Locale.US)
                                    .split(Regex("\\s+"))
                                    .filter { it.isNotBlank() }
                                val filteredPopularTags = if (blockedTagsSearchTokens.isEmpty()) {
                                    vm.popularTags
                                } else {
                                    vm.popularTags.filter { tag ->
                                        val haystack = "${tag.name} ${tag.type}".lowercase(Locale.US)
                                        blockedTagsSearchTokens.all { token -> haystack.contains(token) }
                                    }
                                }

                                Button(
                                    onClick = vm::fetchAllPopularTags,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !vm.popularTagsFetchInProgress
                                ) {
                                    Text(
                                        if (vm.popularTagsFetchInProgress) {
                                            "Fetching Popular Tags..."
                                        } else {
                                            "Fetch All Tags"
                                        }
                                    )
                                }
                                Button(
                                    onClick = vm::toggleApplyBlockedTagsToHome,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (vm.applyBlockedTagsToHome) {
                                            "Apply Blocked Tags To Home: On"
                                        } else {
                                            "Apply Blocked Tags To Home: Off"
                                        }
                                    )
                                }
                                Button(
                                    onClick = vm::toggleApplyBlockedTagsToSearchTerms,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (vm.applyBlockedTagsToSearchTerms) {
                                            "Apply Blocked Tags To Search Terms: On"
                                        } else {
                                            "Apply Blocked Tags To Search Terms: Off"
                                        }
                                    )
                                }
                                Text(
                                    text = "Blocked tags: ${vm.blockedTagsSummary()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                                OutlinedTextField(
                                    value = blockedTagsSearchQuery,
                                    onValueChange = { blockedTagsSearchQuery = it },
                                    label = { Text("Search blocked tags") },
                                    placeholder = { Text("Search is empty") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item {
                                        EntrySortChip(
                                            label = "Name${blockedTagSortArrow(vm, TagSortField.NAME)}",
                                            selected = vm.blockedTagSortField == TagSortField.NAME,
                                            activeDirection = if (vm.blockedTagSortField == TagSortField.NAME) vm.blockedTagSortDirection else null,
                                            onClick = { vm.onBlockedTagSortClicked(TagSortField.NAME) }
                                        )
                                    }
                                    item {
                                        EntrySortChip(
                                            label = "Type${blockedTagSortArrow(vm, TagSortField.TYPE)}",
                                            selected = vm.blockedTagSortField == TagSortField.TYPE,
                                            activeDirection = if (vm.blockedTagSortField == TagSortField.TYPE) vm.blockedTagSortDirection else null,
                                            onClick = { vm.onBlockedTagSortClicked(TagSortField.TYPE) }
                                        )
                                    }
                                    item {
                                        EntrySortChip(
                                            label = "Count${blockedTagSortArrow(vm, TagSortField.COUNT)}",
                                            selected = vm.blockedTagSortField == TagSortField.COUNT,
                                            activeDirection = if (vm.blockedTagSortField == TagSortField.COUNT) vm.blockedTagSortDirection else null,
                                            onClick = { vm.onBlockedTagSortClicked(TagSortField.COUNT) }
                                        )
                                    }
                                    item {
                                        ImmediateActionText(
                                            label = "Reset Filter",
                                            onAction = vm::clearBlockedTags,
                                            textStyle = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                if (vm.popularTags.isEmpty()) {
                                    Text(
                                        text = "No cached popular tags yet. Tap Fetch All Tags.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else if (filteredPopularTags.isEmpty()) {
                                    Text(
                                        text = "No tags match your blocked-tag search.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 180.dp, max = 340.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(
                                            filteredPopularTags,
                                            key = { it.id },
                                            contentType = { "blocked_tag_row" }
                                        ) { tag ->
                                            val rowInteraction = remember { MutableInteractionSource() }
                                            val containerColor = if (tag.blocked) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(containerColor, shape = MaterialTheme.shapes.small)
                                                    .clickable(
                                                        interactionSource = rowInteraction,
                                                        indication = null
                                                    ) { vm.togglePopularTagBlocked(tag.id) }
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = tag.name,
                                                    modifier = Modifier.weight(0.50f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = tag.type,
                                                    modifier = Modifier.weight(0.26f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = tag.count.toString(),
                                                    modifier = Modifier.weight(0.24f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (tag.blocked) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Button(
                                onClick = vm::togglePreloadOnLaunch,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (vm.preloadOnLaunch) {
                                        "Load Data On Launch: On"
                                    } else {
                                        "Load Data On Launch: Off"
                                    }
                                )
                            }
                            Text(
                                text = "Launch preload amount: ${vm.preloadPercent}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = vm.preloadPercent.toFloat(),
                                onValueChange = { vm.updatePreloadPercent(it.roundToInt()) },
                                valueRange = 0f..100f
                            )
                            Text(
                                text = "Turn thumbnails off for smoother scrolling on slower phones.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "When enabled, app startup shows a loading dialog and preloads data (and thumbnails if enabled) for smoother browsing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                                thickness = 1.dp
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Security",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                ImmediateActionText(
                                    label = if (settingsSecurityExpanded) "Collapse" else "Expand",
                                    onAction = { settingsSecurityExpanded = !settingsSecurityExpanded },
                                    textStyle = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (settingsSecurityExpanded) {
                            Button(
                                onClick = {
                                    if (vm.appLockEnabled) {
                                        showDisableAppLockPrompt = true
                                    } else {
                                        showEnableAppLockDialog = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (vm.appLockEnabled) "App Lock: On" else "App Lock: Off")
                            }
                            if (vm.appLockEnabled) {
                                Button(
                                    onClick = { showChangePinDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Change PIN")
                                }
                                Button(
                                    onClick = {
                                        vm.chooseAppLockBiometricEnabled(!vm.appLockBiometricEnabled)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (vm.appLockBiometricEnabled) {
                                            "Biometric Unlock: On"
                                        } else {
                                            "Biometric Unlock: Off"
                                        }
                                    )
                                }
                                Text(
                                    text = "App lock appears on app open. After background/close, PIN is required again after 30 seconds.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                                thickness = 1.dp
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Stats",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                ImmediateActionText(
                                    label = if (settingsStatsExpanded) "Collapse" else "Expand",
                                    onAction = {
                                        settingsStatsExpanded = !settingsStatsExpanded
                                        if (settingsStatsExpanded) {
                                            vm.ensureReadAnalyticsLoaded(forceRefresh = false)
                                        }
                                    },
                                    textStyle = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (settingsStatsExpanded) {
                            val readCount = vm.readAnalytics.readCounts[selectedStatsRange] ?: 0
                            val pagesRead = vm.readAnalytics.pagesRead[selectedStatsRange] ?: 0
                            val averageRating = vm.readAnalytics.averageRatings[selectedStatsRange] ?: 0f
                            val topTags = vm.readAnalytics.topTags[selectedStatsRange].orEmpty()
                            val topCreators = vm.readAnalytics.topCreators[selectedStatsRange].orEmpty()
                            val coverage = if (vm.savedStats.entries > 0) {
                                (readCount * 100f / vm.savedStats.entries.toFloat()).coerceIn(0f, 100f)
                            } else {
                                0f
                            }
                            val statsObscured = vm.incognitoModeEnabled
                            val statsPrivacyOverlay = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)

                            if (vm.readAnalyticsLoading) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(StatsRange.entries, key = { it.name }) { range ->
                                        FilterChip(
                                            selected = selectedStatsRange == range,
                                            onClick = { selectedStatsRange = range },
                                            label = { Text(range.label) }
                                        )
                                    }
                                }
                                Text(
                                    text = "Read (${selectedStatsRange.label}): $readCount",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.privacyObfuscate(
                                        enabled = statsObscured,
                                        overlayColor = statsPrivacyOverlay
                                    )
                                )
                                Text(
                                    text = "Pages read (${selectedStatsRange.label}): $pagesRead",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.privacyObfuscate(
                                        enabled = statsObscured,
                                        overlayColor = statsPrivacyOverlay
                                    )
                                )
                                Text(
                                    text = "Read coverage (all entries): ${String.format(Locale.US, "%.1f", coverage)}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.privacyObfuscate(
                                        enabled = statsObscured,
                                        overlayColor = statsPrivacyOverlay
                                    )
                                )
                                Text(
                                    text = "Avg rating on read entries: ${
                                        if (averageRating > 0f) {
                                            String.format(Locale.US, "%.2f/5", averageRating)
                                        } else {
                                            "-"
                                        }
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.privacyObfuscate(
                                        enabled = statsObscured,
                                        overlayColor = statsPrivacyOverlay
                                    )
                                )
                                Text(
                                    text = "Top read tags (${selectedStatsRange.label}):",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.privacyObfuscate(
                                        enabled = statsObscured,
                                        overlayColor = statsPrivacyOverlay
                                    )
                                )
                                if (topTags.isEmpty()) {
                                    Text(
                                        text = "No read-tag data yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.privacyObfuscate(
                                            enabled = statsObscured,
                                            overlayColor = statsPrivacyOverlay
                                        )
                                    )
                                } else {
                                    topTags.forEachIndexed { index, row ->
                                        Text(
                                            text = "${index + 1}. ${row.name} (${row.type}) - ${row.count}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.privacyObfuscate(
                                                enabled = statsObscured,
                                                overlayColor = statsPrivacyOverlay
                                            )
                                        )
                                    }
                                }
                                Text(
                                    text = "Top read artists/groups (${selectedStatsRange.label}):",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.privacyObfuscate(
                                        enabled = statsObscured,
                                        overlayColor = statsPrivacyOverlay
                                    )
                                )
                                if (topCreators.isEmpty()) {
                                    Text(
                                        text = "No read creator data yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.privacyObfuscate(
                                            enabled = statsObscured,
                                            overlayColor = statsPrivacyOverlay
                                        )
                                    )
                                } else {
                                    topCreators.forEachIndexed { index, row ->
                                        Text(
                                            text = "${index + 1}. ${row.name} (${row.type}) - ${row.count}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.privacyObfuscate(
                                                enabled = statsObscured,
                                                overlayColor = statsPrivacyOverlay
                                            )
                                        )
                                    }
                                }
                            }
                            Button(
                                onClick = { vm.ensureReadAnalyticsLoaded(forceRefresh = true) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Refresh Stats")
                            }
                            }
                            }
                            Text(
                                text = vm.statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Saved totals: ${vm.savedStats.entries} entries, ${vm.savedStats.artists} artists, ${vm.savedStats.groups} groups, ${vm.savedStats.readEntries} read",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.privacyObfuscate(
                                    enabled = vm.incognitoModeEnabled,
                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                )
                            )
                        }
                    }
                }
            } else {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            val searchEmptyDisplay = if (vm.incognitoModeEnabled) {
                                "••••••••••"
                            } else {
                                "Search is empty"
                            }
                            val displayedSearchValue = if (vm.codeInput.isNotBlank() || searchFieldFocused) {
                                vm.codeInput
                            } else {
                                searchEmptyDisplay
                            }

                            OutlinedTextField(
                                value = displayedSearchValue,
                                onValueChange = vm::updateUnifiedInput,
                                label = { Text("Search everything") },
                                singleLine = false,
                                minLines = 1,
                                maxLines = 4,
                                visualTransformation = if (vm.incognitoModeEnabled && (vm.codeInput.isNotBlank() || searchFieldFocused)) {
                                    PasswordVisualTransformation()
                                } else {
                                    VisualTransformation.None
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Search
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = { vm.openUnifiedInputInBrowser() }
                                ),
                                trailingIcon = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        if (vm.codeInput.trim().isNotEmpty()) {
                                            Text(
                                                text = "Showing:$searchEverythingShowingCount",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(onClick = vm::pasteCodeInputFromClipboard) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_content_paste_24),
                                                contentDescription = "Paste from clipboard",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = vm::openUnifiedInputInBrowser
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_open_in_new_24),
                                                contentDescription = "Open current input/tags in browser",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(
                                    topStart = 14.dp,
                                    topEnd = 14.dp,
                                    bottomStart = 0.dp,
                                    bottomEnd = 0.dp
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        searchFieldFocused = focusState.isFocused
                                    }
                            )
                            OutlinedTextField(
                                value = vm.activeFilterLabel(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Tag filter") },
                                singleLine = false,
                                minLines = 1,
                                maxLines = 4,
                                visualTransformation = if (vm.incognitoModeEnabled) {
                                    PasswordVisualTransformation()
                                } else {
                                    VisualTransformation.None
                                },
                                trailingIcon = if (vm.activeTagFilterIds.isNotEmpty()) {
                                    {
                                        Text(
                                            text = "Showing:$tagFilterShowingEntriesCount",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(end = 10.dp)
                                        )
                                    }
                                } else {
                                    null
                                },
                                shape = RoundedCornerShape(
                                    topStart = 0.dp,
                                    topEnd = 0.dp,
                                    bottomStart = 14.dp,
                                    bottomEnd = 14.dp
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = vm::addOrUpdateByInput,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Add / Update")
                            }
                            Button(
                                onClick = { batchLauncher.launch(arrayOf("text/plain", "text/*")) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Batch Add/Update")
                            }
                        }

                        Text(
                            text = vm.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Card {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Tags",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ImmediateActionText(
                                    label = "Reset Search",
                                    onAction = vm::clearEntrySearch,
                                    onPressStart = ::stopActiveScrolls,
                                    runOnPressWhen = {
                                        rootListState.isScrollInProgress ||
                                            tagsListState.isScrollInProgress ||
                                            creatorsListState.isScrollInProgress
                                    },
                                    textStyle = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                ImmediateActionText(
                                    label = "Reset Filter",
                                    onAction = vm::clearTagFilter,
                                    onPressStart = ::stopActiveScrolls,
                                    runOnPressWhen = {
                                        rootListState.isScrollInProgress ||
                                            tagsListState.isScrollInProgress ||
                                            creatorsListState.isScrollInProgress
                                    },
                                    textStyle = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                ImmediateActionText(
                                    label = if (vm.tagsCardCollapsed) "Expand" else "Collapse",
                                    onAction = vm::toggleTagsCardCollapsed,
                                    onPressStart = ::stopActiveScrolls,
                                    runOnPressWhen = {
                                        rootListState.isScrollInProgress ||
                                            tagsListState.isScrollInProgress ||
                                            creatorsListState.isScrollInProgress
                                    },
                                    textStyle = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (!vm.tagsCardCollapsed) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    EntrySortChip(
                                        label = "Tag${tagSortArrow(vm, TagSortField.NAME)}",
                                        selected = vm.tagSortField == TagSortField.NAME,
                                        activeDirection = if (vm.tagSortField == TagSortField.NAME) vm.tagSortDirection else null,
                                        onClick = { vm.onTagSortClicked(TagSortField.NAME) },
                                        onPressStart = ::stopActiveScrolls,
                                        runOnPressWhen = isAnyListScrolling
                                    )
                                }
                                item {
                                    EntrySortChip(
                                        label = "Type${tagSortArrow(vm, TagSortField.TYPE)}",
                                        selected = vm.tagSortField == TagSortField.TYPE,
                                        activeDirection = if (vm.tagSortField == TagSortField.TYPE) vm.tagSortDirection else null,
                                        onClick = { vm.onTagSortClicked(TagSortField.TYPE) },
                                        onPressStart = ::stopActiveScrolls,
                                        runOnPressWhen = isAnyListScrolling
                                    )
                                }
                                item {
                                    EntrySortChip(
                                        label = "Count${tagSortArrow(vm, TagSortField.COUNT)}",
                                        selected = vm.tagSortField == TagSortField.COUNT,
                                        activeDirection = if (vm.tagSortField == TagSortField.COUNT) vm.tagSortDirection else null,
                                        onClick = { vm.onTagSortClicked(TagSortField.COUNT) },
                                        onPressStart = ::stopActiveScrolls,
                                        runOnPressWhen = isAnyListScrolling
                                    )
                                }
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 260.dp),
                                state = tagsListState,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(
                                    vm.tags,
                                    key = { it.id },
                                    contentType = { "tag_row" }
                                ) { tag ->
                                    val selected = vm.activeTagFilterIds.contains(tag.id)
                                    val tagInteraction = remember { MutableInteractionSource() }
                                    val containerColor = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(containerColor, shape = MaterialTheme.shapes.small)
                                            .clickable(
                                                enabled = !vm.incognitoModeEnabled,
                                                interactionSource = tagInteraction,
                                                indication = null
                                            ) { vm.toggleTagFilter(tag.id) }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tag.name,
                                            modifier = Modifier
                                                .weight(0.48f)
                                                .privacyObfuscate(
                                                    enabled = vm.incognitoModeEnabled,
                                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = tag.type,
                                            modifier = Modifier
                                                .weight(0.30f)
                                                .privacyObfuscate(
                                                    enabled = vm.incognitoModeEnabled,
                                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = tag.count.toString(),
                                            modifier = Modifier
                                                .weight(0.22f)
                                                .privacyObfuscate(
                                                    enabled = vm.incognitoModeEnabled,
                                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Entries",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ImmediateActionText(
                                    label = vm.entryReadFilterLabel(),
                                    onAction = vm::cycleEntryReadFilter,
                                    onPressStart = ::stopActiveScrolls,
                                    runOnPressWhen = {
                                        rootListState.isScrollInProgress ||
                                            tagsListState.isScrollInProgress ||
                                            creatorsListState.isScrollInProgress
                                    },
                                    textStyle = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                ImmediateActionText(
                                    label = if (vm.entriesCardCollapsed) "Expand" else "Collapse",
                                    onAction = vm::toggleEntriesCardCollapsed,
                                    onPressStart = ::stopActiveScrolls,
                                    runOnPressWhen = {
                                        rootListState.isScrollInProgress ||
                                            tagsListState.isScrollInProgress ||
                                            creatorsListState.isScrollInProgress
                                    },
                                    textStyle = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (!vm.entriesCardCollapsed) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    EntrySortChip(
                                        label = "Rating",
                                        selected = vm.sortField == EntrySortField.RATING,
                                        activeDirection = if (vm.sortField == EntrySortField.RATING) SortDirection.DESC else null,
                                        onClick = vm::toggleRatingSort,
                                        onPressStart = ::stopActiveScrolls,
                                        runOnPressWhen = isAnyListScrolling
                                    )
                                }
                                item {
                                    EntrySortChip(
                                        label = "Title${entrySortArrow(vm, EntrySortField.TITLE)}",
                                        selected = vm.sortField == EntrySortField.TITLE,
                                        activeDirection = if (vm.sortField == EntrySortField.TITLE) vm.sortDirection else null,
                                        onClick = { vm.onEntrySortClicked(EntrySortField.TITLE) },
                                        onPressStart = ::stopActiveScrolls,
                                        runOnPressWhen = isAnyListScrolling
                                    )
                                }
                                item {
                                    EntrySortChip(
                                        label = "Pages${entrySortArrow(vm, EntrySortField.PAGES)}",
                                        selected = vm.sortField == EntrySortField.PAGES,
                                        activeDirection = if (vm.sortField == EntrySortField.PAGES) vm.sortDirection else null,
                                        onClick = { vm.onEntrySortClicked(EntrySortField.PAGES) },
                                        onPressStart = ::stopActiveScrolls,
                                        runOnPressWhen = isAnyListScrolling
                                    )
                                }
                                item {
                                    EntrySortChip(
                                        label = "Uploaded${entrySortArrow(vm, EntrySortField.UPLOAD)}",
                                        selected = vm.sortField == EntrySortField.UPLOAD,
                                        activeDirection = if (vm.sortField == EntrySortField.UPLOAD) vm.sortDirection else null,
                                        onClick = { vm.onEntrySortClicked(EntrySortField.UPLOAD) },
                                        onPressStart = ::stopActiveScrolls,
                                        runOnPressWhen = isAnyListScrolling
                                    )
                                }
                                item {
                                    EntrySortChip(
                                        label = "Fetched${entrySortArrow(vm, EntrySortField.ADDED)}",
                                        selected = vm.sortField == EntrySortField.ADDED,
                                        activeDirection = if (vm.sortField == EntrySortField.ADDED) vm.sortDirection else null,
                                        onClick = { vm.onEntrySortClicked(EntrySortField.ADDED) },
                                        onPressStart = ::stopActiveScrolls,
                                        runOnPressWhen = isAnyListScrolling
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!vm.entriesCardCollapsed) {
                if (vm.pureGalleryMode) {
                    val galleryColumns = vm.galleryColumns.coerceIn(1, 10)
                    val galleryRows = vm.entries.chunked(galleryColumns)
                    items(
                        galleryRows,
                        key = { row -> row.joinToString("_") { it.code.toString() } },
                        contentType = { "entry_gallery_row" }
                    ) { rowEntries ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowEntries.forEach { entry ->
                                    val selected = vm.selectedCode == entry.code
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHigh
                                            }
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                        border = if (selected) {
                                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                        } else {
                                            null
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .onGloballyPositioned { coordinates ->
                                                entryItemYByCode[entry.code] = coordinates.positionInRoot().y
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(196.dp)
                                            ) {
                                                ThumbnailImage(
                                                    thumbnailUrl = entry.thumbnailUrl,
                                                    contentDescription = "Cover for code ${entry.code}",
                                                    obscure = vm.incognitoModeEnabled,
                                                    onClick = {
                                                        val y = entryItemYByCode[entry.code]
                                                        if (y != null) {
                                                            pendingSelectionAnchor = SelectionAnchor(
                                                                context = SelectionAnchorContext.ENTRY,
                                                                code = entry.code,
                                                                yInRoot = y
                                                            )
                                                        } else {
                                                            pendingSelectionAnchor = null
                                                        }
                                                        vm.onEntryCardClicked(entry.code)
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                IconButton(
                                                    onClick = { vm.requestToggleEntryPinned(entry.code) },
                                                    enabled = !vm.incognitoModeEnabled,
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .size(32.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_push_pin_24),
                                                        contentDescription = if (entry.pinned) {
                                                            "Unpin entry ${entry.code}"
                                                        } else {
                                                            "Pin entry ${entry.code}"
                                                        },
                                                        tint = if (vm.incognitoModeEnabled) {
                                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                                        } else if (entry.pinned) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        },
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Code: ${entry.code}",
                                                modifier = Modifier.privacyObfuscate(
                                                    enabled = vm.incognitoModeEnabled,
                                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = entry.title,
                                                modifier = Modifier.privacyObfuscate(
                                                    enabled = vm.incognitoModeEnabled,
                                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                ),
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Rating: ${renderStars(entry.rating)} (${entry.rating}/5)",
                                                modifier = Modifier.privacyObfuscate(
                                                    enabled = vm.incognitoModeEnabled,
                                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                if (rowEntries.size < galleryColumns) {
                                    repeat(galleryColumns - rowEntries.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }

                            val selectedInRow = rowEntries.firstOrNull { it.code == vm.selectedCode }
                            if (selectedInRow != null) {
                                val detail = vm.selectedDetail?.takeIf { it.code == selectedInRow.code }
                                SelectedEntryDetailCard(
                                    detail = detail,
                                    onOpenInBrowser = vm::openSelectedInBrowser,
                                    onOpenCreatorFromDetail = vm::openCreatorFromDetail,
                                    onCopyCode = vm::copyCodeToClipboard,
                                    onToggleReadStatus = vm::toggleEntryRead,
                                    onSetRating = vm::setEntryRating,
                                    onResetRating = { code -> vm.setEntryRating(code, 0) },
                                    onRefetch = vm::refetchCode,
                                    onDelete = { code ->
                                        if (vm.selectedCode != code) {
                                            vm.selectEntry(code)
                                        }
                                        showDeleteConfirm = true
                                    },
                                    seriesNeighbors = vm.selectedSeriesNeighbors,
                                    onOpenSeriesEntry = vm::openSeriesEntry,
                                    onCopyCreatorName = vm::copyCreatorNameToClipboard,
                                    onSelectedThumbnailClick = { url, description ->
                                        thumbnailPreview = ThumbnailPreviewState(
                                            thumbnailUrl = url,
                                            contentDescription = description
                                        )
                                    },
                                    showThumbnails = vm.showThumbnails,
                                    incognitoModeEnabled = vm.incognitoModeEnabled
                                )
                            }
                        }
                    }
                } else {
                val normalColumns = vm.galleryColumns.coerceIn(1, 10)
                if (normalColumns <= 1) {
                items(
                    vm.entries,
                    key = { it.code },
                    contentType = { "entry_row" }
                ) { entry ->
                    val selected = vm.selectedCode == entry.code
                    val entryInteraction = remember { MutableInteractionSource() }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = if (selected) {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    entryItemYByCode[entry.code] = coordinates.positionInRoot().y
                                }
                                .clickable(
                                    interactionSource = entryInteraction,
                                    indication = null
                                ) {
                                    val y = entryItemYByCode[entry.code]
                                    if (y != null) {
                                        pendingSelectionAnchor = SelectionAnchor(
                                            context = SelectionAnchorContext.ENTRY,
                                            code = entry.code,
                                            yInRoot = y
                                        )
                                    } else {
                                        pendingSelectionAnchor = null
                                    }
                                    vm.onEntryCardClicked(entry.code)
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.title,
                                            modifier = Modifier.privacyObfuscate(
                                                enabled = vm.incognitoModeEnabled,
                                                overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                            ),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Code: ${entry.code}",
                                            modifier = Modifier.privacyObfuscate(
                                                enabled = vm.incognitoModeEnabled,
                                                overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { vm.requestToggleEntryPinned(entry.code) },
                                        enabled = !vm.incognitoModeEnabled,
                                        modifier = Modifier
                                            .padding(top = 2.dp, end = 2.dp)
                                            .size(33.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_push_pin_24),
                                            contentDescription = if (entry.pinned) {
                                                "Unpin entry ${entry.code}"
                                            } else {
                                                "Pin entry ${entry.code}"
                                            },
                                            tint = if (vm.incognitoModeEnabled) {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                            } else if (entry.pinned) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.size(21.dp)
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(0.dp)
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Text(
                                                    text = "Pages: ${entry.numPages}",
                                                    modifier = Modifier.privacyObfuscate(
                                                        enabled = vm.incognitoModeEnabled,
                                                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                    ),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                    "Uploaded: ${entry.uploadDate.ifBlank { "-" }}",
                                                    modifier = Modifier.privacyObfuscate(
                                                        enabled = vm.incognitoModeEnabled,
                                                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                    ),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            Text(
                                                "Fetched: ${entry.addedAt.ifBlank { "-" }}",
                                                modifier = Modifier.privacyObfuscate(
                                                    enabled = vm.incognitoModeEnabled,
                                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "Status: ${if (entry.isRead) "Read" else "Unread"}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (entry.isRead) READ_STATE_COLOR else UNREAD_STATE_COLOR,
                                                    modifier = Modifier.privacyObfuscate(
                                                        enabled = vm.incognitoModeEnabled,
                                                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                    )
                                                )
                                            }
                                        }

                                        if (vm.showThumbnails && entry.thumbnailUrl.isNotBlank()) {
                                            ThumbnailImage(
                                                thumbnailUrl = entry.thumbnailUrl,
                                                contentDescription = "Cover for code ${entry.code}",
                                                obscure = vm.incognitoModeEnabled,
                                                modifier = Modifier
                                                    .width(112.dp)
                                                    .height(76.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Rating: ${renderStars(entry.rating)} (${entry.rating}/5)",
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .privacyObfuscate(
                                                enabled = vm.incognitoModeEnabled,
                                                overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                            ),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Text(
                                    text = "Tags: ${entry.tags.ifBlank { "-" }}",
                                    modifier = Modifier.privacyObfuscate(
                                        enabled = vm.incognitoModeEnabled,
                                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (selected) {
                            val detail = vm.selectedDetail?.takeIf { it.code == entry.code }
                            SelectedEntryDetailCard(
                                detail = detail,
                                onOpenInBrowser = vm::openSelectedInBrowser,
                                onOpenCreatorFromDetail = vm::openCreatorFromDetail,
                                onCopyCode = vm::copyCodeToClipboard,
                                onToggleReadStatus = vm::toggleEntryRead,
                                onSetRating = vm::setEntryRating,
                                onResetRating = { code -> vm.setEntryRating(code, 0) },
                                onRefetch = vm::refetchCode,
                                onDelete = { code ->
                                    if (vm.selectedCode != code) {
                                        vm.selectEntry(code)
                                    }
                                    showDeleteConfirm = true
                                },
                                seriesNeighbors = vm.selectedSeriesNeighbors,
                                onOpenSeriesEntry = vm::openSeriesEntry,
                                onCopyCreatorName = vm::copyCreatorNameToClipboard,
                                onSelectedThumbnailClick = { url, description ->
                                    thumbnailPreview = ThumbnailPreviewState(
                                        thumbnailUrl = url,
                                        contentDescription = description
                                    )
                                },
                                showThumbnails = vm.showThumbnails,
                                incognitoModeEnabled = vm.incognitoModeEnabled
                            )
                        }
                    }
                }
                }
                if (normalColumns > 1) {
                    val normalRows = vm.entries.chunked(normalColumns)
                    items(
                        normalRows,
                        key = { row -> row.joinToString("_") { it.code.toString() } },
                        contentType = { "entry_row_grid" }
                    ) { rowEntries ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowEntries.forEach { entry ->
                                    val selected = vm.selectedCode == entry.code
                                    val entryInteraction = remember { MutableInteractionSource() }
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHigh
                                            }
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                        border = if (selected) {
                                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                        } else {
                                            null
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .onGloballyPositioned { coordinates ->
                                                entryItemYByCode[entry.code] = coordinates.positionInRoot().y
                                            }
                                            .clickable(
                                                interactionSource = entryInteraction,
                                                indication = null
                                            ) {
                                                val y = entryItemYByCode[entry.code]
                                                if (y != null) {
                                                    pendingSelectionAnchor = SelectionAnchor(
                                                        context = SelectionAnchorContext.ENTRY,
                                                        code = entry.code,
                                                        yInRoot = y
                                                    )
                                                } else {
                                                    pendingSelectionAnchor = null
                                                }
                                                vm.onEntryCardClicked(entry.code)
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text(
                                                    text = entry.title,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .privacyObfuscate(
                                                            enabled = vm.incognitoModeEnabled,
                                                            overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                        ),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                IconButton(
                                                    onClick = { vm.requestToggleEntryPinned(entry.code) },
                                                    enabled = !vm.incognitoModeEnabled,
                                                    modifier = Modifier
                                                        .padding(top = 1.dp, end = 1.dp)
                                                        .size(28.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_push_pin_24),
                                                        contentDescription = if (entry.pinned) {
                                                            "Unpin entry ${entry.code}"
                                                        } else {
                                                            "Pin entry ${entry.code}"
                                                        },
                                                        tint = if (vm.incognitoModeEnabled) {
                                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                                        } else if (entry.pinned) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        },
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "Code: ${entry.code}",
                                                modifier = Modifier.privacyObfuscate(
                                                    enabled = vm.incognitoModeEnabled,
                                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Pages: ${entry.numPages}",
                                                modifier = Modifier.privacyObfuscate(
                                                    enabled = vm.incognitoModeEnabled,
                                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                ),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                text = "Uploaded: ${entry.uploadDate.ifBlank { "-" }}",
                                                modifier = Modifier.privacyObfuscate(
                                                    enabled = vm.incognitoModeEnabled,
                                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Fetched: ${entry.addedAt.ifBlank { "-" }}",
                                                modifier = Modifier.privacyObfuscate(
                                                    enabled = vm.incognitoModeEnabled,
                                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            if (vm.showThumbnails && entry.thumbnailUrl.isNotBlank()) {
                                                ThumbnailImage(
                                                    thumbnailUrl = entry.thumbnailUrl,
                                                    contentDescription = "Cover for code ${entry.code}",
                                                    obscure = vm.incognitoModeEnabled,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(86.dp)
                                                )
                                            }

                                            Text(
                                                text = "Rating: ${renderStars(entry.rating)} (${entry.rating}/5)",
                                                modifier = Modifier.privacyObfuscate(
                                                    enabled = vm.incognitoModeEnabled,
                                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                if (rowEntries.size < normalColumns) {
                                    repeat(normalColumns - rowEntries.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }

                            val selectedInRow = rowEntries.firstOrNull { it.code == vm.selectedCode }
                            if (selectedInRow != null) {
                                val detail = vm.selectedDetail?.takeIf { it.code == selectedInRow.code }
                                SelectedEntryDetailCard(
                                    detail = detail,
                                    onOpenInBrowser = vm::openSelectedInBrowser,
                                    onOpenCreatorFromDetail = vm::openCreatorFromDetail,
                                    onCopyCode = vm::copyCodeToClipboard,
                                    onToggleReadStatus = vm::toggleEntryRead,
                                    onSetRating = vm::setEntryRating,
                                    onResetRating = { code -> vm.setEntryRating(code, 0) },
                                    onRefetch = vm::refetchCode,
                                    onDelete = { code ->
                                        if (vm.selectedCode != code) {
                                            vm.selectEntry(code)
                                        }
                                        showDeleteConfirm = true
                                    },
                                    seriesNeighbors = vm.selectedSeriesNeighbors,
                                    onOpenSeriesEntry = vm::openSeriesEntry,
                                    onCopyCreatorName = vm::copyCreatorNameToClipboard,
                                    onSelectedThumbnailClick = { url, description ->
                                        thumbnailPreview = ThumbnailPreviewState(
                                            thumbnailUrl = url,
                                            contentDescription = description
                                        )
                                    },
                                    showThumbnails = vm.showThumbnails,
                                    incognitoModeEnabled = vm.incognitoModeEnabled
                                )
                            }
                        }
                    }
                }
            }
            }

            item {
                Card {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Artists / Groups",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            ImmediateActionText(
                                label = if (vm.creatorsCardCollapsed) "Expand" else "Collapse",
                                onAction = vm::toggleCreatorsCardCollapsed,
                                onPressStart = ::stopActiveScrolls,
                                runOnPressWhen = {
                                    rootListState.isScrollInProgress ||
                                        tagsListState.isScrollInProgress ||
                                        creatorsListState.isScrollInProgress
                                },
                                textStyle = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (!vm.creatorsCardCollapsed) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    EntrySortChip(
                                        label = "Name${creatorSortArrow(vm, CreatorSortField.NAME)}",
                                        selected = vm.creatorSortField == CreatorSortField.NAME,
                                        activeDirection = if (vm.creatorSortField == CreatorSortField.NAME) vm.creatorSortDirection else null,
                                        onClick = { vm.onCreatorSortClicked(CreatorSortField.NAME) },
                                        onPressStart = ::stopActiveScrolls,
                                        runOnPressWhen = isAnyListScrolling
                                    )
                                }
                                item {
                                    EntrySortChip(
                                        label = "Type${creatorSortArrow(vm, CreatorSortField.TYPE)}",
                                        selected = vm.creatorSortField == CreatorSortField.TYPE,
                                        activeDirection = if (vm.creatorSortField == CreatorSortField.TYPE) vm.creatorSortDirection else null,
                                        onClick = { vm.onCreatorSortClicked(CreatorSortField.TYPE) },
                                        onPressStart = ::stopActiveScrolls,
                                        runOnPressWhen = isAnyListScrolling
                                    )
                                }
                                item {
                                    EntrySortChip(
                                        label = "Count${creatorSortArrow(vm, CreatorSortField.COUNT)}",
                                        selected = vm.creatorSortField == CreatorSortField.COUNT,
                                        activeDirection = if (vm.creatorSortField == CreatorSortField.COUNT) vm.creatorSortDirection else null,
                                        onClick = { vm.onCreatorSortClicked(CreatorSortField.COUNT) },
                                        onPressStart = ::stopActiveScrolls,
                                        runOnPressWhen = isAnyListScrolling
                                    )
                                }
                            }

                            if (vm.creators.isEmpty()) {
                                Text(
                                    "No artists/groups tracked.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 180.dp, max = creatorsListMaxHeight),
                                    state = creatorsListState,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(
                                        vm.creators,
                                        key = { it.id },
                                        contentType = { "creator_row" }
                                    ) { creator ->
                                        val expanded = vm.expandedCreatorIds.contains(creator.id)
                                        val creatorInteraction = remember { MutableInteractionSource() }
                                        val containerColor = if (expanded) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(containerColor, shape = MaterialTheme.shapes.small)
                                                .clickable(
                                                    interactionSource = creatorInteraction,
                                                    indication = null
                                                ) { vm.toggleCreatorExpanded(creator.id) }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CompositionLocalProvider(
                                                LocalMinimumInteractiveComponentEnforcement provides false
                                            ) {
                                                TextButton(
                                                    onClick = { vm.openCreatorPreviewInBrowser(creator.type, creator.name) },
                                                    enabled = !vm.incognitoModeEnabled,
                                                    modifier = Modifier.weight(0.48f),
                                                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                                                ) {
                                                    Text(
                                                        text = creator.name,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .privacyObfuscate(
                                                                enabled = vm.incognitoModeEnabled,
                                                                overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                            ),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign = TextAlign.Start,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                            Text(
                                                text = creator.type,
                                                modifier = Modifier
                                                    .weight(0.24f)
                                                    .privacyObfuscate(
                                                        enabled = vm.incognitoModeEnabled,
                                                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                    ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = creator.entryCount.toString(),
                                                modifier = Modifier
                                                    .weight(0.16f)
                                                    .privacyObfuscate(
                                                        enabled = vm.incognitoModeEnabled,
                                                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                    ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = if (expanded) "▲" else "▼",
                                                modifier = Modifier.weight(0.12f),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        if (expanded) {
                                            val linkedEntries = vm.creatorEntriesFor(creator.id)
                                            if (vm.isCreatorLoading(creator.id)) {
                                                Text(
                                                    "(loading...)",
                                                    modifier = Modifier.padding(start = 12.dp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else if (linkedEntries.isEmpty()) {
                                                Text(
                                                    "(no linked entries)",
                                                    modifier = Modifier.padding(start = 12.dp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else {
                                                linkedEntries.forEach { linked ->
                                                    val linkKey = creatorLinkKey(creator.id, linked.code)
                                                    TextButton(
                                                        onClick = {
                                                            val y = creatorLinkYByKey[linkKey]
                                                            if (y != null) {
                                                                pendingSelectionAnchor = SelectionAnchor(
                                                                    context = SelectionAnchorContext.CREATOR_LINK,
                                                                    code = linked.code,
                                                                    creatorId = creator.id,
                                                                    yInRoot = y
                                                                )
                                                            } else {
                                                                pendingSelectionAnchor = null
                                                            }
                                                            vm.selectEntryFromCreator(linked.code)
                                                        },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(start = 8.dp)
                                                            .onGloballyPositioned { coordinates ->
                                                                creatorLinkYByKey[linkKey] = coordinates.positionInRoot().y
                                                            }
                                                    ) {
                                                        Text(
                                                            "${linked.code} - ${linked.title}",
                                                            modifier = Modifier.privacyObfuscate(
                                                                enabled = vm.incognitoModeEnabled,
                                                                overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                            ),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    if (vm.selectedCode == linked.code) {
                                                        val detail = vm.selectedDetail?.takeIf { it.code == linked.code }
                                                        SelectedEntryDetailCard(
                                                            detail = detail,
                                                            onOpenInBrowser = vm::openSelectedInBrowser,
                                                            onOpenCreatorFromDetail = vm::openCreatorFromDetail,
                                                            onCopyCode = vm::copyCodeToClipboard,
                                                            onToggleReadStatus = vm::toggleEntryRead,
                                                            onSetRating = vm::setEntryRating,
                                                            onResetRating = { code -> vm.setEntryRating(code, 0) },
                                                            onRefetch = vm::refetchCode,
                                                            onDelete = { code ->
                                                                if (vm.selectedCode != code) {
                                                                    vm.selectEntry(code)
                                                                }
                                                                showDeleteConfirm = true
                                                            },
                                                            seriesNeighbors = vm.selectedSeriesNeighbors,
                                                            onOpenSeriesEntry = vm::openSeriesEntry,
                                                            onCopyCreatorName = vm::copyCreatorNameToClipboard,
                                                            onSelectedThumbnailClick = { url, description ->
                                                                thumbnailPreview = ThumbnailPreviewState(
                                                                    thumbnailUrl = url,
                                                                    contentDescription = description
                                                                )
                                                            },
                                                            showThumbnails = vm.showThumbnails,
                                                            incognitoModeEnabled = vm.incognitoModeEnabled,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(start = 8.dp, bottom = 6.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}
}
}

@Composable
private fun SelectedEntryDetailCard(
    detail: EntryDetail?,
    onOpenInBrowser: () -> Unit,
    onOpenCreatorFromDetail: (String, String) -> Unit,
    onCopyCode: (Int) -> Unit,
    onToggleReadStatus: (Int) -> Unit,
    onSetRating: (Int, Int) -> Unit,
    onResetRating: (Int) -> Unit,
    onRefetch: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    seriesNeighbors: SeriesNeighbors,
    onOpenSeriesEntry: (Int) -> Unit,
    onCopyCreatorName: (String, String) -> Unit,
    onSelectedThumbnailClick: (String, String) -> Unit,
    showThumbnails: Boolean,
    incognitoModeEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        val privacyOverlay = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Selected Entry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (detail != null) {
                    IconButton(
                        onClick = onOpenInBrowser,
                        enabled = !incognitoModeEnabled
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_open_in_new_24),
                            contentDescription = "Open in slideshow",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (detail == null) {
                Text(
                    "Selected entry details unavailable.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (showThumbnails && detail.thumbnailUrl.isNotBlank()) {
                    ThumbnailImage(
                        thumbnailUrl = detail.thumbnailUrl,
                        contentDescription = "Large cover for code ${detail.code}",
                        obscure = incognitoModeEnabled,
                        onClick = {
                            onSelectedThumbnailClick(
                                detail.thumbnailUrl,
                                "Large cover for code ${detail.code}"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Code:",
                            modifier = Modifier.privacyObfuscate(
                                enabled = incognitoModeEnabled,
                                overlayColor = privacyOverlay
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = { onCopyCode(detail.code) },
                            enabled = !incognitoModeEnabled,
                            modifier = Modifier.heightIn(min = 0.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = detail.code.toString(),
                                modifier = Modifier.privacyObfuscate(
                                    enabled = incognitoModeEnabled,
                                    overlayColor = privacyOverlay
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(
                            onClick = { onRefetch(detail.code) },
                            enabled = !incognitoModeEnabled,
                            modifier = Modifier.heightIn(min = 0.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "Re-fetch",
                                modifier = Modifier.privacyObfuscate(
                                    enabled = incognitoModeEnabled,
                                    overlayColor = privacyOverlay
                                )
                            )
                        }
                        TextButton(
                            onClick = { onDelete(detail.code) },
                            enabled = !incognitoModeEnabled,
                            modifier = Modifier.heightIn(min = 0.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "Delete",
                                modifier = Modifier.privacyObfuscate(
                                    enabled = incognitoModeEnabled,
                                    overlayColor = privacyOverlay
                                )
                            )
                        }
                    }
                }
                Text(
                    text = "Title: ${detail.title}",
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    )
                )
                Text(
                    text = "Subtitle: ${detail.subtitle.ifBlank { "-" }}",
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    )
                )
                Text(
                    text = "Pages: ${detail.numPages}",
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    )
                )
                Text(
                    text = "Uploaded: ${detail.uploadDate.ifBlank { "-" }}",
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    )
                )
                Text(
                    text = "Rating: ${renderStars(detail.rating)} (${detail.rating}/5)",
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    )
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Rate:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.privacyObfuscate(
                            enabled = incognitoModeEnabled,
                            overlayColor = privacyOverlay
                        )
                    )
                    for (index in 1..5) {
                        val filled = index <= detail.rating
                        val starInteraction = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clickable(
                                    interactionSource = starInteraction,
                                    indication = null,
                                    enabled = !incognitoModeEnabled
                                ) { onSetRating(detail.code, index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (filled) "★" else "☆",
                                modifier = Modifier.privacyObfuscate(
                                    enabled = incognitoModeEnabled,
                                    overlayColor = privacyOverlay
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (filled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                    TextButton(
                        onClick = { onResetRating(detail.code) },
                        enabled = !incognitoModeEnabled,
                        modifier = Modifier.heightIn(min = 0.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Reset",
                            modifier = Modifier.privacyObfuscate(
                                enabled = incognitoModeEnabled,
                                overlayColor = privacyOverlay
                            )
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Status:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.privacyObfuscate(
                            enabled = incognitoModeEnabled,
                            overlayColor = privacyOverlay
                        )
                    )
                    TextButton(
                        onClick = { onToggleReadStatus(detail.code) },
                        enabled = !incognitoModeEnabled,
                        modifier = Modifier.heightIn(min = 0.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = if (detail.isRead) "Read" else "Unread",
                            color = if (detail.isRead) READ_STATE_COLOR else UNREAD_STATE_COLOR,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.privacyObfuscate(
                                enabled = incognitoModeEnabled,
                                overlayColor = privacyOverlay
                            )
                        )
                    }
                }
                Text(
                    text = "Updated at: ${detail.fetchedAt}",
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    )
                )
                Text(
                    text = "Fetched at: ${detail.addedAt}",
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    )
                )
                Text(
                    text = "URL: ${detail.sourceUrl}",
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    )
                )
                val artistNames = detail.tagsByType["artist"].orEmpty()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                val groupNames = detail.tagsByType["group"].orEmpty()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                artistNames.forEach { artistName ->
                    CreatorJumpRow(
                        label = "Artist",
                        creatorType = "artist",
                        creatorName = artistName,
                        onOpenCreator = onOpenCreatorFromDetail,
                        onCopyCreatorName = onCopyCreatorName,
                        incognitoModeEnabled = incognitoModeEnabled
                    )
                }
                groupNames.forEach { groupName ->
                    CreatorJumpRow(
                        label = "Group",
                        creatorType = "group",
                        creatorName = groupName,
                        onOpenCreator = onOpenCreatorFromDetail,
                        onCopyCreatorName = onCopyCreatorName,
                        incognitoModeEnabled = incognitoModeEnabled
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tags:",
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    ),
                    fontWeight = FontWeight.SemiBold
                )
                if (detail.tagsByType.isEmpty()) {
                    Text(
                        text = "(none)",
                        modifier = Modifier.privacyObfuscate(
                            enabled = incognitoModeEnabled,
                            overlayColor = privacyOverlay
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    detail.tagsByType.forEach { (type, names) ->
                        Text(
                            text = "$type: ${names.joinToString(", ")}",
                            modifier = Modifier.privacyObfuscate(
                                enabled = incognitoModeEnabled,
                                overlayColor = privacyOverlay
                            )
                        )
                    }
                }
                if (seriesNeighbors.previous != null || seriesNeighbors.next != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Related In Series:",
                        modifier = Modifier.privacyObfuscate(
                            enabled = incognitoModeEnabled,
                            overlayColor = privacyOverlay
                        ),
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val prev = seriesNeighbors.previous
                        if (prev != null) {
                            TextButton(
                                onClick = { onOpenSeriesEntry(prev.code) },
                                enabled = !incognitoModeEnabled,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "← Previous: ${prev.code} ${prev.title}",
                                    modifier = Modifier.privacyObfuscate(
                                        enabled = incognitoModeEnabled,
                                        overlayColor = privacyOverlay
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        val next = seriesNeighbors.next
                        if (next != null) {
                            TextButton(
                                onClick = { onOpenSeriesEntry(next.code) },
                                enabled = !incognitoModeEnabled,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "→ Next: ${next.code} ${next.title}",
                                    modifier = Modifier.privacyObfuscate(
                                        enabled = incognitoModeEnabled,
                                        overlayColor = privacyOverlay
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
    }
}
}

private object ThumbnailBitmapCache {
    private val maxItems = run {
        val maxMemMb = (Runtime.getRuntime().maxMemory() / (1024 * 1024)).toInt()
        when {
            maxMemMb >= 512 -> 320
            maxMemMb >= 384 -> 280
            maxMemMb >= 256 -> 240
            else -> 180
        }
    }
    private val map = object : LinkedHashMap<String, ImageBitmap>(maxItems, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean {
            return size > maxItems
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

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun CreatorJumpRow(
    label: String,
    creatorType: String,
    creatorName: String,
    onOpenCreator: (String, String) -> Unit,
    onCopyCreatorName: (String, String) -> Unit,
    incognitoModeEnabled: Boolean
) {
    val creatorInteraction = remember { MutableInteractionSource() }
    val privacyOverlay = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$label:",
            modifier = Modifier.privacyObfuscate(
                enabled = incognitoModeEnabled,
                overlayColor = privacyOverlay
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .combinedClickable(
                    interactionSource = creatorInteraction,
                    indication = rememberRipple(
                        bounded = true,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                    ),
                    enabled = !incognitoModeEnabled,
                    onClick = { onOpenCreator(creatorType, creatorName) },
                    onLongClick = { onCopyCreatorName(creatorType, creatorName) }
                )
                .heightIn(min = 32.dp)
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .privacyObfuscate(
                    enabled = incognitoModeEnabled,
                    overlayColor = privacyOverlay
                )
        ) {
            Text(
                text = creatorName,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private val thumbnailHttpClient: OkHttpClient by lazy {
    val requestDispatcher = okhttp3.Dispatcher().apply {
        maxRequests = 64
        maxRequestsPerHost = 12
    }
    OkHttpClient.Builder()
        .dispatcher(requestDispatcher)
        .connectionPool(ConnectionPool(12, 5, TimeUnit.MINUTES))
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()
}

@Composable
private fun ThumbnailImage(
    thumbnailUrl: String,
    contentDescription: String,
    obscure: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = ThumbnailBitmapCache.get(thumbnailUrl), thumbnailUrl) {
        if (thumbnailUrl.isBlank()) {
            value = null
            return@produceState
        }

        val cached = ThumbnailBitmapCache.get(thumbnailUrl)
        if (cached != null) {
            value = cached
            return@produceState
        }

        val fetched = withContext(Dispatchers.IO) { fetchThumbnailBitmap(thumbnailUrl) }
        if (fetched != null) {
            ThumbnailBitmapCache.put(thumbnailUrl, fetched)
        }
        value = fetched
    }

    val boxModifier = modifier
        .clip(MaterialTheme.shapes.small)
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .privacyObfuscate(
            enabled = obscure,
            overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
        )
        .let { base ->
            if (onClick != null) {
                base.clickable(onClick = onClick)
            } else {
                base
            }
        }

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        val imageBitmap = bitmap
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            Text(
                text = "No preview",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ThumbnailPreviewDialog(
    thumbnailUrl: String,
    contentDescription: String,
    obscure: Boolean,
    onDismiss: () -> Unit
) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.86f)
        .coerceIn(260.dp, 860.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThumbnailImage(
                        thumbnailUrl = thumbnailUrl,
                        contentDescription = contentDescription,
                        obscure = obscure,
                        onClick = onDismiss,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = maxHeight)
                    )
                }
            }
        }
    }
}

private fun buildThumbnailCandidateUrls(url: String): List<String> {
    val trimmed = url.trim()
    if (trimmed.isBlank()) return emptyList()
    val pattern = Regex("(?i)^(https?://[^/]+/galleries/\\d+/cover)\\.([a-z0-9]+)(\\?.*)?$")
    val match = pattern.matchEntire(trimmed) ?: return listOf(trimmed)
    val base = match.groupValues.getOrNull(1).orEmpty()
    val ext = match.groupValues.getOrNull(2).orEmpty().lowercase(Locale.US)
    val suffix = match.groupValues.getOrNull(3).orEmpty()
    val extOrder = buildList {
        if (ext.isNotBlank()) add(ext)
        addAll(listOf("jpg", "jpeg", "png", "webp", "gif"))
    }.distinct()
    return extOrder.map { "$base.$it$suffix" }
}

private fun fetchThumbnailBitmapOnce(url: String): ImageBitmap? {
    val request = Request.Builder()
        .url(url)
        .header(
            "User-Agent",
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
        )
        .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
        .header("Referer", "https://nhentai.net/")
        .build()

    return thumbnailHttpClient.newCall(request).execute().use { rsp ->
        if (!rsp.isSuccessful) return null
        val bytes = rsp.body?.bytes() ?: return null
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        bitmap.asImageBitmap()
    }
}

private fun fetchThumbnailBitmap(url: String): ImageBitmap? {
    if (url.isBlank()) return null
    val candidates = buildThumbnailCandidateUrls(url)
    if (candidates.isEmpty()) return null

    candidates.forEach { candidateUrl ->
        repeat(2) { attempt ->
            val fetched = runCatching { fetchThumbnailBitmapOnce(candidateUrl) }.getOrNull()
            if (fetched != null) {
                return fetched
            }
            if (attempt == 0) {
                Thread.sleep(65)
            }
        }
    }
    return null
}

private fun Modifier.privacyObfuscate(
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

@Composable
private fun ImmediateActionText(
    label: String,
    onAction: () -> Unit,
    onPressStart: () -> Unit = {},
    runOnPressWhen: () -> Boolean = { false },
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.labelLarge,
    fontWeight: FontWeight = FontWeight.Normal
) {
    var firedOnPress by remember { mutableStateOf(false) }
    val actionScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    TextButton(
        onClick = {
            if (firedOnPress) {
                firedOnPress = false
            } else {
                onAction()
            }
        },
        interactionSource = interactionSource,
        modifier = modifier.pointerInput(onPressStart, runOnPressWhen) {
            awaitEachGesture {
                firedOnPress = false
                val down = awaitFirstDown(requireUnconsumed = false)
                val shouldRunOnPress = runOnPressWhen()
                onPressStart()

                if (shouldRunOnPress) {
                    firedOnPress = true
                    val press = PressInteraction.Press(down.position)
                    actionScope.launch {
                        interactionSource.emit(press)
                    }
                    actionScope.launch {
                        // Keep a short delay so the press animation is visible.
                        delay(32)
                        onAction()
                    }
                    val up = waitForUpOrCancellation()
                    actionScope.launch {
                        if (up == null) {
                            interactionSource.emit(PressInteraction.Cancel(press))
                        } else {
                            interactionSource.emit(PressInteraction.Release(press))
                        }
                    }
                } else {
                    waitForUpOrCancellation()
                }
            }
        }
    ) {
        Text(
            text = label,
            style = textStyle,
            fontWeight = fontWeight
        )
    }
}

@Composable
private fun EntrySortChip(
    label: String,
    selected: Boolean,
    activeDirection: SortDirection? = null,
    onClick: () -> Unit,
    onPressStart: () -> Unit = {},
    runOnPressWhen: () -> Boolean = { false },
    modifier: Modifier = Modifier
) {
    var firedOnPress by remember { mutableStateOf(false) }
    val actionScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val descAccent = Color(0xFF22C55E)
    val ascAccent = Color(0xFFEF4444)
    val selectedAccent = when (activeDirection) {
        SortDirection.DESC -> descAccent
        SortDirection.ASC -> ascAccent
        null -> MaterialTheme.colorScheme.primary
    }
    val selectedContainer = selectedAccent.copy(alpha = 0.20f)
    val inactiveBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val inactiveLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f)
    val chipBorder = if (selected) {
        BorderStroke(1.3.dp, selectedAccent.copy(alpha = 0.90f))
    } else {
        BorderStroke(1.2.dp, inactiveBorderColor)
    }
    FilterChip(
        modifier = modifier.pointerInput(onPressStart, runOnPressWhen) {
            awaitEachGesture {
                firedOnPress = false
                val down = awaitFirstDown(requireUnconsumed = false)
                val shouldRunOnPress = runOnPressWhen()
                onPressStart()

                if (shouldRunOnPress) {
                    firedOnPress = true
                    val press = PressInteraction.Press(down.position)
                    actionScope.launch {
                        interactionSource.emit(press)
                    }
                    actionScope.launch {
                        delay(32)
                        onClick()
                    }
                    val up = waitForUpOrCancellation()
                    actionScope.launch {
                        if (up == null) {
                            interactionSource.emit(PressInteraction.Cancel(press))
                        } else {
                            interactionSource.emit(PressInteraction.Release(press))
                        }
                    }
                } else {
                    waitForUpOrCancellation()
                }
            }
        },
        selected = selected,
        onClick = {
            if (firedOnPress) {
                firedOnPress = false
            } else {
                onClick()
            }
        },
        interactionSource = interactionSource,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = inactiveLabelColor,
            selectedContainerColor = selectedContainer,
            selectedLabelColor = selectedAccent
        ),
        border = chipBorder,
        label = { Text(label) }
    )
}

@Composable
private fun ThemeToggleWithAccentPicker(
    themeMode: ThemeMode,
    accentMode: AccentMode,
    incognitoModeEnabled: Boolean,
    onCycleThemeMode: () -> Unit,
    onAccentModeSelected: (AccentMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = ACCENT_PICKER_OPTIONS
    var pickerVisible by remember { mutableStateOf(false) }
    var highlightedIndex by remember { mutableStateOf<Int?>(null) }

    val iconSize = 40.dp
    val chipSize = 22.dp
    val chipSpacing = 8.dp
    val pillPadding = 10.dp
    val pillGap = 8.dp
    val pillHeight = 34.dp
    val pillWidth =
        (pillPadding * 2) + (chipSize * options.size) + (chipSpacing * (options.size - 1))
    val expandedWidth = iconSize + pillGap + pillWidth

    val density = androidx.compose.ui.platform.LocalDensity.current
    fun indexForX(x: Float): Int? {
        val iconPx = with(density) { iconSize.toPx() }
        val gapPx = with(density) { pillGap.toPx() }
        val paddingPx = with(density) { pillPadding.toPx() }
        val chipPx = with(density) { chipSize.toPx() }
        val slotPx = with(density) { (chipSize + chipSpacing).toPx() }

        val start = iconPx + gapPx + paddingPx
        val end = start + ((options.size - 1) * slotPx) + chipPx
        if (x < start || x > end) return null
        val idx = ((x - start) / slotPx).toInt()
        return idx.coerceIn(0, options.lastIndex)
    }

    Box(
        modifier = modifier
            .width(if (pickerVisible && !incognitoModeEnabled) expandedWidth else iconSize)
            .height(iconSize)
            .pointerInput(incognitoModeEnabled, accentMode) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (incognitoModeEnabled) {
                        waitForUpOrCancellation()
                        return@awaitEachGesture
                    }

                    val longPress = awaitLongPressOrCancellation(down.id)
                    if (longPress == null) {
                        onCycleThemeMode()
                        return@awaitEachGesture
                    }

                    pickerVisible = true
                    highlightedIndex = indexForX(longPress.position.x)
                        ?: options.indexOfFirst { it.mode == accentMode }.takeIf { it >= 0 }

                    var released = false
                    while (!released) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: event.changes.firstOrNull()
                        if (change == null) {
                            released = true
                        } else if (!change.pressed) {
                            released = true
                        } else {
                            highlightedIndex = indexForX(change.position.x) ?: highlightedIndex
                        }
                    }

                    val picked = highlightedIndex
                    if (picked != null && picked in options.indices) {
                        onAccentModeSelected(options[picked].mode)
                    }
                    pickerVisible = false
                    highlightedIndex = null
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        if (pickerVisible && !incognitoModeEnabled) {
            Box(
                modifier = Modifier
                    .offset(x = iconSize + pillGap)
                    .height(pillHeight)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = pillPadding),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(chipSpacing)
                ) {
                    options.forEachIndexed { index, option ->
                        val selected = accentMode == option.mode
                        val hovered = highlightedIndex == index
                        val ringColor = when {
                            hovered -> MaterialTheme.colorScheme.primary
                            selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                        if (option.color == null) {
                            val autoCircleColor = if (hovered || selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            }
                            Canvas(
                                modifier = Modifier
                                    .size(chipSize)
                                    .padding(1.dp)
                            ) {
                                val stroke = if (hovered || selected) 3f else 2f
                                val radius = (size.minDimension / 2f) - stroke
                                drawCircle(
                                    color = autoCircleColor,
                                    radius = radius,
                                    style = Stroke(width = stroke)
                                )
                                drawLine(
                                    color = autoCircleColor,
                                    start = Offset(size.width * 0.72f, size.height * 0.18f),
                                    end = Offset(size.width * 0.28f, size.height * 0.82f),
                                    strokeWidth = stroke
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(chipSize)
                                    .clip(CircleShape)
                                    .background(option.color)
                                    .border(
                                        width = if (hovered || selected) 2.2.dp else 1.2.dp,
                                        color = ringColor,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(CircleShape)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (incognitoModeEnabled) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_incognito_24),
                    contentDescription = "Incognito mode enabled",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = themeModeSymbol(themeMode),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun themeModeSymbol(mode: ThemeMode): String {
    return when (mode) {
        ThemeMode.LIGHT -> "☀"
        ThemeMode.DARK -> "☾"
        ThemeMode.SYSTEM -> "◐"
    }
}

private data class AccentPickerOption(
    val mode: AccentMode,
    val color: Color?
)

private val ACCENT_PICKER_OPTIONS = listOf(
    AccentPickerOption(AccentMode.AUTO, null),
    AccentPickerOption(AccentMode.RED, Color(0xFFE53935)),
    AccentPickerOption(AccentMode.ORANGE, Color(0xFFFB8C00)),
    AccentPickerOption(AccentMode.AMBER, Color(0xFFF9A825)),
    AccentPickerOption(AccentMode.GREEN, Color(0xFF43A047)),
    AccentPickerOption(AccentMode.TEAL, Color(0xFF00897B)),
    AccentPickerOption(AccentMode.BLUE, Color(0xFF1E88E5)),
    AccentPickerOption(AccentMode.INDIGO, Color(0xFF5E35B1)),
    AccentPickerOption(AccentMode.PINK, Color(0xFFD81B60))
)

private fun accentColorForMode(mode: AccentMode): Color? {
    return ACCENT_PICKER_OPTIONS.firstOrNull { it.mode == mode }?.color
}

private fun preferredOnAccent(color: Color): Color {
    val lum = (0.299f * color.red) + (0.587f * color.green) + (0.114f * color.blue)
    return if (lum >= 0.62f) Color(0xFF111111) else Color.White
}

private fun applyAccentMode(
    baseScheme: ColorScheme,
    accentMode: AccentMode,
    isDark: Boolean
): ColorScheme {
    val accent = accentColorForMode(accentMode) ?: return baseScheme
    val onAccent = preferredOnAccent(accent)
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

private fun tagSortArrow(vm: TagBookViewModel, field: TagSortField): String {
    if (vm.tagSortField != field) return ""
    return if (vm.tagSortDirection == SortDirection.DESC) " ▼" else " ▲"
}

private fun blockedTagSortArrow(vm: TagBookViewModel, field: TagSortField): String {
    if (vm.blockedTagSortField != field) return ""
    return if (vm.blockedTagSortDirection == SortDirection.DESC) " ▼" else " ▲"
}

private fun creatorSortArrow(vm: TagBookViewModel, field: CreatorSortField): String {
    if (vm.creatorSortField != field) return ""
    return if (vm.creatorSortDirection == SortDirection.DESC) " ▼" else " ▲"
}

private fun entrySortArrow(vm: TagBookViewModel, field: EntrySortField): String {
    if (vm.sortField != field) return ""
    return if (vm.sortDirection == SortDirection.DESC) " ▼" else " ▲"
}

private fun entrySortLabel(field: EntrySortField): String {
    return when (field) {
        EntrySortField.CODE -> "Code"
        EntrySortField.TITLE -> "Title"
        EntrySortField.PAGES -> "Pages"
        EntrySortField.UPLOAD -> "Uploaded Date"
        EntrySortField.ADDED -> "Fetched Date"
        EntrySortField.RATING -> "Rating"
    }
}

private fun tagSortLabel(field: TagSortField): String {
    return when (field) {
        TagSortField.NAME -> "name"
        TagSortField.TYPE -> "type"
        TagSortField.COUNT -> "count"
    }
}

private fun creatorSortLabel(field: CreatorSortField): String {
    return when (field) {
        CreatorSortField.NAME -> "name"
        CreatorSortField.TYPE -> "type"
        CreatorSortField.COUNT -> "count"
    }
}

private fun normalizeTagName(name: String): String {
    return name
        .trim()
        .lowercase(Locale.US)
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" ")
}

private fun buildNhentaiTagSearchQuery(
    includeTagNames: List<String>,
    excludeTagNames: List<String> = emptyList()
): String {
    fun encodeTerm(rawName: String, excluded: Boolean): String? {
        val normalized = rawName.trim().replace(Regex("\\s+"), " ")
        if (normalized.isBlank()) return null
        val words = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return null

        val prefix = if (excluded) "-" else ""
        return if (words.size == 1) {
            prefix + Uri.encode(words.first())
        } else {
            val joined = words.joinToString("+") { Uri.encode(it) }
            "$prefix\"$joined\""
        }
    }

    val includeTerms = includeTagNames
        .asSequence()
        .mapNotNull { encodeTerm(it, excluded = false) }
        .toList()
    val excludeTerms = excludeTagNames
        .asSequence()
        .mapNotNull { encodeTerm(it, excluded = true) }
        .toList()

    return (includeTerms + excludeTerms).joinToString("+")
}

private fun extractSearchEverythingBrowserTerms(raw: String): List<String> {
    val input = raw.trim()
    if (input.isBlank()) return emptyList()

    val tokens = mutableListOf<String>()
    val pattern = Regex("\"([^\"]+)\"|(\\S+)")
    pattern.findAll(input).forEach { match ->
        val phrase = match.groupValues.getOrNull(1).orEmpty()
        val token = match.groupValues.getOrNull(2).orEmpty()
        val resolved = (if (phrase.isNotBlank()) phrase else token)
            .trim()
            .replace(Regex("\\s+"), " ")
        if (resolved.isNotBlank()) {
            tokens += resolved
        }
    }
    return tokens
}

private fun utcNowString(): String {
    return LocalDateTime.now(ZoneOffset.UTC).format(UTC_TIMESTAMP_FORMAT)
}

private fun parseMediaId(raw: Any?): Long {
    val parsed = when (raw) {
        is Number -> raw.toLong()
        is String -> raw.trim().toLongOrNull() ?: 0L
        else -> 0L
    }
    return parsed.coerceAtLeast(0L)
}

private fun parseCoverExtension(raw: String?): String {
    return when (raw?.trim()?.lowercase(Locale.US).orEmpty()) {
        "j", "jpg", "jpeg" -> "jpg"
        "p", "png" -> "png"
        "w", "webp" -> "webp"
        "g", "gif" -> "gif"
        else -> ""
    }
}

private fun buildThumbnailUrl(mediaId: Long, coverExt: String): String {
    if (mediaId <= 0L) return ""
    val ext = parseCoverExtension(coverExt).ifBlank { "jpg" }
    return "https://t.nhentai.net/galleries/$mediaId/cover.$ext"
}

private fun parseCode(raw: String): Int? {
    val input = raw.trim()
    if (input.isBlank()) return null

    val galleryMatch = GALLERY_LINK_PATTERN.find(input)
    val linkedCode = galleryMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
    if (linkedCode != null && linkedCode > 0) {
        return linkedCode
    }

    var cleaned = input.removePrefix("#").trim()
    while (
        cleaned.isNotEmpty() &&
        (cleaned.last() == '/' || URL_TRAILING_PUNCT.contains(cleaned.last()))
    ) {
        cleaned = cleaned.dropLast(1).trimEnd()
    }

    if (cleaned.isBlank() || cleaned.any { !it.isDigit() }) return null
    val code = cleaned.toIntOrNull() ?: return null
    return code.takeIf { it > 0 }
}

private fun parseTypedCreatorInput(raw: String): Pair<String, String>? {
    val match = CREATOR_TYPED_INPUT_PATTERN.matchEntire(raw.trim()) ?: return null
    val creatorType = match.groupValues.getOrNull(1).orEmpty().trim().lowercase(Locale.US)
    if (creatorType != "artist" && creatorType != "group") return null
    val value = match.groupValues.getOrNull(2).orEmpty().trim()
    if (value.isBlank()) return null
    return creatorType to value
}

private fun parseAmbiguousTwoWordCreatorInput(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return null
    if (parseCode(trimmed) != null) return null
    if (parseCreatorLink(trimmed) != null) return null
    if (parseTypedCreatorInput(trimmed) != null) return null
    if (!CREATOR_NAME_LINE_PATTERN.matches(trimmed)) return null

    val normalized = parseCreatorSlug(trimmed)
    if (!isTwoWordCreatorName(normalized)) return null
    return normalized
}

private fun isTwoWordCreatorName(value: String): Boolean {
    val normalized = parseCreatorSlug(value)
    if (normalized.isBlank()) return false
    val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
    return tokens.size == 2
}

private fun toHyphenatedTwoWordCreatorName(value: String): String {
    val tokens = parseCreatorSlug(value)
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    if (tokens.size != 2) return value.trim()
    return "${tokens[0]}-${tokens[1]}"
}

private fun splitTwoWordCreatorName(value: String): List<String> {
    val tokens = parseCreatorSlug(value)
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    return if (tokens.size == 2) tokens else listOf(value.trim()).filter { it.isNotBlank() }
}

private fun toCreatorUrlSlug(name: String): String {
    val normalized = parseCreatorSlug(name)
    if (normalized.isBlank()) return ""
    return normalized
        .replace(Regex("\\s+"), "-")
        .lowercase(Locale.US)
}

private fun creatorMatchScore(targetNormalized: String, candidateNormalized: String): Int {
    if (targetNormalized.isBlank() || candidateNormalized.isBlank()) return 0
    if (targetNormalized == candidateNormalized) return 3
    if (candidateNormalized.contains(targetNormalized) || targetNormalized.contains(candidateNormalized)) return 2

    val targetTokens = targetNormalized.split(Regex("\\s+")).filter { it.isNotBlank() }
    val candidateTokens = candidateNormalized.split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
    if (targetTokens.isNotEmpty() && targetTokens.all { it in candidateTokens }) return 1
    return 0
}

private fun creatorStrictIdentityKey(raw: String): String {
    return parseCreatorSlug(raw)
        .lowercase(Locale.US)
        .let { normalized ->
            Regex("[\\p{L}\\p{N}]+")
                .findAll(normalized)
                .map { it.value }
                .filter { it.isNotBlank() }
                .joinToString(" ")
        }
        .trim()
}

private fun isStrictCreatorNameMatch(input: String, resolvedName: String): Boolean {
    val inputKey = creatorStrictIdentityKey(input)
    val resolvedKey = creatorStrictIdentityKey(resolvedName)
    return inputKey.isNotBlank() && inputKey == resolvedKey
}

private fun parseCreatorSlug(rawSlug: String): String {
    var cleaned = rawSlug.trim()
    while (cleaned.isNotEmpty() && URL_TRAILING_PUNCT.contains(cleaned.last())) {
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

private fun parseCreatorLink(raw: String): CreatorLink? {
    val match = CREATOR_LINK_PATTERN.matchEntire(raw.trim()) ?: return null
    val creatorType = match.groupValues.getOrNull(1).orEmpty().trim().lowercase(Locale.US)
    if (creatorType != "artist" && creatorType != "group") return null

    var slug = match.groupValues.getOrNull(2).orEmpty().trim()
    while (slug.isNotEmpty() && URL_TRAILING_PUNCT.contains(slug.last())) {
        slug = slug.dropLast(1)
    }
    if (slug.isBlank()) return null

    val creatorName = parseCreatorSlug(slug)
    if (creatorName.isBlank()) return null
    return CreatorLink(
        type = creatorType,
        name = creatorName,
        sourceUrl = "https://nhentai.net/$creatorType/$slug/"
    )
}

private fun extractCreatorLinks(text: String): Pair<List<CreatorLink>, String> {
    val creators = mutableListOf<CreatorLink>()
    val seen = linkedSetOf<String>()
    CREATOR_LINK_PATTERN.findAll(text).forEach { match ->
        val creatorType = match.groupValues.getOrNull(1).orEmpty().trim().lowercase(Locale.US)
        if (creatorType != "artist" && creatorType != "group") return@forEach
        var slug = match.groupValues.getOrNull(2).orEmpty().trim()
        while (slug.isNotEmpty() && URL_TRAILING_PUNCT.contains(slug.last())) {
            slug = slug.dropLast(1)
        }
        if (slug.isBlank()) return@forEach

        val creatorName = parseCreatorSlug(slug)
        if (creatorName.isBlank()) return@forEach
        val dedupeKey = "$creatorType:${normalizeTagName(creatorName)}"
        if (!seen.add(dedupeKey)) return@forEach

        creators += CreatorLink(
            type = creatorType,
            name = creatorName,
            sourceUrl = "https://nhentai.net/$creatorType/$slug/"
        )
    }
    val codeSourceText = CREATOR_LINK_PATTERN.replace(text, " ")
    return creators to codeSourceText
}

private fun buildCreatorSlugCandidates(rawInput: String): List<String> {
    var cleaned = rawInput.trim().trim('/')
    while (cleaned.isNotEmpty() && URL_TRAILING_PUNCT.contains(cleaned.last())) {
        cleaned = cleaned.dropLast(1)
    }
    if (cleaned.isBlank()) return emptyList()

    val tokens = parseCreatorSlug(cleaned)
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    val candidates = linkedSetOf<String>()

    fun addCandidate(value: String) {
        val candidate = value.trim().trim('/').trim()
        if (candidate.isBlank()) return
        candidates += candidate
    }

    addCandidate(cleaned)
    addCandidate(cleaned.lowercase(Locale.US))
    if (tokens.isNotEmpty()) {
        addCandidate(tokens.joinToString("-"))
        addCandidate(tokens.joinToString("_"))
        addCandidate(tokens.joinToString("+"))
    }
    return candidates.toList()
}

private fun extractCreatorNameCandidates(text: String): List<String> {
    val names = linkedSetOf<String>()
    text.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isBlank()) return@forEach
        if (line.length > 60) return@forEach
        val lower = line.lowercase(Locale.US)
        if (lower.startsWith("sauce exported date") || lower.startsWith("format:")) return@forEach
        if (parseCode(line) != null) return@forEach
        if (parseCreatorLink(line) != null) return@forEach
        if (!CREATOR_NAME_LINE_PATTERN.matches(line)) return@forEach
        val normalized = parseCreatorSlug(line)
        val tokenCount = normalized.split(Regex("\\s+")).count { it.isNotBlank() }
        if (normalized.isNotBlank() && tokenCount in 1..6) {
            names += normalized
        }
    }
    return names.toList()
}

private fun parseSearchQuery(raw: String): ParsedSearchQuery {
    val source = raw.trim()
    if (source.isBlank()) return ParsedSearchQuery(freeText = "", filters = emptyList())

    val matches = SEARCH_FIELD_PATTERN.findAll(source).toList()
    if (matches.isEmpty()) {
        return ParsedSearchQuery(freeText = source, filters = emptyList())
    }

    val freeParts = mutableListOf<String>()
    val filters = mutableListOf<SearchFieldFilter>()
    var cursor = 0
    matches.forEachIndexed { index, match ->
        val fieldStart = match.range.first
        val fieldEndExclusive = match.range.last + 1
        if (fieldStart > cursor) {
            val freeTextSlice = source.substring(cursor, fieldStart).trim()
            if (freeTextSlice.isNotBlank()) {
                freeParts += freeTextSlice
            }
        }

        val nextStart = matches.getOrNull(index + 1)?.range?.first ?: source.length
        val rawKey = match.groupValues.getOrNull(1).orEmpty()
        val key = canonicalSearchField(rawKey)
        val value = source.substring(fieldEndExclusive, nextStart).trim()
        if (key.isNotBlank() && value.isNotBlank()) {
            filters += SearchFieldFilter(key = key, value = value)
        }
        cursor = nextStart
    }

    if (cursor < source.length) {
        val tail = source.substring(cursor).trim()
        if (tail.isNotBlank()) {
            freeParts += tail
        }
    }

    return ParsedSearchQuery(
        freeText = freeParts.joinToString(" ").trim(),
        filters = filters
    )
}

private fun canonicalSearchField(rawKey: String): String {
    val normalized = rawKey.trim().lowercase(Locale.US).replace(Regex("\\s+"), " ")
    return when (normalized) {
        "code" -> "code"
        "title" -> "title"
        "subtitle" -> "subtitle"
        "page", "pages", "num pages" -> "pages"
        "upload", "upload date" -> "upload"
        "rating" -> "rating"
        "fetched", "fetched at" -> "fetched"
        "added", "added at" -> "added"
        "url", "source", "source url", "link" -> "url"
        "tag", "tags" -> "tag"
        "artist" -> "artist"
        "group" -> "group"
        "parody" -> "parody"
        "character" -> "character"
        "category" -> "category"
        "language", "lang" -> "language"
        "type" -> "type"
        else -> ""
    }
}

private fun parseDateRange(raw: String): Pair<LocalDate, LocalDate>? {
    val dates = DATE_TOKEN_PATTERN.findAll(raw)
        .mapNotNull { match ->
            runCatching {
                LocalDate.parse(match.value, UPLOAD_DATE_FORMAT)
            }.getOrNull()
        }
        .toList()
    if (dates.size < 2) return null
    val first = dates[0]
    val second = dates[1]
    return if (first <= second) {
        first to second
    } else {
        second to first
    }
}

private fun parseFirstDate(raw: String): LocalDate? {
    val token = DATE_TOKEN_PATTERN.find(raw)?.value ?: return null
    return runCatching {
        LocalDate.parse(token, UPLOAD_DATE_FORMAT)
    }.getOrNull()
}

private fun extractNumericTokens(raw: String): List<Int> {
    return Regex("\\d+").findAll(raw)
        .mapNotNull { match -> match.value.toIntOrNull() }
        .toList()
}

private fun renderStars(rating: Int): String {
    val safe = rating.coerceIn(0, 5)
    return buildString {
        repeat(5) { idx ->
            append(if (idx < safe) '★' else '☆')
        }
    }
}

private fun decodeStrict(bytes: ByteArray, charset: java.nio.charset.Charset): String? {
    return try {
        val decoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        decoder.decode(ByteBuffer.wrap(bytes)).toString()
    } catch (_: CharacterCodingException) {
        null
    }
}

private fun spansOverlap(aStart: Int, aEndExclusive: Int, bStart: Int, bEndExclusive: Int): Boolean {
    return aStart < bEndExclusive && aEndExclusive > bStart
}

private fun findSplitCodeSequences(text: String): List<SplitSequence> {
    val sequences = mutableListOf<SplitSequence>()
    SPLIT_CODE_PATTERN.findAll(text).forEach { match ->
        val raw = match.groupValues.getOrNull(1).orEmpty()
        val merged = raw.replace(Regex("[ \\t]+"), "")
        if (merged.isBlank() || merged.length > 8) return@forEach
        sequences += SplitSequence(
            start = match.range.first,
            endExclusive = match.range.last + 1,
            raw = raw,
            merged = merged
        )
    }
    return sequences
}

private fun extractCandidates(
    text: String,
    splitSequences: List<SplitSequence>,
    combineSplitCodes: Boolean
): List<Pair<Int, Int>> {
    val blocked = splitSequences.map { it.start to it.endExclusive }
    val candidates = mutableListOf<Pair<Int, Int>>()

    CODE_PATTERN.findAll(text).forEach { match ->
        val spanStart = match.range.first
        val spanEnd = match.range.last + 1
        if (blocked.any { spansOverlap(spanStart, spanEnd, it.first, it.second) }) {
            return@forEach
        }

        val digits = match.groupValues.getOrNull(1).orEmpty()
        if (digits.length > 8) return@forEach
        val code = digits.toIntOrNull() ?: return@forEach
        if (code > 0) {
            candidates += code to digits.length
        }
    }

    if (combineSplitCodes) {
        splitSequences.forEach { seq ->
            val code = seq.merged.toIntOrNull() ?: return@forEach
            if (code > 0) {
                candidates += code to seq.merged.length
            }
        }
    }

    val deduped = LinkedHashMap<Int, Int>()
    candidates.forEach { (code, len) ->
        if (!deduped.containsKey(code)) {
            deduped[code] = len
        }
    }

    return deduped.entries.map { it.key to it.value }
}

