package com.roinur.saucetracker.data.backup

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class BackupHistoryValidationTest {
    private val legacy = """{"entry_code":123,"started_at":"2026-01-01 12:00:00","ended_at":"2026-01-01 12:01:00","day_key":"2026-01-01","pages_viewed":20,"seconds_elapsed":60,"rating":4,"is_reread":0}"""
    @Test fun oldSessionDefaultsRoundTripWithoutIgnoringValues() {
        val restored = Json.parseToJsonElement(legacy).jsonObject.toMutableMap().apply {
            put("profile_id", JsonPrimitive("main")); put("source_id", JsonPrimitive("nhentai"))
            put("remote_id", JsonPrimitive("123")); put("chapter_id", JsonPrimitive(""))
            put("session_key", JsonPrimitive(UUID.nameUUIDFromBytes("nhentai|123|2026-01-01 12:00:00|2026-01-01 12:01:00|123|20|60|false".toByteArray()).toString()))
        }
        fun canonical(value: String) = BackupHistoryValidation.canonicalRows(value, "reading_sessions")
        assertEquals(canonical("[$legacy]"), canonical("[${JsonObject(restored)}]"))
        restored["pages_viewed"] = JsonPrimitive(19)
        assertNotEquals(canonical("[$legacy]"), canonical("[${JsonObject(restored)}]"))
        assertNotEquals(canonical("[$legacy,$legacy]"), canonical("[$legacy]"))
    }
    @Test fun subscriptionDefaultsPreserveExplicitOwnership() {
        for (table in listOf("subscriptions", "subscription_seen_codes", "subscription_events")) {
            val original = """[{"route_type":"tag","route_name":"english"}]"""
            val restored = """[{"route_name":"english","route_type":"tag","profile_id":"main","source_id":"nhentai"}]"""
            assertEquals(BackupHistoryValidation.canonicalRows(original, table), BackupHistoryValidation.canonicalRows(restored, table))
            assertNotEquals(BackupHistoryValidation.canonicalRows(original, table), BackupHistoryValidation.canonicalRows(restored.replace("main", "another"), table))
        }
    }
    @Test fun incompleteMultiSourceBackupsCannotSilentlyRestore() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupHistoryValidation.validateOwners(null, listOf("""[{"source_id":"mangadex","profile_id":"main"}]"""))
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackupHistoryValidation.validateOwners(null, listOf("""[{"profile_id":"another"}]"""))
        }
        BackupHistoryValidation.validateOwners(null, listOf("[$legacy]", null))
    }
    @Test fun unimportedBrowserEntriesAreValidButUnknownOwnersAreNot() {
        val platform = """{"profiles":[{"id":"dex"}],"sources":[{"id":"mangadex"}]}"""
        val sessions = """[{"profile_id":"dex","source_id":"mangadex","remote_id":"not-in-library"}]"""
        BackupHistoryValidation.validateOwners(platform, listOf(sessions))
        assertThrows(IllegalArgumentException::class.java) { BackupHistoryValidation.validateOwners(platform, listOf(sessions.replace("dex\"", "missing\""))) }
    }
}
