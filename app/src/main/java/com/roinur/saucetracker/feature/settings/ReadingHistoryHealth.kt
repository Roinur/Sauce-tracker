package com.roinur.saucetracker.feature.settings

/** Informational classification, not an invented import/deletion/completion flag. */
internal fun readingHistoryHealthChecks(
    missingSessionEntries: Int,
    unratedMissingSessions: Int,
    invalidSessionOwners: Int,
    readWithoutSession: Int
): List<LibraryHealthCheck> = buildList {
    add(LibraryHealthCheck(
        "Read history",
        "$invalidSessionOwners sessions have invalid profile/source identities. " +
            "$readWithoutSession read entries have no recorded session (manual read status or older history). " +
            "A completed read or rating is not required for a valid backup.",
        if (invalidSessionOwners > 0) LibraryHealthLevel.ACTION_REQUIRED else LibraryHealthLevel.HEALTHY
    ))
    if (missingSessionEntries > 0) add(LibraryHealthCheck(
        "Reading outside library",
        "$missingSessionEntries preserved sessions, including $unratedMissingSessions unrated. " +
            "These may be unimported browser reads or entries removed later; older records do not distinguish them. " +
            "Their reading time and pages are retained and verified during restore, not treated as missing backup entries.",
        LibraryHealthLevel.HEALTHY
    ))
}
