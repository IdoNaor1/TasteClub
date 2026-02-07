package com.tasteclub.app.ui.profile

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasteclub.app.data.model.User
import com.tasteclub.app.data.repository.AuthRepository
import com.tasteclub.app.data.repository.ReviewRepository
import kotlinx.coroutines.launch

/**
 * ProfileViewModel - Manages user profile screen state and business logic
 *
 * Responsibilities:
 * - Load and observe current user data
 * - Update profile information (name, email)
 * - Upload and update profile picture
 * - Count user's reviews
 * - Handle logout
 */
class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    // --------------------
    // State Management
    // --------------------

    /**
     * Sealed class representing all possible states for profile operations
     */
    sealed class ProfileState {
        object Idle : ProfileState()
        object Loading : ProfileState()
        data class Success(val user: User) : ProfileState()
        data class Error(val message: String) : ProfileState()
        data class Uploading(val progress: Int) : ProfileState()
        object UploadSuccess : ProfileState()
    }

    private val _profileState = MutableLiveData<ProfileState>(ProfileState.Idle)
    val profileState: LiveData<ProfileState> = _profileState

    // --------------------
    // User Data
    // --------------------

    /**
     * Current user data - observed from Room cache
     */
    val currentUser: LiveData<User?> = MediatorLiveData<User?>().apply {
        val userId = authRepository.currentUserId()
        if (userId != null) {
            addSource(authRepository.observeUser(userId)) { user ->
                value = user
            }
        }
    }

    /**
     * Count of user's reviews
     */
    private val _reviewCount = MutableLiveData<Int>(0)
    val reviewCount: LiveData<Int> = _reviewCount

    // --------------------
    // Initialization
    // --------------------

    init {
        loadUserData()
        loadReviewCount()
    }

    /**
     * Load current user data from repository
     */
    private fun loadUserData() {
        viewModelScope.launch {
            try {
                val userId = authRepository.currentUserId()
                if (userId != null) {
                    _profileState.value = ProfileState.Loading
                    authRepository.refreshUserFromRemote(userId)
                    // Success will be emitted via currentUser LiveData
                } else {
                    _profileState.value = ProfileState.Error("User not logged in")
                }
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to load user data")
            }
        }
    }

    /**
     * Load count of user's reviews
     */
    private fun loadReviewCount() {
        viewModelScope.launch {
            try {
                val userId = authRepository.currentUserId()
                if (userId != null) {
                    reviewRepository.observeReviewsByUser(userId).observeForever { reviews ->
                        _reviewCount.value = reviews.size
                    }
                }
            } catch (e: Exception) {
                // Silently fail for review count
                _reviewCount.value = 0
            }
        }
    }

    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? = authRepository.currentUserId()

    // --------------------
    // Update Profile
    // --------------------

    /**
     * Update user profile (name and email)
     *
     * @param name New user name
     * @param email New email address
     */
    fun updateProfile(name: String, email: String) {
        viewModelScope.launch {
            try {
                // Validate inputs
                if (name.isBlank()) {
                    _profileState.value = ProfileState.Error("Name cannot be empty")
                    return@launch
                }

                if (name.length < 2) {
                    _profileState.value = ProfileState.Error("Name must be at least 2 characters")
                    return@launch
                }

                if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    _profileState.value = ProfileState.Error("Invalid email format")
                    return@launch
                }

                _profileState.value = ProfileState.Loading

                val userId = authRepository.currentUserId()
                if (userId == null) {
                    _profileState.value = ProfileState.Error("User not logged in")
                    return@launch
                }

                // Update profile in Firestore and cache
                authRepository.updateProfile(
                    uid = userId,
                    userName = name,
                    bio = null, // Not changing bio in this update
                    profileImageUrl = null // Not changing profile image in this update
                )

                // Update email in Firebase Auth if changed
                val currentUser = currentUser.value
                if (currentUser != null && currentUser.email != email) {
                    // Note: This may require re-authentication in production
                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.updateEmail(email)
                }

                // Refresh user data
                authRepository.refreshUserFromRemote(userId)

                val user = authRepository.observeUser(userId).value
                _profileState.value = ProfileState.Success(user ?: User())
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to update profile")
            }
        }
    }

    /**
     * Upload and update profile picture
     *
     * @param bitmap Image bitmap to upload
     */
    fun updateProfilePicture(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                val userId = authRepository.currentUserId()
                if (userId == null) {
                    _profileState.value = ProfileState.Error("User not logged in")
                    return@launch
                }

                android.util.Log.d("ProfileViewModel", "Starting upload for userId: $userId")
                _profileState.value = ProfileState.Uploading(0)

                // Upload to Firebase Storage
                val storageSource = com.tasteclub.app.data.remote.firebase.FirebaseStorageSource()
                android.util.Log.d("ProfileViewModel", "Uploading to path: profile_images/$userId.jpg")
                val imageUrl = storageSource.uploadProfileImage(userId, bitmap)

                android.util.Log.d("ProfileViewModel", "Upload successful, URL: $imageUrl")
                _profileState.value = ProfileState.Uploading(50)

                // Update Firestore user document
                authRepository.updateProfile(
                    uid = userId,
                    userName = null, // Not changing name
                    bio = null, // Not changing bio
                    profileImageUrl = imageUrl
                )

                _profileState.value = ProfileState.Uploading(100)

                // Refresh user data
                authRepository.refreshUserFromRemote(userId)

                _profileState.value = ProfileState.UploadSuccess
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Upload failed", e)
                val errorMsg = when {
                    e.message?.contains("403") == true -> "Permission denied. Check Firebase Storage rules."
                    e.message?.contains("401") == true -> "Not authenticated. Please log in again."
                    e.message?.contains("404") == true -> "Storage path not found. Check Firebase Storage configuration."
                    else -> "Upload failed: ${e.message}"
                }
                _profileState.value = ProfileState.Error(errorMsg)
            }
        }
    }

    // --------------------
    // Logout
    // --------------------

    /**
     * Sign out current user and clear local cache
     */
    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()

                // Clear Room cache
                // Note: We'll handle this via ServiceLocator reset if needed
                _profileState.value = ProfileState.Idle
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to logout")
            }
        }
    }

    /**
     * Reset state to idle
     */
    fun resetState() {
        _profileState.value = ProfileState.Idle
    }
}

