package com.tasteclub.app.ui.auth

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.tasteclub.app.R

/**
 * SplashFragment - Entry point of the app
 * Shows splash screen while checking authentication state
 */
class SplashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Auto-navigate to feed after 2 seconds
        // TODO: In Phase 5, check if user is logged in here
        // For now, always navigate to feed to demonstrate navigation
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                findNavController().navigate(R.id.action_splash_to_feed)
            } catch (e: Exception) {
                // Navigation already happened or fragment not attached
            }
        }, 2000) // 2 second delay
    }
}

