package com.roinur.saucetracker.data.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceQueryParserTest {
    @Test fun structuredTermsAndFreeTextAreSeparated() {
        val query = SourceQueryParser.parse("slow burn artist:\"Jane Doe\" -language:english group:circle")
        assertEquals("slow burn", query.freeText)
        assertTrue(query.terms.any { it.field == SourceQueryField.ARTIST && it.value == "Jane Doe" && !it.excluded })
        assertTrue(query.terms.any { it.field == SourceQueryField.LANGUAGE && it.value == "english" && it.excluded })
        assertTrue(query.terms.any { it.field == SourceQueryField.GROUP && it.value == "circle" })
    }

    @Test fun sourceIdentityNeverCollidesOnRemoteIdAlone() {
        val nh = SourceEntryKey(SourceId("nhentai"), "123")
        val md = SourceEntryKey(SourceId("mangadex"), "123")
        assertFalse(nh == md)
        assertEquals("#123", nh.displayId)
        assertEquals("123", md.displayId)
    }
}
