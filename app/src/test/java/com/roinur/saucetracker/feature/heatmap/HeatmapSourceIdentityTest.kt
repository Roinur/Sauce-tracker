package com.roinur.saucetracker.feature.heatmap

import com.roinur.saucetracker.TagGraphEntryNode
import com.roinur.saucetracker.TagGraphSnapshot
import com.roinur.saucetracker.tagGraphEntryLayoutCacheKey
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HeatmapSourceIdentityTest {
    @Test
    fun `layout cache identity includes source and remote id`() {
        val nhentai = snapshot(entry(sourceId = "nhentai", remoteId = "42"))
        val mangaDex = snapshot(entry(sourceId = "mangadex", remoteId = "42"))

        assertNotEquals(tagGraphEntryLayoutCacheKey(nhentai), tagGraphEntryLayoutCacheKey(mangaDex))
    }

    private fun snapshot(entry: TagGraphEntryNode) = TagGraphSnapshot(
        nodes = emptyList(),
        entryNodes = listOf(entry),
        strongestNeighborsByTag = emptyMap(),
        totalEntries = 1,
        totalRatedEntries = 0,
        totalPopularTagUsage = 0
    )

    private fun entry(sourceId: String, remoteId: String) = TagGraphEntryNode(
        code = -1,
        title = "Example",
        thumbnailUrl = "",
        rating = 0,
        isRead = false,
        pinned = false,
        tagNames = listOf("example"),
        dominantCircleTags = emptyList(),
        boundaryCenterX = 0.5f,
        boundaryCenterY = 0.5f,
        boundaryRadiusPx = 0f,
        x = 0.5f,
        y = 0.5f,
        sourceId = sourceId,
        remoteId = remoteId
    )
}
