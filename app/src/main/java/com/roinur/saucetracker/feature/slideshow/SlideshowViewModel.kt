package com.roinur.saucetracker.feature.slideshow

import com.roinur.saucetracker.*
import com.roinur.saucetracker.core.media.BitmapMemoryCache
import com.roinur.saucetracker.core.media.applySourceImageHeaders
import com.roinur.saucetracker.data.source.MangaDexSourceAdapter
import com.roinur.saucetracker.data.source.SourceEntryKey
import com.roinur.saucetracker.data.source.SourceId
import com.roinur.saucetracker.data.source.SourceReaderContent
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.roinur.saucetracker.core.network.HttpClientFactory
import com.roinur.saucetracker.core.network.HttpClientProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Cache
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.sqrt
internal class SlideshowViewModel : androidx.lifecycle.ViewModel() {
    fun loadRemotePage(mediaId: Long, pageNumber: Int, preferredExt: String): ImageBitmap? {
        return fetchGalleryPageBitmap(mediaId, pageNumber, preferredExt)
    }

    override fun onCleared() {
        GalleryPageBitmapCache.clear()
    }
}
internal sealed interface GalleryPageState {
    data object Loading : GalleryPageState
    data class Ready(val bitmap: ImageBitmap) : GalleryPageState
    data object Failed : GalleryPageState
}

internal object GalleryPageBitmapCache {
    // MangaDex pages vary drastically in pixel size. An item-count LRU could retain a
    // hundred full pages on a 256 MB device and eventually crash the process. Bound the
    // cache by decoded RGB_565 bytes, matching the actual resource being retained.
    private val bitmapBudgetBytes = (Runtime.getRuntime().maxMemory() / 8L)
        .coerceIn(24L * 1024L * 1024L, 48L * 1024L * 1024L)
    private val bitmaps = BitmapMemoryCache<String, ImageBitmap>(
        maximumBytes = bitmapBudgetBytes,
        sizeOf = { bitmap -> bitmap.width.toLong() * bitmap.height.toLong() * 2L }
    )
    private val inFlight = ConcurrentHashMap<String, CompletableFuture<ImageBitmap?>>()

    private val resolvedExtensions = object : LinkedHashMap<String, String>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
            return size > 512
        }
    }

    fun getBitmap(url: String): ImageBitmap? = bitmaps[url]

    fun putBitmap(url: String, bitmap: ImageBitmap) {
        if (url.isBlank()) return
        bitmaps.put(url, bitmap)
    }

    /** Coalesces Compose, read-ahead and retry requests for the same immutable page URL. */
    fun loadOnce(url: String, loader: () -> ImageBitmap?): ImageBitmap? {
        getBitmap(url)?.let { return it }
        val mine = CompletableFuture<ImageBitmap?>()
        val existing = inFlight.putIfAbsent(url, mine)
        if (existing != null) return runCatching { existing.get() }.getOrNull()
        return try {
            val loaded = loader()
            if (loaded != null) putBitmap(url, loaded)
            mine.complete(loaded)
            loaded
        } catch (error: Throwable) {
            mine.completeExceptionally(error)
            throw error
        } finally {
            inFlight.remove(url, mine)
        }
    }

    @Synchronized
    fun getResolvedExtension(pageKey: String): String? = resolvedExtensions[pageKey]

    @Synchronized
    fun putResolvedExtension(pageKey: String, extension: String) {
        if (pageKey.isBlank() || extension.isBlank()) return
        resolvedExtensions[pageKey] = extension
    }

    @Synchronized
    fun clear() {
        bitmaps.clear()
        resolvedExtensions.clear()
    }
}

internal val galleryReadAheadExecutor = Executors.newFixedThreadPool(5) { runnable ->
    Thread(runnable, "sauce-reader-ahead").apply { isDaemon = true }
}

internal val galleryVisiblePageExecutor = Executors.newSingleThreadExecutor { runnable ->
    Thread(runnable, "sauce-reader-visible").apply {
        isDaemon = true
        priority = Thread.MAX_PRIORITY
    }
}

private object MangaDexReaderManifestDiskCache {
    private const val FORMAT_VERSION = 1
    private const val MAX_AGE_MS = 30L * 24L * 60L * 60L * 1000L
    private const val MAX_FILES = 256

    @Synchronized
    fun load(context: Context, remoteId: String, requestedChapterId: String): SourceReaderContent? {
        val file = file(context, remoteId, requestedChapterId)
        if (!file.isFile || file.length() !in 1..1_048_576) return null
        if (System.currentTimeMillis() - file.lastModified() > MAX_AGE_MS) {
            file.delete()
            return null
        }
        return runCatching {
            val root = JSONObject(file.readText())
            if (root.optInt("version") != FORMAT_VERSION || root.optString("remoteId") != remoteId) return null
            val chapterId = root.optString("chapterId").trim()
            if (chapterId.isBlank() || (requestedChapterId.isNotBlank() && chapterId != requestedChapterId)) return null
            val pages = root.optJSONArray("pages").stringValues()
            val fallbacks = root.optJSONArray("fallbacks").stringValues()
            if (pages.isEmpty() && fallbacks.isEmpty()) return null
            SourceReaderContent(
                entryKey = SourceEntryKey(SourceId("mangadex"), remoteId),
                chapterId = chapterId,
                chapterLabel = root.optString("chapterLabel"),
                chapterTitle = root.optString("chapterTitle"),
                language = root.optString("language"),
                scanlationGroups = root.optJSONArray("groups").stringValues(),
                pageUrls = pages,
                dataSaverPageUrls = fallbacks
            )
        }.getOrNull()
    }

    @Synchronized
    fun save(context: Context, remoteId: String, requestedChapterId: String, content: SourceReaderContent) {
        if (content.chapterId.isBlank() || (content.pageUrls.isEmpty() && content.dataSaverPageUrls.isEmpty())) return
        val root = JSONObject()
            .put("version", FORMAT_VERSION)
            .put("remoteId", remoteId)
            .put("chapterId", content.chapterId)
            .put("chapterLabel", content.chapterLabel)
            .put("chapterTitle", content.chapterTitle)
            .put("language", content.language)
            .put("groups", JSONArray(content.scanlationGroups))
            .put("pages", JSONArray(content.pageUrls))
            .put("fallbacks", JSONArray(content.dataSaverPageUrls))
        write(file(context, remoteId, content.chapterId), root.toString())
        if (requestedChapterId.isBlank()) write(file(context, remoteId, ""), root.toString())
        trim(context)
    }

    @Synchronized
    fun invalidate(context: Context, remoteId: String, chapterId: String) {
        file(context, remoteId, chapterId).delete()
        file(context, remoteId, "").delete()
    }

    private fun file(context: Context, remoteId: String, chapterId: String): File {
        val directory = context.cacheDir.resolve("mangadex-reader-manifests-v1").apply { mkdirs() }
        val rawKey = if (chapterId.isBlank()) "entry-$remoteId" else "chapter-$chapterId"
        val safeKey = rawKey.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return directory.resolve("$safeKey.json")
    }

    private fun write(target: File, payload: String) {
        val temporary = target.resolveSibling("${target.name}.tmp")
        temporary.writeText(payload)
        if (!temporary.renameTo(target)) {
            target.writeText(payload)
            temporary.delete()
        }
    }

    private fun trim(context: Context) {
        val files = context.cacheDir.resolve("mangadex-reader-manifests-v1")
            .listFiles { candidate -> candidate.extension == "json" }
            .orEmpty()
            .sortedByDescending(File::lastModified)
        files.drop(MAX_FILES).forEach(File::delete)
    }

    private fun JSONArray?.stringValues(): List<String> = buildList {
        if (this@stringValues == null) return@buildList
        for (index in 0 until length()) {
            optString(index).trim().takeIf(String::isNotBlank)?.let(::add)
        }
    }
}

/**
 * Starts MangaDex At-Home resolution before the Slideshow Activity has finished opening.
 * Browser, Library and Slideshow all join the same future and then the same page bitmap
 * requests, so a chapter tap never creates duplicate manifest or image work.
 */
internal object MangaDexReaderWarmup {
    private val manifestExecutor = Executors.newFixedThreadPool(2) { runnable ->
        Thread(runnable, "sauce-reader-manifest").apply { isDaemon = true }
    }
    private val inFlight = ConcurrentHashMap<String, CompletableFuture<SourceReaderContent>>()
    private val measuredOpenStartedAt = AtomicLong(0L)
    private val measuredFirstPageUrl = AtomicReference("")

    fun start(
        context: Context,
        adapter: MangaDexSourceAdapter,
        remoteId: String,
        chapterId: String?,
        startPageIndex: Int = 0,
        measureOpen: Boolean = false,
        prefetchPages: Boolean = true
    ): CompletableFuture<SourceReaderContent> {
        val safeChapterId = chapterId?.trim().orEmpty()
        val key = "${remoteId.trim()}|$safeChapterId"
        val created = CompletableFuture<SourceReaderContent>()
        val existing = inFlight.putIfAbsent(key, created)
        if (existing != null) return existing

        if (measureOpen) {
            measuredOpenStartedAt.set(android.os.SystemClock.elapsedRealtime())
            measuredFirstPageUrl.set("")
        }

        val appContext = context.applicationContext
        manifestExecutor.execute {
            val startedAt = android.os.SystemClock.elapsedRealtime()
            try {
                val cachedReader = MangaDexReaderManifestDiskCache.load(appContext, remoteId, safeChapterId)
                val reader = cachedReader ?: adapter.fetchReaderContent(remoteId, safeChapterId.ifBlank { null }).also {
                    MangaDexReaderManifestDiskCache.save(appContext, remoteId, safeChapterId, it)
                }
                val pages = reader.pageUrls.ifEmpty { reader.dataSaverPageUrls }
                val fallbacks = if (reader.pageUrls.isNotEmpty()) reader.dataSaverPageUrls else emptyList()
                // Queue the visible page first. Completing the manifest future afterwards lets
                // Compose join the exact same in-flight bitmap request instead of duplicating it.
                val safeStartIndex = startPageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                if (measureOpen) measuredFirstPageUrl.set(pages.getOrNull(safeStartIndex).orEmpty())
                if (prefetchPages) {
                    prefetchExplicitGalleryPages(
                        context = appContext,
                        pageUris = pages,
                        fallbackPageUris = fallbacks,
                        currentIndex = safeStartIndex,
                        includeCurrent = true
                    )
                }
                created.complete(reader)
                Log.d(
                    "SauceTrackerReaderPerf",
                    "MangaDex manifest ready in ${android.os.SystemClock.elapsedRealtime() - startedAt}ms; pages=${pages.size}; diskCache=${cachedReader != null}"
                )
            } catch (error: Throwable) {
                created.completeExceptionally(error)
            } finally {
                inFlight.remove(key, created)
            }
        }
        return created
    }

    fun markPageReady(url: String) {
        val expected = measuredFirstPageUrl.get()
        if (expected.isBlank() || url != expected) return
        val startedAt = measuredOpenStartedAt.getAndSet(0L)
        if (startedAt <= 0L || !measuredFirstPageUrl.compareAndSet(expected, "")) return
        Log.d(
            "SauceTrackerReaderPerf",
            "MangaDex selected page ready in ${android.os.SystemClock.elapsedRealtime() - startedAt}ms"
        )
    }

    fun invalidate(context: Context, remoteId: String, chapterId: String) {
        MangaDexReaderManifestDiskCache.invalidate(context.applicationContext, remoteId, chapterId)
    }
}

internal val slideshowHttpClient: OkHttpClient by lazy {
    HttpClientFactory.create(HttpClientProfile.SLIDESHOW)
}

private object ReaderPageHttpClient {
    @Volatile private var client: OkHttpClient? = null

    fun get(context: Context): OkHttpClient = client ?: synchronized(this) {
        client ?: HttpClientFactory.create(
            HttpClientProfile.SLIDESHOW,
            Cache(context.applicationContext.cacheDir.resolve("reader-http-v1"), 256L * 1024L * 1024L)
        ).also { client = it }
    }
}

internal fun normalizeImageExtension(raw: String?): String {
    return when (raw?.trim()?.lowercase(Locale.US).orEmpty()) {
        "j", "jpg", "jpeg" -> "jpg"
        "p", "png" -> "png"
        "w", "webp" -> "webp"
        "g", "gif" -> "gif"
        else -> ""
    }
}

internal fun buildGalleryImageUrl(mediaId: Long, pageNumber: Int, extension: String): String {
    return "https://i.nhentai.net/galleries/$mediaId/$pageNumber.$extension"
}

internal fun buildGalleryPageExtensions(
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

internal fun fetchGalleryPageBitmap(
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

internal fun fetchGalleryPageBitmapOnce(
    url: String,
    client: OkHttpClient = slideshowHttpClient
): ImageBitmap? {
    val host = runCatching { Uri.parse(url).host.orEmpty() }.getOrDefault("")
    val request = Request.Builder()
        .url(url)
        .applySourceImageHeaders(url)
        .build()

    return client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            Log.w("SauceTrackerSlideshow", "Image request failed: host=$host status=${response.code}")
            return null
        }
        val body = response.body
        if (body == null) {
            Log.w("SauceTrackerSlideshow", "Image response was empty: host=$host status=${response.code}")
            return null
        }
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
        }
        // Decode directly from OkHttp's stream. body.bytes() temporarily retained the full
        // compressed MangaDex page beside its decoded bitmap and amplified peak heap usage.
        val bitmap = body.byteStream().buffered().use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
        if (bitmap == null) {
            Log.w(
                "SauceTrackerSlideshow",
                "Image decode failed: host=$host type=${response.header("Content-Type").orEmpty()} length=${body.contentLength()}"
            )
            return null
        }
        bitmap.asImageBitmap()
    }
}

internal fun readerPageHttpClient(context: Context): OkHttpClient = ReaderPageHttpClient.get(context)
