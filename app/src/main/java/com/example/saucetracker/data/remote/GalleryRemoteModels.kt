package com.example.saucetracker.data.remote

data class GalleryTag(val name: String, val type: String)

data class GalleryData(
    val code: Int,
    val title: String,
    val subtitle: String,
    val numPages: Int,
    val uploadDate: String,
    val sourceUrl: String,
    val mediaId: Long,
    val coverExt: String,
    val tags: List<GalleryTag>
)

