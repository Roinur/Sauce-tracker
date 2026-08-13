package com.example.saucetracker.feature.library.entries

import com.example.saucetracker.*
import com.example.saucetracker.core.media.*
import com.example.saucetracker.core.ui.components.*
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.key
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.saucetracker.core.ui.privacy.privacyObfuscate
import com.example.saucetracker.feature.dashboard.DashboardViewModel
import com.example.saucetracker.feature.library.privacy.LibraryIncognitoPolicy
import com.example.saucetracker.feature.library.detail.EntryCodeLine

internal fun LazyListScope.dashboardEntriesSection(
    vm: DashboardViewModel,
    localEntryHoldPopupState: MutableState<LocalEntryHoldPopupState?>,
    entryItemXByCode: MutableMap<Int, Float>,
    entryItemYByCode: MutableMap<Int, Float>,
    entryItemWidthByCode: MutableMap<Int, Float>,
    entryItemHeightByCode: MutableMap<Int, Float>,
    haptic: HapticFeedback,
    holdPopupScreenWidthPx: Float,
    holdPopupWidthPx: Float,
    selectedEntryDownloaded: Boolean,
    dashboardEntryDetailActions: DashboardEntryDetailActions,
    useReducedScrollThumbnails: Boolean,
    onCollapseHeatmap: () -> Unit,
    onPressStart: () -> Unit,
    runOnPressWhen: () -> Boolean,
    onSelectEntry: (Int) -> Unit
) {
                item {
                    if (vm.legacyHomeUi) {
                        Card {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Entries",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ImmediateActionText(
                                        label = vm.entryReadFilterLabel(),
                                        onAction = vm::cycleEntryReadFilter,
                                        onPressStart = onPressStart,
                                        runOnPressWhen = runOnPressWhen,
                                        textStyle = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    ImmediateActionText(
                                        label = if (vm.entriesCardCollapsed) "Expand" else "Collapse",
                                        onAction = {
                                            onCollapseHeatmap()
                                            vm.toggleEntriesCardCollapsed()
                                        },
                                        onPressStart = onPressStart,
                                        runOnPressWhen = runOnPressWhen,
                                        textStyle = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            if (!vm.entriesCardCollapsed) {
                                EntrySortControls(
                                    vm = vm,
                                    onPressStart = onPressStart,
                                    runOnPressWhen = runOnPressWhen
                                )
                            }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        "Entries",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        "${vm.entries.size} visible",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                ImmediateActionText(
                                    label = vm.entryReadFilterLabel(),
                                    onAction = vm::cycleEntryReadFilter,
                                    onPressStart = onPressStart,
                                    runOnPressWhen = runOnPressWhen,
                                    textStyle = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    
                        if (!vm.entriesCardCollapsed || !vm.legacyHomeUi) {
                            EntrySortControls(
                                vm = vm,
                                onPressStart = onPressStart,
                                runOnPressWhen = runOnPressWhen,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.70f),
                                        RoundedCornerShape(18.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            )
                        }
                        }
                    }
    }
    
    if (LibraryIncognitoPolicy.shouldRenderEntries(vm.legacyHomeUi, vm.entriesCardCollapsed) && vm.pureGalleryMode) {
        if (vm.pureGalleryMode) {
            val galleryColumns = vm.galleryColumns.coerceIn(1, 10)
            val galleryRows = vm.entries.chunked(galleryColumns)
            items(
                galleryRows,
                key = { row -> row.joinToString("_") { it.code.toString() } },
                contentType = { "entry_gallery_row" }
            ) { rowEntries ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(
                            animationSpec = androidx.compose.animation.core.tween(
                                durationMillis = 150,
                                easing = FastOutSlowInEasing
                            )
                        )
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowEntries.forEach { entry ->
                            val selected = vm.selectedCode == entry.code || vm.isEntryBatchDownloadSelected(entry.code)
                            val entryInteraction = remember { MutableInteractionSource() }
                            Box(modifier = Modifier.weight(1f)) {
                                EntrySwipeDismissContainer(
                                    code = entry.code,
                                    isPinned = entry.pinned,
                                    isRead = entry.isRead,
                                    incognitoModeEnabled = vm.incognitoModeEnabled,
                                    onTogglePinned = vm::quickToggleEntryPinned,
                                    onToggleRead = vm::quickToggleEntryRead
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHigh
                                            }
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                        border = if (selected) {
                                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                        } else {
                                            null
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heldSelectionMask(
                                                enabled = localEntryHoldPopupState.value?.code == entry.code,
                                                overlayColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .onGloballyPositioned { coordinates ->
                                                entryItemXByCode[entry.code] = coordinates.positionInRoot().x
                                                entryItemYByCode[entry.code] = coordinates.positionInRoot().y
                                                entryItemWidthByCode[entry.code] = coordinates.size.width.toFloat()
                                                entryItemHeightByCode[entry.code] = coordinates.size.height.toFloat()
                                            }
                                            .pointerInput(entry.code, vm.incognitoModeEnabled) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = { start ->
                                                        if (vm.incognitoModeEnabled) {
                                                            vm.setStatus("Rating changes are disabled in incognito mode.")
                                                            localEntryHoldPopupState.value = null
                                                            return@detectDragGesturesAfterLongPress
                                                        }
                                                        val absoluteX = (entryItemXByCode[entry.code] ?: 0f) + start.x
                                                        val initialRating = mapAbsoluteDragPositionToRating(
                                                            absoluteX = absoluteX,
                                                            screenWidthPx = holdPopupScreenWidthPx,
                                                            popupWidthPx = holdPopupWidthPx
                                                        )
                                                        localEntryHoldPopupState.value = LocalEntryHoldPopupState(
                                                            code = entry.code,
                                                            rating = initialRating
                                                        )
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    },
                                                    onDrag = { change, _ ->
                                                        val active = localEntryHoldPopupState.value
                                                        if (active == null || active.code != entry.code) {
                                                            return@detectDragGesturesAfterLongPress
                                                        }
                                                        val absoluteX = (entryItemXByCode[entry.code] ?: 0f) + change.position.x
                                                        val next = mapAbsoluteDragPositionToRating(
                                                            absoluteX = absoluteX,
                                                            screenWidthPx = holdPopupScreenWidthPx,
                                                            popupWidthPx = holdPopupWidthPx
                                                        )
                                                        if (next != active.rating) {
                                                            localEntryHoldPopupState.value = active.copy(rating = next)
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        }
                                                        change.consume()
                                                    },
                                                    onDragEnd = {
                                                        val active = localEntryHoldPopupState.value
                                                        localEntryHoldPopupState.value = null
                                                        if (active != null && active.code == entry.code && active.rating > 0) {
                                                            vm.setEntryRating(entry.code, active.rating)
                                                        }
                                                    },
                                                    onDragCancel = {
                                                        val active = localEntryHoldPopupState.value
                                                        if (active != null && active.code == entry.code) {
                                                            localEntryHoldPopupState.value = null
                                                        }
                                                    }
                                                )
                                            }
                                            .clickable(
                                                interactionSource = entryInteraction,
                                                indication = null
                                            ) {
                                                onSelectEntry(entry.code)
                                            }
                                    ) {
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            SelectedCardEdgeGlow(
                                                active = selected,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.matchParentSize()
                                            )
                                            PinnedCornerBleedGlow(
                                                visible = entry.pinned,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.matchParentSize()
                                            )
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(196.dp)
                                            ) {
                                                ThumbnailImage(
                                                    thumbnailUrl = entry.thumbnailUrl,
                                                    backupCode = entry.code,
                                                    contentDescription = "Cover for code ${entry.code}",
                                                    obscure = vm.incognitoModeEnabled,
                                                    preferLowRes = useReducedScrollThumbnails,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                IconButton(
                                                    onClick = { vm.requestToggleEntryPinned(entry.code) },
                                                    enabled = !vm.incognitoModeEnabled,
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .size(32.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_push_pin_24),
                                                        contentDescription = if (entry.pinned) {
                                                            "Unpin entry ${entry.code}"
                                                        } else {
                                                            "Pin entry ${entry.code}"
                                                        },
                                                        tint = if (vm.incognitoModeEnabled) {
                                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                                        } else if (entry.pinned) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        },
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            EntryCodeLine(
                                                code = entry.code,
                                                showSessionNewBadge = !selected && vm.isSessionNewEntry(entry.code),
                                                incognitoModeEnabled = vm.incognitoModeEnabled,
                                                textStyle = MaterialTheme.typography.bodySmall,
                                                codeColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = entry.title,
                                                modifier = Modifier.privacyObfuscate(
                                                    enabled = vm.incognitoModeEnabled,
                                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                ),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            RatingSummaryLine(
                                                rating = entry.averageRating,
                                                modifier = Modifier.privacyObfuscate(
                                                    enabled = vm.incognitoModeEnabled,
                                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                ),
                                                textColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (rowEntries.size < galleryColumns) {
                            repeat(galleryColumns - rowEntries.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
    
                    val selectedInRow = if (vm.isEntryDownloadBatchSelecting()) {
                        null
                    } else {
                        rowEntries.firstOrNull { it.code == vm.selectedCode }
                    }
                    if (selectedInRow != null) {
                        DashboardSelectedEntryDetail(
                            vm = vm,
                            code = selectedInRow.code,
                            selectedEntryDownloaded = selectedEntryDownloaded,
                            actions = dashboardEntryDetailActions
                        )
                    }
                }
            }
        } else {
        val normalColumns = vm.galleryColumns.coerceIn(1, 10)
        if (LibraryIncognitoPolicy.shouldRenderEntries(vm.legacyHomeUi, vm.entriesCardCollapsed) && normalColumns <= 0) {
        items(
            vm.entries,
            key = { it.code },
            contentType = { "entry_row" }
        ) { entry ->
            val selected = vm.selectedCode == entry.code || vm.isEntryBatchDownloadSelected(entry.code)
            val entryInteraction = remember { MutableInteractionSource() }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EntrySwipeDismissContainer(
                    code = entry.code,
                    isPinned = entry.pinned,
                    isRead = entry.isRead,
                    incognitoModeEnabled = vm.incognitoModeEnabled,
                    onTogglePinned = vm::quickToggleEntryPinned,
                    onToggleRead = vm::quickToggleEntryRead
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = if (selected) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heldSelectionMask(
                                enabled = localEntryHoldPopupState.value?.code == entry.code,
                                overlayColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            .onGloballyPositioned { coordinates ->
                                entryItemXByCode[entry.code] = coordinates.positionInRoot().x
                                entryItemYByCode[entry.code] = coordinates.positionInRoot().y
                                entryItemWidthByCode[entry.code] = coordinates.size.width.toFloat()
                                entryItemHeightByCode[entry.code] = coordinates.size.height.toFloat()
                            }
                            .pointerInput(entry.code, vm.incognitoModeEnabled) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { start ->
                                        if (vm.incognitoModeEnabled) {
                                            vm.setStatus("Rating changes are disabled in incognito mode.")
                                            localEntryHoldPopupState.value = null
                                            return@detectDragGesturesAfterLongPress
                                        }
                                        val absoluteX = (entryItemXByCode[entry.code] ?: 0f) + start.x
                                        val initialRating = mapAbsoluteDragPositionToRating(
                                            absoluteX = absoluteX,
                                            screenWidthPx = holdPopupScreenWidthPx,
                                            popupWidthPx = holdPopupWidthPx
                                        )
                                        localEntryHoldPopupState.value = LocalEntryHoldPopupState(
                                            code = entry.code,
                                            rating = initialRating
                                        )
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDrag = { change, _ ->
                                        val active = localEntryHoldPopupState.value
                                        if (active == null || active.code != entry.code) {
                                            return@detectDragGesturesAfterLongPress
                                        }
                                        val absoluteX = (entryItemXByCode[entry.code] ?: 0f) + change.position.x
                                        val next = mapAbsoluteDragPositionToRating(
                                            absoluteX = absoluteX,
                                            screenWidthPx = holdPopupScreenWidthPx,
                                            popupWidthPx = holdPopupWidthPx
                                        )
                                        if (next != active.rating) {
                                            localEntryHoldPopupState.value = active.copy(rating = next)
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        change.consume()
                                    },
                                    onDragEnd = {
                                        val active = localEntryHoldPopupState.value
                                        localEntryHoldPopupState.value = null
                                        if (active != null && active.code == entry.code && active.rating > 0) {
                                            vm.setEntryRating(entry.code, active.rating)
                                        }
                                    },
                                    onDragCancel = {
                                        val active = localEntryHoldPopupState.value
                                        if (active != null && active.code == entry.code) {
                                            localEntryHoldPopupState.value = null
                                        }
                                    }
                                )
                            }
                            .clickable(
                                interactionSource = entryInteraction,
                                indication = null
                            ) {
                                onSelectEntry(entry.code)
                            }
                    ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                    SelectedCardEdgeGlow(
                        active = selected,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.matchParentSize()
                    )
                    PinnedCornerBleedGlow(
                        visible = entry.pinned,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.matchParentSize()
                    )
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.title,
                                    modifier = Modifier.privacyObfuscate(
                                        enabled = vm.incognitoModeEnabled,
                                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                EntryCodeLine(
                                    code = entry.code,
                                    showSessionNewBadge = !selected && vm.isSessionNewEntry(entry.code),
                                    incognitoModeEnabled = vm.incognitoModeEnabled,
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    codeColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { vm.requestToggleEntryPinned(entry.code) },
                                enabled = !vm.incognitoModeEnabled,
                                modifier = Modifier
                                    .padding(top = 2.dp, end = 2.dp)
                                    .size(33.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_push_pin_24),
                                    contentDescription = if (entry.pinned) {
                                        "Unpin entry ${entry.code}"
                                    } else {
                                        "Pin entry ${entry.code}"
                                    },
                                    tint = if (vm.incognitoModeEnabled) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                    } else if (entry.pinned) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
    
                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(0.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "Pages: ${entry.numPages}",
                                            modifier = Modifier.privacyObfuscate(
                                                enabled = vm.incognitoModeEnabled,
                                                overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                            ),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            "Uploaded: ${entry.uploadDate.ifBlank { "-" }}",
                                            modifier = Modifier.privacyObfuscate(
                                                enabled = vm.incognitoModeEnabled,
                                                overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                            ),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Text(
                                        "Added: ${formatStoredUtcTimestampForDisplay(entry.addedAt)}",
                                        modifier = Modifier.privacyObfuscate(
                                            enabled = vm.incognitoModeEnabled,
                                            overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Status: ${if (entry.isRead) "Read" else "Unread"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (entry.isRead) READ_STATE_COLOR else UNREAD_STATE_COLOR,
                                            modifier = Modifier.privacyObfuscate(
                                                enabled = vm.incognitoModeEnabled,
                                                overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                            )
                                        )
                                    }
                                }
    
                                if (vm.showThumbnails && entry.thumbnailUrl.isNotBlank()) {
                                    ThumbnailImage(
                                        thumbnailUrl = entry.thumbnailUrl,
                                        backupCode = entry.code,
                                        contentDescription = "Cover for code ${entry.code}",
                                        obscure = vm.incognitoModeEnabled,
                                        preferLowRes = useReducedScrollThumbnails,
                                        modifier = Modifier
                                            .width(112.dp)
                                            .height(76.dp)
                                    )
                                }
                            }
    
                            RatingSummaryLine(
                                rating = entry.averageRating,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .privacyObfuscate(
                                        enabled = vm.incognitoModeEnabled,
                                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                    ),
                                textColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
    
                        Text(
                            text = "Tags: ${entry.tags.ifBlank { "-" }}",
                            modifier = Modifier.privacyObfuscate(
                                enabled = vm.incognitoModeEnabled,
                                overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        }
                    }
                }
    
                if (selected) {
                        DashboardSelectedEntryDetail(
                            vm = vm,
                            code = entry.code,
                            selectedEntryDownloaded = selectedEntryDownloaded,
                            actions = dashboardEntryDetailActions
                        )
                }
                    }
            }
        }
        }
        if (LibraryIncognitoPolicy.shouldRenderEntries(vm.legacyHomeUi, vm.entriesCardCollapsed) && normalColumns >= 1) {
            val normalRows = vm.entries.chunked(normalColumns)
            items(
                normalRows,
                key = { row -> row.joinToString("_") { it.code.toString() } },
                contentType = { "entry_row_grid" }
            ) { rowEntries ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowEntries.forEach { entry ->
                            val selected = vm.selectedCode == entry.code || vm.isEntryBatchDownloadSelected(entry.code)
                            val entryInteraction = remember { MutableInteractionSource() }
                            Box(modifier = Modifier.weight(1f)) {
                                EntrySwipeDismissContainer(
                                    code = entry.code,
                                    isPinned = entry.pinned,
                                    isRead = entry.isRead,
                                    incognitoModeEnabled = vm.incognitoModeEnabled,
                                    onTogglePinned = vm::quickToggleEntryPinned,
                                    onToggleRead = vm::quickToggleEntryRead
                                ) {
                                    Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        }
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    border = if (selected) {
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    } else {
                                        null
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heldSelectionMask(
                                            enabled = localEntryHoldPopupState.value?.code == entry.code,
                                            overlayColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .onGloballyPositioned { coordinates ->
                                            entryItemXByCode[entry.code] = coordinates.positionInRoot().x
                                            entryItemYByCode[entry.code] = coordinates.positionInRoot().y
                                            entryItemWidthByCode[entry.code] = coordinates.size.width.toFloat()
                                            entryItemHeightByCode[entry.code] = coordinates.size.height.toFloat()
                                        }
                                        .pointerInput(entry.code, vm.incognitoModeEnabled) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { start ->
                                                    if (vm.incognitoModeEnabled) {
                                                        vm.setStatus("Rating changes are disabled in incognito mode.")
                                                        localEntryHoldPopupState.value = null
                                                        return@detectDragGesturesAfterLongPress
                                                    }
                                                    val absoluteX = (entryItemXByCode[entry.code] ?: 0f) + start.x
                                                    val initialRating = mapAbsoluteDragPositionToRating(
                                                        absoluteX = absoluteX,
                                                        screenWidthPx = holdPopupScreenWidthPx,
                                                        popupWidthPx = holdPopupWidthPx
                                                    )
                                                    localEntryHoldPopupState.value = LocalEntryHoldPopupState(
                                                        code = entry.code,
                                                        rating = initialRating
                                                    )
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onDrag = { change, _ ->
                                                    val active = localEntryHoldPopupState.value
                                                    if (active == null || active.code != entry.code) {
                                                        return@detectDragGesturesAfterLongPress
                                                    }
                                                    val absoluteX = (entryItemXByCode[entry.code] ?: 0f) + change.position.x
                                                    val next = mapAbsoluteDragPositionToRating(
                                                        absoluteX = absoluteX,
                                                        screenWidthPx = holdPopupScreenWidthPx,
                                                        popupWidthPx = holdPopupWidthPx
                                                    )
                                                    if (next != active.rating) {
                                                        localEntryHoldPopupState.value = active.copy(rating = next)
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                    change.consume()
                                                },
                                                onDragEnd = {
                                                    val active = localEntryHoldPopupState.value
                                                    localEntryHoldPopupState.value = null
                                                    if (active != null && active.code == entry.code && active.rating > 0) {
                                                        vm.setEntryRating(entry.code, active.rating)
                                                    }
                                                },
                                                onDragCancel = {
                                                    val active = localEntryHoldPopupState.value
                                                    if (active != null && active.code == entry.code) {
                                                        localEntryHoldPopupState.value = null
                                                    }
                                                }
                                            )
                                        }
                                        .clickable(
                                            interactionSource = entryInteraction,
                                            indication = null
                                        ) {
                                            onSelectEntry(entry.code)
                                        }
                                    ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                SelectedCardEdgeGlow(
                                    active = selected,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.matchParentSize()
                                )
                                PinnedCornerBleedGlow(
                                    visible = entry.pinned,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.matchParentSize()
                                )
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = entry.title,
                                            modifier = Modifier
                                                .weight(1f)
                                                .privacyObfuscate(
                                                    enabled = vm.incognitoModeEnabled,
                                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                ),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        IconButton(
                                            onClick = { vm.requestToggleEntryPinned(entry.code) },
                                            enabled = !vm.incognitoModeEnabled,
                                            modifier = Modifier
                                                .padding(top = 1.dp, end = 1.dp)
                                                .size(28.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_push_pin_24),
                                                contentDescription = if (entry.pinned) {
                                                    "Unpin entry ${entry.code}"
                                                } else {
                                                    "Pin entry ${entry.code}"
                                                },
                                                tint = if (vm.incognitoModeEnabled) {
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                                } else if (entry.pinned) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
    
                                    EntryCodeLine(
                                        code = entry.code,
                                        showSessionNewBadge = !selected && vm.isSessionNewEntry(entry.code),
                                        incognitoModeEnabled = vm.incognitoModeEnabled,
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        codeColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Pages: ${entry.numPages}",
                                        modifier = Modifier.privacyObfuscate(
                                            enabled = vm.incognitoModeEnabled,
                                            overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                        ),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Uploaded: ${entry.uploadDate.ifBlank { "-" }}",
                                        modifier = Modifier.privacyObfuscate(
                                            enabled = vm.incognitoModeEnabled,
                                            overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Added: ${formatStoredUtcTimestampForDisplay(entry.addedAt)}",
                                        modifier = Modifier.privacyObfuscate(
                                            enabled = vm.incognitoModeEnabled,
                                            overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
    
                                    if (vm.showThumbnails && entry.thumbnailUrl.isNotBlank()) {
                                        ThumbnailImage(
                                            thumbnailUrl = entry.thumbnailUrl,
                                            backupCode = entry.code,
                                            contentDescription = "Cover for code ${entry.code}",
                                            obscure = vm.incognitoModeEnabled,
                                            preferLowRes = useReducedScrollThumbnails,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(86.dp)
                                        )
                                    }
    
                                    RatingSummaryLine(
                                        rating = entry.averageRating,
                                        modifier = Modifier.privacyObfuscate(
                                            enabled = vm.incognitoModeEnabled,
                                            overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                        ),
                                        textColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    }
                                }
                                }
                            }
                            }
                        }
                        if (rowEntries.size < normalColumns) {
                            repeat(normalColumns - rowEntries.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
    
                    val selectedInRow = if (vm.isEntryDownloadBatchSelecting()) {
                        null
                    } else {
                        rowEntries.firstOrNull { it.code == vm.selectedCode }
                    }
                    if (selectedInRow != null) {
                    DashboardSelectedEntryDetail(
                        vm = vm,
                        code = selectedInRow.code,
                        selectedEntryDownloaded = selectedEntryDownloaded,
                        actions = dashboardEntryDetailActions
                    )
                    }
                }
                }
            }
        }
    }
    }
    
    if (LibraryIncognitoPolicy.shouldRenderEntries(vm.legacyHomeUi, vm.entriesCardCollapsed) && !vm.pureGalleryMode) {
        val normalColumns = vm.galleryColumns.coerceIn(1, 10)
        val normalRows = vm.entries.chunked(normalColumns)
        items(
            normalRows,
            key = { row -> "normal_${row.joinToString("_") { it.code.toString() }}" },
            contentType = { "entry_row_grid_live" }
        ) { rowEntries ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowEntries.forEach { entry ->
                        val selected = vm.selectedCode == entry.code || vm.isEntryBatchDownloadSelected(entry.code)
                        val entryInteraction = remember { MutableInteractionSource() }
                        Box(modifier = Modifier.weight(1f)) {
                            EntrySwipeDismissContainer(
                                code = entry.code,
                                isPinned = entry.pinned,
                                isRead = entry.isRead,
                                incognitoModeEnabled = vm.incognitoModeEnabled,
                                onTogglePinned = vm::quickToggleEntryPinned,
                                onToggleRead = vm::quickToggleEntryRead
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        }
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    border = if (selected) {
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    } else {
                                        null
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heldSelectionMask(
                                            enabled = localEntryHoldPopupState.value?.code == entry.code,
                                            overlayColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .onGloballyPositioned { coordinates ->
                                            entryItemXByCode[entry.code] = coordinates.positionInRoot().x
                                            entryItemYByCode[entry.code] = coordinates.positionInRoot().y
                                            entryItemWidthByCode[entry.code] = coordinates.size.width.toFloat()
                                            entryItemHeightByCode[entry.code] = coordinates.size.height.toFloat()
                                        }
                                        .pointerInput(entry.code, vm.incognitoModeEnabled) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { start ->
                                                    if (vm.incognitoModeEnabled) {
                                                        vm.setStatus("Rating changes are disabled in incognito mode.")
                                                        localEntryHoldPopupState.value = null
                                                        return@detectDragGesturesAfterLongPress
                                                    }
                                                    val absoluteX = (entryItemXByCode[entry.code] ?: 0f) + start.x
                                                    val initialRating = mapAbsoluteDragPositionToRating(
                                                        absoluteX = absoluteX,
                                                        screenWidthPx = holdPopupScreenWidthPx,
                                                        popupWidthPx = holdPopupWidthPx
                                                    )
                                                    localEntryHoldPopupState.value = LocalEntryHoldPopupState(
                                                        code = entry.code,
                                                        rating = initialRating
                                                    )
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onDrag = { change, _ ->
                                                    val active = localEntryHoldPopupState.value
                                                    if (active == null || active.code != entry.code) {
                                                        return@detectDragGesturesAfterLongPress
                                                    }
                                                    val absoluteX = (entryItemXByCode[entry.code] ?: 0f) + change.position.x
                                                    val next = mapAbsoluteDragPositionToRating(
                                                        absoluteX = absoluteX,
                                                        screenWidthPx = holdPopupScreenWidthPx,
                                                        popupWidthPx = holdPopupWidthPx
                                                    )
                                                    if (next != active.rating) {
                                                        localEntryHoldPopupState.value = active.copy(rating = next)
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                    change.consume()
                                                },
                                                onDragEnd = {
                                                    val active = localEntryHoldPopupState.value
                                                    localEntryHoldPopupState.value = null
                                                    if (active != null && active.code == entry.code && active.rating > 0) {
                                                        vm.setEntryRating(entry.code, active.rating)
                                                    }
                                                },
                                                onDragCancel = {
                                                    val active = localEntryHoldPopupState.value
                                                    if (active != null && active.code == entry.code) {
                                                        localEntryHoldPopupState.value = null
                                                    }
                                                }
                                            )
                                        }
                                        .clickable(
                                            interactionSource = entryInteraction,
                                            indication = null
                                        ) {
                                            onSelectEntry(entry.code)
                                        }
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        SelectedCardEdgeGlow(
                                            active = selected,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.matchParentSize()
                                        )
                                        PinnedCornerBleedGlow(
                                            visible = entry.pinned,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.matchParentSize()
                                        )
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text(
                                                    text = entry.title,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .privacyObfuscate(
                                                            enabled = vm.incognitoModeEnabled,
                                                            overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                        ),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                IconButton(
                                                    onClick = { vm.requestToggleEntryPinned(entry.code) },
                                                    enabled = !vm.incognitoModeEnabled,
                                                    modifier = Modifier
                                                        .padding(top = 1.dp, end = 1.dp)
                                                        .size(28.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_push_pin_24),
                                                        contentDescription = if (entry.pinned) {
                                                            "Unpin entry ${entry.code}"
                                                        } else {
                                                            "Pin entry ${entry.code}"
                                                        },
                                                        tint = if (vm.incognitoModeEnabled) {
                                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                                        } else if (entry.pinned) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        },
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
    
                                            EntryCodeLine(
                                                code = entry.code,
                                                showSessionNewBadge = !selected && vm.isSessionNewEntry(entry.code),
                                                incognitoModeEnabled = vm.incognitoModeEnabled,
                                                textStyle = MaterialTheme.typography.bodySmall,
                                                codeColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Pages: ${entry.numPages}",
                                                modifier = Modifier.privacyObfuscate(
                                                    enabled = vm.incognitoModeEnabled,
                                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                ),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                text = "Uploaded: ${entry.uploadDate.ifBlank { "-" }}",
                                                modifier = Modifier.privacyObfuscate(
                                                    enabled = vm.incognitoModeEnabled,
                                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Added: ${formatStoredUtcTimestampForDisplay(entry.addedAt)}",
                                                modifier = Modifier.privacyObfuscate(
                                                    enabled = vm.incognitoModeEnabled,
                                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
    
                                            if (vm.showThumbnails && entry.thumbnailUrl.isNotBlank()) {
                                                ThumbnailImage(
                                                    thumbnailUrl = entry.thumbnailUrl,
                                                    backupCode = entry.code,
                                                    contentDescription = "Cover for code ${entry.code}",
                                                    obscure = vm.incognitoModeEnabled,
                                                    preferLowRes = useReducedScrollThumbnails,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(86.dp)
                                                )
                                            }
    
                                            RatingSummaryLine(
                                                rating = entry.averageRating,
                                                modifier = Modifier.privacyObfuscate(
                                                    enabled = vm.incognitoModeEnabled,
                                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                ),
                                                textColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Tags: ${entry.tags.ifBlank { "-" }}",
                                                modifier = Modifier.privacyObfuscate(
                                                    enabled = vm.incognitoModeEnabled,
                                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (rowEntries.size < normalColumns) {
                        repeat(normalColumns - rowEntries.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
    
                val selectedInRow = if (vm.isEntryDownloadBatchSelecting()) {
                    null
                } else {
                    rowEntries.firstOrNull { it.code == vm.selectedCode }
                }
                if (selectedInRow != null) {
                    DashboardSelectedEntryDetail(
                        vm = vm,
                        code = selectedInRow.code,
                        selectedEntryDownloaded = selectedEntryDownloaded,
                        actions = dashboardEntryDetailActions
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun EntrySortControls(
    vm: DashboardViewModel,
    onPressStart: () -> Unit,
    runOnPressWhen: () -> Boolean,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        item {
            EntrySortChip(
                label = "Rating",
                selected = vm.sortField == EntrySortField.RATING,
                activeDirection = if (vm.sortField == EntrySortField.RATING) SortDirection.DESC else null,
                onClick = vm::toggleRatingSort,
                onPressStart = onPressStart,
                runOnPressWhen = runOnPressWhen
            )
        }
        item {
            EntrySortChip(
                label = "Title${entrySortArrow(vm, EntrySortField.TITLE)}",
                selected = vm.sortField == EntrySortField.TITLE,
                activeDirection = if (vm.sortField == EntrySortField.TITLE) vm.sortDirection else null,
                onClick = { vm.onEntrySortClicked(EntrySortField.TITLE) },
                onPressStart = onPressStart,
                runOnPressWhen = runOnPressWhen
            )
        }
        item {
            EntrySortChip(
                label = "Pages${entrySortArrow(vm, EntrySortField.PAGES)}",
                selected = vm.sortField == EntrySortField.PAGES,
                activeDirection = if (vm.sortField == EntrySortField.PAGES) vm.sortDirection else null,
                onClick = { vm.onEntrySortClicked(EntrySortField.PAGES) },
                onPressStart = onPressStart,
                runOnPressWhen = runOnPressWhen
            )
        }
        item {
            EntrySortChip(
                label = "Uploaded${entrySortArrow(vm, EntrySortField.UPLOAD)}",
                selected = vm.sortField == EntrySortField.UPLOAD,
                activeDirection = if (vm.sortField == EntrySortField.UPLOAD) vm.sortDirection else null,
                onClick = { vm.onEntrySortClicked(EntrySortField.UPLOAD) },
                onPressStart = onPressStart,
                runOnPressWhen = runOnPressWhen
            )
        }
        item {
            EntrySortChip(
                label = "Fetched${entrySortArrow(vm, EntrySortField.ADDED)}",
                selected = vm.sortField == EntrySortField.ADDED,
                activeDirection = if (vm.sortField == EntrySortField.ADDED) vm.sortDirection else null,
                onClick = { vm.onEntrySortClicked(EntrySortField.ADDED) },
                onPressStart = onPressStart,
                runOnPressWhen = runOnPressWhen
            )
        }
        item {
            EntrySortChip(
                label = "Read${entrySortArrow(vm, EntrySortField.READ)}",
                selected = vm.sortField == EntrySortField.READ,
                activeDirection = if (vm.sortField == EntrySortField.READ) vm.sortDirection else null,
                onClick = { vm.onEntrySortClicked(EntrySortField.READ) },
                onPressStart = onPressStart,
                runOnPressWhen = runOnPressWhen
            )
        }
        item {
            EntrySortChip(
                label = "Pin",
                selected = vm.entryPinPriorityEnabled,
                activeDirection = if (vm.entryPinPriorityEnabled) SortDirection.DESC else null,
                onClick = vm::toggleEntryPinPriority,
                onPressStart = onPressStart,
                runOnPressWhen = runOnPressWhen
            )
        }
    }
}
