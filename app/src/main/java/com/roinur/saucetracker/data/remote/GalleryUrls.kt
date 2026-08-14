package com.roinur.saucetracker.data.remote

internal object GalleryUrls {
    private const val BASE = "https://nhentai.net"

    fun api(code: Int): String = "$BASE/api/gallery/$code"
    fun relatedV2(code: Int): String = "$BASE/api/v2/galleries/$code/related"
    fun relatedLegacy(code: Int): String = "$BASE/api/gallery/$code/related"
    fun comments(code: Int): String = "$BASE/api/v2/galleries/$code/comments"
    fun gallery(code: Int): String = "$BASE/g/$code/"
    fun popularTags(page: Int): String = "$BASE/tags/popular?page=$page"
}
