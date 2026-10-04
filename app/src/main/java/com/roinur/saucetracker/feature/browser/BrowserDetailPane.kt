package com.roinur.saucetracker.feature.browser

import com.roinur.saucetracker.*
import com.roinur.saucetracker.core.media.*
import com.roinur.saucetracker.core.ui.theme.AccentMode
import com.roinur.saucetracker.core.ui.theme.applyAccentMode
import com.roinur.saucetracker.core.ui.components.*
import com.roinur.saucetracker.data.backup.*
import com.roinur.saucetracker.data.database.SauceTrackerDatabase
import com.roinur.saucetracker.data.profile.ProfileStore
import com.roinur.saucetracker.data.source.SourceChapter
import com.roinur.saucetracker.feature.slideshow.GallerySlideshowActivity

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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.roinur.saucetracker.core.diagnostics.PerformanceMetrics
import com.roinur.saucetracker.core.media.BitmapMemoryCache
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


@Composable
internal fun BrowserDetailTransitionShell(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.98f))
    )
}

@Composable
private fun BrowserChapterPreview(
    chapter: SourceChapter,
    obscure: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val url = chapter.previewUrl
    if (!url.isNullOrBlank()) {
        RemoteThumbnail(
            urls = listOf(url),
            contentDescription = "${chapter.displayTitle} preview",
            obscure = obscure,
            onClick = onClick,
            modifier = modifier
        )
    } else {
        Surface(
            shape = RoundedCornerShape(9.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = modifier
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun GalleryDetailPane(
    detail: BrowserGalleryDetail,
    listState: LazyListState,
    incognitoModeEnabled: Boolean,
    loading: Boolean,
    onOpenSlideshow: (Int) -> Unit,
    onOpenChapter: (String) -> Unit,
    onOpenCode: (Int) -> Unit,
    onOpenRelatedCode: (Int) -> Unit,
    onOpenCreator: (String, String) -> Unit,
    onSearchTag: (String) -> Unit,
    onCopyCandidateDetected: (String) -> Unit,
    onImportSuccessFlash: (Int) -> Unit,
    onLibraryStateChanged: (Int, BrowserLocalLibraryState) -> Unit,
    onLanguageSelected: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember(context) { SauceTrackerDatabase(context.applicationContext) }
    val library = remember(db) { BrowserLibraryAccess(db) }
    val privacyOverlay = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = GALLERY_BROWSER_INCOGNITO_OVERLAY_ALPHA)
    val summary = detail.summary
    val lifecycleOwner = LocalLifecycleOwner.current
    var readerReturnNonce by remember { mutableLongStateOf(0L) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) readerReturnNonce++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val chapterProgress by produceState(initialValue = emptyMap<String, com.roinur.saucetracker.data.database.SourceChapterProgress>(), summary.remoteId, detail.chapters, readerReturnNonce) {
        value = if (!summary.isNhentai) withContext(Dispatchers.IO) {
            db.sourceChapterProgress(ProfileStore(db).activeProfileId(), summary.sourceId, summary.remoteId)
        } else emptyMap()
    }
    val thumbRows = remember(detail.pageThumbs) { detail.pageThumbs.chunked(3) }
    var chaptersAscending by remember(summary.sourceId, summary.remoteId) { mutableStateOf(false) }
    val displayedChapters = remember(detail.chapters, chaptersAscending) {
        com.roinur.saucetracker.data.source.orderedSourceChapters(detail.chapters, chaptersAscending)
    }
    var localLibraryState by remember(summary.sourceId, summary.remoteId) {
        mutableStateOf(BrowserLocalLibraryState(exists = false, rating = 0, isRead = false, pinned = false))
    }
    var localLibraryLoading by remember(summary.sourceId, summary.remoteId) { mutableStateOf(true) }
    var pendingImportAction by remember(summary.sourceId, summary.remoteId) { mutableStateOf<BrowserPendingLibraryAction?>(null) }
    var importLanguage by remember(summary.remoteId) { mutableStateOf(detail.selectedLanguage) }
    val importLanguages = remember(detail.availableLanguages, detail.chapters, detail.selectedLanguage) {
        buildList {
            detail.selectedLanguage.takeIf(String::isNotBlank)?.let(::add)
            addAll(detail.availableLanguages)
            detail.chapters.mapNotNullTo(this) { it.language.takeIf(String::isNotBlank) }
        }.map { it.trim().lowercase() }.filter(String::isNotBlank).distinct()
    }
    LaunchedEffect(incognitoModeEnabled) {
        if (incognitoModeEnabled) pendingImportAction = null
    }
    val detailReadStateColor = if (localLibraryState.isRead) {
        GALLERY_BROWSER_POSITIVE_ACTION_COLOR
    } else {
        GALLERY_BROWSER_NEGATIVE_ACTION_COLOR
    }
    val detailPinStateColor = if (localLibraryState.pinned) {
        GALLERY_BROWSER_POSITIVE_ACTION_COLOR
    } else {
        GALLERY_BROWSER_NEGATIVE_ACTION_COLOR
    }

    fun buildImportGalleryData(): GalleryData {
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

    fun readLocalLibraryState(): BrowserLocalLibraryState = library.state(summary)

    fun applyLibraryActionInDb(action: BrowserPendingLibraryAction) { library.apply(summary, action) }

    fun applyLocalLibraryState(state: BrowserLocalLibraryState) {
        localLibraryState = state
        onLibraryStateChanged(summary.code, state)
    }

    fun refreshLocalLibraryState() {
        scope.launch {
            localLibraryLoading = true
            val refreshed = withContext(Dispatchers.IO) { readLocalLibraryState() }
            applyLocalLibraryState(refreshed)
            localLibraryLoading = false
        }
    }

    fun requestOrApplyLibraryAction(action: BrowserPendingLibraryAction) {
        if (localLibraryState.exists) {
            scope.launch {
                localLibraryLoading = true
                val refreshed = withContext(Dispatchers.IO) {
                    applyLibraryActionInDb(action)
                    readLocalLibraryState()
                }
                applyLocalLibraryState(refreshed)
                localLibraryLoading = false
            }
        } else {
            pendingImportAction = action
        }
    }

    fun removeFromLocalLibrary() {
        if (!localLibraryState.exists) return
        scope.launch {
            localLibraryLoading = true
            val refreshed = withContext(Dispatchers.IO) {
                library.remove(summary)
            }
            applyLocalLibraryState(refreshed)
            localLibraryLoading = false
            Toast.makeText(
                context,
                "Removed ${summary.displayId} from local library.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(summary.sourceId, summary.remoteId, readerReturnNonce) {
        localLibraryLoading = true
        try {
            applyLocalLibraryState(withContext(Dispatchers.IO) { readLocalLibraryState() })
        } finally {
            localLibraryLoading = false
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            val detailCardShape = RoundedCornerShape(18.dp)
            Box(modifier = Modifier.fillMaxWidth()) {
                Card(
                    shape = detailCardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .browserPrivacyObfuscate(
                            enabled = incognitoModeEnabled,
                            overlayColor = privacyOverlay,
                            expandHorizontal = 0.dp,
                            expandVertical = 0.dp,
                            cornerRadius = 18.dp
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .padding(end = 38.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = summary.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (summary.subtitle.isNotBlank() && summary.subtitle != summary.title) {
                            Text(
                                text = summary.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = if (summary.isNhentai) "Code: ${summary.displayId}" else "MangaDex · Copy ID",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !incognitoModeEnabled) {
                                copyTextToClipboard(
                                    context = context,
                                    label = "Sauce code",
                                    value = summary.remoteId,
                                    successMessage = if (summary.isNhentai) "Copied ${summary.displayId}." else "Copied MangaDex ID."
                                )
                                onCopyCandidateDetected(summary.remoteId)
                            }
                        )
                        Text("${summary.unitLabel.replaceFirstChar { it.uppercase() }}: ${summary.numPages}")
                        Text("Uploaded: ${summary.uploadDate.ifBlank { "-" }}")
                        if (!incognitoModeEnabled) {
                            Text(
                                text = if (localLibraryLoading) {
                                    "Library: checking..."
                                } else if (localLibraryState.exists) {
                                    "Library: imported"
                                } else {
                                    "Library: not imported"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.browserPrivacyObfuscate(
                                enabled = incognitoModeEnabled,
                                overlayColor = privacyOverlay
                            )
                        ) {
                            Text(
                                text = "Read:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val readChipShape = RoundedCornerShape(999.dp)
                            Box(
                                modifier = Modifier
                                    .clip(readChipShape)
                                    .clickable(enabled = !incognitoModeEnabled) {
                                        requestOrApplyLibraryAction(
                                            BrowserPendingLibraryAction.SetRead(!localLibraryState.isRead)
                                        )
                                    }
                            ) {
                                AccentPulseOverlay(
                                    triggerKey = localLibraryState.exists to localLibraryState.isRead,
                                    tint = detailReadStateColor,
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(readChipShape)
                                )
                                Text(
                                    text = if (localLibraryState.isRead) "Read" else "Unread",
                                    color = detailReadStateColor,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.browserPrivacyObfuscate(
                                enabled = incognitoModeEnabled,
                                overlayColor = privacyOverlay
                            )
                        ) {
                            Text(
                                text = "Rating:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            for (index in 1..5) {
                                val filled = index <= localLibraryState.rating
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .let { base ->
                                            if (incognitoModeEnabled) {
                                                base
                                            } else {
                                                base.clickable {
                                                    requestOrApplyLibraryAction(
                                                        BrowserPendingLibraryAction.SetRating(index)
                                                    )
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (filled) "\u2605" else "\u2606",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (filled) {
                                            RATING_STAR_GOLD
                                        } else {
                                            RATING_STAR_MUTED
                                        }
                                    )
                                }
                            }
                            TextButton(
                                enabled = !incognitoModeEnabled,
                                onClick = {
                                    if (localLibraryState.exists && localLibraryState.rating != 0) {
                                        requestOrApplyLibraryAction(
                                            BrowserPendingLibraryAction.SetRating(0)
                                        )
                                    }
                                }
                            ) {
                                Text("Reset")
                            }
                        }
                        if (loading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Loading...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onOpenSlideshow(1) }) { Text("Open Slideshow") }
                            TextButton(onClick = { onOpenCode(summary.code) }, enabled = summary.isNhentai) {
                                Text("Refresh")
                            }
                            TextButton(
                                enabled = !incognitoModeEnabled && !localLibraryLoading,
                                onClick = {
                                    if (localLibraryState.exists) removeFromLocalLibrary()
                                    else requestOrApplyLibraryAction(BrowserPendingLibraryAction.ImportOnly)
                                }
                            ) {
                                Text(if (localLibraryState.exists) "Remove" else "Import")
                            }
                        }
                    }
                }
                // This must sit above the opaque detail surface; beneath it the
                // shimmer exists but is completely hidden by the card background.
                PinnedCornerBleedGlow(
                    visible = localLibraryState.pinned && !incognitoModeEnabled,
                    tint = MaterialTheme.colorScheme.primary,
                    cornerRadius = 18.dp,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(detailCardShape)
                )
                IconButton(
                    onClick = {
                        requestOrApplyLibraryAction(
                            BrowserPendingLibraryAction.SetPinned(!localLibraryState.pinned)
                        )
                    },
                    enabled = !incognitoModeEnabled,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(999.dp))
                    ) {
                        AccentPulseOverlay(
                            triggerKey = localLibraryState.exists to localLibraryState.pinned,
                            tint = detailPinStateColor,
                            modifier = Modifier.matchParentSize()
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_push_pin_24),
                            contentDescription = when {
                                incognitoModeEnabled -> null
                                localLibraryState.pinned -> "Unpin entry ${summary.code}"
                                else -> "Pin entry ${summary.code}"
                            },
                            tint = if (incognitoModeEnabled) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            } else if (localLibraryState.pinned) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(20.dp)
                        )
                    }
                }
            }
        }

        item {
            RemoteThumbnail(
                urls = summary.coverUrls,
                backupCode = summary.code.takeIf { summary.isNhentai },
                contentDescription = if (summary.isNhentai) "Cover for ${summary.displayId}" else "MangaDex cover",
                obscure = incognitoModeEnabled,
                onClick = { onOpenSlideshow(1) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 300.dp)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Tags",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (detail.tagsByType.isEmpty()) {
                        Text(
                            text = "(none)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        detail.tagsByType.entries.forEach { (type, names) ->
                            val normalizedType = type.trim().lowercase(Locale.US)
                            val cleanedNames = names
                                .map { rawName ->
                                    if (normalizedType == "artist" || normalizedType == "group") {
                                        normalizeCreatorDisplayName(rawName)
                                    } else {
                                        rawName.trim()
                                    }
                                }
                                .filter { it.isNotBlank() }
                                .distinctBy { it.lowercase(Locale.US) }
                            if (cleanedNames.isEmpty()) return@forEach

                            Text(
                                text = "${type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }}:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                cleanedNames.forEach { name ->
                                    val clickableCreator = normalizedType == "artist" || normalizedType == "group"
                                    val localCount = detail.tagCountsByKey[browserTagLookupKey(type = normalizedType, name = name)]
                                    BrowserDetailTagChip(
                                        name = name,
                                        count = localCount,
                                        incognitoModeEnabled = incognitoModeEnabled,
                                        onClick = {
                                            if (clickableCreator) {
                                                onOpenCreator(normalizedType, name)
                                            } else {
                                                onSearchTag(name)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!summary.isNhentai) {
            if (importLanguages.isNotEmpty()) {
                item {
                    Text("Language", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            importLanguages.forEach { language ->
                                FilterChip(
                                    selected = detail.selectedLanguage == language,
                                    onClick = { if (!incognitoModeEnabled) onLanguageSelected(language) },
                                    enabled = !incognitoModeEnabled,
                                    label = { Text(com.roinur.saucetracker.data.source.mangaDexLanguageDisplayName(language)) }
                                )
                            }
                        }
                    }
                }
            }
            item {
                ChapterListHeader(
                    count = detail.chapters.size, loading = loading,
                    ascending = chaptersAscending, onToggleOrder = { chaptersAscending = !chaptersAscending },
                    enabled = !incognitoModeEnabled
                )
            }
            items(displayedChapters, key = { chapter -> chapter.id }) { chapter ->
                val shape = RoundedCornerShape(12.dp)
                val progress = chapterProgress[chapter.id]
                Surface(
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .clickable(enabled = !incognitoModeEnabled) { onOpenChapter(chapter.id) }
                ) {
                    Box(modifier = Modifier.fillMaxWidth().clip(shape)) {
                        if (!incognitoModeEnabled && (progress?.fraction ?: 0f) > 0f) {
                            ReadingProgressBleedGlow(
                                fraction = progress!!.fraction,
                                tint = MaterialTheme.colorScheme.primary,
                                cornerRadius = 12.dp,
                                modifier = Modifier.matchParentSize().clip(shape)
                            )
                        }
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BrowserChapterPreview(
                                chapter = chapter,
                                obscure = incognitoModeEnabled,
                                onClick = { onOpenChapter(chapter.id) },
                                modifier = Modifier.size(42.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chapter.displayTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (progress?.completed == true) FontWeight.Normal else FontWeight.Medium,
                                    color = if (progress?.completed == true) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val secondary = buildList {
                                    chapter.language.takeIf(String::isNotBlank)?.let(::add)
                                    chapter.scanlationGroups.firstOrNull()?.takeIf(String::isNotBlank)?.let(::add)
                                    chapter.pageCount.takeIf { it > 0 }?.let { add("$it pages") }
                                }.joinToString(" · ")
                                if (secondary.isNotBlank()) {
                                    Text(
                                        text = secondary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Text("›", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        } else {
        item {
            Text(
                text = "Gallery",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(thumbRows, key = { row -> row.firstOrNull()?.pageNumber ?: -1 }) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { thumb ->
                    Box(modifier = Modifier.weight(1f)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            RemoteThumbnail(
                                urls = thumb.thumbnailUrls,
                                contentDescription = "Page ${thumb.pageNumber} thumbnail",
                                obscure = incognitoModeEnabled,
                                onClick = { onOpenSlideshow(thumb.pageNumber) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp)
                            )
                            Text(
                                text = "Page ${thumb.pageNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.browserPrivacyObfuscate(
                                    enabled = incognitoModeEnabled,
                                    overlayColor = privacyOverlay
                                )
                            )
                        }
                    }
                }
                if (row.size < 3) {
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        }

        if (detail.relatedGalleries.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "More like this",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Related on nhentai.net",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        detail.relatedGalleries.take(5).forEach { related ->
                            Card(
                                onClick = { onOpenRelatedCode(related.code) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.width(154.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    RemoteThumbnail(
                                        urls = buildCoverThumbnailUrls(related.mediaId, related.coverExt),
                                        backupCode = related.code,
                                        contentDescription = "Related cover for code ${related.code}",
                                        obscure = incognitoModeEnabled,
                                        onClick = { onOpenRelatedCode(related.code) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(190.dp)
                                    )
                                    Column(
                                        modifier = Modifier.browserPrivacyObfuscate(
                                            enabled = incognitoModeEnabled,
                                            overlayColor = privacyOverlay,
                                            expandHorizontal = 0.dp,
                                            expandVertical = 0.dp,
                                            cornerRadius = 8.dp
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = related.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${related.numPages} pages",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Comments",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (detail.comments.isEmpty()) {
                        Text(
                            text = "(none found)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        detail.comments.forEach { comment ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .browserPrivacyObfuscate(
                                        enabled = incognitoModeEnabled,
                                        overlayColor = privacyOverlay,
                                        expandHorizontal = 0.dp,
                                        expandVertical = 0.dp,
                                        cornerRadius = 12.dp
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = comment.author.ifBlank { "Anonymous" },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = comment.text.ifBlank { "(empty)" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingImportAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingImportAction = null },
            title = { Text(if (action == BrowserPendingLibraryAction.ImportOnly) "Import Entry" else "Import Required") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (action == BrowserPendingLibraryAction.ImportOnly) "Add this entry to your library?" else "You must import this sauce to change rating, read status, or pin state. Import now?")
                    if (!summary.isNhentai) {
                        Text("Choose language", fontWeight = FontWeight.SemiBold)
                        if (importLanguages.isEmpty()) {
                            Text("No translated languages are currently available.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    importLanguages.forEach { language ->
                                        FilterChip(selected = importLanguage == language, onClick = { importLanguage = language },
                                            label = { Text(com.roinur.saucetracker.data.source.mangaDexLanguageDisplayName(language)) })
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = summary.isNhentai || importLanguage.isNotBlank(),
                    onClick = {
                        val confirmedAction = action
                        pendingImportAction = null
                        scope.launch {
                            localLibraryLoading = true
                            val importDetail = if (!summary.isNhentai && importLanguage.isNotBlank()) detail.withMangaDexLanguage(importLanguage) else detail
                            val importSummary = importDetail.summary
                            val refreshed = try {
                                withContext(Dispatchers.IO) {
                                    library.import(importDetail)
                                    if (library.state(importSummary).exists) {
                                        library.apply(importSummary, confirmedAction)
                                    }
                                    library.state(importSummary)
                                }
                            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                BrowserLocalLibraryState(false, 0, false, false)
                            } finally {
                                localLibraryLoading = false
                            }
                            applyLocalLibraryState(refreshed)
                            if (refreshed.exists) {
                                if (!summary.isNhentai && importLanguage != detail.selectedLanguage) {
                                    onLanguageSelected(importLanguage)
                                }
                                onImportSuccessFlash(summary.code)
                            } else {
                                Toast.makeText(
                                    context,
                                    "Could not import ${summary.displayId}.",
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
                OutlinedButton(onClick = { pendingImportAction = null }) {
                    Text("No")
                }
            }
        )
    }
}

@Composable
internal fun BrowserDetailTagChip(
    name: String,
    count: Int?,
    incognitoModeEnabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val countLabel = count?.takeIf { it >= 0 }?.let(::formatCompactTagCount)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                shape = shape
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.78f),
                shape = shape
            )
            .clickable(enabled = !incognitoModeEnabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = name,
            modifier = Modifier.browserPrivacyObfuscate(
                enabled = incognitoModeEnabled,
                overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = GALLERY_BROWSER_INCOGNITO_OVERLAY_ALPHA)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        if (!countLabel.isNullOrBlank()) {
            Text(
                text = countLabel,
                modifier = Modifier.browserPrivacyObfuscate(
                    enabled = incognitoModeEnabled,
                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = GALLERY_BROWSER_INCOGNITO_OVERLAY_ALPHA)
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
