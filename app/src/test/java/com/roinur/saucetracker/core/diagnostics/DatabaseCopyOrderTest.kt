package com.roinur.saucetracker.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseCopyOrderTest {
    @Test fun restrictiveSourceParentsAreCopiedBeforeProfilesAndProgress() {
        val parents = linkedMapOf(
            "progress" to setOf("sources", "profiles"),
            "profile_sources" to setOf("profiles", "sources"),
            "profiles" to emptySet(), "sources" to emptySet()
        )
        val order = DatabaseCopyOrder.parentFirst(parents)
        parents.forEach { (child, relations) ->
            relations.forEach { assertTrue(order.indexOf(it) < order.indexOf(child)) }
        }
        assertEquals(parents.size, order.size)
    }
    @Test fun independentTablesKeepStableOrder() {
        assertEquals(listOf("entries", "tags"), DatabaseCopyOrder.parentFirst(linkedMapOf("entries" to emptySet(), "tags" to emptySet())))
    }
    @Test fun selfReferencingTableIsOneCopyStatement() {
        assertEquals(listOf("tree"), DatabaseCopyOrder.parentFirst(mapOf("tree" to setOf("tree"))))
    }
    @Test(expected = IllegalStateException::class)
    fun cyclesFailBeforeAnyRowsAreChanged() {
        DatabaseCopyOrder.parentFirst(mapOf("a" to setOf("b"), "b" to setOf("a")))
    }
}
