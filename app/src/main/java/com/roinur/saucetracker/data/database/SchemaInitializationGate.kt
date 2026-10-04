package com.roinur.saucetracker.data.database

/** Opportunistic compatibility repairs are process work, not per-screen work. */
internal class SchemaInitializationGate {
    private val initialized = HashMap<String, Int>()

    @Synchronized
    fun ensure(key: String, version: Int, initialize: () -> Unit) {
        if (initialized[key] == version) return
        initialize()
        // A failed repair is never recorded as successful; the next open can retry.
        initialized[key] = version
    }

    @Synchronized
    fun invalidate(key: String) {
        initialized.remove(key)
    }
}
