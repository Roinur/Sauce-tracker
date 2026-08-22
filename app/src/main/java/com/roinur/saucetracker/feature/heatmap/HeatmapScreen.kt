package com.roinur.saucetracker.feature.heatmap

import com.roinur.saucetracker.*
import com.roinur.saucetracker.feature.library.detail.SelectedEntryDetailCard
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
internal fun HeatmapScreen(
    snapshot: TagGraphSnapshot?,
    loading: Boolean,
    errorMessage: String?,
    selectedTab: TagGraphTab,
    selectedHeatmapDisplayMode: TagHeatmapDisplayMode,
    incognitoModeEnabled: Boolean,
    analyticsSnapshot: ReadAnalyticsSnapshot,
    showThumbnails: Boolean,
    entryDetailProvider: (Int) -> EntryDetail?,
    onSelectTab: (TagGraphTab) -> Unit,
    onSelectHeatmapDisplayMode: (TagHeatmapDisplayMode) -> Unit,
    onRefresh: () -> Unit,
    onSelectGraphEntry: (Int) -> Unit,
    onOpenEntryInBrowser: (Int) -> Unit,
    onOpenCreatorFromDetail: (String, String) -> Unit,
    onCopyCode: (Int) -> Unit,
    onToggleReadStatus: (Int) -> Unit,
    onSetRating: (Int, Int) -> Unit,
    onResetRating: (Int) -> Unit,
    onRefetch: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    selectedSeriesNeighborsForCode: (Int) -> SeriesNeighbors,
    onOpenSeriesEntry: (Int) -> Unit,
    onOpenCreatorInBrowser: (String, String) -> Unit,
    onSelectedThumbnailClick: (Int, String, String) -> Unit,
    thumbnailSessionBitmaps: SnapshotStateMap<Int, ImageBitmap>? = null,
    entryLayoutSessionCache: HeatmapLayoutCache? = null,
    persistentEntryLayoutProvider: (suspend (String, TagGraphSnapshot) -> TagGraphEntryLayoutResult?)? = null,
    onDismiss: () -> Unit
) {
    @Composable
    fun GraphSegmentedTabs(
        labels: List<String>,
        selectedIndex: Int,
        modifier: Modifier = Modifier,
        onSelected: (Int) -> Unit
    ) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                labels.forEachIndexed { index, label ->
                    val selected = selectedIndex == index
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelected(index) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        } else {
                            Color.Transparent
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun HeatmapModeTitles(
        labels: List<String>,
        selectedIndex: Int,
        modifier: Modifier = Modifier,
        onSelected: (Int) -> Unit
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            labels.forEachIndexed { index, label ->
                val selected = selectedIndex == index
                Text(
                    text = label,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelected(index) }
                        .padding(horizontal = 2.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }

    var selectionSheetState by remember(snapshot, selectedTab, selectedHeatmapDisplayMode) {
        mutableStateOf<GraphSelectionSheetState?>(null)
    }
    fun dismissSelectionLayer() {
        selectionSheetState = when (val current = selectionSheetState) {
            is GraphSelectionSheetState.Entry -> current.returnTagNode?.let { GraphSelectionSheetState.Tag(it) }
            is GraphSelectionSheetState.Tag -> null
            null -> null
        }
    }
    val selectionSheetVisible = selectionSheetState != null
    var selectionSheetHeightFraction by remember(selectionSheetState) { mutableStateOf(0.58f) }
    val selectionSheetProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selectionSheetVisible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 220),
        label = "graphSelectionSheetProgress"
    )
    LaunchedEffect(selectedTab, selectedHeatmapDisplayMode, incognitoModeEnabled) {
        selectionSheetState = null
        if (incognitoModeEnabled) {
            return@LaunchedEffect
        }
    }
    LaunchedEffect(snapshot, loading, errorMessage) {
        if (snapshot == null && !loading && errorMessage == null) {
            onRefresh()
        }
    }
    LaunchedEffect(selectionSheetState) {
        selectionSheetHeightFraction = when (selectionSheetState) {
            is GraphSelectionSheetState.Entry -> 0.58f
            is GraphSelectionSheetState.Tag -> 0.54f
            null -> 0.58f
        }
    }
    BackHandler(enabled = selectionSheetState != null) {
        dismissSelectionLayer()
    }

    Dialog(
        onDismissRequest = {
            if (selectionSheetState != null) {
                dismissSelectionLayer()
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Tag Graph",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = when {
                                    snapshot != null -> "${snapshot.nodes.size} tags from ${snapshot.totalEntries} local entries"
                                    loading -> "Building graph from your local library..."
                                    errorMessage != null -> "Graph build failed"
                                    else -> "No graph data yet"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.privacyObfuscate(
                                    enabled = incognitoModeEnabled,
                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                )
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onRefresh, enabled = !loading) {
                                Text("Refresh")
                            }
                            TextButton(onClick = onDismiss) {
                                Text("Close")
                            }
                        }
                    }

                    GraphSegmentedTabs(
                        labels = TagGraphTab.entries.map { it.label },
                        selectedIndex = selectedTab.ordinal,
                        onSelected = { index -> onSelectTab(TagGraphTab.entries[index]) }
                    )
                    if (selectedTab == TagGraphTab.HEATMAP) {
                        HeatmapModeTitles(
                            labels = TagHeatmapDisplayMode.entries.map { it.label },
                            selectedIndex = selectedHeatmapDisplayMode.ordinal,
                            modifier = Modifier.fillMaxWidth(),
                            onSelected = { onSelectHeatmapDisplayMode(TagHeatmapDisplayMode.entries[it]) }
                        )
                    }

                    when {
                        loading && snapshot == null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        text = "Computing tag field...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        errorMessage != null && snapshot == null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Could not build graph: $errorMessage",
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        snapshot == null || snapshot.nodes.isEmpty() -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No tag graph data yet.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        else -> {
                            HeatmapCanvas(
                                snapshot = snapshot,
                                selectedTab = selectedTab,
                                selectedHeatmapDisplayMode = selectedHeatmapDisplayMode,
                                incognitoModeEnabled = incognitoModeEnabled,
                                onTagSelected = { node ->
                                    selectionSheetState = GraphSelectionSheetState.Tag(node)
                                },
                                onEntrySelected = { entry, dominantCircleTags ->
                                    onSelectGraphEntry(entry.code)
                                    selectionSheetState = GraphSelectionSheetState.Entry(
                                        entry = entry,
                                        dominantCircleTags = dominantCircleTags,
                                        returnTagNode = null
                                    )
                                },
                                thumbnailSessionBitmaps = thumbnailSessionBitmaps,
                                entryLayoutSessionCache = entryLayoutSessionCache,
                                persistentEntryLayoutProvider = persistentEntryLayoutProvider,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                            Text(
                                text = when (selectedTab) {
                                    TagGraphTab.HEATMAP -> "Tag heatmap: tags that co-occur in your local library cluster together."
                                    TagGraphTab.RAW -> "Raw frequency: left = avoided locally, right = liked locally, top = objectively common."
                                    TagGraphTab.RATED -> "Rated frequency: left = poorly rated, right = highly rated, top = objectively common. Rating 0 is skipped."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (selectedTab == TagGraphTab.RATED) {
                                Text(
                                    text = "Rated entries used: ${snapshot.totalRatedEntries}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
            }

            if (selectionSheetProgress > 0.001f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.18f * selectionSheetProgress))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            dismissSelectionLayer()
                        }
                )
            }

            selectionSheetState?.let { sheet ->
                val currentSnapshot = snapshot ?: return@let
                val baseSheetHeightFraction = when (sheet) {
                    is GraphSelectionSheetState.Entry -> 0.58f
                    is GraphSelectionSheetState.Tag -> 0.54f
                }
                val sheetHeight = LocalConfiguration.current.screenHeightDp.dp * selectionSheetHeightFraction
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(sheetHeight)
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                        .graphicsLayer(
                            alpha = selectionSheetProgress,
                            translationY = (1f - selectionSheetProgress) * 520f
                        ),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    when (sheet) {
                        is GraphSelectionSheetState.Entry -> {
                            val detail = entryDetailProvider(sheet.entry.code)
                            val dominantNodes = sheet.dominantCircleTags.mapNotNull { dominantTag ->
                                val normalized = normalizeTagName(dominantTag)
                                currentSnapshot.nodes.firstOrNull { it.normalizedName == normalized }
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerInput(sheet, baseSheetHeightFraction) {
                                            detectVerticalDragGestures { change, dragAmount ->
                                                val nextFraction = (selectionSheetHeightFraction - (dragAmount / 2200f))
                                                    .coerceIn(baseSheetHeightFraction, 0.82f)
                                                selectionSheetHeightFraction = nextFraction
                                                change.consume()
                                            } 
                                        },
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(34.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(42.dp)
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f))
                                        )
                                    }
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "Entry from Graph",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.align(Alignment.CenterStart)
                                        )
                                        TextButton(
                                            onClick = { dismissSelectionLayer() },
                                            modifier = Modifier.align(Alignment.CenterEnd)
                                        ) {
                                            Text("Close")
                                        }
                                    }
                                    GraphTagPrevalenceBar(
                                        nodes = dominantNodes,
                                        snapshot = currentSnapshot,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    SelectedEntryDetailCard(
                                        detail = detail,
                                        analyticsSnapshot = analyticsSnapshot,
                                        onOpenInBrowser = { onOpenEntryInBrowser(sheet.entry.code) },
                                        onOpenCreatorFromDetail = onOpenCreatorFromDetail,
                                        onCopyCode = onCopyCode,
                                        onToggleReadStatus = onToggleReadStatus,
                                        onSetRating = onSetRating,
                                        onResetRating = onResetRating,
                                        onRefetch = onRefetch,
                                        onDelete = onDelete,
                                        seriesNeighbors = selectedSeriesNeighborsForCode(sheet.entry.code),
                                        onOpenSeriesEntry = onOpenSeriesEntry,
                                        onOpenCreatorInBrowser = onOpenCreatorInBrowser,
                                        onSelectedThumbnailClick = onSelectedThumbnailClick,
                                        showThumbnails = showThumbnails,
                                        incognitoModeEnabled = incognitoModeEnabled,
                                        headerCenterText = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                    )
                                }
                            }
                        }

                        is GraphSelectionSheetState.Tag -> {
                            val matchingCodes = remember(sheet.node.normalizedName, snapshot) {
                                currentSnapshot.entryNodes
                                    .asSequence()
                                    .filter { sheet.node.normalizedName in it.tagNames }
                                    .map { it.code }
                                    .toSet()
                            }
                            val matchingDetails = remember(matchingCodes) {
                                matchingCodes
                                    .mapNotNull(entryDetailProvider)
                                    .sortedByDescending { it.rating }
                                    .sortedByDescending { it.isRead }
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerInput(sheet, baseSheetHeightFraction) {
                                            detectVerticalDragGestures { change, dragAmount ->
                                                val nextFraction = (selectionSheetHeightFraction - (dragAmount / 2200f))
                                                    .coerceIn(baseSheetHeightFraction, 0.82f)
                                                selectionSheetHeightFraction = nextFraction
                                                change.consume()
                                            } 
                                        },
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(34.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(42.dp)
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f))
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = sheet.node.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        TextButton(onClick = { dismissSelectionLayer() }) {
                                            Text("Close")
                                        }
                                    }
                                    GraphTagPrevalenceBar(
                                        nodes = listOf(sheet.node),
                                        snapshot = currentSnapshot,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (matchingDetails.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No local entries currently match this tag.",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(
                                            matchingDetails,
                                            key = { it.code },
                                            contentType = { "graph_tag_entry_row" }
                                        ) { detail ->
                                            GraphCompactEntryRow(
                                                detail = detail,
                                                incognitoModeEnabled = incognitoModeEnabled,
                                                onClick = {
                                                    onSelectGraphEntry(detail.code)
                                                    currentSnapshot.entryNodes
                                                        .firstOrNull { it.code == detail.code }
                                                        ?.let { entryNode ->
                                                            selectionSheetState = GraphSelectionSheetState.Entry(
                                                                entry = entryNode,
                                                                dominantCircleTags = entryNode.dominantCircleTags,
                                                                returnTagNode = sheet.node
                                                            )
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
            }
        }
    }
}


