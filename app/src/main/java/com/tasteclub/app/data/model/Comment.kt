package com.tasteclub.app.data.model

data class Comment(
    val id: String = "",
    val reviewId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userImageUrl: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

