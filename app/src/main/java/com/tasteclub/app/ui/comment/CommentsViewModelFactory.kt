package com.tasteclub.app.ui.comment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tasteclub.app.data.repository.CommentRepository

class CommentsViewModelFactory(
    private val commentRepository: CommentRepository,
    private val currentUserId: String,
    private val currentUserName: String,
    private val currentUserImageUrl: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CommentsViewModel::class.java)) {
            return CommentsViewModel(
                commentRepository,
                currentUserId,
                currentUserName,
                currentUserImageUrl
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

