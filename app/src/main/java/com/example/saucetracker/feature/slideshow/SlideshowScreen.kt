@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.saucetracker.feature.slideshow

import com.example.saucetracker.*
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.saucetracker.core.network.HttpClientFactory
import com.example.saucetracker.core.network.HttpClientProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SlideshowScreen(
    code: Int,
    title: String,
    mediaId: Long,
    coverExt: String,
    numPages: Int,
    startPage: Int,
    incognitoModeEnabled: Boolean,
    initialReadingMode: SlideshowReadingMode,
    initialHorizontalDirection: SlideshowHorizontalDirection,
    localPageUris: List<String>,
    onPageViewed: (Int) -> Unit,
    onDone: () -> Unit
) {
    val slideshowViewModel: SlideshowViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val context = LocalContext.current
    val hasLocalPages = localPageUris.isNotEmpty()
    val safePages = if (hasLocalPages) {
        localPageUris.size.coerceAtLeast(0)
    } else {
        numPages.coerceAtLeast(0)
    }
    val hasData = if (hasLocalPages) {
        safePages > 0
    } else {
        mediaId > 0L && safePages > 0
    }
    val pageCount = safePages.coerceAtLeast(1)
    val initialPage = (startPage - 1).coerceIn(0, pageCount - 1)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pageCount })
    val verticalListState = rememberLazyListState(initialFirstVisibleItemIndex = initialPage)
    val scope = rememberCoroutineScope()
    var pageNavJob by remember { mutableStateOf<Job?>(null) }
    var showModeChooser by remember { mutableStateOf(false) }
    var readingModeName by rememberSaveable { mutableStateOf(initialReadingMode.name) }
    var requestedReadingModeName by rememberSaveable { mutableStateOf<String?>(null) }
    var rememberedPageIndex by rememberSaveable { mutableStateOf(initialPage) }
    var pendingModeSwitchPageIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var chooserHighlightedMode by remember { mutableStateOf<SlideshowReadingMode?>(null) }
    var immersionMode by rememberSaveable { mutableStateOf(false) }
    var modeSwitchInFlight by rememberSaveable { mutableStateOf(false) }
    val readingMode = SlideshowReadingMode.entries.firstOrNull { it.name == readingModeName } ?: initialReadingMode
    val horizontalDirection = remember(initialHorizontalDirection) { initialHorizontalDirection }
    val mangaDirection = horizontalDirection == SlideshowHorizontalDirection.MANGA
    var verticalReadyPages by remember { mutableStateOf(setOf<Int>()) }
    var verticalRevealTargets by rememberSaveable { mutableStateOf(listOf(initialPage)) }
    var verticalRevealPending by rememberSaveable {
        mutableStateOf(initialReadingMode == SlideshowReadingMode.VERTICAL)
    }
    val density = LocalDensity.current
    val chromeOffsetY by animateDpAsState(
        targetValue = if (immersionMode) (-4).dp else 0.dp,
        animationSpec = tween(
            durationMillis = 320,
            easing = FastOutSlowInEasing
        ),
        label = "slideshowChromeOffsetY"
    )
    val slideshowContentAlpha by animateFloatAsState(
        targetValue = if (modeSwitchInFlight) 0.08f else 1f,
        animationSpec = tween(
            durationMillis = if (modeSwitchInFlight) 180 else 210,
            easing = if (modeSwitchInFlight) FastOutSlowInEasing else LinearOutSlowInEasing
        ),
        label = "slideshowContentAlpha"
    )
    var switchOverlayVisible by rememberSaveable { mutableStateOf(false) }
    val switchOverlayAlpha by animateFloatAsState(
        targetValue = if (switchOverlayVisible) 0.92f else 0f,
        animationSpec = tween(
            durationMillis = if (switchOverlayVisible) 180 else 220,
            easing = if (switchOverlayVisible) FastOutSlowInEasing else LinearOutSlowInEasing
        ),
        label = "slideshowModeSwitchOverlayAlpha"
    )
    val verticalRevealAlpha by animateFloatAsState(
        targetValue = if (verticalRevealPending && readingMode == SlideshowReadingMode.VERTICAL) 0f else 1f,
        animationSpec = tween(
            durationMillis = 220,
            easing = LinearOutSlowInEasing
        ),
        label = "slideshowVerticalRevealAlpha"
    )
    val verticalRevealScrimAlpha by animateFloatAsState(
        targetValue = if (verticalRevealPending && readingMode == SlideshowReadingMode.VERTICAL) 1f else 0f,
        animationSpec = tween(
            durationMillis = 220,
            easing = LinearOutSlowInEasing
        ),
        label = "slideshowVerticalRevealScrimAlpha"
    )
    val currentVerticalPageIndex = currentVerticalPageIndex(verticalListState, safePages)
    var rootViewportSize by remember { mutableStateOf(IntSize.Zero) }

    fun computeVerticalRevealTargets(centerIndex: Int): List<Int> {
        if (safePages <= 0) return emptyList()
        val target = centerIndex.coerceIn(0, safePages - 1)
        return buildList {
            add(target)
            if (target > 0) add(target - 1)
            if (target < safePages - 1) add(target + 1)
        }.distinct()
    }

    SlideshowApplySystemBars(immersionMode = immersionMode)

    suspend fun scrollVerticalToCenteredPage(target: Int) {
        val safeTarget = target.coerceIn(0, safePages - 1)
        if (safeTarget <= 0 || safeTarget >= safePages - 1) {
            verticalListState.scrollToItem(safeTarget)
            return
        }
        repeat(6) {
            verticalListState.scrollToItem(safeTarget)
            val layoutInfo = verticalListState.layoutInfo
            val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == safeTarget } ?: return
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
            val itemCenter = itemInfo.offset + (itemInfo.size / 2f)
            val delta = itemCenter - viewportCenter
            if (abs(delta) > 1f) {
                verticalListState.scrollBy(delta)
            }
            if (abs(delta) <= 1f) {
                return
            }
            delay(24)
        }
    }

    fun setReadingMode(nextMode: SlideshowReadingMode) {
        if (nextMode == readingMode) {
            showModeChooser = false
            return
        }
        val currentPageIndex = rememberedPageIndex.coerceIn(0, pageCount - 1)
        rememberedPageIndex = currentPageIndex
        pendingModeSwitchPageIndex = currentPageIndex
        if (nextMode == SlideshowReadingMode.VERTICAL) {
            verticalRevealTargets = computeVerticalRevealTargets(currentPageIndex)
            verticalRevealPending = true
        }
        modeSwitchInFlight = true
        switchOverlayVisible = true
        requestedReadingModeName = nextMode.name
        showModeChooser = false
    }

    fun updateChooserHover(
        x: Float,
        y: Float,
        widthPx: Float,
        heightPx: Float
    ) {
        chooserHighlightedMode = mapSlideshowModeChoice(
            x = x,
            y = y,
            screenWidthPx = widthPx,
            screenHeightPx = heightPx,
            density = density.density
        ) ?: chooserHighlightedMode ?: readingMode
    }

    LaunchedEffect(hasData, safePages, pagerState.currentPage, readingMode, pendingModeSwitchPageIndex) {
        if (!hasData || safePages <= 0 || readingMode != SlideshowReadingMode.HORIZONTAL || pendingModeSwitchPageIndex != null) return@LaunchedEffect
        val currentIndex = pagerState.currentPage.coerceIn(0, safePages - 1)
        rememberedPageIndex = currentIndex
        onPageViewed(currentIndex)
    }

    LaunchedEffect(hasData, safePages, verticalListState.firstVisibleItemIndex, verticalListState.firstVisibleItemScrollOffset, readingMode, pendingModeSwitchPageIndex) {
        if (!hasData || safePages <= 0 || readingMode != SlideshowReadingMode.VERTICAL || pendingModeSwitchPageIndex != null) return@LaunchedEffect
        val currentIndex = currentVerticalPageIndex
        rememberedPageIndex = currentIndex
        onPageViewed(currentIndex)
    }

    LaunchedEffect(readingMode, safePages) {
        if (!hasData || safePages <= 0) return@LaunchedEffect
        if (readingMode == SlideshowReadingMode.VERTICAL && verticalRevealTargets.isEmpty()) {
            verticalRevealTargets = computeVerticalRevealTargets(rememberedPageIndex)
            verticalRevealPending = true
        }
    }

    LaunchedEffect(readingMode, verticalReadyPages, verticalRevealTargets) {
        if (readingMode != SlideshowReadingMode.VERTICAL) return@LaunchedEffect
        if (verticalRevealTargets.isNotEmpty() && verticalRevealTargets.all { it in verticalReadyPages }) {
            verticalRevealPending = false
        }
    }

    LaunchedEffect(modeSwitchInFlight, requestedReadingModeName, pendingModeSwitchPageIndex, safePages) {
        val requested = requestedReadingModeName ?: return@LaunchedEffect
        val pendingTarget = pendingModeSwitchPageIndex ?: return@LaunchedEffect
        if (!modeSwitchInFlight || safePages <= 0) return@LaunchedEffect
        val target = pendingTarget.coerceIn(0, safePages - 1)
        try {
            delay(150)
            val targetMode = SlideshowReadingMode.entries.firstOrNull { it.name == requested } ?: initialReadingMode
            readingModeName = requested
            storeSlideshowReadingMode(context, targetMode)
            delay(40)
            withTimeoutOrNull(900) {
                when (targetMode) {
                    SlideshowReadingMode.HORIZONTAL -> if (pagerState.currentPage != target) pagerState.scrollToPage(target)
                    SlideshowReadingMode.VERTICAL -> scrollVerticalToCenteredPage(target)
                }
            }
            rememberedPageIndex = target
            delay(140)
        } finally {
            pendingModeSwitchPageIndex = null
            requestedReadingModeName = null
            modeSwitchInFlight = false
            switchOverlayVisible = false
        }
    }

    fun jumpToRelativePage(delta: Int) {
        if (!hasData || safePages <= 0 || delta == 0) return
        val current = pagerState.currentPage
        val target = (current + delta).coerceIn(0, safePages - 1)
        if (target == current) return

        pageNavJob?.cancel()
        pageNavJob = scope.launch {
            runCatching {
                pagerState.animateScrollToPage(target)
            }
        }
    }

    BackHandler(enabled = true) {
        onDone()
    }

    LaunchedEffect(hasData, hasLocalPages, mediaId, safePages, coverExt, pagerState.currentPage) {
        if (!hasData || hasLocalPages) return@LaunchedEffect
        val currentPage = pagerState.currentPage + 1
        withContext(Dispatchers.IO) {
            listOf(currentPage + 1, currentPage + 2)
                .filter { it in 1..safePages }
                .forEach { page ->
                    slideshowViewModel.loadRemotePage(
                        mediaId = mediaId,
                        pageNumber = page,
                        preferredExt = coverExt
                    )
                }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .onGloballyPositioned { rootViewportSize = it.size }
                .pointerInput(hasData, safePages, readingMode) {
                        if (readingMode != SlideshowReadingMode.HORIZONTAL || showModeChooser) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(
                                pass = PointerEventPass.Final,
                                requireUnconsumed = false
                            )
                            if (!hasData || safePages <= 0) return@awaitEachGesture
                            val longPress = awaitLongPressOrCancellation(down.id)
                            if (longPress == null) return@awaitEachGesture

                            showModeChooser = true
                            chooserHighlightedMode = readingMode
                            updateChooserHover(
                                x = longPress.position.x,
                                y = longPress.position.y,
                                widthPx = size.width.toFloat(),
                                heightPx = size.height.toFloat()
                            )

                            var finished = false
                            while (!finished) {
                                val event = awaitPointerEvent(pass = PointerEventPass.Final)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                                if (change == null) {
                                    finished = true
                                } else if (!change.pressed || change.changedToUpIgnoreConsumed()) {
                                    chooserHighlightedMode?.let { setReadingMode(it) }
                                    finished = true
                                } else {
                                    updateChooserHover(
                                        x = change.position.x,
                                        y = change.position.y,
                                        widthPx = size.width.toFloat(),
                                        heightPx = size.height.toFloat()
                                    )
                                }
                            }
                            showModeChooser = false
                            chooserHighlightedMode = null
                        }
                    }
        ) {
            if (!hasData) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(alpha = slideshowContentAlpha)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Image data is not available for code $code.",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = onDone) {
                        Text("Done")
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(alpha = slideshowContentAlpha)
                ) {
                when (readingMode) {
                    SlideshowReadingMode.HORIZONTAL -> {
                        HorizontalPager(
                            state = pagerState,
                            reverseLayout = mangaDirection,
                            userScrollEnabled = !showModeChooser,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(hasData, safePages, showModeChooser) {
                                    val tapSlopPx = 24.dp.toPx()
                                    val exitSwipeThresholdPx = 72f
                                    awaitEachGesture {
                                        if (showModeChooser) {
                                            val down = awaitFirstDown(
                                                pass = PointerEventPass.Final,
                                                requireUnconsumed = false
                                            )
                                            down.consume()
                                            while (true) {
                                                val event = awaitPointerEvent(pass = PointerEventPass.Final)
                                                event.changes.forEach { it.consume() }
                                                if (event.changes.none { it.pressed }) break
                                            }
                                            return@awaitEachGesture
                                        }
                                        val down = awaitFirstDown(pass = PointerEventPass.Final, requireUnconsumed = false)
                                        if (!hasData || safePages <= 0) return@awaitEachGesture

                                        val lastPage = safePages - 1
                                        val startedPage = pagerState.currentPage
                                        var movedPastTapSlop = false
                                        var totalDx = 0f
                                        var totalDy = 0f
                                        while (true) {
                                            val event = awaitPointerEvent(pass = PointerEventPass.Final)
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                            val delta = change.positionChange()
                                            totalDx += delta.x
                                            totalDy += delta.y
                                            if (!movedPastTapSlop && (abs(totalDx) > tapSlopPx || abs(totalDy) > tapSlopPx)) {
                                                movedPastTapSlop = true
                                            }
                                            if (startedPage == lastPage && totalDx < -exitSwipeThresholdPx) {
                                                onDone()
                                                break
                                            }
                                            if (change.changedToUpIgnoreConsumed() || !change.pressed) {
                                                if (!movedPastTapSlop) {
                                                    if (isSlideshowCenterTap(down.position.x, down.position.y, size.width.toFloat(), size.height.toFloat())) {
                                                        immersionMode = !immersionMode
                                                    } else if (down.position.x < size.width / 2f) {
                                                        if (safePages > 1) jumpToRelativePage(if (mangaDirection) 1 else -1)
                                                    } else {
                                                        if (mangaDirection) {
                                                            if (pagerState.currentPage <= 0) onDone() else jumpToRelativePage(-1)
                                                        } else {
                                                            if (pagerState.currentPage >= safePages - 1) onDone() else jumpToRelativePage(1)
                                                        }
                                                    }
                                                }
                                                break
                                            }
                                        }
                                    }
                                }
                        ) { index ->
                            if (hasLocalPages) {
                                LocalGalleryPage(
                                    pageUriString = localPageUris.getOrNull(index).orEmpty(),
                                    pageNumber = index + 1,
                                    obscure = incognitoModeEnabled
                                )
                            } else {
                                GalleryPage(
                                    mediaId = mediaId,
                                    pageNumber = index + 1,
                                    preferredExt = coverExt,
                                    obscure = incognitoModeEnabled
                                )
                            }
                        }
                    }
                    SlideshowReadingMode.VERTICAL -> {
                        LazyColumn(
                            state = verticalListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = verticalRevealAlpha },
                            userScrollEnabled = !showModeChooser,
                            contentPadding = PaddingValues(
                                start = 14.dp,
                                end = 14.dp,
                                top = 12.dp,
                                bottom = 12.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(safePages, key = { it }) { index ->
                                if (hasLocalPages) {
                                    VerticalLocalGalleryPageCard(
                                        pageUriString = localPageUris.getOrNull(index).orEmpty(),
                                        pageNumber = index + 1,
                                        obscure = incognitoModeEnabled,
                                        showPageLabel = false,
                                        onReady = { verticalReadyPages = verticalReadyPages + index },
                                        onCenterTap = { immersionMode = !immersionMode },
                                        onCenterLongPress = {
                                            showModeChooser = true
                                            chooserHighlightedMode = readingMode
                                        },
                                        rootViewportSize = rootViewportSize,
                                        onStartModeChooser = { x, y ->
                                            showModeChooser = true
                                            chooserHighlightedMode = readingMode
                                            updateChooserHover(
                                                x = x,
                                                y = y,
                                                widthPx = rootViewportSize.width.toFloat(),
                                                heightPx = rootViewportSize.height.toFloat()
                                            )
                                        },
                                        onUpdateModeChooser = { x, y ->
                                            updateChooserHover(
                                                x = x,
                                                y = y,
                                                widthPx = rootViewportSize.width.toFloat(),
                                                heightPx = rootViewportSize.height.toFloat()
                                            )
                                        },
                                        onFinishModeChooser = {
                                            chooserHighlightedMode?.let { setReadingMode(it) }
                                            showModeChooser = false
                                            chooserHighlightedMode = null
                                        }
                                    )
                                } else {
                                    VerticalRemoteGalleryPageCard(
                                        mediaId = mediaId,
                                        pageNumber = index + 1,
                                        preferredExt = coverExt,
                                        obscure = incognitoModeEnabled,
                                        showPageLabel = false,
                                        onReady = { verticalReadyPages = verticalReadyPages + index },
                                        onCenterTap = { immersionMode = !immersionMode },
                                        onCenterLongPress = {
                                            showModeChooser = true
                                            chooserHighlightedMode = readingMode
                                        },
                                        rootViewportSize = rootViewportSize,
                                        onStartModeChooser = { x, y ->
                                            showModeChooser = true
                                            chooserHighlightedMode = readingMode
                                            updateChooserHover(
                                                x = x,
                                                y = y,
                                                widthPx = rootViewportSize.width.toFloat(),
                                                heightPx = rootViewportSize.height.toFloat()
                                            )
                                        },
                                        onUpdateModeChooser = { x, y ->
                                            updateChooserHover(
                                                x = x,
                                                y = y,
                                                widthPx = rootViewportSize.width.toFloat(),
                                                heightPx = rootViewportSize.height.toFloat()
                                            )
                                        },
                                        onFinishModeChooser = {
                                            chooserHighlightedMode?.let { setReadingMode(it) }
                                            showModeChooser = false
                                            chooserHighlightedMode = null
                                        }
                                    )
                                }
                            }
                        }
                        if (verticalRevealScrimAlpha > 0.001f) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(MaterialTheme.colorScheme.background.copy(alpha = verticalRevealScrimAlpha))
                            )
                        }
                    }
                }
                }
            }

                AnimatedVisibility(
                    visible = !immersionMode,
                    enter = fadeIn(animationSpec = tween(280, easing = LinearOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing)),
                    modifier = Modifier
                        .graphicsLayer(alpha = slideshowContentAlpha)
                        .align(Alignment.TopCenter)
                        .offset(y = chromeOffsetY)
            ) {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                        scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    title = {
                        Text(
                            text = title.ifBlank { "Gallery $code" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.slideshowPrivacyObfuscate(
                                enabled = incognitoModeEnabled,
                                overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = SLIDESHOW_INCOGNITO_OVERLAY_ALPHA)
                            )
                        )
                    },
                    actions = {
                        TextButton(
                            onClick = onDone,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text("Done", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .graphicsLayer(alpha = slideshowContentAlpha)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                AnimatedVisibility(
                    visible = !immersionMode,
                    enter = fadeIn(animationSpec = tween(280, easing = LinearOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing))
                ) {
                    when (readingMode) {
                        SlideshowReadingMode.HORIZONTAL -> {
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            if (mangaDirection) {
                                                if (pagerState.currentPage < safePages - 1) jumpToRelativePage(1)
                                            } else if (pagerState.currentPage > 0) {
                                                jumpToRelativePage(-1)
                                            }
                                        },
                                        enabled = if (mangaDirection) pagerState.currentPage < safePages - 1 else pagerState.currentPage > 0
                                    ) {
                                        Text(if (mangaDirection) "Next" else "Prev", fontWeight = FontWeight.SemiBold)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${pagerState.currentPage + 1} / $safePages",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    TextButton(
                                        onClick = {
                                            if (mangaDirection) {
                                                if (pagerState.currentPage > 0) jumpToRelativePage(-1) else onDone()
                                            } else {
                                                if (pagerState.currentPage < safePages - 1) jumpToRelativePage(1) else onDone()
                                            }
                                        },
                                        enabled = hasData
                                    ) {
                                        Text(
                                            when {
                                                mangaDirection && pagerState.currentPage <= 0 -> "Exit"
                                                !mangaDirection && pagerState.currentPage >= safePages - 1 -> "Exit"
                                                mangaDirection -> "Prev"
                                                else -> "Next"
                                            },
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                        SlideshowReadingMode.VERTICAL -> {
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                shape = RoundedCornerShape(22.dp),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
                                )
                            ) {
                                Text(
                                    text = "${currentVerticalPageIndex.coerceIn(0, safePages - 1) + 1} / $safePages",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (switchOverlayAlpha > 0.001f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(alpha = switchOverlayAlpha)
                        .background(MaterialTheme.colorScheme.background)
                )
            }

            if (showModeChooser) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(readingMode) {
                            awaitEachGesture {
                                val down = awaitFirstDown(
                                    pass = PointerEventPass.Final,
                                    requireUnconsumed = false
                                )
                                updateChooserHover(
                                    x = down.position.x,
                                    y = down.position.y,
                                    widthPx = size.width.toFloat(),
                                    heightPx = size.height.toFloat()
                                )
                                var finished = false
                                while (!finished) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Final)
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                                    if (change == null) {
                                        finished = true
                                    } else if (!change.pressed || change.changedToUpIgnoreConsumed()) {
                                        chooserHighlightedMode?.let { setReadingMode(it) }
                                        finished = true
                                    } else {
                                        updateChooserHover(
                                            x = change.position.x,
                                            y = change.position.y,
                                            widthPx = size.width.toFloat(),
                                            heightPx = size.height.toFloat()
                                        )
                                    }
                                }
                                showModeChooser = false
                                chooserHighlightedMode = null
                            }
                        }
                ) {
                    SlideshowReadingModeChooser(
                        currentMode = chooserHighlightedMode ?: readingMode
                    )
                }
            }
        }
    }
}


