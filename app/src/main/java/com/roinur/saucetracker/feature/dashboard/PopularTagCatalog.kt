package com.roinur.saucetracker

/** Match each name once, rather than normalizing both sides of every catalog pair. */
internal fun missingPopularTags(
    local: List<PopularTagSeed>,
    catalog: List<PopularTagRow>,
    normalize: (String) -> String = ::normalizeTagName
): List<PopularTagSeed> {
    val known = catalog.mapTo(HashSet(catalog.size)) { normalize(it.name) to it.type }
    return local.filter { (normalize(it.name) to it.type) !in known }
}
