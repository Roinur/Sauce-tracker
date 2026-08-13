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


@Composable
internal fun RemoteThumbnail(
    urls: List<String>,
    backupCode: Int? = null,
    contentDescription: String,
    obscure: Boolean = false,
    onClick: () -> Unit,
    onReadyToDisplay: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val thumbShape = RoundedCornerShape(12.dp)
    val privacyOverlay = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = GALLERY_BROWSER_INCOGNITO_OVERLAY_ALPHA)
    val candidates = remember(urls) {
        urls.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }
    val cacheKey = remember(candidates) { candidates.firstOrNull().orEmpty() }
    val initialBitmap = remember(candidates) {
        candidates.firstNotNullOfOrNull { GalleryBrowserThumbnailCache.get(it) }
    }
    val bitmap by produceState<ImageBitmap?>(initialValue = initialBitmap, candidates, backupCode) {
        if (candidates.isEmpty()) {
            value = null
            return@produceState
        }
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            fetchGalleryBrowserThumbnail(
                context = context.applicationContext,
                urls = candidates,
                backupCode = backupCode
            )
        }?.also { loaded ->
            if (cacheKey.isNotBlank()) {
                GalleryBrowserThumbnailCache.put(cacheKey, loaded)
            }
        }
    }
    LaunchedEffect(bitmap != null, backupCode, cacheKey) {
        if (bitmap != null) {
            onReadyToDisplay?.invoke()
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
            .browserPrivacyObfuscate(
                enabled = obscure,
                overlayColor = privacyOverlay
            )
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

@Composable
internal fun BrowserDuplicateComparisonDialog(
    state: BrowserDuplicateComparisonState,
    incognitoModeEnabled: Boolean,
    entryDetailProvider: suspend (Int) -> EntryDetail?,
    flaggedDetailProvider: suspend (Int) -> BrowserGalleryDetail?,
    onHide: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val originalState by produceState<Pair<EntryDetail?, Boolean>>(
        initialValue = null to false,
        state.hint.matchedCode
    ) {
        value = null to false
        value = entryDetailProvider(state.hint.matchedCode) to true
    }
    val originalDetail = originalState.first
    val originalLoaded = originalState.second
    val flaggedState by produceState<Pair<BrowserGalleryDetail?, Boolean>>(
        initialValue = null to false,
        state.row.code
    ) {
        value = null to false
        value = flaggedDetailProvider(state.row.code) to true
    }
    val flaggedDetail = flaggedState.first
    val flaggedLoaded = flaggedState.second
    val privacyOverlay = MaterialTheme.colorScheme.surfaceVariant.copy(
        alpha = GALLERY_BROWSER_INCOGNITO_OVERLAY_ALPHA
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Duplicate Check",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = state.hint.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.browserPrivacyObfuscate(
                            enabled = incognitoModeEnabled,
                            overlayColor = privacyOverlay
                        )
                    )

                    Text(
                        text = "Imported original",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Card(
                        modifier = Modifier.browserPrivacyObfuscate(
                            enabled = incognitoModeEnabled,
                            overlayColor = privacyOverlay,
                            expandHorizontal = 0.dp,
                            expandVertical = 0.dp,
                            cornerRadius = 12.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                    ) {
                        if (!originalLoaded) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        } else if (originalDetail == null) {
                            Text(
                                text = "Imported entry #${state.hint.matchedCode} could not be loaded.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                RemoteThumbnail(
                                    urls = listOf(originalDetail.thumbnailUrl),
                                    backupCode = originalDetail.code,
                                    contentDescription = "Cover for imported code ${originalDetail.code}",
                                    obscure = incognitoModeEnabled,
                                    onClick = {},
                                    modifier = Modifier
                                        .width(92.dp)
                                        .height(124.dp)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "#${originalDetail.code} - ${originalDetail.title.ifBlank { "Gallery ${originalDetail.code}" }}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Pages: ${originalDetail.numPages} - Uploaded: ${originalDetail.uploadDate.ifBlank { "-" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Read: ${if (originalDetail.isRead) "Yes" else "No"} - Rating: ${originalDetail.rating}/5",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                    )

                    Text(
                        text = "Flagged duplicate",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Card(
                        modifier = Modifier.browserPrivacyObfuscate(
                            enabled = incognitoModeEnabled,
                            overlayColor = privacyOverlay,
                            expandHorizontal = 0.dp,
                            expandVertical = 0.dp,
                            cornerRadius = 12.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                    ) {
                        if (!flaggedLoaded) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        } else {
                            val flaggedSummary = flaggedDetail?.summary ?: state.row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                RemoteThumbnail(
                                    urls = buildCoverThumbnailUrls(flaggedSummary.mediaId, flaggedSummary.coverExt),
                                    backupCode = flaggedSummary.code,
                                    contentDescription = "Cover for browser code ${flaggedSummary.code}",
                                    obscure = incognitoModeEnabled,
                                    onClick = {},
                                    modifier = Modifier
                                        .width(92.dp)
                                        .height(124.dp)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "#${flaggedSummary.code} - ${flaggedSummary.title.ifBlank { "Gallery ${flaggedSummary.code}" }}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Pages: ${flaggedSummary.numPages} - Uploaded: ${flaggedSummary.uploadDate.ifBlank { "-" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (flaggedSummary.subtitle.isNotBlank() && flaggedSummary.subtitle != flaggedSummary.title) {
                                        Text(
                                            text = flaggedSummary.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Close")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            enabled = !incognitoModeEnabled,
                            onClick = { onHide(state.row.code) }
                        ) {
                            Text("Hide")
                        }
                    }
                }
            }
        }
    }
}

internal object GalleryBrowserThumbnailCache {
    private val maximumBytes = (Runtime.getRuntime().maxMemory() / 24L)
        .coerceIn(12L * 1024L * 1024L, 24L * 1024L * 1024L)
    private val cache = BitmapMemoryCache<String, ImageBitmap>(
        maximumBytes = maximumBytes,
        sizeOf = { bitmap ->
            bitmap.width.toLong().coerceAtLeast(1L) *
                bitmap.height.toLong().coerceAtLeast(1L) * 4L
        }
    )

    @Synchronized
    fun get(url: String): ImageBitmap? = cache[url]

    @Synchronized
    fun put(url: String, bitmap: ImageBitmap) {
        if (url.isBlank()) return
        cache.put(url, bitmap)
    }

    @Synchronized
    fun clear() {
        cache.clear()
    }
}

internal val galleryBrowserImageClient: OkHttpClient by lazy {
    HttpClientFactory.create(HttpClientProfile.BROWSER_IMAGE)
}

internal fun fetchGalleryBrowserThumbnail(
    context: Context,
    urls: List<String>,
    backupCode: Int? = null
): ImageBitmap? {
    if (backupCode != null && backupCode > 0) {
        readBackupThumbnailBitmapForCode(context, backupCode)?.let { bitmap ->
            val duplicateHash = runCatching { computeDHash64(bitmap) }.getOrNull()
            urls.forEach { candidate ->
                val normalized = browserNormalizeDuplicateThumbnailCacheKey(candidate)
                if (normalized.isNotBlank()) {
                    BrowserDuplicateThumbnailHashCache.put(normalized, duplicateHash)
                }
            }
            val archived = bitmap.asImageBitmap()
            urls.forEach { candidate -> GalleryBrowserThumbnailCache.put(candidate, archived) }
            return archived
        }
    }
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
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return@use null
                bitmap.asImageBitmap()
            }
        }.getOrNull()
        if (fetched != null) {
            urls.forEach { candidate -> GalleryBrowserThumbnailCache.put(candidate, fetched) }
            return fetched
        }
    }
    return null
}

