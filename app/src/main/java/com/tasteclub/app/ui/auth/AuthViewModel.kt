package com.tasteclub.app.ui.auth

import com.tasteclub.app.data.repository.AuthRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Patterns
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import android.util.Log

/**
 * Represents the different states for the authentication UI.
 */
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * One-time navigation events.
 */
sealed class AuthNavigationEvent {
    object NavigateToMain : AuthNavigationEvent()
}

/**
 * ViewModel responsible for handling authentication-related operations such as login, registration,
 * password reset, and logout. It manages the UI state and emits navigation events for successful
 * authentication flows.
 *
 * @property authState A [StateFlow] emitting the current authentication state (e.g., Idle, Loading, Success, Error).
 * @property authNavigationEvent A [SharedFlow] emitting one-time navigation events (e.g., NavigateToMain).
 */
class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    /**
     * Private mutable state flow for authentication state.
     */
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)

    /**
     * Public immutable state flow exposing the current authentication state.
     */
    val authState: StateFlow<AuthState> = _authState

    /**
     * Private mutable shared flow for navigation events.
     */
    private val _authNavigationEvent = MutableSharedFlow<AuthNavigationEvent>()

    /**
     * Public immutable shared flow exposing one-time navigation events.
     */
    val authNavigationEvent: SharedFlow<AuthNavigationEvent> = _authNavigationEvent

    /**
     * Handles user login by validating inputs, performing authentication via the repository,
     * and updating the state or emitting navigation events accordingly.
     *
     * @param email The user's email address for login.
     * @param password The user's password for login.
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
                    _authState.value = AuthState.Success("Login successful")
                    // Navigate to main app
                    _authNavigationEvent.emit(AuthNavigationEvent.NavigateToMain)
                } else {
                    _authState.value = AuthState.Error("Login failed: User not found")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    /**
     * Handles new user registration by validating inputs, creating a new account via the repository,
     * and updating the state or emitting navigation events on success or failure.
     *
     * @param email The user's email address for registration.
     * @param password The user's password for registration (must be at least 6 characters).
     * @param userName The user's display name for the profile.
     */
    fun register(email: String, password: String, userName: String) {
        android.util.Log.d("AuthViewModel", "Register function called")
        if (!isValidEmail(email)) {
            _authState.value = AuthState.Error("Invalid email format")
            return
        }
        if (password.length < 6) { // Example rule: password must be at least 6 characters
            _authState.value = AuthState.Error("Password must be at least 6 characters long")
            return
        }
        if (userName.isBlank()) {
            _authState.value = AuthState.Error("User name cannot be empty")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.register(email, password, userName)
            if (result.isSuccess) {
                _authState.value = AuthState.Success("Registration successful")
                // Navigate to main app
                _authNavigationEvent.emit(AuthNavigationEvent.NavigateToMain)
            } else {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "An unknown error occurred")
            }
        }
    }

    /**
     * Sends a password reset email to the specified user by validating the email and using Firebase Auth directly.
     * Updates the state to reflect success or failure with robust error handling.
     *
     * @param email The user's email address to send the reset email to.
     */
    fun sendPasswordResetEmail(email: String) {
        if (!isValidEmail(email)) {
            _authState.value = AuthState.Error("Invalid email format")
            return
        }

        _authState.value = AuthState.Loading

        val auth = FirebaseAuth.getInstance()
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Success("If an account exists, a password reset email has been sent to your email address.")
                } else {
                    val exception = task.exception
                    when (exception) {
                        is FirebaseAuthInvalidUserException -> {
                            _authState.value = AuthState.Error("No account found for this email address. Please check and try again.")
                            Log.e("AuthViewModel", "FirebaseAuthInvalidUserException: ${exception.message}", exception)
                        }
                        is FirebaseAuthException -> {
                            _authState.value = AuthState.Error("An unexpected error occurred. Please try again later.")
                            Log.e("AuthViewModel", "FirebaseAuthException: ${exception.message}", exception)
                        }
                        else -> {
                            _authState.value = AuthState.Error("An unexpected error occurred. Please try again later.")
                            Log.e("AuthViewModel", "Exception: ${exception?.message}", exception)
                        }
                    }
                }
            }
    }

    /**
     * Handles user logout by calling the repository to sign out and updating the state accordingly.
     * Does not emit navigation events; navigation is handled by observing auth state changes elsewhere.
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

    /**
     * Alias for [resetAuthState]. Resets the authentication state to Idle.
     */
    fun resetToIdle() = resetAuthState()

    /**
     * Validates the email format using Android's Patterns.EMAIL_ADDRESS matcher.
     *
     * @param email The email string to validate.
     * @return True if the email format is valid, false otherwise.
     */
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
