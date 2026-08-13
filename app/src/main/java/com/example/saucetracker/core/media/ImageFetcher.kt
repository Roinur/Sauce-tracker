package com.example.saucetracker.core.media

import com.example.saucetracker.*
import com.example.saucetracker.data.backup.*
import com.example.saucetracker.data.backup.*
import com.example.saucetracker.data.downloads.*
import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
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
import com.example.saucetracker.feature.heatmap.HeatmapCanvas
import com.example.saucetracker.feature.heatmap.HeatmapEngine
import com.example.saucetracker.feature.heatmap.HeatmapScreen
import com.example.saucetracker.feature.heatmap.HeatmapLayoutCache
import com.example.saucetracker.feature.heatmap.HeatmapThumbnailLoader
import com.example.saucetracker.feature.experimentalgallery.ExperimentalGalleryActivity
import com.example.saucetracker.feature.slideshow.SlideshowHorizontalDirection
import com.example.saucetracker.feature.slideshow.loadSlideshowHorizontalDirection
import com.example.saucetracker.feature.slideshow.storeSlideshowHorizontalDirection
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
private val thumbnailHttpClient: OkHttpClient by lazy {
    HttpClientFactory.create(HttpClientProfile.THUMBNAIL)
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

internal fun fetchThumbnailBitmapRawOnce(url: String, lowRes: Boolean = false): Bitmap? {
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
            inSampleSize = if (lowRes) 2 else 1
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }
}

internal fun fetchThumbnailBitmapRaw(url: String): Bitmap? {
    return fetchThumbnailBitmapRaw(url, lowRes = false)
}

internal fun fetchThumbnailBitmapRaw(url: String, lowRes: Boolean): Bitmap? {
    if (url.isBlank()) return null
    val candidates = buildThumbnailCandidateUrls(url)
    if (candidates.isEmpty()) return null
    candidates.forEach { candidateUrl ->
        repeat(2) { attempt ->
            val fetched = runCatching { fetchThumbnailBitmapRawOnce(candidateUrl, lowRes = lowRes) }.getOrNull()
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

internal fun fetchThumbnailBitmapOnce(url: String, lowRes: Boolean = false): ImageBitmap? {
    val bitmap = fetchThumbnailBitmapRawOnce(url, lowRes = lowRes) ?: return null
    return bitmap.asImageBitmap()
}

internal object DuplicateThumbnailHashCache {
    private const val MAX_ITEMS = 720
    private val map = object : LinkedHashMap<String, Long?>(MAX_ITEMS, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long?>?): Boolean {
            return size > MAX_ITEMS
        }
    }

    @Synchronized
    fun getOrCompute(key: String, compute: () -> Long?): Long? {
        if (key.isBlank()) return null
        if (map.containsKey(key)) return map[key]
        val value = compute()
        map[key] = value
        return value
    }

    @Synchronized
    fun clear() {
        map.clear()
    }
}

internal object DuplicateLocalHashIndex {
    private var hashesByCode: Map<Int, Long> = emptyMap()

    @Synchronized
    fun replaceAll(values: Map<Int, Long>) {
        hashesByCode = values.toMap()
    }

    @Synchronized
    fun get(code: Int): Long? = hashesByCode[code]

    @Synchronized
    fun put(code: Int, hash: Long) {
        if (code <= 0) return
        hashesByCode = hashesByCode.toMutableMap().apply { put(code, hash) }
    }

    @Synchronized
    fun isEmpty(): Boolean = hashesByCode.isEmpty()
}

private fun normalizeDuplicateThumbnailCacheKey(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return ""
    val pattern = Regex("(?i)^(https?://[^/]+/galleries/\\d+/cover)\\.[a-z0-9]+(\\?.*)?$")
    val match = pattern.matchEntire(trimmed) ?: return trimmed
    val base = match.groupValues.getOrNull(1).orEmpty()
    val suffix = match.groupValues.getOrNull(2).orEmpty()
    return "$base$suffix"
}

private fun resolveDuplicateThumbnailHash(
    thumbnailUrl: String,
    localBackupCode: Int? = null
): Long? {
    val cacheKey = normalizeDuplicateThumbnailCacheKey(thumbnailUrl)
    if (cacheKey.isBlank()) return null
    return DuplicateThumbnailHashCache.getOrCompute(cacheKey) {
        if (localBackupCode != null && localBackupCode > 0) {
            DuplicateLocalHashIndex.get(localBackupCode)?.let { return@getOrCompute it }
        }
        val candidates = buildThumbnailCandidateUrls(thumbnailUrl)
        if (candidates.isEmpty()) return@getOrCompute null
        candidates.forEach { candidateUrl ->
            repeat(2) { attempt ->
                val hash = runCatching {
                    val bitmap = fetchThumbnailBitmapRawOnce(candidateUrl) ?: return@runCatching null
                    try {
                        computeDHash64(bitmap)
                    } finally {
                        if (!bitmap.isRecycled) {
                            bitmap.recycle()
                        }
                    }
                }.getOrNull()
                if (hash != null) {
                    return@getOrCompute hash
                }
                if (attempt == 0) {
                    Thread.sleep(65)
                }
            }
        }
        null
    }
}

internal fun duplicateThumbnailSimilarity(
    candidateThumbnailUrl: String,
    seedThumbnailUrl: String,
    candidateCode: Int? = null,
    seedCode: Int? = null
): Float {
    if (candidateThumbnailUrl.isBlank() || seedThumbnailUrl.isBlank()) return 0f
    val leftHash = resolveDuplicateThumbnailHash(candidateThumbnailUrl, localBackupCode = candidateCode) ?: return 0f
    val rightHash = resolveDuplicateThumbnailHash(seedThumbnailUrl, localBackupCode = seedCode) ?: return 0f
    val distance = java.lang.Long.bitCount(leftHash xor rightHash)
    return ((64 - distance).toFloat() / 64f).coerceIn(0f, 1f)
}

internal fun duplicateMetadataPrefilter(
    titleSimilarity: Float,
    tagSimilarity: Float,
    artistOverlap: Int,
    groupOverlap: Int,
    samePagesExact: Boolean,
    pagesClose: Boolean,
    sameUploadDate: Boolean,
    sameMedia: Boolean,
    candidateHasThumbnail: Boolean,
    seedHasThumbnail: Boolean
): Boolean {
    if (candidateHasThumbnail && seedHasThumbnail) {
        if (samePagesExact || pagesClose || sameUploadDate) return true
        if (artistOverlap > 0 || groupOverlap > 0) return true
        if (titleSimilarity >= 0.28f || tagSimilarity >= 0.16f) return true
    }
    if (sameMedia) return true
    if (artistOverlap > 0 || groupOverlap > 0) return true
    if (titleSimilarity >= 0.58f) return true
    if (tagSimilarity >= 0.30f) return true
    if (samePagesExact && (titleSimilarity >= 0.46f || tagSimilarity >= 0.22f || sameUploadDate)) return true
    if (pagesClose && (titleSimilarity >= 0.54f || tagSimilarity >= 0.26f || groupOverlap > 0)) return true
    if (sameUploadDate && (titleSimilarity >= 0.50f || tagSimilarity >= 0.24f)) return true
    return false
}

internal fun duplicateLooksLikeCreatorOnlyFalsePositive(
    artistOverlap: Int,
    groupOverlap: Int,
    titleSimilarity: Float,
    tagSimilarity: Float,
    thumbnailSimilarity: Float
): Boolean {
    return artistOverlap > 0 &&
        groupOverlap == 0 &&
        titleSimilarity < 0.64f &&
        tagSimilarity < 0.34f &&
        thumbnailSimilarity < 0.84f
}

internal fun duplicateCandidatePassesWithArtistMismatch(
    artistMismatchNoGroup: Boolean,
    thumbnailSimilarity: Float,
    tagSimilarity: Float,
    titleSimilarity: Float
): Boolean {
    if (!artistMismatchNoGroup) return true
    return thumbnailSimilarity >= 0.93f &&
        (tagSimilarity >= 0.58f || titleSimilarity >= 0.90f)
}

internal fun buildGalleryCoverThumbnailUrl(gallery: GalleryData): String {
    val thumbExt = gallery.coverExt.trim().ifBlank { "jpg" }
    return if (gallery.mediaId > 0L) {
        "https://t.nhentai.net/galleries/${gallery.mediaId}/cover.$thumbExt"
    } else {
        ""
    }
}

internal fun fetchThumbnailBitmap(
    context: Context,
    url: String,
    backupCode: Int? = null,
    lowRes: Boolean = false
): ImageBitmap? {
    if (url.isBlank()) return null
    if (backupCode != null && backupCode > 0) {
        readBackupThumbnailBitmapForCode(context, backupCode)?.let { bitmap ->
            return if (lowRes) {
                scaleBitmapForGraph(bitmap, maxDimensionPx = 192)
            } else {
                bitmap.asImageBitmap()
            }
        }
    }
    val candidates = buildThumbnailCandidateUrls(url)
    if (candidates.isEmpty()) return null

    candidates.forEach { candidateUrl ->
        repeat(2) { attempt ->
            val fetched = runCatching { fetchThumbnailBitmapOnce(candidateUrl, lowRes = lowRes) }.getOrNull()
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

private fun scaleBitmapForGraph(bitmap: Bitmap, maxDimensionPx: Int = 72): ImageBitmap {
    val safeMax = maxDimensionPx.coerceAtLeast(48)
    val sourceWidth = bitmap.width.coerceAtLeast(1)
    val sourceHeight = bitmap.height.coerceAtLeast(1)
    val longestSide = max(sourceWidth, sourceHeight)
    if (longestSide <= safeMax) {
        return bitmap.asImageBitmap()
    }
    val scale = safeMax.toFloat() / longestSide.toFloat()
    val targetWidth = (sourceWidth * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (sourceHeight * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true).asImageBitmap()
}

internal fun fetchGraphThumbnailBitmap(context: Context, url: String, backupCode: Int? = null): ImageBitmap? {
    if (url.isBlank()) return null
    if (backupCode != null && backupCode > 0) {
        readBackupThumbnailBitmapForCode(context, backupCode)?.let { bitmap ->
            return scaleBitmapForGraph(bitmap).also { scaled ->
                ThumbnailBitmapCache.put(url, scaled, lowRes = true)
            }
        }
    }
    val cached = ThumbnailBitmapCache.get(url, lowRes = true) ?: ThumbnailBitmapCache.get(url)
    if (cached != null) {
        val androidBitmap = cached.asAndroidBitmap()
        return scaleBitmapForGraph(androidBitmap).also { scaled ->
            ThumbnailBitmapCache.put(url, scaled, lowRes = true)
        }
    }
    val raw = fetchThumbnailBitmapRaw(url) ?: return null
    return scaleBitmapForGraph(raw).also { scaled ->
        ThumbnailBitmapCache.put(url, scaled, lowRes = true)
    }
}
