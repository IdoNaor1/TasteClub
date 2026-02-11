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

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel: AuthViewModel by viewModels {
        val repo = ServiceLocator.provideAuthRepository(requireContext())
        AuthViewModelFactory(repo)
    }

    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupListeners()
        observeViewModel()
    }

    private fun bindViews(view: View) {
        emailLayout = view.findViewById(R.id.emailLayout)
        passwordLayout = view.findViewById(R.id.passwordLayout)
        emailEditText = view.findViewById(R.id.emailEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
    }

    private fun setupListeners() {
        val signInButton = requireView().findViewById<View>(R.id.signInButton)
        val forgotPasswordButton = requireView().findViewById<View>(R.id.forgotPasswordButton)
        val signUpButton = requireView().findViewById<View>(R.id.signUpButton)

        // error cleaning while typing
        emailEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) emailLayout.error = null
        }
        passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) passwordLayout.error = null
        }

        signInButton.setOnClickListener {
            clearFieldErrors()

            val email = emailEditText.text?.toString().orEmpty().trim()
            val password = passwordEditText.text?.toString().orEmpty()

            val ok = validate(email, password)
            if (!ok) return@setOnClickListener

            viewModel.login(email, password)
        }

        forgotPasswordButton.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_forgot_password)
        }

        signUpButton.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
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
                            findNavController().navigate(R.id.action_login_to_feed)
                        }
                    }
                }
            }
        }
    }

    private fun validate(email: String, password: String): Boolean {
        var valid = true

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
            // rules to be enforced
            passwordLayout.error = getString(R.string.error_password_too_short)
            valid = false
        }

        return valid
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun clearFieldErrors() {
        emailLayout.error = null
        passwordLayout.error = null
    }

    private fun setUiEnabled(enabled: Boolean) {
        // prevent double clicking while Loading
        emailEditText.isEnabled = enabled
        passwordEditText.isEnabled = enabled
        requireView().findViewById<View>(R.id.signInButton).isEnabled = enabled
        requireView().findViewById<View>(R.id.forgotPasswordButton).isEnabled = enabled
        requireView().findViewById<View>(R.id.signUpButton).isEnabled = enabled
    }
}
