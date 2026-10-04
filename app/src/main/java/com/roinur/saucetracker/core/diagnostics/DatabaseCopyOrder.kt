package com.roinur.saucetracker.core.diagnostics

/** Parent-first insertion and reverse deletion without disabling foreign keys. */
internal object DatabaseCopyOrder {
    fun parentFirst(parents: Map<String, Set<String>>): List<String> {
        val remaining = parents.keys.toMutableSet()
        val result = mutableListOf<String>()
        while (remaining.isNotEmpty()) {
            val ready = remaining.firstOrNull { table ->
                parents.getValue(table).none { it != table && it in remaining }
            } ?: error("Cyclic database-copy dependencies.")
            result += ready
            remaining.remove(ready)
        }
        return result
    }
}
