package com.tasteclub.app

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.libraries.places.api.Places
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.squareup.picasso.Picasso
import com.tasteclub.app.databinding.ActivityMainBinding
import com.tasteclub.app.util.NetworkStatus
import com.tasteclub.app.util.ServiceLocator
import kotlinx.coroutines.flow.collectLatest
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

    // Tracks the LiveData currently observed for the toolbar avatar, so we can
    // detach it before attaching a new one when a different user logs in.
    private var toolbarUserLiveData: LiveData<com.tasteclub.app.data.model.User?>? = null
    private val toolbarUserObserver = Observer<com.tasteclub.app.data.model.User?> { user ->
        if (user != null && user.profileImageUrl.isNotBlank()) {
            val sep = if (user.profileImageUrl.contains('?')) "&" else "?"
            val url = "${user.profileImageUrl}${sep}t=${user.lastUpdated}"
            Picasso.get()
                .load(url)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(binding.toolbarProfileImage)
        } else {
            binding.toolbarProfileImage.setImageResource(R.drawable.ic_profile)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applySystemBarColors()
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

        // Show a global offline indicator and gate create actions.
        observeNetworkStatus()

        //initPlaces()
    }

    private fun applySystemBarColors() {
        val typedArray = theme.obtainStyledAttributes(intArrayOf(com.google.android.material.R.attr.colorSurface))
        val barColor = typedArray.getColor(0, 0)
        typedArray.recycle()

        val useDarkIcons = ColorUtils.calculateLuminance(barColor) > 0.5
        val barStyle = if (useDarkIcons) {
            SystemBarStyle.light(barColor, barColor)
        } else {
            SystemBarStyle.dark(barColor)
        }

        enableEdgeToEdge(
            statusBarStyle = barStyle,
            navigationBarStyle = barStyle
        )
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
                R.id.discoverFragment
            )
        )

        // Setup action bar with nav controller
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Profile image click -> navigate to profile tab
        binding.toolbarProfileImage.setOnClickListener {
            navController.navigate(R.id.profileFragment)
        }

        // Add review button click -> navigate to create review
        binding.toolbarAddReview.setOnClickListener {
            navController.navigate(R.id.action_global_create_review)
        }


        // Detail toolbar: back button click -> navigate up
        binding.toolbarBackButton.setOnClickListener {
            navController.navigateUp()
        }
    }

    /**
     * Control bottom navigation visibility based on destination
     * Hide bottom nav on auth screens and detail screens
     */
    private fun controlBottomNavVisibility() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                // Main destinations: show main toolbar + bottom nav
                R.id.feedFragment,
                R.id.myPostsFragment,
                R.id.discoverFragment -> {
                    bottomNavigationView.visibility = View.VISIBLE
                    binding.toolbar.visibility = View.VISIBLE
                    binding.toolbarMainContent.visibility = View.VISIBLE
                    binding.toolbarDetailContent.visibility = View.GONE
                    supportActionBar?.title = ""
                    binding.toolbar.navigationIcon = null
                }

                // Profile: show detail toolbar (back arrow + logo) + bottom nav
                R.id.profileFragment -> {
                    bottomNavigationView.visibility = View.VISIBLE
                    binding.toolbar.visibility = View.VISIBLE
                    binding.toolbarMainContent.visibility = View.GONE
                    binding.toolbarDetailContent.visibility = View.VISIBLE
                    supportActionBar?.title = ""
                    binding.toolbar.navigationIcon = null
                }

                // Auth screens: hide everything
                R.id.splashFragment,
                R.id.loginFragment,
                R.id.registerFragment,
                R.id.forgotPasswordFragment -> {
                    bottomNavigationView.visibility = View.GONE
                    binding.toolbar.visibility = View.GONE
                }

                // All other screens: show detail toolbar (back arrow + logo)
                else -> {
                    bottomNavigationView.visibility = View.GONE
                    binding.toolbar.visibility = View.VISIBLE
                    binding.toolbarMainContent.visibility = View.GONE
                    binding.toolbarDetailContent.visibility = View.VISIBLE
                    supportActionBar?.title = ""
                    binding.toolbar.navigationIcon = null
                }
            }
        }
    }

    /**
     * Observe authentication state and navigate accordingly
     */
    private fun observeAuthState() {
        val repo = ServiceLocator.provideAuthRepository(this@MainActivity)

        // Attach observer for whoever is already logged in at startup
        repo.currentUserId()?.let { observeToolbarUser(it) }

        lifecycleScope.launch {
            repo.observeAuthState().collect { isLoggedIn ->
                if (skipInitialAuthState) {
                    skipInitialAuthState = false
                } else {
                    if (!isLoggedIn) {
                        // Detach old observer and clear avatar
                        toolbarUserLiveData?.removeObserver(toolbarUserObserver)
                        toolbarUserLiveData = null
                        binding.toolbarProfileImage.setImageResource(R.drawable.ic_profile)
                        navController.navigate(R.id.action_global_login)
                    } else {
                        // New user just logged in — attach fresh observer
                        repo.currentUserId()?.let { observeToolbarUser(it) }
                    }
                }
            }
        }
    }

    /** Detach any existing toolbar-user observer and attach one for [userId]. */
    private fun observeToolbarUser(userId: String) {
        val repo = ServiceLocator.provideAuthRepository(this)
        toolbarUserLiveData?.removeObserver(toolbarUserObserver)
        val liveData = repo.observeUser(userId)
        toolbarUserLiveData = liveData
        liveData.observe(this, toolbarUserObserver)
    }

    private fun observeNetworkStatus() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ServiceLocator.provideNetworkMonitor(this@MainActivity)
                    .status
                    .collectLatest { status ->
                        val isOffline = status != NetworkStatus.Available
                        binding.offlineBanner.visibility = if (isOffline) View.VISIBLE else View.GONE
                        binding.toolbarAddReview.isEnabled = !isOffline
                        binding.toolbarAddReview.alpha = if (isOffline) 0.5f else 1f
                    }
            }
        }
    }

    private fun initPlaces() {
        val apiKey = BuildConfig.PLACES_API_KEY

        if (apiKey.isEmpty() || apiKey == "DEFAULT_API_KEY") {
            Log.e("Places test", "No api key")
            finish()
            return
        }

        Places.initializeWithNewPlacesApiEnabled(applicationContext, apiKey)
    }

    /**
     * Handle up/back navigation in toolbar
     */
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}