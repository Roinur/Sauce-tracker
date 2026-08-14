@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.roinur.saucetracker.feature.subscriptions

import com.roinur.saucetracker.core.ui.components.*

import com.roinur.saucetracker.*
import com.roinur.saucetracker.core.media.*
import com.roinur.saucetracker.data.backup.*
import com.roinur.saucetracker.data.downloads.*
import com.roinur.saucetracker.data.backup.*
import com.roinur.saucetracker.data.downloads.*
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
import com.roinur.saucetracker.core.diagnostics.PerformanceMetrics
import com.roinur.saucetracker.core.media.*
import com.roinur.saucetracker.core.media.computeDHash64
import com.roinur.saucetracker.core.network.HttpClientFactory
import com.roinur.saucetracker.core.network.HttpClientProfile
import com.roinur.saucetracker.core.preferences.KEY_ACCENT_MODE
import com.roinur.saucetracker.core.preferences.KEY_ADAPTIVE_SCROLL_THUMBNAILS
import com.roinur.saucetracker.core.preferences.KEY_APP_LOCK_BIOMETRIC_ENABLED
import com.roinur.saucetracker.core.preferences.KEY_APP_LOCK_ENABLED
import com.roinur.saucetracker.core.preferences.KEY_APPLY_BLOCKED_TAGS_HOME
import com.roinur.saucetracker.core.preferences.KEY_APPLY_BLOCKED_TAGS_SEARCH
import com.roinur.saucetracker.core.preferences.KEY_AUTO_BACKUP_TREE_URI
import com.roinur.saucetracker.core.preferences.KEY_BROWSER_DUPLICATE_CHECK_MODE
import com.roinur.saucetracker.core.preferences.KEY_CUNNY_MODE_ARMED
import com.roinur.saucetracker.core.preferences.KEY_DEFAULT_CREATOR_SORT_DIRECTION
import com.roinur.saucetracker.core.preferences.KEY_DEFAULT_CREATOR_SORT_FIELD
import com.roinur.saucetracker.core.preferences.KEY_DEFAULT_ENTRY_SORT_DIRECTION
import com.roinur.saucetracker.core.preferences.KEY_DEFAULT_ENTRY_SORT_FIELD
import com.roinur.saucetracker.core.preferences.KEY_DEFAULT_TAG_SORT_DIRECTION
import com.roinur.saucetracker.core.preferences.KEY_DEFAULT_TAG_SORT_FIELD
import com.roinur.saucetracker.core.preferences.KEY_DESKTOP_BRIDGE_ENABLED
import com.roinur.saucetracker.core.preferences.KEY_DESKTOP_BRIDGE_PORT
import com.roinur.saucetracker.core.preferences.KEY_ENTRY_FILTER_CYCLE_ORDER
import com.roinur.saucetracker.core.preferences.KEY_ENTRY_PIN_PRIORITY
import com.roinur.saucetracker.core.preferences.KEY_EXPERIMENTAL_DASHBOARD_LONG_PRESS
import com.roinur.saucetracker.core.preferences.KEY_EXPERIMENTAL_FILTER_STATUS_STRIP
import com.roinur.saucetracker.core.preferences.KEY_EXPERIMENTAL_LAZY_ENTRY_DETAIL
import com.roinur.saucetracker.core.preferences.KEY_EXPERIMENTAL_SUBSCRIPTION_INBOX
import com.roinur.saucetracker.core.preferences.KEY_GALLERY_COLUMNS
import com.roinur.saucetracker.core.preferences.KEY_HOME_SECTION_ORDER
import com.roinur.saucetracker.core.preferences.KEY_INCOGNITO_MODE_ENABLED
import com.roinur.saucetracker.core.preferences.KEY_LEGACY_HOME_UI
import com.roinur.saucetracker.core.preferences.KEY_PERFORMANCE_OVERLAY_ENABLED
import com.roinur.saucetracker.core.preferences.KEY_PRELOAD_ON_LAUNCH
import com.roinur.saucetracker.core.preferences.KEY_PRELOAD_PERCENT
import com.roinur.saucetracker.core.preferences.KEY_PURE_GALLERY_MODE
import com.roinur.saucetracker.core.preferences.KEY_SHOW_THUMBNAILS
import com.roinur.saucetracker.core.preferences.KEY_SUBSCRIPTION_REFRESH_INTERVAL_HOURS
import com.roinur.saucetracker.core.preferences.KEY_SUGGESTION_HIDDEN_CODES
import com.roinur.saucetracker.core.preferences.KEY_SUGGESTION_HIDDEN_ENTRIES
import com.roinur.saucetracker.core.preferences.KEY_SUGGESTION_THEME_STRENGTH
import com.roinur.saucetracker.core.preferences.KEY_SUGGESTION_WEIGHT_PREFIX
import com.roinur.saucetracker.core.preferences.KEY_THEME_MODE
import com.roinur.saucetracker.core.preferences.SaucePreferences
import com.roinur.saucetracker.core.security.AppLockController
import com.roinur.saucetracker.core.ui.privacy.privacyObfuscate
import com.roinur.saucetracker.data.repository.HeatmapRepository
import com.roinur.saucetracker.data.repository.LibraryRepository
import com.roinur.saucetracker.data.repository.SubscriptionRepository
import com.roinur.saucetracker.data.repository.SuggestionsRepository
import com.roinur.saucetracker.feature.heatmap.HeatmapCanvas
import com.roinur.saucetracker.feature.heatmap.HeatmapEngine
import com.roinur.saucetracker.feature.heatmap.HeatmapScreen
import com.roinur.saucetracker.feature.heatmap.HeatmapLayoutCache
import com.roinur.saucetracker.feature.heatmap.HeatmapThumbnailLoader
import com.roinur.saucetracker.feature.experimentalgallery.ExperimentalGalleryActivity
import com.roinur.saucetracker.feature.slideshow.SlideshowHorizontalDirection
import com.roinur.saucetracker.feature.slideshow.loadSlideshowHorizontalDirection
import com.roinur.saucetracker.feature.slideshow.storeSlideshowHorizontalDirection
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
@Composable
internal fun SubscriptionSwipeDismissContainer(
    eventId: Long,
    isPinned: Boolean,
    onTogglePinned: (Long) -> Unit,
    onDismiss: (Long) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val swipeCommitTracker = rememberSwipeCommitTrackerState(
        key = eventId.toInt(),
        config = SwipeCommitConfig(
            minHorizontalSwipePx = with(density) { 88.dp.toPx() },
            minGestureDurationMs = 160L,
            maxVerticalPerHorizontalRatio = 0.176327f,
            maxSwipeSpeedPxPerMs = 1.15f
        )
    )
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * 0.45f },
        confirmValueChange = { target ->
            if (target != SwipeToDismissBoxValue.Settled && !swipeCommitTracker.deliberate) {
                return@rememberSwipeToDismissBoxState false
            }
            when (target) {
                SwipeToDismissBoxValue.StartToEnd -> onTogglePinned(eventId)
                SwipeToDismissBoxValue.EndToStart -> onDismiss(eventId)
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        }
    )
    val commitReadyTarget = trackSwipeCommitFeedback(
        key = eventId.toInt(),
        tracker = swipeCommitTracker,
        dismissTarget = dismissState.targetValue,
        feedbackEnabled = true,
        haptic = haptic
    )
    val direction = dismissState.dismissDirection
    val progress = dismissState.progress
    val backgroundAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (progress <= 0.001f) 0f else (0.42f + (progress * 0.58f)).coerceIn(0f, 1f),
        label = "subscriptionSwipeAlpha"
    )
    val backgroundColor by androidx.compose.animation.animateColorAsState(
        targetValue = when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> if (isPinned) UNREAD_STATE_COLOR else MaterialTheme.colorScheme.primaryContainer
            SwipeToDismissBoxValue.EndToStart -> UNREAD_STATE_COLOR
            SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "subscriptionSwipeColor"
    )

    SwipeToDismissBox(
        modifier = modifier.trackSwipeCommitGestures(
            gestureKey = eventId.toInt(),
            tracker = swipeCommitTracker
        ),
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(backgroundColor.copy(alpha = backgroundAlpha))
            ) {
                SwipeCommitReadySwoosh(
                    commitReadyTarget = commitReadyTarget,
                    tint = if (direction == SwipeToDismissBoxValue.EndToStart) UNREAD_STATE_COLOR else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.matchParentSize()
                )
                val label = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> if (isPinned) "Unpin" else "Pin"
                    SwipeToDismissBoxValue.EndToStart -> "Dismiss"
                    SwipeToDismissBoxValue.Settled -> ""
                }
                val glyph = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> "\uD83D\uDCCC"
                    SwipeToDismissBoxValue.EndToStart -> "\u2715"
                    SwipeToDismissBoxValue.Settled -> ""
                }
                if (label.isNotBlank()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = glyph, style = MaterialTheme.typography.titleMedium)
                        Text(text = label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        content = {
            Box(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    )
}

@Composable
internal fun SubscriptionEventDetailCard(
    event: SubscriptionEventRow,
    onOpen: () -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Discovered: ${formatStoredUtcTimestampForDisplay(event.discoveredAt)}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        ImmediateActionText(
            label = "Open",
            onAction = onOpen,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
            modifier = Modifier.heightIn(min = 0.dp)
        )
        ImmediateActionText(
            label = "Import",
            onAction = onImport,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
            modifier = Modifier.heightIn(min = 0.dp)
        )
        ImmediateActionText(
            label = "Dismiss",
            onAction = onDismiss,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
            modifier = Modifier.heightIn(min = 0.dp)
        )
    }
}

@Composable
internal fun ModernSubscriptionsPage(
    subscriptionCount: Int,
    events: List<SubscriptionEventRow>,
    selectedEventId: Long?,
    refreshRunning: Boolean,
    incognitoModeEnabled: Boolean,
    preferLowRes: Boolean,
    listState: LazyListState,
    maxHeight: Dp,
    onOpenList: () -> Unit,
    onRefresh: () -> Unit,
    onSelectEvent: (Long) -> Unit,
    onTogglePinned: (Long) -> Unit,
    onDismiss: (Long) -> Unit,
    onOpen: (Int) -> Unit,
    onImport: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Subscriptions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "$subscriptionCount subscription(s) • ${events.size} update(s)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ImmediateActionText(
                    label = "List",
                    onAction = onOpenList,
                    textStyle = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                ImmediateActionText(
                    label = if (refreshRunning) "Checking..." else "Refresh",
                    onAction = onRefresh,
                    textStyle = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        when {
            incognitoModeEnabled -> {
                Text(
                    text = "Subscriptions are hidden while incognito mode is enabled.",
                    modifier = Modifier.privacyObfuscate(
                        enabled = true,
                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            refreshRunning -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        text = "Checking subscribed tags and artists/groups...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (!incognitoModeEnabled) {
            if (events.isEmpty()) {
                Text(
                    text = if (subscriptionCount <= 0) "No subscriptions yet." else "No new subscription updates right now.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ExperimentalSubscriptionInbox(
                    events = events,
                    selectedEventId = selectedEventId,
                    incognitoModeEnabled = incognitoModeEnabled,
                    preferLowRes = preferLowRes,
                    listState = listState,
                    maxHeight = maxHeight,
                    onSelectEvent = onSelectEvent,
                    onTogglePinned = onTogglePinned,
                    onDismiss = onDismiss,
                    onOpen = onOpen,
                    onImport = onImport
                )
            }
        }
    }
}

@Composable
internal fun ExperimentalSubscriptionInbox(
    events: List<SubscriptionEventRow>,
    selectedEventId: Long?,
    incognitoModeEnabled: Boolean,
    preferLowRes: Boolean,
    listState: LazyListState,
    maxHeight: Dp,
    onSelectEvent: (Long) -> Unit,
    onTogglePinned: (Long) -> Unit,
    onDismiss: (Long) -> Unit,
    onOpen: (Int) -> Unit,
    onImport: (Int) -> Unit
) {
    val groupedEvents = remember(events) {
        events
            .sortedWith(compareByDescending<SubscriptionEventRow> { it.pinned }.thenByDescending { it.discoveredAt })
            .groupBy { "${it.routeType}:${it.routeName}" }
            .toList()
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp, max = maxHeight),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedEvents.forEach { (_, groupEvents) ->
            val first = groupEvents.firstOrNull() ?: return@forEach
            item(key = "group_${first.routeType}_${first.routeName}", contentType = "subscription_inbox_group_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = subscriptionRouteDisplayLabel(first.routeType, first.routeName),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = "${groupEvents.size} unread",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            items(
                groupEvents,
                key = { it.id },
                contentType = { "experimental_subscription_inbox_event" }
            ) { event ->
                ExperimentalSubscriptionInboxRow(
                    event = event,
                    selected = selectedEventId == event.id,
                    incognitoModeEnabled = incognitoModeEnabled,
                    preferLowRes = preferLowRes,
                    onSelect = { onSelectEvent(event.id) },
                    onTogglePinned = onTogglePinned,
                    onDismiss = onDismiss,
                    onOpen = { onOpen(event.code) },
                    onImport = { onImport(event.code) }
                )
            }
        }
    }
}

@Composable
internal fun ExperimentalSubscriptionInboxRow(
    event: SubscriptionEventRow,
    selected: Boolean,
    incognitoModeEnabled: Boolean,
    preferLowRes: Boolean,
    onSelect: () -> Unit,
    onTogglePinned: (Long) -> Unit,
    onDismiss: (Long) -> Unit,
    onOpen: () -> Unit,
    onImport: () -> Unit
) {
    val eventInteraction = remember { MutableInteractionSource() }
    SubscriptionSwipeDismissContainer(
        eventId = event.id,
        isPinned = event.pinned,
        onTogglePinned = onTogglePinned,
        onDismiss = onDismiss
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = if (selected) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            } else {
                null
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = eventInteraction,
                    indication = null,
                    onClick = onSelect
                )
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    if (event.thumbnailUrl.isNotBlank()) {
                        ThumbnailImage(
                            thumbnailUrl = event.thumbnailUrl,
                            backupCode = event.code,
                            contentDescription = "Subscription cover for code ${event.code}",
                            obscure = incognitoModeEnabled,
                            preferLowRes = preferLowRes,
                            modifier = Modifier
                                .width(108.dp)
                                .height(72.dp)
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .privacyObfuscate(
                                enabled = incognitoModeEnabled,
                                overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = INCOGNITO_OVERLAY_ALPHA
                                ),
                                blurRadius = 9.dp,
                                cornerRadius = 8.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "#${event.code} • ${event.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subscriptionRouteDisplayLabel(event.routeType, event.routeName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Pages: ${event.numPages} • Uploaded: ${event.uploadDate.ifBlank { "-" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (selected) {
                    SubscriptionEventDetailCard(
                        event = event,
                        onOpen = onOpen,
                        onImport = onImport,
                        onDismiss = { onDismiss(event.id) }
                    )
                }
            }
        }
    }
}

