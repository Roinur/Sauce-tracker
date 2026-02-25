@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.saucetracker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

private const val SLIDESHOW_PREFS_NAME = "nhtagbook_prefs"
private const val SLIDESHOW_KEY_THEME_MODE = "theme_mode"
private const val SLIDESHOW_KEY_ACCENT_MODE = "accent_mode"

class GallerySlideshowActivity : ComponentActivity() {
    companion object {
        private const val EXTRA_CODE = "extra_code"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_MEDIA_ID = "extra_media_id"
        private const val EXTRA_COVER_EXT = "extra_cover_ext"
        private const val EXTRA_NUM_PAGES = "extra_num_pages"
        private const val EXTRA_START_PAGE = "extra_start_page"

        fun createIntent(
            context: Context,
            code: Int,
            title: String,
            mediaId: Long,
            coverExt: String,
            numPages: Int,
            startPage: Int = 1
        ): Intent {
            return Intent(context, GallerySlideshowActivity::class.java)
                .putExtra(EXTRA_CODE, code)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_MEDIA_ID, mediaId)
                .putExtra(EXTRA_COVER_EXT, coverExt)
                .putExtra(EXTRA_NUM_PAGES, numPages)
                .putExtra(EXTRA_START_PAGE, startPage)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val code = intent?.getIntExtra(EXTRA_CODE, 0) ?: 0
        val title = intent?.getStringExtra(EXTRA_TITLE)?.trim().orEmpty()
        val mediaId = intent?.getLongExtra(EXTRA_MEDIA_ID, 0L) ?: 0L
        val coverExt = intent?.getStringExtra(EXTRA_COVER_EXT)?.trim().orEmpty()
        val numPages = intent?.getIntExtra(EXTRA_NUM_PAGES, 0) ?: 0
        val startPage = intent?.getIntExtra(EXTRA_START_PAGE, 1) ?: 1
        val themeMode = loadThemeMode()
        val accentMode = loadAccentMode()

        setContent {
            SlideshowTheme(
                themeMode = themeMode,
                accentMode = accentMode
            ) {
                SlideshowApplySystemBars()
                GallerySlideshowScreen(
                    code = code,
                    title = title,
                    mediaId = mediaId,
                    coverExt = coverExt,
                    numPages = numPages,
                    startPage = startPage,
                    onDone = ::finish
                )
            }
        }
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
}

@Composable
private fun SlideshowTheme(
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
        applySlideshowAccentMode(
            baseScheme = if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context),
            accentMode = accentMode,
            isDark = useDark
        )
    } else {
        applySlideshowAccentMode(
            baseScheme = fallbackScheme,
            accentMode = accentMode,
            isDark = useDark
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        content()
    }
}

private fun slideshowAccentColorForMode(mode: AccentMode): Color? {
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

private fun slideshowPreferredOnAccent(color: Color): Color {
    val lum = (0.299f * color.red) + (0.587f * color.green) + (0.114f * color.blue)
    return if (lum >= 0.62f) Color(0xFF111111) else Color.White
}

private fun applySlideshowAccentMode(
    baseScheme: ColorScheme,
    accentMode: AccentMode,
    isDark: Boolean
): ColorScheme {
    val accent = slideshowAccentColorForMode(accentMode) ?: return baseScheme
    val onAccent = slideshowPreferredOnAccent(accent)
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

@Composable
private fun SlideshowApplySystemBars() {
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

private sealed interface GalleryPageState {
    data object Loading : GalleryPageState
    data class Ready(val bitmap: ImageBitmap) : GalleryPageState
    data object Failed : GalleryPageState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GallerySlideshowScreen(
    code: Int,
    title: String,
    mediaId: Long,
    coverExt: String,
    numPages: Int,
    startPage: Int,
    onDone: () -> Unit
) {
    val safePages = numPages.coerceAtLeast(0)
    val hasData = mediaId > 0L && safePages > 0
    val pageCount = safePages.coerceAtLeast(1)
    val initialPage = (startPage - 1).coerceIn(0, pageCount - 1)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    var pageNavJob by remember { mutableStateOf<Job?>(null) }
    fun jumpToRelativePage(delta: Int) {
        if (!hasData || safePages <= 0 || delta == 0) return
        val current = pagerState.currentPage
        val target = (current + delta).coerceIn(0, safePages - 1)
        if (target == current) return

        pageNavJob?.cancel()
        pageNavJob = scope.launch {
            runCatching {
                pagerState.animateScrollToPage(target)
            }
        }
    }

    BackHandler(enabled = true) {
        onDone()
    }

    LaunchedEffect(hasData, mediaId, safePages, coverExt, pagerState.currentPage) {
        if (!hasData) return@LaunchedEffect
        val currentPage = pagerState.currentPage + 1
        withContext(Dispatchers.IO) {
            listOf(currentPage + 1, currentPage + 2)
                .filter { it in 1..safePages }
                .forEach { page ->
                    fetchGalleryPageBitmap(
                        mediaId = mediaId,
                        pageNumber = page,
                        preferredExt = coverExt
                    )
                }
        }
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
                    Text(
                        text = title.ifBlank { "Gallery $code" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    TextButton(
                        onClick = onDone,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.SemiBold)
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (!hasData) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Image data is not available for code $code.",
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TextButton(onClick = onDone) {
                            Text("Done")
                        }
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(hasData, safePages) {
                                val tapSlopPx = 24.dp.toPx()
                                val exitSwipeThresholdPx = 72f
                                awaitEachGesture {
                                    val down = awaitFirstDown(
                                        pass = PointerEventPass.Final,
                                        requireUnconsumed = false
                                    )
                                    if (!hasData || safePages <= 0) return@awaitEachGesture

                                    val lastPage = safePages - 1
                                    val startedPage = pagerState.currentPage

                                    var movedPastTapSlop = false
                                    var totalDx = 0f
                                    var totalDy = 0f
                                    while (true) {
                                        val event = awaitPointerEvent(pass = PointerEventPass.Final)
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        val delta = change.positionChange()
                                        totalDx += delta.x
                                        totalDy += delta.y
                                        if (!movedPastTapSlop &&
                                            (abs(totalDx) > tapSlopPx || abs(totalDy) > tapSlopPx)
                                        ) {
                                            movedPastTapSlop = true
                                        }

                                        if (
                                            startedPage == lastPage &&
                                            totalDx < -exitSwipeThresholdPx
                                        ) {
                                            onDone()
                                            break
                                        }

                                        if (change.changedToUpIgnoreConsumed() || !change.pressed) {
                                            if (!movedPastTapSlop && safePages > 1) {
                                                if (down.position.x < size.width / 2f) {
                                                    jumpToRelativePage(-1)
                                                } else {
                                                    if (pagerState.currentPage >= safePages - 1) {
                                                        onDone()
                                                    } else {
                                                        jumpToRelativePage(1)
                                                    }
                                                }
                                            }
                                            break
                                        }
                                    }
                                }
                            }
                    ) { index ->
                        GalleryPage(
                            mediaId = mediaId,
                            pageNumber = index + 1,
                            preferredExt = coverExt
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    if (pagerState.currentPage > 0) {
                                        jumpToRelativePage(-1)
                                    }
                                },
                                enabled = pagerState.currentPage > 0
                            ) {
                                Text("Prev", fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${pagerState.currentPage + 1} / $safePages",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            TextButton(
                                onClick = {
                                    if (pagerState.currentPage < safePages - 1) {
                                        jumpToRelativePage(1)
                                    } else {
                                        onDone()
                                    }
                                },
                                enabled = hasData
                            ) {
                                Text(
                                    if (pagerState.currentPage >= safePages - 1) "Exit" else "Next",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryPage(
    mediaId: Long,
    pageNumber: Int,
    preferredExt: String
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

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = pageState) {
            GalleryPageState.Loading -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            is GalleryPageState.Ready -> {
                Image(
                    bitmap = state.bitmap,
                    contentDescription = "Page $pageNumber",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
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

private object GalleryPageBitmapCache {
    private val maxItems = run {
        val maxMemMb = (Runtime.getRuntime().maxMemory() / (1024 * 1024)).toInt()
        when {
            maxMemMb >= 768 -> 240
            maxMemMb >= 512 -> 180
            maxMemMb >= 384 -> 140
            else -> 100
        }
    }

    private val bitmaps = object : LinkedHashMap<String, ImageBitmap>(maxItems, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean {
            return size > maxItems
        }
    }

    private val resolvedExtensions = object : LinkedHashMap<String, String>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
            return size > 512
        }
    }

    @Synchronized
    fun getBitmap(url: String): ImageBitmap? = bitmaps[url]

    @Synchronized
    fun putBitmap(url: String, bitmap: ImageBitmap) {
        if (url.isBlank()) return
        bitmaps[url] = bitmap
    }

    @Synchronized
    fun getResolvedExtension(pageKey: String): String? = resolvedExtensions[pageKey]

    @Synchronized
    fun putResolvedExtension(pageKey: String, extension: String) {
        if (pageKey.isBlank() || extension.isBlank()) return
        resolvedExtensions[pageKey] = extension
    }
}

private val slideshowHttpClient: OkHttpClient by lazy {
    val requestDispatcher = okhttp3.Dispatcher().apply {
        maxRequests = 64
        maxRequestsPerHost = 12
    }
    OkHttpClient.Builder()
        .dispatcher(requestDispatcher)
        .connectionPool(ConnectionPool(12, 5, TimeUnit.MINUTES))
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(16, TimeUnit.SECONDS)
        .build()
}

private fun normalizeImageExtension(raw: String?): String {
    return when (raw?.trim()?.lowercase(Locale.US).orEmpty()) {
        "j", "jpg", "jpeg" -> "jpg"
        "p", "png" -> "png"
        "w", "webp" -> "webp"
        "g", "gif" -> "gif"
        else -> ""
    }
}

private fun buildGalleryImageUrl(mediaId: Long, pageNumber: Int, extension: String): String {
    return "https://i.nhentai.net/galleries/$mediaId/$pageNumber.$extension"
}

private fun buildGalleryPageExtensions(
    preferredExt: String,
    resolvedExt: String?
): List<String> {
    val preferred = normalizeImageExtension(preferredExt)
    val resolved = normalizeImageExtension(resolvedExt)
    return buildList {
        if (resolved.isNotBlank()) add(resolved)
        if (preferred.isNotBlank()) add(preferred)
        add("jpg")
        add("png")
        add("webp")
        add("gif")
    }.distinct()
}

private fun fetchGalleryPageBitmap(
    mediaId: Long,
    pageNumber: Int,
    preferredExt: String
): ImageBitmap? {
    if (mediaId <= 0L || pageNumber <= 0) return null
    val pageKey = "$mediaId:$pageNumber"
    val resolved = GalleryPageBitmapCache.getResolvedExtension(pageKey)
    val extCandidates = buildGalleryPageExtensions(
        preferredExt = preferredExt,
        resolvedExt = resolved
    )

    extCandidates.forEach { ext ->
        val candidateUrl = buildGalleryImageUrl(mediaId, pageNumber, ext)
        val cached = GalleryPageBitmapCache.getBitmap(candidateUrl)
        if (cached != null) {
            GalleryPageBitmapCache.putResolvedExtension(pageKey, ext)
            return cached
        }

        repeat(2) { attempt ->
            val fetched = runCatching {
                fetchGalleryPageBitmapOnce(candidateUrl)
            }.getOrNull()
            if (fetched != null) {
                GalleryPageBitmapCache.putBitmap(candidateUrl, fetched)
                GalleryPageBitmapCache.putResolvedExtension(pageKey, ext)
                return fetched
            }
            if (attempt == 0) {
                Thread.sleep(60L)
            }
        }
    }

    return null
}

private fun fetchGalleryPageBitmapOnce(url: String): ImageBitmap? {
    val request = Request.Builder()
        .url(url)
        .header(
            "User-Agent",
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
        )
        .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
        .header("Referer", "https://nhentai.net/")
        .build()

    return slideshowHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        val bytes = response.body?.bytes() ?: return null
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        bitmap.asImageBitmap()
    }
}
