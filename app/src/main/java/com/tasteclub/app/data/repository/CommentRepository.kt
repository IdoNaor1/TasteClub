package com.tasteclub.app.data.repository

import com.tasteclub.app.data.local.dao.CommentDao
import com.tasteclub.app.data.local.entity.toDomain
import com.tasteclub.app.data.local.entity.toEntity
import com.tasteclub.app.data.model.Comment
import com.tasteclub.app.data.remote.firebase.FirestoreSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class CommentRepository(
    private val firestoreSource: FirestoreSource,
    private val commentDao: CommentDao
) {

    /**
     * Fetch comments for a review.
     * Fetches from Firestore, updates the cache, and returns fresh data.
     * Falls back to Room on network error.
     */
    suspend fun getComments(reviewId: String): List<Comment> {
        return try {
            val remote = firestoreSource.getComments(reviewId)
            commentDao.insertAll(remote.map { it.toEntity() })
            remote
        } catch (e: Exception) {
            commentDao.getCommentsForReviewOnce(reviewId).map { it.toDomain() }
        }
    }

    /**
     * Fetch comment counts for a batch of reviews in parallel.
     * Tries Firestore first (to get an accurate live count and warm the cache),
     * then falls back to the Room count if Firestore fails for that review.
     *
     * Returns a map of reviewId -> count.
     */
    suspend fun getCommentCountsBatch(reviewIds: List<String>): Map<String, Int> =
        coroutineScope {
            reviewIds.map { reviewId ->
                async {
                    val count = try {
                        val comments = firestoreSource.getComments(reviewId)
                        commentDao.insertAll(comments.map { it.toEntity() })
                        comments.size
                    } catch (e: Exception) {
                        commentDao.getCommentCount(reviewId)
                    }
                    reviewId to count
                }
            }.awaitAll().toMap()
        }

    suspend fun addComment(comment: Comment): Comment {
        val saved = firestoreSource.addComment(comment)
        commentDao.insert(saved.toEntity())
        return saved
    }

    suspend fun deleteComment(reviewId: String, commentId: String) {
        firestoreSource.deleteComment(reviewId, commentId)
        commentDao.deleteById(commentId)
    }
}

