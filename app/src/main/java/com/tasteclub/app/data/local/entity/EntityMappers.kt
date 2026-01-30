package com.tasteclub.app.data.local.entity

import com.tasteclub.app.data.model.Review
import com.tasteclub.app.data.model.User

// -------- User --------
fun UserEntity.toDomain(): User = User(
    uid = uid,
    email = email,
    displayName = displayName,
    profileImageUrl = profileImageUrl,
    bio = bio,
    followersCount = followersCount,
    followingCount = followingCount,
    createdAt = createdAt,
    lastUpdated = lastUpdated
)

fun User.toEntity(): UserEntity = UserEntity(
    uid = uid,
    email = email,
    displayName = displayName,
    profileImageUrl = profileImageUrl,
    bio = bio,
    followersCount = followersCount,
    followingCount = followingCount,
    createdAt = createdAt,
    lastUpdated = lastUpdated
)

// -------- Review --------
fun ReviewEntity.toDomain(): Review = Review(
    id = id,
    userId = userId,
    userDisplayName = userDisplayName,
    userProfileImageUrl = userProfileImageUrl,
    restaurantId = restaurantId,
    restaurantName = restaurantName,
    restaurantAddress = restaurantAddress,
    rating = rating,
    text = text,
    imageUrl = imageUrl,
    createdAt = createdAt,
    lastUpdated = lastUpdated
)

fun Review.toEntity(): ReviewEntity = ReviewEntity(
    id = id,
    userId = userId,
    userDisplayName = userDisplayName,
    userProfileImageUrl = userProfileImageUrl,
    restaurantId = restaurantId,
    restaurantName = restaurantName,
    restaurantAddress = restaurantAddress,
    rating = rating,
    text = text,
    imageUrl = imageUrl,
    createdAt = createdAt,
    lastUpdated = lastUpdated
)
