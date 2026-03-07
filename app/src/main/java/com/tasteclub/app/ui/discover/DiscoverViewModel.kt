package com.tasteclub.app.ui.discover

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasteclub.app.data.model.Restaurant
import com.tasteclub.app.data.model.Review
import com.tasteclub.app.data.model.User
import com.tasteclub.app.data.repository.AuthRepository
import com.tasteclub.app.data.repository.RestaurantRepository
import com.tasteclub.app.data.repository.ReviewRepository
import kotlinx.coroutines.launch

/**
 * DiscoverViewModel - Manages state for the Discover/search screen.
 *
 * Loads all restaurants, users, and reviews then filters locally
 * based on the search query. Exposes filtered results and counts
 * per category for the tab badges.
 */
class DiscoverViewModel(
    private val reviewRepository: ReviewRepository,
    private val restaurantRepository: RestaurantRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "DiscoverViewModel"
    }

    // ---- Current user ----
    val currentUserId: String
        get() = authRepository.currentUserId() ?: ""

    // ---- Tab enum ----
    enum class Tab { ALL, RESTAURANTS, USERS, REVIEWS }

    // ---- Raw data caches ----
    private var allRestaurants: List<Restaurant> = emptyList()
    private var allUsers: List<User> = emptyList()
    private var allReviews: List<Review> = emptyList()

    // ---- Search query ----
    private val _query = MutableLiveData("")
    val query: LiveData<String> = _query

    // ---- Selected tab ----
    private val _selectedTab = MutableLiveData(Tab.ALL)
    val selectedTab: LiveData<Tab> = _selectedTab

    // ---- Filtered results ----
    private val _filteredRestaurants = MutableLiveData<List<Restaurant>>(emptyList())
    val filteredRestaurants: LiveData<List<Restaurant>> = _filteredRestaurants

    private val _filteredUsers = MutableLiveData<List<User>>(emptyList())
    val filteredUsers: LiveData<List<User>> = _filteredUsers

    private val _filteredReviews = MutableLiveData<List<Review>>(emptyList())
    val filteredReviews: LiveData<List<Review>> = _filteredReviews

    // ---- Counts ----
    private val _restaurantCount = MutableLiveData(0)
    val restaurantCount: LiveData<Int> = _restaurantCount

    private val _userCount = MutableLiveData(0)
    val userCount: LiveData<Int> = _userCount

    private val _reviewCount = MutableLiveData(0)
    val reviewCount: LiveData<Int> = _reviewCount

    // ---- Loading / error ----
    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _hasData = MutableLiveData(false)
    val hasData: LiveData<Boolean> = _hasData

    init {
        loadAllData()
    }

    // ---- Public API ----

    fun setQuery(newQuery: String) {
        _query.value = newQuery
        applyFilter()
    }

    fun clearQuery() {
        _query.value = ""
        applyFilter()
    }

    fun selectTab(tab: Tab) {
        _selectedTab.value = tab
    }

    /**
     * Toggle like on a review for the current user.
     */
    fun toggleLike(reviewId: String) {
        val userId = currentUserId
        if (userId.isBlank()) return

        viewModelScope.launch {
            try {
                val updated = reviewRepository.toggleLike(reviewId, userId)
                // Update both the raw cache and re-filter
                allReviews = allReviews.map { if (it.id == reviewId) updated else it }
                applyFilter()
            } catch (e: Exception) {
                Log.e(TAG, "toggleLike failed for review=$reviewId", e)
            }
        }
    }

    /**
     * Update the comment count for a review after the comments bottom sheet changes it.
     */
    fun updateCommentCount(reviewId: String, newCount: Int) {
        allReviews = allReviews.map {
            if (it.id == reviewId) it.copy(commentCount = newCount) else it
        }
        applyFilter()
    }

    fun refresh() {
        loadAllData()
    }

    /**
     * Called when the search button is pressed.
     * Re-fetches fresh data from Firestore, then re-runs the
     * local filter with the current query.
     */
    fun onSearchClick() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                allRestaurants = restaurantRepository.refreshAllRestaurants()
                allUsers = authRepository.refreshAllUsers()
                allReviews = reviewRepository.refreshAllReviews()

                _hasData.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh data on search click", e)
                _hasData.value = allRestaurants.isNotEmpty() ||
                        allUsers.isNotEmpty() ||
                        allReviews.isNotEmpty()
            } finally {
                _isLoading.value = false
                applyFilter()
            }
        }
    }

    // ---- Private ----

    private fun loadAllData() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Fetch all data from Firestore in parallel-ish (sequential but fast)
                allRestaurants = restaurantRepository.refreshAllRestaurants()
                allUsers = authRepository.refreshAllUsers()
                allReviews = reviewRepository.refreshAllReviews()

                _hasData.value = true
                applyFilter()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load discover data", e)
                _hasData.value = allRestaurants.isNotEmpty() ||
                        allUsers.isNotEmpty() ||
                        allReviews.isNotEmpty()
                applyFilter()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun applyFilter() {
        val q = (_query.value ?: "").trim().lowercase()
        val queryWords = q.split("\\s+".toRegex()).filter { it.isNotEmpty() }

        val filteredR = if (queryWords.isEmpty()) {
            allRestaurants
        } else {
            allRestaurants.filter { restaurant ->
                matchesQuery(restaurant.name, queryWords) ||
                        matchesQuery(restaurant.address, queryWords) ||
                        matchesQuery(restaurant.primaryType, queryWords)
            }
        }

        val filteredU = if (queryWords.isEmpty()) {
            allUsers
        } else {
            allUsers.filter { user ->
                matchesQuery(user.userName, queryWords) ||
                        matchesQuery(user.email, queryWords)
            }
        }

        val filteredRev = if (queryWords.isEmpty()) {
            allReviews
        } else {
            allReviews.filter { review ->
                matchesQuery(review.text, queryWords) ||
                        matchesQuery(review.restaurantName, queryWords) ||
                        matchesQuery(review.userName, queryWords)
            }
        }

        _filteredRestaurants.value = filteredR
        _filteredUsers.value = filteredU
        _filteredReviews.value = filteredRev

        _restaurantCount.value = filteredR.size
        _userCount.value = filteredU.size
        _reviewCount.value = filteredRev.size
    }

    /**
     * Checks if [text] matches the search [queryWords] using word-boundary matching.
     * Each query word must match the start of at least one word in the text.
     * For example, query "the" matches "The Diner" but NOT "three".
     */
    private fun matchesQuery(text: String, queryWords: List<String>): Boolean {
        val textWords = text.lowercase().split("\\s+".toRegex())
        return queryWords.all { qWord ->
            textWords.any { tWord -> tWord.startsWith(qWord) }
        }
    }
}


