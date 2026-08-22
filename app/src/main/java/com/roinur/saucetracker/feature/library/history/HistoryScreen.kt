package com.roinur.saucetracker.feature.library.history

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
internal fun ReadingHistoryPreviewPanel(
    analyticsSnapshot: ReadAnalyticsSnapshot,
    analyticsLoading: Boolean,
    interactionSource: MutableInteractionSource,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthReads = analyticsSnapshot.readCounts[StatsRange.MONTH] ?: 0
    val monthPages = analyticsSnapshot.pagesRead[StatsRange.MONTH] ?: 0
    val monthSpeed = analyticsSnapshot.readingSpeed[StatsRange.MONTH] ?: ReadingSpeedStats()
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onOpenHistory
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("↻", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
        }
        Text("History", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        if (analyticsLoading && monthReads == 0 && monthPages == 0) {
            EntryDetailSkeletonLines(compactContent = true, alpha = 0.5f)
        } else {
            Text(
                text = "$monthReads reads",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (monthPages > 0) {
                    "$monthPages pages"
                } else if (monthSpeed.totalSecondsElapsed > 0L) {
                    formatDurationFromSeconds(monthSpeed.totalSecondsElapsed)
                } else {
                    "This month"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun ReadingHistoryPage(
    analyticsSnapshot: ReadAnalyticsSnapshot,
    analyticsLoading: Boolean,
    onRefresh: () -> Unit,
    dayEntriesProvider: suspend (LocalDate) -> List<DayReadEntryRow>
) {
    var selectedRange by remember { mutableStateOf(StatsRange.MONTH) }
    var showReadBreakdown by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf<DailyActivityPoint?>(null) }
    var selectedDayEntries by remember { mutableStateOf<List<DayReadEntryRow>>(emptyList()) }
    var selectedDayEntriesLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val readCount = analyticsSnapshot.readCounts[selectedRange] ?: 0
    val readBreakdown = analyticsSnapshot.readBreakdowns[selectedRange]
        ?: ReadCountBreakdown(uniqueEntries = readCount, rereads = 0)
    val pagesRead = analyticsSnapshot.pagesRead[selectedRange] ?: 0
    val averageRating = analyticsSnapshot.averageRatings[selectedRange] ?: 0f
    val speedStats = analyticsSnapshot.readingSpeed[selectedRange] ?: ReadingSpeedStats()
    val dailyActivity = analyticsSnapshot.dailyActivity[selectedRange].orEmpty()
    val activeDays = remember(selectedRange, dailyActivity) {
        dailyActivity
            .filter { it.pagesRead > 0 || it.entriesRead > 0 }
            .sortedByDescending { it.date }
            .take(8)
    }
    fun selectHistoryDay(day: DailyActivityPoint) {
        selectedDay = day
        selectedDayEntries = emptyList()
        selectedDayEntriesLoading = true
        scope.launch {
            selectedDayEntries = runCatching { dayEntriesProvider(day.date) }
                .getOrElse { emptyList() }
            selectedDayEntriesLoading = false
        }
    }

    if (showReadBreakdown) {
        ReadingBreakdownDialog(
            range = selectedRange,
            breakdown = readBreakdown,
            obscure = false,
            onDismiss = { showReadBreakdown = false }
        )
    }

    selectedDay?.let { day ->
        AlertDialog(
            onDismissRequest = { selectedDay = null },
            title = { Text(day.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${day.entriesRead} entr${if (day.entriesRead == 1) "y" else "ies"} read • ${day.pagesRead} page${if (day.pagesRead == 1) "" else "s"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selectedDayEntriesLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Loading entries...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (selectedDayEntries.isEmpty()) {
                        Text(
                            text = "No individual entry rows were recorded for this day.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(selectedDayEntries, key = { it.code }) { entry ->
                                DayReadEntryCard(entry = entry)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDay = null }) {
                    Text("Close")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Reading history",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Global reading stats and recent activity",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = onRefresh,
                        enabled = !analyticsLoading
                    ) {
                        Text(if (analyticsLoading) "Loading" else "Refresh")
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(StatsRange.entries, key = { it.name }) { range ->
                        FilterChip(
                            selected = selectedRange == range,
                            onClick = { selectedRange = range },
                            label = { Text(range.label) }
                        )
                    }
                }

                if (analyticsLoading && readCount == 0 && pagesRead == 0) {
                    EntryDetailSkeletonLines(compactContent = false, alpha = 0.5f)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReadingHistoryMetricCard(
                        label = "Reads",
                        value = readCount.toString(),
                        onClick = { showReadBreakdown = true },
                        modifier = Modifier.weight(1f)
                    )
                    ReadingHistoryMetricCard(
                        label = "Pages",
                        value = pagesRead.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReadingHistoryMetricCard(
                        label = "Avg rating",
                        value = if (averageRating > 0f) "${formatRatingValue(averageRating)}/5" else "--",
                        modifier = Modifier.weight(1f)
                    )
                    ReadingHistoryMetricCard(
                        label = "Time",
                        value = if (speedStats.totalSecondsElapsed > 0L) {
                            formatDurationFromSeconds(speedStats.totalSecondsElapsed)
                        } else {
                            "--"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Activity map",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                ActivityHeatmap(
                    range = selectedRange,
                    points = dailyActivity,
                    onDaySelected = ::selectHistoryDay,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Recent active days",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (activeDays.isEmpty()) {
                    Text(
                        text = "No reading activity in this range yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    activeDays.forEach { day ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectHistoryDay(day) }
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = day.date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${day.entriesRead} read • ${day.pagesRead} pages",
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

@Composable
internal fun ReadingHistorySummaryCard(
    analyticsSnapshot: ReadAnalyticsSnapshot,
    analyticsLoading: Boolean,
    incognitoModeEnabled: Boolean,
    selectedRange: StatsRange,
    onSelectedRangeChange: (StatsRange) -> Unit,
    onRefresh: () -> Unit
) {
    var showReadBreakdown by remember { mutableStateOf(false) }
    val readCount = analyticsSnapshot.readCounts[selectedRange] ?: 0
    val readBreakdown = analyticsSnapshot.readBreakdowns[selectedRange]
        ?: ReadCountBreakdown(uniqueEntries = readCount, rereads = 0)
    val pagesRead = analyticsSnapshot.pagesRead[selectedRange] ?: 0
    val averageRating = analyticsSnapshot.averageRatings[selectedRange] ?: 0f
    val speedStats = analyticsSnapshot.readingSpeed[selectedRange] ?: ReadingSpeedStats()
    if (showReadBreakdown) {
        ReadingBreakdownDialog(
            range = selectedRange,
            breakdown = readBreakdown,
            obscure = incognitoModeEnabled,
            onDismiss = { showReadBreakdown = false }
        )
    }
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Reading history",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Global reading stats and recent activity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = onRefresh,
                    enabled = !analyticsLoading
                ) {
                    Text(if (analyticsLoading) "Loading" else "Refresh")
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(StatsRange.entries, key = { it.name }) { range ->
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { onSelectedRangeChange(range) },
                        label = { Text(range.label) }
                    )
                }
            }

            if (analyticsLoading && readCount == 0 && pagesRead == 0) {
                EntryDetailSkeletonLines(compactContent = false, alpha = 0.5f)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                    ReadingHistoryMetricCard(
                        label = "Reads",
                        value = readCount.toString(),
                        obscure = incognitoModeEnabled,
                        onClick = if (incognitoModeEnabled) null else ({ showReadBreakdown = true }),
                        modifier = Modifier.weight(1f)
                    )
                    ReadingHistoryMetricCard(
                        label = "Pages",
                        value = pagesRead.toString(),
                        obscure = incognitoModeEnabled,
                        modifier = Modifier.weight(1f)
                    )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                    ReadingHistoryMetricCard(
                        label = "Avg rating",
                        value = if (averageRating > 0f) "${formatRatingValue(averageRating)}/5" else "--",
                        obscure = incognitoModeEnabled,
                        modifier = Modifier.weight(1f)
                    )
                    ReadingHistoryMetricCard(
                        label = "Time",
                        value = if (speedStats.totalSecondsElapsed > 0L) {
                        formatDurationFromSeconds(speedStats.totalSecondsElapsed)
                        } else {
                            "--"
                        },
                        obscure = incognitoModeEnabled,
                        modifier = Modifier.weight(1f)
                    )
            }
        }
    }
}

@Composable
internal fun ReadingHistoryActivityMapCard(
    range: StatsRange,
    points: List<DailyActivityPoint>,
    incognitoModeEnabled: Boolean,
    onDaySelected: (DailyActivityPoint) -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Activity map",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            ActivityHeatmap(
                range = range,
                points = points,
                onDaySelected = onDaySelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                    )
            )
        }
    }
}

@Composable
internal fun ReadingHistoryRecentDaysCard(
    activeDays: List<DailyActivityPoint>,
    incognitoModeEnabled: Boolean,
    onDaySelected: (DailyActivityPoint) -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Recent active days",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (activeDays.isEmpty()) {
                Text(
                    text = "No reading activity in this range yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                activeDays.forEach { day ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onDaySelected(day) }
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = day.date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())),
                                modifier = Modifier.privacyObfuscate(
                                    enabled = incognitoModeEnabled,
                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${day.entriesRead} read • ${day.pagesRead} pages",
                                modifier = Modifier.privacyObfuscate(
                                    enabled = incognitoModeEnabled,
                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun DayReadEntriesDialog(
    day: DailyActivityPoint,
    entries: List<DayReadEntryRow>,
    loading: Boolean,
    incognitoModeEnabled: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = day.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())),
                modifier = Modifier.privacyObfuscate(
                    enabled = incognitoModeEnabled,
                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${day.entriesRead} entr${if (day.entriesRead == 1) "y" else "ies"} read • ${day.pagesRead} page${if (day.pagesRead == 1) "" else "s"}",
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (loading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Loading entries...", style = MaterialTheme.typography.bodySmall)
                    }
                } else if (entries.isEmpty()) {
                    Text(
                        text = "No individual entry rows were recorded for this day.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(entries, key = { it.rowKey }) { entry ->
                            DayReadEntryCard(
                                entry = entry,
                                incognitoModeEnabled = incognitoModeEnabled
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
internal fun ReadingHistoryMetricCard(
    label: String,
    value: String,
    obscure: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier
                    .clip(shape)
                    .clickable(onClick = onClick)
            } else {
                Modifier
            }
        ),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                modifier = Modifier.privacyObfuscate(
                    enabled = obscure,
                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReadingBreakdownDialog(
    range: StatsRange,
    breakdown: ReadCountBreakdown,
    obscure: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reading breakdown") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = range.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ReadingBreakdownRow("Unique entries", breakdown.uniqueEntries, obscure)
                ReadingBreakdownRow("Re-reads", breakdown.rereads, obscure)
                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                ReadingBreakdownRow("Total reading events", breakdown.total, obscure, emphasize = true)
                Text(
                    text = "Entries shows unique currently-read items. History also includes recorded re-reads.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun ReadingBreakdownRow(
    label: String,
    value: Int,
    obscure: Boolean,
    emphasize: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value.toString(),
            modifier = Modifier.privacyObfuscate(
                enabled = obscure,
                overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
internal fun DayReadEntryCard(
    entry: DayReadEntryRow,
    incognitoModeEnabled: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (entry.thumbnailUrl.isNotBlank()) {
                ThumbnailImage(
                    thumbnailUrl = entry.thumbnailUrl,
                    backupCode = entry.code,
                    contentDescription = "Cover for code ${entry.code}",
                    obscure = incognitoModeEnabled,
                    modifier = Modifier
                        .width(46.dp)
                        .height(62.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .height(62.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("#", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA),
                        expandHorizontal = 0.dp,
                        expandVertical = 0.dp,
                        cornerRadius = 12.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = entry.title.ifBlank { "#${entry.code}" },
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "#${entry.code}",
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                if (entry.isReread) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text(
                            text = "Re-read",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                val detailBits = buildList {
                    if (entry.sessionCount > 0) add("${entry.sessionCount} session${if (entry.sessionCount == 1) "" else "s"}")
                    if (entry.pagesViewed > 0) add("${entry.pagesViewed} pages")
                    if (entry.secondsElapsed > 0L) add(formatDurationFromSeconds(entry.secondsElapsed))
                }
                Text(
                    text = detailBits.takeIf { it.isNotEmpty() }?.joinToString(" • ")
                        ?: "Read ${formatStoredUtcTimestampForDisplay(entry.readAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
