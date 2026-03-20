package com.tasteclub.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "comments",
    indices = [Index(value = ["reviewId"])]
)
data class CommentEntity(
    @PrimaryKey val id: String,
    val reviewId: String,
    val userId: String,
    val text: String,
    val createdAt: Long
)

