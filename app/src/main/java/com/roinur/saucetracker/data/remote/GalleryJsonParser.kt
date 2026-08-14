package com.roinur.saucetracker.data.remote

import com.roinur.saucetracker.UPLOAD_DATE_FORMAT
import com.roinur.saucetracker.parseCoverExtension
import com.roinur.saucetracker.parseMediaId
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

internal object GalleryJsonParser {
    fun parse(code: Int, json: JSONObject, embedded: Boolean = false): GalleryData {
        val titleObject = json.optJSONObject("title") ?: JSONObject()
        val title = listOf(
            titleObject.optString("english", "").trim(),
            titleObject.optString("japanese", "").trim(),
            titleObject.optString("pretty", "").trim()
        ).firstOrNull { it.isNotBlank() } ?: "Gallery $code"
        val subtitle = titleObject.optString("pretty", "").trim()
            .takeUnless { embedded && it.equals(title, ignoreCase = true) }
            .orEmpty()
        val uploadTimestamp = json.optLong("upload_date", 0L)
        val uploadDate = if (uploadTimestamp > 0L) {
            Instant.ofEpochSecond(uploadTimestamp)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .format(UPLOAD_DATE_FORMAT)
        } else {
            ""
        }

        val tags = buildList {
            val rawTags = json.optJSONArray("tags") ?: JSONArray()
            for (index in 0 until rawTags.length()) {
                val rawTag = rawTags.optJSONObject(index) ?: continue
                val name = rawTag.optString("name", "").trim()
                val type = rawTag.optString("type", "tag")
                    .trim()
                    .lowercase(Locale.US)
                    .ifBlank { "tag" }
                if (name.isNotBlank()) add(GalleryTag(name, type))
            }
        }

        val mediaId = parseMediaId(json.opt("media_id"))
        val coverExt = parseCoverExtension(
            json.optJSONObject("images")
                ?.optJSONObject("cover")
                ?.optString("t", "")
                ?: json.optJSONObject("cover")
                    ?.optString("path", "")
                    ?.substringAfterLast('.', "")
        )

        return GalleryData(
            code = code,
            title = title,
            subtitle = subtitle,
            numPages = json.optInt("num_pages", 0).coerceAtLeast(0),
            uploadDate = uploadDate,
            sourceUrl = GalleryUrls.gallery(code),
            mediaId = mediaId,
            coverExt = coverExt,
            tags = tags
        )
    }
}
