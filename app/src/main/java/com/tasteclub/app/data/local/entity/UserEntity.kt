package com.tasteclub.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val email: String,
    val displayName: String,
    val profileImageUrl: String,
    val bio: String,

    val followersCount: Int,
    val followingCount: Int,

    // timestamps (for sync / cache)
    val createdAt: Long,
    val lastUpdated: Long
)
