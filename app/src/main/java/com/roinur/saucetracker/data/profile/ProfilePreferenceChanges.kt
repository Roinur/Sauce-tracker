package com.roinur.saucetracker.data.profile

internal data class ProfilePreferenceChanges(
    val removed: Set<String>,
    val upserts: Map<String, String>
)

internal fun profilePreferenceChanges(previous: Map<String, String>, incoming: Map<String, String>) =
    ProfilePreferenceChanges(previous.keys - incoming.keys, incoming.filter { (key, value) -> previous[key] != value })
