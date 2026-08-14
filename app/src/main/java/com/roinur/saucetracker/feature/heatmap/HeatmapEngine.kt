package com.roinur.saucetracker.feature.heatmap

import com.roinur.saucetracker.*
import com.roinur.saucetracker.feature.suggestions.computeSuggestionThemeSimilarity
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

internal object HeatmapEngine {
private data class HeatmapEdge(
    val leftIndex: Int,
    val rightIndex: Int,
    val weight: Float
)

private data class HeatmapGraphData(
    val sortedSeeds: List<TagGraphSeed>,
    val pairScores: Array<FloatArray>,
    val edges: List<HeatmapEdge>,
    val adjacency: Array<List<Pair<Int, Float>>>,
    val weightedDegree: FloatArray,
    val strongestNeighborsByIndex: List<List<Int>>
)

private data class HeatmapCommunityLayout(
    val members: List<Int>,
    val localPositions: Map<Int, Pair<Float, Float>>,
    val radius: Float
)

private fun normalizeHeatmapEntryCodes(codes: IntArray): IntArray {
    return codes
        .distinct()
        .sorted()
        .toIntArray()
}

private fun computeSortedIntersectionSize(left: IntArray, right: IntArray): Int {
    var leftIndex = 0
    var rightIndex = 0
    var intersection = 0
    while (leftIndex < left.size && rightIndex < right.size) {
        when {
            left[leftIndex] == right[rightIndex] -> {
                intersection += 1
                leftIndex += 1
                rightIndex += 1
            }
            left[leftIndex] < right[rightIndex] -> leftIndex += 1
            else -> rightIndex += 1
        }
    }
    return intersection
}

private fun computeHeatmapSimilarityScore(
    leftEntries: IntArray,
    rightEntries: IntArray,
    totalEntryCount: Int
): Float {
    if (leftEntries.isEmpty() || rightEntries.isEmpty() || totalEntryCount <= 1) return 0f
    val intersection = computeSortedIntersectionSize(leftEntries, rightEntries)
    if (intersection <= 0) return 0f

    val leftSize = leftEntries.size.toDouble().coerceAtLeast(1.0)
    val rightSize = rightEntries.size.toDouble().coerceAtLeast(1.0)
    val smaller = min(leftSize, rightSize)
    val larger = max(leftSize, rightSize)
    val union = (leftEntries.size + rightEntries.size - intersection).toDouble().coerceAtLeast(1.0)
    val overlap = (intersection.toDouble() / smaller).coerceIn(0.0, 1.0)
    val jaccard = (intersection.toDouble() / union).coerceIn(0.0, 1.0)

    val total = totalEntryCount.toDouble().coerceAtLeast(union)
    val px = (leftSize / total).coerceIn(0.0000001, 1.0)
    val py = (rightSize / total).coerceIn(0.0000001, 1.0)
    val pxy = (intersection.toDouble() / total).coerceIn(0.0000001, 1.0)
    val npmi = if (pxy >= 1.0) {
        1.0
    } else {
        val denominator = (-ln(pxy)).coerceAtLeast(0.0000001)
        (ln((pxy / (px * py)).coerceAtLeast(0.0000001)) / denominator).coerceIn(-1.0, 1.0)
    }
    val positiveNpmi = npmi.coerceAtLeast(0.0)
    val balance = sqrt((smaller / larger).coerceIn(0.0, 1.0))
    val confidence = (intersection.toDouble() / (intersection.toDouble() + 1.75)).coerceIn(0.0, 1.0)
    val score = (
        (overlap * 0.38) +
            (jaccard * 0.24) +
            (positiveNpmi * 0.38)
        ) * (0.80 + (0.20 * balance)) * confidence
    return score.toFloat().coerceIn(0f, 1f)
}

private fun heatmapFallbackVector(first: Int, second: Int): Pair<Float, Float> {
    val degrees = (((first + 1) * 37) + ((second + 1) * 91)) % 360
    val radians = Math.toRadians(degrees.toDouble())
    return cos(radians).toFloat() to sin(radians).toFloat()
}

private fun buildHeatmapGraphData(seeds: List<TagGraphSeed>): HeatmapGraphData {
    val sortedSeeds = seeds.sortedBy { it.name.lowercase(Locale.US) }
    val normalizedEntryCodes = sortedSeeds.map { seed ->
        normalizeHeatmapEntryCodes(seed.entryCodes)
    }
    val count = sortedSeeds.size
    val pairScores = Array(count) { FloatArray(count) }
    val strongestNeighborsByIndex = MutableList(count) { emptyList<Int>() }
    val directedNeighbors = Array(count) { linkedSetOf<Int>() }
    val bestScoreByIndex = FloatArray(count)
    val allEntries = linkedSetOf<Int>()
    normalizedEntryCodes.forEach { codes ->
        codes.forEach { code -> allEntries += code }
    }
    val totalEntryCount = allEntries.size.coerceAtLeast(1)

    for (leftIndex in 0 until count) {
        pairScores[leftIndex][leftIndex] = 1f
        for (rightIndex in (leftIndex + 1) until count) {
            val score = computeHeatmapSimilarityScore(
                leftEntries = normalizedEntryCodes[leftIndex],
                rightEntries = normalizedEntryCodes[rightIndex],
                totalEntryCount = totalEntryCount
            )
            pairScores[leftIndex][rightIndex] = score
            pairScores[rightIndex][leftIndex] = score
        }
    }

    val topK = when {
        count <= 12 -> 3
        count <= 36 -> 4
        else -> 5
    }
    for (index in 0 until count) {
        val candidates = (0 until count)
            .asSequence()
            .filter { it != index }
            .map { otherIndex -> otherIndex to pairScores[index][otherIndex] }
            .filter { it.second > 0f }
            .sortedWith(
                compareByDescending<Pair<Int, Float>> { it.second }
                    .thenBy { sortedSeeds[it.first].name.lowercase(Locale.US) }
            )
            .toList()
        strongestNeighborsByIndex[index] = candidates
            .filter { it.second >= 0.07f }
            .take(8)
            .map { it.first }
        bestScoreByIndex[index] = candidates.firstOrNull()?.second ?: 0f
        val selectionFloor = max(0.10f, bestScoreByIndex[index] * 0.42f)
        val chosen = candidates
            .filter { it.second >= selectionFloor }
            .take(topK)
            .map { it.first }
            .toMutableList()
        if (chosen.isEmpty()) {
            candidates.firstOrNull { it.second >= 0.05f }?.let { chosen += it.first }
        }
        chosen.forEach { directedNeighbors[index] += it }
    }

    val edgeWeightsByPair = linkedMapOf<Pair<Int, Int>, Float>()
    for (leftIndex in 0 until count) {
        directedNeighbors[leftIndex].forEach { rightIndex ->
            val pair = if (leftIndex < rightIndex) leftIndex to rightIndex else rightIndex to leftIndex
            val score = pairScores[leftIndex][rightIndex]
            val mutual = leftIndex in directedNeighbors[rightIndex]
            val strongBridgeFloor = max(bestScoreByIndex[leftIndex], bestScoreByIndex[rightIndex]) * 0.58f
            if (!mutual && score < max(0.16f, strongBridgeFloor)) return@forEach
            edgeWeightsByPair[pair] = max(edgeWeightsByPair[pair] ?: 0f, score)
        }
    }

    for (index in 0 until count) {
        val hasEdge = edgeWeightsByPair.keys.any { it.first == index || it.second == index }
        if (hasEdge) continue
        val fallback = strongestNeighborsByIndex[index].firstOrNull() ?: continue
        val pair = if (index < fallback) index to fallback else fallback to index
        edgeWeightsByPair[pair] = max(edgeWeightsByPair[pair] ?: 0f, pairScores[index][fallback])
    }

    val edges = edgeWeightsByPair.entries
        .map { (pair, weight) ->
            HeatmapEdge(
                leftIndex = pair.first,
                rightIndex = pair.second,
                weight = weight.coerceIn(0f, 1f)
            )
        }
        .sortedWith(
            compareBy<HeatmapEdge> { sortedSeeds[it.leftIndex].name.lowercase(Locale.US) }
                .thenBy { sortedSeeds[it.rightIndex].name.lowercase(Locale.US) }
        )

    val adjacency = Array(count) { mutableListOf<Pair<Int, Float>>() }
    val weightedDegree = FloatArray(count)
    edges.forEach { edge ->
        adjacency[edge.leftIndex] += edge.rightIndex to edge.weight
        adjacency[edge.rightIndex] += edge.leftIndex to edge.weight
        weightedDegree[edge.leftIndex] += edge.weight
        weightedDegree[edge.rightIndex] += edge.weight
    }

    return HeatmapGraphData(
        sortedSeeds = sortedSeeds,
        pairScores = pairScores,
        edges = edges,
        adjacency = Array(count) { index ->
            adjacency[index]
                .sortedWith(
                    compareByDescending<Pair<Int, Float>> { it.second }
                        .thenBy { sortedSeeds[it.first].name.lowercase(Locale.US) }
                )
                .toList()
        },
        weightedDegree = weightedDegree,
        strongestNeighborsByIndex = strongestNeighborsByIndex
    )
}

private fun detectHeatmapCommunities(graphData: HeatmapGraphData): List<List<Int>> {
    val count = graphData.sortedSeeds.size
    if (count == 0) return emptyList()
    if (graphData.edges.isEmpty()) return (0 until count).map { listOf(it) }

    val communityByNode = IntArray(count) { it }
    val communityDegree = FloatArray(count) { graphData.weightedDegree[it] }
    val moveOrder = (0 until count).sortedWith(
        compareByDescending<Int> { graphData.weightedDegree[it] }
            .thenBy { graphData.sortedSeeds[it].name.lowercase(Locale.US) }
    )
    val resolution = 1.08f
    val twoM = graphData.weightedDegree.sum().coerceAtLeast(0.0001f)

    repeat(16) {
        var movedAny = false
        moveOrder.forEach { nodeIndex ->
            val nodeDegree = graphData.weightedDegree[nodeIndex]
            if (nodeDegree <= 0f) return@forEach
            val currentCommunity = communityByNode[nodeIndex]
            val candidateWeights = linkedMapOf<Int, Float>()
            graphData.adjacency[nodeIndex].forEach { (neighborIndex, weight) ->
                val candidateCommunity = communityByNode[neighborIndex]
                candidateWeights[candidateCommunity] = (candidateWeights[candidateCommunity] ?: 0f) + weight
            }
            if (!candidateWeights.containsKey(currentCommunity)) {
                candidateWeights[currentCommunity] = 0f
            }

            communityDegree[currentCommunity] -= nodeDegree
            val currentScore = (candidateWeights[currentCommunity] ?: 0f) -
                ((resolution * nodeDegree * communityDegree[currentCommunity]) / twoM)
            var bestCommunity = currentCommunity
            var bestScore = currentScore
            candidateWeights.entries
                .sortedWith(
                    compareByDescending<Map.Entry<Int, Float>> { it.value }
                        .thenBy { it.key }
                )
                .forEach { (candidateCommunity, weightToCommunity) ->
                    val candidateScore = weightToCommunity -
                        ((resolution * nodeDegree * communityDegree[candidateCommunity]) / twoM)
                    if (candidateScore > bestScore + 0.0001f ||
                        (abs(candidateScore - bestScore) <= 0.0001f && candidateCommunity < bestCommunity)
                    ) {
                        bestCommunity = candidateCommunity
                        bestScore = candidateScore
                    }
                }

            communityByNode[nodeIndex] = bestCommunity
            communityDegree[bestCommunity] += nodeDegree
            if (bestCommunity != currentCommunity) {
                movedAny = true
            }
        }
        if (!movedAny) return@repeat
    }

    val groupedMembers = linkedMapOf<Int, MutableList<Int>>()
    communityByNode.forEachIndexed { nodeIndex, community ->
        groupedMembers.getOrPut(community) { mutableListOf() }.add(nodeIndex)
    }

    val splitCommunities = mutableListOf<List<Int>>()
    groupedMembers.values.forEach { members ->
        val memberSet = members.toSet()
        val visited = mutableSetOf<Int>()
        members.sortedBy { graphData.sortedSeeds[it].name.lowercase(Locale.US) }.forEach { start ->
            if (!visited.add(start)) return@forEach
            val queue = ArrayDeque<Int>()
            queue.add(start)
            val component = mutableListOf<Int>()
            while (queue.isNotEmpty()) {
                val node = queue.removeFirst()
                component += node
                graphData.adjacency[node].forEach { (neighborIndex, _) ->
                    if (neighborIndex !in memberSet || !visited.add(neighborIndex)) return@forEach
                    queue.addLast(neighborIndex)
                }
            }
            splitCommunities += component.sortedBy { graphData.sortedSeeds[it].name.lowercase(Locale.US) }
        }
    }

    return splitCommunities.sortedWith(
        compareByDescending<List<Int>> { community ->
            community.sumOf { graphData.sortedSeeds[it].localCount.coerceAtLeast(1) }
        }
            .thenByDescending { community -> community.size }
            .thenBy { community -> graphData.sortedSeeds[community.first()].name.lowercase(Locale.US) }
    )
}

private fun layoutHeatmapCommunityLocally(
    graphData: HeatmapGraphData,
    members: List<Int>
): HeatmapCommunityLayout {
    if (members.isEmpty()) {
        return HeatmapCommunityLayout(
            members = emptyList(),
            localPositions = emptyMap(),
            radius = 0.75f
        )
    }
    if (members.size == 1) {
        return HeatmapCommunityLayout(
            members = members,
            localPositions = mapOf(members.first() to (0f to 0f)),
            radius = 0.95f
        )
    }

    val memberSet = members.toSet()
    val localDegree = members.associateWith { member ->
        graphData.adjacency[member]
            .filter { it.first in memberSet }
            .sumOf { it.second.toDouble() }
            .toFloat()
    }
    val orderedMembers = members.sortedWith(
        compareByDescending<Int> { localDegree[it] ?: 0f }
            .thenByDescending { graphData.sortedSeeds[it].localCount }
            .thenBy { graphData.sortedSeeds[it].name.lowercase(Locale.US) }
    )
    val anchor = orderedMembers.first()
    val nodeRadius = members.associateWith { member ->
        0.26f + (0.34f * sqrt(graphData.sortedSeeds[member].localCount.coerceAtLeast(1).toFloat()))
    }
    val graphDistance = mutableMapOf(anchor to 0)
    val queue = ArrayDeque<Int>()
    queue.add(anchor)
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        val currentDistance = graphDistance[current] ?: 0
        graphData.adjacency[current]
            .asSequence()
            .filter { it.first in memberSet }
            .map { it.first }
            .sortedBy { graphData.sortedSeeds[it].name.lowercase(Locale.US) }
            .forEach { neighbor ->
                if (graphDistance.containsKey(neighbor)) return@forEach
                graphDistance[neighbor] = currentDistance + 1
                queue.addLast(neighbor)
            }
    }
    var fallbackDistance = (graphDistance.values.maxOrNull() ?: 0) + 1
    members.forEach { member ->
        if (!graphDistance.containsKey(member)) {
            graphDistance[member] = fallbackDistance
            fallbackDistance += 1
        }
    }

    val positions = members.associateWith { floatArrayOf(0f, 0f) }.toMutableMap()
    val angleSeed = Math.toRadians(
        ((graphData.sortedSeeds[anchor].normalizedName.hashCode() and 0x7fffffff) % 360).toDouble()
    )
    val membersByDistance = members.groupBy { graphDistance[it] ?: 0 }
    positions[anchor] = floatArrayOf(0f, 0f)
    membersByDistance.toSortedMap().forEach { (distance, layerMembers) ->
        if (distance == 0) return@forEach
        val sortedLayer = layerMembers.sortedWith(
            compareByDescending<Int> { localDegree[it] ?: 0f }
                .thenBy { graphData.sortedSeeds[it].name.lowercase(Locale.US) }
        )
        val radius = (distance.toFloat() * 1.45f) + (sqrt(sortedLayer.size.toFloat()) * 0.22f)
        sortedLayer.forEachIndexed { slotIndex, member ->
            val angle = angleSeed + (distance * 0.37) + ((Math.PI * 2.0 * slotIndex) / sortedLayer.size.coerceAtLeast(1))
            positions[member] = floatArrayOf(
                (cos(angle) * radius).toFloat(),
                (sin(angle) * radius).toFloat()
            )
        }
    }

    val localEdges = graphData.edges.filter { it.leftIndex in memberSet && it.rightIndex in memberSet }
    repeat(180) { iteration ->
        val deltas = members.associateWith { floatArrayOf(0f, 0f) }.toMutableMap()
        val cooling = 0.26f - (0.16f * (iteration / 179f))
        members.forEach { member ->
            val pos = positions[member] ?: return@forEach
            val pullStrength = if (member == anchor) 0.018f else 0.010f + ((localDegree[member] ?: 0f) * 0.004f)
            deltas[member]?.let { delta ->
                delta[0] -= pos[0] * pullStrength
                delta[1] -= pos[1] * pullStrength
            }
        }

        localEdges.forEach { edge ->
            val leftPos = positions[edge.leftIndex] ?: return@forEach
            val rightPos = positions[edge.rightIndex] ?: return@forEach
            var dx = rightPos[0] - leftPos[0]
            var dy = rightPos[1] - leftPos[1]
            var distance = sqrt((dx * dx) + (dy * dy) + 0.000001f)
            if (distance < 0.0001f) {
                val fallback = heatmapFallbackVector(edge.leftIndex, edge.rightIndex)
                dx = fallback.first
                dy = fallback.second
                distance = 1f
            }
            val nx = dx / distance
            val ny = dy / distance
            val desired = (
                (nodeRadius[edge.leftIndex] ?: 0.4f) +
                    (nodeRadius[edge.rightIndex] ?: 0.4f) +
                    0.42f +
                    ((1f - edge.weight).coerceIn(0f, 1f) * 0.90f)
                ).coerceAtLeast(0.65f)
            val force = (distance - desired) * (0.055f + (edge.weight * 0.065f))
            deltas[edge.leftIndex]?.let { delta ->
                delta[0] += nx * force
                delta[1] += ny * force
            }
            deltas[edge.rightIndex]?.let { delta ->
                delta[0] -= nx * force
                delta[1] -= ny * force
            }
        }

        for (leftOffset in members.indices) {
            val left = members[leftOffset]
            val leftPos = positions[left] ?: continue
            for (rightOffset in (leftOffset + 1) until members.size) {
                val right = members[rightOffset]
                val rightPos = positions[right] ?: continue
                var dx = rightPos[0] - leftPos[0]
                var dy = rightPos[1] - leftPos[1]
                var distance = sqrt((dx * dx) + (dy * dy) + 0.000001f)
                if (distance < 0.0001f) {
                    val fallback = heatmapFallbackVector(left, right)
                    dx = fallback.first
                    dy = fallback.second
                    distance = 1f
                }
                val nx = dx / distance
                val ny = dy / distance
                val linkedWeight = graphData.adjacency[left]
                    .firstOrNull { it.first == right }
                    ?.second
                    ?: 0f
                val minimumDistance = (
                    (nodeRadius[left] ?: 0.4f) +
                        (nodeRadius[right] ?: 0.4f) +
                        if (linkedWeight > 0f) 0.34f else 0.72f
                    ).coerceAtLeast(0.9f)
                val push = if (distance < minimumDistance) {
                    (minimumDistance - distance) * 0.18f
                } else {
                    (0.024f / ((distance * distance) + 0.4f)) * if (linkedWeight > 0f) 0.35f else 1f
                }
                deltas[left]?.let { delta ->
                    delta[0] -= nx * push
                    delta[1] -= ny * push
                }
                deltas[right]?.let { delta ->
                    delta[0] += nx * push
                    delta[1] += ny * push
                }
            }
        }

        members.forEach { member ->
            val pos = positions[member] ?: return@forEach
            val delta = deltas[member] ?: return@forEach
            pos[0] += delta[0] * cooling
            pos[1] += delta[1] * cooling
        }
    }

    val centroidX = members.mapNotNull { positions[it]?.get(0) }.average().toFloat()
    val centroidY = members.mapNotNull { positions[it]?.get(1) }.average().toFloat()
    val centeredPositions = members.associateWith { member ->
        val pos = positions[member] ?: floatArrayOf(0f, 0f)
        (pos[0] - centroidX) to (pos[1] - centroidY)
    }
    val radius = members.maxOfOrNull { member ->
        val pos = centeredPositions[member] ?: (0f to 0f)
        sqrt((pos.first * pos.first) + (pos.second * pos.second)) + ((nodeRadius[member] ?: 0.4f) * 0.85f)
    }?.coerceAtLeast(0.9f) ?: 0.9f

    return HeatmapCommunityLayout(
        members = members.sortedBy { graphData.sortedSeeds[it].name.lowercase(Locale.US) },
        localPositions = centeredPositions,
        radius = radius
    )
}

private fun computeHeatmapCommunityCenters(
    graphData: HeatmapGraphData,
    communities: List<HeatmapCommunityLayout>
): Map<Int, Pair<Float, Float>> {
    if (communities.isEmpty()) return emptyMap()
    if (communities.size == 1) return mapOf(0 to (0f to 0f))

    val positions = communities.mapIndexed { index, _ ->
        val spiralRadius = if (index == 0) 0.6f else 2.4f + (sqrt(index.toFloat()) * 2.1f)
        val angle = 0.35 + (index * 2.399963229728653)
        index to floatArrayOf(
            (cos(angle) * spiralRadius).toFloat(),
            (sin(angle) * spiralRadius).toFloat()
        )
    }.toMap().toMutableMap()

    val communityByMember = mutableMapOf<Int, Int>()
    communities.forEachIndexed { communityIndex, community ->
        community.members.forEach { member -> communityByMember[member] = communityIndex }
    }

    val bridgeStrengthByPair = linkedMapOf<Pair<Int, Int>, Float>()
    graphData.edges.forEach { edge ->
        val leftCommunity = communityByMember[edge.leftIndex] ?: return@forEach
        val rightCommunity = communityByMember[edge.rightIndex] ?: return@forEach
        if (leftCommunity == rightCommunity) return@forEach
        val pair = if (leftCommunity < rightCommunity) {
            leftCommunity to rightCommunity
        } else {
            rightCommunity to leftCommunity
        }
        val current = bridgeStrengthByPair[pair] ?: 0f
        bridgeStrengthByPair[pair] = max(current, edge.weight)
    }

    repeat(260) { iteration ->
        val deltas = communities.indices.associateWith { floatArrayOf(0f, 0f) }.toMutableMap()
        val cooling = 0.24f - (0.14f * (iteration / 259f))

        communities.indices.forEach { leftIndex ->
            val leftPos = positions[leftIndex] ?: return@forEach
            for (rightIndex in (leftIndex + 1) until communities.size) {
                val rightPos = positions[rightIndex] ?: continue
                var dx = rightPos[0] - leftPos[0]
                var dy = rightPos[1] - leftPos[1]
                var distance = sqrt((dx * dx) + (dy * dy) + 0.000001f)
                if (distance < 0.0001f) {
                    val fallback = heatmapFallbackVector(leftIndex, rightIndex)
                    dx = fallback.first
                    dy = fallback.second
                    distance = 1f
                }
                val nx = dx / distance
                val ny = dy / distance
                val minimumDistance = communities[leftIndex].radius + communities[rightIndex].radius + 1.45f
                val push = if (distance < minimumDistance) {
                    (minimumDistance - distance) * 0.22f
                } else {
                    ((communities[leftIndex].radius + communities[rightIndex].radius) * 0.018f) /
                        ((distance * distance) + 0.8f)
                }
                deltas[leftIndex]?.let { delta ->
                    delta[0] -= nx * push
                    delta[1] -= ny * push
                }
                deltas[rightIndex]?.let { delta ->
                    delta[0] += nx * push
                    delta[1] += ny * push
                }
            }
        }

        bridgeStrengthByPair.forEach { (pair, bridgeStrength) ->
            val leftIndex = pair.first
            val rightIndex = pair.second
            val leftPos = positions[leftIndex] ?: return@forEach
            val rightPos = positions[rightIndex] ?: return@forEach
            var dx = rightPos[0] - leftPos[0]
            var dy = rightPos[1] - leftPos[1]
            var distance = sqrt((dx * dx) + (dy * dy) + 0.000001f)
            if (distance < 0.0001f) {
                val fallback = heatmapFallbackVector(leftIndex, rightIndex)
                dx = fallback.first
                dy = fallback.second
                distance = 1f
            }
            val nx = dx / distance
            val ny = dy / distance
            val desiredDistance = (
                communities[leftIndex].radius +
                    communities[rightIndex].radius +
                    1.05f +
                    ((1f - bridgeStrength).coerceIn(0f, 1f) * 1.85f)
                ).coerceAtLeast(1.4f)
            val force = (distance - desiredDistance) * (0.030f + (bridgeStrength * 0.055f))
            deltas[leftIndex]?.let { delta ->
                delta[0] += nx * force
                delta[1] += ny * force
            }
            deltas[rightIndex]?.let { delta ->
                delta[0] -= nx * force
                delta[1] -= ny * force
            }
        }

        positions.forEach { (communityIndex, pos) ->
            val delta = deltas[communityIndex] ?: return@forEach
            val distanceFromOrigin = sqrt((pos[0] * pos[0]) + (pos[1] * pos[1]) + 0.000001f)
            val originFloor = communities[communityIndex].radius + 0.8f
            if (distanceFromOrigin < originFloor) {
                val nx = pos[0] / distanceFromOrigin.coerceAtLeast(0.0001f)
                val ny = pos[1] / distanceFromOrigin.coerceAtLeast(0.0001f)
                delta[0] += nx * ((originFloor - distanceFromOrigin) * 0.08f)
                delta[1] += ny * ((originFloor - distanceFromOrigin) * 0.08f)
            }
            pos[0] += delta[0] * cooling
            pos[1] += delta[1] * cooling
        }

        val centroidX = positions.values.map { it[0] }.average().toFloat()
        val centroidY = positions.values.map { it[1] }.average().toFloat()
        positions.values.forEach { pos ->
            pos[0] -= centroidX * 0.08f
            pos[1] -= centroidY * 0.08f
        }
    }

    return positions.mapValues { (_, pos) -> pos[0] to pos[1] }
}

private fun buildStrongestTagNeighborsMap(seeds: List<TagGraphSeed>): Map<String, List<String>> {
    if (seeds.isEmpty()) return emptyMap()
    val graphData = buildHeatmapGraphData(seeds)
    return graphData.sortedSeeds.mapIndexed { index, seed ->
        val neighbors = graphData.strongestNeighborsByIndex[index]
            .map { graphData.sortedSeeds[it].normalizedName }
        seed.normalizedName to neighbors
    }.toMap()
}

internal fun computeTagGraphSnapshot(data: TagGraphDataSnapshot): TagGraphSnapshot {
    val seeds = data.seeds
        .asSequence()
        .filter { it.localCount > 0 && it.entryCodes.isNotEmpty() }
        .sortedWith(
            compareByDescending<TagGraphSeed> { it.localCount }
                .thenByDescending { abs(it.ratedSignalSum) }
                .thenBy { it.name.lowercase(Locale.US) }
        )
        .take(90)
        .toList()
    if (seeds.isEmpty()) {
        return TagGraphSnapshot(
            nodes = emptyList(),
            entryNodes = emptyList(),
            strongestNeighborsByTag = emptyMap(),
            totalEntries = data.totalEntries,
            totalRatedEntries = data.totalRatedEntries,
            totalPopularTagUsage = data.totalPopularTagUsage
        )
    }

    val maxPopular = seeds.maxOf { max(it.popularCount, 1) }.toFloat()
    val totalEntries = data.totalEntries.coerceAtLeast(1).toFloat()
    val maxRatedMentions = seeds.maxOf { max(it.ratedMentionCount, 1) }.toFloat()
    val totalPopularTagUsage = data.totalPopularTagUsage.coerceAtLeast(1L).toFloat()

    val rawScores = seeds.associate { seed ->
        val localRate = (seed.localCount.toFloat() / totalEntries).coerceAtLeast(0.00001f)
        val globalShare = (seed.popularCount.toFloat() / totalPopularTagUsage).coerceAtLeast(0.0000001f)
        val ratio = ln(((localRate + 0.0005f) / (globalShare + 0.0005f)).toDouble()).toFloat()
        seed.normalizedName to ratio
    }

    val ratedScores = seeds.associate { seed ->
        val avgSignal = if (seed.ratedMentionCount > 0 && data.totalRatedEntries > 0) {
            seed.ratedSignalSum / seed.ratedMentionCount.toFloat()
        } else {
            0f
        }
        val ratingDirection = (avgSignal / 3f).coerceIn(-1f, 1f)
        val ratedShare = if (data.totalRatedEntries > 0) {
            seed.ratedMentionCount.toFloat() / data.totalRatedEntries.toFloat()
        } else {
            0f
        }
        val globalShare = (seed.popularCount.toFloat() / totalPopularTagUsage).coerceAtLeast(0.0000001f)
        val confidence = sqrt(seed.ratedMentionCount.toFloat() / maxRatedMentions).coerceIn(0f, 1f)
        val ratio = ln(((ratedShare + 0.0005f) / (globalShare + 0.0005f)).toDouble()).toFloat()
        seed.normalizedName to (ratio * ratingDirection * confidence)
    }

    val positions = computeHeatmapLayout(seeds)
    val strongestNeighborsByTag = buildStrongestTagNeighborsMap(seeds)

    val nodes = seeds.map { seed ->
        val globalShare = (seed.popularCount.toFloat() / totalPopularTagUsage).coerceAtLeast(0.0000001f)
        val topGlobalShare = (maxPopular / totalPopularTagUsage).coerceAtLeast(globalShare)
        val popularityY = (0.10f + (
            (ln((globalShare * 1_000_000f + 1f).toDouble()) /
                ln((topGlobalShare * 1_000_000f + 1f).toDouble()))
                .toFloat()
                .coerceIn(0f, 1f) * 0.80f
            )).coerceIn(0f, 1f)
        val rawScore = squashGraphScore(rawScores[seed.normalizedName] ?: 0f, softness = 0.8f)
        val ratedScore = squashGraphScore(ratedScores[seed.normalizedName] ?: 0f, softness = 0.45f)
        val heat = positions[seed.normalizedName] ?: (0.5f to 0.5f)
        TagGraphNode(
            name = seed.name,
            normalizedName = seed.normalizedName,
            localCount = seed.localCount,
            popularCount = seed.popularCount,
            ratedSignalSum = seed.ratedSignalSum,
            ratedMentionCount = seed.ratedMentionCount,
            heatX = heat.first.coerceIn(0f, 1f),
            heatY = heat.second.coerceIn(0f, 1f),
            rawX = ((rawScore * 0.5f) + 0.5f).coerceIn(0f, 1f),
            rawY = popularityY,
            ratedX = ((ratedScore * 0.5f) + 0.5f).coerceIn(0f, 1f),
            ratedY = popularityY
        )
    }
    val nodePositionsByTag = nodes.associateBy { it.normalizedName }
    val rawEntryNodes = data.entrySeeds.mapNotNull { entry ->
        val tagPositions = entry.tagNames.mapNotNull { tagName ->
            nodePositionsByTag[tagName]?.let { it.heatX to it.heatY }
        }
        if (tagPositions.isEmpty()) return@mapNotNull null
        val avgX = tagPositions.map { it.first }.average().toFloat().coerceIn(0f, 1f)
        val avgY = tagPositions.map { it.second }.average().toFloat().coerceIn(0f, 1f)
        TagGraphEntryNode(
            code = entry.code,
            title = entry.title,
            thumbnailUrl = entry.thumbnailUrl,
            rating = entry.rating,
            isRead = entry.isRead,
            pinned = entry.pinned,
            tagNames = entry.tagNames,
            dominantCircleTags = emptyList(),
            boundaryCenterX = avgX,
            boundaryCenterY = avgY,
            boundaryRadiusPx = 0f,
            x = avgX,
            y = avgY
        )
    }
    val entryNodes = spreadTagGraphEntryNodes(rawEntryNodes)
    return TagGraphSnapshot(
        nodes = nodes,
        entryNodes = entryNodes,
        strongestNeighborsByTag = strongestNeighborsByTag,
        totalEntries = data.totalEntries,
        totalRatedEntries = data.totalRatedEntries,
        totalPopularTagUsage = data.totalPopularTagUsage
    )
}

private fun suggestionWeightForGraphRating(rating: Int): Float {
    return when (rating.coerceIn(0, 5)) {
        5 -> 3f
        4 -> 2f
        3 -> 1f
        1 -> -1f
        else -> 0f
    }
}

internal fun buildFilteredTagGraphSnapshot(
    entryRows: List<EntryRow>,
    details: List<EntryDetail>,
    popularTags: List<PopularTagRow>
): TagGraphSnapshot {
    if (details.isEmpty()) {
        return TagGraphSnapshot(
            nodes = emptyList(),
            entryNodes = emptyList(),
            strongestNeighborsByTag = emptyMap(),
            totalEntries = 0,
            totalRatedEntries = 0,
            totalPopularTagUsage = popularTags
                .asSequence()
                .filter { it.type.equals("tag", ignoreCase = true) }
                .sumOf { it.count.coerceAtLeast(0).toLong() }
        )
    }

    val popularCountsByName = popularTags
        .asSequence()
        .filter { it.type.equals("tag", ignoreCase = true) }
        .associate { normalizeTagName(it.name) to it.count.coerceAtLeast(0) }

    data class MutableSeed(
        val name: String,
        val normalizedName: String,
        var localCount: Int = 0,
        var popularCount: Int = 0,
        var ratedSignalSum: Float = 0f,
        var ratedMentionCount: Int = 0,
        val entryCodes: MutableList<Int> = mutableListOf()
    )

    val seedsByName = linkedMapOf<String, MutableSeed>()
    val pinnedByCode = entryRows.associate { it.code to it.pinned }
    val entrySeeds = details.map { detail ->
        val normalizedTagNames = detail.tagsByType["tag"]
            .orEmpty()
            .map(::normalizeTagName)
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        normalizedTagNames.forEach { normalizedName ->
            val seed = seedsByName.getOrPut(normalizedName) {
                MutableSeed(
                    name = detail.tagsByType["tag"].orEmpty().firstOrNull { normalizeTagName(it) == normalizedName }
                        ?: normalizedName,
                    normalizedName = normalizedName,
                    popularCount = popularCountsByName[normalizedName] ?: 0
                )
            }
            seed.localCount += 1
            seed.entryCodes += detail.code
            if (detail.rating > 0) {
                seed.ratedMentionCount += 1
                seed.ratedSignalSum += suggestionWeightForGraphRating(detail.rating)
            }
        }
        TagGraphEntrySeed(
            code = detail.code,
            title = detail.title.ifBlank { "Gallery ${detail.code}" },
            thumbnailUrl = buildThumbnailUrl(detail.mediaId, detail.coverExt),
            rating = detail.rating,
            isRead = detail.isRead,
            pinned = pinnedByCode[detail.code] == true,
            tagNames = normalizedTagNames
        )
    }

    return computeTagGraphSnapshot(
        TagGraphDataSnapshot(
            totalEntries = details.size,
            totalRatedEntries = details.count { it.rating > 0 },
            totalPopularTagUsage = popularTags
                .asSequence()
                .filter { it.type.equals("tag", ignoreCase = true) }
                .sumOf { it.count.coerceAtLeast(0).toLong() },
            seeds = seedsByName.values.map { seed ->
                TagGraphSeed(
                    name = seed.name,
                    normalizedName = seed.normalizedName,
                    localCount = seed.localCount,
                    popularCount = seed.popularCount,
                    ratedSignalSum = seed.ratedSignalSum,
                    ratedMentionCount = seed.ratedMentionCount,
                    entryCodes = seed.entryCodes.distinct().sorted().toIntArray()
                )
            }.sortedWith(
                compareByDescending<TagGraphSeed> { it.localCount }
                    .thenBy { it.name.lowercase(Locale.US) }
            ),
            entrySeeds = entrySeeds
        )
    )
}

private fun spreadTagGraphEntryNodes(nodes: List<TagGraphEntryNode>): List<TagGraphEntryNode> {
    if (nodes.size <= 1) return nodes
    val positions = nodes.associate { it.code to floatArrayOf(it.x, it.y) }.toMutableMap()
    for (iteration in 0 until 45) {
        var moved = false
        for (leftIndex in nodes.indices) {
            val left = nodes[leftIndex]
            val leftPos = positions[left.code] ?: continue
            for (rightIndex in (leftIndex + 1) until nodes.size) {
                val right = nodes[rightIndex]
                val rightPos = positions[right.code] ?: continue
                var dx = rightPos[0] - leftPos[0]
                var dy = rightPos[1] - leftPos[1]
                val distanceSq = (dx * dx) + (dy * dy) + 0.000001f
                val distance = sqrt(distanceSq)
                val minimumDistance = 0.030f
                if (distance < minimumDistance) {
                    moved = true
                    if (distance < 0.0001f) {
                        dx = (((left.code % 17) - 8) * 0.002f)
                        dy = (((right.code % 19) - 9) * 0.002f)
                    }
                    val nx = dx / distance.coerceAtLeast(0.0001f)
                    val ny = dy / distance.coerceAtLeast(0.0001f)
                    val push = (minimumDistance - distance) * 0.52f
                    leftPos[0] = (leftPos[0] - (nx * push)).coerceIn(0.03f, 0.97f)
                    leftPos[1] = (leftPos[1] - (ny * push)).coerceIn(0.03f, 0.97f)
                    rightPos[0] = (rightPos[0] + (nx * push)).coerceIn(0.03f, 0.97f)
                    rightPos[1] = (rightPos[1] + (ny * push)).coerceIn(0.03f, 0.97f)
                }
            }
        }
        if (!moved) break
    }
    return nodes.map { node ->
        val pos = positions[node.code] ?: floatArrayOf(node.x, node.y)
        node.copy(x = pos[0], y = pos[1])
    }
}

private fun layoutTagGraphEntryNodesForCanvas(
    nodes: List<TagGraphEntryNode>,
    tagNodes: List<TagGraphNode>,
    graphWidthPx: Float,
    graphHeightPx: Float,
    minimumVisualSpacingPx: Float
): List<TagGraphEntryNode> {
    if (nodes.size <= 1) return nodes

    data class EntryCircle(
        val tagName: String,
        val label: String,
        val center: FloatArray,
        val radiusPx: Float,
        val memberCount: Int,
        val gravityWeight: Float
    )

    data class EntryBoundary(
        val dominantTags: List<String>,
        val centerX: Float,
        val centerY: Float,
        val radiusPx: Float
    )

    val safeGraphWidthPx = graphWidthPx.coerceAtLeast(1f)
    val safeGraphHeightPx = graphHeightPx.coerceAtLeast(1f)
    val minGraphDimensionPx = min(safeGraphWidthPx, safeGraphHeightPx)
    val safeSpacingPx = minimumVisualSpacingPx.coerceAtLeast(10f) * 1.12f
    val marginX = ((safeSpacingPx * 0.58f) / safeGraphWidthPx).coerceIn(0.025f, 0.13f)
    val marginY = ((safeSpacingPx * 0.58f) / safeGraphHeightPx).coerceIn(0.025f, 0.13f)
    val tagMemberCodes = tagNodes.associate { tag ->
        tag.normalizedName to nodes.asSequence()
            .filter { tag.normalizedName in it.tagNames }
            .map { it.code }
            .toSet()
    }
    val dominantAnchors = selectDominantEntryHeatmapAnchors(nodes, tagNodes)
    val anchorVisualWeightByTag = dominantAnchors.associate { anchor ->
        val memberCodes = tagMemberCodes[anchor.normalizedName].orEmpty()
        val supportStrength = (memberCodes.size.toFloat() / nodes.size.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
        val strongestOverlap = dominantAnchors
            .asSequence()
            .filter { it.normalizedName != anchor.normalizedName }
            .map { other ->
                computeSuggestionThemeSimilarity(
                    leftEntries = memberCodes,
                    rightEntries = tagMemberCodes[other.normalizedName].orEmpty()
                )
            }
            .maxOrNull()
            ?: 0f
        val exclusivityStrength = (1f - strongestOverlap).coerceIn(0f, 1f)
        val edgeDistance = sqrt(
            ((anchor.heatX - 0.5f) * (anchor.heatX - 0.5f)) +
                ((anchor.heatY - 0.5f) * (anchor.heatY - 0.5f))
        )
        val edgeStrength = (edgeDistance / 0.52f).coerceIn(0f, 1f)
        val genericPenalty = when {
            supportStrength <= 0.18f -> 1f
            supportStrength <= 0.30f -> 1f - (((supportStrength - 0.18f) / 0.12f).coerceIn(0f, 1f) * 0.14f)
            else -> 0.86f - (((supportStrength - 0.30f) / 0.45f).coerceIn(0f, 1f) * 0.50f)
        }
        anchor.normalizedName to (
            sqrt(memberCodes.size.toFloat().coerceAtLeast(1f)) *
                (0.34f + (exclusivityStrength * 0.98f)) *
                (0.84f + (edgeStrength * 0.18f)) *
                genericPenalty
            )
    }
    val primaryAnchorByCode = nodes.associate { node ->
        val chosen = dominantAnchors
            .asSequence()
            .filter { it.normalizedName in node.tagNames }
            .maxByOrNull { anchor ->
                anchorVisualWeightByTag[anchor.normalizedName] ?: 0f
            }
            ?.normalizedName
        node.code to chosen
    }
    val circleAssignmentCounts = dominantAnchors.associate { anchor ->
        anchor.normalizedName to nodes.count { node -> primaryAnchorByCode[node.code] == anchor.normalizedName }
    }
    val circles = dominantAnchors.map { anchor ->
        val assignmentCount = circleAssignmentCounts[anchor.normalizedName]?.coerceAtLeast(1) ?: 1
        val gravityWeight = (sqrt(assignmentCount.toFloat()) * 1.35f).coerceAtLeast(1f)
        EntryCircle(
            tagName = anchor.normalizedName,
            label = anchor.name,
            center = floatArrayOf(
                anchor.heatX.coerceIn(marginX, 1f - marginX),
                anchor.heatY.coerceIn(marginY, 1f - marginY)
            ),
            radiusPx = (minGraphDimensionPx * (0.072f + (sqrt(assignmentCount.toFloat()) * 0.018f)))
                .coerceIn(minGraphDimensionPx * 0.085f, minGraphDimensionPx * 0.30f),
            memberCount = assignmentCount,
            gravityWeight = gravityWeight
        )
    }
    val circlesByTag = circles.associateBy { it.tagName }
    val circleWeightByTag = circles.associate { circle ->
        circle.tagName to ((anchorVisualWeightByTag[circle.tagName] ?: circle.gravityWeight) * 1.08f)
    }

    fun sortedMatchedCircles(node: TagGraphEntryNode): List<EntryCircle> {
        return node.tagNames
            .mapNotNull { circlesByTag[it] }
            .distinctBy { it.tagName }
            .sortedWith(
                compareByDescending<EntryCircle> { circleWeightByTag[it.tagName] ?: it.gravityWeight }
                    .thenByDescending { it.memberCount }
                    .thenBy { it.tagName }
            )
    }

    repeat(36) {
        for (leftIndex in circles.indices) {
            val left = circles[leftIndex]
            for (rightIndex in (leftIndex + 1) until circles.size) {
                val right = circles[rightIndex]
                var dxPx = (right.center[0] - left.center[0]) * safeGraphWidthPx
                var dyPx = (right.center[1] - left.center[1]) * safeGraphHeightPx
                var distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx) + 0.000001f)
                val minimumCircleDistancePx = left.radiusPx + right.radiusPx + (safeSpacingPx * 2.6f)
                if (distancePx < minimumCircleDistancePx) {
                    if (distancePx < 0.01f) {
                        val angleDegrees = (((leftIndex + 1) * 67) + ((rightIndex + 1) * 41)).toFloat()
                        val angleRadians = angleDegrees * (3.1415927f / 180f)
                        dxPx = cos(angleRadians) * 0.5f
                        dyPx = sin(angleRadians) * 0.5f
                        distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx)).coerceAtLeast(0.0001f)
                    }
                    val nx = dxPx / distancePx
                    val ny = dyPx / distancePx
                    val pushPx = (minimumCircleDistancePx - distancePx) * 0.46f
                    left.center[0] = (left.center[0] - ((nx * pushPx) / safeGraphWidthPx)).coerceIn(marginX, 1f - marginX)
                    left.center[1] = (left.center[1] - ((ny * pushPx) / safeGraphHeightPx)).coerceIn(marginY, 1f - marginY)
                    right.center[0] = (right.center[0] + ((nx * pushPx) / safeGraphWidthPx)).coerceIn(marginX, 1f - marginX)
                    right.center[1] = (right.center[1] + ((ny * pushPx) / safeGraphHeightPx)).coerceIn(marginY, 1f - marginY)
                }
            }
        }
        circles.forEachIndexed { index, circle ->
            val anchor = dominantAnchors[index]
            circle.center[0] = (circle.center[0] * 0.94f) + (anchor.heatX.coerceIn(marginX, 1f - marginX) * 0.06f)
            circle.center[1] = (circle.center[1] * 0.94f) + (anchor.heatY.coerceIn(marginY, 1f - marginY) * 0.06f)
        }
    }
    val maxOverlapBoundaryCount = (circles.size * 4).coerceIn(18, 32)
    val overlapComboSupports = nodes
        .asSequence()
        .mapNotNull { node ->
            val matchedTags = sortedMatchedCircles(node)
                .take(3)
                .map { it.tagName }
            if (matchedTags.size >= 2) matchedTags else null
        }
        .groupingBy { it }
        .eachCount()
    val overlapBoundaries = overlapComboSupports
        .entries
        .asSequence()
        .filter { (_, supportCount) -> supportCount >= 2 }
        .sortedWith(
            compareByDescending<Map.Entry<List<String>, Int>> { it.value }
                .thenByDescending { entry -> entry.key.sumOf { tag -> circlesByTag[tag]?.radiusPx?.toDouble() ?: 0.0 } }
        )
        .take(maxOverlapBoundaryCount)
        .map { (comboTags, _) ->
            val memberCircles = comboTags.mapNotNull { circlesByTag[it] }
            val totalWeight = memberCircles.mapIndexed { index, circle ->
                val baseWeight = circleWeightByTag[circle.tagName] ?: circle.gravityWeight
                if (index == 0) baseWeight * 1.8f else baseWeight
            }.sum().coerceAtLeast(0.001f)
            val centerX = memberCircles.mapIndexed { index, circle ->
                val baseWeight = circleWeightByTag[circle.tagName] ?: circle.gravityWeight
                val weight = if (index == 0) baseWeight * 1.8f else baseWeight
                circle.center[0] * weight
            }.sum() / totalWeight
            val centerY = memberCircles.mapIndexed { index, circle ->
                val baseWeight = circleWeightByTag[circle.tagName] ?: circle.gravityWeight
                val weight = if (index == 0) baseWeight * 1.8f else baseWeight
                circle.center[1] * weight
            }.sum() / totalWeight
            val memberSpreadPx = memberCircles
                .map { circle ->
                    val dxPx = (circle.center[0] - centerX) * safeGraphWidthPx
                    val dyPx = (circle.center[1] - centerY) * safeGraphHeightPx
                    sqrt((dxPx * dxPx) + (dyPx * dyPx))
                }
                .average()
                .toFloat()
            EntryBoundary(
                dominantTags = memberCircles.map { it.label },
                centerX = centerX.coerceIn(marginX, 1f - marginX),
                centerY = centerY.coerceIn(marginY, 1f - marginY),
                radiusPx = (memberSpreadPx * 0.44f).coerceAtLeast(safeSpacingPx * 2.2f)
            )
        }
        .toMutableList()
    repeat(28) {
        for (leftIndex in overlapBoundaries.indices) {
            val left = overlapBoundaries[leftIndex]
            for (rightIndex in (leftIndex + 1) until overlapBoundaries.size) {
                val right = overlapBoundaries[rightIndex]
                var dxPx = (right.centerX - left.centerX) * safeGraphWidthPx
                var dyPx = (right.centerY - left.centerY) * safeGraphHeightPx
                var distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx) + 0.000001f)
                val minimumDistancePx = left.radiusPx + right.radiusPx + (safeSpacingPx * 2.8f)
                if (distancePx < minimumDistancePx) {
                    if (distancePx < 0.01f) {
                        val angleDegrees = (((leftIndex + 3) * 53) + ((rightIndex + 5) * 19)).toFloat()
                        val angleRadians = angleDegrees * (3.1415927f / 180f)
                        dxPx = cos(angleRadians) * 0.5f
                        dyPx = sin(angleRadians) * 0.5f
                        distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx)).coerceAtLeast(0.0001f)
                    }
                    val nx = dxPx / distancePx
                    val ny = dyPx / distancePx
                    val pushPx = (minimumDistancePx - distancePx) * 0.34f
                    overlapBoundaries[leftIndex] = left.copy(
                        centerX = (left.centerX - ((nx * pushPx) / safeGraphWidthPx)).coerceIn(marginX, 1f - marginX),
                        centerY = (left.centerY - ((ny * pushPx) / safeGraphHeightPx)).coerceIn(marginY, 1f - marginY)
                    )
                    overlapBoundaries[rightIndex] = right.copy(
                        centerX = (right.centerX + ((nx * pushPx) / safeGraphWidthPx)).coerceIn(marginX, 1f - marginX),
                        centerY = (right.centerY + ((ny * pushPx) / safeGraphHeightPx)).coerceIn(marginY, 1f - marginY)
                    )
                }
            }
        }
    }
    val overlapBoundaryByKey = overlapBoundaries.associateBy { it.dominantTags.sorted().joinToString("|") }

    fun clampIntoBoundary(
        x: Float,
        y: Float,
        centerX: Float,
        centerY: Float,
        radiusPx: Float
    ): Pair<Float, Float> {
        val radiusX = (radiusPx / safeGraphWidthPx).coerceAtLeast(0.001f)
        val radiusY = (radiusPx / safeGraphHeightPx).coerceAtLeast(0.001f)
        val dx = x - centerX
        val dy = y - centerY
        val norm = ((dx * dx) / (radiusX * radiusX)) + ((dy * dy) / (radiusY * radiusY))
        return if (norm <= 1f) {
            x to y
        } else {
            val scale = 1f / sqrt(norm)
            (centerX + (dx * scale)).coerceIn(marginX, 1f - marginX) to
            (centerY + (dy * scale)).coerceIn(marginY, 1f - marginY)
        }
    }

    fun pushOutOfBoundary(
        x: Float,
        y: Float,
        centerX: Float,
        centerY: Float,
        radiusPx: Float,
        moatPx: Float
    ): Pair<Float, Float> {
        val radiusX = (radiusPx / safeGraphWidthPx).coerceAtLeast(0.001f)
        val radiusY = (radiusPx / safeGraphHeightPx).coerceAtLeast(0.001f)
        var dx = x - centerX
        var dy = y - centerY
        var norm = ((dx * dx) / (radiusX * radiusX)) + ((dy * dy) / (radiusY * radiusY))
        if (norm >= 1f) return x to y
        if (abs(dx) < 0.00001f && abs(dy) < 0.00001f) {
            dx = 0.0001f
            dy = 0.0001f
            norm = ((dx * dx) / (radiusX * radiusX)) + ((dy * dy) / (radiusY * radiusY))
        }
        val scaleOut = (1f / sqrt(norm)) + (moatPx / radiusPx.coerceAtLeast(1f))
        return (centerX + (dx * scaleOut)).coerceIn(marginX, 1f - marginX) to
            (centerY + (dy * scaleOut)).coerceIn(marginY, 1f - marginY)
    }

    val initialBoundaryByCode = nodes.associate { node ->
        val matchedCircles = sortedMatchedCircles(node).take(3)
        val boundary = when {
            matchedCircles.isEmpty() -> {
                EntryBoundary(
                    dominantTags = emptyList(),
                    centerX = node.x.coerceIn(marginX, 1f - marginX),
                    centerY = node.y.coerceIn(marginY, 1f - marginY),
                    radiusPx = safeSpacingPx * 1.8f
                )
            }
            matchedCircles.size == 1 -> {
                val circle = matchedCircles.first()
                EntryBoundary(
                    dominantTags = listOf(circle.label),
                    centerX = circle.center[0],
                    centerY = circle.center[1],
                    radiusPx = circle.radiusPx * 0.86f
                )
            }
            else -> {
                val comboKey = matchedCircles.map { it.label }.sorted().joinToString("|")
                overlapBoundaryByKey[comboKey] ?: run {
                    val totalWeight = matchedCircles.mapIndexed { index, circle ->
                        val baseWeight = circleWeightByTag[circle.tagName] ?: circle.gravityWeight
                        if (index == 0) baseWeight * 1.8f else baseWeight
                    }.sum().coerceAtLeast(0.001f)
                    val centerX = matchedCircles.mapIndexed { index, circle ->
                        val baseWeight = circleWeightByTag[circle.tagName] ?: circle.gravityWeight
                        val weight = if (index == 0) baseWeight * 1.8f else baseWeight
                        circle.center[0] * weight
                    }.sum() / totalWeight
                    val centerY = matchedCircles.mapIndexed { index, circle ->
                        val baseWeight = circleWeightByTag[circle.tagName] ?: circle.gravityWeight
                        val weight = if (index == 0) baseWeight * 1.8f else baseWeight
                        circle.center[1] * weight
                    }.sum() / totalWeight
                    EntryBoundary(
                        dominantTags = matchedCircles.map { it.label },
                        centerX = centerX.coerceIn(marginX, 1f - marginX),
                        centerY = centerY.coerceIn(marginY, 1f - marginY),
                        radiusPx = safeSpacingPx * 2.2f
                    )
                }
            }
        }
        node.code to boundary
    }
    val boundaryGroupKeyByCode = nodes.associate { node ->
        val boundary = initialBoundaryByCode[node.code] ?: EntryBoundary(emptyList(), node.x, node.y, safeSpacingPx * 1.8f)
        val key = if (boundary.dominantTags.isNotEmpty()) {
            boundary.dominantTags.sorted().joinToString("|")
        } else {
            "entry:${node.code}"
        }
        node.code to key
    }
    val initialBoundaryByGroupKey = boundaryGroupKeyByCode
        .entries
        .associate { (code, key) -> key to (initialBoundaryByCode[code] ?: EntryBoundary(emptyList(), 0.5f, 0.5f, safeSpacingPx * 1.8f)) }
    val labelFootprintPxByGroupKey = initialBoundaryByGroupKey.mapValues { (_, boundary) ->
        val label = formatTagGraphCircleLabel(boundary.dominantTags)
        val estimatedWidthPx = (label.length * 10.5f) + 28f
        val estimatedHeightPx = 30f
        sqrt((estimatedWidthPx * estimatedWidthPx) + (estimatedHeightPx * estimatedHeightPx)) * 0.55f
    }
    val boundaryByGroupKey = initialBoundaryByGroupKey
        .toMutableMap()
    val boundaryKeys = boundaryByGroupKey.keys.toList()
    repeat(36) {
        for (leftIndex in boundaryKeys.indices) {
            val leftKey = boundaryKeys[leftIndex]
            val left = boundaryByGroupKey[leftKey] ?: continue
            val leftTags = left.dominantTags.toSet()
            for (rightIndex in (leftIndex + 1) until boundaryKeys.size) {
                val rightKey = boundaryKeys[rightIndex]
                val right = boundaryByGroupKey[rightKey] ?: continue
                val rightTags = right.dominantTags.toSet()
                var dxPx = (right.centerX - left.centerX) * safeGraphWidthPx
                var dyPx = (right.centerY - left.centerY) * safeGraphHeightPx
                var distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx) + 0.000001f)
                val overlapCount = leftTags.intersect(rightTags).size.toFloat()
                val unionCount = leftTags.union(rightTags).size.toFloat().coerceAtLeast(1f)
                val similarity = (overlapCount / unionCount).coerceIn(0f, 1f)
                val subsetRelation =
                    (leftTags.isNotEmpty() && rightTags.isNotEmpty()) &&
                        (leftTags.containsAll(rightTags) || rightTags.containsAll(leftTags))
                val allowedOverlapPx = when {
                    similarity <= 0f -> 0f
                    subsetRelation -> min(left.radiusPx, right.radiusPx) * 0.06f
                    else -> min(left.radiusPx, right.radiusPx) * (0.14f + (0.26f * similarity))
                }
                val baseMinimumDistancePx = (left.radiusPx + right.radiusPx - allowedOverlapPx)
                    .coerceAtLeast(max(left.radiusPx, right.radiusPx) * 0.58f)
                    .plus(
                        if (similarity <= 0f) {
                            safeSpacingPx * 1.45f
                        } else {
                            safeSpacingPx * (0.18f - (0.08f * similarity)).coerceAtLeast(0f)
                        }
                    )
                val smallFamilyFloorPx = max(
                    safeSpacingPx * 3.8f,
                    (min(left.radiusPx, right.radiusPx) * 2.4f) + (safeSpacingPx * 0.8f)
                )
                val containmentFloorPx = if (subsetRelation) {
                    abs(left.radiusPx - right.radiusPx) + min(left.radiusPx, right.radiusPx) * 0.82f + (safeSpacingPx * 0.8f)
                } else {
                    abs(left.radiusPx - right.radiusPx) + (safeSpacingPx * 1.25f)
                }
                val labelFloorPx = ((labelFootprintPxByGroupKey[leftKey] ?: 0f) + (labelFootprintPxByGroupKey[rightKey] ?: 0f))
                    .coerceAtLeast(safeSpacingPx * 2.2f)
                val minimumDistancePx = max(max(max(baseMinimumDistancePx, smallFamilyFloorPx), containmentFloorPx), labelFloorPx)
                if (distancePx < minimumDistancePx) {
                    if (distancePx < 0.01f) {
                        val angleDegrees = (((leftIndex + 7) * 31) + ((rightIndex + 11) * 17)).toFloat()
                        val angleRadians = angleDegrees * (3.1415927f / 180f)
                        dxPx = cos(angleRadians)
                        dyPx = sin(angleRadians)
                        distancePx = 1f
                    }
                    val nx = dxPx / distancePx
                    val ny = dyPx / distancePx
                    val pushPx = (minimumDistancePx - distancePx) * 0.5f
                    boundaryByGroupKey[leftKey] = left.copy(
                        centerX = (left.centerX - ((nx * pushPx) / safeGraphWidthPx)).coerceIn(marginX, 1f - marginX),
                        centerY = (left.centerY - ((ny * pushPx) / safeGraphHeightPx)).coerceIn(marginY, 1f - marginY)
                    )
                    boundaryByGroupKey[rightKey] = right.copy(
                        centerX = (right.centerX + ((nx * pushPx) / safeGraphWidthPx)).coerceIn(marginX, 1f - marginX),
                        centerY = (right.centerY + ((ny * pushPx) / safeGraphHeightPx)).coerceIn(marginY, 1f - marginY)
                    )
                }
            }
        }
        boundaryKeys.forEach { key ->
            val base = initialBoundaryByGroupKey[key] ?: return@forEach
            val current = boundaryByGroupKey[key] ?: return@forEach
            boundaryByGroupKey[key] = current.copy(
                centerX = ((current.centerX * 0.93f) + (base.centerX * 0.07f)).coerceIn(marginX, 1f - marginX),
                centerY = ((current.centerY * 0.93f) + (base.centerY * 0.07f)).coerceIn(marginY, 1f - marginY)
            )
        }
    }
    val boundariesByCode = nodes.associate { node ->
        val groupKey = boundaryGroupKeyByCode[node.code].orEmpty()
        node.code to (boundaryByGroupKey[groupKey] ?: initialBoundaryByCode[node.code] ?: EntryBoundary(emptyList(), node.x, node.y, safeSpacingPx * 1.8f))
    }
    val nodesByBoundary = nodes.groupBy { node -> boundaryGroupKeyByCode[node.code].orEmpty() }
    val positions = mutableMapOf<Int, FloatArray>()
    val effectiveBoundaryRadiusByGroup = mutableMapOf<String, Float>()
    val centerClampRadiusByGroup = mutableMapOf<String, Float>()
    val cellSpacingPx = safeSpacingPx

    fun halton(index: Int, base: Int): Float {
        var result = 0.0
        var fraction = 1.0 / base.toDouble()
        var current = index
        while (current > 0) {
            result += fraction * (current % base)
            current /= base
            fraction /= base.toDouble()
        }
        return result.toFloat()
    }

    fun concentricSampleDisk(u1: Float, u2: Float): Pair<Float, Float> {
        val sx = (2f * u1) - 1f
        val sy = (2f * u2) - 1f
        if (abs(sx) < 0.00001f && abs(sy) < 0.00001f) return 0f to 0f
        val radius: Float
        val theta: Float
        if (abs(sx) > abs(sy)) {
            radius = sx
            theta = (3.1415927f / 4f) * (sy / sx)
        } else {
            radius = sy
            theta = (3.1415927f / 2f) - ((3.1415927f / 4f) * (sx / sy))
        }
        return (radius * cos(theta)) to (radius * sin(theta))
    }

    nodesByBoundary.forEach { (groupKey, boundaryNodes) ->
        val firstBoundary = boundariesByCode[boundaryNodes.first().code]
            ?: EntryBoundary(emptyList(), boundaryNodes.first().x, boundaryNodes.first().y, safeSpacingPx * 1.8f)
        val sortedNodes = boundaryNodes.sortedBy { node ->
            val dx = node.x - firstBoundary.centerX
            val dy = node.y - firstBoundary.centerY
            ((dx * dx) + (dy * dy))
        }
        val thumbnailRadiusPx = minimumVisualSpacingPx * 0.5f
        val boundaryMoatPx = safeSpacingPx * 1.18f
        val requiredRadiusPx =
            (cellSpacingPx * sqrt(sortedNodes.size.toFloat()).coerceAtLeast(1f) * 0.72f) +
                cellSpacingPx +
                boundaryMoatPx
        val effectiveRadiusPx = max(firstBoundary.radiusPx, requiredRadiusPx)
        effectiveBoundaryRadiusByGroup[groupKey] = effectiveRadiusPx
        val placementRadiusPx = (effectiveRadiusPx - boundaryMoatPx).coerceAtLeast(safeSpacingPx * 1.35f)
        val centerClampRadiusPx = (placementRadiusPx - thumbnailRadiusPx).coerceAtLeast(thumbnailRadiusPx + (safeSpacingPx * 0.18f))
        centerClampRadiusByGroup[groupKey] = centerClampRadiusPx
        val candidatePoints = buildList<Pair<Float, Float>>(sortedNodes.size) {
            add(firstBoundary.centerX to firstBoundary.centerY)
            for (index in 1 until sortedNodes.size) {
                val diskPoint = concentricSampleDisk(
                    halton(index, 2),
                    halton(index, 3)
                )
                val radiusPx = (centerClampRadiusPx - (safeSpacingPx * 0.12f)).coerceAtLeast(safeSpacingPx * 0.9f)
                add(
                    firstBoundary.centerX + ((diskPoint.first * radiusPx) / safeGraphWidthPx) to
                        firstBoundary.centerY + ((diskPoint.second * radiusPx) / safeGraphHeightPx)
                )
            }
        }.toMutableList()
        val orderedCandidates = candidatePoints.sortedBy { (x, y) ->
            val dxPx = (x - firstBoundary.centerX) * safeGraphWidthPx
            val dyPx = (y - firstBoundary.centerY) * safeGraphHeightPx
            (dxPx * dxPx) + (dyPx * dyPx)
        }.toMutableList()
        sortedNodes.forEach { node ->
            val preferredX = node.x
            val preferredY = node.y
            val bestIndex = orderedCandidates.indices.minByOrNull { candidateIndex ->
                val candidate = orderedCandidates[candidateIndex]
                val dxPx = (candidate.first - preferredX) * safeGraphWidthPx
                val dyPx = (candidate.second - preferredY) * safeGraphHeightPx
                (dxPx * dxPx) + (dyPx * dyPx)
            } ?: 0
            val candidate = orderedCandidates.removeAt(bestIndex)
            val clamped = clampIntoBoundary(
                x = candidate.first,
                y = candidate.second,
                centerX = firstBoundary.centerX,
                centerY = firstBoundary.centerY,
                radiusPx = centerClampRadiusPx
            )
            positions[node.code] = floatArrayOf(clamped.first, clamped.second)
        }
        repeat(32) {
            for (leftIndex in sortedNodes.indices) {
                val leftNode = sortedNodes[leftIndex]
                val leftPos = positions[leftNode.code] ?: continue
                for (rightIndex in (leftIndex + 1) until sortedNodes.size) {
                    val rightNode = sortedNodes[rightIndex]
                    val rightPos = positions[rightNode.code] ?: continue
                    var dxPx = (rightPos[0] - leftPos[0]) * safeGraphWidthPx
                    var dyPx = (rightPos[1] - leftPos[1]) * safeGraphHeightPx
                    var distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx) + 0.000001f)
                    val minDistancePx = safeSpacingPx * 1.08f
                    if (distancePx < minDistancePx) {
                        if (distancePx < 0.01f) {
                            val angle = ((leftIndex + 1) * 73f) + ((rightIndex + 1) * 29f)
                            val radians = angle * (3.1415927f / 180f)
                            dxPx = cos(radians)
                            dyPx = sin(radians)
                            distancePx = 1f
                        }
                        val nx = dxPx / distancePx
                        val ny = dyPx / distancePx
                        val pushPx = (minDistancePx - distancePx) * 0.5f
                        val leftClamped = clampIntoBoundary(
                            x = leftPos[0] - ((nx * pushPx) / safeGraphWidthPx),
                            y = leftPos[1] - ((ny * pushPx) / safeGraphHeightPx),
                            centerX = firstBoundary.centerX,
                            centerY = firstBoundary.centerY,
                            radiusPx = centerClampRadiusPx
                        )
                        val rightClamped = clampIntoBoundary(
                            x = rightPos[0] + ((nx * pushPx) / safeGraphWidthPx),
                            y = rightPos[1] + ((ny * pushPx) / safeGraphHeightPx),
                            centerX = firstBoundary.centerX,
                            centerY = firstBoundary.centerY,
                            radiusPx = centerClampRadiusPx
                        )
                        leftPos[0] = leftClamped.first
                        leftPos[1] = leftClamped.second
                        rightPos[0] = rightClamped.first
                        rightPos[1] = rightClamped.second
                    }
                }
            }
        }
    }
    val centerClampRadiusByCode = nodes.associate { node ->
        val groupKey = boundaryGroupKeyByCode[node.code].orEmpty()
        node.code to (centerClampRadiusByGroup[groupKey] ?: safeSpacingPx * 1.35f)
    }
    for (iteration in 0 until 44) {
        var moved = false
        for (leftIndex in nodes.indices) {
            val leftNode = nodes[leftIndex]
            val leftPos = positions[leftNode.code] ?: continue
            val leftBoundary = boundariesByCode[leftNode.code] ?: continue
            val leftClampRadiusPx = centerClampRadiusByCode[leftNode.code] ?: continue
            for (rightIndex in (leftIndex + 1) until nodes.size) {
                val rightNode = nodes[rightIndex]
                val rightPos = positions[rightNode.code] ?: continue
                val rightBoundary = boundariesByCode[rightNode.code] ?: continue
                val rightClampRadiusPx = centerClampRadiusByCode[rightNode.code] ?: continue
                var dxPx = (rightPos[0] - leftPos[0]) * safeGraphWidthPx
                var dyPx = (rightPos[1] - leftPos[1]) * safeGraphHeightPx
                var distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx) + 0.000001f)
                val minDistancePx = minimumVisualSpacingPx * 1.12f
                if (distancePx < minDistancePx) {
                    moved = true
                    if (distancePx < 0.01f) {
                        val angle = ((leftNode.code % 97) * 17f) + ((rightNode.code % 89) * 13f)
                        val radians = angle * (3.1415927f / 180f)
                        dxPx = cos(radians)
                        dyPx = sin(radians)
                        distancePx = 1f
                    }
                    val nx = dxPx / distancePx
                    val ny = dyPx / distancePx
                    val pushPx = (minDistancePx - distancePx) * 0.5f
                    val leftClamped = clampIntoBoundary(
                        x = leftPos[0] - ((nx * pushPx) / safeGraphWidthPx),
                        y = leftPos[1] - ((ny * pushPx) / safeGraphHeightPx),
                        centerX = leftBoundary.centerX,
                        centerY = leftBoundary.centerY,
                        radiusPx = leftClampRadiusPx
                    )
                    val rightClamped = clampIntoBoundary(
                        x = rightPos[0] + ((nx * pushPx) / safeGraphWidthPx),
                        y = rightPos[1] + ((ny * pushPx) / safeGraphHeightPx),
                        centerX = rightBoundary.centerX,
                        centerY = rightBoundary.centerY,
                        radiusPx = rightClampRadiusPx
                    )
                    leftPos[0] = leftClamped.first
                    leftPos[1] = leftClamped.second
                    rightPos[0] = rightClamped.first
                    rightPos[1] = rightClamped.second
                }
            }
        }
        if (!moved) break
    }
    val nodeCodesByGroupKey = nodesByBoundary.mapValues { (_, groupNodes) -> groupNodes.map { it.code } }
    repeat(20) {
        val actualDisplayRadiusByGroup = boundaryKeys.associateWith { groupKey ->
            val boundary = boundaryByGroupKey[groupKey] ?: initialBoundaryByGroupKey[groupKey] ?: EntryBoundary(emptyList(), 0.5f, 0.5f, safeSpacingPx * 1.8f)
            val codes = nodeCodesByGroupKey[groupKey].orEmpty()
            val displayRadius = codes
                .mapNotNull { code ->
                    val position = positions[code] ?: return@mapNotNull null
                    val dxPx = (position[0] - boundary.centerX) * safeGraphWidthPx
                    val dyPx = (position[1] - boundary.centerY) * safeGraphHeightPx
                    sqrt((dxPx * dxPx) + (dyPx * dyPx)) + (minimumVisualSpacingPx * 0.56f)
                }
                .maxOrNull()
                ?: (minimumVisualSpacingPx * 0.78f)
            max(displayRadius, minimumVisualSpacingPx * 0.78f)
        }
        for (leftIndex in boundaryKeys.indices) {
            val leftKey = boundaryKeys[leftIndex]
            val left = boundaryByGroupKey[leftKey] ?: continue
            val leftRadiusPx = actualDisplayRadiusByGroup[leftKey] ?: continue
            val leftTags = left.dominantTags.toSet()
            for (rightIndex in (leftIndex + 1) until boundaryKeys.size) {
                val rightKey = boundaryKeys[rightIndex]
                val right = boundaryByGroupKey[rightKey] ?: continue
                val rightRadiusPx = actualDisplayRadiusByGroup[rightKey] ?: continue
                val rightTags = right.dominantTags.toSet()
                var dxPx = (right.centerX - left.centerX) * safeGraphWidthPx
                var dyPx = (right.centerY - left.centerY) * safeGraphHeightPx
                var distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx) + 0.000001f)
                val overlapCount = leftTags.intersect(rightTags).size.toFloat()
                val unionCount = leftTags.union(rightTags).size.toFloat().coerceAtLeast(1f)
                val similarity = (overlapCount / unionCount).coerceIn(0f, 1f)
                val subsetRelation =
                    (leftTags.isNotEmpty() && rightTags.isNotEmpty()) &&
                        (leftTags.containsAll(rightTags) || rightTags.containsAll(leftTags))
                val allowedOverlapPx = when {
                    similarity <= 0f -> 0f
                    subsetRelation -> min(leftRadiusPx, rightRadiusPx) * 0.10f
                    else -> min(leftRadiusPx, rightRadiusPx) * (0.12f + (0.20f * similarity))
                }
                val minimumDistancePx = (leftRadiusPx + rightRadiusPx - allowedOverlapPx)
                    .coerceAtLeast(max(leftRadiusPx, rightRadiusPx) * 0.62f)
                    .plus(if (similarity <= 0f) safeSpacingPx * 1.75f else safeSpacingPx * 0.28f)
                val labelFloorPx = ((labelFootprintPxByGroupKey[leftKey] ?: 0f) + (labelFootprintPxByGroupKey[rightKey] ?: 0f))
                    .coerceAtLeast(safeSpacingPx * 2.2f)
                val enforcedMinimumDistancePx = max(minimumDistancePx, labelFloorPx)
                if (distancePx < enforcedMinimumDistancePx) {
                    if (distancePx < 0.01f) {
                        val angleDegrees = (((leftIndex + 13) * 29) + ((rightIndex + 17) * 23)).toFloat()
                        val angleRadians = angleDegrees * (3.1415927f / 180f)
                        dxPx = cos(angleRadians)
                        dyPx = sin(angleRadians)
                        distancePx = 1f
                    }
                    val nx = dxPx / distancePx
                    val ny = dyPx / distancePx
                    val pushPx = (enforcedMinimumDistancePx - distancePx) * 0.5f
                    val leftShiftX = ((nx * pushPx) / safeGraphWidthPx)
                    val leftShiftY = ((ny * pushPx) / safeGraphHeightPx)
                    val rightShiftX = -leftShiftX
                    val rightShiftY = -leftShiftY
                    val shiftedLeft = left.copy(
                        centerX = (left.centerX - leftShiftX).coerceIn(marginX, 1f - marginX),
                        centerY = (left.centerY - leftShiftY).coerceIn(marginY, 1f - marginY)
                    )
                    val shiftedRight = right.copy(
                        centerX = (right.centerX - rightShiftX).coerceIn(marginX, 1f - marginX),
                        centerY = (right.centerY - rightShiftY).coerceIn(marginY, 1f - marginY)
                    )
                    boundaryByGroupKey[leftKey] = shiftedLeft
                    boundaryByGroupKey[rightKey] = shiftedRight
                    nodeCodesByGroupKey[leftKey].orEmpty().forEach { code ->
                        val position = positions[code] ?: return@forEach
                        val clampRadius = centerClampRadiusByGroup[leftKey] ?: leftRadiusPx
                        val shifted = clampIntoBoundary(
                            x = position[0] - leftShiftX,
                            y = position[1] - leftShiftY,
                            centerX = shiftedLeft.centerX,
                            centerY = shiftedLeft.centerY,
                            radiusPx = clampRadius
                        )
                        position[0] = shifted.first
                        position[1] = shifted.second
                    }
                    nodeCodesByGroupKey[rightKey].orEmpty().forEach { code ->
                        val position = positions[code] ?: return@forEach
                        val clampRadius = centerClampRadiusByGroup[rightKey] ?: rightRadiusPx
                        val shifted = clampIntoBoundary(
                            x = position[0] - rightShiftX,
                            y = position[1] - rightShiftY,
                            centerX = shiftedRight.centerX,
                            centerY = shiftedRight.centerY,
                            radiusPx = clampRadius
                        )
                        position[0] = shifted.first
                        position[1] = shifted.second
                    }
                }
            }
        }
    }
    val finalBoundariesByCode = nodes.associate { node ->
        val groupKey = boundaryGroupKeyByCode[node.code].orEmpty()
        node.code to (boundaryByGroupKey[groupKey] ?: initialBoundaryByCode[node.code] ?: EntryBoundary(emptyList(), node.x, node.y, safeSpacingPx * 1.8f))
    }
    val finalEffectiveBoundaryRadiusByGroup = boundaryKeys.associateWith { groupKey ->
        val boundary = boundaryByGroupKey[groupKey] ?: initialBoundaryByGroupKey[groupKey] ?: EntryBoundary(emptyList(), 0.5f, 0.5f, safeSpacingPx * 1.8f)
        val displayRadius = nodeCodesByGroupKey[groupKey]
            .orEmpty()
            .mapNotNull { code ->
                val position = positions[code] ?: return@mapNotNull null
                val dxPx = (position[0] - boundary.centerX) * safeGraphWidthPx
                val dyPx = (position[1] - boundary.centerY) * safeGraphHeightPx
                sqrt((dxPx * dxPx) + (dyPx * dyPx)) + (minimumVisualSpacingPx * 0.56f)
            }
            .maxOrNull()
            ?: boundary.radiusPx
        max(effectiveBoundaryRadiusByGroup[groupKey] ?: 0f, displayRadius)
    }
    val finalCenterClampRadiusByGroup = finalEffectiveBoundaryRadiusByGroup.mapValues { (groupKey, radiusPx) ->
        val thumbnailRadiusPx = minimumVisualSpacingPx * 0.5f
        val boundaryMoatPx = safeSpacingPx * 1.18f
        val placementRadiusPx = (radiusPx - boundaryMoatPx).coerceAtLeast(safeSpacingPx * 1.35f)
        (placementRadiusPx - thumbnailRadiusPx).coerceAtLeast(thumbnailRadiusPx + (safeSpacingPx * 0.18f))
    }
    nodesByBoundary.forEach { (groupKey, boundaryNodes) ->
        val boundary = boundaryByGroupKey[groupKey]
            ?: initialBoundaryByGroupKey[groupKey]
            ?: EntryBoundary(emptyList(), 0.5f, 0.5f, safeSpacingPx * 1.8f)
        val centerClampRadiusPx = finalCenterClampRadiusByGroup[groupKey] ?: return@forEach
        repeat(28) {
            for (leftIndex in boundaryNodes.indices) {
                val leftNode = boundaryNodes[leftIndex]
                val leftPos = positions[leftNode.code] ?: continue
                for (rightIndex in (leftIndex + 1) until boundaryNodes.size) {
                    val rightNode = boundaryNodes[rightIndex]
                    val rightPos = positions[rightNode.code] ?: continue
                    var dxPx = (rightPos[0] - leftPos[0]) * safeGraphWidthPx
                    var dyPx = (rightPos[1] - leftPos[1]) * safeGraphHeightPx
                    var distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx) + 0.000001f)
                    val minDistancePx = minimumVisualSpacingPx * 1.10f
                    if (distancePx < minDistancePx) {
                        if (distancePx < 0.01f) {
                            val angle = ((leftNode.code % 131) * 19f) + ((rightNode.code % 127) * 11f)
                            val radians = angle * (3.1415927f / 180f)
                            dxPx = cos(radians)
                            dyPx = sin(radians)
                            distancePx = 1f
                        }
                        val nx = dxPx / distancePx
                        val ny = dyPx / distancePx
                        val pushPx = (minDistancePx - distancePx) * 0.5f
                        val leftClamped = clampIntoBoundary(
                            x = leftPos[0] - ((nx * pushPx) / safeGraphWidthPx),
                            y = leftPos[1] - ((ny * pushPx) / safeGraphHeightPx),
                            centerX = boundary.centerX,
                            centerY = boundary.centerY,
                            radiusPx = centerClampRadiusPx
                        )
                        val rightClamped = clampIntoBoundary(
                            x = rightPos[0] + ((nx * pushPx) / safeGraphWidthPx),
                            y = rightPos[1] + ((ny * pushPx) / safeGraphHeightPx),
                            centerX = boundary.centerX,
                            centerY = boundary.centerY,
                            radiusPx = centerClampRadiusPx
                        )
                        leftPos[0] = leftClamped.first
                        leftPos[1] = leftClamped.second
                        rightPos[0] = rightClamped.first
                        rightPos[1] = rightClamped.second
                    }
                }
            }
        }
    }
    val finalCenterClampRadiusByCode = nodes.associate { node ->
        val groupKey = boundaryGroupKeyByCode[node.code].orEmpty()
        node.code to (finalCenterClampRadiusByGroup[groupKey] ?: safeSpacingPx * 1.35f)
    }
    for (iteration in 0 until 32) {
        var moved = false
        for (leftIndex in nodes.indices) {
            val leftNode = nodes[leftIndex]
            val leftPos = positions[leftNode.code] ?: continue
            val leftBoundary = finalBoundariesByCode[leftNode.code] ?: continue
            val leftClampRadiusPx = finalCenterClampRadiusByCode[leftNode.code] ?: continue
            for (rightIndex in (leftIndex + 1) until nodes.size) {
                val rightNode = nodes[rightIndex]
                val rightPos = positions[rightNode.code] ?: continue
                val rightBoundary = finalBoundariesByCode[rightNode.code] ?: continue
                val rightClampRadiusPx = finalCenterClampRadiusByCode[rightNode.code] ?: continue
                var dxPx = (rightPos[0] - leftPos[0]) * safeGraphWidthPx
                var dyPx = (rightPos[1] - leftPos[1]) * safeGraphHeightPx
                var distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx) + 0.000001f)
                val minDistancePx = minimumVisualSpacingPx * 1.16f
                if (distancePx < minDistancePx) {
                    moved = true
                    if (distancePx < 0.01f) {
                        val angle = ((leftNode.code % 149) * 13f) + ((rightNode.code % 151) * 17f)
                        val radians = angle * (3.1415927f / 180f)
                        dxPx = cos(radians)
                        dyPx = sin(radians)
                        distancePx = 1f
                    }
                    val nx = dxPx / distancePx
                    val ny = dyPx / distancePx
                    val pushPx = (minDistancePx - distancePx) * 0.5f
                    val leftClamped = clampIntoBoundary(
                        x = leftPos[0] - ((nx * pushPx) / safeGraphWidthPx),
                        y = leftPos[1] - ((ny * pushPx) / safeGraphHeightPx),
                        centerX = leftBoundary.centerX,
                        centerY = leftBoundary.centerY,
                        radiusPx = leftClampRadiusPx
                    )
                    val rightClamped = clampIntoBoundary(
                        x = rightPos[0] + ((nx * pushPx) / safeGraphWidthPx),
                        y = rightPos[1] + ((ny * pushPx) / safeGraphHeightPx),
                        centerX = rightBoundary.centerX,
                        centerY = rightBoundary.centerY,
                        radiusPx = rightClampRadiusPx
                    )
                    leftPos[0] = leftClamped.first
                    leftPos[1] = leftClamped.second
                    rightPos[0] = rightClamped.first
                    rightPos[1] = rightClamped.second
                }
            }
        }
        if (!moved) break
    }
    val allFinalGroupBoundaries = boundaryKeys.associateWith { groupKey ->
        boundaryByGroupKey[groupKey] ?: initialBoundaryByGroupKey[groupKey] ?: EntryBoundary(emptyList(), 0.5f, 0.5f, safeSpacingPx * 1.8f)
    }
    val disallowedGroupKeysByCode = nodes.associate { node ->
        val allowed = node.tagNames.toSet()
        node.code to boundaryKeys.filter { groupKey ->
            val boundary = allFinalGroupBoundaries[groupKey] ?: return@filter false
            boundary.dominantTags.isNotEmpty() && !boundary.dominantTags.all { it in allowed }
        }
    }
    repeat(28) {
        nodes.forEach { node ->
            val pos = positions[node.code] ?: return@forEach
            val ownBoundary = finalBoundariesByCode[node.code] ?: return@forEach
            val ownClampRadius = finalCenterClampRadiusByGroup[boundaryGroupKeyByCode[node.code].orEmpty()] ?: return@forEach
            var currentX = pos[0]
            var currentY = pos[1]
            disallowedGroupKeysByCode[node.code].orEmpty().forEach { groupKey ->
                val disallowedBoundary = allFinalGroupBoundaries[groupKey] ?: return@forEach
                val disallowedRadius = finalEffectiveBoundaryRadiusByGroup[groupKey] ?: disallowedBoundary.radiusPx
                val pushed = pushOutOfBoundary(
                    x = currentX,
                    y = currentY,
                    centerX = disallowedBoundary.centerX,
                    centerY = disallowedBoundary.centerY,
                    radiusPx = disallowedRadius,
                    moatPx = safeSpacingPx * 1.08f
                )
                currentX = pushed.first
                currentY = pushed.second
            }
            val reclamped = clampIntoBoundary(
                x = currentX,
                y = currentY,
                centerX = ownBoundary.centerX,
                centerY = ownBoundary.centerY,
                radiusPx = ownClampRadius
            )
            pos[0] = reclamped.first
            pos[1] = reclamped.second
        }
    }
    nodesByBoundary.forEach { (groupKey, boundaryNodes) ->
        val boundary = finalBoundariesByCode[boundaryNodes.first().code]
            ?: initialBoundaryByGroupKey[groupKey]
            ?: EntryBoundary(emptyList(), 0.5f, 0.5f, safeSpacingPx * 1.8f)
        val centerClampRadiusPx = finalCenterClampRadiusByGroup[groupKey] ?: return@forEach
        repeat(20) {
            for (leftIndex in boundaryNodes.indices) {
                val leftNode = boundaryNodes[leftIndex]
                val leftPos = positions[leftNode.code] ?: continue
                for (rightIndex in (leftIndex + 1) until boundaryNodes.size) {
                    val rightNode = boundaryNodes[rightIndex]
                    val rightPos = positions[rightNode.code] ?: continue
                    var dxPx = (rightPos[0] - leftPos[0]) * safeGraphWidthPx
                    var dyPx = (rightPos[1] - leftPos[1]) * safeGraphHeightPx
                    var distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx) + 0.000001f)
                    val similarity = computeEntryTagSetSimilarity(
                        leftNode.tagNames.toSet(),
                        rightNode.tagNames.toSet()
                    )
                    val minDistancePx = minimumVisualSpacingPx * when {
                        similarity < 0.12f -> 1.46f
                        similarity < 0.24f -> 1.28f
                        else -> 1.10f
                    }
                    if (distancePx < minDistancePx) {
                        if (distancePx < 0.01f) {
                            val angle = ((leftNode.code % 163) * 13f) + ((rightNode.code % 167) * 17f)
                            val radians = angle * (3.1415927f / 180f)
                            dxPx = cos(radians)
                            dyPx = sin(radians)
                            distancePx = 1f
                        }
                        val nx = dxPx / distancePx
                        val ny = dyPx / distancePx
                        val pushPx = (minDistancePx - distancePx) * 0.5f
                        val leftClamped = clampIntoBoundary(
                            x = leftPos[0] - ((nx * pushPx) / safeGraphWidthPx),
                            y = leftPos[1] - ((ny * pushPx) / safeGraphHeightPx),
                            centerX = boundary.centerX,
                            centerY = boundary.centerY,
                            radiusPx = centerClampRadiusPx
                        )
                        val rightClamped = clampIntoBoundary(
                            x = rightPos[0] + ((nx * pushPx) / safeGraphWidthPx),
                            y = rightPos[1] + ((ny * pushPx) / safeGraphHeightPx),
                            centerX = boundary.centerX,
                            centerY = boundary.centerY,
                            radiusPx = centerClampRadiusPx
                        )
                        leftPos[0] = leftClamped.first
                        leftPos[1] = leftClamped.second
                        rightPos[0] = rightClamped.first
                        rightPos[1] = rightClamped.second
                    }
                }
            }
        }
    }
    for (iteration in 0 until 24) {
        var moved = false
        for (leftIndex in nodes.indices) {
            val leftNode = nodes[leftIndex]
            val leftPos = positions[leftNode.code] ?: continue
            val leftBoundary = finalBoundariesByCode[leftNode.code] ?: continue
            val leftClampRadiusPx = finalCenterClampRadiusByCode[leftNode.code] ?: continue
            for (rightIndex in (leftIndex + 1) until nodes.size) {
                val rightNode = nodes[rightIndex]
                val rightPos = positions[rightNode.code] ?: continue
                val rightBoundary = finalBoundariesByCode[rightNode.code] ?: continue
                val rightClampRadiusPx = finalCenterClampRadiusByCode[rightNode.code] ?: continue
                var dxPx = (rightPos[0] - leftPos[0]) * safeGraphWidthPx
                var dyPx = (rightPos[1] - leftPos[1]) * safeGraphHeightPx
                var distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx) + 0.000001f)
                val similarity = computeEntryTagSetSimilarity(
                    leftNode.tagNames.toSet(),
                    rightNode.tagNames.toSet()
                )
                val minDistancePx = minimumVisualSpacingPx * when {
                    similarity < 0.12f -> 1.52f
                    similarity < 0.24f -> 1.34f
                    else -> 1.16f
                }
                if (distancePx < minDistancePx) {
                    moved = true
                    if (distancePx < 0.01f) {
                        val angle = ((leftNode.code % 173) * 11f) + ((rightNode.code % 179) * 19f)
                        val radians = angle * (3.1415927f / 180f)
                        dxPx = cos(radians)
                        dyPx = sin(radians)
                        distancePx = 1f
                    }
                    val nx = dxPx / distancePx
                    val ny = dyPx / distancePx
                    val pushPx = (minDistancePx - distancePx) * 0.5f
                    val leftClamped = clampIntoBoundary(
                        x = leftPos[0] - ((nx * pushPx) / safeGraphWidthPx),
                        y = leftPos[1] - ((ny * pushPx) / safeGraphHeightPx),
                        centerX = leftBoundary.centerX,
                        centerY = leftBoundary.centerY,
                        radiusPx = leftClampRadiusPx
                    )
                    val rightClamped = clampIntoBoundary(
                        x = rightPos[0] + ((nx * pushPx) / safeGraphWidthPx),
                        y = rightPos[1] + ((ny * pushPx) / safeGraphHeightPx),
                        centerX = rightBoundary.centerX,
                        centerY = rightBoundary.centerY,
                        radiusPx = rightClampRadiusPx
                    )
                    leftPos[0] = leftClamped.first
                    leftPos[1] = leftClamped.second
                    rightPos[0] = rightClamped.first
                    rightPos[1] = rightClamped.second
                }
            }
        }
        if (!moved) break
    }
    return nodes.map { node ->
        val pos = positions[node.code] ?: floatArrayOf(node.x, node.y)
        val boundary = finalBoundariesByCode[node.code]
        val groupKey = boundaryGroupKeyByCode[node.code].orEmpty()
        node.copy(
            boundaryCenterX = boundary?.centerX ?: node.boundaryCenterX,
            boundaryCenterY = boundary?.centerY ?: node.boundaryCenterY,
            boundaryRadiusPx = finalEffectiveBoundaryRadiusByGroup[groupKey] ?: boundary?.radiusPx ?: node.boundaryRadiusPx,
            x = pos[0],
            y = pos[1],
            dominantCircleTags = finalBoundariesByCode[node.code]?.dominantTags.orEmpty()
        )
    }
}

private fun layoutTagGraphEntryNodesForCacheByTopFamilies(
    nodes: List<TagGraphEntryNode>,
    tagNodes: List<TagGraphNode>,
    graphWidthPx: Float,
    graphHeightPx: Float,
    minimumVisualSpacingPx: Float
): List<TagGraphEntryNode> {
    if (nodes.size <= 1) return nodes

    data class FamilyBoundary(
        val key: String,
        val tags: List<String>,
        val denominatorTag: String,
        val memberCodes: MutableList<Int>,
        val desiredX: Float,
        val desiredY: Float,
        var centerX: Float,
        var centerY: Float,
        var radiusPx: Float
    )

    val safeGraphWidthPx = graphWidthPx.coerceAtLeast(1f)
    val safeGraphHeightPx = graphHeightPx.coerceAtLeast(1f)
    val safeSpacingPx = minimumVisualSpacingPx.coerceAtLeast(10f) * 1.08f
    val thumbnailRadiusPx = minimumVisualSpacingPx * 0.5f
    val minimumNodeDistancePx = minimumVisualSpacingPx * 1.18f
    val marginX = ((safeSpacingPx * 0.62f) / safeGraphWidthPx).coerceIn(0.025f, 0.14f)
    val marginY = ((safeSpacingPx * 0.62f) / safeGraphHeightPx).coerceIn(0.025f, 0.14f)
    val goldenAngle = 2.3999632f

    fun clampIntoBoundary(
        x: Float,
        y: Float,
        centerX: Float,
        centerY: Float,
        radiusPx: Float
    ): Pair<Float, Float> {
        val radiusX = (radiusPx / safeGraphWidthPx).coerceAtLeast(0.001f)
        val radiusY = (radiusPx / safeGraphHeightPx).coerceAtLeast(0.001f)
        val dx = x - centerX
        val dy = y - centerY
        val norm = ((dx * dx) / (radiusX * radiusX)) + ((dy * dy) / (radiusY * radiusY))
        return if (norm <= 1f) {
            x.coerceIn(marginX, 1f - marginX) to y.coerceIn(marginY, 1f - marginY)
        } else {
            val scale = 1f / sqrt(norm)
            (centerX + (dx * scale)).coerceIn(marginX, 1f - marginX) to
                (centerY + (dy * scale)).coerceIn(marginY, 1f - marginY)
        }
    }

    fun pushOutOfBoundary(
        x: Float,
        y: Float,
        centerX: Float,
        centerY: Float,
        radiusPx: Float,
        moatPx: Float
    ): Pair<Float, Float> {
        val radiusX = (radiusPx / safeGraphWidthPx).coerceAtLeast(0.001f)
        val radiusY = (radiusPx / safeGraphHeightPx).coerceAtLeast(0.001f)
        var dx = x - centerX
        var dy = y - centerY
        var norm = ((dx * dx) / (radiusX * radiusX)) + ((dy * dy) / (radiusY * radiusY))
        if (norm >= 1f) return x to y
        if (abs(dx) < 0.00001f && abs(dy) < 0.00001f) {
            dx = 0.0001f
            dy = 0.0001f
            norm = ((dx * dx) / (radiusX * radiusX)) + ((dy * dy) / (radiusY * radiusY))
        }
        val scaleOut = (1f / sqrt(norm)) + (moatPx / radiusPx.coerceAtLeast(1f))
        return (centerX + (dx * scaleOut)).coerceIn(marginX, 1f - marginX) to
            (centerY + (dy * scaleOut)).coerceIn(marginY, 1f - marginY)
    }

    val tagNodesByName = tagNodes.associateBy { it.normalizedName }
    val tagMemberCodes = tagNodes.associate { tag ->
        tag.normalizedName to nodes.asSequence()
            .filter { tag.normalizedName in it.tagNames }
            .map { it.code }
            .toSet()
    }
    val supportFloor = max(4, min(18, (nodes.size * 0.018f).roundToInt()))
    val candidateTags = tagNodes
        .filter { (tagMemberCodes[it.normalizedName]?.size ?: 0) >= supportFloor }
        .sortedWith(
            compareByDescending<TagGraphNode> { it.localCount }
                .thenBy { it.normalizedName }
        )

    val denominatorStrengthByTag = candidateTags.associate { tag ->
        val members = tagMemberCodes[tag.normalizedName].orEmpty()
        val supportRatio = (members.size.toFloat() / nodes.size.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
        val strongestOverlaps = candidateTags
            .asSequence()
            .filter { it.normalizedName != tag.normalizedName }
            .map { other ->
                computeSuggestionThemeSimilarity(
                    leftEntries = members,
                    rightEntries = tagMemberCodes[other.normalizedName].orEmpty()
                )
            }
            .sortedDescending()
            .take(5)
            .toList()
        val exclusivity = if (strongestOverlaps.isEmpty()) {
            1f
        } else {
            1f - strongestOverlaps.average().toFloat().coerceIn(0f, 1f)
        }
        val edgeDistance = sqrt(
            ((tag.heatX - 0.5f) * (tag.heatX - 0.5f)) +
                ((tag.heatY - 0.5f) * (tag.heatY - 0.5f))
        )
        val edgeBias = (edgeDistance / 0.52f).coerceIn(0f, 1f)
        val genericPenalty = when {
            supportRatio <= 0.34f -> 1f
            else -> 1f - (((supportRatio - 0.34f) / 0.50f).coerceIn(0f, 1f) * 0.55f)
        }
        tag.normalizedName to (
            sqrt(members.size.toFloat().coerceAtLeast(1f)) *
                (0.30f + (supportRatio * 0.78f)) *
                (0.35f + (exclusivity * 0.95f)) *
                (0.80f + (edgeBias * 0.22f)) *
                genericPenalty
            )
    }

    fun topFamilyTags(node: TagGraphEntryNode): List<String> {
        return node.tagNames
            .distinct()
            .mapNotNull { tagName ->
                val tag = tagNodesByName[tagName] ?: return@mapNotNull null
                Triple(tagName, tag.localCount, denominatorStrengthByTag[tagName] ?: 0f)
            }
            .sortedWith(
                compareByDescending<Triple<String, Int, Float>> { it.second }
                    .thenByDescending { it.third }
                    .thenBy { it.first }
            )
            .take(3)
            .map { it.first }
    }

    fun chooseDenominatorTag(familyTags: List<String>): String {
        return familyTags.maxByOrNull { tagName ->
            val baseStrength = denominatorStrengthByTag[tagName] ?: 0f
            val members = tagMemberCodes[tagName].orEmpty()
            val exclusivityAgainstFamily = familyTags
                .filter { it != tagName }
                .map { other ->
                    1f - computeSuggestionThemeSimilarity(
                        leftEntries = members,
                        rightEntries = tagMemberCodes[other].orEmpty()
                    )
                }
                .average()
                .toFloat()
                .coerceIn(0f, 1f)
            baseStrength * (0.72f + (exclusivityAgainstFamily * 0.50f))
        } ?: familyTags.first()
    }

    val familyTagsByCode = nodes.associate { node ->
        val topTags = topFamilyTags(node)
        val orderedTags = if (topTags.isNotEmpty()) {
            val denominator = chooseDenominatorTag(topTags)
            listOf(denominator) + topTags.filter { it != denominator }
        } else {
            emptyList()
        }
        node.code to orderedTags
    }

    val families = nodes
        .groupBy { node ->
            val tags = familyTagsByCode[node.code].orEmpty()
            if (tags.isEmpty()) "entry:${node.code}" else tags.joinToString("|")
        }
        .map { (familyKey, members) ->
            val tags = familyTagsByCode[members.first().code].orEmpty()
            val denominatorTag = tags.firstOrNull().orEmpty()
            val denominatorNode = tagNodesByName[denominatorTag]
            val denominatorX = denominatorNode?.heatX?.coerceIn(marginX, 1f - marginX) ?: 0.5f
            val denominatorY = denominatorNode?.heatY?.coerceIn(marginY, 1f - marginY) ?: 0.5f
            val weightedTags = tags.mapIndexedNotNull { index, tagName ->
                val tag = tagNodesByName[tagName] ?: return@mapIndexedNotNull null
                val weight = when (index) {
                    0 -> 3.6f
                    1 -> 1.7f
                    else -> 1.15f
                }
                Triple(tag.heatX, tag.heatY, weight)
            }
            val totalWeight = weightedTags.sumOf { it.third.toDouble() }.toFloat().coerceAtLeast(0.001f)
            val weightedCenterX = if (weightedTags.isEmpty()) {
                denominatorX
            } else {
                weightedTags.sumOf { (it.first * it.third).toDouble() }.toFloat() / totalWeight
            }
            val weightedCenterY = if (weightedTags.isEmpty()) {
                denominatorY
            } else {
                weightedTags.sumOf { (it.second * it.third).toDouble() }.toFloat() / totalWeight
            }
            val branchDxPx = (weightedCenterX - denominatorX) * safeGraphWidthPx
            val branchDyPx = (weightedCenterY - denominatorY) * safeGraphHeightPx
            val branchDistancePx = sqrt((branchDxPx * branchDxPx) + (branchDyPx * branchDyPx))
            val memberCount = members.size.coerceAtLeast(1)
            val hashOffset = ((Math.abs(familyKey.hashCode()) % 360).toFloat() * (3.1415927f / 180f))
            val angle = if (branchDistancePx >= 4f) {
                Math.atan2(branchDyPx.toDouble(), branchDxPx.toDouble()).toFloat()
            } else {
                hashOffset
            }
            val branchRadiusPx = (
                (safeSpacingPx * (1.8f + (tags.size * 0.45f))) +
                    (sqrt(memberCount.toFloat()) * safeSpacingPx * 0.42f) +
                    (branchDistancePx * 0.58f)
                ).coerceAtLeast(safeSpacingPx * 1.8f)
            val desiredX = (denominatorX + ((cos(angle.toDouble()).toFloat() * branchRadiusPx) / safeGraphWidthPx)).coerceIn(marginX, 1f - marginX)
            val desiredY = (denominatorY + ((sin(angle.toDouble()).toFloat() * branchRadiusPx) / safeGraphHeightPx)).coerceIn(marginY, 1f - marginY)
            val initialRadiusPx = (
                safeSpacingPx * 1.05f +
                    (sqrt(memberCount.toFloat()) * minimumVisualSpacingPx * 0.62f)
                ).coerceAtLeast(safeSpacingPx * 1.12f)
            FamilyBoundary(
                key = familyKey,
                tags = tags,
                denominatorTag = denominatorTag,
                memberCodes = members.map { it.code }.toMutableList(),
                desiredX = desiredX,
                desiredY = desiredY,
                centerX = desiredX,
                centerY = desiredY,
                radiusPx = initialRadiusPx
            )
        }
        .toMutableList()

    val familiesByKey = families.associateBy { it.key }
    val familyByCode = nodes.associate { node ->
        val key = if (familyTagsByCode[node.code].isNullOrEmpty()) "entry:${node.code}" else familyTagsByCode[node.code].orEmpty().joinToString("|")
        node.code to (familiesByKey[key] ?: families.first())
    }

    repeat(44) {
        for (leftIndex in families.indices) {
            val left = families[leftIndex]
            val leftTags = left.tags.toSet()
            for (rightIndex in (leftIndex + 1) until families.size) {
                val right = families[rightIndex]
                val rightTags = right.tags.toSet()
                var dxPx = (right.centerX - left.centerX) * safeGraphWidthPx
                var dyPx = (right.centerY - left.centerY) * safeGraphHeightPx
                var distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx) + 0.000001f)
                val tagOverlap = leftTags.intersect(rightTags).size.toFloat()
                val unionCount = leftTags.union(rightTags).size.toFloat().coerceAtLeast(1f)
                val similarity = (tagOverlap / unionCount).coerceIn(0f, 1f)
                val sameDenominator = left.denominatorTag.isNotBlank() && left.denominatorTag == right.denominatorTag
                val allowedOverlapPx = when {
                    sameDenominator -> min(left.radiusPx, right.radiusPx) * (0.34f + (similarity * 0.22f))
                    similarity > 0f -> min(left.radiusPx, right.radiusPx) * (0.12f + (similarity * 0.16f))
                    else -> 0f
                }
                val minimumDistancePx = (
                    left.radiusPx + right.radiusPx - allowedOverlapPx +
                        if (sameDenominator) safeSpacingPx * 0.24f else safeSpacingPx * 0.96f
                    ).coerceAtLeast(max(left.radiusPx, right.radiusPx) * 0.58f)
                if (distancePx < minimumDistancePx) {
                    if (distancePx < 0.01f) {
                        val radians = (((leftIndex + 1) * 31f) + ((rightIndex + 1) * 17f)) * (3.1415927f / 180f)
                        dxPx = cos(radians)
                        dyPx = sin(radians)
                        distancePx = 1f
                    }
                    val nx = dxPx / distancePx
                    val ny = dyPx / distancePx
                    val pushPx = (minimumDistancePx - distancePx) * 0.5f
                    left.centerX = (left.centerX - ((nx * pushPx) / safeGraphWidthPx)).coerceIn(marginX, 1f - marginX)
                    left.centerY = (left.centerY - ((ny * pushPx) / safeGraphHeightPx)).coerceIn(marginY, 1f - marginY)
                    right.centerX = (right.centerX + ((nx * pushPx) / safeGraphWidthPx)).coerceIn(marginX, 1f - marginX)
                    right.centerY = (right.centerY + ((ny * pushPx) / safeGraphHeightPx)).coerceIn(marginY, 1f - marginY)
                }
            }
        }
        families.forEach { family ->
            family.centerX = (family.centerX * 0.92f) + (family.desiredX * 0.08f)
            family.centerY = (family.centerY * 0.92f) + (family.desiredY * 0.08f)
        }
    }

    val positions = mutableMapOf<Int, FloatArray>()
    nodes.forEach { node ->
        val family = familyByCode[node.code] ?: return@forEach
        val members = family.memberCodes.sorted()
        val index = members.indexOf(node.code).coerceAtLeast(0)
        val count = members.size.coerceAtLeast(1)
        val radiusFactor = sqrt(((index + 0.5f) / count.toFloat()).coerceIn(0f, 1f))
        val seedAngle = (index * goldenAngle) + ((Math.abs(family.key.hashCode()) % 360).toFloat() * (3.1415927f / 180f))
        val placementRadiusPx = (family.radiusPx - (safeSpacingPx * 0.92f)).coerceAtLeast(safeSpacingPx * 0.78f)
        val rawX = family.centerX + ((cos(seedAngle.toDouble()).toFloat() * radiusFactor * placementRadiusPx) / safeGraphWidthPx)
        val rawY = family.centerY + ((sin(seedAngle.toDouble()).toFloat() * radiusFactor * placementRadiusPx) / safeGraphHeightPx)
        val weightedEntryTags = node.tagNames
            .mapIndexedNotNull { indexInEntry, tagName ->
                val tag = tagNodesByName[tagName] ?: return@mapIndexedNotNull null
                val weight = when {
                    tagName == family.denominatorTag -> 3.0f
                    tagName in family.tags -> 1.65f
                    indexInEntry < 3 -> 0.95f
                    else -> 0.42f
                }
                Triple(tag.heatX, tag.heatY, weight)
            }
        val totalEntryWeight = weightedEntryTags.sumOf { it.third.toDouble() }.toFloat().coerceAtLeast(0.001f)
        val semanticX = if (weightedEntryTags.isEmpty()) {
            family.centerX
        } else {
            weightedEntryTags.sumOf { (it.first * it.third).toDouble() }.toFloat() / totalEntryWeight
        }
        val semanticY = if (weightedEntryTags.isEmpty()) {
            family.centerY
        } else {
            weightedEntryTags.sumOf { (it.second * it.third).toDouble() }.toFloat() / totalEntryWeight
        }
        val blendedX = (rawX * 0.74f) + (semanticX * 0.26f)
        val blendedY = (rawY * 0.74f) + (semanticY * 0.26f)
        val clamped = clampIntoBoundary(
            x = blendedX.coerceIn(marginX, 1f - marginX),
            y = blendedY.coerceIn(marginY, 1f - marginY),
            centerX = family.centerX,
            centerY = family.centerY,
            radiusPx = placementRadiusPx
        )
        positions[node.code] = floatArrayOf(clamped.first, clamped.second)
    }

    val nodesByFamily = nodes.groupBy { familyByCode[it.code]?.key.orEmpty() }

    repeat(24) {
        nodesByFamily.forEach { (familyKey, familyNodes) ->
            val family = familiesByKey[familyKey] ?: return@forEach
            val clampRadiusPx = (family.radiusPx - (safeSpacingPx * 0.92f)).coerceAtLeast(safeSpacingPx * 0.78f)
            for (leftIndex in familyNodes.indices) {
                val leftNode = familyNodes[leftIndex]
                val leftPos = positions[leftNode.code] ?: continue
                for (rightIndex in (leftIndex + 1) until familyNodes.size) {
                    val rightNode = familyNodes[rightIndex]
                    val rightPos = positions[rightNode.code] ?: continue
                    var dxPx = (rightPos[0] - leftPos[0]) * safeGraphWidthPx
                    var dyPx = (rightPos[1] - leftPos[1]) * safeGraphHeightPx
                    var distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx) + 0.000001f)
                    if (distancePx < minimumNodeDistancePx) {
                        if (distancePx < 0.01f) {
                            val radians = (((leftNode.code % 193) * 19f) + ((rightNode.code % 197) * 11f)) * (3.1415927f / 180f)
                            dxPx = cos(radians)
                            dyPx = sin(radians)
                            distancePx = 1f
                        }
                        val nx = dxPx / distancePx
                        val ny = dyPx / distancePx
                        val pushPx = (minimumNodeDistancePx - distancePx) * 0.5f
                        val leftClamped = clampIntoBoundary(
                            x = leftPos[0] - ((nx * pushPx) / safeGraphWidthPx),
                            y = leftPos[1] - ((ny * pushPx) / safeGraphHeightPx),
                            centerX = family.centerX,
                            centerY = family.centerY,
                            radiusPx = clampRadiusPx
                        )
                        val rightClamped = clampIntoBoundary(
                            x = rightPos[0] + ((nx * pushPx) / safeGraphWidthPx),
                            y = rightPos[1] + ((ny * pushPx) / safeGraphHeightPx),
                            centerX = family.centerX,
                            centerY = family.centerY,
                            radiusPx = clampRadiusPx
                        )
                        leftPos[0] = leftClamped.first
                        leftPos[1] = leftClamped.second
                        rightPos[0] = rightClamped.first
                        rightPos[1] = rightClamped.second
                    }
                }
            }
        }
    }

    for (iteration in 0 until 34) {
        var moved = false
        for (leftIndex in nodes.indices) {
            val leftNode = nodes[leftIndex]
            val leftPos = positions[leftNode.code] ?: continue
            val leftFamily = familyByCode[leftNode.code] ?: continue
            val leftClampRadius = (leftFamily.radiusPx - (safeSpacingPx * 0.92f)).coerceAtLeast(safeSpacingPx * 0.78f)
            for (rightIndex in (leftIndex + 1) until nodes.size) {
                val rightNode = nodes[rightIndex]
                val rightPos = positions[rightNode.code] ?: continue
                val rightFamily = familyByCode[rightNode.code] ?: continue
                val rightClampRadius = (rightFamily.radiusPx - (safeSpacingPx * 0.92f)).coerceAtLeast(safeSpacingPx * 0.78f)
                var dxPx = (rightPos[0] - leftPos[0]) * safeGraphWidthPx
                var dyPx = (rightPos[1] - leftPos[1]) * safeGraphHeightPx
                var distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx) + 0.000001f)
                if (distancePx < minimumNodeDistancePx) {
                    moved = true
                    if (distancePx < 0.01f) {
                        val radians = (((leftNode.code % 211) * 17f) + ((rightNode.code % 223) * 13f)) * (3.1415927f / 180f)
                        dxPx = cos(radians)
                        dyPx = sin(radians)
                        distancePx = 1f
                    }
                    val nx = dxPx / distancePx
                    val ny = dyPx / distancePx
                    val pushPx = (minimumNodeDistancePx - distancePx) * 0.5f
                    val leftClamped = clampIntoBoundary(
                        x = leftPos[0] - ((nx * pushPx) / safeGraphWidthPx),
                        y = leftPos[1] - ((ny * pushPx) / safeGraphHeightPx),
                        centerX = leftFamily.centerX,
                        centerY = leftFamily.centerY,
                        radiusPx = leftClampRadius
                    )
                    val rightClamped = clampIntoBoundary(
                        x = rightPos[0] + ((nx * pushPx) / safeGraphWidthPx),
                        y = rightPos[1] + ((ny * pushPx) / safeGraphHeightPx),
                        centerX = rightFamily.centerX,
                        centerY = rightFamily.centerY,
                        radiusPx = rightClampRadius
                    )
                    leftPos[0] = leftClamped.first
                    leftPos[1] = leftClamped.second
                    rightPos[0] = rightClamped.first
                    rightPos[1] = rightClamped.second
                }
            }
        }
        if (!moved) break
    }

    repeat(12) {
        nodes.forEach { node ->
            val ownFamily = familyByCode[node.code] ?: return@forEach
            val ownClampRadius = (ownFamily.radiusPx - (safeSpacingPx * 0.92f)).coerceAtLeast(safeSpacingPx * 0.78f)
            val allowedTags = node.tagNames.toSet()
            val current = positions[node.code] ?: return@forEach
            var currentX = current[0]
            var currentY = current[1]
            families.forEach { family ->
                if (family.key == ownFamily.key || family.tags.isEmpty()) return@forEach
                if (family.tags.all { it in allowedTags }) return@forEach
                val pushed = pushOutOfBoundary(
                    x = currentX,
                    y = currentY,
                    centerX = family.centerX,
                    centerY = family.centerY,
                    radiusPx = family.radiusPx,
                    moatPx = safeSpacingPx * 0.88f
                )
                currentX = pushed.first
                currentY = pushed.second
            }
            val reclamped = clampIntoBoundary(
                x = currentX,
                y = currentY,
                centerX = ownFamily.centerX,
                centerY = ownFamily.centerY,
                radiusPx = ownClampRadius
            )
            current[0] = reclamped.first
            current[1] = reclamped.second
        }
    }

    for (iteration in 0 until 18) {
        var moved = false
        for (leftIndex in nodes.indices) {
            val leftNode = nodes[leftIndex]
            val leftPos = positions[leftNode.code] ?: continue
            val leftFamily = familyByCode[leftNode.code] ?: continue
            val leftClampRadius = (leftFamily.radiusPx - (safeSpacingPx * 0.92f)).coerceAtLeast(safeSpacingPx * 0.78f)
            for (rightIndex in (leftIndex + 1) until nodes.size) {
                val rightNode = nodes[rightIndex]
                val rightPos = positions[rightNode.code] ?: continue
                val rightFamily = familyByCode[rightNode.code] ?: continue
                val rightClampRadius = (rightFamily.radiusPx - (safeSpacingPx * 0.92f)).coerceAtLeast(safeSpacingPx * 0.78f)
                var dxPx = (rightPos[0] - leftPos[0]) * safeGraphWidthPx
                var dyPx = (rightPos[1] - leftPos[1]) * safeGraphHeightPx
                var distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx) + 0.000001f)
                if (distancePx < minimumNodeDistancePx) {
                    moved = true
                    if (distancePx < 0.01f) {
                        val radians = (((leftNode.code % 229) * 11f) + ((rightNode.code % 233) * 13f)) * (3.1415927f / 180f)
                        dxPx = cos(radians)
                        dyPx = sin(radians)
                        distancePx = 1f
                    }
                    val nx = dxPx / distancePx
                    val ny = dyPx / distancePx
                    val pushPx = (minimumNodeDistancePx - distancePx) * 0.5f
                    val leftClamped = clampIntoBoundary(
                        x = leftPos[0] - ((nx * pushPx) / safeGraphWidthPx),
                        y = leftPos[1] - ((ny * pushPx) / safeGraphHeightPx),
                        centerX = leftFamily.centerX,
                        centerY = leftFamily.centerY,
                        radiusPx = leftClampRadius
                    )
                    val rightClamped = clampIntoBoundary(
                        x = rightPos[0] + ((nx * pushPx) / safeGraphWidthPx),
                        y = rightPos[1] + ((ny * pushPx) / safeGraphHeightPx),
                        centerX = rightFamily.centerX,
                        centerY = rightFamily.centerY,
                        radiusPx = rightClampRadius
                    )
                    leftPos[0] = leftClamped.first
                    leftPos[1] = leftClamped.second
                    rightPos[0] = rightClamped.first
                    rightPos[1] = rightClamped.second
                }
            }
        }
        if (!moved) break
    }

    families.forEach { family ->
        val radii = family.memberCodes
            .mapNotNull { code ->
                val pos = positions[code] ?: return@mapNotNull null
                val dxPx = (pos[0] - family.centerX) * safeGraphWidthPx
                val dyPx = (pos[1] - family.centerY) * safeGraphHeightPx
                sqrt((dxPx * dxPx) + (dyPx * dyPx)) + (thumbnailRadiusPx * 1.08f)
            }
            .sorted()
        family.radiusPx = when {
            radii.isEmpty() -> family.radiusPx
            radii.size <= 4 -> max(radii.last(), safeSpacingPx * 1.08f)
            else -> {
                val quantileIndex = ((radii.lastIndex.toFloat()) * 0.64f).roundToInt()
                    .coerceIn(0, radii.lastIndex)
                max(radii[quantileIndex], safeSpacingPx * 1.08f)
            }
        }
    }

    return nodes.map { node ->
        val family = familyByCode[node.code]
        val pos = positions[node.code] ?: floatArrayOf(node.x, node.y)
        node.copy(
            boundaryCenterX = family?.centerX ?: node.boundaryCenterX,
            boundaryCenterY = family?.centerY ?: node.boundaryCenterY,
            boundaryRadiusPx = family?.radiusPx ?: node.boundaryRadiusPx,
            x = pos[0],
            y = pos[1],
            dominantCircleTags = family?.tags.orEmpty()
        )
    }
}

private fun buildLegacyEntryLayoutResult(
    nodes: List<TagGraphEntryNode>,
    graphWidthPx: Float,
    graphHeightPx: Float,
    minimumVisualSpacingPx: Float
): TagGraphEntryLayoutResult {
    data class WorkingCircle(
        val tagName: String,
        val label: String,
        val tags: List<String>,
        var centerX: Float,
        var centerY: Float,
        var radiusPx: Float,
        val entryCount: Int
    )

    val safeGraphWidthPx = graphWidthPx.coerceAtLeast(1f)
    val safeGraphHeightPx = graphHeightPx.coerceAtLeast(1f)
    val minCircleRadiusPx = minimumVisualSpacingPx * 0.56f
    val memberPaddingPx = minimumVisualSpacingPx * 0.24f

    fun buildCircle(tagName: String, tags: List<String>, entries: List<TagGraphEntryNode>): WorkingCircle? {
        entries.firstOrNull() ?: return null
        val centerX = entries.map { entry ->
            if (entry.boundaryRadiusPx > 0f) entry.boundaryCenterX else entry.x
        }.average().toFloat()
        val centerY = entries.map { entry ->
            if (entry.boundaryRadiusPx > 0f) entry.boundaryCenterY else entry.y
        }.average().toFloat()
        val sortedMemberRadii = entries.map { entry ->
            val dxPx = (entry.x - centerX) * safeGraphWidthPx
            val dyPx = (entry.y - centerY) * safeGraphHeightPx
            sqrt((dxPx * dxPx) + (dyPx * dyPx)) + memberPaddingPx
        }.sorted()
        val memberRadiusPx = when {
            sortedMemberRadii.isEmpty() -> minCircleRadiusPx
            sortedMemberRadii.size <= 4 -> sortedMemberRadii.last()
            else -> {
                val quantileIndex = ((sortedMemberRadii.lastIndex.toFloat()) * 0.46f).roundToInt()
                    .coerceIn(0, sortedMemberRadii.lastIndex)
                sortedMemberRadii[quantileIndex]
            }
        }
        val sortedBoundaryRadii = entries.map { entry ->
            val dxPx = (entry.boundaryCenterX - centerX) * safeGraphWidthPx
            val dyPx = (entry.boundaryCenterY - centerY) * safeGraphHeightPx
            sqrt((dxPx * dxPx) + (dyPx * dyPx)) +
                (entry.boundaryRadiusPx * if (tags.size <= 1) 1f else 0.70f)
        }.sorted()
        val boundaryRadiusPx = when {
            sortedBoundaryRadii.isEmpty() -> minCircleRadiusPx
            sortedBoundaryRadii.size <= 4 -> sortedBoundaryRadii.last()
            else -> {
                val quantileIndex = ((sortedBoundaryRadii.lastIndex.toFloat()) * 0.52f).roundToInt()
                    .coerceIn(0, sortedBoundaryRadii.lastIndex)
                sortedBoundaryRadii[quantileIndex]
            }
        }
        return WorkingCircle(
            tagName = tagName,
            label = formatTagGraphCircleLabel(tags),
            tags = tags,
            centerX = centerX,
            centerY = centerY,
            radiusPx = max(max(memberRadiusPx, boundaryRadiusPx), minCircleRadiusPx)
                .coerceAtMost(min(safeGraphWidthPx, safeGraphHeightPx) * if (tags.size <= 1) 0.24f else 0.16f),
            entryCount = entries.size
        )
    }

    val singleTagCircles = nodes
        .mapNotNull { entry -> entry.dominantCircleTags.firstOrNull()?.let { tag -> tag to entry } }
        .groupBy({ it.first }, { it.second })
        .mapNotNull { (tag, entries) -> buildCircle(tag, listOf(tag), entries) }
    val overlapCircles = nodes
        .filter { it.dominantCircleTags.size >= 2 }
        .groupBy { entry ->
            entry.dominantCircleTags.sorted().joinToString("|")
        }
        .mapNotNull { (groupKey, entries) ->
            val tags = groupKey.split("|").filter { it.isNotBlank() }
            buildCircle(groupKey, tags, entries)
        }
    val familyCircles = (singleTagCircles + overlapCircles)
        .distinctBy { it.tagName }
        .toMutableList()

    return TagGraphEntryLayoutResult(
        nodes = nodes,
        familyCircles = familyCircles.map { circle ->
            TagGraphEntryFamilyCircle(
                tagName = circle.tagName,
                label = circle.label,
                centerX = circle.centerX,
                centerY = circle.centerY,
                radiusPx = circle.radiusPx,
                entryCount = circle.entryCount
            )
        }
    )
}

internal fun deriveLegacyEntryLayoutResultFromBase(
    snapshot: TagGraphSnapshot,
    baseLayout: TagGraphEntryLayoutResult,
    graphWidthPx: Float,
    graphHeightPx: Float,
    minimumVisualSpacingPx: Float
): TagGraphEntryLayoutResult? {
    if (snapshot.entryNodes.isEmpty() || baseLayout.nodes.isEmpty()) {
        return TagGraphEntryLayoutResult(
            nodes = emptyList(),
            familyCircles = emptyList()
        )
    }
    val baseNodesByCode = baseLayout.nodes.associateBy { it.code }
    val derivedNodes = snapshot.entryNodes.mapNotNull { entry ->
        val base = baseNodesByCode[entry.code] ?: return@mapNotNull null
        base.copy(
            title = entry.title,
            thumbnailUrl = entry.thumbnailUrl,
            rating = entry.rating,
            isRead = entry.isRead,
            pinned = entry.pinned,
            tagNames = entry.tagNames
        )
    }
    if (derivedNodes.isEmpty()) {
        return null
    }
    return buildLegacyEntryLayoutResult(
        nodes = derivedNodes,
        graphWidthPx = graphWidthPx,
        graphHeightPx = graphHeightPx,
        minimumVisualSpacingPx = minimumVisualSpacingPx
    )
}

private fun selectDominantEntryHeatmapAnchors(
    nodes: List<TagGraphEntryNode>,
    tagNodes: List<TagGraphNode>
): List<TagGraphNode> {
    if (nodes.isEmpty() || tagNodes.isEmpty()) return emptyList()
    data class AnchorCandidateScore(
        val node: TagGraphNode,
        val score: Float,
        val exclusiveRatio: Float,
        val maxCoOccurrenceRatio: Float,
        val minHeatDistance: Float
    )
    val maxAnchorCount = min(
        tagNodes.size,
        max(16, (sqrt(nodes.size.toFloat()) * 1.45f).roundToInt().coerceAtMost(24))
    )
    val minimumSupportCount = max(
        4,
        min(
            12,
            (nodes.size * 0.015f).roundToInt()
        )
    )
    val tagMemberCodes = tagNodes.associate { tag ->
        tag.normalizedName to nodes.asSequence()
            .filter { tag.normalizedName in it.tagNames }
            .map { it.code }
            .toSet()
    }
    val candidateAnchors = tagNodes
        .asSequence()
        .mapNotNull { tag ->
            val memberCodes = tagMemberCodes[tag.normalizedName].orEmpty()
            if (memberCodes.size < minimumSupportCount) null else tag
        }
        .sortedWith(
            compareByDescending<TagGraphNode> { it.localCount }
                .thenByDescending { it.popularCount }
                .thenBy { it.normalizedName }
        )
        .toList()
    if (candidateAnchors.isEmpty()) return emptyList()
    val selectedAnchors = mutableListOf<TagGraphNode>()
    val selectedMemberSets = mutableListOf<Set<Int>>()
    val selectedCoveredCodes = mutableSetOf<Int>()
    while (selectedAnchors.size < maxAnchorCount) {
        val next = candidateAnchors
            .asSequence()
            .filter { candidate -> selectedAnchors.none { it.normalizedName == candidate.normalizedName } }
            .map { candidate ->
                val candidateMembers = tagMemberCodes[candidate.normalizedName].orEmpty()
                val memberCount = candidateMembers.size.toFloat().coerceAtLeast(1f)
                val supportStrength = (memberCount / nodes.size.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                val overlapAgainstSelected = candidateMembers.intersect(selectedCoveredCodes).size.toFloat()
                val exclusiveCount = (candidateMembers.size - overlapAgainstSelected.toInt()).coerceAtLeast(0)
                val exclusiveRatio = (exclusiveCount.toFloat() / memberCount).coerceIn(0f, 1f)
                val uncoveredCoverage = (exclusiveCount.toFloat() / nodes.size.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                val maxCoOccurrenceRatio = selectedMemberSets.maxOfOrNull { selectedMembers ->
                    val intersection = candidateMembers.intersect(selectedMembers).size.toFloat()
                    val smaller = min(candidateMembers.size, selectedMembers.size).toFloat().coerceAtLeast(1f)
                    (intersection / smaller).coerceIn(0f, 1f)
                } ?: 0f
                val minHeatDistance = if (selectedAnchors.isEmpty()) {
                    1f
                } else {
                    selectedAnchors.map { existing ->
                        val dx = existing.heatX - candidate.heatX
                        val dy = existing.heatY - candidate.heatY
                        sqrt((dx * dx) + (dy * dy))
                    }.minOrNull() ?: 0f
                }
                val distanceBonus = (minHeatDistance / 0.28f).coerceIn(0f, 1f)
                val edgeDistance = sqrt(
                    ((candidate.heatX - 0.5f) * (candidate.heatX - 0.5f)) +
                        ((candidate.heatY - 0.5f) * (candidate.heatY - 0.5f))
                )
                val edgeStrength = (edgeDistance / 0.52f).coerceIn(0f, 1f)
                val exclusivityStrength = (1f - maxCoOccurrenceRatio).coerceIn(0f, 1f)
                val genericPenalty = when {
                    supportStrength <= 0.18f -> 1f
                    supportStrength <= 0.30f -> 1f - (((supportStrength - 0.18f) / 0.12f).coerceIn(0f, 1f) * 0.16f)
                    else -> 0.84f - (((supportStrength - 0.30f) / 0.45f).coerceIn(0f, 1f) * 0.58f)
                }
                val score = sqrt(memberCount) *
                    (0.16f + (supportStrength * 0.42f)) *
                    (0.16f + (uncoveredCoverage * 2.35f)) *
                    (0.18f + (exclusiveRatio * exclusiveRatio * 0.96f)) *
                    (0.20f + (exclusivityStrength * exclusivityStrength * 1.06f)) *
                    (0.84f + (distanceBonus * 0.16f)) *
                    (0.82f + (edgeStrength * 0.20f)) *
                    genericPenalty
                AnchorCandidateScore(
                    node = candidate,
                    score = score,
                    exclusiveRatio = exclusiveRatio,
                    maxCoOccurrenceRatio = maxCoOccurrenceRatio,
                    minHeatDistance = minHeatDistance
                )
            }
            .filter {
                it.exclusiveRatio >= 0.06f &&
                    it.maxCoOccurrenceRatio <= 0.76f &&
                    it.minHeatDistance >= 0.012f
            }
            .maxByOrNull { it.score }
            ?: break
        if (next.score < 0.26f && selectedAnchors.size >= 12) break
        selectedAnchors += next.node
        val selectedMembers = tagMemberCodes[next.node.normalizedName].orEmpty()
        selectedMemberSets += selectedMembers
        selectedCoveredCodes += selectedMembers
    }
    return if (selectedAnchors.isEmpty()) {
        candidateAnchors.take(maxAnchorCount)
    } else {
        selectedAnchors
    }
}

private fun computeEntryTagSetSimilarity(
    leftTags: Set<String>,
    rightTags: Set<String>
): Float {
    if (leftTags.isEmpty() || rightTags.isEmpty()) return 0f
    val intersection = leftTags.intersect(rightTags).size.toFloat()
    if (intersection <= 0f) return 0f
    val union = leftTags.union(rightTags).size.toFloat().coerceAtLeast(1f)
    val smaller = min(leftTags.size, rightTags.size).toFloat().coerceAtLeast(1f)
    val jaccard = intersection / union
    val overlap = intersection / smaller
    return ((jaccard * 0.58f) + (overlap * 0.42f)).coerceIn(0f, 1f)
}

private fun deterministicUnitJitter(code: Int): Pair<Float, Float> {
    val base = code.toLong() * 1103515245L + 12345L
    val x = (((base ushr 8) and 0xFFFF).toFloat() / 65535f) - 0.5f
    val y = ((((base * 48271L) ushr 8) and 0xFFFF).toFloat() / 65535f) - 0.5f
    return x to y
}

private fun layoutTagGraphEntryNodesOfflineRefined(
    nodes: List<TagGraphEntryNode>,
    tagNodes: List<TagGraphNode>,
    graphWidthPx: Float,
    graphHeightPx: Float,
    minimumVisualSpacingPx: Float
): List<TagGraphEntryNode> {
    if (nodes.size <= 1 || tagNodes.isEmpty()) return nodes

    data class WorkingFamily(
        val tagName: String,
        val label: String,
        val baseX: Float,
        val baseY: Float,
        var centerX: Float,
        var centerY: Float,
        val memberCodes: Set<Int>,
        val familyWeight: Float,
        val dominanceWeight: Float,
        val radiusSeedPx: Float
    )

    data class SimilarityEdge(
        val leftIndex: Int,
        val rightIndex: Int,
        val strength: Float,
        val desiredDistancePx: Float
    )

    val safeGraphWidthPx = graphWidthPx.coerceAtLeast(1f)
    val safeGraphHeightPx = graphHeightPx.coerceAtLeast(1f)
    val minGraphDimensionPx = min(safeGraphWidthPx, safeGraphHeightPx)
    val safeSpacingPx = minimumVisualSpacingPx.coerceAtLeast(10f) * 1.18f
    val marginX = ((safeSpacingPx * 0.72f) / safeGraphWidthPx).coerceIn(0.03f, 0.16f)
    val marginY = ((safeSpacingPx * 0.72f) / safeGraphHeightPx).coerceIn(0.03f, 0.16f)
    val tagNodeByName = tagNodes.associateBy { it.normalizedName }
    val entryTagSets = nodes.associate { it.code to it.tagNames.toSet() }

    val rawFamilyMembers = tagNodes.mapNotNull { tag ->
        val memberCodes = nodes.asSequence()
            .filter { tag.normalizedName in it.tagNames }
            .map { it.code }
            .toSet()
        if (memberCodes.size < 2) return@mapNotNull null
        tag to memberCodes
    }
    if (rawFamilyMembers.isEmpty()) return nodes

    val rawFamilyMemberMap = rawFamilyMembers.associate { it.first.normalizedName to it.second }
    val minimumFamilySupport = max(4, min(18, (nodes.size * 0.020f).roundToInt()))
    val selectedAnchorTags = selectDominantEntryHeatmapAnchors(nodes, tagNodes)
        .filter { (rawFamilyMemberMap[it.normalizedName]?.size ?: 0) >= minimumFamilySupport }
        .ifEmpty {
            rawFamilyMembers
                .sortedWith(
                    compareByDescending<Pair<TagGraphNode, Set<Int>>> { it.second.size }
                        .thenBy { it.first.normalizedName }
                )
                .take(min(20, rawFamilyMembers.size))
                .map { it.first }
        }
    val selectedFamilyMembers = selectedAnchorTags.mapNotNull { tag ->
        rawFamilyMemberMap[tag.normalizedName]?.let { tag to it }
    }
    val maxMemberCount = selectedFamilyMembers.maxOf { it.second.size }.coerceAtLeast(1)
    val workingFamilies = selectedFamilyMembers.map { (tag, memberCodes) ->
        val memberCount = memberCodes.size.toFloat()
        val supportRatio = (memberCount / nodes.size.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
        val normalizedCount = (memberCodes.size.toFloat() / maxMemberCount.toFloat()).coerceIn(0f, 1f)
        val familyWeight = sqrt(memberCount).coerceAtLeast(1f)
        val strongestOverlap = selectedFamilyMembers
            .asSequence()
            .filter { it.first.normalizedName != tag.normalizedName }
            .map { (_, otherMembers) ->
                computeSuggestionThemeSimilarity(memberCodes, otherMembers)
            }
            .maxOrNull()
            ?: 0f
        val exclusivity = (1f - strongestOverlap).coerceIn(0.12f, 1f)
        val dominanceWeight = (
            familyWeight *
                (1.08f + (exclusivity * 0.58f)) *
                (0.90f + (supportRatio * 0.26f)) *
                (1.02f - (normalizedCount * 0.18f))
            ).coerceAtLeast(0.35f)
        val radialScale = (1.12f + (exclusivity * 0.18f)).coerceIn(1f, 1.28f)
        val radiusSeedPx = (
            minGraphDimensionPx * 0.032f +
                (sqrt(memberCount) * safeSpacingPx * (0.54f + (supportRatio * 0.08f)))
            ).coerceIn(safeSpacingPx * 1.16f, minGraphDimensionPx * 0.20f)
        WorkingFamily(
            tagName = tag.normalizedName,
            label = tag.name,
            baseX = (0.5f + ((tag.heatX - 0.5f) * radialScale)).coerceIn(marginX, 1f - marginX),
            baseY = (0.5f + ((tag.heatY - 0.5f) * radialScale)).coerceIn(marginY, 1f - marginY),
            centerX = (0.5f + ((tag.heatX - 0.5f) * radialScale)).coerceIn(marginX, 1f - marginX),
            centerY = (0.5f + ((tag.heatY - 0.5f) * radialScale)).coerceIn(marginY, 1f - marginY),
            memberCodes = memberCodes,
            familyWeight = familyWeight,
            dominanceWeight = dominanceWeight,
            radiusSeedPx = radiusSeedPx
        )
    }
        .sortedWith(
            compareByDescending<WorkingFamily> { it.dominanceWeight }
                .thenBy { it.tagName }
        )
        .toMutableList()
    val familyByTag = workingFamilies.associateBy { it.tagName }

    repeat(220) { iteration ->
        val cooling = 1f - (iteration.toFloat() / 220f)
        for (leftIndex in workingFamilies.indices) {
            val left = workingFamilies[leftIndex]
            for (rightIndex in (leftIndex + 1) until workingFamilies.size) {
                val right = workingFamilies[rightIndex]
                var dxPx = (right.centerX - left.centerX) * safeGraphWidthPx
                var dyPx = (right.centerY - left.centerY) * safeGraphHeightPx
                var distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx) + 0.000001f)
                val similarity = computeSuggestionThemeSimilarity(left.memberCodes, right.memberCodes)
                val allowedOverlapPx = min(left.radiusSeedPx, right.radiusSeedPx) *
                    (0.04f + (0.10f * similarity))
                val minimumDistancePx = max(
                    left.radiusSeedPx + right.radiusSeedPx - allowedOverlapPx +
                        (safeSpacingPx * (2.12f - (0.44f * similarity))),
                    max(left.radiusSeedPx, right.radiusSeedPx) * 1.16f
                )
                if (distancePx < minimumDistancePx) {
                    if (distancePx < 0.01f) {
                        val angle = (((leftIndex + 3) * 37) + ((rightIndex + 5) * 19)).toFloat()
                        val radians = angle * (3.1415927f / 180f)
                        dxPx = cos(radians)
                        dyPx = sin(radians)
                        distancePx = 1f
                    }
                    val nx = dxPx / distancePx
                    val ny = dyPx / distancePx
                    val pushPx = (minimumDistancePx - distancePx) * (0.56f + (0.22f * cooling))
                    left.centerX = (left.centerX - ((nx * pushPx) / safeGraphWidthPx)).coerceIn(marginX, 1f - marginX)
                    left.centerY = (left.centerY - ((ny * pushPx) / safeGraphHeightPx)).coerceIn(marginY, 1f - marginY)
                    right.centerX = (right.centerX + ((nx * pushPx) / safeGraphWidthPx)).coerceIn(marginX, 1f - marginX)
                    right.centerY = (right.centerY + ((ny * pushPx) / safeGraphHeightPx)).coerceIn(marginY, 1f - marginY)
                }
            }
        }
        workingFamilies.forEach { family ->
            family.centerX = (family.centerX * 0.90f) + (family.baseX * 0.10f)
            family.centerY = (family.centerY * 0.90f) + (family.baseY * 0.10f)
        }
    }

    val allTagHeatPositionsByCode = nodes.associate { node ->
        val positions = node.tagNames.mapNotNull { tagName ->
            tagNodeByName[tagName]?.let { it.heatX to it.heatY }
        }
        val avg = if (positions.isNotEmpty()) {
            positions.map { it.first }.average().toFloat() to positions.map { it.second }.average().toFloat()
        } else {
            node.x to node.y
        }
        node.code to avg
    }

    val dominantTagsByCode = nodes.associate { node ->
        val matchedFamilies = node.tagNames.mapNotNull { familyByTag[it] }
        val selected = if (matchedFamilies.isNotEmpty()) {
            val ranked = matchedFamilies
                .sortedWith(
                    compareByDescending<WorkingFamily> { it.dominanceWeight }
                        .thenByDescending { it.memberCodes.size }
                        .thenBy { it.tagName }
                )
                .distinctBy { it.tagName }
            val primary = ranked.firstOrNull()
                    buildList {
                if (primary != null) {
                    add(primary.tagName)
                    ranked.drop(1).forEach { family ->
                        if (size >= 4) return@forEach
                        val ratio = family.dominanceWeight / primary.dominanceWeight.coerceAtLeast(0.0001f)
                        if (ratio >= 0.20f) {
                            add(family.tagName)
                        }
                    }
                }
            }
        } else {
            val semanticCenter = allTagHeatPositionsByCode[node.code] ?: (node.x to node.y)
            workingFamilies
                .sortedWith(
                    compareBy<WorkingFamily> {
                        val dx = it.centerX - semanticCenter.first
                        val dy = it.centerY - semanticCenter.second
                        sqrt((dx * dx) + (dy * dy))
                    }.thenByDescending { it.dominanceWeight }
                )
                .take(1)
                .map { it.tagName }
        }
        node.code to selected
    }

    val indexedNodes = nodes.withIndex().associate { it.value.code to it.index }
    val positions = mutableMapOf<Int, FloatArray>()
    nodes.forEach { node ->
        val dominantFamilies = dominantTagsByCode[node.code].orEmpty().mapNotNull { familyByTag[it] }
        val allFamilyMatches = node.tagNames.mapNotNull { familyByTag[it] }
        val familyPull = if (dominantFamilies.isNotEmpty()) {
            val totalWeight = dominantFamilies.mapIndexed { index, family ->
                when (index) {
                    0 -> family.dominanceWeight
                    else -> family.dominanceWeight * 0.22f
                }
            }.sum().coerceAtLeast(0.001f)
            val x = dominantFamilies.mapIndexed { index, family ->
                val weight = when (index) {
                    0 -> family.dominanceWeight
                    else -> family.dominanceWeight * 0.22f
                }
                family.centerX * weight
            }.sum() / totalWeight
            val y = dominantFamilies.mapIndexed { index, family ->
                val weight = when (index) {
                    0 -> family.dominanceWeight
                    else -> family.dominanceWeight * 0.22f
                }
                family.centerY * weight
            }.sum() / totalWeight
            x to y
        } else {
            allTagHeatPositionsByCode[node.code] ?: (node.x to node.y)
        }
        val nuance = allTagHeatPositionsByCode[node.code] ?: familyPull
        val extraPull = if (allFamilyMatches.isNotEmpty()) {
            val avgX = allFamilyMatches.map { it.centerX }.average().toFloat()
            val avgY = allFamilyMatches.map { it.centerY }.average().toFloat()
            avgX to avgY
        } else {
            nuance
        }
        val jitter = deterministicUnitJitter(node.code)
        val startX = ((familyPull.first * 0.84f) + (nuance.first * 0.10f) + (extraPull.first * 0.06f) +
            ((jitter.first * safeSpacingPx * 0.08f) / safeGraphWidthPx)).coerceIn(marginX, 1f - marginX)
        val startY = ((familyPull.second * 0.84f) + (nuance.second * 0.10f) + (extraPull.second * 0.06f) +
            ((jitter.second * safeSpacingPx * 0.08f) / safeGraphHeightPx)).coerceIn(marginY, 1f - marginY)
        positions[node.code] = floatArrayOf(startX, startY)
    }

    val similarityEdges = buildList {
        for (leftIndex in nodes.indices) {
            val leftNode = nodes[leftIndex]
            val leftTags = entryTagSets[leftNode.code].orEmpty()
            val leftDominant = dominantTagsByCode[leftNode.code].orEmpty().toSet()
            val leftPrimary = dominantTagsByCode[leftNode.code].orEmpty().firstOrNull().orEmpty()
            for (rightIndex in (leftIndex + 1) until nodes.size) {
                val rightNode = nodes[rightIndex]
                val rightTags = entryTagSets[rightNode.code].orEmpty()
                val rightDominant = dominantTagsByCode[rightNode.code].orEmpty().toSet()
                val rightPrimary = dominantTagsByCode[rightNode.code].orEmpty().firstOrNull().orEmpty()
                val tagSimilarity = computeEntryTagSetSimilarity(leftTags, rightTags)
                val dominantSimilarity = if (leftDominant.isEmpty() || rightDominant.isEmpty()) {
                    0f
                } else {
                    leftDominant.intersect(rightDominant).size.toFloat() /
                        leftDominant.union(rightDominant).size.toFloat().coerceAtLeast(1f)
                }
                val samePrimary = leftPrimary.isNotBlank() && leftPrimary == rightPrimary
                val strength = (
                    (tagSimilarity * if (samePrimary) 0.46f else 0.34f) +
                        (dominantSimilarity * 0.24f) +
                        if (samePrimary) 0.34f else 0f
                    ).coerceIn(0f, 1f)
                if (strength >= if (samePrimary) 0.20f else 0.30f) {
                    add(
                        SimilarityEdge(
                            leftIndex = leftIndex,
                            rightIndex = rightIndex,
                            strength = strength,
                            desiredDistancePx = safeSpacingPx * if (samePrimary) {
                                1.06f - (0.22f * strength)
                            } else {
                                1.36f - (0.10f * strength)
                            }
                        )
                    )
                }
            }
        }
    }

    repeat(300) { iteration ->
        val cooling = 1f - (iteration.toFloat() / 300f)
        nodes.forEach { node ->
            val pos = positions[node.code] ?: return@forEach
            val dominantFamilies = dominantTagsByCode[node.code].orEmpty().mapNotNull { familyByTag[it] }
            val nuance = allTagHeatPositionsByCode[node.code] ?: (pos[0] to pos[1])
            if (dominantFamilies.isNotEmpty()) {
                val totalWeight = dominantFamilies.mapIndexed { index, family ->
                    when (index) {
                        0 -> family.dominanceWeight
                        else -> family.dominanceWeight * 0.20f
                    }
                }.sum().coerceAtLeast(0.001f)
                val targetX = dominantFamilies.mapIndexed { index, family ->
                    val weight = when (index) {
                        0 -> family.dominanceWeight
                        else -> family.dominanceWeight * 0.20f
                    }
                    family.centerX * weight
                }.sum() / totalWeight
                val targetY = dominantFamilies.mapIndexed { index, family ->
                    val weight = when (index) {
                        0 -> family.dominanceWeight
                        else -> family.dominanceWeight * 0.20f
                    }
                    family.centerY * weight
                }.sum() / totalWeight
                pos[0] = (pos[0] * (1f - (0.12f * cooling))) + (targetX * (0.12f * cooling))
                pos[1] = (pos[1] * (1f - (0.12f * cooling))) + (targetY * (0.12f * cooling))
            }
            pos[0] = (pos[0] * 0.997f) + (nuance.first * 0.003f)
            pos[1] = (pos[1] * 0.997f) + (nuance.second * 0.003f)
            pos[0] = pos[0].coerceIn(marginX, 1f - marginX)
            pos[1] = pos[1].coerceIn(marginY, 1f - marginY)
        }

        similarityEdges.forEach { edge ->
            val leftNode = nodes[edge.leftIndex]
            val rightNode = nodes[edge.rightIndex]
            val leftPos = positions[leftNode.code] ?: return@forEach
            val rightPos = positions[rightNode.code] ?: return@forEach
            var dxPx = (rightPos[0] - leftPos[0]) * safeGraphWidthPx
            var dyPx = (rightPos[1] - leftPos[1]) * safeGraphHeightPx
            var distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx) + 0.000001f)
            if (distancePx > edge.desiredDistancePx) {
                val nx = dxPx / distancePx
                val ny = dyPx / distancePx
                val pullPx = (distancePx - edge.desiredDistancePx) * edge.strength * 0.040f * cooling
                leftPos[0] = (leftPos[0] + ((nx * pullPx) / safeGraphWidthPx)).coerceIn(marginX, 1f - marginX)
                leftPos[1] = (leftPos[1] + ((ny * pullPx) / safeGraphHeightPx)).coerceIn(marginY, 1f - marginY)
                rightPos[0] = (rightPos[0] - ((nx * pullPx) / safeGraphWidthPx)).coerceIn(marginX, 1f - marginX)
                rightPos[1] = (rightPos[1] - ((ny * pullPx) / safeGraphHeightPx)).coerceIn(marginY, 1f - marginY)
            }
        }

        var moved = false
        for (leftIndex in nodes.indices) {
            val leftNode = nodes[leftIndex]
            val leftPos = positions[leftNode.code] ?: continue
            val leftPrimary = dominantTagsByCode[leftNode.code].orEmpty().firstOrNull().orEmpty()
            val leftDominant = dominantTagsByCode[leftNode.code].orEmpty().toSet()
            for (rightIndex in (leftIndex + 1) until nodes.size) {
                val rightNode = nodes[rightIndex]
                val rightPos = positions[rightNode.code] ?: continue
                val rightPrimary = dominantTagsByCode[rightNode.code].orEmpty().firstOrNull().orEmpty()
                val rightDominant = dominantTagsByCode[rightNode.code].orEmpty().toSet()
                var dxPx = (rightPos[0] - leftPos[0]) * safeGraphWidthPx
                var dyPx = (rightPos[1] - leftPos[1]) * safeGraphHeightPx
                var distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx) + 0.000001f)
                val samePrimary = leftPrimary.isNotBlank() && leftPrimary == rightPrimary
                val sharedDominant = leftDominant.intersect(rightDominant).isNotEmpty()
                val minDistancePx = when {
                    samePrimary -> safeSpacingPx * 0.96f
                    sharedDominant -> safeSpacingPx * 1.10f
                    else -> safeSpacingPx * 1.30f
                }
                if (distancePx < minDistancePx) {
                    moved = true
                    if (distancePx < 0.01f) {
                        val jitter = deterministicUnitJitter(leftNode.code xor rightNode.code)
                        dxPx = jitter.first.coerceAtLeast(0.0001f)
                        dyPx = jitter.second.coerceAtLeast(0.0001f)
                        distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx)).coerceAtLeast(0.0001f)
                    }
                    val nx = dxPx / distancePx
                    val ny = dyPx / distancePx
                    val pushPx = (minDistancePx - distancePx) * 0.52f
                    leftPos[0] = (leftPos[0] - ((nx * pushPx) / safeGraphWidthPx)).coerceIn(marginX, 1f - marginX)
                    leftPos[1] = (leftPos[1] - ((ny * pushPx) / safeGraphHeightPx)).coerceIn(marginY, 1f - marginY)
                    rightPos[0] = (rightPos[0] + ((nx * pushPx) / safeGraphWidthPx)).coerceIn(marginX, 1f - marginX)
                    rightPos[1] = (rightPos[1] + ((ny * pushPx) / safeGraphHeightPx)).coerceIn(marginY, 1f - marginY)
                }
            }
        }
        if (!moved && iteration > 120) return@repeat
    }

    val familyRadiusByTag = workingFamilies.associate { family ->
        val memberPositions = family.memberCodes.mapNotNull { code -> positions[code] }
        val radiusPx = if (memberPositions.isEmpty()) {
            family.radiusSeedPx
        } else {
            memberPositions.map { position ->
                val dxPx = (position[0] - family.centerX) * safeGraphWidthPx
                val dyPx = (position[1] - family.centerY) * safeGraphHeightPx
                sqrt((dxPx * dxPx) + (dyPx * dyPx)) + (safeSpacingPx * 0.44f)
            }.sorted().let { distances ->
                when {
                    distances.isEmpty() -> family.radiusSeedPx
                    distances.size <= 4 -> max(distances.last(), family.radiusSeedPx * 0.78f)
                    else -> {
                        val index = ((distances.lastIndex.toFloat()) * 0.76f).roundToInt().coerceIn(0, distances.lastIndex)
                        max(distances[index], family.radiusSeedPx * 0.78f)
                    }
                }
            }
        }
        family.tagName to radiusPx
    }

    return nodes.map { node ->
        val pos = positions[node.code] ?: floatArrayOf(node.x, node.y)
        val dominantTags = dominantTagsByCode[node.code].orEmpty()
        val dominantFamilies = dominantTags.mapNotNull { familyByTag[it] }
        val primaryFamily = dominantFamilies.firstOrNull()
        val boundaryCenter = if (primaryFamily != null) {
            val secondaryFamily = dominantFamilies.getOrNull(1)
            val centerX = primaryFamily.centerX +
                ((secondaryFamily?.centerX ?: primaryFamily.centerX) - primaryFamily.centerX) * 0.14f
            val centerY = primaryFamily.centerY +
                ((secondaryFamily?.centerY ?: primaryFamily.centerY) - primaryFamily.centerY) * 0.14f
            centerX to centerY
        } else {
            pos[0] to pos[1]
        }
        val boundaryRadius = when {
            primaryFamily == null -> safeSpacingPx * 1.8f
            dominantFamilies.size == 1 -> familyRadiusByTag[primaryFamily.tagName]
                ?: primaryFamily.radiusSeedPx
            else -> {
                val blendedCenterX = boundaryCenter.first
                val blendedCenterY = boundaryCenter.second
                val familySpread = dominantFamilies.maxOf { family ->
                    val dxPx = (family.centerX - blendedCenterX) * safeGraphWidthPx
                    val dyPx = (family.centerY - blendedCenterY) * safeGraphHeightPx
                    sqrt((dxPx * dxPx) + (dyPx * dyPx)) + (familyRadiusByTag[family.tagName]
                        ?: family.radiusSeedPx) * 0.54f
                }
                max(
                    familySpread.coerceAtLeast(safeSpacingPx * 1.78f),
                    familyRadiusByTag[primaryFamily.tagName] ?: primaryFamily.radiusSeedPx
                )
            }
        }
        node.copy(
            dominantCircleTags = dominantTags,
            boundaryCenterX = boundaryCenter.first,
            boundaryCenterY = boundaryCenter.second,
            boundaryRadiusPx = boundaryRadius,
            x = pos[0],
            y = pos[1]
        )
    }
}

internal fun computeLegacyEntryLayoutResult(
    snapshot: TagGraphSnapshot,
    graphWidthPx: Float,
    graphHeightPx: Float,
    minimumVisualSpacingPx: Float
): TagGraphEntryLayoutResult {
    // Legacy saved-layout path kept in the file for comparison/recovery:
    // val resolvedNodes = layoutTagGraphEntryNodesForCanvas(...)
    // return buildLegacyEntryLayoutResult(...)
    return computeHierarchicalEntryLayoutResult(
        snapshot = snapshot,
        graphWidthPx = graphWidthPx,
        graphHeightPx = graphHeightPx,
        minimumVisualSpacingPx = minimumVisualSpacingPx
    )
}

private fun computeHierarchicalEntryLayoutResult(
    snapshot: TagGraphSnapshot,
    graphWidthPx: Float,
    graphHeightPx: Float,
    minimumVisualSpacingPx: Float
): TagGraphEntryLayoutResult {
    if (snapshot.entryNodes.isEmpty() || snapshot.nodes.isEmpty()) {
        return TagGraphEntryLayoutResult(
            nodes = emptyList(),
            familyCircles = emptyList()
        )
    }

    data class TagStats(
        val tag: String,
        val node: TagGraphNode,
        val memberCodes: Set<Int>,
        val support: Int,
        val edgeBias: Float,
        var genericity: Float = 0f
    )

    data class WorkingEntry(
        val base: TagGraphEntryNode,
        val tags: Set<String>,
        var primaryTag: String = "",
        var secondaryTags: List<String> = emptyList()
    )

    data class Subcluster(
        val anchorTag: String,
        val tags: List<String>,
        val members: MutableList<WorkingEntry>,
        var preferredLocalX: Float = 0f,
        var preferredLocalY: Float = 0f,
        var localX: Float = 0f,
        var localY: Float = 0f,
        var radiusPx: Float = 0f
    )

    data class AnchorCluster(
        val tag: String,
        val node: TagGraphNode,
        val members: MutableList<WorkingEntry>,
        val subclusters: MutableList<Subcluster> = mutableListOf(),
        var desiredX: Float = 0f,
        var desiredY: Float = 0f,
        var centerX: Float = 0f,
        var centerY: Float = 0f,
        var envelopeRadiusPx: Float = 0f
    )

    fun hashUnit(seed: String): Float {
        val positive = seed.hashCode().toUInt().toLong()
        val bucket = (positive % 10_000L).toFloat()
        return bucket / 10_000f
    }

    fun circularOffsets(count: Int, spacingPx: Float, rotationTurns: Float): List<Pair<Float, Float>> {
        if (count <= 0) return emptyList()
        if (count == 1) return listOf(0f to 0f)
        val offsets = mutableListOf<Pair<Float, Float>>()
        offsets += 0f to 0f
        var placed = 1
        var ring = 1
        while (placed < count) {
            val ringRadius = ring * spacingPx * 0.92f
            val circumference = (2.0 * Math.PI * ringRadius.toDouble()).toFloat()
            val slots = max(6, (circumference / (spacingPx * 0.95f)).roundToInt())
            val phase = rotationTurns * (Math.PI * 2.0)
            for (slot in 0 until slots) {
                if (placed >= count) break
                val angle = phase + ((slot.toDouble() / slots.toDouble()) * Math.PI * 2.0)
                offsets += (cos(angle.toFloat()) * ringRadius) to (sin(angle.toFloat()) * ringRadius)
                placed++
            }
            ring++
        }
        return offsets
    }

    fun packedClusterRadiusPx(count: Int, collisionDistancePx: Float): Float {
        if (count <= 1) return collisionDistancePx * 0.62f
        val offsets = circularOffsets(
            count = count,
            spacingPx = collisionDistancePx,
            rotationTurns = 0f
        )
        val furthestOffset = offsets.maxOfOrNull { (dx, dy) ->
            sqrt((dx * dx) + (dy * dy))
        } ?: 0f
        return furthestOffset + (collisionDistancePx * 0.74f)
    }

    fun refineClusterOffsets(
        offsets: List<Pair<Float, Float>>,
        minimumCenterDistancePx: Float
    ): List<Pair<Float, Float>> {
        if (offsets.size <= 1) return offsets
        val points = offsets.map { floatArrayOf(it.first, it.second) }.toMutableList()
        repeat(80) {
            var moved = false
            for (leftIndex in 0 until points.size) {
                val left = points[leftIndex]
                for (rightIndex in (leftIndex + 1) until points.size) {
                    val right = points[rightIndex]
                    val dx = right[0] - left[0]
                    val dy = right[1] - left[1]
                    val distance = sqrt((dx * dx) + (dy * dy)).coerceAtLeast(0.0001f)
                    if (distance >= minimumCenterDistancePx) continue
                    val push = (minimumCenterDistancePx - distance) * 0.52f
                    val nx = dx / distance
                    val ny = dy / distance
                    left[0] -= nx * push
                    left[1] -= ny * push
                    right[0] += nx * push
                    right[1] += ny * push
                    moved = true
                }
            }
            points.forEach { point ->
                point[0] *= 0.992f
                point[1] *= 0.992f
            }
            if (!moved) return@repeat
        }
        return points.map { it[0] to it[1] }
    }

    val safeGraphWidthPx = graphWidthPx.coerceAtLeast(1f)
    val safeGraphHeightPx = graphHeightPx.coerceAtLeast(1f)
    val spacingPx = (minimumVisualSpacingPx * 1.14f).coerceAtLeast(18f)
    // One shared footprint for the actual loaded thumbnail body. This needs to drive
    // local packing, family radii, family repel, and final fit together.
    val loadedThumbCollisionPx = minimumVisualSpacingPx.coerceAtLeast(18f)
    val usableTagNodes = snapshot.nodes.associateBy { it.normalizedName }
    val strongestNeighbors = snapshot.strongestNeighborsByTag
        .mapValues { (_, neighbors) -> neighbors.toSet() }
    val entryRows = snapshot.entryNodes
        .map { entry ->
            WorkingEntry(
                base = entry,
                tags = entry.tagNames.filter { it in usableTagNodes }.toSet()
            )
        }
        .filter { it.tags.isNotEmpty() }
    if (entryRows.isEmpty()) {
        return TagGraphEntryLayoutResult(
            nodes = emptyList(),
            familyCircles = emptyList()
        )
    }

    val tagMembers = mutableMapOf<String, MutableSet<Int>>()
    entryRows.forEach { entry ->
        entry.tags.forEach { tag ->
            tagMembers.getOrPut(tag) { linkedSetOf() } += entry.base.code
        }
    }
    val allCodes = entryRows.map { it.base.code }.toSet()
    val totalEntryCount = allCodes.size.coerceAtLeast(1)

    val tagStatsByName = usableTagNodes.mapNotNull { (name, node) ->
        val members = tagMembers[name].orEmpty()
        if (members.isEmpty()) null else {
            val dx = node.heatX - 0.5f
            val dy = node.heatY - 0.5f
            val edgeBias = (sqrt((dx * dx) + (dy * dy)) / 0.71f).coerceIn(0f, 1f)
            name to TagStats(
                tag = name,
                node = node,
                memberCodes = members,
                support = members.size,
                edgeBias = edgeBias
            )
        }
    }.toMap()
    if (tagStatsByName.isEmpty()) {
        return TagGraphEntryLayoutResult(
            nodes = emptyList(),
            familyCircles = emptyList()
        )
    }

    fun overlapRatio(left: String, right: String): Float {
        if (left == right) return 1f
        val leftMembers = tagStatsByName[left]?.memberCodes.orEmpty()
        val rightMembers = tagStatsByName[right]?.memberCodes.orEmpty()
        if (leftMembers.isEmpty() || rightMembers.isEmpty()) return 0f
        val shared = leftMembers.count { it in rightMembers }
        return (shared.toFloat() / min(leftMembers.size, rightMembers.size).coerceAtLeast(1).toFloat())
            .coerceIn(0f, 1f)
    }

    fun familySimilarity(leftTags: List<String>, rightTags: List<String>): Float {
        val leftSecondary = leftTags.drop(1)
        val rightSecondary = rightTags.drop(1)
        if (leftSecondary.isEmpty() || rightSecondary.isEmpty()) return 0f
        var best = 0f
        leftSecondary.forEach { leftTag ->
            rightSecondary.forEach { rightTag ->
                val overlapScore = overlapRatio(leftTag, rightTag)
                val neighborScore = when {
                    rightTag in strongestNeighbors[leftTag].orEmpty() -> 0.92f
                    leftTag in strongestNeighbors[rightTag].orEmpty() -> 0.92f
                    else -> 0f
                }
                val leftNode = usableTagNodes[leftTag]
                val rightNode = usableTagNodes[rightTag]
                val heatScore = if (leftNode != null && rightNode != null) {
                    val dx = leftNode.heatX - rightNode.heatX
                    val dy = leftNode.heatY - rightNode.heatY
                    (1f - (sqrt((dx * dx) + (dy * dy)) / 0.55f)).coerceIn(0f, 1f)
                } else {
                    0f
                }
                best = max(best, max(neighborScore, max(overlapScore, heatScore * 0.85f)))
            }
        }
        return best
    }

    fun blendAngle(current: Float, target: Float, amount: Float): Float {
        val wrappedDelta = Math.atan2(
            sin(target - current).toDouble(),
            cos(target - current).toDouble()
        ).toFloat()
        return current + (wrappedDelta * amount.coerceIn(0f, 1f))
    }

    val candidateStats = tagStatsByName.values
        .filter { it.support >= max(2, (entryRows.size * 0.008f).roundToInt()) }
        .sortedWith(
            compareByDescending<TagStats> { it.support }
                .thenByDescending { it.edgeBias }
                .thenBy { it.tag }
        )
    val genericPeers = candidateStats.take(30)
    candidateStats.forEach { stat ->
        val topOverlaps = genericPeers
            .asSequence()
            .filter { it.tag != stat.tag }
            .map { peer ->
                overlapRatio(stat.tag, peer.tag) * sqrt(peer.support.toFloat())
            }
            .sortedDescending()
            .take(6)
            .toList()
        stat.genericity = if (topOverlaps.isEmpty()) {
            0f
        } else {
            (topOverlaps.average().toFloat() / sqrt(genericPeers.firstOrNull()?.support?.toFloat() ?: 1f))
                .coerceIn(0f, 1.35f)
        }
    }

    val selectedAnchors = linkedSetOf<String>()
    val coveredCodes = mutableSetOf<Int>()
    val targetAnchorCount = ((sqrt(entryRows.size.toFloat()) * 0.96f).roundToInt() + 12).coerceIn(18, 32)

    while (selectedAnchors.size < targetAnchorCount) {
        val next = candidateStats
            .asSequence()
            .filter { it.tag !in selectedAnchors }
            .map { stat ->
                val uncoveredCount = stat.memberCodes.count { it !in coveredCodes }
                val uncoveredRatio = uncoveredCount.toFloat() / stat.support.toFloat().coerceAtLeast(1f)
                val exclusivityVsSelected = if (selectedAnchors.isEmpty()) {
                    1f
                } else {
                    selectedAnchors
                        .map { 1f - overlapRatio(stat.tag, it) }
                        .average()
                        .toFloat()
                        .coerceIn(0f, 1f)
                }
                val supportScore = stat.support.toFloat() / totalEntryCount.toFloat()
                val score = (supportScore * 1.4f) +
                    (uncoveredRatio * 1.8f) +
                    (exclusivityVsSelected * 1.95f) +
                    (stat.edgeBias * 0.55f) -
                    (stat.genericity * 1.45f)
                stat to score
            }
            .maxByOrNull { it.second }
            ?: break
        if (selectedAnchors.size >= 20 && next.second < 0.32f) break
        selectedAnchors += next.first.tag
        coveredCodes += next.first.memberCodes
        if (coveredCodes.size >= (entryRows.size * 0.96f).roundToInt() && selectedAnchors.size >= 18) {
            break
        }
    }

    while (selectedAnchors.size < 40) {
        val uncoveredEntries = entryRows.filter { entry -> entry.tags.none { it in selectedAnchors } }
        if (uncoveredEntries.isEmpty()) break
        val tagCounts = mutableMapOf<String, Int>()
        uncoveredEntries.forEach { entry ->
            entry.tags.forEach { tag ->
                tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
            }
        }
        val nextTag = tagCounts.entries
            .mapNotNull { (tag, uncoveredCount) ->
                val stat = tagStatsByName[tag] ?: return@mapNotNull null
                val score = uncoveredCount.toFloat() +
                    (1f - stat.genericity) * 3.6f +
                    stat.edgeBias * 0.9f
                tag to score
            }
            .maxByOrNull { it.second }
            ?.first
            ?: break
        selectedAnchors += nextTag
    }

    if (selectedAnchors.isEmpty()) {
        selectedAnchors += candidateStats.take(10).map { it.tag }
    }

    fun entryHeatCentroid(entryTags: Set<String>): Pair<Float, Float> {
        val nodes = entryTags.mapNotNull { usableTagNodes[it] }
        if (nodes.isEmpty()) return 0.5f to 0.5f
        val x = nodes.map { it.heatX }.average().toFloat()
        val y = nodes.map { it.heatY }.average().toFloat()
        return x to y
    }

    fun assignmentScore(tag: String, entryTags: Set<String>, centroid: Pair<Float, Float>): Float {
        val stat = tagStatsByName[tag] ?: return Float.NEGATIVE_INFINITY
        val exclusivityInsideEntry = entryTags
            .asSequence()
            .filter { it != tag && it in tagStatsByName }
            .map { 1f - overlapRatio(tag, it) }
            .toList()
            .let { values ->
                if (values.isEmpty()) 1f else values.average().toFloat().coerceIn(0f, 1f)
            }
        val cohesionToEntry = entryTags
            .asSequence()
            .filter { it != tag && it in usableTagNodes }
            .map { otherTag ->
                val overlap = overlapRatio(tag, otherTag)
                val leftNode = usableTagNodes[tag]
                val rightNode = usableTagNodes[otherTag]
                val heatAffinity = if (leftNode != null && rightNode != null) {
                    val dx = leftNode.heatX - rightNode.heatX
                    val dy = leftNode.heatY - rightNode.heatY
                    (1f - (sqrt((dx * dx) + (dy * dy)) / 0.62f)).coerceIn(0f, 1f)
                } else {
                    0f
                }
                max(overlap, heatAffinity)
            }
            .toList()
            .let { values ->
                if (values.isEmpty()) 0.5f else values.average().toFloat().coerceIn(0f, 1f)
            }
        val centroidDistance = run {
            val dx = stat.node.heatX - centroid.first
            val dy = stat.node.heatY - centroid.second
            sqrt((dx * dx) + (dy * dy))
        }
        val centroidProximity = (1f - (centroidDistance / 0.44f)).coerceIn(0f, 1f)
        val supportScore = stat.support.toFloat() / totalEntryCount.toFloat()
        return (centroidProximity * 2.6f) +
            (cohesionToEntry * 1.35f) +
            (exclusivityInsideEntry * 1.35f) +
            ((1f - stat.genericity.coerceIn(0f, 1f)) * 1.45f) +
            (supportScore * 0.55f) +
            (stat.edgeBias * 0.35f)
    }

    entryRows.forEach { entry ->
        val centroid = entryHeatCentroid(entry.tags)
        val candidateAnchors = entry.tags.filter { it in selectedAnchors }
        val resolvedPrimary = if (candidateAnchors.isNotEmpty()) {
            candidateAnchors.maxByOrNull { assignmentScore(it, entry.tags, centroid) }
        } else {
            entry.tags.maxByOrNull { assignmentScore(it, entry.tags, centroid) }
        }
        entry.primaryTag = resolvedPrimary ?: entry.tags.first()
    }

    val anchorGroups = entryRows
        .groupBy { it.primaryTag }
        .mapNotNull { (tag, members) ->
            val stat = tagStatsByName[tag] ?: return@mapNotNull null
            AnchorCluster(
                tag = tag,
                node = stat.node,
                members = members.toMutableList()
            )
        }
        .sortedByDescending { it.members.size }
        .toMutableList()

    anchorGroups.forEach { anchor ->
        val withinAnchorCounts = mutableMapOf<String, Int>()
        anchor.members.forEach { entry ->
            entry.tags.forEach { tag ->
                if (tag != anchor.tag) {
                    withinAnchorCounts[tag] = (withinAnchorCounts[tag] ?: 0) + 1
                }
            }
        }
        anchor.members.forEach { entry ->
            val scoredSecondaries = entry.tags
                .asSequence()
                .filter { it != anchor.tag }
                .mapNotNull { tag ->
                    val stat = tagStatsByName[tag] ?: return@mapNotNull null
                    val withinCount = withinAnchorCounts[tag] ?: 0
                    val score = (withinCount.toFloat() * 0.34f) +
                        ((1f - overlapRatio(anchor.tag, tag)) * 7.2f) +
                        ((1f - stat.genericity.coerceIn(0f, 1f)) * 2.8f) +
                        (stat.edgeBias * 0.55f)
                    tag to score
                }
                .sortedByDescending { it.second }
                .take(2)
                .map { it.first }
                .toList()
            entry.secondaryTags = scoredSecondaries
        }

        val groupedMembers = anchor.members
            .groupBy { listOf(anchor.tag) + it.secondaryTags }

        val subclusters = groupedMembers.entries
            .map { (tags, members) ->
                val radiusPx = packedClusterRadiusPx(
                    count = members.size,
                    collisionDistancePx = loadedThumbCollisionPx
                )
                Subcluster(
                    anchorTag = anchor.tag,
                    tags = tags,
                    members = members.toMutableList(),
                    radiusPx = radiusPx
                )
            }
            .sortedByDescending { it.members.size }
            .toMutableList()

        val anchorNode = anchor.node
        fun clusterAngle(cluster: Subcluster): Float {
            val weightedSecondaryNodes = cluster.tags
                .drop(1)
                .mapNotNull { usableTagNodes[it] }
            if (weightedSecondaryNodes.isEmpty()) {
                return ((Math.PI * 2.0) * hashUnit(cluster.tags.joinToString("|"))).toFloat()
            }
            val avgHeatX = weightedSecondaryNodes.map { it.heatX }.average().toFloat()
            val avgHeatY = weightedSecondaryNodes.map { it.heatY }.average().toFloat()
            val dx = avgHeatX - anchorNode.heatX
            val dy = avgHeatY - anchorNode.heatY
            return if ((dx * dx) + (dy * dy) > 0.000001f) {
                Math.atan2(dy.toDouble(), dx.toDouble()).toFloat()
            } else {
                ((Math.PI * 2.0) * hashUnit(cluster.tags.joinToString("|"))).toFloat()
            }
        }

        fun familyGapPx(left: Subcluster, right: Subcluster): Float {
            val similarity = familySimilarity(left.tags, right.tags)
            val samePrimary = left.tags.firstOrNull() == right.tags.firstOrNull()
            return when {
                similarity < 0.12f -> loadedThumbCollisionPx * if (samePrimary) 1.34f else 1.16f
                similarity < 0.28f -> loadedThumbCollisionPx * if (samePrimary) 1.14f else 0.98f
                similarity < 0.48f -> loadedThumbCollisionPx * if (samePrimary) 0.90f else 0.76f
                else -> loadedThumbCollisionPx * if (samePrimary) 0.66f else 0.56f
            }
        }

        val coreClusters = subclusters.filter { it.tags.size <= 1 }
        val branchClusters = subclusters
            .filter { it.tags.size > 1 }
            .sortedBy { clusterAngle(it) }

        if (coreClusters.isNotEmpty()) {
            coreClusters.first().preferredLocalX = 0f
            coreClusters.first().preferredLocalY = 0f
            coreClusters.first().localX = 0f
            coreClusters.first().localY = 0f
            coreClusters.drop(1).forEachIndexed { index, cluster ->
                val angle = ((Math.PI * 2.0) * hashUnit("${anchor.tag}|core|$index")).toFloat()
                val orbitDistancePx = coreClusters.first().radiusPx + cluster.radiusPx + (loadedThumbCollisionPx * 0.44f)
                cluster.preferredLocalX = cos(angle) * orbitDistancePx
                cluster.preferredLocalY = sin(angle) * orbitDistancePx
                cluster.localX = cluster.preferredLocalX
                cluster.localY = cluster.preferredLocalY
            }
        }

        data class RingSlot(
            val radiusPx: Float,
            val members: MutableList<Subcluster> = mutableListOf()
        )

        val placedClusters = coreClusters.toMutableList()
        val rings = mutableListOf<RingSlot>()
        val coreEnvelopeRadius = coreClusters.maxOfOrNull { cluster ->
            sqrt((cluster.localX * cluster.localX) + (cluster.localY * cluster.localY)) + cluster.radiusPx
        } ?: 0f
        var nextRingRadius = (coreEnvelopeRadius + loadedThumbCollisionPx * 1.42f).coerceAtLeast(spacingPx * 2.15f)

        fun canPlaceCluster(cluster: Subcluster, x: Float, y: Float): Boolean {
            return placedClusters.all { other ->
                val dx = x - other.localX
                val dy = y - other.localY
                val distance = sqrt((dx * dx) + (dy * dy))
                distance >= (cluster.radiusPx + other.radiusPx + familyGapPx(cluster, other))
            }
        }

        branchClusters.forEach { cluster ->
            val desiredAngle = clusterAngle(cluster)
            var placed = false
            val angleStep = ((Math.PI * 2.0) / max(18, branchClusters.size * 4)).toFloat()
            for (ring in rings) {
                for (attempt in 0..18) {
                    val offsetMultiplier = (attempt + 1) / 2
                    val offsetSign = if (attempt % 2 == 0) 1f else -1f
                    val candidateAngle = if (attempt == 0) {
                        desiredAngle
                    } else {
                        desiredAngle + (angleStep * offsetMultiplier * offsetSign)
                    }
                    val candidateX = cos(candidateAngle) * ring.radiusPx
                    val candidateY = sin(candidateAngle) * ring.radiusPx
                    if (!canPlaceCluster(cluster, candidateX, candidateY)) continue
                    cluster.preferredLocalX = candidateX
                    cluster.preferredLocalY = candidateY
                    cluster.localX = candidateX
                    cluster.localY = candidateY
                    ring.members += cluster
                    placedClusters += cluster
                    placed = true
                    break
                }
                if (placed) break
            }
            if (!placed) {
                val ringRadius = max(
                    nextRingRadius,
                    coreEnvelopeRadius + cluster.radiusPx + (loadedThumbCollisionPx * 1.12f)
                )
                val newRing = RingSlot(radiusPx = ringRadius)
                rings += newRing
                val candidateX = cos(desiredAngle) * ringRadius
                val candidateY = sin(desiredAngle) * ringRadius
                cluster.preferredLocalX = candidateX
                cluster.preferredLocalY = candidateY
                cluster.localX = candidateX
                cluster.localY = candidateY
                newRing.members += cluster
                placedClusters += cluster
                nextRingRadius = ringRadius + (cluster.radiusPx * 1.34f) + (loadedThumbCollisionPx * 1.22f)
            }
        }

        repeat(14) {
            var moved = false
            for (leftIndex in 0 until subclusters.size) {
                val left = subclusters[leftIndex]
                for (rightIndex in (leftIndex + 1) until subclusters.size) {
                    val right = subclusters[rightIndex]
                    val dx = right.localX - left.localX
                    val dy = right.localY - left.localY
                    val distance = sqrt((dx * dx) + (dy * dy)).coerceAtLeast(0.001f)
                    val desired = left.radiusPx + right.radiusPx + familyGapPx(left, right)
                    if (distance >= desired) continue
                    val push = (desired - distance) * 0.48f
                    val nx = dx / distance
                    val ny = dy / distance
                    left.localX -= nx * push
                    left.localY -= ny * push
                    right.localX += nx * push
                    right.localY += ny * push
                    moved = true
                }
            }
            subclusters.forEach { cluster ->
                cluster.localX += (cluster.preferredLocalX - cluster.localX) * 0.18f
                cluster.localY += (cluster.preferredLocalY - cluster.localY) * 0.18f
            }
            if (!moved) return@repeat
        }

        anchor.subclusters += subclusters
        anchor.envelopeRadiusPx = subclusters.maxOfOrNull { cluster ->
            sqrt((cluster.localX * cluster.localX) + (cluster.localY * cluster.localY)) + cluster.radiusPx
        }?.coerceAtLeast(spacingPx * 1.2f) ?: (spacingPx * 1.2f)
    }

    val rawAnchorCenters = anchorGroups.map { anchor ->
        anchor.tag to (anchor.node.heatX to anchor.node.heatY)
    }.toMap()
    val minHeatX = rawAnchorCenters.values.minOfOrNull { it.first } ?: 0f
    val maxHeatX = rawAnchorCenters.values.maxOfOrNull { it.first } ?: 1f
    val minHeatY = rawAnchorCenters.values.minOfOrNull { it.second } ?: 0f
    val maxHeatY = rawAnchorCenters.values.maxOfOrNull { it.second } ?: 1f
    val spanHeatX = (maxHeatX - minHeatX).coerceAtLeast(0.0001f)
    val spanHeatY = (maxHeatY - minHeatY).coerceAtLeast(0.0001f)
    val marginX = safeGraphWidthPx * 0.12f
    val marginY = safeGraphHeightPx * 0.10f
    val usableWidthPx = (safeGraphWidthPx - (marginX * 2f)).coerceAtLeast(1f)
    val usableHeightPx = (safeGraphHeightPx - (marginY * 2f)).coerceAtLeast(1f)
    anchorGroups.forEach { anchor ->
        val heatPos = rawAnchorCenters[anchor.tag] ?: (0.5f to 0.5f)
        anchor.desiredX = marginX + (((heatPos.first - minHeatX) / spanHeatX) * usableWidthPx)
        anchor.desiredY = marginY + (((heatPos.second - minHeatY) / spanHeatY) * usableHeightPx)
        anchor.centerX = anchor.desiredX
        anchor.centerY = anchor.desiredY
    }

    repeat(72) {
        for (leftIndex in 0 until anchorGroups.size) {
            val left = anchorGroups[leftIndex]
            for (rightIndex in (leftIndex + 1) until anchorGroups.size) {
                val right = anchorGroups[rightIndex]
                val dx = right.centerX - left.centerX
                val dy = right.centerY - left.centerY
                val distance = sqrt((dx * dx) + (dy * dy)).coerceAtLeast(0.001f)
                val incompatibility = 1f - overlapRatio(left.tag, right.tag)
                val heatDx = left.node.heatX - right.node.heatX
                val heatDy = left.node.heatY - right.node.heatY
                val heatDistance = sqrt((heatDx * heatDx) + (heatDy * heatDy))
                val heatProximity = (1f - (heatDistance / 0.58f)).coerceIn(0f, 1f)
                val desired = left.envelopeRadiusPx +
                    right.envelopeRadiusPx +
                    spacingPx * (0.72f + incompatibility * 0.92f)
                if (distance < desired) {
                    val push = (desired - distance) * 0.48f
                    val nx = dx / distance
                    val ny = dy / distance
                    left.centerX -= nx * push
                    left.centerY -= ny * push
                    right.centerX += nx * push
                    right.centerY += ny * push
                } else if (heatProximity > 0.46f && incompatibility < 0.64f) {
                    val target = left.envelopeRadiusPx +
                        right.envelopeRadiusPx +
                        spacingPx * (0.34f + ((1f - heatProximity) * 0.26f))
                    if (distance > target) {
                        val pull = (distance - target) * (0.026f + (heatProximity * 0.018f))
                        val nx = dx / distance
                        val ny = dy / distance
                        left.centerX += nx * pull
                        left.centerY += ny * pull
                        right.centerX -= nx * pull
                        right.centerY -= ny * pull
                    }
                }
            }
        }
        anchorGroups.forEach { anchor ->
            anchor.centerX += (anchor.desiredX - anchor.centerX) * 0.10f
            anchor.centerY += (anchor.desiredY - anchor.centerY) * 0.10f
        }
    }

    val resolvedNodes = mutableListOf<TagGraphEntryNode>()
    anchorGroups.forEach { anchor ->
        anchor.subclusters.forEach { cluster ->
            val clusterCenterX = anchor.centerX + cluster.localX
            val clusterCenterY = anchor.centerY + cluster.localY
            val rawOffsets = circularOffsets(
                count = cluster.members.size,
                spacingPx = loadedThumbCollisionPx,
                rotationTurns = hashUnit(cluster.tags.joinToString("|"))
            )
            val offsets = refineClusterOffsets(
                offsets = rawOffsets,
                minimumCenterDistancePx = loadedThumbCollisionPx
            )
            val packedRadiusPx = offsets.maxOfOrNull { (dx, dy) ->
                sqrt((dx * dx) + (dy * dy))
            }?.plus(loadedThumbCollisionPx * 0.76f)
                ?.coerceAtLeast(cluster.radiusPx)
                ?: cluster.radiusPx
            cluster.members.forEachIndexed { memberIndex, entry ->
                val offset = offsets.getOrElse(memberIndex) { 0f to 0f }
                val posX = clusterCenterX + offset.first
                val posY = clusterCenterY + offset.second
                resolvedNodes += entry.base.copy(
                    dominantCircleTags = cluster.tags,
                    boundaryCenterX = clusterCenterX / safeGraphWidthPx,
                    boundaryCenterY = clusterCenterY / safeGraphHeightPx,
                    boundaryRadiusPx = packedRadiusPx,
                    x = posX / safeGraphWidthPx,
                    y = posY / safeGraphHeightPx
                )
            }
        }
    }

    if (resolvedNodes.isEmpty()) {
        return TagGraphEntryLayoutResult(
            nodes = emptyList(),
            familyCircles = emptyList()
        )
    }

    val minPosX = resolvedNodes.minOf { (it.x * safeGraphWidthPx) - (loadedThumbCollisionPx * 0.56f) }
    val maxPosX = resolvedNodes.maxOf { (it.x * safeGraphWidthPx) + (loadedThumbCollisionPx * 0.56f) }
    val minPosY = resolvedNodes.minOf { (it.y * safeGraphHeightPx) - (loadedThumbCollisionPx * 0.56f) }
    val maxPosY = resolvedNodes.maxOf { (it.y * safeGraphHeightPx) + (loadedThumbCollisionPx * 0.56f) }
    val currentWidth = (maxPosX - minPosX).coerceAtLeast(1f)
    val currentHeight = (maxPosY - minPosY).coerceAtLeast(1f)
    val targetWidth = safeGraphWidthPx * 0.84f
    val targetHeight = safeGraphHeightPx * 0.87f
    val scale = min(targetWidth / currentWidth, targetHeight / currentHeight).coerceAtMost(1.45f)
    val centerX = (minPosX + maxPosX) * 0.5f
    val centerY = (minPosY + maxPosY) * 0.5f
    val targetCenterX = safeGraphWidthPx * 0.5f
    val targetCenterY = safeGraphHeightPx * 0.5f

    val scaledNodes = resolvedNodes.map { node ->
        val posX = (((node.x * safeGraphWidthPx) - centerX) * scale) + targetCenterX
        val posY = (((node.y * safeGraphHeightPx) - centerY) * scale) + targetCenterY
        val boundaryX = (((node.boundaryCenterX * safeGraphWidthPx) - centerX) * scale) + targetCenterX
        val boundaryY = (((node.boundaryCenterY * safeGraphHeightPx) - centerY) * scale) + targetCenterY
        node.copy(
            x = posX / safeGraphWidthPx,
            y = posY / safeGraphHeightPx,
            boundaryCenterX = boundaryX / safeGraphWidthPx,
            boundaryCenterY = boundaryY / safeGraphHeightPx,
            boundaryRadiusPx = (node.boundaryRadiusPx * scale).coerceAtLeast(spacingPx * 0.56f)
        )
    }

    val finalizedNodes = scaledNodes
        .map { node -> floatArrayOf(node.x, node.y, node.boundaryCenterX, node.boundaryCenterY, node.boundaryRadiusPx) }
        .toMutableList()
    val clusterGroups = scaledNodes.indices.groupBy { index ->
        scaledNodes[index].dominantCircleTags.joinToString("|")
    }
    val minimumNormalizedClusterDistance = ((loadedThumbCollisionPx * 1.02f) / min(safeGraphWidthPx, safeGraphHeightPx))
        .coerceAtLeast(0.010f)

    clusterGroups.values.forEach { indices ->
        if (indices.size <= 1) return@forEach
        repeat(80) {
            var moved = false
            for (leftPos in 0 until indices.size) {
                val leftIndex = indices[leftPos]
                val left = finalizedNodes[leftIndex]
                for (rightPos in (leftPos + 1) until indices.size) {
                    val rightIndex = indices[rightPos]
                    val right = finalizedNodes[rightIndex]
                    val dx = right[0] - left[0]
                    val dy = right[1] - left[1]
                    val distance = sqrt((dx * dx) + (dy * dy)).coerceAtLeast(0.00001f)
                    if (distance >= minimumNormalizedClusterDistance) continue
                    val push = (minimumNormalizedClusterDistance - distance) * 0.52f
                    val nx = dx / distance
                    val ny = dy / distance
                    left[0] -= nx * push
                    left[1] -= ny * push
                    right[0] += nx * push
                    right[1] += ny * push
                    moved = true
                }
            }
            if (!moved) return@repeat
        }

        val centerXNorm = indices.map { finalizedNodes[it][0] }.average().toFloat()
        val centerYNorm = indices.map { finalizedNodes[it][1] }.average().toFloat()
        val solvedRadiusPx = indices.maxOfOrNull { index ->
            val node = finalizedNodes[index]
            val dxPx = (node[0] - centerXNorm) * safeGraphWidthPx
            val dyPx = (node[1] - centerYNorm) * safeGraphHeightPx
            sqrt((dxPx * dxPx) + (dyPx * dyPx)) + (loadedThumbCollisionPx * 0.56f)
        }?.coerceAtLeast(loadedThumbCollisionPx * 0.72f)
            ?: (loadedThumbCollisionPx * 0.72f)

        indices.forEach { index ->
            finalizedNodes[index][2] = centerXNorm
            finalizedNodes[index][3] = centerYNorm
            finalizedNodes[index][4] = solvedRadiusPx
        }
    }

    val outputNodes = scaledNodes.mapIndexed { index, node ->
        val solved = finalizedNodes[index]
        node.copy(
            x = solved[0],
            y = solved[1],
            boundaryCenterX = solved[2],
            boundaryCenterY = solved[3],
            boundaryRadiusPx = solved[4]
        )
    }

    val adjustedOutputNodes = outputNodes
        .map { node -> floatArrayOf(node.x, node.y, node.boundaryCenterX, node.boundaryCenterY, node.boundaryRadiusPx) }
        .toMutableList()
    val familyGroups = outputNodes.indices.groupBy { index ->
        outputNodes[index].dominantCircleTags.joinToString("|")
    }

    fun familyTagOverlap(left: List<String>, right: List<String>): Float {
        if (left.isEmpty() || right.isEmpty()) return 0f
        val leftSet = left.toSet()
        val rightSet = right.toSet()
        val shared = leftSet.count { it in rightSet }
        val union = (leftSet + rightSet).size.coerceAtLeast(1)
        return (shared.toFloat() / union.toFloat()).coerceIn(0f, 1f)
    }

    repeat(36) {
        var movedAny = false
        val groupEntries = familyGroups.entries.toList()
        for (leftIndex in 0 until groupEntries.size) {
            val (leftKey, leftMembers) = groupEntries[leftIndex]
            val leftTags = if (leftKey.isBlank()) emptyList() else leftKey.split("|")
            val leftCenterX = leftMembers.map { adjustedOutputNodes[it][2] }.average().toFloat()
            val leftCenterY = leftMembers.map { adjustedOutputNodes[it][3] }.average().toFloat()
            val leftRadiusPx = leftMembers.maxOfOrNull { adjustedOutputNodes[it][4] } ?: 0f
            for (rightIndex in (leftIndex + 1) until groupEntries.size) {
                val (rightKey, rightMembers) = groupEntries[rightIndex]
                val rightTags = if (rightKey.isBlank()) emptyList() else rightKey.split("|")
                val rightCenterX = rightMembers.map { adjustedOutputNodes[it][2] }.average().toFloat()
                val rightCenterY = rightMembers.map { adjustedOutputNodes[it][3] }.average().toFloat()
                val rightRadiusPx = rightMembers.maxOfOrNull { adjustedOutputNodes[it][4] } ?: 0f
                val dxNorm = rightCenterX - leftCenterX
                val dyNorm = rightCenterY - leftCenterY
                val dxPx = dxNorm * safeGraphWidthPx
                val dyPx = dyNorm * safeGraphHeightPx
                val distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx)).coerceAtLeast(0.0001f)
                val overlapRatio = familyTagOverlap(leftTags, rightTags)
                val samePrimary = leftTags.firstOrNull().orEmpty().isNotBlank() &&
                    leftTags.firstOrNull() == rightTags.firstOrNull()
                val minFamilySize = min(leftMembers.size, rightMembers.size)
                val smallFamilyBoost = when {
                    minFamilySize <= 8 -> 1.20f
                    minFamilySize <= 16 -> 1.10f
                    else -> 1f
                }
                val samePrimaryBoost = if (samePrimary && minFamilySize <= 20) 1.14f else 1f
                val samePrimaryMoatPx = when {
                    minFamilySize <= 8 -> loadedThumbCollisionPx * 1.12f
                    minFamilySize <= 16 -> loadedThumbCollisionPx * 0.92f
                    else -> loadedThumbCollisionPx * 0.72f
                }
                val desiredDistancePx = when {
                    overlapRatio <= 0.001f ->
                        (leftRadiusPx + rightRadiusPx + (loadedThumbCollisionPx * 0.92f)) *
                            smallFamilyBoost *
                            samePrimaryBoost +
                            if (samePrimary) samePrimaryMoatPx else 0f
                    overlapRatio < 0.34f ->
                        (leftRadiusPx + rightRadiusPx + (loadedThumbCollisionPx * 0.46f)) *
                            smallFamilyBoost *
                            samePrimaryBoost +
                            if (samePrimary) samePrimaryMoatPx else 0f
                    samePrimary ->
                        (leftRadiusPx + rightRadiusPx + (loadedThumbCollisionPx * 0.22f)) *
                            smallFamilyBoost *
                            samePrimaryBoost +
                            samePrimaryMoatPx
                    else ->
                        (leftRadiusPx + rightRadiusPx + (loadedThumbCollisionPx * 0.08f)) *
                            smallFamilyBoost
                }
                if (distancePx >= desiredDistancePx) continue
                val pushPx = (desiredDistancePx - distancePx) * 0.48f
                val nxPx = dxPx / distancePx
                val nyPx = dyPx / distancePx
                val pushXNorm = (nxPx * pushPx) / safeGraphWidthPx
                val pushYNorm = (nyPx * pushPx) / safeGraphHeightPx
                leftMembers.forEach { memberIndex ->
                    adjustedOutputNodes[memberIndex][0] =
                        adjustedOutputNodes[memberIndex][0] - pushXNorm
                    adjustedOutputNodes[memberIndex][1] =
                        adjustedOutputNodes[memberIndex][1] - pushYNorm
                    adjustedOutputNodes[memberIndex][2] =
                        adjustedOutputNodes[memberIndex][2] - pushXNorm
                    adjustedOutputNodes[memberIndex][3] =
                        adjustedOutputNodes[memberIndex][3] - pushYNorm
                }
                rightMembers.forEach { memberIndex ->
                    adjustedOutputNodes[memberIndex][0] =
                        adjustedOutputNodes[memberIndex][0] + pushXNorm
                    adjustedOutputNodes[memberIndex][1] =
                        adjustedOutputNodes[memberIndex][1] + pushYNorm
                    adjustedOutputNodes[memberIndex][2] =
                        adjustedOutputNodes[memberIndex][2] + pushXNorm
                    adjustedOutputNodes[memberIndex][3] =
                        adjustedOutputNodes[memberIndex][3] + pushYNorm
                }
                movedAny = true
            }
        }
        if (!movedAny) return@repeat
    }

    val nodeDominantTags = outputNodes.map { it.dominantCircleTags }
    repeat(44) {
        var movedAny = false
        for (leftIndex in adjustedOutputNodes.indices) {
            val left = adjustedOutputNodes[leftIndex]
            val leftTags = nodeDominantTags[leftIndex]
            for (rightIndex in (leftIndex + 1) until adjustedOutputNodes.size) {
                val right = adjustedOutputNodes[rightIndex]
                val rightTags = nodeDominantTags[rightIndex]
                val dx = right[0] - left[0]
                val dy = right[1] - left[1]
                val distance = sqrt((dx * dx) + (dy * dy)).coerceAtLeast(0.00001f)
                val overlapRatio = familyTagOverlap(leftTags, rightTags)
                val desiredDistanceNorm = (loadedThumbCollisionPx / min(safeGraphWidthPx, safeGraphHeightPx)) * when {
                    overlapRatio <= 0.001f -> 1.14f
                    overlapRatio < 0.34f -> 1.08f
                    else -> 1.00f
                }
                if (distance >= desiredDistanceNorm) continue
                val push = (desiredDistanceNorm - distance) * 0.48f
                val nx = dx / distance
                val ny = dy / distance
                left[0] -= nx * push
                left[1] -= ny * push
                right[0] += nx * push
                right[1] += ny * push
                movedAny = true
            }
        }
        if (!movedAny) return@repeat
    }

    familyGroups.values.forEach { indices ->
        val centerXNorm = indices.map { adjustedOutputNodes[it][0] }.average().toFloat()
        val centerYNorm = indices.map { adjustedOutputNodes[it][1] }.average().toFloat()
        val solvedRadiusPx = indices.maxOfOrNull { index ->
            val node = adjustedOutputNodes[index]
            val dxPx = (node[0] - centerXNorm) * safeGraphWidthPx
            val dyPx = (node[1] - centerYNorm) * safeGraphHeightPx
            sqrt((dxPx * dxPx) + (dyPx * dyPx)) + (loadedThumbCollisionPx * 0.56f)
        }?.coerceAtLeast(loadedThumbCollisionPx * 0.72f)
            ?: (loadedThumbCollisionPx * 0.72f)
        indices.forEach { index ->
            adjustedOutputNodes[index][2] = centerXNorm
            adjustedOutputNodes[index][3] = centerYNorm
            adjustedOutputNodes[index][4] = solvedRadiusPx
        }
    }

    val thumbMarginNorm = ((loadedThumbCollisionPx * 0.58f) / min(safeGraphWidthPx, safeGraphHeightPx)).coerceAtLeast(0.008f)
    val boundsMinX = adjustedOutputNodes.minOfOrNull { it[0] - thumbMarginNorm } ?: 0f
    val boundsMaxX = adjustedOutputNodes.maxOfOrNull { it[0] + thumbMarginNorm } ?: 1f
    val boundsMinY = adjustedOutputNodes.minOfOrNull { it[1] - thumbMarginNorm } ?: 0f
    val boundsMaxY = adjustedOutputNodes.maxOfOrNull { it[1] + thumbMarginNorm } ?: 1f
    val graphMarginMin = 0.015f
    val graphMarginMax = 0.985f
    val availableWidth = (graphMarginMax - graphMarginMin).coerceAtLeast(0.01f)
    val availableHeight = (graphMarginMax - graphMarginMin).coerceAtLeast(0.01f)
    val adjustedWidthNorm = (boundsMaxX - boundsMinX).coerceAtLeast(0.0001f)
    val adjustedHeightNorm = (boundsMaxY - boundsMinY).coerceAtLeast(0.0001f)
    val fitScale = min(1f, min(availableWidth / adjustedWidthNorm, availableHeight / adjustedHeightNorm))
    val adjustedCenterX = (boundsMinX + boundsMaxX) * 0.5f
    val adjustedCenterY = (boundsMinY + boundsMaxY) * 0.5f
    val targetCenterNormX = (graphMarginMin + graphMarginMax) * 0.5f
    val targetCenterNormY = (graphMarginMin + graphMarginMax) * 0.5f
    if (fitScale < 0.999f || adjustedCenterX != targetCenterNormX || adjustedCenterY != targetCenterNormY) {
        adjustedOutputNodes.forEach { node ->
            node[0] = ((node[0] - adjustedCenterX) * fitScale) + targetCenterNormX
            node[1] = ((node[1] - adjustedCenterY) * fitScale) + targetCenterNormY
            node[2] = ((node[2] - adjustedCenterX) * fitScale) + targetCenterNormX
            node[3] = ((node[3] - adjustedCenterY) * fitScale) + targetCenterNormY
            node[4] *= fitScale
        }
    }

    val postFitDesiredDistanceNorm =
        ((minimumVisualSpacingPx * 1.04f) / min(safeGraphWidthPx, safeGraphHeightPx)).coerceAtLeast(0.010f)
    familyGroups.values.forEach { indices ->
        if (indices.size <= 1) return@forEach
        val centerXNorm = indices.map { adjustedOutputNodes[it][0] }.average().toFloat()
        val centerYNorm = indices.map { adjustedOutputNodes[it][1] }.average().toFloat()
        repeat(48) {
            var movedAny = false
            for (leftPos in 0 until indices.size) {
                val leftIndex = indices[leftPos]
                val left = adjustedOutputNodes[leftIndex]
                for (rightPos in (leftPos + 1) until indices.size) {
                    val rightIndex = indices[rightPos]
                    val right = adjustedOutputNodes[rightIndex]
                    val dx = right[0] - left[0]
                    val dy = right[1] - left[1]
                    val distance = sqrt((dx * dx) + (dy * dy)).coerceAtLeast(0.00001f)
                    if (distance >= postFitDesiredDistanceNorm) continue
                    val push = (postFitDesiredDistanceNorm - distance) * 0.44f
                    val nx = dx / distance
                    val ny = dy / distance
                    left[0] -= nx * push
                    left[1] -= ny * push
                    right[0] += nx * push
                    right[1] += ny * push
                    movedAny = true
                }
            }
            if (!movedAny) return@repeat
        }
        indices.forEach { index ->
            adjustedOutputNodes[index][2] = centerXNorm
            adjustedOutputNodes[index][3] = centerYNorm
        }
    }

    val overlapBasePositions = adjustedOutputNodes.map { it[0] to it[1] }
    val finalEntryOverlapDistancePx = loadedThumbCollisionPx * 0.96f
    repeat(18) {
        var movedAny = false
        for (leftIndex in adjustedOutputNodes.indices) {
            val left = adjustedOutputNodes[leftIndex]
            for (rightIndex in (leftIndex + 1) until adjustedOutputNodes.size) {
                val right = adjustedOutputNodes[rightIndex]
                var dxPx = (right[0] - left[0]) * safeGraphWidthPx
                var dyPx = (right[1] - left[1]) * safeGraphHeightPx
                var distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx)).coerceAtLeast(0.0001f)
                if (distancePx >= finalEntryOverlapDistancePx) continue
                if (distancePx < 0.01f) {
                    val jitter = deterministicUnitJitter(leftIndex xor rightIndex)
                    dxPx = jitter.first.coerceAtLeast(0.0001f)
                    dyPx = jitter.second.coerceAtLeast(0.0001f)
                    distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx)).coerceAtLeast(0.0001f)
                }
                val nx = dxPx / distancePx
                val ny = dyPx / distancePx
                val pushPx = (finalEntryOverlapDistancePx - distancePx) * 0.24f
                val pushXNorm = (nx * pushPx) / safeGraphWidthPx
                val pushYNorm = (ny * pushPx) / safeGraphHeightPx
                left[0] -= pushXNorm
                left[1] -= pushYNorm
                right[0] += pushXNorm
                right[1] += pushYNorm
                movedAny = true
            }
        }
        adjustedOutputNodes.forEachIndexed { index, node ->
            val base = overlapBasePositions[index]
            node[0] += (base.first - node[0]) * 0.08f
            node[1] += (base.second - node[1]) * 0.08f
        }
        if (!movedAny) return@repeat
    }

    familyGroups.values.forEach { indices ->
        val centerXNorm = indices.map { adjustedOutputNodes[it][0] }.average().toFloat()
        val centerYNorm = indices.map { adjustedOutputNodes[it][1] }.average().toFloat()
        val solvedRadiusPx = indices.maxOfOrNull { index ->
            val node = adjustedOutputNodes[index]
            val dxPx = (node[0] - centerXNorm) * safeGraphWidthPx
            val dyPx = (node[1] - centerYNorm) * safeGraphHeightPx
            sqrt((dxPx * dxPx) + (dyPx * dyPx)) + (loadedThumbCollisionPx * 0.56f)
        }?.coerceAtLeast(loadedThumbCollisionPx * 0.72f)
            ?: (loadedThumbCollisionPx * 0.72f)
        indices.forEach { index ->
            adjustedOutputNodes[index][2] = centerXNorm
            adjustedOutputNodes[index][3] = centerYNorm
            adjustedOutputNodes[index][4] = solvedRadiusPx
        }
    }

    val finalBoundsMinX = adjustedOutputNodes.minOfOrNull { it[0] - thumbMarginNorm } ?: 0f
    val finalBoundsMaxX = adjustedOutputNodes.maxOfOrNull { it[0] + thumbMarginNorm } ?: 1f
    val finalBoundsMinY = adjustedOutputNodes.minOfOrNull { it[1] - thumbMarginNorm } ?: 0f
    val finalBoundsMaxY = adjustedOutputNodes.maxOfOrNull { it[1] + thumbMarginNorm } ?: 1f
    val finalWidthNorm = (finalBoundsMaxX - finalBoundsMinX).coerceAtLeast(0.0001f)
    val finalHeightNorm = (finalBoundsMaxY - finalBoundsMinY).coerceAtLeast(0.0001f)
    val finalFitScale = min(1f, min(availableWidth / finalWidthNorm, availableHeight / finalHeightNorm))
    val finalCenterX = (finalBoundsMinX + finalBoundsMaxX) * 0.5f
    val finalCenterY = (finalBoundsMinY + finalBoundsMaxY) * 0.5f
    if (finalFitScale < 0.999f || finalCenterX != targetCenterNormX || finalCenterY != targetCenterNormY) {
        adjustedOutputNodes.forEach { node ->
            node[0] = ((node[0] - finalCenterX) * finalFitScale) + targetCenterNormX
            node[1] = ((node[1] - finalCenterY) * finalFitScale) + targetCenterNormY
            node[2] = ((node[2] - finalCenterX) * finalFitScale) + targetCenterNormX
            node[3] = ((node[3] - finalCenterY) * finalFitScale) + targetCenterNormY
            node[4] *= finalFitScale
        }
    }

    repeat(20) {
        var movedAny = false
        val groupEntries = familyGroups.entries.toList()
        for (leftIndex in 0 until groupEntries.size) {
            val (leftKey, leftMembers) = groupEntries[leftIndex]
            val leftTags = if (leftKey.isBlank()) emptyList() else leftKey.split("|")
            val leftCenterX = leftMembers.map { adjustedOutputNodes[it][2] }.average().toFloat()
            val leftCenterY = leftMembers.map { adjustedOutputNodes[it][3] }.average().toFloat()
            val leftRadiusPx = leftMembers.maxOfOrNull { adjustedOutputNodes[it][4] } ?: 0f
            for (rightIndex in (leftIndex + 1) until groupEntries.size) {
                val (rightKey, rightMembers) = groupEntries[rightIndex]
                val rightTags = if (rightKey.isBlank()) emptyList() else rightKey.split("|")
                val rightCenterX = rightMembers.map { adjustedOutputNodes[it][2] }.average().toFloat()
                val rightCenterY = rightMembers.map { adjustedOutputNodes[it][3] }.average().toFloat()
                val rightRadiusPx = rightMembers.maxOfOrNull { adjustedOutputNodes[it][4] } ?: 0f
                val dxNorm = rightCenterX - leftCenterX
                val dyNorm = rightCenterY - leftCenterY
                val dxPx = dxNorm * safeGraphWidthPx
                val dyPx = dyNorm * safeGraphHeightPx
                val distancePx = sqrt((dxPx * dxPx) + (dyPx * dyPx)).coerceAtLeast(0.0001f)
                val overlapRatio = familyTagOverlap(leftTags, rightTags)
                val samePrimary = leftTags.firstOrNull().orEmpty().isNotBlank() &&
                    leftTags.firstOrNull() == rightTags.firstOrNull()
                val samePrimaryMoatPx = when {
                    min(leftMembers.size, rightMembers.size) <= 8 -> loadedThumbCollisionPx * 0.96f
                    min(leftMembers.size, rightMembers.size) <= 16 -> loadedThumbCollisionPx * 0.78f
                    else -> loadedThumbCollisionPx * 0.60f
                }
                val desiredDistancePx = when {
                    overlapRatio <= 0.001f ->
                        leftRadiusPx + rightRadiusPx + (loadedThumbCollisionPx * 0.78f) +
                            if (samePrimary) samePrimaryMoatPx else 0f
                    overlapRatio < 0.34f ->
                        leftRadiusPx + rightRadiusPx + (loadedThumbCollisionPx * 0.42f) +
                            if (samePrimary) samePrimaryMoatPx else 0f
                    samePrimary ->
                        leftRadiusPx + rightRadiusPx + (loadedThumbCollisionPx * 0.18f) + samePrimaryMoatPx
                    else ->
                        leftRadiusPx + rightRadiusPx + (loadedThumbCollisionPx * 0.18f)
                }
                if (distancePx >= desiredDistancePx) continue
                val pushPx = (desiredDistancePx - distancePx) * 0.28f
                val nxPx = dxPx / distancePx
                val nyPx = dyPx / distancePx
                val pushXNorm = (nxPx * pushPx) / safeGraphWidthPx
                val pushYNorm = (nyPx * pushPx) / safeGraphHeightPx
                leftMembers.forEach { memberIndex ->
                    adjustedOutputNodes[memberIndex][0] -= pushXNorm
                    adjustedOutputNodes[memberIndex][1] -= pushYNorm
                    adjustedOutputNodes[memberIndex][2] -= pushXNorm
                    adjustedOutputNodes[memberIndex][3] -= pushYNorm
                }
                rightMembers.forEach { memberIndex ->
                    adjustedOutputNodes[memberIndex][0] += pushXNorm
                    adjustedOutputNodes[memberIndex][1] += pushYNorm
                    adjustedOutputNodes[memberIndex][2] += pushXNorm
                    adjustedOutputNodes[memberIndex][3] += pushYNorm
                }
                movedAny = true
            }
        }
        if (!movedAny) return@repeat
    }

    val finalFamilyKeys = outputNodes.map { it.dominantCircleTags.joinToString("|") }
    val finalCrossFamilyDistanceNorm =
        ((loadedThumbCollisionPx * 0.98f) / min(safeGraphWidthPx, safeGraphHeightPx)).coerceAtLeast(0.0105f)
    repeat(18) {
        var movedAny = false
        for (leftIndex in adjustedOutputNodes.indices) {
            val left = adjustedOutputNodes[leftIndex]
            val leftFamilyKey = finalFamilyKeys[leftIndex]
            for (rightIndex in (leftIndex + 1) until adjustedOutputNodes.size) {
                if (leftFamilyKey == finalFamilyKeys[rightIndex]) continue
                val right = adjustedOutputNodes[rightIndex]
                val dx = right[0] - left[0]
                val dy = right[1] - left[1]
                val distance = sqrt((dx * dx) + (dy * dy)).coerceAtLeast(0.00001f)
                if (distance >= finalCrossFamilyDistanceNorm) continue
                val push = (finalCrossFamilyDistanceNorm - distance) * 0.24f
                val nx = dx / distance
                val ny = dy / distance
                left[0] -= nx * push
                left[1] -= ny * push
                right[0] += nx * push
                right[1] += ny * push
                movedAny = true
            }
        }
        if (!movedAny) return@repeat
    }

    familyGroups.values.forEach { indices ->
        val centerXNorm = indices.map { adjustedOutputNodes[it][0] }.average().toFloat()
        val centerYNorm = indices.map { adjustedOutputNodes[it][1] }.average().toFloat()
        val solvedRadiusPx = indices.maxOfOrNull { index ->
            val node = adjustedOutputNodes[index]
            val dxPx = (node[0] - centerXNorm) * safeGraphWidthPx
            val dyPx = (node[1] - centerYNorm) * safeGraphHeightPx
            sqrt((dxPx * dxPx) + (dyPx * dyPx)) + (loadedThumbCollisionPx * 0.56f)
        }?.coerceAtLeast(loadedThumbCollisionPx * 0.72f)
            ?: (loadedThumbCollisionPx * 0.72f)
        indices.forEach { index ->
            adjustedOutputNodes[index][2] = centerXNorm
            adjustedOutputNodes[index][3] = centerYNorm
            adjustedOutputNodes[index][4] = solvedRadiusPx
        }
    }

    val finalOutputNodes = outputNodes.mapIndexed { index, node ->
        val solved = adjustedOutputNodes[index]
        node.copy(
            x = solved[0],
            y = solved[1],
            boundaryCenterX = solved[2],
            boundaryCenterY = solved[3],
            boundaryRadiusPx = solved[4]
        )
    }

    return buildLegacyEntryLayoutResult(
        nodes = finalOutputNodes,
        graphWidthPx = graphWidthPx,
        graphHeightPx = graphHeightPx,
        minimumVisualSpacingPx = minimumVisualSpacingPx
    )
}

private fun squashGraphScore(value: Float, softness: Float): Float {
    val safeSoftness = softness.coerceAtLeast(0.05f)
    return (value / (abs(value) + safeSoftness)).coerceIn(-1f, 1f)
}

private fun computeHeatmapLayout(seeds: List<TagGraphSeed>): Map<String, Pair<Float, Float>> {
    if (seeds.isEmpty()) return emptyMap()
    if (seeds.size == 1) return mapOf(seeds.first().normalizedName to (0.5f to 0.5f))
    val graphData = buildHeatmapGraphData(seeds)
    val communityLayouts = detectHeatmapCommunities(graphData)
        .map { members -> layoutHeatmapCommunityLocally(graphData, members) }
    val communityCenters = computeHeatmapCommunityCenters(graphData, communityLayouts)

    val rawPositions = mutableMapOf<String, Pair<Float, Float>>()
    communityLayouts.forEachIndexed { communityIndex, community ->
        val center = communityCenters[communityIndex] ?: (0f to 0f)
        community.localPositions.forEach { (memberIndex, localPosition) ->
            val seed = graphData.sortedSeeds[memberIndex]
            rawPositions[seed.normalizedName] = (
                center.first + localPosition.first to
                    center.second + localPosition.second
                )
        }
    }

    val minX = rawPositions.values.minOfOrNull { it.first } ?: -1f
    val maxX = rawPositions.values.maxOfOrNull { it.first } ?: 1f
    val minY = rawPositions.values.minOfOrNull { it.second } ?: -1f
    val maxY = rawPositions.values.maxOfOrNull { it.second } ?: 1f
    val centerX = (minX + maxX) * 0.5f
    val centerY = (minY + maxY) * 0.5f
    val spanX = (maxX - minX).coerceAtLeast(0.0001f)
    val spanY = (maxY - minY).coerceAtLeast(0.0001f)
    val sharedScale = 0.80f / max(spanX, spanY)

    return rawPositions.mapValues { (_, pos) ->
        val normalizedX = (0.5f + ((pos.first - centerX) * sharedScale)).coerceIn(0.08f, 0.92f)
        val normalizedY = (0.5f + ((pos.second - centerY) * sharedScale)).coerceIn(0.08f, 0.92f)
        normalizedX to normalizedY
    }
}
}
