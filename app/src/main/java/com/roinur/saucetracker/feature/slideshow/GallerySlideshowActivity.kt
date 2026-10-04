@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.roinur.saucetracker.feature.slideshow

import com.roinur.saucetracker.*
import com.roinur.saucetracker.core.ui.theme.AccentMode
import com.roinur.saucetracker.core.ui.theme.applyAccentMode
import com.roinur.saucetracker.data.downloads.*
import com.roinur.saucetracker.data.database.SauceTrackerDatabase
import com.roinur.saucetracker.data.profile.ProfileStore
import com.roinur.saucetracker.data.source.MangaDexSourceAdapter
import com.roinur.saucetracker.data.source.SourceChapter
import com.roinur.saucetracker.data.source.SourceChapterCacheStore
import com.roinur.saucetracker.data.source.SourceId
import com.roinur.saucetracker.data.source.SourceReaderContent
import com.roinur.saucetracker.data.source.SourceRegistry

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.content.ContextCompat
import com.roinur.saucetracker.core.network.HttpClientFactory
import com.roinur.saucetracker.core.network.HttpClientProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

internal const val SLIDESHOW_PREFS_NAME = "nhtagbook_prefs"
internal const val SLIDESHOW_KEY_THEME_MODE = "theme_mode"
internal const val SLIDESHOW_KEY_ACCENT_MODE = "accent_mode"
internal const val SLIDESHOW_KEY_APP_LOCK_ENABLED = "app_lock_enabled"
internal const val SLIDESHOW_KEY_APP_LOCK_PIN_HASH = "app_lock_pin_hash"
internal const val SLIDESHOW_KEY_APP_LOCK_PIN_SALT = "app_lock_pin_salt"
internal const val SLIDESHOW_KEY_APP_LOCK_GRACE_UNTIL = "app_lock_grace_until"
internal const val SLIDESHOW_KEY_READING_MODE = "slideshow_reading_mode"
const val SLIDESHOW_KEY_HORIZONTAL_DIRECTION = "slideshow_horizontal_direction"
const val SLIDESHOW_KEY_VOLUME_BUTTON_NAVIGATION = "slideshow_volume_button_navigation"
internal const val SLIDESHOW_APP_LOCK_GRACE_MS = 30_000L
internal const val SLIDESHOW_INCOGNITO_OVERLAY_ALPHA = 0.82f

internal enum class SlideshowReadingMode {
    HORIZONTAL,
    VERTICAL
}

internal enum class SlideshowHoldChoice {
    HORIZONTAL,
    SCREENSHOT,
    VERTICAL
}

internal fun SlideshowReadingMode.toHoldChoice(): SlideshowHoldChoice = when (this) {
    SlideshowReadingMode.HORIZONTAL -> SlideshowHoldChoice.HORIZONTAL
    SlideshowReadingMode.VERTICAL -> SlideshowHoldChoice.VERTICAL
}

enum class SlideshowHorizontalDirection {
    WESTERN,
    MANGA
}

internal enum class SlideshowVolumeNavigation {
    VOLUME_UP,
    VOLUME_DOWN
}

class GallerySlideshowActivity : ComponentActivity() {
    private lateinit var db: SauceTrackerDatabase
    private var sessionCode: Int = 0
    private var sessionProfileId: String = ProfileStore.MAIN_PROFILE_ID
    private var sessionSourceId: String = "nhentai"
    private var sessionRemoteId: String = ""
    private var sessionChapterId: String = ""
    private var sessionNumPages: Int = 0
    private var sessionHasData: Boolean = false
    private var sessionStartMillisUtc: Long = 0L
    private var sessionStartPageIndex: Int = 0
    private var sessionFurthestPageIndex: Int = 0
    private val sessionViewedPageIndexes = mutableSetOf<Int>()
    private var sessionPersisted = false
    private var sessionMediaId: Long = 0L
    private var sessionCoverExt: String = "jpg"
    private var sessionIncognitoMode: Boolean = false
    private var currentVisiblePageIndex: Int = 0
    private var currentPrimaryPageUrls: List<String> = emptyList()
    private var currentFallbackPageUrls: List<String> = emptyList()
    private var screenCaptureCallback: Any? = null
    private var showRawPagePrompt by mutableStateOf(false)
    private var rawPageSaveInProgress by mutableStateOf(false)
    private var rawPageSaveMessage by mutableStateOf<String?>(null)
    private var volumeButtonNavigationEnabled = false
    private val volumeNavigationEvents = MutableSharedFlow<SlideshowVolumeNavigation>(
        extraBufferCapacity = 1
    )

    companion object {
        private const val EXTRA_CODE = "extra_code"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_MEDIA_ID = "extra_media_id"
        private const val EXTRA_COVER_EXT = "extra_cover_ext"
        private const val EXTRA_NUM_PAGES = "extra_num_pages"
        private const val EXTRA_START_PAGE = "extra_start_page"
        private const val EXTRA_INCOGNITO_MODE = "extra_incognito_mode"
        private const val EXTRA_SOURCE_ID = "extra_source_id"
        private const val EXTRA_REMOTE_ID = "extra_remote_id"
        private const val EXTRA_PAGE_URLS = "extra_page_urls"
        private const val EXTRA_FALLBACK_PAGE_URLS = "extra_fallback_page_urls"
        private const val EXTRA_CHAPTER_ID = "extra_chapter_id"
        fun createIntent(
            context: Context,
            code: Int,
            title: String,
            mediaId: Long,
            coverExt: String,
            numPages: Int,
            startPage: Int = 1,
            incognitoModeEnabled: Boolean = false,
            sourceId: String = "nhentai",
            remoteId: String = code.toString(),
            pageUrls: List<String> = emptyList(),
            fallbackPageUrls: List<String> = emptyList(),
            chapterId: String = ""
        ): Intent {
            return Intent(context, GallerySlideshowActivity::class.java)
                .putExtra(EXTRA_CODE, code)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_MEDIA_ID, mediaId)
                .putExtra(EXTRA_COVER_EXT, coverExt)
                .putExtra(EXTRA_NUM_PAGES, numPages)
                .putExtra(EXTRA_START_PAGE, startPage)
                .putExtra(EXTRA_INCOGNITO_MODE, incognitoModeEnabled)
                .putExtra(EXTRA_SOURCE_ID, sourceId)
                .putExtra(EXTRA_REMOTE_ID, remoteId)
                .putStringArrayListExtra(EXTRA_PAGE_URLS, ArrayList(pageUrls))
                .putStringArrayListExtra(EXTRA_FALLBACK_PAGE_URLS, ArrayList(fallbackPageUrls))
                .putExtra(EXTRA_CHAPTER_ID, chapterId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        val code = intent?.getIntExtra(EXTRA_CODE, 0) ?: 0
        val title = intent?.getStringExtra(EXTRA_TITLE)?.trim().orEmpty()
        val mediaId = intent?.getLongExtra(EXTRA_MEDIA_ID, 0L) ?: 0L
        val coverExt = intent?.getStringExtra(EXTRA_COVER_EXT)?.trim().orEmpty()
        val numPages = intent?.getIntExtra(EXTRA_NUM_PAGES, 0) ?: 0
        val startPage = intent?.getIntExtra(EXTRA_START_PAGE, 1) ?: 1
        val incognitoModeEnabled = intent?.getBooleanExtra(EXTRA_INCOGNITO_MODE, false) == true
        val sourceId = intent?.getStringExtra(EXTRA_SOURCE_ID)?.trim().orEmpty().ifBlank { "nhentai" }
        val remoteId = intent?.getStringExtra(EXTRA_REMOTE_ID)?.trim().orEmpty().ifBlank { code.toString() }
        val remotePageUrls = intent?.getStringArrayListExtra(EXTRA_PAGE_URLS).orEmpty()
        val remoteFallbackPageUrls = intent?.getStringArrayListExtra(EXTRA_FALLBACK_PAGE_URLS).orEmpty()
        val initialChapterId = intent?.getStringExtra(EXTRA_CHAPTER_ID)?.trim().orEmpty()
        val localBundle = if (sourceId == "nhentai") loadDownloadedGalleryBundle(applicationContext, code) else null
        val explicitPageUris = localBundle?.pageUriStrings.orEmpty().ifEmpty { remotePageUrls }
        val effectiveStartPage = startPage
        val slideshowTitle = localBundle?.title?.ifBlank { title } ?: title
        val slideshowPageCount = explicitPageUris.size.takeIf { it > 0 } ?: numPages
        val themeMode = loadThemeMode()
        val accentMode = loadAccentMode()
        val initialReadingMode = loadSlideshowReadingMode(applicationContext)
        val initialHorizontalDirection = loadSlideshowHorizontalDirection(applicationContext)
        volumeButtonNavigationEnabled = loadSlideshowVolumeButtonNavigationEnabled(applicationContext)
        window.attributes = window.attributes.apply {
            preferredRefreshRate = 120f
        }
        db = SauceTrackerDatabase(applicationContext)
        sessionProfileId = ProfileStore(db).activeProfileId()
        sessionCode = code
        sessionSourceId = sourceId
        sessionRemoteId = remoteId
        sessionChapterId = initialChapterId
        sessionMediaId = mediaId
        sessionCoverExt = coverExt
        sessionIncognitoMode = incognitoModeEnabled
        currentPrimaryPageUrls = explicitPageUris
        currentFallbackPageUrls = remoteFallbackPageUrls
        sessionNumPages = slideshowPageCount.coerceAtLeast(0)
        sessionHasData = if (explicitPageUris.isNotEmpty()) {
            sessionNumPages > 0
        } else {
            mediaId > 0L && sessionNumPages > 0
        }
        sessionStartPageIndex = (effectiveStartPage - 1).coerceAtLeast(0)
        currentVisiblePageIndex = sessionStartPageIndex
        sessionFurthestPageIndex = sessionStartPageIndex
        sessionViewedPageIndexes += sessionStartPageIndex
        sessionStartMillisUtc = System.currentTimeMillis()

        setContent {
            var exitFlowStarted by remember { mutableStateOf(false) }
            var advancingChapter by remember { mutableStateOf(false) }
            var refreshingChapterDelivery by remember { mutableStateOf(false) }
            var deliveryRefreshAttempts by remember { mutableStateOf(0) }
            var activeTitle by remember { mutableStateOf(slideshowTitle) }
            var activePageUris by remember { mutableStateOf(explicitPageUris) }
            var activeFallbackPageUris by remember { mutableStateOf(remoteFallbackPageUrls) }
            var activeStartPage by remember { mutableStateOf(effectiveStartPage) }
            var activeChapterId by remember { mutableStateOf(initialChapterId) }
            var initialReaderLoading by remember {
                mutableStateOf(sourceId == "mangadex" && explicitPageUris.isEmpty())
            }
            var initialReaderError by remember { mutableStateOf<String?>(null) }
            var deliveryNonce by remember { mutableStateOf(0) }
            var availableChapters by remember { mutableStateOf<List<SourceChapter>>(emptyList()) }
            var prefetchedNextReader by remember { mutableStateOf<SourceReaderContent?>(null) }
            var prefetchedPreviousReader by remember { mutableStateOf<SourceReaderContent?>(null) }
            val scope = rememberCoroutineScope()
            val mangaDexAdapter = remember(sourceId) {
                if (sourceId == "mangadex") {
                    SourceRegistry.createDefault().requireAdapter(SourceId("mangadex")) as MangaDexSourceAdapter
                } else null
            }

            fun saveCurrentRawPageFromHoldMenu() {
                if (incognitoModeEnabled || rawPageSaveInProgress) return
                rawPageSaveInProgress = true
                scope.launch {
                    val pageNumber = currentVisiblePageIndex + 1
                    val result = withContext(Dispatchers.IO) {
                        runCatching {
                            saveRemotePageToExperimentalGallery(
                                context = applicationContext,
                                candidateUrls = rawPageUrlCandidates(pageNumber),
                                displayName = rawPageDisplayName(activeTitle, pageNumber),
                                sourceId = sessionSourceId,
                                remoteId = sessionRemoteId
                            )
                        }
                    }
                    result.onSuccess {
                        Toast.makeText(
                            applicationContext,
                            "Raw page saved to Experimental Gallery.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }.onFailure { error ->
                        Toast.makeText(
                            applicationContext,
                            error.message ?: "Could not save the raw page.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    rawPageSaveInProgress = false
                }
            }

            // Browser and Library launch MangaDex immediately, just like NHentai. Resolve the
            // saved chapter position and At-Home manifest inside the already-visible reader.
            LaunchedEffect(mangaDexAdapter, remoteId, initialChapterId) {
                if (mangaDexAdapter != null && activePageUris.isEmpty()) {
                    initialReaderLoading = true
                    initialReaderError = null
                    val resolved = withContext(Dispatchers.IO) {
                        runCatching {
                            val progress = if (initialChapterId.isBlank()) {
                                db.sourceReaderProgress(sessionProfileId, sourceId, remoteId)
                            } else {
                                db.sourceChapterProgress(sessionProfileId, sourceId, remoteId)[initialChapterId]
                                    ?.let { chapter ->
                                        com.roinur.saucetracker.data.database.SourceReaderProgress(
                                            chapterId = chapter.chapterId,
                                            pageIndex = chapter.furthestPageIndex
                                        )
                                    }
                            }
                            val requestedChapter = initialChapterId.ifBlank { progress?.chapterId.orEmpty() }
                            val requestedStartIndex = progress
                                ?.takeIf { requestedChapter.isBlank() || it.chapterId == requestedChapter }
                                ?.pageIndex ?: (effectiveStartPage - 1)
                            val reader = runCatching {
                                MangaDexReaderWarmup.start(
                                    context = applicationContext,
                                    adapter = mangaDexAdapter,
                                    remoteId = remoteId,
                                    chapterId = requestedChapter.ifBlank { null },
                                    startPageIndex = requestedStartIndex
                                ).get()
                            }.recoverCatching {
                                if (progress != null) {
                                    MangaDexReaderWarmup.start(
                                        context = applicationContext,
                                        adapter = mangaDexAdapter,
                                        remoteId = remoteId,
                                        chapterId = null,
                                        startPageIndex = 0
                                    ).get()
                                } else throw it
                            }.getOrThrow()
                            reader to progress
                        }
                    }
                    resolved.onSuccess { (reader, progress) ->
                        val pages = reader.pageUrls.ifEmpty { reader.dataSaverPageUrls }
                        if (pages.isEmpty()) {
                            initialReaderError = "The MangaDex chapter contains no readable pages."
                        } else {
                            val fallbacks = if (reader.pageUrls.isNotEmpty()) reader.dataSaverPageUrls else emptyList()
                            val resolvedStart = (progress
                                ?.takeIf { it.chapterId == reader.chapterId }
                                ?.pageIndex?.plus(1) ?: effectiveStartPage).coerceIn(1, pages.size)
                            activeChapterId = reader.chapterId
                            activePageUris = pages
                            activeFallbackPageUris = fallbacks
                            activeStartPage = resolvedStart
                            activeTitle = mangaDexReaderTitle(title, reader)
                            currentPrimaryPageUrls = pages
                            currentFallbackPageUrls = fallbacks
                            currentVisiblePageIndex = resolvedStart - 1
                            beginChapterSession(reader.chapterId, pages.size, resolvedStart - 1)
                        }
                    }.onFailure { error ->
                        initialReaderError = error.message ?: "Could not load the MangaDex chapter."
                    }
                    initialReaderLoading = false
                }
            }
            LaunchedEffect(sourceId, remoteId, initialReaderLoading) {
                if (mangaDexAdapter != null && !initialReaderLoading) {
                    val cache = SourceChapterCacheStore(db)
                    val (fresh, cached) = withContext(Dispatchers.IO) {
                        val freshValue = cache.fresh(sourceId, remoteId)
                        freshValue to (freshValue ?: cache.load(sourceId, remoteId))
                    }
                    if (cached != null) availableChapters = cached.chapters
                    // A usable cached list wins first paint. Refreshing it is lower priority
                    // than resolving and displaying the current chapter.
                    if (fresh == null) {
                        val loaded = withContext(Dispatchers.IO) {
                            runCatching { loadAllMangaDexChapters(mangaDexAdapter, remoteId) }
                        }.getOrNull()
                        if (!loaded.isNullOrEmpty()) {
                            availableChapters = loaded
                            withContext(Dispatchers.IO) { cache.save(sourceId, remoteId, loaded) }
                        }
                    }
                }
            }
            LaunchedEffect(mangaDexAdapter, activeChapterId, availableChapters) {
                if (mangaDexAdapter != null && activeChapterId.isNotBlank() && availableChapters.isNotEmpty()) {
                    val next = nextMangaDexChapter(availableChapters, activeChapterId)
                    val previous = previousMangaDexChapter(availableChapters, activeChapterId)
                    val nextFuture = next?.let {
                        MangaDexReaderWarmup.start(
                            applicationContext, mangaDexAdapter, remoteId, it.id, 0,
                            prefetchPages = false
                        )
                    }
                    val previousFuture = previous?.let {
                        MangaDexReaderWarmup.start(
                            applicationContext, mangaDexAdapter, remoteId, it.id, Int.MAX_VALUE,
                            prefetchPages = false
                        )
                    }
                    val prefetched = withContext(Dispatchers.IO) {
                        runCatching { nextFuture?.get() }.getOrNull() to
                            runCatching { previousFuture?.get() }.getOrNull()
                    }
                    if (activeChapterId.isNotBlank()) {
                        prefetchedNextReader = prefetched.first
                        prefetchedPreviousReader = prefetched.second
                    }
                }
            }

            SlideshowTheme(
                themeMode = themeMode,
                accentMode = accentMode,
                incognitoModeEnabled = incognitoModeEnabled
            ) {
                if (initialReaderLoading || (initialReaderError != null && activePageUris.isEmpty())) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(28.dp)
                        ) {
                            if (initialReaderLoading) {
                                CircularProgressIndicator()
                                Text("Loading chapter…", color = MaterialTheme.colorScheme.onBackground)
                            } else {
                                Text(
                                    initialReaderError.orEmpty(),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    textAlign = TextAlign.Center
                                )
                                TextButton(onClick = { finish() }) { Text("Close") }
                            }
                        }
                    }
                } else {
                key(activeChapterId.ifBlank { "gallery" }, deliveryNonce) {
                SlideshowScreen(
                    code = code,
                    title = activeTitle,
                    mediaId = mediaId,
                    coverExt = coverExt,
                    numPages = activePageUris.size.takeIf { it > 0 } ?: slideshowPageCount,
                    startPage = activeStartPage,
                    incognitoModeEnabled = incognitoModeEnabled,
                    initialReadingMode = initialReadingMode,
                    initialHorizontalDirection = initialHorizontalDirection,
                    volumeNavigationEvents = volumeNavigationEvents,
                    explicitPageUris = activePageUris,
                    explicitFallbackPageUris = activeFallbackPageUris,
                    onPageViewed = { index ->
                        currentVisiblePageIndex = index.coerceAtLeast(0)
                        sessionViewedPageIndexes += index.coerceAtLeast(0)
                        if (index > sessionFurthestPageIndex) {
                            sessionFurthestPageIndex = index
                        }
                        if (sessionChapterId.isNotBlank()) {
                            runCatching {
                                db.saveSourceReaderProgress(
                                    profileId = sessionProfileId,
                                    sourceId = sessionSourceId,
                                    remoteId = sessionRemoteId,
                                    chapterId = sessionChapterId,
                                    pageIndex = index,
                                    pageCount = sessionNumPages
                                )
                            }
                        }
                    },
                    onDone = {
                        if (!exitFlowStarted) {
                            exitFlowStarted = true
                            persistReadingSessionIfNeeded()
                            finish()
                        }
                    },
                    onReachedEnd = {
                        if (mangaDexAdapter == null || activeChapterId.isBlank() || advancingChapter) {
                            if (!exitFlowStarted) {
                                exitFlowStarted = true
                                persistReadingSessionIfNeeded()
                                finish()
                            }
                        } else {
                            advancingChapter = true
                            runCatching {
                                db.markSourceChapterCompleted(
                                    profileId = sessionProfileId,
                                    sourceId = sessionSourceId,
                                    remoteId = sessionRemoteId,
                                    chapterId = activeChapterId
                                )
                            }
                            scope.launch {
                                val chapters = if (availableChapters.isNotEmpty()) availableChapters else {
                                    withContext(Dispatchers.IO) {
                                        loadAllMangaDexChapters(mangaDexAdapter, remoteId)
                                    }.also { availableChapters = it }
                                }
                                val next = nextMangaDexChapter(chapters, activeChapterId)
                                val nextReader = prefetchedNextReader?.takeIf { it.chapterId == next?.id } ?: next?.let { chapter ->
                                    withContext(Dispatchers.IO) {
                                        runCatching {
                                            mangaDexAdapter.fetchReaderContent(remoteId, chapter.id)
                                        }.getOrNull()
                                    }
                                }
                                if (nextReader == null) {
                                    exitFlowStarted = true
                                    persistReadingSessionIfNeeded()
                                    finish()
                                } else {
                                    val nextPages = nextReader.pageUrls.ifEmpty { nextReader.dataSaverPageUrls }
                                    val nextFallbackPages = if (nextReader.pageUrls.isNotEmpty()) {
                                        nextReader.dataSaverPageUrls
                                    } else {
                                        emptyList()
                                    }
                                    persistReadingSessionIfNeeded()
                                    beginChapterSession(nextReader.chapterId, nextPages.size, 0)
                                    activeChapterId = nextReader.chapterId
                                    deliveryRefreshAttempts = 0
                                    activePageUris = nextPages
                                    activeFallbackPageUris = nextFallbackPages
                                    currentPrimaryPageUrls = nextPages
                                    currentFallbackPageUrls = nextFallbackPages
                                    currentVisiblePageIndex = 0
                                    activeStartPage = 1
                                    activeTitle = mangaDexReaderTitle(title, nextReader)
                                    advancingChapter = false
                                }
                            }
                        }
                    },
                    onReachedStart = {
                        if (mangaDexAdapter != null && activeChapterId.isNotBlank() && !advancingChapter) {
                            advancingChapter = true
                            scope.launch {
                                val chapters = if (availableChapters.isNotEmpty()) availableChapters else withContext(Dispatchers.IO) {
                                    loadAllMangaDexChapters(mangaDexAdapter, remoteId)
                                }.also { availableChapters = it }
                                val previous = previousMangaDexChapter(chapters, activeChapterId)
                                val previousReader = prefetchedPreviousReader?.takeIf { it.chapterId == previous?.id } ?: previous?.let { chapter -> withContext(Dispatchers.IO) {
                                    runCatching { mangaDexAdapter.fetchReaderContent(remoteId, chapter.id) }.getOrNull()
                                } }
                                if (previousReader != null) {
                                    val previousPages = previousReader.pageUrls.ifEmpty { previousReader.dataSaverPageUrls }
                                    val previousFallback = if (previousReader.pageUrls.isNotEmpty()) previousReader.dataSaverPageUrls else emptyList()
                                    val lastIndex = (previousPages.size - 1).coerceAtLeast(0)
                                    persistReadingSessionIfNeeded()
                                    beginChapterSession(previousReader.chapterId, previousPages.size, lastIndex)
                                    activeChapterId = previousReader.chapterId
                                    deliveryRefreshAttempts = 0
                                    activePageUris = previousPages
                                    activeFallbackPageUris = previousFallback
                                    currentPrimaryPageUrls = previousPages
                                    currentFallbackPageUrls = previousFallback
                                    currentVisiblePageIndex = lastIndex
                                    activeStartPage = lastIndex + 1
                                    activeTitle = mangaDexReaderTitle(title, previousReader)
                                }
                                advancingChapter = false
                            }
                        }
                    },
                    nextChapterLabel = nextMangaDexChapter(availableChapters, activeChapterId)?.number
                        ?.takeIf(String::isNotBlank)?.let { "Ch. $it" },
                    previousChapterLabel = previousMangaDexChapter(availableChapters, activeChapterId)?.number
                        ?.takeIf(String::isNotBlank)?.let { "Ch. $it" },
                    onSaveRawPage = ::saveCurrentRawPageFromHoldMenu,
                    onExplicitPageLoadFailed = {
                        if (mangaDexAdapter != null && activeChapterId.isNotBlank() && !refreshingChapterDelivery && deliveryRefreshAttempts < 3) {
                            refreshingChapterDelivery = true
                            deliveryRefreshAttempts += 1
                            mangaDexAdapter.invalidateReaderContent(activeChapterId)
                            MangaDexReaderWarmup.invalidate(applicationContext, remoteId, activeChapterId)
                            scope.launch {
                                val refreshed = withContext(Dispatchers.IO) {
                                    runCatching {
                                        MangaDexReaderWarmup.start(
                                            context = applicationContext,
                                            adapter = mangaDexAdapter,
                                            remoteId = remoteId,
                                            chapterId = activeChapterId,
                                            startPageIndex = currentVisiblePageIndex,
                                            prefetchPages = true
                                        ).get()
                                    }.getOrNull()
                                }
                                if (refreshed != null && refreshed.chapterId == activeChapterId) {
                                    activePageUris = refreshed.pageUrls.ifEmpty { refreshed.dataSaverPageUrls }
                                    activeFallbackPageUris = if (refreshed.pageUrls.isNotEmpty()) refreshed.dataSaverPageUrls else emptyList()
                                    currentPrimaryPageUrls = activePageUris
                                    currentFallbackPageUrls = activeFallbackPageUris
                                    deliveryNonce += 1
                                }
                                delay(600)
                                refreshingChapterDelivery = false
                            }
                        }
                    }
                )
                }
                }
                if (showRawPagePrompt) {
                    AlertDialog(
                        onDismissRequest = {
                            if (!rawPageSaveInProgress) {
                                showRawPagePrompt = false
                                rawPageSaveMessage = null
                            }
                        },
                        title = { Text("Save the raw page instead?") },
                        text = {
                            Text(
                                rawPageSaveMessage ?: if (rawPageSaveInProgress) {
                                    "Downloading the current page at the highest available quality..."
                                } else {
                                    "The screenshot was detected. Save only this page at its original quality and add it to Experimental Gallery?"
                                }
                            )
                        },
                        confirmButton = {
                            TextButton(
                                enabled = !rawPageSaveInProgress,
                                onClick = {
                                    rawPageSaveInProgress = true
                                    rawPageSaveMessage = null
                                    scope.launch {
                                        val pageNumber = currentVisiblePageIndex + 1
                                        val result = withContext(Dispatchers.IO) {
                                            runCatching {
                                                saveRemotePageToExperimentalGallery(
                                                    context = applicationContext,
                                                    candidateUrls = rawPageUrlCandidates(pageNumber),
                                                    displayName = rawPageDisplayName(activeTitle, pageNumber),
                                                    sourceId = sessionSourceId,
                                                    remoteId = sessionRemoteId
                                                )
                                            }
                                        }
                                        result.onSuccess {
                                            rawPageSaveMessage = "Saved to Experimental Gallery."
                                            delay(850L)
                                            showRawPagePrompt = false
                                            rawPageSaveMessage = null
                                        }.onFailure { error ->
                                            rawPageSaveMessage = error.message ?: "Could not save the raw page."
                                        }
                                        rawPageSaveInProgress = false
                                    }
                                }
                            ) { Text(if (rawPageSaveInProgress) "Saving..." else "Save raw page") }
                        },
                        dismissButton = {
                            TextButton(
                                enabled = !rawPageSaveInProgress,
                                onClick = {
                                    showRawPagePrompt = false
                                    rawPageSaveMessage = null
                                }
                            ) { Text("Keep screenshot") }
                        }
                    )
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val navigation = slideshowVolumeNavigationForKey(event.keyCode)
        if (volumeButtonNavigationEnabled && navigation != null) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                volumeNavigationEvents.tryEmit(navigation)
            }
            // Consume both down and up so Android never changes the media volume.
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= 34 && !sessionIncognitoMode) {
            registerScreenshotDetection()
        }
    }

    override fun onDestroy() {
        persistReadingSessionIfNeeded()
        if (::db.isInitialized) {
            db.close()
        }
        super.onDestroy()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onStop() {
        if (Build.VERSION.SDK_INT >= 34) unregisterScreenshotDetection()
        val prefs = getSharedPreferences(SLIDESHOW_PREFS_NAME, Context.MODE_PRIVATE)
        val appLockEnabled = prefs.getBoolean(SLIDESHOW_KEY_APP_LOCK_ENABLED, false)
        val pinHash = prefs.getString(SLIDESHOW_KEY_APP_LOCK_PIN_HASH, "").orEmpty()
        val pinSalt = prefs.getString(SLIDESHOW_KEY_APP_LOCK_PIN_SALT, "").orEmpty()
        if (appLockEnabled && pinHash.isNotBlank() && pinSalt.isNotBlank()) {
            prefs.edit()
                .putLong(
                    SLIDESHOW_KEY_APP_LOCK_GRACE_UNTIL,
                    System.currentTimeMillis() + SLIDESHOW_APP_LOCK_GRACE_MS
                )
                .apply()
        }
        super.onStop()
    }

    @androidx.annotation.RequiresApi(34)
    private fun registerScreenshotDetection() {
        if (screenCaptureCallback != null) return
        val callback = Activity.ScreenCaptureCallback {
            if (!rawPageSaveInProgress) showRawPagePrompt = true
        }
        registerScreenCaptureCallback(ContextCompat.getMainExecutor(this), callback)
        screenCaptureCallback = callback
    }

    @androidx.annotation.RequiresApi(34)
    private fun unregisterScreenshotDetection() {
        val callback = screenCaptureCallback as? Activity.ScreenCaptureCallback ?: return
        runCatching { unregisterScreenCaptureCallback(callback) }
        screenCaptureCallback = null
    }

    private fun rawPageUrlCandidates(pageNumber: Int): List<String> {
        val index = (pageNumber - 1).coerceAtLeast(0)
        val explicit = buildList {
            // MangaDex displays the smaller reader rendition first, while the fallback
            // deliberately retains the original page for raw export.
            if (sessionSourceId == "mangadex") {
                currentFallbackPageUrls.getOrNull(index)?.let(::add)
                currentPrimaryPageUrls.getOrNull(index)?.let(::add)
            } else {
                currentPrimaryPageUrls.getOrNull(index)?.let(::add)
                currentFallbackPageUrls.getOrNull(index)?.let(::add)
            }
        }
        if (explicit.isNotEmpty()) return explicit.distinct()
        if (sessionMediaId <= 0L) return emptyList()
        return buildGalleryPageExtensions(sessionCoverExt, null)
            .map { extension -> buildGalleryImageUrl(sessionMediaId, pageNumber, extension) }
    }

    private fun rawPageDisplayName(title: String, pageNumber: Int): String {
        val chapter = sessionChapterId.takeIf(String::isNotBlank)
            ?.take(12)
            ?.let { "_chapter_$it" }
            .orEmpty()
        return "${title.take(48)}${chapter}_page_$pageNumber"
    }

    private fun loadThemeMode(): ThemeMode {
        val prefs = getSharedPreferences(SLIDESHOW_PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(SLIDESHOW_KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.SYSTEM
    }

    private fun loadAccentMode(): AccentMode {
        val prefs = getSharedPreferences(SLIDESHOW_PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(SLIDESHOW_KEY_ACCENT_MODE, AccentMode.AUTO.name)
        return AccentMode.entries.firstOrNull { it.name == raw } ?: AccentMode.AUTO
    }

    private fun loadSlideshowReadingMode(context: Context): SlideshowReadingMode {
        val raw = context.getSharedPreferences(SLIDESHOW_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(SLIDESHOW_KEY_READING_MODE, SlideshowReadingMode.HORIZONTAL.name)
        return SlideshowReadingMode.entries.firstOrNull { it.name == raw } ?: SlideshowReadingMode.HORIZONTAL
    }

    private fun persistReadingSessionIfNeeded() {
        if (sessionPersisted) return
        sessionPersisted = true
        if (!::db.isInitialized) return
        if (!sessionHasData || sessionCode <= 0 || sessionNumPages <= 0) return

        val endMillisUtc = System.currentTimeMillis().coerceAtLeast(sessionStartMillisUtc)
        val secondsElapsed = ((endMillisUtc - sessionStartMillisUtc) / 1000L).coerceAtLeast(1L)
        val maxIndex = (sessionNumPages - 1).coerceAtLeast(0)
        val pagesViewed = sessionViewedPageIndexes
            .map { it.coerceIn(0, maxIndex) }
            .toSet()
            .size
            .coerceIn(1, sessionNumPages)

        runCatching {
            db.insertReadingSession(
                entryCode = sessionCode,
                startedAtMillisUtc = sessionStartMillisUtc,
                endedAtMillisUtc = endMillisUtc,
                pagesViewed = pagesViewed,
                secondsElapsed = secondsElapsed,
                profileId = sessionProfileId,
                sourceId = sessionSourceId,
                remoteId = sessionRemoteId,
                chapterId = sessionChapterId
            )
        }
    }

    private fun beginChapterSession(chapterId: String, pageCount: Int, startPageIndex: Int) {
        sessionPersisted = false
        sessionChapterId = chapterId
        sessionNumPages = pageCount.coerceAtLeast(0)
        sessionHasData = sessionNumPages > 0
        sessionStartPageIndex = startPageIndex.coerceAtLeast(0)
        sessionFurthestPageIndex = sessionStartPageIndex
        sessionViewedPageIndexes.clear()
        sessionViewedPageIndexes += sessionStartPageIndex
        sessionStartMillisUtc = System.currentTimeMillis()
    }

    private fun loadAllMangaDexChapters(adapter: MangaDexSourceAdapter, remoteId: String): List<SourceChapter> {
        val chapters = mutableListOf<SourceChapter>()
        var offset = 0
        repeat(10) {
            val page = adapter.fetchChapters(remoteId, offset = offset, limit = 100)
            chapters += page.chapters
            if (!page.hasMore || page.chapters.isEmpty()) return chapters.distinctBy { it.id }
            offset += page.chapters.size
        }
        return chapters.distinctBy { it.id }
    }

    private fun nextMangaDexChapter(chapters: List<SourceChapter>, currentChapterId: String): SourceChapter? {
        val currentIndex = chapters.indexOfFirst { it.id == currentChapterId }
        if (currentIndex <= 0) return null
        val currentNumber = chapters[currentIndex].number
        return chapters.subList(0, currentIndex)
            .asReversed()
            .firstOrNull { currentNumber.isBlank() || it.number != currentNumber }
    }

    private fun previousMangaDexChapter(chapters: List<SourceChapter>, currentChapterId: String): SourceChapter? {
        val currentIndex = chapters.indexOfFirst { it.id == currentChapterId }
        if (currentIndex < 0 || currentIndex >= chapters.lastIndex) return null
        val currentNumber = chapters[currentIndex].number
        return chapters.subList(currentIndex + 1, chapters.size)
            .firstOrNull { currentNumber.isBlank() || it.number != currentNumber }
    }

    private fun mangaDexReaderTitle(seriesTitle: String, reader: SourceReaderContent): String {
        val cleanSeriesTitle = seriesTitle.substringBefore(" — ").trim()
        val credit = reader.scanlationGroups.takeIf { it.isNotEmpty() }
            ?.joinToString(" · ")?.let { " · $it" }.orEmpty()
        return listOf(cleanSeriesTitle, reader.displayTitle)
            .filter(String::isNotBlank)
            .joinToString(" — ") + credit
    }
}

internal fun storeSlideshowReadingMode(context: Context, mode: SlideshowReadingMode) {
    context.getSharedPreferences(SLIDESHOW_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(SLIDESHOW_KEY_READING_MODE, mode.name)
        .apply()
}

fun loadSlideshowHorizontalDirection(context: Context): SlideshowHorizontalDirection {
    val raw = context.getSharedPreferences(SLIDESHOW_PREFS_NAME, Context.MODE_PRIVATE)
        .getString(SLIDESHOW_KEY_HORIZONTAL_DIRECTION, SlideshowHorizontalDirection.WESTERN.name)
    return SlideshowHorizontalDirection.entries.firstOrNull { it.name == raw }
        ?: SlideshowHorizontalDirection.WESTERN
}

fun storeSlideshowHorizontalDirection(context: Context, direction: SlideshowHorizontalDirection) {
    context.getSharedPreferences(SLIDESHOW_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(SLIDESHOW_KEY_HORIZONTAL_DIRECTION, direction.name)
        .apply()
}

fun loadSlideshowVolumeButtonNavigationEnabled(context: Context): Boolean {
    return context.getSharedPreferences(SLIDESHOW_PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(SLIDESHOW_KEY_VOLUME_BUTTON_NAVIGATION, false)
}

fun storeSlideshowVolumeButtonNavigationEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(SLIDESHOW_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(SLIDESHOW_KEY_VOLUME_BUTTON_NAVIGATION, enabled)
        .apply()
}

internal fun isSlideshowVolumeKey(keyCode: Int): Boolean {
    return slideshowVolumeNavigationForKey(keyCode) != null
}

internal fun slideshowVolumeNavigationForKey(keyCode: Int): SlideshowVolumeNavigation? {
    return when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP -> SlideshowVolumeNavigation.VOLUME_UP
        KeyEvent.KEYCODE_VOLUME_DOWN -> SlideshowVolumeNavigation.VOLUME_DOWN
        else -> null
    }
}

@Composable
internal fun SlideshowTheme(
    themeMode: ThemeMode,
    accentMode: AccentMode,
    incognitoModeEnabled: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val extraDark = remember(context) {
        com.roinur.saucetracker.core.preferences.SaucePreferences.from(context)
            .boolean(com.roinur.saucetracker.core.preferences.KEY_EXTRA_DARK)
    }
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
        incognitoModeEnabled -> incognitoScheme
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            applyAccentMode(
                baseScheme = com.roinur.saucetracker.core.ui.theme.applyExtraDarkMode(
                    if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context), useDark, extraDark
                ),
                accentMode = accentMode,
                isDark = useDark
            )
        }
        else -> {
            applyAccentMode(
                baseScheme = com.roinur.saucetracker.core.ui.theme.applyExtraDarkMode(fallbackScheme, useDark, extraDark),
                accentMode = accentMode,
                isDark = useDark
            )
        }
    }

    MaterialTheme(colorScheme = colorScheme) {
        content()
    }
}

@Composable
internal fun SlideshowApplySystemBars(
    immersionMode: Boolean
) {
    val view = LocalView.current
    if (view.isInEditMode) return

    val color = MaterialTheme.colorScheme.background.toArgb()
    val darkContent = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val activity = view.context as? Activity ?: return
    DisposableEffect(view, activity, immersionMode, color, darkContent) {
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = color
        window.navigationBarColor = color
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = darkContent
            isAppearanceLightNavigationBars = darkContent
        }
        val compat = WindowInsetsControllerCompat(window, view)
        compat.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (immersionMode) {
            compat.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            compat.show(WindowInsetsCompat.Type.statusBars())
        }
        onDispose { }
    }
}

internal fun mapSlideshowHoldChoice(
    x: Float,
    y: Float,
    screenWidthPx: Float,
    screenHeightPx: Float,
    density: Float,
    screenshotEnabled: Boolean = true
): SlideshowHoldChoice? {
    val optionWidth = 138f * density
    val gap = 16f * density
    val rowWidth = (optionWidth * 2f) + gap
    val rowLeft = (screenWidthPx - rowWidth) / 2f
    val leftCenterX = rowLeft + (optionWidth / 2f)
    val rightCenterX = rowLeft + optionWidth + gap + (optionWidth / 2f)
    val optionCenterY = screenHeightPx / 2f
    val screenshotCenterX = screenWidthPx / 2f
    val screenshotCenterY = (screenHeightPx / 2f) + (190f * density)
    // The visible screenshot control is 76 dp wide. Its pickup circle is deliberately
    // only 1.5x the visible 38 dp radius so the area below the mode cards does not
    // accidentally become one large screenshot target.
    val screenshotPickupRadius = 57f * density

    fun distanceSquared(centerX: Float, centerY: Float): Float =
        ((x - centerX) * (x - centerX)) + ((y - centerY) * (y - centerY))

    if (
        screenshotEnabled &&
        distanceSquared(screenshotCenterX, screenshotCenterY) <=
        screenshotPickupRadius * screenshotPickupRadius
    ) {
        return SlideshowHoldChoice.SCREENSHOT
    }

    return listOf(
        SlideshowHoldChoice.HORIZONTAL to distanceSquared(leftCenterX, optionCenterY),
        SlideshowHoldChoice.VERTICAL to distanceSquared(rightCenterX, optionCenterY)
    ).minByOrNull { it.second }?.first
}

internal fun currentVerticalPageIndex(
    listState: LazyListState,
    safePages: Int
): Int {
    if (safePages <= 0) return 0
    val layoutInfo = listState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) {
        return listState.firstVisibleItemIndex.coerceIn(0, safePages - 1)
    }
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
    return visibleItems
        .minByOrNull { abs((it.offset + (it.size / 2f)) - viewportCenter) }
        ?.index
        ?.coerceIn(0, safePages - 1)
        ?: listState.firstVisibleItemIndex.coerceIn(0, safePages - 1)
}

internal fun verticalPageAlignmentDelta(
    targetIndex: Int,
    lastIndex: Int,
    viewportStart: Int,
    viewportEnd: Int,
    itemOffset: Int,
    itemSize: Int
): Float {
    return when (targetIndex) {
        0 -> itemOffset - viewportStart.toFloat()
        lastIndex -> (itemOffset + itemSize) - viewportEnd.toFloat()
        else -> {
            val viewportCenter = (viewportStart + viewportEnd) / 2f
            val itemCenter = itemOffset + (itemSize / 2f)
            itemCenter - viewportCenter
        }
    }
}

internal fun isSlideshowCenterTap(
    x: Float,
    y: Float,
    widthPx: Float,
    heightPx: Float
): Boolean {
    val minX = widthPx * 0.40f
    val maxX = widthPx * 0.60f
    val minY = heightPx * 0.40f
    val maxY = heightPx * 0.60f
    return x in minX..maxX && y in minY..maxY
}

@Composable
internal fun SlideshowReadingModeChooser(
    highlightedChoice: SlideshowHoldChoice,
    screenshotEnabled: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.36f))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Final)
                        event.changes.forEach { it.consume() }
                        if (event.changes.none { it.pressed }) break
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SlideshowModeOptionCard(
                label = "Horizontal",
                selected = highlightedChoice == SlideshowHoldChoice.HORIZONTAL,
                shimmer = highlightedChoice == SlideshowHoldChoice.HORIZONTAL
            ) {
                ReadingModePreviewIcon(SlideshowReadingMode.HORIZONTAL)
            }
            SlideshowModeOptionCard(
                label = "Vertical",
                selected = highlightedChoice == SlideshowHoldChoice.VERTICAL,
                shimmer = highlightedChoice == SlideshowHoldChoice.VERTICAL
            ) {
                ReadingModePreviewIcon(SlideshowReadingMode.VERTICAL)
            }
        }
        SlideshowScreenshotOption(
            selected = highlightedChoice == SlideshowHoldChoice.SCREENSHOT,
            enabled = screenshotEnabled,
            modifier = Modifier.offset(y = 190.dp)
        )
    }
}

@Composable
private fun SlideshowScreenshotOption(
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val ringAlpha by rememberInfiniteTransition(label = "slideshowScreenshotPulse").animateFloat(
        initialValue = 0.28f,
        targetValue = 0.72f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1200, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "slideshowScreenshotRingAlpha"
    )
    Surface(
        modifier = modifier.size(76.dp),
        shape = CircleShape,
        color = when {
            !enabled -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
            selected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        },
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = when {
                !enabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
                selected -> accent.copy(alpha = ringAlpha)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            }
        ),
        shadowElevation = if (selected) 8.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (selected && enabled) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(accent.copy(alpha = ringAlpha * 0.18f), radius = size.minDimension * 0.43f)
                    drawCircle(
                        accent.copy(alpha = ringAlpha * 0.42f),
                        radius = size.minDimension * 0.34f,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
            Icon(
                painter = painterResource(R.drawable.ic_screenshot_24),
                contentDescription = "Save raw page to Experimental Gallery",
                tint = if (enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
internal fun SlideshowModeOptionCard(
    label: String,
    selected: Boolean,
    shimmer: Boolean,
    icon: @Composable () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val cornerPx = 24.dp
    val shimmerPulse = rememberInfiniteTransition(label = "slideshowModeShimmerPulse")
    val borderAlpha by shimmerPulse.animateFloat(
        initialValue = 0.56f,
        targetValue = 0.94f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 2200,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "slideshowModeBorderAlpha"
    )
    val shimmerTravel by shimmerPulse.animateFloat(
        initialValue = -1.35f,
        targetValue = 2.35f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 3600,
                easing = androidx.compose.animation.core.LinearEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "slideshowModeShimmerTravel"
    )
    val shimmerAlpha = if (shimmer) 1f else 0f
    Card(
        modifier = Modifier
            .width(138.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                primary.copy(alpha = 0.22f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawWithContent {
                    drawContent()
                    if (shimmerAlpha > 0.001f) {
                        val sweepCenterX = size.width * shimmerTravel
                        val shimmerBrush = Brush.linearGradient(
                            colors = listOf(
                                primary.copy(alpha = 0f),
                                onPrimary.copy(alpha = 0.08f * shimmerAlpha),
                                primary.copy(alpha = 0.18f * shimmerAlpha),
                                onPrimary.copy(alpha = 0.30f * shimmerAlpha),
                                primary.copy(alpha = 0.18f * shimmerAlpha),
                                onPrimary.copy(alpha = 0.08f * shimmerAlpha),
                                primary.copy(alpha = 0f)
                            ),
                            start = Offset(sweepCenterX - (size.width * 0.95f), 0f),
                            end = Offset(sweepCenterX + (size.width * 0.95f), size.height)
                        )
                        drawRoundRect(
                            brush = shimmerBrush,
                            cornerRadius = CornerRadius(cornerPx.toPx(), cornerPx.toPx())
                        )
                        drawRoundRect(
                            color = primary.copy(alpha = borderAlpha * 0.72f),
                            cornerRadius = CornerRadius(cornerPx.toPx(), cornerPx.toPx()),
                            style = Stroke(width = 2.4.dp.toPx())
                        )
                    }
                }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
internal fun ReadingModePreviewIcon(mode: SlideshowReadingMode) {
    val accent = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.36f)
    Canvas(modifier = Modifier.size(82.dp)) {
        val cardWidth = if (mode == SlideshowReadingMode.HORIZONTAL) size.width * 0.34f else size.width * 0.70f
        val cardHeight = if (mode == SlideshowReadingMode.HORIZONTAL) size.height * 0.72f else size.height * 0.22f
        val corner = size.minDimension * 0.09f
        val stroke = Stroke(width = size.minDimension * 0.03f)

        fun drawPreviewCard(center: Offset, highlighted: Boolean) {
            drawRoundRect(
                color = if (highlighted) accent.copy(alpha = 0.18f) else muted.copy(alpha = 0.10f),
                topLeft = Offset(center.x - cardWidth / 2f, center.y - cardHeight / 2f),
                size = Size(cardWidth, cardHeight),
                cornerRadius = CornerRadius(corner, corner),
                style = Fill
            )
            drawRoundRect(
                color = if (highlighted) accent else muted,
                topLeft = Offset(center.x - cardWidth / 2f, center.y - cardHeight / 2f),
                size = Size(cardWidth, cardHeight),
                cornerRadius = CornerRadius(corner, corner),
                style = stroke
            )
        }

        if (mode == SlideshowReadingMode.HORIZONTAL) {
            drawPreviewCard(Offset(size.width * 0.28f, size.height * 0.5f), highlighted = false)
            drawPreviewCard(Offset(size.width * 0.5f, size.height * 0.5f), highlighted = true)
            drawPreviewCard(Offset(size.width * 0.72f, size.height * 0.5f), highlighted = false)
        } else {
            drawPreviewCard(Offset(size.width * 0.5f, size.height * 0.27f), highlighted = false)
            drawPreviewCard(Offset(size.width * 0.5f, size.height * 0.5f), highlighted = true)
            drawPreviewCard(Offset(size.width * 0.5f, size.height * 0.73f), highlighted = false)
        }
    }
}

@Composable
internal fun VerticalLocalGalleryPageCard(
    pageUriString: String,
    fallbackPageUriString: String = "",
    pageNumber: Int,
    obscure: Boolean,
    showPageLabel: Boolean,
    onReady: () -> Unit,
    onCenterTap: () -> Unit,
    onCenterLongPress: () -> Unit,
    rootViewportSize: IntSize,
    onStartModeChooser: (Float, Float) -> Unit,
    onUpdateModeChooser: (Float, Float) -> Unit,
    onFinishModeChooser: () -> Unit
) {
    val context = LocalContext.current
    var retryToken by remember { mutableStateOf(0) }
    val pageState by produceState<GalleryPageState>(
        initialValue = GalleryPageState.Loading,
        pageUriString,
        fallbackPageUriString,
        retryToken
    ) {
        value = GalleryPageState.Loading
        val bitmap = withContext(Dispatchers.IO) {
            fetchExplicitGalleryPageBitmap(context, pageUriString, fallbackPageUriString)
        }
        value = if (bitmap != null) GalleryPageState.Ready(bitmap) else GalleryPageState.Failed
    }
    LaunchedEffect(pageState) {
        if (pageState is GalleryPageState.Ready) onReady()
        if (pageState is GalleryPageState.Ready) MangaDexReaderWarmup.markPageReady(pageUriString)
    }
    VerticalSlideshowPageCard(
        pageState = pageState,
        pageNumber = pageNumber,
        obscure = obscure,
        showPageLabel = showPageLabel,
        onCenterTap = onCenterTap,
        onCenterLongPress = onCenterLongPress,
        rootViewportSize = rootViewportSize,
        onStartModeChooser = onStartModeChooser,
        onUpdateModeChooser = onUpdateModeChooser,
        onFinishModeChooser = onFinishModeChooser,
        retry = { retryToken += 1 },
        failedMessage = "Could not load page $pageNumber."
    )
}

@Composable
internal fun VerticalRemoteGalleryPageCard(
    mediaId: Long,
    pageNumber: Int,
    preferredExt: String,
    obscure: Boolean,
    showPageLabel: Boolean,
    onReady: () -> Unit,
    onCenterTap: () -> Unit,
    onCenterLongPress: () -> Unit,
    rootViewportSize: IntSize,
    onStartModeChooser: (Float, Float) -> Unit,
    onUpdateModeChooser: (Float, Float) -> Unit,
    onFinishModeChooser: () -> Unit
) {
    var retryToken by remember { mutableStateOf(0) }
    val pageState by produceState<GalleryPageState>(
        initialValue = GalleryPageState.Loading,
        mediaId,
        pageNumber,
        preferredExt,
        retryToken
    ) {
        value = GalleryPageState.Loading
        val bitmap = withContext(Dispatchers.IO) {
            fetchGalleryPageBitmap(
                mediaId = mediaId,
                pageNumber = pageNumber,
                preferredExt = preferredExt
            )
        }
        value = if (bitmap != null) GalleryPageState.Ready(bitmap) else GalleryPageState.Failed
    }
    LaunchedEffect(pageState) {
        if (pageState is GalleryPageState.Ready) onReady()
    }
    VerticalSlideshowPageCard(
        pageState = pageState,
        pageNumber = pageNumber,
        obscure = obscure,
        showPageLabel = showPageLabel,
        onCenterTap = onCenterTap,
        onCenterLongPress = onCenterLongPress,
        rootViewportSize = rootViewportSize,
        onStartModeChooser = onStartModeChooser,
        onUpdateModeChooser = onUpdateModeChooser,
        onFinishModeChooser = onFinishModeChooser,
        retry = { retryToken += 1 },
        failedMessage = "Could not load page $pageNumber."
    )
}

@Composable
internal fun VerticalSlideshowPageCard(
    pageState: GalleryPageState,
    pageNumber: Int,
    obscure: Boolean,
    showPageLabel: Boolean,
    onCenterTap: () -> Unit,
    onCenterLongPress: () -> Unit,
    rootViewportSize: IntSize,
    onStartModeChooser: (Float, Float) -> Unit,
    onUpdateModeChooser: (Float, Float) -> Unit,
    onFinishModeChooser: () -> Unit,
    retry: () -> Unit,
    failedMessage: String
) {
    var cardPositionInRoot by remember(pageNumber) { mutableStateOf(Offset.Zero) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                cardPositionInRoot = coordinates.positionInRoot()
            }
            .pointerInput(pageNumber, rootViewportSize) {
                if (rootViewportSize.width <= 0 || rootViewportSize.height <= 0) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val longPress = awaitLongPressOrCancellation(down.id)
                    if (longPress == null) return@awaitEachGesture
                    val start = cardPositionInRoot + longPress.position
                    onStartModeChooser(start.x, start.y)
                    var finished = false
                    while (!finished) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Final)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                        if (change == null) {
                            finished = true
                        } else if (!change.pressed || change.changedToUpIgnoreConsumed()) {
                            onFinishModeChooser()
                            finished = true
                        } else {
                            val position = cardPositionInRoot + change.position
                            onUpdateModeChooser(position.x, position.y)
                        }
                    }
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (val state = pageState) {
                GalleryPageState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 240.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.5.dp)
                    }
                }

                is GalleryPageState.Ready -> {
                    val aspect = (state.bitmap.width.toFloat() / state.bitmap.height.toFloat()).coerceIn(0.55f, 1.8f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspect)
                    ) {
                        Image(
                            bitmap = state.bitmap,
                            contentDescription = "Page $pageNumber",
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(20.dp))
                                .slideshowPrivacyObfuscate(
                                    enabled = obscure,
                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = SLIDESHOW_INCOGNITO_OVERLAY_ALPHA)
                                ),
                            contentScale = ContentScale.Fit
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(0.14f)
                                .fillMaxHeight(0.12f)
                                .clip(RoundedCornerShape(18.dp))
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onCenterTap,
                                    onLongClick = onCenterLongPress
                                )
                        )
                    }
                }

                GalleryPageState.Failed -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = failedMessage,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        TextButton(onClick = retry) {
                            Text("Retry")
                        }
                    }
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = showPageLabel,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = "Page $pageNumber",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
internal fun LocalGalleryPage(
    pageUriString: String,
    fallbackPageUriString: String = "",
    pageNumber: Int,
    obscure: Boolean,
    onLoadFailed: () -> Unit = {}
) {
    val context = LocalContext.current
    var retryToken by androidx.compose.runtime.remember { mutableStateOf(0) }
    val pageState by produceState<GalleryPageState>(
        initialValue = GalleryPageState.Loading,
        pageUriString,
        fallbackPageUriString,
        retryToken
    ) {
        value = GalleryPageState.Loading
        val bitmap = withContext(Dispatchers.IO) {
            fetchExplicitGalleryPageBitmap(context, pageUriString, fallbackPageUriString)
        }
        value = if (bitmap != null) {
            GalleryPageState.Ready(bitmap)
        } else {
            GalleryPageState.Failed
        }
    }
    var pageRevealTarget by remember(pageUriString) { mutableStateOf(0f) }
    LaunchedEffect(pageState) {
        pageRevealTarget = if (pageState is GalleryPageState.Ready) 1f else 0f
        if (pageState is GalleryPageState.Ready) MangaDexReaderWarmup.markPageReady(pageUriString)
        if (pageState is GalleryPageState.Failed) onLoadFailed()
    }
    val pageRevealAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = pageRevealTarget,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 190,
            easing = androidx.compose.animation.core.LinearOutSlowInEasing
        ),
        label = "slideshowLocalPageRevealAlpha"
    )
    val pageRevealScrimAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pageState is GalleryPageState.Ready) 0f else 1f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 220,
            easing = androidx.compose.animation.core.LinearOutSlowInEasing
        ),
        label = "slideshowLocalPageRevealScrim"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = pageState) {
            GalleryPageState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(strokeWidth = 2.5.dp)
                }
            }

            is GalleryPageState.Ready -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        bitmap = state.bitmap,
                        contentDescription = "Page $pageNumber",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = pageRevealAlpha }
                            .slideshowPrivacyObfuscate(
                                enabled = obscure,
                                overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = SLIDESHOW_INCOGNITO_OVERLAY_ALPHA)
                            ),
                        contentScale = ContentScale.Fit
                    )
                    if (pageRevealScrimAlpha > 0.001f) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(MaterialTheme.colorScheme.background.copy(alpha = pageRevealScrimAlpha))
                        )
                    }
                }
            }

            GalleryPageState.Failed -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Could not load page $pageNumber.",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = { retryToken += 1 }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

internal fun fetchExplicitGalleryPageBitmap(
    context: Context,
    uriString: String,
    fallbackUriString: String = ""
): ImageBitmap? {
    if (uriString.startsWith("https://", ignoreCase = true) || uriString.startsWith("http://", ignoreCase = true)) {
        listOf(uriString, fallbackUriString)
            .map(String::trim)
            .filter(String::isNotBlank)
            .flatMap { candidate ->
                val parsed = runCatching { Uri.parse(candidate) }.getOrNull()
                val centralFallback = parsed?.host.orEmpty().takeIf { it.endsWith(".mangadex.network", ignoreCase = true) }
                    ?.let { parsed!!.buildUpon().authority("uploads.mangadex.org").build().toString() }
                if (centralFallback != null) {
                    // Avoid the serial assigned-node 404 before every fallback. The stable
                    // uploads host is tried first and the MangaDex@Home node remains a fallback.
                    listOfNotNull(centralFallback, candidate)
                } else {
                    listOfNotNull(candidate, centralFallback)
                }
            }
            .distinct()
            .forEach { candidate ->
                GalleryPageBitmapCache.getBitmap(candidate)?.let { return it }
                GalleryPageBitmapCache.loadOnce(candidate) {
                    fetchGalleryPageBitmapOnce(candidate, readerPageHttpClient(context))
                }?.let { bitmap ->
                    return bitmap
                }
            }
        return null
    }
    return fetchLocalGalleryPageBitmap(context, uriString)
}

internal fun prefetchExplicitGalleryPages(
    context: Context,
    pageUris: List<String>,
    fallbackPageUris: List<String>,
    currentIndex: Int,
    includeCurrent: Boolean = false
) {
    if (pageUris.isEmpty()) return
    val indexes = explicitGalleryPrefetchIndexes(pageUris.size, currentIndex, includeCurrent)
    val currentUrl = pageUris.getOrNull(currentIndex).orEmpty()
    // At chapter open, give the selected page exclusive first priority. As soon as it is
    // decoded, fill a wider forward window concurrently. Subsequent page changes refill it
    // immediately, so protecting first paint does not reduce reading continuity.
    indexes
        .forEachIndexed { order, index ->
            val executor = if (includeCurrent && order == 0) {
                galleryVisiblePageExecutor
            } else {
                galleryReadAheadExecutor
            }
            executor.execute {
                val shouldWaitForVisiblePage = currentUrl.isNotBlank() && (
                    (includeCurrent && order > 0) || !includeCurrent
                )
                if (shouldWaitForVisiblePage) {
                    var attempts = 0
                    while (!isExplicitGalleryPageCached(currentUrl) && attempts < 100) {
                        Thread.sleep(50L)
                        attempts += 1
                    }
                }
                fetchExplicitGalleryPageBitmap(
                    context = context.applicationContext,
                    uriString = pageUris[index],
                    fallbackUriString = fallbackPageUris.getOrNull(index).orEmpty()
                )
            }
        }
}

private fun isExplicitGalleryPageCached(url: String): Boolean {
    if (GalleryPageBitmapCache.getBitmap(url) != null) return true
    val parsed = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    if (!parsed.host.orEmpty().endsWith(".mangadex.network", ignoreCase = true)) return false
    val centralUrl = parsed.buildUpon().authority("uploads.mangadex.org").build().toString()
    return GalleryPageBitmapCache.getBitmap(centralUrl) != null
}

internal fun explicitGalleryPrefetchIndexes(
    pageCount: Int,
    currentIndex: Int,
    includeCurrent: Boolean
): List<Int> = buildList {
    if (includeCurrent) add(currentIndex)
    add(currentIndex + 1)
    add(currentIndex + 2)
    add(currentIndex + 3)
    add(currentIndex + 4)
    add(currentIndex + 5)
    add(currentIndex - 1)
}.distinct().filter { it in 0 until pageCount.coerceAtLeast(0) }

@Composable
internal fun GalleryPage(
    mediaId: Long,
    pageNumber: Int,
    preferredExt: String,
    obscure: Boolean
) {
    var retryToken by androidx.compose.runtime.remember { mutableStateOf(0) }
    val pageState by produceState<GalleryPageState>(
        initialValue = GalleryPageState.Loading,
        mediaId,
        pageNumber,
        preferredExt,
        retryToken
    ) {
        value = GalleryPageState.Loading
        val bitmap = withContext(Dispatchers.IO) {
            fetchGalleryPageBitmap(
                mediaId = mediaId,
                pageNumber = pageNumber,
                preferredExt = preferredExt
            )
        }
        value = if (bitmap != null) {
            GalleryPageState.Ready(bitmap)
        } else {
            GalleryPageState.Failed
        }
    }
    var pageRevealTarget by remember(mediaId, pageNumber, preferredExt) { mutableStateOf(0f) }
    LaunchedEffect(pageState) {
        pageRevealTarget = if (pageState is GalleryPageState.Ready) 1f else 0f
    }
    val pageRevealAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = pageRevealTarget,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 190,
            easing = androidx.compose.animation.core.LinearOutSlowInEasing
        ),
        label = "slideshowPageRevealAlpha"
    )
    val pageRevealScrimAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pageState is GalleryPageState.Ready) 0f else 1f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 220,
            easing = androidx.compose.animation.core.LinearOutSlowInEasing
        ),
        label = "slideshowPageRevealScrim"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = pageState) {
            GalleryPageState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            }

            is GalleryPageState.Ready -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        bitmap = state.bitmap,
                        contentDescription = "Page $pageNumber",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = pageRevealAlpha
                            }
                            .slideshowPrivacyObfuscate(
                                enabled = obscure,
                                overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = SLIDESHOW_INCOGNITO_OVERLAY_ALPHA)
                            ),
                        contentScale = ContentScale.Fit
                    )
                    if (pageRevealScrimAlpha > 0.001f) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(MaterialTheme.colorScheme.background.copy(alpha = pageRevealScrimAlpha))
                        )
                    }
                }
            }

            GalleryPageState.Failed -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Could not load page $pageNumber.",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = { retryToken += 1 }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
