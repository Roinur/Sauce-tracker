package com.roinur.saucetracker.data.profile

import org.junit.Assert.*
import org.junit.Test

class ProfilePreferenceChangesTest {
    @Test fun backupOfUnchangedPreferencesDoesNotRewriteRows() {
        val values = mapOf("taste_training_a" to "answer", "suggestion_weight" to "0.5")
        assertEquals(ProfilePreferenceChanges(emptySet(), emptyMap()), profilePreferenceChanges(values, values))
    }

    @Test fun updatesAndDeletesOnlyChangedKeys() {
        val previous = mapOf("retained" to "same", "changed" to "old", "removed" to "value")
        val incoming = mapOf("retained" to "same", "changed" to "new", "added" to "value")
        assertEquals(ProfilePreferenceChanges(setOf("removed"), mapOf("changed" to "new", "added" to "value")),
            profilePreferenceChanges(previous, incoming))
    }

    @Test fun freshProfileStillCapturesEverythingAndEmptySnapshotRemovesOldKeys() {
        val incoming = mapOf("key" to "encoded value")
        assertEquals(incoming, profilePreferenceChanges(emptyMap(), incoming).upserts)
        assertEquals(setOf("key"), profilePreferenceChanges(incoming, emptyMap()).removed)
    }
}
