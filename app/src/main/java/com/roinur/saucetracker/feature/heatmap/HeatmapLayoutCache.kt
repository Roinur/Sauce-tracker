package com.roinur.saucetracker.feature.heatmap

import com.roinur.saucetracker.TagGraphEntryLayoutResult

internal class HeatmapLayoutCache(
    private val maximumEntries: Int = 8
) {
    private val layouts = LinkedHashMap<String, TagGraphEntryLayoutResult>(maximumEntries, 0.75f, true)

    init {
        require(maximumEntries > 0)
    }

    @Synchronized
    operator fun get(key: String): TagGraphEntryLayoutResult? = layouts[key]

    @Synchronized
    operator fun set(key: String, value: TagGraphEntryLayoutResult) {
        layouts[key] = value
        val iterator = layouts.entries.iterator()
        while (layouts.size > maximumEntries && iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }

    @Synchronized
    fun clear() = layouts.clear()
}
