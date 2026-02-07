package com.tasteclub.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tasteclub.app.data.repository.AuthRepository
import com.tasteclub.app.data.repository.ReviewRepository

class ProfileViewModelFactory(
    private val authRepository: AuthRepository,
    private val reviewRepository: ReviewRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(authRepository, reviewRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

