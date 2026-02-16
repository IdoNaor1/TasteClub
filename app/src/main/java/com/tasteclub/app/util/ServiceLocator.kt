package com.tasteclub.app.util

import android.content.Context
import com.tasteclub.app.data.local.TasteClubDatabase
import com.tasteclub.app.data.remote.firebase.FirebaseAuthSource
import com.tasteclub.app.data.remote.firebase.FirebaseStorageSource
import com.tasteclub.app.data.remote.firebase.FirestoreSource
import com.tasteclub.app.data.remote.places.PlacesService
import com.tasteclub.app.data.repository.AuthRepository
import com.tasteclub.app.data.repository.ReviewRepository
import com.tasteclub.app.data.repository.RestaurantRepository

object ServiceLocator {

    @Volatile
    private var database: TasteClubDatabase? = null

    @Volatile
    private var firestoreSource: FirestoreSource? = null

    @Volatile
    private var authSource: FirebaseAuthSource? = null

    @Volatile
    private var storageSource: FirebaseStorageSource? = null

    @Volatile
    private var authRepository: AuthRepository? = null

    @Volatile
    private var reviewRepository: ReviewRepository? = null

    @Volatile
    private var restaurantRepository: RestaurantRepository? = null

    private var placesService: PlacesService? = null

    // --------------------
    // Public API
    // --------------------

    fun provideAuthRepository(context: Context): AuthRepository {
        return authRepository ?: synchronized(this) {
            authRepository ?: AuthRepository(
                authSource = provideAuthSource(),
                firestoreSource = provideFirestoreSource(),
                userDao = provideDatabase(context).userDao()
            ).also { authRepository = it }
        }
    }

    fun provideReviewRepository(context: Context): ReviewRepository {
        return reviewRepository ?: synchronized(this) {
            reviewRepository ?: ReviewRepository(
                firestoreSource = provideFirestoreSource(),
                storageSource = provideStorageSource(),
                reviewDao = provideDatabase(context).reviewDao()
            ).also { reviewRepository = it }
        }
    }

    fun provideRestaurantRepository(context: Context): RestaurantRepository {
        return restaurantRepository ?: synchronized(this) {
            restaurantRepository ?: RestaurantRepository(
                firestoreSource = provideFirestoreSource(),
                restaurantDao = provideDatabase(context).restaurantDao()
            ).also { restaurantRepository = it }
        }
    }

    // --------------------
    // Internals
    // --------------------

    private fun provideDatabase(context: Context): TasteClubDatabase {
        return database ?: synchronized(this) {
            database ?: TasteClubDatabase.getInstance(context).also { database = it }
        }
    }

    private fun provideFirestoreSource(): FirestoreSource {
        return firestoreSource ?: synchronized(this) {
            firestoreSource ?: FirestoreSource().also { firestoreSource = it }
        }
    }

    private fun provideAuthSource(): FirebaseAuthSource {
        return authSource ?: synchronized(this) {
            authSource ?: FirebaseAuthSource().also { authSource = it }
        }
    }

    private fun provideStorageSource(): FirebaseStorageSource {
        return storageSource ?: synchronized(this) {
            storageSource ?: FirebaseStorageSource().also { storageSource = it }
        }
    }

    fun providePlacesService(context: Context): PlacesService {
        return placesService ?: synchronized(this) {
            val instance = PlacesService(context.applicationContext)
            placesService = instance
            instance
        }
    }

    /**
     * Optional: useful for tests or "logout and reset app state"
     */
    fun resetForTesting() {
        authRepository = null
        reviewRepository = null
        restaurantRepository = null
        authSource = null
        firestoreSource = null
        storageSource = null
        database = null
    }
}
