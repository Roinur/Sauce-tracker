package com.roinur.saucetracker.feature.settings

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
internal fun EntryReadFilterCycleEditor(
    enabledModes: List<EntryReadFilterMode>,
    onReorder: (List<EntryReadFilterMode>) -> Unit,
    onRemove: (EntryReadFilterMode) -> Unit,
    onAdd: (EntryReadFilterMode) -> Unit
) {
    ReorderableToggleListEditor(
        enabledItems = enabledModes,
        allItems = EntryReadFilterMode.entries.toList(),
        itemKey = { it.name },
        itemLabel = ::readableEntryReadFilterModeLabel,
        enabledDescription = { _, index ->
            if (index == 0) "Default start mode" else "Toggles in this order"
        },
        disabledDescription = { "Excluded from the quick toggle" },
        enabledTitle = "Enabled cycle",
        disabledTitle = "Disabled modes",
        minEnabledCount = 1,
        onReorder = onReorder,
        onRemove = onRemove,
        onAdd = onAdd
    )
}

@Composable
internal fun HomeSectionLayoutEditor(
    enabledSections: List<HomeSection>,
    onReorder: (List<HomeSection>) -> Unit,
    onRemove: (HomeSection) -> Unit,
    onAdd: (HomeSection) -> Unit
) {
    ReorderableToggleListEditor(
        enabledItems = enabledSections,
        allItems = HomeSection.entries.toList(),
        itemKey = { it.name },
        itemLabel = ::homeSectionLabel,
        enabledDescription = { _, index ->
            if (index == 0) "First visible section on the home page" else "Visible in this home page order"
        },
        disabledDescription = { "Hidden from the home page" },
        enabledTitle = "Visible sections",
        disabledTitle = "Hidden sections",
        minEnabledCount = 0,
        onReorder = onReorder,
        onRemove = onRemove,
        onAdd = onAdd
    )
}

@Composable
internal fun DashboardDiscoveryPageOrderEditor(
    pages: List<DashboardDiscoveryPage>,
    onReorder: (List<DashboardDiscoveryPage>) -> Unit
) {
    FixedDashboardPageOrderEditor(pages, ::dashboardDiscoveryPageLabel, onReorder)
}

@Composable
internal fun DashboardInsightPageOrderEditor(
    pages: List<DashboardInsightPage>,
    onReorder: (List<DashboardInsightPage>) -> Unit
) {
    FixedDashboardPageOrderEditor(pages, ::dashboardInsightPageLabel, onReorder)
}

@Composable
internal fun HeatmapOverviewPageOrderEditor(
    pages: List<HeatmapOverviewPage>,
    onReorder: (List<HeatmapOverviewPage>) -> Unit
) {
    FixedDashboardPageOrderEditor(pages, ::heatmapOverviewPageLabel, onReorder)
}

@Composable
private fun <T> FixedDashboardPageOrderEditor(
    items: List<T>,
    itemLabel: (T) -> String,
    onReorder: (List<T>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { index, item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = itemLabel(item),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            if (index > 0) onReorder(items.toMutableList().apply {
                                add(index - 1, removeAt(index))
                            })
                        },
                        enabled = index > 0,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) { Text("↑") }
                    TextButton(
                        onClick = {
                            if (index < items.lastIndex) onReorder(items.toMutableList().apply {
                                add(index + 1, removeAt(index))
                            })
                        },
                        enabled = index < items.lastIndex,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) { Text("↓") }
                }
            }
        }
    }
}

@Composable
internal fun <T> ReorderableToggleListEditor(
    enabledItems: List<T>,
    allItems: List<T>,
    itemKey: (T) -> String,
    itemLabel: (T) -> String,
    enabledDescription: (T, Int) -> String,
    disabledDescription: (T) -> String,
    enabledTitle: String,
    disabledTitle: String,
    minEnabledCount: Int,
    onReorder: (List<T>) -> Unit,
    onRemove: (T) -> Unit,
    onAdd: (T) -> Unit
) {
    val normalizedEnabled = enabledItems.distinct()
    var draftItems by remember { mutableStateOf(normalizedEnabled) }
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var draggingStartIndex by remember { mutableStateOf<Int?>(null) }
    var draggingTargetIndex by remember { mutableStateOf<Int?>(null) }
    var rawDragOffsetY by remember { mutableStateOf(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val reorderRowHeight = 78.dp
    val fallbackRowHeightPx = with(density) { reorderRowHeight.toPx() }
    val dragElevationPx = with(density) { 12.dp.toPx() }
    val haptic = LocalHapticFeedback.current
    val disabledItems = allItems.filterNot { it in draftItems }

    fun resetDrag() {
        draggingKey = null
        draggingStartIndex = null
        draggingTargetIndex = null
        rawDragOffsetY = 0f
    }

    fun itemHeightPx(index: Int, items: List<T> = draftItems): Float {
        return if (index in items.indices) fallbackRowHeightPx else fallbackRowHeightPx
    }

    fun moveItem(items: List<T>, fromIndex: Int, toIndex: Int): List<T> {
        if (fromIndex == toIndex) return items
        return items.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
    }

    fun resolveTargetIndex(startIndex: Int, dragOffsetY: Float): Int {
        var targetIndex = startIndex
        if (dragOffsetY > 0f) {
            var crossedDistancePx = 0f
            while (targetIndex < draftItems.lastIndex) {
                val nextHeightPx = itemHeightPx(targetIndex + 1)
                if (dragOffsetY <= crossedDistancePx + (nextHeightPx / 2f)) break
                crossedDistancePx += nextHeightPx
                targetIndex += 1
            }
        } else if (dragOffsetY < 0f) {
            var crossedDistancePx = 0f
            while (targetIndex > 0) {
                val previousHeightPx = itemHeightPx(targetIndex - 1)
                if (-dragOffsetY <= crossedDistancePx + (previousHeightPx / 2f)) break
                crossedDistancePx += previousHeightPx
                targetIndex -= 1
            }
        }
        return targetIndex
    }

    LaunchedEffect(normalizedEnabled) {
        draftItems = normalizedEnabled
        draggingKey?.let { activeKey ->
            val nextIndex = normalizedEnabled.indexOfFirst { itemKey(it) == activeKey }
            if (nextIndex >= 0) {
                draggingStartIndex = nextIndex
                draggingTargetIndex = nextIndex
            } else {
                resetDrag()
            }
        }
    }

    val previewItems = if (draggingStartIndex != null && draggingTargetIndex != null) {
        moveItem(draftItems, draggingStartIndex!!, draggingTargetIndex!!)
    } else {
        draftItems
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = enabledTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        if (draftItems.isEmpty()) {
            Text(
                text = "Nothing visible.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val rowSpacingDp = 10.dp
            val rowSpacingPx = with(density) { rowSpacingDp.toPx() }
            fun slotOffsetPx(items: List<T>, endExclusive: Int): Float {
                var total = 0f
                for (i in 0 until endExclusive) {
                    total += itemHeightPx(i, items)
                    total += rowSpacingPx
                }
                return total
            }
            fun totalHeightPx(items: List<T>): Float {
                if (items.isEmpty()) return 0f
                var total = 0f
                items.forEachIndexed { index, _ ->
                    total += itemHeightPx(index, items)
                    if (index < items.lastIndex) {
                        total += rowSpacingPx
                    }
                }
                return total
            }
            val overlayItem = draggingKey?.let { activeKey ->
                draftItems.firstOrNull { itemKey(it) == activeKey }
            }
            val overlayStartIndex = draggingStartIndex
            val overlayTargetIndex = draggingTargetIndex ?: overlayStartIndex
            val overlayBaseOffsetPx = overlayStartIndex?.let { slotOffsetPx(draftItems, it) } ?: 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { totalHeightPx(previewItems).toDp() })
            ) {
                previewItems.forEachIndexed { index, item ->
                    val key = itemKey(item)
                    key(key) {
                        val isDraggedPlaceholder = draggingKey == key
                        val slotOffset = slotOffsetPx(previewItems, index)
                        val animatedSlotOffsetY by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = slotOffset,
                            animationSpec = androidx.compose.animation.core.spring(
                                stiffness = androidx.compose.animation.core.Spring.StiffnessLow,
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy
                            ),
                            label = "reorderablePlacement"
                        )
                        val displaySlotOffsetY = if (draggingKey != null) {
                            animatedSlotOffsetY
                        } else {
                            slotOffset
                        }
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDraggedPlaceholder) {
                                    Color.Transparent
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isDraggedPlaceholder) {
                                    Color.Transparent
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                                }
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(reorderRowHeight)
                                .offset {
                                    IntOffset(
                                        x = 0,
                                        y = displaySlotOffsetY.roundToInt()
                                    )
                                }
                                .graphicsLayer {
                                    alpha = if (isDraggedPlaceholder) 0f else 1f
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .pointerInput(draftItems, key) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    val startIndex = draftItems.indexOfFirst { itemKey(it) == key }
                                                        .takeIf { it >= 0 }
                                                        ?: return@detectDragGestures
                                                    draggingKey = key
                                                    draggingStartIndex = startIndex
                                                    draggingTargetIndex = startIndex
                                                    rawDragOffsetY = 0f
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    val startIndex = draggingStartIndex ?: return@detectDragGestures
                                                    val previousTargetIndex = draggingTargetIndex ?: startIndex
                                                    val startSlotOffset = slotOffsetPx(draftItems, startIndex)
                                                    val maxOffsetUp = -startSlotOffset
                                                    val maxOffsetDown =
                                                        (totalHeightPx(draftItems) - itemHeightPx(startIndex, draftItems) - startSlotOffset)
                                                            .coerceAtLeast(0f)
                                                    rawDragOffsetY = (rawDragOffsetY + dragAmount.y)
                                                        .coerceIn(maxOffsetUp, maxOffsetDown)
                                                    val nextTargetIndex = resolveTargetIndex(startIndex, rawDragOffsetY)
                                                    if (nextTargetIndex != previousTargetIndex) {
                                                        draggingTargetIndex = nextTargetIndex
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                },
                                                onDragEnd = {
                                                    val startIndex = draggingStartIndex
                                                    val targetIndex = draggingTargetIndex
                                                    if (startIndex != null && targetIndex != null && startIndex != targetIndex) {
                                                        val nextItems = moveItem(draftItems, startIndex, targetIndex)
                                                        draftItems = nextItems
                                                        onReorder(nextItems)
                                                    }
                                                    resetDrag()
                                                },
                                                onDragCancel = { resetDrag() }
                                            )
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "⋮⋮",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = itemLabel(item),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = enabledDescription(item, index),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                TextButton(
                                    onClick = { onRemove(item) },
                                    enabled = draftItems.size > minEnabledCount,
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text("-")
                                }
                            }
                        }
                    }
                }

                if (overlayItem != null && overlayStartIndex != null) {
                    val overlayOffsetY by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = overlayBaseOffsetPx + rawDragOffsetY,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 0),
                        label = "reorderableOverlay"
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(reorderRowHeight)
                            .offset {
                                IntOffset(
                                    x = 0,
                                    y = overlayOffsetY.roundToInt()
                                )
                            }
                            .graphicsLayer {
                                shadowElevation = dragElevationPx
                                scaleX = 1.01f
                                scaleY = 1.01f
                            }
                            .zIndex(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⋮⋮",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = itemLabel(overlayItem),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = enabledDescription(overlayItem, overlayTargetIndex ?: overlayStartIndex),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(
                                onClick = { onRemove(overlayItem) },
                                enabled = draftItems.size > minEnabledCount,
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text("-")
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f),
            thickness = 1.dp
        )

        Text(
            text = disabledTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        if (disabledItems.isEmpty()) {
            Text(
                text = "Nothing hidden.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            disabledItems.forEach { item ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(reorderRowHeight)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Spacer(
                            modifier = Modifier
                                .width(40.dp)
                                .fillMaxHeight()
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = itemLabel(item),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = disabledDescription(item),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(
                            onClick = { onAdd(item) },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text("+")
                        }
                    }
                }
            }
        }
    }
}
