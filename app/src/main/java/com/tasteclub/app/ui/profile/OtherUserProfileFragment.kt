package com.tasteclub.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.picasso.Picasso
import com.tasteclub.app.R
import com.tasteclub.app.databinding.FragmentOtherUserProfileBinding
import com.tasteclub.app.ui.comment.CommentsBottomSheetFragment
import com.tasteclub.app.ui.feed.ReviewAdapter
import com.tasteclub.app.util.ServiceLocator

/**
 * OtherUserProfileFragment – Displays another user's public profile.
 *
 * Features:
 * - Profile header with avatar, username, bio
 * - Stats row (reviews, following, followers)
 * - Follow / unfollow button with reactive state
 * - User's reviews listed newest-first
 * - Empty state when the user has no reviews
 */
class OtherUserProfileFragment : Fragment() {

    private var _binding: FragmentOtherUserProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: OtherUserProfileViewModel
    private lateinit var reviewAdapter: ReviewAdapter

    private val targetUserId: String by lazy {
        arguments?.getString("userId") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOtherUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecyclerView()
        setupFollowButton()
        observeViewModel()
    }

    // --------------------
    // Setup
    // --------------------

    private fun setupViewModel() {
        val authRepository = ServiceLocator.provideAuthRepository(requireContext())
        val reviewRepository = ServiceLocator.provideReviewRepository(requireContext())
        val commentRepository = ServiceLocator.provideCommentRepository(requireContext())

        val factory = OtherUserProfileViewModelFactory(
            authRepository,
            reviewRepository,
            commentRepository,
            targetUserId
        )
        viewModel = ViewModelProvider(this, factory)[OtherUserProfileViewModel::class.java]
    }


    private fun setupRecyclerView() {
        reviewAdapter = ReviewAdapter(
            currentUserId = viewModel.currentUserId,
            onLikeClick = { review -> viewModel.toggleLike(review.id) },
            onRestaurantClick = { restaurantId, restaurantName ->
                // Navigate to restaurant detail if nav action exists from here
                try {
                    val bundle = Bundle().apply {
                        putString("restaurantId", restaurantId)
                        putString("restaurantName", restaurantName)
                    }
                    findNavController().navigate(
                        R.id.restaurantDetailFragment,
                        bundle
                    )
                } catch (_: Exception) {
                    // Navigation action may not exist from this destination
                }
            },
            onUserClick = { userId ->
                // Already on a user profile – navigate only if different user
                if (userId != targetUserId) {
                    val bundle = Bundle().apply {
                        putString("userId", userId)
                    }
                    findNavController().navigate(
                        R.id.otherUserProfileFragment,
                        bundle
                    )
                }
            },
            onCommentClick = { review ->
                val sheet = CommentsBottomSheetFragment.newInstance(review.id)
                sheet.onCommentCountChanged = { reviewId, newCount ->
                    viewModel.updateCommentCount(reviewId, newCount)
                }
                sheet.show(childFragmentManager, "comments_${review.id}")
            }
        )

        binding.reviewsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reviewAdapter
        }
    }

    private fun setupFollowButton() {
        binding.followButton.setOnClickListener {
            viewModel.toggleFollow()
        }
    }

    // --------------------
    // Observe ViewModel
    // --------------------

    private fun observeViewModel() {
        // Profile loading state
        viewModel.profileState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is OtherUserProfileViewModel.ProfileState.Loading -> {
                    binding.loadingProgressBar.visibility = View.VISIBLE
                }
                is OtherUserProfileViewModel.ProfileState.Idle -> {
                    binding.loadingProgressBar.visibility = View.GONE
                }
                is OtherUserProfileViewModel.ProfileState.Error -> {
                    binding.loadingProgressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Target user profile
        viewModel.targetUser.observe(viewLifecycleOwner) { user ->
            user ?: return@observe

            binding.userName.text = user.userName
            binding.userBio.text = user.bio.ifEmpty { "" }
            binding.userBio.visibility = if (user.bio.isEmpty()) View.GONE else View.VISIBLE

            // Profile image
            if (user.profileImageUrl.isNotEmpty()) {
                Picasso.get()
                    .load(user.profileImageUrl)
                    .placeholder(R.drawable.ic_user_placeholder)
                    .error(R.drawable.ic_user_placeholder)
                    .into(binding.profileImage)
            } else {
                binding.profileImage.setImageResource(R.drawable.ic_user_placeholder)
            }

            // Stats
            binding.followingCountText.text = user.followingCount.toString()
            binding.followersCountText.text = user.followersCount.toString()
        }

        // Review count (dynamic, based on loaded reviews)
        viewModel.reviewCount.observe(viewLifecycleOwner) { count ->
            binding.reviewCountText.text = count.toString()
            binding.reviewsSectionTitle.text = getString(R.string.reviews_section_count, count)
        }

        // Reviews list
        viewModel.reviews.observe(viewLifecycleOwner) { reviews ->
            reviewAdapter.submitList(reviews)

            if (reviews.isEmpty()) {
                binding.emptyReviewsText.visibility = View.VISIBLE
                binding.reviewsRecyclerView.visibility = View.GONE
            } else {
                binding.emptyReviewsText.visibility = View.GONE
                binding.reviewsRecyclerView.visibility = View.VISIBLE
            }
        }

        // Follow state
        viewModel.isFollowing.observe(viewLifecycleOwner) { following ->
            updateFollowButton(following)
        }

        // Follow loading
        viewModel.isFollowLoading.observe(viewLifecycleOwner) { loading ->
            binding.followButton.isEnabled = !loading
        }

        // Follow error (rollback already applied in repo – just inform the user)
        viewModel.followError.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                viewModel.clearFollowError()
            }
        }
    }

    /**
     * Toggle button appearance between Follow and Following states.
     * Every property is explicitly set in both branches so re-entry never shows stale styles.
     */
    private fun updateFollowButton(isFollowing: Boolean) {
        val primaryBrown = resources.getColor(R.color.primary_brown, null)
        val white = resources.getColor(R.color.white, null)
        val strokePx = (2 * resources.displayMetrics.density).toInt()

        if (isFollowing) {
            binding.followButton.text = getString(R.string.following_button)
            // Transparent fill
            binding.followButton.backgroundTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
            binding.followButton.setTextColor(primaryBrown)
            binding.followButton.strokeColor =
                android.content.res.ColorStateList.valueOf(primaryBrown)
            binding.followButton.strokeWidth = strokePx
        } else {
            binding.followButton.text = getString(R.string.follow)
            // Solid primary_brown fill
            binding.followButton.backgroundTintList =
                android.content.res.ColorStateList.valueOf(primaryBrown)
            binding.followButton.setTextColor(white)
            binding.followButton.strokeColor =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
            binding.followButton.strokeWidth = 0
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
