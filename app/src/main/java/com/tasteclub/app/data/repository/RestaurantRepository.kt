package com.tasteclub.app.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.tasteclub.app.data.local.dao.RestaurantDao
import com.tasteclub.app.data.local.entity.RestaurantEntity
import com.tasteclub.app.data.local.entity.toDomain
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
            address = restaurant.address,
            lat = restaurant.lat,
            lng = restaurant.lng,
            photoUrl = restaurant.photoUrl,
            primaryType = restaurant.primaryType,
            averageRating = restaurant.averageRating,
            numReviews = restaurant.numReviews,
            createdAt = restaurant.createdAt,
            lastUpdated = restaurant.lastUpdated
        )
        restaurantDao.upsert(entity)

        return restaurant
    }

    /**
     * Observe a restaurant in the local Room cache. This is reactive and will update when Room changes.
     */
    fun observeRestaurantById(id: String): LiveData<Restaurant?> {
        return restaurantDao.observeById(id).map { entity ->
            entity?.toDomain()
        }
    }

    /**
     * Get a restaurant by ID from local Room (for offline access).
     * If not found locally, fetch from Firestore and cache it in Room.
     */
    suspend fun getRestaurantById(id: String): Restaurant? {
        // Try local cache first
        val entity = restaurantDao.getById(id)
        if (entity != null) {
            return Restaurant(
                id = entity.id,
                name = entity.name,
                addressComponents = entity.addressComponents,
                address = entity.address,
                lat = entity.lat,
                lng = entity.lng,
                photoUrl = entity.photoUrl,
                primaryType = entity.primaryType,
                averageRating = entity.averageRating,
                numReviews = entity.numReviews,
                createdAt = entity.createdAt,
                lastUpdated = entity.lastUpdated
            )
        }

        // Not found locally -> fetch from Firestore
        val remote = firestoreSource.getRestaurant(id) ?: return null

        // Cache fetched restaurant in Room
        val cached = RestaurantEntity(
            id = remote.id,
            name = remote.name,
            addressComponents = remote.addressComponents,
            address = remote.address,
            lat = remote.lat,
            lng = remote.lng,
            photoUrl = remote.photoUrl,
            primaryType = remote.primaryType,
            averageRating = remote.averageRating,
            numReviews = remote.numReviews,
            createdAt = remote.createdAt,
            lastUpdated = remote.lastUpdated
        )
        restaurantDao.upsert(cached)

        return remote
    }



    /**
     * Delete a restaurant from both Firestore and Room.
     */
    suspend fun deleteRestaurant(id: String) {
        firestoreSource.deleteRestaurant(id)
        restaurantDao.deleteById(id)
    }
}