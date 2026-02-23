package com.tasteclub.app.data.local.entity

import com.tasteclub.app.data.model.Review
import com.tasteclub.app.data.model.Restaurant
import com.tasteclub.app.data.model.User

// -------- User --------
fun UserEntity.toDomain(): User = User(
    uid = uid,
    email = email,
    userName = userName,
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
    userName = userName,
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
    userName = userName,
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
    userName = userName,
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

// -------- Restaurant --------
fun RestaurantEntity.toDomain(): Restaurant = Restaurant(
    id = id,
    name = name,
    addressComponents = addressComponents,
    address = address,
    lat = lat,
    lng = lng,
    photoUrl = photoUrl,
    primaryType = primaryType,
    averageRating = averageRating,
    numReviews = numReviews,
    createdAt = createdAt,
    lastUpdated = lastUpdated
)

fun Restaurant.toEntity(): RestaurantEntity = RestaurantEntity(
    id = id,
    name = name,
    addressComponents = addressComponents,
    address = address,
    lat = lat,
    lng = lng,
    photoUrl = photoUrl,
    primaryType = primaryType,
    averageRating = averageRating,
    numReviews = numReviews,
    createdAt = createdAt,
    lastUpdated = lastUpdated
)
