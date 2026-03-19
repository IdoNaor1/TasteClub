package com.tasteclub.app.data.repository

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.tasteclub.app.data.local.dao.ReviewDao
import com.tasteclub.app.data.local.dao.RestaurantDao
import com.tasteclub.app.data.local.entity.toDomain
import com.tasteclub.app.data.local.entity.toEntity
import com.tasteclub.app.data.model.Review
import com.tasteclub.app.data.remote.firebase.FirestoreSource
import com.tasteclub.app.data.remote.firebase.FirebaseStorageSource
import com.google.firebase.storage.StorageException
import com.tasteclub.app.util.NetworkMonitor
import com.tasteclub.app.util.OfflineException

class ReviewRepository(
    private val firestoreSource: FirestoreSource,
    private val storageSource: FirebaseStorageSource,
    private val reviewDao: ReviewDao,
    private val restaurantDao: RestaurantDao,
    private val networkMonitor: NetworkMonitor
) {
    // --------------------
    // Observers (UI reads from Room)
    // --------------------
    fun observeFeed(): LiveData<List<Review>> =
        reviewDao.observeFeed().map { list -> list.map { it.toDomain() } }

    fun observeReviewsByUser(userId: String): LiveData<List<Review>> =
        reviewDao.observeByUser(userId).map { list -> list.map { it.toDomain() } }

    fun observeReviewsByRestaurant(restaurantId: String): LiveData<List<Review>> =
        reviewDao.observeByRestaurant(restaurantId).map { list -> list.map { it.toDomain() } }

    /**
     * Fetch all reviews from Firestore (no pagination) and cache in Room.
     * Used by Discover screen to populate search index.
     */
    suspend fun refreshAllReviews(): List<Review> {
        return try {
            val reviews = firestoreSource.getAllReviews()
            if (reviews.isNotEmpty()) {
                reviewDao.upsertAll(reviews.map { it.toEntity() })
            }
            reviews
        } catch (_: Exception) {
            reviewDao.getAllOnce().map { it.toDomain() }
        }
    }

    // --------------------
    // Sync from Firestore -> Room
    // --------------------

    /**
     * Pull a page from Firestore and cache it in Room.
     * Return value is the fetched page (useful to compute next cursor).
     */
    suspend fun refreshFeedPage(
        limit: Int,
        lastCreatedAt: Long? = null
    ): List<Review> {
        return try {
            val page = firestoreSource.getFeedPage(limit = limit, lastCreatedAt = lastCreatedAt)
            if (page.isNotEmpty()) {
                reviewDao.upsertAll(page.map { it.toEntity() })
            }
            page
        } catch (_: Exception) {
            val cached = if (lastCreatedAt == null) {
                reviewDao.getLatestOnce(limit)
            } else {
                reviewDao.getFeedPageAfterOnce(limit, lastCreatedAt)
            }
            cached.map { it.toDomain() }
        }
    }

    /**
     * Pull a page of reviews from the accounts the current user follows.
     * On first load (lastCreatedAt == null) the feed cache is replaced so Room's
     * observeFeed() only surfaces following-filtered posts.
     * On subsequent pages the new posts are appended to the cache.
     */
    suspend fun refreshFollowingFeedPage(
        followingIds: List<String>,
        limit: Int,
        lastCreatedAt: Long? = null
    ): List<Review> {
        return try {
            val page = firestoreSource.getFollowingFeedPage(
                followingIds = followingIds,
                limit = limit,
                lastCreatedAt = lastCreatedAt
            )
            if (lastCreatedAt == null) {
                // First load — wipe old cache so Room only shows the filtered feed
                reviewDao.deleteAll()
            }
            if (page.isNotEmpty()) {
                reviewDao.upsertAll(page.map { it.toEntity() })
            }
            page
        } catch (_: Exception) {
            val cached = if (lastCreatedAt == null) {
                reviewDao.getFollowingFeedOnce(followingIds, limit)
            } else {
                reviewDao.getFollowingFeedAfterOnce(followingIds, limit, lastCreatedAt)
            }
            cached.map { it.toDomain() }
        }
    }

    suspend fun refreshUserReviewsPage(
        userId: String,
        limit: Int,
        lastCreatedAt: Long? = null
    ): List<Review> {
        return try {
            val page = firestoreSource.getReviewsByUserPage(
                userId = userId,
                limit = limit,
                lastCreatedAt = lastCreatedAt
            )
            if (page.isNotEmpty()) {
                reviewDao.upsertAll(page.map { it.toEntity() })
            }
            page
        } catch (_: Exception) {
            val cached = if (lastCreatedAt == null) {
                reviewDao.getByUserOnce(userId, limit)
            } else {
                reviewDao.getByUserAfterOnce(userId, limit, lastCreatedAt)
            }
            cached.map { it.toDomain() }
        }
    }

    suspend fun refreshRestaurantReviewsPage(
        restaurantId: String,
        limit: Int,
        lastCreatedAt: Long? = null
    ): List<Review> {
        return try {
            val page = firestoreSource.getReviewsByRestaurantPage(
                restaurantId = restaurantId,
                limit = limit,
                lastCreatedAt = lastCreatedAt
            )
            if (page.isNotEmpty()) {
                reviewDao.upsertAll(page.map { it.toEntity() })
            }
            page
        } catch (_: Exception) {
            val cached = if (lastCreatedAt == null) {
                reviewDao.getByRestaurantOnce(restaurantId, limit)
            } else {
                reviewDao.getByRestaurantAfterOnce(restaurantId, limit, lastCreatedAt)
            }
            cached.map { it.toDomain() }
        }
    }

    // --------------------
    // Create / Update review (optionally with image)
    // --------------------

    /**
     * Create/update a review.
     * If imageBitmap != null:
     * 1) upsert review in Firestore to get an id (if needed)
     * 2) upload image to Storage using that id
     * 3) update review.imageUrl in Firestore
     * 4) cache final review in Room
     */
    suspend fun upsertReview(
        review: Review,
        imageBitmap: Bitmap? = null,
        removeImage: Boolean = false
    ): Review {
        if (!networkMonitor.isOnline()) throw OfflineException()

        // Step 1: ensure review has id + timestamps
        var saved = firestoreSource.upsertReview(review)

        // Step 2-3: optional image upload + update Firestore
        if (imageBitmap != null) {
            val url = storageSource.uploadReviewImage(saved.id, imageBitmap)
            saved = firestoreSource.upsertReview(saved.copy(imageUrl = url))
        } else if (removeImage) {
            // Best-effort delete existing image in storage, then clear imageUrl in Firestore
            try {
                storageSource.deleteReviewImage(saved.id)
            } catch (_: Exception) {
                // ignore storage delete errors
            }
            saved = firestoreSource.upsertReview(saved.copy(imageUrl = ""))
        }

        // Step 4: cache locally
        reviewDao.upsert(saved.toEntity())

        // Update local restaurant cache with recomputed aggregates
        try {
            val restaurant = firestoreSource.getRestaurant(saved.restaurantId)
            if (restaurant != null) {
                restaurantDao.upsert(restaurant.toEntity())
            }
        } catch (e: Exception) {
            Log.w("ReviewRepository", "Failed to refresh restaurant cache after review upsert: ${e.message}")
        }

        return saved
    }

    // --------------------
    // Like / Unlike
    // --------------------

    suspend fun toggleLike(reviewId: String, userId: String): Review {
        if (!networkMonitor.isOnline()) throw OfflineException()
        val updated = firestoreSource.toggleLike(reviewId, userId)
        reviewDao.upsert(updated.toEntity())
        return updated
    }

    suspend fun deleteReview(reviewId: String) {
        if (!networkMonitor.isOnline()) throw OfflineException()

        // Fetch the review before deletion so we can determine restaurantId and imageUrl
        val reviewBeforeDelete: Review? = try {
            firestoreSource.getReview(reviewId)
        } catch (e: Exception) {
            Log.w("ReviewRepository", "Failed to fetch review $reviewId before delete: ${e.message}", e)
            null
        }

        val imageUrl: String? = reviewBeforeDelete?.imageUrl
        val restaurantId: String? = reviewBeforeDelete?.restaurantId

        // Delete the Firestore document
        firestoreSource.deleteReview(reviewId)

        // If we observed a non-blank image URL, attempt to delete the storage object.
        if (!imageUrl.isNullOrBlank()) {
            try {
                storageSource.deleteReviewImage(reviewId)
            } catch (e: StorageException) {
                val msg = e.message ?: ""
                val isNotFound = (e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND)
                        || msg.contains("Not Found", ignoreCase = true)
                        || msg.contains("Object does not exist", ignoreCase = true)

                if (isNotFound) {
                    Log.i("ReviewRepository", "Review image not found for $reviewId; skipping storage delete. Details: $msg")
                } else {
                    Log.w("ReviewRepository", "StorageException deleting image for $reviewId: ${e.message}", e)
                }
            } catch (e: Exception) {
                Log.w("ReviewRepository", "Failed to delete review image for $reviewId: ${e.message}", e)
            }
        } else {
            Log.i("ReviewRepository", "No image URL for review $reviewId; skipping storage delete.")
        }

        // Remove from local cache (Room) regardless of Storage result
        reviewDao.deleteById(reviewId)

        // Immediately refresh local Restaurant cache for the affected restaurant
        if (!restaurantId.isNullOrBlank()) {
            try {
                val restaurant = firestoreSource.getRestaurant(restaurantId)
                if (restaurant != null) {
                    restaurantDao.upsert(restaurant.toEntity())
                }
            } catch (e: Exception) {
                Log.w("ReviewRepository", "Failed to refresh restaurant cache after review delete: ${e.message}")
            }
        }
    }
}
