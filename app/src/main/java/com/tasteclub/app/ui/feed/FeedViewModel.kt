package com.tasteclub.app.ui.feed

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasteclub.app.data.model.Review
import com.tasteclub.app.data.repository.AuthRepository
import com.tasteclub.app.data.repository.CommentRepository
import com.tasteclub.app.data.repository.ReviewRepository
import kotlinx.coroutines.launch

/**
 * FeedViewModel - Manages the main feed screen state and business logic
 *
 * Implements MVVM pattern with:
 * - LiveData for reactive UI updates
 * - Sealed class for state management
 * - Pagination support with page tracking
 * - Pull-to-refresh functionality
 * - Proper error handling
 */
class FeedViewModel(
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository,
    private val commentRepository: CommentRepository
) : ViewModel() {

    // --------------------
    // State Management
    // --------------------

    // Current user ID for like state
    val currentUserId: String
        get() = authRepository.currentUserId() ?: ""

    /**
     * Sealed class representing all possible states of the feed
     */
    sealed class FeedState {
        object Loading : FeedState()
        data class Success(val reviews: List<Review>) : FeedState()
        data class Error(val message: String) : FeedState()
        object Empty : FeedState()
    }

    // Private mutable state
    private val _feedState = MutableLiveData<FeedState>(FeedState.Loading)
    // Public immutable state for UI
    val feedState: LiveData<FeedState> = _feedState

    // Pagination tracking
    private val _isLoadingMore = MutableLiveData<Boolean>(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private var currentPage = 0
    private var lastCreatedAt: Long? = null
    private var hasMorePages = true
    private val pageSize = 10

    // Cache of all loaded reviews
    private val allReviews = mutableListOf<Review>()

    init {
        // Observe feed from local database for reactive updates
        observeLocalFeed()
        // Load initial data from Firestore
        loadInitialReviews()
    }

    // --------------------
    // Public Functions
    // --------------------

    /**
     * Fetch all reviews from repository - initial load
     */
    fun getAllReviews() {
        if (_feedState.value is FeedState.Loading) {
            return // Already loading
        }

        _feedState.value = FeedState.Loading
        loadInitialReviews()
    }

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
                Log.e("FeedViewModel", "toggleLike failed for review=$reviewId user=$userId", e)
            }
        }
    }

    /**
     * Patch the comment count for a single review in the feed list.
     * Called from FeedFragment when CommentsBottomSheet reports a count change.
     */
    fun updateCommentCount(reviewId: String, newCount: Int) {
        val idx = allReviews.indexOfFirst { it.id == reviewId }
        if (idx == -1) return
        allReviews[idx] = allReviews[idx].copy(commentCount = newCount)
        _feedState.value = FeedState.Success(allReviews.toList())
    }

    /**
     * Pull-to-refresh implementation
     * Clears local cache and fetches fresh data from Firestore
     */
    fun refreshFeed() {
        viewModelScope.launch {
            try {
                // Reset pagination state
                currentPage = 0
                lastCreatedAt = null
                hasMorePages = true
                allReviews.clear()

                // Fetch first page from Firestore
                val reviews = reviewRepository.refreshFeedPage(
                    limit = pageSize,
                    lastCreatedAt = null
                )

                // Update pagination tracking
                if (reviews.isNotEmpty()) {
                    lastCreatedAt = reviews.last().createdAt
                    hasMorePages = reviews.size == pageSize
                    allReviews.addAll(reviews)
                } else {
                    hasMorePages = false
                }

                // Update state based on results
                _feedState.value = when {
                    reviews.isEmpty() -> FeedState.Empty
                    else -> FeedState.Success(reviews)
                }

                if (allReviews.isNotEmpty()) {
                    fetchAndPatchCommentCounts(allReviews.map { it.id })
                }

            } catch (e: Exception) {
                _feedState.value = FeedState.Error(
                    e.message ?: "Failed to refresh feed"
                )
            }
        }
    }

    /**
     * Lazy loading/pagination - Load next page of reviews
     * Called when user scrolls near bottom
     */
    fun loadMoreReviews() {
        // Prevent duplicate loading requests
        if (_isLoadingMore.value == true || !hasMorePages) {
            return
        }

        _isLoadingMore.value = true

        viewModelScope.launch {
            try {
                // Fetch next page using cursor
                val reviews = reviewRepository.refreshFeedPage(
                    limit = pageSize,
                    lastCreatedAt = lastCreatedAt
                )

                // Update pagination state
                if (reviews.isNotEmpty()) {
                    lastCreatedAt = reviews.last().createdAt
                    hasMorePages = reviews.size == pageSize
                    currentPage++

                    // Add to cache
                    allReviews.addAll(reviews)

                    // Update UI with combined list
                    _feedState.value = FeedState.Success(allReviews.toList())

                    // Fetch counts for the newly loaded page
                    fetchAndPatchCommentCounts(reviews.map { it.id })
                } else {
                    hasMorePages = false
                }

            } catch (e: Exception) {
                _feedState.value = FeedState.Error(e.message ?: "Failed to load more reviews")
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    // --------------------
    // Private Helper Functions
    // --------------------

    /**
     * Observe local Room database for reactive updates.
     * Preserves any in-memory commentCount values already patched into allReviews
     * so a Room emission never resets counts back to 0.
     */
    private fun observeLocalFeed() {
        reviewRepository.observeFeed().observeForever { reviews ->
            if (_feedState.value !is FeedState.Loading && reviews.isNotEmpty()) {
                // Build a lookup of counts we already know about
                val knownCounts = allReviews.associate { it.id to it.commentCount }
                val merged = reviews.map { review ->
                    val known = knownCounts[review.id] ?: 0
                    if (known > 0) review.copy(commentCount = known) else review
                }
                // Keep allReviews in sync
                allReviews.clear()
                allReviews.addAll(merged)
                _feedState.value = FeedState.Success(merged)
            }
        }
    }

    /**
     * Load initial reviews from Firestore, then fetch comment counts in the background.
     */
    private fun loadInitialReviews() {
        viewModelScope.launch {
            try {
                _feedState.value = FeedState.Loading

                val reviews = reviewRepository.refreshFeedPage(
                    limit = pageSize,
                    lastCreatedAt = null
                )

                if (reviews.isNotEmpty()) {
                    lastCreatedAt = reviews.last().createdAt
                    hasMorePages = reviews.size == pageSize
                    allReviews.clear()
                    allReviews.addAll(reviews)
                } else {
                    hasMorePages = false
                }

                _feedState.value = when {
                    reviews.isEmpty() -> FeedState.Empty
                    else -> FeedState.Success(allReviews.toList())
                }

                // Fetch comment counts in the background and patch the list
                if (allReviews.isNotEmpty()) {
                    fetchAndPatchCommentCounts(allReviews.map { it.id })
                }

            } catch (e: Exception) {
                _feedState.value = FeedState.Error(e.message ?: "Failed to load reviews")
            }
        }
    }

    /**
     * Fetch comment counts for the given reviewIds and patch allReviews + state.
     */
    private suspend fun fetchAndPatchCommentCounts(reviewIds: List<String>) {
        try {
            val counts = commentRepository.getCommentCountsBatch(reviewIds)
            var changed = false
            counts.forEach { (reviewId, count) ->
                val idx = allReviews.indexOfFirst { it.id == reviewId }
                if (idx != -1 && allReviews[idx].commentCount != count) {
                    allReviews[idx] = allReviews[idx].copy(commentCount = count)
                    changed = true
                }
            }
            if (changed) {
                _feedState.value = FeedState.Success(allReviews.toList())
            }
        } catch (_: Exception) {
            // Non-fatal — counts stay at 0, will be updated when user opens comments
        }
    }

    /**
     * Clean up resources when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        // Clean up any observers or resources if needed
    }
}

