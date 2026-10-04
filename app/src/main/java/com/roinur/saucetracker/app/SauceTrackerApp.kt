package com.roinur.saucetracker.app

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.work.Configuration
import androidx.work.WorkManager
import com.roinur.saucetracker.SauceTrackerContent
import com.roinur.saucetracker.feature.dashboard.DashboardViewModel
import com.roinur.saucetracker.ThumbnailBitmapCache
import com.roinur.saucetracker.feature.browser.GalleryBrowserThumbnailCache
import com.roinur.saucetracker.feature.slideshow.GalleryPageBitmapCache

class SauceTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (android.os.Build.VERSION.SDK_INT >= 28 && Application.getProcessName().endsWith(":githubmedia")) return
        ensureWorkManagerInitialized(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            // These are all rebuildable display caches. Dropping them under pressure keeps a
            // MangaDex Browser -> Reader transition from competing with the 256 MB app heap.
            GalleryBrowserThumbnailCache.clear()
            ThumbnailBitmapCache.clear()
            GalleryPageBitmapCache.clear()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        GalleryBrowserThumbnailCache.clear()
        ThumbnailBitmapCache.clear()
        GalleryPageBitmapCache.clear()
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
