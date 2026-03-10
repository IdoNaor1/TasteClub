package com.tasteclub.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tasteclub.app.data.repository.AuthRepository
import com.tasteclub.app.data.repository.CommentRepository
import com.tasteclub.app.data.repository.ReviewRepository

class OtherUserProfileViewModelFactory(
    private val authRepository: AuthRepository,
    private val reviewRepository: ReviewRepository,
    private val commentRepository: CommentRepository,
    private val targetUserId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OtherUserProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OtherUserProfileViewModel(
                authRepository,
                reviewRepository,
                commentRepository,
                targetUserId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

