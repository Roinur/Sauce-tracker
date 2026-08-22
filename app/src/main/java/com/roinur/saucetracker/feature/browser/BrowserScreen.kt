package com.roinur.saucetracker.feature.browser

import com.roinur.saucetracker.core.ui.components.*
import com.roinur.saucetracker.data.database.SauceTrackerDatabase

import com.roinur.saucetracker.*
import com.roinur.saucetracker.core.media.*
import com.roinur.saucetracker.data.backup.*
import com.roinur.saucetracker.feature.slideshow.GallerySlideshowActivity
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
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.input.VisualTransformation
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
import com.roinur.saucetracker.core.diagnostics.PerformanceMetrics
import com.roinur.saucetracker.core.media.computeDHash64
import com.roinur.saucetracker.core.network.HttpClientFactory
import com.roinur.saucetracker.core.network.HttpClientProfile
import com.roinur.saucetracker.core.preferences.KEY_BROWSER_DUPLICATE_CHECK_MODE
import com.roinur.saucetracker.core.preferences.KEY_PERFORMANCE_OVERLAY_ENABLED
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
import com.roinur.saucetracker.core.diagnostics.GitHubMediaSession
private val BROWSER_EXIT_RATING_PROMPT_SAVER = listSaver<BrowserExitRatingPromptState?, Any>(
    save = { prompt ->
        if (prompt == null) emptyList() else listOf(
            prompt.code,
            prompt.title,
            prompt.rating,
            prompt.closeAfter,
            prompt.wasReadBefore,
            prompt.isReread
        )
    },
    restore = { values ->
        if (values.size != 6) null else BrowserExitRatingPromptState(
            code = values[0] as Int,
            title = values[1] as String,
            rating = values[2] as Int,
            closeAfter = values[3] as Boolean,
            wasReadBefore = values[4] as Boolean,
            isReread = values[5] as Boolean
        )
    }
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun BrowserScreen(
    initialCode: Int?,
    initialQuery: String,
    initialCreatorType: String?,
    initialCreatorName: String?,
    incognitoModeEnabled: Boolean,
    onIncognitoModeChanged: (Boolean) -> Unit,
    blockedTags: List<String>,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api: BrowserViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val db = remember { SauceTrackerDatabase(context.applicationContext) }
    val prefs = remember(context) {
        context.getSharedPreferences(
            GitHubMediaSession.preferencesName(GALLERY_BROWSER_PREFS_NAME),
            Context.MODE_PRIVATE
        )
    }
    val performanceOverlayEnabled = remember(prefs) {
        prefs.getBoolean(KEY_PERFORMANCE_OVERLAY_ENABLED, false)
    }
    PerformanceOverlay(enabled = performanceOverlayEnabled)
    val listScrollState = rememberLazyListState()
    val detailScrollState = rememberLazyListState()
    val blocked = remember(blockedTags) {
        blockedTags
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.US) }
    }
    val defaultBrowserDuplicateCheckMode = remember(prefs) {
        loadBrowserDuplicateCheckMode(prefs)
    }
    var browserDuplicateCheckMode by remember {
        mutableStateOf(defaultBrowserDuplicateCheckMode)
    }
    var showBrowserDuplicateModeDialog by remember { mutableStateOf(false) }
    var duplicateComparisonState by remember { mutableStateOf<BrowserDuplicateComparisonState?>(null) }

    var searchInput by remember { mutableStateOf(initialQuery) }
    var activeSearchTerm by remember { mutableStateOf(initialQuery) }
    var listRows by remember { mutableStateOf<List<BrowserGallerySummary>>(emptyList()) }
    val initialHiddenSuggestionState = remember {
        val rawCodes = prefs.getString(GALLERY_BROWSER_KEY_SUGGESTION_HIDDEN_CODES, "").orEmpty()
        val rawEntries = prefs.getString(GALLERY_BROWSER_KEY_SUGGESTION_HIDDEN_ENTRIES, "").orEmpty()
        val orderedCodes = rawCodes
            .split(',')
            .asSequence()
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it > 0 }
            .distinct()
            .toList()

        val parsedEntriesByCode = mutableMapOf<Int, Long>()
        runCatching { JSONArray(rawEntries) }.getOrNull()?.let { array ->
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val code = when (val raw = obj.opt("code")) {
                    is Number -> raw.toInt()
                    is String -> raw.trim().toIntOrNull() ?: 0
                    else -> 0
                }
                if (code <= 0) continue
                val hiddenAtMillis = when (val raw = obj.opt("hidden_at_ms")) {
                    is Number -> raw.toLong()
                    is String -> raw.trim().toLongOrNull() ?: 0L
                    else -> 0L
                }
                if (hiddenAtMillis > 0L) {
                    parsedEntriesByCode[code] = hiddenAtMillis
                }
            }
        }

        val fallbackBase = 1L
        val resolvedHiddenAtByCode = mutableMapOf<Int, Long>()
        orderedCodes.forEachIndexed { index, code ->
            resolvedHiddenAtByCode[code] = parsedEntriesByCode[code]?.coerceAtLeast(1L) ?: (fallbackBase + index)
        }
        orderedCodes.toSet() to resolvedHiddenAtByCode.toMap()
    }
    var hiddenSuggestionCodes by remember { mutableStateOf(initialHiddenSuggestionState.first) }
    val hiddenSuggestionHiddenAtMillis = remember {
        mutableStateMapOf<Int, Long>().apply {
            putAll(initialHiddenSuggestionState.second)
        }
    }
    var selectedDetail by remember { mutableStateOf<BrowserGalleryDetail?>(null) }
    var loadingList by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var loadingDetailCode by remember { mutableStateOf<Int?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingInitialDetailCode by remember { mutableStateOf(initialCode?.takeIf { it > 0 }) }
    var paneTransitionDirection by remember { mutableStateOf(BrowserPaneTransitionDirection.Forward) }
    var detailContentVisible by remember { mutableStateOf(false) }
    var detailRevealNonce by remember { mutableStateOf(0L) }
    var currentPage by remember { mutableStateOf(0) }
    var hasMorePages by remember { mutableStateOf(false) }
    var activeCreator by remember { mutableStateOf<BrowserCreatorRef?>(null) }
    var searchSortMode by remember { mutableStateOf(BrowserSearchSortMode.RECENT) }
    var clipboardImportPrompt by remember { mutableStateOf<String?>(null) }
    var ratingPromptState by rememberSaveable(stateSaver = BROWSER_EXIT_RATING_PROMPT_SAVER) {
        mutableStateOf<BrowserExitRatingPromptState?>(null)
    }
    var pendingListImportRequest by remember { mutableStateOf<BrowserPendingImportRequest?>(null) }
    // This is an obligation, not transient UI state. Image pressure can recreate
    // Browser while Slideshow is open, so retain the code until Save or Skip.
    var pendingSlideshowRatingCode by rememberSaveable { mutableStateOf<Int?>(null) }
    var detailLoadRequestId by remember { mutableStateOf(0L) }
    var listLibraryStates by remember { mutableStateOf<Map<Int, BrowserLocalLibraryState>>(emptyMap()) }
    var listLibraryRequestId by remember { mutableStateOf(0L) }
    var localDuplicateSeeds by remember { mutableStateOf<List<LocalDuplicateSeed>>(emptyList()) }
    var localDuplicateSeedIndex by remember {
        mutableStateOf(buildBrowserDuplicateSeedIndex(emptyList()))
    }
    var duplicateSeedVersion by remember { mutableStateOf(0) }
    var duplicateHashLoadRequestId by remember { mutableStateOf(0L) }
    val duplicateHintsByCode = remember { mutableStateMapOf<Int, DuplicateHint>() }
    val duplicateHintLoadingByCode = remember { mutableStateMapOf<Int, Boolean>() }
    val duplicateHintResolvedByCode = remember { mutableStateMapOf<Int, Boolean>() }
    val duplicateHintCheckedThisSession = remember { mutableStateMapOf<Int, Boolean>() }
    val duplicateHintSemaphore = remember {
        Semaphore(
            Runtime.getRuntime().availableProcessors()
                .coerceIn(2, 6)
        )
    }
    val importFlashEpochByCode = remember { mutableStateMapOf<Int, Int>() }
    var incognitoToggleAuthPending by remember { mutableStateOf(false) }
    var incognitoToggleAuthNonce by remember { mutableStateOf(0L) }
    var incognitoTogglePinHash by remember { mutableStateOf("") }
    var incognitoTogglePinSalt by remember { mutableStateOf("") }
    var incognitoToggleBiometricEnabled by remember { mutableStateOf(true) }
    var incognitoToggleAllowCancel by remember { mutableStateOf(false) }
    val navStack = remember { mutableStateListOf<BrowserNavSnapshot>() }
    val listScrollByRoute = remember { mutableStateMapOf<String, Pair<Int, Int>>() }
    val detailScrollByRoute = remember { mutableStateMapOf<String, Pair<Int, Int>>() }

    LaunchedEffect(incognitoModeEnabled) {
        if (incognitoModeEnabled) {
            showBrowserDuplicateModeDialog = false
            duplicateComparisonState = null
            clipboardImportPrompt = null
            ratingPromptState = null
            pendingListImportRequest = null
            duplicateHintsByCode.clear()
            duplicateHintLoadingByCode.clear()
            duplicateHintResolvedByCode.clear()
        }
    }
    fun buildListRouteKey(
        searchTerm: String = activeSearchTerm,
        creator: BrowserCreatorRef? = activeCreator,
        sortMode: BrowserSearchSortMode = searchSortMode
    ): String {
        return if (creator != null) {
            val type = creator.type.trim().lowercase(Locale.US).ifBlank { "creator" }
            val slug = creator.slug.trim().lowercase(Locale.US).ifBlank {
                creator.name.trim().lowercase(Locale.US)
            }
            "creator:$type:$slug:sort=${sortMode.name}"
        } else {
            val normalizedSearch = searchTerm.trim().lowercase(Locale.US)
            "search:$normalizedSearch:sort=${sortMode.name}"
        }
    }

    fun buildDetailRouteKey(detail: BrowserGalleryDetail? = selectedDetail): String {
        val code = detail?.summary?.code ?: 0
        return "detail:$code"
    }

    fun rememberCurrentRouteScroll() {
        val detail = selectedDetail
        if (detail != null) {
            detailScrollByRoute[buildDetailRouteKey(detail)] =
                detailScrollState.firstVisibleItemIndex to detailScrollState.firstVisibleItemScrollOffset
        } else {
            listScrollByRoute[buildListRouteKey()] =
                listScrollState.firstVisibleItemIndex to listScrollState.firstVisibleItemScrollOffset
        }
    }

    fun resetListScrollToTop() {
        scope.launch {
            runCatching { listScrollState.scrollToItem(0, 0) }
        }
    }

    fun closeBrowserSecurely() {
        val shouldClear = (context as? GalleryBrowserActivity)?.tryBeginBrowserCleanup() != false
        if (shouldClear) {
            BrowserPrivacyController.clearArtifacts(
                api = api,
                clearSensitiveStorage = incognitoModeEnabled
            )
        }
        onDone()
    }

    fun persistHiddenSuggestionCodes(
        codes: Set<Int>,
        hiddenAtByCode: Map<Int, Long>,
        appendedCode: Int? = null
    ) {
        val normalizedCodes = codes
            .asSequence()
            .filter { it > 0 }
            .toSet()
        val existingOrder = prefs.getString(GALLERY_BROWSER_KEY_SUGGESTION_HIDDEN_CODES, "")
            .orEmpty()
            .split(',')
            .asSequence()
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it > 0 && it in normalizedCodes }
            .distinct()
            .toList()
        val missingCodes = normalizedCodes
            .asSequence()
            .filterNot { it in existingOrder }
            .sorted()
            .toMutableList()
        val orderedCodes = buildList {
            addAll(existingOrder)
            appendedCode?.takeIf { it > 0 && it in missingCodes }?.let { appended ->
                add(appended)
                missingCodes.remove(appended)
            }
            addAll(missingCodes)
        }

        val now = System.currentTimeMillis()
        val orderedEntries = orderedCodes.mapIndexed { index, code ->
            val resolvedHiddenAt = when {
                hiddenAtByCode[code] != null -> hiddenAtByCode[code]!!.coerceAtLeast(1L)
                code == appendedCode -> now
                else -> 1L + index
            }
            code to resolvedHiddenAt
        }

        hiddenSuggestionHiddenAtMillis.clear()
        orderedEntries.forEach { (code, hiddenAt) ->
            hiddenSuggestionHiddenAtMillis[code] = hiddenAt
        }

        val serializedCodes = orderedEntries.joinToString(",") { (code, _) -> code.toString() }
        val serializedEntries = JSONArray().apply {
            orderedEntries.forEach { (code, hiddenAt) ->
                put(
                    JSONObject()
                        .put("code", code)
                        .put("hidden_at_ms", hiddenAt)
                )
            }
        }.toString()

        prefs.edit()
            .putString(GALLERY_BROWSER_KEY_SUGGESTION_HIDDEN_CODES, serializedCodes)
            .putString(GALLERY_BROWSER_KEY_SUGGESTION_HIDDEN_ENTRIES, serializedEntries)
            .apply()
    }

    fun hideSuggestionCode(code: Int) {
        if (code <= 0) return
        if (code in hiddenSuggestionCodes) return
        val next = hiddenSuggestionCodes + code
        hiddenSuggestionCodes = next
        hiddenSuggestionHiddenAtMillis[code] = System.currentTimeMillis()
        persistHiddenSuggestionCodes(next, hiddenSuggestionHiddenAtMillis, appendedCode = code)
        listRows = listRows.filterNot { it.code == code }
        listLibraryStates = listLibraryStates.filterKeys { it != code }
        duplicateHintsByCode.remove(code)
        duplicateHintLoadingByCode.remove(code)
        if (selectedDetail?.summary?.code == code) {
            selectedDetail = null
            scope.launch {
                runCatching { detailScrollState.scrollToItem(0, 0) }
            }
        }
        Toast.makeText(context, "Hidden code $code from suggestions.", Toast.LENGTH_SHORT).show()
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
            val wasReadBefore = detail?.isRead == true
            ratingPromptState = BrowserExitRatingPromptState(
                code = code,
                title = detail?.title?.ifBlank { fallbackTitle } ?: fallbackTitle,
                rating = initial,
                closeAfter = closeAfter,
                wasReadBefore = wasReadBefore,
                isReread = wasReadBefore
            )
        }
    }

    val slideshowLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val code = pendingSlideshowRatingCode
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
                val batchStates = db.getBrowserLibraryStates(codes)
                codes.associateWith { code ->
                    batchStates[code]?.let { local ->
                        BrowserLocalLibraryState(
                            exists = true,
                            rating = local.rating.coerceIn(0, 5),
                            isRead = local.isRead,
                            pinned = local.pinned
                        )
                    } ?: BrowserLocalLibraryState(exists = false, rating = 0, isRead = false, pinned = false)
                }
            }
            if (listLibraryRequestId == requestId) {
                listLibraryStates = states
            }
        }
    }

    fun refreshDuplicateSeeds() {
        scope.launch {
            val duplicateSnapshot = withContext(Dispatchers.IO) {
                val seeds = db.listDuplicateSeeds()
                BrowserDuplicateSeedIndexCache.snapshot(seeds)
            }
            val seeds = duplicateSnapshot.seeds
            localDuplicateSeeds = seeds
            localDuplicateSeedIndex = duplicateSnapshot.index
            val nextVersion = duplicateSnapshot.version
            val shouldReloadHashIndex =
                duplicateSeedVersion != nextVersion || BrowserDuplicateLocalHashIndex.isEmpty()
            if (browserDuplicateCheckMode == BrowserDuplicateCheckMode.OFF) {
                duplicateSeedVersion = nextVersion
                duplicateHintsByCode.clear()
                duplicateHintLoadingByCode.clear()
                duplicateHintResolvedByCode.clear()
            } else if (duplicateSeedVersion != nextVersion &&
                browserDuplicateCheckMode == BrowserDuplicateCheckMode.AGGRESSIVE
            ) {
                duplicateSeedVersion = nextVersion
                duplicateHintsByCode.clear()
                duplicateHintLoadingByCode.clear()
                duplicateHintResolvedByCode.clear()
            } else {
                duplicateSeedVersion = nextVersion
            }
            if (shouldReloadHashIndex) {
                BrowserDuplicateLocalHashIndex.replaceAll(emptyMap())
                val requestId = System.nanoTime()
                duplicateHashLoadRequestId = requestId
                val applicationContext = context.applicationContext
                val seedCodes = seeds.asSequence().map { it.code }.filter { it > 0 }.toSet()
                scope.launch {
                    val hashes = withContext(Dispatchers.IO) {
                        readBackupThumbnailHashesByCode(applicationContext, seedCodes)
                    }
                    if (duplicateHashLoadRequestId == requestId && duplicateSeedVersion == nextVersion) {
                        BrowserDuplicateLocalHashIndex.replaceAll(hashes)
                    }
                }
            }
        }
    }

    fun triggerImportFlash(code: Int) {
        importFlashEpochByCode[code] = (importFlashEpochByCode[code] ?: 0) + 1
    }

    fun pruneDuplicateHintState(rows: List<BrowserGallerySummary>) {
        val activeCodes = rows.asSequence().map { it.code }.toSet()
        importFlashEpochByCode.keys
            .filter { it !in activeCodes }
            .toList()
            .forEach { importFlashEpochByCode.remove(it) }
    }

    fun queueDuplicateHintForRow(row: BrowserGallerySummary) {
        val code = row.code
        if (code <= 0) return
        if (listScrollState.isScrollInProgress) return
        if (browserDuplicateCheckMode == BrowserDuplicateCheckMode.OFF) return
        if (browserDuplicateCheckMode == BrowserDuplicateCheckMode.ONCE_PER_SESSION &&
            duplicateHintCheckedThisSession[code] == true
        ) {
            return
        }
        if (duplicateHintsByCode.containsKey(code)) return
        if (duplicateHintLoadingByCode[code] == true) return
        if (duplicateHintResolvedByCode[code] == true) return
        val seedIndexSnapshot = localDuplicateSeedIndex
        if (seedIndexSnapshot.allSeeds.isEmpty()) return

        if (browserDuplicateCheckMode == BrowserDuplicateCheckMode.ONCE_PER_SESSION) {
            duplicateHintCheckedThisSession[code] = true
        }
        duplicateHintLoadingByCode[code] = true
        PerformanceMetrics.recordDuplicateCheckStarted()
        scope.launch {
            val selectedSnapshot = selectedDetail?.takeIf { it.summary.code == code }
            val hint = duplicateHintSemaphore.withPermit {
                withContext(Dispatchers.IO) {
                    val summaryTitle = listOf(row.title, row.subtitle)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                    val summaryThumbnailUrl = buildCoverThumbnailUrls(
                        mediaId = row.mediaId,
                        preferredExt = row.coverExt
                    ).firstOrNull().orEmpty()
                    val summaryCandidateSeeds = browserCollectDuplicateCandidateSeeds(
                        index = seedIndexSnapshot,
                        candidateCode = row.code,
                        candidateTitle = summaryTitle,
                        candidateNumPages = row.numPages,
                        candidateUploadDate = row.uploadDate,
                        candidateMediaId = row.mediaId
                    )
                    val summaryHint = browserFindLikelyDuplicateHint(
                        appContext = context.applicationContext,
                        candidateCode = row.code,
                        candidateTitle = summaryTitle,
                        candidateNumPages = row.numPages,
                        candidateUploadDate = row.uploadDate,
                        candidateMediaId = row.mediaId,
                        candidateArtistKeys = emptySet(),
                        candidateGroupKeys = emptySet(),
                        candidateTagKeys = emptySet(),
                        candidateThumbnailUrl = summaryThumbnailUrl,
                        candidateSeeds = summaryCandidateSeeds
                    )
                    if (summaryHint != null) {
                        return@withContext summaryHint
                    }
                    if (!browserShouldFetchDetailForDuplicateHint(
                            candidateCode = row.code,
                            candidateTitle = summaryTitle,
                            candidateNumPages = row.numPages,
                            candidateUploadDate = row.uploadDate,
                            candidateMediaId = row.mediaId,
                            candidateThumbnailUrl = summaryThumbnailUrl,
                            candidateSeeds = summaryCandidateSeeds
                        )
                    ) {
                        return@withContext null
                    }
                    val detail = selectedSnapshot ?: runCatching { api.fetchGalleryDetail(code) }.getOrNull()
                    if (detail == null) {
                        return@withContext null
                    }
                    val detailCandidateSeeds = browserCollectDuplicateCandidateSeeds(
                        index = seedIndexSnapshot,
                        candidateCode = detail.summary.code,
                        candidateTitle = listOf(detail.summary.title, detail.summary.subtitle)
                            .filter { it.isNotBlank() }
                            .joinToString(" "),
                        candidateNumPages = detail.summary.numPages,
                        candidateUploadDate = detail.summary.uploadDate,
                        candidateMediaId = detail.summary.mediaId
                    )
                    val artistKeys = detail.tagsByType["artist"].orEmpty()
                        .asSequence()
                        .map { browserNormalizeTagKey(it) }
                        .filter { it.isNotBlank() }
                        .toSet()
                    val groupKeys = detail.tagsByType["group"].orEmpty()
                        .asSequence()
                        .map { browserNormalizeTagKey(it) }
                        .filter { it.isNotBlank() }
                        .toSet()
                    val tagKeys = detail.tagsByType.entries
                        .asSequence()
                        .filter { (type, _) ->
                            val normalized = type.trim().lowercase(Locale.US)
                            normalized != "artist" && normalized != "group"
                        }
                        .flatMap { (_, names) -> names.asSequence() }
                        .map { browserNormalizeTagKey(it) }
                        .filter { it.isNotBlank() }
                        .toSet()
                    browserFindLikelyDuplicateHint(
                        appContext = context.applicationContext,
                        candidateCode = detail.summary.code,
                        candidateTitle = listOf(detail.summary.title, detail.summary.subtitle)
                            .filter { it.isNotBlank() }
                            .joinToString(" "),
                        candidateNumPages = detail.summary.numPages,
                        candidateUploadDate = detail.summary.uploadDate,
                        candidateMediaId = detail.summary.mediaId,
                        candidateArtistKeys = artistKeys,
                        candidateGroupKeys = groupKeys,
                        candidateTagKeys = tagKeys,
                        candidateThumbnailUrl = buildCoverThumbnailUrls(
                            mediaId = detail.summary.mediaId,
                            preferredExt = detail.summary.coverExt
                        ).firstOrNull().orEmpty(),
                        candidateSeeds = detailCandidateSeeds
                    )
                }
            }
            duplicateHintLoadingByCode.remove(code)
            duplicateHintResolvedByCode[code] = true
            if (hint != null) {
                duplicateHintsByCode[code] = hint
            } else {
                duplicateHintsByCode.remove(code)
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
                val filteredPageRows = page.results.filterNot { it.code in hiddenSuggestionCodes }
                val nextRows = if (reset) {
                    filteredPageRows.distinctBy { it.code }
                } else {
                    val seenCodes = listRows.asSequence().map { it.code }.toMutableSet()
                    val appendRows = filteredPageRows.filter { seenCodes.add(it.code) }
                    listRows + appendRows
                }
                listRows = nextRows
                pruneDuplicateHintState(nextRows)
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
            sortMode = searchSortMode,
            listRouteKey = buildListRouteKey(),
            detailRouteKey = buildDetailRouteKey(),
            listFirstVisibleItemIndex = listScrollState.firstVisibleItemIndex,
            listFirstVisibleItemScrollOffset = listScrollState.firstVisibleItemScrollOffset,
            detailFirstVisibleItemIndex = detailScrollState.firstVisibleItemIndex,
            detailFirstVisibleItemScrollOffset = detailScrollState.firstVisibleItemScrollOffset
        )
    }

    fun pushNavSnapshot() {
        rememberCurrentRouteScroll()
        navStack.add(captureNavSnapshot())
    }

    fun applyNavSnapshot(snapshot: BrowserNavSnapshot) {
        detailLoadRequestId = System.nanoTime()
        loadingDetailCode = null
        loadingMore = false
        loadingList = false
        val filteredRows = snapshot.rows.filterNot { it.code in hiddenSuggestionCodes }
        val filteredSelectedDetail =
            snapshot.selectedDetail?.takeUnless { it.summary.code in hiddenSuggestionCodes }

        searchInput = snapshot.searchInput
        activeSearchTerm = snapshot.activeSearchTerm
        listRows = filteredRows
        selectedDetail = filteredSelectedDetail
        detailContentVisible = filteredSelectedDetail != null
        currentPage = snapshot.currentPage
        hasMorePages = snapshot.hasMorePages
        activeCreator = snapshot.activeCreator
        searchSortMode = snapshot.sortMode
        refreshListLibraryStates(filteredRows)
        pruneDuplicateHintState(filteredRows)
        errorMessage = null
        scope.launch {
            if (filteredSelectedDetail == null) {
                val saved = listScrollByRoute[snapshot.listRouteKey]
                if (filteredRows.isNotEmpty()) {
                    val index = (saved?.first ?: snapshot.listFirstVisibleItemIndex)
                        .coerceIn(0, (filteredRows.lastIndex).coerceAtLeast(0))
                    val offset = (saved?.second ?: snapshot.listFirstVisibleItemScrollOffset).coerceAtLeast(0)
                    runCatching { listScrollState.scrollToItem(index, offset) }
                } else {
                    runCatching { listScrollState.scrollToItem(0, 0) }
                }
            } else {
                val saved = detailScrollByRoute[snapshot.detailRouteKey]
                val index = (saved?.first ?: snapshot.detailFirstVisibleItemIndex).coerceAtLeast(0)
                val offset = (saved?.second ?: snapshot.detailFirstVisibleItemScrollOffset).coerceAtLeast(0)
                runCatching { detailScrollState.scrollToItem(index, offset) }
            }
        }
    }

    fun updateSearchSortMode(nextMode: BrowserSearchSortMode) {
        if (searchSortMode == nextMode) return
        pushNavSnapshot()
        searchSortMode = nextMode
        loadPage(reset = true)
    }

    fun navigateBackOrClose() {
        rememberCurrentRouteScroll()
        val previous = navStack.lastOrNull()
        if (previous != null) {
            paneTransitionDirection = BrowserPaneTransitionDirection.Backward
            navStack.removeAt(navStack.lastIndex)
            applyNavSnapshot(previous)
            return
        }
        closeBrowserSecurely()
    }

    fun scrollBrowserContentToTop() {
        scope.launch {
            runCatching { listScrollState.scrollToItem(0) }
            runCatching { detailScrollState.scrollToItem(0) }
        }
    }

    fun applyIncognitoModeToggle() {
        val next = !incognitoModeEnabled
        onIncognitoModeChanged(next)
        prefs.edit().putBoolean(GALLERY_BROWSER_KEY_INCOGNITO_MODE_ENABLED, next).apply()
    }

    fun requestIncognitoModeToggle(allowCancel: Boolean = true) {
        if (incognitoToggleAuthPending) return
        val appLockEnabled = prefs.getBoolean(GALLERY_BROWSER_KEY_APP_LOCK_ENABLED, false)
        val pinHash = prefs.getString(GALLERY_BROWSER_KEY_APP_LOCK_PIN_HASH, "").orEmpty()
        val pinSalt = prefs.getString(GALLERY_BROWSER_KEY_APP_LOCK_PIN_SALT, "").orEmpty()
        val biometricEnabled = prefs.getBoolean(GALLERY_BROWSER_KEY_APP_LOCK_BIOMETRIC_ENABLED, true)
        if (!appLockEnabled || pinHash.isBlank() || pinSalt.isBlank()) {
            incognitoToggleAllowCancel = false
            applyIncognitoModeToggle()
            return
        }
        incognitoTogglePinHash = pinHash
        incognitoTogglePinSalt = pinSalt
        incognitoToggleBiometricEnabled = biometricEnabled
        incognitoToggleAllowCancel = allowCancel
        incognitoToggleAuthPending = true
        incognitoToggleAuthNonce = System.currentTimeMillis()
    }

    fun openDetail(code: Int, pushHistory: Boolean = true) {
        if (code <= 0) return
        if (code in hiddenSuggestionCodes) {
            Toast.makeText(context, "Code $code is hidden.", Toast.LENGTH_SHORT).show()
            return
        }
        paneTransitionDirection = BrowserPaneTransitionDirection.Forward
        detailContentVisible = false
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
                    if (pendingInitialDetailCode == code) {
                        pendingInitialDetailCode = null
                    }
                    selectedDetail = detail
                    detailRevealNonce = System.nanoTime()
                    scope.launch {
                        runCatching { detailScrollState.scrollToItem(0, 0) }
                    }
                    // Comments are below the initial viewport and can be expensive to extract
                    // from fallback HTML. Populate them after the detail pane is interactive.
                    scope.launch {
                        val comments = withContext(Dispatchers.IO) {
                            runCatching { api.fetchGalleryComments(code) }.getOrDefault(emptyList())
                        }
                        if (detailLoadRequestId == requestId && selectedDetail?.summary?.code == code) {
                            selectedDetail = selectedDetail?.copy(comments = comments)
                        }
                    }
                    scope.launch {
                        val related = withContext(Dispatchers.IO) {
                            runCatching { api.fetchRelatedGalleries(code) }.getOrDefault(emptyList())
                        }
                        if (detailLoadRequestId == requestId && selectedDetail?.summary?.code == code) {
                            selectedDetail = selectedDetail?.copy(relatedGalleries = related)
                        }
                    }
                }
            }.onFailure { exc ->
                if (detailLoadRequestId == requestId) {
                    if (pendingInitialDetailCode == code) {
                        pendingInitialDetailCode = null
                    }
                    detailContentVisible = false
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
            startPage = startPage.coerceIn(1, summary.numPages.coerceAtLeast(1)),
            incognitoModeEnabled = incognitoModeEnabled
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
        paneTransitionDirection = BrowserPaneTransitionDirection.Forward
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
        resetListScrollToTop()
        loadPage(reset = true)
    }

    fun runSearchFromInput() {
        paneTransitionDirection = BrowserPaneTransitionDirection.Forward
        pushNavSnapshot()
        searchSortMode = BrowserSearchSortMode.RECENT
        activeSearchTerm = searchInput.trim()
        activeCreator = null
        selectedDetail = null
        resetListScrollToTop()
        loadPage(reset = true)
    }

    fun runTagSearch(tagName: String) {
        val encodedTag = encodeTagSearchTerm(tagName)
        if (encodedTag.isBlank()) return
        paneTransitionDirection = BrowserPaneTransitionDirection.Forward
        pushNavSnapshot()
        searchSortMode = BrowserSearchSortMode.RECENT
        searchInput = encodedTag
        activeSearchTerm = encodedTag
        activeCreator = null
        selectedDetail = null
        resetListScrollToTop()
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

    LaunchedEffect(initialCode, initialCreatorType, initialCreatorName) {
        if (initialCode != null && initialCode > 0) {
            openDetail(initialCode, pushHistory = false)
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
                resetListScrollToTop()
                loadPage(reset = true)
            } else {
                loadPage(reset = true)
            }
        } else {
            loadPage(reset = true)
        }
    }

    LaunchedEffect(browserDuplicateCheckMode) {
        if (browserDuplicateCheckMode == BrowserDuplicateCheckMode.OFF) {
            duplicateHintsByCode.clear()
            duplicateHintLoadingByCode.clear()
            duplicateHintResolvedByCode.clear()
        } else {
            refreshDuplicateSeeds()
        }
    }

    LaunchedEffect(selectedDetail?.summary?.code, detailRevealNonce, pendingInitialDetailCode) {
        val code = selectedDetail?.summary?.code
        val revealNonce = detailRevealNonce
        if (code == null || pendingInitialDetailCode != null) {
            detailContentVisible = false
            return@LaunchedEffect
        }
        delay(130)
        if (selectedDetail?.summary?.code == code && detailRevealNonce == revealNonce) {
            detailContentVisible = true
        }
    }

    BackHandler(enabled = true) {
        if (duplicateComparisonState != null) {
            duplicateComparisonState = null
        } else if (showBrowserDuplicateModeDialog) {
            showBrowserDuplicateModeDialog = false
        } else {
            navigateBackOrClose()
        }
    }

    val browserColorScheme = MaterialTheme.colorScheme

    MaterialTheme(colorScheme = browserColorScheme) {
        GalleryCodeBrowserApplySystemBars(browserColorScheme.background)
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = browserColorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = browserColorScheme.background,
                        scrolledContainerColor = browserColorScheme.background,
                        navigationIconContentColor = browserColorScheme.onBackground,
                        titleContentColor = browserColorScheme.onBackground,
                        actionIconContentColor = browserColorScheme.onBackground
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
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .pointerInput(selectedDetail, incognitoModeEnabled) {
                                    detectTapGestures(
                                        onPress = {
                                            scrollBrowserContentToTop()
                                            tryAwaitRelease()
                                        },
                                        onLongPress = {
                                            showBrowserDuplicateModeDialog = true
                                        },
                                        onDoubleTap = {
                                            requestIncognitoModeToggle()
                                        }
                                    )
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = selectedDetail?.summary?.title?.ifBlank {
                                    pendingInitialDetailCode?.let { "Gallery $it" } ?: defaultTitle
                                } ?: pendingInitialDetailCode?.let { "Gallery $it" } ?: defaultTitle,
                                modifier = Modifier.browserPrivacyObfuscate(
                                    enabled = incognitoModeEnabled,
                                    overlayColor = browserColorScheme.surfaceVariant.copy(
                                        alpha = GALLERY_BROWSER_INCOGNITO_OVERLAY_ALPHA
                                    )
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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

            if (showBrowserDuplicateModeDialog) {
                BrowserDuplicateCheckModeDialog(
                    title = "Browser Duplicate Checks",
                    currentMode = browserDuplicateCheckMode,
                    defaultMode = defaultBrowserDuplicateCheckMode,
                    temporary = true,
                    coverSystemBars = true,
                    onSelect = {
                        browserDuplicateCheckMode = it
                        showBrowserDuplicateModeDialog = false
                    },
                    onReset = {
                        browserDuplicateCheckMode = defaultBrowserDuplicateCheckMode
                        showBrowserDuplicateModeDialog = false
                    },
                    onDismiss = { showBrowserDuplicateModeDialog = false }
                )
            }

            if (!incognitoModeEnabled) duplicateComparisonState?.let { comparisonState ->
                BrowserDuplicateComparisonDialog(
                    state = comparisonState,
                    incognitoModeEnabled = incognitoModeEnabled,
                    entryDetailProvider = { code ->
                        withContext(Dispatchers.IO) { db.getEntryDetail(code) }
                    },
                    flaggedDetailProvider = { code ->
                        withContext(Dispatchers.IO) {
                            runCatching { api.fetchGalleryDetail(code) }.getOrNull()
                        }
                    },
                    onHide = { code ->
                        hideSuggestionCode(code)
                        duplicateComparisonState = null
                    },
                    onDismiss = { duplicateComparisonState = null }
                )
            }

            AnimatedContent(
                targetState = selectedDetail?.summary?.code != null || pendingInitialDetailCode != null,
                transitionSpec = {
                    val contentTransform = if (paneTransitionDirection == BrowserPaneTransitionDirection.Forward) {
                        (
                            fadeIn(
                                animationSpec = tween(
                                    durationMillis = 320,
                                    delayMillis = 44,
                                    easing = LinearOutSlowInEasing
                                )
                            ) + slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(
                                    durationMillis = 460,
                                    easing = FastOutSlowInEasing
                                )
                            )
                            ) togetherWith (
                            fadeOut(
                                animationSpec = tween(
                                    durationMillis = 230,
                                    easing = LinearOutSlowInEasing
                                )
                            ) + slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth / 5 },
                                animationSpec = tween(
                                    durationMillis = 460,
                                    easing = FastOutSlowInEasing
                                )
                            )
                            )
                    } else {
                        (
                            fadeIn(
                                animationSpec = tween(
                                    durationMillis = 320,
                                    delayMillis = 44,
                                    easing = LinearOutSlowInEasing
                                )
                            ) + slideInHorizontally(
                                initialOffsetX = { fullWidth -> -fullWidth / 5 },
                                animationSpec = tween(
                                    durationMillis = 460,
                                    easing = FastOutSlowInEasing
                                )
                            )
                            ) togetherWith (
                            fadeOut(
                                animationSpec = tween(
                                    durationMillis = 230,
                                    easing = LinearOutSlowInEasing
                                )
                            ) + slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(
                                    durationMillis = 460,
                                    easing = FastOutSlowInEasing
                                )
                            )
                            )
                    }
                    contentTransform.using(
                        SizeTransform(clip = true) { _, _ ->
                            tween(durationMillis = 1)
                        }
                    )
                },
                contentAlignment = Alignment.TopStart,
                label = "browserPaneTransition"
            ) { showDetail ->
                if (showDetail) {
                    val detail = selectedDetail
                    if (detail != null && detailContentVisible) {
                        var detailPaneAlphaTarget by remember(detail.summary.code) { mutableStateOf(0f) }
                        var revealScrimTarget by remember(detail.summary.code) { mutableStateOf(0.12f) }
                        LaunchedEffect(detail.summary.code) {
                            detailPaneAlphaTarget = 1f
                            revealScrimTarget = 0f
                        }
                        val detailPaneAlpha by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = detailPaneAlphaTarget,
                            animationSpec = tween(durationMillis = 190, easing = LinearOutSlowInEasing),
                            label = "browserDetailRevealAlpha"
                        )
                        val revealScrimAlpha by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = revealScrimTarget,
                            animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing),
                            label = "browserDetailRevealScrim"
                        )
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    alpha = detailPaneAlpha
                                }
                            ) {
                            GalleryDetailPane(
                                detail = detail,
                                listState = detailScrollState,
                                incognitoModeEnabled = incognitoModeEnabled,
                                loading = loadingDetailCode == detail.summary.code,
                                onOpenSlideshow = { page -> openSlideshow(detail, page) },
                                onOpenCode = { code -> openDetail(code, pushHistory = false) },
                                onOpenRelatedCode = { code -> openDetail(code, pushHistory = true) },
                                onOpenCreator = ::openCreator,
                                onSearchTag = ::runTagSearch,
                                onCopyCandidateDetected = { candidate ->
                                    clipboardImportPrompt = extractImportCandidateFromClipboard(candidate)
                                },
                                onImportSuccessFlash = ::triggerImportFlash,
                                onLibraryStateChanged = { code, state ->
                                    listLibraryStates = listLibraryStates.toMutableMap().apply {
                                        put(code, state)
                                    }
                                    refreshDuplicateSeeds()
                                }
                            )
                            }
                            if (revealScrimAlpha > 0.001f) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(MaterialTheme.colorScheme.background.copy(alpha = revealScrimAlpha))
                                )
                            }
                        }
                    } else {
                        BrowserDetailTransitionShell(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else if (loadingList && listRows.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (errorMessage != null && listRows.isEmpty()) {
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
                } else {
                    fun applyQuickLibraryActionFromList(code: Int, action: BrowserPendingLibraryAction) {
                        if (code <= 0) return
                        if (incognitoModeEnabled) {
                            Toast.makeText(
                                context,
                                "Library gestures are disabled in incognito mode.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return
                        }
                        val existingState = listLibraryStates[code]
                        if (existingState?.exists == false) {
                            pendingListImportRequest = BrowserPendingImportRequest(
                                code = code,
                                action = action
                            )
                            return
                        }
                        scope.launch {
                            val refreshed = withContext(Dispatchers.IO) {
                                val existingEntry = db.getEntryDetail(code)
                                if (existingEntry == null) {
                                    return@withContext BrowserLocalLibraryState(
                                        exists = false,
                                        rating = 0,
                                        isRead = false,
                                        pinned = false
                                    )
                                }
                                when (action) {
                                    is BrowserPendingLibraryAction.SetRating -> {
                                        db.setEntryRating(code, action.rating.coerceIn(0, 5))
                                        db.setEntryRead(code, true)
                                    }
                                    is BrowserPendingLibraryAction.SetRead -> {
                                        db.setEntryRead(code, action.isRead)
                                    }
                                    is BrowserPendingLibraryAction.SetPinned -> {
                                        db.setEntryPinned(code, action.pinned)
                                    }
                                    BrowserPendingLibraryAction.ToggleRead -> {
                                        db.setEntryRead(code, !existingEntry.isRead)
                                    }
                                    BrowserPendingLibraryAction.TogglePinned -> {
                                        db.setEntryPinned(code, !db.isEntryPinned(code))
                                    }
                                }
                                val local = db.getEntryDetail(code)
                                if (local != null) {
                                    BrowserLocalLibraryState(
                                        exists = true,
                                        rating = local.rating.coerceIn(0, 5),
                                        isRead = local.isRead,
                                        pinned = db.isEntryPinned(code)
                                    )
                                } else {
                                    BrowserLocalLibraryState(exists = false, rating = 0, isRead = false, pinned = false)
                                }
                            }
                            if (!refreshed.exists) {
                                pendingListImportRequest = BrowserPendingImportRequest(
                                    code = code,
                                    action = action
                                )
                                listLibraryStates = listLibraryStates.toMutableMap().apply {
                                    put(code, refreshed)
                                }
                                return@launch
                            }
                            listLibraryStates = listLibraryStates.toMutableMap().apply {
                                put(code, refreshed)
                            }
                            refreshDuplicateSeeds()
                        }
                    }

                    val showHomepageSectionSplit =
                        activeCreator == null &&
                            activeSearchTerm.isBlank() &&
                            blocked.isEmpty()
                    LazyColumn(
                        state = listScrollState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "browserListHeader") {
                            if (activeCreator == null) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
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
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Text,
                                                imeAction = ImeAction.Search
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onSearch = { runSearchFromInput() }
                                            ),
                                            visualTransformation = if (incognitoModeEnabled) {
                                                PasswordVisualTransformation()
                                            } else {
                                                VisualTransformation.None
                                            },
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
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.browserPrivacyObfuscate(
                                                enabled = incognitoModeEnabled,
                                                overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                                    alpha = GALLERY_BROWSER_INCOGNITO_OVERLAY_ALPHA
                                                )
                                            )
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
                                                    updateSearchSortMode(BrowserSearchSortMode.RECENT)
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
                                                    updateSearchSortMode(BrowserSearchSortMode.POPULAR_TODAY)
                                                },
                                                label = { Text("Today") }
                                            )
                                            FilterChip(
                                                selected = searchSortMode == BrowserSearchSortMode.POPULAR_WEEK,
                                                onClick = {
                                                    updateSearchSortMode(BrowserSearchSortMode.POPULAR_WEEK)
                                                },
                                                label = { Text("Week") }
                                            )
                                            FilterChip(
                                                selected = searchSortMode == BrowserSearchSortMode.POPULAR_ALL_TIME,
                                                onClick = {
                                                    updateSearchSortMode(BrowserSearchSortMode.POPULAR_ALL_TIME)
                                                },
                                                label = { Text("All Time") }
                                            )
                                        }
                                    }
                                }
                            } else {
                                activeCreator?.let { creatorRef ->
                                    val creatorLabel = creatorRef.type.replaceFirstChar {
                                        if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
                                    }
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
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
                                                    updateSearchSortMode(BrowserSearchSortMode.RECENT)
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
                                                    updateSearchSortMode(BrowserSearchSortMode.POPULAR_TODAY)
                                                },
                                                label = { Text("Today") }
                                            )
                                            FilterChip(
                                                selected = searchSortMode == BrowserSearchSortMode.POPULAR_WEEK,
                                                onClick = {
                                                    updateSearchSortMode(BrowserSearchSortMode.POPULAR_WEEK)
                                                },
                                                label = { Text("Week") }
                                            )
                                            FilterChip(
                                                selected = searchSortMode == BrowserSearchSortMode.POPULAR_ALL_TIME,
                                                onClick = {
                                                    updateSearchSortMode(BrowserSearchSortMode.POPULAR_ALL_TIME)
                                                },
                                                label = { Text("All Time") }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        itemsIndexed(
                            items = listRows,
                            key = { _, row -> row.code },
                            contentType = { _, _ -> "browserGalleryRow" }
                        ) { index, row ->
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
                                    ?: BrowserLocalLibraryState(exists = false, rating = 0, isRead = false, pinned = false),
                                loading = loadingDetailCode == row.code,
                                browserDuplicateCheckMode = browserDuplicateCheckMode,
                                duplicateHint = duplicateHintsByCode[row.code],
                                duplicateHintLoading = duplicateHintLoadingByCode[row.code] == true,
                                importFlashKey = importFlashEpochByCode[row.code] ?: 0,
                                duplicateSeedVersion = duplicateSeedVersion,
                                duplicateChecksPaused = listScrollState.isScrollInProgress || incognitoModeEnabled,
                                onOpen = { openDetail(row.code) },
                                onOpenSlideshow = { openDetail(row.code) },
                                onOpenDuplicateHint = { hint ->
                                    duplicateComparisonState = BrowserDuplicateComparisonState(
                                        row = row,
                                        hint = hint
                                    )
                                },
                                onEnsureDuplicateHint = { queueDuplicateHintForRow(row) },
                                onQuickTogglePinned = { code, _ ->
                                    applyQuickLibraryActionFromList(
                                        code = code,
                                        action = BrowserPendingLibraryAction.TogglePinned
                                    )
                                },
                                onQuickToggleRead = { code, _ ->
                                    applyQuickLibraryActionFromList(
                                        code = code,
                                        action = BrowserPendingLibraryAction.ToggleRead
                                    )
                                },
                                onQuickSetRating = { code, rating ->
                                    applyQuickLibraryActionFromList(
                                        code = code,
                                        action = BrowserPendingLibraryAction.SetRating(rating.coerceIn(0, 5))
                                    )
                                },
                                onHide = { code -> hideSuggestionCode(code) },
                                onActionBlocked = {
                                    Toast.makeText(
                                        context,
                                        "Library gestures are disabled in incognito mode.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
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
        }

        if (incognitoToggleAuthPending) {
            val activity = context as? FragmentActivity
            val keyboardController = LocalSoftwareKeyboardController.current
            val view = LocalView.current
            val density = LocalDensity.current
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
            val biometricManager = remember(context) { BiometricManager.from(context) }
            val biometricAvailable = remember(biometricManager) {
                biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                    BiometricManager.BIOMETRIC_SUCCESS
            }
            val focusRequester = remember(incognitoToggleAuthNonce) { FocusRequester() }
            var pinInput by remember(incognitoToggleAuthNonce) { mutableStateOf("") }
            var helperMessage by remember(incognitoToggleAuthNonce) { mutableStateOf<String?>(null) }
            var biometricPromptInFlight by remember(incognitoToggleAuthNonce) { mutableStateOf(false) }
            var autoPromptedNonce by remember { mutableStateOf<Long?>(null) }

            fun dismissIncognitoToggleAuth() {
                if (!incognitoToggleAllowCancel) return
                keyboardController?.hide()
                incognitoToggleAuthPending = false
                incognitoToggleAllowCancel = false
            }

            fun completeIncognitoToggleAuth() {
                keyboardController?.hide()
                incognitoToggleAuthPending = false
                incognitoToggleAllowCancel = false
                applyIncognitoModeToggle()
            }

            fun submitPin() {
                val normalized = normalizeBrowserPinInput(pinInput)
                pinInput = normalized
                if (normalized.isBlank()) {
                    helperMessage = "Enter PIN."
                    return
                }
                val expected = hashBrowserPin(normalized, incognitoTogglePinSalt)
                if (expected == incognitoTogglePinHash) {
                    helperMessage = null
                    completeIncognitoToggleAuth()
                } else {
                    helperMessage = "Incorrect PIN."
                }
            }

            fun startBiometricPrompt() {
                if (!incognitoToggleAuthPending || !incognitoToggleBiometricEnabled) return
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
                            completeIncognitoToggleAuth()
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

            LaunchedEffect(
                incognitoToggleAuthPending,
                incognitoToggleAuthNonce,
                incognitoToggleBiometricEnabled,
                biometricAvailable
            ) {
                if (!incognitoToggleAuthPending) return@LaunchedEffect
                if (!incognitoToggleBiometricEnabled || !biometricAvailable) return@LaunchedEffect
                if (autoPromptedNonce == incognitoToggleAuthNonce) return@LaunchedEffect
                autoPromptedNonce = incognitoToggleAuthNonce
                startBiometricPrompt()
            }

            LaunchedEffect(incognitoToggleAuthPending, incognitoToggleAuthNonce) {
                if (!incognitoToggleAuthPending) return@LaunchedEffect
                delay(120)
                focusRequester.requestFocus()
                keyboardController?.show()
            }

            Dialog(
                onDismissRequest = ::dismissIncognitoToggleAuth,
                properties = DialogProperties(
                    dismissOnBackPress = incognitoToggleAllowCancel,
                    dismissOnClickOutside = incognitoToggleAllowCancel,
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
                        ) {
                            dismissIncognitoToggleAuth()
                        },
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
                                text = "Unlock Sauce Tracker",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Unlock to toggle incognito mode.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { raw ->
                                    pinInput = normalizeBrowserPinInput(raw)
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
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { pinInput = ""; helperMessage = null }) {
                                        Text("Clear")
                                    }
                                    if (incognitoToggleAllowCancel) {
                                        TextButton(
                                            onClick = {
                                                pinInput = ""
                                                helperMessage = null
                                                dismissIncognitoToggleAuth()
                                            }
                                        ) {
                                            Text("Cancel")
                                        }
                                    }
                                }
                                if (biometricAvailable && incognitoToggleBiometricEnabled) {
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

        pendingListImportRequest?.let { request ->
            AlertDialog(
                onDismissRequest = { pendingListImportRequest = null },
                title = { Text("Import Required") },
                text = {
                    Text(
                        text = "You must import this sauce to change rating, read status, or pin state. Import now?"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val confirmedRequest = request
                            pendingListImportRequest = null
                            val selectedSnapshot =
                                selectedDetail?.takeIf { it.summary.code == confirmedRequest.code }
                            scope.launch {
                                val refreshed = withContext(Dispatchers.IO) {
                                    val detailForImport = selectedSnapshot
                                        ?: runCatching {
                                            api.fetchGalleryDetail(confirmedRequest.code)
                                        }.getOrNull()
                                    if (detailForImport != null) {
                                        db.upsertGallery(toGalleryData(detailForImport))
                                    }
                                    if (db.getEntryDetail(confirmedRequest.code) != null) {
                                        when (val action = confirmedRequest.action) {
                                            is BrowserPendingLibraryAction.SetRating -> {
                                                db.setEntryRating(
                                                    confirmedRequest.code,
                                                    action.rating.coerceIn(0, 5)
                                                )
                                                db.setEntryRead(confirmedRequest.code, true)
                                            }
                                            is BrowserPendingLibraryAction.SetRead -> {
                                                db.setEntryRead(confirmedRequest.code, action.isRead)
                                            }
                                            is BrowserPendingLibraryAction.SetPinned -> {
                                                db.setEntryPinned(confirmedRequest.code, action.pinned)
                                            }
                                            BrowserPendingLibraryAction.ToggleRead -> {
                                                val local = db.getEntryDetail(confirmedRequest.code)
                                                db.setEntryRead(
                                                    confirmedRequest.code,
                                                    !(local?.isRead ?: false)
                                                )
                                            }
                                            BrowserPendingLibraryAction.TogglePinned -> {
                                                db.setEntryPinned(
                                                    confirmedRequest.code,
                                                    !db.isEntryPinned(confirmedRequest.code)
                                                )
                                            }
                                        }
                                    }
                                    val local = db.getEntryDetail(confirmedRequest.code)
                                    if (local != null) {
                                        BrowserLocalLibraryState(
                                            exists = true,
                                            rating = local.rating.coerceIn(0, 5),
                                            isRead = local.isRead,
                                            pinned = db.isEntryPinned(confirmedRequest.code)
                                        )
                                    } else {
                                        BrowserLocalLibraryState(
                                            exists = false,
                                            rating = 0,
                                            isRead = false,
                                            pinned = false
                                        )
                                    }
                                }
                                listLibraryStates = listLibraryStates.toMutableMap().apply {
                                    put(confirmedRequest.code, refreshed)
                                }
                                refreshDuplicateSeeds()
                                if (refreshed.exists) {
                                    triggerImportFlash(confirmedRequest.code)
                                }
                                if (!refreshed.exists) {
                                    Toast.makeText(
                                        context,
                                        "Could not import code ${confirmedRequest.code}.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    ) {
                        Text("Import")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { pendingListImportRequest = null }) {
                        Text("Cancel")
                    }
                }
            )
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
                    pendingSlideshowRatingCode = null
                    if (prompt.closeAfter) {
                        closeBrowserSecurely()
                    }
                },
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false
                ),
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
                                        RATING_STAR_GOLD
                                    } else {
                                        RATING_STAR_MUTED
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
                        if (prompt.wasReadBefore) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        ratingPromptState = prompt.copy(isReread = !prompt.isReread)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = prompt.isReread,
                                    onCheckedChange = { checked ->
                                        ratingPromptState = prompt.copy(isReread = checked)
                                    }
                                )
                                Text("Re-read", style = MaterialTheme.typography.bodyMedium)
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
                                        if (savePrompt.isReread) {
                                            db.recordEntryRatingSession(code, safeRating, isReread = true)
                                        } else {
                                            db.setEntryRating(code, safeRating)
                                            db.setEntryRead(code, true)
                                        }
                                    }
                                    entryExists
                                }
                                ratingPromptState = null
                                pendingSlideshowRatingCode = null
                                if (!saved) {
                                    Toast.makeText(
                                        context,
                                        "Could not save rating: failed to import entry $code.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                if (shouldClose) {
                                    closeBrowserSecurely()
                                }
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
                            pendingSlideshowRatingCode = null
                            if (shouldClose) {
                                closeBrowserSecurely()
                            }
                        }
                    ) {
                        Text("Skip")
                    }
                }
            )
        }
    }
}
}

