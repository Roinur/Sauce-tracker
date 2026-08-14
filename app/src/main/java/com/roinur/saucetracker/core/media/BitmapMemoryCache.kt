package com.roinur.saucetracker.core.media

internal class BitmapMemoryCache<K : Any, V : Any>(
    private val maximumBytes: Long,
    private val sizeOf: (V) -> Long
) {
    private val entries = LinkedHashMap<K, V>(64, 0.75f, true)
    private var currentBytes = 0L

    init {
        require(maximumBytes > 0L)
    }

    @Synchronized
    operator fun get(key: K): V? = entries[key]

    @Synchronized
    fun put(key: K, value: V) {
        entries.remove(key)?.let { currentBytes -= measuredSize(it) }
        entries[key] = value
        currentBytes += measuredSize(value)
        trimToBudget()
    }

    @Synchronized
    fun clear() {
        entries.clear()
        currentBytes = 0L
    }

    @Synchronized
    fun sizeBytes(): Long = currentBytes

    private fun trimToBudget() {
        val iterator = entries.entries.iterator()
        while (currentBytes > maximumBytes && iterator.hasNext()) {
            currentBytes -= measuredSize(iterator.next().value)
            iterator.remove()
        }
    }

    private fun measuredSize(value: V): Long = sizeOf(value).coerceAtLeast(1L)
}
