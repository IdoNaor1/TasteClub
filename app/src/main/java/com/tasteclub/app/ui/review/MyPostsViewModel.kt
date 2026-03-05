package com.tasteclub.app.ui.review

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasteclub.app.data.model.Review
import com.tasteclub.app.data.repository.AuthRepository
import com.tasteclub.app.data.repository.ReviewRepository
import kotlinx.coroutines.launch

/**
 * MyPostsViewModel - Manages the My Posts screen state and business logic
 *
 * Implements MVVM pattern with:
 * - LiveData for reactive UI updates
 * - Sealed class for state management
 * - Delete functionality with loading states
 * - Proper error handling
 */
class MyPostsViewModel(
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // --------------------
    // State Management
    // --------------------

    /**
     * Sealed class representing all possible states of the My Posts screen
     */
    sealed class MyPostsState {
        object Loading : MyPostsState()
        data class Success(val reviews: List<Review>) : MyPostsState()
        data class Error(val message: String) : MyPostsState()
        object Empty : MyPostsState()
        data class Deleting(val reviewId: String) : MyPostsState()
        object DeleteSuccess : MyPostsState()
    }

    // Private mutable state
    private val _myPostsState = MutableLiveData<MyPostsState>(MyPostsState.Loading)
    // Public immutable state for UI
    val myPostsState: LiveData<MyPostsState> = _myPostsState

    // Track currently deleting review
    private var deletingReviewId: String? = null

    // Cache the current user ID
    val currentUserId: String
        get() = authRepository.currentUserId() ?: ""

    // --------------------
    // Core Functions
    // --------------------

    /**
     * Toggle like on a review for the current user.
     */
    fun toggleLike(reviewId: String) {
        val userId = currentUserId
        if (userId.isBlank()) return

        viewModelScope.launch {
            try {
                reviewRepository.toggleLike(reviewId, userId)
            } catch (e: Exception) {
                Log.e("MyPostsViewModel", "toggleLike failed for review=$reviewId user=$userId", e)
            }
        }
    }

    /**
     * Fetch the current user's reviews from the repository.
     * Automatically observes changes from Room database.
     */
    fun getMyReviews() {
        val userId = currentUserId
        if (userId.isBlank()) {
            _myPostsState.value = MyPostsState.Error("User not authenticated")
            return
        }

        _myPostsState.value = MyPostsState.Loading

        viewModelScope.launch {
            try {
                // First, refresh from Firestore to get latest data
                reviewRepository.refreshUserReviewsPage(
                    userId = userId,
                    limit = 50 // Load all user's reviews
                )

                // Then observe from Room (cached data)
                reviewRepository.observeReviewsByUser(userId).observeForever { reviews ->
                    if (reviews.isEmpty()) {
                        _myPostsState.value = MyPostsState.Empty
                    } else {
                        _myPostsState.value = MyPostsState.Success(reviews)
                    }
                }
            } catch (e: Exception) {
                _myPostsState.value = MyPostsState.Error(
                    e.message ?: "Failed to load reviews. Check your connection."
                )
            }
        }
    }

    /**
     * Delete a review by its ID.
     * Handles:
     * - Deleting from Firestore
     * - Deleting associated image from Firebase Storage
     * - Deleting from Room cache
     * - Updating UI state during and after deletion
     */
    fun deleteReview(reviewId: String) {
        deletingReviewId = reviewId
        _myPostsState.value = MyPostsState.Deleting(reviewId)

        viewModelScope.launch {
            try {
                // Delete from Firestore, Storage, and Room
                reviewRepository.deleteReview(reviewId)

                // Notify success
                deletingReviewId = null
                _myPostsState.value = MyPostsState.DeleteSuccess

                // Refresh the list after a short delay
                kotlinx.coroutines.delay(500) // Give time for Toast to show
                getMyReviews()
            } catch (e: Exception) {
                deletingReviewId = null
                _myPostsState.value = MyPostsState.Error(
                    e.message ?: "Failed to delete review. Please try again."
                )
            }
        }
    }

    /**
     * Check if a specific review is currently being deleted
     */
    fun isDeletingReview(reviewId: String): Boolean {
        return deletingReviewId == reviewId
    }

    /**
     * Reset state to idle after showing error or success
     */
    fun resetState() {
        if (currentUserId.isNotBlank()) {
            getMyReviews()
        }
    }

    // --------------------
    // Initialization
    // --------------------

    init {
        getMyReviews()
    }
}

