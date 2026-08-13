package com.example.saucetracker.feature.dashboard

import com.example.saucetracker.*
import com.example.saucetracker.core.ui.theme.AccentMode
import com.example.saucetracker.core.media.*
import com.example.saucetracker.background.syncSubscriptionBackgroundWork
import com.example.saucetracker.background.syncSubscriptionNotificationSummaryForContext
import com.example.saucetracker.data.backup.*
import com.example.saucetracker.data.downloads.*
import com.example.saucetracker.data.database.SauceTrackerDatabase
import com.example.saucetracker.data.remote.GalleryHtmlParser
import com.example.saucetracker.feature.desktopbridge.DesktopBridgeServer
import com.example.saucetracker.feature.browser.GalleryBrowserActivity
import com.example.saucetracker.feature.heatmap.HeatmapEngine
import com.example.saucetracker.feature.heatmap.HeatmapViewModel
import com.example.saucetracker.feature.library.detail.RelatedEntryMode
import com.example.saucetracker.feature.library.detail.SelectedEntryRelatedUiState
import com.example.saucetracker.feature.library.detail.EntrySeriesResolver
import com.example.saucetracker.feature.library.detail.filterRelatedEntriesByReadState
import com.example.saucetracker.feature.library.detail.showReadRelatedEntries
import com.example.saucetracker.feature.library.downloads.EntryDownloadController
import com.example.saucetracker.feature.slideshow.GallerySlideshowActivity
import com.example.saucetracker.feature.suggestions.SuggestionsViewModel
import com.example.saucetracker.feature.suggestions.buildSuggestionProfile
import com.example.saucetracker.feature.suggestions.buildSuggestionSearchQuery
import com.example.saucetracker.feature.suggestions.matchesSuggestionFilters
import com.example.saucetracker.feature.suggestions.SuggestionCreatorToken
import com.example.saucetracker.feature.suggestions.SuggestionTagToken
import com.example.saucetracker.feature.suggestions.SuggestionCacheStore
import com.example.saucetracker.feature.subscriptions.SubscriptionSyncUseCase
import com.example.saucetracker.feature.subscriptions.SubscriptionsViewModel

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.saucetracker.core.diagnostics.GitHubMediaSession
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.provider.DocumentsContract
import android.text.Html
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.key
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.saucetracker.core.diagnostics.PerformanceMetrics
import com.example.saucetracker.core.media.BitmapMemoryCache
import com.example.saucetracker.core.media.computeDHash64
import com.example.saucetracker.core.network.HttpClientFactory
import com.example.saucetracker.core.network.HttpClientProfile
import com.example.saucetracker.core.preferences.KEY_ACCENT_MODE
import com.example.saucetracker.core.preferences.KEY_ADAPTIVE_SCROLL_THUMBNAILS
import com.example.saucetracker.core.preferences.KEY_APP_LOCK_BIOMETRIC_ENABLED
import com.example.saucetracker.core.preferences.KEY_APP_LOCK_ENABLED
import com.example.saucetracker.core.preferences.KEY_APPLY_BLOCKED_TAGS_HOME
import com.example.saucetracker.core.preferences.KEY_APPLY_BLOCKED_TAGS_SEARCH
import com.example.saucetracker.core.preferences.KEY_AUTO_BACKUP_TREE_URI
import com.example.saucetracker.core.preferences.KEY_BROWSER_DUPLICATE_CHECK_MODE
import com.example.saucetracker.core.preferences.KEY_CUNNY_MODE_ARMED
import com.example.saucetracker.core.preferences.KEY_DEFAULT_CREATOR_SORT_DIRECTION
import com.example.saucetracker.core.preferences.KEY_DEFAULT_CREATOR_SORT_FIELD
import com.example.saucetracker.core.preferences.KEY_DEFAULT_ENTRY_SORT_DIRECTION
import com.example.saucetracker.core.preferences.KEY_DEFAULT_ENTRY_SORT_FIELD
import com.example.saucetracker.core.preferences.KEY_DEFAULT_TAG_SORT_DIRECTION
import com.example.saucetracker.core.preferences.KEY_DEFAULT_TAG_SORT_FIELD
import com.example.saucetracker.core.preferences.KEY_DESKTOP_BRIDGE_ENABLED
import com.example.saucetracker.core.preferences.KEY_DESKTOP_BRIDGE_PORT
import com.example.saucetracker.core.preferences.KEY_ENTRY_FILTER_CYCLE_ORDER
import com.example.saucetracker.core.preferences.KEY_ENTRY_PIN_PRIORITY
import com.example.saucetracker.core.preferences.KEY_EXPERIMENTAL_DASHBOARD_LONG_PRESS
import com.example.saucetracker.core.preferences.KEY_EXPERIMENTAL_FILTER_STATUS_STRIP
import com.example.saucetracker.core.preferences.KEY_EXPERIMENTAL_LAZY_ENTRY_DETAIL
import com.example.saucetracker.core.preferences.KEY_EXPERIMENTAL_SUBSCRIPTION_INBOX
import com.example.saucetracker.core.preferences.KEY_GALLERY_COLUMNS
import com.example.saucetracker.core.preferences.KEY_HOME_SECTION_ORDER
import com.example.saucetracker.core.preferences.KEY_INCOGNITO_MODE_ENABLED
import com.example.saucetracker.core.preferences.KEY_LEGACY_HOME_UI
import com.example.saucetracker.core.preferences.KEY_PERFORMANCE_OVERLAY_ENABLED
import com.example.saucetracker.core.preferences.KEY_PRELOAD_ON_LAUNCH
import com.example.saucetracker.core.preferences.KEY_PRELOAD_PERCENT
import com.example.saucetracker.core.preferences.KEY_PURE_GALLERY_MODE
import com.example.saucetracker.core.preferences.KEY_SHOW_THUMBNAILS
import com.example.saucetracker.core.preferences.KEY_SUBSCRIPTION_REFRESH_INTERVAL_HOURS
import com.example.saucetracker.core.preferences.KEY_SUGGESTION_HIDDEN_CODES
import com.example.saucetracker.core.preferences.KEY_SUGGESTION_HIDDEN_ENTRIES
import com.example.saucetracker.core.preferences.KEY_SUGGESTION_THEME_STRENGTH
import com.example.saucetracker.core.preferences.KEY_SUGGESTION_WEIGHT_PREFIX
import com.example.saucetracker.core.preferences.KEY_THEME_MODE
import com.example.saucetracker.core.preferences.SaucePreferences
import com.example.saucetracker.core.security.AppLockController
import com.example.saucetracker.core.ui.privacy.privacyObfuscate
import com.example.saucetracker.data.repository.HeatmapRepository
import com.example.saucetracker.data.repository.LibraryRepository
import com.example.saucetracker.data.repository.SubscriptionRepository
import com.example.saucetracker.data.repository.SuggestionsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.roundToInt

private const val APP_LOCK_GRACE_MS = 30_000L
private const val DESKTOP_BRIDGE_DEFAULT_PORT = 17366
private const val SUBSCRIPTION_ROUTE_FETCH_PAGES = 2
private const val THUMB_PRELOAD_MIN_PARALLEL = 4
private const val THUMB_PRELOAD_MAX_PARALLEL = 16
private const val THUMB_PRELOAD_TOP_PRIORITY_COUNT = 48
private const val ENTRY_HEATMAP_CACHE_SOLVER_WIDTH_PX = 1600f
private const val ENTRY_HEATMAP_CACHE_SOLVER_HEIGHT_PX = 2200f
private const val ENTRY_HEATMAP_CACHE_SPACING_PX = 56f
private const val EXPORT_PREFIX = "Sauce exported Date"
private const val EXPORT_FORMAT = "NH_TAGBOOK_EXPORT_V1"
private const val URL_TRAILING_PUNCT = ".,;:!?)]}"
private val EXPORT_FILENAME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.US)
private val IGNORED_SUGGESTION_TAG_NAMES = setOf("translated", "translation")
private val NHENTAI_HOME_PATTERN = Regex("(?i)^(?:https?://)?(?:www\\.)?nhentai\\.net/?$")
private val DIRECT_ROUTE_LINK_PATTERN = Regex(
    "(?i)(?:https?://)?(?:www\\.)?nhentai\\.net/(tag|language|category|parody|character|artist|group)/([^/\\s?#]+)(?:/(?:popular|popular-week|popular-today))?(?:/)?(?:[?#][^\\s]*)?"
)
private val CREATOR_LINK_PATTERN = Regex(
    "(?i)(?:https?://)?(?:www\\.)?nhentai\\.net/(artist|group)/([^/\\s?#]+)(?:/)?(?:[?#][^\\s]*)?"
)
private val CREATOR_NAME_LINE_PATTERN = Regex("^[\\p{L}\\p{N} _.'()\\-]{2,80}$")

private fun parseCreatorSlug(raw: String): String = GalleryHtmlParser.parseCreatorSlug(raw)
private fun parseCreatorLink(raw: String): CreatorLink? = GalleryHtmlParser.parseCreatorLink(raw)
private fun parseTypedCreatorInput(raw: String): Pair<String, String>? =
    GalleryHtmlParser.parseTypedCreatorInput(raw)

private fun parseAmbiguousTwoWordCreatorInput(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isBlank() || parseCode(trimmed) != null) return null
    if (parseCreatorLink(trimmed) != null || parseTypedCreatorInput(trimmed) != null) return null
    if (!CREATOR_NAME_LINE_PATTERN.matches(trimmed)) return null
    return parseCreatorSlug(trimmed).takeIf(::isTwoWordCreatorName)
}

internal fun includeDirectNavigationEntry(
    visibleEntries: List<EntryRow>,
    directTarget: EntryRow?
): List<EntryRow> {
    if (directTarget == null || visibleEntries.any { it.code == directTarget.code }) {
        return visibleEntries
    }
    return visibleEntries + directTarget
}

private fun isTwoWordCreatorName(value: String): Boolean =
    parseCreatorSlug(value).split(Regex("\\s+")).count(String::isNotBlank) == 2

private fun toHyphenatedTwoWordCreatorName(value: String): String {
    val tokens = parseCreatorSlug(value).split(Regex("\\s+")).filter(String::isNotBlank)
    return if (tokens.size == 2) "${tokens[0]}-${tokens[1]}" else value.trim()
}

private fun splitTwoWordCreatorName(value: String): List<String> {
    val tokens = parseCreatorSlug(value).split(Regex("\\s+")).filter(String::isNotBlank)
    return if (tokens.size == 2) tokens else listOf(value.trim()).filter(String::isNotBlank)
}

private fun creatorStrictIdentityKey(raw: String): String = Regex("[\\p{L}\\p{N}]+")
    .findAll(parseCreatorSlug(raw).lowercase(Locale.US))
    .map { it.value }
    .filter(String::isNotBlank)
    .joinToString(" ")
    .trim()

private fun isStrictCreatorNameMatch(input: String, resolvedName: String): Boolean {
    val inputKey = creatorStrictIdentityKey(input)
    return inputKey.isNotBlank() && inputKey == creatorStrictIdentityKey(resolvedName)
}

private fun extractCreatorLinks(text: String): Pair<List<CreatorLink>, String> {
    val creators = mutableListOf<CreatorLink>()
    val seen = linkedSetOf<String>()
    CREATOR_LINK_PATTERN.findAll(text).forEach { match ->
        val parsed = parseCreatorLink(match.value) ?: return@forEach
        val key = "${parsed.type}:${normalizeTagName(parsed.name)}"
        if (seen.add(key)) creators += parsed
    }
    return creators to CREATOR_LINK_PATTERN.replace(text, " ")
}

private fun extractCreatorNameCandidates(text: String): List<String> {
    val names = linkedSetOf<String>()
    text.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isBlank() || line.length > 60) return@forEach
        val lower = line.lowercase(Locale.US)
        if (lower.startsWith("sauce exported date") || lower.startsWith("format:")) return@forEach
        if (parseCode(line) != null || parseCreatorLink(line) != null) return@forEach
        if (!CREATOR_NAME_LINE_PATTERN.matches(line)) return@forEach
        val normalized = parseCreatorSlug(line)
        val tokenCount = normalized.split(Regex("\\s+")).count(String::isNotBlank)
        if (normalized.isNotBlank() && tokenCount in 1..6) names += normalized
    }
    return names.toList()
}

private const val SUGGESTION_VISIBLE_TARGET = 12
private const val SUGGESTION_CANDIDATE_TARGET = 30

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val backupImporter = BackupImporter()
    private val suggestionsViewModel = SuggestionsViewModel()
    private val db = SauceTrackerDatabase(application)
    private val client = NhentaiApiClient()
    private val suggestionApi = SuggestionApiClient()
    private val libraryRepository = LibraryRepository(db)
    private val suggestionsRepository = SuggestionsRepository(suggestionApi)
    private val suggestionCacheStore by lazy { SuggestionCacheStore(prefs) }
    private val subscriptionRepository = SubscriptionRepository(db)
    private val subscriptionSyncUseCase = SubscriptionSyncUseCase(
        subscriptions = subscriptionRepository,
        suggestions = suggestionsRepository,
        galleryClient = client,
        routeFetchPages = SUBSCRIPTION_ROUTE_FETCH_PAGES
    )
    private val subscriptionsViewModel = SubscriptionsViewModel()
    private val heatmapRepository = HeatmapRepository(db)
    private val prefs = SaucePreferences.from(application).raw
    private val preferenceReader = DashboardPreferenceReader(prefs)
    private val appLockController = AppLockController.from(application, APP_LOCK_GRACE_MS)

    var themeMode by mutableStateOf(GitHubMediaSession.themeOverride ?: preferenceReader.loadThemeMode())
        private set
    var accentMode by mutableStateOf(preferenceReader.loadAccentMode())
        private set
    var cunnyModeArmed by mutableStateOf(loadCunnyModeArmed())
        private set
    var showThumbnails by mutableStateOf(preferenceReader.loadShowThumbnails())
        private set
    var adaptiveScrollThumbnails by mutableStateOf(preferenceReader.loadAdaptiveScrollThumbnails())
        private set
    var performanceOverlayEnabled by mutableStateOf(preferenceReader.loadPerformanceOverlayEnabled())
        private set
    var pureGalleryMode by mutableStateOf(preferenceReader.loadPureGalleryMode())
        private set
    var galleryColumns by mutableStateOf(preferenceReader.loadGalleryColumns())
        private set
    var legacyHomeUi by mutableStateOf(preferenceReader.loadLegacyHomeUi())
        private set
    var experimentalLazyEntryDetail by mutableStateOf(preferenceReader.loadExperimentalLazyEntryDetail())
        private set
    var experimentalSubscriptionInbox by mutableStateOf(preferenceReader.loadExperimentalSubscriptionInbox())
        private set
    var experimentalFilterStatusStrip by mutableStateOf(preferenceReader.loadExperimentalFilterStatusStrip())
        private set
    var experimentalDashboardLongPress by mutableStateOf(preferenceReader.loadExperimentalDashboardLongPress())
        private set
    // One deliberate dashboard entrance per app process; normal navigation stays immediate.
    private var dashboardEntranceConsumed = false
    var applyBlockedTagsToHome by mutableStateOf(preferenceReader.loadApplyBlockedTagsToHome())
        private set
    var applyBlockedTagsToSearchTerms by mutableStateOf(preferenceReader.loadApplyBlockedTagsToSearchTerms())
        private set
    var preloadOnLaunch by mutableStateOf(preferenceReader.loadPreloadOnLaunch())
        private set
    var preloadPercent by mutableStateOf(preferenceReader.loadPreloadPercent())
        private set
    var subscriptionRefreshIntervalHours by mutableStateOf(preferenceReader.loadSubscriptionRefreshIntervalHours())
        private set
    var autoBackupTreeUri by mutableStateOf(preferenceReader.loadAutoBackupTreeUri())
        private set
    var galleryDownloadTreeUri by mutableStateOf(loadGalleryDownloadTreeUri(application))
        private set
    var backupThumbnailArchiveEnabled by mutableStateOf(preferenceReader.loadBackupThumbnailArchiveEnabled())
        private set
    var desktopBridgeEnabled by mutableStateOf(preferenceReader.loadDesktopBridgeEnabled())
        private set
    var desktopBridgePort by mutableStateOf(preferenceReader.loadDesktopBridgePort())
        private set
    var desktopBridgeRunning by mutableStateOf(false)
        private set
    var desktopBridgeUrl by mutableStateOf("")
        private set
    var desktopBridgeChallengeCode by mutableStateOf("--")
        private set
    var desktopBridgeScreenBlackout by mutableStateOf(false)
        private set
    var incognitoModeEnabled by mutableStateOf(
        if (GitHubMediaSession.active) {
            GitHubMediaSession.initialIncognitoEnabled
        } else {
            preferenceReader.loadIncognitoMode()
        }
    )
        private set
    var appLockEnabled by mutableStateOf(preferenceReader.loadAppLockEnabled())
        private set
    var appLockBiometricEnabled by mutableStateOf(preferenceReader.loadAppLockBiometricEnabled())
        private set
    var appLocked by mutableStateOf(false)
        private set
    var appLockNonce by mutableStateOf(0L)
        private set
    var pendingOpenSubscriptions by mutableStateOf(false)
        private set
    var incognitoToggleAuthPending by mutableStateOf(false)
        private set
    var incognitoToggleAuthNonce by mutableStateOf(0L)
        private set
    private var appLockGraceUntilMs: Long = appLockController.graceUntilMillis

    var codeInput by mutableStateOf("")
        private set
    var entrySearch by mutableStateOf("")
        private set
    var statusMessage by mutableStateOf("Ready.")
        private set

    var entries by mutableStateOf<List<EntryRow>>(emptyList())
        private set
    var suggestedEntries by mutableStateOf<List<SuggestedEntryRow>>(emptyList())
        private set
    var suggestedImportFlashEpochs by mutableStateOf<Map<Int, Int>>(emptyMap())
        private set
    var suggestedEntriesLoading by mutableStateOf(false)
        private set
    var suggestedEntriesCollapsed by mutableStateOf(true)
        private set
    var suggestedEntriesInfoMessage by mutableStateOf<String?>(null)
        private set
    var suggestionMode by mutableStateOf(SuggestionMode.MIXED)
        private set
    var suggestionCategoryWeights by mutableStateOf(preferenceReader.loadSuggestionCategoryWeights())
        private set
    var suggestionThemeStrength by mutableStateOf(preferenceReader.loadSuggestionThemeStrength())
        private set
    var tags by mutableStateOf<List<TagCountRow>>(emptyList())
        private set
    var popularTags by mutableStateOf<List<PopularTagRow>>(emptyList())
        private set
    var entryLayoutPreviewSamples by mutableStateOf<List<EntryRow>>(emptyList())
        private set
    var creators by mutableStateOf<List<CreatorRow>>(emptyList())
        private set
    var subscriptions: List<SubscriptionRow>
        get() = subscriptionsViewModel.uiState.subscriptions
        private set(value) { subscriptionsViewModel.update { it.copy(subscriptions = value) } }
    var subscriptionEvents: List<SubscriptionEventRow>
        get() = subscriptionsViewModel.uiState.events
        private set(value) { subscriptionsViewModel.update { it.copy(events = value) } }
    val visibleSubscriptionEvents: List<SubscriptionEventRow> by derivedStateOf {
        filteredSubscriptionEvents()
    }
    var subscriptionRefreshRunning: Boolean
        get() = subscriptionsViewModel.uiState.refreshRunning
        private set(value) { subscriptionsViewModel.update { it.copy(refreshRunning = value) } }
    var savedStats by mutableStateOf(SavedStats(0, 0, 0, 0))
        private set
    var readAnalytics by mutableStateOf(ReadAnalyticsSnapshot())
        private set
    var readAnalyticsLoading by mutableStateOf(false)
        private set
    private var readAnalyticsLoaded = false
    private val heatmapViewModel = HeatmapViewModel()
    val heatmapUiState get() = heatmapViewModel.uiState
    var tagGraphSnapshot: TagGraphSnapshot?
        get() = heatmapUiState.snapshot
        private set(value) { heatmapViewModel.update { it.copy(snapshot = value) } }
    var tagGraphLoading: Boolean
        get() = heatmapUiState.loading
        private set(value) { heatmapViewModel.update { it.copy(loading = value) } }
    var tagGraphErrorMessage: String?
        get() = heatmapUiState.errorMessage
        private set(value) { heatmapViewModel.update { it.copy(errorMessage = value) } }
    var entryHeatmapCacheRecalculationRunning: Boolean
        get() = heatmapUiState.recalculationRunning
        private set(value) { heatmapViewModel.update { it.copy(recalculationRunning = value) } }
    var entryHeatmapCacheStatusText: String
        get() = heatmapUiState.cacheStatusText
        private set(value) { heatmapViewModel.update { it.copy(cacheStatusText = value) } }
    var entryHeatmapCacheProgressLabel: String
        get() = heatmapUiState.progressLabel
        private set(value) { heatmapViewModel.update { it.copy(progressLabel = value) } }
    var entryHeatmapCacheProgressFraction: Float?
        get() = heatmapUiState.progressFraction
        private set(value) { heatmapViewModel.update { it.copy(progressFraction = value) } }
    var entryHeatmapCacheCompletionSummary: EntryHeatmapRecalculationSummary?
        get() = heatmapUiState.completionSummary
        private set(value) { heatmapViewModel.update { it.copy(completionSummary = value) } }
    var entryHeatmapCacheNonce: Long
        get() = heatmapUiState.cacheNonce
        private set(value) { heatmapViewModel.update { it.copy(cacheNonce = value) } }
    private var tagGraphLoaded = false
    private var tagGraphLoadJob: Job? = null
    private var tagGraphThumbnailPreloadJob: Job? = null
    private var selectedDetailLoadJob: Job? = null
    private var selectedEntryRelatedJob: Job? = null
    var creatorEntriesById by mutableStateOf<Map<Long, List<CreatorEntryRow>>>(emptyMap())
        private set

    val activeTagFilterIds = mutableStateListOf<Long>()
    val expandedCreatorIds = mutableStateListOf<Long>()

    var selectedCode by mutableStateOf<Int?>(null)
        private set
    var selectedSummary by mutableStateOf<EntryRow?>(null)
        private set
    var selectedDetail by mutableStateOf<EntryDetail?>(null)
        private set
    var selectedDetailLoading by mutableStateOf(false)
        private set
    var selectedSeriesNeighbors by mutableStateOf(SeriesNeighbors())
        private set
    internal var selectedEntryRelatedUiState by mutableStateOf(SelectedEntryRelatedUiState())
        private set
    internal var selectedRelatedEntryMode by mutableStateOf<RelatedEntryMode?>(null)
        private set
    var pendingEntryJumpCode by mutableStateOf<Int?>(null)
        private set
    var pendingCreatorJumpId by mutableStateOf<Long?>(null)
        private set

    var entryReadFilterCycleOrder by mutableStateOf(preferenceReader.loadEntryReadFilterCycleOrder())
        private set
    var homeSectionOrder by mutableStateOf(preferenceReader.loadHomeSectionOrder())
        private set
    var defaultEntrySortField by mutableStateOf(preferenceReader.loadDefaultEntrySortField())
        private set
    var defaultEntrySortDirection by mutableStateOf(preferenceReader.loadDefaultEntrySortDirection(defaultEntrySortField))
        private set
    var defaultTagSortField by mutableStateOf(preferenceReader.loadDefaultTagSortField())
        private set
    var defaultTagSortDirection by mutableStateOf(preferenceReader.loadDefaultTagSortDirection(defaultTagSortField))
        private set
    var defaultCreatorSortField by mutableStateOf(preferenceReader.loadDefaultCreatorSortField())
        private set
    var defaultCreatorSortDirection by mutableStateOf(preferenceReader.loadDefaultCreatorSortDirection(defaultCreatorSortField))
        private set
    var defaultBrowserDuplicateCheckMode by mutableStateOf(preferenceReader.loadDefaultBrowserDuplicateCheckMode())
        private set

    var sortField by mutableStateOf<EntrySortField?>(defaultEntrySortField)
        private set
    var sortDirection by mutableStateOf(defaultEntrySortDirection)
        private set
    var entryReadFilter by mutableStateOf(preferenceReader.initialEntryReadFilterForCycle(entryReadFilterCycleOrder))
        private set
    var entryPinPriorityEnabled by mutableStateOf(preferenceReader.loadEntryPinPriorityEnabled())
        private set

    var tagSortField by mutableStateOf(defaultTagSortField)
        private set
    var tagSortDirection by mutableStateOf(defaultTagSortDirection)
        private set
    var creatorSortField by mutableStateOf(defaultCreatorSortField)
        private set
    var creatorSortDirection by mutableStateOf(defaultCreatorSortDirection)
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
    var subscriptionsCardCollapsed by mutableStateOf(true)
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
    var backupProgressState by mutableStateOf<BackupProgressState?>(null)
        private set
    val entryDownloadProgressState: EntryDownloadProgressState?
        get() = entryDownloadController.progressState
    val entryDownloadBatchMode: EntryDownloadBatchMode?
        get() = entryDownloadController.batchMode
    val entryDownloadBatchSelectedCodes: Set<Int>
        get() = entryDownloadController.batchSelectedCodes
    val entryDownloadBatchProgressState: EntryDownloadBatchProgressState?
        get() = entryDownloadController.batchProgressState
    val downloadedGalleryNonce: Long
        get() = entryDownloadController.downloadedGalleryNonce

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
    private var subscriptionAutoRefreshAttempted = false
    private var seriesNeighborsJob: Job? = null
    private data class SelectedEntryRelatedCacheKey(
        val code: Int,
        val showReadEntries: Boolean
    )

    private val selectedEntryRelatedCache = object : LinkedHashMap<SelectedEntryRelatedCacheKey, SelectedEntryRelatedUiState>(48, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<SelectedEntryRelatedCacheKey, SelectedEntryRelatedUiState>?
        ): Boolean {
            return size > 48
        }
    }
    private val loadingCreatorIds = mutableStateListOf<Long>()
    private var creatorEntryFilterKey: String = ""
    private val autoBackupInFlight = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val entryDownloadController by lazy {
        EntryDownloadController(
            appContext = getApplication<Application>().applicationContext,
            scope = viewModelScope,
            libraryRepository = libraryRepository,
            mainHandler = mainHandler,
            shouldReloadDownloadedEntries = { entryReadFilter == EntryReadFilterMode.DOWNLOADED },
            reloadEntries = { loadEntries(selectedCode) },
            onStatus = ::setStatus,
            onError = { message -> errorDialogMessage = message },
            onInfo = { message -> infoDialogMessage = message }
        )
    }
    private val inAppBackActions = ArrayDeque<InAppBackAction>()
    private var applyingInAppBackAction = false
    private var lastAutoBackupAttemptMs: Long = 0L
    private val desktopBridgeServer by lazy {
        DesktopBridgeServer(
            appContext = getApplication<Application>().applicationContext,
            db = db,
            client = client,
            onDataChanged = {
                mainHandler.post { refreshAll(selectedCode) }
            },
            onScreenBlackoutChanged = { enabled ->
                mainHandler.post { desktopBridgeScreenBlackout = enabled }
            },
            onAccentModeChanged = { modeName ->
                mainHandler.post { chooseAccentModeFromBridge(modeName) }
            },
            onChallengeCodeChanged = { code ->
                mainHandler.post { desktopBridgeChallengeCode = code.ifBlank { "--" } }
            },
            currentAccentMode = { accentMode.name }
        )
    }
    private var pendingBrowserRatingCode: Int? = null
    private var awaitingBrowserRatingPrompt: Boolean = false
    private var pendingBrowserRatingWasRead: Boolean = false
    private var pendingIncomingShareText: String? = null
    // Session-scoped "NEW" markers (in-memory only; reset on process restart).
    private val sessionNewEntryCodes = mutableStateMapOf<Int, Boolean>()
    private val sessionKnownEntryCodes = linkedSetOf<Int>()
    private val hiddenSuggestedCodes = mutableStateMapOf<Int, Boolean>()
    private val hiddenSuggestedAtMillis = mutableStateMapOf<Int, Long>()
    private val hiddenSuggestedThumbnailUrls = mutableStateMapOf<Int, String>()
    private val hiddenSuggestedThumbnailLoading = mutableStateMapOf<Int, Boolean>()
    private val suggestedOverflowEntries = mutableListOf<SuggestedEntryRow>()
    private val suggestionDuplicateHintCache = mutableMapOf<Int, DuplicateHint?>()
    private val suggestionGalleryCache = mutableMapOf<Int, GalleryData>()
    private var suggestionsRefreshJob: Job? = null
    private var suggestionsRefreshGeneration: Long = 0L
    private var suggestionDuplicateHintCacheSeedVersion: Int = 0
    private var suggestionDuplicateSeedIndex = buildLocalDuplicateSeedIndex(emptyList())
    private var hiddenSuggestionCodesCacheRaw: String = ""
    private var hiddenSuggestionEntriesCacheRaw: String = ""
    private val sessionExcludedSuggestionCodes = linkedSetOf<Int>()
    private var sessionEntryTrackingInitialized = false
    var popularTagsFetchInProgress by mutableStateOf(false)
        private set
    @Volatile
    private var batchCancelRequested: Boolean = false
    @Volatile
    private var suggestionsRefreshRunning: Boolean = false

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
        entryDownloadController.refreshDownloadedCodes()
        reloadSubscriptionsState()
        updateEntryHeatmapCacheStatus(null)
        loadHiddenSuggestionCodesIntoMemory()
        synchronized(suggestionGalleryCache) {
            suggestionGalleryCache.putAll(suggestionCacheStore.loadGalleryMetadata())
        }
        // Always start a fresh app process with desktop bridge disabled.
        // Users must explicitly enable it each session.
        desktopBridgeEnabled = false
        desktopBridgeRunning = false
        desktopBridgeUrl = ""
        desktopBridgeChallengeCode = "--"
        desktopBridgeScreenBlackout = false
        prefs.edit().putBoolean(KEY_DESKTOP_BRIDGE_ENABLED, false).apply()
        viewModelScope.launch {
            maybeAutoRefreshSubscriptions()
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
        suggestedOverflowEntries.clear()
        if (!suggestedEntriesCollapsed) {
            suggestedEntries = emptyList()
            suggestedEntriesInfoMessage = "Search/filter changed. Refresh suggested entries."
        }
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
        if (!suggestedEntriesCollapsed) {
            refreshSuggestedEntries(force = true)
        }
        setStatus("Search everything cleared.")
    }

    fun setStatus(message: String) {
        statusMessage = message
    }

    fun isSessionNewEntry(code: Int): Boolean {
        if (code <= 0) return false
        return sessionNewEntryCodes[code] == true
    }

    private fun registerSessionNewEntryCode(code: Int) {
        if (code <= 0) return
        sessionNewEntryCodes[code] = true
        sessionKnownEntryCodes += code
    }

    private fun forgetSessionEntryCode(code: Int) {
        if (code <= 0) return
        sessionNewEntryCodes.remove(code)
        sessionKnownEntryCodes.remove(code)
    }

    private fun clearSessionNewEntryTracking() {
        sessionNewEntryCodes.clear()
        sessionKnownEntryCodes.clear()
        sessionEntryTrackingInitialized = false
    }

    private fun reconcileSessionEntryTracking(rows: List<EntryRow>) {
        if (!sessionEntryTrackingInitialized) {
            sessionKnownEntryCodes.clear()
            // Seed from the full library so filtered views do not mark old entries as NEW.
            libraryRepository.allEntryCodes()
                .asSequence()
                .filter { it > 0 }
                .forEach { code -> sessionKnownEntryCodes += code }
            sessionEntryTrackingInitialized = true
        }
        rows.forEach { row ->
            val code = row.code
            if (code <= 0) return@forEach
            if (!sessionKnownEntryCodes.contains(code)) {
                registerSessionNewEntryCode(code)
            } else {
                sessionKnownEntryCodes += code
            }
        }
    }

    fun hideSuggestedEntry(code: Int, thumbnailUrl: String = "") {
        if (code <= 0) return
        hiddenSuggestedCodes.remove(code)
        hiddenSuggestedAtMillis.remove(code)
        hiddenSuggestedCodes[code] = true
        hiddenSuggestedAtMillis[code] = System.currentTimeMillis()
        val resolvedThumbnailUrl = thumbnailUrl.trim().ifBlank {
            suggestedEntries.firstOrNull { it.code == code }?.thumbnailUrl?.trim().orEmpty()
        }
        if (resolvedThumbnailUrl.isNotBlank()) {
            hiddenSuggestedThumbnailUrls[code] = resolvedThumbnailUrl
        }
        persistHiddenSuggestionCodes()
        suggestedEntries = suggestedEntries.filterNot { it.code == code }
        suggestedOverflowEntries.removeAll { it.code == code }
        if (hiddenSuggestedThumbnailUrls[code].isNullOrBlank()) {
            prefetchHiddenSuggestedThumbnail(code)
        }
        setStatus("Suggestion $code hidden.")
    }

    private fun takeSuggestedOverflowReplacement(
        existingCodes: Set<Int>,
        importedCodes: Set<Int>
    ): SuggestedEntryRow? {
        while (suggestedOverflowEntries.isNotEmpty()) {
            val candidate = suggestedOverflowEntries.removeAt(0)
            val candidateCode = candidate.code
            if (candidateCode <= 0) continue
            if (candidateCode in existingCodes) continue
            if (candidateCode in importedCodes) continue
            if (candidateCode in hiddenSuggestedCodes) continue
            if (candidateCode in sessionExcludedSuggestionCodes) continue
            return candidate
        }
        return null
    }

    private fun fetchSuggestedGalleryCached(code: Int): GalleryData? {
        if (code <= 0) return null
        synchronized(suggestionGalleryCache) {
            suggestionGalleryCache[code]?.let {
                PerformanceMetrics.recordSuggestionMetadataCacheHit()
                return it
            }
        }
        PerformanceMetrics.recordSuggestionMetadataNetworkFetch()
        val fetched = runCatching { client.fetchGallery(code) }.getOrNull() ?: return null
        synchronized(suggestionGalleryCache) {
            suggestionGalleryCache.putIfAbsent(code, fetched)
            return suggestionGalleryCache[code]
        }
    }

    private fun populateSuggestionDuplicateHintsAsync(rows: List<SuggestedEntryRow>) {
        val unresolvedCodes = rows
            .asSequence()
            .filter { it.code > 0 && it.duplicateHint == null }
            .map { it.code }
            .distinct()
            .toList()
        if (unresolvedCodes.isEmpty()) return
        val seedIndexSnapshot = suggestionDuplicateSeedIndex
        if (seedIndexSnapshot.allSeeds.isEmpty()) return

        viewModelScope.launch {
            val duplicateStartedAt = android.os.SystemClock.elapsedRealtime()
            val resolved = withContext(Dispatchers.IO) {
                val parallelism = Runtime.getRuntime().availableProcessors().coerceIn(2, 6)
                coroutineScope {
                    unresolvedCodes
                        .chunked(parallelism)
                        .flatMap { batch ->
                            batch.map { code ->
                                async {
                                    val cached = synchronized(suggestionDuplicateHintCache) {
                                        if (suggestionDuplicateHintCache.containsKey(code)) {
                                            suggestionDuplicateHintCache[code]
                                        } else {
                                            null
                                        }
                                    }
                                    if (cached != null || synchronized(suggestionDuplicateHintCache) { suggestionDuplicateHintCache.containsKey(code) }) {
                                        return@async code to cached
                                    }
                                    val gallery = fetchSuggestedGalleryCached(code) ?: return@async code to null
                                    val computed = findLikelyDuplicateHint(
                                        candidateCode = gallery.code,
                                        candidateTitle = listOf(gallery.title, gallery.subtitle)
                                            .filter { it.isNotBlank() }
                                            .joinToString(" "),
                                        candidateNumPages = gallery.numPages,
                                        candidateUploadDate = gallery.uploadDate,
                                        candidateMediaId = gallery.mediaId,
                                        candidateArtistKeys = gallery.tags
                                            .asSequence()
                                            .filter { it.type.equals("artist", ignoreCase = true) }
                                            .map { normalizeTagName(it.name) }
                                            .filter { it.isNotBlank() }
                                            .toSet(),
                                        candidateGroupKeys = gallery.tags
                                            .asSequence()
                                            .filter { it.type.equals("group", ignoreCase = true) }
                                            .map { normalizeTagName(it.name) }
                                            .filter { it.isNotBlank() }
                                            .toSet(),
                                        candidateTagKeys = gallery.tags
                                            .asSequence()
                                            .filterNot {
                                                it.type.equals("artist", ignoreCase = true) ||
                                                    it.type.equals("group", ignoreCase = true)
                                            }
                                            .map { normalizeTagName(it.name) }
                                            .filter { it.isNotBlank() }
                                            .toSet(),
                                        candidateThumbnailUrl = buildGalleryCoverThumbnailUrl(gallery),
                                        localSeeds = collectLocalDuplicateCandidateSeeds(
                                            index = seedIndexSnapshot,
                                            candidateCode = gallery.code,
                                            candidateTitle = listOf(gallery.title, gallery.subtitle)
                                                .filter { it.isNotBlank() }
                                                .joinToString(" "),
                                            candidateNumPages = gallery.numPages,
                                            candidateUploadDate = gallery.uploadDate,
                                            candidateMediaId = gallery.mediaId
                                        )
                                    )
                                    synchronized(suggestionDuplicateHintCache) {
                                        if (!suggestionDuplicateHintCache.containsKey(code)) {
                                            suggestionDuplicateHintCache[code] = computed
                                        }
                                        code to suggestionDuplicateHintCache[code]
                                    }
                                }
                            }.awaitAll()
                        }
                }.toMap()
            }
            if (resolved.isEmpty()) return@launch
            PerformanceMetrics.recordSuggestionDuplicateMillis(
                android.os.SystemClock.elapsedRealtime() - duplicateStartedAt
            )
            suggestedEntries = suggestedEntries.map { row ->
                if (row.duplicateHint != null) row else resolved[row.code]?.let { row.copy(duplicateHint = it) } ?: row
            }
            if (suggestedOverflowEntries.isNotEmpty()) {
                val updatedOverflow = suggestedOverflowEntries.map { row ->
                    if (row.duplicateHint != null) row else resolved[row.code]?.let { row.copy(duplicateHint = it) } ?: row
                }
                suggestedOverflowEntries.clear()
                suggestedOverflowEntries.addAll(updatedOverflow)
            }
        }
    }

    fun skipSuggestedEntry(code: Int) {
        if (code <= 0) return
        if (incognitoModeEnabled) {
            setStatus("Skip is disabled in incognito mode.")
            return
        }
        if (suggestedEntries.none { it.code == code }) return
        sessionExcludedSuggestionCodes += code

        val remaining = suggestedEntries.filterNot { it.code == code }.toMutableList()
        val existingCodes = remaining.asSequence().map { it.code }.toMutableSet()
        val importedCodes = libraryRepository.allEntryCodes().toSet()
        val replacement = takeSuggestedOverflowReplacement(existingCodes, importedCodes)
        if (replacement != null) {
            remaining += replacement
            if (replacement.duplicateHint == null) {
                populateSuggestionDuplicateHintsAsync(listOf(replacement))
            }
        }
        suggestedEntries = remaining
        setStatus(
            if (replacement != null) {
                "Skipped suggestion $code."
            } else {
                "Skipped suggestion $code. No queued replacement was available."
            }
        )
    }

    fun hiddenSuggestedEntries(): List<Int> {
        syncHiddenSuggestionCodesFromPrefs()
        return hiddenSuggestedCodes.keys
            .sortedWith(
                compareByDescending<Int> { hiddenSuggestedAtMillis[it] ?: Long.MIN_VALUE }
                    .thenByDescending { it }
            )
    }

    fun hiddenSuggestedAtLabel(code: Int): String {
        if (code <= 0) return ""
        syncHiddenSuggestionCodesFromPrefs()
        val hiddenAtMillis = hiddenSuggestedAtMillis[code] ?: return ""
        return runCatching {
            Instant.ofEpochMilli(hiddenAtMillis)
                .atOffset(ZoneOffset.UTC)
                .toLocalDateTime()
                .format(UTC_TIMESTAMP_FORMAT)
        }.getOrDefault("")
    }

    fun unhideSuggestedEntry(code: Int) {
        if (code <= 0) return
        if (hiddenSuggestedCodes.remove(code) != null) {
            hiddenSuggestedAtMillis.remove(code)
            hiddenSuggestedThumbnailUrls.remove(code)
            hiddenSuggestedThumbnailLoading.remove(code)
            persistHiddenSuggestionCodes()
            setStatus("Suggestion $code restored.")
            if (!suggestedEntriesCollapsed) {
                refreshSuggestedEntries(force = true)
            }
        }
    }

    fun clearHiddenSuggestedEntries() {
        if (hiddenSuggestedCodes.isEmpty()) return
        hiddenSuggestedCodes.clear()
        hiddenSuggestedAtMillis.clear()
        hiddenSuggestedThumbnailUrls.clear()
        hiddenSuggestedThumbnailLoading.clear()
        persistHiddenSuggestionCodes()
        setStatus("Cleared hidden suggestions.")
        if (!suggestedEntriesCollapsed) {
            refreshSuggestedEntries(force = true)
        }
    }

    private fun applyImportedHiddenSuggestedCodes(codes: Set<Int>) {
        val normalized = codes
            .asSequence()
            .filter { it > 0 }
            .distinct()
            .toList()
        hiddenSuggestedCodes.clear()
        hiddenSuggestedAtMillis.clear()
        hiddenSuggestedThumbnailUrls.clear()
        hiddenSuggestedThumbnailLoading.clear()
        val baseMs = 1L
        normalized.forEachIndexed { index, code ->
            hiddenSuggestedCodes[code] = true
            hiddenSuggestedAtMillis[code] = baseMs + index
        }
        persistHiddenSuggestionCodes()
        if (suggestedEntries.isNotEmpty()) {
            suggestedEntries = suggestedEntries.filterNot { it.code in hiddenSuggestedCodes }
        }
        if (suggestedOverflowEntries.isNotEmpty()) {
            suggestedOverflowEntries.removeAll { it.code in hiddenSuggestedCodes }
        }
    }

    private fun applyImportedHiddenSuggestedEntries(entries: List<HiddenSuggestedEntryState>) {
        val normalized = entries
            .asSequence()
            .filter { it.code > 0 }
            .sortedBy { it.hiddenAtMillis }
            .distinctBy { it.code }
            .toList()
        hiddenSuggestedCodes.clear()
        hiddenSuggestedAtMillis.clear()
        hiddenSuggestedThumbnailUrls.clear()
        hiddenSuggestedThumbnailLoading.clear()
        normalized.forEach { entry ->
            hiddenSuggestedCodes[entry.code] = true
            hiddenSuggestedAtMillis[entry.code] = entry.hiddenAtMillis.coerceAtLeast(1L)
        }
        persistHiddenSuggestionCodes()
        if (suggestedEntries.isNotEmpty()) {
            suggestedEntries = suggestedEntries.filterNot { it.code in hiddenSuggestedCodes }
        }
        if (suggestedOverflowEntries.isNotEmpty()) {
            suggestedOverflowEntries.removeAll { it.code in hiddenSuggestedCodes }
        }
    }

    private fun applyImportedSuggestionCategoryWeights(
        imported: Map<SuggestionWeightCategory, Float>
    ) {
        suggestionCategoryWeights = preferenceReader.defaultSuggestionCategoryWeights().toMutableMap().apply {
            imported.forEach { (category, value) ->
                this[category] = value.coerceIn(0f, 2f)
            }
        }
        persistSuggestionCategoryWeights()
    }

    private fun applyImportedEntryPinPriority(enabled: Boolean) {
        entryPinPriorityEnabled = enabled
        prefs.edit().putBoolean(KEY_ENTRY_PIN_PRIORITY, entryPinPriorityEnabled).apply()
    }

    private fun loadHiddenSuggestionCodesIntoMemory() {
        val rawCodes = prefs.getString(KEY_SUGGESTION_HIDDEN_CODES, "").orEmpty()
        val rawEntries = prefs.getString(KEY_SUGGESTION_HIDDEN_ENTRIES, "").orEmpty()
        hiddenSuggestionCodesCacheRaw = rawCodes
        hiddenSuggestionEntriesCacheRaw = rawEntries
        hydrateHiddenSuggestionStateFromRaw(
            rawCodes = rawCodes,
            rawEntries = rawEntries
        )
    }

    private fun syncHiddenSuggestionCodesFromPrefs() {
        val rawCodes = prefs.getString(KEY_SUGGESTION_HIDDEN_CODES, "").orEmpty()
        val rawEntries = prefs.getString(KEY_SUGGESTION_HIDDEN_ENTRIES, "").orEmpty()
        if (rawCodes == hiddenSuggestionCodesCacheRaw && rawEntries == hiddenSuggestionEntriesCacheRaw) return
        hiddenSuggestionCodesCacheRaw = rawCodes
        hiddenSuggestionEntriesCacheRaw = rawEntries
        hydrateHiddenSuggestionStateFromRaw(
            rawCodes = rawCodes,
            rawEntries = rawEntries
        )
    }

    private fun hydrateHiddenSuggestionStateFromRaw(
        rawCodes: String,
        rawEntries: String
    ) {
        val codesOrdered = BackupSnapshotExport.parseHiddenSuggestionCodeList(rawCodes)
        val entriesByCode = BackupSnapshotExport.parseHiddenSuggestionEntries(rawEntries, backupImporter)
            .associateBy { it.code }

        hiddenSuggestedCodes.clear()
        hiddenSuggestedAtMillis.clear()
        val fallbackBase = 1L
        codesOrdered.forEachIndexed { index, code ->
            if (code <= 0) return@forEachIndexed
            hiddenSuggestedCodes[code] = true
            val imported = entriesByCode[code]?.hiddenAtMillis ?: 0L
            val resolved = if (imported > 0L) imported else fallbackBase + index
            hiddenSuggestedAtMillis[code] = resolved
        }
        hiddenSuggestedThumbnailUrls.keys
            .filterNot { hiddenSuggestedCodes.containsKey(it) }
            .forEach { hiddenSuggestedThumbnailUrls.remove(it) }
        hiddenSuggestedThumbnailLoading.keys
            .filterNot { hiddenSuggestedCodes.containsKey(it) }
            .forEach { hiddenSuggestedThumbnailLoading.remove(it) }
        if (suggestedEntries.isNotEmpty()) {
            suggestedEntries.forEach { row ->
                if (row.code in hiddenSuggestedCodes && row.thumbnailUrl.isNotBlank()) {
                    hiddenSuggestedThumbnailUrls[row.code] = row.thumbnailUrl
                }
            }
            suggestedEntries = suggestedEntries.filterNot { it.code in hiddenSuggestedCodes }
        }
    }

    private fun hiddenSuggestionCodesSnapshot(): Set<Int> {
        syncHiddenSuggestionCodesFromPrefs()
        return hiddenSuggestedCodes.keys.toSet()
    }

    fun hiddenSuggestedThumbnailUrl(code: Int): String {
        if (code <= 0) return ""
        syncHiddenSuggestionCodesFromPrefs()
        return hiddenSuggestedThumbnailUrls[code].orEmpty()
    }

    fun prefetchHiddenSuggestedThumbnail(code: Int) {
        if (code <= 0) return
        syncHiddenSuggestionCodesFromPrefs()
        if (!hiddenSuggestedCodes.containsKey(code)) return
        if (hiddenSuggestedThumbnailUrls[code].orEmpty().isNotBlank()) return
        if (hiddenSuggestedThumbnailLoading[code] == true) return
        hiddenSuggestedThumbnailLoading[code] = true
        viewModelScope.launch {
            try {
                val resolved = withContext(Dispatchers.IO) {
                    val local = libraryRepository.entryDetail(code)
                    val localUrl = local?.thumbnailUrl?.trim().orEmpty().ifBlank {
                        if (local != null && local.mediaId > 0L) {
                            buildThumbnailUrl(local.mediaId, local.coverExt)
                        } else {
                            ""
                        }
                    }
                    if (localUrl.isNotBlank()) {
                        return@withContext localUrl
                    }
                    val fetched = runCatching { client.fetchGallery(code) }.getOrNull()
                    if (fetched != null && fetched.mediaId > 0L) {
                        buildThumbnailUrl(fetched.mediaId, fetched.coverExt)
                    } else {
                        ""
                    }
                }
                if (resolved.isNotBlank() && hiddenSuggestedCodes.containsKey(code)) {
                    hiddenSuggestedThumbnailUrls[code] = resolved
                }
            } finally {
                hiddenSuggestedThumbnailLoading.remove(code)
            }
        }
    }

    fun queueIncomingShareText(sharedText: String) {
        val normalized = sharedText.trim()
        if (normalized.isBlank()) return
        pendingIncomingShareText = normalized
        enforceLockIfRequiredNow()
        consumePendingShareTextIfUnlocked()
    }

    private fun isLockRequiredNow(): Boolean {
        return appLockController.shouldLock(appLockEnabled)
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

    private fun chooseAccentModeFromBridge(modeName: String) {
        val resolved = AccentMode.entries.firstOrNull { it.name.equals(modeName.trim(), ignoreCase = true) } ?: return
        if (accentMode == resolved) return
        accentMode = resolved
        prefs.edit().putString(KEY_ACCENT_MODE, resolved.name).apply()
        setStatus("Accent mode synced from desktop bridge.")
    }

    fun setOrChangeAppLockPin(pinInput: String, enableIfDisabled: Boolean = true): String? {
        appLockController.storePin(pinInput)?.let { return it }
        val editor = prefs.edit()

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
        appLockNonce = System.currentTimeMillis()
        appLockGraceUntilMs = appLockController.clearGrace()
        appLockController.clearCredentials()
        prefs.edit()
            .putBoolean(KEY_APP_LOCK_ENABLED, false)
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
        if (!appLockController.verifyPin(pinInput)) return false
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
        prefs.edit().putBoolean(KEY_INCOGNITO_MODE_ENABLED, incognitoModeEnabled).apply()
        val bridgeWasRunning = desktopBridgeRunning
        if (incognitoModeEnabled && bridgeWasRunning) {
            stopDesktopBridge(reportStatus = false)
        }
        setStatus(
            if (incognitoModeEnabled) {
                if (bridgeWasRunning) {
                    "Incognito privacy mode enabled. Desktop bridge stopped."
                } else {
                    "Incognito privacy mode enabled."
                }
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

    fun cancelIncognitoToggleAuth() {
        if (!incognitoToggleAuthPending) return
        incognitoToggleAuthPending = false
        val now = System.currentTimeMillis()
        setAppLockGraceUntil(now + APP_LOCK_GRACE_MS)
        appLocked = false
        appLockNonce = now
        consumePendingShareTextIfUnlocked()
        setStatus("Incognito mode change cancelled.")
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

    val cunnyModeActive: Boolean
        get() = cunnyModeArmed && isLoliconTagFilterActive()

    private fun loadCunnyModeArmed(): Boolean {
        return prefs.getBoolean(KEY_CUNNY_MODE_ARMED, false)
    }

    private fun isLoliconTagFilterActive(): Boolean {
        return activeTagFilterNames().any { normalizeTagName(it) == "lolicon" }
    }

    fun onHeaderTitleDoubleTap() {
        if (isLoliconTagFilterActive()) {
            val next = !cunnyModeArmed
            cunnyModeArmed = next
            prefs.edit().putBoolean(KEY_CUNNY_MODE_ARMED, next).apply()
            setStatus(
                if (next) {
                    "Cunny mode armed."
                } else {
                    "Cunny mode disabled."
                }
            )
        } else {
            toggleIncognitoMode()
        }
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

    fun toggleAdaptiveScrollThumbnails() {
        adaptiveScrollThumbnails = !adaptiveScrollThumbnails
        prefs.edit().putBoolean(KEY_ADAPTIVE_SCROLL_THUMBNAILS, adaptiveScrollThumbnails).apply()
        setStatus(
            if (adaptiveScrollThumbnails) {
                "Reduced thumbnails while scrolling enabled."
            } else {
                "Reduced thumbnails while scrolling disabled."
            }
        )
    }

    fun togglePerformanceOverlay() {
        performanceOverlayEnabled = !performanceOverlayEnabled
        prefs.edit().putBoolean(KEY_PERFORMANCE_OVERLAY_ENABLED, performanceOverlayEnabled).apply()
        setStatus(
            if (performanceOverlayEnabled) {
                "Performance overlay enabled."
            } else {
                "Performance overlay disabled."
            }
        )
    }

    fun toggleLegacyHomeUi() {
        legacyHomeUi = !legacyHomeUi
        prefs.edit().putBoolean(KEY_LEGACY_HOME_UI, legacyHomeUi).apply()
        setStatus(if (legacyHomeUi) "Legacy home UI enabled." else "Dashboard home UI enabled.")
    }

    fun toggleExperimentalLazyEntryDetail() {
        experimentalLazyEntryDetail = !experimentalLazyEntryDetail
        prefs.edit().putBoolean(KEY_EXPERIMENTAL_LAZY_ENTRY_DETAIL, experimentalLazyEntryDetail).apply()
        setStatus(if (experimentalLazyEntryDetail) "Experimental lazy entry detail enabled." else "Experimental lazy entry detail disabled.")
    }

    fun toggleExperimentalSubscriptionInbox() {
        experimentalSubscriptionInbox = !experimentalSubscriptionInbox
        prefs.edit().putBoolean(KEY_EXPERIMENTAL_SUBSCRIPTION_INBOX, experimentalSubscriptionInbox).apply()
        setStatus(if (experimentalSubscriptionInbox) "Experimental subscription inbox enabled." else "Experimental subscription inbox disabled.")
    }

    fun toggleExperimentalFilterStatusStrip() {
        experimentalFilterStatusStrip = !experimentalFilterStatusStrip
        prefs.edit().putBoolean(KEY_EXPERIMENTAL_FILTER_STATUS_STRIP, experimentalFilterStatusStrip).apply()
        setStatus(if (experimentalFilterStatusStrip) "Experimental filter strip enabled." else "Experimental filter strip disabled.")
    }

    fun toggleExperimentalDashboardLongPress() {
        experimentalDashboardLongPress = !experimentalDashboardLongPress
        prefs.edit().putBoolean(KEY_EXPERIMENTAL_DASHBOARD_LONG_PRESS, experimentalDashboardLongPress).apply()
        setStatus(if (experimentalDashboardLongPress) "Experimental dashboard long-press shortcuts enabled." else "Experimental dashboard long-press shortcuts disabled.")
    }

    fun consumeDashboardEntrance(): Boolean {
        if (dashboardEntranceConsumed) return false
        dashboardEntranceConsumed = true
        return true
    }

    fun expandEntriesSection() {
        entriesCardCollapsed = false
    }

    fun expandTagsSection() {
        tagsCardCollapsed = false
    }

    fun expandCreatorsSection() {
        creatorsCardCollapsed = false
    }

    fun togglePureGalleryMode() {
        pureGalleryMode = !pureGalleryMode
        entriesCardCollapsed = false
        prefs.edit()
            .putBoolean(KEY_PURE_GALLERY_MODE, pureGalleryMode)
            .putInt(KEY_GALLERY_COLUMNS, galleryColumns)
            .apply()
        loadEntries(selectedCode)
        setStatus(
            if (pureGalleryMode) {
                "Pure Gallery Mode enabled ($galleryColumns across)."
            } else {
                "Pure Gallery Mode disabled (standard entries layout)."
            }
        )
    }

    fun applyEntryLayout(modeGallery: Boolean, columns: Int) {
        val safeColumns = columns.coerceIn(1, 10)
        pureGalleryMode = modeGallery
        galleryColumns = safeColumns
        entriesCardCollapsed = false
        prefs.edit()
            .putBoolean(KEY_PURE_GALLERY_MODE, pureGalleryMode)
            .putInt(KEY_GALLERY_COLUMNS, galleryColumns)
            .apply()
        loadEntries(selectedCode)
        setStatus(
            if (pureGalleryMode) {
                "Gallery layout enabled ($galleryColumns across)."
            } else {
                "Normal entry layout enabled."
            }
        )
    }

    fun loadEntryLayoutPreviewSamples() {
        val all = libraryRepository.entries(
            textFilter = "",
            tagFilterIds = emptyList(),
            sortField = EntrySortField.ADDED,
            sortDirection = SortDirection.DESC,
            readFilter = EntryReadFilterMode.ALL,
            prioritizePinned = entryPinPriorityEnabled
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

    fun setGalleryDownloadFolder(uri: Uri?) {
        val normalized = uri?.toString().orEmpty()
        galleryDownloadTreeUri = normalized
        storeGalleryDownloadTreeUri(getApplication<Application>().applicationContext, normalized)
        entryDownloadController.refreshDownloadedCodes(invalidateUi = true)
        setStatus(
            if (normalized.isBlank()) {
                "Gallery downloads folder reset to use the backup folder."
            } else {
                "Gallery downloads folder set."
            }
        )
    }

    fun galleryDownloadFolderLabel(): String = entryDownloadController.folderLabel()

    fun isEntryDownloaded(code: Int): Boolean = entryDownloadController.isDownloaded(code)

    fun downloadEntry(detail: EntryDetail) = entryDownloadController.download(detail)

    fun startEntryDownloadBatch(mode: EntryDownloadBatchMode, initialCode: Int?) =
        entryDownloadController.startBatch(mode, initialCode)

    fun cancelEntryDownloadBatchSelection() = entryDownloadController.cancelBatchSelection()

    fun isEntryDownloadBatchSelecting(): Boolean = entryDownloadController.isBatchSelecting()

    fun isEntryBatchDownloadSelected(code: Int): Boolean =
        entryDownloadController.isBatchSelected(code)

    fun toggleEntryDownloadBatchSelection(code: Int) =
        entryDownloadController.toggleBatchSelection(code)

    fun runEntryDownloadBatch() = entryDownloadController.runBatch()

    fun redownloadEntry(detail: EntryDetail) = entryDownloadController.redownload(detail)

    fun openDownloadedEntryFolder(code: Int) = entryDownloadController.openFolder(code)

    fun removeDownloadedEntry(code: Int) = entryDownloadController.remove(code)

    fun updateBackupThumbnailArchiveEnabled(enabled: Boolean) {
        if (backupThumbnailArchiveEnabled == enabled) return
        backupThumbnailArchiveEnabled = enabled
        prefs.edit().putBoolean(KEY_BACKUP_THUMBNAIL_ARCHIVE_ENABLED, enabled).apply()
        if (enabled) {
            setStatus("Backup thumbnail archive enabled.")
            triggerProceduralBackup(ignoreThrottle = true, reportStatus = true)
        } else {
            setStatus("Backup thumbnail archive disabled.")
        }
    }

    fun calculateBackupThumbnailArchiveEstimate(): BackupThumbnailStorageEstimate {
        val appContext = getApplication<Application>().applicationContext
        return computeBackupThumbnailArchiveEstimate(
            context = appContext,
            treeUriString = autoBackupTreeUri,
            seeds = db.listDuplicateSeeds()
        )
    }

    private fun postBackupProgress(label: String, progress: BackupThumbnailSyncProgress) {
        mainHandler.post {
            backupProgressState = BackupProgressState(
                label = label,
                processed = progress.processedCount,
                total = progress.totalCount,
                written = progress.writtenCount,
                reused = progress.reusedCount,
                failed = progress.failedCount
            )
        }
    }

    private fun clearBackupProgress() {
        mainHandler.post {
            backupProgressState = null
        }
    }

    private fun syncBackupThumbnailArchiveWithProgress(
        treeUriString: String,
        label: String
    ): BackupThumbnailSyncResult? {
        if (!backupThumbnailArchiveEnabled || treeUriString.isBlank()) return null
        val appContext = getApplication<Application>().applicationContext
        return syncBackupThumbnailArchive(
            context = appContext,
            treeUriString = treeUriString,
            seeds = db.listDuplicateSeeds(),
            fetchBitmap = ::fetchThumbnailBitmapRaw,
            onProgress = { progress ->
                postBackupProgress(label, progress)
            }
        )
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

    fun requestOpenSubscriptions() {
        pendingOpenSubscriptions = true
    }

    fun consumeOpenSubscriptionsRequest() {
        pendingOpenSubscriptions = false
    }

    fun onHostResumed() {
        incognitoModeEnabled = preferenceReader.loadIncognitoMode()
        refreshAppLockOnResume()
        consumePendingShareTextIfUnlocked()
        refreshAll(selectedCode)
        if (!awaitingBrowserRatingPrompt) return
        awaitingBrowserRatingPrompt = false
        val code = pendingBrowserRatingCode ?: return

        val detail = libraryRepository.entryDetail(code)
        val wasReadBefore = pendingBrowserRatingWasRead || (detail?.isRead == true)
        browserRatingPromptState = BrowserRatingPromptState(
            code = code,
            title = detail?.title?.ifBlank { "Gallery $code" } ?: "Gallery $code",
            rating = detail?.rating?.coerceIn(0, 5) ?: 0,
            wasReadBefore = wasReadBefore,
            isReread = wasReadBefore
        )
    }

    fun backupNow() {
        triggerProceduralBackup(ignoreThrottle = true, reportStatus = true)
    }

    fun toggleDesktopBridge() {
        if (!desktopBridgeRunning && incognitoModeEnabled) {
            setStatus("Desktop bridge is disabled in incognito mode.")
            return
        }
        if (desktopBridgeRunning) {
            stopDesktopBridge(reportStatus = true)
        } else {
            startDesktopBridge(reportStatus = true)
        }
    }

    fun startDesktopBridge(reportStatus: Boolean = true) {
        if (incognitoModeEnabled) {
            if (reportStatus) {
                setStatus("Desktop bridge is disabled in incognito mode.")
            }
            return
        }
        val result = desktopBridgeServer.start(desktopBridgePort)
        val state = desktopBridgeServer.state()
        desktopBridgeRunning = state.running
        desktopBridgeUrl = state.baseUrl
        desktopBridgeChallengeCode = state.challengeCode.ifBlank { "--" }
        desktopBridgeScreenBlackout = false
        if (state.port in 1024..65535) {
            desktopBridgePort = state.port
        }

        if (state.running) {
            desktopBridgeEnabled = true
            prefs.edit()
                .putBoolean(KEY_DESKTOP_BRIDGE_ENABLED, true)
                .putInt(KEY_DESKTOP_BRIDGE_PORT, desktopBridgePort)
                .apply()
        } else {
            desktopBridgeRunning = false
            desktopBridgeUrl = ""
        }

        if (reportStatus) {
            setStatus(result.message)
        }
    }

    fun stopDesktopBridge(reportStatus: Boolean = true) {
        desktopBridgeServer.stop()
        desktopBridgeRunning = false
        desktopBridgeUrl = ""
        desktopBridgeChallengeCode = "--"
        desktopBridgeScreenBlackout = false
        desktopBridgeEnabled = false
        prefs.edit().putBoolean(KEY_DESKTOP_BRIDGE_ENABLED, false).apply()
        if (reportStatus) {
            setStatus("Desktop bridge stopped.")
        }
    }

    fun copyDesktopBridgeUrlToClipboard() {
        val url = desktopBridgeUrl.trim()
        if (!desktopBridgeRunning || url.isBlank()) {
            setStatus("Desktop bridge is not running.")
            return
        }
        val app = getApplication<Application>()
        val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard == null) {
            setStatus("Clipboard unavailable.")
            return
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("Sauce Tracker Desktop Bridge", url))
        setStatus("Desktop bridge URL copied.")
    }

    private fun triggerProceduralBackup(ignoreThrottle: Boolean, reportStatus: Boolean) {
        if (GitHubMediaSession.active) {
            if (reportStatus) setStatus("Backups are disabled in GitHub media mode.")
            return
        }
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
                var refreshedPopularTagCount = -1
                if (reportStatus) {
                    runCatching { client.fetchAllPopularTags() }.getOrNull()?.let { payload ->
                        db.replacePopularTags(payload.tags)
                        refreshedPopularTagCount = payload.tags.size
                    }
                }
                val existingSnapshot = readCurrentProceduralBackupTextOrNull(
                    context = getApplication<Application>().applicationContext,
                    treeUri = treeUri
                )?.let(::parseBackupSnapshotOrNull)
                val snapshot = BackupSnapshotExport.buildSnapshotWithSettings(
                    db = db,
                    prefs = prefs,
                    backupImporter = backupImporter,
                    entryPinPriorityEnabled = entryPinPriorityEnabled
                )
                val mergedSnapshot = BackupSnapshotExport.mergeProceduralSnapshots(
                    latestSnapshot = snapshot,
                    existingSnapshot = existingSnapshot
                )
                val exportText = BackupSerializer.serialize(mergedSnapshot)
                writeRollingProceduralBackup(
                    context = getApplication<Application>().applicationContext,
                    treeUri = treeUri,
                    text = exportText,
                    validator = { candidate -> parseBackupSnapshotOrNull(candidate) != null }
                )
                val thumbnailSyncResult = if (backupThumbnailArchiveEnabled) {
                    syncBackupThumbnailArchiveWithProgress(
                        treeUriString = treeUriValue,
                        label = "Updating backup thumbnail archive..."
                    )
                } else {
                    null
                }
                val entryCount = mergedSnapshot.optJSONArray("entries")?.length() ?: 0
                val creatorCount = mergedSnapshot.optJSONArray("creators")?.length() ?: 0
                val popularTagCount = mergedSnapshot.optJSONArray("popular_tags")?.length() ?: 0
                val hiddenCount = mergedSnapshot.optJSONArray("hidden_suggested_codes")?.length() ?: 0
                val weightCount = mergedSnapshot.optJSONObject("suggestion_category_weights")?.length() ?: 0
                val dailyActivityCount = mergedSnapshot.optJSONArray("daily_read_activity")?.length() ?: 0
                val readingSessionCount = mergedSnapshot.optJSONArray("reading_sessions")?.length() ?: 0
                arrayOf(
                    entryCount,
                    creatorCount,
                    popularTagCount,
                    hiddenCount,
                    weightCount,
                    dailyActivityCount,
                    readingSessionCount,
                    thumbnailSyncResult?.syncedCount ?: 0,
                    thumbnailSyncResult?.totalBytes ?: 0L,
                    refreshedPopularTagCount
                )
            }
            autoBackupInFlight.set(false)
            clearBackupProgress()
            if (reportStatus) {
                result.onSuccess { counts ->
                    mainHandler.post {
                        loadPopularTags()
                        tagGraphLoaded = false
                        tagGraphSnapshot = null
                        val entriesCount = counts.getOrNull(0) as? Int ?: 0
                        val creatorsCount = counts.getOrNull(1) as? Int ?: 0
                        val popularTagCount = counts.getOrNull(2) as? Int ?: 0
                        val hiddenCount = counts.getOrNull(3) as? Int ?: 0
                        val weightCount = counts.getOrNull(4) as? Int ?: 0
                        val dailyActivityCount = counts.getOrNull(5) as? Int ?: 0
                        val readingSessionCount = counts.getOrNull(6) as? Int ?: 0
                        val thumbCount = counts.getOrNull(7) as? Int ?: 0
                        val thumbBytes = counts.getOrNull(8) as? Long ?: 0L
                        val refreshedPopularTagCount = counts.getOrNull(9) as? Int ?: -1
                        val refreshSuffix = if (refreshedPopularTagCount >= 0) ", refreshed popular tags" else ""
                        setStatus(
                            if (backupThumbnailArchiveEnabled) {
                                "Procedural backup updated ($entriesCount entries, $creatorsCount creators/groups, $popularTagCount popular tags, $hiddenCount hidden suggestions, $weightCount weights, $dailyActivityCount daily activity rows, $readingSessionCount reading sessions, $thumbCount backup covers, ${formatStorageSize(thumbBytes)} thumbnail archive$refreshSuffix)."
                            } else {
                                "Procedural backup updated ($entriesCount entries, $creatorsCount creators/groups, $popularTagCount popular tags, $hiddenCount hidden suggestions, $weightCount weights, $dailyActivityCount daily activity rows, $readingSessionCount reading sessions$refreshSuffix)."
                            }
                        )
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

    fun activeTagFilterChips(): List<Pair<Long, String>> {
        return activeTagFilterIds.mapNotNull { tagId ->
            val name = tagNameCache[tagId] ?: db.getTagName(tagId)?.also { resolved ->
                tagNameCache[tagId] = resolved
            }
            name?.takeIf { it.isNotBlank() }?.let { tagId to it }
        }
    }

    private fun activeTagFilterNames(): List<String> {
        return activeTagFilterIds.mapNotNull { tagId ->
            tagNameCache[tagId] ?: db.getTagName(tagId)?.also { resolved ->
                tagNameCache[tagId] = resolved
            }
        }.map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.isNotBlank() }
    }

    private fun activeTagFilterRouteKeys(): Set<String> {
        return activeTagFilterIds.mapNotNull { tagId ->
            getTagRouteRef(tagId)?.let { routeRef ->
                subscriptionRouteKey(routeRef.type, routeRef.name).takeIf { it.isNotBlank() }
            }
        }.toSet()
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
        val fromDb = libraryRepository.tagRoute(tagId) ?: return null
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
        if (!suggestedEntriesCollapsed) {
            refreshSuggestedEntries(force = true)
        }
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

        if (rawInput.equals("github", ignoreCase = true)) {
            toggleGitHubMediaMode()
            return
        }

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

    private fun toggleGitHubMediaMode() {
        val app = getApplication<Application>()
        val result = runCatching {
            val intent = if (GitHubMediaSession.active) {
                GitHubMediaSession.deactivate()
                Intent().setClassName(app.packageName, "com.example.saucetracker.app.MainActivity")
            } else {
                Intent().apply {
                    component = ComponentName(
                        app.packageName,
                        GitHubMediaSession.LAUNCHER_COMPONENT_CLASS
                    )
                    putExtra(
                        GitHubMediaSession.EXTRA_CONFIG_BASE64,
                        GitHubMediaSession.encodedLaunchConfig(themeMode = themeMode)
                    )
                }
            }.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            app.startActivity(intent)
        }
        if (result.isFailure) {
            errorDialogMessage = "Could not switch GitHub media mode:\n${result.exceptionOrNull()?.message ?: "unknown error"}"
            setStatus("Could not switch GitHub media mode.")
        }
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
        if (!suggestedEntriesCollapsed) {
            refreshSuggestedEntries(force = true)
        }
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
                EntrySortField.READ -> SortDirection.DESC
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

    fun toggleEntryPinPriority() {
        entryPinPriorityEnabled = !entryPinPriorityEnabled
        prefs.edit().putBoolean(KEY_ENTRY_PIN_PRIORITY, entryPinPriorityEnabled).apply()
        loadEntries(selectedCode, autoSelectFirst = false)
        setStatus(
            if (entryPinPriorityEnabled) {
                "Pin priority enabled."
            } else {
                "Pin priority disabled. Entries now follow the selected sort."
            }
        )
    }

    fun entryReadFilterLabel(): String {
        return when (entryReadFilter) {
            EntryReadFilterMode.ALL -> "Show All"
            EntryReadFilterMode.READ -> "Show Read"
            EntryReadFilterMode.UNREAD -> "Show Unread"
            EntryReadFilterMode.DOWNLOADED -> "Show Downloaded"
        }
    }

    fun cycleEntryReadFilter() {
        val cycle = preferenceReader.normalizedEntryReadFilterCycle(entryReadFilterCycleOrder)
        val currentIndex = cycle.indexOf(entryReadFilter)
        entryReadFilter = if (currentIndex < 0) {
            cycle.first()
        } else {
            cycle[(currentIndex + 1) % cycle.size]
        }
        loadEntries(selectedCode)
        val message = when (entryReadFilter) {
            EntryReadFilterMode.ALL -> "Showing all entries."
            EntryReadFilterMode.READ -> "Showing read entries only."
            EntryReadFilterMode.UNREAD -> "Showing unread entries only."
            EntryReadFilterMode.DOWNLOADED -> "Showing downloaded entries only."
        }
        setStatus(message)
    }

    fun applyEntryReadFilter(mode: EntryReadFilterMode) {
        if (entryReadFilter == mode) return
        entryReadFilter = mode
        loadEntries(selectedCode)
        setStatus(
            when (mode) {
                EntryReadFilterMode.ALL -> "Showing all entries."
                EntryReadFilterMode.READ -> "Showing read entries only."
                EntryReadFilterMode.UNREAD -> "Showing unread entries only."
                EntryReadFilterMode.DOWNLOADED -> "Showing downloaded entries only."
            }
        )
    }

    fun updateEntryReadFilterCycleOrder(order: List<EntryReadFilterMode>) {
        val normalized = preferenceReader.normalizedEntryReadFilterCycle(order)
        entryReadFilterCycleOrder = normalized
        prefs.edit()
            .putString(KEY_ENTRY_FILTER_CYCLE_ORDER, normalized.joinToString(",") { it.name })
            .apply()
        if (entryReadFilter !in normalized) {
            entryReadFilter = normalized.first()
            loadEntries(selectedCode)
        }
        setStatus("Entry mode cycle updated.")
    }

    fun removeEntryReadFilterFromCycle(mode: EntryReadFilterMode) {
        val current = preferenceReader.normalizedEntryReadFilterCycle(entryReadFilterCycleOrder)
        if (current.size <= 1) {
            setStatus("At least one entry mode must stay enabled.")
            return
        }
        updateEntryReadFilterCycleOrder(current.filterNot { it == mode })
    }

    fun addEntryReadFilterToCycle(mode: EntryReadFilterMode) {
        val current = preferenceReader.normalizedEntryReadFilterCycle(entryReadFilterCycleOrder)
        if (mode in current) return
        updateEntryReadFilterCycleOrder(current + mode)
    }

    fun entryReadFilterCycleSummary(): String {
        return preferenceReader.normalizedEntryReadFilterCycle(entryReadFilterCycleOrder)
            .joinToString(" → ") { readableEntryReadFilterModeLabel(it) }
    }

    fun resetEntryReadFilterCycleOrder() {
        updateEntryReadFilterCycleOrder(preferenceReader.defaultEntryReadFilterCycle())
    }

    fun updateHomeSectionOrder(order: List<HomeSection>) {
        val normalized = preferenceReader.normalizedHomeSectionOrder(order)
        homeSectionOrder = normalized
        prefs.edit()
            .putString(KEY_HOME_SECTION_ORDER, normalized.joinToString(",") { it.name })
            .apply()
        setStatus("Home page layout updated.")
    }

    fun removeHomeSection(section: HomeSection) {
        updateHomeSectionOrder(homeSectionOrder.filterNot { it == section })
    }

    fun addHomeSection(section: HomeSection) {
        if (section in homeSectionOrder) return
        updateHomeSectionOrder(homeSectionOrder + section)
    }

    fun resetHomeSectionOrder() {
        updateHomeSectionOrder(preferenceReader.defaultHomeSectionOrder())
    }

    fun homeSectionOrderSummary(): String {
        val sections = preferenceReader.normalizedHomeSectionOrder(homeSectionOrder)
        return if (sections.isEmpty()) {
            "No expandable sections enabled"
        } else {
            sections.joinToString(" → ") { homeSectionLabel(it) }
        }
    }

    fun setDefaultEntrySort(field: EntrySortField?, direction: SortDirection) {
        defaultEntrySortField = field
        defaultEntrySortDirection = direction
        prefs.edit()
            .putString(KEY_DEFAULT_ENTRY_SORT_FIELD, field?.name ?: "NONE")
            .putString(KEY_DEFAULT_ENTRY_SORT_DIRECTION, direction.name)
            .apply()
        sortField = field
        sortDirection = direction
        loadEntries(selectedCode, autoSelectFirst = false)
        setStatus("Default entry sort set to ${describeEntrySort(field, direction)}.")
    }

    fun resetDefaultEntrySort() {
        setDefaultEntrySort(field = null, direction = SortDirection.DESC)
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

    fun setDefaultTagSort(field: TagSortField, direction: SortDirection) {
        defaultTagSortField = field
        defaultTagSortDirection = direction
        prefs.edit()
            .putString(KEY_DEFAULT_TAG_SORT_FIELD, field.name)
            .putString(KEY_DEFAULT_TAG_SORT_DIRECTION, direction.name)
            .apply()
        tagSortField = field
        tagSortDirection = direction
        loadTags()
        setStatus("Default tag sort set to ${describeTagSort(field, direction)}.")
    }

    fun resetDefaultTagSort() {
        setDefaultTagSort(field = TagSortField.COUNT, direction = SortDirection.DESC)
    }

    fun updateDefaultBrowserDuplicateCheckMode(mode: BrowserDuplicateCheckMode) {
        defaultBrowserDuplicateCheckMode = mode
        prefs.edit()
            .putString(KEY_BROWSER_DUPLICATE_CHECK_MODE, mode.name)
            .apply()
        setStatus("Default browser duplicate check set to ${mode.label}.")
    }

    fun resetDefaultBrowserDuplicateCheckMode() {
        updateDefaultBrowserDuplicateCheckMode(BrowserDuplicateCheckMode.AGGRESSIVE)
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
                tagGraphLoaded = false
                tagGraphSnapshot = null
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

    fun setDefaultCreatorSort(field: CreatorSortField, direction: SortDirection) {
        defaultCreatorSortField = field
        defaultCreatorSortDirection = direction
        prefs.edit()
            .putString(KEY_DEFAULT_CREATOR_SORT_FIELD, field.name)
            .putString(KEY_DEFAULT_CREATOR_SORT_DIRECTION, direction.name)
            .apply()
        creatorSortField = field
        creatorSortDirection = direction
        loadCreators()
        setStatus("Default artist/group sort set to ${describeCreatorSort(field, direction)}.")
    }

    fun resetDefaultCreatorSort() {
        setDefaultCreatorSort(field = CreatorSortField.COUNT, direction = SortDirection.DESC)
    }

    fun selectEntry(code: Int?) {
        selectedDetailLoadJob?.cancel()
        selectedEntryRelatedJob?.cancel()
        selectedCode = code
        selectedSummary = code?.let { target ->
            entries.firstOrNull { it.code == target }
                ?: creatorEntriesById.values.asSequence().flatten()
                    .firstOrNull { it.code == target }
                    ?.let { linked ->
                        EntryRow(
                            code = linked.code,
                            title = linked.title,
                            numPages = 0,
                            uploadDate = "",
                            addedAt = "",
                            rating = 0,
                            averageRating = 0f,
                            isRead = false,
                            pinned = false,
                            fetchedAt = "",
                            sourceUrl = "",
                            thumbnailUrl = "",
                            tags = ""
                        )
                    }
        }
        selectedDetail = null
        selectedSeriesNeighbors = SeriesNeighbors()
        selectedEntryRelatedUiState = if (code == null) {
            SelectedEntryRelatedUiState()
        } else {
            selectedEntryRelatedCache[selectedEntryRelatedCacheKey(code)]
                ?: SelectedEntryRelatedUiState(code = code, loading = true)
        }
        selectedDetailLoading = code != null
        if (code == null) {
            selectedDetailLoading = false
            return
        }
        val requestCode = code
        selectedDetailLoadJob = viewModelScope.launch {
            val detail = withContext(Dispatchers.IO) {
                libraryRepository.entryDetail(requestCode)
            }
            if (selectedCode != requestCode) return@launch
            selectedDetail = detail
            if (detail != null) {
                selectedSummary = EntryRow(
                    code = detail.code,
                    title = detail.title,
                    numPages = detail.numPages,
                    uploadDate = detail.uploadDate,
                    addedAt = detail.addedAt,
                    rating = detail.rating,
                    averageRating = detail.rating.toFloat(),
                    isRead = detail.isRead,
                    pinned = selectedSummary?.pinned ?: false,
                    fetchedAt = detail.fetchedAt,
                    sourceUrl = detail.sourceUrl,
                    thumbnailUrl = detail.thumbnailUrl,
                    tags = detail.tagsByType.values.flatten().joinToString(", ")
                )
            }
            selectedDetailLoading = false
            scheduleSelectedEntrySupport(requestCode, detail)
        }
    }

    internal fun selectRelatedEntryMode(mode: RelatedEntryMode) {
        selectedRelatedEntryMode = mode
    }

    fun openSeriesEntry(code: Int) {
        if (code <= 0) return
        pendingEntryJumpCode = code
        loadEntries(
            selectCode = code,
            autoSelectFirst = false,
            forceIncludeCode = code
        )
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

    fun openThumbnailPreviewInBrowser(code: Int) {
        if (code <= 0) return
        if (incognitoModeEnabled) {
            setStatus("Open from expanded thumbnail is disabled in incognito mode.")
            return
        }
        val detail = selectedDetail?.takeIf { it.code == code } ?: libraryRepository.entryDetail(code)
        if (detail == null) {
            infoDialogMessage = "Select an entry first."
            return
        }
        viewModelScope.launch {
            var currentDetail: EntryDetail = detail
            if (currentDetail.mediaId <= 0L || currentDetail.numPages <= 0) {
                setStatus("Refreshing image metadata for code $code...")
                val result = withContext(Dispatchers.IO) {
                    runCatching { client.fetchGallery(code) }
                }
                result.onSuccess { gallery ->
                    libraryRepository.upsertGallery(gallery)
                    loadEntries(code)
                    val refreshed = libraryRepository.entryDetail(code)
                    if (refreshed != null) {
                        currentDetail = refreshed
                        if (selectedCode == code) {
                            selectedDetail = refreshed
                            selectedSummary = EntryRow(
                                code = refreshed.code,
                                title = refreshed.title,
                                numPages = refreshed.numPages,
                                uploadDate = refreshed.uploadDate,
                                addedAt = refreshed.addedAt,
                                rating = refreshed.rating,
                                averageRating = refreshed.rating.toFloat(),
                                isRead = refreshed.isRead,
                                pinned = selectedSummary?.pinned ?: false,
                                fetchedAt = refreshed.fetchedAt,
                                sourceUrl = refreshed.sourceUrl,
                                thumbnailUrl = refreshed.thumbnailUrl,
                                tags = refreshed.tagsByType.values.flatten().joinToString(", ")
                            )
                            selectedDetailLoading = false
                            selectedEntryRelatedCache.keys.removeAll { it.code == refreshed.code }
                            scheduleSelectedEntrySupport(refreshed.code, refreshed)
                        }
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
                errorDialogMessage = "Could not open slideshow because image metadata is unavailable for code $code."
                setStatus("Slideshow failed: missing image metadata.")
                pendingBrowserRatingCode = null
                awaitingBrowserRatingPrompt = false
                pendingBrowserRatingWasRead = false
                return@launch
            }

            val opened = openInAppSlideshow(
                code = finalDetail.code,
                title = finalDetail.title,
                mediaId = finalDetail.mediaId,
                coverExt = finalDetail.coverExt,
                numPages = finalDetail.numPages,
                startPage = 1
            )
            if (opened) {
                pendingBrowserRatingCode = finalDetail.code
                pendingBrowserRatingWasRead = finalDetail.isRead
                awaitingBrowserRatingPrompt = true
            } else {
                pendingBrowserRatingCode = null
                awaitingBrowserRatingPrompt = false
                pendingBrowserRatingWasRead = false
            }
        }
    }

    fun importFromBrowserClipboard(rawInput: String) {
        val candidate = resolveBrowserClipboardCandidate(rawInput)
        if (candidate.isNullOrBlank()) {
            setStatus("Copied text was not recognized as code/artist/group input.")
            return
        }
        addOrUpdateRawInput(candidate.trim())
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
        openGalleryCodeBrowser(
            initialCode = detail.code,
            initialQuery = "",
            blockedTags = emptyList(),
            successStatus = "Opened code ${detail.code} preview in gallery browser."
        )
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
            val intent = GalleryBrowserActivity.createIntent(
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

    private suspend fun openInAppSlideshow(
        code: Int,
        title: String,
        mediaId: Long,
        coverExt: String,
        numPages: Int,
        startPage: Int = 1
    ): Boolean {
        val app = getApplication<Application>()
        val result = runCatching {
            val intent = GallerySlideshowActivity.createIntent(
                context = app,
                code = code,
                title = title,
                mediaId = mediaId,
                coverExt = coverExt,
                numPages = numPages,
                startPage = startPage.coerceIn(1, numPages.coerceAtLeast(1)),
                incognitoModeEnabled = incognitoModeEnabled
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

    fun getEntryDetail(code: Int): EntryDetail? = libraryRepository.entryDetail(code)

    fun getEntryDetails(codes: List<Int>): List<EntryDetail> = libraryRepository.entryDetails(codes)

    fun getEntryRatingHistory(code: Int): List<EntryRatingHistoryRow> = db.getEntryRatingHistory(code)

    fun getAverageEntryRating(code: Int): Float? = db.getAverageEntryRating(code)

    fun updateRatingHistoryRow(code: Int, row: EntryRatingHistoryRow, rating: Int) {
        db.updateRatingHistoryRow(code, row, rating)
        loadEntries(code)
        selectEntry(code)
    }

    fun deleteRatingHistoryRow(code: Int, row: EntryRatingHistoryRow) {
        db.deleteRatingHistoryRow(code, row)
        readAnalyticsLoaded = false
        ensureReadAnalyticsLoaded(forceRefresh = true)
        loadEntries(code)
        selectEntry(code)
    }

    fun toggleSuggestedEntriesCollapsed() {
        suggestedEntriesCollapsed = !suggestedEntriesCollapsed
        sessionExcludedSuggestionCodes.clear()
        suggestedEntries = emptyList()
        suggestedOverflowEntries.clear()
        suggestedEntriesInfoMessage = null
        if (!suggestedEntriesCollapsed) {
            refreshSuggestedEntries(force = true)
        }
    }

    fun refreshSuggestedEntriesForCurrentSession() {
        val currentRows = suggestedEntries
        currentRows
            .asSequence()
            .map { it.code }
            .filter { it > 0 }
            .forEach { sessionExcludedSuggestionCodes += it }
        val targetCount = currentRows.size.coerceAtLeast(1)
        val importedCodes = libraryRepository.allEntryCodes().toSet()
        val nextRows = mutableListOf<SuggestedEntryRow>()
        val existingCodes = mutableSetOf<Int>()
        while (nextRows.size < targetCount) {
            val replacement = takeSuggestedOverflowReplacement(existingCodes, importedCodes) ?: break
            nextRows += replacement
            existingCodes += replacement.code
        }
        if (nextRows.isNotEmpty()) {
            suggestedEntries = nextRows
            suggestedEntriesInfoMessage = null
            nextRows
                .filter { it.duplicateHint == null }
                .takeIf { it.isNotEmpty() }
                ?.let(::populateSuggestionDuplicateHintsAsync)
            setStatus("Refreshed suggested entries (${nextRows.size}).")
            return
        }
        refreshSuggestedEntries(force = true, applySessionExclusions = true)
    }

    fun updateSuggestionMode(mode: SuggestionMode) {
        if (suggestionMode == mode) return
        suggestionMode = mode
        if (!suggestedEntriesCollapsed) {
            refreshSuggestedEntries(force = true)
        }
    }

    private fun suggestionThemeCoupling(target: SuggestionWeightCategory): Float {
        return when (target) {
            SuggestionWeightCategory.TAG -> 0.45f
            SuggestionWeightCategory.PARODY -> 0.28f
            SuggestionWeightCategory.CHARACTER -> 0.34f
            SuggestionWeightCategory.CATEGORY -> 0.22f
            SuggestionWeightCategory.LANGUAGE -> 0.12f
            SuggestionWeightCategory.CREATOR -> 0.18f
            SuggestionWeightCategory.LENGTH -> 0.10f
            SuggestionWeightCategory.OTHER -> 0.16f
        }
    }

    private fun suggestionCategoryCoupling(
        source: SuggestionWeightCategory,
        target: SuggestionWeightCategory
    ): Float {
        if (source == target) return 0f
        return when (source) {
            SuggestionWeightCategory.TAG -> when (target) {
                SuggestionWeightCategory.CREATOR -> 2.1f
                SuggestionWeightCategory.CHARACTER -> 1.35f
                SuggestionWeightCategory.PARODY -> 1.05f
                SuggestionWeightCategory.CATEGORY -> 0.85f
                SuggestionWeightCategory.LANGUAGE -> 0.22f
                SuggestionWeightCategory.LENGTH -> 0.32f
                SuggestionWeightCategory.OTHER -> 0.55f
                else -> 0f
            }
            SuggestionWeightCategory.CREATOR -> when (target) {
                SuggestionWeightCategory.TAG -> 2.2f
                SuggestionWeightCategory.CHARACTER -> 0.85f
                SuggestionWeightCategory.PARODY -> 0.75f
                SuggestionWeightCategory.CATEGORY -> 0.50f
                SuggestionWeightCategory.LANGUAGE -> 0.18f
                SuggestionWeightCategory.LENGTH -> 0.28f
                SuggestionWeightCategory.OTHER -> 0.45f
                else -> 0f
            }
            SuggestionWeightCategory.LENGTH -> when (target) {
                SuggestionWeightCategory.TAG -> 0.95f
                SuggestionWeightCategory.CREATOR -> 1.35f
                SuggestionWeightCategory.CHARACTER -> 0.72f
                SuggestionWeightCategory.PARODY -> 0.62f
                SuggestionWeightCategory.CATEGORY -> 0.48f
                SuggestionWeightCategory.LANGUAGE -> 0.12f
                SuggestionWeightCategory.OTHER -> 0.35f
                else -> 0f
            }
            SuggestionWeightCategory.CHARACTER -> when (target) {
                SuggestionWeightCategory.TAG -> 1.15f
                SuggestionWeightCategory.CREATOR -> 0.92f
                SuggestionWeightCategory.PARODY -> 0.62f
                SuggestionWeightCategory.CATEGORY -> 0.52f
                SuggestionWeightCategory.LANGUAGE -> 0.16f
                SuggestionWeightCategory.LENGTH -> 0.24f
                SuggestionWeightCategory.OTHER -> 0.40f
                else -> 0f
            }
            SuggestionWeightCategory.PARODY -> when (target) {
                SuggestionWeightCategory.TAG -> 0.92f
                SuggestionWeightCategory.CREATOR -> 0.82f
                SuggestionWeightCategory.CHARACTER -> 0.55f
                SuggestionWeightCategory.CATEGORY -> 0.42f
                SuggestionWeightCategory.LANGUAGE -> 0.14f
                SuggestionWeightCategory.LENGTH -> 0.22f
                SuggestionWeightCategory.OTHER -> 0.35f
                else -> 0f
            }
            SuggestionWeightCategory.CATEGORY -> when (target) {
                SuggestionWeightCategory.TAG -> 0.85f
                SuggestionWeightCategory.CREATOR -> 0.58f
                SuggestionWeightCategory.CHARACTER -> 0.45f
                SuggestionWeightCategory.PARODY -> 0.35f
                SuggestionWeightCategory.LANGUAGE -> 0.12f
                SuggestionWeightCategory.LENGTH -> 0.20f
                SuggestionWeightCategory.OTHER -> 0.30f
                else -> 0f
            }
            SuggestionWeightCategory.LANGUAGE -> when (target) {
                SuggestionWeightCategory.TAG -> 0.18f
                SuggestionWeightCategory.CREATOR -> 0.12f
                SuggestionWeightCategory.CHARACTER -> 0.10f
                SuggestionWeightCategory.PARODY -> 0.08f
                SuggestionWeightCategory.CATEGORY -> 0.10f
                SuggestionWeightCategory.LENGTH -> 0.06f
                SuggestionWeightCategory.OTHER -> 0.12f
                else -> 0f
            }
            SuggestionWeightCategory.OTHER -> when (target) {
                SuggestionWeightCategory.TAG -> 0.52f
                SuggestionWeightCategory.CREATOR -> 0.42f
                SuggestionWeightCategory.CHARACTER -> 0.28f
                SuggestionWeightCategory.PARODY -> 0.25f
                SuggestionWeightCategory.CATEGORY -> 0.22f
                SuggestionWeightCategory.LANGUAGE -> 0.10f
                SuggestionWeightCategory.LENGTH -> 0.16f
                else -> 0f
            }
        }
    }

    fun setSuggestionCategoryWeight(category: SuggestionWeightCategory, value: Float) {
        val clamped = value.coerceIn(0f, 2f)
        val currentMap = suggestionCategoryWeights.toMutableMap()
        val current = currentMap[category] ?: 1f
        if (abs(current - clamped) < 0.001f) return
        currentMap[category] = clamped

        val delta = clamped - current
        val otherCategories = SuggestionWeightCategory.entries.filter { it != category }
        if (delta > 0f) {
            val reducible = otherCategories.associateWith { key ->
                (currentMap[key] ?: 1f).coerceAtLeast(0f)
            }
            val categoryPriority = otherCategories.associateWith { key ->
                (reducible[key] ?: 0f) * suggestionCategoryCoupling(category, key)
            }
            val themeReducible = suggestionThemeStrength.coerceAtLeast(0f)
            val themePriority = themeReducible * suggestionCategoryCoupling(category, SuggestionWeightCategory.TAG) * 0.18f
            val totalReducible = categoryPriority.values.sum() + themePriority
            if (totalReducible > 0f) {
                val targetReduction = delta.coerceAtMost(
                    reducible.values.sum() + themeReducible
                )
                var remaining = targetReduction
                val adjusted = currentMap.toMutableMap()
                otherCategories.forEachIndexed { index, key ->
                    val available = reducible[key] ?: 0f
                    val priority = categoryPriority[key] ?: 0f
                    if (available <= 0f || priority <= 0f) return@forEachIndexed
                    val reduction = if (index == otherCategories.lastIndex) {
                        0f
                    } else {
                        (targetReduction * (priority / totalReducible)).coerceAtMost(remaining)
                    }
                    adjusted[key] = ((adjusted[key] ?: 1f) - reduction).coerceIn(0f, 2f)
                    remaining -= reduction
                }
                if (themePriority > 0f && remaining > 0f) {
                    val themeReduction = (targetReduction * (themePriority / totalReducible))
                        .coerceAtMost(remaining)
                        .coerceAtMost(themeReducible)
                    suggestionThemeStrength = (suggestionThemeStrength - themeReduction).coerceIn(0f, 2f)
                    prefs.edit().putFloat(KEY_SUGGESTION_THEME_STRENGTH, suggestionThemeStrength).apply()
                    remaining -= themeReduction
                }
                if (remaining > 0f) {
                    otherCategories
                        .sortedByDescending { categoryPriority[it] ?: 0f }
                        .forEach { key ->
                            if (remaining <= 0f) return@forEach
                            val available = adjusted[key] ?: 0f
                            if (available <= 0f) return@forEach
                            val extra = available.coerceAtMost(remaining)
                            adjusted[key] = (available - extra).coerceIn(0f, 2f)
                            remaining -= extra
                        }
                }
                adjusted[category] = (current + targetReduction).coerceIn(0f, 2f)
                suggestionCategoryWeights = adjusted
            } else {
                suggestionCategoryWeights = currentMap
            }
        } else {
            val headroom = otherCategories.associateWith { key ->
                (2f - (currentMap[key] ?: 1f)).coerceAtLeast(0f)
            }
            val categoryPriority = otherCategories.associateWith { key ->
                (headroom[key] ?: 0f) * suggestionCategoryCoupling(category, key)
            }
            val themeHeadroom = (2f - suggestionThemeStrength).coerceAtLeast(0f)
            val themePriority = themeHeadroom * suggestionCategoryCoupling(category, SuggestionWeightCategory.TAG) * 0.18f
            val totalHeadroom = categoryPriority.values.sum() + themePriority
            val freed = -delta
            if (totalHeadroom > 0f && freed > 0f) {
                var remaining = freed.coerceAtMost(totalHeadroom)
                val adjusted = currentMap.toMutableMap()
                otherCategories.forEachIndexed { index, key ->
                    val available = headroom[key] ?: 0f
                    val priority = categoryPriority[key] ?: 0f
                    if (available <= 0f || priority <= 0f) return@forEachIndexed
                    val increase = if (index == otherCategories.lastIndex) {
                        0f
                    } else {
                        (freed * (priority / totalHeadroom)).coerceAtMost(remaining)
                    }
                    adjusted[key] = ((adjusted[key] ?: 1f) + increase).coerceIn(0f, 2f)
                    remaining -= increase
                }
                if (themePriority > 0f && remaining > 0f) {
                    val themeIncrease = (freed * (themePriority / totalHeadroom))
                        .coerceAtMost(remaining)
                        .coerceAtMost(themeHeadroom)
                    suggestionThemeStrength = (suggestionThemeStrength + themeIncrease).coerceIn(0f, 2f)
                    prefs.edit().putFloat(KEY_SUGGESTION_THEME_STRENGTH, suggestionThemeStrength).apply()
                    remaining -= themeIncrease
                }
                if (remaining > 0f) {
                    otherCategories
                        .sortedByDescending { categoryPriority[it] ?: 0f }
                        .forEach { key ->
                            if (remaining <= 0f) return@forEach
                            val available = (2f - (adjusted[key] ?: 1f)).coerceAtLeast(0f)
                            if (available <= 0f) return@forEach
                            val extra = available.coerceAtMost(remaining)
                            adjusted[key] = ((adjusted[key] ?: 1f) + extra).coerceIn(0f, 2f)
                            remaining -= extra
                        }
                }
                suggestionCategoryWeights = adjusted
            } else {
                suggestionCategoryWeights = currentMap
            }
        }
        persistSuggestionCategoryWeights()
    }

    fun updateSuggestionThemeStrength(value: Float) {
        val clamped = value.coerceIn(0f, 2f)
        if (abs(suggestionThemeStrength - clamped) < 0.001f) return
        val delta = clamped - suggestionThemeStrength
        suggestionThemeStrength = clamped
        if (delta > 0f) {
            val currentMap = suggestionCategoryWeights.toMutableMap()
            val reducible = SuggestionWeightCategory.entries.associateWith { key ->
                (currentMap[key] ?: 1f).coerceAtLeast(0f)
            }
            val priorities = SuggestionWeightCategory.entries.associateWith { key ->
                (reducible[key] ?: 0f) * suggestionThemeCoupling(key)
            }
            val totalPriority = priorities.values.sum()
            if (totalPriority > 0f) {
                var remaining = delta
                SuggestionWeightCategory.entries.forEach { key ->
                    val available = reducible[key] ?: 0f
                    val priority = priorities[key] ?: 0f
                    if (available <= 0f || priority <= 0f) return@forEach
                    val reduction = (delta * (priority / totalPriority)).coerceAtMost(remaining).coerceAtMost(available)
                    currentMap[key] = ((currentMap[key] ?: 1f) - reduction).coerceIn(0f, 2f)
                    remaining -= reduction
                }
                suggestionCategoryWeights = currentMap
                persistSuggestionCategoryWeights()
            }
        } else if (delta < 0f) {
            val freed = -delta
            val currentMap = suggestionCategoryWeights.toMutableMap()
            val headroom = SuggestionWeightCategory.entries.associateWith { key ->
                (2f - (currentMap[key] ?: 1f)).coerceAtLeast(0f)
            }
            val priorities = SuggestionWeightCategory.entries.associateWith { key ->
                (headroom[key] ?: 0f) * suggestionThemeCoupling(key)
            }
            val totalPriority = priorities.values.sum()
            if (totalPriority > 0f) {
                var remaining = freed
                SuggestionWeightCategory.entries.forEach { key ->
                    val available = headroom[key] ?: 0f
                    val priority = priorities[key] ?: 0f
                    if (available <= 0f || priority <= 0f) return@forEach
                    val increase = (freed * (priority / totalPriority)).coerceAtMost(remaining).coerceAtMost(available)
                    currentMap[key] = ((currentMap[key] ?: 1f) + increase).coerceIn(0f, 2f)
                    remaining -= increase
                }
                suggestionCategoryWeights = currentMap
                persistSuggestionCategoryWeights()
            }
        }
        prefs.edit().putFloat(KEY_SUGGESTION_THEME_STRENGTH, suggestionThemeStrength).apply()
    }

    fun resetSuggestionCategoryWeights() {
        suggestionCategoryWeights = preferenceReader.defaultSuggestionCategoryWeights()
        persistSuggestionCategoryWeights()
        suggestionThemeStrength = 1f
        prefs.edit().putFloat(KEY_SUGGESTION_THEME_STRENGTH, 1f).apply()
        setStatus("Suggestion category weights reset to default.")
        if (!suggestedEntriesCollapsed && !incognitoModeEnabled) {
            refreshSuggestedEntries(force = true)
        }
    }

    fun openSuggestedEntryInBrowser(code: Int) {
        if (code <= 0) return
        if (incognitoModeEnabled) {
            setStatus("Open in browser is disabled in incognito mode.")
            return
        }
        openGalleryCodeBrowser(
            initialCode = code,
            initialQuery = "",
            blockedTags = emptyList(),
            successStatus = "Opened suggested code $code in browser."
        )
    }

    private fun triggerSuggestedImportFlash(code: Int) {
        suggestedImportFlashEpochs = suggestedImportFlashEpochs.toMutableMap().apply {
            put(code, (this[code] ?: 0) + 1)
        }
    }

    private fun clearSuggestedImportFlash(code: Int) {
        if (!suggestedImportFlashEpochs.containsKey(code)) return
        suggestedImportFlashEpochs = suggestedImportFlashEpochs.toMutableMap().apply {
            remove(code)
        }
    }

    fun importSuggestedEntry(code: Int) {
        if (code <= 0) return
        if (incognitoModeEnabled) {
            setStatus("Import is disabled in incognito mode.")
            return
        }
        viewModelScope.launch {
            setStatus("Importing suggested code $code...")
            val result = withContext(Dispatchers.IO) {
                runCatching { client.fetchGallery(code) }
            }
            result.onSuccess { gallery ->
                val insertedNew = withContext(Dispatchers.IO) { libraryRepository.upsertGallery(gallery) }
                if (insertedNew) {
                    registerSessionNewEntryCode(code)
                }
                triggerSuggestedImportFlash(code)
                delay(520L)
                suggestedEntries = suggestedEntries.filterNot { it.code == code }
                suggestedOverflowEntries.removeAll { it.code == code }
                clearSuggestedImportFlash(code)
                refreshAll(code)
                setStatus("Imported suggested code $code.")
            }.onFailure { exc ->
                errorDialogMessage = exc.message ?: "Could not import suggested entry."
                setStatus("Suggestion import failed.")
            }
        }
    }

    private fun suggestionCacheFingerprint(
        libraryRevision: String,
        blocked: Set<String>,
        requiredTags: Set<String>,
        parsedSearch: ParsedSearchQuery,
        categoryWeights: Map<SuggestionWeightCategory, Float>,
        themeStrength: Float,
        applySessionExclusions: Boolean
    ): String {
        val stableInput = buildString {
            append(libraryRevision)
            append('|').append(suggestionMode.name)
            append('|').append(entrySearch.trim())
            append('|').append(requiredTags.sorted().joinToString(","))
            append('|').append(blocked.sorted().joinToString(","))
            append('|').append(parsedSearch.filters.joinToString(",") { "${it.key}:${it.value}" })
            append('|').append(categoryWeights.entries.sortedBy { it.key.name }
                .joinToString(",") { "${it.key.name}:${it.value}" })
            append('|').append(themeStrength)
            append('|').append(applySessionExclusions)
        }
        return stableInput.hashCode().toUInt().toString(16)
    }

    fun refreshSuggestedEntries(force: Boolean = true, applySessionExclusions: Boolean = false) {
        if (!force && suggestedEntries.isNotEmpty()) return
        suggestionsRefreshJob?.cancel()
        val refreshGeneration = ++suggestionsRefreshGeneration
        suggestionsRefreshRunning = true
        suggestedEntriesLoading = true
        val refreshStartedAt = android.os.SystemClock.elapsedRealtime()
        var firstVisibleRecorded = false
        if (suggestedEntries.isEmpty()) suggestedEntriesInfoMessage = null
        suggestionsRefreshJob = viewModelScope.launch {
            try {
                val blocked = blockedTagNamesForBrowser()
                    .map { normalizeTagName(it) }
                    .filter { it.isNotBlank() }
                    .toSet()
                val requiredTagFilters = activeTagFilterNames()
                    .map { normalizeTagName(it) }
                    .filter { it.isNotBlank() }
                    .toSet()
                val parsedSearch = parseSearchQuery(entrySearch)
                val searchTerms = extractSearchEverythingBrowserTerms(parsedSearch.freeText)
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                val parsedFilterTagTerms = buildList {
                    parsedSearch.filters.forEach { filter ->
                        val value = normalizeTagName(filter.value)
                        if (value.isBlank()) return@forEach
                        when (filter.key) {
                            "tag", "type", "parody", "character", "category", "language" -> add(value)
                            "artist", "group" -> add(value)
                        }
                    }
                }.distinct()
                val parsedFilterCreators = buildList {
                    parsedSearch.filters.forEach { filter ->
                        val value = normalizeTagName(filter.value)
                        if (value.isBlank()) return@forEach
                        when (filter.key) {
                            "artist", "group" -> add(SuggestionCreatorToken(name = value, type = filter.key))
                        }
                    }
                }.distinctBy { "${it.type}:${it.name}" }
                val hiddenCodes = hiddenSuggestionCodesSnapshot()
                val sessionExcludedCodes = if (applySessionExclusions) {
                    sessionExcludedSuggestionCodes.toSet()
                } else {
                    emptySet()
                }
                val suggestionCategoryWeightsSnapshot = suggestionCategoryWeights
                val suggestionThemeStrengthSnapshot = suggestionThemeStrength
                val libraryRevision = withContext(Dispatchers.IO) { db.suggestionLibraryRevision() }
                val cacheFingerprint = suggestionCacheFingerprint(
                    libraryRevision = libraryRevision,
                    blocked = blocked,
                    requiredTags = requiredTagFilters,
                    parsedSearch = parsedSearch,
                    categoryWeights = suggestionCategoryWeightsSnapshot,
                    themeStrength = suggestionThemeStrengthSnapshot,
                    applySessionExclusions = applySessionExclusions
                )
                suggestionCacheStore.loadRows(cacheFingerprint)?.let { cached ->
                    val visible = cached.rows
                        .filterNot { it.code in hiddenCodes || it.code in sessionExcludedCodes }
                    if (visible.isNotEmpty()) {
                        suggestedEntries = visible.take(SUGGESTION_VISIBLE_TARGET)
                        suggestedOverflowEntries.clear()
                        suggestedOverflowEntries.addAll(visible.drop(SUGGESTION_VISIBLE_TARGET))
                        suggestedEntriesInfoMessage = "Updating cached recommendations..."
                        populateSuggestionDuplicateHintsAsync(suggestedEntries)
                        PerformanceMetrics.recordSuggestionFirstVisibleMillis(
                            android.os.SystemClock.elapsedRealtime() - refreshStartedAt
                        )
                        firstVisibleRecorded = true
                    }
                }

                val computation = runCatching {
                    withContext(Dispatchers.IO) {
                        val snapshot = db.exportSuggestionProfileSnapshot()
                        val importedCodes = libraryRepository.allEntryCodes().toSet()
                        val localDuplicateSeeds = db.listDuplicateSeeds()
                        val duplicateSeedVersion = computeLocalDuplicateSeedVersion(localDuplicateSeeds)
                        val duplicateSeedsChanged = suggestionDuplicateHintCacheSeedVersion != duplicateSeedVersion
                        if (suggestionDuplicateHintCacheSeedVersion != duplicateSeedVersion) {
                            suggestionDuplicateHintCache.clear()
                            suggestionDuplicateHintCacheSeedVersion = duplicateSeedVersion
                        }
                        suggestionDuplicateSeedIndex = buildLocalDuplicateSeedIndex(localDuplicateSeeds)
                        if (DuplicateLocalHashIndex.isEmpty() || duplicateSeedsChanged) {
                            DuplicateLocalHashIndex.replaceAll(
                                readBackupThumbnailHashesByCode(
                                    getApplication<Application>().applicationContext,
                                    localDuplicateSeeds.asSequence().map { it.code }.filter { it > 0 }.toSet()
                                )
                            )
                        }
                        val profileStartedAt = android.os.SystemClock.elapsedRealtime()
                        val profile = buildSuggestionProfile(
                            snapshot = snapshot,
                            blockedTags = blocked,
                            categoryWeights = suggestionCategoryWeightsSnapshot,
                            themeStrength = suggestionThemeStrengthSnapshot,
                            suggestionsViewModel = suggestionsViewModel
                        )
                        PerformanceMetrics.recordSuggestionProfileMillis(
                            android.os.SystemClock.elapsedRealtime() - profileStartedAt
                        )
                        if (profile.tagWeights.isEmpty() && profile.creatorWeights.isEmpty()) {
                            return@withContext SuggestionRefreshResult(
                                rows = emptyList(),
                                infoMessage = "Add read/rated entries first to unlock suggestions."
                            )
                        }

                        val topTags = profile.tagWeights.entries
                            .sortedByDescending { it.value }
                            .mapNotNull { entry ->
                                entry.key.takeIf { it.isNotBlank() }?.let { name ->
                                    SuggestionTagToken(
                                        name = name,
                                        type = profile.tagTypeByName[name] ?: "tag"
                                    )
                                }
                            }
                            .take(8)
                        val languageBias = (suggestionCategoryWeightsSnapshot[SuggestionWeightCategory.LANGUAGE] ?: 1f)
                            .coerceIn(0f, 2f)
                        val prioritizedLanguageTags = if (languageBias > 1.1f) {
                            topTags.filter { it.type == "language" }.take(2)
                        } else {
                            emptyList()
                        }
                        val topCreators = profile.creatorWeights.entries
                            .sortedByDescending { it.value }
                            .map { entry ->
                                SuggestionCreatorToken(
                                    name = entry.key,
                                    type = profile.creatorTypeByName[entry.key] ?: "artist"
                                )
                            }
                            .filter { it.isNotBlank() }
                            .take(3)

                        val includeTokens = (prioritizedLanguageTags + topTags)
                            .distinctBy { "${it.type}:${it.name}" }
                            .take(8)
                        val includeCreators = (topCreators.take(3) + parsedFilterCreators)
                            .filter { it.isNotBlank() }
                            .distinctBy { "${it.type}:${normalizeTagName(it.name)}" }
                        val includeTags = (
                            includeTokens +
                                requiredTagFilters.map { SuggestionTagToken(it, "tag") } +
                                searchTerms.take(4).map { SuggestionTagToken(normalizeTagName(it), "tag") } +
                                parsedFilterTagTerms.take(4).map { SuggestionTagToken(it, "tag") }
                            )
                            .filter { it.isNotBlank() }
                            .distinctBy { "${it.type}:${it.name}" }
                        if (includeTags.isEmpty() && includeCreators.isEmpty()) {
                            return@withContext SuggestionRefreshResult(
                                rows = emptyList(),
                                infoMessage = if (
                                    requiredTagFilters.isNotEmpty() ||
                                    searchTerms.isNotEmpty() ||
                                    parsedSearch.filters.isNotEmpty()
                                ) {
                                    "No candidates match current Search/Tag filters."
                                } else {
                                    "Not enough preference signal yet. Read/rate more entries and refresh."
                                }
                            )
                        }

                        val blockedTerms = blocked.take(20).toList()
                        val queryCandidates = buildList {
                            val creatorSeed = includeCreators.take(2)
                            val tagSeed = includeTags.take(6)

                            // Broad mixed query.
                            val primary = buildSuggestionSearchQuery(tagSeed.take(3), creatorSeed, blockedTerms)
                            if (primary.isNotBlank()) add(primary)

                            // Relaxed tag queries so results don't require every top tag at once.
                            tagSeed.windowed(size = 2, step = 1, partialWindows = false)
                                .take(4)
                                .forEach { pair ->
                                    val q = buildSuggestionSearchQuery(pair, creatorSeed.take(1), blockedTerms)
                                    if (q.isNotBlank()) add(q)
                                }
                                tagSeed.forEach { tag ->
                                    val q = buildSuggestionSearchQuery(listOf(tag), creatorSeed.take(1), blockedTerms)
                                    if (q.isNotBlank()) add(q)
                                }

                            if (languageBias > 1.1f) {
                                tagSeed
                                    .filter { it.type == "language" }
                                    .forEach { tag ->
                                        val q = buildSuggestionSearchQuery(listOf(tag), emptyList(), blockedTerms)
                                        if (q.isNotBlank()) add(q)
                                    }
                            }

                            // Creator-only fallback, both combined and individual.
                            if (creatorSeed.isNotEmpty()) {
                                val creatorsOnly = buildSuggestionSearchQuery(emptyList(), creatorSeed, blockedTerms)
                                if (creatorsOnly.isNotBlank()) add(creatorsOnly)
                                creatorSeed.forEach { creator ->
                                    val q = buildSuggestionSearchQuery(emptyList(), listOf(creator), blockedTerms)
                                    if (q.isNotBlank()) add(q)
                                }
                            }

                            if (blockedTerms.isNotEmpty()) {
                                val noBlocked = buildSuggestionSearchQuery(tagSeed.take(3), creatorSeed, emptyList())
                                if (noBlocked.isNotBlank()) add(noBlocked)
                            }
                        }.distinct()
                        if (queryCandidates.isEmpty()) {
                            return@withContext SuggestionRefreshResult(rows = emptyList(), infoMessage = "Could not build a valid suggestions query.")
                        }
                        val firstStageQueries = buildList {
                            queryCandidates.firstOrNull()?.let(::add)
                            includeTags.firstOrNull()?.let { tag ->
                                buildSuggestionSearchQuery(listOf(tag), emptyList(), blockedTerms)
                                    .takeIf { it.isNotBlank() }
                                    ?.let(::add)
                            }
                            includeCreators.firstOrNull()?.let { creator ->
                                buildSuggestionSearchQuery(emptyList(), listOf(creator), blockedTerms)
                                    .takeIf { it.isNotBlank() }
                                    ?.let(::add)
                            }
                        }.distinct()

                        val searchParallelism = Runtime.getRuntime().availableProcessors().coerceIn(2, 6)
                        suspend fun collectCandidateCodes(
                            queries: List<String>,
                            pages: IntRange,
                            foundCodes: LinkedHashSet<Int>,
                            stopAt: Int = SUGGESTION_CANDIDATE_TARGET * 3
                        ): LinkedHashSet<Int> {
                            for (page in pages) {
                                if (foundCodes.size >= stopAt) break
                                coroutineScope {
                                    queries
                                        .chunked(searchParallelism)
                                        .forEach { batch ->
                                            if (foundCodes.size >= stopAt) return@forEach
                                            batch.map { query ->
                                            async {
                                                runCatching {
                                                    PerformanceMetrics.recordSuggestionSearchRequest()
                                                    suggestionsRepository.searchCodes(query = query, page = page)
                                                }.getOrDefault(emptyList())
                                            }
                                        }.awaitAll().forEach { pageCodes ->
                                            pageCodes.forEach { found ->
                                                if (
                                                    found > 0 &&
                                                    found !in importedCodes &&
                                                    found !in hiddenCodes
                                                ) {
                                                    foundCodes += found
                                                }
                                            }
                                        }
                                        }
                                }
                            }
                            return foundCodes
                        }
                        val candidateCodes = linkedSetOf<Int>()
                        val searchStartedAt = android.os.SystemClock.elapsedRealtime()
                        collectCandidateCodes(
                            queries = firstStageQueries,
                            pages = 1..1,
                            foundCodes = candidateCodes
                        )
                        if (candidateCodes.size < SUGGESTION_CANDIDATE_TARGET) {
                            collectCandidateCodes(
                                queries = queryCandidates.filterNot { it in firstStageQueries },
                                pages = 1..1,
                                foundCodes = candidateCodes
                            )
                        }
                        if (candidateCodes.size < SUGGESTION_CANDIDATE_TARGET) {
                            collectCandidateCodes(
                                queries = queryCandidates.take(6),
                                pages = 2..3,
                                foundCodes = candidateCodes
                            )
                        }
                        if (candidateCodes.isEmpty()) {
                            val relaxedQueries = buildList {
                                val creatorSeed = includeCreators.take(1)
                                val tagSeed = includeTags.take(10)
                                tagSeed.forEach { tag ->
                                    buildSuggestionSearchQuery(listOf(tag), emptyList(), blockedTerms)
                                        .takeIf { it.isNotBlank() }
                                        ?.let(::add)
                                    buildSuggestionSearchQuery(listOf(tag), emptyList(), emptyList())
                                        .takeIf { it.isNotBlank() }
                                        ?.let(::add)
                                }
                                tagSeed.windowed(size = 2, step = 1, partialWindows = false)
                                    .take(6)
                                    .forEach { pair ->
                                        buildSuggestionSearchQuery(pair, emptyList(), emptyList())
                                            .takeIf { it.isNotBlank() }
                                            ?.let(::add)
                                    }
                                creatorSeed.forEach { creator ->
                                    buildSuggestionSearchQuery(emptyList(), listOf(creator), emptyList())
                                        .takeIf { it.isNotBlank() }
                                        ?.let(::add)
                                    tagSeed.take(4).forEach { tag ->
                                        buildSuggestionSearchQuery(listOf(tag), listOf(creator), emptyList())
                                            .takeIf { it.isNotBlank() }
                                            ?.let(::add)
                                    }
                                }
                            }.distinct()
                            collectCandidateCodes(
                                queries = relaxedQueries.take(8),
                                pages = 1..2,
                                foundCodes = candidateCodes
                            )
                        }
                        PerformanceMetrics.recordSuggestionSearchMillis(
                            android.os.SystemClock.elapsedRealtime() - searchStartedAt
                        )
                        PerformanceMetrics.recordSuggestionCandidateCount(candidateCodes.size)
                        if (candidateCodes.isEmpty()) {
                            return@withContext SuggestionRefreshResult(
                                rows = emptyList(),
                                infoMessage = "No suggestions matched current mode/filter combination."
                            )
                        }

                        data class ScoredSuggestionCandidate(
                            val gallery: GalleryData,
                            val thumbnailUrl: String,
                            val score: Float,
                            val topTags: List<String>,
                            val whySuggestedReason: String
                        )

                        val suggestionParallelism = Runtime.getRuntime().availableProcessors().coerceIn(2, 6)
                        val out = mutableListOf<ScoredSuggestionCandidate>()
                        coroutineScope {
                            for (batch in candidateCodes.chunked(suggestionParallelism)) {
                                if (out.size >= SUGGESTION_CANDIDATE_TARGET) break
                                out += batch.map { candidateCode ->
                                        async {
                                            val gallery = fetchSuggestedGalleryCached(candidateCode)
                                                ?: return@async null
                                            if (gallery.code in importedCodes || gallery.code in hiddenCodes) {
                                                return@async null
                                            }
                                            if (!gallery.matchesSuggestionFilters(
                                                    requiredTagFilters = requiredTagFilters,
                                                    parsedSearch = parsedSearch
                                                )
                                            ) {
                                                return@async null
                                            }

                                            val scoreBreakdown = suggestionsViewModel.scoreCandidate(
                                                candidateNumPages = gallery.numPages,
                                                tags = gallery.tags,
                                                tagWeights = profile.tagWeights,
                                                tagThemeWeights = profile.tagThemeWeights,
                                                creatorWeights = profile.creatorWeights,
                                                averageNumPages = profile.averageNumPages,
                                                numPagesDeviation = profile.numPagesDeviation,
                                                lengthWeight = suggestionCategoryWeightsSnapshot[SuggestionWeightCategory.LENGTH]
                                                    ?: 1f,
                                                blockedTags = blocked
                                            )
                                            val score = scoreBreakdown.score
                                            if (score <= -1.25f) {
                                                return@async null
                                            }

                                            val thumbExt = gallery.coverExt.trim().ifBlank { "jpg" }
                                            val thumbUrl = if (gallery.mediaId > 0L) {
                                                "https://t.nhentai.net/galleries/${gallery.mediaId}/cover.$thumbExt"
                                            } else {
                                                ""
                                            }
                                            val topTagPreview = scoreBreakdown.rankedTags
                                                .sortedByDescending { it.second }
                                                .map { it.first }
                                                .distinct()
                                                .take(4)
                                            ScoredSuggestionCandidate(
                                                gallery = gallery,
                                                thumbnailUrl = thumbUrl,
                                                score = score,
                                                topTags = topTagPreview,
                                                whySuggestedReason = scoreBreakdown.whySuggestedReason
                                            )
                                        }
                                    }.awaitAll().filterNotNull()
                            }
                        }

                        val ranked = out
                            .sortedByDescending { it.score }
                            .let { rankedRows ->
                                if (sessionExcludedCodes.isEmpty()) {
                                    rankedRows
                                } else {
                                    rankedRows.filterNot { it.gallery.code in sessionExcludedCodes }
                                }
                            }
                        val strong = ranked.filter { it.score > 0f }
                        val fallbackCount = (SUGGESTION_CANDIDATE_TARGET - strong.size).coerceAtLeast(0)
                        val mixedRows = if (strong.size >= SUGGESTION_VISIBLE_TARGET) {
                            strong
                        } else {
                            (strong + ranked.filter { it.score <= 0f }.take(fallbackCount))
                                .distinctBy { it.gallery.code }
                        }
                        val allRows = mixedRows.take(SUGGESTION_CANDIDATE_TARGET).map { candidate ->
                            SuggestedEntryRow(
                                code = candidate.gallery.code,
                                title = candidate.gallery.title.ifBlank { "Gallery ${candidate.gallery.code}" },
                                numPages = candidate.gallery.numPages,
                                uploadDate = candidate.gallery.uploadDate,
                                thumbnailUrl = candidate.thumbnailUrl,
                                topTags = candidate.topTags,
                                score = candidate.score,
                                whySuggestedReason = candidate.whySuggestedReason,
                                duplicateHint = null
                            )
                        }
                        val shownRows = allRows.take(SUGGESTION_VISIBLE_TARGET)
                        SuggestionRefreshResult(
                            rows = shownRows,
                            overflowRows = allRows.drop(shownRows.size),
                            infoMessage = when {
                                includeCreators.isEmpty() -> {
                                    "Mixed suggestions are active. Creator signal improves after reading/rating entries with artist/group tags."
                                }
                                strong.size < 5 -> {
                                    "Only ${strong.size} strong matches right now; showing near-matches too."
                                }
                                else -> null
                            }
                        )
                    }
                }.getOrElse { exc ->
                    errorDialogMessage = "Could not refresh suggestions:\n${exc.message ?: "unknown error"}"
                    SuggestionRefreshResult(rows = emptyList(), infoMessage = null)
                }

                if (computation.rows.isNotEmpty() || suggestedEntries.isEmpty()) {
                    suggestedEntries = computation.rows
                    suggestedOverflowEntries.clear()
                    suggestedOverflowEntries.addAll(computation.overflowRows)
                    suggestedEntriesInfoMessage = computation.infoMessage
                } else {
                    suggestedEntriesInfoMessage = computation.infoMessage
                        ?: "Showing cached recommendations; background refresh found no replacements."
                }
                if (computation.rows.isNotEmpty()) {
                    if (!firstVisibleRecorded) {
                        PerformanceMetrics.recordSuggestionFirstVisibleMillis(
                            android.os.SystemClock.elapsedRealtime() - refreshStartedAt
                        )
                        firstVisibleRecorded = true
                    }
                    val cachedRows = computation.rows + computation.overflowRows
                    withContext(Dispatchers.IO) {
                        suggestionCacheStore.saveRows(cacheFingerprint, cachedRows)
                        val galleries = synchronized(suggestionGalleryCache) {
                            suggestionGalleryCache.values.toList()
                        }
                        suggestionCacheStore.saveGalleryMetadata(galleries)
                    }
                    populateSuggestionDuplicateHintsAsync(computation.rows)
                }
                if (computation.rows.isEmpty()) {
                    setStatus("No suggestions found yet. Try rating/reading more entries, then refresh.")
                } else {
                    setStatus("Updated suggested entries (${computation.rows.size}).")
                }
            } finally {
                if (refreshGeneration == suggestionsRefreshGeneration) {
                    PerformanceMetrics.recordSuggestionTotalMillis(
                        android.os.SystemClock.elapsedRealtime() - refreshStartedAt
                    )
                    suggestedEntriesLoading = false
                    suggestionsRefreshRunning = false
                    suggestionsRefreshJob = null
                }
            }
        }
    }

    private data class SuggestionRefreshResult(
        val rows: List<SuggestedEntryRow>,
        val overflowRows: List<SuggestedEntryRow> = emptyList(),
        val infoMessage: String?
    )

    fun addOrUpdateByInput() {
        addOrUpdateRawInput(codeInput.trim())
    }

    private fun addOrUpdateRawInput(rawInput: String) {
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
                    val insertedNew = libraryRepository.upsertGallery(gallery)
                    if (insertedNew) {
                        registerSessionNewEntryCode(code)
                    }
                    refreshAll(code)
                    setStatus(
                        if (insertedNew) {
                            "Saved new code $code."
                        } else {
                            "Saved/updated code $code."
                        }
                    )
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
        val added = libraryRepository.addCreator(
            name = creatorLink.name,
            creatorType = creatorLink.type,
            sourceUrl = creatorLink.sourceUrl
        )
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
                    if (libraryRepository.addCreator(link.name, link.type, link.sourceUrl)) {
                        addedCount += 1
                    } else {
                        skippedCount += 1
                    }
                }
                addedCount to skippedCount
            }

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
                libraryRepository.upsertGallery(gallery)
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

    fun deleteSelected(removeLocalDownload: Boolean = false) {
        val code = selectedCode
        if (code == null) {
            infoDialogMessage = "Select an entry first."
            return
        }

        if (removeLocalDownload) {
            removeDownloadedEntry(code)
        }
        libraryRepository.deleteEntry(code)
        forgetSessionEntryCode(code)
        refreshAll(null)
        setStatus(
            if (removeLocalDownload) {
                "Deleted code $code and removed its local download."
            } else {
                "Deleted code $code."
            }
        )
    }

    fun clearAllWithoutExport() {
        val deleted = db.clearAllEntries()
        clearSessionNewEntryTracking()
        refreshAll(null)
        infoDialogMessage = "Cleared ${deleted.entriesCleared} entries and ${deleted.creatorsCleared} artists/groups."
        setStatus("Cleared ${deleted.entriesCleared} entries and ${deleted.creatorsCleared} artists/groups.")
    }

    fun setEntryRating(code: Int, rating: Int) {
        val safe = rating.coerceIn(0, 5)
        libraryRepository.setEntryRating(code, safe)
        libraryRepository.setEntryRead(code, true)
        selectedEntryRelatedCache.clear()
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
            ?: libraryRepository.entryDetail(code)?.isRead
            ?: false
        val next = !current
        libraryRepository.setEntryRead(code, next)
        selectedEntryRelatedCache.clear()
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

    fun entryRatingForCode(code: Int): Int {
        if (code <= 0) return 0
        return entries.firstOrNull { it.code == code }?.rating?.coerceIn(0, 5)
            ?: selectedDetail?.takeIf { it.code == code }?.rating?.coerceIn(0, 5)
            ?: libraryRepository.entryDetail(code)?.rating?.coerceIn(0, 5)
            ?: 0
    }

    fun entryReadForCode(code: Int): Boolean {
        if (code <= 0) return false
        return entries.firstOrNull { it.code == code }?.isRead
            ?: selectedDetail?.takeIf { it.code == code }?.isRead
            ?: libraryRepository.entryDetail(code)?.isRead
            ?: false
    }

    fun entryPinnedForCode(code: Int): Boolean {
        if (code <= 0) return false
        return entries.firstOrNull { it.code == code }?.pinned
            ?: db.isEntryPinned(code)
    }

    private enum class SuggestedQuickAction {
        PIN_TOGGLE,
        READ_TOGGLE,
        SET_RATING
    }

    fun quickToggleSuggestedPinned(code: Int) {
        applySuggestedQuickAction(code = code, action = SuggestedQuickAction.PIN_TOGGLE)
    }

    fun quickToggleSuggestedRead(code: Int) {
        applySuggestedQuickAction(code = code, action = SuggestedQuickAction.READ_TOGGLE)
    }

    fun quickSetSuggestedRating(code: Int, rating: Int) {
        applySuggestedQuickAction(
            code = code,
            action = SuggestedQuickAction.SET_RATING,
            rating = rating.coerceIn(0, 5)
        )
    }

    private fun applySuggestedQuickAction(
        code: Int,
        action: SuggestedQuickAction,
        rating: Int = 0
    ) {
        if (code <= 0) return
        if (incognitoModeEnabled) {
            setStatus("Suggestion gestures are disabled in incognito mode.")
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                var detail = libraryRepository.entryDetail(code)
                var insertedNew = false
                if (detail == null) {
                    val fetched = runCatching { client.fetchGallery(code) }.getOrNull()
                    if (fetched != null) {
                        insertedNew = libraryRepository.upsertGallery(fetched)
                        detail = libraryRepository.entryDetail(code)
                    }
                }
                if (detail == null) {
                    return@withContext null
                }
                when (action) {
                    SuggestedQuickAction.PIN_TOGGLE -> {
                        val nextPinned = !db.isEntryPinned(code)
                        libraryRepository.setEntryPinned(code, nextPinned)
                    }
                    SuggestedQuickAction.READ_TOGGLE -> {
                        val nextRead = !(detail?.isRead ?: false)
                        libraryRepository.setEntryRead(code, nextRead)
                    }
                    SuggestedQuickAction.SET_RATING -> {
                        libraryRepository.setEntryRating(code, rating.coerceIn(0, 5))
                        libraryRepository.setEntryRead(code, true)
                    }
                }
                val refreshed = libraryRepository.entryDetail(code)
                SuggestedQuickActionResult(
                    insertedNew = insertedNew,
                    rating = refreshed?.rating?.coerceIn(0, 5) ?: 0,
                    isRead = refreshed?.isRead == true,
                    pinned = db.isEntryPinned(code)
                )
            }
            if (result == null) {
                setStatus("Could not apply suggestion gesture to code $code.")
                return@launch
            }
            if (result.insertedNew) {
                registerSessionNewEntryCode(code)
            }
            triggerSuggestedImportFlash(code)
            delay(520L)
            readAnalyticsLoaded = false
            suggestedEntries = suggestedEntries.filterNot { it.code == code }
            clearSuggestedImportFlash(code)
            refreshAll(code)
            when (action) {
                SuggestedQuickAction.PIN_TOGGLE -> {
                    setStatus(if (result.pinned) "Pinned code $code from suggestions." else "Unpinned code $code from suggestions.")
                }
                SuggestedQuickAction.READ_TOGGLE -> {
                    setStatus(if (result.isRead) "Marked code $code as read from suggestions." else "Marked code $code as unread from suggestions.")
                }
                SuggestedQuickAction.SET_RATING -> {
                    setStatus("Rated code $code as ${result.rating}/5 from suggestions.")
                }
            }
        }
    }

    private data class SuggestedQuickActionResult(
        val insertedNew: Boolean,
        val rating: Int,
        val isRead: Boolean,
        val pinned: Boolean
    )

    fun quickToggleEntryPinned(code: Int) {
        if (code <= 0) return
        if (incognitoModeEnabled) {
            setStatus("Pin/unpin is disabled in incognito mode.")
            return
        }
        val currentPinned = entries.firstOrNull { it.code == code }?.pinned
            ?: db.isEntryPinned(code)
        val nextPinned = !currentPinned
        libraryRepository.setEntryPinned(code, nextPinned)
        loadEntries(code)
        selectEntry(code)
        setStatus(
            if (nextPinned) {
                "Pinned code $code."
            } else {
                "Unpinned code $code."
            }
        )
    }

    fun quickToggleEntryRead(code: Int) {
        toggleEntryRead(code)
    }

    fun updateBrowserRatingSelection(rating: Int) {
        val prompt = browserRatingPromptState ?: return
        browserRatingPromptState = prompt.copy(rating = rating.coerceIn(0, 5))
    }

    fun updateBrowserRatingReread(isReread: Boolean) {
        val prompt = browserRatingPromptState ?: return
        browserRatingPromptState = prompt.copy(isReread = isReread && prompt.wasReadBefore)
    }

    fun saveBrowserRatingPrompt() {
        val prompt = browserRatingPromptState ?: return
        val safeRating = prompt.rating.coerceIn(0, 5)
        if (prompt.isReread) {
            db.recordEntryRatingSession(prompt.code, safeRating, isReread = true)
        } else {
            libraryRepository.setEntryRating(prompt.code, safeRating)
            libraryRepository.setEntryRead(prompt.code, true)
            selectedEntryRelatedCache.clear()
        }
        readAnalyticsLoaded = false
        browserRatingPromptState = null
        pendingBrowserRatingCode = null
        pendingBrowserRatingWasRead = false
        loadEntries(prompt.code)
        selectEntry(prompt.code)
        setStatus(
            if (prompt.isReread) {
                "Saved reread rating for ${prompt.code} without changing the original rating."
            } else {
                "Saved rating for ${prompt.code} and marked as read."
            }
        )
    }

    fun skipBrowserRatingPrompt() {
        browserRatingPromptState = null
        pendingBrowserRatingCode = null
        pendingBrowserRatingWasRead = false
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
        libraryRepository.setEntryPinned(code, newPinned)
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
                    if (libraryRepository.addCreator(creator.name, creator.type, creator.sourceUrl)) {
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
            val snapshot = runCatching {
                BackupSnapshotExport.buildSnapshotWithSettings(
                    db = db,
                    prefs = prefs,
                    backupImporter = backupImporter,
                    entryPinPriorityEnabled = entryPinPriorityEnabled
                )
            }
            snapshot.onFailure { exc ->
                errorDialogMessage = "Export failed:\n${exc.message ?: "unknown error"}"
                return@launch
            }

            val json = snapshot.getOrNull() ?: JSONObject()
            val exportText = BackupSerializer.serialize(json)
            val exportResult = runCatching {
                withContext(Dispatchers.IO) {
                    writeTextToUri(uri, exportText)
                    if (backupThumbnailArchiveEnabled && autoBackupTreeUri.isNotBlank()) {
                        syncBackupThumbnailArchiveWithProgress(
                            treeUriString = autoBackupTreeUri,
                            label = "Syncing backup thumbnails for export..."
                        )
                    } else {
                        null
                    }
                }
            }
            clearBackupProgress()
            exportResult.onFailure { exc ->
                errorDialogMessage = "Export failed:\n${exc.message ?: "unknown error"}"
                return@launch
            }
            val thumbnailSyncResult = exportResult.getOrNull()

            val entryCount = json.optJSONArray("entries")?.length() ?: 0
            val creatorCount = json.optJSONArray("creators")?.length() ?: 0
            val popularTagCount = json.optJSONArray("popular_tags")?.length() ?: 0
            val entryHeatmapCacheCount = json.optJSONArray("entry_heatmap_cache")?.length() ?: 0
            val hiddenCount = json.optJSONArray("hidden_suggested_codes")?.length() ?: 0
            val weightCount = json.optJSONObject("suggestion_category_weights")?.length() ?: 0
            val dailyActivityCount = json.optJSONArray("daily_read_activity")?.length() ?: 0
            val readingSessionCount = json.optJSONArray("reading_sessions")?.length() ?: 0
            val thumbCount = thumbnailSyncResult?.syncedCount ?: 0
            val thumbBytes = thumbnailSyncResult?.totalBytes ?: 0L
            if (clearAfterExport) {
                val deleted = db.clearAllEntries()
                clearSessionNewEntryTracking()
                refreshAll(null)
                infoDialogMessage =
                    if (backupThumbnailArchiveEnabled && autoBackupTreeUri.isNotBlank()) {
                        "Exported $entryCount entries, $creatorCount creators/groups, $hiddenCount hidden suggestions, $weightCount suggestion weights, $dailyActivityCount daily activity rows, and $readingSessionCount reading sessions, synced $thumbCount backup covers (${formatStorageSize(thumbBytes)} thumbnail archive), then cleared ${deleted.entriesCleared} entries and ${deleted.creatorsCleared} artists/groups."
                    } else {
                        "Exported $entryCount entries, $creatorCount creators/groups, $popularTagCount popular-tag cache rows, $entryHeatmapCacheCount entry heatmap cache rows, $hiddenCount hidden suggestions, $weightCount suggestion weights, $dailyActivityCount daily activity rows, and $readingSessionCount reading sessions, then cleared ${deleted.entriesCleared} entries and ${deleted.creatorsCleared} artists/groups."
                    }
                setStatus(
                    if (backupThumbnailArchiveEnabled && autoBackupTreeUri.isNotBlank()) {
                        "Exported data, synced $thumbCount backup covers, and cleared ${deleted.entriesCleared} entries and ${deleted.creatorsCleared} artists/groups."
                    } else {
                        "Exported and cleared ${deleted.entriesCleared} entries and ${deleted.creatorsCleared} artists/groups."
                    }
                )
            } else {
                infoDialogMessage =
                    if (backupThumbnailArchiveEnabled && autoBackupTreeUri.isNotBlank()) {
                        "Exported $entryCount entries, $creatorCount creators/groups, $popularTagCount popular-tag cache rows, $hiddenCount hidden suggestions, $weightCount suggestion weights, $dailyActivityCount daily activity rows, and $readingSessionCount reading sessions. Synced $thumbCount backup covers (${formatStorageSize(thumbBytes)} thumbnail archive)."
                    } else {
                        "Exported $entryCount entries, $creatorCount creators/groups, $popularTagCount popular-tag cache rows, $entryHeatmapCacheCount entry heatmap cache rows, $hiddenCount hidden suggestions, $weightCount suggestion weights, $dailyActivityCount daily activity rows, and $readingSessionCount reading sessions."
                    }
                setStatus(
                    if (backupThumbnailArchiveEnabled && autoBackupTreeUri.isNotBlank()) {
                        "Exported $entryCount entries, $creatorCount creators/groups, $hiddenCount hidden suggestions, $weightCount suggestion weights, $dailyActivityCount daily activity rows, $readingSessionCount reading sessions, $entryHeatmapCacheCount entry heatmap cache rows, and synced $thumbCount backup covers."
                    } else {
                        "Exported $entryCount entries, $creatorCount creators/groups, $hiddenCount hidden suggestions, $weightCount suggestion weights, $dailyActivityCount daily activity rows, $readingSessionCount reading sessions, and $entryHeatmapCacheCount entry heatmap cache rows."
                    }
                )
            }
        }
    }

    fun exportCsvToUri(uri: Uri) {
        viewModelScope.launch {
            val snapshotResult = runCatching {
                BackupSnapshotExport.buildSnapshotWithSettings(
                    db = db,
                    prefs = prefs,
                    backupImporter = backupImporter,
                    entryPinPriorityEnabled = entryPinPriorityEnabled
                )
            }
            snapshotResult.onFailure { exc ->
                errorDialogMessage = "CSV export failed:\n${exc.message ?: "unknown error"}"
                return@launch
            }
            val snapshot = snapshotResult.getOrNull() ?: JSONObject()
            val exportResult = runCatching {
                withContext(Dispatchers.IO) {
                    writeTextToUri(uri, BackupSnapshotExport.toCsv(snapshot, savedStats))
                }
            }
            exportResult.onFailure { exc ->
                errorDialogMessage = "CSV export failed:\n${exc.message ?: "unknown error"}"
                return@launch
            }
            val entryCount = snapshot.optJSONArray("entries")?.length() ?: 0
            val creatorCount = snapshot.optJSONArray("creators")?.length() ?: 0
            val dailyActivityCount = snapshot.optJSONArray("daily_read_activity")?.length() ?: 0
            val readingSessionCount = snapshot.optJSONArray("reading_sessions")?.length() ?: 0
            val popularTagCount = snapshot.optJSONArray("popular_tags")?.length() ?: 0
            infoDialogMessage =
                "Exported CSV with $entryCount entries, $creatorCount creators/groups, $dailyActivityCount daily activity rows, $readingSessionCount reading sessions, and $popularTagCount popular-tag rows."
            setStatus("CSV export completed.")
        }
    }

    fun refetchAllEntries() {
        if (batchProgressState != null) {
            infoDialogMessage = "A batch operation is already running."
            return
        }

        viewModelScope.launch {
            val codes = withContext(Dispatchers.IO) { libraryRepository.allEntryCodes() }
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
                backupImporter.parse(text)
            } catch (exc: Exception) {
                errorDialogMessage = "Import failed:\n${exc.message ?: "Invalid import file."}"
                return@launch
            }

            val result = runCatching {
                db.importSnapshot(
                    entries = payload.entries,
                    creators = payload.creators,
                    popularTags = payload.popularTags,
                    entryHeatmapCache = payload.entryHeatmapCache,
                    subscriptions = payload.subscriptions,
                    subscriptionSeenCodes = payload.subscriptionSeenCodes,
                    subscriptionEvents = payload.subscriptionEvents,
                    dailyReadActivity = payload.dailyReadActivity,
                    readingSessions = payload.readingSessions
                )
            }
            result.onFailure { exc ->
                errorDialogMessage = "Import failed:\n${exc.message ?: "unknown error"}"
                return@launch
            }

            val import = result.getOrNull() ?: return@launch
            if (import.insertedCodes.isNotEmpty()) {
                import.insertedCodes.forEach { registerSessionNewEntryCode(it) }
            }
            if (!payload.hiddenSuggestedEntries.isNullOrEmpty()) {
                applyImportedHiddenSuggestedEntries(payload.hiddenSuggestedEntries)
            } else {
                payload.hiddenSuggestedCodes?.let { applyImportedHiddenSuggestedCodes(it) }
            }
            payload.suggestionCategoryWeights?.let { applyImportedSuggestionCategoryWeights(it) }
            payload.entryPinPriorityEnabled?.let { applyImportedEntryPinPriority(it) }
            refreshAll(null)
            reloadSubscriptionsState()
            if (!suggestedEntriesCollapsed && !incognitoModeEnabled) {
                refreshSuggestedEntries(force = true)
            }
            val restoredHiddenCountText = payload.hiddenSuggestedEntries?.size?.toString()
                ?: payload.hiddenSuggestedCodes?.size?.toString()
                ?: "Not included"
            val restoredWeightCountText = payload.suggestionCategoryWeights?.size?.toString() ?: "Not included"
            val restoredPinPriorityText = payload.entryPinPriorityEnabled?.let {
                if (it) "Enabled" else "Disabled"
            } ?: "Not included"
            val restoredPopularTagCountText = import.popularTagRows?.toString() ?: "Not included"
            val restoredEntryHeatmapCacheText = import.entryHeatmapCacheRows?.toString() ?: "Not included"
            val restoredSubscriptionCountText = import.subscriptionRows?.toString() ?: "Not included"
            val restoredSubscriptionSeenCountText = import.subscriptionSeenRows?.toString() ?: "Not included"
            val restoredSubscriptionEventCountText = import.subscriptionEventRows?.toString() ?: "Not included"
            val restoredDailyActivityCountText = import.dailyReadActivityRows?.toString() ?: "Not included"
            val restoredReadingSessionCountText = import.readingSessionRows?.toString() ?: "Not included"
            infoDialogMessage = """
                Entries processed: ${import.processed}
                Entries added: ${import.inserted}
                Entry duplicates: ${import.updated}
                Entries skipped: ${import.skipped}
                
                Artists processed: ${import.artistsProcessed}
                Artists added: ${import.artistsAdded}
                Artist duplicates: ${import.artistsDuplicates}
                Artists skipped: ${import.artistsSkipped}
                
                Groups processed: ${import.groupsProcessed}
                Groups added: ${import.groupsAdded}
                Group duplicates: ${import.groupsDuplicates}
                Groups skipped: ${import.groupsSkipped}
                
                Creator rows processed: ${import.creatorsProcessed}
                Creator rows added: ${import.creatorsAdded}
                Creator row duplicates: ${import.creatorsDuplicates}
                Creator rows skipped: ${import.creatorsSkipped}

                Hidden suggested entries restored: $restoredHiddenCountText
                Suggestion weight settings restored: $restoredWeightCountText
                Pin priority setting restored: $restoredPinPriorityText
                Popular tag cache rows restored: $restoredPopularTagCountText
                Entry heatmap cache rows restored: $restoredEntryHeatmapCacheText
                Subscription rows restored: $restoredSubscriptionCountText
                Subscription seen-code rows restored: $restoredSubscriptionSeenCountText
                Subscription event rows restored: $restoredSubscriptionEventCountText
                Daily activity rows restored: $restoredDailyActivityCountText
                Reading sessions restored: $restoredReadingSessionCountText
            """.trimIndent()
            setStatus(
                "Import complete. Entries: +${import.inserted}, duplicates ${import.updated}, skipped ${import.skipped}. " +
                    "Artists: +${import.artistsAdded}, duplicates ${import.artistsDuplicates}. " +
                    "Groups: +${import.groupsAdded}, duplicates ${import.groupsDuplicates}. " +
                    "Hidden suggestions: $restoredHiddenCountText, weights: $restoredWeightCountText, popular tags: $restoredPopularTagCountText, entry heatmap cache: $restoredEntryHeatmapCacheText, " +
                    "subscriptions: $restoredSubscriptionCountText, seen rows: $restoredSubscriptionSeenCountText, event rows: $restoredSubscriptionEventCountText, " +
                    "daily activity: $restoredDailyActivityCountText, sessions: $restoredReadingSessionCountText."
            )
        }
    }

    fun defaultExportFilename(): String {
        return "sauce_export_${LocalDateTime.now().format(EXPORT_FILENAME_FORMAT)}.txt"
    }

    fun defaultCsvExportFilename(): String {
        return "sauce_export_${LocalDateTime.now().format(EXPORT_FILENAME_FORMAT)}.csv"
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
                    val insertedNew = libraryRepository.upsertGallery(gallery)
                    if (insertedNew) {
                        registerSessionNewEntryCode(code)
                    }
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
        selectedEntryRelatedCache.clear()
        loadTags()
        loadEntries(selectCode)
        loadCreators()
        loadSavedStats()
        if (suggestedEntries.isNotEmpty()) {
            val importedCodes = libraryRepository.allEntryCodes().toSet()
            suggestedEntries = suggestionsViewModel.excludeImported(suggestedEntries, importedCodes)
            if (suggestedOverflowEntries.isNotEmpty()) {
                suggestedOverflowEntries.removeAll { it.code in importedCodes || it.code in hiddenSuggestedCodes }
            }
        }
        readAnalyticsLoaded = false
        tagGraphLoaded = false
        tagGraphSnapshot = null
        tagGraphErrorMessage = null
        entryHeatmapCacheNonce += 1L
        updateEntryHeatmapCacheStatus(null)
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

    suspend fun readEntriesForDay(day: LocalDate): List<DayReadEntryRow> {
        return withContext(Dispatchers.IO) {
            db.listReadEntriesForDay(day)
        }
    }

    fun ensureTagGraphLoaded(forceRefresh: Boolean = false) {
        if (tagGraphLoading) return
        if (tagGraphLoaded && !forceRefresh && tagGraphSnapshot != null) return

        tagGraphLoading = true
        tagGraphErrorMessage = null
        tagGraphLoadJob?.cancel()
        tagGraphLoadJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { HeatmapEngine.computeTagGraphSnapshot(heatmapRepository.graphData()) }
            }
            if (!isActive) return@launch
            result.onSuccess { snapshot ->
                tagGraphSnapshot = snapshot
                tagGraphLoaded = true
                preloadTagGraphEntryThumbnails(snapshot.entryNodes)
                updateEntryHeatmapCacheStatus(snapshot)
            }.onFailure { exc ->
                tagGraphErrorMessage = exc.message ?: "unknown error"
                setStatus("Could not build tag graph: ${exc.message ?: "unknown error"}")
                updateEntryHeatmapCacheStatus(null)
            }
            tagGraphLoading = false
        }
    }

    fun releaseTagGraphSession() {
        if (entryHeatmapCacheRecalculationRunning) return
        tagGraphLoadJob?.cancel()
        tagGraphLoadJob = null
        tagGraphThumbnailPreloadJob?.cancel()
        tagGraphThumbnailPreloadJob = null
        tagGraphSnapshot = null
        tagGraphLoaded = false
        tagGraphLoading = false
        tagGraphErrorMessage = null
    }

    private fun preloadTagGraphEntryThumbnails(entryNodes: List<TagGraphEntryNode>) {
        tagGraphThumbnailPreloadJob?.cancel()
        if (entryNodes.isEmpty()) return
        val appContext = getApplication<Application>().applicationContext
        val candidates = entryNodes
            .asSequence()
            .filter { it.thumbnailUrl.isNotBlank() }
            .distinctBy { it.code }
            .take(72)
            .toList()
        tagGraphThumbnailPreloadJob = viewModelScope.launch {
            val parallelism = Runtime.getRuntime().availableProcessors().coerceIn(4, 12)
            candidates.chunked(parallelism).forEach { chunk ->
                coroutineScope {
                    chunk.map { entry ->
                        async(Dispatchers.IO) {
                            if (ThumbnailBitmapCache.get(entry.thumbnailUrl) == null) {
                                fetchThumbnailBitmap(
                                    context = appContext,
                                    url = entry.thumbnailUrl,
                                    backupCode = entry.code
                                )?.let { bitmap ->
                                    ThumbnailBitmapCache.put(entry.thumbnailUrl, bitmap)
                                }
                            }
                        }
                    }.awaitAll()
                }
            }
        }
    }

    private fun updateEntryHeatmapCacheStatus(snapshotHint: TagGraphSnapshot? = tagGraphSnapshot) {
        viewModelScope.launch {
            val statusText = withContext(Dispatchers.IO) {
                val record = heatmapRepository.cacheRecord()
                if (record == null) {
                    "Entry heatmap cache: Not calculated"
                } else {
                    val expectedKey = snapshotHint?.let(::tagGraphEntryLayoutCacheKey)
                    val stateLabel = when {
                        expectedKey == null -> "Saved"
                        expectedKey == record.cacheKey -> "Ready"
                        else -> "Outdated"
                    }
                    val updatedLabel = record.updatedAt.ifBlank { "Unknown time" }
                    "Entry heatmap cache: $stateLabel ($updatedLabel)"
                }
            }
            entryHeatmapCacheStatusText = statusText
        }
    }

    suspend fun loadEntryHeatmapLayoutForSnapshot(
        cacheKey: String,
        snapshot: TagGraphSnapshot
    ): TagGraphEntryLayoutResult? = withContext(Dispatchers.IO) {
        heatmapRepository.load(cacheKey, snapshot)
    }

    fun recalculateEntryHeatmapCache() {
        if (entryHeatmapCacheRecalculationRunning) return
        entryHeatmapCacheRecalculationRunning = true
        entryHeatmapCacheCompletionSummary = null
        entryHeatmapCacheProgressLabel = "Building tag graph snapshot..."
        entryHeatmapCacheProgressFraction = 0.08f
        viewModelScope.launch {
            setStatus("Recalculating entry heatmap cache. This is hardware intensive and may make the phone feel hot or sluggish until it finishes.")
            val snapshotResult = withContext(Dispatchers.Default) {
                runCatching { HeatmapEngine.computeTagGraphSnapshot(heatmapRepository.graphData()) }
            }
            snapshotResult.onFailure { exc ->
                entryHeatmapCacheRecalculationRunning = false
                entryHeatmapCacheProgressFraction = null
                errorDialogMessage = "Entry heatmap recalculation failed:\n${exc.message ?: "unknown error"}"
                setStatus("Entry heatmap recalculation failed.")
                updateEntryHeatmapCacheStatus(tagGraphSnapshot)
                return@launch
            }
            val snapshot = snapshotResult.getOrNull()
            if (snapshot == null || snapshot.entryNodes.isEmpty()) {
                entryHeatmapCacheProgressLabel = "Clearing empty entry heatmap cache..."
                entryHeatmapCacheProgressFraction = 0.55f
                withContext(Dispatchers.IO) {
                    heatmapRepository.clear()
                }
                tagGraphSnapshot = snapshot
                tagGraphLoaded = snapshot != null
                entryHeatmapCacheNonce += 1L
                entryHeatmapCacheRecalculationRunning = false
                entryHeatmapCacheProgressFraction = null
                updateEntryHeatmapCacheStatus(snapshot)
                infoDialogMessage = "No local entries were available for the entry heatmap cache, so the saved layout was cleared."
                setStatus("Entry heatmap cache cleared because there were no local entries to solve.")
                return@launch
            }
            entryHeatmapCacheProgressLabel = "Solving entry heatmap layout for ${snapshot.entryNodes.size} entries..."
            entryHeatmapCacheProgressFraction = 0.28f
            val layoutResult = withContext(Dispatchers.Default) {
                HeatmapEngine.computeLegacyEntryLayoutResult(
                    snapshot = snapshot,
                    graphWidthPx = ENTRY_HEATMAP_CACHE_SOLVER_WIDTH_PX,
                    graphHeightPx = ENTRY_HEATMAP_CACHE_SOLVER_HEIGHT_PX,
                    minimumVisualSpacingPx = ENTRY_HEATMAP_CACHE_SPACING_PX
                )
            }
            val cacheKey = tagGraphEntryLayoutCacheKey(snapshot)
            entryHeatmapCacheProgressLabel = "Saving reusable entry heatmap cache..."
            entryHeatmapCacheProgressFraction = 0.82f
            withContext(Dispatchers.IO) {
                heatmapRepository.save(cacheKey, layoutResult)
            }
            tagGraphSnapshot = snapshot
            tagGraphLoaded = true
            entryHeatmapCacheProgressLabel = "Preloading heatmap thumbnails..."
            entryHeatmapCacheProgressFraction = 0.92f
            preloadTagGraphEntryThumbnails(snapshot.entryNodes)
            entryHeatmapCacheNonce += 1L
            entryHeatmapCacheRecalculationRunning = false
            entryHeatmapCacheProgressFraction = null
            val dominantFamilies = layoutResult.familyCircles
                .asSequence()
                .filter { "|" !in it.tagName }
                .sortedByDescending { it.entryCount }
                .map { it.label }
                .distinct()
                .take(18)
                .toList()
            entryHeatmapCacheCompletionSummary = EntryHeatmapRecalculationSummary(
                entryCount = snapshot.entryNodes.size,
                dominantFamilies = dominantFamilies
            )
            updateEntryHeatmapCacheStatus(snapshot)
            setStatus("Entry heatmap cache recalculated for ${snapshot.entryNodes.size} entries.")
        }
    }

    fun dismissEntryHeatmapCacheCompletionSummary() {
        entryHeatmapCacheCompletionSummary = null
    }

    fun prepareTagGraphData() {
        loadPopularTags()
        if (popularTags.isEmpty()) {
            if (popularTagsFetchInProgress) return
            popularTagsFetchInProgress = true
            viewModelScope.launch {
                setStatus("Fetching popular tags for graph...")
                val result = withContext(Dispatchers.IO) {
                    runCatching { client.fetchAllPopularTags() }
                }
                val payload = result.getOrNull()
                if (payload != null) {
                    withContext(Dispatchers.IO) {
                        db.replacePopularTags(payload.tags)
                    }
                    loadPopularTags()
                    tagGraphLoaded = false
                    tagGraphSnapshot = null
                    ensureTagGraphLoaded(forceRefresh = true)
                    updateEntryHeatmapCacheStatus(null)
                    setStatus(
                        if (payload.tags.isEmpty()) {
                            "No popular tags were found on nhentai."
                        } else {
                            "Fetched ${payload.tags.size} tags across ${payload.pagesFetched} page(s) for graph."
                        }
                    )
                } else {
                    val exc = result.exceptionOrNull()
                    errorDialogMessage = exc?.message ?: "Failed to fetch popular tags."
                    setStatus("Fetching popular tags failed.")
                    updateEntryHeatmapCacheStatus(null)
                }
                popularTagsFetchInProgress = false
            }
        } else {
            ensureTagGraphLoaded(forceRefresh = false)
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
                    libraryRepository.entries(
                        textFilter = textSnapshot,
                        tagFilterIds = tagSnapshot,
                        sortField = sortField,
                        sortDirection = sortDirection,
                        readFilter = entryReadFilter,
                        prioritizePinned = entryPinPriorityEnabled
                    )
                }

                val targetCode = when {
                    selectedCode != null && loadedEntries.any { it.code == selectedCode } -> selectedCode
                    loadedEntries.size == 1 -> loadedEntries.first().code
                    else -> null
                }
                val detail = withContext(Dispatchers.IO) {
                    targetCode?.let { libraryRepository.entryDetail(it) }
                }

                entries = loadedEntries
                selectedCode = targetCode
                selectedSummary = targetCode?.let { target ->
                    loadedEntries.firstOrNull { it.code == target }
                }
                selectedDetail = detail
                selectedDetailLoading = false
                scheduleSelectedEntrySupport(targetCode, detail)
                startupPreloadState = StartupPreloadState(
                    phase = "Loading tags...",
                    completedSteps = 1,
                    totalSteps = totalSteps
                )

                val loadedTags = withContext(Dispatchers.IO) {
                    libraryRepository.tags(
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
                    val ref = libraryRepository.tagRoute(tagId)
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
                    libraryRepository.creators(
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
                    data class PreloadThumbnailRequest(
                        val url: String,
                        val backupCode: Int
                    )

                    val allRequests = loadedEntries
                        .asSequence()
                        .mapNotNull { entry ->
                            val url = entry.thumbnailUrl.trim()
                            if (url.isBlank()) {
                                null
                            } else {
                                PreloadThumbnailRequest(
                                    url = url,
                                    backupCode = entry.code
                                )
                            }
                        }
                        .distinctBy { it.url }
                        .toList()
                    val selectedCount = ((allRequests.size * (preloadPercent / 100f)).roundToInt())
                        .coerceIn(0, allRequests.size)
                    val prioritizedHeadCount = if (preloadPercent > 0) {
                        THUMB_PRELOAD_TOP_PRIORITY_COUNT.coerceAtMost(allRequests.size)
                    } else {
                        0
                    }
                    val targetCount = maxOf(selectedCount, prioritizedHeadCount)
                        .coerceIn(0, allRequests.size)
                    val requests = buildList {
                        addAll(
                            allRequests
                                .drop(prioritizedHeadCount)
                                .take((targetCount - prioritizedHeadCount).coerceAtLeast(0))
                        )
                        addAll(allRequests.take(prioritizedHeadCount))
                    }.distinctBy { it.url }
                    val missingRequests = requests.filter { ThumbnailBitmapCache.get(it.url, lowRes = false) == null }
                    val thumbsTotal = missingRequests.size
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
                        val appContext = getApplication<Application>().applicationContext
                        missingRequests.chunked(parallelism * 12).forEach { preloadBatch ->
                            val archivedBitmaps = withContext(Dispatchers.IO) {
                                readBackupThumbnailBitmapsByCode(
                                    context = appContext,
                                    codes = preloadBatch.map { it.backupCode }.toSet()
                                )
                            }
                            archivedBitmaps.forEach { (code, bitmap) ->
                                preloadBatch.firstOrNull { it.backupCode == code }?.let { request ->
                                    ThumbnailBitmapCache.put(request.url, bitmap.asImageBitmap(), lowRes = false)
                                }
                            }
                            val networkBatch = preloadBatch.filter { request ->
                                archivedBitmaps[request.backupCode] == null
                            }
                            networkBatch.chunked(parallelism).forEach { batch ->
                                batch.map { request ->
                                async(Dispatchers.IO) {
                                    runCatching {
                                        fetchThumbnailBitmap(
                                            context = appContext,
                                            url = request.url,
                                            backupCode = request.backupCode
                                        )
                                    }
                                        .getOrNull()
                                        ?.let { fetched -> ThumbnailBitmapCache.put(request.url, fetched, lowRes = false) }
                                }
                                }.awaitAll()
                            }
                            done += preloadBatch.size
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
                DuplicateThumbnailHashCache.clear()
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

    private fun loadEntries(
        selectCode: Int?,
        autoSelectFirst: Boolean = true,
        forceIncludeCode: Int? = null
    ) {
        val rawEntries = libraryRepository.entries(
            textFilter = entrySearch,
            tagFilterIds = activeTagFilterIds.toList(),
            sortField = sortField,
            sortDirection = sortDirection,
            readFilter = entryReadFilter,
            prioritizePinned = entryPinPriorityEnabled
        )
        val filteredEntries = if (entryReadFilter == EntryReadFilterMode.DOWNLOADED) {
            rawEntries.filter { row -> row.code in entryDownloadController.downloadedCodes }
        } else {
            rawEntries
        }
        val forcedEntry = forceIncludeCode
            ?.takeIf { target -> filteredEntries.none { it.code == target } }
            ?.let(libraryRepository::entryRow)
        entries = includeDirectNavigationEntry(filteredEntries, forcedEntry)
        reconcileSessionEntryTracking(entries)

        val targetCode = when {
            selectCode != null && entries.any { it.code == selectCode } -> selectCode
            selectedCode != null && entries.any { it.code == selectedCode } -> selectedCode
            autoSelectFirst && entries.size == 1 -> entries.first().code
            else -> null
        }

        selectedCode = targetCode
        selectedSummary = targetCode?.let { target ->
            entries.firstOrNull { it.code == target }
        }
        selectedDetail = targetCode?.let { libraryRepository.entryDetail(it) }
        selectedDetailLoading = false
        scheduleSelectedEntrySupport(targetCode, selectedDetail)
    }

    private fun loadTags() {
        tags = libraryRepository.tags(
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
            val ref = libraryRepository.tagRoute(tagId)
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
        creators = libraryRepository.creators(
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

    private fun reloadSubscriptionsState() {
        viewModelScope.launch {
            val loadedSubscriptions = withContext(Dispatchers.IO) { subscriptionRepository.list() }
            val loadedEvents = withContext(Dispatchers.IO) { subscriptionRepository.events() }
            subscriptions = loadedSubscriptions
            subscriptionEvents = loadedEvents
        syncSubscriptionBackgroundWork(
            context = getApplication<Application>().applicationContext,
            hasSubscriptions = loadedSubscriptions.isNotEmpty(),
            intervalHours = subscriptionRefreshIntervalHours
        )
            syncSubscriptionNotificationSummary()
        }
    }

    private suspend fun maybeAutoRefreshSubscriptions() {
        if (subscriptionAutoRefreshAttempted) return
        subscriptionAutoRefreshAttempted = true
        val currentSubscriptions = withContext(Dispatchers.IO) { subscriptionRepository.list() }
        subscriptions = currentSubscriptions
        if (currentSubscriptions.isEmpty()) {
            subscriptionEvents = withContext(Dispatchers.IO) { subscriptionRepository.events() }
            syncSubscriptionNotificationSummary()
            return
        }
        refreshSubscriptions(manual = false)
    }

    fun toggleSubscriptionsCardCollapsed() {
        subscriptionsCardCollapsed = !subscriptionsCardCollapsed
    }

    fun isRouteSubscribed(routeType: String, routeName: String): Boolean {
        val key = subscriptionRouteKey(routeType, routeName)
        if (key.isBlank()) return false
        return subscriptions.any { subscriptionRouteKey(it.routeType, it.routeName) == key }
    }

    fun subscriptionForRoute(routeType: String, routeName: String): SubscriptionRow? {
        val key = subscriptionRouteKey(routeType, routeName)
        if (key.isBlank()) return null
        return subscriptions.firstOrNull { subscriptionRouteKey(it.routeType, it.routeName) == key }
    }

    private fun filteredSubscriptionEvents(): List<SubscriptionEventRow> {
        val parsedSearch = parseSearchQuery(entrySearch)
        val hasSearchFilter = parsedSearch.freeText.isNotBlank() || parsedSearch.filters.isNotEmpty()
        val hasTagFilter = activeTagFilterIds.isNotEmpty()
        val hasScopedEntries = hasSearchFilter || hasTagFilter || entryReadFilter != EntryReadFilterMode.ALL
        val filteredEntryCodes = if (hasScopedEntries) {
            entries.asSequence().map { it.code }.toSet()
        } else {
            emptySet()
        }
        val selectedRouteKeys = if (hasTagFilter) activeTagFilterRouteKeys() else emptySet()

        return subscriptionEvents
            .asSequence()
            .filter { event ->
                event.matchesCurrentSubscriptionFilters(
                    parsedSearch = parsedSearch,
                    hasSearchFilter = hasSearchFilter,
                    hasTagFilter = hasTagFilter,
                    filteredEntryCodes = filteredEntryCodes,
                    selectedRouteKeys = selectedRouteKeys
                )
            }
            .sortedWith(compareByDescending<SubscriptionEventRow> { it.pinned }.thenByDescending { it.discoveredAt })
            .toList()
    }

    private fun SubscriptionEventRow.matchesCurrentSubscriptionFilters(
        parsedSearch: ParsedSearchQuery,
        hasSearchFilter: Boolean,
        hasTagFilter: Boolean,
        filteredEntryCodes: Set<Int>,
        selectedRouteKeys: Set<String>
    ): Boolean {
        val codeMatchesFilteredLibrary = code in filteredEntryCodes
        val eventRouteKey = subscriptionRouteKey(routeType, routeName)
        if (hasTagFilter && !codeMatchesFilteredLibrary && eventRouteKey !in selectedRouteKeys) {
            return false
        }
        if (hasSearchFilter && codeMatchesFilteredLibrary) {
            return true
        }

        val normalizedTitle = normalizeTagName(title)
        val normalizedRouteName = normalizeTagName(routeName)
        val normalizedRouteType = routeType.trim().lowercase(Locale.US)
        val normalizedSourceUrl = sourceUrl.trim().lowercase(Locale.US)
        val normalizedUploadDate = uploadDate.trim().lowercase(Locale.US)
        val codeText = code.toString()
        val pagesText = numPages.toString()

        fun matchesUniversalTerm(rawTerm: String): Boolean {
            val normalizedTerm = normalizeTagName(rawTerm)
            if (normalizedTerm.isBlank()) return true
            parseCode(rawTerm)?.let { parsedCode ->
                if (parsedCode == code) return true
            }
            return codeText.contains(normalizedTerm) ||
                pagesText.contains(normalizedTerm) ||
                normalizedTitle.contains(normalizedTerm) ||
                normalizedRouteName.contains(normalizedTerm) ||
                normalizedRouteType.contains(normalizedTerm) ||
                normalizedSourceUrl.contains(normalizedTerm) ||
                normalizedUploadDate.contains(normalizedTerm)
        }

        fun matchesFieldFilter(filter: SearchFieldFilter): Boolean {
            val value = filter.value.trim()
            val normalizedValue = normalizeTagName(value)
            if (normalizedValue.isBlank()) return true
            return when (filter.key) {
                "code" -> {
                    val cleaned = value.removePrefix("#").trim()
                    val parsedCode = cleaned.toIntOrNull()
                    if (parsedCode != null) code == parsedCode else codeText.contains(normalizedValue)
                }
                "title", "subtitle" -> normalizedTitle.contains(normalizedValue)
                "pages" -> {
                    val pageNumbers = extractNumericTokens(value)
                    when {
                        pageNumbers.size >= 2 -> {
                            val start = minOf(pageNumbers[0], pageNumbers[1])
                            val end = maxOf(pageNumbers[0], pageNumbers[1])
                            numPages in start..end
                        }
                        pageNumbers.size == 1 -> numPages == pageNumbers.first()
                        else -> pagesText.contains(normalizedValue)
                    }
                }
                "upload", "fetched", "added" -> normalizedUploadDate.contains(normalizedValue) || matchesUniversalTerm(value)
                "url" -> normalizedSourceUrl.contains(normalizedValue)
                "tag" -> normalizedRouteName.contains(normalizedValue) || normalizedRouteType.contains(normalizedValue)
                "type" -> normalizedRouteType.contains(normalizedValue)
                "artist", "group", "parody", "character", "category", "language" -> {
                    normalizedRouteType == filter.key && normalizedRouteName.contains(normalizedValue)
                }
                else -> matchesUniversalTerm(value)
            }
        }

        val freeTerms = extractSearchEverythingBrowserTerms(parsedSearch.freeText)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { parsedSearch.freeText.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList() }
        if (freeTerms.any { term -> !matchesUniversalTerm(term) }) {
            return false
        }
        if (parsedSearch.filters.any { filter -> !matchesFieldFilter(filter) }) {
            return false
        }
        return true
    }

    fun toggleTagSubscription(tagId: Long) {
        val ref = getTagRouteRef(tagId) ?: return
        toggleSubscriptionRoute(ref.type, ref.name)
    }

    fun toggleCreatorSubscription(creatorType: String, creatorName: String) {
        toggleSubscriptionRoute(creatorType, creatorName)
    }

    private fun toggleSubscriptionRoute(routeType: String, routeName: String) {
        val normalizedType = normalizeSubscriptionRouteType(routeType)
        val normalizedName = normalizeSubscriptionRouteName(normalizedType, routeName)
        if (normalizedType.isBlank() || normalizedName.isBlank()) return
        viewModelScope.launch {
            val existing = withContext(Dispatchers.IO) {
                subscriptionRepository.find(normalizedType, normalizedName)
            }
            if (existing != null) {
                withContext(Dispatchers.IO) {
                    subscriptionRepository.remove(existing.id)
                }
                reloadSubscriptionsState()
                setStatus("Unsubscribed from ${subscriptionRouteDisplayLabel(normalizedType, normalizedName)}.")
            } else {
                val created = withContext(Dispatchers.IO) {
                    subscriptionRepository.upsert(normalizedType, normalizedName)
                }
                reloadSubscriptionsState()
                if (created == null) {
                    setStatus("Could not subscribe to ${subscriptionRouteDisplayLabel(normalizedType, normalizedName)}.")
                } else {
                    setStatus("Subscribed to ${subscriptionRouteDisplayLabel(normalizedType, normalizedName)}. Initial sync is running.")
                    initializeSubscription(created)
                }
            }
        }
    }

    fun updateSubscriptionSettings(
        subscriptionId: Long,
        notificationsEnabled: Boolean,
        notificationDotEnabled: Boolean
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                subscriptionRepository.updateSettings(
                    subscriptionId = subscriptionId,
                    notificationsEnabled = notificationsEnabled,
                    notificationDotEnabled = notificationDotEnabled
                )
            }
            reloadSubscriptionsState()
            setStatus("Updated subscription settings.")
        }
    }

    fun cycleSubscriptionRefreshInterval() {
        val options = listOf(1, 3, 6, 12, 24)
        val currentIndex = options.indexOf(subscriptionRefreshIntervalHours)
        val baseIndex = if (currentIndex >= 0) currentIndex else 0
        val next = options[(baseIndex + 1).mod(options.size)]
        subscriptionRefreshIntervalHours = next
        prefs.edit().putInt(KEY_SUBSCRIPTION_REFRESH_INTERVAL_HOURS, next).apply()
        syncSubscriptionBackgroundWork(
            context = getApplication<Application>().applicationContext,
            hasSubscriptions = subscriptions.isNotEmpty(),
            intervalHours = next
        )
        setStatus("Subscription check interval: ${formatSubscriptionRefreshInterval(next)}.")
    }

    fun formatSubscriptionRefreshInterval(hours: Int = subscriptionRefreshIntervalHours): String {
        return when (hours) {
            1 -> "1 hour"
            else -> "$hours hours"
        }
    }

    fun unsubscribeSubscription(subscriptionId: Long) {
        if (subscriptionId <= 0L) return
        viewModelScope.launch {
            val existing = withContext(Dispatchers.IO) {
                subscriptionRepository.list().firstOrNull { it.id == subscriptionId }
            }
            withContext(Dispatchers.IO) { subscriptionRepository.remove(subscriptionId) }
            reloadSubscriptionsState()
            if (existing != null) {
                setStatus("Unsubscribed from ${subscriptionRouteDisplayLabel(existing.routeType, existing.routeName)}.")
            }
        }
    }

    fun dismissSubscriptionEvent(eventId: Long) {
        if (eventId <= 0L) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { subscriptionRepository.dismissEvent(eventId) }
            reloadSubscriptionsState()
            setStatus("Subscription item dismissed.")
        }
    }

    fun toggleSubscriptionEventPinned(eventId: Long) {
        if (eventId <= 0L) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { subscriptionRepository.toggleEventPinned(eventId) }
            reloadSubscriptionsState()
        }
    }

    fun importSubscriptionEvent(code: Int) {
        importSuggestedEntry(code)
    }

    private suspend fun initializeSubscription(subscription: SubscriptionRow) {
        subscriptionSyncUseCase.initialize(subscription)
        reloadSubscriptionsState()
    }

    fun refreshSubscriptions(manual: Boolean = true) {
        if (subscriptionRefreshRunning) return
        subscriptionRefreshRunning = true
        viewModelScope.launch {
            val subscriptionSnapshot = withContext(Dispatchers.IO) { subscriptionRepository.list() }
            if (subscriptionSnapshot.isEmpty()) {
                subscriptionRefreshRunning = false
                subscriptions = emptyList()
                subscriptionEvents = withContext(Dispatchers.IO) { subscriptionRepository.events() }
                syncSubscriptionNotificationSummary()
                if (manual) setStatus("No subscriptions yet.")
                return@launch
            }

            if (manual) {
                setStatus("Checking ${subscriptionSnapshot.size} subscription(s)...")
            }

            var newEventCount = 0
            var failedCount = 0
            for (subscription in subscriptionSnapshot) {
                try {
                    newEventCount += subscriptionSyncUseCase.refresh(subscription)
                } catch (_: Throwable) {
                    failedCount += 1
                    withContext(Dispatchers.IO) {
                        subscriptionRepository.markChecked(subscription.id)
                    }
                }
            }

            subscriptions = withContext(Dispatchers.IO) { subscriptionRepository.list() }
            subscriptionEvents = withContext(Dispatchers.IO) { subscriptionRepository.events() }
            syncSubscriptionNotificationSummary()
            subscriptionRefreshRunning = false
            if (manual) {
                setStatus(
                    when {
                        newEventCount > 0 && failedCount > 0 ->
                            "Found $newEventCount new subscription item(s). $failedCount subscription(s) failed to refresh."
                        newEventCount > 0 ->
                            "Found $newEventCount new subscription item(s)."
                        failedCount > 0 ->
                            "$failedCount subscription(s) failed to refresh."
                        else ->
                            "No new subscription items."
                    }
                )
            }
        }
    }

    fun refreshSingleSubscription(subscriptionId: Long) {
        if (subscriptionRefreshRunning || subscriptionId <= 0L) return
        subscriptionRefreshRunning = true
        viewModelScope.launch {
            val subscription = withContext(Dispatchers.IO) {
                subscriptionRepository.list().firstOrNull { it.id == subscriptionId }
            }
            if (subscription == null) {
                subscriptionRefreshRunning = false
                return@launch
            }
            setStatus("Checking ${subscriptionRouteDisplayLabel(subscription.routeType, subscription.routeName)}...")
            val result = runCatching {
                refreshSingleSubscriptionInternal(subscription)
            }
            subscriptions = withContext(Dispatchers.IO) { subscriptionRepository.list() }
            subscriptionEvents = withContext(Dispatchers.IO) { subscriptionRepository.events() }
            syncSubscriptionNotificationSummary()
            subscriptionRefreshRunning = false
            result.onSuccess { inserted ->
                setStatus(
                    if (inserted > 0) {
                        "Found $inserted new item(s) for ${subscriptionRouteDisplayLabel(subscription.routeType, subscription.routeName)}."
                    } else {
                        "No new items for ${subscriptionRouteDisplayLabel(subscription.routeType, subscription.routeName)}."
                    }
                )
            }.onFailure {
                setStatus("Subscription check failed for ${subscriptionRouteDisplayLabel(subscription.routeType, subscription.routeName)}.")
            }
        }
    }

    private suspend fun refreshSingleSubscriptionInternal(subscription: SubscriptionRow): Int {
        return subscriptionSyncUseCase.refresh(subscription)
    }

    private fun syncSubscriptionNotificationSummary() {
        val appContext = getApplication<Application>().applicationContext
        syncSubscriptionNotificationSummaryForContext(appContext, db)
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
                libraryRepository.creatorEntries(
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

    private fun persistSuggestionCategoryWeights() {
        val editor = prefs.edit()
        SuggestionWeightCategory.entries.forEach { category ->
            val key = "$KEY_SUGGESTION_WEIGHT_PREFIX${category.storageKey}"
            val value = suggestionCategoryWeights[category] ?: 1f
            editor.putFloat(key, value.coerceIn(0f, 2f))
        }
        editor.apply()
    }

    private fun persistHiddenSuggestionCodes() {
        val orderedEntries = hiddenSuggestedCodes.keys
            .asSequence()
            .filter { it > 0 }
            .distinct()
            .sortedWith(
                compareBy<Int> { hiddenSuggestedAtMillis[it] ?: Long.MAX_VALUE }
                    .thenBy { it }
            )
            .map { code ->
                HiddenSuggestedEntryState(
                    code = code,
                    hiddenAtMillis = (hiddenSuggestedAtMillis[code] ?: 1L).coerceAtLeast(1L)
                )
            }
            .toList()

        val serializedCodes = orderedEntries.joinToString(",") { it.code.toString() }
        val serializedEntries = JSONArray().apply {
            orderedEntries.forEach { entry ->
                put(
                    JSONObject()
                        .put("code", entry.code)
                        .put("hidden_at_ms", entry.hiddenAtMillis)
                )
            }
        }.toString()

        hiddenSuggestionCodesCacheRaw = serializedCodes
        hiddenSuggestionEntriesCacheRaw = serializedEntries
        prefs.edit()
            .putString(KEY_SUGGESTION_HIDDEN_CODES, serializedCodes)
            .putString(KEY_SUGGESTION_HIDDEN_ENTRIES, serializedEntries)
            .apply()
    }

    private fun setAppLockGraceUntil(untilMs: Long) {
        appLockGraceUntilMs = appLockController.setGraceUntil(untilMs)
    }

    private fun isAppLockConfigured(): Boolean {
        return appLockController.isConfigured()
    }

    private fun loadSavedStats() {
        savedStats = db.getSavedStats()
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

    private fun parseBackupSnapshotOrNull(text: String): JSONObject? {
        val lines = text.lineSequence().toList()
        if (lines.isEmpty() || !lines.first().startsWith(EXPORT_PREFIX)) {
            return null
        }
        val jsonStart = text.indexOf('{')
        if (jsonStart < 0) return null
        return runCatching { JSONObject(text.substring(jsonStart)) }.getOrNull()
    }

    private fun readBackupSnapshotOrNull(uri: Uri): JSONObject? {
        return runCatching { parseBackupSnapshotOrNull(readTextFromUri(uri)) }.getOrNull()
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

    private fun scheduleSelectedEntrySupport(code: Int?, detailHint: EntryDetail?) {
        scheduleSeriesNeighborComputation(code, detailHint)
        scheduleSelectedEntryRelatedEntries(code)
    }

    private fun scheduleSelectedEntryRelatedEntries(code: Int?) {
        selectedEntryRelatedJob?.cancel()
        if (code == null || code <= 0) {
            selectedEntryRelatedUiState = SelectedEntryRelatedUiState()
            return
        }

        val cacheKey = selectedEntryRelatedCacheKey(code)
        selectedEntryRelatedCache[cacheKey]?.let { cached ->
            selectedEntryRelatedUiState = cached
            return
        }

        selectedEntryRelatedUiState = SelectedEntryRelatedUiState(code = code, loading = true)
        selectedEntryRelatedJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val rawMoreLikeThis = libraryRepository.relatedEntries(code = code, limit = 120)
                    val rawSameArtist = libraryRepository.sameArtistEntries(code = code, limit = 120)
                    val relatedCodes = (rawMoreLikeThis.asSequence() + rawSameArtist.asSequence())
                        .map { it.code }
                        .distinct()
                        .toList()
                    val readStateByCode = libraryRepository.browserStates(relatedCodes)
                        .mapValues { (_, state) -> state.isRead }
                    val moreLikeThis = filterRelatedEntriesByReadState(
                        entries = rawMoreLikeThis,
                        readStateByCode = readStateByCode,
                        showReadEntries = cacheKey.showReadEntries
                    )
                    val sameArtist = filterRelatedEntriesByReadState(
                        entries = rawSameArtist,
                        readStateByCode = readStateByCode,
                        showReadEntries = cacheKey.showReadEntries
                    )
                    SelectedEntryRelatedUiState(
                        code = code,
                        loading = false,
                        moreLikeThis = moreLikeThis,
                        sameArtist = sameArtist
                    )
                }
            }
            if (!isActive || selectedCode != code) return@launch
            result.onSuccess { state ->
                selectedEntryRelatedCache[cacheKey] = state
                selectedEntryRelatedUiState = state
            }.onFailure { error ->
                Log.e("SauceTrackerRelated", "Could not load related entries for $code", error)
                selectedEntryRelatedUiState = SelectedEntryRelatedUiState(code = code, loading = false)
            }
        }
    }

    private fun selectedEntryRelatedCacheKey(code: Int): SelectedEntryRelatedCacheKey =
        SelectedEntryRelatedCacheKey(
            code = code,
            showReadEntries = showReadRelatedEntries(entryReadFilter)
        )

    private fun scheduleSeriesNeighborComputation(code: Int?, detailHint: EntryDetail?) {
        seriesNeighborsJob?.cancel()
        if (code == null || code <= 0) {
            selectedSeriesNeighbors = SeriesNeighbors()
            return
        }

        seriesNeighborsJob = viewModelScope.launch {
            val neighbors = withContext(Dispatchers.IO) {
                runCatching {
                    val current = detailHint ?: libraryRepository.entryDetail(code)
                    if (current == null) {
                        return@runCatching SeriesNeighbors()
                    }
                    val candidates = db.listSeriesCandidates()
                    EntrySeriesResolver.resolve(current, candidates)
                }.onFailure { error ->
                    Log.e("SauceTrackerRelated", "Could not compute series parts for $code", error)
                }.getOrDefault(SeriesNeighbors())
            }
            if (selectedCode == code) {
                selectedSeriesNeighbors = neighbors
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        seriesNeighborsJob?.cancel()
        selectedEntryRelatedJob?.cancel()
        selectedEntryRelatedCache.clear()
        creatorLoadJobs.values.forEach { it.cancel() }
        creatorLoadJobs.clear()
        loadingCreatorIds.clear()
        desktopBridgeServer.stop()
        db.close()
    }
}

