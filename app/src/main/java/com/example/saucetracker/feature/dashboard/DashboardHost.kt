package com.example.saucetracker

import com.example.saucetracker.data.backup.*
import com.example.saucetracker.data.downloads.*
import com.example.saucetracker.core.ui.theme.AccentMode
import com.example.saucetracker.core.network.executeWebsiteRequestWithRetry
import com.example.saucetracker.core.network.websiteHttpFailure
import com.example.saucetracker.core.ui.components.*
import com.example.saucetracker.feature.library.entries.EntrySwipeDismissContainer
import com.example.saucetracker.feature.library.entries.dashboardEntriesSection
import com.example.saucetracker.feature.library.privacy.LibraryIncognitoPolicy

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
import androidx.compose.ui.layout.layout
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
import androidx.compose.ui.unit.Constraints
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
import com.example.saucetracker.feature.library.creators.*
import com.example.saucetracker.feature.library.detail.*
import com.example.saucetracker.feature.library.history.*
import com.example.saucetracker.feature.library.tags.*
import com.example.saucetracker.feature.settings.*
import com.example.saucetracker.feature.subscriptions.*
import com.example.saucetracker.feature.suggestions.*
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

internal const val APP_TITLE = "Sauce Tracker"
private const val CUNNY_APP_TITLE = "Cunny Tracker"
private const val SUBSCRIPTION_NOTIFICATION_CHANNEL_ID = "subscription_updates"
private const val SUBSCRIPTION_NOTIFICATION_ID = 32041
private const val SUBSCRIPTION_ROUTE_FETCH_PAGES = 2
private const val EXPORT_PREFIX = "Sauce exported Date"
private const val EXPORT_FORMAT = "NH_TAGBOOK_EXPORT_V1"
private const val DESKTOP_BRIDGE_DEFAULT_PORT = 17366
private const val THUMB_PRELOAD_MIN_PARALLEL = 4
private const val THUMB_PRELOAD_MAX_PARALLEL = 16
private const val THUMB_PRELOAD_TOP_PRIORITY_COUNT = 48
private const val APP_LOCK_GRACE_MS = 30_000L
internal const val INCOGNITO_OVERLAY_ALPHA = 0.82f
internal const val POPULAR_TAG_FETCH_MAX_PAGES = 500
const val EXTRA_BROWSER_IMPORT_INPUT = "extra_browser_import_input"
internal val READ_STATE_COLOR = Color(0xFF22C55E)
internal val UNREAD_STATE_COLOR = Color(0xFFEF4444)


class SuggestionApiClient {
    private val client: OkHttpClient = HttpClientFactory.create(HttpClientProfile.SUGGESTIONS)

    fun searchCodes(query: String, page: Int): List<Int> {
        val safePage = page.coerceAtLeast(1)
        val trimmed = query.trim()
        val url = if (trimmed.isBlank()) {
            if (safePage <= 1) {
                "https://nhentai.net/"
            } else {
                "https://nhentai.net/?page=$safePage"
            }
        } else {
            val encoded = Uri.encode(trimmed)
            "https://nhentai.net/search/?q=$encoded&page=$safePage"
        }
        val html = requestHtml(url)
        val out = linkedSetOf<Int>()
        SEARCH_GALLERY_CODE_PATTERN.findAll(html).forEach { match ->
            val parsed = match.groupValues.getOrNull(1)?.toIntOrNull()
            if (parsed != null && parsed > 0) {
                out += parsed
            }
        }
        return out.toList()
    }

    fun fetchDirectRouteCodes(routeType: String, routeName: String, pages: Int = 1): List<Int> {
        val normalizedType = normalizeSubscriptionRouteType(routeType)
        val normalizedName = normalizeSubscriptionRouteName(normalizedType, routeName)
        if (normalizedType.isBlank() || normalizedName.isBlank()) return emptyList()
        val safePages = pages.coerceIn(1, 5)
        val out = linkedSetOf<Int>()
        repeat(safePages) { offset ->
            val page = offset + 1
            val url = buildSubscriptionRouteUrl(normalizedType, normalizedName, page)
            if (url.isBlank()) return@repeat
            val html = requestHtml(url)
            SEARCH_GALLERY_CODE_PATTERN.findAll(html).forEach { match ->
                val parsed = match.groupValues.getOrNull(1)?.toIntOrNull()
                if (parsed != null && parsed > 0) {
                    out += parsed
                }
            }
        }
        return out.toList()
    }

    private fun requestHtml(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
            )
            .header("Accept", "text/html,application/xhtml+xml,application/xml")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()
        val response = executeWebsiteRequestWithRetry(client, request, "searching the website")
        response.use { rsp ->
            if (!rsp.isSuccessful) {
                throw websiteHttpFailure("searching the website", rsp.code)
            }
            return rsp.body?.string()
                ?: throw GalleryFetchException("The website returned an empty search page.")
        }
    }
}

@Composable
internal fun SauceTrackerContent(vm: com.example.saucetracker.feature.dashboard.DashboardViewModel) {
    com.example.saucetracker.feature.dashboard.DashboardScreen(vm)
}

@Composable
internal fun AppLockOverlay(vm: com.example.saucetracker.feature.dashboard.DashboardViewModel) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    var imeBottomPx by remember { mutableStateOf(0) }
    DisposableEffect(view) {
        val visibleRect = android.graphics.Rect()
        val listener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
            view.getWindowVisibleDisplayFrame(visibleRect)
            val rootHeight = view.rootView.height.coerceAtLeast(0)
            val visibleHeight = (visibleRect.bottom - visibleRect.top).coerceAtLeast(0)
            val diff = (rootHeight - visibleHeight).coerceAtLeast(0)
            imeBottomPx = if (rootHeight > 0 && diff > (rootHeight * 0.15f)) diff else 0
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            if (view.viewTreeObserver.isAlive) {
                view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
            }
        }
    }
    val imeVisible = imeBottomPx > 0
    val imeBottomDp = with(density) { imeBottomPx.toDp() }
    val focusRequester = remember(vm.appLockNonce) { FocusRequester() }
    val biometricManager = remember(context) { BiometricManager.from(context) }
    val biometricAvailable = remember(biometricManager) {
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }
    var pinInput by remember(vm.appLockNonce) { mutableStateOf("") }
    var helperMessage by remember(vm.appLockNonce) { mutableStateOf<String?>(null) }
    var biometricPromptInFlight by remember(vm.appLockNonce) { mutableStateOf(false) }
    var autoPromptedNonce by remember { mutableStateOf<Long?>(null) }

    fun submitPin() {
        val normalized = pinInput.filter { it.isDigit() }.take(20)
        pinInput = normalized
        if (normalized.isBlank()) {
            helperMessage = "Enter PIN."
            return
        }
        val unlocked = vm.tryUnlockWithPin(normalized)
        if (unlocked) {
            keyboardController?.hide()
            helperMessage = null
        } else {
            helperMessage = "Incorrect PIN."
        }
    }

    fun startBiometricPrompt() {
        if (!vm.appLocked || !vm.appLockEnabled || !vm.appLockBiometricEnabled) return
        if (!biometricAvailable || activity == null || biometricPromptInFlight) return

        biometricPromptInFlight = true
        helperMessage = null
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    biometricPromptInFlight = false
                    helperMessage = null
                    vm.unlockAppFromBiometric()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    biometricPromptInFlight = false
                    helperMessage = if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        "Use your PIN to unlock."
                    } else {
                        errString.toString().ifBlank { "Biometric unlock failed." }
                    }
                }

                override fun onAuthenticationFailed() {
                    biometricPromptInFlight = false
                    helperMessage = "Biometric not recognized. Try again or use PIN."
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Sauce Tracker")
            .setSubtitle("Use fingerprint or face unlock")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButtonText("Use PIN")
            .build()

        prompt.authenticate(promptInfo)
    }

    LaunchedEffect(vm.appLockNonce, vm.appLocked, vm.appLockBiometricEnabled, biometricAvailable) {
        if (!vm.appLocked || !vm.appLockEnabled) return@LaunchedEffect
        if (!vm.appLockBiometricEnabled || !biometricAvailable) return@LaunchedEffect
        if (autoPromptedNonce == vm.appLockNonce) return@LaunchedEffect
        autoPromptedNonce = vm.appLockNonce
        startBiometricPrompt()
    }

    LaunchedEffect(vm.appLockNonce, vm.appLocked) {
        if (!vm.appLocked || !vm.appLockEnabled) return@LaunchedEffect
        delay(120)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    BackHandler(enabled = vm.appLocked) {
        // Keep lock screen in place until unlock succeeds.
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {},
            contentAlignment = if (imeVisible) Alignment.BottomCenter else Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
                modifier = if (imeVisible) {
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .padding(bottom = imeBottomDp + 8.dp, top = 12.dp)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 22.dp)
                }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Sauce Tracker Locked",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (biometricAvailable && vm.appLockBiometricEnabled) {
                            "Use fingerprint/face unlock or enter your PIN."
                        } else {
                            "Enter your PIN to unlock."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { raw ->
                            pinInput = raw.filter { it.isDigit() }.take(20)
                            helperMessage = null
                        },
                        label = { Text("PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitPin() },
                            onGo = { submitPin() },
                            onSend = { submitPin() },
                            onSearch = { submitPin() }
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { pinInput = ""; helperMessage = null }) {
                                Text("Clear")
                            }
                            if (vm.incognitoToggleAuthPending) {
                                TextButton(
                                    onClick = {
                                        pinInput = ""
                                        helperMessage = null
                                        keyboardController?.hide()
                                        vm.cancelIncognitoToggleAuth()
                                    }
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                        if (biometricAvailable && vm.appLockBiometricEnabled) {
                            TextButton(onClick = ::startBiometricPrompt) {
                                Text("Use biometric")
                            }
                        }
                    }
                    Text(
                        text = helperMessage ?: "Press Enter/Done on keyboard to unlock with PIN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (helperMessage != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun ApplySystemBars(
    darkContent: Boolean,
    barColor: Int
) {
    val view = LocalView.current
    if (view.isInEditMode) return

    SideEffect {
        val activity = view.context as? Activity ?: return@SideEffect
        val window = activity.window
        window.statusBarColor = barColor
        window.navigationBarColor = barColor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = darkContent
            isAppearanceLightNavigationBars = darkContent
        }
    }
}

@Composable
private fun DashboardBodyReveal(
    revealKey: Any,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    key(revealKey) {
        var entered by remember { mutableStateOf(false) }
        val alpha by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (entered) 1f else 0f,
            animationSpec = tween(durationMillis = 170, easing = FastOutSlowInEasing),
            label = "dashboardBodyReveal"
        )
        LaunchedEffect(Unit) {
            entered = true
        }
        Box(
            modifier = modifier.graphicsLayer(alpha = alpha)
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun DashboardContent(
    vm: com.example.saucetracker.feature.dashboard.DashboardViewModel,
    homeSurface: HomeSurface,
    onHomeSurfaceChange: (HomeSurface) -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRemoveLocalDownloadConfirm by remember { mutableStateOf(false) }
    var showRedownloadLocalConfirm by remember { mutableStateOf(false) }
    var showRefetchConfirm by remember { mutableStateOf(false) }
    var pendingRefetchCode by remember { mutableStateOf<Int?>(null) }
    var showClearAllPrompt by remember { mutableStateOf(false) }
    var showClearUnsafePrompt by remember { mutableStateOf(false) }
    var showRefetchAllPrompt by remember { mutableStateOf(false) }
    var pendingClearAfterExport by remember { mutableStateOf(false) }
    var showSettingsTab by remember { mutableStateOf(false) }
    var settingsDataExpanded by remember { mutableStateOf(false) }
    var settingsDisplayExpanded by remember { mutableStateOf(false) }
    var settingsPersonalizationExpanded by remember { mutableStateOf(false) }
    var settingsSecurityExpanded by remember { mutableStateOf(false) }
    var settingsStatsExpanded by remember { mutableStateOf(false) }
    var selectedStatsRange by remember { mutableStateOf(StatsRange.WEEK) }
    var showTagGraphDialog by remember { mutableStateOf(false) }
    var selectedTagGraphTab by remember { mutableStateOf(TagGraphTab.HEATMAP) }
    var selectedTagHeatmapDisplayMode by remember { mutableStateOf(TagHeatmapDisplayMode.TAGS) }
    var heatmapOverviewCollapsed by remember { mutableStateOf(true) }
    var homeHeatmapDisplayMode by remember { mutableStateOf(TagHeatmapDisplayMode.TAGS) }
    var homeHeatmapSelectionSheetState by remember { mutableStateOf<GraphSelectionSheetState?>(null) }
    var homeHeatmapSelectionSheetHeightFraction by remember(homeHeatmapSelectionSheetState) {
        mutableStateOf(homeHeatmapBaseSheetHeightFraction(homeHeatmapSelectionSheetState))
    }
    var showBlockedTagsManager by remember { mutableStateOf(false) }
    var hiddenSuggestionsDropdownExpanded by remember { mutableStateOf(false) }
    var pendingUnhideSuggestionCode by remember { mutableStateOf<Int?>(null) }
    val suggestedDuplicateComparisonStateHolder = remember {
        mutableStateOf<SuggestedDuplicateComparisonState?>(null)
    }
    var suggestedDuplicateComparisonState by suggestedDuplicateComparisonStateHolder
    var showClearHiddenSuggestionsPrompt by remember { mutableStateOf(false) }
    var showSuggestedWeightsDialog by remember { mutableStateOf(false) }
    var showPersonalizationDialog by remember { mutableStateOf(false) }
    var showBrowserDuplicateModeDialog by remember { mutableStateOf(false) }
    var showEntryModeCycleDialog by remember { mutableStateOf(false) }
    var showCustomizeHomeDialog by remember { mutableStateOf(false) }
    var dashboardVisitNonce by remember { mutableStateOf(0L) }
    var historyStatsRange by remember { mutableStateOf(StatsRange.MONTH) }
    var historySelectedDay by remember { mutableStateOf<DailyActivityPoint?>(null) }
    var historySelectedDayEntries by remember { mutableStateOf<List<DayReadEntryRow>>(emptyList()) }
    var historySelectedDayEntriesLoading by remember { mutableStateOf(false) }
    var personalizationSortTarget by remember { mutableStateOf<PersonalizationSortTarget?>(null) }
    var showEntryLayoutDialog by remember { mutableStateOf(false) }
    var pendingEntryLayoutApplyMode by remember { mutableStateOf<Boolean?>(null) }
    var pendingEntryLayoutApplyColumns by remember { mutableStateOf(2) }
    var showBackupThumbnailArchiveWarning by remember { mutableStateOf(false) }
    var showRecalculateEntryHeatmapWarning by remember { mutableStateOf(false) }
    var showEntryDownloadIntroPrompt by remember { mutableStateOf(false) }
    var showEntryDownloadConfirmPrompt by remember { mutableStateOf(false) }
    var showEntryDownloadBatchConfirmPrompt by remember { mutableStateOf(false) }
    var showOpenDownloadedFolderPrompt by remember { mutableStateOf(false) }
    var selectedSubscriptionEventId by remember { mutableStateOf<Long?>(null) }
    var pendingSubscriptionSettings by remember { mutableStateOf<SubscriptionRow?>(null) }
    var showSubscriptionsListDialog by remember { mutableStateOf(false) }
    var pendingDownloadDetail by remember { mutableStateOf<EntryDetail?>(null) }
    var pendingDownloadChangeFolder by remember { mutableStateOf(false) }
    var skipEntryDownloadIntroChecked by remember { mutableStateOf(false) }
    var thumbnailPreview by remember { mutableStateOf<ThumbnailPreviewState?>(null) }
    val localEntryHoldPopupStateHolder = remember {
        mutableStateOf<LocalEntryHoldPopupState?>(null)
    }
    var localEntryHoldPopupState by localEntryHoldPopupStateHolder
    var selectedActivityPoint by remember { mutableStateOf<DailyActivityPoint?>(null) }
    var showEnableAppLockDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showDisableAppLockPrompt by remember { mutableStateOf(false) }
    var showEnableGalleryNeedsThumbnailsPrompt by remember { mutableStateOf(false) }
    var showDisableThumbnailsForGalleryPrompt by remember { mutableStateOf(false) }
    var searchFieldFocused by remember { mutableStateOf(false) }
    var blockedTagsSearchQuery by remember { mutableStateOf("") }
    var slideshowHorizontalDirection by remember {
        mutableStateOf(loadSlideshowHorizontalDirection(context))
    }
    val configuration = LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val entryItemYByCode = remember { mutableMapOf<Int, Float>() }
    val entryItemXByCode = remember { mutableMapOf<Int, Float>() }
    val entryItemWidthByCode = remember { mutableMapOf<Int, Float>() }
    val entryItemHeightByCode = remember { mutableMapOf<Int, Float>() }
    val creatorLinkYByKey = remember { mutableMapOf<String, Float>() }
    var pendingSelectionAnchor by remember { mutableStateOf<SelectionAnchor?>(null) }
    val screenScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val rootListState = rememberLazyListState()
    val tagsListState = rememberLazyListState()
    val suggestedListState = rememberLazyListState()
    var pendingSuggestedScrollCode by remember { mutableStateOf<Int?>(null) }
    val creatorsListState = rememberLazyListState()
    val subscriptionsListState = rememberLazyListState()
    val homeSurfaceScrollAnchors = remember { mutableStateMapOf<HomeSurface, Pair<Int, Int>>() }

    fun switchHomeSurface(nextSurface: HomeSurface, restoreScroll: Boolean = true) {
        if (homeSurface == nextSurface) return
        homeSurfaceScrollAnchors[homeSurface] =
            rootListState.firstVisibleItemIndex to rootListState.firstVisibleItemScrollOffset
        if (nextSurface == HomeSurface.DASHBOARD) {
            dashboardVisitNonce += 1L
        }
        onHomeSurfaceChange(nextSurface)
        if (restoreScroll) {
            val targetAnchor = homeSurfaceScrollAnchors[nextSurface] ?: (0 to 0)
            screenScope.launch {
                // Let the target surface enter the LazyColumn before restoring its anchor.
                delay(16)
                runCatching {
                    rootListState.scrollToItem(
                        targetAnchor.first.coerceAtLeast(0),
                        targetAnchor.second.coerceAtLeast(0)
                    )
                }
            }
        }
    }

    fun scrollActiveHomeSurfaceToTop(animated: Boolean) {
        screenScope.launch {
            if (animated) {
                rootListState.animateScrollToItem(0)
            } else {
                rootListState.scrollToItem(0)
            }
            when (homeSurface) {
                HomeSurface.TAGS -> {
                    if (animated) tagsListState.animateScrollToItem(0) else tagsListState.scrollToItem(0)
                }
                HomeSurface.SUGGESTED -> {
                    if (animated) suggestedListState.animateScrollToItem(0) else suggestedListState.scrollToItem(0)
                }
                HomeSurface.SUBSCRIPTIONS -> {
                    if (animated) subscriptionsListState.animateScrollToItem(0) else subscriptionsListState.scrollToItem(0)
                }
                HomeSurface.CREATORS -> {
                    if (animated) creatorsListState.animateScrollToItem(0) else creatorsListState.scrollToItem(0)
                }
                else -> Unit
            }
        }
    }

    fun openCreatorFromEntryDetail(creatorType: String, creatorName: String) {
        vm.openCreatorFromDetail(creatorType, creatorName)
        if (!vm.legacyHomeUi) {
            switchHomeSurface(HomeSurface.CREATORS, restoreScroll = false)
        }
    }
    var creatorJumpTransitionActive by remember { mutableStateOf(false) }
    var graphEntryJumpTransitionActive by remember { mutableStateOf(false) }
    var pendingGraphEntryOpenCode by remember { mutableStateOf<Int?>(null) }
    val creatorJumpContentAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = when {
            graphEntryJumpTransitionActive -> 0f
            creatorJumpTransitionActive -> 0.16f
            else -> 1f
        },
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = when {
                graphEntryJumpTransitionActive -> 70
                creatorJumpTransitionActive -> 110
                else -> 220
            },
            easing = if (graphEntryJumpTransitionActive) {
                androidx.compose.animation.core.LinearOutSlowInEasing
            } else {
                androidx.compose.animation.core.FastOutSlowInEasing
            }
        ),
        label = "creatorJumpContentAlpha"
    )
    val graphEntryJumpScrimAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (graphEntryJumpTransitionActive) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = if (graphEntryJumpTransitionActive) 75 else 220,
            easing = androidx.compose.animation.core.LinearOutSlowInEasing
        ),
        label = "graphEntryJumpScrimAlpha"
    )
    val searchEverythingShowingCount = vm.tags.size + vm.creators.size + vm.entries.size
    val tagFilterShowingEntriesCount = vm.entries.size
    val creatorsListMaxHeight = (configuration.screenHeightDp.dp * 0.72f)
        .coerceIn(320.dp, 720.dp)
    val holdPopupScreenWidthDp = configuration.screenWidthDp.dp
    val holdPopupMaxWidthDp = (holdPopupScreenWidthDp - 24.dp).coerceAtLeast(220.dp)
    val holdPopupWidthDp = (holdPopupScreenWidthDp * 0.94f).coerceIn(220.dp, holdPopupMaxWidthDp)
    val holdPopupScreenWidthPx = with(density) { holdPopupScreenWidthDp.toPx() }
    val holdPopupWidthPx = with(density) { holdPopupWidthDp.toPx() }
    val isAnyListScrolling = {
        rootListState.isScrollInProgress ||
            tagsListState.isScrollInProgress ||
            suggestedListState.isScrollInProgress ||
            creatorsListState.isScrollInProgress ||
            subscriptionsListState.isScrollInProgress
    }
    val useReducedScrollThumbnails =
        vm.adaptiveScrollThumbnails && isAnyListScrolling()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            vm.setStatus("Android notification permission was not granted. In-app subscriptions still work.")
        }
    }
    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    PerformanceOverlay(enabled = vm.performanceOverlayEnabled)
    val graphEntryJumpScrimColor = MaterialTheme.colorScheme.background
    val sharedTagGraphEntryLayoutCache = remember { HeatmapLayoutCache(maximumEntries = 8) }
    LaunchedEffect(vm.entryHeatmapCacheNonce) {
        sharedTagGraphEntryLayoutCache.clear()
    }
    val filteredEntryRows = remember(vm.entries) { vm.entries.toList() }
    val popularTagRows = remember(vm.popularTags) { vm.popularTags.toList() }
    val homeHeatmapExpanded = homeSurface == HomeSurface.HEATMAP && !heatmapOverviewCollapsed
    val heatmapSessionActive = showTagGraphDialog || homeHeatmapExpanded
    val hasFilteredHeatmapScope = vm.entrySearch.isNotBlank() ||
        vm.activeTagFilterIds.isNotEmpty() ||
        vm.entryReadFilter != EntryReadFilterMode.ALL
    val filteredHeatmapSnapshot by produceState<TagGraphSnapshot?>(
        initialValue = null,
        homeHeatmapExpanded,
        hasFilteredHeatmapScope,
        filteredEntryRows,
        popularTagRows,
        vm.tagGraphSnapshot
    ) {
        if (!homeHeatmapExpanded) {
            return@produceState
        }
        if (!hasFilteredHeatmapScope) {
            value = vm.tagGraphSnapshot
            return@produceState
        }
        val rows = filteredEntryRows
        val popular = popularTagRows
        value = withContext(Dispatchers.IO) {
            val details = vm.getEntryDetails(rows.map { it.code })
            HeatmapEngine.buildFilteredTagGraphSnapshot(rows, details, popular)
        }
    }
    val visibleHomeSections = when {
        vm.legacyHomeUi -> vm.homeSectionOrder
        homeSurface == HomeSurface.ENTRIES -> listOf(HomeSection.ENTRIES)
        homeSurface == HomeSurface.SUGGESTED -> listOf(HomeSection.SUGGESTED)
        homeSurface == HomeSurface.SUBSCRIPTIONS -> listOf(HomeSection.SUBSCRIPTIONS)
        homeSurface == HomeSurface.HEATMAP -> listOf(HomeSection.HEATMAP)
        else -> emptyList()
    }

    LaunchedEffect(heatmapSessionActive, vm.entryHeatmapCacheRecalculationRunning) {
        if (heatmapSessionActive) {
            vm.prepareTagGraphData()
        } else if (!vm.entryHeatmapCacheRecalculationRunning) {
            sharedTagGraphEntryLayoutCache.clear()
            vm.releaseTagGraphSession()
        }
    }

    fun dismissHomeHeatmapSelectionLayer() {
        homeHeatmapSelectionSheetState = when (val current = homeHeatmapSelectionSheetState) {
            is GraphSelectionSheetState.Entry -> current.returnTagNode?.let { GraphSelectionSheetState.Tag(it) }
            is GraphSelectionSheetState.Tag -> null
            null -> null
        }
    }

    fun stopActiveScrolls() {
        screenScope.launch {
            rootListState.stopScroll()
            tagsListState.stopScroll()
            suggestedListState.stopScroll()
            creatorsListState.stopScroll()
            subscriptionsListState.stopScroll()
        }
    }

    fun creatorLinkKey(creatorId: Long, code: Int): String {
        return "$creatorId:$code"
    }

    fun homeSectionRootItemCount(section: HomeSection): Int {
        return when (section) {
            HomeSection.ENTRIES -> {
                if (vm.entriesCardCollapsed) {
                    1
                } else {
                    val columns = vm.galleryColumns.coerceIn(1, 10)
                    val rowCount = (vm.entries.size + columns - 1) / columns
                    1 + rowCount
                }
            }

            else -> 1
        }
    }

    fun homeSectionRootStartIndex(section: HomeSection): Int? {
        if (section !in vm.homeSectionOrder) return null
        var index = 1
        vm.homeSectionOrder.forEach { current ->
            if (current == section) {
                return index
            }
            index += homeSectionRootItemCount(current)
        }
        return null
    }

    pendingSubscriptionSettings?.let { subscription ->
        val currentSubscription = vm.subscriptionForRoute(subscription.routeType, subscription.routeName) ?: subscription
        var notificationsEnabled by remember(currentSubscription.id) {
            mutableStateOf(currentSubscription.notificationsEnabled)
        }
        var notificationDotEnabled by remember(currentSubscription.id) {
            mutableStateOf(currentSubscription.notificationDotEnabled)
        }
        AlertDialog(
            onDismissRequest = { pendingSubscriptionSettings = null },
            title = {
                Text(
                    text = subscriptionRouteDisplayLabel(currentSubscription.routeType, currentSubscription.routeName),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Configure how this subscription should alert you when new galleries appear.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { notificationsEnabled = !notificationsEnabled },
                                onLongClick = {}
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Notifications", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Show a system notification when this subscription finds something new.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Checkbox(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { notificationDotEnabled = !notificationDotEnabled },
                                onLongClick = {}
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Notification dot", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Keep an app-icon badge/dot on supported launchers while updates are waiting.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Checkbox(
                            checked = notificationDotEnabled,
                            onCheckedChange = { notificationDotEnabled = it }
                        )
                    }
                    Text(
                        text = "Checked: ${formatStoredUtcTimestampForDisplay(currentSubscription.lastCheckedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            vm.refreshSingleSubscription(currentSubscription.id)
                        }
                    ) {
                        Text("Check Now")
                    }
                    TextButton(
                        onClick = {
                            vm.updateSubscriptionSettings(
                                subscriptionId = currentSubscription.id,
                                notificationsEnabled = notificationsEnabled,
                                notificationDotEnabled = notificationDotEnabled
                            )
                            if (Build.VERSION.SDK_INT >= 33 &&
                                (notificationsEnabled || notificationDotEnabled) &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            pendingSubscriptionSettings = null
                        }
                    ) {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            vm.unsubscribeSubscription(currentSubscription.id)
                            pendingSubscriptionSettings = null
                        }
                    ) {
                        Text("Unsubscribe")
                    }
                    TextButton(onClick = { pendingSubscriptionSettings = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    if (showSubscriptionsListDialog) {
        Dialog(
            onDismissRequest = { showSubscriptionsListDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 640.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Subscriptions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        ImmediateActionText(
                            label = "Close",
                            onAction = { showSubscriptionsListDialog = false },
                            onPressStart = ::stopActiveScrolls,
                            runOnPressWhen = isAnyListScrolling,
                            textStyle = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = if (vm.subscriptions.isEmpty()) {
                            "No subscriptions yet."
                        } else {
                            "${vm.subscriptions.size} subscription(s)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (vm.subscriptions.isEmpty()) {
                        Text(
                            text = "Subscribe from a tag or artist/group bell to start tracking updates.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = creatorsListMaxHeight),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(
                                vm.subscriptions,
                                key = { it.id },
                                contentType = { "subscription_row" }
                            ) { subscription ->
                                val subscribed = vm.isRouteSubscribed(subscription.routeType, subscription.routeName)
                                val rowInteraction = remember { MutableInteractionSource() }
                                val bellInteraction = remember { MutableInteractionSource() }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surface,
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .privacyObfuscate(
                                            enabled = vm.incognitoModeEnabled,
                                            overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                        )
                                        .clickable(
                                            enabled = !vm.incognitoModeEnabled,
                                            interactionSource = rowInteraction,
                                            indication = null
                                        ) {
                                            pendingSubscriptionSettings = subscription
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = subscription.routeName,
                                        modifier = Modifier.weight(0.74f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = subscription.routeType,
                                        modifier = Modifier.weight(0.26f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(
                                        painter = painterResource(
                                            id = if (subscribed) {
                                                R.drawable.ic_notifications_24
                                            } else {
                                                R.drawable.ic_notifications_none_24
                                            }
                                        ),
                                        contentDescription = if (subscribed) {
                                            "Subscribed to ${subscription.routeName}"
                                        } else {
                                            "Subscribe to ${subscription.routeName}"
                                        },
                                        tint = if (subscribed) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier
                                            .size(20.dp)
                                                .combinedClickable(
                                                    enabled = !vm.incognitoModeEnabled,
                                                    interactionSource = bellInteraction,
                                                    indication = rememberRipple(bounded = false, radius = 18.dp),
                                                    onClick = {
                                                        val wasSubscribed = vm.isRouteSubscribed(subscription.routeType, subscription.routeName)
                                                        when (subscription.routeType) {
                                                            "artist", "group" -> vm.toggleCreatorSubscription(subscription.routeType, subscription.routeName)
                                                            else -> {
                                                                vm.subscriptionForRoute(subscription.routeType, subscription.routeName)?.let {
                                                                    vm.unsubscribeSubscription(it.id)
                                                                }
                                                            }
                                                        }
                                                        if (!wasSubscribed) {
                                                            requestNotificationPermissionIfNeeded()
                                                        }
                                                    },
                                                    onLongClick = { pendingSubscriptionSettings = subscription }
                                                )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSuggestedWeightsDialog) {
        SuggestionWeightsDialog(
            vm = vm,
            maxHeight = configuration.screenHeightDp.dp * 0.8f,
            onDismiss = { showSuggestedWeightsDialog = false },
            onPressStart = ::stopActiveScrolls,
            runOnPressWhen = isAnyListScrolling
        )
    }

    LaunchedEffect(homeSurface, pendingSuggestedScrollCode, vm.suggestedEntries) {
        val targetCode = pendingSuggestedScrollCode ?: return@LaunchedEffect
        if (homeSurface != HomeSurface.SUGGESTED) return@LaunchedEffect
        val targetIndex = vm.suggestedEntries.indexOfFirst { it.code == targetCode }
        if (targetIndex < 0) return@LaunchedEffect
        delay(48)
        suggestedListState.animateScrollToItem(targetIndex)
        pendingSuggestedScrollCode = null
    }

    if (showPersonalizationDialog) {
        AnimatedOverlayCard(
            onDismissRequest = { showPersonalizationDialog = false },
            modifier = Modifier.heightIn(max = configuration.screenHeightDp.dp * 0.82f),
            coverSystemBars = true
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Personalization",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        ImmediateActionText(
                            label = "Close",
                            onAction = { showPersonalizationDialog = false },
                            onPressStart = ::stopActiveScrolls,
                            runOnPressWhen = isAnyListScrolling,
                            textStyle = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "Default entry mode uses the first enabled item in your cycle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = vm.entryReadFilterCycleSummary(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { showEntryModeCycleDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Customize Enabled Cycle")
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f),
                        thickness = 1.dp
                    )
                    Text(
                        text = "Home page",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = vm.homeSectionOrderSummary(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { showCustomizeHomeDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Customize Home Page")
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f),
                        thickness = 1.dp
                    )
                    Text(
                        text = "Browser",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = vm.defaultBrowserDuplicateCheckMode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { showBrowserDuplicateModeDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Duplicate checks: ${vm.defaultBrowserDuplicateCheckMode.label}")
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f),
                        thickness = 1.dp
                    )
                    Text(
                        text = "Default sorts",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick = { personalizationSortTarget = PersonalizationSortTarget.ENTRIES },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Entries: ${describeEntrySort(vm.defaultEntrySortField, vm.defaultEntrySortDirection)}")
                    }
                    Button(
                        onClick = { personalizationSortTarget = PersonalizationSortTarget.TAGS },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tags: ${describeTagSort(vm.defaultTagSortField, vm.defaultTagSortDirection)}")
                    }
                    Button(
                        onClick = { personalizationSortTarget = PersonalizationSortTarget.CREATORS },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Artists / Groups: ${describeCreatorSort(vm.defaultCreatorSortField, vm.defaultCreatorSortDirection)}")
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f),
                        thickness = 1.dp
                    )
                    Text(
                        text = "Dashboard order",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = vm.homeSectionOrderSummary(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            showPersonalizationDialog = false
                            showCustomizeHomeDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Change dashboard order")
                    }
            }
        }
    }

    if (showBrowserDuplicateModeDialog) {
        BrowserDuplicateCheckModeDialog(
            title = "Default Browser Duplicate Checks",
            currentMode = vm.defaultBrowserDuplicateCheckMode,
            defaultMode = vm.defaultBrowserDuplicateCheckMode,
            temporary = false,
            onSelect = {
                vm.updateDefaultBrowserDuplicateCheckMode(it)
                showBrowserDuplicateModeDialog = false
            },
            onReset = {
                vm.resetDefaultBrowserDuplicateCheckMode()
                showBrowserDuplicateModeDialog = false
            },
            onDismiss = { showBrowserDuplicateModeDialog = false }
        )
    }

    if (showEntryModeCycleDialog) {
        AnimatedOverlayCard(
            onDismissRequest = { showEntryModeCycleDialog = false },
            modifier = Modifier.heightIn(max = configuration.screenHeightDp.dp * 0.82f),
            coverSystemBars = true
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Customize Enabled Cycle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        ImmediateActionText(
                            label = "Close",
                            onAction = { showEntryModeCycleDialog = false },
                            onPressStart = ::stopActiveScrolls,
                            runOnPressWhen = isAnyListScrolling,
                            textStyle = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "Drag the handle to set the toggle order. Remove with -, restore with + below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    EntryReadFilterCycleEditor(
                        enabledModes = vm.entryReadFilterCycleOrder,
                        onReorder = vm::updateEntryReadFilterCycleOrder,
                        onRemove = vm::removeEntryReadFilterFromCycle,
                        onAdd = vm::addEntryReadFilterToCycle
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = vm::resetEntryReadFilterCycleOrder) {
                            Text("Set To Default")
                        }
                    }
            }
        }
    }

    if (showCustomizeHomeDialog) {
        AnimatedOverlayCard(
            onDismissRequest = { showCustomizeHomeDialog = false },
            modifier = Modifier.heightIn(max = configuration.screenHeightDp.dp * 0.82f),
            coverSystemBars = true
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Customize Home Page",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        ImmediateActionText(
                            label = "Close",
                            onAction = { showCustomizeHomeDialog = false },
                            onPressStart = ::stopActiveScrolls,
                            runOnPressWhen = isAnyListScrolling,
                            textStyle = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "Drag the handle to reorder the visible sections. Remove with -, restore with + below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HomeSectionLayoutEditor(
                        enabledSections = vm.homeSectionOrder,
                        onReorder = vm::updateHomeSectionOrder,
                        onRemove = vm::removeHomeSection,
                        onAdd = vm::addHomeSection
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = vm::resetHomeSectionOrder) {
                            Text("Set To Default")
                        }
                    }
            }
        }
    }

    personalizationSortTarget?.let { target ->
        when (target) {
            PersonalizationSortTarget.ENTRIES -> {
                SelectionDialog(
                    title = "Default Entry Sort",
                    options = entrySortPresets(),
                    selectedKey = "${vm.defaultEntrySortField?.name ?: "NONE"}|${vm.defaultEntrySortDirection.name}",
                    optionKey = { "${it.field?.name ?: "NONE"}|${it.direction.name}" },
                    optionLabel = { it.label },
                    onSelect = { preset ->
                        vm.setDefaultEntrySort(preset.field, preset.direction)
                        personalizationSortTarget = null
                    },
                    onReset = {
                        vm.resetDefaultEntrySort()
                        personalizationSortTarget = null
                    },
                    onDismiss = { personalizationSortTarget = null }
                )
            }
            PersonalizationSortTarget.TAGS -> {
                SelectionDialog(
                    title = "Default Tag Sort",
                    options = tagSortPresets(),
                    selectedKey = "${vm.defaultTagSortField.name}|${vm.defaultTagSortDirection.name}",
                    optionKey = { "${it.field.name}|${it.direction.name}" },
                    optionLabel = { it.label },
                    onSelect = { preset ->
                        vm.setDefaultTagSort(preset.field, preset.direction)
                        personalizationSortTarget = null
                    },
                    onReset = {
                        vm.resetDefaultTagSort()
                        personalizationSortTarget = null
                    },
                    onDismiss = { personalizationSortTarget = null }
                )
            }
            PersonalizationSortTarget.CREATORS -> {
                SelectionDialog(
                    title = "Default Artist / Group Sort",
                    options = creatorSortPresets(),
                    selectedKey = "${vm.defaultCreatorSortField.name}|${vm.defaultCreatorSortDirection.name}",
                    optionKey = { "${it.field.name}|${it.direction.name}" },
                    optionLabel = { it.label },
                    onSelect = { preset ->
                        vm.setDefaultCreatorSort(preset.field, preset.direction)
                        personalizationSortTarget = null
                    },
                    onReset = {
                        vm.resetDefaultCreatorSort()
                        personalizationSortTarget = null
                    },
                    onDismiss = { personalizationSortTarget = null }
                )
            }
        }
    }

    fun selectEntryFromRow(code: Int) {
        if (vm.isEntryDownloadBatchSelecting()) {
            vm.toggleEntryDownloadBatchSelection(code)
            return
        }
        val y = entryItemYByCode[code]
        if (y != null) {
            pendingSelectionAnchor = SelectionAnchor(
                context = SelectionAnchorContext.ENTRY,
                code = code,
                yInRoot = y
            )
        } else {
            pendingSelectionAnchor = null
        }
        vm.onEntryCardClicked(code)
    }

    fun openRelatedLibraryEntry(code: Int) {
        if (code <= 0) return
        heatmapOverviewCollapsed = true
        vm.expandEntriesSection()
        if (!vm.legacyHomeUi && homeSurface != HomeSurface.ENTRIES) {
            switchHomeSurface(HomeSurface.ENTRIES, restoreScroll = false)
        }
        vm.openSeriesEntry(code)
    }

    fun selectHistoryDay(day: DailyActivityPoint) {
        historySelectedDay = day
        historySelectedDayEntries = emptyList()
        historySelectedDayEntriesLoading = true
        screenScope.launch {
            historySelectedDayEntries = runCatching { vm.readEntriesForDay(day.date) }
                .getOrElse { emptyList() }
            historySelectedDayEntriesLoading = false
        }
    }


    LaunchedEffect(vm.selectedCode) {
        if (vm.selectedCode != null) {
            vm.ensureReadAnalyticsLoaded(forceRefresh = false)
        }
    }

    LaunchedEffect(vm.visibleSubscriptionEvents) {
        val selectedId = selectedSubscriptionEventId ?: return@LaunchedEffect
        if (vm.visibleSubscriptionEvents.none { it.id == selectedId }) {
            selectedSubscriptionEventId = null
        }
    }

    LaunchedEffect(vm.incognitoModeEnabled) {
        if (vm.incognitoModeEnabled) {
            selectedSubscriptionEventId = null
            pendingSubscriptionSettings = null
            localEntryHoldPopupState = null
            suggestedDuplicateComparisonState = null
            pendingUnhideSuggestionCode = null
            showDeleteConfirm = false
            showRemoveLocalDownloadConfirm = false
            showRedownloadLocalConfirm = false
            showRefetchConfirm = false
            pendingRefetchCode = null
            showEntryDownloadIntroPrompt = false
            showEntryDownloadConfirmPrompt = false
            showEntryDownloadBatchConfirmPrompt = false
            showOpenDownloadedFolderPrompt = false
            pendingDownloadDetail = null
            vm.dismissPinTogglePrompt()
        }
    }

    LaunchedEffect(vm.selectedCode, pendingSelectionAnchor) {
        val anchor = pendingSelectionAnchor ?: return@LaunchedEffect
        if (vm.selectedCode != anchor.code) {
            pendingSelectionAnchor = null
            return@LaunchedEffect
        }
        val deltaThresholdPx = 3f

        when (anchor.context) {
            SelectionAnchorContext.ENTRY -> {
                val newY = entryItemYByCode[anchor.code]
                if (newY == null) {
                    pendingSelectionAnchor = null
                    return@LaunchedEffect
                }
                val delta = newY - anchor.yInRoot
                if (abs(delta) > deltaThresholdPx) {
                    rootListState.scrollBy(delta)
                }
            }

            SelectionAnchorContext.CREATOR_LINK -> {
                val creatorId = anchor.creatorId ?: return@LaunchedEffect
                val key = creatorLinkKey(creatorId, anchor.code)
                val newY = creatorLinkYByKey[key]
                if (newY == null) {
                    pendingSelectionAnchor = null
                    return@LaunchedEffect
                }
                val delta = newY - anchor.yInRoot
                if (abs(delta) > deltaThresholdPx) {
                    creatorsListState.scrollBy(delta)
                }
            }
        }

        pendingSelectionAnchor = null
    }

    LaunchedEffect(
        vm.pendingEntryJumpCode,
        vm.entries,
        vm.entriesCardCollapsed,
        vm.galleryColumns,
        vm.homeSectionOrder,
        showSettingsTab
    ) {
        val targetCode = vm.pendingEntryJumpCode ?: return@LaunchedEffect
        val targetIndex = vm.entries.indexOfFirst { it.code == targetCode }
        if (targetIndex < 0) {
            vm.consumePendingEntryJump()
            graphEntryJumpTransitionActive = false
            return@LaunchedEffect
        }

        if (showSettingsTab) {
            showSettingsTab = false
            delay(60)
        }
        val entriesStartIndex = homeSectionRootStartIndex(HomeSection.ENTRIES)
        if (entriesStartIndex == null) {
            vm.setStatus("Entries is hidden on the home page.")
            vm.consumePendingEntryJump()
            graphEntryJumpTransitionActive = false
            return@LaunchedEffect
        }
        if (vm.entriesCardCollapsed) {
            vm.toggleEntriesCardCollapsed()
            delay(60)
        }

        val columns = vm.galleryColumns.coerceIn(1, 10)
        val targetRowIndex = (targetIndex / columns).coerceAtLeast(0)
        val totalItems = rootListState.layoutInfo.totalItemsCount
        if (totalItems > 0) {
            val targetRootIndex = (entriesStartIndex + targetRowIndex).coerceIn(0, totalItems - 1)
            rootListState.scrollToItem(targetRootIndex)
        }
        var settledFrames = 0
        for (attempt in 0 until 32) {
            val settledNow =
                !showSettingsTab &&
                    !vm.entriesCardCollapsed &&
                    vm.selectedCode == targetCode &&
                    vm.selectedDetail?.code == targetCode &&
                    !rootListState.isScrollInProgress
            if (settledNow) {
                settledFrames += 1
                if (settledFrames >= 3) {
                    break
                }
            } else {
                settledFrames = 0
            }
            delay(40)
        }
        delay(90)
        vm.consumePendingEntryJump()
        graphEntryJumpTransitionActive = false
    }

    LaunchedEffect(pendingGraphEntryOpenCode, graphEntryJumpTransitionActive) {
        val targetCode = pendingGraphEntryOpenCode ?: return@LaunchedEffect
        if (!graphEntryJumpTransitionActive) return@LaunchedEffect
        delay(110)
        showSettingsTab = false
        vm.openSeriesEntry(targetCode)
        pendingGraphEntryOpenCode = null
    }

    LaunchedEffect(
        vm.pendingCreatorJumpId,
        vm.creators,
        vm.entries,
        vm.entriesCardCollapsed,
        vm.creatorsCardCollapsed,
        vm.galleryColumns,
        vm.homeSectionOrder,
        showSettingsTab,
        homeSurface
    ) {
        val targetId = vm.pendingCreatorJumpId ?: return@LaunchedEffect
        val targetIndex = vm.creators.indexOfFirst { it.id == targetId }
        if (targetIndex < 0) return@LaunchedEffect

        if (!vm.legacyHomeUi) {
            if (homeSurface != HomeSurface.CREATORS) return@LaunchedEffect
            // Keep the handoff deliberate while the linked-entry list is prepared,
            // but always release the fade even if the coroutine is superseded.
            creatorJumpTransitionActive = true
            try {
                delay(70)
                creatorsListState.scrollToItem(targetIndex)
                var checksRemaining = 16
                while (checksRemaining > 0 && vm.isCreatorLoading(targetId)) {
                    delay(35)
                    checksRemaining -= 1
                }
                delay(80)
                vm.consumePendingCreatorJump()
            } finally {
                creatorJumpTransitionActive = false
            }
            return@LaunchedEffect
        }

        creatorJumpTransitionActive = true
        delay(95)

        if (showSettingsTab) {
            showSettingsTab = false
            delay(80)
        }
        val creatorsCardIndex = homeSectionRootStartIndex(HomeSection.CREATORS)
        if (creatorsCardIndex == null) {
            vm.setStatus("Artists / Groups is hidden on the home page.")
            creatorJumpTransitionActive = false
            vm.consumePendingCreatorJump()
            return@LaunchedEffect
        }
        if (vm.creatorsCardCollapsed) {
            vm.toggleCreatorsCardCollapsed()
            delay(90)
        }

        val totalItems = rootListState.layoutInfo.totalItemsCount
        if (totalItems > 0) {
            val targetRootIndex = creatorsCardIndex.coerceIn(0, totalItems - 1)
            rootListState.animateScrollToItem(targetRootIndex)
        }
        creatorsListState.animateScrollToItem(targetIndex)
        repeat(18) {
            if (!vm.isCreatorLoading(targetId)) return@repeat
            delay(35)
        }
        delay(70)
        creatorJumpTransitionActive = false
        vm.consumePendingCreatorJump()
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            vm.importFromUri(uri)
        }
    }

    val batchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            vm.onBatchFileChosen(uri)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val clearAfter = pendingClearAfterExport
        pendingClearAfterExport = false
        if (uri != null) {
            vm.exportToUri(uri, clearAfterExport = clearAfter)
        } else {
            vm.onExportCancelled(clearAfter)
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            vm.exportCsvToUri(uri)
        } else {
            vm.setStatus("CSV export cancelled.")
        }
    }

    val backupFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val persisted = runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }.isSuccess
            vm.setAutoBackupFolder(uri)
            if (!persisted) {
                vm.setStatus("Folder picked, but persistent permission was not granted by Android. Backup may fail after app restarts.")
            }
        } else {
            vm.setStatus("Backup folder selection cancelled.")
        }
    }

    val downloadFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val persisted = runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }.isSuccess
            vm.setGalleryDownloadFolder(uri)
            if (!persisted) {
                vm.setStatus("Downloads folder picked, but persistent permission was not granted by Android.")
            }
            if (pendingDownloadChangeFolder && pendingDownloadDetail != null) {
                pendingDownloadChangeFolder = false
                showEntryDownloadConfirmPrompt = true
            }
        } else {
            if (pendingDownloadChangeFolder) {
                pendingDownloadChangeFolder = false
                vm.setStatus("Downloads folder selection cancelled.")
            }
        }
    }

    val selectedEntryDownloaded by produceState(
        initialValue = false,
        vm.selectedDetail?.code,
        vm.downloadedGalleryNonce,
        vm.galleryDownloadTreeUri,
        vm.autoBackupTreeUri
    ) {
        val code = vm.selectedDetail?.code ?: 0
        value = if (code > 0) {
            withContext(Dispatchers.IO) { vm.isEntryDownloaded(code) }
        } else {
            false
        }
    }

    val dashboardEntryDetailActions = DashboardEntryDetailActions(
        onOpenCreatorFromDetail = ::openCreatorFromEntryDetail,
        onRefetch = { code ->
            pendingRefetchCode = code
            showRefetchConfirm = true
        },
        onDownload = { entry ->
            pendingDownloadDetail = entry
            if (selectedEntryDownloaded && vm.selectedDetail?.code == entry.code) {
                showOpenDownloadedFolderPrompt = true
            } else if (loadGalleryDownloadSkipPrompt(context)) {
                showEntryDownloadConfirmPrompt = true
            } else {
                skipEntryDownloadIntroChecked = false
                showEntryDownloadIntroPrompt = true
            }
        },
        onRedownload = { entry ->
            pendingDownloadDetail = entry
            showRedownloadLocalConfirm = true
        },
        onDelete = { code ->
            if (vm.selectedCode != code) {
                vm.selectEntry(code)
            }
            showDeleteConfirm = true
        },
        onOpenRelatedEntry = ::openRelatedLibraryEntry,
        onThumbnailClick = { code, url, description ->
            thumbnailPreview = ThumbnailPreviewState(
                code = code,
                thumbnailUrl = url,
                contentDescription = description
            )
        }
    )

    BackHandler(
        enabled = thumbnailPreview != null ||
            localEntryHoldPopupState != null ||
            showRefetchAllPrompt ||
            showClearUnsafePrompt ||
            showClearAllPrompt ||
            showDeleteConfirm ||
            showRemoveLocalDownloadConfirm ||
            showRedownloadLocalConfirm ||
            showRefetchConfirm ||
            showEnableGalleryNeedsThumbnailsPrompt ||
            showDisableThumbnailsForGalleryPrompt ||
            showEntryLayoutDialog ||
            showBlockedTagsManager ||
            showSuggestedWeightsDialog ||
            suggestedDuplicateComparisonState != null ||
            showBrowserDuplicateModeDialog ||
            showEntryModeCycleDialog ||
            showCustomizeHomeDialog ||
            showPersonalizationDialog ||
            personalizationSortTarget != null ||
            showSettingsTab ||
            (!vm.legacyHomeUi && homeSurface != HomeSurface.DASHBOARD) ||
            vm.manualCreatorPromptState != null ||
            vm.batchCreatorPromptState != null ||
            vm.hasInAppBackAction()
    ) {
        when {
            thumbnailPreview != null -> {
                thumbnailPreview = null
            }
            localEntryHoldPopupState != null -> {
                localEntryHoldPopupState = null
            }
            vm.manualCreatorPromptState != null -> {
                vm.cancelManualCreatorPrompt()
            }
            vm.batchCreatorPromptState != null -> {
                vm.cancelBatchCreatorPrompt()
            }
            showRefetchAllPrompt -> {
                showRefetchAllPrompt = false
                vm.setStatus("Re-fetch all cancelled.")
            }
            showClearUnsafePrompt -> {
                showClearUnsafePrompt = false
                vm.setStatus("Clear all cancelled.")
            }
            showClearAllPrompt -> {
                showClearAllPrompt = false
                vm.setStatus("Clear all cancelled.")
            }
            showDeleteConfirm -> {
                showDeleteConfirm = false
            }
            showRemoveLocalDownloadConfirm -> {
                showRemoveLocalDownloadConfirm = false
                pendingDownloadDetail = null
            }
            showRedownloadLocalConfirm -> {
                showRedownloadLocalConfirm = false
                pendingDownloadDetail = null
            }
            showRefetchConfirm -> {
                showRefetchConfirm = false
                pendingRefetchCode = null
            }
            showEnableGalleryNeedsThumbnailsPrompt -> {
                showEnableGalleryNeedsThumbnailsPrompt = false
                pendingEntryLayoutApplyMode = null
            }
            showDisableThumbnailsForGalleryPrompt -> {
                showDisableThumbnailsForGalleryPrompt = false
            }
            showEntryLayoutDialog -> {
                showEntryLayoutDialog = false
            }
            showBlockedTagsManager -> {
                showBlockedTagsManager = false
            }
            showBrowserDuplicateModeDialog -> {
                showBrowserDuplicateModeDialog = false
            }
            personalizationSortTarget != null -> {
                personalizationSortTarget = null
            }
            showEntryModeCycleDialog -> {
                showEntryModeCycleDialog = false
            }
            showCustomizeHomeDialog -> {
                showCustomizeHomeDialog = false
            }
            showPersonalizationDialog -> {
                showPersonalizationDialog = false
            }
            showSuggestedWeightsDialog -> {
                showSuggestedWeightsDialog = false
            }
            suggestedDuplicateComparisonState != null -> {
                suggestedDuplicateComparisonState = null
            }
            showSettingsTab -> {
                showSettingsTab = false
            }
            !vm.legacyHomeUi && homeSurface != HomeSurface.DASHBOARD -> {
                switchHomeSurface(HomeSurface.DASHBOARD)
                heatmapOverviewCollapsed = true
            }
            vm.hasInAppBackAction() -> {
                vm.handleInAppBackAction()
            }
        }
    }

    BackHandler(enabled = homeHeatmapSelectionSheetState != null) {
        dismissHomeHeatmapSelectionLayer()
    }

    LaunchedEffect(showSettingsTab, showTagGraphDialog, heatmapOverviewCollapsed, filteredHeatmapSnapshot) {
        if (showSettingsTab || showTagGraphDialog || heatmapOverviewCollapsed || filteredHeatmapSnapshot == null) {
            homeHeatmapSelectionSheetState = null
        }
    }

    if (showBackupThumbnailArchiveWarning) {
        val archiveEstimate by produceState<BackupThumbnailStorageEstimate?>(initialValue = null, vm.autoBackupTreeUri) {
            value = withContext(Dispatchers.IO) {
                runCatching { vm.calculateBackupThumbnailArchiveEstimate() }.getOrNull()
            }
        }
        AlertDialog(
            onDismissRequest = { showBackupThumbnailArchiveWarning = false },
            title = { Text("Enable Backup Thumbnail Archive") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "High storage requirements. This stores local cover thumbnails and their dHashes inside your procedural backup folder so browser duplicate checks can reuse them without refetching."
                    )
                    Text(
                        text = when (val estimate = archiveEstimate) {
                            null -> "Calculating storage requirement..."
                            else -> "Calculated storage requirement for ${estimate.entryCount} entries: ${formatStorageSize(estimate.estimatedTotalBytes)}."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    archiveEstimate?.let { estimate ->
                        if (estimate.storedCount > 0) {
                            Text(
                                text = "${estimate.storedCount} cover backups are already present in this folder.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = archiveEstimate != null,
                    onClick = {
                        vm.updateBackupThumbnailArchiveEnabled(true)
                        showBackupThumbnailArchiveWarning = false
                    }
                ) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupThumbnailArchiveWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTagGraphDialog) {
        HeatmapScreen(
            snapshot = vm.tagGraphSnapshot,
            loading = vm.tagGraphLoading,
            errorMessage = vm.tagGraphErrorMessage,
            selectedTab = selectedTagGraphTab,
            selectedHeatmapDisplayMode = selectedTagHeatmapDisplayMode,
            incognitoModeEnabled = vm.incognitoModeEnabled,
            analyticsSnapshot = vm.readAnalytics,
            showThumbnails = vm.showThumbnails,
            entryDetailProvider = { code -> vm.getEntryDetail(code) },
            onSelectTab = { selectedTagGraphTab = it },
            onSelectHeatmapDisplayMode = { selectedTagHeatmapDisplayMode = it },
            onRefresh = { vm.ensureTagGraphLoaded(forceRefresh = true) },
            onSelectGraphEntry = vm::selectEntry,
            onOpenEntryInBrowser = { code ->
                if (vm.selectedCode != code) {
                    vm.selectEntry(code)
                }
                vm.openSelectedInBrowser()
            },
            onOpenCreatorFromDetail = ::openCreatorFromEntryDetail,
            onCopyCode = vm::copyCodeToClipboard,
            onToggleReadStatus = vm::toggleEntryRead,
            onSetRating = vm::setEntryRating,
            onResetRating = { code -> vm.setEntryRating(code, 0) },
            onRefetch = { code ->
                pendingRefetchCode = code
                showRefetchConfirm = true
            },
            onDelete = { code ->
                if (vm.selectedCode != code) {
                    vm.selectEntry(code)
                }
                showDeleteConfirm = true
            },
            selectedSeriesNeighborsForCode = { code ->
                if (vm.selectedCode == code) vm.selectedSeriesNeighbors else SeriesNeighbors()
            },
            onOpenSeriesEntry = ::openRelatedLibraryEntry,
            onOpenCreatorInBrowser = vm::openCreatorPreviewInBrowser,
            onSelectedThumbnailClick = { code, url, description ->
                thumbnailPreview = ThumbnailPreviewState(
                    code = code,
                    thumbnailUrl = url,
                    contentDescription = description
                )
            },
            entryLayoutSessionCache = sharedTagGraphEntryLayoutCache,
            persistentEntryLayoutProvider = vm::loadEntryHeatmapLayoutForSnapshot,
            onDismiss = { showTagGraphDialog = false }
        )
    }

    thumbnailPreview?.let { preview ->
        ThumbnailPreviewDialog(
            thumbnailUrl = preview.thumbnailUrl,
            contentDescription = preview.contentDescription,
            obscure = vm.incognitoModeEnabled,
            onOpenInBrowser = {
                vm.openThumbnailPreviewInBrowser(preview.code)
                screenScope.launch {
                    delay(220L)
                    if (thumbnailPreview?.code == preview.code) {
                        thumbnailPreview = null
                    }
                }
            },
            onDismiss = { thumbnailPreview = null }
        )
    }

    if (!vm.incognitoModeEnabled) localEntryHoldPopupState?.let { popup ->
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                LocalEntryHoldPopup(
                    code = popup.code,
                    rating = popup.rating,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    selectedActivityPoint?.let { point ->
        val privacyOverlay = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
        AlertDialog(
            onDismissRequest = { selectedActivityPoint = null },
            title = {
                Text(
                    text = "Activity: ${point.date.format(UPLOAD_DATE_FORMAT)}",
                    modifier = Modifier.privacyObfuscate(
                        enabled = vm.incognitoModeEnabled,
                        overlayColor = privacyOverlay
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Pages read: ${point.pagesRead}",
                        modifier = Modifier.privacyObfuscate(
                            enabled = vm.incognitoModeEnabled,
                            overlayColor = privacyOverlay
                        )
                    )
                    Text(
                        text = "Entries read: ${point.entriesRead}",
                        modifier = Modifier.privacyObfuscate(
                            enabled = vm.incognitoModeEnabled,
                            overlayColor = privacyOverlay
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedActivityPoint = null }) {
                    Text("Close")
                }
            }
        )
    }

    historySelectedDay?.let { day ->
        DayReadEntriesDialog(
            day = day,
            entries = historySelectedDayEntries,
            loading = historySelectedDayEntriesLoading,
            incognitoModeEnabled = vm.incognitoModeEnabled,
            onDismiss = { historySelectedDay = null }
        )
    }

    if (vm.errorDialogMessage != null) {
        AlertDialog(
            onDismissRequest = vm::dismissErrorDialog,
            title = { Text("Error") },
            text = { Text(vm.errorDialogMessage.orEmpty()) },
            confirmButton = {
                TextButton(onClick = vm::dismissErrorDialog) {
                    Text("OK")
                }
            }
        )
    }

    if (vm.infoDialogMessage != null) {
        AlertDialog(
            onDismissRequest = vm::dismissInfoDialog,
            title = { Text("Info") },
            text = { Text(vm.infoDialogMessage.orEmpty()) },
            confirmButton = {
                TextButton(onClick = vm::dismissInfoDialog) {
                    Text("OK")
                }
            }
        )
    }

    if (showEnableGalleryNeedsThumbnailsPrompt) {
        AlertDialog(
            onDismissRequest = { showEnableGalleryNeedsThumbnailsPrompt = false },
            title = { Text("Enable thumbnails first?") },
            text = {
                Text("To use Pure Gallery Mode you must turn on thumbnails. Do you wish to proceed?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEnableGalleryNeedsThumbnailsPrompt = false
                        if (!vm.showThumbnails) {
                            vm.toggleThumbnailsEnabled()
                        }
                        val pendingMode = pendingEntryLayoutApplyMode
                        if (pendingMode != null) {
                            vm.applyEntryLayout(
                                modeGallery = pendingMode,
                                columns = pendingEntryLayoutApplyColumns
                            )
                            pendingEntryLayoutApplyMode = null
                        } else if (!vm.pureGalleryMode) {
                            vm.togglePureGalleryMode()
                        }
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEnableGalleryNeedsThumbnailsPrompt = false
                        pendingEntryLayoutApplyMode = null
                    }
                ) {
                    Text("No")
                }
            }
        )
    }

    if (showDisableThumbnailsForGalleryPrompt) {
        AlertDialog(
            onDismissRequest = { showDisableThumbnailsForGalleryPrompt = false },
            title = { Text("Disable gallery mode too?") },
            text = {
                Text("Pure Gallery Mode requires thumbnails. Turning thumbnails off will also turn off Pure Gallery Mode. Do you want to proceed?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisableThumbnailsForGalleryPrompt = false
                        if (vm.pureGalleryMode) {
                            vm.togglePureGalleryMode()
                        }
                        if (vm.showThumbnails) {
                            vm.toggleThumbnailsEnabled()
                        }
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableThumbnailsForGalleryPrompt = false }) {
                    Text("No")
                }
            }
        )
    }

    if (showEntryLayoutDialog) {
        var draftModeGallery by remember(showEntryLayoutDialog) { mutableStateOf(vm.pureGalleryMode) }
        var draftColumnsFloat by remember(showEntryLayoutDialog) { mutableStateOf(vm.galleryColumns.toFloat()) }
        val draftColumns = draftColumnsFloat.roundToInt().coerceIn(1, 10)

        AlertDialog(
            onDismissRequest = { showEntryLayoutDialog = false },
            title = { Text("Set Entry Layout") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Mode",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !draftModeGallery,
                            onClick = { draftModeGallery = false },
                            label = { Text("Normal") }
                        )
                        FilterChip(
                            selected = draftModeGallery,
                            onClick = { draftModeGallery = true },
                            label = { Text("Gallery") }
                        )
                    }

                    Text(
                        text = "Preview (${if (draftModeGallery) "Gallery" else "Normal"})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    val previewEntries = vm.entryLayoutPreviewSamples
                    if (previewEntries.isEmpty()) {
                        Text(
                            text = "No saved entries to preview.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val clampedColumns = draftColumns.coerceAtLeast(1)
                        val shownEntries = previewEntries.take(minOf(previewEntries.size, clampedColumns))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            shownEntries.forEach { sample ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (draftModeGallery) {
                                        ThumbnailImage(
                                            thumbnailUrl = sample.thumbnailUrl,
                                            backupCode = sample.code,
                                            contentDescription = "Preview thumbnail ${sample.code}",
                                            obscure = vm.incognitoModeEnabled,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(74.dp)
                                        )
                                    } else {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = sample.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Code: ${sample.code}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                            if (shownEntries.size < clampedColumns) {
                                repeat(clampedColumns - shownEntries.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Text(
                        text = "Entries across: $draftColumns",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = draftColumnsFloat,
                        onValueChange = { draftColumnsFloat = it.coerceIn(1f, 10f) },
                        valueRange = 1f..10f
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (draftModeGallery && !vm.showThumbnails) {
                            pendingEntryLayoutApplyMode = true
                            pendingEntryLayoutApplyColumns = draftColumns
                            showEntryLayoutDialog = false
                            showEnableGalleryNeedsThumbnailsPrompt = true
                        } else {
                            vm.applyEntryLayout(
                                modeGallery = draftModeGallery,
                                columns = draftColumns
                            )
                            showEntryLayoutDialog = false
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEntryLayoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEnableAppLockDialog) {
        var pinInput by remember { mutableStateOf("") }
        var confirmInput by remember { mutableStateOf("") }
        var useBiometric by remember { mutableStateOf(true) }
        var validationMessage by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showEnableAppLockDialog = false },
            title = { Text("Enable App Lock") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it.filter { ch -> ch.isDigit() }.take(20) },
                        label = { Text("PIN (max 20 digits)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmInput,
                        onValueChange = { confirmInput = it.filter { ch -> ch.isDigit() }.take(20) },
                        label = { Text("Confirm PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Biometric unlock",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = { useBiometric = !useBiometric }) {
                            Text(if (useBiometric) "On" else "Off")
                        }
                    }
                    Text(
                        text = "Unlock opens immediately once the full correct PIN is entered.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    validationMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pinInput != confirmInput) {
                            validationMessage = "PIN values do not match."
                            return@TextButton
                        }
                        val error = vm.setOrChangeAppLockPin(pinInput, enableIfDisabled = true)
                        if (error != null) {
                            validationMessage = error
                            return@TextButton
                        }
                        vm.chooseAppLockBiometricEnabled(useBiometric)
                        showEnableAppLockDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEnableAppLockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showChangePinDialog) {
        var pinInput by remember { mutableStateOf("") }
        var confirmInput by remember { mutableStateOf("") }
        var validationMessage by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Change App Lock PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it.filter { ch -> ch.isDigit() }.take(20) },
                        label = { Text("New PIN (max 20 digits)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmInput,
                        onValueChange = { confirmInput = it.filter { ch -> ch.isDigit() }.take(20) },
                        label = { Text("Confirm new PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    validationMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pinInput != confirmInput) {
                            validationMessage = "PIN values do not match."
                            return@TextButton
                        }
                        val error = vm.setOrChangeAppLockPin(pinInput, enableIfDisabled = false)
                        if (error != null) {
                            validationMessage = error
                            return@TextButton
                        }
                        showChangePinDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDisableAppLockPrompt) {
        AlertDialog(
            onDismissRequest = { showDisableAppLockPrompt = false },
            title = { Text("Disable App Lock?") },
            text = {
                Text("This removes PIN and biometric protection until you set it up again.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.disableAppLock()
                        showDisableAppLockPrompt = false
                    }
                ) {
                    Text("Disable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableAppLockPrompt = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEntryDownloadIntroPrompt) {
        val currentDownloadFolderLabel = vm.galleryDownloadFolderLabel()
        val hasCustomDownloadFolder = vm.galleryDownloadTreeUri.isNotBlank()
        AlertDialog(
            onDismissRequest = {
                showEntryDownloadIntroPrompt = false
                pendingDownloadDetail = null
            },
            title = { Text("Download Entry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (hasCustomDownloadFolder) {
                            "Downloads are currently set to:\n$currentDownloadFolderLabel\n\nDo you want to continue or change the downloads folder for this entry?"
                        } else {
                            "Downloads currently default to your backup folder:\n$currentDownloadFolderLabel\n\nDo you want to continue or change the downloads folder for this entry?"
                        }
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = skipEntryDownloadIntroChecked,
                            onCheckedChange = { skipEntryDownloadIntroChecked = it }
                        )
                        Text(
                            text = "Do not show this again",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                storeGalleryDownloadSkipPrompt(context, skipEntryDownloadIntroChecked)
                                pendingDownloadChangeFolder = true
                                showEntryDownloadIntroPrompt = false
                                downloadFolderLauncher.launch(null)
                            }
                        ) {
                            Text("Change Folder")
                        }
                        TextButton(
                            onClick = {
                                showEntryDownloadIntroPrompt = false
                                pendingDownloadDetail = null
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(1.dp))
                        TextButton(
                            onClick = {
                                storeGalleryDownloadSkipPrompt(context, skipEntryDownloadIntroChecked)
                                showEntryDownloadIntroPrompt = false
                                showEntryDownloadConfirmPrompt = true
                            }
                        ) {
                            Text("Continue")
                        }
                    }
                }
            },
            dismissButton = {}
        )
    }

    if (showEntryDownloadConfirmPrompt) {
        val detail = pendingDownloadDetail
        AlertDialog(
            onDismissRequest = {
                showEntryDownloadConfirmPrompt = false
                pendingDownloadDetail = null
            },
            title = { Text("Confirm Download") },
            text = {
                Text(
                    detail?.let {
                        "Download ${it.title.ifBlank { "Gallery ${it.code}" }}\ncode ${it.code}\nto ${vm.galleryDownloadFolderLabel()}?"
                    } ?: "No entry selected."
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            val initialCode = pendingDownloadDetail?.code
                            showEntryDownloadConfirmPrompt = false
                            pendingDownloadDetail = null
                            vm.startEntryDownloadBatch(EntryDownloadBatchMode.DOWNLOAD, initialCode)
                        }
                    ) {
                        Text("Batch Download")
                    }
                    TextButton(
                        onClick = {
                            showEntryDownloadConfirmPrompt = false
                            pendingDownloadDetail = null
                        }
                    ) {
                        Text("No")
                    }
                    TextButton(
                        onClick = {
                            val current = pendingDownloadDetail
                            showEntryDownloadConfirmPrompt = false
                            if (current != null) {
                                vm.downloadEntry(current)
                            }
                            pendingDownloadDetail = null
                        },
                        enabled = detail != null
                    ) {
                        Text("Yes")
                    }
                }
            },
            dismissButton = {}
        )
    }

    if (showEntryDownloadBatchConfirmPrompt) {
        val mode = vm.entryDownloadBatchMode
        val count = vm.entryDownloadBatchSelectedCodes.size
        AlertDialog(
            onDismissRequest = { showEntryDownloadBatchConfirmPrompt = false },
            title = {
                Text(
                    if (mode == EntryDownloadBatchMode.REDOWNLOAD) {
                        "Confirm Batch Re-download"
                    } else {
                        "Confirm Batch Download"
                    }
                )
            },
            text = {
                Text(
                    when (mode) {
                        EntryDownloadBatchMode.REDOWNLOAD ->
                            "Do you want to re-download $count entr${if (count == 1) "y" else "ies"}?"
                        else ->
                            "Do you want to download $count entr${if (count == 1) "y" else "ies"}?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEntryDownloadBatchConfirmPrompt = false
                        vm.runEntryDownloadBatch()
                    },
                    enabled = count > 0 && mode != null
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEntryDownloadBatchConfirmPrompt = false }) {
                    Text("No")
                }
            }
        )
    }

    if (showOpenDownloadedFolderPrompt) {
        val detail = pendingDownloadDetail
        AlertDialog(
            onDismissRequest = {
                showOpenDownloadedFolderPrompt = false
                pendingDownloadDetail = null
            },
            title = { Text("Open Download Folder?") },
            text = {
                Text(
                    detail?.let {
                        "Code ${it.code} is already downloaded locally. Do you want to open its download folder?"
                    } ?: "This entry is already downloaded locally."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val current = pendingDownloadDetail
                        showOpenDownloadedFolderPrompt = false
                        if (current != null) {
                            vm.openDownloadedEntryFolder(current.code)
                        }
                        pendingDownloadDetail = null
                    },
                    enabled = detail != null
                ) {
                    Text("Open")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showOpenDownloadedFolderPrompt = false
                        pendingDownloadDetail = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    vm.browserRatingPromptState?.let { prompt ->
        AlertDialog(
            onDismissRequest = vm::skipBrowserRatingPrompt,
            title = { Text("What do you rate this sauce?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${prompt.code} - ${prompt.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (index in 1..5) {
                            val filled = index <= prompt.rating
                            val starInteraction = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable(
                                        interactionSource = starInteraction,
                                        indication = null
                                    ) { vm.updateBrowserRatingSelection(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (filled) "★" else "☆",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (filled) RATING_STAR_GOLD else RATING_STAR_MUTED
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (prompt.wasReadBefore) {
                        Row(
                            modifier = Modifier.clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                vm.updateBrowserRatingReread(!prompt.isReread)
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = prompt.isReread,
                                onCheckedChange = { checked -> vm.updateBrowserRatingReread(checked) }
                            )
                            Text(
                                text = "Re-read",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = vm::skipBrowserRatingPrompt) {
                        Text("Skip")
                    }
                    TextButton(onClick = vm::saveBrowserRatingPrompt) {
                        Text("Save")
                    }
                }
            }
        )
    }

    if (showRecalculateEntryHeatmapWarning) {
        AlertDialog(
            onDismissRequest = { showRecalculateEntryHeatmapWarning = false },
            title = { Text("Recalculate Entry Heatmap") },
            text = {
                Text(
                    "This recalculates and saves the full entry heatmap layout for later reuse. It is hardware intensive, may run hot, and the phone may feel sluggish until it finishes. Search and tag-filtered entry heatmaps will reuse the saved layout afterward."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !vm.entryHeatmapCacheRecalculationRunning,
                    onClick = {
                        showRecalculateEntryHeatmapWarning = false
                        vm.recalculateEntryHeatmapCache()
                    }
                ) {
                    Text("Recalculate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecalculateEntryHeatmapWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (vm.entryHeatmapCacheRecalculationRunning) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Recalculating Entry Heatmap") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = vm.entryHeatmapCacheProgressLabel,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val progress = vm.entryHeatmapCacheProgressFraction
                    if (progress != null) {
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${(progress * 100f).roundToInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Text(
                        text = "This may take a while and the phone can feel hot while the saved layout is being rebuilt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {}
        )
    }

    if (!vm.incognitoModeEnabled) vm.entryHeatmapCacheCompletionSummary?.let { summary ->
        Dialog(
            onDismissRequest = vm::dismissEntryHeatmapCacheCompletionSummary,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 560.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Entry Heatmap Recalculated",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Saved layout for ${summary.entryCount} entries. These dominant families are now the main anchor islands for the cached entry heatmap:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        summary.dominantFamilies.forEachIndexed { index, family ->
                            Text(
                                text = "${index + 1}. $family",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = vm::dismissEntryHeatmapCacheCompletionSummary) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }

    vm.splitPromptState?.let { prompt ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Split numbers detected") },
            text = {
                Text(
                    "Found ${prompt.count} split number groups.\n" +
                        "Examples: ${prompt.preview}\n\n" +
                        "Do you want to combine them into contiguous codes?\n" +
                        "Yes = combine (e.g. '00 00 0' -> '00000')\n" +
                        "No = skip split groups"
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.onSplitPromptAnswered(true) }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.onSplitPromptAnswered(false) }) {
                    Text("No")
                }
            }
        )
    }

    vm.shortPromptState?.let { prompt ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Short codes detected") },
            text = {
                Text(
                    "Found ${prompt.count} codes with fewer than 5 digits.\n" +
                        "Examples: ${prompt.preview}\n\n" +
                        "Skip these short codes?"
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.onShortPromptAnswered(true) }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.onShortPromptAnswered(false) }) {
                    Text("No")
                }
            }
        )
    }

    vm.manualCreatorPromptState?.let { prompt ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Two-word creator name") },
            text = {
                Text(
                    "Input '${prompt.phrase}' has two words.\n\n" +
                        "Treat this as:\n" +
                        "One entry: '${toHyphenatedTwoWordCreatorName(prompt.phrase)}'\n" +
                        "Two entries: '${splitTwoWordCreatorName(prompt.phrase).joinToString("', '")}'"
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.onManualCreatorPromptAnswered(true) }) {
                    Text("One entry")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { vm.onManualCreatorPromptAnswered(false) }) {
                        Text("Two entries")
                    }
                    TextButton(onClick = vm::cancelManualCreatorPrompt) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    vm.batchCreatorPromptState?.let { prompt ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Two-word names in batch") },
            text = {
                Text(
                    "Found ${prompt.count} two-word creator line(s).\n" +
                        "Examples: ${prompt.preview}\n\n" +
                        "Should these be treated as one entry per line (hyphenated) or two separate entries?"
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.onBatchCreatorPromptAnswered(true) }) {
                    Text("One entry")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { vm.onBatchCreatorPromptAnswered(false) }) {
                        Text("Two entries")
                    }
                    TextButton(onClick = vm::cancelBatchCreatorPrompt) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    vm.batchProgressState?.let { state ->
        val progress = if (state.total > 0) {
            state.processed.toFloat() / state.total.toFloat()
        } else {
            0f
        }

        AlertDialog(
            onDismissRequest = {},
            title = { Text(vm.batchDialogTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (state.currentCode != null) {
                            "Fetching code ${state.currentCode}..."
                        } else {
                            "Starting..."
                        }
                    )
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text("${state.processed} / ${state.total}")
                    Text("Saved: ${state.saved}  |  Not found: ${state.notFound}  |  Failed: ${state.failed}")
                }
            },
            confirmButton = {
                TextButton(onClick = vm::cancelBatch) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRefetchAllPrompt) {
        AlertDialog(
            onDismissRequest = {
                showRefetchAllPrompt = false
                vm.setStatus("Re-fetch all cancelled.")
            },
            title = { Text("Re-fetch all entries") },
            text = {
                Text(
                    "This will re-fetch every saved entry from the network and update local metadata.\n\n" +
                        "It can take a while and may feel laggy during the batch, especially with many entries.\n\n" +
                        "Proceed?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRefetchAllPrompt = false
                        vm.refetchAllEntries()
                    }
                ) {
                    Text("Start")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRefetchAllPrompt = false
                        vm.setStatus("Re-fetch all cancelled.")
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    pendingUnhideSuggestionCode?.let { code ->
        AlertDialog(
            onDismissRequest = { pendingUnhideSuggestionCode = null },
            title = { Text("Unhide suggestion") },
            text = { Text("Restore hidden suggestion #$code?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.unhideSuggestedEntry(code)
                        pendingUnhideSuggestionCode = null
                    }
                ) {
                    Text("Unhide")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnhideSuggestionCode = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (!vm.incognitoModeEnabled) suggestedDuplicateComparisonState?.let { comparisonState ->
        val originalState by produceState<Pair<EntryDetail?, Boolean>>(
            initialValue = null to false,
            comparisonState.hint.matchedCode
        ) {
            value = null to false
            val detail = withContext(Dispatchers.IO) {
                vm.getEntryDetail(comparisonState.hint.matchedCode)
            }
            value = detail to true
        }
        val originalDetail = originalState.first
        val originalLoaded = originalState.second

        Dialog(
            onDismissRequest = { suggestedDuplicateComparisonState = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Duplicate Check",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = comparisonState.hint.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Imported original",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                        ) {
                            if (!originalLoaded) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            } else if (originalDetail == null) {
                                Text(
                                    text = "Imported entry #${comparisonState.hint.matchedCode} could not be loaded.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    ThumbnailImage(
                                        thumbnailUrl = originalDetail.thumbnailUrl,
                                        backupCode = originalDetail.code,
                                        contentDescription = "Thumbnail for imported code ${originalDetail.code}",
                                        obscure = vm.incognitoModeEnabled,
                                        modifier = Modifier
                                            .width(92.dp)
                                            .height(124.dp)
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "#${originalDetail.code} - ${originalDetail.title.ifBlank { "Gallery ${originalDetail.code}" }}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Pages: ${originalDetail.numPages} - Uploaded: ${originalDetail.uploadDate.ifBlank { "-" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Read: ${if (originalDetail.isRead) "Yes" else "No"} - Rating: ${originalDetail.rating}/5",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                        )

                        Text(
                            text = "Flagged duplicate",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                ThumbnailImage(
                                    thumbnailUrl = comparisonState.suggestion.thumbnailUrl,
                                    backupCode = comparisonState.suggestion.code,
                                    contentDescription = "Thumbnail for suggested code ${comparisonState.suggestion.code}",
                                    obscure = vm.incognitoModeEnabled,
                                    modifier = Modifier
                                        .width(92.dp)
                                        .height(124.dp)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "#${comparisonState.suggestion.code} - ${comparisonState.suggestion.title}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Pages: ${comparisonState.suggestion.numPages} - Uploaded: ${comparisonState.suggestion.uploadDate.ifBlank { "-" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Score: ${"%.2f".format(Locale.US, comparisonState.suggestion.score)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { suggestedDuplicateComparisonState = null }) {
                                Text("Close")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    vm.hideSuggestedEntry(
                                        comparisonState.suggestion.code,
                                        comparisonState.suggestion.thumbnailUrl
                                    )
                                    suggestedDuplicateComparisonState = null
                                }
                            ) {
                                Text("Hide")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearHiddenSuggestionsPrompt) {
        AlertDialog(
            onDismissRequest = { showClearHiddenSuggestionsPrompt = false },
            title = { Text("Clear hidden suggestions") },
            text = { Text("Clear all hidden suggested entries? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.clearHiddenSuggestedEntries()
                        showClearHiddenSuggestionsPrompt = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHiddenSuggestionsPrompt = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    vm.pinTogglePromptState?.let { prompt ->
        AlertDialog(
            onDismissRequest = vm::dismissPinTogglePrompt,
            title = {
                Text(
                    if (prompt.targetPinned) {
                        "Pin entry"
                    } else {
                        "Unpin entry"
                    }
                )
            },
            text = {
                Text(
                    if (prompt.targetPinned) {
                        "Pin code ${prompt.code}? Pinned entries stay at the top of the list."
                    } else {
                        "Unpin code ${prompt.code}? It will return to normal list ordering."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = vm::confirmPinToggle) {
                    Text(if (prompt.targetPinned) "Pin" else "Unpin")
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissPinTogglePrompt) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete entry") },
            text = {
                Text(
                    vm.selectedCode?.let {
                        if (selectedEntryDownloaded) {
                            "Delete code $it from your local database.\n\nYou can keep the local download, delete both, or remove only the local download."
                        } else {
                            "Delete code $it and its tag links from your local database?"
                        }
                    } ?: "Select an entry first."
                )
            },
            confirmButton = {
                if (selectedEntryDownloaded) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = {
                                    pendingDownloadDetail = vm.selectedDetail
                                    showDeleteConfirm = false
                                    showRemoveLocalDownloadConfirm = true
                                }
                            ) {
                                Text("Remove Local")
                            }
                            TextButton(
                                onClick = {
                                    showDeleteConfirm = false
                                    vm.deleteSelected(removeLocalDownload = false)
                                }
                            ) {
                                Text("Remove Entry")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = {
                                    showDeleteConfirm = false
                                    vm.deleteSelected(removeLocalDownload = true)
                                }
                            ) {
                                Text("Delete Both")
                            }
                            TextButton(
                                onClick = {
                                    showDeleteConfirm = false
                                }
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                } else {
                    TextButton(
                        onClick = {
                            showDeleteConfirm = false
                            vm.deleteSelected()
                        }
                    ) {
                        Text("Delete")
                    }
                }
            },
            dismissButton = {
                if (!selectedEntryDownloaded) {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
            }
        )
    }

    if (showRemoveLocalDownloadConfirm) {
        val detail = pendingDownloadDetail
        AlertDialog(
            onDismissRequest = {
                showRemoveLocalDownloadConfirm = false
                pendingDownloadDetail = null
            },
            title = { Text("Remove local download") },
            text = {
                Text(
                    detail?.let {
                        "Remove the local download for ${it.title.ifBlank { "Gallery ${it.code}" }}?\n\nThis keeps the entry in the app but removes its downloaded files."
                    } ?: "No local download selected."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val current = pendingDownloadDetail
                        showRemoveLocalDownloadConfirm = false
                        pendingDownloadDetail = null
                        if (current != null) {
                            vm.removeDownloadedEntry(current.code)
                        }
                    },
                    enabled = detail != null
                ) {
                    Text("Remove Local")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRemoveLocalDownloadConfirm = false
                        pendingDownloadDetail = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRedownloadLocalConfirm) {
        val detail = pendingDownloadDetail
        AlertDialog(
            onDismissRequest = {
                showRedownloadLocalConfirm = false
                pendingDownloadDetail = null
            },
            title = { Text("Re-download entry") },
            text = {
                Text(
                    detail?.let {
                        "Re-download ${it.title.ifBlank { "Gallery ${it.code}" }}?\n\nThis removes the current local files and replaces them."
                    } ?: "No local download selected."
                )
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    TextButton(
                        onClick = {
                            val initialCode = pendingDownloadDetail?.code
                            showRedownloadLocalConfirm = false
                            pendingDownloadDetail = null
                            vm.startEntryDownloadBatch(EntryDownloadBatchMode.REDOWNLOAD, initialCode)
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Batch Download")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                showRedownloadLocalConfirm = false
                                pendingDownloadDetail = null
                            }
                        ) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = {
                                val current = pendingDownloadDetail
                                showRedownloadLocalConfirm = false
                                pendingDownloadDetail = null
                                if (current != null) {
                                    vm.redownloadEntry(current)
                                }
                            },
                            enabled = detail != null
                        ) {
                            Text("Re-download")
                        }
                    }
                }
            },
            dismissButton = {}
        )
    }

    if (showRefetchConfirm) {
        val code = pendingRefetchCode
        AlertDialog(
            onDismissRequest = {
                showRefetchConfirm = false
                pendingRefetchCode = null
            },
            title = { Text("Re-fetch entry") },
            text = {
                Text(
                    code?.let { "Re-fetch metadata for code $it from the website?" }
                        ?: "No entry selected."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val current = pendingRefetchCode
                        showRefetchConfirm = false
                        pendingRefetchCode = null
                        if (current != null) {
                            vm.refetchCode(current)
                        }
                    },
                    enabled = code != null
                ) {
                    Text("Re-fetch")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRefetchConfirm = false
                        pendingRefetchCode = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearAllPrompt) {
        AlertDialog(
            onDismissRequest = {
                showClearAllPrompt = false
                vm.setStatus("Clear all cancelled.")
            },
            title = { Text("Clear all entries") },
            text = {
                Text(
                    "Do you wish to export before you clear all entries?\n\n" +
                        "Yes: Export metadata, then clear all entries.\n" +
                        "No: Continue without exporting.\n" +
                        "Cancel: Keep everything unchanged."
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showClearAllPrompt = false
                            pendingClearAfterExport = true
                            exportLauncher.launch(vm.defaultExportFilename())
                        }
                    ) {
                        Text("Yes")
                    }
                    TextButton(
                        onClick = {
                            showClearAllPrompt = false
                            vm.setStatus("Clear all cancelled.")
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearAllPrompt = false
                        showClearUnsafePrompt = true
                    }
                ) {
                    Text("No")
                }
            }
        )
    }

    if (showClearUnsafePrompt) {
        AlertDialog(
            onDismissRequest = {
                showClearUnsafePrompt = false
                vm.setStatus("Clear all cancelled.")
            },
            title = { Text("Proceed without export?") },
            text = {
                Text(
                    "Clearing without saving can result in unintended loss of data.\n\n" +
                        "Do you wish to proceed?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearUnsafePrompt = false
                        vm.clearAllWithoutExport()
                    }
                ) {
                    Text("Proceed")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClearUnsafePrompt = false
                        vm.setStatus("Clear all cancelled.")
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                if (graphEntryJumpScrimAlpha > 0.001f) {
                    drawRect(
                        color = graphEntryJumpScrimColor.copy(alpha = graphEntryJumpScrimAlpha)
                    )
                }
            }
    ) {
        vm.startupPreloadState?.takeIf { !vm.appLocked }?.let { state ->
            val progress = if (state.totalSteps > 0) {
                val base = state.completedSteps.toFloat()
                val thumbProgress = if (state.thumbsTotal > 0) {
                    state.thumbsDone.toFloat() / state.thumbsTotal.toFloat()
                } else {
                    0f
                }
                ((base + thumbProgress) / state.totalSteps.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(20f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Loading data in background...",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = state.phase,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${state.completedSteps} / ${state.totalSteps} steps",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (state.thumbsTotal > 0) {
                            Text(
                                text = "Thumbnails: ${state.thumbsDone} / ${state.thumbsTotal}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        vm.entryDownloadBatchProgressState?.let { state ->
            val itemProgress = state.itemFraction?.coerceIn(0f, 1f) ?: 0f
            val progress = if (state.total > 0) {
                ((state.processed.toFloat() + itemProgress) / state.total.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(24f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (state.mode == EntryDownloadBatchMode.REDOWNLOAD) {
                            "Re-downloading entries..."
                        } else {
                            "Downloading entries..."
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = state.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${state.processed.coerceAtMost(state.total)} / ${state.total} entries",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (vm.isEntryDownloadBatchSelecting() && vm.entryDownloadBatchProgressState == null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(24f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${vm.entryDownloadBatchSelectedCodes.size} selected",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { vm.cancelEntryDownloadBatchSelection() }) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = { showEntryDownloadBatchConfirmPrompt = true },
                            enabled = vm.entryDownloadBatchSelectedCodes.isNotEmpty()
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
        if (homeHeatmapSelectionSheetState != null && filteredHeatmapSnapshot != null) {
            val currentHeatmapSnapshot = filteredHeatmapSnapshot!!
            val prevalenceSnapshot = vm.tagGraphSnapshot ?: currentHeatmapSnapshot
            val baseSheetHeightFraction = when (homeHeatmapSelectionSheetState) {
                is GraphSelectionSheetState.Entry -> 0.58f
                is GraphSelectionSheetState.Tag -> 0.54f
                null -> 0.58f
            }
            val homeHeatmapSheetHeight =
                (configuration.screenHeightDp.dp * homeHeatmapSelectionSheetHeightFraction).coerceIn(420.dp, 760.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.18f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { homeHeatmapSelectionSheetState = null }
            ) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(homeHeatmapSheetHeight)
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {}
                    ) {
                        when (val sheet = homeHeatmapSelectionSheetState) {
                            is GraphSelectionSheetState.Entry -> {
                                val detail = vm.getEntryDetail(sheet.entry.code)
                                val dominantNodes = sheet.dominantCircleTags.mapNotNull { dominantTag ->
                                    val normalized = normalizeTagName(dominantTag)
                                    prevalenceSnapshot.nodes.firstOrNull { it.normalizedName == normalized }
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
                                                    val nextFraction = (homeHeatmapSelectionSheetHeightFraction - (dragAmount / 2200f))
                                                        .coerceIn(baseSheetHeightFraction, 0.82f)
                                                    homeHeatmapSelectionSheetHeightFraction = nextFraction
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
                                                text = "Entry from Graph",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            TextButton(onClick = { dismissHomeHeatmapSelectionLayer() }) {
                                                Text("Close")
                                            }
                                        }
                                        GraphTagPrevalenceBar(
                                            nodes = dominantNodes,
                                            snapshot = prevalenceSnapshot,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    DashboardSelectedEntryDetail(
                                        vm = vm,
                                        code = sheet.entry.code,
                                        selectedEntryDownloaded = selectedEntryDownloaded,
                                        actions = dashboardEntryDetailActions,
                                        detail = detail,
                                        enableLibraryRelatedNavigation = false,
                                        seriesNeighbors = if (vm.selectedCode == sheet.entry.code) {
                                            vm.selectedSeriesNeighbors
                                        } else {
                                            SeriesNeighbors()
                                        },
                                        onOpenInBrowser = {
                                            if (vm.selectedCode != sheet.entry.code) {
                                                vm.selectEntry(sheet.entry.code)
                                            }
                                            vm.openSelectedInBrowser()
                                        },
                                        headerCenterText = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                    )
                                }
                            }

                            is GraphSelectionSheetState.Tag -> {
                                val prevalenceNode =
                                    prevalenceSnapshot.nodes.firstOrNull { it.normalizedName == sheet.node.normalizedName }
                                        ?: sheet.node
                                val matchingCodes = currentHeatmapSnapshot.entryNodes
                                    .asSequence()
                                    .filter { sheet.node.normalizedName in it.tagNames }
                                    .map { it.code }
                                    .toSet()
                                val matchingDetails = matchingCodes
                                    .mapNotNull(vm::getEntryDetail)
                                    .sortedByDescending { it.rating }
                                    .sortedByDescending { it.isRead }
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
                                                    val nextFraction = (homeHeatmapSelectionSheetHeightFraction - (dragAmount / 2200f))
                                                        .coerceIn(baseSheetHeightFraction, 0.82f)
                                                    homeHeatmapSelectionSheetHeightFraction = nextFraction
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
                                            TextButton(onClick = { dismissHomeHeatmapSelectionLayer() }) {
                                                Text("Close")
                                            }
                                        }
                                        GraphTagPrevalenceBar(
                                            nodes = listOf(prevalenceNode),
                                            snapshot = prevalenceSnapshot,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(
                                            matchingDetails,
                                            key = { it.code },
                                            contentType = { "home_graph_tag_entry_row" }
                                        ) { detail ->
                                            GraphCompactEntryRow(
                                                detail = detail,
                                                incognitoModeEnabled = vm.incognitoModeEnabled,
                                                averageRating = vm.getAverageEntryRating(detail.code),
                                                onClick = {
                                                    vm.selectEntry(detail.code)
                                                    currentHeatmapSnapshot.entryNodes
                                                        .firstOrNull { it.code == detail.code }
                                                        ?.let { entryNode ->
                                                            homeHeatmapSelectionSheetHeightFraction = homeHeatmapBaseSheetHeightFraction(
                                                                GraphSelectionSheetState.Entry(
                                                                    entry = entryNode,
                                                                    dominantCircleTags = entryNode.dominantCircleTags,
                                                                    returnTagNode = sheet.node
                                                                )
                                                            )
                                                            homeHeatmapSelectionSheetState = GraphSelectionSheetState.Entry(
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

                            null -> Unit
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = creatorJumpContentAlpha)
        ) {
        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.background,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground
            ),
            title = {
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .pointerInput(showSettingsTab, vm.incognitoModeEnabled, vm.legacyHomeUi, homeSurface) {
                            detectTapGestures(
                                onTap = {
                                    scrollActiveHomeSurfaceToTop(animated = true)
                                },
                                onPress = {
                                    scrollActiveHomeSurfaceToTop(animated = false)
                                    tryAwaitRelease()
                                },
                                onDoubleTap = {
                                    vm.onHeaderTitleDoubleTap()
                                }
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = when {
                            showSettingsTab -> "Settings"
                            !vm.legacyHomeUi && homeSurface != HomeSurface.DASHBOARD -> homeSurfaceTitle(homeSurface)
                            vm.cunnyModeActive -> CUNNY_APP_TITLE
                            else -> APP_TITLE
                        },
                        cunnyExempt = true
                    )
                }
            },
            navigationIcon = {
                ThemeToggleWithAccentPicker(
                    themeMode = vm.themeMode,
                    accentMode = vm.accentMode,
                    incognitoModeEnabled = vm.incognitoModeEnabled,
                    cunnyModeActive = vm.cunnyModeActive,
                    onCycleThemeMode = vm::cycleThemeMode,
                    onAccentModeSelected = vm::chooseAccentMode,
                    modifier = Modifier.padding(start = 8.dp)
                )
            },
            actions = {
                IconButton(
                    onClick = {
                        val next = !showSettingsTab
                        showSettingsTab = next
                        if (next) {
                            heatmapOverviewCollapsed = true
                            settingsDataExpanded = false
                            settingsDisplayExpanded = false
                            settingsPersonalizationExpanded = false
                            settingsSecurityExpanded = false
                            settingsStatsExpanded = false
                            showBlockedTagsManager = false
                        }
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = if (vm.cunnyModeActive) "\uD83E\uDD27" else "⚙",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        cunnyExempt = true
                    )
                }
            }
        )

        val useAdaptiveDashboardViewport = !showSettingsTab &&
            !vm.legacyHomeUi &&
            homeSurface == HomeSurface.DASHBOARD
        val dashboardScale = adaptiveDashboardScale(configuration.screenHeightDp)
        val rootListModifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .then(
                if (useAdaptiveDashboardViewport && dashboardScale != 1f) {
                    Modifier.adaptiveDashboardViewport(dashboardScale)
                } else {
                    Modifier
                }
            )

        LazyColumn(
            modifier = rootListModifier,
            state = rootListState,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showSettingsTab) {
                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Data",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                ImmediateActionText(
                                    label = if (settingsDataExpanded) "Collapse" else "Expand",
                                    onAction = { settingsDataExpanded = !settingsDataExpanded },
                                    textStyle = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (settingsDataExpanded) {
                            val backupBusy = vm.backupProgressState != null
                            Button(
                                onClick = {
                                    importLauncher.launch(arrayOf("text/plain", "text/*"))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Import Data")
                            }
                            Button(
                                onClick = {
                                    pendingClearAfterExport = false
                                    exportLauncher.launch(vm.defaultExportFilename())
                                },
                                enabled = !backupBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Export Data")
                            }
                            Button(
                                onClick = {
                                    csvExportLauncher.launch(vm.defaultCsvExportFilename())
                                },
                                enabled = !backupBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Export CSV")
                            }
                            Button(
                                onClick = { backupFolderLauncher.launch(null) },
                                enabled = !backupBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Set Procedural Backup Folder")
                            }
                            Button(
                                onClick = { downloadFolderLauncher.launch(null) },
                                enabled = !backupBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Set Gallery Downloads Folder")
                            }
                            Button(
                                onClick = {
                                    context.startActivity(
                                        ExperimentalGalleryActivity.createIntent(context)
                                    )
                                },
                                enabled = !backupBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Experimental Gallery")
                            }
                            Button(
                                onClick = vm::backupNow,
                                enabled = !backupBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Backup Now")
                            }
                            Button(
                                onClick = { showRecalculateEntryHeatmapWarning = true },
                                enabled = !backupBusy && !vm.entryHeatmapCacheRecalculationRunning,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (vm.entryHeatmapCacheRecalculationRunning) {
                                        "Recalculating Entry Heatmap..."
                                    } else {
                                        "Recalculate Entry Heatmap"
                                    }
                                )
                            }
                            Button(
                                onClick = {
                                    if (vm.backupThumbnailArchiveEnabled) {
                                        vm.updateBackupThumbnailArchiveEnabled(false)
                                    } else {
                                        showBackupThumbnailArchiveWarning = true
                                    }
                                },
                                enabled = vm.autoBackupTreeUri.isNotBlank() && !backupBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (vm.backupThumbnailArchiveEnabled) {
                                        "Store Thumbnails Locally In Backup: On"
                                    } else {
                                        "Store Thumbnails Locally In Backup: Off"
                                    }
                                )
                            }
                            vm.backupProgressState?.let { progress ->
                                val fraction = if (progress.total > 0) {
                                    (progress.processed.toFloat() / progress.total.toFloat()).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = progress.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (progress.total > 0) {
                                        LinearProgressIndicator(
                                            progress = { fraction },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    }
                                    Text(
                                        text = if (progress.total > 0) {
                                            "Processed ${progress.processed} / ${progress.total} covers"
                                        } else {
                                            "Preparing backup archive..."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "New or updated: ${progress.written}  |  Reused: ${progress.reused}  |  Failed: ${progress.failed}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = "Backup folder: ${vm.autoBackupFolderLabel()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Downloads folder: ${vm.galleryDownloadFolderLabel()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "On app exit, backups are updated inside a SauceTracker Backup subfolder here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = vm.entryHeatmapCacheStatusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = vm::cycleSubscriptionRefreshInterval,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Subscription Check Interval: ${vm.formatSubscriptionRefreshInterval()}")
                            }
                            Text(
                                text = "Background subscription refresh checks subscribed tags and artists/groups on this interval when Android allows scheduled work.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Entry heatmap layout is now a saved cache. It is reused until you explicitly recalculate it after local library changes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (vm.autoBackupTreeUri.isBlank()) {
                                    "Set a procedural backup folder before enabling the thumbnail archive."
                                } else if (vm.backupThumbnailArchiveEnabled) {
                                    "Thumbnail covers and duplicate hashes will also be mirrored into that SauceTracker Backup subfolder."
                                } else {
                                    "Optional: also store cover thumbnails and duplicate hashes in that SauceTracker Backup subfolder for faster browser duplicate checks."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = vm::toggleDesktopBridge,
                                enabled = !vm.incognitoModeEnabled || vm.desktopBridgeRunning,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (vm.desktopBridgeRunning) {
                                        "Stop Desktop Bridge"
                                    } else {
                                        "Start Desktop Bridge"
                                    }
                                )
                            }
                            if (vm.desktopBridgeRunning) {
                                Button(
                                    onClick = vm::copyDesktopBridgeUrlToClipboard,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Copy Desktop Bridge URL")
                                }
                            }
                            Text(
                                text = if (vm.desktopBridgeRunning && vm.desktopBridgeUrl.isNotBlank()) {
                                    "Desktop URL: ${vm.desktopBridgeUrl}"
                                } else {
                                    "Desktop URL: Not running"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (vm.desktopBridgeRunning) {
                                    "Bridge access code: ${vm.desktopBridgeChallengeCode}"
                                } else {
                                    "Bridge access code: --"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Desktop bridge works on the same local network. Keep Sauce Tracker open while using desktop.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (vm.incognitoModeEnabled) {
                                Text(
                                    text = "Desktop bridge start is blocked while incognito mode is enabled.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val hiddenSuggestionCodes = vm.hiddenSuggestedEntries()
                            val hiddenSuggestionsMasked = vm.incognitoModeEnabled
                            LaunchedEffect(hiddenSuggestionCodes, hiddenSuggestionsMasked) {
                                if (hiddenSuggestionCodes.isEmpty() || hiddenSuggestionsMasked) {
                                    hiddenSuggestionsDropdownExpanded = false
                                }
                            }
                            Text(
                                text = if (hiddenSuggestionCodes.isEmpty()) {
                                    "Hidden suggested entries: None"
                                } else if (hiddenSuggestionsMasked) {
                                    "Hidden suggested entries: Hidden in incognito mode"
                                } else {
                                    "Hidden suggested entries: ${hiddenSuggestionCodes.size}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (hiddenSuggestionCodes.isNotEmpty() && !hiddenSuggestionsMasked) {
                                Button(
                                    onClick = {
                                        hiddenSuggestionsDropdownExpanded = !hiddenSuggestionsDropdownExpanded
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (hiddenSuggestionsDropdownExpanded) {
                                            "Hide Hidden Suggestions"
                                        } else {
                                            "Manage Hidden Suggestions"
                                        }
                                    )
                                }
                                if (hiddenSuggestionsDropdownExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                                shape = MaterialTheme.shapes.medium
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                                shape = MaterialTheme.shapes.medium
                                            )
                                            .padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(onClick = { showClearHiddenSuggestionsPrompt = true }) {
                                                Text("Clear Hidden Suggestions")
                                            }
                                        }
                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 120.dp, max = 260.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            items(
                                                hiddenSuggestionCodes,
                                                key = { it },
                                                contentType = { "hidden_suggestion_code" }
                                            ) {
                                                val code = it
                                                val thumbnailUrl = vm.hiddenSuggestedThumbnailUrl(code)
                                                val hiddenAtLabel = vm.hiddenSuggestedAtLabel(code)
                                                LaunchedEffect(code, thumbnailUrl) {
                                                    if (thumbnailUrl.isBlank()) {
                                                        vm.prefetchHiddenSuggestedThumbnail(code)
                                                    }
                                                }
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        if (thumbnailUrl.isNotBlank()) {
                                                            ThumbnailImage(
                                                                thumbnailUrl = thumbnailUrl,
                                                                backupCode = code,
                                                                contentDescription = "Hidden suggestion cover for code $code",
                                                                modifier = Modifier
                                                                    .width(22.dp)
                                                                    .height(30.dp)
                                                                    .clip(RoundedCornerShape(5.dp))
                                                                    .border(
                                                                        width = 1.dp,
                                                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                                                        shape = RoundedCornerShape(5.dp)
                                                                    )
                                                            )
                                                        } else {
                                                            Box(
                                                                modifier = Modifier
                                                                    .width(22.dp)
                                                                    .height(30.dp)
                                                                    .clip(RoundedCornerShape(5.dp))
                                                                    .background(MaterialTheme.colorScheme.surface)
                                                                    .border(
                                                                        width = 1.dp,
                                                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                                                        shape = RoundedCornerShape(5.dp)
                                                                    ),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = "#",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                        Column(
                                                            verticalArrangement = Arrangement.spacedBy(1.dp)
                                                        ) {
                                                            Text(
                                                                text = "#$code",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                            if (hiddenAtLabel.isNotBlank()) {
                                                                Text(
                                                                    text = "Hidden $hiddenAtLabel UTC",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    }
                                                    TextButton(
                                                        onClick = { pendingUnhideSuggestionCode = code },
                                                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
                                                    ) {
                                                        Text("Unhide", style = MaterialTheme.typography.labelMedium)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Button(
                                onClick = { showClearAllPrompt = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Clear All Entries")
                            }
                            Button(
                                onClick = { showRefetchAllPrompt = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Re-fetch All Entries")
                            }
                            }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                                thickness = 1.dp
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Personalization",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                ImmediateActionText(
                                    label = if (settingsPersonalizationExpanded) "Collapse" else "Expand",
                                    onAction = { settingsPersonalizationExpanded = !settingsPersonalizationExpanded },
                                    textStyle = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (settingsPersonalizationExpanded) {
                            Text(
                                text = "Entries mode cycle: ${vm.entryReadFilterCycleSummary()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Default entry sort: ${describeEntrySort(vm.defaultEntrySortField, vm.defaultEntrySortDirection)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Default tag sort: ${describeTagSort(vm.defaultTagSortField, vm.defaultTagSortDirection)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Default artist/group sort: ${describeCreatorSort(vm.defaultCreatorSortField, vm.defaultCreatorSortDirection)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Home page order: ${vm.homeSectionOrderSummary()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { showPersonalizationDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open Personalization")
                            }
                            }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                                thickness = 1.dp
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Display",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                ImmediateActionText(
                                    label = if (settingsDisplayExpanded) "Collapse" else "Expand",
                                    onAction = { settingsDisplayExpanded = !settingsDisplayExpanded },
                                    textStyle = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (settingsDisplayExpanded) {
                            Button(
                                onClick = vm::toggleLegacyHomeUi,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (vm.legacyHomeUi) {
                                        "Home UI: Legacy"
                                    } else {
                                        "Home UI: Dashboard"
                                    }
                                )
                            }
                            Button(
                                onClick = {
                                    if (vm.showThumbnails && vm.pureGalleryMode) {
                                        showDisableThumbnailsForGalleryPrompt = true
                                    } else {
                                        vm.toggleThumbnailsEnabled()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (vm.showThumbnails) {
                                        "Thumbnails: On"
                                    } else {
                                        "Thumbnails: Off"
                                    }
                                )
                            }
                            Button(
                                onClick = vm::toggleAdaptiveScrollThumbnails,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (vm.adaptiveScrollThumbnails) {
                                        "Reduced Scroll Thumbnails: On"
                                    } else {
                                        "Reduced Scroll Thumbnails: Off"
                                    }
                                )
                            }
                            Button(
                                onClick = vm::togglePerformanceOverlay,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (vm.performanceOverlayEnabled) {
                                        "Performance Overlay: On"
                                    } else {
                                        "Performance Overlay: Off"
                                    }
                                )
                            }
                            Button(
                                onClick = {
                                    vm.loadEntryLayoutPreviewSamples()
                                    showEntryLayoutDialog = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Set Entry Layout"
                                )
                            }
                            Text(
                                text = if (vm.pureGalleryMode) {
                                    "Current layout: Gallery (${vm.galleryColumns} across)"
                                } else {
                                    "Current layout: Normal (${vm.galleryColumns} across)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    slideshowHorizontalDirection =
                                        if (slideshowHorizontalDirection == SlideshowHorizontalDirection.WESTERN) {
                                            SlideshowHorizontalDirection.MANGA
                                        } else {
                                            SlideshowHorizontalDirection.WESTERN
                                        }
                                    storeSlideshowHorizontalDirection(context, slideshowHorizontalDirection)
                                    vm.setStatus(
                                        "Horizontal slideshow direction: ${
                                            if (slideshowHorizontalDirection == SlideshowHorizontalDirection.WESTERN) {
                                                "Western"
                                            } else {
                                                "Manga"
                                            }
                                        }"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (slideshowHorizontalDirection == SlideshowHorizontalDirection.WESTERN) {
                                        "Horizontal Slideshow Direction: Western"
                                    } else {
                                        "Horizontal Slideshow Direction: Manga"
                                    }
                                )
                            }
                            Button(
                                onClick = {
                                    showBlockedTagsManager = !showBlockedTagsManager
                                    if (showBlockedTagsManager) {
                                        vm.ensurePopularTagsLoaded(fetchIfEmpty = true)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (showBlockedTagsManager) "Hide Block Tags" else "Block Tags")
                            }
                            if (showBlockedTagsManager) {
                                val blockedTagsSearchTrimmed = blockedTagsSearchQuery.trim()
                                val blockedTagsSearchTokens = blockedTagsSearchTrimmed
                                    .lowercase(Locale.US)
                                    .split(Regex("\\s+"))
                                    .filter { it.isNotBlank() }
                                val filteredPopularTags = if (blockedTagsSearchTokens.isEmpty()) {
                                    vm.popularTags
                                } else {
                                    vm.popularTags.filter { tag ->
                                        val haystack = "${tag.name} ${tag.type}".lowercase(Locale.US)
                                        blockedTagsSearchTokens.all { token -> haystack.contains(token) }
                                    }
                                }

                                Button(
                                    onClick = vm::fetchAllPopularTags,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !vm.popularTagsFetchInProgress
                                ) {
                                    Text(
                                        if (vm.popularTagsFetchInProgress) {
                                            "Fetching Popular Tags..."
                                        } else {
                                            "Fetch All Tags"
                                        }
                                    )
                                }
                                Button(
                                    onClick = vm::toggleApplyBlockedTagsToHome,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (vm.applyBlockedTagsToHome) {
                                            "Apply Blocked Tags To Home: On"
                                        } else {
                                            "Apply Blocked Tags To Home: Off"
                                        }
                                    )
                                }
                                Button(
                                    onClick = vm::toggleApplyBlockedTagsToSearchTerms,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (vm.applyBlockedTagsToSearchTerms) {
                                            "Apply Blocked Tags To Search Terms: On"
                                        } else {
                                            "Apply Blocked Tags To Search Terms: Off"
                                        }
                                    )
                                }
                                Text(
                                    text = "Blocked tags: ${vm.blockedTagsSummary()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                                OutlinedTextField(
                                    value = blockedTagsSearchQuery,
                                    onValueChange = { blockedTagsSearchQuery = it },
                                    label = { Text("Search blocked tags") },
                                    placeholder = { Text("Search is empty") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item {
                                        EntrySortChip(
                                            label = "Name${blockedTagSortArrow(vm, TagSortField.NAME)}",
                                            selected = vm.blockedTagSortField == TagSortField.NAME,
                                            activeDirection = if (vm.blockedTagSortField == TagSortField.NAME) vm.blockedTagSortDirection else null,
                                            onClick = { vm.onBlockedTagSortClicked(TagSortField.NAME) }
                                        )
                                    }
                                    item {
                                        EntrySortChip(
                                            label = "Type${blockedTagSortArrow(vm, TagSortField.TYPE)}",
                                            selected = vm.blockedTagSortField == TagSortField.TYPE,
                                            activeDirection = if (vm.blockedTagSortField == TagSortField.TYPE) vm.blockedTagSortDirection else null,
                                            onClick = { vm.onBlockedTagSortClicked(TagSortField.TYPE) }
                                        )
                                    }
                                    item {
                                        EntrySortChip(
                                            label = "Count${blockedTagSortArrow(vm, TagSortField.COUNT)}",
                                            selected = vm.blockedTagSortField == TagSortField.COUNT,
                                            activeDirection = if (vm.blockedTagSortField == TagSortField.COUNT) vm.blockedTagSortDirection else null,
                                            onClick = { vm.onBlockedTagSortClicked(TagSortField.COUNT) }
                                        )
                                    }
                                    item {
                                        ImmediateActionText(
                                            label = "Reset Filter",
                                            onAction = vm::clearBlockedTags,
                                            textStyle = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                if (vm.popularTags.isEmpty()) {
                                    Text(
                                        text = "No cached popular tags yet. Tap Fetch All Tags.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else if (filteredPopularTags.isEmpty()) {
                                    Text(
                                        text = "No tags match your blocked-tag search.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 180.dp, max = 340.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(
                                            filteredPopularTags,
                                            key = { it.id },
                                            contentType = { "blocked_tag_row" }
                                        ) { tag ->
                                            val rowInteraction = remember { MutableInteractionSource() }
                                            val containerColor = if (tag.blocked) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(containerColor, shape = MaterialTheme.shapes.small)
                                                    .clickable(
                                                        interactionSource = rowInteraction,
                                                        indication = null
                                                    ) { vm.togglePopularTagBlocked(tag.id) }
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = tag.name,
                                                    modifier = Modifier.weight(0.50f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = tag.type,
                                                    modifier = Modifier.weight(0.26f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = tag.count.toString(),
                                                    modifier = Modifier.weight(0.24f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (tag.blocked) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Button(
                                onClick = vm::togglePreloadOnLaunch,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (vm.preloadOnLaunch) {
                                        "Load Data On Launch: On"
                                    } else {
                                        "Load Data On Launch: Off"
                                    }
                                )
                            }
                            Text(
                                text = "Launch preload amount: ${vm.preloadPercent}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = vm.preloadPercent.toFloat(),
                                onValueChange = { vm.updatePreloadPercent(it.roundToInt()) },
                                valueRange = 0f..100f
                            )
                            Button(
                                onClick = {
                                    selectedTagGraphTab = TagGraphTab.HEATMAP
                                    selectedTagHeatmapDisplayMode = TagHeatmapDisplayMode.TAGS
                                    showTagGraphDialog = true
                                    vm.prepareTagGraphData()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open Graph")
                            }
                            Text(
                                text = "Turn thumbnails off for smoother scrolling on slower phones.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "When enabled, app startup preloads data (and thumbnails if enabled) in the background and shows a progress bar at the bottom for smoother browsing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                                thickness = 1.dp
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Security",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                ImmediateActionText(
                                    label = if (settingsSecurityExpanded) "Collapse" else "Expand",
                                    onAction = { settingsSecurityExpanded = !settingsSecurityExpanded },
                                    textStyle = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (settingsSecurityExpanded) {
                            Button(
                                onClick = {
                                    if (vm.appLockEnabled) {
                                        showDisableAppLockPrompt = true
                                    } else {
                                        showEnableAppLockDialog = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (vm.appLockEnabled) "App Lock: On" else "App Lock: Off")
                            }
                            if (vm.appLockEnabled) {
                                Button(
                                    onClick = { showChangePinDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Change PIN")
                                }
                                Button(
                                    onClick = {
                                        vm.chooseAppLockBiometricEnabled(!vm.appLockBiometricEnabled)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (vm.appLockBiometricEnabled) {
                                            "Biometric Unlock: On"
                                        } else {
                                            "Biometric Unlock: Off"
                                        }
                                    )
                                }
                                Text(
                                    text = "App lock appears on app open. After background/close, PIN is required again after 30 seconds.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                                thickness = 1.dp
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Stats",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                ImmediateActionText(
                                    label = if (settingsStatsExpanded) "Collapse" else "Expand",
                                    onAction = {
                                        settingsStatsExpanded = !settingsStatsExpanded
                                        if (settingsStatsExpanded) {
                                            vm.ensureReadAnalyticsLoaded(forceRefresh = false)
                                        }
                                    },
                                    textStyle = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (settingsStatsExpanded) {
                            val readCount = vm.readAnalytics.readCounts[selectedStatsRange] ?: 0
                            val pagesRead = vm.readAnalytics.pagesRead[selectedStatsRange] ?: 0
                            val averageRating = vm.readAnalytics.averageRatings[selectedStatsRange] ?: 0f
                            val topTags = vm.readAnalytics.topTags[selectedStatsRange].orEmpty()
                            val topCreators = vm.readAnalytics.topCreators[selectedStatsRange].orEmpty()
                            val dailyActivity = vm.readAnalytics.dailyActivity[selectedStatsRange].orEmpty()
                            val speedStats = vm.readAnalytics.readingSpeed[selectedStatsRange] ?: ReadingSpeedStats()
                            val coverage = if (vm.savedStats.entries > 0) {
                                (readCount * 100f / vm.savedStats.entries.toFloat()).coerceIn(0f, 100f)
                            } else {
                                0f
                            }
                            val statsObscured = vm.incognitoModeEnabled
                            val statsPrivacyOverlay = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)

                            if (vm.readAnalyticsLoading) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(StatsRange.entries, key = { it.name }) { range ->
                                        FilterChip(
                                            selected = selectedStatsRange == range,
                                            onClick = { selectedStatsRange = range },
                                            label = { Text(range.label) }
                                        )
                                    }
                                }
                                Text(
                                    text = "Read (${selectedStatsRange.label}): $readCount",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.privacyObfuscate(
                                        enabled = statsObscured,
                                        overlayColor = statsPrivacyOverlay
                                    )
                                )
                                Text(
                                    text = "Pages read (${selectedStatsRange.label}): $pagesRead",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.privacyObfuscate(
                                        enabled = statsObscured,
                                        overlayColor = statsPrivacyOverlay
                                    )
                                )
                                Text(
                                    text = "Read coverage (all entries): ${String.format(Locale.US, "%.1f", coverage)}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.privacyObfuscate(
                                        enabled = statsObscured,
                                        overlayColor = statsPrivacyOverlay
                                    )
                                )
                                Text(
                                    text = "Avg rating on read entries: ${
                                        if (averageRating > 0f) {
                                            String.format(Locale.US, "%.2f/5", averageRating)
                                        } else {
                                            "-"
                                        }
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.privacyObfuscate(
                                        enabled = statsObscured,
                                        overlayColor = statsPrivacyOverlay
                                    )
                                )
                                Text(
                                    text = "Top read tags (${selectedStatsRange.label}):",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.privacyObfuscate(
                                        enabled = statsObscured,
                                        overlayColor = statsPrivacyOverlay
                                    )
                                )
                                if (topTags.isEmpty()) {
                                    Text(
                                        text = "No read-tag data yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.privacyObfuscate(
                                            enabled = statsObscured,
                                            overlayColor = statsPrivacyOverlay
                                        )
                                    )
                                } else {
                                    topTags.forEachIndexed { index, row ->
                                        Text(
                                            text = "${index + 1}. ${row.name} (${row.type}) - ${row.count}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.privacyObfuscate(
                                                enabled = statsObscured,
                                                overlayColor = statsPrivacyOverlay
                                            )
                                        )
                                    }
                                }
                                Text(
                                    text = "Top read artists/groups (${selectedStatsRange.label}):",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.privacyObfuscate(
                                        enabled = statsObscured,
                                        overlayColor = statsPrivacyOverlay
                                    )
                                )
                                if (topCreators.isEmpty()) {
                                    Text(
                                        text = "No read creator data yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.privacyObfuscate(
                                            enabled = statsObscured,
                                            overlayColor = statsPrivacyOverlay
                                        )
                                    )
                                } else {
                                    topCreators.forEachIndexed { index, row ->
                                        Text(
                                            text = "${index + 1}. ${row.name} (${row.type}) - ${row.count}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.privacyObfuscate(
                                                enabled = statsObscured,
                                                overlayColor = statsPrivacyOverlay
                                            )
                                        )
                                    }
                                }
                                Text(
                                    text = "Activity",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.privacyObfuscate(
                                        enabled = statsObscured,
                                        overlayColor = statsPrivacyOverlay
                                    )
                                )
                                ActivityHeatmap(
                                    range = selectedStatsRange,
                                    points = dailyActivity,
                                    onDaySelected = { selectedActivityPoint = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .privacyObfuscate(
                                            enabled = statsObscured,
                                            overlayColor = statsPrivacyOverlay
                                        )
                                )
                                Text(
                                    text = "Reading speed",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.privacyObfuscate(
                                        enabled = statsObscured,
                                        overlayColor = statsPrivacyOverlay
                                    )
                                )
                                if (speedStats.hasEnoughData) {
                                    Text(
                                        text = "Avg pages/min (${selectedStatsRange.label}): ${
                                            String.format(Locale.US, "%.2f", speedStats.pagesPerMinute)
                                        }",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.privacyObfuscate(
                                            enabled = statsObscured,
                                            overlayColor = statsPrivacyOverlay
                                        )
                                    )
                                    Text(
                                        text = "Total reading time (${selectedStatsRange.label}): ${
                                            formatDurationFromSeconds(speedStats.totalSecondsElapsed)
                                        }",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.privacyObfuscate(
                                            enabled = statsObscured,
                                            overlayColor = statsPrivacyOverlay
                                        )
                                    )
                                    Text(
                                        text = "Total pages viewed (${selectedStatsRange.label}): ${speedStats.totalPagesViewed}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.privacyObfuscate(
                                            enabled = statsObscured,
                                            overlayColor = statsPrivacyOverlay
                                        )
                                    )
                                } else {
                                    Text(
                                        text = "Avg pages/min (${selectedStatsRange.label}): --",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.privacyObfuscate(
                                            enabled = statsObscured,
                                            overlayColor = statsPrivacyOverlay
                                        )
                                    )
                                    Text(
                                        text = "Total reading time (${selectedStatsRange.label}): --",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.privacyObfuscate(
                                            enabled = statsObscured,
                                            overlayColor = statsPrivacyOverlay
                                        )
                                    )
                                    Text(
                                        text = "Total pages viewed (${selectedStatsRange.label}): --",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.privacyObfuscate(
                                            enabled = statsObscured,
                                            overlayColor = statsPrivacyOverlay
                                        )
                                    )
                                    Text(
                                        text = "Not enough reading data yet to calculate speed.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.privacyObfuscate(
                                            enabled = statsObscured,
                                            overlayColor = statsPrivacyOverlay
                                        )
                                    )
                                }
                            }
                            Button(
                                onClick = { vm.ensureReadAnalyticsLoaded(forceRefresh = true) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Refresh Stats")
                            }
                            }
                            }
                            val localBackupEstimate by produceState<BackupThumbnailStorageEstimate?>(initialValue = null, vm.autoBackupTreeUri, vm.backupThumbnailArchiveEnabled, vm.savedStats.entries) {
                                value = if (vm.autoBackupTreeUri.isBlank()) {
                                    null
                                } else {
                                    withContext(Dispatchers.IO) {
                                        runCatching { vm.calculateBackupThumbnailArchiveEstimate() }.getOrNull()
                                    }
                                }
                            }
                            Text(
                                text = vm.statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Saved totals: ${vm.savedStats.entries} entries, ${vm.savedStats.artists} artists, ${vm.savedStats.groups} groups, ${vm.savedStats.readEntries} read",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.privacyObfuscate(
                                    enabled = vm.incognitoModeEnabled,
                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                )
                            )
                            Text(
                                text = when {
                                    !vm.backupThumbnailArchiveEnabled -> "Local backup mirror: disabled"
                                    vm.autoBackupTreeUri.isBlank() -> "Local backup mirror: no folder set"
                                    localBackupEstimate != null -> {
                                        val estimate = localBackupEstimate ?: BackupThumbnailStorageEstimate(0, 0, 0L)
                                        "Drawing from local backup first: ${estimate.storedCount} / ${estimate.entryCount} cover thumbnails mirrored locally"
                                    }
                                    else -> "Local backup mirror: checking..."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.privacyObfuscate(
                                    enabled = vm.incognitoModeEnabled,
                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                )
                            )
                        }
                    }
                }
            } else {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            val searchEmptyDisplay = if (vm.incognitoModeEnabled) {
                                "••••••••••"
                            } else {
                                "Search is empty"
                            }
                            val displayedSearchValue = if (vm.codeInput.isNotBlank() || searchFieldFocused) {
                                vm.codeInput
                            } else {
                                searchEmptyDisplay
                            }

                            OutlinedTextField(
                                value = displayedSearchValue,
                                onValueChange = vm::updateUnifiedInput,
                                label = { Text("Search everything") },
                                singleLine = false,
                                minLines = 1,
                                maxLines = 4,
                                visualTransformation = if (vm.incognitoModeEnabled && (vm.codeInput.isNotBlank() || searchFieldFocused)) {
                                    PasswordVisualTransformation()
                                } else {
                                    VisualTransformation.None
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Search
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = { vm.openUnifiedInputInBrowser() }
                                ),
                                trailingIcon = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        if (vm.codeInput.trim().isNotEmpty()) {
                                            Text(
                                                text = "Showing:$searchEverythingShowingCount",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            IconButton(onClick = vm::clearEntrySearch) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_clear_circle_24),
                                                    contentDescription = "Clear search",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                        IconButton(onClick = vm::pasteCodeInputFromClipboard) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_content_paste_24),
                                                contentDescription = "Paste from clipboard",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = vm::openUnifiedInputInBrowser
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_open_in_new_24),
                                                contentDescription = "Open current input/tags in browser",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(
                                    topStart = 14.dp,
                                    topEnd = 14.dp,
                                    bottomStart = 0.dp,
                                    bottomEnd = 0.dp
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        searchFieldFocused = focusState.isFocused
                                    }
                            )
                            if (vm.experimentalFilterStatusStrip) {
                                ExperimentalTagFilterChipField(
                                    chips = vm.activeTagFilterChips(),
                                    showingCount = tagFilterShowingEntriesCount,
                                    incognitoModeEnabled = vm.incognitoModeEnabled,
                                    onRemoveTag = vm::toggleTagFilter,
                                    onClearAll = vm::clearTagFilter,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                OutlinedTextField(
                                    value = vm.activeFilterLabel(),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Tag filter") },
                                    singleLine = false,
                                    minLines = 1,
                                    maxLines = 4,
                                    visualTransformation = if (vm.incognitoModeEnabled) {
                                        PasswordVisualTransformation()
                                    } else {
                                        VisualTransformation.None
                                    },
                                    trailingIcon = if (vm.activeTagFilterIds.isNotEmpty()) {
                                        {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Showing:$tagFilterShowingEntriesCount",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                IconButton(onClick = vm::clearTagFilter) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_clear_circle_24),
                                                        contentDescription = "Clear tag filter",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        null
                                    },
                                    shape = RoundedCornerShape(
                                        topStart = 0.dp,
                                        topEnd = 0.dp,
                                        bottomStart = 14.dp,
                                        bottomEnd = 14.dp
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = vm::addOrUpdateByInput,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Add / Update")
                            }
                            Button(
                                onClick = { batchLauncher.launch(arrayOf("text/plain", "text/*")) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Batch Add/Update")
                            }
                        }

                        Text(
                            text = vm.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!vm.legacyHomeUi && homeSurface == HomeSurface.DASHBOARD) {
                item {
                    DashboardBodyReveal(revealKey = homeSurface) {
                        ModernHomeDashboard(
                            vm = vm,
                            dashboardVisitNonce = dashboardVisitNonce,
                            onOpenEntries = {
                                heatmapOverviewCollapsed = true
                                vm.expandEntriesSection()
                                switchHomeSurface(HomeSurface.ENTRIES)
                            },
                            onOpenTags = {
                                heatmapOverviewCollapsed = true
                                vm.expandTagsSection()
                                switchHomeSurface(HomeSurface.TAGS)
                            },
                            onOpenCreators = {
                                heatmapOverviewCollapsed = true
                                vm.expandCreatorsSection()
                                switchHomeSurface(HomeSurface.CREATORS)
                            },
                            onOpenSubscriptions = {
                                heatmapOverviewCollapsed = true
                                if (vm.subscriptionsCardCollapsed) {
                                    vm.toggleSubscriptionsCardCollapsed()
                                }
                                switchHomeSurface(HomeSurface.SUBSCRIPTIONS)
                            },
                            onOpenSubscriptionsList = {
                                heatmapOverviewCollapsed = true
                                showSubscriptionsListDialog = true
                            },
                            onOpenHeatmap = {
                                heatmapOverviewCollapsed = false
                                switchHomeSurface(HomeSurface.HEATMAP)
                            },
                            onOpenHistory = {
                                heatmapOverviewCollapsed = true
                                vm.ensureReadAnalyticsLoaded(forceRefresh = false)
                                switchHomeSurface(HomeSurface.HISTORY)
                            },
                            onOpenSuggestions = {
                                heatmapOverviewCollapsed = true
                                if (vm.suggestedEntriesCollapsed) {
                                    vm.toggleSuggestedEntriesCollapsed()
                                }
                                switchHomeSurface(HomeSurface.SUGGESTED)
                            },
                            onOpenEntry = { code ->
                                heatmapOverviewCollapsed = true
                                if (vm.suggestedEntries.any { it.code == code }) {
                                    pendingSuggestedScrollCode = code
                                    if (vm.suggestedEntriesCollapsed) {
                                        vm.toggleSuggestedEntriesCollapsed()
                                    }
                                    switchHomeSurface(HomeSurface.SUGGESTED, restoreScroll = false)
                                } else {
                                    vm.expandEntriesSection()
                                    switchHomeSurface(HomeSurface.ENTRIES, restoreScroll = false)
                                    selectEntryFromRow(code)
                                }
                            },
                            onOpenRandomEntry = { code ->
                                heatmapOverviewCollapsed = true
                                vm.expandEntriesSection()
                                switchHomeSurface(HomeSurface.ENTRIES, restoreScroll = false)
                                vm.openSeriesEntry(code)
                            },
                            onEntriesLongPress = if (vm.experimentalDashboardLongPress) {
                                { showEntryModeCycleDialog = true }
                            } else {
                                null
                            },
                            onTagsLongPress = if (vm.experimentalDashboardLongPress) {
                                { personalizationSortTarget = PersonalizationSortTarget.TAGS }
                            } else {
                                null
                            },
                            onCreatorsLongPress = if (vm.experimentalDashboardLongPress) {
                                { personalizationSortTarget = PersonalizationSortTarget.CREATORS }
                            } else {
                                null
                            }
                        )
                    }
                }
            }

            if (!vm.legacyHomeUi && homeSurface == HomeSurface.HISTORY) {
                item {
                    DashboardBodyReveal(revealKey = homeSurface) {
                        ReadingHistorySummaryCard(
                            analyticsSnapshot = vm.readAnalytics,
                            analyticsLoading = vm.readAnalyticsLoading,
                            incognitoModeEnabled = vm.incognitoModeEnabled,
                            selectedRange = historyStatsRange,
                            onSelectedRangeChange = { historyStatsRange = it },
                            onRefresh = { vm.ensureReadAnalyticsLoaded(forceRefresh = true) },
                        )
                    }
                }
                item {
                    DashboardBodyReveal(revealKey = homeSurface) {
                        ReadingHistoryActivityMapCard(
                            range = historyStatsRange,
                            points = vm.readAnalytics.dailyActivity[historyStatsRange].orEmpty(),
                            incognitoModeEnabled = vm.incognitoModeEnabled,
                            onDaySelected = ::selectHistoryDay
                        )
                    }
                }
                item {
                    val activeDays = vm.readAnalytics.dailyActivity[historyStatsRange].orEmpty()
                        .filter { it.pagesRead > 0 || it.entriesRead > 0 }
                        .sortedByDescending { it.date }
                        .take(8)
                    DashboardBodyReveal(revealKey = homeSurface) {
                        ReadingHistoryRecentDaysCard(
                            activeDays = activeDays,
                            incognitoModeEnabled = vm.incognitoModeEnabled,
                            onDaySelected = ::selectHistoryDay
                        )
                    }
                }
            }

            if (!vm.legacyHomeUi && homeSurface == HomeSurface.TAGS) {
                item {
                    DashboardBodyReveal(revealKey = homeSurface) {
                        DashboardTagsSection(
                            vm = vm,
                            listState = tagsListState,
                            onNotificationPermissionRequired = ::requestNotificationPermissionIfNeeded,
                            onConfigureSubscription = { type, name ->
                                vm.subscriptionForRoute(type, name)?.let {
                                    pendingSubscriptionSettings = it
                                } ?: vm.setStatus("Subscribe to this tag first to configure alerts.")
                            }
                        )
                    }
                }
            }
            if (!vm.legacyHomeUi && homeSurface == HomeSurface.CREATORS) {
                item {
                    DashboardBodyReveal(revealKey = homeSurface) {
                        DashboardCreatorsSection(
                            vm = vm,
                            listState = creatorsListState,
                            selectedEntryDownloaded = selectedEntryDownloaded,
                            entryDetailActions = dashboardEntryDetailActions,
                            onNotificationPermissionRequired = ::requestNotificationPermissionIfNeeded,
                            onConfigureSubscription = { type, name ->
                                vm.subscriptionForRoute(type, name)?.let {
                                    pendingSubscriptionSettings = it
                                } ?: vm.setStatus("Subscribe to this artist/group first to configure alerts.")
                            },
                            onOpenEntry = { code ->
                                vm.expandEntriesSection()
                                switchHomeSurface(HomeSurface.ENTRIES, restoreScroll = false)
                                selectEntryFromRow(code)
                            }
                        )
                    }
                }
            }
            for (homeSection in visibleHomeSections) {
                when (homeSection) {
                    HomeSection.TAGS -> {
                        item {
                            LegacyTagsSection(
                                vm = vm,
                                listState = tagsListState,
                                onPressStart = ::stopActiveScrolls,
                                runOnPressWhen = isAnyListScrolling,
                                onNotificationPermissionRequired = ::requestNotificationPermissionIfNeeded,
                                onConfigureSubscription = { type, name ->
                                    vm.subscriptionForRoute(type, name)?.let {
                                        pendingSubscriptionSettings = it
                                    } ?: vm.setStatus("Subscribe to this tag first to configure alerts.")
                                }
                            )
                        }
                    }
                    HomeSection.ENTRIES -> {
                        dashboardEntriesSection(
                            vm = vm,
                            localEntryHoldPopupState = localEntryHoldPopupStateHolder,
                            entryItemXByCode = entryItemXByCode,
                            entryItemYByCode = entryItemYByCode,
                            entryItemWidthByCode = entryItemWidthByCode,
                            entryItemHeightByCode = entryItemHeightByCode,
                            haptic = haptic,
                            holdPopupScreenWidthPx = holdPopupScreenWidthPx,
                            holdPopupWidthPx = holdPopupWidthPx,
                            selectedEntryDownloaded = selectedEntryDownloaded,
                            dashboardEntryDetailActions = dashboardEntryDetailActions,
                            useReducedScrollThumbnails = useReducedScrollThumbnails,
                            onCollapseHeatmap = { heatmapOverviewCollapsed = true },
                            onPressStart = ::stopActiveScrolls,
                            runOnPressWhen = isAnyListScrolling,
                            onSelectEntry = ::selectEntryFromRow
                        )
                    }
                    HomeSection.SUGGESTED -> {
                        item {
                            DashboardSuggestionsSection(
                                vm = vm,
                                listState = suggestedListState,
                                maxHeight = creatorsListMaxHeight,
                                preferLowRes = useReducedScrollThumbnails,
                                suggestedDuplicateComparisonState = suggestedDuplicateComparisonStateHolder,
                                entryItemYByCode = entryItemYByCode,
                                entryItemWidthByCode = entryItemWidthByCode,
                                entryItemHeightByCode = entryItemHeightByCode,
                                haptic = haptic,
                                onShowWeights = { showSuggestedWeightsDialog = true },
                                onCollapseHeatmap = { heatmapOverviewCollapsed = true },
                                onPressStart = ::stopActiveScrolls,
                                runOnPressWhen = isAnyListScrolling
                            )
                        }
                    }
                    HomeSection.SUBSCRIPTIONS -> {
                        item {
                            DashboardSubscriptionsSection(
                                vm = vm,
                                selectedEventId = selectedSubscriptionEventId,
                                onSelectedEventIdChange = { selectedSubscriptionEventId = it },
                                preferLowRes = useReducedScrollThumbnails,
                                listState = subscriptionsListState,
                                maxHeight = creatorsListMaxHeight,
                                onOpenList = { showSubscriptionsListDialog = true },
                                onPressStart = ::stopActiveScrolls,
                                runOnPressWhen = isAnyListScrolling
                            )
                        }
                    }
                    HomeSection.CREATORS -> {
                        item {
                            LegacyCreatorsSection(
                                vm = vm,
                                listState = creatorsListState,
                                maxHeight = creatorsListMaxHeight,
                                selectedEntryDownloaded = selectedEntryDownloaded,
                                entryDetailActions = dashboardEntryDetailActions,
                                onCollapseHeatmap = { heatmapOverviewCollapsed = true },
                                onPressStart = ::stopActiveScrolls,
                                runOnPressWhen = isAnyListScrolling,
                                onNotificationPermissionRequired = ::requestNotificationPermissionIfNeeded,
                                onConfigureSubscription = { type, name ->
                                    vm.subscriptionForRoute(type, name)?.let {
                                        pendingSubscriptionSettings = it
                                    } ?: vm.setStatus("Subscribe to this artist/group first to configure alerts.")
                                },
                                onSelectLinkedEntry = { creatorId, code ->
                                    val y = creatorLinkYByKey[creatorLinkKey(creatorId, code)]
                                    pendingSelectionAnchor = y?.let {
                                        SelectionAnchor(
                                            context = SelectionAnchorContext.CREATOR_LINK,
                                            code = code,
                                            creatorId = creatorId,
                                            yInRoot = it
                                        )
                                    }
                                    vm.selectEntryFromCreator(code)
                                },
                                onLinkedEntryPositioned = { creatorId, code, y ->
                                    creatorLinkYByKey[creatorLinkKey(creatorId, code)] = y
                                }
                            )
                        }
                    }
                    HomeSection.HEATMAP -> {
                        item {
                            DashboardHeatmapSection(
                                vm = vm,
                                snapshot = filteredHeatmapSnapshot,
                                displayMode = homeHeatmapDisplayMode,
                                onDisplayModeChange = { homeHeatmapDisplayMode = it },
                                onPressStart = ::stopActiveScrolls,
                                runOnPressWhen = isAnyListScrolling,
                                screenHeightDp = configuration.screenHeightDp,
                                onTagSelected = { node ->
                                    homeHeatmapSelectionSheetHeightFraction = homeHeatmapBaseSheetHeightFraction(
                                        GraphSelectionSheetState.Tag(node)
                                    )
                                    homeHeatmapSelectionSheetState = GraphSelectionSheetState.Tag(node)
                                },
                                onEntrySelected = { entry, dominantCircleTags ->
                                    val selection = GraphSelectionSheetState.Entry(
                                        entry = entry,
                                        dominantCircleTags = dominantCircleTags
                                    )
                                    homeHeatmapSelectionSheetHeightFraction =
                                        homeHeatmapBaseSheetHeightFraction(selection)
                                    homeHeatmapSelectionSheetState = selection
                                },
                                entryLayoutSessionCache = sharedTagGraphEntryLayoutCache,
                                legacyCollapsed = heatmapOverviewCollapsed,
                                onLegacyCollapsedChange = { heatmapOverviewCollapsed = it }
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
