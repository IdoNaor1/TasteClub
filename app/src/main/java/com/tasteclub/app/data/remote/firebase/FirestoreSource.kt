package com.tasteclub.app.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tasteclub.app.data.model.Restaurant
import com.tasteclub.app.data.model.Review
import com.tasteclub.app.data.model.User
import kotlinx.coroutines.tasks.await

class FirestoreSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val COL_USERS = "users"
        private const val COL_REVIEWS = "reviews"
        private const val COL_RESTAURANTS = "restaurants"
    }

    // Collections
    private val usersCol get() = firestore.collection(COL_USERS)
    private val reviewsCol get() = firestore.collection(COL_REVIEWS)
    private val restaurantsCol get() = firestore.collection(COL_RESTAURANTS)

    // ----------------------------
    // Users
    // ----------------------------

    suspend fun getUser(uid: String): User? {
        val snap = usersCol.document(uid).get().await()
        return snap.toObject(User::class.java)
    }

    /**
     * Create/update the user doc at users/{uid}.
     * We keep timestamps consistent.
     */
    suspend fun upsertUser(user: User) {
        val now = nowMillis()
        val uid = user.uid
        require(uid.isNotBlank()) { "User uid must not be blank" }

        val existing = usersCol.document(uid).get().await()
        val createdAt = if (existing.exists()) {
            user.createdAt.takeIf { it > 0 } ?: existing.getLong("createdAt") ?: now
        } else {
            now
        }

        val updated = user.copy(
            createdAt = createdAt,
            lastUpdated = now
        )

        usersCol.document(uid).set(updated).await()
    }

    /**
     * Partial update for profile fields (safe, doesn't overwrite whole document).
     */
    suspend fun updateUserProfile(
        uid: String,
        userName: String? = null,
        bio: String? = null,
        profileImageUrl: String? = null
    ) {
        require(uid.isNotBlank()) { "uid must not be blank" }

        val updates = mutableMapOf<String, Any>()
        userName?.let { updates["userName"] = it }
        bio?.let { updates["bio"] = it }
        profileImageUrl?.let { updates["profileImageUrl"] = it }
        updates["lastUpdated"] = nowMillis()

        if (updates.size == 1) return // only lastUpdated was added? (no real updates)

        usersCol.document(uid).update(updates).await()
    }

    suspend fun deleteUser(uid: String) {
        require(uid.isNotBlank()) { "uid must not be blank" }
        usersCol.document(uid).delete().await()
    }

    // ----------------------------
    // Reviews
    // ----------------------------

    suspend fun getReview(reviewId: String): Review? {
        val snap = reviewsCol.document(reviewId).get().await()
        return snap.toObject(Review::class.java)
    }

    /**
     * Create or update a review. If review.id is blank -> generates a new id.
     * Timestamps:
     * - createdAt stays once set
     * - lastUpdated updates every save
     */
    suspend fun upsertReview(review: Review): Review {
        val now = nowMillis()

        val docId = if (review.id.isNotBlank()) review.id else reviewsCol.document().id
        val docRef = reviewsCol.document(docId)

        val existing = docRef.get().await()
        val createdAt = if (existing.exists()) {
            review.createdAt.takeIf { it > 0 } ?: existing.getLong("createdAt") ?: now
        } else {
            now
        }

        val updated = review.copy(
            id = docId,
            createdAt = createdAt,
            lastUpdated = now
        )

        docRef.set(updated).await()
        return updated
    }

    suspend fun deleteReview(reviewId: String) {
        require(reviewId.isNotBlank()) { "reviewId must not be blank" }
        reviewsCol.document(reviewId).delete().await()
    }

    /**
     * Feed pagination with cursor:
     * - First page: pass lastCreatedAt = null
     * - Next pages: pass the last item createdAt from previous page
     */
    suspend fun getFeedPage(
        limit: Int,
        lastCreatedAt: Long? = null
    ): List<Review> {
        require(limit > 0) { "limit must be > 0" }

        var q: Query = reviewsCol
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())

        // Cursor: if lastCreatedAt exists, start AFTER it
        if (lastCreatedAt != null) {
            q = q.startAfter(lastCreatedAt)
        }

        val snap = q.get().await()
        return snap.toObjects(Review::class.java)
    }

    /**
     * Reviews by user with cursor.
     * Note: whereEqualTo + orderBy usually requires a composite index in Firestore console.
     */
    suspend fun getReviewsByUserPage(
        userId: String,
        limit: Int,
        lastCreatedAt: Long? = null
    ): List<Review> {
        require(userId.isNotBlank()) { "userId must not be blank" }
        require(limit > 0) { "limit must be > 0" }

        var q: Query = reviewsCol
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())

        if (lastCreatedAt != null) {
            q = q.startAfter(lastCreatedAt)
        }

        val snap = q.get().await()
        return snap.toObjects(Review::class.java)
    }

    /**
     * Reviews by restaurant with cursor.
     * Note: may require composite index.
     */
    suspend fun getReviewsByRestaurantPage(
        restaurantId: String,
        limit: Int,
        lastCreatedAt: Long? = null
    ): List<Review> {
        require(restaurantId.isNotBlank()) { "restaurantId must not be blank" }
        require(limit > 0) { "limit must be > 0" }

        var q: Query = reviewsCol
            .whereEqualTo("restaurantId", restaurantId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())

        if (lastCreatedAt != null) {
            q = q.startAfter(lastCreatedAt)
        }

        val snap = q.get().await()
        return snap.toObjects(Review::class.java)
    }

    // ----------------------------
    // Restaurants
    // ----------------------------

    suspend fun getRestaurant(id: String): Restaurant? {
        val snap = restaurantsCol.document(id).get().await()
        return snap.toObject(Restaurant::class.java)
    }

    /**
     * For restaurants, you might store minimal cached info (id, name, address...).
     * Google Places will still be your "source" for search/details.
     */
    suspend fun upsertRestaurant(restaurant: Restaurant) {
        val now = nowMillis()
        val id = restaurant.id
        require(id.isNotBlank()) { "Restaurant id must not be blank" }

        val existing = restaurantsCol.document(id).get().await()
        val createdAt = if (existing.exists()) {
            restaurant.createdAt.takeIf { it > 0 } ?: existing.getLong("createdAt") ?: now
        } else {
            now
        }

        val updated = restaurant.copy(
            createdAt = createdAt,
            lastUpdated = now
        )

        restaurantsCol.document(id).set(updated).await()
    }

    suspend fun deleteRestaurant(id: String) {
        require(id.isNotBlank()) { "id must not be blank" }
        restaurantsCol.document(id).delete().await()
    }

    // ----------------------------
    // Helpers
    // ----------------------------

    private fun nowMillis(): Long = System.currentTimeMillis()
}


