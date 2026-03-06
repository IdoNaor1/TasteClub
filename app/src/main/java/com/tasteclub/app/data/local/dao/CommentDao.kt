package com.tasteclub.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tasteclub.app.data.local.entity.CommentEntity

@Dao
interface CommentDao {

    /** One-shot read — used as offline fallback before Firestore responds. */
    @Query("SELECT * FROM comments WHERE reviewId = :reviewId ORDER BY createdAt ASC")
    suspend fun getCommentsForReviewOnce(reviewId: String): List<CommentEntity>

    /** Returns the cached comment count for a single review. */
    @Query("SELECT COUNT(*) FROM comments WHERE reviewId = :reviewId")
    suspend fun getCommentCount(reviewId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(comments: List<CommentEntity>)

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteById(commentId: String)
}

