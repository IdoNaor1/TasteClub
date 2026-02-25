package com.tasteclub.app.ui.restaurant

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasteclub.app.data.model.Restaurant
import com.tasteclub.app.data.repository.RestaurantRepository
import kotlinx.coroutines.launch

class RestaurantViewModel(
    private val restaurantRepository: RestaurantRepository
) : ViewModel() {

    private val _restaurant = MutableLiveData<Restaurant?>()
    val restaurant: LiveData<Restaurant?> = _restaurant

    /**
     * Observe a restaurant reactively from local Room cache through repository.
     */
    fun observeRestaurant(id: String): LiveData<Restaurant?> {
        return restaurantRepository.observeRestaurantById(id)
    }

    /**
     * Loads a restaurant by id and posts the result to [restaurant].
     */
    fun loadRestaurant(id: String) {
        if (id.isBlank()) return

        viewModelScope.launch {
            try {
                val r = restaurantRepository.getRestaurantById(id)
                _restaurant.postValue(r)
            } catch (e: Exception) {
                // On error, post null and allow UI to show fallback
                _restaurant.postValue(null)
            }
        }
    }
}
