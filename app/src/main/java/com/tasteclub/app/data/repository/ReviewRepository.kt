package com.tasteclub.app.data.repository

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.tasteclub.app.data.local.dao.ReviewDao
import com.tasteclub.app.data.local.entity.toDomain
import com.tasteclub.app.data.local.entity.toEntity
import com.tasteclub.app.data.model.Review
import com.tasteclub.app.data.remote.firebase.FirestoreSource
import com.tasteclub.app.data.remote.firebase.FirebaseStorageSource

class ReviewRepository(
    private val firestoreSource: FirestoreSource,
    private val storageSource: FirebaseStorageSource,
    private val reviewDao: ReviewDao
) {
    // --------------------
    // Observers (UI reads from Room)
    // --------------------
    fun observeFeed(): LiveData<List<Review>> =
        reviewDao.observeFeed().map { list -> list.map { it.toDomain() } }

    fun observeReviewsByUser(userId: String): LiveData<List<Review>> =
        reviewDao.observeByUser(userId).map { list -> list.map { it.toDomain() } }

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
        val page = firestoreSource.getFeedPage(limit = limit, lastCreatedAt = lastCreatedAt)
        if (page.isNotEmpty()) {
            reviewDao.upsertAll(page.map { it.toEntity() })
        }
        return page
    }

    suspend fun refreshUserReviewsPage(
        userId: String,
        limit: Int,
        lastCreatedAt: Long? = null
    ): List<Review> {
        val page = firestoreSource.getReviewsByUserPage(
            userId = userId,
            limit = limit,
            lastCreatedAt = lastCreatedAt
        )
        if (page.isNotEmpty()) {
            reviewDao.upsertAll(page.map { it.toEntity() })
        }
        return page
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
        imageBitmap: Bitmap? = null
    ): Review {
        // Step 1: ensure review has id + timestamps
        var saved = firestoreSource.upsertReview(review)

        // Step 2-3: optional image upload + update Firestore
        if (imageBitmap != null) {
            val url = storageSource.uploadReviewImage(saved.id, imageBitmap)
            saved = firestoreSource.upsertReview(saved.copy(imageUrl = url))
        }

        // Step 4: cache locally
        reviewDao.upsert(saved.toEntity())
        return saved
    }

    suspend fun deleteReview(reviewId: String) {
        firestoreSource.deleteReview(reviewId)
        storageSource.deleteReviewImage(reviewId)
        reviewDao.deleteById(reviewId)
    }
}

