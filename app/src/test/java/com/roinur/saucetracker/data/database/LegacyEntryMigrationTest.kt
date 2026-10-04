package com.roinur.saucetracker.data.database

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LegacyEntryMigrationTest {
    @Test fun subtitlesSurviveQuotesSlashesWhitespaceAndControlCharacters() {
        val subtitle = "Alternate \"title\" \\ path\nline\r\ttab\u0001 日本語"
        val encoded = legacyAlternateTitlesReplacement("[]", subtitle)!!
        val titles = Json.parseToJsonElement(encoded) as JsonArray
        assertEquals(subtitle, titles.single().jsonPrimitive.content)
    }

    @Test fun repairsMalformedPreviouslyMigratedTitles() {
        val subtitle = "Title \"variant\""
        val encoded = legacyAlternateTitlesReplacement("[broken", subtitle)!!
        assertEquals(subtitle, (Json.parseToJsonElement(encoded) as JsonArray).single().jsonPrimitive.content)
    }

    @Test fun preservesAlreadyFetchedProviderVariants() {
        assertNull(legacyAlternateTitlesReplacement("[\"Original\",\"Other variant\"]", "Old subtitle"))
    }

    @Test fun emptySubtitleNeedsNoInventedTitle() {
        assertNull(legacyAlternateTitlesReplacement("[]", ""))
        assertEquals("[]", legacyAlternateTitlesReplacement("not json", ""))
    }
}
