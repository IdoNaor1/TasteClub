package com.tasteclub.app.ui.review

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasteclub.app.data.model.Restaurant
import com.tasteclub.app.data.model.Review
import com.tasteclub.app.data.repository.ReviewRepository
import com.tasteclub.app.data.repository.RestaurantRepository
import com.tasteclub.app.data.remote.places.PlacesService
import kotlinx.coroutines.launch

class ReviewViewModel(
    private val reviewRepository: ReviewRepository,
    private val restaurantRepository: RestaurantRepository,
    private val placesService: PlacesService
) : ViewModel() {

    // Form data
    private val _selectedRestaurantId = MutableLiveData<String?>()
    val selectedRestaurantId: LiveData<String?> = _selectedRestaurantId

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

        if (restaurantId == null || rating == 0f || text.isBlank()) {
            _createResult.value = Result.failure(Exception("Please fill all fields"))
            return
        }

        _isCreating.value = true
        viewModelScope.launch {
            try {
                var finalRestaurant = restaurantRepository.getRestaurantById(restaurantId)
                if (finalRestaurant == null) {
                    // Fetch full place details
                    val place = placesService.getPlaceDetails(restaurantId)
                    if (place != null) {
                        finalRestaurant = Restaurant(
                            id = restaurantId,
                            name = place.displayName?: "",
                            address = place.formattedAddress ?: "",
                            photoUrl = place.photoMetadatas?.firstOrNull()?.photoReference ?: "",
                            categories = listOf(place.primaryTypeDisplayName ?: "")
                        )
                        restaurantRepository.upsertRestaurant(finalRestaurant)
                    }
                }
                val review = Review(
                    restaurantId = finalRestaurant?.id ?: "",
                    restaurantName = finalRestaurant?.name ?: "",
                    restaurantAddress = finalRestaurant?.address ?: "",
                    rating = rating.toInt(),
                    text = text
                )
                val created = reviewRepository.upsertReview(review)
                _createResult.value = Result.success(created)
            } catch (e: Exception) {
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
