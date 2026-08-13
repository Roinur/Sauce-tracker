package com.example.saucetracker.feature.experimentalgallery

import com.example.saucetracker.*
import com.example.saucetracker.core.ui.theme.AccentMode
import com.example.saucetracker.core.ui.components.*
import com.example.saucetracker.data.downloads.*

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed as lazyItemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

private const val EXPERIMENTAL_GALLERY_PREFS = "nhtagbook_prefs"
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_ACCENT_MODE = "accent_mode"
private const val KEY_EXPERIMENTAL_GALLERY_COLUMNS = "experimental_gallery_columns"
private const val KEY_EXPERIMENTAL_GALLERY_PREVIEW_CHROME = "experimental_gallery_preview_chrome"
private const val KEY_EXPERIMENTAL_GALLERY_READING_MODE = "experimental_gallery_reading_mode"
private val EXPERIMENTAL_PIN_ON_COLOR = Color(0xFF22C55E)
private val EXPERIMENTAL_PIN_OFF_COLOR = Color(0xFFEF4444)

private data class ExperimentalGalleryPreviewState(
    val photoId: String,
    val initialAspectRatio: Float
)

private enum class ExperimentalGalleryPreviewChrome(val label: String) {
    PIN_AND_SHIMMER("Pin + shimmer"),
    PIN_ONLY("Pin"),
    CLEAN("Nothing")
}

private enum class ExperimentalGalleryReadingMode(val label: String) {
    HORIZONTAL("Horizontal"),
    VERTICAL("Vertical")
}

class ExperimentalGalleryActivity : ComponentActivity() {
    companion object {
        fun createIntent(context: Context): Intent = Intent(context, ExperimentalGalleryActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ExperimentalGalleryApp { ExperimentalGalleryScreen(onClose = ::finish) } }
    }
}

@Composable
private fun ExperimentalGalleryApp(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(EXPERIMENTAL_GALLERY_PREFS, Context.MODE_PRIVATE) }
    val themeMode = remember(prefs) {
        prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)?.let { raw -> ThemeMode.entries.firstOrNull { it.name == raw } } ?: ThemeMode.SYSTEM
    }
    val accentMode = remember(prefs) {
        prefs.getString(KEY_ACCENT_MODE, AccentMode.AUTO.name)?.let { raw -> AccentMode.entries.firstOrNull { it.name == raw } } ?: AccentMode.AUTO
    }
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val fallbackScheme = if (useDark) {
        darkColorScheme(
            primary = Color(0xFF8BC1FF), onPrimary = Color(0xFF002B52),
            secondary = Color(0xFF8CC8A8), background = Color(0xFF1D2127),
            onBackground = Color(0xFFE9EDF2), surface = Color(0xFF292E36),
            onSurface = Color(0xFFE9EDF2), onSurfaceVariant = Color(0xFFB4BEC8), error = Color(0xFFFF8A8A)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF1F63D8), onPrimary = Color.White,
            secondary = Color(0xFF0D8F4F), background = Color(0xFFF6F8FB),
            onBackground = Color(0xFF1F2935), surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1F2935), onSurfaceVariant = Color(0xFF5C6470), error = Color(0xFFB00020)
        )
    }
    val baseScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else fallbackScheme
    val colorScheme = applyExperimentalAccentMode(baseScheme, accentMode, useDark)
    ExperimentalGallerySystemBars(darkContent = !useDark, barColor = colorScheme.background.toArgb())
    MaterialTheme(colorScheme = colorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
    }
}

@Composable
private fun ExperimentalGallerySystemBars(darkContent: Boolean, barColor: Int) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val activity = view.context as? Activity ?: return@SideEffect
        val window = activity.window
        window.statusBarColor = barColor
        window.navigationBarColor = barColor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = darkContent
            isAppearanceLightNavigationBars = darkContent
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExperimentalGalleryScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(EXPERIMENTAL_GALLERY_PREFS, Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var reloadNonce by remember { mutableLongStateOf(0L) }
    var busyLabel by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var columnCount by remember { mutableIntStateOf(prefs.getInt(KEY_EXPERIMENTAL_GALLERY_COLUMNS, 3).coerceIn(2, 4)) }
    var previewChrome by remember(prefs) {
        mutableStateOf(
            prefs.getString(KEY_EXPERIMENTAL_GALLERY_PREVIEW_CHROME, ExperimentalGalleryPreviewChrome.PIN_AND_SHIMMER.name)
                ?.let { raw -> ExperimentalGalleryPreviewChrome.entries.firstOrNull { it.name == raw } }
                ?: ExperimentalGalleryPreviewChrome.PIN_AND_SHIMMER
        )
    }
    var readingMode by remember(prefs) {
        mutableStateOf(
            prefs.getString(KEY_EXPERIMENTAL_GALLERY_READING_MODE, ExperimentalGalleryReadingMode.HORIZONTAL.name)
                ?.let { raw -> ExperimentalGalleryReadingMode.entries.firstOrNull { it.name == raw } }
                ?: ExperimentalGalleryReadingMode.HORIZONTAL
        )
    }
    var showLayoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var previewState by remember { mutableStateOf<ExperimentalGalleryPreviewState?>(null) }
    var duplicateConflictQueue by remember { mutableStateOf<List<ExperimentalGalleryImportConflict>>(emptyList()) }
    val replaceSourceUriStrings = remember { mutableStateListOf<String>() }
    val skipSourceUriStrings = remember { mutableStateListOf<String>() }
    val pendingImportUris = remember { mutableStateListOf<String>() }
    val selectedIds = remember { mutableStateListOf<String>() }
    val itemBounds = remember { mutableStateMapOf<String, Rect>() }
    var gridRootOffset by remember { mutableStateOf(Offset.Zero) }
    val storageConfigured = remember(reloadNonce) { resolveEffectiveGalleryDownloadTreeUri(context).isNotBlank() }
    val folderLabel = remember(reloadNonce) { experimentalGalleryFolderLabel(context) }
    val contents by produceState<ExperimentalGalleryContents?>(initialValue = null, reloadNonce) {
        value = withContext(Dispatchers.IO) { loadExperimentalGalleryContents(context) }
    }
    val photos = contents?.photos.orEmpty()

    fun clearImportConflictFlow() {
        duplicateConflictQueue = emptyList()
        replaceSourceUriStrings.clear()
        skipSourceUriStrings.clear()
        pendingImportUris.clear()
    }

    fun importPhotos(sourceUris: List<String>) {
        if (sourceUris.isEmpty()) return
        scope.launch {
            message = null
            busyLabel = "Preparing experimental gallery..."
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    importExperimentalGalleryPhotos(
                        context = context,
                        sourceUris = sourceUris.map(android.net.Uri::parse),
                        replaceSourceUriStrings = replaceSourceUriStrings.toSet(),
                        skipSourceUriStrings = skipSourceUriStrings.toSet()
                    ) { label, _ -> busyLabel = label }
                }
            }
            busyLabel = null
            clearImportConflictFlow()
            result.onSuccess { importResult ->
                reloadNonce = System.currentTimeMillis()
                message = when {
                    importResult.importedCount <= 0 -> "No new photos were imported."
                    importResult.deleteFailureCount > 0 ->
                        "Imported ${importResult.importedCount} photo${if (importResult.importedCount == 1) "" else "s"}. ${importResult.movedCount} moved fully, ${importResult.deleteFailureCount} original file${if (importResult.deleteFailureCount == 1) " could not" else "s could not"} be removed by Android."
                    else ->
                        "Imported ${importResult.importedCount} photo${if (importResult.importedCount == 1) "" else "s"} and moved the original file${if (importResult.importedCount == 1) "" else "s"} into Experimental Gallery."
                }
            }.onFailure { exc -> message = exc.message ?: "Could not import selected photos." }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            message = null
            val serializedUris = uris.map { it.toString() }
            pendingImportUris.clear()
            pendingImportUris.addAll(serializedUris)
            replaceSourceUriStrings.clear()
            skipSourceUriStrings.clear()
            val conflicts = withContext(Dispatchers.IO) { findExperimentalGalleryImportConflicts(context, uris) }
            if (conflicts.isEmpty()) importPhotos(serializedUris) else duplicateConflictQueue = conflicts
        }
    }

    if (showLayoutDialog) {
        ExperimentalGalleryLayoutDialog(
            columnCount = columnCount,
            previewChrome = previewChrome,
            readingMode = readingMode,
            onDismiss = { showLayoutDialog = false },
            onColumnSelected = {
                columnCount = it
                prefs.edit().putInt(KEY_EXPERIMENTAL_GALLERY_COLUMNS, it).apply()
            },
            onPreviewChromeSelected = {
                previewChrome = it
                prefs.edit().putString(KEY_EXPERIMENTAL_GALLERY_PREVIEW_CHROME, it.name).apply()
            },
            onReadingModeSelected = {
                readingMode = it
                prefs.edit().putString(KEY_EXPERIMENTAL_GALLERY_READING_MODE, it.name).apply()
            }
        )
    }

    duplicateConflictQueue.firstOrNull()?.let { conflict ->
        ExperimentalGalleryConflictDialog(
            conflict = conflict,
            onReplace = {
                replaceSourceUriStrings += conflict.sourceUri.toString()
                duplicateConflictQueue = duplicateConflictQueue.drop(1)
                if (duplicateConflictQueue.isEmpty()) importPhotos(pendingImportUris.toList())
            },
            onSkip = {
                skipSourceUriStrings += conflict.sourceUri.toString()
                duplicateConflictQueue = duplicateConflictQueue.drop(1)
                if (duplicateConflictQueue.isEmpty()) importPhotos(pendingImportUris.toList())
            },
            onCancel = { clearImportConflictFlow() }
        )
    }

    if (showDeleteDialog && selectedIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val targets = selectedIds.toSet()
                    showDeleteDialog = false
                    scope.launch {
                        val removed = withContext(Dispatchers.IO) { removeExperimentalGalleryPhotos(context, targets) }
                        selectedIds.clear()
                        reloadNonce = System.currentTimeMillis()
                        message = if (removed > 0) "Removed $removed photo${if (removed == 1) "" else "s"}." else "No photos were removed."
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
            title = { Text("Delete selected photos?") },
            text = { Text("Delete ${selectedIds.size} selected photo${if (selectedIds.size == 1) "" else "s"} from Experimental Gallery?") }
        )
    }

    previewState?.let { preview ->
        if (photos.isNotEmpty()) {
            val initialIndex = photos.indexOfFirst { it.id == preview.photoId }.takeIf { it >= 0 } ?: 0
            ExperimentalGalleryPreviewDialog(
                photos = photos,
                initialIndex = initialIndex.coerceIn(0, photos.lastIndex),
                initialPhotoId = preview.photoId,
                initialAspectRatio = preview.initialAspectRatio,
                previewChrome = previewChrome,
                readingMode = readingMode,
                onDismiss = { previewState = null },
                onPin = { photo ->
                    scope.launch {
                        val changed = withContext(Dispatchers.IO) {
                            setExperimentalGalleryPhotoPinned(context, photo.id, photo.pinnedAtMillis <= 0L)
                        }
                        if (changed) {
                            reloadNonce = System.currentTimeMillis()
                            message = if (photo.pinnedAtMillis > 0L) {
                                "\"${photo.displayName}\" unpinned."
                            } else {
                                "\"${photo.displayName}\" pinned to the top."
                            }
                        }
                    }
                }
            )
        } else previewState = null
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (selectedIds.isNotEmpty()) "${selectedIds.size} selected" else "Experimental Gallery",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    TextButton(onClick = {
                        if (selectedIds.isNotEmpty()) selectedIds.clear() else onClose()
                    }) { Text(if (selectedIds.isNotEmpty()) "Cancel" else "Close") }
                },
                actions = {
                    if (selectedIds.isNotEmpty()) {
                        TextButton(onClick = { showDeleteDialog = true }) { Text("Delete") }
                    } else {
                        TextButton(onClick = { showLayoutDialog = true }) { Text("Layout") }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                ),
                windowInsets = WindowInsets(0)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Stored in the hidden local-download root so the files ride with that backup/download folder.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Folder: $folderLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { photoPicker.launch(arrayOf("image/*")) },
                    enabled = storageConfigured && busyLabel == null && selectedIds.isEmpty(),
                    modifier = Modifier.weight(1f)
                ) { Text("Add Photos") }
                Button(
                    onClick = { if (!openExperimentalGalleryFolder(context)) message = "Could not open experimental gallery folder." },
                    enabled = storageConfigured && busyLabel == null && selectedIds.isEmpty(),
                    modifier = Modifier.weight(1f)
                ) { Text("Open Folder") }
            }
            if (busyLabel != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Text(text = busyLabel.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            message?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when {
                !storageConfigured -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Set a procedural backup folder or downloads folder first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                photos.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No experimental photos yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { coords -> gridRootOffset = coords.localToRoot(Offset.Zero) }
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columnCount),
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            userScrollEnabled = selectedIds.isEmpty()
                        ) {
                            itemsIndexed(photos, key = { _, photo -> photo.id }) { index, photo ->
                                ExperimentalGalleryPhotoTile(
                                    photo = photo,
                                    selected = photo.id in selectedIds,
                                    onBoundsChanged = { itemBounds[photo.id] = it },
                                    onDragSelect = { sourceId, localOffset ->
                                        val sourceRect = itemBounds[sourceId] ?: return@ExperimentalGalleryPhotoTile
                                        val rootOffset = sourceRect.topLeft + localOffset
                                        itemBounds.entries.firstOrNull { it.value.contains(rootOffset) }?.key?.let {
                                            if (it !in selectedIds) selectedIds += it
                                        }
                                    },
                                    onClick = { initialAspectRatio ->
                                        if (selectedIds.isNotEmpty()) {
                                            if (photo.id in selectedIds) selectedIds.remove(photo.id) else selectedIds += photo.id
                                        } else {
                                            previewState = ExperimentalGalleryPreviewState(
                                                photoId = photo.id,
                                                initialAspectRatio = initialAspectRatio
                                            )
                                        }
                                    },
                                    onLongPress = { if (photo.id !in selectedIds) selectedIds += photo.id }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExperimentalGalleryPhotoTile(
    photo: ExperimentalGalleryPhoto,
    selected: Boolean,
    onBoundsChanged: (Rect) -> Unit,
    onDragSelect: (String, Offset) -> Unit,
    onClick: (Float) -> Unit,
    onLongPress: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, photo.uriString) {
        value = withContext(Dispatchers.IO) { fetchLocalGalleryPageBitmap(context, photo.uriString) }
    }
    val aspectRatio = remember(bitmap) {
        bitmap?.let {
            (it.width.toFloat() / it.height.toFloat()).coerceIn(0.55f, 1.8f)
        } ?: 0.76f
    }
    val previewAspectRatio = remember(bitmap) {
        bitmap?.let {
            (it.width.toFloat() / it.height.toFloat()).coerceIn(0.35f, 3.2f)
        } ?: 0.76f
    }
    Card(
        modifier = Modifier.fillMaxWidth().onGloballyPositioned { onBoundsChanged(it.boundsInRoot()) },
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .pointerInput(photo.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            onLongPress()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDrag = { change, _ ->
                            onDragSelect(photo.id, change.position)
                            change.consume()
                        }
                    )
                }
                .combinedClickable(onClick = { onClick(previewAspectRatio) }, onLongClick = onLongPress)
        ) {
            SelectedCardEdgeGlow(
                active = selected,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.matchParentSize()
            )
            PinnedCornerBleedGlow(
                visible = photo.pinnedAtMillis > 0L,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.matchParentSize()
            )
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = photo.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio)
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Preview unavailable",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
            if (selected) {
                Box(modifier = Modifier.matchParentSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)))
            }
            if (photo.pinnedAtMillis > 0L) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_push_pin_24),
                    contentDescription = "Pinned",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ExperimentalGalleryLayoutDialog(
    columnCount: Int,
    previewChrome: ExperimentalGalleryPreviewChrome,
    readingMode: ExperimentalGalleryReadingMode,
    onDismiss: () -> Unit,
    onColumnSelected: (Int) -> Unit,
    onPreviewChromeSelected: (ExperimentalGalleryPreviewChrome) -> Unit,
    onReadingModeSelected: (ExperimentalGalleryReadingMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Layout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Photos per row", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 3, 4).forEach { count ->
                        val selected = count == columnCount
                        Button(
                            onClick = { onColumnSelected(count) },
                            modifier = Modifier.weight(1f),
                            colors = if (selected) {
                                ButtonDefaults.buttonColors()
                            } else {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        ) { Text("$count") }
                    }
                }
                Text("Expanded view", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExperimentalGalleryPreviewChrome.entries.forEach { mode ->
                        val selected = mode == previewChrome
                        Button(
                            onClick = { onPreviewChromeSelected(mode) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (selected) {
                                ButtonDefaults.buttonColors()
                            } else {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        ) {
                            Text(mode.label)
                        }
                    }
                }
                Text("Expanded navigation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExperimentalGalleryReadingMode.entries.forEach { mode ->
                        val selected = mode == readingMode
                        Button(
                            onClick = { onReadingModeSelected(mode) },
                            modifier = Modifier.weight(1f),
                            colors = if (selected) {
                                ButtonDefaults.buttonColors()
                            } else {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        ) { Text(mode.label) }
                    }
                }
            }
        }
    )
}

@Composable
private fun ExperimentalGalleryConflictDialog(
    conflict: ExperimentalGalleryImportConflict,
    onReplace: () -> Unit,
    onSkip: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = { TextButton(onClick = onReplace) { Text("Replace") } },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSkip) { Text("Skip") }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        },
        title = { Text("Replace existing photo?") },
        text = {
            Text("A photo named \"${conflict.displayName}\" already exists in Experimental Gallery. Replace the existing file, or skip importing this one?")
        }
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ExperimentalGalleryPreviewDialogLegacy(
    photos: List<ExperimentalGalleryPhoto>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onPin: (ExperimentalGalleryPhoto) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, photos.lastIndex), pageCount = { photos.size })
    var dragX by remember { mutableStateOf(0f) }
    var dragY by remember { mutableStateOf(0f) }
    var dragActive by remember { mutableStateOf(false) }
    var pinReady by remember { mutableStateOf(false) }
    val currentIndex = pagerState.currentPage.coerceIn(0, photos.lastIndex)
    val photo = photos.getOrNull(currentIndex) ?: return
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val previewCurrentBitmap by produceState<ImageBitmap?>(initialValue = null, photo.uriString) {
        value = withContext(Dispatchers.IO) { fetchLocalGalleryPageBitmap(context, photo.uriString) }
    }
    val currentAspectRatio = remember(previewCurrentBitmap) {
        previewCurrentBitmap?.let {
            (it.width.toFloat() / it.height.toFloat()).coerceIn(0.35f, 3.2f)
        } ?: 0.76f
    }
    val pinProgress = (dragY / 180f).coerceIn(0f, 1f)
    val pinTint = if (photo.pinnedAtMillis > 0L) EXPERIMENTAL_PIN_OFF_COLOR else EXPERIMENTAL_PIN_ON_COLOR
    val commitTarget = if (pinReady) SwipeToDismissBoxValue.StartToEnd else SwipeToDismissBoxValue.Settled
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.56f)), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.92f),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = photo.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${currentIndex + 1} / ${photos.size}" + if (photo.pinnedAtMillis > 0L) " • Pinned" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onPin(photo) }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_push_pin_24),
                                contentDescription = if (photo.pinnedAtMillis > 0L) "Unpin photo" else "Pin photo",
                                tint = if (photo.pinnedAtMillis > 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = onDismiss) { Text("Close") }
                    }
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 620.dp)
                            .pointerInput(photo.id, currentIndex, photos.size) {
                                detectDragGestures(
                                    onDragStart = {
                                        dragX = 0f
                                        dragY = 0f
                                        dragActive = true
                                        pinReady = false
                                    },
                                    onDragEnd = {
                                        if (dragY > 120f && abs(dragY) > abs(dragX)) {
                                            onPin(photo)
                                        }
                                        dragX = 0f
                                        dragY = 0f
                                        dragActive = false
                                        pinReady = false
                                    },
                                    onDragCancel = {
                                        dragX = 0f
                                        dragY = 0f
                                        dragActive = false
                                        pinReady = false
                                    },
                                    onDrag = { change, dragAmount ->
                                        dragX += dragAmount.x
                                        dragY += dragAmount.y
                                        val nextReady = dragY > 120f && abs(dragY) > abs(dragX)
                                        if (nextReady != pinReady) {
                                            pinReady = nextReady
                                            if (nextReady) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        }
                                        change.consume()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        SwipeCommitReadySwoosh(
                            commitReadyTarget = commitTarget,
                            tint = pinTint,
                            modifier = Modifier.matchParentSize()
                        )
                        HorizontalPager(
                            state = pagerState,
                            pageSpacing = 12.dp,
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) { page ->
                            val pagePhoto = photos[page]
                            val bitmap by produceState<ImageBitmap?>(initialValue = null, pagePhoto.uriString) {
                                value = withContext(Dispatchers.IO) { fetchLocalGalleryPageBitmap(context, pagePhoto.uriString) }
                            }
                            val aspectRatio = remember(bitmap) {
                                bitmap?.let {
                                    (it.width.toFloat() / it.height.toFloat()).coerceIn(0.35f, 3.2f)
                                } ?: currentAspectRatio
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(aspectRatio)
                                    .heightIn(max = 600.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                PinnedCornerBleedGlow(
                                    visible = pagePhoto.pinnedAtMillis > 0L,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.matchParentSize()
                                )
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap!!,
                                        contentDescription = pagePhoto.displayName,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text("Preview unavailable", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (dragActive && pinProgress > 0f) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .background(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f * pinProgress),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .padding(horizontal = 18.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_push_pin_24),
                                    contentDescription = null,
                                    tint = pinTint
                                )
                                Text(
                                    text = if (photo.pinnedAtMillis > 0L) "Release to unpin" else "Release to pin",
                                    color = pinTint,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Text(
                        text = "Swipe left or right to move between photos. Swipe down to pin or unpin this photo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ExperimentalGalleryPreviewDialog(
    photos: List<ExperimentalGalleryPhoto>,
    initialIndex: Int,
    initialPhotoId: String,
    initialAspectRatio: Float,
    previewChrome: ExperimentalGalleryPreviewChrome,
    readingMode: ExperimentalGalleryReadingMode,
    onDismiss: () -> Unit,
    onPin: (ExperimentalGalleryPhoto) -> Unit
) {
    if (photos.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, photos.lastIndex), pageCount = { photos.size })
    val verticalListState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex.coerceIn(0, photos.lastIndex))
    var sessionReadingMode by remember { mutableStateOf(readingMode) }
    var showModeChooser by remember { mutableStateOf(false) }
    var chooserHighlightedMode by remember { mutableStateOf<ExperimentalGalleryReadingMode?>(null) }
    var rawPreviewMode by remember { mutableStateOf(false) }
    var rawPreviewPhotoId by remember { mutableStateOf<String?>(null) }
    var dragX by remember { mutableStateOf(0f) }
    var dragY by remember { mutableStateOf(0f) }
    var dragActive by remember { mutableStateOf(false) }
    var pinReady by remember { mutableStateOf(false) }
    var dragStartX by remember { mutableStateOf(0f) }
    var pinTargetPhotoId by remember { mutableStateOf<String?>(null) }
    var modeSyncRequest by remember { mutableIntStateOf(0) }
    var anchoredPhotoId by remember(initialIndex) {
        mutableStateOf(initialPhotoId)
    }
    val currentIndex = pagerState.currentPage.coerceIn(0, photos.lastIndex)
    val verticalIndex = currentExperimentalGalleryVerticalPreviewIndex(verticalListState, photos.size)
    val activeIndex = if (sessionReadingMode == ExperimentalGalleryReadingMode.VERTICAL) verticalIndex else currentIndex
    val photo = photos.getOrNull(activeIndex) ?: return
    val rawPreviewPhoto = photos.firstOrNull { it.id == rawPreviewPhotoId } ?: photo
    val pinTargetPhoto = if (sessionReadingMode == ExperimentalGalleryReadingMode.VERTICAL) {
        photos.firstOrNull { it.id == pinTargetPhotoId } ?: photo
    } else {
        photo
    }
    val latestPhoto by rememberUpdatedState(photo)
    val latestPinTargetPhoto by rememberUpdatedState(pinTargetPhoto)
    val latestOnPin by rememberUpdatedState(onPin)
    val context = LocalContext.current
    val displayDensity = context.resources.displayMetrics.density
    val haptic = LocalHapticFeedback.current
    val viewportBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        photo.uriString
    ) {
        value = withContext(Dispatchers.IO) { fetchLocalGalleryPageBitmap(context, photo.uriString) }
    }
    val rawPreviewBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        rawPreviewPhoto.uriString
    ) {
        value = withContext(Dispatchers.IO) { fetchLocalGalleryPageBitmap(context, rawPreviewPhoto.uriString) }
    }
    val currentAspectRatio = viewportBitmap?.let { (it.width.toFloat() / it.height.toFloat()).coerceIn(0.35f, 3.2f) }
        ?: initialAspectRatio
    val rawPreviewAspectRatio = rawPreviewBitmap?.let { (it.width.toFloat() / it.height.toFloat()).coerceIn(0.35f, 3.2f) }
        ?: currentAspectRatio
    val pinTint = if (pinTargetPhoto.pinnedAtMillis > 0L) EXPERIMENTAL_PIN_OFF_COLOR else EXPERIMENTAL_PIN_ON_COLOR
    val commitTarget = if (pinReady) SwipeToDismissBoxValue.StartToEnd else SwipeToDismissBoxValue.Settled
    val pinProgress = if (sessionReadingMode == ExperimentalGalleryReadingMode.VERTICAL) {
        (dragX / 180f).coerceIn(0f, 1f)
    } else {
        (dragY / 180f).coerceIn(0f, 1f)
    }
    var initialSizeSettled by remember { mutableStateOf(false) }
    val showPreviewPinChrome = previewChrome != ExperimentalGalleryPreviewChrome.CLEAN && !rawPreviewMode
    val showPreviewShimmer = true
    val showDragCue = true
    val previewFrameColor = if (rawPreviewMode) Color.Black else MaterialTheme.colorScheme.surface
    val previewBackgroundColor = if (rawPreviewMode) Color.Black else MaterialTheme.colorScheme.background
    val contentPadding = if (rawPreviewMode) 0.dp else 16.dp
    var previewViewportPositionInRoot by remember { mutableStateOf(Offset.Zero) }
    var previewViewportSize by remember { mutableStateOf(IntSize.Zero) }

    fun resetPinGestureState() {
        dragX = 0f
        dragY = 0f
        dragStartX = 0f
        pinTargetPhotoId = null
        dragActive = false
        pinReady = false
    }

    val updateChooserHover = remember {
        { x: Float, y: Float, widthPx: Float, heightPx: Float, density: Float ->
            chooserHighlightedMode = mapExperimentalGalleryModeChoice(
                x = x,
                y = y,
                widthPx = widthPx,
                heightPx = heightPx,
                density = density
            ) ?: chooserHighlightedMode ?: sessionReadingMode
        }
    }
    val updateChooserHoverFromRoot: (Float, Float) -> Unit = { rootX, rootY ->
        val widthPx = previewViewportSize.width.toFloat()
        val heightPx = previewViewportSize.height.toFloat()
        if (widthPx > 0f && heightPx > 0f) {
            updateChooserHover(
                (rootX - previewViewportPositionInRoot.x).coerceIn(0f, widthPx),
                (rootY - previewViewportPositionInRoot.y).coerceIn(0f, heightPx),
                widthPx,
                heightPx,
                displayDensity
            )
        }
    }

    LaunchedEffect(rawPreviewMode) {
        if (rawPreviewMode) {
            resetPinGestureState()
        }
    }
    LaunchedEffect(pagerState, photos, sessionReadingMode) {
        snapshotFlow { pagerState.currentPage to pagerState.isScrollInProgress }
            .collectLatest { (page, inProgress) ->
                if (sessionReadingMode != ExperimentalGalleryReadingMode.HORIZONTAL || photos.isEmpty()) {
                    return@collectLatest
                }
                if (inProgress) return@collectLatest
                photos.getOrNull(page.coerceIn(0, photos.lastIndex))?.id?.let { anchoredPhotoId = it }
            }
    }
    LaunchedEffect(verticalListState, photos, sessionReadingMode) {
        snapshotFlow { verticalListState.firstVisibleItemIndex to verticalListState.firstVisibleItemScrollOffset }
            .collectLatest {
                if (sessionReadingMode != ExperimentalGalleryReadingMode.VERTICAL || photos.isEmpty()) {
                    return@collectLatest
                }
                val resolvedIndex = currentExperimentalGalleryVerticalPreviewIndex(verticalListState, photos.size)
                photos.getOrNull(resolvedIndex)?.id?.let { anchoredPhotoId = it }
            }
    }
    LaunchedEffect(modeSyncRequest, photos) {
        val targetIndex = photos.indexOfFirst { it.id == anchoredPhotoId }
        if (targetIndex < 0) return@LaunchedEffect
        if (sessionReadingMode == ExperimentalGalleryReadingMode.VERTICAL) {
            if (verticalIndex != targetIndex) {
                verticalListState.scrollToItem(targetIndex)
            }
        } else if (pagerState.currentPage != targetIndex) {
            pagerState.scrollToPage(targetIndex)
        }
    }
    LaunchedEffect(viewportBitmap) {
        if (viewportBitmap != null && !initialSizeSettled) {
            initialSizeSettled = true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = previewBackgroundColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(previewBackgroundColor)
                    .padding(
                        horizontal = contentPadding,
                        vertical = contentPadding
                    )
            ) {
                if (!rawPreviewMode) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = onDismiss) { Text("Close") }
                            Text(
                                text = "${activeIndex + 1} / ${photos.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = photo.displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        )
                        Text(
                            text = if (photo.pinnedAtMillis > 0L) "Pinned" else "Hold to change scroll mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    }
                }
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .onGloballyPositioned { coordinates ->
                            previewViewportPositionInRoot = coordinates.positionInRoot()
                            previewViewportSize = coordinates.size
                        }
                        .pointerInput(photo.id, activeIndex, photos.size, sessionReadingMode, showModeChooser, rawPreviewMode) {
                            if (showModeChooser || rawPreviewMode) return@pointerInput
                        if (sessionReadingMode == ExperimentalGalleryReadingMode.VERTICAL) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                if (down.position.x > size.width.toFloat() * 0.28f) {
                                    resetPinGestureState()
                                    return@awaitEachGesture
                                }
                                dragX = 0f
                                dragY = 0f
                                dragStartX = down.position.x
                                pinTargetPhotoId = experimentalGalleryVerticalPinTargetPhotoId(
                                    listState = verticalListState,
                                    photos = photos,
                                    y = down.position.y
                                )
                                dragActive = true
                                pinReady = false
                                val pointerId = down.id
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                    if (!change.pressed) {
                                        val shouldCommitPin =
                                            dragStartX <= size.width.toFloat() * 0.28f &&
                                                dragX > 120f &&
                                                abs(dragX) > abs(dragY)
                                        if (shouldCommitPin) {
                                            latestOnPin(latestPinTargetPhoto)
                                        }
                                        resetPinGestureState()
                                        break
                                    }
                                    val dragAmount = change.position - change.previousPosition
                                    dragX += dragAmount.x
                                    dragY += dragAmount.y
                                    val nextReady =
                                        dragStartX <= size.width.toFloat() * 0.28f &&
                                            dragX > 120f &&
                                            abs(dragX) > abs(dragY)
                                    if (nextReady != pinReady) {
                                        pinReady = nextReady
                                        if (nextReady) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                    if (dragX > 0f && abs(dragX) > abs(dragY)) {
                                        change.consume()
                                    }
                                }
                            }
                        } else {
                            detectDragGestures(
                                onDragStart = {
                                    dragX = 0f
                                    dragY = 0f
                                    dragStartX = it.x
                                    dragActive = true
                                    pinReady = false
                                },
                                onDragEnd = {
                                    val shouldCommitPin = dragY > 120f && abs(dragY) > abs(dragX)
                                    if (shouldCommitPin) {
                                        latestOnPin(latestPhoto)
                                    }
                                    resetPinGestureState()
                                },
                                onDragCancel = { resetPinGestureState() },
                                onDrag = { change, dragAmount ->
                                    dragX += dragAmount.x
                                    dragY += dragAmount.y
                                    val nextReady = dragY > 120f && abs(dragY) > abs(dragX)
                                    if (nextReady != pinReady) {
                                        pinReady = nextReady
                                        if (nextReady) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                    change.consume()
                                }
                            )
                        }
                    }
            ) {
                val density = LocalDensity.current
                val maxWidthPx = with(density) { maxWidth.toPx() }
                val viewportHeightTarget = (maxWidth / currentAspectRatio).coerceAtMost(maxHeight)
                val sizeAnimationSpec: FiniteAnimationSpec<androidx.compose.ui.unit.Dp> =
                    if (initialSizeSettled) {
                        tween(durationMillis = 220, easing = FastOutSlowInEasing)
                    } else {
                        snap()
                    }
                val animatedViewportHeight by animateDpAsState(
                    targetValue = viewportHeightTarget,
                    animationSpec = sizeAnimationSpec,
                    label = "experimentalPreviewViewportHeight"
                )
                val activeImageWidth = if ((maxWidth / currentAspectRatio) > animatedViewportHeight) {
                    animatedViewportHeight * currentAspectRatio
                } else {
                    maxWidth
                }
                val activeImageHeight = if (sessionReadingMode == ExperimentalGalleryReadingMode.VERTICAL) {
                    (maxWidth / currentAspectRatio).coerceAtMost(maxHeight)
                } else {
                    activeImageWidth / currentAspectRatio
                }
                val activePreviewFrameModifier = Modifier
                    .align(Alignment.Center)
                    .size(width = activeImageWidth, height = activeImageHeight)
                    .clip(RoundedCornerShape(18.dp))
                Crossfade(
                    targetState = rawPreviewMode,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    label = "experimentalRawPreviewCrossfade"
                ) { inRawPreview ->
                    if (inRawPreview) {
                        val rawLayout = remember(rawPreviewAspectRatio, maxWidth, maxHeight) {
                            calculateExperimentalRawPreviewLayout(
                                aspectRatio = rawPreviewAspectRatio,
                                maxWidth = maxWidth,
                                maxHeight = maxHeight
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            if (rawPreviewBitmap != null) {
                                Image(
                                    bitmap = rawPreviewBitmap!!,
                                    contentDescription = rawPreviewPhoto.displayName,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(width = rawLayout.width, height = rawLayout.height)
                                        .graphicsLayer { rotationZ = rawLayout.rotationZ }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(width = rawLayout.width, height = rawLayout.height)
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Preview unavailable",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxWidth(0.14f)
                                    .fillMaxHeight(0.12f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            rawPreviewMode = false
                                            rawPreviewPhotoId = null
                                        }
                                    )
                            )
                        }
                    } else {
                val pageContent: @Composable (Int) -> Unit = { page ->
                    val pagePhoto = photos[page]
                    var pagePositionInRoot by remember(pagePhoto.id) { mutableStateOf(Offset.Zero) }
                    val bitmap by produceState<ImageBitmap?>(
                        initialValue = null,
                        pagePhoto.uriString
                    ) {
                        value = withContext(Dispatchers.IO) { fetchLocalGalleryPageBitmap(context, pagePhoto.uriString) }
                    }
                    val aspectRatio = remember(bitmap) {
                        bitmap?.let { (it.width.toFloat() / it.height.toFloat()).coerceIn(0.35f, 3.2f) } ?: currentAspectRatio
                    }
                    val imageAlignment = when {
                        sessionReadingMode == ExperimentalGalleryReadingMode.VERTICAL && page < activeIndex -> Alignment.BottomCenter
                        sessionReadingMode == ExperimentalGalleryReadingMode.VERTICAL && page > activeIndex -> Alignment.TopCenter
                        sessionReadingMode == ExperimentalGalleryReadingMode.VERTICAL -> Alignment.Center
                        page < activeIndex -> Alignment.CenterEnd
                        page > activeIndex -> Alignment.CenterStart
                        dragX < -4f -> Alignment.CenterEnd
                        dragX > 4f -> Alignment.CenterStart
                        else -> Alignment.Center
                    }
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(animatedViewportHeight),
                        contentAlignment = imageAlignment
                    ) {
                        val targetWidth = if ((maxWidth / aspectRatio) > animatedViewportHeight) {
                            animatedViewportHeight * aspectRatio
                        } else {
                            maxWidth
                        }
                        val targetHeight = targetWidth / aspectRatio
                        val animatedWidth by animateDpAsState(
                            targetValue = targetWidth,
                            animationSpec = if (page == activeIndex) sizeAnimationSpec else tween(durationMillis = 220, easing = FastOutSlowInEasing),
                            label = "experimentalPreviewImageWidth"
                        )
                        val animatedHeight by animateDpAsState(
                            targetValue = targetHeight,
                            animationSpec = if (page == activeIndex) sizeAnimationSpec else tween(durationMillis = 220, easing = FastOutSlowInEasing),
                            label = "experimentalPreviewImageHeight"
                        )
                        Box(
                            modifier = Modifier
                                .onGloballyPositioned { coordinates ->
                                    pagePositionInRoot = coordinates.positionInRoot()
                                }
                                .size(width = animatedWidth, height = animatedHeight)
                                .clip(RoundedCornerShape(18.dp))
                                .background(previewFrameColor)
                                .pointerInput(pagePhoto.id, previewViewportSize) {
                                    if (showModeChooser || previewViewportSize.width <= 0 || previewViewportSize.height <= 0) {
                                        return@pointerInput
                                    }
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                                        down.consume()
                                        longPress.consume()
                                        val start = pagePositionInRoot + longPress.position
                                        showModeChooser = true
                                        chooserHighlightedMode = sessionReadingMode
                                        updateChooserHoverFromRoot(start.x, start.y)
                                        var finished = false
                                        while (!finished) {
                                            val event = awaitPointerEvent(pass = PointerEventPass.Final)
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                                            if (change == null) {
                                                finished = true
                                            } else if (!change.pressed || change.changedToUpIgnoreConsumed()) {
                                                val position = pagePositionInRoot + change.position
                                                updateChooserHoverFromRoot(position.x, position.y)
                                                event.changes.forEach { it.consume() }
                                                chooserHighlightedMode?.let { selectedMode ->
                                                    anchoredPhotoId = pagePhoto.id
                                                    if (selectedMode != sessionReadingMode) {
                                                        sessionReadingMode = selectedMode
                                                        modeSyncRequest += 1
                                                    }
                                                }
                                                showModeChooser = false
                                                chooserHighlightedMode = null
                                                finished = true
                                            } else {
                                                val position = pagePositionInRoot + change.position
                                                updateChooserHoverFromRoot(position.x, position.y)
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap!!,
                                    contentDescription = pagePhoto.displayName,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.matchParentSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(previewFrameColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Preview unavailable",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxWidth(0.14f)
                                    .fillMaxHeight(0.12f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            rawPreviewPhotoId = pagePhoto.id
                                            rawPreviewMode = true
                                        }
                                    )
                            )
                        }
                    }
                }
                if (sessionReadingMode == ExperimentalGalleryReadingMode.VERTICAL) {
                    LazyColumn(
                        state = verticalListState,
                        userScrollEnabled = !showModeChooser,
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        lazyItemsIndexed(photos, key = { _, item -> item.id }) { _, pagePhoto ->
                            var pagePositionInRoot by remember(pagePhoto.id) { mutableStateOf(Offset.Zero) }
                            val bitmap by produceState<ImageBitmap?>(
                                initialValue = null,
                                pagePhoto.uriString
                            ) {
                                value = withContext(Dispatchers.IO) { fetchLocalGalleryPageBitmap(context, pagePhoto.uriString) }
                            }
                            val aspectRatio = remember(bitmap) {
                                bitmap?.let { (it.width.toFloat() / it.height.toFloat()).coerceIn(0.35f, 3.2f) }
                                    ?: currentAspectRatio
                            }
                            val isActiveVerticalPhoto = pagePhoto.id == pinTargetPhoto.id
                            val verticalDragCueActive = dragActive && dragStartX <= maxWidthPx * 0.28f && dragX > 12f
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(aspectRatio)
                                    .onGloballyPositioned { coordinates ->
                                        pagePositionInRoot = coordinates.positionInRoot()
                                    }
                                    .pointerInput(pagePhoto.id, previewViewportSize) {
                                        if (showModeChooser || previewViewportSize.width <= 0 || previewViewportSize.height <= 0) {
                                            return@pointerInput
                                        }
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                                            down.consume()
                                            longPress.consume()
                                            val start = pagePositionInRoot + longPress.position
                                            showModeChooser = true
                                            chooserHighlightedMode = sessionReadingMode
                                            updateChooserHoverFromRoot(start.x, start.y)
                                            var finished = false
                                            while (!finished) {
                                                val event = awaitPointerEvent(pass = PointerEventPass.Final)
                                                val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                                                if (change == null) {
                                                    finished = true
                                                } else if (!change.pressed || change.changedToUpIgnoreConsumed()) {
                                                    val position = pagePositionInRoot + change.position
                                                    updateChooserHoverFromRoot(position.x, position.y)
                                                    event.changes.forEach { it.consume() }
                                                    chooserHighlightedMode?.let { selectedMode ->
                                                        anchoredPhotoId = pagePhoto.id
                                                        if (selectedMode != sessionReadingMode) {
                                                            sessionReadingMode = selectedMode
                                                            modeSyncRequest += 1
                                                        }
                                                    }
                                                    showModeChooser = false
                                                    chooserHighlightedMode = null
                                                    finished = true
                                                } else {
                                                    val position = pagePositionInRoot + change.position
                                                    updateChooserHoverFromRoot(position.x, position.y)
                                                    event.changes.forEach { it.consume() }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(18.dp))
                                ) {
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap!!,
                                            contentDescription = pagePhoto.displayName,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clip(RoundedCornerShape(18.dp))
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(previewFrameColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Preview unavailable",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .fillMaxWidth(0.14f)
                                            .fillMaxHeight(0.12f)
                                            .clip(RoundedCornerShape(18.dp))
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            rawPreviewPhotoId = pagePhoto.id
                                            rawPreviewMode = true
                                        }
                                    )
                            )
                        }
                                if (showModeChooser) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.36f))
                                    )
                                }
                                if (isActiveVerticalPhoto && showPreviewShimmer) {
                                    SwipeCommitReadySwoosh(
                                        commitReadyTarget = commitTarget,
                                        tint = pinTint,
                                        modifier = Modifier
                                            .matchParentSize()
                                            .zIndex(1f)
                                    )
                                }
                                if (isActiveVerticalPhoto && showDragCue && verticalDragCueActive) {
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .zIndex(2f)
                                            .background(
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f * pinProgress.coerceAtLeast(0.2f)),
                                                shape = RoundedCornerShape(18.dp)
                                            )
                                            .padding(horizontal = 18.dp, vertical = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_push_pin_24),
                                            contentDescription = null,
                                            tint = pinTint
                                        )
                                        Text(
                                            text = if (pinReady) {
                                                if (pinTargetPhoto.pinnedAtMillis > 0L) "Release to unpin" else "Release to pin"
                                            } else {
                                                if (pinTargetPhoto.pinnedAtMillis > 0L) "Swipe in from left to unpin" else "Swipe in from left to pin"
                                            },
                                            color = pinTint,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                if (showPreviewPinChrome) {
                                    PinnedCornerBleedGlow(
                                        visible = pagePhoto.pinnedAtMillis > 0L,
                                        tint = MaterialTheme.colorScheme.primary,
                                        cornerRadius = 18.dp,
                                        modifier = Modifier.matchParentSize()
                                    )
                                }
                                if (showPreviewPinChrome && pagePhoto.pinnedAtMillis > 0L) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .zIndex(2f)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_push_pin_24),
                                            contentDescription = "Pinned",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(10.dp)
                                                .size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        pageSpacing = 0.dp,
                        userScrollEnabled = !showModeChooser,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        pageContent(page)
                    }
                }
                if (sessionReadingMode != ExperimentalGalleryReadingMode.VERTICAL && showPreviewShimmer) {
                    SwipeCommitReadySwoosh(
                        commitReadyTarget = commitTarget,
                        tint = pinTint,
                        modifier = activePreviewFrameModifier
                            .zIndex(1f)
                    )
                }
                val dragCueActive = if (sessionReadingMode == ExperimentalGalleryReadingMode.VERTICAL) {
                    dragActive && dragStartX <= maxWidthPx * 0.28f && dragX > 12f
                } else {
                    dragActive && dragY > 12f
                }
                if (sessionReadingMode != ExperimentalGalleryReadingMode.VERTICAL && showDragCue && dragCueActive) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .zIndex(2f)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f * pinProgress.coerceAtLeast(0.2f)),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_push_pin_24),
                            contentDescription = null,
                            tint = pinTint
                        )
                        Text(
                            text = if (pinReady) {
                                if (photo.pinnedAtMillis > 0L) "Release to unpin" else "Release to pin"
                            } else {
                                if (sessionReadingMode == ExperimentalGalleryReadingMode.VERTICAL) {
                                    if (photo.pinnedAtMillis > 0L) "Swipe in from left to unpin" else "Swipe in from left to pin"
                                } else {
                                    if (photo.pinnedAtMillis > 0L) "Drag down to unpin" else "Drag down to pin"
                                }
                            },
                            color = pinTint,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (sessionReadingMode != ExperimentalGalleryReadingMode.VERTICAL && showPreviewPinChrome) {
                    PinnedCornerBleedGlow(
                        visible = photo.pinnedAtMillis > 0L,
                        tint = MaterialTheme.colorScheme.primary,
                        cornerRadius = 18.dp,
                        modifier = activePreviewFrameModifier
                    )
                }
                if (sessionReadingMode != ExperimentalGalleryReadingMode.VERTICAL && showPreviewPinChrome && photo.pinnedAtMillis > 0L) {
                    Box(
                        modifier = activePreviewFrameModifier
                            .zIndex(2f)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_push_pin_24),
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .size(20.dp)
                        )
                    }
                }
                if (showModeChooser) {
                    if (sessionReadingMode != ExperimentalGalleryReadingMode.VERTICAL) {
                        Box(
                            modifier = activePreviewFrameModifier
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.36f))
                        )
                    }
                    ExperimentalGalleryReadingModeChooser(
                        currentMode = chooserHighlightedMode ?: sessionReadingMode,
                        modifier = Modifier.matchParentSize(),
                        scrimEnabled = false
                    )
                }
                    }
                }
            }
        }
        }
    }
}

private fun currentExperimentalGalleryVerticalPreviewIndex(
    listState: LazyListState,
    photoCount: Int
): Int {
    if (photoCount <= 0) return 0
    val layoutInfo = listState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) {
        return listState.firstVisibleItemIndex.coerceIn(0, photoCount - 1)
    }
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
    return visibleItems
        .minByOrNull { abs((it.offset + (it.size / 2f)) - viewportCenter) }
        ?.index
        ?.coerceIn(0, photoCount - 1)
        ?: listState.firstVisibleItemIndex.coerceIn(0, photoCount - 1)
}

private fun experimentalGalleryVerticalPinTargetPhotoId(
    listState: LazyListState,
    photos: List<ExperimentalGalleryPhoto>,
    y: Float
): String? {
    val visibleItems = listState.layoutInfo.visibleItemsInfo
    val targetIndex = visibleItems
        .firstOrNull { y >= it.offset && y <= (it.offset + it.size) }
        ?.index
        ?: visibleItems
            .minByOrNull { abs((it.offset + (it.size / 2f)) - y) }
            ?.index
    return targetIndex?.let { photos.getOrNull(it)?.id }
}

private data class ExperimentalRawPreviewLayout(
    val width: Dp,
    val height: Dp,
    val rotationZ: Float
)

private fun calculateExperimentalRawPreviewLayout(
    aspectRatio: Float,
    maxWidth: Dp,
    maxHeight: Dp
): ExperimentalRawPreviewLayout {
    val safeAspectRatio = aspectRatio.coerceIn(0.2f, 5f)
    val normalWidth = if ((maxWidth / safeAspectRatio) > maxHeight) {
        maxHeight * safeAspectRatio
    } else {
        maxWidth
    }
    val normalHeight = normalWidth / safeAspectRatio

    val rotatedVisibleAspectRatio = (1f / safeAspectRatio).coerceIn(0.2f, 5f)
    val rotatedVisibleWidth = if ((maxWidth / rotatedVisibleAspectRatio) > maxHeight) {
        maxHeight * rotatedVisibleAspectRatio
    } else {
        maxWidth
    }
    val rotatedVisibleHeight = rotatedVisibleWidth / rotatedVisibleAspectRatio
    val rotatedWidth = rotatedVisibleHeight
    val rotatedHeight = rotatedVisibleWidth

    val normalArea = normalWidth.value * normalHeight.value
    val rotatedArea = rotatedVisibleWidth.value * rotatedVisibleHeight.value
    return if (rotatedArea > normalArea + 1f) {
        ExperimentalRawPreviewLayout(
            width = rotatedWidth,
            height = rotatedHeight,
            rotationZ = 90f
        )
    } else {
        ExperimentalRawPreviewLayout(
            width = normalWidth,
            height = normalHeight,
            rotationZ = 0f
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExperimentalGalleryLegacyVerticalPagerContent(
    pagerState: PagerState,
    viewportHeight: Dp,
    pageContent: @Composable (Int) -> Unit
) {
    VerticalPager(
        state = pagerState,
        pageSpacing = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(viewportHeight)
    ) { page ->
        pageContent(page)
    }
}

@Composable
private fun ExperimentalGalleryReadingModeChooser(
    currentMode: ExperimentalGalleryReadingMode,
    modifier: Modifier = Modifier,
    scrimEnabled: Boolean = true
) {
    Box(
        modifier = modifier.then(
            if (scrimEnabled) {
                Modifier.background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.36f))
            } else {
                Modifier
            }
        ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExperimentalGalleryReadingModeOptionCard(
                label = "Horizontal",
                selected = currentMode == ExperimentalGalleryReadingMode.HORIZONTAL
            ) {
                ExperimentalGalleryReadingModePreviewIcon(ExperimentalGalleryReadingMode.HORIZONTAL)
            }
            ExperimentalGalleryReadingModeOptionCard(
                label = "Vertical",
                selected = currentMode == ExperimentalGalleryReadingMode.VERTICAL
            ) {
                ExperimentalGalleryReadingModePreviewIcon(ExperimentalGalleryReadingMode.VERTICAL)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExperimentalGalleryReadingModeOptionCard(
    label: String,
    selected: Boolean,
    icon: @Composable () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(durationMillis = 180),
        label = "experimentalModeOptionContainer"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 180),
        label = "experimentalModeOptionLabel"
    )
    val optionAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.96f,
        animationSpec = tween(durationMillis = 180),
        label = "experimentalModeOptionAlpha"
    )
    Card(
        modifier = Modifier
            .width(114.dp)
            .graphicsLayer(alpha = optionAlpha)
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = labelColor
            )
        }
    }
}

private fun mapExperimentalGalleryModeChoice(
    x: Float,
    y: Float,
    widthPx: Float,
    heightPx: Float,
    density: Float
): ExperimentalGalleryReadingMode? {
    val optionWidth = 114f * density
    val optionHeight = 108f * density
    val gap = 12f * density
    val rowWidth = (optionWidth * 2f) + gap
    val rowLeft = (widthPx - rowWidth) / 2f
    val rowTop = (heightPx - optionHeight) / 2f
    val leftCenterX = rowLeft + (optionWidth / 2f)
    val rightCenterX = rowLeft + optionWidth + gap + (optionWidth / 2f)
    val optionCenterY = rowTop + (optionHeight / 2f)
    val leftDistance = kotlin.math.sqrt(((x - leftCenterX) * (x - leftCenterX)) + ((y - optionCenterY) * (y - optionCenterY)))
    val rightDistance = kotlin.math.sqrt(((x - rightCenterX) * (x - rightCenterX)) + ((y - optionCenterY) * (y - optionCenterY)))
    return if (leftDistance <= rightDistance) {
        ExperimentalGalleryReadingMode.HORIZONTAL
    } else {
        ExperimentalGalleryReadingMode.VERTICAL
    }
}

@Composable
private fun ExperimentalGalleryReadingModePreviewIcon(mode: ExperimentalGalleryReadingMode) {
    Box(
        modifier = Modifier
            .size(width = 58.dp, height = 42.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .padding(6.dp)
    ) {
        if (mode == ExperimentalGalleryReadingMode.HORIZONTAL) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.24f))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.24f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                )
            }
        }
    }
}

private fun experimentalAccentColorForMode(mode: AccentMode): Color? = when (mode) {
    AccentMode.AUTO -> null
    AccentMode.RED -> Color(0xFFE14B63)
    AccentMode.ORANGE -> Color(0xFFFF8A3D)
    AccentMode.AMBER -> Color(0xFFF4B400)
    AccentMode.GREEN -> Color(0xFF38B66B)
    AccentMode.TEAL -> Color(0xFF1FA7A1)
    AccentMode.BLUE -> Color(0xFF3B82F6)
    AccentMode.INDIGO -> Color(0xFF6366F1)
    AccentMode.PINK -> Color(0xFFEC4899)
}

private fun preferredOnExperimentalAccent(color: Color): Color {
    val lum = (0.299f * color.red) + (0.587f * color.green) + (0.114f * color.blue)
    return if (lum >= 0.62f) Color(0xFF111111) else Color.White
}

private fun applyExperimentalAccentMode(
    baseScheme: androidx.compose.material3.ColorScheme,
    accentMode: AccentMode,
    isDark: Boolean
): androidx.compose.material3.ColorScheme {
    val accent = experimentalAccentColorForMode(accentMode) ?: return baseScheme
    val onAccent = preferredOnExperimentalAccent(accent)
    val container = accent.copy(alpha = if (isDark) 0.34f else 0.22f)
    return baseScheme.copy(
        primary = accent,
        onPrimary = onAccent,
        secondary = accent,
        tertiary = accent,
        primaryContainer = container,
        secondaryContainer = container,
        tertiaryContainer = container
    )
}
