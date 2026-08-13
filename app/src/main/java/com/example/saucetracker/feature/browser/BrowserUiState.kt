package com.example.saucetracker.feature.browser

import com.example.saucetracker.*
import com.example.saucetracker.data.backup.*
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
import com.example.saucetracker.core.diagnostics.PerformanceMetrics
import com.example.saucetracker.core.media.computeDHash64
import com.example.saucetracker.core.network.HttpClientFactory
import com.example.saucetracker.core.network.HttpClientProfile
import com.example.saucetracker.core.preferences.KEY_BROWSER_DUPLICATE_CHECK_MODE
import com.example.saucetracker.core.preferences.KEY_PERFORMANCE_OVERLAY_ENABLED
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
internal data class BrowserGallerySummary(
    val code: Int,
    val title: String,
    val subtitle: String,
    val mediaId: Long,
    val coverExt: String,
    val numPages: Int,
    val uploadDate: String
)

internal data class BrowserPageThumb(
    val pageNumber: Int,
    val thumbnailUrls: List<String>
)

internal data class BrowserGalleryComment(
    val author: String,
    val text: String
)

internal data class BrowserGalleryDetail(
    val summary: BrowserGallerySummary,
    val tagsByType: Map<String, List<String>>,
    val tagCountsByKey: Map<String, Int>,
    val pageThumbs: List<BrowserPageThumb>,
    val comments: List<BrowserGalleryComment>,
    val relatedGalleries: List<BrowserGallerySummary> = emptyList()
)

internal data class BrowserDuplicateComparisonState(
    val row: BrowserGallerySummary,
    val hint: DuplicateHint
)

internal data class BrowserSearchPage(
    val results: List<BrowserGallerySummary>,
    val page: Int,
    val hasMore: Boolean
)

internal data class BrowserCreatorRef(
    val type: String,
    val name: String,
    val slug: String
)

internal data class BrowserNavSnapshot(
    val searchInput: String,
    val activeSearchTerm: String,
    val rows: List<BrowserGallerySummary>,
    val selectedDetail: BrowserGalleryDetail?,
    val currentPage: Int,
    val hasMorePages: Boolean,
    val activeCreator: BrowserCreatorRef?,
    val sortMode: BrowserSearchSortMode,
    val listRouteKey: String,
    val detailRouteKey: String,
    val listFirstVisibleItemIndex: Int,
    val listFirstVisibleItemScrollOffset: Int,
    val detailFirstVisibleItemIndex: Int,
    val detailFirstVisibleItemScrollOffset: Int
)

internal data class BrowserDuplicateSeedIndex(
    val allSeeds: List<LocalDuplicateSeed>,
    val byCode: Map<Int, LocalDuplicateSeed>,
    val byMediaId: Map<Long, List<LocalDuplicateSeed>>,
    val byPageCount: Map<Int, List<LocalDuplicateSeed>>,
    val byUploadDate: Map<String, List<LocalDuplicateSeed>>,
    val byTitleKey: Map<String, List<LocalDuplicateSeed>>,
    val byTitleTrigram: Map<String, List<LocalDuplicateSeed>>
)

internal data class BrowserDuplicateSeedSnapshot(
    val seeds: List<LocalDuplicateSeed>,
    val index: BrowserDuplicateSeedIndex,
    val version: Int
)

internal object BrowserDuplicateSeedIndexCache {
    private var cachedVersion: Int? = null
    private var cachedIndex: BrowserDuplicateSeedIndex? = null

    @Synchronized
    fun snapshot(seeds: List<LocalDuplicateSeed>): BrowserDuplicateSeedSnapshot {
        val version = computeBrowserDuplicateSeedVersion(seeds)
        val index = if (cachedVersion == version) {
            cachedIndex
        } else {
            null
        } ?: buildBrowserDuplicateSeedIndex(seeds).also { built ->
            cachedVersion = version
            cachedIndex = built
        }
        return BrowserDuplicateSeedSnapshot(seeds = seeds, index = index, version = version)
    }
}

internal enum class BrowserSearchSortMode(
    val label: String,
    val searchSortValue: String,
    val creatorPathValue: String
) {
    RECENT(label = "Recent", searchSortValue = "", creatorPathValue = ""),
    POPULAR_TODAY(label = "Today", searchSortValue = "popular-today", creatorPathValue = "popular-today"),
    POPULAR_WEEK(label = "Week", searchSortValue = "popular-week", creatorPathValue = "popular-week"),
    POPULAR_ALL_TIME(label = "All Time", searchSortValue = "popular", creatorPathValue = "popular")
}

internal enum class BrowserPaneTransitionDirection {
    Forward,
    Backward
}

internal fun computeBrowserDuplicateSeedVersion(seeds: List<LocalDuplicateSeed>): Int {
    var result = 17
    seeds.forEach { seed ->
        result = (31 * result) + seed.code
        result = (31 * result) + seed.numPages
        result = (31 * result) + seed.uploadDate.hashCode()
        result = (31 * result) + seed.mediaId.hashCode()
        result = (31 * result) + seed.titleKey.hashCode()
        result = (31 * result) + seed.creatorKeys.hashCode()
        result = (31 * result) + seed.tagKeys.hashCode()
        result = (31 * result) + seed.artistKeys.hashCode()
        result = (31 * result) + seed.groupKeys.hashCode()
        result = (31 * result) + seed.thumbnailUrl.hashCode()
    }
    return result
}

internal data class BrowserExitRatingPromptState(
    val code: Int,
    val title: String,
    val rating: Int,
    val closeAfter: Boolean
)

internal data class BrowserLocalLibraryState(
    val exists: Boolean,
    val rating: Int,
    val isRead: Boolean,
    val pinned: Boolean
)

internal sealed interface BrowserPendingLibraryAction {
    data class SetRating(val rating: Int) : BrowserPendingLibraryAction
    data class SetRead(val isRead: Boolean) : BrowserPendingLibraryAction
    data class SetPinned(val pinned: Boolean) : BrowserPendingLibraryAction
    data object ToggleRead : BrowserPendingLibraryAction
    data object TogglePinned : BrowserPendingLibraryAction
}

internal data class BrowserPendingImportRequest(
    val code: Int,
    val action: BrowserPendingLibraryAction
)
