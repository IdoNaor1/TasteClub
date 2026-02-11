package com.tasteclub.app.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.tasteclub.app.R
import com.tasteclub.app.util.ServiceLocator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ForgotPasswordFragment - Password reset screen
 */
class ForgotPasswordFragment : Fragment(R.layout.fragment_forgot_password) {

    private val viewModel: AuthViewModel by viewModels {
        val repo = ServiceLocator.provideAuthRepository(requireContext())
        AuthViewModelFactory(repo)
    }

    private lateinit var emailLayout: TextInputLayout
    private lateinit var emailEditText: TextInputEditText
    private lateinit var sendResetButton: MaterialButton
    private lateinit var messageText: android.widget.TextView
    private lateinit var backToLoginButton: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupListeners()
        observeViewModel()
    }

    private fun bindViews(view: View) {
        emailLayout = view.findViewById(R.id.emailLayout)
        emailEditText = view.findViewById(R.id.emailEditText)
        sendResetButton = view.findViewById(R.id.sendResetButton)
        messageText = view.findViewById(R.id.messageText)
        backToLoginButton = view.findViewById(R.id.backToLoginButton)
    }

    private fun setupListeners() {
        // Clear errors on focus
        emailEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                emailLayout.error = null
                messageText.visibility = View.GONE
            }
        }

        sendResetButton.setOnClickListener {
            clearFieldErrors()

            val email = emailEditText.text?.toString().orEmpty().trim()

            if (email.isBlank()) {
                emailLayout.error = getString(R.string.error_email_required)
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.error = getString(R.string.error_email_invalid)
                return@setOnClickListener
            }

            viewModel.sendPasswordResetEmail(email)
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
                            messageText.visibility = View.GONE
                        }
                        is AuthState.Loading -> {
                            setUiEnabled(false)
                            messageText.visibility = View.GONE
                        }
                        is AuthState.Success -> {
                            setUiEnabled(true)
                            // No message shown for success to avoid misleading users about email existence
                            viewModel.resetToIdle()
                        }
                        is AuthState.Error -> {
                            setUiEnabled(true)
                            messageText.text = state.message
                            messageText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                            messageText.visibility = View.VISIBLE
                            // Delay resetting to Idle to keep message visible
                            viewLifecycleOwner.lifecycleScope.launch {
                                delay(2000L) // 2 seconds
                                viewModel.resetToIdle()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun clearFieldErrors() {
        emailLayout.error = null
    }

    private fun setUiEnabled(enabled: Boolean) {
        emailEditText.isEnabled = enabled
        sendResetButton.isEnabled = enabled
    }
}
