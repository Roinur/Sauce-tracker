package com.roinur.saucetracker.app

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.work.Configuration
import androidx.work.WorkManager
import com.roinur.saucetracker.SauceTrackerContent
import com.roinur.saucetracker.feature.dashboard.DashboardViewModel

class SauceTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (android.os.Build.VERSION.SDK_INT >= 28 && Application.getProcessName().endsWith(":githubmedia")) return
        ensureWorkManagerInitialized(this)
    }
}

@Composable
internal fun SauceTrackerApp(viewModel: DashboardViewModel) {
    SauceTrackerContent(viewModel)
}

private fun ensureWorkManagerInitialized(application: Application) {
    if (runCatching { WorkManager.getInstance(application) }.isSuccess) return
    runCatching {
        WorkManager.initialize(
            application,
            Configuration.Builder().build()
        )
    }
}
