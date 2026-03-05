package com.tasteclub.app.ui.comment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasteclub.app.data.model.Comment
import com.tasteclub.app.data.repository.CommentRepository
import kotlinx.coroutines.launch

class CommentsViewModel(
    private val commentRepository: CommentRepository,
    val currentUserId: String,
    val currentUserName: String,
    val currentUserImageUrl: String
) : ViewModel() {

    sealed class State {
        object Idle : State()
        object Loading : State()
        data class Success(val comments: List<Comment>) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableLiveData<State>(State.Idle)
    val state: LiveData<State> = _state

    fun loadComments(reviewId: String) {
        _state.value = State.Loading
        viewModelScope.launch {
            try {
                val comments = commentRepository.getComments(reviewId)
                _state.value = State.Success(comments)
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Failed to load comments")
            }
        }
    }

    fun postComment(reviewId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                val comment = Comment(
                    reviewId = reviewId,
                    userId = currentUserId,
                    userName = currentUserName,
                    userImageUrl = currentUserImageUrl,
                    text = text.trim()
                )
                val saved = commentRepository.addComment(comment)
                // Append to current list optimistically
                val current = (_state.value as? State.Success)?.comments.orEmpty()
                _state.value = State.Success(current + saved)
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Failed to post comment")
            }
        }
    }

    fun deleteComment(reviewId: String, comment: Comment) {
        viewModelScope.launch {
            try {
                commentRepository.deleteComment(reviewId, comment.id)
                val current = (_state.value as? State.Success)?.comments.orEmpty()
                _state.value = State.Success(current.filter { it.id != comment.id })
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Failed to delete comment")
            }
        }
    }
}

