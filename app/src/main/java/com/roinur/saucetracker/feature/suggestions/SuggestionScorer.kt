package com.roinur.saucetracker.feature.suggestions

import com.roinur.saucetracker.*
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
import com.roinur.saucetracker.feature.library.creators.*
import com.roinur.saucetracker.feature.library.detail.*
import com.roinur.saucetracker.feature.library.history.*
import com.roinur.saucetracker.feature.library.tags.*
import com.roinur.saucetracker.feature.settings.*
import com.roinur.saucetracker.feature.subscriptions.*
import com.roinur.saucetracker.feature.suggestions.*
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
internal fun suggestionTagTypeWeight(type: String): Float {
    return when (type.trim().lowercase(Locale.US)) {
        "tag" -> 1f
        "parody" -> 0.42f
        "character" -> 0.34f
        "category" -> 0.24f
        "language" -> 0.42f
        "artist", "group" -> 0f
        else -> 0.28f
    }
}

internal fun suggestionWeightForRating(rating: Int, isRead: Boolean): Float {
    // Rating 0 is intentionally neutral; read-only history can still provide a light positive signal.
    return when (rating.coerceIn(0, 5)) {
        5 -> 3f
        4 -> 2f
        3 -> 1f
        2 -> 0f
        1 -> -1f
        else -> if (isRead) 1f else 0f
    }
}

internal data class SuggestionScoreBreakdown(
    val score: Float,
    val rankedTags: List<Pair<String, Float>>,
    val whySuggestedReason: String
)

private fun computeLengthAffinityScore(
    candidateNumPages: Int,
    averageNumPages: Float,
    numPagesDeviation: Float
): Float {
    if (candidateNumPages <= 0 || averageNumPages <= 0f || numPagesDeviation <= 0f) return 0f
    val diff = abs(candidateNumPages.toFloat() - averageNumPages)
    val normalized = diff / numPagesDeviation.coerceAtLeast(1f)
    return when {
        normalized <= 0.35f -> 1.75f
        normalized <= 0.70f -> 1.15f
        normalized <= 1.10f -> 0.55f
        normalized <= 1.60f -> -0.25f
        normalized <= 2.20f -> -0.85f
        else -> -1.35f
    }
}

internal fun scoreSuggestionCandidate(
    candidateNumPages: Int,
    tags: List<GalleryTag>,
    tagWeights: Map<String, Float>,
    tagThemeWeights: Map<String, Float>,
    creatorWeights: Map<String, Float>,
    averageNumPages: Float,
    numPagesDeviation: Float,
    lengthWeight: Float,
    blockedTags: Set<String>
): SuggestionScoreBreakdown {
    var score = 0f
    val rankedTags = mutableListOf<Pair<String, Float>>()
    val reasonParts = mutableListOf<Pair<String, Float>>()
    val lengthScore = computeLengthAffinityScore(
        candidateNumPages = candidateNumPages,
        averageNumPages = averageNumPages,
        numPagesDeviation = numPagesDeviation
    ) * lengthWeight.coerceIn(0f, 2f)
    if (lengthScore != 0f) {
        score += lengthScore
        reasonParts += "Length" to lengthScore
    }
    tags.forEach { tag ->
        val normalized = normalizeTagName(tag.name)
        if (normalized.isBlank()) return@forEach
        if (normalized in IGNORED_SUGGESTION_TAG_NAMES) return@forEach
        if (normalized in blockedTags) {
            return SuggestionScoreBreakdown(score = 0f, rankedTags = emptyList(), whySuggestedReason = "")
        }
        val type = tag.type.trim().lowercase(Locale.US)
        val exactTagScore = if (type == "artist" || type == "group") {
            0f
        } else {
            tagWeights[normalized] ?: 0f
        }
        val creatorScore = if (type == "artist" || type == "group") {
            creatorWeights[normalized] ?: 0f
        } else {
            0f
        }
        val themeScore = if (type == "tag") {
            tagThemeWeights[normalized] ?: 0f
        } else {
            0f
        }
        val tagScore = exactTagScore + creatorScore + themeScore
        if (exactTagScore != 0f) {
            reasonParts += "Exact: $normalized" to exactTagScore
        }
        if (themeScore != 0f) {
            reasonParts += "Theme: $normalized" to themeScore
        }
        if (creatorScore != 0f) {
            reasonParts += "Creator: $normalized" to creatorScore
        }
        if (tagScore != 0f) {
            score += tagScore
            rankedTags += normalized to tagScore
        }
    }
    val whySuggestedReason = reasonParts
        .sortedByDescending { abs(it.second) }
        .map { it.first }
        .distinct()
        .take(3)
        .joinToString(", ")
    return SuggestionScoreBreakdown(score = score, rankedTags = rankedTags, whySuggestedReason = whySuggestedReason)
}

internal fun buildSuggestionTagThemeWeights(
    tagWeights: Map<String, Float>,
    localTagEntryCodes: Map<String, Set<Int>>,
    themeStrength: Float
): Map<String, Float> {
    if (tagWeights.isEmpty() || localTagEntryCodes.size <= 1) return emptyMap()

    val normalizedSets = localTagEntryCodes
        .filterKeys { it.isNotBlank() }
        .mapValues { (_, codes) -> codes.toSet() }
        .filterValues { it.isNotEmpty() }
    if (normalizedSets.size <= 1) return emptyMap()

    val weightedTags = tagWeights.keys.filter { it in normalizedSets }
    if (weightedTags.isEmpty()) return emptyMap()

    val propagated = linkedMapOf<String, MutableList<Float>>()
    weightedTags.forEach { sourceTag ->
        val sourceEntries = normalizedSets[sourceTag].orEmpty()
        if (sourceEntries.isEmpty()) return@forEach
        normalizedSets.forEach { (candidateTag, candidateEntries) ->
            if (candidateTag == sourceTag) return@forEach
            val similarity = computeSuggestionThemeSimilarity(sourceEntries, candidateEntries)
            if (similarity < 0.12f) return@forEach
            val propagatedWeight = (tagWeights[sourceTag] ?: 0f) * similarity * 0.42f * themeStrength.coerceIn(0f, 2f)
            if (abs(propagatedWeight) < 0.035f) return@forEach
            propagated.getOrPut(candidateTag) { mutableListOf() }.add(propagatedWeight)
        }
    }

    return propagated.mapValues { (_, contributions) ->
        contributions
            .sortedByDescending { abs(it) }
            .take(3)
            .sum()
            .coerceIn(-3.5f, 3.5f)
    }
}

internal fun computeSuggestionThemeSimilarity(
    leftEntries: Set<Int>,
    rightEntries: Set<Int>
): Float {
    if (leftEntries.isEmpty() || rightEntries.isEmpty()) return 0f
    val intersection = leftEntries.intersect(rightEntries).size.toFloat()
    if (intersection <= 0f) return 0f
    val smallerSet = min(leftEntries.size, rightEntries.size).toFloat().coerceAtLeast(1f)
    val largerSet = max(leftEntries.size, rightEntries.size).toFloat().coerceAtLeast(1f)
    val union = leftEntries.union(rightEntries).size.toFloat().coerceAtLeast(1f)
    val jaccard = (intersection / union).coerceIn(0f, 1f)
    val overlap = (intersection / smallerSet).coerceIn(0f, 1f)
    val support = (intersection / largerSet).coerceIn(0f, 1f)
    return ((jaccard * 0.45f) + (overlap * 0.40f) + (support * 0.15f)).coerceIn(0f, 1f)
}

internal fun filterOutImportedSuggestions(
    suggestions: List<SuggestedEntryRow>,
    importedCodes: Set<Int>
): List<SuggestedEntryRow> {
    if (suggestions.isEmpty() || importedCodes.isEmpty()) return suggestions
    return suggestions.filterNot { it.code in importedCodes }
}
