package com.example.saucetracker.data.database.entity

data class TagEntity(
    val id: Long,
    val name: String,
    val type: String,
    val count: Int
)

