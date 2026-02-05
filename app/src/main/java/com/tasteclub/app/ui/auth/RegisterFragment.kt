package com.tasteclub.app.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.tasteclub.app.R
import com.tasteclub.app.util.ServiceLocator
import kotlinx.coroutines.launch

/**
 * RegisterFragment - User registration screen
 */
class RegisterFragment : Fragment(R.layout.fragment_register) {

    private val viewModel: AuthViewModel by viewModels {
        val repo = ServiceLocator.provideAuthRepository(requireContext())
        AuthViewModelFactory(repo)
    }

    private lateinit var userNameLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout
    private lateinit var userNameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupListeners()
        observeViewModel()
    }

    private fun bindViews(view: View) {
        userNameLayout = view.findViewById(R.id.userNameLayout)
        emailLayout = view.findViewById(R.id.emailLayout)
        passwordLayout = view.findViewById(R.id.passwordLayout)
        confirmPasswordLayout = view.findViewById(R.id.confirmPasswordLayout)
        userNameEditText = view.findViewById(R.id.userNameEditText)
        emailEditText = view.findViewById(R.id.emailEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        confirmPasswordEditText = view.findViewById(R.id.confirmPasswordEditText)
    }

    private fun setupListeners() {
        val registerButton = requireView().findViewById<View>(R.id.registerButton)
        val backToLoginButton = requireView().findViewById<View>(R.id.backToLoginButton)

        // Clear errors on focus
        userNameEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) userNameLayout.error = null
        }
        emailEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) emailLayout.error = null
        }
        passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) passwordLayout.error = null
        }
        confirmPasswordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) confirmPasswordLayout.error = null
        }

        registerButton.setOnClickListener {
            clearFieldErrors()

            val userName = userNameEditText.text?.toString().orEmpty().trim()
            val email = emailEditText.text?.toString().orEmpty().trim()
            val password = passwordEditText.text?.toString().orEmpty()
            val confirmPassword = confirmPasswordEditText.text?.toString().orEmpty()

            val ok = validate(userName, email, password, confirmPassword)
            if (!ok) return@setOnClickListener

            viewModel.register(email, password, userName)
        }

        backToLoginButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { state ->
                    when (state) {
                        is AuthState.Idle -> {
                            setUiEnabled(true)
                        }

                        is AuthState.Loading -> {
                            setUiEnabled(false)
                        }

                        is AuthState.Error -> {
                            setUiEnabled(true)
                            showError(state.message)
                            viewModel.resetToIdle()
                        }
                        is AuthState.Success -> {
                            setUiEnabled(true)
                            state.message.let {
                                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

        // Observe navigation events
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authNavigationEvent.collect { event ->
                    when (event) {
                        is AuthNavigationEvent.NavigateToMain -> {
                            // It's now safe to navigate
                            findNavController().navigate(R.id.action_register_to_feed)
                        }
                    }
                }
            }
        }
    }

    private fun validate(userName: String, email: String, password: String, confirmPassword: String): Boolean {
        var valid = true

        if (userName.isBlank()) {
            userNameLayout.error = getString(R.string.error_username_required)
            valid = false
        }

        if (email.isBlank()) {
            emailLayout.error = getString(R.string.error_email_required)
            valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = getString(R.string.error_email_invalid)
            valid = false
        }

        if (password.isBlank()) {
            passwordLayout.error = getString(R.string.error_password_required)
            valid = false
        } else if (password.length < 6) {
            passwordLayout.error = getString(R.string.error_password_too_short)
            valid = false
        }

        if (confirmPassword != password) {
            confirmPasswordLayout.error = getString(R.string.error_password_mismatch)
            valid = false
        }

        return valid
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun clearFieldErrors() {
        userNameLayout.error = null
        emailLayout.error = null
        passwordLayout.error = null
        confirmPasswordLayout.error = null
    }

    private fun setUiEnabled(enabled: Boolean) {
        userNameEditText.isEnabled = enabled
        emailEditText.isEnabled = enabled
        passwordEditText.isEnabled = enabled
        confirmPasswordEditText.isEnabled = enabled
        requireView().findViewById<View>(R.id.registerButton).isEnabled = enabled
        requireView().findViewById<View>(R.id.backToLoginButton).isEnabled = enabled
    }
}
