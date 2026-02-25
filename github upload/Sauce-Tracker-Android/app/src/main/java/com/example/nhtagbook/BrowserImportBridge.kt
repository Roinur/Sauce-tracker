package com.example.saucetracker

import java.util.concurrent.ConcurrentLinkedQueue

object BrowserImportBridge {
    private val pending = ConcurrentLinkedQueue<String>()

    @Volatile
    private var listener: ((String) -> Unit)? = null

    @Synchronized
    fun setListener(newListener: ((String) -> Unit)?) {
        listener = newListener
        if (newListener != null) {
            while (true) {
                val next = pending.poll() ?: break
                newListener(next)
            }
        }
    }

    fun submit(rawInput: String) {
        val clean = rawInput.trim()
        if (clean.isBlank()) return
        val currentListener = listener
        if (currentListener != null) {
            currentListener(clean)
            return
        }
        pending.add(clean)
    }
}
