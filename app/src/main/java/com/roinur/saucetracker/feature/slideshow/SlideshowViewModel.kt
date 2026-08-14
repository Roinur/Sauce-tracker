package com.roinur.saucetracker.feature.slideshow

import com.roinur.saucetracker.*
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
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
import okhttp3.Request
import java.util.Locale
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

    @Synchronized
    fun clear() {
        bitmaps.clear()
        resolvedExtensions.clear()
    }
}

internal val slideshowHttpClient: OkHttpClient by lazy {
    HttpClientFactory.create(HttpClientProfile.SLIDESHOW)
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

internal fun fetchGalleryPageBitmapOnce(url: String): ImageBitmap? {
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
