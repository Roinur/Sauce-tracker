package com.roinur.saucetracker.feature.heatmap

import com.roinur.saucetracker.*
import com.roinur.saucetracker.core.media.*
import com.roinur.saucetracker.core.ui.components.*
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

import com.roinur.saucetracker.core.diagnostics.GitHubMediaSession
import com.roinur.saucetracker.core.media.BitmapMemoryCache
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
internal fun HeatmapCanvas(
    snapshot: TagGraphSnapshot,
    selectedTab: TagGraphTab,
    selectedHeatmapDisplayMode: TagHeatmapDisplayMode,
    incognitoModeEnabled: Boolean,
    onTagSelected: (TagGraphNode) -> Unit,
    onEntrySelected: (TagGraphEntryNode, List<String>) -> Unit,
    thumbnailSessionBitmaps: SnapshotStateMap<Int, ImageBitmap>? = null,
    entryLayoutSessionCache: HeatmapLayoutCache? = null,
    referenceEntryLayoutSnapshot: TagGraphSnapshot? = null,
    persistentEntryLayoutProvider: (suspend (String, TagGraphSnapshot) -> TagGraphEntryLayoutResult?)? = null,
    graphViewportHeight: Dp? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isEntryHeatmap = selectedTab == TagGraphTab.HEATMAP && selectedHeatmapDisplayMode == TagHeatmapDisplayMode.ENTRIES
    val githubTagTextMask = GitHubMediaSession.active && selectedHeatmapDisplayMode == TagHeatmapDisplayMode.TAGS
    val isHeatmapGraph = selectedTab == TagGraphTab.HEATMAP
    val initialGraphZoom = when {
        isHeatmapGraph -> 0.46f
        else -> 1f
    }
    val minGraphZoom = initialGraphZoom
    var graphZoom by remember(snapshot, selectedTab, selectedHeatmapDisplayMode) { mutableStateOf(initialGraphZoom) }
    var graphPanX by remember(snapshot, selectedTab, selectedHeatmapDisplayMode) { mutableStateOf(0f) }
    var graphPanY by remember(snapshot, selectedTab, selectedHeatmapDisplayMode) { mutableStateOf(0f) }
    var selectedNodeName by remember(snapshot, selectedTab) { mutableStateOf<String?>(null) }
    var selectedEntryCode by remember(snapshot, selectedTab, selectedHeatmapDisplayMode) { mutableStateOf<Int?>(null) }
    var selectedEntryTitle by remember(snapshot, selectedTab, selectedHeatmapDisplayMode) { mutableStateOf<String?>(null) }
    var selectedEntryCircleTags by remember(snapshot, selectedTab, selectedHeatmapDisplayMode) { mutableStateOf<List<String>>(emptyList()) }
    var familyOutlineVisible by remember(snapshot, selectedTab, selectedHeatmapDisplayMode) { mutableStateOf(false) }
    var strongestNeighborViewEnabled by remember(snapshot, selectedTab, selectedHeatmapDisplayMode) { mutableStateOf(false) }
    var graphInteractionActive by remember(snapshot, selectedTab, selectedHeatmapDisplayMode) { mutableStateOf(false) }
    var graphInteractionEpoch by remember(snapshot, selectedTab, selectedHeatmapDisplayMode) { mutableStateOf(0L) }
    var thumbnailZonePercent by rememberSaveable(snapshot, selectedTab, selectedHeatmapDisplayMode) { mutableStateOf(10) }
    val internalGraphEntrySessionBitmaps = remember { mutableStateMapOf<Int, ImageBitmap>() }
    val graphEntrySessionBitmaps = thumbnailSessionBitmaps ?: internalGraphEntrySessionBitmaps
    val internalEntryLayoutSessionCache = remember { HeatmapLayoutCache(maximumEntries = 8) }
    val resolvedEntryLayoutSessionCache = entryLayoutSessionCache ?: internalEntryLayoutSessionCache
    val nodes = remember(snapshot, selectedTab) {
        snapshot.nodes.sortedByDescending { node ->
            when (selectedTab) {
                TagGraphTab.HEATMAP -> node.localCount.toFloat()
                TagGraphTab.RAW -> (node.localCount + node.popularCount).toFloat()
                TagGraphTab.RATED -> abs(node.ratedSignalSum) + node.localCount.toFloat()
            }
        }
    }
    val labelPriorityNodes = remember(snapshot.nodes) {
        snapshot.nodes.sortedByDescending { it.localCount }
    }
    val labelNodeTarget = when {
        graphZoom >= 6f -> nodes.size
        graphZoom >= 3.5f -> 72
        graphZoom >= 2.1f -> 44
        graphZoom >= 1.35f -> 24
        else -> 12
    }
    val labelNodes = remember(labelPriorityNodes, graphZoom, selectedNodeName) {
        val base = labelPriorityNodes.take(labelNodeTarget).toMutableList()
        selectedNodeName?.let { selected ->
            labelPriorityNodes.firstOrNull { it.normalizedName == selected }?.let { selectedNode ->
                if (base.none { it.normalizedName == selectedNode.normalizedName }) {
                    base += selectedNode
                }
            }
        }
        base
    }
    val strongestNeighborNames = remember(snapshot, selectedNodeName) {
        selectedNodeName?.let { selected ->
            linkedSetOf<String>().apply {
                add(selected)
                addAll(snapshot.strongestNeighborsByTag[selected].orEmpty())
            }
        }.orEmpty()
    }
    val strongestNeighborModeActive =
        selectedTab == TagGraphTab.HEATMAP &&
            selectedHeatmapDisplayMode == TagHeatmapDisplayMode.TAGS &&
            strongestNeighborViewEnabled &&
            selectedNodeName != null
    LaunchedEffect(incognitoModeEnabled) {
        if (incognitoModeEnabled) {
            selectedNodeName = null
            selectedEntryCode = null
            selectedEntryTitle = null
            selectedEntryCircleTags = emptyList()
            strongestNeighborViewEnabled = false
        }
    }
    LaunchedEffect(selectedNodeName, selectedTab, selectedHeatmapDisplayMode) {
        if (selectedNodeName == null || selectedTab != TagGraphTab.HEATMAP || selectedHeatmapDisplayMode != TagHeatmapDisplayMode.TAGS) {
            strongestNeighborViewEnabled = false
        }
    }
    val maxLocal = nodes.maxOfOrNull { it.localCount }?.coerceAtLeast(1) ?: 1
    val pointBaseColor = when (selectedTab) {
        TagGraphTab.HEATMAP -> MaterialTheme.colorScheme.primary
        TagGraphTab.RAW -> MaterialTheme.colorScheme.secondary
        TagGraphTab.RATED -> MaterialTheme.colorScheme.tertiary
    }
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val ratedPositiveColor = MaterialTheme.colorScheme.primary
    val thumbnailPlaceholderColor = MaterialTheme.colorScheme.surfaceVariant
    val selectedEntryBorderColor = MaterialTheme.colorScheme.primary
    val pinnedGraphGlowColor = MaterialTheme.colorScheme.primary
    val defaultEntryBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
    val hiddenCircleFillColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.10f)
    val hiddenCircleStrokeColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f)
    val hiddenCircleTextColor = MaterialTheme.colorScheme.primary
    val thumbnailLoadingRingColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
    val privacyOverlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
    val loadingSpinnerTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "graph-thumb-load")
    val loadingSpinnerAngle by loadingSpinnerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 900),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "graph-thumb-load-angle"
    )
    LaunchedEffect(graphInteractionEpoch) {
        val epoch = graphInteractionEpoch
        if (epoch == 0L) return@LaunchedEffect
        delay(140)
        if (graphInteractionEpoch == epoch) {
            graphInteractionActive = false
        }
    }
    fun TagGraphNode.position(): Pair<Float, Float> = when (selectedTab) {
        TagGraphTab.HEATMAP -> heatX to heatY
        TagGraphTab.RAW -> rawX to rawY
        TagGraphTab.RATED -> ratedX to ratedY
    }

    val tagBaseBounds = remember(nodes, selectedTab) {
        val xs = nodes.map { it.position().first }
        val ys = nodes.map { it.position().second }
        val minX = xs.minOrNull() ?: 0.5f
        val maxX = xs.maxOrNull() ?: 0.5f
        val minY = ys.minOrNull() ?: 0.5f
        val maxY = ys.maxOrNull() ?: 0.5f
        val rawSpanX = (maxX - minX).coerceAtLeast(0.001f)
        val rawSpanY = (maxY - minY).coerceAtLeast(0.001f)
        val marginX = max(0.05f, rawSpanX * 0.10f)
        val marginY = max(0.05f, rawSpanY * 0.10f)
        floatArrayOf(minX - marginX, maxX + marginX, minY - marginY, maxY + marginY)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        var dominantFamilyDebugText = ""
        val graphViewportModifier = if (graphViewportHeight != null) {
            Modifier
                .fillMaxWidth()
                .height(graphViewportHeight)
        } else {
            Modifier
                .fillMaxWidth()
                .weight(1f)
        }
        BoxWithConstraints(
            modifier = graphViewportModifier
                .pointerInput(snapshot, selectedTab, selectedHeatmapDisplayMode) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        graphInteractionActive = true
                        graphInteractionEpoch += 1L
                        val oldZoom = graphZoom
                        val nextZoom = (oldZoom * zoom).coerceIn(minGraphZoom, 22f)
                        val appliedZoomFactor = nextZoom / oldZoom
                        val graphCenterXPx = size.width / 2f
                        val graphCenterYPx = size.height / 2f
                        val centerAdjustmentX = centroid.x - graphCenterXPx - graphPanX
                        val centerAdjustmentY = centroid.y - graphCenterYPx - graphPanY
                        graphPanX += pan.x + (centerAdjustmentX * (1f - appliedZoomFactor))
                        graphPanY += pan.y + (centerAdjustmentY * (1f - appliedZoomFactor))
                        graphZoom = nextZoom
                    }
                }
        ) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val boxWidthPx = with(density) { maxWidth.toPx() }
            val boxHeightPx = with(density) { maxHeight.toPx() }
            val isHeatmapEntries = isEntryHeatmap
            val leftPad = if (isHeatmapEntries) 12f else 54f
            val rightPad = if (isHeatmapEntries) 12f else 54f
            val topPad = if (isHeatmapEntries) 12f else 32f
            val bottomPad = if (isHeatmapEntries) 24f else 42f
            val graphLeftPx = leftPad
            val graphTopPx = topPad
            val graphRightPx = boxWidthPx - rightPad
            val graphBottomPx = boxHeightPx - bottomPad
            val graphWidthPx = (graphRightPx - graphLeftPx).coerceAtLeast(1f)
            val graphHeightPx = (graphBottomPx - graphTopPx).coerceAtLeast(1f)
            val graphCenterXPx = graphLeftPx + (graphWidthPx * 0.5f)
            val graphCenterYPx = graphTopPx + (graphHeightPx * 0.5f)
            val baseEntryThumbSize = 18.dp
            val defaultEntryThumbSizePx = with(density) { baseEntryThumbSize.toPx() }
            val maxHiddenCircleButtonWidthPx = with(density) { 196.dp.toPx() }
            val minHiddenCircleButtonHeightPx = with(density) { 36.dp.toPx() }
            val maxHiddenCircleButtonHeightPx = with(density) { 52.dp.toPx() }
            val entryLayoutSolverWidthPx = 1600f
            val entryLayoutSolverHeightPx =
                (entryLayoutSolverWidthPx * (boxHeightPx / boxWidthPx.coerceAtLeast(1f)))
                    .coerceIn(1080f, 2200f)
            val entryLayoutCacheKey = remember(snapshot) {
                tagGraphEntryLayoutCacheKey(snapshot)
            }
            val referenceEntryLayoutCacheKey = remember(referenceEntryLayoutSnapshot) {
                referenceEntryLayoutSnapshot?.let(::tagGraphEntryLayoutCacheKey)
            }
            val entryLayoutSpacingPx = defaultEntryThumbSizePx * (TAG_GRAPH_ENTRY_SPACING_MULTIPLIER + 0.08f)
            var entryLayoutResult by remember(
                entryLayoutCacheKey,
                isHeatmapEntries,
                defaultEntryThumbSizePx
            ) {
                mutableStateOf<TagGraphEntryLayoutResult?>(if (isHeatmapEntries) null else TagGraphEntryLayoutResult(snapshot.entryNodes, emptyList()))
            }
            var entryLayoutCacheMissing by remember(
                entryLayoutCacheKey,
                isHeatmapEntries
            ) {
                mutableStateOf(false)
            }
            LaunchedEffect(
                referenceEntryLayoutCacheKey,
                selectedTab,
                selectedHeatmapDisplayMode,
                defaultEntryThumbSizePx
            ) {
                if (
                    selectedTab != TagGraphTab.HEATMAP ||
                    selectedHeatmapDisplayMode != TagHeatmapDisplayMode.TAGS
                ) {
                    return@LaunchedEffect
                }
                val referenceSnapshot = referenceEntryLayoutSnapshot ?: return@LaunchedEffect
                if (referenceSnapshot.entryNodes.isEmpty()) {
                    return@LaunchedEffect
                }
                val referenceKey = referenceEntryLayoutCacheKey ?: return@LaunchedEffect
                if (resolvedEntryLayoutSessionCache[referenceKey] != null) {
                    return@LaunchedEffect
                }
                val solvedLayout = persistentEntryLayoutProvider?.invoke(referenceKey, referenceSnapshot)
                    ?: return@LaunchedEffect
                if (resolvedEntryLayoutSessionCache[referenceKey] == null) {
                    resolvedEntryLayoutSessionCache[referenceKey] = solvedLayout
                }
            }
            LaunchedEffect(
                entryLayoutCacheKey,
                isHeatmapEntries,
                defaultEntryThumbSizePx
            ) {
                if (!isHeatmapEntries) {
                    entryLayoutResult = TagGraphEntryLayoutResult(snapshot.entryNodes, emptyList())
                    entryLayoutCacheMissing = false
                } else {
                    val cachedLayout = resolvedEntryLayoutSessionCache[entryLayoutCacheKey]
                    if (cachedLayout != null) {
                        entryLayoutResult = cachedLayout
                        entryLayoutCacheMissing = false
                    } else {
                        val persistedLayout = persistentEntryLayoutProvider?.invoke(entryLayoutCacheKey, snapshot)
                        if (persistedLayout != null) {
                            entryLayoutResult = persistedLayout
                            entryLayoutCacheMissing = false
                            resolvedEntryLayoutSessionCache[entryLayoutCacheKey] = persistedLayout
                            return@LaunchedEffect
                        }
                        val derivedLayout = referenceEntryLayoutCacheKey
                            ?.let { resolvedEntryLayoutSessionCache[it] }
                            ?.takeIf { referenceEntryLayoutSnapshot != null && referenceEntryLayoutSnapshot !== snapshot }
                            ?.let { baseLayout ->
                                HeatmapEngine.deriveLegacyEntryLayoutResultFromBase(
                                    snapshot = snapshot,
                                    baseLayout = baseLayout,
                                    graphWidthPx = entryLayoutSolverWidthPx,
                                    graphHeightPx = entryLayoutSolverHeightPx,
                                    minimumVisualSpacingPx = entryLayoutSpacingPx
                                )
                            }
                        if (derivedLayout != null) {
                            entryLayoutResult = derivedLayout
                            entryLayoutCacheMissing = false
                            resolvedEntryLayoutSessionCache[entryLayoutCacheKey] = derivedLayout
                            return@LaunchedEffect
                        }
                        entryLayoutResult = null
                        entryLayoutCacheMissing = true
                    }
                }
            }
            val resolvedEntryDisplayNodes = entryLayoutResult?.nodes.orEmpty()
            val resolvedFamilyCircles = entryLayoutResult?.familyCircles.orEmpty()
            val entryLayoutReady = !isHeatmapEntries || entryLayoutResult != null
            val baseBounds = if (isHeatmapEntries && entryLayoutReady) {
                val xs = resolvedEntryDisplayNodes.map { it.x }
                val ys = resolvedEntryDisplayNodes.map { it.y }
                val minX = xs.minOrNull() ?: 0.5f
                val maxX = xs.maxOrNull() ?: 0.5f
                val minY = ys.minOrNull() ?: 0.5f
                val maxY = ys.maxOrNull() ?: 0.5f
                val rawSpanX = (maxX - minX).coerceAtLeast(0.001f)
                val rawSpanY = (maxY - minY).coerceAtLeast(0.001f)
                val marginX = max(0.03f, rawSpanX * 0.07f)
                val marginY = max(0.03f, rawSpanY * 0.07f)
                floatArrayOf(minX - marginX, maxX + marginX, minY - marginY, maxY + marginY)
            } else {
                tagBaseBounds
            }

            val baseMinX = baseBounds[0]
            val baseMaxX = baseBounds[1]
            val baseMinY = baseBounds[2]
            val baseMaxY = baseBounds[3]
            val baseSpanX = (baseMaxX - baseMinX).coerceAtLeast(0.001f)
            val baseSpanY = (baseMaxY - baseMinY).coerceAtLeast(0.001f)
            val baseCenterX = (baseMinX + baseMaxX) * 0.5f
            val baseCenterY = (baseMinY + baseMaxY) * 0.5f
            val viewportScale = if (isHeatmapGraph) 2f else 1f
            val virtualWidthPx = graphWidthPx * viewportScale
            val virtualHeightPx = graphHeightPx * viewportScale
            val virtualLeftPx = graphCenterXPx - (virtualWidthPx * 0.5f)
            val virtualTopPx = graphCenterYPx - (virtualHeightPx * 0.5f)
            val baseScaleX = virtualWidthPx / baseSpanX
            val baseScaleY = virtualHeightPx / baseSpanY
            val isotropicBaseScale =
                if (isHeatmapEntries) {
                    min(virtualWidthPx / baseSpanX, virtualHeightPx / baseSpanY)
                } else {
                    val squareVirtualSizePx = min(graphWidthPx, graphHeightPx) * viewportScale
                    min(squareVirtualSizePx / baseSpanX, squareVirtualSizePx / baseSpanY)
                }
            val transformScaleX = if (isHeatmapGraph) {
                isotropicBaseScale * graphZoom
            } else {
                baseScaleX * graphZoom
            }
            val transformScaleY = if (isHeatmapGraph) {
                -isotropicBaseScale * graphZoom
            } else {
                -baseScaleY * graphZoom
            }
            val transformOffsetX =
                if (isHeatmapGraph) {
                    graphCenterXPx - (baseCenterX * transformScaleX) + graphPanX
                } else {
                    graphCenterXPx + (((virtualLeftPx - (baseMinX * baseScaleX)) - graphCenterXPx) * graphZoom) + graphPanX
                }
            val transformOffsetY =
                if (isHeatmapGraph) {
                    graphCenterYPx - (baseCenterY * transformScaleY) + graphPanY
                } else {
                    graphCenterYPx + ((((virtualTopPx + virtualHeightPx) + (baseMinY * baseScaleY)) - graphCenterYPx) * graphZoom) + graphPanY
                }

            fun transformPoint(xFraction: Float, yFraction: Float): Offset {
                return Offset(
                    transformOffsetX + (xFraction * transformScaleX),
                    transformOffsetY + (yFraction * transformScaleY)
                )
            }
            data class EntryCircleRenderNode(
                val key: String,
                val label: String,
                val centerXFraction: Float,
                val centerYFraction: Float,
                val boundaryRadiusPx: Float,
                val entryCount: Int
            )
            data class EntryHeatmapDrawNode(
                val familyKey: String,
                val code: Int,
                val title: String,
                val dominantCircleTags: List<String>,
                val center: Offset,
                val left: Float,
                val top: Float,
                val bitmap: ImageBitmap?,
                val thumbnailRequested: Boolean,
                val rating: Int,
                val isRead: Boolean,
                val pinned: Boolean,
                val isSelected: Boolean
            )
            data class EntryHeatmapHitTarget(
                val code: Int,
                val title: String,
                val dominantCircleTags: List<String>,
                val center: Offset,
                val radiusPx: Float
            )
            data class TagGraphHitTarget(
                val normalizedName: String,
                val center: Offset
            )
            fun hiddenCircleChipSize(circle: EntryCircleRenderNode): Pair<Float, Float> {
                val estimatedTextWidth = (circle.label.length * 8.0f) + 26f
                val width = estimatedTextWidth
                    .coerceIn(56f, maxHiddenCircleButtonWidthPx)
                val height = 34f.coerceIn(minHiddenCircleButtonHeightPx, maxHiddenCircleButtonHeightPx)
                return width to height
            }
            val entryCircleRenderNodes = remember(resolvedFamilyCircles, isHeatmapEntries, entryLayoutReady) {
                if (isHeatmapEntries && entryLayoutReady) {
                    resolvedFamilyCircles
                        .sortedByDescending { it.entryCount }
                        .map { circle ->
                            EntryCircleRenderNode(
                                key = circle.tagName,
                                label = circle.label,
                                centerXFraction = circle.centerX,
                                centerYFraction = circle.centerY,
                                boundaryRadiusPx = circle.radiusPx,
                                entryCount = circle.entryCount
                            )
                        }
                } else {
                    emptyList()
                }
            }
            dominantFamilyDebugText =
                if (isEntryHeatmap && entryCircleRenderNodes.isNotEmpty()) {
                    entryCircleRenderNodes
                        .sortedByDescending { it.entryCount }
                        .map { it.label }
                        .distinct()
                        .take(12)
                        .joinToString("  •  ")
                } else {
                    ""
                }
            val thumbnailZoneEntries = remember(
                resolvedEntryDisplayNodes,
                thumbnailZonePercent,
                graphLeftPx,
                graphRightPx,
                graphTopPx,
                graphBottomPx,
                transformOffsetX,
                transformOffsetY,
                transformScaleX,
                transformScaleY
            ) {
                if (!isHeatmapEntries || !entryLayoutReady) {
                    emptyList()
                } else {
                    val center = Offset(
                        (graphLeftPx + graphRightPx) * 0.5f,
                        (graphTopPx + graphBottomPx) * 0.5f
                    )
                    resolvedEntryDisplayNodes
                        .asSequence()
                        .filter { it.thumbnailUrl.isNotBlank() }
                        .filter { entry ->
                            val point = transformPoint(entry.x, entry.y)
                            isInsideCenteredThumbnailZone(
                                x = point.x,
                                y = point.y,
                                left = graphLeftPx,
                                right = graphRightPx,
                                top = graphTopPx,
                                bottom = graphBottomPx,
                                percent = thumbnailZonePercent
                            )
                        }
                        .sortedWith(
                            compareByDescending<TagGraphEntryNode> { it.isRead }
                                .thenByDescending { it.pinned }
                                .thenBy { entry ->
                                    val point = transformPoint(entry.x, entry.y)
                                    val dx = point.x - center.x
                                    val dy = point.y - center.y
                                    (dx * dx) + (dy * dy)
                                }
                                .thenBy { it.code }
                        )
                        .distinctBy { it.code }
                        .toList()
                }
            }
            val thumbnailZoneCodes = remember(thumbnailZoneEntries) {
                thumbnailZoneEntries.asSequence().map { it.code }.toSet()
            }
            LaunchedEffect(
                thumbnailZoneEntries,
                graphInteractionActive,
                incognitoModeEnabled,
                isHeatmapEntries
            ) {
                if (!isHeatmapEntries || incognitoModeEnabled) {
                    graphEntrySessionBitmaps.clear()
                    return@LaunchedEffect
                }
                if (graphInteractionActive) return@LaunchedEffect

                graphEntrySessionBitmaps.keys
                    .filter { it !in thumbnailZoneCodes }
                    .forEach(graphEntrySessionBitmaps::remove)

                val candidates = thumbnailZoneEntries.filter { entry ->
                    graphEntrySessionBitmaps[entry.code] == null
                }
                val parallelism = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
                val resolved = HeatmapThumbnailLoader.load(candidates, parallelism) { entry ->
                    withContext(Dispatchers.IO) {
                        ThumbnailBitmapCache.get(entry.thumbnailUrl, lowRes = true)
                            ?: ThumbnailBitmapCache.get(entry.thumbnailUrl)
                            ?: fetchGraphThumbnailBitmap(
                                context = context.applicationContext,
                                url = entry.thumbnailUrl,
                                backupCode = entry.code
                            )
                    }
                }
                if (resolved.isNotEmpty()) {
                    graphEntrySessionBitmaps.putAll(resolved)
                }
            }
            val resolvedEntryBitmaps = remember(thumbnailZoneEntries, graphEntrySessionBitmaps.size) {
                buildMap<Int, ImageBitmap> {
                    thumbnailZoneEntries.forEach { entry ->
                        val bitmap = graphEntrySessionBitmaps[entry.code]
                        if (bitmap != null) {
                            put(entry.code, bitmap)
                        }
                    }
                }
            }
            val entryThumbRenderSizePx = if (isHeatmapEntries) defaultEntryThumbSizePx * graphZoom else 0f
            val entryThumbRenderRadiusPx = entryThumbRenderSizePx * 0.5f
            val entryThumbRenderSizeInt = entryThumbRenderSizePx.roundToInt().coerceAtLeast(8)
            fun renderedCircleRadiusPx(
                circle: EntryCircleRenderNode,
                thumbRadiusPx: Float = (defaultEntryThumbSizePx * graphZoom * 0.5f)
            ): Float {
                val familyCount = circle.entryCount.coerceAtLeast(1)
                val countScaledMinimumRadius = when (familyCount) {
                    1 -> thumbRadiusPx * 1.05f
                    2 -> thumbRadiusPx * 1.22f
                    3 -> thumbRadiusPx * 1.38f
                    else -> thumbRadiusPx * (1.24f + (sqrt(familyCount.toFloat()) * 0.24f))
                }
                val center = transformPoint(circle.centerXFraction, circle.centerYFraction)
                val edgePoint = transformPoint(
                    circle.centerXFraction + (circle.boundaryRadiusPx / entryLayoutSolverWidthPx.coerceAtLeast(1f)),
                    circle.centerYFraction
                )
                val solverRadius = abs(edgePoint.x - center.x)
                return max(solverRadius, countScaledMinimumRadius)
            }
            val entryHeatmapDrawNodes = remember(
                resolvedEntryDisplayNodes,
                resolvedEntryBitmaps,
                thumbnailZoneCodes,
                selectedEntryCode,
                entryThumbRenderSizePx,
                graphLeftPx,
                graphRightPx,
                graphTopPx,
                graphBottomPx,
                transformOffsetX,
                transformOffsetY,
                transformScaleX,
                transformScaleY
            ) {
                if (!isHeatmapEntries || !entryLayoutReady) {
                    emptyList()
                } else {
                    buildList {
                        var selectedDrawNode: EntryHeatmapDrawNode? = null
                        resolvedEntryDisplayNodes.forEach entryLoop@{ entry ->
                            val point = transformPoint(entry.x, entry.y)
                            val left = point.x - entryThumbRenderRadiusPx
                            val top = point.y - entryThumbRenderRadiusPx
                            if (
                                left > graphRightPx + entryThumbRenderSizePx ||
                                point.x + entryThumbRenderRadiusPx < graphLeftPx - entryThumbRenderSizePx ||
                                top > graphBottomPx + entryThumbRenderSizePx ||
                                point.y + entryThumbRenderRadiusPx < graphTopPx - entryThumbRenderSizePx
                            ) {
                                return@entryLoop
                            }
                            val drawNode = EntryHeatmapDrawNode(
                                familyKey = entry.dominantCircleTags.sorted().joinToString("|"),
                                code = entry.code,
                                title = entry.title,
                                dominantCircleTags = entry.dominantCircleTags,
                                center = point,
                                left = left,
                                top = top,
                                bitmap = resolvedEntryBitmaps[entry.code],
                                thumbnailRequested = entry.code in thumbnailZoneCodes,
                                rating = entry.rating,
                                isRead = entry.isRead,
                                pinned = entry.pinned,
                                isSelected = selectedEntryCode == entry.code
                            )
                            if (drawNode.isSelected) {
                                selectedDrawNode = drawNode
                            } else {
                                add(drawNode)
                            }
                        }
                        selectedDrawNode?.let(::add)
                    }
                }
            }
            val entryHeatmapHitTargets = remember(entryHeatmapDrawNodes, entryThumbRenderRadiusPx) {
                entryHeatmapDrawNodes
                    .asReversed()
                    .asSequence()
                    .map { drawNode ->
                        EntryHeatmapHitTarget(
                            code = drawNode.code,
                            title = drawNode.title,
                            dominantCircleTags = drawNode.dominantCircleTags,
                            center = drawNode.center,
                            radiusPx = entryThumbRenderRadiusPx
                        )
                    }
                    .toList()
            }
            val drawLightweightEntryNodes =
                isHeatmapEntries &&
                    graphInteractionActive
            val tagGraphHitTargets = remember(
                nodes,
                selectedTab,
                transformOffsetX,
                transformOffsetY,
                transformScaleX,
                transformScaleY
            ) {
                nodes.map { node ->
                    val (xFraction, yFraction) = node.position()
                    TagGraphHitTarget(
                        normalizedName = node.normalizedName,
                        center = transformPoint(xFraction, yFraction)
                    )
                }
            }
            val thumbClipPath = remember { Path() }
            val labelPaint = remember {
                android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 24f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                }
            }
            val privacyLabelPaint = remember {
                android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    style = android.graphics.Paint.Style.FILL
                }
            }
            val axisPaint = remember {
                android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 23f
                }
            }
            val privacyLabelColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f)
            SideEffect {
                labelPaint.color = textColor.toArgb()
                privacyLabelPaint.color = privacyLabelColor.toArgb()
                axisPaint.color = axisColor.toArgb()
            }

            val currentEntryHeatmapHitTargets by rememberUpdatedState(entryHeatmapHitTargets)
            val currentTagGraphHitTargets by rememberUpdatedState(tagGraphHitTargets)
            val currentIsHeatmapEntries by rememberUpdatedState(isHeatmapEntries)
            val currentOnTagSelected by rememberUpdatedState(onTagSelected)
            val currentOnEntrySelected by rememberUpdatedState(onEntrySelected)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(
                        snapshot,
                        selectedTab,
                        selectedHeatmapDisplayMode,
                        incognitoModeEnabled
                    ) {
                        if (incognitoModeEnabled) return@pointerInput
                        detectTapGestures(
                            onTap = { tapOffset ->
                                if (currentIsHeatmapEntries) {
                                    val tappedEntry = currentEntryHeatmapHitTargets.firstOrNull { entry ->
                                        val dx = entry.center.x - tapOffset.x
                                        val dy = entry.center.y - tapOffset.y
                                        ((dx * dx) + (dy * dy)) <= (entry.radiusPx * entry.radiusPx)
                                    }
                                    if (tappedEntry != null) {
                                        selectedEntryCode = tappedEntry.code
                                        selectedEntryTitle = tappedEntry.title
                                        selectedEntryCircleTags = tappedEntry.dominantCircleTags
                                        snapshot.entryNodes
                                            .firstOrNull { it.code == tappedEntry.code }
                                            ?.let { currentOnEntrySelected(it, tappedEntry.dominantCircleTags) }
                                    }
                                } else {
                                    val nearest = currentTagGraphHitTargets.minByOrNull { node ->
                                        val dx = node.center.x - tapOffset.x
                                        val dy = node.center.y - tapOffset.y
                                        (dx * dx) + (dy * dy)
                                    }
                                    selectedNodeName = nearest?.normalizedName
                                    nearest?.normalizedName?.let { normalizedName ->
                                        snapshot.nodes
                                            .firstOrNull { it.normalizedName == normalizedName }
                                            ?.let(currentOnTagSelected)
                                    }
                                }
                            }
                        )
                    }
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                if (!isHeatmapEntries) {
                    nodes.forEach { node ->
                        if (strongestNeighborModeActive && node.normalizedName !in strongestNeighborNames) {
                            return@forEach
                        }
                        val (xFraction, yFraction) = node.position()
                        val center = transformPoint(xFraction, yFraction)
                        val isSelected = node.normalizedName == selectedNodeName
                        if (
                            center.x < graphLeftPx - 42f ||
                            center.x > graphRightPx + 42f ||
                            center.y < graphTopPx - 42f ||
                            center.y > graphBottomPx + 42f
                        ) {
                            return@forEach
                        }
                        val radius = (4.5f + (8.5f * sqrt(node.localCount.toFloat() / maxLocal.toFloat()))) *
                            graphZoom.coerceIn(1f, 4.6f)
                        val fillColor = when (selectedTab) {
                            TagGraphTab.RATED -> {
                                when {
                                    node.ratedSignalSum > 0.25f -> ratedPositiveColor
                                    node.ratedSignalSum < -0.25f -> UNREAD_STATE_COLOR
                                    else -> pointBaseColor.copy(alpha = 0.75f)
                                }
                            }
                            else -> pointBaseColor.copy(alpha = 0.84f)
                        }
                        drawCircle(
                            color = fillColor.copy(alpha = if (isSelected) 0.24f else 0.16f),
                            radius = radius * if (isSelected) 2.7f else 2.25f,
                            center = center
                        )
                        drawCircle(
                            color = fillColor,
                            radius = if (isSelected) radius * 1.16f else radius,
                            center = center
                        )
                    }
                } else if (entryLayoutReady) {
                    entryHeatmapDrawNodes.forEach { entry ->
                        val point = entry.center
                        val thumbRadius = entryThumbRenderRadiusPx
                        if (incognitoModeEnabled) {
                            drawCircle(
                                color = privacyOverlayColor.copy(alpha = 0.92f),
                                radius = thumbRadius,
                                center = point
                            )
                            drawCircle(
                                color = defaultEntryBorderColor.copy(alpha = 0.8f),
                                radius = thumbRadius,
                                center = point,
                                style = Stroke(width = 1.2f)
                            )
                            return@forEach
                        }
                        val hueColor = when {
                            entry.rating > 0 -> RATING_STAR_GOLD
                            entry.isRead -> READ_STATE_COLOR
                            else -> null
                        }
                        if (entry.pinned) {
                            val pinGlowScale = graphZoom.coerceIn(1f, 2.4f)
                            drawCircle(
                                color = pinnedGraphGlowColor.copy(alpha = 0.16f),
                                radius = thumbRadius + (6.6f * pinGlowScale),
                                center = point
                            )
                            drawCircle(
                                color = pinnedGraphGlowColor.copy(alpha = 0.10f),
                                radius = thumbRadius + (3.8f * pinGlowScale),
                                center = point
                            )
                            drawCircle(
                                color = pinnedGraphGlowColor.copy(alpha = 0.52f),
                                radius = thumbRadius + (0.95f * pinGlowScale),
                                center = point,
                                style = Stroke(width = 1.45f * pinGlowScale.coerceAtMost(1.7f))
                            )
                        }
                        if (drawLightweightEntryNodes || !entry.thumbnailRequested) {
                            val markerRadius = (thumbRadius * 0.42f).coerceAtLeast(3.8f)
                            val markerColor = hueColor?.copy(alpha = 0.92f)
                                ?: if (entry.bitmap != null) {
                                    pointBaseColor.copy(alpha = 0.78f)
                                } else {
                                    thumbnailPlaceholderColor.copy(alpha = 0.92f)
                                }
                            drawCircle(
                                color = markerColor.copy(alpha = 0.20f),
                                radius = markerRadius * 1.9f,
                                center = point
                            )
                            drawCircle(
                                color = markerColor,
                                radius = markerRadius,
                                center = point
                            )
                            if (entry.isSelected) {
                                drawCircle(
                                    color = selectedEntryBorderColor,
                                    radius = markerRadius * 1.48f,
                                    center = point,
                                    style = Stroke(width = 1.6f)
                                )
                            }
                            return@forEach
                        }
                        if (hueColor != null) {
                            val ratingFactor = if (entry.rating > 0) {
                                entry.rating.coerceIn(1, 5) / 5f
                            } else {
                                0f
                            }
                            val glowScale = graphZoom.coerceIn(1f, 2.4f)
                            val innerGlowRadius = thumbRadius + if (entry.rating > 0) {
                                (6.0f * ratingFactor) * glowScale
                            } else {
                                4.4f * glowScale
                            }
                            val outerGlowRadius = thumbRadius + if (entry.rating > 0) {
                                (10.8f * ratingFactor) * glowScale
                            } else {
                                7.4f * glowScale
                            }
                            val outerAlpha = if (entry.rating > 0) {
                                0.12f + (0.13f * ratingFactor)
                            } else {
                                0.24f
                            }
                            val innerAlpha = if (entry.rating > 0) {
                                0.10f + (0.11f * ratingFactor)
                            } else {
                                0.18f
                            }
                            drawCircle(
                                color = hueColor.copy(alpha = outerAlpha),
                                radius = outerGlowRadius,
                                center = point
                            )
                            drawCircle(
                                color = hueColor.copy(alpha = innerAlpha),
                                radius = innerGlowRadius,
                                center = point
                            )
                            drawCircle(
                                color = hueColor.copy(alpha = if (entry.rating > 0) 0.44f + (0.22f * ratingFactor) else 0.54f),
                                radius = thumbRadius + (1.05f * glowScale),
                                center = point,
                                style = Stroke(
                                    width = (
                                        if (entry.rating > 0) {
                                            1.3f + (0.4f * ratingFactor)
                                        } else {
                                            1.75f
                                        }
                                        ) * glowScale.coerceAtMost(1.8f)
                                )
                            )
                        }
                        if (entry.bitmap != null) {
                            thumbClipPath.reset()
                            thumbClipPath.addOval(
                                androidx.compose.ui.geometry.Rect(
                                    left = entry.left,
                                    top = entry.top,
                                    right = entry.left + entryThumbRenderSizePx,
                                    bottom = entry.top + entryThumbRenderSizePx
                                )
                            )
                            clipPath(thumbClipPath) {
                                drawImage(
                                    image = entry.bitmap,
                                    dstOffset = IntOffset(entry.left.roundToInt(), entry.top.roundToInt()),
                                    dstSize = IntSize(entryThumbRenderSizeInt, entryThumbRenderSizeInt)
                                )
                            }
                        } else {
                            drawCircle(
                                color = thumbnailPlaceholderColor,
                                radius = thumbRadius,
                                center = point
                            )
                            drawArc(
                                color = thumbnailLoadingRingColor,
                                startAngle = loadingSpinnerAngle,
                                sweepAngle = 220f,
                                useCenter = false,
                                topLeft = Offset(
                                    point.x - (entryThumbRenderSizePx * 0.18f),
                                    point.y - (entryThumbRenderSizePx * 0.18f)
                                ),
                                size = androidx.compose.ui.geometry.Size(
                                    entryThumbRenderSizePx * 0.36f,
                                    entryThumbRenderSizePx * 0.36f
                                ),
                                style = Stroke(
                                    width = (entryThumbRenderSizePx * 0.08f).coerceAtLeast(1.4f),
                                    cap = StrokeCap.Round
                                )
                            )
                        }
                        drawCircle(
                            color = if (entry.isSelected) selectedEntryBorderColor else defaultEntryBorderColor,
                            radius = thumbRadius,
                            center = point,
                            style = Stroke(width = 1.2f)
                        )
                    }
                }

                drawContext.canvas.nativeCanvas.apply {
                    if (!isHeatmapEntries && !incognitoModeEnabled) {
                        val renderedLabelNodes = if (strongestNeighborModeActive) {
                            labelPriorityNodes.filter { it.normalizedName in strongestNeighborNames }
                        } else {
                            labelNodes
                        }
                        renderedLabelNodes.forEach { node ->
                            val (xFraction, yFraction) = node.position()
                            val point = transformPoint(xFraction, yFraction)
                            if (
                                point.x < graphLeftPx ||
                                point.x > graphRightPx ||
                                point.y < graphTopPx ||
                                point.y > graphBottomPx
                            ) {
                                return@forEach
                            }
                            val isSelected = node.normalizedName == selectedNodeName
                            val verticalOffset = if (graphZoom >= 2.1f || isSelected) 6f else 18f
                            if (isSelected) {
                                labelPaint.textSize = 28f
                            } else {
                                labelPaint.textSize = 24f
                            }
                            val textWidth = labelPaint.measureText(node.name)
                            val centeredX = point.x - (textWidth / 2f)
                            val labelX = centeredX.coerceIn(graphLeftPx + 4f, graphRightPx - textWidth - 4f)
                            val labelY = (point.y - verticalOffset).coerceIn(graphTopPx + 20f, graphBottomPx - 10f)
                            if (githubTagTextMask) {
                                val cornerRadius = labelPaint.textSize * 0.32f
                                drawRoundRect(
                                    labelX,
                                    labelY - (labelPaint.textSize * 0.82f),
                                    labelX + textWidth,
                                    labelY + (labelPaint.textSize * 0.08f),
                                    cornerRadius,
                                    cornerRadius,
                                    privacyLabelPaint
                                )
                            } else {
                                drawText(node.name, labelX, labelY, labelPaint)
                            }
                        }
                    } else if (entryLayoutReady) {
                        if (familyOutlineVisible) {
                            entryCircleRenderNodes.forEach { circle ->
                                val center = transformPoint(circle.centerXFraction, circle.centerYFraction)
                                val renderRadius = renderedCircleRadiusPx(circle, defaultEntryThumbSizePx * graphZoom * 0.5f)
                                if (
                                    center.x + renderRadius < graphLeftPx ||
                                    center.x - renderRadius > graphRightPx ||
                                    center.y + renderRadius < graphTopPx ||
                                    center.y - renderRadius > graphBottomPx
                                ) {
                                    return@forEach
                                }
                                if (!incognitoModeEnabled) {
                                    drawCircle(
                                        color = hiddenCircleFillColor,
                                        radius = renderRadius,
                                        center = center
                                    )
                                }
                                drawCircle(
                                    color = hiddenCircleStrokeColor,
                                    radius = renderRadius,
                                    center = center,
                                    style = Stroke(width = 1.6f)
                                )
                                if (!incognitoModeEnabled) {
                                    labelPaint.textSize = 24f
                                    labelPaint.color = hiddenCircleTextColor.toArgb()
                                    val textWidth = labelPaint.measureText(circle.label)
                                    drawText(
                                        circle.label,
                                        center.x - (textWidth * 0.5f),
                                        center.y + 8f,
                                        labelPaint
                                    )
                                    labelPaint.color = textColor.toArgb()
                                }
                            }
                        }
                    }

                    when (selectedTab) {
                        TagGraphTab.HEATMAP -> Unit
                        TagGraphTab.RAW, TagGraphTab.RATED -> {
                            val commonAvoided = "Common Avoided"
                            val commonLiked = "Common Liked"
                            val obscureAvoided = "Obscure Avoided"
                            val obscureLiked = "Obscure Liked"
                            val commonLikedWidth = axisPaint.measureText(commonLiked)
                            val obscureLikedWidth = axisPaint.measureText(obscureLiked)
                            drawText(commonAvoided, graphLeftPx + 6f, graphTopPx + 22f, axisPaint)
                            drawText(commonLiked, graphRightPx - commonLikedWidth - 6f, graphTopPx + 22f, axisPaint)
                            drawText(obscureAvoided, graphLeftPx + 6f, graphBottomPx - 10f, axisPaint)
                            drawText(obscureLiked, graphRightPx - obscureLikedWidth - 6f, graphBottomPx - 10f, axisPaint)
                        }
                    }
                }
                }
                if (isHeatmapEntries && !entryLayoutReady) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (entryLayoutCacheMissing) {
                            Text(
                                text = "No cached entry heatmap layout yet.\nRecalculate it in Settings > Data.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
        if (isEntryHeatmap) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thumbnail area",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(10, 25, 50, 100).forEach { percent ->
                    FilterChip(
                        selected = thumbnailZonePercent == percent,
                        onClick = { thumbnailZonePercent = percent },
                        label = { Text("$percent%") }
                    )
                }
            }
        }
        val shouldShowReset = graphZoom > minGraphZoom + 0.02f || abs(graphPanX) > 1f || abs(graphPanY) > 1f
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val entryDetailScroll = rememberScrollState()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .horizontalScroll(entryDetailScroll),
                contentAlignment = Alignment.CenterStart
            ) {
                if (incognitoModeEnabled) {
                    Text(
                        text = "Incognito mode hides graph details.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (!isEntryHeatmap) {
                    selectedNodeName?.let { selected ->
                        nodes.firstOrNull { it.normalizedName == selected }?.let { node ->
                            val localShare = node.localCount.toFloat() / snapshot.totalEntries.coerceAtLeast(1).toFloat()
                            val globalShare = node.popularCount.toFloat() / snapshot.totalPopularTagUsage.coerceAtLeast(1L).toFloat()
                            val localVsGlobalText = when {
                                node.popularCount <= 0 -> "No global popularity cached yet"
                                localShare >= globalShare -> {
                                    val ratio = (localShare / globalShare.coerceAtLeast(0.0000001f)).coerceAtLeast(1f)
                                    "Locally ${"%.1f".format(Locale.US, ratio)}x more common"
                                }
                                else -> {
                                    val ratio = (globalShare / localShare.coerceAtLeast(0.0000001f)).coerceAtLeast(1f)
                                    "Locally ${"%.1f".format(Locale.US, ratio)}x less common"
                                }
                            }
                            val detailText = when (selectedTab) {
                                TagGraphTab.HEATMAP -> "Local count ${node.localCount} | Popular ${node.popularCount} | $localVsGlobalText"
                                TagGraphTab.RAW -> "Library count ${node.localCount} | Popular ${node.popularCount} | $localVsGlobalText"
                                TagGraphTab.RATED -> "Rated signal ${"%.2f".format(Locale.US, node.ratedSignalSum)} | Rated mentions ${node.ratedMentionCount} | $localVsGlobalText"
                            }
                            Text(
                                text = "${node.name}  |  $detailText",
                                modifier = Modifier.privacyObfuscate(
                                    enabled = false,
                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f),
                                    expandHorizontal = 0.dp,
                                    expandVertical = 0.dp,
                                    cornerRadius = 6.dp
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                } else if (selectedEntryCode != null) {
                    Text(
                        text = selectedEntryTitle ?: "Entry",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                } else if (isEntryHeatmap && dominantFamilyDebugText.isNotBlank()) {
                    Text(
                        text = "Families: $dominantFamilyDebugText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            if (shouldShowReset) {
                TextButton(
                    onClick = {
                        graphZoom = initialGraphZoom
                        graphPanX = 0f
                        graphPanY = 0f
                        graphInteractionActive = false
                        graphInteractionEpoch = 0L
                        selectedNodeName = null
                        selectedEntryCode = null
                        selectedEntryTitle = null
                        selectedEntryCircleTags = emptyList()
                    }
                ) {
                    Text("Reset View")
                }
            }
            if (isEntryHeatmap) {
                TextButton(
                    onClick = {
                        familyOutlineVisible = !familyOutlineVisible
                    }
                ) {
                    Text(if (familyOutlineVisible) "Family Outline: On" else "Family Outline: Off")
                }
            } else if (!incognitoModeEnabled && selectedTab == TagGraphTab.HEATMAP && selectedHeatmapDisplayMode == TagHeatmapDisplayMode.TAGS && selectedNodeName != null) {
                TextButton(
                    onClick = {
                        strongestNeighborViewEnabled = !strongestNeighborViewEnabled
                    }
                ) {
                    Text(if (strongestNeighborViewEnabled) "Strongest Neighbors: On" else "Strongest Neighbors")
                }
            }
        }
    }
}

