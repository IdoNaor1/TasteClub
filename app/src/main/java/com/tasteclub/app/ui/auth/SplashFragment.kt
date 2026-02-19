package com.tasteclub.app.ui.auth

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.tasteclub.app.R
import com.tasteclub.app.util.ServiceLocator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment(R.layout.fragment_splash) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val authRepo = ServiceLocator.provideAuthRepository(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            delay(2000)

            val navController = findNavController()
            if (navController.currentDestination?.id != R.id.splashFragment) return@launch

            val action = if (authRepo.isLoggedIn()) {
                // Pull to local cache and check if profile exists
                // Defensive: refreshUserFromRemote can throw; catch to avoid crash on startup
                val user = try {
                    authRepo.currentUserId()?.let { authRepo.refreshUserFromRemote(it) }
                } catch (e: Exception) {
                    android.util.Log.e("SplashFragment", "Error refreshing user on splash: ${e.message}", e)
                    null
                }
                if (user != null) {
                    R.id.action_splash_to_feed
                } else {
                    R.id.action_splash_to_login
                }
            } else {
                R.id.action_splash_to_login
            }

            navController.navigate(action)
        }
    }
}
