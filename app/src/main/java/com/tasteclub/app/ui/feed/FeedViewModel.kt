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
 * FeedViewModel - Manages the main feed screen state and business logic.
 *
 * The feed only shows posts from users the current user follows.
 * When the following list is empty a dedicated NoFollowing state is emitted
 * so the UI can prompt the user to find people on the Discover screen.
 */
class FeedViewModel(
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository,
    private val commentRepository: CommentRepository
) : ViewModel() {

    // --------------------
    // State Management
    // --------------------

    val currentUserId: String
        get() = authRepository.currentUserId() ?: ""

    sealed class FeedState {
        object Loading : FeedState()
        data class Success(val reviews: List<Review>) : FeedState()
        data class Error(val message: String) : FeedState()
        object Empty : FeedState()
        /** Current user follows nobody yet — prompt them to discover people. */
        object NoFollowing : FeedState()
    }

    private val _feedState = MutableLiveData<FeedState>(FeedState.Loading)
    val feedState: LiveData<FeedState> = _feedState

    private val _isLoadingMore = MutableLiveData<Boolean>(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    // Pagination
    private var lastCreatedAt: Long? = null
    private var hasMorePages = true
    private val pageSize = 10

    // In-memory cache of the current feed page
    private val allReviews = mutableListOf<Review>()

    // The following list resolved at load/refresh time — stable for the lifetime of a load cycle
    private var followingIds: List<String> = emptyList()

    init {
        observeLocalFeed()
        loadInitialReviews()
    }

    // --------------------
    // Public API
    // --------------------

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

    fun updateCommentCount(reviewId: String, newCount: Int) {
        val idx = allReviews.indexOfFirst { it.id == reviewId }
        if (idx == -1) return
        allReviews[idx] = allReviews[idx].copy(commentCount = newCount)
        _feedState.value = FeedState.Success(allReviews.toList())
    }

    /** Pull-to-refresh: re-resolves the following list and fetches a fresh first page. */
    fun refreshFeed() {
        viewModelScope.launch {
            try {
                resetPagination()

                // Always refresh the user profile first so the following list is up-to-date
                val uid = authRepository.currentUserId()
                if (uid != null) authRepository.refreshUserFromRemote(uid)

                followingIds = authRepository.getFollowingListOnce()

                if (followingIds.isEmpty()) {
                    _feedState.value = FeedState.NoFollowing
                    return@launch
                }

                val reviews = reviewRepository.refreshFollowingFeedPage(
                    followingIds = followingIds,
                    limit = pageSize,
                    lastCreatedAt = null
                )

                updatePaginationState(reviews)
                _feedState.value = if (reviews.isEmpty()) FeedState.Empty
                                   else FeedState.Success(allReviews.toList())

                if (allReviews.isNotEmpty()) fetchAndPatchCommentCounts(allReviews.map { it.id })

            } catch (e: Exception) {
                _feedState.value = FeedState.Error(e.message ?: "Failed to refresh feed")
            }
        }
    }

    /** Pagination — load next page using the cursor from the previous page. */
    fun loadMoreReviews() {
        if (_isLoadingMore.value == true || !hasMorePages || followingIds.isEmpty()) return

        _isLoadingMore.value = true
        viewModelScope.launch {
            try {
                val reviews = reviewRepository.refreshFollowingFeedPage(
                    followingIds = followingIds,
                    limit = pageSize,
                    lastCreatedAt = lastCreatedAt
                )

                if (reviews.isNotEmpty()) {
                    lastCreatedAt = reviews.last().createdAt
                    hasMorePages = reviews.size == pageSize
                    allReviews.addAll(reviews)
                    _feedState.value = FeedState.Success(allReviews.toList())
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
    // Private helpers
    // --------------------

    /**
     * Observe Room so any local write (like toggle, new post) is reflected reactively.
     * We skip Room updates while Loading or NoFollowing to avoid clobbering those states.
     */
    private fun observeLocalFeed() {
        reviewRepository.observeFeed().observeForever { reviews ->
            val current = _feedState.value
            if (current is FeedState.Loading || current is FeedState.NoFollowing) return@observeForever
            if (reviews.isEmpty()) return@observeForever

            val knownCounts = allReviews.associate { it.id to it.commentCount }
            val merged = reviews.map { review ->
                val known = knownCounts[review.id] ?: 0
                if (known > 0) review.copy(commentCount = known) else review
            }
            allReviews.clear()
            allReviews.addAll(merged)
            _feedState.value = FeedState.Success(merged)
        }
    }

    private fun loadInitialReviews() {
        viewModelScope.launch {
            try {
                _feedState.value = FeedState.Loading
                resetPagination()

                // Ensure the local user cache is fresh so the following list is accurate
                val uid = authRepository.currentUserId()
                if (uid != null) authRepository.refreshUserFromRemote(uid)

                followingIds = authRepository.getFollowingListOnce()

                if (followingIds.isEmpty()) {
                    _feedState.value = FeedState.NoFollowing
                    return@launch
                }

                val reviews = reviewRepository.refreshFollowingFeedPage(
                    followingIds = followingIds,
                    limit = pageSize,
                    lastCreatedAt = null
                )

                updatePaginationState(reviews)
                _feedState.value = if (reviews.isEmpty()) FeedState.Empty
                                   else FeedState.Success(allReviews.toList())

                if (allReviews.isNotEmpty()) fetchAndPatchCommentCounts(allReviews.map { it.id })

            } catch (e: Exception) {
                _feedState.value = FeedState.Error(e.message ?: "Failed to load reviews")
            }
        }
    }

    private fun resetPagination() {
        lastCreatedAt = null
        hasMorePages = true
        allReviews.clear()
    }

    private fun updatePaginationState(reviews: List<Review>) {
        if (reviews.isNotEmpty()) {
            lastCreatedAt = reviews.last().createdAt
            hasMorePages = reviews.size == pageSize
            allReviews.addAll(reviews)
        } else {
            hasMorePages = false
        }
    }

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
            if (changed) _feedState.value = FeedState.Success(allReviews.toList())
        } catch (_: Exception) {
            // Non-fatal — counts stay at 0
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}

