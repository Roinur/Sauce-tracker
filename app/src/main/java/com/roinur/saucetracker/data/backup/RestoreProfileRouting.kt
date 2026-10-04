package com.roinur.saucetracker.data.backup

import com.roinur.saucetracker.data.profile.ProfileStore

/** Pre-2.0 exports always belong to the NHentai default profile, never the active MangaDex profile. */
internal object RestoreProfileRouting {
    fun targetProfileId(hasSourcePlatform: Boolean, selectedProfileId: String?, activeProfileId: String): String =
        if (hasSourcePlatform) selectedProfileId ?: activeProfileId else ProfileStore.MAIN_PROFILE_ID
}
