package com.tasteclub.app.data.model

data class Review(
    val id: String = "",
    val userId: String = "",

    val restaurantId: String = "",
    val restaurantName: String = "",
    val restaurantAddress: String = "",

    val rating: Int = 0,
    val text: String = "",
    val imageUrl: String = "",

    // Likes
    val likedBy: List<String> = emptyList(),

    // Comments (UI-only, not stored in Room/Firestore)
    val commentCount: Int = 0,

    // Meta
    val createdAt: Long = 0L,
    val lastUpdated: Long = 0L
)