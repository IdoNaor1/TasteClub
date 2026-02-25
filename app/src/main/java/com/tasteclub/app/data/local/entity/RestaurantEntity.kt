package com.tasteclub.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.libraries.places.api.model.AddressComponents

@Entity(tableName = "restaurants")
data class RestaurantEntity(
    @PrimaryKey val id: String,
    val name: String,
    val addressComponents: AddressComponents?,
    val address: String,
    val lat: Double,
    val lng: Double,
    val photoUrl: String,
    val primaryType: String, // Single primary type

    // New aggregate fields
    val averageRating: Double,
    val numReviews: Int,

    val createdAt: Long,
    val lastUpdated: Long
)
