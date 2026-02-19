package com.tasteclub.app.ui.review

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasteclub.app.data.model.Restaurant
import com.tasteclub.app.data.model.Review
import com.tasteclub.app.data.repository.ReviewRepository
import com.tasteclub.app.data.repository.RestaurantRepository
import com.tasteclub.app.data.repository.AuthRepository
import com.tasteclub.app.data.remote.places.PlacesService
import kotlinx.coroutines.launch

class ReviewViewModel(
    private val reviewRepository: ReviewRepository,
    private val restaurantRepository: RestaurantRepository,
    private val placesService: PlacesService,
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ReviewViewModel"
    }

    // Form data
    private val _selectedRestaurantId = MutableLiveData<String?>()
    val selectedRestaurantId: LiveData<String?> = _selectedRestaurantId

    // Human-friendly display text for the selected restaurant (name + address)
    private val _selectedRestaurantDisplay = MutableLiveData<String?>()
    val selectedRestaurantDisplay: LiveData<String?> = _selectedRestaurantDisplay

    private val _rating = MutableLiveData<Float>(0f)
    val rating: LiveData<Float> = _rating

    private val _reviewText = MutableLiveData<String>("")
    val reviewText: LiveData<String> = _reviewText

    // State
    private val _isCreating = MutableLiveData<Boolean>(false)
    val isCreating: LiveData<Boolean> = _isCreating

    private val _createResult = MutableLiveData<Result<Review>?>()
    val createResult: LiveData<Result<Review>?> = _createResult

    fun setSelectedRestaurantId(restaurantId: String) {
        _selectedRestaurantId.value = restaurantId
    }

    // New API: set both id and a display string (e.g., "Name, Address")
    fun setSelectedRestaurant(restaurantId: String, displayText: String?) {
        _selectedRestaurantId.value = restaurantId
        _selectedRestaurantDisplay.value = displayText
    }

    fun setRating(rating: Float) {
        _rating.value = rating
    }

    fun setReviewText(text: String) {
        _reviewText.value = text
    }

    fun createReview() {
        val restaurantId = _selectedRestaurantId.value
        val rating = _rating.value ?: 0f
        val text = _reviewText.value ?: ""

        Log.d(TAG, "createReview called. restaurantId=$restaurantId, rating=$rating, text='${text.take(50)}'")

        val missing = mutableListOf<String>()
        if (restaurantId.isNullOrBlank()) missing.add("restaurantId")
        if (rating == 0f) missing.add("rating")
        if (text.isBlank()) missing.add("text")

        if (missing.isNotEmpty()) {
            val message = "Missing inputs: ${missing.joinToString(", ")}"
            Log.w(TAG, message)
            _createResult.value = Result.failure(Exception(message))
            return
        }

        // At this point restaurantId is guaranteed non-null/non-blank
        val rid = restaurantId!!

        _isCreating.value = true
        viewModelScope.launch {
            try {
                // Ensure we have a logged-in user and their profile
                val uid = authRepository.currentUserId()
                Log.d(TAG, "current user id: $uid")
                if (uid.isNullOrBlank()) {
                    val msg = "User not authenticated"
                    Log.w(TAG, msg)
                    _createResult.value = Result.failure(Exception(msg))
                    return@launch
                }

                val user = try {
                    authRepository.refreshUserFromRemote(uid).also { Log.d(TAG, "refreshed user: $it") }
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing user from remote: ${e.message}", e)
                    null
                }

                Log.d(TAG, "Looking up restaurant in local cache: $rid")
                var finalRestaurant = restaurantRepository.getRestaurantById(rid)
                Log.d(TAG, "local restaurant: $finalRestaurant")
                if (finalRestaurant == null) {
                    Log.d(TAG, "Fetching place details from Places API for id=$restaurantId")
                    // Fetch full place details
                    val place = try {
                        placesService.getPlaceDetails(rid)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching place details: ${e.message}", e)
                        null
                    }
                    Log.d(TAG, "place fetched: $place")
                    if (place != null) {
                        finalRestaurant = Restaurant(
                            id = restaurantId,
                            name = place.displayName ?: "",
                            addressComponents = place.addressComponents,
                            address = place.formattedAddress ?: "",
                            lat = place.location?.latitude ?: 0.0,
                            lng = place.location?.longitude ?: 0.0,
                            photoUrl = "",
                            primaryType = place.primaryTypeDisplayName ?: ""
                        )
                        Log.d(TAG, "Caching fetched restaurant: $finalRestaurant")
                        try {
                            restaurantRepository.upsertRestaurant(finalRestaurant)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error caching restaurant: ${e.message}", e)
                        }
                    }
                }

                // If we still don't have restaurant details, abort with a clear error
                if (finalRestaurant == null) {
                    val msg = "Failed to fetch restaurant details for id=$restaurantId"
                    Log.e(TAG, msg)
                    _createResult.value = Result.failure(Exception(msg))
                    return@launch
                }

                val review = Review(
                    userId = uid,
                    userName = user?.userName ?: "",
                    userProfileImageUrl = user?.profileImageUrl ?: "",
                    restaurantId = finalRestaurant.id,
                    restaurantName = finalRestaurant.name,
                    rating = rating.toInt(),
                    text = text
                )

                Log.d(TAG, "About to upsert review: $review")
                val created = reviewRepository.upsertReview(review)
                Log.d(TAG, "Review created: $created")
                _createResult.value = Result.success(created)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating review: ${e.message}", e)
                _createResult.value = Result.failure(e)
            } finally {
                _isCreating.value = false
            }
        }
    }

    fun resetCreateResult() {
        _createResult.value = null
    }
}
