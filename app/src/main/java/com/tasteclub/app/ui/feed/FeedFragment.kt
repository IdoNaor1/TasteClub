package com.tasteclub.app.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tasteclub.app.R
import com.tasteclub.app.databinding.FragmentFeedBinding
import com.tasteclub.app.databinding.LayoutEmptyStateBinding
import com.tasteclub.app.util.ServiceLocator

/**
 * FeedFragment - Main feed screen showing all reviews
 * Implements MVVM pattern with ViewModel for business logic
 */
class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: FeedViewModel
    private lateinit var reviewAdapter: ReviewAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecyclerView()
        setupSwipeRefresh()
        setupToolbar()
        observeViewModel()
    }

    /**
     * Initialize ViewModel with Factory and Repository
     */
    private fun setupViewModel() {
        val repository = ServiceLocator.provideReviewRepository(requireContext())
        val factory = FeedViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[FeedViewModel::class.java]
    }

    /**
     * Setup RecyclerView with adapter and scroll listener for pagination
     */
    private fun setupRecyclerView() {
        reviewAdapter = ReviewAdapter { restaurantId, restaurantName ->
            val bundle = Bundle().apply {
                putString("restaurantId", restaurantId)
                putString("restaurantName", restaurantName)
            }
            findNavController().navigate(R.id.action_feed_to_restaurant_detail, bundle)
        }

        binding.reviewsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reviewAdapter

            // Add scroll listener for lazy loading
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    // Load more when scrolled to last 3 items
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3
                        && firstVisibleItemPosition >= 0
                        && dy > 0) { // Only when scrolling down
                        viewModel.loadMoreReviews()
                    }
                }
            })
        }
    }

    /**
     * Setup SwipeRefreshLayout for pull-to-refresh
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.apply {
            setColorSchemeResources(R.color.primary_brown)
            setOnRefreshListener {
                viewModel.refreshFeed()
            }
        }
    }

    /**
     * Setup create review FAB
     */
    private fun setupToolbar() {

        // Create review FAB click -> Navigate to CreateReview
        binding.createReviewFab.setOnClickListener {
            findNavController().navigate(R.id.createReviewFragment)
        }
    }

    /**
     * Observe ViewModel state changes and update UI accordingly
     */
    private fun observeViewModel() {
        // Observe feed state
        viewModel.feedState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is FeedViewModel.FeedState.Loading -> {
                    showLoading()
                }
                is FeedViewModel.FeedState.Success -> {
                    showReviews(state.reviews)
                }
                is FeedViewModel.FeedState.Error -> {
                    showError(state.message)
                }
                is FeedViewModel.FeedState.Empty -> {
                    showEmptyState()
                }
            }
        }

        // Observe loading more state for pagination
        viewModel.isLoadingMore.observe(viewLifecycleOwner) { isLoading ->
            // Could show a loading indicator at bottom of list if needed
            // For now, silent loading is fine
        }
    }

    /**
     * Show loading skeleton
     */
    private fun showLoading() {
        binding.swipeRefreshLayout.isRefreshing = false
        binding.reviewsRecyclerView.visibility = View.GONE
        binding.emptyStateContainer.visibility = View.GONE
        binding.skeletonContainer.visibility = View.VISIBLE
    }

    /**
     * Show reviews in RecyclerView
     */
    private fun showReviews(reviews: List<com.tasteclub.app.data.model.Review>) {
        binding.swipeRefreshLayout.isRefreshing = false
        binding.skeletonContainer.visibility = View.GONE
        binding.emptyStateContainer.visibility = View.GONE
        binding.reviewsRecyclerView.visibility = View.VISIBLE

        reviewAdapter.submitList(reviews)
    }

    /**
     * Show error message
     */
    private fun showError(message: String) {
        binding.swipeRefreshLayout.isRefreshing = false
        binding.skeletonContainer.visibility = View.GONE

        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_LONG
        ).show()

        // Keep showing existing list if available, otherwise show empty state
        if (reviewAdapter.itemCount == 0) {
            showEmptyState()
        }
    }

    /**
     * Show empty state with action button
     */
    private fun showEmptyState() {
        binding.swipeRefreshLayout.isRefreshing = false
        binding.skeletonContainer.visibility = View.GONE
        binding.reviewsRecyclerView.visibility = View.GONE
        binding.emptyStateContainer.visibility = View.VISIBLE

        // Inflate empty state layout if not already inflated
        if (binding.emptyStateContainer.childCount == 0) {
            val emptyStateBinding = LayoutEmptyStateBinding.inflate(
                layoutInflater,
                binding.emptyStateContainer,
                true
            )

            // Setup action button
            emptyStateBinding.emptyStateActionButton.setOnClickListener {
                findNavController().navigate(R.id.createReviewFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
