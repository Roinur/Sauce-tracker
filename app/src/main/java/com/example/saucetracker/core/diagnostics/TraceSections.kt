package com.example.saucetracker.core.diagnostics

import android.os.Trace

internal inline fun <T> tracedSection(name: String, block: () -> T): T {
    Trace.beginSection(name.take(127))
    return try {
        block()
    } finally {
        Trace.endSection()
    }
}
