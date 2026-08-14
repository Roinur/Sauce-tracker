package com.roinur.saucetracker

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import com.roinur.saucetracker.feature.dashboard.DashboardViewModel
import com.roinur.saucetracker.feature.library.tags.ModernTagsPage

@Composable
internal fun DashboardTagsSection(
    vm: DashboardViewModel,
    listState: LazyListState,
    onNotificationPermissionRequired: () -> Unit,
    onConfigureSubscription: (String, String) -> Unit
) {
    ModernTagsPage(
        tags = vm.tags,
        listState = listState,
        selectedIds = vm.activeTagFilterIds.toSet(),
        incognitoModeEnabled = vm.incognitoModeEnabled,
        isSubscribed = { tag -> vm.isRouteSubscribed(tag.type, tag.name) },
        onTagClick = vm::toggleTagFilter,
        onToggleSubscription = { tag ->
            val wasSubscribed = vm.isRouteSubscribed(tag.type, tag.name)
            vm.toggleTagSubscription(tag.id)
            if (!wasSubscribed) {
                onNotificationPermissionRequired()
            }
        },
        onConfigureSubscription = { tag ->
            onConfigureSubscription(tag.type, tag.name)
        },
        onClearFilter = vm::clearTagFilter,
        onSortByName = { vm.onTagSortClicked(TagSortField.NAME) },
        onSortByType = { vm.onTagSortClicked(TagSortField.TYPE) },
        onSortByCount = { vm.onTagSortClicked(TagSortField.COUNT) }
    )
}
