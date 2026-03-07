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

    fun refresh() {
        loadAllData()
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

        val filteredR = if (q.isEmpty()) {
            allRestaurants
        } else {
            allRestaurants.filter { restaurant ->
                restaurant.name.lowercase().contains(q) ||
                        restaurant.address.lowercase().contains(q) ||
                        restaurant.primaryType.lowercase().contains(q)
            }
        }

        val filteredU = if (q.isEmpty()) {
            allUsers
        } else {
            allUsers.filter { user ->
                user.userName.lowercase().contains(q) ||
                        user.email.lowercase().contains(q)
            }
        }

        val filteredRev = if (q.isEmpty()) {
            allReviews
        } else {
            allReviews.filter { review ->
                review.text.lowercase().contains(q) ||
                        review.restaurantName.lowercase().contains(q) ||
                        review.userName.lowercase().contains(q)
            }
        }

        _filteredRestaurants.value = filteredR
        _filteredUsers.value = filteredU
        _filteredReviews.value = filteredRev

        _restaurantCount.value = filteredR.size
        _userCount.value = filteredU.size
        _reviewCount.value = filteredRev.size
    }
}


