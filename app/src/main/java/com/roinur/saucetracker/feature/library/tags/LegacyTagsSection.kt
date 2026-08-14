package com.roinur.saucetracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.roinur.saucetracker.core.ui.privacy.privacyObfuscate
import com.roinur.saucetracker.feature.dashboard.DashboardViewModel

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun LegacyTagsSection(
    vm: DashboardViewModel,
    listState: LazyListState,
    onPressStart: () -> Unit,
    runOnPressWhen: () -> Boolean,
    onNotificationPermissionRequired: () -> Unit,
    onConfigureSubscription: (String, String) -> Unit
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
                    "Tags",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ImmediateActionText(
                        label = "Reset Search",
                        onAction = vm::clearEntrySearch,
                        onPressStart = onPressStart,
                        runOnPressWhen = runOnPressWhen,
                        textStyle = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    ImmediateActionText(
                        label = "Reset Filter",
                        onAction = vm::clearTagFilter,
                        onPressStart = onPressStart,
                        runOnPressWhen = runOnPressWhen,
                        textStyle = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    ImmediateActionText(
                        label = if (vm.tagsCardCollapsed) "Expand" else "Collapse",
                        onAction = vm::toggleTagsCardCollapsed,
                        onPressStart = onPressStart,
                        runOnPressWhen = runOnPressWhen,
                        textStyle = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
    
            if (!vm.tagsCardCollapsed) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        EntrySortChip(
                            label = "Tag${tagSortArrow(vm, TagSortField.NAME)}",
                            selected = vm.tagSortField == TagSortField.NAME,
                            activeDirection = if (vm.tagSortField == TagSortField.NAME) vm.tagSortDirection else null,
                            onClick = { vm.onTagSortClicked(TagSortField.NAME) },
                            onPressStart = onPressStart,
                            runOnPressWhen = runOnPressWhen
                        )
                    }
                    item {
                        EntrySortChip(
                            label = "Type${tagSortArrow(vm, TagSortField.TYPE)}",
                            selected = vm.tagSortField == TagSortField.TYPE,
                            activeDirection = if (vm.tagSortField == TagSortField.TYPE) vm.tagSortDirection else null,
                            onClick = { vm.onTagSortClicked(TagSortField.TYPE) },
                            onPressStart = onPressStart,
                            runOnPressWhen = runOnPressWhen
                        )
                    }
                    item {
                        EntrySortChip(
                            label = "Count${tagSortArrow(vm, TagSortField.COUNT)}",
                            selected = vm.tagSortField == TagSortField.COUNT,
                            activeDirection = if (vm.tagSortField == TagSortField.COUNT) vm.tagSortDirection else null,
                            onClick = { vm.onTagSortClicked(TagSortField.COUNT) },
                            onPressStart = onPressStart,
                            runOnPressWhen = runOnPressWhen
                        )
                    }
                }
    
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 260.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        vm.tags,
                        key = { it.id },
                        contentType = { "tag_row" }
                        ) { tag ->
                            val selected = vm.activeTagFilterIds.contains(tag.id)
                            val subscribed = vm.isRouteSubscribed(tag.type, tag.name)
                            val tagInteraction = remember { MutableInteractionSource() }
                            val bellInteraction = remember { MutableInteractionSource() }
                            val containerColor = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(containerColor, shape = MaterialTheme.shapes.small)
                                .clickable(
                                    enabled = !vm.incognitoModeEnabled,
                                    interactionSource = tagInteraction,
                                    indication = null
                                ) { vm.toggleTagFilter(tag.id) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tag.name,
                                modifier = Modifier
                                    .weight(0.42f)
                                    .privacyObfuscate(
                                        enabled = vm.incognitoModeEnabled,
                                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                    ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tag.type,
                                modifier = Modifier
                                    .weight(0.24f)
                                    .privacyObfuscate(
                                        enabled = vm.incognitoModeEnabled,
                                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                    ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = tag.count.toString(),
                                modifier = Modifier
                                    .weight(0.14f)
                                    .privacyObfuscate(
                                        enabled = vm.incognitoModeEnabled,
                                        overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                                    ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Medium
                            )
                            Box(
                                modifier = Modifier.weight(0.20f),
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
                                        "Subscribed to ${tag.name}"
                                    } else {
                                        "Subscribe to ${tag.name}"
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
                                                    val wasSubscribed = vm.isRouteSubscribed(tag.type, tag.name)
                                                    vm.toggleTagSubscription(tag.id)
                                                    if (!wasSubscribed) {
                                                        onNotificationPermissionRequired()
                                                    }
                                                },
                                                onLongClick = {
                                                    onConfigureSubscription(tag.type, tag.name)
                                            }
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
