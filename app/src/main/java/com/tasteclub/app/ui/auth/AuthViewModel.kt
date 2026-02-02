package com.tasteclub.app.ui.auth

import com.tasteclub.app.data.repository.AuthRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Patterns

/**
 * Represents the different states for the authentication UI.
 */
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String? = null) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    /**
     * Handles user login.
     * @param email The user's email.
     * @param password The user's password.
     */
    fun login(email: String, password: String) {
        if (!isValidEmail(email)) {
            _authState.value = AuthState.Error("Invalid email format")
            return
        }
        if (password.isBlank()) {
            _authState.value = AuthState.Error("Password cannot be empty")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val user = authRepository.login(email, password)
                if (user != null) {
                    _authState.value = AuthState.Success()
                } else {
                    _authState.value = AuthState.Error("Login failed: User not found")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    /**
     * Handles new user registration.
     * @param email The user's email.
     * @param password The user's password.
     * @param displayName The user's display name.
     */
    fun register(email: String, password: String, displayName: String) {
        if (!isValidEmail(email)) {
            _authState.value = AuthState.Error("Invalid email format")
            return
        }
        if (password.length < 6) { // Example rule: password must be at least 6 characters
            _authState.value = AuthState.Error("Password must be at least 6 characters long")
            return
        }
        if (displayName.isBlank()) {
            _authState.value = AuthState.Error("Display name cannot be empty")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                authRepository.register(email, password, displayName)
                _authState.value = AuthState.Success()
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    /**
     * Sends a password reset email to the user.
     * @param email The user's email.
     */
    fun sendPasswordResetEmail(email: String) {
        if (!isValidEmail(email)) {
            _authState.value = AuthState.Error("Invalid email format")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                authRepository.sendPasswordReset(email)
                _authState.value = AuthState.Success()
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    /**
     * Handles user logout.
     */
    fun logout() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                authRepository.logout()
                _authState.value = AuthState.Success("Logged out successfully")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Logout failed")
            }
        }
    }

    /**
     * Resets the authentication state to Idle.
     * Useful for clearing errors or success states when the user navigates away or dismisses a message.
     */
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    fun resetToIdle() = resetAuthState()

    /**
     * Validates the email format.
     */
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
