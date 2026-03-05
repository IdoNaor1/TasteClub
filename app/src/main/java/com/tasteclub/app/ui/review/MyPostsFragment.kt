package com.tasteclub.app.ui.review

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tasteclub.app.R
import com.tasteclub.app.data.model.Review
import com.tasteclub.app.databinding.LayoutEmptyStateBinding
import com.tasteclub.app.util.ServiceLocator

/**
 * MyPostsFragment - Shows the user's own reviews with edit and delete functionality
 *
 * Features:
 * - Displays user's reviews in a RecyclerView
 * - Edit reviews via navigation to EditReviewFragment
 * - Delete reviews with confirmation dialog
 * - Create new reviews via FAB
 * - Empty state when no reviews exist
 * - Loading states
 * - Error handling
 */
class MyPostsFragment : Fragment() {

    // ViewModel with factory
    private val viewModel: MyPostsViewModel by viewModels {
        MyPostsViewModelFactory(
            reviewRepository = ServiceLocator.provideReviewRepository(requireContext()),
            authRepository = ServiceLocator.provideAuthRepository(requireContext())
        )
    }

    // Views
    private lateinit var myPostsRecyclerView: RecyclerView
    private lateinit var emptyStateContainer: ViewGroup
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var fabCreateReview: FloatingActionButton

    // Adapter
    private lateinit var adapter: MyPostsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_posts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupRecyclerView()
        setupFab()
        observeViewModel()
    }

    /**
     * Bind all views from layout
     */
    private fun bindViews(view: View) {
        myPostsRecyclerView = view.findViewById(R.id.myPostsRecyclerView)
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
        fabCreateReview = view.findViewById(R.id.fabCreateReview)
    }

    /**
     * Setup RecyclerView with adapter and layout manager
     */
    private fun setupRecyclerView() {
        adapter = MyPostsAdapter(
            currentUserId = viewModel.currentUserId,
            onEditClick = { review -> onEditReview(review) },
            onDeleteClick = { review -> onDeleteReview(review) },
            onLikeClick = { review -> viewModel.toggleLike(review.id) },
            onRestaurantClick = { restaurantId, restaurantName ->
                val bundle = Bundle().apply {
                    putString("restaurantId", restaurantId)
                    putString("restaurantName", restaurantName)
                }
                findNavController().navigate(R.id.action_my_posts_to_restaurant_detail, bundle)
            }
        )

        myPostsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MyPostsFragment.adapter
            setHasFixedSize(true)
        }
    }

    /**
     * Setup FAB click listener for creating new reviews
     */
    private fun setupFab() {
        fabCreateReview.setOnClickListener {
            navigateToCreateReview()
        }
    }

    /**
     * Observe ViewModel state changes
     */
    private fun observeViewModel() {
        viewModel.myPostsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MyPostsViewModel.MyPostsState.Loading -> {
                    showLoading()
                }
                is MyPostsViewModel.MyPostsState.Success -> {
                    showPosts(state.reviews)
                }
                is MyPostsViewModel.MyPostsState.Empty -> {
                    showEmptyState()
                }
                is MyPostsViewModel.MyPostsState.Deleting -> {
                    // Optional: Show deleting indicator on specific card
                    // For now, we just keep the current UI
                }
                is MyPostsViewModel.MyPostsState.DeleteSuccess -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.review_deleted),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is MyPostsViewModel.MyPostsState.Error -> {
                    showError(state.message)
                }
            }
        }
    }

    /**
     * Show loading state
     */
    private fun showLoading() {
        loadingProgressBar.visibility = View.VISIBLE
        myPostsRecyclerView.visibility = View.GONE
        emptyStateContainer.visibility = View.GONE
    }

    /**
     * Show posts in RecyclerView
     */
    private fun showPosts(reviews: List<Review>) {
        loadingProgressBar.visibility = View.GONE
        myPostsRecyclerView.visibility = View.VISIBLE
        emptyStateContainer.visibility = View.GONE

        // Submit list to adapter
        adapter.submitList(reviews)
    }

    /**
     * Show empty state when no reviews exist
     */
    private fun showEmptyState() {
        loadingProgressBar.visibility = View.GONE
        myPostsRecyclerView.visibility = View.GONE
        emptyStateContainer.visibility = View.VISIBLE

        // Inflate empty state layout if not already inflated
        if (emptyStateContainer.childCount == 0) {
            val emptyStateBinding = LayoutEmptyStateBinding.inflate(
                layoutInflater,
                emptyStateContainer,
                true
            )

            // Customize text for My Posts
            emptyStateBinding.emptyStateTitleTextView.text = getString(R.string.empty_my_posts_title)
            emptyStateBinding.emptyStateDescriptionTextView.text = getString(R.string.empty_my_posts_description)

            // Setup action button
            emptyStateBinding.emptyStateActionButton.setOnClickListener {
                navigateToCreateReview()
            }
        }
    }

    /**
     * Show error message
     */
    private fun showError(message: String) {
        loadingProgressBar.visibility = View.GONE
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

        // Optionally show retry UI or keep current state
    }

    /**
     * Handle edit review click
     */
    private fun onEditReview(review: Review) {
        try {
            val action = MyPostsFragmentDirections.actionMyPostsToEditReview(review.id)
            findNavController().navigate(action)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Failed to navigate to edit screen",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Handle delete review click - shows confirmation dialog
     */
    private fun onDeleteReview(review: Review) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_review_title))
            .setMessage(getString(R.string.delete_review_message))
            .setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                viewModel.deleteReview(review.id)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Navigate to create review screen
     */
    private fun navigateToCreateReview() {
        try {
            findNavController().navigate(R.id.action_my_posts_to_create_review)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Failed to navigate to create review screen",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh reviews when returning to this fragment
        viewModel.getMyReviews()
    }
}
