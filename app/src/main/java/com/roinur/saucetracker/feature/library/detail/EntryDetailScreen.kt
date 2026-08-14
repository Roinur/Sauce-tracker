package com.roinur.saucetracker.feature.library.detail

import com.roinur.saucetracker.core.ui.components.*

import com.roinur.saucetracker.*
import com.roinur.saucetracker.core.media.*
import com.roinur.saucetracker.data.backup.*
import com.roinur.saucetracker.data.downloads.*
import com.roinur.saucetracker.data.database.entity.RelatedEntryEntity
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
internal fun EntryCodeLine(
    code: Int,
    showSessionNewBadge: Boolean,
    incognitoModeEnabled: Boolean,
    textStyle: TextStyle,
    codeColor: Color,
    modifier: Modifier = Modifier
) {
    val privacyOverlay = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Code: $code",
            modifier = Modifier.privacyObfuscate(
                enabled = incognitoModeEnabled,
                overlayColor = privacyOverlay
            ),
            style = textStyle,
            color = codeColor
        )
        if (showSessionNewBadge) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(percent = 50)
                    )
                    .privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    )
                    .padding(horizontal = 7.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "NEW",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SelectedEntryDetailCard(
    detail: EntryDetail?,
    summary: EntryRow? = null,
    detailLoading: Boolean = false,
    analyticsSnapshot: ReadAnalyticsSnapshot,
    onOpenInBrowser: () -> Unit,
    onOpenCreatorFromDetail: (String, String) -> Unit,
    onCopyCode: (Int) -> Unit,
    onToggleReadStatus: (Int) -> Unit,
    onSetRating: (Int, Int) -> Unit,
    onResetRating: (Int) -> Unit,
    ratingHistoryProvider: (Int) -> List<EntryRatingHistoryRow> = { emptyList() },
    averageRatingProvider: (Int) -> Float? = { null },
    onUpdateRatingHistory: (Int, EntryRatingHistoryRow, Int) -> Unit = { _, _, _ -> },
    onDeleteRatingHistory: (Int, EntryRatingHistoryRow) -> Unit = { _, _ -> },
    onRefetch: (Int) -> Unit,
    downloadButtonLabel: String? = null,
    downloadProgressLabel: String? = null,
    downloadProgressFraction: Float? = null,
    onDownloadAction: ((EntryDetail) -> Unit)? = null,
    onRedownloadAction: ((EntryDetail) -> Unit)? = null,
    onDelete: (Int) -> Unit,
    seriesNeighbors: SeriesNeighbors,
    onOpenSeriesEntry: (Int) -> Unit,
    enableLibraryRelatedNavigation: Boolean = false,
    relatedEntriesState: SelectedEntryRelatedUiState = SelectedEntryRelatedUiState(),
    relatedEntryMode: RelatedEntryMode? = null,
    onRelatedEntryModeChange: (RelatedEntryMode) -> Unit = {},
    onOpenRelatedEntry: (Int) -> Unit = onOpenSeriesEntry,
    onOpenCreatorInBrowser: (String, String) -> Unit,
    onSelectedThumbnailClick: (Int, String, String) -> Unit,
    showThumbnails: Boolean,
    incognitoModeEnabled: Boolean,
    experimentalLazyMetadata: Boolean = false,
    headerCenterText: String? = null,
    compactContent: Boolean = false,
    modifier: Modifier = Modifier
) {
    var ratingHistoryRows by remember(detail?.code) { mutableStateOf<List<EntryRatingHistoryRow>>(emptyList()) }
    var showRatingHistoryDialog by remember(detail?.code) { mutableStateOf(false) }
    var expandedHistoryRow by remember(detail?.code) { mutableStateOf<EntryRatingHistoryRow?>(null) }
    var editingHistoryRating by remember(detail?.code) { mutableStateOf(0) }
    var detailsExpanded by remember(detail?.code) { mutableStateOf(false) }
    LaunchedEffect(incognitoModeEnabled) {
        if (incognitoModeEnabled) {
            showRatingHistoryDialog = false
            ratingHistoryRows = emptyList()
            expandedHistoryRow = null
        }
    }
    var metadataVisible by remember(detail?.code, experimentalLazyMetadata) { mutableStateOf(!experimentalLazyMetadata) }
    LaunchedEffect(detail?.code, experimentalLazyMetadata) {
        if (detail != null && experimentalLazyMetadata) {
            delay(70)
            metadataVisible = true
        } else {
            metadataVisible = true
        }
    }
    val metadataAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (metadataVisible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 170, easing = FastOutSlowInEasing),
        label = "selectedEntryMetadataAlpha"
    )
    val displayedRating = remember(detail?.code, detail?.rating, showRatingHistoryDialog, ratingHistoryRows) {
        detail?.let { averageRatingProvider(it.code) }
            ?: detail?.rating?.toFloat()
            ?: 0f
    }.coerceIn(0f, 5f)
    if (showRatingHistoryDialog && detail != null) {
        AlertDialog(
            onDismissRequest = { showRatingHistoryDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = "${ratingHistoryRows.size}x",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text("Rating history")
                }
            },
            text = {
                if (ratingHistoryRows.isEmpty()) {
                    Text(
                        text = "No reading history recorded for this entry yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ratingHistoryRows.forEachIndexed { index, row ->
                            val cardShape = RoundedCornerShape(16.dp)
                            val isExpanded = expandedHistoryRow == row
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(cardShape)
                                    .clickable {
                                        if (isExpanded) {
                                            expandedHistoryRow = null
                                        } else {
                                            expandedHistoryRow = row
                                            editingHistoryRating = row.rating.coerceIn(0, 5)
                                        }
                                    },
                                shape = cardShape,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = formatStoredUtcTimestampForDisplay(row.readAt),
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (row.isReread) {
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
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (isExpanded) {
                                            for (ratingIndex in 1..5) {
                                                val filled = ratingIndex <= editingHistoryRating
                                                val starInteraction = remember { MutableInteractionSource() }
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clickable(
                                                            interactionSource = starInteraction,
                                                            indication = null
                                                        ) { editingHistoryRating = ratingIndex },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = if (filled) "★" else "☆",
                                                        style = MaterialTheme.typography.titleLarge,
                                                        color = if (filled) RATING_STAR_GOLD else RATING_STAR_MUTED
                                                    )
                                                }
                                            }
                                        } else {
                                            FractionalRatingStars(
                                                rating = row.rating.toFloat(),
                                                starSize = 18.dp
                                            )
                                        }
                                        Text(
                                            text = "(${if (isExpanded) editingHistoryRating else row.rating}/5)",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                    if (isExpanded) {
                                        Text(
                                            text = if (row.isEntrySummary) {
                                                "Removing this resets the entry's read/rating state."
                                            } else {
                                                "Removing this only deletes this re-read rating."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    onDeleteRatingHistory(detail.code, row)
                                                    ratingHistoryRows = ratingHistoryProvider(detail.code)
                                                    expandedHistoryRow = null
                                                }
                                            ) {
                                                Text("Remove")
                                            }
                                            TextButton(onClick = { expandedHistoryRow = null }) {
                                                Text("Cancel")
                                            }
                                            TextButton(
                                                onClick = {
                                                    onUpdateRatingHistory(detail.code, row, editingHistoryRating)
                                                    ratingHistoryRows = ratingHistoryProvider(detail.code)
                                                    expandedHistoryRow = null
                                                }
                                            ) {
                                                Text("Save")
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "Tap to edit",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRatingHistoryDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        val privacyOverlay = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
        val detailTitleStyle = if (compactContent) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium
        val detailBodyStyle = if (compactContent) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
        val detailLabelStyle = if (compactContent) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge
        val actionHorizontalPadding = if (compactContent) 4.dp else 8.dp
        val detailThumbnailHeight = if (compactContent) 160.dp else 190.dp
        val rateStarBoxSize = if (compactContent) 24.dp else 28.dp
        val rateStarSize = if (compactContent) 20.dp else 24.dp
        Column(
            modifier = Modifier.padding(if (compactContent) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (compactContent) 4.dp else 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Selected Entry",
                    style = detailTitleStyle,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    headerCenterText?.takeIf { it.isNotBlank() }?.let { dominantText ->
                        Text(
                            text = dominantText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(
                    onClick = onOpenInBrowser,
                    enabled = detail != null && !incognitoModeEnabled
                ) {
                    if (detail != null) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_open_in_new_24),
                            contentDescription = "Open in browser",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (detail == null) {
                if (summary != null) {
                    SelectedEntrySummarySkeleton(
                        summary = summary,
                        loading = detailLoading,
                        displayedRating = summary.averageRating.takeIf { it > 0f } ?: summary.rating.toFloat(),
                        showThumbnails = showThumbnails,
                        incognitoModeEnabled = incognitoModeEnabled,
                        privacyOverlay = privacyOverlay,
                        detailThumbnailHeight = detailThumbnailHeight,
                        detailBodyStyle = detailBodyStyle,
                        detailTitleStyle = detailTitleStyle,
                        onCopyCode = onCopyCode,
                        onSelectedThumbnailClick = onSelectedThumbnailClick
                    )
                } else {
                    Text(
                        if (detailLoading) "Loading selected entry..." else "Selected entry details unavailable.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (detailLoading) {
                        EntryDetailSkeletonLines(compactContent = compactContent)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(if (compactContent) 4.dp else 6.dp)) {
                if (showThumbnails && detail.thumbnailUrl.isNotBlank()) {
                    ThumbnailImage(
                        thumbnailUrl = detail.thumbnailUrl,
                        backupCode = detail.code,
                        contentDescription = "Large cover for code ${detail.code}",
                        obscure = incognitoModeEnabled,
                        onClick = {
                            onSelectedThumbnailClick(
                                detail.code,
                                detail.thumbnailUrl,
                                "Large cover for code ${detail.code}"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(detailThumbnailHeight)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Code:",
                            modifier = Modifier.privacyObfuscate(
                                enabled = incognitoModeEnabled,
                                overlayColor = privacyOverlay
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = { onCopyCode(detail.code) },
                            enabled = !incognitoModeEnabled,
                            modifier = Modifier.heightIn(min = 0.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = detail.code.toString(),
                                modifier = Modifier.privacyObfuscate(
                                    enabled = incognitoModeEnabled,
                                    overlayColor = privacyOverlay
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                style = detailTitleStyle,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (downloadProgressLabel != null) {
                            Column(
                                modifier = Modifier.widthIn(min = 88.dp, max = 128.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = downloadProgressLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (downloadProgressFraction != null) {
                                    LinearProgressIndicator(
                                        progress = { downloadProgressFraction.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                    )
                                } else {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                    )
                                }
                            }
                        } else if (detail != null && downloadButtonLabel != null && onDownloadAction != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .combinedClickable(
                                        enabled = !incognitoModeEnabled,
                                        onClick = { onDownloadAction(detail) },
                                        onLongClick = if (downloadButtonLabel == "Local" && onRedownloadAction != null) {
                                            { onRedownloadAction(detail) }
                                        } else {
                                            null
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = downloadButtonLabel,
                                    modifier = Modifier.privacyObfuscate(
                                        enabled = incognitoModeEnabled,
                                        overlayColor = privacyOverlay
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = detailLabelStyle
                                )
                            }
                        }
                        TextButton(
                            onClick = { onRefetch(detail.code) },
                            enabled = !incognitoModeEnabled,
                            modifier = Modifier.heightIn(min = 0.dp),
                            contentPadding = PaddingValues(horizontal = actionHorizontalPadding, vertical = 0.dp)
                        ) {
                            Text(
                                text = "Re-fetch",
                                style = detailBodyStyle,
                                modifier = Modifier.privacyObfuscate(
                                    enabled = incognitoModeEnabled,
                                    overlayColor = privacyOverlay
                                )
                            )
                        }
                        TextButton(
                            onClick = { onDelete(detail.code) },
                            enabled = !incognitoModeEnabled,
                            modifier = Modifier.heightIn(min = 0.dp),
                            contentPadding = PaddingValues(horizontal = actionHorizontalPadding, vertical = 0.dp)
                        ) {
                            Text(
                                text = "Delete",
                                style = detailBodyStyle,
                                modifier = Modifier.privacyObfuscate(
                                    enabled = incognitoModeEnabled,
                                    overlayColor = privacyOverlay
                                )
                            )
                        }
                    }
                }
                Text(
                    text = "Title: ${detail.title}",
                    style = detailBodyStyle,
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    )
                )
                Column(
                    modifier = Modifier.graphicsLayer(alpha = if (experimentalLazyMetadata) metadataAlpha else 1f),
                    verticalArrangement = Arrangement.spacedBy(if (compactContent) 4.dp else 6.dp)
                ) {
                Text(
                    text = "Subtitle: ${detail.subtitle.ifBlank { "-" }}",
                    style = detailBodyStyle,
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    )
                )
                Text(
                    text = "Pages: ${detail.numPages}",
                    style = detailBodyStyle,
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    )
                )
                Text(
                    text = buildEtaTextForEntry(detail.numPages, analyticsSnapshot),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    )
                )
                if (!enableLibraryRelatedNavigation) {
                    Text(
                        text = "Uploaded: ${detail.uploadDate.ifBlank { "-" }}",
                        style = detailBodyStyle,
                        modifier = Modifier.privacyObfuscate(
                            enabled = incognitoModeEnabled,
                            overlayColor = privacyOverlay
                        )
                    )
                }
                Row(
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Rating:", style = detailBodyStyle)
                    FractionalRatingStars(
                        rating = displayedRating,
                        starSize = if (compactContent) 16.dp else 18.dp
                    )
                    Text(
                        text = "(${formatRatingValue(displayedRating)}/5)",
                        style = detailBodyStyle
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Rate:",
                        style = detailBodyStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.privacyObfuscate(
                            enabled = incognitoModeEnabled,
                            overlayColor = privacyOverlay
                        )
                    )
                    for (index in 1..5) {
                        val fill = (displayedRating - (index - 1)).coerceIn(0f, 1f)
                        val starInteraction = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .size(rateStarBoxSize)
                                .clickable(
                                    interactionSource = starInteraction,
                                    indication = null,
                                    enabled = !incognitoModeEnabled
                                ) { onSetRating(detail.code, index) },
                            contentAlignment = Alignment.Center
                        ) {
                            FractionalRatingStar(
                                fill = fill,
                                starSize = rateStarSize,
                                modifier = Modifier.privacyObfuscate(
                                    enabled = incognitoModeEnabled,
                                    overlayColor = privacyOverlay
                                )
                            )
                        }
                    }
                    TextButton(
                        onClick = { onResetRating(detail.code) },
                        enabled = !incognitoModeEnabled,
                        modifier = Modifier.heightIn(min = 0.dp),
                        contentPadding = PaddingValues(horizontal = actionHorizontalPadding, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Reset",
                            style = detailBodyStyle,
                            modifier = Modifier.privacyObfuscate(
                                enabled = incognitoModeEnabled,
                                overlayColor = privacyOverlay
                            )
                        )
                    }
                    TextButton(
                        onClick = {
                            ratingHistoryRows = ratingHistoryProvider(detail.code)
                            showRatingHistoryDialog = true
                        },
                        enabled = !incognitoModeEnabled,
                        modifier = Modifier.heightIn(min = 0.dp),
                        contentPadding = PaddingValues(horizontal = actionHorizontalPadding, vertical = 0.dp)
                    ) {
                        Text(
                            text = "History",
                            style = detailBodyStyle,
                            modifier = Modifier.privacyObfuscate(
                                enabled = incognitoModeEnabled,
                                overlayColor = privacyOverlay
                            )
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val detailReadStateColor = if (detail.isRead) READ_STATE_COLOR else UNREAD_STATE_COLOR
                    Text(
                        text = "Status:",
                        style = detailBodyStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.privacyObfuscate(
                            enabled = incognitoModeEnabled,
                            overlayColor = privacyOverlay
                        )
                    )
                    val detailReadChipShape = RoundedCornerShape(999.dp)
                    Box(
                        modifier = Modifier
                            .clip(detailReadChipShape)
                            .clickable(enabled = !incognitoModeEnabled) {
                                onToggleReadStatus(detail.code)
                            }
                    ) {
                        AccentPulseOverlay(
                            triggerKey = detail.isRead,
                            tint = detailReadStateColor,
                            modifier = Modifier
                                .matchParentSize()
                                .clip(detailReadChipShape)
                        )
                        Text(
                            text = if (detail.isRead) "Read" else "Unread",
                            color = detailReadStateColor,
                            style = detailBodyStyle,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .padding(horizontal = if (compactContent) 9.dp else 12.dp, vertical = if (compactContent) 5.dp else 8.dp)
                                .privacyObfuscate(
                                    enabled = incognitoModeEnabled,
                                    overlayColor = privacyOverlay
                                )
                        )
                    }
                }
                if (!enableLibraryRelatedNavigation) {
                    LegacyEntryMetadata(
                        detail = detail,
                        detailBodyStyle = detailBodyStyle,
                        incognitoModeEnabled = incognitoModeEnabled,
                        privacyOverlay = privacyOverlay
                    )
                }
                val artistNames = detail.tagsByType["artist"].orEmpty()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                val groupNames = detail.tagsByType["group"].orEmpty()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                artistNames.forEach { artistName ->
                    CreatorJumpRow(
                        label = "Artist",
                        creatorType = "artist",
                        creatorName = artistName,
                        onOpenCreator = onOpenCreatorFromDetail,
                        onOpenCreatorInBrowser = onOpenCreatorInBrowser,
                        incognitoModeEnabled = incognitoModeEnabled
                    )
                }
                groupNames.forEach { groupName ->
                    CreatorJumpRow(
                        label = "Group",
                        creatorType = "group",
                        creatorName = groupName,
                        onOpenCreator = onOpenCreatorFromDetail,
                        onOpenCreatorInBrowser = onOpenCreatorInBrowser,
                        incognitoModeEnabled = incognitoModeEnabled
                    )
                }
                if (enableLibraryRelatedNavigation) {
                    EntryDetailsSection(
                        detail = detail,
                        expanded = detailsExpanded,
                        onToggle = { detailsExpanded = !detailsExpanded },
                        detailBodyStyle = detailBodyStyle,
                        incognitoModeEnabled = incognitoModeEnabled,
                        privacyOverlay = privacyOverlay
                    )
                    RelatedEntrySection(
                        seriesNeighbors = seriesNeighbors,
                        state = relatedEntriesState,
                        requestedMode = relatedEntryMode,
                        onModeChange = onRelatedEntryModeChange,
                        onOpenEntry = onOpenRelatedEntry,
                        showThumbnails = showThumbnails,
                        incognitoModeEnabled = incognitoModeEnabled,
                        compactContent = compactContent
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tags:",
                    style = detailBodyStyle,
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    ),
                    fontWeight = FontWeight.SemiBold
                )
                if (detail.tagsByType.isEmpty()) {
                    Text(
                        text = "(none)",
                        style = detailBodyStyle,
                        modifier = Modifier.privacyObfuscate(
                            enabled = incognitoModeEnabled,
                            overlayColor = privacyOverlay
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    detail.tagsByType.forEach { (type, names) ->
                        Text(
                            text = "$type: ${names.joinToString(", ")}",
                            style = detailBodyStyle,
                            modifier = Modifier.privacyObfuscate(
                                enabled = incognitoModeEnabled,
                                overlayColor = privacyOverlay
                            )
                        )
                    }
                }
                if (!enableLibraryRelatedNavigation &&
                    (seriesNeighbors.previous != null || seriesNeighbors.next != null)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Related In Series:",
                        modifier = Modifier.privacyObfuscate(
                            enabled = incognitoModeEnabled,
                            overlayColor = privacyOverlay
                        ),
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val prev = seriesNeighbors.previous
                        if (prev != null) {
                            TextButton(
                                onClick = { onOpenSeriesEntry(prev.code) },
                                enabled = !incognitoModeEnabled,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "← Previous: ${prev.code} ${prev.title}",
                                    modifier = Modifier.privacyObfuscate(
                                        enabled = incognitoModeEnabled,
                                        overlayColor = privacyOverlay
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        val next = seriesNeighbors.next
                        if (next != null) {
                            TextButton(
                                onClick = { onOpenSeriesEntry(next.code) },
                                enabled = !incognitoModeEnabled,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "→ Next: ${next.code} ${next.title}",
                                    modifier = Modifier.privacyObfuscate(
                                        enabled = incognitoModeEnabled,
                                        overlayColor = privacyOverlay
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
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

@Composable
private fun LegacyEntryMetadata(
    detail: EntryDetail,
    detailBodyStyle: TextStyle,
    incognitoModeEnabled: Boolean,
    privacyOverlay: Color
) {
    val privacyModifier = Modifier.privacyObfuscate(
        enabled = incognitoModeEnabled,
        overlayColor = privacyOverlay
    )
    Text(
        text = "Fetched at: ${formatStoredUtcTimestampForDisplay(detail.fetchedAt)}",
        style = detailBodyStyle,
        modifier = privacyModifier
    )
    Text(
        text = "Added at: ${formatStoredUtcTimestampForDisplay(detail.addedAt)}",
        style = detailBodyStyle,
        modifier = privacyModifier
    )
    Text(
        text = "Read at: ${formatStoredUtcTimestampForDisplay(detail.readAt)}",
        style = detailBodyStyle,
        modifier = privacyModifier
    )
    Text(
        text = "URL: ${detail.sourceUrl}",
        style = detailBodyStyle,
        modifier = privacyModifier
    )
}

@Composable
private fun EntryDetailsSection(
    detail: EntryDetail,
    expanded: Boolean,
    onToggle: () -> Unit,
    detailBodyStyle: TextStyle,
    incognitoModeEnabled: Boolean,
    privacyOverlay: Color
) {
    val shape = RoundedCornerShape(12.dp)
    val disclosureRotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 180),
        label = "entryDetailsDisclosureRotation"
    )
    val disclosureColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), shape)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Details",
                style = detailBodyStyle,
                fontWeight = FontWeight.SemiBold
            )
            Canvas(
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer(rotationZ = disclosureRotation)
            ) {
                val strokeWidth = 1.8.dp.toPx()
                drawLine(
                    color = disclosureColor,
                    start = Offset(size.width * 0.24f, size.height * 0.38f),
                    end = Offset(size.width * 0.5f, size.height * 0.64f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = disclosureColor,
                    start = Offset(size.width * 0.5f, size.height * 0.64f),
                    end = Offset(size.width * 0.76f, size.height * 0.38f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
        if (expanded) {
            Column(
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Uploaded: ${detail.uploadDate.ifBlank { "-" }}",
                    style = detailBodyStyle,
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    )
                )
                Text(
                    text = "Stored rating: ${detail.rating.coerceIn(0, 5)}/5",
                    style = detailBodyStyle,
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    )
                )
                LegacyEntryMetadata(
                    detail = detail,
                    detailBodyStyle = detailBodyStyle,
                    incognitoModeEnabled = incognitoModeEnabled,
                    privacyOverlay = privacyOverlay
                )
            }
        }
    }
}

@Composable
private fun RelatedEntrySection(
    seriesNeighbors: SeriesNeighbors,
    state: SelectedEntryRelatedUiState,
    requestedMode: RelatedEntryMode?,
    onModeChange: (RelatedEntryMode) -> Unit,
    onOpenEntry: (Int) -> Unit,
    showThumbnails: Boolean,
    incognitoModeEnabled: Boolean,
    compactContent: Boolean
) {
    val modes = availableRelatedEntryModes(seriesNeighbors, state)
    val activeMode = resolvedRelatedEntryMode(requestedMode, modes)
    if (modes.isEmpty() && !state.loading) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (modes.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 1.dp)
            ) {
                items(modes, key = { it.name }) { mode ->
                    FilterChip(
                        selected = activeMode == mode,
                        onClick = { onModeChange(mode) },
                        enabled = !incognitoModeEnabled,
                        label = { Text(mode.label, maxLines = 1) }
                    )
                }
            }
        } else if (modes.size == 1) {
            Text(
                text = modes.first().label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        when (activeMode) {
            RelatedEntryMode.PARTS -> PartsNavigator(
                seriesNeighbors = seriesNeighbors,
                onOpenEntry = onOpenEntry,
                showThumbnails = showThumbnails,
                incognitoModeEnabled = incognitoModeEnabled,
                compactContent = compactContent
            )
            RelatedEntryMode.MORE_LIKE_THIS -> RelatedEntryCarousel(
                entries = state.moreLikeThis,
                onOpenEntry = onOpenEntry,
                showThumbnails = showThumbnails,
                incognitoModeEnabled = incognitoModeEnabled,
                compactContent = compactContent
            )
            RelatedEntryMode.SAME_ARTIST -> RelatedEntryCarousel(
                entries = state.sameArtist,
                onOpenEntry = onOpenEntry,
                showThumbnails = showThumbnails,
                incognitoModeEnabled = incognitoModeEnabled,
                compactContent = compactContent
            )
            null -> if (state.loading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text(
                        text = "Finding related library entries...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PartsNavigator(
    seriesNeighbors: SeriesNeighbors,
    onOpenEntry: (Int) -> Unit,
    showThumbnails: Boolean,
    incognitoModeEnabled: Boolean,
    compactContent: Boolean
) {
    val partCount = seriesNeighbors.parts.size
    val currentIndex = seriesNeighbors.currentPartIndex
    if (partCount <= 1 || currentIndex !in seriesNeighbors.parts.indices) return
    val privacyOverlay = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
    val previewWidth = if (compactContent) 116.dp else 136.dp
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Part ${currentIndex + 1} of $partCount",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .privacyObfuscate(
                    enabled = incognitoModeEnabled,
                    overlayColor = privacyOverlay
                ),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            val previous = seriesNeighbors.previous
            if (previous != null) {
                PartPreview(
                    label = "Previous part",
                    preview = previous,
                    onOpenEntry = onOpenEntry,
                    showThumbnails = showThumbnails,
                    incognitoModeEnabled = incognitoModeEnabled,
                    compactContent = compactContent,
                    modifier = Modifier.width(previewWidth)
                )
            } else {
                Spacer(modifier = Modifier.width(previewWidth))
            }
            Spacer(modifier = Modifier.weight(1f))
            val next = seriesNeighbors.next
            if (next != null) {
                PartPreview(
                    label = "Next part",
                    preview = next,
                    onOpenEntry = onOpenEntry,
                    showThumbnails = showThumbnails,
                    incognitoModeEnabled = incognitoModeEnabled,
                    compactContent = compactContent,
                    modifier = Modifier.width(previewWidth)
                )
            } else {
                Spacer(modifier = Modifier.width(previewWidth))
            }
        }
    }
}

@Composable
private fun PartPreview(
    label: String,
    preview: SeriesEntryPreview,
    onOpenEntry: (Int) -> Unit,
    showThumbnails: Boolean,
    incognitoModeEnabled: Boolean,
    compactContent: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        RelatedEntryPreviewCard(
            entry = RelatedEntryEntity(
                code = preview.code,
                title = preview.title,
                subtitle = "",
                thumbnailUrl = preview.thumbnailUrl,
                numPages = preview.numPages
            ),
            onOpenEntry = onOpenEntry,
            showThumbnails = showThumbnails,
            incognitoModeEnabled = incognitoModeEnabled,
            compactContent = compactContent,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RelatedEntryPreviewCard(
    entry: RelatedEntryEntity,
    onOpenEntry: (Int) -> Unit,
    showThumbnails: Boolean,
    incognitoModeEnabled: Boolean,
    compactContent: Boolean,
    modifier: Modifier = Modifier
) {
    val privacyOverlay = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = modifier
            .clip(shape)
            .clickable(
                enabled = !incognitoModeEnabled,
                interactionSource = interactionSource,
                indication = null
            ) { onOpenEntry(entry.code) },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showThumbnails && entry.thumbnailUrl.isNotBlank()) {
                ThumbnailImage(
                    thumbnailUrl = entry.thumbnailUrl,
                    backupCode = entry.code,
                    contentDescription = "Cover for related code ${entry.code}",
                    obscure = incognitoModeEnabled,
                    preferLowRes = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (compactContent) 104.dp else 122.dp)
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = entry.title,
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append("#${entry.code}")
                        if (entry.numPages > 0) append(" - ${entry.numPages} pages")
                    },
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RelatedEntryCarousel(
    entries: List<RelatedEntryEntity>,
    onOpenEntry: (Int) -> Unit,
    showThumbnails: Boolean,
    incognitoModeEnabled: Boolean,
    compactContent: Boolean
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 1.dp)
    ) {
        items(entries, key = { it.code }, contentType = { "related_entry" }) { entry ->
            RelatedEntryPreviewCard(
                entry = entry,
                onOpenEntry = onOpenEntry,
                showThumbnails = showThumbnails,
                incognitoModeEnabled = incognitoModeEnabled,
                compactContent = compactContent,
                modifier = Modifier.width(if (compactContent) 116.dp else 136.dp)
            )
        }
    }
}

@Composable
internal fun SelectedEntrySummarySkeleton(
    summary: EntryRow,
    loading: Boolean,
    displayedRating: Float,
    showThumbnails: Boolean,
    incognitoModeEnabled: Boolean,
    privacyOverlay: Color,
    detailThumbnailHeight: Dp,
    detailBodyStyle: TextStyle,
    detailTitleStyle: TextStyle,
    onCopyCode: (Int) -> Unit,
    onSelectedThumbnailClick: (Int, String, String) -> Unit
) {
    val shimmerAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (loading) 0.72f else 0.34f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 700,
            easing = FastOutSlowInEasing
        ),
        label = "selectedEntrySkeletonAlpha"
    )
    if (showThumbnails && summary.thumbnailUrl.isNotBlank()) {
        ThumbnailImage(
            thumbnailUrl = summary.thumbnailUrl,
            backupCode = summary.code,
            contentDescription = "Large cover for code ${summary.code}",
            obscure = incognitoModeEnabled,
            onClick = {
                onSelectedThumbnailClick(
                    summary.code,
                    summary.thumbnailUrl,
                    "Large cover for code ${summary.code}"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(detailThumbnailHeight)
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Code:",
                modifier = Modifier.privacyObfuscate(
                    enabled = incognitoModeEnabled,
                    overlayColor = privacyOverlay
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = { onCopyCode(summary.code) },
                enabled = !incognitoModeEnabled,
                modifier = Modifier.heightIn(min = 0.dp),
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
            ) {
                Text(
                    text = summary.code.toString(),
                    modifier = Modifier.privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    style = detailTitleStyle,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Text(
                text = if (loading) "Loading details" else "Preview",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Text(
        text = "Title: ${summary.title}",
        style = detailBodyStyle,
        modifier = Modifier.privacyObfuscate(
            enabled = incognitoModeEnabled,
            overlayColor = privacyOverlay
        )
    )
    Text(
        text = "Pages: ${if (summary.numPages > 0) summary.numPages.toString() else "-"}",
        style = detailBodyStyle,
        modifier = Modifier.privacyObfuscate(
            enabled = incognitoModeEnabled,
            overlayColor = privacyOverlay
        )
    )
    Row(
        modifier = Modifier.privacyObfuscate(
            enabled = incognitoModeEnabled,
            overlayColor = privacyOverlay
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Rating:", style = detailBodyStyle)
        FractionalRatingStars(
            rating = displayedRating.coerceIn(0f, 5f),
            starSize = 18.dp
        )
        Text(
            text = "(${formatRatingValue(displayedRating.coerceIn(0f, 5f))}/5)",
            style = detailBodyStyle
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Status:",
            style = detailBodyStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.privacyObfuscate(
                enabled = incognitoModeEnabled,
                overlayColor = privacyOverlay
            )
        )
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = (if (summary.isRead) READ_STATE_COLOR else UNREAD_STATE_COLOR).copy(alpha = 0.14f)
        ) {
            Text(
                text = if (summary.isRead) "Read" else "Unread",
                color = if (summary.isRead) READ_STATE_COLOR else UNREAD_STATE_COLOR,
                style = detailBodyStyle,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .privacyObfuscate(
                        enabled = incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    )
            )
        }
    }
    EntryDetailSkeletonLines(compactContent = false, alpha = shimmerAlpha)
}
