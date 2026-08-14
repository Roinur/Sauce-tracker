package com.example.saucetracker.feature.saucefinder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SauceFinderMatcherTest {
    @Test
    fun identicalFingerprintsAreStrongMatches() {
        val fingerprint = SauceImageFingerprint(longArrayOf(1L, 2L, 3L, 4L, 5L))

        assertEquals(1f, sauceFingerprintSimilarity(fingerprint, fingerprint), 0.0001f)
        assertEquals("Strong match", sauceConfidenceLabel(1f))
    }

    @Test
    fun cropVariantCanMatchCandidateVariant() {
        val query = SauceImageFingerprint(longArrayOf(0b1111L, 0b1010L, 0b0101L))
        val candidate = SauceImageFingerprint(longArrayOf(Long.MAX_VALUE, 0b1010L, Long.MIN_VALUE))

        assertTrue(sauceFingerprintSimilarity(query, candidate) > 0.70f)
    }

    @Test
    fun confidenceBandsStayStable() {
        assertEquals("Strong match", sauceConfidenceLabel(0.90f))
        assertEquals("Likely match", sauceConfidenceLabel(0.82f))
        assertEquals("Possible match", sauceConfidenceLabel(0.81f))
    }
}
