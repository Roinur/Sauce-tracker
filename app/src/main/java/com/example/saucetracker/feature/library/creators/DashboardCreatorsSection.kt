package com.example.saucetracker

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.saucetracker.feature.dashboard.DashboardViewModel
import com.example.saucetracker.feature.library.creators.ModernCreatorsPage

@Composable
internal fun DashboardCreatorsSection(
    vm: DashboardViewModel,
    listState: LazyListState,
    selectedEntryDownloaded: Boolean,
    entryDetailActions: DashboardEntryDetailActions,
    onNotificationPermissionRequired: () -> Unit,
    onConfigureSubscription: (String, String) -> Unit,
    onOpenEntry: (Int) -> Unit
) {
    ModernCreatorsPage(
        creators = vm.creators,
        listState = listState,
        incognitoModeEnabled = vm.incognitoModeEnabled,
        expandedIds = vm.expandedCreatorIds.toSet(),
        linkedEntriesProvider = vm::creatorEntriesFor,
        loadingProvider = vm::isCreatorLoading,
        onCreatorClick = vm::toggleCreatorExpanded,
        onOpenCreator = vm::openCreatorPreviewInBrowser,
        isSubscribed = { creator -> vm.isRouteSubscribed(creator.type, creator.name) },
        onToggleSubscription = { creator ->
            val wasSubscribed = vm.isRouteSubscribed(creator.type, creator.name)
            vm.toggleCreatorSubscription(creator.type, creator.name)
            if (!wasSubscribed) {
                onNotificationPermissionRequired()
            }
        },
        onConfigureSubscription = { creator ->
            onConfigureSubscription(creator.type, creator.name)
        },
        onOpenEntry = onOpenEntry,
        onSelectLinkedEntry = vm::selectEntryFromCreator,
        expandedEntryContent = { code ->
            if (vm.selectedCode == code) {
                DashboardSelectedEntryDetail(
                    vm = vm,
                    code = code,
                    selectedEntryDownloaded = selectedEntryDownloaded,
                    actions = entryDetailActions,
                    compactContent = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        },
        onSortByName = { vm.onCreatorSortClicked(CreatorSortField.NAME) },
        onSortByType = { vm.onCreatorSortClicked(CreatorSortField.TYPE) },
        onSortByCount = { vm.onCreatorSortClicked(CreatorSortField.COUNT) }
    )
}
