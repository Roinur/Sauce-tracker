package com.roinur.saucetracker

import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionRouteTest {
    @Test
    fun `mangadex author names remain subscribable with punctuation intact`() {
        assertEquals("author", normalizeSubscriptionRouteType("Author"))
        assertEquals("TYPE-MOON", normalizeSubscriptionRouteName("author", "TYPE-MOON"))
        assertEquals("author|type-moon", subscriptionRouteKey("author", "TYPE-MOON"))
    }
}
