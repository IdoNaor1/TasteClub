package com.tasteclub.app.ui.profile

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.tasteclub.app.data.model.Review
import com.tasteclub.app.data.model.User
import com.tasteclub.app.data.repository.AuthRepository
import com.tasteclub.app.data.repository.CommentRepository
import com.tasteclub.app.data.repository.ReviewRepository
import kotlinx.coroutines.launch

/**
 * OtherUserProfileViewModel – drives the public profile screen for another user.
 *
 * Responsibilities:
 * - Load and observe the target user's profile
 * - Load and observe the target user's reviews (sorted newest-first)
 * - Follow / unfollow the target user
 * - Expose follow state to the UI
 * - Expose review count, followers count, following count
 * - Toggle likes on reviews shown in the profile
 */
class OtherUserProfileViewModel(
    private val authRepository: AuthRepository,
    private val reviewRepository: ReviewRepository,
    private val commentRepository: CommentRepository,
    private val targetUserId: String
) : ViewModel() {

    companion object {
        private const val TAG = "OtherUserProfileVM"
        private const val PAGE_SIZE = 20
    }

    // --------------------
    // Current (logged-in) user
    // --------------------
    val currentUserId: String
        get() = authRepository.currentUserId() ?: ""

    // --------------------
    // Target user profile
    // --------------------
    val targetUser: LiveData<User?> = MediatorLiveData<User?>().apply {
        addSource(authRepository.observeUser(targetUserId)) { user ->
            value = user
        }
    }

    // --------------------
    // Follow state – derived from current user's "following" list
    // --------------------
    private val _currentUser: LiveData<User?> = MediatorLiveData<User?>().apply {
        val uid = authRepository.currentUserId()
        if (uid != null) {
            addSource(authRepository.observeUser(uid)) { value = it }
        }
    }

    val isFollowing: LiveData<Boolean> = _currentUser.map { user ->
        user?.following?.contains(targetUserId) == true
    }

    // --------------------
    // Reviews
    // --------------------
    private val _reviews = MutableLiveData<List<Review>>(emptyList())
    val reviews: LiveData<List<Review>> = _reviews

    val reviewCount: LiveData<Int> = MediatorLiveData<Int>().apply {
        addSource(_reviews) { value = it.size }
    }

    // --------------------
    // Loading / error states
    // --------------------
    sealed class ProfileState {
        object Loading : ProfileState()
        object Idle : ProfileState()
        data class Error(val message: String) : ProfileState()
    }

    private val _profileState = MutableLiveData<ProfileState>(ProfileState.Loading)
    val profileState: LiveData<ProfileState> = _profileState

    private val _isFollowLoading = MutableLiveData(false)
    val isFollowLoading: LiveData<Boolean> = _isFollowLoading

    // --------------------
    // Init
    // --------------------
    init {
        loadProfile()
        loadReviews()
        observeLocalReviews()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                authRepository.refreshUserFromRemote(targetUserId)
                // Also refresh current user so follow list is up-to-date
                val uid = authRepository.currentUserId()
                if (uid != null) authRepository.refreshUserFromRemote(uid)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to refresh target user profile: ${e.message}")
            }
        }
    }

    /**
     * Observe reviews from Room so the list updates reactively.
     */
    private fun observeLocalReviews() {
        reviewRepository.observeReviewsByUser(targetUserId).observeForever { localReviews ->
            val sorted = localReviews.sortedByDescending { it.createdAt }
            _reviews.postValue(sorted)
        }
    }

    private fun loadReviews() {
        viewModelScope.launch {
            try {
                _profileState.value = ProfileState.Loading
                // Fetch from Firestore and cache in Room
                reviewRepository.refreshUserReviewsPage(
                    userId = targetUserId,
                    limit = PAGE_SIZE
                )
                // Enrich with comment counts
                enrichCommentCounts()
                _profileState.value = ProfileState.Idle
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load reviews: ${e.message}")
                _profileState.value = ProfileState.Error(e.message ?: "Failed to load reviews")
            }
        }
    }

    /**
     * Fetch comment counts for every loaded review (best-effort).
     */
    private suspend fun enrichCommentCounts() {
        val current = _reviews.value ?: return
        val enriched = current.map { review ->
            try {
                val comments = commentRepository.getComments(review.id)
                review.copy(commentCount = comments.size)
            } catch (_: Exception) {
                review
            }
        }
        _reviews.postValue(enriched)
    }

    // --------------------
    // Follow / Unfollow
    // --------------------
    fun toggleFollow() {
        if (_isFollowLoading.value == true) return
        val currentlyFollowing = isFollowing.value == true

        viewModelScope.launch {
            try {
                _isFollowLoading.value = true
                if (currentlyFollowing) {
                    authRepository.unfollowUser(targetUserId)
                } else {
                    authRepository.followUser(targetUserId)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Follow/unfollow failed: ${e.message}")
            } finally {
                _isFollowLoading.value = false
            }
        }
    }

    // --------------------
    // Like
    // --------------------
    fun toggleLike(reviewId: String) {
        viewModelScope.launch {
            try {
                reviewRepository.toggleLike(reviewId, currentUserId)
            } catch (e: Exception) {
                Log.w(TAG, "Like toggle failed: ${e.message}")
            }
        }
    }

    /**
     * Update comment count for a review after comments bottom sheet changes.
     */
    fun updateCommentCount(reviewId: String, newCount: Int) {
        val current = _reviews.value ?: return
        val updated = current.map { review ->
            if (review.id == reviewId) review.copy(commentCount = newCount)
            else review
        }
        _reviews.value = updated
    }
}

