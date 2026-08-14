package com.example.saucetracker.feature.suggestions

import com.example.saucetracker.*
import com.example.saucetracker.core.media.*
import com.example.saucetracker.core.ui.components.*
import com.example.saucetracker.core.ui.privacy.privacyObfuscate
import com.example.saucetracker.feature.dashboard.DashboardViewModel
import com.example.saucetracker.feature.library.entries.EntrySwipeDismissContainer
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
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
internal fun LegacyDashboardSuggestionsSection(
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Suggested entries",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onShowWeights() }) {
                        Text(
                            "Weights",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    TextButton(onClick = { vm.refreshSuggestedEntriesForCurrentSession() }) {
                        Text(
                            "Refresh",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    TextButton(
                        onClick = {
                            onCollapseHeatmap()
                            vm.toggleSuggestedEntriesCollapsed()
                        }
                    ) {
                        Text(
                            if (vm.suggestedEntriesCollapsed) "Expand" else "Collapse",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
    
            if (!vm.suggestedEntriesCollapsed) {
                Text(
                    text = "Based on your library (local)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                "Refreshing suggestions...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
    
                    if (vm.suggestedEntries.isEmpty() && !vm.suggestedEntriesLoading) {
                        Text(
                            text = "No suggestions yet. Read/rate entries (0 is neutral), then refresh.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                EntrySwipeDismissContainer(
                                    code = suggestion.code,
                                    isPinned = suggestionPinned,
                                    isRead = suggestionRead,
                                    incognitoModeEnabled = vm.incognitoModeEnabled,
                                    onTogglePinned = vm::quickToggleSuggestedPinned,
                                    onToggleRead = vm::quickToggleSuggestedRead
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
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
                                            LoadingShimmerOverlay(
                                                active = suggestion.duplicateHint != null,
                                                tint = UNREAD_STATE_COLOR,
                                                modifier = Modifier.matchParentSize()
                                            )
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                                                .width(108.dp)
                                                                .height(72.dp)
                                                        )
                                                    }
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Text(
                                                            text = "#${suggestion.code} â€¢ ${suggestion.title}",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "Pages: ${suggestion.numPages} â€¢ Uploaded: ${suggestion.uploadDate.ifBlank { "-" }}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = "Score: ${"%.2f".format(Locale.US, suggestion.score)}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
    
                                                if (suggestion.topTags.isNotEmpty()) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                                                        label = "Duplicate? #${hint.matchedCode}",
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
                                                                if (suggestion.duplicateHint == null && suggestion.whySuggestedReason.isNotBlank()) {
                                                                    Spacer(modifier = Modifier.weight(1f))
                                                                    Text(
                                                                        text = "Why suggested?",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = READ_STATE_COLOR,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            }
                                                            suggestion.duplicateHint?.let { hint ->
                                                                Text(
                                                                    text = hint.reason,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = UNREAD_STATE_COLOR.copy(alpha = 0.88f),
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .padding(top = 0.dp),
                                                                    maxLines = 2,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    textAlign = TextAlign.End
                                                                )
                                                            }
                                                            if (suggestion.duplicateHint == null && suggestion.whySuggestedReason.isNotBlank()) {
                                                                Text(
                                                                    text = suggestion.whySuggestedReason,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = READ_STATE_COLOR.copy(alpha = 0.92f),
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .padding(top = 0.dp),
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
    }
}


