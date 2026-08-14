package com.example.saucetracker.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.saucetracker.EXTRA_BROWSER_IMPORT_INPUT

internal class ExternalIntentRouter(
    private val context: Context,
    private val importBrowserInput: (String) -> Unit,
    private val queueSharedText: (String) -> Unit,
    private val queueSharedImage: (Uri) -> Unit
) {
    fun route(intent: Intent?) {
        routeBrowserImport(intent)
        routeSharedImage(intent)
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
        if (incoming.type.orEmpty().startsWith("image/", ignoreCase = true)) return

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

    private fun routeSharedImage(intent: Intent?) {
        val incoming = intent ?: return
        if (!Intent.ACTION_SEND.equals(incoming.action, ignoreCase = true)) return
        if (!incoming.type.orEmpty().startsWith("image/", ignoreCase = true)) return

        val stream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            incoming.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            incoming.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
        } ?: incoming.clipData
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.uri

        stream?.let(queueSharedImage)
        incoming.removeExtra(Intent.EXTRA_STREAM)
    }
}
