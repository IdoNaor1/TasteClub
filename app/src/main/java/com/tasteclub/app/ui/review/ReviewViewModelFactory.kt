package com.tasteclub.app.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tasteclub.app.data.repository.ReviewRepository
import com.tasteclub.app.data.remote.places.PlacesService

/**
 * ViewModelFactory for ReviewViewModel
 * Handles dependency injection using ServiceLocator pattern
 */
class ReviewViewModelFactory(
    private val reviewRepository: ReviewRepository,
    private val placesService: PlacesService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReviewViewModel::class.java)) {
            return ReviewViewModel(reviewRepository, placesService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
