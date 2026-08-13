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


internal fun browserNormalizeDuplicateTitle(raw: String): String {
    return raw
        .trim()
        .lowercase(Locale.US)
        .replace("&", " and ")
        .replace(browserNonAlphanumericRegex, " ")
        .replace(browserWhitespaceRegex, " ")
        .trim()
}

private fun browserDuplicateTrigramsNormalized(normalized: String): Set<String> {
    if (normalized.isBlank()) return emptySet()
    if (normalized.length < 3) return setOf(normalized)
    val out = linkedSetOf<String>()
    for (index in 0..(normalized.length - 3)) {
        out += normalized.substring(index, index + 3)
    }
    return out
}

internal fun browserDuplicateTrigrams(value: String): Set<String> =
    browserDuplicateTrigramsNormalized(browserNormalizeDuplicateTitle(value))

internal fun browserDuplicateTitleSimilarity(left: String, right: String): Float {
    val a = browserNormalizeDuplicateTitle(left)
    val b = browserNormalizeDuplicateTitle(right)
    if (a.isBlank() || b.isBlank()) return 0f
    if (a == b) return 1f
    val leftSet = browserDuplicateTrigramsNormalized(a)
    val rightSet = browserDuplicateTrigramsNormalized(b)
    if (leftSet.isEmpty() || rightSet.isEmpty()) return 0f
    val overlap = leftSet.intersect(rightSet).size.toFloat()
    return ((2f * overlap) / (leftSet.size + rightSet.size).toFloat()).coerceIn(0f, 1f)
}

internal fun browserDuplicateJaccard(left: Set<String>, right: Set<String>): Float {
    if (left.isEmpty() || right.isEmpty()) return 0f
    val union = left.union(right)
    if (union.isEmpty()) return 0f
    val intersection = left.intersect(right)
    return (intersection.size.toFloat() / union.size.toFloat()).coerceIn(0f, 1f)
}

internal fun buildBrowserDuplicateSeedIndex(seeds: List<LocalDuplicateSeed>): BrowserDuplicateSeedIndex {
    if (seeds.isEmpty()) {
        return BrowserDuplicateSeedIndex(
            allSeeds = emptyList(),
            byCode = emptyMap(),
            byMediaId = emptyMap(),
            byPageCount = emptyMap(),
            byUploadDate = emptyMap(),
            byTitleKey = emptyMap(),
            byTitleTrigram = emptyMap()
        )
    }
    val dedupedSeeds = seeds
        .asSequence()
        .filter { it.code > 0 }
        .distinctBy { it.code }
        .toList()
    return BrowserDuplicateSeedIndex(
        allSeeds = dedupedSeeds,
        byCode = dedupedSeeds.associateBy { it.code },
        byMediaId = dedupedSeeds
            .asSequence()
            .filter { it.mediaId > 0L }
            .groupBy { it.mediaId },
        byPageCount = dedupedSeeds
            .asSequence()
            .filter { it.numPages > 0 }
            .groupBy { it.numPages },
        byUploadDate = dedupedSeeds
            .asSequence()
            .filter { it.uploadDate.isNotBlank() }
            .groupBy { it.uploadDate },
        byTitleKey = dedupedSeeds
            .asSequence()
            .filter { it.titleKey.isNotBlank() }
            .groupBy { it.titleKey },
        byTitleTrigram = buildMap<String, List<LocalDuplicateSeed>> {
            val mutableBuckets = linkedMapOf<String, MutableList<LocalDuplicateSeed>>()
            dedupedSeeds.forEach { seed ->
                browserDuplicateTrigramsNormalized(seed.titleKey).forEach trigramLoop@{ trigram ->
                    if (trigram.isBlank()) return@trigramLoop
                    mutableBuckets.getOrPut(trigram) { mutableListOf() }.add(seed)
                }
            }
            mutableBuckets.forEach { (trigram, bucket) -> put(trigram, bucket.toList()) }
        }
    )
}

internal fun browserCollectDuplicateCandidateSeeds(
    index: BrowserDuplicateSeedIndex,
    candidateCode: Int,
    candidateTitle: String,
    candidateNumPages: Int,
    candidateUploadDate: String,
    candidateMediaId: Long
): List<LocalDuplicateSeed> {
    if (index.allSeeds.isEmpty()) return emptyList()
    val out = linkedMapOf<Int, LocalDuplicateSeed>()

    fun addSeeds(seeds: Iterable<LocalDuplicateSeed>) {
        seeds.forEach { seed ->
            if (seed.code <= 0 || seed.code == candidateCode) return@forEach
            out.putIfAbsent(seed.code, seed)
        }
    }

    if (candidateMediaId > 0L) {
        addSeeds(index.byMediaId[candidateMediaId].orEmpty())
    }

    if (candidateNumPages > 0) {
        for (pages in (candidateNumPages - 2)..(candidateNumPages + 2)) {
            if (pages <= 0) continue
            addSeeds(index.byPageCount[pages].orEmpty())
        }
    }

    val uploadDateKey = candidateUploadDate.trim()
    if (uploadDateKey.isNotBlank()) {
        addSeeds(index.byUploadDate[uploadDateKey].orEmpty())
    }

    val titleKey = browserNormalizeDuplicateTitle(candidateTitle)
    if (titleKey.isNotBlank()) {
        addSeeds(index.byTitleKey[titleKey].orEmpty())
        val trigramScores = linkedMapOf<Int, Int>()
        browserDuplicateTrigrams(titleKey).forEach { trigram ->
            index.byTitleTrigram[trigram].orEmpty().forEach seedLoop@{ seed ->
                if (seed.code <= 0 || seed.code == candidateCode) return@seedLoop
                trigramScores[seed.code] = (trigramScores[seed.code] ?: 0) + 1
            }
        }
        trigramScores.entries
            .sortedWith(
                compareByDescending<Map.Entry<Int, Int>> { it.value }
                    .thenBy { it.key }
            )
            .forEach { entry ->
                index.byCode[entry.key]?.let { seed -> out.putIfAbsent(seed.code, seed) }
            }
    }

    return out.values.toList()
}

internal object BrowserDuplicateLocalHashIndex {
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

internal object BrowserDuplicateThumbnailHashCache {
    private const val MAX_ITEMS = 560
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
    fun put(key: String, value: Long?) {
        if (key.isBlank()) return
        map[key] = value
    }

    @Synchronized
    fun clear() {
        map.clear()
    }
}

internal fun browserBuildCoverCandidateUrls(rawUrl: String): List<String> {
    val trimmed = rawUrl.trim()
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

internal fun browserNormalizeDuplicateThumbnailCacheKey(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return ""
    val pattern = Regex("(?i)^(https?://[^/]+/galleries/\\d+/cover)\\.[a-z0-9]+(\\?.*)?$")
    val match = pattern.matchEntire(trimmed) ?: return trimmed
    val base = match.groupValues.getOrNull(1).orEmpty()
    val suffix = match.groupValues.getOrNull(2).orEmpty()
    return "$base$suffix"
}

internal fun browserFetchThumbnailBitmapRawOnce(url: String): Bitmap? {
    val request = Request.Builder()
        .url(url)
        .header(
            "User-Agent",
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
        )
        .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
        .header("Referer", "https://nhentai.net/")
        .build()
    return galleryBrowserImageClient.newCall(request).execute().use { rsp ->
        if (!rsp.isSuccessful) return null
        val bytes = rsp.body?.bytes() ?: return null
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }
}

internal fun browserResolveDuplicateThumbnailHash(
    appContext: Context?,
    thumbnailUrl: String,
    localBackupCode: Int? = null
): Long? {
    val cacheKey = browserNormalizeDuplicateThumbnailCacheKey(thumbnailUrl)
    if (cacheKey.isBlank()) return null
    return BrowserDuplicateThumbnailHashCache.getOrCompute(cacheKey) {
        if (appContext != null && localBackupCode != null && localBackupCode > 0) {
            BrowserDuplicateLocalHashIndex.get(localBackupCode)?.let { return@getOrCompute it }
            readBackupThumbnailHashForCode(appContext, localBackupCode)?.let { hash ->
                BrowserDuplicateLocalHashIndex.put(localBackupCode, hash)
                return@getOrCompute hash
            }
        }
        val candidates = browserBuildCoverCandidateUrls(thumbnailUrl)
        if (candidates.isEmpty()) return@getOrCompute null
        candidates.forEach { candidateUrl ->
            repeat(2) { attempt ->
                val hash = runCatching {
                    val bitmap = browserFetchThumbnailBitmapRawOnce(candidateUrl) ?: return@runCatching null
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

internal fun browserDuplicateThumbnailSimilarity(
    appContext: Context?,
    candidateThumbnailUrl: String,
    seedThumbnailUrl: String,
    candidateCode: Int? = null,
    seedCode: Int? = null
): Float {
    if (candidateThumbnailUrl.isBlank() || seedThumbnailUrl.isBlank()) return 0f
    val leftHash = browserResolveDuplicateThumbnailHash(
        appContext,
        candidateThumbnailUrl,
        localBackupCode = candidateCode
    ) ?: return 0f
    val rightHash = browserResolveDuplicateThumbnailHash(appContext, seedThumbnailUrl, localBackupCode = seedCode) ?: return 0f
    val distance = java.lang.Long.bitCount(leftHash xor rightHash)
    return ((64 - distance).toFloat() / 64f).coerceIn(0f, 1f)
}

internal fun browserDuplicateMetadataPrefilter(
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

internal fun browserDuplicateLooksLikeCreatorOnlyFalsePositive(
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

internal fun browserDuplicateCandidatePassesWithArtistMismatch(
    artistMismatchNoGroup: Boolean,
    thumbnailSimilarity: Float,
    tagSimilarity: Float,
    titleSimilarity: Float
): Boolean {
    if (!artistMismatchNoGroup) return true
    return thumbnailSimilarity >= 0.93f &&
        (tagSimilarity >= 0.58f || titleSimilarity >= 0.90f)
}

internal fun browserShouldFetchDetailForDuplicateHint(
    candidateCode: Int,
    candidateTitle: String,
    candidateNumPages: Int,
    candidateUploadDate: String,
    candidateMediaId: Long,
    candidateThumbnailUrl: String,
    candidateSeeds: List<LocalDuplicateSeed>
): Boolean {
    if (candidateSeeds.isEmpty()) return false
    val titleKey = browserNormalizeDuplicateTitle(candidateTitle)
    val candidateHasThumbnail = candidateThumbnailUrl.isNotBlank()
    candidateSeeds.forEach { seed ->
        if (seed.code <= 0 || seed.code == candidateCode) return@forEach
        val sameMedia = candidateMediaId > 0L && seed.mediaId > 0L && candidateMediaId == seed.mediaId
        val titleSimilarity = browserDuplicateTitleSimilarity(titleKey, seed.titleKey)
        val samePagesExact = candidateNumPages > 0 && seed.numPages > 0 && candidateNumPages == seed.numPages
        val pagesClose = candidateNumPages > 0 && seed.numPages > 0 &&
            abs(candidateNumPages - seed.numPages) <= 2
        val sameUploadDate = candidateUploadDate.isNotBlank() &&
            seed.uploadDate.isNotBlank() &&
            candidateUploadDate == seed.uploadDate
        if (sameMedia || titleSimilarity >= 0.82f) {
            return true
        }
        if (browserDuplicateMetadataPrefilter(
                titleSimilarity = titleSimilarity,
                tagSimilarity = 0f,
                artistOverlap = 0,
                groupOverlap = 0,
                samePagesExact = samePagesExact,
                pagesClose = pagesClose,
                sameUploadDate = sameUploadDate,
                sameMedia = sameMedia,
                candidateHasThumbnail = candidateHasThumbnail,
                seedHasThumbnail = seed.thumbnailUrl.isNotBlank()
            )
        ) {
            return true
        }
    }
    return false
}

internal fun browserFindLikelyDuplicateHint(
    appContext: Context?,
    candidateCode: Int,
    candidateTitle: String,
    candidateNumPages: Int,
    candidateUploadDate: String,
    candidateMediaId: Long,
    candidateArtistKeys: Set<String>,
    candidateGroupKeys: Set<String>,
    candidateTagKeys: Set<String>,
    candidateThumbnailUrl: String,
    candidateSeeds: List<LocalDuplicateSeed>
): DuplicateHint? {
    if (candidateSeeds.isEmpty()) return null
    val titleKey = browserNormalizeDuplicateTitle(candidateTitle)
    if (titleKey.isBlank() &&
        candidateArtistKeys.isEmpty() &&
        candidateGroupKeys.isEmpty() &&
        candidateTagKeys.isEmpty() &&
        candidateMediaId <= 0L &&
        candidateThumbnailUrl.isBlank()
    ) {
        return null
    }

    var bestHint: DuplicateHint? = null
    candidateSeeds.forEach { seed ->
        if (seed.code <= 0 || seed.code == candidateCode) return@forEach

        val sameMedia = candidateMediaId > 0L && seed.mediaId > 0L && candidateMediaId == seed.mediaId
        val titleSimilarity = browserDuplicateTitleSimilarity(titleKey, seed.titleKey)
        val artistOverlap = if (candidateArtistKeys.isEmpty() || seed.artistKeys.isEmpty()) {
            0
        } else {
            candidateArtistKeys.intersect(seed.artistKeys).size
        }
        val groupOverlap = if (candidateGroupKeys.isEmpty() || seed.groupKeys.isEmpty()) {
            0
        } else {
            candidateGroupKeys.intersect(seed.groupKeys).size
        }
        val tagSimilarity = browserDuplicateJaccard(candidateTagKeys, seed.tagKeys)
        val samePagesExact = candidateNumPages > 0 && seed.numPages > 0 && candidateNumPages == seed.numPages
        val pagesClose = candidateNumPages > 0 && seed.numPages > 0 &&
            abs(candidateNumPages - seed.numPages) <= 2
        val sameUploadDate = candidateUploadDate.isNotBlank() &&
            seed.uploadDate.isNotBlank() &&
            candidateUploadDate == seed.uploadDate
        val artistMismatchNoGroup =
            candidateArtistKeys.isNotEmpty() &&
                seed.artistKeys.isNotEmpty() &&
                artistOverlap == 0 &&
                groupOverlap == 0
        val shouldCompareThumbnail = browserDuplicateMetadataPrefilter(
            titleSimilarity = titleSimilarity,
            tagSimilarity = tagSimilarity,
            artistOverlap = artistOverlap,
            groupOverlap = groupOverlap,
            samePagesExact = samePagesExact,
            pagesClose = pagesClose,
            sameUploadDate = sameUploadDate,
            sameMedia = sameMedia,
            candidateHasThumbnail = candidateThumbnailUrl.isNotBlank(),
            seedHasThumbnail = seed.thumbnailUrl.isNotBlank()
        )
        val thumbnailSimilarity = if (shouldCompareThumbnail) {
            browserDuplicateThumbnailSimilarity(
                appContext = appContext,
                candidateThumbnailUrl = candidateThumbnailUrl,
                seedThumbnailUrl = seed.thumbnailUrl,
                candidateCode = candidateCode,
                seedCode = seed.code
            )
        } else {
            0f
        }

        var score = 0f
        if (sameMedia) score += 0.22f
        score += when {
            thumbnailSimilarity >= 0.95f -> 0.88f
            thumbnailSimilarity >= 0.90f -> 0.74f
            thumbnailSimilarity >= 0.84f -> 0.52f
            thumbnailSimilarity >= 0.76f -> 0.30f
            else -> 0f
        }
        score += when {
            titleSimilarity >= 0.94f -> 0.20f
            titleSimilarity >= 0.86f -> 0.14f
            titleSimilarity >= 0.76f -> 0.08f
            else -> 0f
        }
        score += when {
            artistOverlap >= 2 -> 0.08f
            artistOverlap == 1 -> 0.05f
            else -> 0f
        }
        score += when {
            groupOverlap >= 2 -> 0.16f
            groupOverlap == 1 -> 0.11f
            else -> 0f
        }
        score += when {
            tagSimilarity >= 0.72f -> 0.16f
            tagSimilarity >= 0.52f -> 0.11f
            tagSimilarity >= 0.36f -> 0.06f
            else -> 0f
        }
        if (samePagesExact) {
            score += 0.08f
        } else if (pagesClose) {
            score += 0.04f
        }
        if (sameUploadDate) score += 0.05f
        if (artistMismatchNoGroup) score -= 0.42f

        val corroborationCount = listOf(
            sameMedia,
            titleSimilarity >= 0.56f,
            tagSimilarity >= 0.30f,
            samePagesExact,
            pagesClose,
            sameUploadDate,
            artistOverlap > 0,
            groupOverlap > 0
        ).count { it }
        val creatorOnlyFalsePositive = browserDuplicateLooksLikeCreatorOnlyFalsePositive(
            artistOverlap = artistOverlap,
            groupOverlap = groupOverlap,
            titleSimilarity = titleSimilarity,
            tagSimilarity = tagSimilarity,
            thumbnailSimilarity = thumbnailSimilarity
        )
        val passesArtistMismatch = browserDuplicateCandidatePassesWithArtistMismatch(
            artistMismatchNoGroup = artistMismatchNoGroup,
            thumbnailSimilarity = thumbnailSimilarity,
            tagSimilarity = tagSimilarity,
            titleSimilarity = titleSimilarity
        )

        val strongThumbnail = thumbnailSimilarity >= 0.93f
        val mediumThumbnail = thumbnailSimilarity >= 0.86f
        val weakThumbnail = thumbnailSimilarity >= 0.76f
        val strictMetadataFallback =
            titleSimilarity >= 0.95f &&
                tagSimilarity >= 0.72f &&
                samePagesExact &&
                (artistOverlap > 0 || groupOverlap > 0 || sameUploadDate) &&
                !artistMismatchNoGroup

        val likelyDuplicate = when {
            strongThumbnail -> passesArtistMismatch && !creatorOnlyFalsePositive && corroborationCount >= 1
            mediumThumbnail -> passesArtistMismatch && !creatorOnlyFalsePositive && corroborationCount >= 2
            weakThumbnail -> passesArtistMismatch &&
                !creatorOnlyFalsePositive &&
                corroborationCount >= 3 &&
                (titleSimilarity >= 0.56f || tagSimilarity >= 0.32f || samePagesExact || sameMedia)
            sameMedia -> strictMetadataFallback
            else -> strictMetadataFallback && score >= 0.70f
        }
        if (!likelyDuplicate || score < 0.48f) return@forEach

        val reasons = buildList {
            if (thumbnailSimilarity >= 0.90f) add("thumbnail match")
            else if (thumbnailSimilarity >= 0.84f) add("thumbnail similar")
            if (sameMedia) add("same media id")
            if (artistOverlap > 0) add("artist overlap")
            if (groupOverlap > 0) add("group overlap")
            if (titleSimilarity >= 0.82f) add("title match")
            if (tagSimilarity >= 0.52f) add("tag overlap")
            if (samePagesExact) add("same pages")
            if (sameUploadDate) add("same upload date")
        }
        val hint = DuplicateHint(
            matchedCode = seed.code,
            score = score.coerceIn(0f, 1.5f),
            reason = reasons.take(2).joinToString(", ").ifBlank { "metadata similarity" }
        )
        if (bestHint == null || hint.score > bestHint!!.score) {
            bestHint = hint
        }
    }
    return bestHint
}
