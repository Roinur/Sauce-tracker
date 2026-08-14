package com.roinur.saucetracker.feature.browser

import com.roinur.saucetracker.*
import com.roinur.saucetracker.core.media.*
import com.roinur.saucetracker.core.ui.theme.AccentMode
import com.roinur.saucetracker.core.ui.theme.applyAccentMode
import com.roinur.saucetracker.core.ui.components.*
import com.roinur.saucetracker.data.backup.*
import com.roinur.saucetracker.data.database.SauceTrackerDatabase
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
import com.roinur.saucetracker.core.diagnostics.GitHubMediaSession


@Composable
internal fun GalleryCodeBrowserTheme(
    themeMode: ThemeMode,
    accentMode: AccentMode,
    incognitoModeEnabled: Boolean,
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

    val colorScheme = if (incognitoModeEnabled) {
        incognitoScheme
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        applyAccentMode(
            baseScheme = if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context),
            accentMode = accentMode,
            isDark = useDark
        )
    } else {
        applyAccentMode(
            baseScheme = fallbackScheme,
            accentMode = accentMode,
            isDark = useDark
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        key(themeMode, accentMode, useDark, incognitoModeEnabled) {
            GalleryCodeBrowserApplySystemBars(colorScheme.background)
            content()
        }
    }
}

@Composable
internal fun GalleryCodeBrowserApplySystemBars(
    backgroundColor: Color
) {
    val view = LocalView.current
    if (view.isInEditMode) return

    val color = backgroundColor.toArgb()
    val darkContent = backgroundColor.luminance() > 0.5f
    SideEffect {
        val activity = view.context as? Activity ?: return@SideEffect
        val window = activity.window
        window.statusBarColor = color
        window.navigationBarColor = color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = darkContent
            isAppearanceLightNavigationBars = darkContent
        }
    }
}


@Composable
internal fun GalleryListSectionHeader(
    title: String,
    subtitle: String? = null
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GallerySummaryCard(
    row: BrowserGallerySummary,
    incognitoModeEnabled: Boolean,
    localLibraryState: BrowserLocalLibraryState,
    loading: Boolean,
    browserDuplicateCheckMode: BrowserDuplicateCheckMode,
    duplicateHint: DuplicateHint?,
    duplicateHintLoading: Boolean,
    importFlashKey: Int,
    duplicateSeedVersion: Int,
    duplicateChecksPaused: Boolean,
    onOpen: () -> Unit,
    onOpenSlideshow: () -> Unit,
    onOpenDuplicateHint: (DuplicateHint) -> Unit,
    onEnsureDuplicateHint: () -> Unit,
    onQuickTogglePinned: (Int, Boolean) -> Unit,
    onQuickToggleRead: (Int, Boolean) -> Unit,
    onQuickSetRating: (Int, Int) -> Unit,
    onHide: (Int) -> Unit,
    onActionBlocked: () -> Unit
) {
    val privacyOverlay = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = GALLERY_BROWSER_INCOGNITO_OVERLAY_ALPHA)
    val cardShape = RoundedCornerShape(12.dp)
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val swipeCommitTracker = rememberSwipeCommitTrackerState(
        key = row.code,
        config = SwipeCommitConfig(
            minHorizontalSwipePx = with(density) { 88.dp.toPx() },
            minGestureDurationMs = 160L,
            maxVerticalPerHorizontalRatio = 1f / 1.65f,
            maxSwipeSpeedPxPerMs = 1.15f
        )
    )
    var rowWidthPx by remember(row.code) { mutableStateOf(1f) }
    var dragRating by remember(row.code) { mutableStateOf<Int?>(null) }
    LaunchedEffect(
        row.code,
        duplicateSeedVersion,
        browserDuplicateCheckMode,
        duplicateChecksPaused,
        incognitoModeEnabled
    ) {
        if (incognitoModeEnabled) return@LaunchedEffect
        if (browserDuplicateCheckMode == BrowserDuplicateCheckMode.OFF) return@LaunchedEffect
        if (duplicateChecksPaused) return@LaunchedEffect
        delay(GALLERY_BROWSER_DUPLICATE_HINT_THUMBNAIL_PRIORITY_DELAY_MS)
        onEnsureDuplicateHint()
    }
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * 0.45f },
        confirmValueChange = { target ->
            if (target != SwipeToDismissBoxValue.Settled && !swipeCommitTracker.deliberate) {
                return@rememberSwipeToDismissBoxState false
            }
            when (target) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (incognitoModeEnabled) {
                        onActionBlocked()
                    } else {
                        onQuickTogglePinned(row.code, !localLibraryState.pinned)
                    }
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (incognitoModeEnabled) {
                        onActionBlocked()
                    } else {
                        onQuickToggleRead(row.code, !localLibraryState.isRead)
                    }
                }
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        }
    )
    val commitReadyTarget = trackSwipeCommitFeedback(
        key = row.code,
        tracker = swipeCommitTracker,
        dismissTarget = dismissState.targetValue,
        feedbackEnabled = !incognitoModeEnabled,
        haptic = haptic
    )
    val direction = dismissState.dismissDirection
    val swipeProgress = dismissState.progress
    val swipeVisualActive = swipeProgress > 0.001f
    var swipeSnapshotPinned by remember(row.code) { mutableStateOf(localLibraryState.pinned) }
    var swipeSnapshotRead by remember(row.code) { mutableStateOf(localLibraryState.isRead) }
    var swipeSnapshotCaptured by remember(row.code) { mutableStateOf(false) }
    LaunchedEffect(swipeCommitTracker.gestureActive, localLibraryState.pinned, localLibraryState.isRead) {
        if (swipeCommitTracker.gestureActive && !swipeSnapshotCaptured) {
            swipeSnapshotPinned = localLibraryState.pinned
            swipeSnapshotRead = localLibraryState.isRead
            swipeSnapshotCaptured = true
        } else if (!swipeCommitTracker.gestureActive) {
            swipeSnapshotCaptured = false
        }
    }
    val visualDirection = if (swipeVisualActive && direction != SwipeToDismissBoxValue.Settled) {
        direction
    } else {
        SwipeToDismissBoxValue.Settled
    }
    val visualPinnedState = if (swipeCommitTracker.gestureActive && swipeSnapshotCaptured) swipeSnapshotPinned else localLibraryState.pinned
    val visualReadState = if (swipeCommitTracker.gestureActive && swipeSnapshotCaptured) swipeSnapshotRead else localLibraryState.isRead
    val swipeLabel = when (visualDirection) {
        SwipeToDismissBoxValue.StartToEnd -> if (visualPinnedState) "Unpin" else "Pin"
        SwipeToDismissBoxValue.EndToStart -> if (visualReadState) "Unread" else "Read"
        SwipeToDismissBoxValue.Settled -> ""
    }
    val swipeGlyph = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> "\uD83D\uDCCC"
        SwipeToDismissBoxValue.EndToStart -> if (localLibraryState.isRead) "○" else "✓"
        SwipeToDismissBoxValue.Settled -> ""
    }
    val swipeTint = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> if (localLibraryState.pinned) {
            GALLERY_BROWSER_NEGATIVE_ACTION_COLOR
        } else {
            GALLERY_BROWSER_POSITIVE_ACTION_COLOR
        }
        SwipeToDismissBoxValue.EndToStart -> if (localLibraryState.isRead) {
            GALLERY_BROWSER_NEGATIVE_ACTION_COLOR
        } else {
            GALLERY_BROWSER_POSITIVE_ACTION_COLOR
        }
        SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val backgroundAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (!swipeVisualActive) 0f else (0.42f + (swipeProgress * 0.58f)).coerceIn(0f, 1f),
        label = "browserSwipeAlpha"
    )
    val contentScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 180),
        label = "browserSwipeContentScale"
    )
    val contentAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = 1f - (swipeProgress * 0.035f),
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 180),
        label = "browserSwipeContentAlpha"
    )
    val backgroundColor by androidx.compose.animation.animateColorAsState(
        targetValue = when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> if (localLibraryState.pinned) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.secondaryContainer
            SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "browserSwipeColor"
    )
    val resolvedSwipeGlyph = when (visualDirection) {
        SwipeToDismissBoxValue.StartToEnd -> "\uD83D\uDCCC"
        SwipeToDismissBoxValue.EndToStart -> if (visualReadState) "\u25CB" else "\u2713"
        SwipeToDismissBoxValue.Settled -> ""
    }
    val resolvedSwipeTint = when (visualDirection) {
        SwipeToDismissBoxValue.StartToEnd -> if (visualPinnedState) {
            GALLERY_BROWSER_NEGATIVE_ACTION_COLOR
        } else {
            GALLERY_BROWSER_POSITIVE_ACTION_COLOR
        }
        SwipeToDismissBoxValue.EndToStart -> if (visualReadState) {
            GALLERY_BROWSER_NEGATIVE_ACTION_COLOR
        } else {
            GALLERY_BROWSER_POSITIVE_ACTION_COLOR
        }
        SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val resolvedBackgroundAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (swipeProgress <= 0.001f) 0f else (0.42f + (swipeProgress * 0.58f)).coerceIn(0f, 1f),
        label = "browserSwipeResolvedAlpha"
    )
    val resolvedBackgroundColor by androidx.compose.animation.animateColorAsState(
        targetValue = when (visualDirection) {
            SwipeToDismissBoxValue.StartToEnd -> if (visualPinnedState) {
                GALLERY_BROWSER_NEGATIVE_ACTION_COLOR
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
            SwipeToDismissBoxValue.EndToStart -> if (visualReadState) {
                GALLERY_BROWSER_NEGATIVE_ACTION_COLOR
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
            SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "browserSwipeResolvedColor"
    )
    val readStateColor = if (localLibraryState.isRead) {
        GALLERY_BROWSER_POSITIVE_ACTION_COLOR
    } else {
        GALLERY_BROWSER_NEGATIVE_ACTION_COLOR
    }
    val pinStateColor = if (localLibraryState.pinned) {
        GALLERY_BROWSER_POSITIVE_ACTION_COLOR
    } else {
        GALLERY_BROWSER_NEGATIVE_ACTION_COLOR
    }

    SwipeToDismissBox(
        modifier = Modifier.trackSwipeCommitGestures(
            gestureKey = row.code,
            tracker = swipeCommitTracker
        ),
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(cardShape)
                    .background(resolvedBackgroundColor.copy(alpha = resolvedBackgroundAlpha))
            ) {
                SwipeCommitReadySwoosh(
                    commitReadyTarget = commitReadyTarget,
                    tint = resolvedSwipeTint,
                    modifier = Modifier.matchParentSize()
                )
                if (swipeLabel.isNotBlank()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (incognitoModeEnabled) "\u26D4" else resolvedSwipeGlyph,
                            style = MaterialTheme.typography.titleMedium,
                            color = resolvedSwipeTint
                        )
                        Text(
                            text = if (incognitoModeEnabled) "Blocked" else swipeLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = resolvedSwipeTint
                        )
                    }
                }
            }
        }
    ) {
        Card(
            shape = cardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .graphicsLayer {
                    scaleX = contentScale
                    scaleY = contentScale
                    alpha = contentAlpha
                }
                .onGloballyPositioned { coordinates ->
                    rowWidthPx = coordinates.size.width.toFloat().coerceAtLeast(1f)
                }
                .pointerInput(row.code, incognitoModeEnabled) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { start ->
                            if (incognitoModeEnabled) {
                                onActionBlocked()
                                dragRating = null
                                return@detectDragGesturesAfterLongPress
                            }
                            val initial = browserMapDragPositionToRating(start.x, rowWidthPx)
                            dragRating = initial
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDrag = { change, _ ->
                            val active = dragRating ?: return@detectDragGesturesAfterLongPress
                            val next = browserMapDragPositionToRating(change.position.x, rowWidthPx)
                            if (next != active) {
                                dragRating = next
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            change.consume()
                        },
                        onDragEnd = {
                            val selectedAction = dragRating
                            dragRating = null
                            if (!incognitoModeEnabled && selectedAction != null && selectedAction > 0) {
                                if (selectedAction == GALLERY_BROWSER_HOLD_ACTION_HIDE) {
                                    onHide(row.code)
                                } else {
                                    onQuickSetRating(row.code, selectedAction)
                                }
                            }
                        },
                        onDragCancel = { dragRating = null }
                    )
                }
                .clickable(onClick = onOpen)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 146.dp)
            ) {
                PinnedCornerBleedGlow(
                    visible = localLibraryState.pinned && !incognitoModeEnabled,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(cardShape)
                )
                AccentPulseOverlay(
                    triggerKey = importFlashKey,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(cardShape)
                )
                LoadingShimmerOverlay(
                    active = !incognitoModeEnabled && (duplicateHintLoading || duplicateHint != null),
                    tint = if (duplicateHint != null) {
                        GALLERY_BROWSER_NEGATIVE_ACTION_COLOR
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier
                        .matchParentSize()
                        .clip(cardShape)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .heightIn(min = 130.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .width(98.dp)
                            .height(130.dp)
                    ) {
                        RemoteThumbnail(
                            urls = buildCoverThumbnailUrls(row.mediaId, row.coverExt),
                            backupCode = row.code,
                            contentDescription = "Cover for code ${row.code}",
                            obscure = incognitoModeEnabled,
                            onClick = onOpenSlideshow,
                            modifier = Modifier.matchParentSize()
                        )
                        if (loading) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.34f))
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(34.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = row.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(end = if (localLibraryState.pinned) 22.dp else 0.dp)
                                    .browserPrivacyObfuscate(
                                        enabled = incognitoModeEnabled,
                                        overlayColor = privacyOverlay
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onOpen)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Code: ${row.code}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.browserPrivacyObfuscate(
                                            enabled = incognitoModeEnabled,
                                            overlayColor = privacyOverlay
                                        )
                                    )
                                    if (localLibraryState.exists) {
                                        Text(
                                            text = "Imported",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.browserPrivacyObfuscate(
                                                enabled = incognitoModeEnabled,
                                                overlayColor = privacyOverlay
                                            )
                                        )
                                    }
                                    if (row.numPages > 0) {
                                        Text(
                                            text = "Pages: ${row.numPages}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.browserPrivacyObfuscate(
                                                enabled = incognitoModeEnabled,
                                                overlayColor = privacyOverlay
                                            )
                                        )
                                    }
                                    if (row.uploadDate.isNotBlank()) {
                                        Text(
                                            text = "Uploaded: ${row.uploadDate}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.browserPrivacyObfuscate(
                                                enabled = incognitoModeEnabled,
                                                overlayColor = privacyOverlay
                                            )
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .browserPrivacyObfuscate(
                                                enabled = incognitoModeEnabled,
                                                overlayColor = privacyOverlay
                                            )
                                    ) {
                                        AccentPulseOverlay(
                                            triggerKey = localLibraryState.exists to localLibraryState.isRead,
                                            tint = readStateColor,
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clip(RoundedCornerShape(999.dp))
                                        )
                                        Text(
                                            text = "Read: ${if (localLibraryState.isRead) "Read" else "Unread"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = readStateColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        modifier = Modifier.browserPrivacyObfuscate(
                                            enabled = incognitoModeEnabled,
                                            overlayColor = privacyOverlay
                                        )
                                    ) {
                                        val safeRating = localLibraryState.rating.coerceIn(0, 5)
                                        Text(
                                            text = "Rating:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = buildString {
                                                repeat(5) { index ->
                                                    append(if (index < safeRating) '\u2605' else '\u2606')
                                                }
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = RATING_STAR_GOLD,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "($safeRating/5)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                                if (!incognitoModeEnabled && (duplicateHint != null || duplicateHintLoading)) {
                                    Column(
                                        modifier = Modifier.width(132.dp),
                                        horizontalAlignment = Alignment.Start,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        duplicateHint?.let { hint ->
                                        Text(
                                            text = "Duplicate? #${hint.matchedCode}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = GALLERY_BROWSER_NEGATIVE_ACTION_COLOR,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .clickable(onClick = { onOpenDuplicateHint(hint) })
                                                .padding(top = 1.dp)
                                        )
                                        Text(
                                            text = hint.reason,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GALLERY_BROWSER_NEGATIVE_ACTION_COLOR.copy(alpha = 0.92f),
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 1.dp)
                                        )
                                        }
                                        if (duplicateHint == null && duplicateHintLoading && !incognitoModeEnabled) {
                                            Text(
                                                text = "Checking duplicate...",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (localLibraryState.pinned && !incognitoModeEnabled) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 1.dp, end = 1.dp)
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(999.dp))
                            ) {
                                AccentPulseOverlay(
                                    triggerKey = localLibraryState.exists to localLibraryState.pinned,
                                    tint = pinStateColor,
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(999.dp))
                                )
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_push_pin_24),
                                    contentDescription = "Pinned in local library",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(16.dp)
                                )
                            }
                        }
                    }
                }
                dragRating?.let { live ->
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(cardShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f))
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(0.84f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Drag to rate or hide #${row.code}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                (0..GALLERY_BROWSER_HOLD_ACTION_HIDE).forEach { value ->
                                    val selected = value == live
                                    val holdButtonShape = RoundedCornerShape(14.dp)
                                    Box(
                                        modifier = Modifier
                                            .weight(if (value == 0 || value == GALLERY_BROWSER_HOLD_ACTION_HIDE) 1.7f else 1f)
                                            .clip(holdButtonShape)
                                            .background(
                                                when (value) {
                                                    0 -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                                                        alpha = if (selected) 0.96f else 0.72f
                                                    )
                                                    GALLERY_BROWSER_HOLD_ACTION_HIDE -> GALLERY_BROWSER_NEGATIVE_ACTION_COLOR.copy(
                                                        alpha = if (selected) 0.24f else 0.14f
                                                    )
                                                    else -> MaterialTheme.colorScheme.primaryContainer.copy(
                                                        alpha = if (selected) 0.68f else 0.34f
                                                    )
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        LoadingShimmerOverlay(
                                            active = selected,
                                            tint = if (value == GALLERY_BROWSER_HOLD_ACTION_HIDE) {
                                                GALLERY_BROWSER_NEGATIVE_ACTION_COLOR
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            },
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clip(holdButtonShape)
                                        )
                                        Text(
                                            text = when (value) {
                                                0 -> "Cancel"
                                                GALLERY_BROWSER_HOLD_ACTION_HIDE -> "Hide"
                                                else -> "\u2605"
                                            },
                                            style = if (value == 0 || value == GALLERY_BROWSER_HOLD_ACTION_HIDE) {
                                                MaterialTheme.typography.labelMedium
                                            } else {
                                                MaterialTheme.typography.titleMedium
                                            },
                                            color = if (selected) {
                                                if (value == GALLERY_BROWSER_HOLD_ACTION_HIDE) {
                                                    GALLERY_BROWSER_NEGATIVE_ACTION_COLOR
                                                } else if (value == 0) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    RATING_STAR_GOLD
                                                }
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                                            },
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
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
    }
}
