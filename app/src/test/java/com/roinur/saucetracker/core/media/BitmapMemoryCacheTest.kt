package com.roinur.saucetracker.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BitmapMemoryCacheTest {
    @Test
    fun `evicts least recently used values by byte size`() {
        val cache = BitmapMemoryCache<String, Int>(maximumBytes = 11L, sizeOf = Int::toLong)

        cache.put("first", 6)
        cache.put("second", 4)
        assertEquals(6, cache["first"])

        cache.put("third", 5)

        assertEquals(6, cache["first"])
        assertNull(cache["second"])
        assertEquals(5, cache["third"])
        assertEquals(11L, cache.sizeBytes())
    }

    @Test
    fun `oversized value is not retained`() {
        val cache = BitmapMemoryCache<String, Int>(maximumBytes = 8L, sizeOf = Int::toLong)

        cache.put("oversized", 12)

        assertNull(cache["oversized"])
        assertEquals(0L, cache.sizeBytes())
    }
}
