package com.example.saucetracker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.saucetracker.core.ui.privacy.privacyObfuscate
import com.example.saucetracker.feature.dashboard.DashboardViewModel

@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
internal fun LegacyCreatorsSection(
    vm: DashboardViewModel,
    listState: LazyListState,
    maxHeight: Dp,
    selectedEntryDownloaded: Boolean,
    entryDetailActions: DashboardEntryDetailActions,
    onCollapseHeatmap: () -> Unit,
    onPressStart: () -> Unit,
    runOnPressWhen: () -> Boolean,
    onNotificationPermissionRequired: () -> Unit,
    onConfigureSubscription: (String, String) -> Unit,
    onSelectLinkedEntry: (Long, Int) -> Unit,
    onLinkedEntryPositioned: (Long, Int, Float) -> Unit
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
                Text(
                    "Artists / Groups",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                ImmediateActionText(
                    label = if (vm.creatorsCardCollapsed) "Expand" else "Collapse",
                    onAction = {
                        onCollapseHeatmap()
                        vm.toggleCreatorsCardCollapsed()
                    },
                    onPressStart = onPressStart,
                    runOnPressWhen = runOnPressWhen,
                    textStyle = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
    
            if (!vm.creatorsCardCollapsed) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        EntrySortChip(
                            label = "Name${creatorSortArrow(vm, CreatorSortField.NAME)}",
                            selected = vm.creatorSortField == CreatorSortField.NAME,
                            activeDirection = if (vm.creatorSortField == CreatorSortField.NAME) vm.creatorSortDirection else null,
                            onClick = { vm.onCreatorSortClicked(CreatorSortField.NAME) },
                            onPressStart = onPressStart,
                            runOnPressWhen = runOnPressWhen
                        )
                    }
                    item {
                        EntrySortChip(
                            label = "Type${creatorSortArrow(vm, CreatorSortField.TYPE)}",
                            selected = vm.creatorSortField == CreatorSortField.TYPE,
                            activeDirection = if (vm.creatorSortField == CreatorSortField.TYPE) vm.creatorSortDirection else null,
                            onClick = { vm.onCreatorSortClicked(CreatorSortField.TYPE) },
                            onPressStart = onPressStart,
                            runOnPressWhen = runOnPressWhen
                        )
                    }
                    item {
                        EntrySortChip(
                            label = "Count${creatorSortArrow(vm, CreatorSortField.COUNT)}",
                            selected = vm.creatorSortField == CreatorSortField.COUNT,
                            activeDirection = if (vm.creatorSortField == CreatorSortField.COUNT) vm.creatorSortDirection else null,
                            onClick = { vm.onCreatorSortClicked(CreatorSortField.COUNT) },
                            onPressStart = onPressStart,
                            runOnPressWhen = runOnPressWhen
                        )
                    }
                }
    
                if (vm.creators.isEmpty()) {
                    Text(
                        "No artists/groups tracked.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = maxHeight),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            vm.creators,
                            key = { it.id },
                            contentType = { "creator_row" }
                        ) { creator ->
                            val expanded = vm.expandedCreatorIds.contains(creator.id)
                            val subscribed = vm.isRouteSubscribed(creator.type, creator.name)
                            val creatorInteraction = remember { MutableInteractionSource() }
                            val creatorBellInteraction = remember { MutableInteractionSource() }
                            val containerColor = if (expanded) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(containerColor, shape = MaterialTheme.shapes.small)
                                    .clickable(
                                        interactionSource = creatorInteraction,
                                        indication = null
                                    ) { vm.toggleCreatorExpanded(creator.id) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CompositionLocalProvider(
                                    LocalMinimumInteractiveComponentEnforcement provides false
                                ) {
                                    Box(
                                        modifier = Modifier.weight(0.40f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        TextButton(
                                            onClick = { vm.openCreatorPreviewInBrowser(creator.type, creator.name) },
                                            enabled = !vm.incognitoModeEnabled,
                                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                                        ) {
                                            Text(
                                                text = creator.name,
                                                modifier = Modifier
                                                    .privacyObfuscate(
                                                        enabled = vm.incognitoModeEnabled,
                                                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                    ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Start,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = creator.type,
                                    modifier = Modifier
                                        .weight(0.18f)
                                        .privacyObfuscate(
                                            enabled = vm.incognitoModeEnabled,
                                            overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                        ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = creator.entryCount.toString(),
                                    modifier = Modifier
                                        .weight(0.12f)
                                        .privacyObfuscate(
                                            enabled = vm.incognitoModeEnabled,
                                            overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                        ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (expanded) "▲" else "▼",
                                    modifier = Modifier.weight(0.12f),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box(
                                    modifier = Modifier.weight(0.18f),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (subscribed) {
                                                R.drawable.ic_notifications_24
                                            } else {
                                                R.drawable.ic_notifications_none_24
                                            }
                                        ),
                                        contentDescription = if (subscribed) {
                                            "Subscribed to ${creator.name}"
                                        } else {
                                            "Subscribe to ${creator.name}"
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
                                                interactionSource = creatorBellInteraction,
                                                indication = rememberRipple(bounded = false, radius = 18.dp),
                                                onClick = {
                                                    val wasSubscribed = vm.isRouteSubscribed(creator.type, creator.name)
                                                    vm.toggleCreatorSubscription(creator.type, creator.name)
                                                    if (!wasSubscribed) {
                                                        onNotificationPermissionRequired()
                                                    }
                                                },
                                                onLongClick = {
                                                    onConfigureSubscription(creator.type, creator.name)
                                                }
                                            )
                                    )
                                }
                            }
    
                            if (expanded) {
                                val linkedEntries = vm.creatorEntriesFor(creator.id)
                                if (vm.isCreatorLoading(creator.id)) {
                                    Text(
                                        "(loading...)",
                                        modifier = Modifier.padding(start = 12.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else if (linkedEntries.isEmpty()) {
                                    Text(
                                        "(no linked entries)",
                                        modifier = Modifier.padding(start = 12.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    linkedEntries.forEach { linked ->
                                        TextButton(
                                            onClick = {
                                                onSelectLinkedEntry(creator.id, linked.code)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 8.dp)
                                                .onGloballyPositioned { coordinates ->
                                                    onLinkedEntryPositioned(creator.id, linked.code, coordinates.positionInRoot().y)
                                                }
                                        ) {
                                            Text(
                                                "${linked.code} - ${linked.title}",
                                                modifier = Modifier.privacyObfuscate(
                                                    enabled = vm.incognitoModeEnabled,
                                                    overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (vm.selectedCode == linked.code) {
                                            DashboardSelectedEntryDetail(
                                                vm = vm,
                                                code = linked.code,
                                                selectedEntryDownloaded = selectedEntryDownloaded,
                                                actions = entryDetailActions,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 8.dp, bottom = 6.dp)
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
