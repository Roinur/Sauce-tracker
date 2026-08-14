package com.roinur.saucetracker.app

import android.app.Activity
import android.app.ActivityManager
import android.os.Handler
import android.os.Looper

internal class AppLifecycleObserver(
    private val activity: Activity,
    private val onHostStopped: () -> Unit,
    private val onHostResumed: () -> Unit,
    private val isDesktopBridgeRunning: () -> Boolean,
    private val stopDesktopBridge: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var recentsMonitor: Runnable? = null
    private var hostVisible = false

    fun onResume() {
        hostVisible = true
        stopRecentsMonitor()
        onHostResumed()
    }

    fun onStop(isChangingConfigurations: Boolean) {
        hostVisible = false
        if (!isChangingConfigurations) {
            onHostStopped()
        }
        startRecentsMonitor(isChangingConfigurations)
    }

    fun onDestroy() {
        stopRecentsMonitor()
    }

    private fun stopRecentsMonitor() {
        recentsMonitor?.let(handler::removeCallbacks)
        recentsMonitor = null
    }

    private fun startRecentsMonitor(isChangingConfigurations: Boolean) {
        stopRecentsMonitor()
        if (isChangingConfigurations) return
        val currentTaskId = activity.taskId
        val watcher = object : Runnable {
            override fun run() {
                if (hostVisible || activity.isChangingConfigurations) return
                val manager = activity.getSystemService(Activity.ACTIVITY_SERVICE) as? ActivityManager
                val taskStillExists = manager?.appTasks
                    ?.any { it.taskInfo?.id == currentTaskId }
                    ?: true
                if (!taskStillExists) {
                    if (isDesktopBridgeRunning()) stopDesktopBridge()
                    return
                }
                handler.postDelayed(this, RECENTS_POLL_INTERVAL_MS)
            }
        }
        recentsMonitor = watcher
        handler.postDelayed(watcher, RECENTS_INITIAL_DELAY_MS)
    }

    private companion object {
        const val RECENTS_INITIAL_DELAY_MS = 1_000L
        const val RECENTS_POLL_INTERVAL_MS = 1_500L
    }
}
