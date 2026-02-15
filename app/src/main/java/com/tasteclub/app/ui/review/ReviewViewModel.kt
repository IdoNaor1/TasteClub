package com.tasteclub.app.ui.review

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasteclub.app.data.model.Restaurant
import com.tasteclub.app.data.model.Review
import com.tasteclub.app.data.repository.ReviewRepository
import kotlinx.coroutines.launch

class ReviewViewModel(
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    // Form data
    private val _selectedRestaurant = MutableLiveData<Restaurant?>()
    val selectedRestaurant: LiveData<Restaurant?> = _selectedRestaurant

    private val _rating = MutableLiveData<Float>(0f)
    val rating: LiveData<Float> = _rating

    private val _reviewText = MutableLiveData<String>("")
    val reviewText: LiveData<String> = _reviewText

    // State
    private val _isCreating = MutableLiveData<Boolean>(false)
    val isCreating: LiveData<Boolean> = _isCreating

    private val _createResult = MutableLiveData<Result<Review>?>()
    val createResult: LiveData<Result<Review>?> = _createResult

    fun setSelectedRestaurant(restaurant: Restaurant) {
        _selectedRestaurant.value = restaurant
    }

    fun setRating(rating: Float) {
        _rating.value = rating
    }

    fun setReviewText(text: String) {
        _reviewText.value = text
    }

    fun createReview() {
        val restaurant = _selectedRestaurant.value
        val rating = _rating.value ?: 0f
        val text = _reviewText.value ?: ""

        if (restaurant == null || rating == 0f || text.isBlank()) {
            _createResult.value = Result.failure(Exception("Please fill all fields"))
            return
        }

        _isCreating.value = true
        viewModelScope.launch {
            try {
                val review = Review(
                    restaurantId = restaurant.id,
                    restaurantName = restaurant.name,
                    restaurantAddress = restaurant.address,
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
