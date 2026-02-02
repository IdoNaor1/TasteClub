package com.tasteclub.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tasteclub.app.databinding.ActivityMainBinding
import com.tasteclub.app.util.ServiceLocator
import kotlinx.coroutines.launch

/**
 * MainActivity - Single activity container for TasteClub app
 *
 * Responsibilities:
 * - Hosts NavHostFragment for navigation
 * - Manages BottomNavigationView visibility
 * - Coordinates toolbar with navigation
 *
 * Reference: Lecture 7 - Navigation in Android Development
 */
class MainActivity : AppCompatActivity() {

    // View Binding (generates binding class from activity_main.xml)
    private lateinit var binding: ActivityMainBinding

    // Navigation controller from NavHostFragment
    private lateinit var navController: NavController

    // Bottom navigation view
    private lateinit var bottomNavigationView: BottomNavigationView

    // Flag to skip initial auth state emission
    private var skipInitialAuthState = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup navigation
        setupNavigation()

        // Setup toolbar
        setupToolbar()

        // Control bottom nav visibility
        controlBottomNavVisibility()

        // Observe auth state for navigation
        observeAuthState()
    }

    /**
     * Setup navigation controller and bottom navigation
     */
    private fun setupNavigation() {
        // Get NavHostFragment from layout
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController

        // Get bottom navigation view
        bottomNavigationView = binding.bottomNavigation

        // Connect bottom navigation with nav controller
        // This automatically handles navigation when items are clicked
        bottomNavigationView.setupWithNavController(navController)

        // Optional: Handle item reselection (e.g., scroll to top when tab clicked again)
        bottomNavigationView.setOnItemReselectedListener { _ ->
            // Do nothing on reselect, or implement custom behavior
            // Example: scroll feed to top when Feed tab clicked again
        }
    }

    /**
     * Setup toolbar with navigation
     */
    private fun setupToolbar() {
        // Set toolbar as support action bar
        setSupportActionBar(binding.toolbar)

        // Define top-level destinations (no back button shown)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.feedFragment,
                R.id.myPostsFragment,
                R.id.profileFragment,
                // Add discoverFragment when implementing Phase 7
                // R.id.discoverFragment
            )
        )

        // Setup action bar with nav controller
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    /**
     * Control bottom navigation visibility based on destination
     * Hide bottom nav on auth screens and detail screens
     */
    private fun controlBottomNavVisibility() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                // Show bottom nav on main destinations
                R.id.feedFragment,
                R.id.myPostsFragment,
                R.id.profileFragment,
                R.id.discoverFragment -> {
                    bottomNavigationView.visibility = View.VISIBLE
                    binding.toolbar.visibility = View.VISIBLE
                }

                // Hide bottom nav on auth screens
                R.id.splashFragment,
                R.id.loginFragment,
                R.id.registerFragment,
                R.id.forgotPasswordFragment -> {
                    bottomNavigationView.visibility = View.GONE
                    binding.toolbar.visibility = View.GONE
                }

                // Hide bottom nav on detail/edit screens
                R.id.createReviewFragment,
                R.id.editReviewFragment,
                R.id.otherUserProfileFragment,
                R.id.restaurantDetailFragment -> {
                    bottomNavigationView.visibility = View.GONE
                    binding.toolbar.visibility = View.VISIBLE
                }

                // Default: hide bottom nav
                else -> {
                    bottomNavigationView.visibility = View.GONE
                    binding.toolbar.visibility = View.VISIBLE
                }
            }
        }
    }

    /**
     * Observe authentication state and navigate accordingly
     */
    private fun observeAuthState() {
        lifecycleScope.launch {
            val repo = ServiceLocator.provideAuthRepository(this@MainActivity)
            repo.observeAuthState().collect { isLoggedIn ->
                if (skipInitialAuthState) {
                    // Skip the initial emission
                    skipInitialAuthState = false
                } else {
                    if (isLoggedIn) {
                        // Navigate to feed using global action
                        navController.navigate(R.id.action_global_feed)
                    } else {
                        // Navigate to login using global action
                        navController.navigate(R.id.action_global_login)
                    }
                }
            }
        }
    }

    /**
     * Handle up/back navigation in toolbar
     */
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}