package com.tasteclub.app.data.repository

import com.tasteclub.app.data.local.dao.RestaurantDao
import com.tasteclub.app.data.local.entity.toDomain
import com.tasteclub.app.data.local.entity.toEntity
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
        val saved = firestoreSource.upsertRestaurant(restaurant)

        // Then, cache in Room
        restaurantDao.upsert(saved.toEntity())

        return saved
    }

    /**
     * Get a restaurant by ID from local Room (for offline access).
     * If not found locally, could optionally fetch from Firestore, but for now just local.
     */
    suspend fun getRestaurantById(id: String): Restaurant? {
        return restaurantDao.getById(id)?.toDomain()
    }

    /**
     * Delete a restaurant from both Firestore and Room.
     */
    suspend fun deleteRestaurant(id: String) {
        firestoreSource.deleteRestaurant(id)
        restaurantDao.deleteById(id)
    }
}