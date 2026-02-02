package com.tasteclub.app.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val userName: String = "",
    val profileImageUrl: String = "",
    val bio: String = "",

    // Social
    val following: List<String> = emptyList(),
    val followersCount: Int = 0,
    val followingCount: Int = 0,

    // Meta
    val createdAt: Long = 0L,
    val lastUpdated: Long = 0L
)