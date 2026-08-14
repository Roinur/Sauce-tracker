package com.roinur.saucetracker

import java.util.Locale

enum class TagGraphTab(val label: String) {
    HEATMAP("Tag heatmap"),
    RAW("Raw frequency"),
    RATED("Rated frequency")
}

enum class TagHeatmapDisplayMode(val label: String) {
    TAGS("Tags"),
    ENTRIES("Entries")
}

data class TagGraphSeed(
    val name: String,
    val normalizedName: String,
    val localCount: Int,
    val popularCount: Int,
    val ratedSignalSum: Float,
    val ratedMentionCount: Int,
    val entryCodes: IntArray
)

data class TagGraphDataSnapshot(
    val totalEntries: Int,
    val totalRatedEntries: Int,
    val totalPopularTagUsage: Long,
    val seeds: List<TagGraphSeed>,
    val entrySeeds: List<TagGraphEntrySeed>
)

data class TagGraphEntrySeed(
    val code: Int,
    val title: String,
    val thumbnailUrl: String,
    val rating: Int,
    val isRead: Boolean,
    val pinned: Boolean,
    val tagNames: List<String>
)

data class TagGraphNode(
    val name: String,
    val normalizedName: String,
    val localCount: Int,
    val popularCount: Int,
    val ratedSignalSum: Float,
    val ratedMentionCount: Int,
    val heatX: Float,
    val heatY: Float,
    val rawX: Float,
    val rawY: Float,
    val ratedX: Float,
    val ratedY: Float
)

data class TagGraphSnapshot(
    val nodes: List<TagGraphNode>,
    val entryNodes: List<TagGraphEntryNode>,
    val strongestNeighborsByTag: Map<String, List<String>>,
    val totalEntries: Int,
    val totalRatedEntries: Int,
    val totalPopularTagUsage: Long
)

data class TagGraphEntryNode(
    val code: Int,
    val title: String,
    val thumbnailUrl: String,
    val rating: Int,
    val isRead: Boolean,
    val pinned: Boolean,
    val tagNames: List<String>,
    val dominantCircleTags: List<String>,
    val boundaryCenterX: Float,
    val boundaryCenterY: Float,
    val boundaryRadiusPx: Float,
    val x: Float,
    val y: Float
)

data class TagGraphEntryFamilyCircle(
    val tagName: String,
    val label: String,
    val centerX: Float,
    val centerY: Float,
    val radiusPx: Float,
    val entryCount: Int
)

data class TagGraphEntryLayoutResult(
    val nodes: List<TagGraphEntryNode>,
    val familyCircles: List<TagGraphEntryFamilyCircle>
)

typealias EntryHeatmapCacheRecord = com.roinur.saucetracker.data.database.entity.HeatmapCacheEntity

data class EntryHeatmapRecalculationSummary(
    val entryCount: Int,
    val dominantFamilies: List<String>
)

internal sealed interface GraphSelectionSheetState {
    data class Entry(
        val entry: TagGraphEntryNode,
        val dominantCircleTags: List<String>,
        val returnTagNode: TagGraphNode? = null
    ) : GraphSelectionSheetState

    data class Tag(
        val node: TagGraphNode
    ) : GraphSelectionSheetState
}

internal fun homeHeatmapBaseSheetHeightFraction(state: GraphSelectionSheetState?): Float {
    return when (state) {
        is GraphSelectionSheetState.Entry -> 0.58f
        is GraphSelectionSheetState.Tag -> 0.54f
        null -> 0.58f
    }
}

private fun tagGraphCircleKey(entry: TagGraphEntryNode): String {
    return buildString {
        append(entry.dominantCircleTags.sorted().joinToString("|"))
        append('#')
        append("%.4f".format(Locale.US, entry.boundaryCenterX))
        append(':')
        append("%.4f".format(Locale.US, entry.boundaryCenterY))
    }
}

internal fun tagGraphEntryLayoutCacheKey(snapshot: TagGraphSnapshot): String {
    var hash = 17
    hash = (hash * 31) + snapshot.totalEntries
    hash = (hash * 31) + snapshot.nodes.size
    hash = (hash * 31) + snapshot.entryNodes.size
    snapshot.nodes.forEach { node ->
        hash = (hash * 31) + node.normalizedName.hashCode()
        hash = (hash * 31) + node.localCount
        hash = (hash * 31) + java.lang.Float.floatToIntBits(node.heatX)
        hash = (hash * 31) + java.lang.Float.floatToIntBits(node.heatY)
    }
    snapshot.entryNodes.forEach { entry ->
        hash = (hash * 31) + entry.code
        hash = (hash * 31) + entry.tagNames.size
        entry.tagNames.forEach { tag ->
            hash = (hash * 31) + tag.hashCode()
        }
    }
    return hash.toString()
}

internal fun formatTagGraphCircleLabel(tags: List<String>): String {
    if (tags.isEmpty()) return "Mixed"
    val cleaned = tags.map { tag ->
        if (tag.length <= 18) tag else tag.take(15).trimEnd() + "..."
    }
    return when {
        cleaned.size == 1 -> cleaned.first()
        cleaned.size == 2 -> cleaned.joinToString(" + ")
        else -> "${cleaned[0]} + ${cleaned[1]} +${cleaned.size - 2}"
    }
}

internal const val TAG_GRAPH_ENTRY_SPACING_MULTIPLIER = 1.22f
private const val ENTRY_HEATMAP_CACHE_SOLVER_WIDTH_PX = 1600f
private const val ENTRY_HEATMAP_CACHE_SOLVER_HEIGHT_PX = 2200f
private const val ENTRY_HEATMAP_CACHE_SPACING_PX = 56f
