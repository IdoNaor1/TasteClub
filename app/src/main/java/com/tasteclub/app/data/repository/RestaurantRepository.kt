package com.tasteclub.app.data.repository

import com.tasteclub.app.data.local.dao.RestaurantDao
import com.tasteclub.app.data.local.entity.RestaurantEntity
import com.tasteclub.app.data.model.Restaurant
import com.tasteclub.app.data.remote.firebase.FirestoreSource

class RestaurantRepository(
    private val firestoreSource: FirestoreSource,
    private val restaurantDao: RestaurantDao
) {

    /**
     * Save a restaurant to both Firestore and local Room database.
     */
    suspend fun upsertRestaurant(restaurant: Restaurant): Restaurant {
        // First, save to Firestore
        firestoreSource.upsertRestaurant(restaurant)

        // Then, cache in Room - explicit mapping to avoid extension resolution issues
        val entity = RestaurantEntity(
            id = restaurant.id,
            name = restaurant.name,
            addressComponents = restaurant.addressComponents,
            lat = restaurant.lat,
            lng = restaurant.lng,
            photoUrl = restaurant.photoUrl,
            categories = restaurant.categories.joinToString(","),
            createdAt = restaurant.createdAt,
            lastUpdated = restaurant.lastUpdated
        )
        restaurantDao.upsert(entity)

        return restaurant
    }

    /**
     * Get a restaurant by ID from local Room (for offline access).
     * If not found locally, could optionally fetch from Firestore, but for now just local.
     */
    suspend fun getRestaurantById(id: String): Restaurant? {
        val entity = restaurantDao.getById(id) ?: return null
        return Restaurant(
            id = entity.id,
            name = entity.name,
            addressComponents = entity.addressComponents,
            lat = entity.lat,
            lng = entity.lng,
            photoUrl = entity.photoUrl,
            categories = if (entity.categories.isBlank()) emptyList() else entity.categories.split(",").filter { it.isNotBlank() },
            createdAt = entity.createdAt,
            lastUpdated = entity.lastUpdated
        )
    }

    /**
     * Delete a restaurant from both Firestore and Room.
     */
    suspend fun deleteRestaurant(id: String) {
        firestoreSource.deleteRestaurant(id)
        restaurantDao.deleteById(id)
    }
}