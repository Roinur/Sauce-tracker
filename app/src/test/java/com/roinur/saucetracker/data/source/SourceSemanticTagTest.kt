package com.roinur.saucetracker.data.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SourceSemanticTagTest {
    @Test fun sameSemanticTagMatchesAcrossProviderIds() {
        val nhentai = SourceTag(name = "incest", type = "tag", remoteId = "")
        val mangaDex = SourceTag(name = "  Incest  ", type = "TAG", remoteId = "5920b825-4181-4a17-beeb-9918b0ff7a30")

        assertEquals(nhentai.semanticKey(), mangaDex.semanticKey())
    }

    @Test fun differentTagTypesAreNotSilentlyMerged() {
        val subject = SourceTag(name = "English", type = "tag")
        val language = SourceTag(name = "English", type = "language")

        assertNotEquals(subject.semanticKey(), language.semanticKey())
    }
}
