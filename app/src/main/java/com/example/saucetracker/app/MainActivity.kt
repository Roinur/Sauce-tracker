package com.example.saucetracker.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.example.saucetracker.feature.browser.BrowserImportBridge
import com.example.saucetracker.core.diagnostics.GitHubMediaSession
import com.example.saucetracker.feature.dashboard.DashboardViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: DashboardViewModel
    private lateinit var appNavigator: AppNavigator
    private lateinit var externalIntentRouter: ExternalIntentRouter
    private lateinit var lifecycleObserver: AppLifecycleObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GitHubMediaSession.activateIfRequested(applicationContext, intent)
        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
        appNavigator = AppNavigator(viewModel::requestOpenSubscriptions)
        externalIntentRouter = ExternalIntentRouter(
            context = this,
            importBrowserInput = viewModel::importFromBrowserClipboard,
            queueSharedText = viewModel::queueIncomingShareText,
            queueSharedImage = viewModel::queueIncomingShareImage
        )
        lifecycleObserver = AppLifecycleObserver(
            activity = this,
            onHostStopped = viewModel::onHostStopped,
            onHostResumed = viewModel::onHostResumed,
            isDesktopBridgeRunning = { viewModel.desktopBridgeRunning },
            stopDesktopBridge = { viewModel.stopDesktopBridge(reportStatus = false) }
        )

        BrowserImportBridge.setListener { raw ->
            runOnUiThread { viewModel.importFromBrowserClipboard(raw) }
        }
        configureWindow()
        setContent { SauceTrackerApp(viewModel) }
        routeIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        lifecycleObserver.onResume()
    }

    override fun onStop() {
        lifecycleObserver.onStop(isChangingConfigurations)
        super.onStop()
    }

    override fun onDestroy() {
        lifecycleObserver.onDestroy()
        if (!isChangingConfigurations) BrowserImportBridge.setListener(null)
        super.onDestroy()
    }

    private fun routeIntent(intent: Intent?) {
        externalIntentRouter.route(intent)
        appNavigator.route(intent)
    }

    private fun configureWindow() {
        window.attributes = window.attributes.apply { preferredRefreshRate = 120f }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }
}
