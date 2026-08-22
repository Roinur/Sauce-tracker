package com.roinur.saucetracker.feature.suggestions

import com.roinur.saucetracker.*
import com.roinur.saucetracker.core.media.*
import com.roinur.saucetracker.core.ui.components.*
import com.roinur.saucetracker.core.ui.privacy.privacyObfuscate
import com.roinur.saucetracker.feature.dashboard.DashboardViewModel
import com.roinur.saucetracker.feature.library.entries.EntrySwipeDismissContainer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import java.util.Locale

@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
internal fun DashboardSuggestionsSection(
    vm: DashboardViewModel,
    listState: LazyListState,
    maxHeight: Dp,
    preferLowRes: Boolean,
    suggestedDuplicateComparisonState: MutableState<SuggestedDuplicateComparisonState?>,
    entryItemYByCode: MutableMap<Int, Float>,
    entryItemWidthByCode: MutableMap<Int, Float>,
    entryItemHeightByCode: MutableMap<Int, Float>,
    haptic: HapticFeedback,
    onShowWeights: () -> Unit,
    onCollapseHeatmap: () -> Unit,
    onPressStart: () -> Unit,
    runOnPressWhen: () -> Boolean
) {
    if (vm.legacyHomeUi) {
        LegacyDashboardSuggestionsSection(
            vm = vm,
            listState = listState,
            maxHeight = maxHeight,
            preferLowRes = preferLowRes,
            suggestedDuplicateComparisonState = suggestedDuplicateComparisonState,
            entryItemYByCode = entryItemYByCode,
            entryItemWidthByCode = entryItemWidthByCode,
            entryItemHeightByCode = entryItemHeightByCode,
            haptic = haptic,
            onShowWeights = onShowWeights,
            onCollapseHeatmap = onCollapseHeatmap,
            onPressStart = onPressStart,
            runOnPressWhen = runOnPressWhen
        )
        return
    }

    var showTasteTraining by rememberSaveable { mutableStateOf(false) }
    var openTasteTrainingWhenReady by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(openTasteTrainingWhenReady, vm.tasteTrainingPromptLoading) {
        if (openTasteTrainingWhenReady && !vm.tasteTrainingPromptLoading) {
            openTasteTrainingWhenReady = false
            showTasteTraining = true
        }
    }
    if (showTasteTraining) {
        TasteTrainingDialog(vm = vm, onDismiss = { showTasteTraining = false })
    }

    LaunchedEffect(Unit) {
        if (vm.suggestedEntriesCollapsed) {
            vm.toggleSuggestedEntriesCollapsed()
        }
    }

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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    "Suggested entries",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        vm.suggestedEntries.isNotEmpty() -> "${vm.suggestedEntries.size} recommendations"
                        vm.suggestedEntriesLoading -> "Building recommendations"
                        else -> "Based on your local library"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                TextButton(
                    onClick = {
                        openTasteTrainingWhenReady = true
                        vm.refreshTasteTrainingPrompts()
                    },
                    enabled = !vm.incognitoModeEnabled && !vm.tasteTrainingPromptLoading,
                    contentPadding = PaddingValues(horizontal = 5.dp, vertical = 8.dp)
                ) {
                    Text(
                        if (vm.tasteTrainingPromptLoading) "Preparing" else "Train",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                TextButton(
                    onClick = onShowWeights,
                    contentPadding = PaddingValues(horizontal = 5.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Tune",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                TextButton(
                    onClick = vm::refreshSuggestedEntriesForCurrentSession,
                    contentPadding = PaddingValues(horizontal = 5.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Refresh",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SuggestionMode.entries.forEach { mode ->
                FilterChip(
                    selected = vm.suggestionMode == mode,
                    onClick = { vm.updateSuggestionMode(mode) },
                    label = {
                        Text(
                            text = mode.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        vm.suggestedEntriesInfoMessage?.let { info ->
            Text(
                text = info,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (vm.incognitoModeEnabled) {
            Text(
                text = "Suggested entries are hidden while incognito mode is enabled.",
                modifier = Modifier.privacyObfuscate(
                    enabled = true,
                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            if (vm.suggestedEntriesLoading) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(999.dp))
                    )
                    Text(
                        text = if (vm.suggestedEntries.isEmpty()) {
                            "Building recommendations from your library..."
                        } else {
                            "Updating recommendations. Your current list remains available."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (vm.suggestedEntries.isEmpty() && !vm.suggestedEntriesLoading) {
                Column(
                    modifier = Modifier.padding(vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Nothing to recommend yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Read or rate a few entries, then refresh to build recommendations.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = vm::refreshSuggestedEntriesForCurrentSession) {
                        Text("Refresh suggestions")
                    }
                }
            } else if (vm.suggestedEntries.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp, max = maxHeight),
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(
                                vm.suggestedEntries,
                                key = { it.code },
                                contentType = { "suggested_entry_row" }
                            ) { suggestion ->
                                val suggestionPinned = vm.entryPinnedForCode(suggestion.code)
                                val suggestionRead = vm.entryReadForCode(suggestion.code)
                                val suggestionInteraction = remember { MutableInteractionSource() }
                                var suggestedHoldAction by remember(suggestion.code) { mutableStateOf<SuggestedDragAction?>(null) }
                                val suggestionCardShape = RoundedCornerShape(18.dp)
                                val suggestionOutlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
                                EntrySwipeDismissContainer(
                                    code = suggestion.code,
                                    isPinned = suggestionPinned,
                                    isRead = suggestionRead,
                                    incognitoModeEnabled = vm.incognitoModeEnabled,
                                    onTogglePinned = vm::quickToggleSuggestedPinned,
                                    onToggleRead = vm::quickToggleSuggestedRead,
                                    backgroundShape = suggestionCardShape
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                        shape = suggestionCardShape,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .drawWithContent {
                                                drawContent()
                                                val strokeWidth = 0.5.dp.toPx()
                                                val halfStroke = strokeWidth / 2f
                                                val cornerRadius = 18.dp.toPx() - halfStroke
                                                drawRoundRect(
                                                    color = suggestionOutlineColor,
                                                    topLeft = Offset(halfStroke, halfStroke),
                                                    size = Size(
                                                        width = size.width - strokeWidth,
                                                        height = size.height - strokeWidth
                                                    ),
                                                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                                                    style = Stroke(width = strokeWidth)
                                                )
                                            }
                                            .heldSelectionMask(
                                                enabled = suggestedHoldAction != null,
                                                overlayColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .onGloballyPositioned { coordinates ->
                                                entryItemYByCode[suggestion.code] = coordinates.positionInRoot().y
                                                entryItemWidthByCode[suggestion.code] = coordinates.size.width.toFloat()
                                                entryItemHeightByCode[suggestion.code] = coordinates.size.height.toFloat()
                                            }
                                            .pointerInput(suggestion.code, vm.incognitoModeEnabled) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = { start ->
                                                        val width = entryItemWidthByCode[suggestion.code] ?: 1f
                                                        val initialAction = mapDragPositionToSuggestedAction(start.x, width)
                                                        suggestedHoldAction = initialAction
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    },
                                                    onDrag = { change, _ ->
                                                        val active = suggestedHoldAction
                                                            ?: return@detectDragGesturesAfterLongPress
                                                        val width = entryItemWidthByCode[suggestion.code] ?: 1f
                                                        val next = mapDragPositionToSuggestedAction(change.position.x, width)
                                                        if (next != active) {
                                                            suggestedHoldAction = next
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        }
                                                        change.consume()
                                                    },
                                                    onDragEnd = {
                                                        val action = suggestedHoldAction
                                                        suggestedHoldAction = null
                                                        if (action == SuggestedDragAction.HIDE) {
                                                            vm.hideSuggestedEntry(
                                                                code = suggestion.code,
                                                                thumbnailUrl = suggestion.thumbnailUrl
                                                            )
                                                        }
                                                    },
                                                    onDragCancel = {
                                                        suggestedHoldAction = null
                                                    }
                                                )
                                            }
                                            .clickable(
                                                interactionSource = suggestionInteraction,
                                                indication = null
                                            ) {
                                                vm.openSuggestedEntryInBrowser(suggestion.code)
                                            }
                                    ) {
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            TriggeredShimmerOverlay(
                                                triggerKey = vm.suggestedImportFlashEpochs[suggestion.code] ?: 0,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.matchParentSize(),
                                                holdDurationMs = 900L
                                            )
                                            AccentPulseOverlay(
                                                triggerKey = vm.suggestedImportFlashEpochs[suggestion.code] ?: 0,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.matchParentSize()
                                            )
                                            PinnedCornerBleedGlow(
                                                visible = vm.entryPinnedForCode(suggestion.code),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.matchParentSize()
                                            )
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    if (suggestion.thumbnailUrl.isNotBlank()) {
                                                        ThumbnailImage(
                                                            thumbnailUrl = suggestion.thumbnailUrl,
                                                            backupCode = suggestion.code,
                                                            contentDescription = "Suggestion cover for code ${suggestion.code}",
                                                            obscure = vm.incognitoModeEnabled,
                                                            preferLowRes = preferLowRes,
                                                            modifier = Modifier
                                                                .width(112.dp)
                                                                .height(148.dp)
                                                        )
                                                    }
                                                    Column(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .privacyObfuscate(
                                                                enabled = vm.incognitoModeEnabled,
                                                                overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                                                    alpha = INCOGNITO_OVERLAY_ALPHA
                                                                ),
                                                                blurRadius = 9.dp,
                                                                cornerRadius = 8.dp
                                                            ),
                                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                                    ) {
                                                        Text(
                                                            text = "#${suggestion.code}",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Text(
                                                            text = suggestion.title,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "${suggestion.numPages} pages  •  ${suggestion.uploadDate.ifBlank { "Unknown date" }}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = "Match score ${"%.2f".format(Locale.US, suggestion.score)}",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        if (suggestion.duplicateHint == null && suggestion.whySuggestedReason.isNotBlank()) {
                                                            Text(
                                                                text = suggestion.whySuggestedReason,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 3,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
    
                                                if (suggestion.topTags.isNotEmpty()) {
                                                    Row(
                                                        modifier = Modifier.privacyObfuscate(
                                                            enabled = vm.incognitoModeEnabled,
                                                            overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                                                alpha = INCOGNITO_OVERLAY_ALPHA
                                                            )
                                                        ),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        suggestion.topTags.forEach { tag ->
                                                            Text(
                                                                text = tag,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(999.dp))
                                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                                            )
                                                        }
                                                    }
                                                }
    
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                                                        Column(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalArrangement = Arrangement.spacedBy(0.dp)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                ImmediateActionText(
                                                                    label = "Open",
                                                                    onAction = { vm.openSuggestedEntryInBrowser(suggestion.code) },
                                                                    onPressStart = onPressStart,
                                                                    runOnPressWhen = runOnPressWhen,
                                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                                    modifier = Modifier.heightIn(min = 0.dp)
                                                                )
                                                                ImmediateActionText(
                                                                    label = "Skip",
                                                                    onAction = { vm.skipSuggestedEntry(suggestion.code) },
                                                                    onPressStart = onPressStart,
                                                                    runOnPressWhen = runOnPressWhen,
                                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                                    modifier = Modifier.heightIn(min = 0.dp)
                                                                )
                                                                ImmediateActionText(
                                                                    label = "Import",
                                                                    onAction = { vm.importSuggestedEntry(suggestion.code) },
                                                                    onPressStart = onPressStart,
                                                                    runOnPressWhen = runOnPressWhen,
                                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                                    modifier = Modifier.heightIn(min = 0.dp)
                                                                )
                                                                suggestion.duplicateHint?.let { hint ->
                                                                    Spacer(modifier = Modifier.weight(1f))
                                                                    ImmediateActionText(
                                                                        label = "Possible duplicate",
                                                                        onAction = {
                                                                            suggestedDuplicateComparisonState.value =
                                                                                SuggestedDuplicateComparisonState(
                                                                                    suggestion = suggestion,
                                                                                    hint = hint
                                                                                )
                                                                        },
                                                                        onPressStart = onPressStart,
                                                                        runOnPressWhen = runOnPressWhen,
                                                                        textStyle = MaterialTheme.typography.labelSmall.copy(
                                                                            color = UNREAD_STATE_COLOR
                                                                        ),
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                                                                        modifier = Modifier.heightIn(min = 0.dp)
                                                                    )
                                                                }
                                                            }
                                                            suggestion.duplicateHint?.let { hint ->
                                                                Text(
                                                                    text = hint.reason,
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .privacyObfuscate(
                                                                            enabled = vm.incognitoModeEnabled,
                                                                            overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                                                                alpha = INCOGNITO_OVERLAY_ALPHA
                                                                            )
                                                                        ),
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = UNREAD_STATE_COLOR.copy(alpha = 0.88f),
                                                                    maxLines = 2,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    textAlign = TextAlign.End
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            suggestedHoldAction?.let { action ->
                                                LocalSuggestedHoldOverlay(
                                                    code = suggestion.code,
                                                    action = action,
                                                    modifier = Modifier.matchParentSize()
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
