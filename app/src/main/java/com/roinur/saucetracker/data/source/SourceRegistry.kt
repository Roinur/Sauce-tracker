package com.roinur.saucetracker.data.source

class SourceRegistry(adapters: List<SourceAdapter>) {
    private val byId = adapters.associateBy { it.id }

    init {
        require(byId.size == adapters.size) { "Duplicate source id." }
        require(SourceId("nhentai") in byId) { "NHentai adapter is required." }
        require(SourceId("mangadex") in byId) { "MangaDex adapter is required." }
    }

    val sources: List<SourceAdapter> get() = byId.values.sortedBy { it.displayName }
    fun adapter(id: SourceId): SourceAdapter? = byId[id]
    fun requireAdapter(id: SourceId): SourceAdapter = byId[id] ?: error("Unknown source: $id")
    fun recognize(input: String): SourceAdapter? = sources.firstOrNull { it.recognizes(input) }

    companion object {
        // Adapters own connection pools and short-lived provider caches. Reusing the default
        // registry lets Browser, Library, Bridge and Slideshow share those warm paths instead
        // of paying a fresh MangaDex DNS/TLS/manifest setup at every screen boundary.
        private val defaultRegistry: SourceRegistry by lazy {
            SourceRegistry(listOf(NhentaiSourceAdapter(), MangaDexSourceAdapter()))
        }

        fun createDefault(): SourceRegistry = defaultRegistry
    }
}

internal fun resolveBrowserSource(
    allowedSources: Set<SourceId>,
    input: String,
    forcedSource: SourceId? = null,
    registry: SourceRegistry = SourceRegistry.createDefault()
): SourceId? {
    forcedSource?.takeIf { it in allowedSources }?.let { return it }
    val requestedByQuery = runCatching {
        SourceQueryParser.parse(input).terms
            .firstOrNull { it.field == SourceQueryField.SOURCE && !it.excluded }
            ?.value?.trim()?.lowercase()?.let(::SourceId)
    }.getOrNull()?.takeIf { it in allowedSources }
    if (requestedByQuery != null) return requestedByQuery
    val recognized = input.takeIf(String::isNotBlank)?.let(registry::recognize)?.id
        ?.takeIf { it in allowedSources }
    return recognized ?: allowedSources.singleOrNull()
}
