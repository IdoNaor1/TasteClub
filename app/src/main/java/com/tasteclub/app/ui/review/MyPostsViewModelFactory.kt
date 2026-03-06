package com.tasteclub.app.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tasteclub.app.data.repository.AuthRepository
import com.tasteclub.app.data.repository.CommentRepository
import com.tasteclub.app.data.repository.ReviewRepository

/**
 * Factory for creating MyPostsViewModel with required dependencies
 */
class MyPostsViewModelFactory(
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository,
    private val commentRepository: CommentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyPostsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MyPostsViewModel(reviewRepository, authRepository, commentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
