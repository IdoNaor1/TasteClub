package com.tasteclub.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reviews",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["restaurantId"]),
        Index(value = ["createdAt"])
    ]
)
data class ReviewEntity(
    @PrimaryKey val id: String,

    val userId: String,
    val userDisplayName: String,
    val userProfileImageUrl: String,

    val restaurantId: String,
    val restaurantName: String,
    val restaurantAddress: String,

    val rating: Int,
    val text: String,
    val imageUrl: String,

    val createdAt: Long,
    val lastUpdated: Long
)
