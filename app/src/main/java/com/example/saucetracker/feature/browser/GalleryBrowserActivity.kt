package com.example.saucetracker.feature.browser

import com.example.saucetracker.*
import com.example.saucetracker.core.media.*
import com.example.saucetracker.core.ui.theme.AccentMode
import com.example.saucetracker.core.ui.theme.applyAccentMode
import com.example.saucetracker.core.ui.components.*
import com.example.saucetracker.data.backup.*
import com.example.saucetracker.data.database.SauceTrackerDatabase
import com.example.saucetracker.feature.slideshow.GallerySlideshowActivity

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
import com.example.saucetracker.core.diagnostics.GitHubMediaSession
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
import com.example.saucetracker.core.media.BitmapMemoryCache
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

internal const val GALLERY_BROWSER_PREFS_NAME = "nhtagbook_prefs"
internal const val GALLERY_BROWSER_KEY_THEME_MODE = "theme_mode"
internal const val GALLERY_BROWSER_KEY_ACCENT_MODE = "accent_mode"
internal const val GALLERY_BROWSER_KEY_INCOGNITO_MODE_ENABLED = "incognito_mode_enabled"
internal const val GALLERY_BROWSER_KEY_APP_LOCK_ENABLED = "app_lock_enabled"
internal const val GALLERY_BROWSER_KEY_APP_LOCK_BIOMETRIC_ENABLED = "app_lock_biometric_enabled"
internal const val GALLERY_BROWSER_KEY_APP_LOCK_PIN_HASH = "app_lock_pin_hash"
internal const val GALLERY_BROWSER_KEY_APP_LOCK_PIN_SALT = "app_lock_pin_salt"
internal const val GALLERY_BROWSER_KEY_APP_LOCK_GRACE_UNTIL = "app_lock_grace_until"
internal const val GALLERY_BROWSER_KEY_SUGGESTION_HIDDEN_CODES = "suggestion_hidden_codes"
internal const val GALLERY_BROWSER_KEY_SUGGESTION_HIDDEN_ENTRIES = "suggestion_hidden_entries"
internal const val GALLERY_BROWSER_APP_LOCK_GRACE_MS = 30_000L
internal const val GALLERY_BROWSER_URL_TRAILING_PUNCT = ".,;:!?)]}'\""
internal const val GALLERY_BROWSER_INCOGNITO_OVERLAY_ALPHA = 0.82f
internal const val GALLERY_BROWSER_DRAG_RATING_ACTIVE_WIDTH_FRACTION = 0.68f
internal const val GALLERY_BROWSER_HOLD_ACTION_HIDE = 6
internal const val GALLERY_BROWSER_DUPLICATE_HINT_THUMBNAIL_PRIORITY_DELAY_MS = 220L
internal val GALLERY_BROWSER_POSITIVE_ACTION_COLOR = Color(0xFF22C55E)
internal val GALLERY_BROWSER_NEGATIVE_ACTION_COLOR = Color(0xFFEF4444)
internal val GALLERY_BROWSER_UPLOAD_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

internal fun loadBrowserDuplicateCheckMode(prefs: android.content.SharedPreferences): BrowserDuplicateCheckMode {
    val raw = prefs.getString(KEY_BROWSER_DUPLICATE_CHECK_MODE, BrowserDuplicateCheckMode.AGGRESSIVE.name).orEmpty()
    return BrowserDuplicateCheckMode.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        ?: BrowserDuplicateCheckMode.AGGRESSIVE
}

class GalleryBrowserActivity : ComponentActivity() {
    private var browserCleanupPerformed = false
    private var sessionIncognitoModeEnabled = false

    companion object {
        private const val EXTRA_INITIAL_CODE = "extra_initial_code"
        private const val EXTRA_INITIAL_QUERY = "extra_initial_query"
        private const val EXTRA_INITIAL_CREATOR_TYPE = "extra_initial_creator_type"
        private const val EXTRA_INITIAL_CREATOR_NAME = "extra_initial_creator_name"
        private const val EXTRA_BLOCKED_TAGS = "extra_blocked_tags"
        private const val EXTRA_INCOGNITO_MODE = "extra_incognito_mode"

        fun createIntent(
            context: Context,
            initialCode: Int? = null,
            initialQuery: String = "",
            initialCreatorType: String? = null,
            initialCreatorName: String? = null,
            incognitoModeEnabled: Boolean = false,
            blockedTags: List<String> = emptyList()
        ): Intent {
            return Intent(context, GalleryBrowserActivity::class.java).apply {
                if (initialCode != null && initialCode > 0) {
                    putExtra(EXTRA_INITIAL_CODE, initialCode)
                }
                putExtra(EXTRA_INITIAL_QUERY, initialQuery)
                if (!initialCreatorType.isNullOrBlank() && !initialCreatorName.isNullOrBlank()) {
                    putExtra(EXTRA_INITIAL_CREATOR_TYPE, initialCreatorType.trim())
                    putExtra(EXTRA_INITIAL_CREATOR_NAME, initialCreatorName.trim())
                }
                putExtra(EXTRA_INCOGNITO_MODE, incognitoModeEnabled)
                putStringArrayListExtra(EXTRA_BLOCKED_TAGS, ArrayList(blockedTags))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        val initialCode = intent?.getIntExtra(EXTRA_INITIAL_CODE, 0)?.takeIf { it > 0 }
        val initialQuery = intent?.getStringExtra(EXTRA_INITIAL_QUERY).orEmpty().trim()
        val initialCreatorType = intent?.getStringExtra(EXTRA_INITIAL_CREATOR_TYPE).orEmpty().trim()
            .ifBlank { null }
        val initialCreatorName = intent?.getStringExtra(EXTRA_INITIAL_CREATOR_NAME).orEmpty().trim()
            .ifBlank { null }
        val intentIncognitoModeEnabled = intent?.getBooleanExtra(EXTRA_INCOGNITO_MODE, false) == true
        val incognitoModeEnabled = if (GitHubMediaSession.active) {
            intentIncognitoModeEnabled
        } else {
            browserPreferences()
                .getBoolean(GALLERY_BROWSER_KEY_INCOGNITO_MODE_ENABLED, intentIncognitoModeEnabled)
        }
        sessionIncognitoModeEnabled = incognitoModeEnabled
        val blockedTags = intent?.getStringArrayListExtra(EXTRA_BLOCKED_TAGS)?.toList().orEmpty()
        val themeMode = loadThemeMode()
        val accentMode = loadAccentMode()
        window.attributes = window.attributes.apply {
            preferredRefreshRate = 120f
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            var browserIncognitoModeEnabled by remember { mutableStateOf(incognitoModeEnabled) }
            var incognitoTransitionCoverVisible by remember { mutableStateOf(false) }
            LaunchedEffect(incognitoTransitionCoverVisible, browserIncognitoModeEnabled) {
                if (incognitoTransitionCoverVisible) {
                    delay(320)
                    incognitoTransitionCoverVisible = false
                }
            }
            GalleryCodeBrowserTheme(
                themeMode = themeMode,
                accentMode = accentMode,
                incognitoModeEnabled = browserIncognitoModeEnabled
            ) {
                Box(Modifier.fillMaxSize()) {
                    BrowserScreen(
                        initialCode = initialCode,
                        initialQuery = initialQuery,
                        initialCreatorType = initialCreatorType,
                        initialCreatorName = initialCreatorName,
                        incognitoModeEnabled = browserIncognitoModeEnabled,
                        onIncognitoModeChanged = { next ->
                            incognitoTransitionCoverVisible = true
                            browserIncognitoModeEnabled = next
                            sessionIncognitoModeEnabled = next
                        },
                        blockedTags = blockedTags,
                        onDone = ::finish
                    )
                    AnimatedVisibility(
                        visible = incognitoTransitionCoverVisible,
                        enter = fadeIn(animationSpec = tween(durationMillis = 90)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 180))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = if (browserIncognitoModeEnabled) "Entering incognito" else "Leaving incognito",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (tryBeginBrowserCleanup()) {
            BrowserPrivacyController.clearArtifacts(
                api = null,
                clearSensitiveStorage = sessionIncognitoModeEnabled
            )
        }
        super.onDestroy()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onStop() {
        val prefs = browserPreferences()
        val appLockEnabled = prefs.getBoolean(GALLERY_BROWSER_KEY_APP_LOCK_ENABLED, false)
        val pinHash = prefs.getString(GALLERY_BROWSER_KEY_APP_LOCK_PIN_HASH, "").orEmpty()
        val pinSalt = prefs.getString(GALLERY_BROWSER_KEY_APP_LOCK_PIN_SALT, "").orEmpty()
        if (appLockEnabled && pinHash.isNotBlank() && pinSalt.isNotBlank()) {
            prefs.edit()
                .putLong(
                    GALLERY_BROWSER_KEY_APP_LOCK_GRACE_UNTIL,
                    System.currentTimeMillis() + GALLERY_BROWSER_APP_LOCK_GRACE_MS
                )
                .apply()
        }
        super.onStop()
    }

    internal fun tryBeginBrowserCleanup(): Boolean {
        if (browserCleanupPerformed) return false
        browserCleanupPerformed = true
        return true
    }

    private fun loadThemeMode(): ThemeMode {
        val prefs = browserPreferences()
        val raw = prefs.getString(GALLERY_BROWSER_KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.SYSTEM
    }

    private fun loadAccentMode(): AccentMode {
        val prefs = browserPreferences()
        val raw = prefs.getString(GALLERY_BROWSER_KEY_ACCENT_MODE, AccentMode.AUTO.name)
        return AccentMode.entries.firstOrNull { it.name == raw } ?: AccentMode.AUTO
    }

    private fun browserPreferences() = getSharedPreferences(
        GitHubMediaSession.preferencesName(GALLERY_BROWSER_PREFS_NAME),
        Context.MODE_PRIVATE
    )
}

internal fun copyTextToClipboard(
    context: Context,
    label: String,
    value: String,
    successMessage: String = "Copied to clipboard."
) {
    val cleaned = value.trim()
    if (cleaned.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, cleaned))
    Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
}

internal fun normalizeBrowserPinInput(value: String): String {
    return value.filter { it.isDigit() }.take(20)
}

internal fun hashBrowserPin(pin: String, salt: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val payload = "$salt:$pin".toByteArray(Charsets.UTF_8)
    val hash = digest.digest(payload)
    return Base64.encodeToString(hash, Base64.NO_WRAP)
}

internal fun extractImportCandidateFromClipboard(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return null
    val firstLine = trimmed.lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
    if (firstLine.isBlank()) return null

    val codePattern = Regex("^#?\\d{1,7}$")
    val creatorTypedPattern = Regex("^(artist|group)\\s*:\\s*.+$", RegexOption.IGNORE_CASE)
    val creatorLinkPattern = Regex("^https?://(?:www\\.)?nhentai\\.net/(artist|group)/[^\\s/]+/?$", RegexOption.IGNORE_CASE)
    val galleryLinkPattern = Regex("^https?://(?:www\\.)?nhentai\\.net/g/\\d+/?$", RegexOption.IGNORE_CASE)

    if (
        codePattern.matches(firstLine) ||
        creatorTypedPattern.matches(firstLine) ||
        creatorLinkPattern.matches(firstLine) ||
        galleryLinkPattern.matches(firstLine)
    ) {
        return firstLine
    }

    if (firstLine.length <= 80) {
        val tokenCount = firstLine.split(Regex("\\s+")).count { it.isNotBlank() }
        if (tokenCount in 1..6) return firstLine
    }
    return null
}

internal fun browserTagLookupKey(type: String, name: String): String {
    val normalizedType = type.trim().lowercase(Locale.US).ifBlank { "tag" }
    val normalizedName = if (normalizedType == "artist" || normalizedType == "group") {
        normalizeCreatorDisplayName(name)
    } else {
        name.trim()
    }.lowercase(Locale.US)
        .replace(Regex("\\s+"), " ")
        .trim()
    return "$normalizedType::$normalizedName"
}

internal fun formatCompactTagCount(count: Int): String {
    val safe = count.coerceAtLeast(0)
    return when {
        safe >= 1_000_000 -> "${safe / 1_000_000}m"
        safe >= 1_000 -> "${safe / 1_000}k"
        else -> safe.toString()
    }
}

internal fun encodeTagSearchTerm(tagName: String): String {
    val words = tagName.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    if (words.isEmpty()) return ""
    return if (words.size == 1) words.first() else "\"${words.joinToString(" ")}\""
}

internal fun normalizeCreatorDisplayName(raw: String): String {
    val normalized = raw
        .replace("｜", "|")
        .replace(Regex("\\s*\\|\\s*"), "|")
        .trim()
    if (normalized.isBlank()) return ""
    val parts = normalized.split("|")
        .map { parseCreatorSlug(it) }
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (parts.isEmpty()) return ""
    return if (parts.size == 1) {
        parts.first()
    } else {
        parts.joinToString("|")
    }
}

internal fun parseCreatorSlug(rawSlug: String): String {
    var cleaned = rawSlug.trim().trim('/')
    while (cleaned.isNotEmpty() && GALLERY_BROWSER_URL_TRAILING_PUNCT.contains(cleaned.last())) {
        cleaned = cleaned.dropLast(1)
    }
    if (cleaned.isBlank()) return ""
    val decoded = Uri.decode(cleaned)
        .replace("+", " ")
        .replace("-", " ")
        .replace("_", " ")
    return decoded
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" ")
}

internal fun toCreatorUrlSlug(name: String): String {
    val normalizedDisplay = normalizeCreatorDisplayName(name)
    if (normalizedDisplay.isBlank()) return ""
    val parts = normalizedDisplay.split("|")
        .map { parseCreatorSlug(it) }
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (parts.isEmpty()) return ""
    return parts.joinToString("-") { part ->
        part.replace(Regex("\\s+"), "-").lowercase(Locale.US)
    }
}

internal fun normalizeBrowserRouteType(rawType: String): String {
    return when (rawType.trim().lowercase(Locale.US)) {
        "artist" -> "artist"
        "group" -> "group"
        "tag", "tags" -> "tag"
        "language" -> "language"
        "character" -> "character"
        "parody" -> "parody"
        "category" -> "category"
        else -> ""
    }
}

internal fun toBrowserRouteSlug(routeType: String, name: String): String {
    val normalizedType = normalizeBrowserRouteType(routeType)
    if (normalizedType.isBlank()) return ""
    return if (normalizedType == "artist" || normalizedType == "group") {
        toCreatorUrlSlug(name)
    } else {
        parseCreatorSlug(name)
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString("-") { it.lowercase(Locale.US) }
    }
}

internal fun buildApiTagSearchQuery(
    includeTagNames: List<String>,
    excludeTagNames: List<String> = emptyList()
): String {
    fun normalize(raw: String): List<String> {
        return raw.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
    }

    fun encodeTerm(rawName: String, excluded: Boolean): String? {
        val words = normalize(rawName)
        if (words.isEmpty()) return null
        val quoted = if (words.size == 1) {
            words.first()
        } else {
            "\"${words.joinToString(" ")}\""
        }
        return if (excluded) "-$quoted" else quoted
    }

    val includeTerms = includeTagNames
        .asSequence()
        .mapNotNull { encodeTerm(it, excluded = false) }
        .toList()
    val excludeTerms = excludeTagNames
        .asSequence()
        .mapNotNull { encodeTerm(it, excluded = true) }
        .toList()
    return (includeTerms + excludeTerms).joinToString(" ").trim()
}

internal val browserNonAlphanumericRegex = Regex("[^\\p{L}\\p{N}]+")
internal val browserWhitespaceRegex = Regex("\\s+")

internal fun browserNormalizeTagKey(raw: String): String {
    return raw
        .trim()
        .lowercase(Locale.US)
        .replace(browserNonAlphanumericRegex, " ")
        .replace(browserWhitespaceRegex, " ")
        .trim()
}

internal fun isValidBrowserComment(author: String, text: String): Boolean {
    val normalizedAuthor = author.trim().replace(Regex("\\s+"), " ")
    val normalizedText = text.trim().replace(Regex("\\s+"), " ")
    return normalizedAuthor.isNotBlank() &&
        normalizedText.isNotBlank() &&
        !normalizedAuthor.equals(normalizedText, ignoreCase = true)
}
