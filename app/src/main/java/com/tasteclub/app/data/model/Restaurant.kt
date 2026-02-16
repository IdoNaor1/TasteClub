package com.tasteclub.app.data.model

import com.google.android.libraries.places.api.model.AddressComponent
import com.google.android.libraries.places.api.model.AddressComponents

data class Restaurant(
    val id: String = "",
    val name: String = "",
    val addressComponents: AddressComponents? = null,
    val lat: Double = 0.0,
    val lng: Double = 0.0,

    // Optional / nice-to-have
    val photoUrl: String = "",
    val categories: List<String> = emptyList(),

    // Meta
    val createdAt: Long = 0L,
    val lastUpdated: Long = 0L
)