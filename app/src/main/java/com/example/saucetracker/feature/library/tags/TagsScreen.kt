package com.example.saucetracker.feature.library.tags

import com.example.saucetracker.*
import com.example.saucetracker.feature.library.creators.tagAccentColor
import com.example.saucetracker.core.media.*
import com.example.saucetracker.data.backup.*
import com.example.saucetracker.data.downloads.*
import com.example.saucetracker.data.backup.*
import com.example.saucetracker.data.downloads.*
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
import com.example.saucetracker.core.diagnostics.PerformanceMetrics
import com.example.saucetracker.core.media.*
import com.example.saucetracker.core.media.computeDHash64
import com.example.saucetracker.core.network.HttpClientFactory
import com.example.saucetracker.core.network.HttpClientProfile
import com.example.saucetracker.core.preferences.KEY_ACCENT_MODE
import com.example.saucetracker.core.preferences.KEY_ADAPTIVE_SCROLL_THUMBNAILS
import com.example.saucetracker.core.preferences.KEY_APP_LOCK_BIOMETRIC_ENABLED
import com.example.saucetracker.core.preferences.KEY_APP_LOCK_ENABLED
import com.example.saucetracker.core.preferences.KEY_APPLY_BLOCKED_TAGS_HOME
import com.example.saucetracker.core.preferences.KEY_APPLY_BLOCKED_TAGS_SEARCH
import com.example.saucetracker.core.preferences.KEY_AUTO_BACKUP_TREE_URI
import com.example.saucetracker.core.preferences.KEY_BROWSER_DUPLICATE_CHECK_MODE
import com.example.saucetracker.core.preferences.KEY_CUNNY_MODE_ARMED
import com.example.saucetracker.core.preferences.KEY_DEFAULT_CREATOR_SORT_DIRECTION
import com.example.saucetracker.core.preferences.KEY_DEFAULT_CREATOR_SORT_FIELD
import com.example.saucetracker.core.preferences.KEY_DEFAULT_ENTRY_SORT_DIRECTION
import com.example.saucetracker.core.preferences.KEY_DEFAULT_ENTRY_SORT_FIELD
import com.example.saucetracker.core.preferences.KEY_DEFAULT_TAG_SORT_DIRECTION
import com.example.saucetracker.core.preferences.KEY_DEFAULT_TAG_SORT_FIELD
import com.example.saucetracker.core.preferences.KEY_DESKTOP_BRIDGE_ENABLED
import com.example.saucetracker.core.preferences.KEY_DESKTOP_BRIDGE_PORT
import com.example.saucetracker.core.preferences.KEY_ENTRY_FILTER_CYCLE_ORDER
import com.example.saucetracker.core.preferences.KEY_ENTRY_PIN_PRIORITY
import com.example.saucetracker.core.preferences.KEY_EXPERIMENTAL_DASHBOARD_LONG_PRESS
import com.example.saucetracker.core.preferences.KEY_EXPERIMENTAL_FILTER_STATUS_STRIP
import com.example.saucetracker.core.preferences.KEY_EXPERIMENTAL_LAZY_ENTRY_DETAIL
import com.example.saucetracker.core.preferences.KEY_EXPERIMENTAL_SUBSCRIPTION_INBOX
import com.example.saucetracker.core.preferences.KEY_GALLERY_COLUMNS
import com.example.saucetracker.core.preferences.KEY_HOME_SECTION_ORDER
import com.example.saucetracker.core.preferences.KEY_INCOGNITO_MODE_ENABLED
import com.example.saucetracker.core.preferences.KEY_LEGACY_HOME_UI
import com.example.saucetracker.core.preferences.KEY_PERFORMANCE_OVERLAY_ENABLED
import com.example.saucetracker.core.preferences.KEY_PRELOAD_ON_LAUNCH
import com.example.saucetracker.core.preferences.KEY_PRELOAD_PERCENT
import com.example.saucetracker.core.preferences.KEY_PURE_GALLERY_MODE
import com.example.saucetracker.core.preferences.KEY_SHOW_THUMBNAILS
import com.example.saucetracker.core.preferences.KEY_SUBSCRIPTION_REFRESH_INTERVAL_HOURS
import com.example.saucetracker.core.preferences.KEY_SUGGESTION_HIDDEN_CODES
import com.example.saucetracker.core.preferences.KEY_SUGGESTION_HIDDEN_ENTRIES
import com.example.saucetracker.core.preferences.KEY_SUGGESTION_THEME_STRENGTH
import com.example.saucetracker.core.preferences.KEY_SUGGESTION_WEIGHT_PREFIX
import com.example.saucetracker.core.preferences.KEY_THEME_MODE
import com.example.saucetracker.core.preferences.SaucePreferences
import com.example.saucetracker.core.security.AppLockController
import com.example.saucetracker.core.ui.privacy.privacyObfuscate
import com.example.saucetracker.data.repository.HeatmapRepository
import com.example.saucetracker.data.repository.LibraryRepository
import com.example.saucetracker.data.repository.SubscriptionRepository
import com.example.saucetracker.data.repository.SuggestionsRepository
import com.example.saucetracker.feature.heatmap.HeatmapCanvas
import com.example.saucetracker.feature.heatmap.HeatmapEngine
import com.example.saucetracker.feature.heatmap.HeatmapScreen
import com.example.saucetracker.feature.heatmap.HeatmapLayoutCache
import com.example.saucetracker.feature.heatmap.HeatmapThumbnailLoader
import com.example.saucetracker.feature.experimentalgallery.ExperimentalGalleryActivity
import com.example.saucetracker.feature.slideshow.SlideshowHorizontalDirection
import com.example.saucetracker.feature.slideshow.loadSlideshowHorizontalDirection
import com.example.saucetracker.feature.slideshow.storeSlideshowHorizontalDirection
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
@OptIn(ExperimentalFoundationApi::class)
internal fun ModernTagsPage(
    tags: List<TagCountRow>,
    listState: LazyListState,
    selectedIds: Set<Long>,
    incognitoModeEnabled: Boolean,
    isSubscribed: (TagCountRow) -> Boolean,
    onTagClick: (Long) -> Unit,
    onToggleSubscription: (TagCountRow) -> Unit,
    onConfigureSubscription: (TagCountRow) -> Unit,
    onClearFilter: () -> Unit,
    onSortByName: () -> Unit,
    onSortByType: () -> Unit,
    onSortByCount: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Tags", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(
                        "${tags.size} visible • ${selectedIds.size} selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ImmediateActionText(
                    label = "Clear",
                    onAction = onClearFilter,
                    textStyle = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    enabled = selectedIds.isNotEmpty()
                )
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    EntrySortChip(
                        label = "Name",
                        selected = false,
                        activeDirection = null,
                        onClick = onSortByName
                    )
                }
                item {
                    EntrySortChip(
                        label = "Type",
                        selected = false,
                        activeDirection = null,
                        onClick = onSortByType
                    )
                }
                item {
                    EntrySortChip(
                        label = "Count",
                        selected = false,
                        activeDirection = null,
                        onClick = onSortByCount
                    )
                }
            }
            if (tags.isEmpty()) {
                Text("No tags match the current search/filter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 720.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tags, key = { it.id }) { tag ->
                        val selected = tag.id in selectedIds
                        val subscribed = isSubscribed(tag)
                        val rowShape = RoundedCornerShape(16.dp)
                        val accent = tagAccentColor(tag.type, tag.name)
                        val bellInteraction = remember(tag.id) { MutableInteractionSource() }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(rowShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, rowShape)
                                .border(
                                    1.dp,
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.70f)
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                    },
                                    rowShape
                                )
                                .clickable(enabled = !incognitoModeEnabled) { onTagClick(tag.id) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        accent.copy(alpha = if (selected) 0.30f else 0.20f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (selected) "◆" else "◇",
                                    color = accent,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tag.name,
                                    modifier = Modifier.privacyObfuscate(
                                        enabled = incognitoModeEnabled,
                                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = tag.type,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = tag.count.toString(),
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                                        RoundedCornerShape(999.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .combinedClickable(
                                        enabled = !incognitoModeEnabled,
                                        interactionSource = bellInteraction,
                                        indication = rememberRipple(bounded = true, radius = 20.dp),
                                        onClick = { onToggleSubscription(tag) },
                                        onLongClick = { onConfigureSubscription(tag) }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = if (subscribed) {
                                            R.drawable.ic_notifications_24
                                        } else {
                                            R.drawable.ic_notifications_none_24
                                        }
                                    ),
                                    contentDescription = if (subscribed) {
                                        "Subscribed to ${tag.name}"
                                    } else {
                                        "Subscribe to ${tag.name}"
                                    },
                                    tint = if (subscribed) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
    }
}
