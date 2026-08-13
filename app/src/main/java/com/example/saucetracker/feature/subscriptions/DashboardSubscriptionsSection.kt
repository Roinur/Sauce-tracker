package com.example.saucetracker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.saucetracker.core.ui.privacy.privacyObfuscate
import com.example.saucetracker.feature.dashboard.DashboardViewModel
import com.example.saucetracker.feature.subscriptions.ModernSubscriptionsPage
import com.example.saucetracker.feature.subscriptions.SubscriptionEventDetailCard
import com.example.saucetracker.feature.subscriptions.SubscriptionSwipeDismissContainer

@Composable
internal fun DashboardSubscriptionsSection(
    vm: DashboardViewModel,
    selectedEventId: Long?,
    onSelectedEventIdChange: (Long?) -> Unit,
    preferLowRes: Boolean,
    listState: LazyListState,
    maxHeight: Dp,
    onOpenList: () -> Unit,
    onPressStart: () -> Unit,
    runOnPressWhen: () -> Boolean
) {
                    val experimentalSubscriptionSurface = !vm.legacyHomeUi && vm.experimentalSubscriptionInbox
                    if (experimentalSubscriptionSurface) {
                        ModernSubscriptionsPage(
                            subscriptionCount = vm.subscriptions.size,
                            events = vm.visibleSubscriptionEvents,
                            selectedEventId = selectedEventId,
                            refreshRunning = vm.subscriptionRefreshRunning,
                            incognitoModeEnabled = vm.incognitoModeEnabled,
                            preferLowRes = preferLowRes,
                            listState = listState,
                            maxHeight = maxHeight,
                            onOpenList = { onOpenList() },
                            onRefresh = {
                                if (vm.incognitoModeEnabled) {
                                    vm.setStatus("Subscriptions are hidden while incognito mode is enabled.")
                                } else {
                                    vm.refreshSubscriptions()
                                }
                            },
                            onSelectEvent = { eventId ->
                                onSelectedEventIdChange(if (selectedEventId == eventId) null else eventId)
                            },
                            onTogglePinned = vm::toggleSubscriptionEventPinned,
                            onDismiss = { eventId ->
                                if (selectedEventId == eventId) {
                                    onSelectedEventIdChange(null)
                                }
                                vm.dismissSubscriptionEvent(eventId)
                            },
                            onOpen = { code -> vm.openSuggestedEntryInBrowser(code) },
                            onImport = vm::importSubscriptionEvent
                        )
                    } else {
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
                        "Subscriptions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ImmediateActionText(
                            label = "List",
                            onAction = { onOpenList() },
                            enabled = true,
                            onPressStart = onPressStart,
                            runOnPressWhen = runOnPressWhen,
                            textStyle = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        ImmediateActionText(
                            label = if (vm.subscriptionRefreshRunning) "Checking..." else "Refresh",
                            onAction = {
                                if (vm.incognitoModeEnabled) {
                                    vm.setStatus("Subscriptions are hidden while incognito mode is enabled.")
                                } else {
                                    vm.refreshSubscriptions()
                                }
                            },
                            enabled = true,
                            onPressStart = onPressStart,
                            runOnPressWhen = runOnPressWhen,
                            textStyle = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        ImmediateActionText(
                            label = if (vm.subscriptionsCardCollapsed) "Expand" else "Collapse",
                            onAction = vm::toggleSubscriptionsCardCollapsed,
                            enabled = true,
                            onPressStart = onPressStart,
                            runOnPressWhen = runOnPressWhen,
                            textStyle = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
    
                if (!vm.subscriptionsCardCollapsed && vm.incognitoModeEnabled) {
                    Text(
                        text = "Subscriptions are hidden while incognito mode is enabled.",
                        modifier = Modifier.privacyObfuscate(
                            enabled = true,
                            overlayColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = INCOGNITO_OVERLAY_ALPHA)
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (!vm.subscriptionsCardCollapsed) {
                    val subscriptionCount = vm.subscriptions.size
                    val eventCount = vm.visibleSubscriptionEvents.size
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (subscriptionCount <= 0) {
                                "Subscribe from a tag or artist/group bell to start tracking updates."
                            } else {
                                "$subscriptionCount subscription(s) • $eventCount undismissed update(s)"
                            },
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (vm.subscriptionRefreshRunning) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Checking subscribed tags and artists/groups...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
    
                    if (vm.visibleSubscriptionEvents.isEmpty()) {
                        Text(
                            text = if (subscriptionCount <= 0) {
                                "No subscriptions yet."
                            } else {
                                "No new subscription updates right now."
                            },
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
                                vm.visibleSubscriptionEvents,
                                key = { it.id },
                                contentType = { "subscription_event_row" }
                            ) { event ->
                                val selected = selectedEventId == event.id
                                val eventInteraction = remember { MutableInteractionSource() }
                                SubscriptionSwipeDismissContainer(
                                    eventId = event.id,
                                    isPinned = event.pinned,
                                    onTogglePinned = vm::toggleSubscriptionEventPinned,
                                    onDismiss = { eventId ->
                                        if (selectedEventId == eventId) {
                                            onSelectedEventIdChange(null)
                                        }
                                        vm.dismissSubscriptionEvent(eventId)
                                    }
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerLow
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
                                            .clickable(
                                                interactionSource = eventInteraction,
                                                indication = null
                                            ) {
                                                onSelectedEventIdChange(if (selected) null else event.id)
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                if (event.thumbnailUrl.isNotBlank()) {
                                                    ThumbnailImage(
                                                        thumbnailUrl = event.thumbnailUrl,
                                                        backupCode = event.code,
                                                        contentDescription = "Subscription cover for code ${event.code}",
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
                                                        text = "#${event.code} • ${event.title}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = subscriptionRouteDisplayLabel(event.routeType, event.routeName),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "Pages: ${event.numPages} • Uploaded: ${event.uploadDate.ifBlank { "-" }}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
    
                                            if (selected) {
                                                SubscriptionEventDetailCard(
                                                    event = event,
                                                    onOpen = { vm.openSuggestedEntryInBrowser(event.code) },
                                                    onImport = { vm.importSubscriptionEvent(event.code) },
                                                    onDismiss = {
                                                        onSelectedEventIdChange(null)
                                                        vm.dismissSubscriptionEvent(event.id)
                                                    }
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
