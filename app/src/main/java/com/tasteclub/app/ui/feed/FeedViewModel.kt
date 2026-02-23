package com.tasteclub.app.ui.feed

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
    private val authRepository: AuthRepository
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
                } else {
                    hasMorePages = false
                }

            } catch (e: Exception) {
                // Don't change state on pagination error, just show toast
                // The current list remains visible
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    // --------------------
    // Private Helper Functions
    // --------------------

    /**
     * Observe local Room database for reactive updates
     * This ensures UI updates automatically when data changes
     */
    private fun observeLocalFeed() {
        reviewRepository.observeFeed().observeForever { reviews ->
            // Only update if we're not in loading state and have data
            if (_feedState.value !is FeedState.Loading && reviews.isNotEmpty()) {
                _feedState.value = FeedState.Success(reviews)
            }
        }
    }

    /**
     * Load initial reviews from Firestore
     */
    private fun loadInitialReviews() {
        viewModelScope.launch {
            try {
                _feedState.value = FeedState.Loading

                // Fetch first page
                val reviews = reviewRepository.refreshFeedPage(
                    limit = pageSize,
                    lastCreatedAt = null
                )

                // Initialize pagination tracking
                if (reviews.isNotEmpty()) {
                    lastCreatedAt = reviews.last().createdAt
                    hasMorePages = reviews.size == pageSize
                    allReviews.clear()
                    allReviews.addAll(reviews)
                } else {
                    hasMorePages = false
                }

                // Update state
                _feedState.value = when {
                    reviews.isEmpty() -> FeedState.Empty
                    else -> FeedState.Success(reviews)
                }

            } catch (e: Exception) {
                _feedState.value = FeedState.Error(
                    e.message ?: "Failed to load reviews"
                )
            }
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

