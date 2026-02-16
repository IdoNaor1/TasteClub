package com.tasteclub.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.libraries.places.api.model.AddressComponents

@Entity(tableName = "restaurants")
data class RestaurantEntity(
    @PrimaryKey val id: String,
    val name: String,
    val addressComponents: AddressComponents?,
    val lat: Double,
    val lng: Double,
    val photoUrl: String,
    val categories: String, // Comma-separated for simplicity
    val createdAt: Long,
    val lastUpdated: Long
)
