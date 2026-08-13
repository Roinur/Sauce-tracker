package com.example.saucetracker

import android.net.Uri
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import java.util.regex.Pattern

internal fun tagSortArrow(vm: com.example.saucetracker.feature.dashboard.DashboardViewModel, field: TagSortField): String {
    if (vm.tagSortField != field) return ""
    return if (vm.tagSortDirection == SortDirection.DESC) " ▼" else " ▲"
}

internal fun blockedTagSortArrow(vm: com.example.saucetracker.feature.dashboard.DashboardViewModel, field: TagSortField): String {
    if (vm.blockedTagSortField != field) return ""
    return if (vm.blockedTagSortDirection == SortDirection.DESC) " ▼" else " ▲"
}

internal fun creatorSortArrow(vm: com.example.saucetracker.feature.dashboard.DashboardViewModel, field: CreatorSortField): String {
    if (vm.creatorSortField != field) return ""
    return if (vm.creatorSortDirection == SortDirection.DESC) " ▼" else " ▲"
}

internal fun entrySortArrow(vm: com.example.saucetracker.feature.dashboard.DashboardViewModel, field: EntrySortField): String {
    if (vm.sortField != field) return ""
    return if (vm.sortDirection == SortDirection.DESC) " ▼" else " ▲"
}

internal fun entrySortLabel(field: EntrySortField): String {
    return when (field) {
        EntrySortField.CODE -> "Code"
        EntrySortField.TITLE -> "Title"
        EntrySortField.PAGES -> "Pages"
        EntrySortField.UPLOAD -> "Uploaded Date"
        EntrySortField.ADDED -> "Fetched Date"
        EntrySortField.READ -> "Read Date"
        EntrySortField.RATING -> "Rating"
    }
}

internal fun readableEntryReadFilterModeLabel(mode: EntryReadFilterMode): String {
    return when (mode) {
        EntryReadFilterMode.ALL -> "Show All"
        EntryReadFilterMode.READ -> "Show Read"
        EntryReadFilterMode.UNREAD -> "Show Unread"
        EntryReadFilterMode.DOWNLOADED -> "Show Downloaded"
    }
}

internal fun homeSectionLabel(section: HomeSection): String {
    return when (section) {
        HomeSection.TAGS -> "Tags"
        HomeSection.ENTRIES -> "Entries"
        HomeSection.SUGGESTED -> "Suggested entries"
        HomeSection.SUBSCRIPTIONS -> "Subscriptions"
        HomeSection.CREATORS -> "Artists / Groups"
        HomeSection.HEATMAP -> "Heatmap Overview"
    }
}

internal fun homeSurfaceTitle(surface: HomeSurface): String {
    return when (surface) {
        HomeSurface.DASHBOARD -> APP_TITLE
        HomeSurface.ENTRIES -> "Entries"
        HomeSurface.TAGS -> "Tags"
        HomeSurface.SUGGESTED -> "Suggested entries"
        HomeSurface.SUBSCRIPTIONS -> "Subscriptions"
        HomeSurface.CREATORS -> "Artists / Groups"
        HomeSurface.HEATMAP -> "Heatmap Overview"
        HomeSurface.HISTORY -> "Reading History"
    }
}

internal fun describeEntrySort(field: EntrySortField?, direction: SortDirection): String {
    if (field == null) return "standard"
    val order = if (direction == SortDirection.DESC) "descending" else "ascending"
    return "${entrySortLabel(field)} ($order)"
}

internal fun tagSortLabel(field: TagSortField): String {
    return when (field) {
        TagSortField.NAME -> "name"
        TagSortField.TYPE -> "type"
        TagSortField.COUNT -> "count"
    }
}

internal fun describeTagSort(field: TagSortField, direction: SortDirection): String {
    val order = if (direction == SortDirection.DESC) "descending" else "ascending"
    return "${tagSortLabel(field)} ($order)"
}

internal fun creatorSortLabel(field: CreatorSortField): String {
    return when (field) {
        CreatorSortField.NAME -> "name"
        CreatorSortField.TYPE -> "type"
        CreatorSortField.COUNT -> "count"
    }
}

internal fun describeCreatorSort(field: CreatorSortField, direction: SortDirection): String {
    val order = if (direction == SortDirection.DESC) "descending" else "ascending"
    return "${creatorSortLabel(field)} ($order)"
}

internal fun entrySortPresets(): List<EntrySortPreset> {
    return listOf(
        EntrySortPreset("Standard (Newest fetched first)", null, SortDirection.DESC),
        EntrySortPreset("Code (Newest first)", EntrySortField.CODE, SortDirection.DESC),
        EntrySortPreset("Code (Oldest first)", EntrySortField.CODE, SortDirection.ASC),
        EntrySortPreset("Title (A-Z)", EntrySortField.TITLE, SortDirection.ASC),
        EntrySortPreset("Title (Z-A)", EntrySortField.TITLE, SortDirection.DESC),
        EntrySortPreset("Pages (High to low)", EntrySortField.PAGES, SortDirection.DESC),
        EntrySortPreset("Pages (Low to high)", EntrySortField.PAGES, SortDirection.ASC),
        EntrySortPreset("Uploaded (Newest first)", EntrySortField.UPLOAD, SortDirection.DESC),
        EntrySortPreset("Uploaded (Oldest first)", EntrySortField.UPLOAD, SortDirection.ASC),
        EntrySortPreset("Fetched (Newest first)", EntrySortField.ADDED, SortDirection.DESC),
        EntrySortPreset("Fetched (Oldest first)", EntrySortField.ADDED, SortDirection.ASC),
        EntrySortPreset("Read (Newest first)", EntrySortField.READ, SortDirection.DESC),
        EntrySortPreset("Read (Oldest first)", EntrySortField.READ, SortDirection.ASC),
        EntrySortPreset("Rating (High to low)", EntrySortField.RATING, SortDirection.DESC)
    )
}

internal fun tagSortPresets(): List<TagSortPreset> {
    return listOf(
        TagSortPreset("Count (High to low)", TagSortField.COUNT, SortDirection.DESC),
        TagSortPreset("Count (Low to high)", TagSortField.COUNT, SortDirection.ASC),
        TagSortPreset("Name (A-Z)", TagSortField.NAME, SortDirection.ASC),
        TagSortPreset("Name (Z-A)", TagSortField.NAME, SortDirection.DESC),
        TagSortPreset("Type (A-Z)", TagSortField.TYPE, SortDirection.ASC),
        TagSortPreset("Type (Z-A)", TagSortField.TYPE, SortDirection.DESC)
    )
}

internal fun creatorSortPresets(): List<CreatorSortPreset> {
    return listOf(
        CreatorSortPreset("Count (High to low)", CreatorSortField.COUNT, SortDirection.DESC),
        CreatorSortPreset("Count (Low to high)", CreatorSortField.COUNT, SortDirection.ASC),
        CreatorSortPreset("Name (A-Z)", CreatorSortField.NAME, SortDirection.ASC),
        CreatorSortPreset("Name (Z-A)", CreatorSortField.NAME, SortDirection.DESC),
        CreatorSortPreset("Type (A-Z)", CreatorSortField.TYPE, SortDirection.ASC),
        CreatorSortPreset("Type (Z-A)", CreatorSortField.TYPE, SortDirection.DESC)
    )
}

internal fun normalizeTagName(name: String): String {
    return name
        .trim()
        .lowercase(Locale.US)
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" ")
}

internal fun normalizeSubscriptionRouteType(rawType: String): String {
    return when (rawType.trim().lowercase(Locale.US)) {
        "artist" -> "artist"
        "group" -> "group"
        "tag", "tags" -> "tag"
        "language" -> "language"
        "character" -> "character"
        "parody" -> "parody"
        "category" -> "category"
        else -> ""
    }
}

internal fun normalizeSubscriptionRouteName(routeType: String, rawName: String): String {
    val normalizedType = normalizeSubscriptionRouteType(routeType)
    if (normalizedType.isBlank()) return ""
    return when (normalizedType) {
        "artist", "group" -> parseCreatorSlug(rawName).ifBlank { rawName.trim() }
        else -> parseCreatorSlug(rawName).ifBlank { rawName.trim() }
    }.trim().replace(Regex("\\s+"), " ")
}

internal fun subscriptionRouteKey(routeType: String, routeName: String): String {
    val normalizedType = normalizeSubscriptionRouteType(routeType)
    val normalizedName = normalizeSubscriptionRouteName(normalizedType, routeName)
    return if (normalizedType.isBlank() || normalizedName.isBlank()) {
        ""
    } else {
        "$normalizedType|${normalizedName.lowercase(Locale.US)}"
    }
}

internal fun subscriptionRouteDisplayLabel(routeType: String, routeName: String): String {
    val cleanType = normalizeSubscriptionRouteType(routeType)
    val cleanName = normalizeSubscriptionRouteName(cleanType, routeName)
    val labelType = cleanType.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
    }
    return if (labelType.isBlank() || cleanName.isBlank()) cleanName else "$labelType: $cleanName"
}

internal fun buildSubscriptionRouteUrl(routeType: String, routeName: String, page: Int = 1): String {
    val normalizedType = normalizeSubscriptionRouteType(routeType)
    val normalizedName = normalizeSubscriptionRouteName(normalizedType, routeName)
    if (normalizedType.isBlank() || normalizedName.isBlank()) return ""
    val slug = if (normalizedType == "artist" || normalizedType == "group") {
        toCreatorUrlSlug(normalizedName)
    } else {
        parseCreatorSlug(normalizedName)
            .replace(Regex("\\s+"), "-")
            .lowercase(Locale.US)
    }
    if (slug.isBlank()) return ""
    val safePage = page.coerceAtLeast(1)
    val encodedSlug = Uri.encode(slug)
    return if (safePage <= 1) {
        "https://nhentai.net/$normalizedType/$encodedSlug/"
    } else {
        "https://nhentai.net/$normalizedType/$encodedSlug/?page=$safePage"
    }
}

internal fun buildNhentaiTagSearchQuery(
    includeTagNames: List<String>,
    excludeTagNames: List<String> = emptyList()
): String {
    fun encodeTerm(rawName: String, excluded: Boolean): String? {
        val normalized = rawName.trim().replace(Regex("\\s+"), " ")
        if (normalized.isBlank()) return null
        val words = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return null

        val prefix = if (excluded) "-" else ""
        return if (words.size == 1) {
            prefix + Uri.encode(words.first())
        } else {
            val joined = words.joinToString("+") { Uri.encode(it) }
            "$prefix\"$joined\""
        }
    }

    val includeTerms = includeTagNames
        .asSequence()
        .mapNotNull { encodeTerm(it, excluded = false) }
        .toList()
    val excludeTerms = excludeTagNames
        .asSequence()
        .mapNotNull { encodeTerm(it, excluded = true) }
        .toList()

    return (includeTerms + excludeTerms).joinToString("+")
}

internal fun extractSearchEverythingBrowserTerms(raw: String): List<String> {
    val input = raw.trim()
    if (input.isBlank()) return emptyList()

    val tokens = mutableListOf<String>()
    val pattern = Regex("\"([^\"]+)\"|(\\S+)")
    pattern.findAll(input).forEach { match ->
        val phrase = match.groupValues.getOrNull(1).orEmpty()
        val token = match.groupValues.getOrNull(2).orEmpty()
        val resolved = (if (phrase.isNotBlank()) phrase else token)
            .trim()
            .replace(Regex("\\s+"), " ")
        if (resolved.isNotBlank()) {
            tokens += resolved
        }
    }
    return tokens
}

internal fun utcNowString(): String {
    return LocalDateTime.now(ZoneOffset.UTC).format(UTC_TIMESTAMP_FORMAT)
}

internal fun formatStoredUtcTimestampForDisplay(raw: String?): String {
    val value = raw?.trim().orEmpty()
    if (value.isBlank()) return "-"
    val parsed = runCatching { LocalDateTime.parse(value, UTC_TIMESTAMP_FORMAT) }.getOrNull()
        ?: return value
    return parsed
        .atZone(ZoneOffset.UTC)
        .withZoneSameInstant(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(UTC_TIMESTAMP_FORMAT)
}

internal fun formatStoredUtcDateForDisplay(raw: String?): String {
    val value = raw?.trim().orEmpty()
    if (value.isBlank()) return "-"
    val parsed = runCatching { LocalDateTime.parse(value, UTC_TIMESTAMP_FORMAT) }.getOrNull()
        ?: return value.take(10)
    return parsed
        .atZone(ZoneOffset.UTC)
        .withZoneSameInstant(ZoneId.systemDefault())
        .toLocalDate()
        .toString()
}

internal fun csvEscape(value: Any?): String {
    val raw = value?.toString().orEmpty()
    val escaped = raw.replace("\"", "\"\"")
    return "\"$escaped\""
}

internal fun parseMediaId(raw: Any?): Long {
    val parsed = when (raw) {
        is Number -> raw.toLong()
        is String -> raw.trim().toLongOrNull() ?: 0L
        else -> 0L
    }
    return parsed.coerceAtLeast(0L)
}

internal fun parseCoverExtension(raw: String?): String {
    return when (raw?.trim()?.lowercase(Locale.US).orEmpty()) {
        "j", "jpg", "jpeg" -> "jpg"
        "p", "png" -> "png"
        "w", "webp" -> "webp"
        "g", "gif" -> "gif"
        else -> ""
    }
}

internal fun buildThumbnailUrl(mediaId: Long, coverExt: String): String {
    if (mediaId <= 0L) return ""
    val ext = parseCoverExtension(coverExt).ifBlank { "jpg" }
    return "https://t.nhentai.net/galleries/$mediaId/cover.$ext"
}

internal fun parseCode(raw: String): Int? {
    val input = raw.trim()
    if (input.isBlank()) return null

    val galleryMatch = GALLERY_LINK_PATTERN.find(input)
    val linkedCode = galleryMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
    if (linkedCode != null && linkedCode > 0) {
        return linkedCode
    }

    var cleaned = input.removePrefix("#").trim()
    while (
        cleaned.isNotEmpty() &&
        (cleaned.last() == '/' || URL_TRAILING_PUNCT.contains(cleaned.last()))
    ) {
        cleaned = cleaned.dropLast(1).trimEnd()
    }

    if (cleaned.isBlank() || cleaned.any { !it.isDigit() }) return null
    val code = cleaned.toIntOrNull() ?: return null
    return code.takeIf { it > 0 }
}
