package com.example.saucetracker.feature.dashboard

import com.example.saucetracker.core.ui.theme.AccentMode
import com.example.saucetracker.HomeSurface
import com.example.saucetracker.ThemeMode

internal data class DashboardUiState(
    val homeSurface: HomeSurface,
    val themeMode: ThemeMode,
    val accentMode: AccentMode,
    val incognitoEnabled: Boolean,
    val cunnyModeActive: Boolean,
    val appLocked: Boolean,
    val desktopBlackout: Boolean
)

internal fun DashboardViewModel.dashboardUiState(homeSurface: HomeSurface): DashboardUiState = DashboardUiState(
    homeSurface = homeSurface,
    themeMode = themeMode,
    accentMode = accentMode,
    incognitoEnabled = incognitoModeEnabled,
    cunnyModeActive = cunnyModeActive,
    appLocked = appLockEnabled && appLocked,
    desktopBlackout = desktopBridgeRunning && desktopBridgeScreenBlackout
)
