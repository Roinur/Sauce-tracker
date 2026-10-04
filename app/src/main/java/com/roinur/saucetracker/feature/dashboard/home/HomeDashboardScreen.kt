package com.roinur.saucetracker

import com.roinur.saucetracker.data.backup.*
import com.roinur.saucetracker.data.downloads.*
import com.roinur.saucetracker.core.ui.components.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.collectAsState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.roinur.saucetracker.data.source.SourceId
import com.roinur.saucetracker.data.source.SourceEntryKey
import com.roinur.saucetracker.data.source.SourceEntry
import com.roinur.saucetracker.data.source.mangaDexAvailableLanguages
import com.roinur.saucetracker.data.source.mangaDexLanguageDisplayName
import com.roinur.saucetracker.data.source.withMangaDexLanguage
import com.roinur.saucetracker.core.media.*
import com.roinur.saucetracker.feature.library.creators.*
import com.roinur.saucetracker.feature.library.detail.*
import com.roinur.saucetracker.feature.library.history.*
import com.roinur.saucetracker.feature.library.tags.*
import com.roinur.saucetracker.feature.settings.*
import com.roinur.saucetracker.feature.subscriptions.*
import com.roinur.saucetracker.feature.suggestions.*
import kotlin.math.roundToInt

@Composable
internal fun ModernHomeDashboard(
    vm: com.roinur.saucetracker.feature.dashboard.DashboardViewModel,
    dashboardVisitNonce: Long,
    onOpenEntries: () -> Unit,
    onOpenTags: () -> Unit,
    onOpenCreators: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onOpenSubscriptionsList: () -> Unit,
    onOpenHeatmap: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSuggestions: () -> Unit,
    onOpenEntry: (Int) -> Unit,
    onOpenRandomEntry: (Int) -> Unit,
    onEntriesLongPress: (() -> Unit)? = null,
    onTagsLongPress: (() -> Unit)? = null,
    onCreatorsLongPress: (() -> Unit)? = null,
    onHeatmapLongPress: (() -> Unit)? = null
) {
    val recentEntries = remember(vm.entries) {
        vm.entries
            .sortedByDescending { it.addedAt.ifBlank { it.fetchedAt } }
            .take(8)
    }
    val suggestionPreview = remember(vm.suggestedEntries, vm.entries) {
        vm.suggestedEntries
            .map { suggestion ->
                EntryRow(
                    code = suggestion.code,
                    title = suggestion.title,
                    numPages = suggestion.numPages,
                    uploadDate = "",
                    addedAt = "",
                    rating = 0,
                    averageRating = 0f,
                    isRead = false,
                    pinned = false,
                    fetchedAt = "",
                    sourceUrl = "",
                    thumbnailUrl = suggestion.thumbnailUrl,
                    tags = suggestion.topTags.joinToString(", ")
                )
            }
            .take(8)
    }
    val randomPreview = remember(vm.entries, dashboardVisitNonce) {
        vm.entries.shuffled().take(8)
    }
    var randomPreviewInitialIndex by remember { mutableStateOf<Int?>(null) }
    val sauceFinderState by vm.sauceFinderState.collectAsState()
    val sauceImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(vm::findSauce)
    }
    val playDashboardEntrance = remember { vm.consumeDashboardEntrance() }
    var dashboardVisible by remember { mutableStateOf(!playDashboardEntrance) }
    LaunchedEffect(playDashboardEntrance) {
        if (playDashboardEntrance) {
            dashboardVisible = true
        }
        vm.ensureReadAnalyticsLoaded(forceRefresh = false)
    }

    randomPreviewInitialIndex?.let { initialIndex ->
        RandomEntryPreviewDialog(
            entries = randomPreview,
            initialIndex = initialIndex,
            incognitoModeEnabled = vm.incognitoModeEnabled,
            onDismiss = { randomPreviewInitialIndex = null },
            onOpenEntry = { code ->
                randomPreviewInitialIndex = null
                onOpenRandomEntry(code)
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProfileSourceBar(vm)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModernMetricTile(
                label = "Entries",
                value = vm.entries.size,
                caption = "Active profile",
                glyph = DashboardMetricGlyph.ENTRIES,
                onClick = onOpenEntries,
                onLongClick = onEntriesLongPress,
                modifier = Modifier
                    .weight(1f)
                    .dashboardEntrance(dashboardVisible, delayMillis = 0, label = "dashboardEntriesEntrance")
            )
            ModernMetricTile(
                label = "Tags",
                value = vm.tags.size,
                caption = "Visible tags",
                glyph = DashboardMetricGlyph.TAGS,
                onClick = onOpenTags,
                onLongClick = onTagsLongPress,
                modifier = Modifier
                    .weight(1f)
                    .dashboardEntrance(dashboardVisible, delayMillis = 48, label = "dashboardTagsEntrance")
            )
            ModernMetricTile(
                label = "Artists / Groups",
                value = vm.creators.size,
                caption = "Matched",
                glyph = DashboardMetricGlyph.ARTISTS,
                onClick = onOpenCreators,
                onLongClick = onCreatorsLongPress,
                modifier = Modifier
                    .weight(1f)
                    .dashboardEntrance(dashboardVisible, delayMillis = 96, label = "dashboardCreatorsEntrance")
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .dashboardEntrance(dashboardVisible, delayMillis = 144, label = "dashboardWidgetsEntrance"),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SuggestedRandomPreviewPanel(
                pageOrder = vm.dashboardDiscoveryPageOrder,
                suggestedEntries = suggestionPreview,
                randomEntries = randomPreview,
                suggestedSubtitle = if (vm.suggestedEntriesLoading) "Refreshing..." else "For you to discover",
                suggestedEmptyMessage = if (vm.suggestedEntriesLoading) {
                    "Loading suggested entries..."
                } else {
                    "Open suggested entries to load recommendations."
                },
                incognitoModeEnabled = vm.incognitoModeEnabled,
                onOpenSuggestions = onOpenSuggestions,
                onSuggestedEntryClick = onOpenEntry,
                onRandomEntryClick = { entry ->
                    val index = randomPreview.indexOfFirst { it.code == entry.code }.coerceAtLeast(0)
                    randomPreviewInitialIndex = index
                },
                sauceFinderState = sauceFinderState,
                onPrepareSauceFinder = vm::prepareSauceFinderLocalIndex,
                onChooseSauceImage = { sauceImagePicker.launch("image/*") },
                onBuildSauceIndex = vm::buildFullSauceFinderIndex,
                onPauseSauceIndex = vm::pauseFullSauceFinderIndex,
                onOpenSauceMatch = onOpenRandomEntry,
                modifier = Modifier
                    .weight(2f)
                    .height(184.dp)
            )
            SubscriptionHeatmapPreviewPanel(
                pageOrder = vm.dashboardInsightPageOrder,
                subscriptionCount = vm.logicalSubscriptionCount,
                updateCount = vm.visibleSubscriptionEvents.size,
                entryCount = vm.entries.size,
                tagCount = vm.tags.size,
                analyticsSnapshot = vm.readAnalytics,
                analyticsLoading = vm.readAnalyticsLoading,
                onOpenUpdates = onOpenSubscriptions,
                onOpenList = onOpenSubscriptionsList,
                onOpenHeatmap = onOpenHeatmap,
                onHeatmapLongPress = onHeatmapLongPress,
                onOpenHistory = onOpenHistory,
                modifier = Modifier
                    .weight(1f)
                    .height(184.dp)
            )
        }

        ModernPreviewPanel(
            title = "Continue where you left off",
            subtitle = "Most recently imported",
            entries = recentEntries,
            incognitoModeEnabled = vm.incognitoModeEnabled,
            onHeaderClick = onOpenEntries,
            onEntryClick = onOpenEntry,
            modifier = Modifier.dashboardEntrance(
                dashboardVisible,
                delayMillis = 192,
                label = "dashboardContinueEntrance"
            )
        )
    }
}

private enum class ProfileManagementPage { OVERVIEW, EDIT, CREATE, TRANSFER }

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileSourceBar(
    vm: com.roinur.saucetracker.feature.dashboard.DashboardViewModel,
    showProfileControls: Boolean = false
) {
    var manageProfiles by remember { mutableStateOf(false) }
    var managementPage by remember { mutableStateOf(ProfileManagementPage.OVERVIEW) }
    val managementScroll = remember(managementPage) { ScrollState(0) }
    var profileName by remember(vm.activeProfileId) { mutableStateOf(vm.activeProfile?.name.orEmpty()) }
    var newProfileName by remember { mutableStateOf("") }
    var newProfileSources by remember { mutableStateOf(setOf(SourceId("nhentai"))) }
    var sourceLocked by remember { mutableStateOf(false) }
    var transferTarget by remember(vm.activeProfileId) { mutableStateOf("") }
    var transferSelection by remember(vm.activeProfileId) { mutableStateOf<Set<SourceEntryKey>>(emptySet()) }
    var pendingDeleteProfile by remember { mutableStateOf<com.roinur.saucetracker.data.profile.LibraryProfile?>(null) }
    var pendingMangaDexImport by remember { mutableStateOf<SourceEntry?>(null) }
    var pendingMangaDexLanguage by remember { mutableStateOf("") }
    var transferIsMove by remember { mutableStateOf(false) }
    val sources = vm.activeProfile?.sourceIds.orEmpty()
    val filteredTransferEntries = vm.profileTransferEntries
    val filteredTransferKeys = filteredTransferEntries.mapTo(linkedSetOf()) { it.entry.key }
    LaunchedEffect(vm.activeProfileId, filteredTransferKeys) {
        transferSelection = transferSelection.intersect(filteredTransferKeys)
    }
    val hasSourceSurface = vm.codeInput.isNotBlank() ||
        vm.sourceSearchErrors.isNotEmpty() || vm.sourceSearchResults.isNotEmpty() ||
        vm.sourceSearchHasMore.any { it.value }
    if (!showProfileControls && !hasSourceSurface) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (showProfileControls) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable { managementPage = ProfileManagementPage.OVERVIEW; manageProfiles = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileAvatar(vm.activeProfile?.name ?: "Main", vm.incognitoModeEnabled, 42.dp)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Profiles", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (vm.incognitoModeEnabled) "Private profile" else vm.activeProfile?.name ?: "Main",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${if (vm.activeProfile?.kind?.name == "SOURCE_LOCKED") "Source locked" else "Combined"} · ${profileSourceLabel(sources)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text("Manage", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (!showProfileControls && vm.codeInput.isNotBlank()) {
                Button(
                    onClick = vm::searchActiveSources,
                    enabled = !vm.sourceSearchRunning,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (vm.sourceSearchRunning) "Searching…" else "Search active sources") }
            }
            if (!showProfileControls) vm.sourceSearchErrors.forEach { (source, message) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${source.value}: $message", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = { vm.retrySourceSearch(source) }, enabled = !vm.sourceSearchRunning) { Text("Retry") }
                }
            }
            if (!showProfileControls) vm.sourceSearchResults.take(8).forEach { entry ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (vm.incognitoModeEnabled) "••••••" else entry.title, style = MaterialTheme.typography.bodyMedium)
                        Text(if (entry.key.sourceId.value == "nhentai") entry.key.displayId else entry.key.sourceId.value.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = {
                        if (entry.key.sourceId.value == "mangadex") {
                            pendingMangaDexImport = entry
                            pendingMangaDexLanguage = ""
                        } else vm.importSourceEntry(entry)
                    }) { Text("Add") }
                }
            }
            if (!showProfileControls) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.sourceSearchHasMore.filterValues { it }.keys.sortedBy { it.value }.forEach { source ->
                    OutlinedButton(onClick = { vm.loadMoreSourceSearch(source) }, enabled = !vm.sourceSearchRunning) { Text("Load more · ${source.value}") }
                }
            }
        }
    }
    pendingMangaDexImport?.let { entry ->
        val languages = entry.mangaDexAvailableLanguages()
        AlertDialog(
            onDismissRequest = { pendingMangaDexImport = null },
            title = { Text("Choose MangaDex language") },
            text = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    languages.forEach { language ->
                        FilterChip(selected = pendingMangaDexLanguage == language, onClick = { pendingMangaDexLanguage = language },
                            label = { Text(mangaDexLanguageDisplayName(language)) })
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = pendingMangaDexLanguage.isNotBlank(), onClick = {
                    vm.importSourceEntry(entry.withMangaDexLanguage(pendingMangaDexLanguage))
                    pendingMangaDexImport = null
                }) { Text("Import") }
            },
            dismissButton = { OutlinedButton(onClick = { pendingMangaDexImport = null }) { Text("Cancel") } }
        )
    }
    if (showProfileControls && manageProfiles) {
        Dialog(
            onDismissRequest = { manageProfiles = false; managementPage = ProfileManagementPage.OVERVIEW },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.94f).widthIn(max = 720.dp).heightIn(max = 700.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (managementPage == ProfileManagementPage.OVERVIEW) {
                            ProfileAvatar(vm.activeProfile?.name ?: "Main", vm.incognitoModeEnabled, 38.dp)
                        } else {
                            TextButton(onClick = { managementPage = ProfileManagementPage.OVERVIEW }) { Text("Back") }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                when (managementPage) {
                                    ProfileManagementPage.OVERVIEW -> "Profiles"
                                    ProfileManagementPage.EDIT -> "Edit profile"
                                    ProfileManagementPage.CREATE -> "New profile"
                                    ProfileManagementPage.TRANSFER -> "Copy or move"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                        TextButton(onClick = { manageProfiles = false; managementPage = ProfileManagementPage.OVERVIEW }) { Text("Done") }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(managementScroll)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (managementPage) {
                        ProfileManagementPage.OVERVIEW -> {
                            Text("Choose a library", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 2.dp))
                            vm.profiles.forEachIndexed { index, profile ->
                                val selected = profile.id == vm.activeProfileId
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                                        .clickable { vm.selectProfile(profile.id) },
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
                                ) {
                                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        ProfileAvatar(profile.name, vm.incognitoModeEnabled, 38.dp)
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(if (vm.incognitoModeEnabled) "Profile ${index + 1}" else profile.name,
                                                style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${if (profile.kind.name == "SOURCE_LOCKED") "Source locked" else "Combined"} · ${profileSourceLabel(profile.sourceIds)}",
                                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        if (selected) Text("Active", style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary)
                                        if (!profile.isMain && !selected && !vm.incognitoModeEnabled) {
                                            TextButton(onClick = { pendingDeleteProfile = profile }) {
                                                Text("Delete", color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                modifier = Modifier.padding(vertical = 4.dp))
                            ProfileManagementAction("Edit current profile", "Name and available sources") {
                                profileName = vm.activeProfile?.name.orEmpty(); managementPage = ProfileManagementPage.EDIT
                            }
                            ProfileManagementAction("Create profile", "A separate library and reading history") {
                                managementPage = ProfileManagementPage.CREATE
                            }
                            if (vm.profiles.size > 1) {
                                ProfileManagementAction("Copy or move entries", "Use your current Search Everything and tag filters") {
                                    managementPage = ProfileManagementPage.TRANSFER
                                }
                            }
                        }
                        ProfileManagementPage.EDIT -> {
                        ProfileManagementSection(title = if (vm.incognitoModeEnabled) "Current profile" else vm.activeProfile?.name ?: "Main") {
                            Text("Your library state and reading history stay separate from other profiles.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(
                                if (vm.incognitoModeEnabled) "" else profileName,
                                { profileName = it },
                                label = { Text(if (vm.incognitoModeEnabled) "Hidden in Incognito" else "Profile name") },
                                singleLine = true,
                                enabled = !vm.incognitoModeEnabled,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = { vm.renameActiveProfile(profileName) },
                                enabled = profileName.isNotBlank() && profileName.trim() != vm.activeProfile?.name && !vm.incognitoModeEnabled
                            ) { Text("Save name") }
                            if (vm.activeProfile?.kind?.name == "COMBINED") {
                                Text("Active sources", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    listOf(SourceId("nhentai"), SourceId("mangadex")).forEach { source ->
                                        FilterChip(
                                            selected = source in vm.activeProfile!!.sourceIds,
                                            onClick = {
                                                val current = vm.activeProfile!!.sourceIds
                                                val next = if (source in current) current - source else current + source
                                                if (next.isNotEmpty()) vm.updateActiveProfileSources(next)
                                            },
                                            label = { Text(if (source.value == "nhentai") "NHentai" else "MangaDex") }
                                        )
                                    }
                                }
                                Text("Entries from a disabled source remain in this profile.",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        }
                        ProfileManagementPage.CREATE -> {
                        ProfileManagementSection(title = "Profile setup") {
                            OutlinedTextField(
                                newProfileName,
                                { newProfileName = it },
                                label = { Text("Profile name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("Profile type", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = !sourceLocked, onClick = { sourceLocked = false }, label = { Text("Combined") })
                                FilterChip(selected = sourceLocked, onClick = {
                                    sourceLocked = true
                                    if (newProfileSources.size > 1) newProfileSources = setOf(newProfileSources.first())
                                }, label = { Text("One source") })
                            }
                            Text(if (sourceLocked) "This profile stays tied to one provider." else "You can use both providers in this profile.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Sources", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                listOf(SourceId("nhentai"), SourceId("mangadex")).forEach { source ->
                                    FilterChip(
                                        selected = source in newProfileSources,
                                        onClick = {
                                            newProfileSources = if (sourceLocked) setOf(source) else {
                                                val next = if (source in newProfileSources) newProfileSources - source else newProfileSources + source
                                                next.ifEmpty { newProfileSources }
                                            }
                                        },
                                        label = { Text(if (source.value == "nhentai") "NHentai" else "MangaDex") }
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    vm.createProfile(
                                        newProfileName,
                                        sourceLocked,
                                        if (sourceLocked) setOf(newProfileSources.first()) else newProfileSources
                                    )
                                    newProfileName = ""
                                    managementPage = ProfileManagementPage.OVERVIEW
                                },
                                enabled = newProfileName.isNotBlank() && newProfileSources.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Create profile") }
                        }

                        }
                        ProfileManagementPage.TRANSFER -> {
                        val transferTargets = vm.profiles.filter { it.id != vm.activeProfileId }
                        val transferEntries = filteredTransferEntries
                        if (transferTargets.isNotEmpty()) {
                            ProfileManagementSection(title = "From ${if (vm.incognitoModeEnabled) "current profile" else vm.activeProfile?.name ?: "Main"}") {
                                Text(
                                    if (transferEntries.isEmpty()) {
                                        "No entries match the current Search Everything, tags and entry filters."
                                    } else {
                                        "${transferEntries.size} match your current library filters · ${transferSelection.size} selected"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text("To profile", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    transferTargets.forEachIndexed { index, profile ->
                                        FilterChip(
                                            selected = transferTarget == profile.id,
                                            onClick = { transferTarget = profile.id },
                                            label = { Text(if (vm.incognitoModeEnabled) "Profile ${index + 1}" else profile.name) }
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                                Text("Action", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(selected = !transferIsMove, onClick = { transferIsMove = false },
                                        enabled = !vm.incognitoModeEnabled, label = { Text("Copy") })
                                    FilterChip(selected = transferIsMove, onClick = { transferIsMove = true },
                                        enabled = !vm.incognitoModeEnabled, label = { Text("Move") })
                                }
                                Text(if (transferIsMove) "The original is removed only after a validated copy." else "The original stays in this profile.",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Matching entries", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                transferEntries.take(100).forEach { item ->
                                    val key = item.entry.key
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.medium)
                                            .background(MaterialTheme.colorScheme.surfaceContainer)
                                            .clickable(enabled = !vm.incognitoModeEnabled) {
                                                transferSelection = if (key in transferSelection) transferSelection - key else transferSelection + key
                                            }
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(checked = key in transferSelection, onCheckedChange = null, enabled = !vm.incognitoModeEnabled)
                                        Spacer(Modifier.width(8.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(if (vm.incognitoModeEnabled) "Private entry" else item.entry.title,
                                                style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(if (key.sourceId.value == "nhentai") "NHentai · ${key.displayId}" else "MangaDex",
                                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                if (transferEntries.size > 100) {
                                    Text("Narrow Search Everything or apply tags to reach entries beyond the first 100.", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = {
                                        vm.transferProfileEntries(transferSelection, transferTarget, transferIsMove)
                                        transferSelection = emptySet()
                                    },
                                    enabled = !vm.incognitoModeEnabled && transferTarget.isNotBlank() && transferSelection.isNotEmpty(),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(if (transferIsMove) "Move selected" else "Copy selected") }
                            }
                        }
                    }
                    }
                }
            }
        }
    }
    }
    if (showProfileControls) pendingDeleteProfile?.let { profile ->
        Dialog(
            onDismissRequest = { pendingDeleteProfile = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.90f).widthIn(max = 520.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.48f))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "Delete ${if (vm.incognitoModeEnabled) "profile" else profile.name}?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "This removes this profile's library membership, read/rating/pin state, local tags, history, subscriptions and model settings. Shared metadata and media used by another profile stay intact. A validated V2 safety snapshot is written first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                    ) {
                        OutlinedButton(onClick = { pendingDeleteProfile = null }) { Text("Cancel") }
                        Button(onClick = { vm.deleteProfile(profile.id); pendingDeleteProfile = null },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError)) {
                            Text("Delete profile")
                        }
                    }
                }
            }
        }
    }
}

private fun profileSourceLabel(sources: Set<SourceId>): String = sources.sortedBy { it.value }
    .joinToString(" + ") { if (it.value == "nhentai") "NHentai" else "MangaDex" }
    .ifBlank { "No sources" }

@Composable
private fun ProfileAvatar(name: String, obscured: Boolean, size: Dp) {
    Box(
        modifier = Modifier.size(size).background(
            MaterialTheme.colorScheme.primary,
            CircleShape
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (obscured) "•" else name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun ProfileManagementAction(title: String, description: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLow).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("Open", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ProfileManagementSection(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

internal fun Modifier.adaptiveDashboardViewport(scale: Float): Modifier = layout { measurable, constraints ->
    if (!constraints.hasBoundedWidth || !constraints.hasBoundedHeight) {
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    } else {
        val safeScale = scale.coerceIn(0.58f, 1.24f)
        val virtualWidth = (constraints.maxWidth / safeScale).roundToInt().coerceAtLeast(1)
        val virtualHeight = (constraints.maxHeight / safeScale).roundToInt().coerceAtLeast(1)
        val placeable = measurable.measure(Constraints.fixed(virtualWidth, virtualHeight))
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.placeWithLayer(0, 0) {
                scaleX = safeScale
                scaleY = safeScale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}

internal fun adaptiveDashboardScale(screenHeightDp: Int): Float {
    val rawScale = screenHeightDp / 933f
    return if (rawScale in 0.94f..1.06f) 1f else rawScale.coerceIn(0.58f, 1.24f)
}

@Composable
internal fun Modifier.dashboardEntrance(
    visible: Boolean,
    delayMillis: Int,
    label: String
): Modifier {
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 210,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing
        ),
        label = "${label}Alpha"
    )
    val offsetY by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 0f else 10f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 230,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing
        ),
        label = "${label}Offset"
    )
    return graphicsLayer {
        this.alpha = alpha
        translationY = offsetY
    }
}
