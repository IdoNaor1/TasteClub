package com.tasteclub.app.ui.discover

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.tasteclub.app.R
import com.tasteclub.app.databinding.FragmentDiscoverBinding
import com.tasteclub.app.ui.discover.DiscoverAdapter.DiscoverItem
import com.tasteclub.app.util.ServiceLocator

/**
 * DiscoverFragment - Search and discovery screen.
 * Allows users to search across restaurants, users, and reviews
 * with category tabs and real-time filtering.
 */
class DiscoverFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DiscoverViewModel
    private lateinit var discoverAdapter: DiscoverAdapter

    // Track current tab to avoid redundant rebuilds
    private var currentTab = DiscoverViewModel.Tab.ALL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecyclerView()
        setupSearchBar()
        setupTabs()
        observeViewModel()
    }

    // ---- Setup ----

    private fun setupViewModel() {
        val reviewRepository = ServiceLocator.provideReviewRepository(requireContext())
        val restaurantRepository = ServiceLocator.provideRestaurantRepository(requireContext())
        val authRepository = ServiceLocator.provideAuthRepository(requireContext())
        val factory = DiscoverViewModelFactory(reviewRepository, restaurantRepository, authRepository)
        viewModel = ViewModelProvider(this, factory)[DiscoverViewModel::class.java]
    }

    private fun setupRecyclerView() {
        discoverAdapter = DiscoverAdapter(
            onRestaurantClick = { restaurantId, restaurantName ->
                val bundle = Bundle().apply {
                    putString("restaurantId", restaurantId)
                    putString("restaurantName", restaurantName)
                }
                findNavController().navigate(R.id.action_discover_to_restaurant_detail, bundle)
            },
            onUserClick = { userId ->
                val bundle = Bundle().apply {
                    putString("userId", userId)
                }
                findNavController().navigate(R.id.action_discover_to_other_profile, bundle)
            },
            onReviewClick = { review ->
                // Navigate to restaurant detail for the review's restaurant
                if (review.restaurantId.isNotBlank()) {
                    val bundle = Bundle().apply {
                        putString("restaurantId", review.restaurantId)
                        putString("restaurantName", review.restaurantName)
                    }
                    findNavController().navigate(R.id.action_discover_to_restaurant_detail, bundle)
                }
            }
        )

        binding.resultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = discoverAdapter
        }
    }

    private fun setupSearchBar() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                viewModel.setQuery(query)
                binding.clearButton.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.clearButton.setOnClickListener {
            binding.searchEditText.text.clear()
            viewModel.clearQuery()
            binding.clearButton.visibility = View.GONE
        }

        // Search button: refresh data from Firestore + re-filter locally
        binding.searchButton.setOnClickListener {
            hideKeyboard()
            viewModel.onSearchClick()
        }

        // Also trigger search on keyboard "Search" IME action
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                viewModel.onSearchClick()
                true
            } else {
                false
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
    }

    private fun setupTabs() {
        val tabs = binding.categoryTabs
        tabs.addTab(tabs.newTab().setText(getString(R.string.tab_all)))
        tabs.addTab(tabs.newTab().setText(getString(R.string.tab_restaurants)))
        tabs.addTab(tabs.newTab().setText(getString(R.string.tab_users)))
        tabs.addTab(tabs.newTab().setText(getString(R.string.tab_reviews)))

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val newTab = when (tab?.position) {
                    1 -> DiscoverViewModel.Tab.RESTAURANTS
                    2 -> DiscoverViewModel.Tab.USERS
                    3 -> DiscoverViewModel.Tab.REVIEWS
                    else -> DiscoverViewModel.Tab.ALL
                }
                currentTab = newTab
                viewModel.selectTab(newTab)
                rebuildList()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    // ---- Observers ----

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                binding.resultsRecyclerView.visibility = View.GONE
                binding.emptyStateContainer.visibility = View.GONE
            } else {
                rebuildList()
            }
        }

        // Observe all filtered results and rebuild on change
        viewModel.filteredRestaurants.observe(viewLifecycleOwner) { rebuildList() }
        viewModel.filteredUsers.observe(viewLifecycleOwner) { rebuildList() }
        viewModel.filteredReviews.observe(viewLifecycleOwner) { rebuildList() }

        // Update tab badges with counts
        viewModel.restaurantCount.observe(viewLifecycleOwner) { count ->
            updateTabText(1, getString(R.string.tab_restaurants), count)
        }
        viewModel.userCount.observe(viewLifecycleOwner) { count ->
            updateTabText(2, getString(R.string.tab_users), count)
        }
        viewModel.reviewCount.observe(viewLifecycleOwner) { count ->
            updateTabText(3, getString(R.string.tab_reviews), count)
        }
    }

    private fun updateTabText(position: Int, label: String, count: Int) {
        val tab = binding.categoryTabs.getTabAt(position) ?: return
        tab.text = "$label ($count)"
    }

    // ---- List Building ----

    private fun rebuildList() {
        if (viewModel.isLoading.value == true) return

        val restaurants = viewModel.filteredRestaurants.value ?: emptyList()
        val users = viewModel.filteredUsers.value ?: emptyList()
        val reviews = viewModel.filteredReviews.value ?: emptyList()

        val items = mutableListOf<DiscoverItem>()

        when (currentTab) {
            DiscoverViewModel.Tab.ALL -> {
                if (restaurants.isNotEmpty()) {
                    items.add(DiscoverItem.SectionHeader(getString(R.string.section_restaurants)))
                    items.addAll(restaurants.map { DiscoverItem.RestaurantItem(it) })
                }
                if (users.isNotEmpty()) {
                    items.add(DiscoverItem.SectionHeader(getString(R.string.section_users)))
                    items.addAll(users.map { DiscoverItem.UserItem(it) })
                }
                if (reviews.isNotEmpty()) {
                    items.add(DiscoverItem.SectionHeader(getString(R.string.section_reviews)))
                    items.addAll(reviews.map { DiscoverItem.ReviewItem(it) })
                }
            }
            DiscoverViewModel.Tab.RESTAURANTS -> {
                items.addAll(restaurants.map { DiscoverItem.RestaurantItem(it) })
            }
            DiscoverViewModel.Tab.USERS -> {
                items.addAll(users.map { DiscoverItem.UserItem(it) })
            }
            DiscoverViewModel.Tab.REVIEWS -> {
                items.addAll(reviews.map { DiscoverItem.ReviewItem(it) })
            }
        }

        discoverAdapter.submitList(items)

        // Show/hide empty state
        val query = viewModel.query.value ?: ""
        if (items.isEmpty()) {
            binding.resultsRecyclerView.visibility = View.GONE
            binding.emptyStateContainer.visibility = View.VISIBLE
            binding.emptyStateText.text = if (query.isNotBlank()) {
                getString(R.string.no_results_found)
            } else {
                getString(R.string.discover_prompt)
            }
        } else {
            binding.resultsRecyclerView.visibility = View.VISIBLE
            binding.emptyStateContainer.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
