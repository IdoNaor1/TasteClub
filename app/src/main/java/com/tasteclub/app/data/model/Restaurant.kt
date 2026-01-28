package com.tasteclub.app.data.model

data class Restaurant(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val city: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,

    // Optional / nice-to-have
    val photoUrl: String = "",
    val categories: List<String> = emptyList(),

    // Meta
    val createdAt: Long = 0L,
    val lastUpdated: Long = 0L
)