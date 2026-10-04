package com.roinur.saucetracker.feature.saucefinder

/** Stable within each tier; downloads win even when they have not been marked read. */
internal fun <T> sauceFinderIndexOrder(items: List<T>, downloaded: (T) -> Boolean, read: (T) -> Boolean): List<T> =
    items.sortedBy { when { downloaded(it) -> 0; read(it) -> 1; else -> 2 } }
