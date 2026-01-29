package com.tasteclub.app.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tasteclub.app.data.local.entity.ReviewEntity

@Dao
interface ReviewDao {

    // --------------------
    // Feed (latest first)
    // --------------------
    @Query("""
        SELECT * FROM reviews
        ORDER BY createdAt DESC
        LIMIT :limit OFFSET :offset
    """)
    fun getFeedPage(limit: Int, offset: Int): LiveData<List<ReviewEntity>>

    // Optional: first page shortcut
    @Query("""
        SELECT * FROM reviews
        ORDER BY createdAt DESC
        LIMIT :limit
    """)
    fun getLatest(limit: Int): LiveData<List<ReviewEntity>>

    // --------------------
    // By user
    // --------------------
    @Query("""
        SELECT * FROM reviews
        WHERE userId = :userId
        ORDER BY createdAt DESC
        LIMIT :limit OFFSET :offset
    """)
    fun getByUserPage(userId: String, limit: Int, offset: Int): LiveData<List<ReviewEntity>>

    // --------------------
    // By restaurant
    // --------------------
    @Query("""
        SELECT * FROM reviews
        WHERE restaurantId = :restaurantId
        ORDER BY createdAt DESC
        LIMIT :limit OFFSET :offset
    """)
    fun getByRestaurantPage(restaurantId: String, limit: Int, offset: Int): LiveData<List<ReviewEntity>>

    // --------------------
    // Write
    // --------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(review: ReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(reviews: List<ReviewEntity>)

    // --------------------
    // Delete
    // --------------------
    @Query("DELETE FROM reviews WHERE id = :reviewId")
    suspend fun deleteById(reviewId: String)

    @Query("DELETE FROM reviews")
    suspend fun deleteAll()

    // --------------------
    // Utilities (optional)
    // --------------------
    @Query("SELECT COUNT(*) FROM reviews")
    suspend fun count(): Int
}

