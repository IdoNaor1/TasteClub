package com.tasteclub.app.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.tasteclub.app.R
import com.tasteclub.app.ui.auth.AuthViewModel
import com.tasteclub.app.ui.auth.AuthViewModelFactory
import com.tasteclub.app.ui.auth.AuthState
import com.tasteclub.app.util.ServiceLocator
import kotlinx.coroutines.launch

/**
 * FeedFragment - Main feed screen showing all reviews
 */
class FeedFragment : Fragment() {

    private val viewModel: AuthViewModel by viewModels {
        val repo = ServiceLocator.provideAuthRepository(requireContext())
        AuthViewModelFactory(repo)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val logoutButton = view.findViewById<Button>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            viewModel.logout()
        }

        lifecycleScope.launch {
            viewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Success -> {
                        Toast.makeText(requireContext(), state.message ?: "Logged out", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_global_login)
                    }
                    is AuthState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }
}
