package com.tasteclub.app.data.model

import com.google.android.libraries.places.api.model.AddressComponents
import com.google.firebase.firestore.Exclude

data class Restaurant(
    val id: String = "",
    val name: String = "",
    @get:Exclude val addressComponents: AddressComponents? = null,
    val address: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val photoUrl: String = "",
    val primaryType: String = "",

    // New aggregate fields
    val averageRating: Double = 0.0,
    val numReviews: Int = 0,

    // Meta
    val createdAt: Long = 0L,
    val lastUpdated: Long = 0L
)