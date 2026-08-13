package com.example.saucetracker.app

import android.content.Context
import android.content.Intent
import com.example.saucetracker.EXTRA_BROWSER_IMPORT_INPUT

internal class ExternalIntentRouter(
    private val context: Context,
    private val importBrowserInput: (String) -> Unit,
    private val queueSharedText: (String) -> Unit
) {
    fun route(intent: Intent?) {
        routeBrowserImport(intent)
        routeSharedText(intent)
    }

    private fun routeBrowserImport(intent: Intent?) {
        val raw = intent
            ?.getStringExtra(EXTRA_BROWSER_IMPORT_INPUT)
            ?.trim()
            .orEmpty()
        if (raw.isBlank()) return
        importBrowserInput(raw)
        intent?.removeExtra(EXTRA_BROWSER_IMPORT_INPUT)
    }

    private fun routeSharedText(intent: Intent?) {
        val incoming = intent ?: return
        if (!Intent.ACTION_SEND.equals(incoming.action, ignoreCase = true)) return

        val sharedText = buildList {
            incoming.getStringExtra(Intent.EXTRA_TEXT)
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?.let(::add)
            runCatching {
                incoming.clipData
                    ?.takeIf { it.itemCount > 0 }
                    ?.getItemAt(0)
                    ?.coerceToText(context)
                    ?.toString()
                    ?.trim()
                    .orEmpty()
            }.getOrDefault("")
                .takeIf(String::isNotBlank)
                ?.let(::add)
        }.firstOrNull().orEmpty()

        if (sharedText.isBlank()) return
        queueSharedText(sharedText)
        incoming.removeExtra(Intent.EXTRA_TEXT)
    }
}
