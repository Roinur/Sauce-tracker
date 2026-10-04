package com.roinur.saucetracker.data.database

import org.junit.Assert.assertEquals
import org.junit.Test

class SchemaInitializationGateTest {
    @Test fun repeatedScreenConnectionsOnlyRepairOnce() {
        val gate = SchemaInitializationGate()
        var repairs = 0
        repeat(20) { gate.ensure("production.db", 8) { repairs++ } }
        assertEquals(1, repairs)
    }

    @Test fun separateDatabasesAndVersionUpgradesStillInitialize() {
        val gate = SchemaInitializationGate()
        var repairs = 0
        gate.ensure("production.db", 8) { repairs++ }
        gate.ensure("github-media.db", 8) { repairs++ }
        gate.ensure("production.db", 9) { repairs++ }
        assertEquals(3, repairs)
    }

    @Test fun failureRetriesInsteadOfCachingPartialRepair() {
        val gate = SchemaInitializationGate()
        runCatching { gate.ensure("production.db", 8) { error("interrupted") } }
        var repairs = 0
        gate.ensure("production.db", 8) { repairs++ }
        gate.ensure("production.db", 8) { repairs++ }
        assertEquals(1, repairs)
    }

    @Test fun recreatedDatabaseAtSamePathIsNotSkipped() {
        val gate = SchemaInitializationGate()
        var repairs = 0
        gate.ensure("production.db", 8) { repairs++ }
        gate.invalidate("production.db")
        gate.ensure("production.db", 8) { repairs++ }
        assertEquals(2, repairs)
    }

    @Test fun simultaneousBrowserAndWorkerOpensShareOneRepair() {
        val gate = SchemaInitializationGate()
        var repairs = 0
        val threads = List(8) { Thread { gate.ensure("production.db", 8) { repairs++ } } }
        threads.forEach(Thread::start)
        threads.forEach(Thread::join)
        assertEquals(1, repairs)
    }
}
