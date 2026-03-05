package com.tasteclub.app.ui.restaurant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.tasteclub.app.R
import com.tasteclub.app.data.model.Restaurant
import com.tasteclub.app.data.model.Review
import com.tasteclub.app.ui.feed.ReviewAdapter
import com.tasteclub.app.util.ServiceLocator
import kotlinx.coroutines.launch

/**
 * RestaurantDetailFragment - Restaurant detail screen (Phase 7)
 */
class RestaurantDetailFragment : Fragment() {

    private var restaurantId: String = ""
    private var restaurantName: String? = null
    private var restaurantAddress: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        restaurantId = arguments?.getString("restaurantId") ?: ""
        restaurantName = arguments?.getString("restaurantName")
        return inflater.inflate(R.layout.fragment_restaurant_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind UI elements
        val nameTv: TextView = view.findViewById(R.id.restaurant_name)
        val ratingScoreTv: TextView = view.findViewById(R.id.rating_score)
        val ratingStars: RatingBar = view.findViewById(R.id.rating_stars)
        val ratingCountTv: TextView = view.findViewById(R.id.rating_count)
        val addressTv: TextView = view.findViewById(R.id.value_address)
        val cuisineTv: TextView = view.findViewById(R.id.value_cuisine)

        // Rating breakdown views
        val ratingCount5: TextView = view.findViewById(R.id.rating_count_5)
        val ratingCount4: TextView = view.findViewById(R.id.rating_count_4)
        val ratingCount3: TextView = view.findViewById(R.id.rating_count_3)
        val ratingCount2: TextView = view.findViewById(R.id.rating_count_2)
        val ratingCount1: TextView = view.findViewById(R.id.rating_count_1)
        val progress5: LinearProgressIndicator = view.findViewById(R.id.progress_5)
        val progress4: LinearProgressIndicator = view.findViewById(R.id.progress_4)
        val progress3: LinearProgressIndicator = view.findViewById(R.id.progress_3)
        val progress2: LinearProgressIndicator = view.findViewById(R.id.progress_2)
        val progress1: LinearProgressIndicator = view.findViewById(R.id.progress_1)

        // Show restaurant name immediately from nav argument if available
        if (!restaurantName.isNullOrBlank()) {
            nameTv.text = restaurantName
        }

        // Wire review button to navigate to create review with pre-selected restaurant
        val btnReview: MaterialButton = view.findViewById(R.id.btn_review)
        btnReview.setOnClickListener {
            val bundle = Bundle().apply {
                putString("restaurantId", restaurantId)
                putString("restaurantName", restaurantName)
                putString("restaurantAddress", restaurantAddress)
            }
            findNavController().navigate(R.id.action_restaurant_detail_to_create_review, bundle)
        }

        // Setup RecyclerView for reviews
        val reviewsRecycler: RecyclerView = view.findViewById(R.id.reviews_recycler_view)
        val authRepository = ServiceLocator.provideAuthRepository(requireContext())
        val reviewRepo = ServiceLocator.provideReviewRepository(requireContext())
        val currentUserId = authRepository.currentUserId() ?: ""

        val reviewAdapter = ReviewAdapter(
            currentUserId = currentUserId,
            onLikeClick = { review ->
                if (currentUserId.isNotBlank()) {
                    lifecycleScope.launch {
                        try {
                            reviewRepo.toggleLike(review.id, currentUserId)
                        } catch (_: Exception) { }
                    }
                }
            }
        )

        reviewsRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reviewAdapter
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
        }

        // Helper to populate restaurant fields
        fun populateRestaurant(restaurant: Restaurant) {
            nameTv.text = restaurant.name.ifBlank { restaurantName ?: getString(R.string.restaurant_name_placeholder) }
            addressTv.text = restaurant.address.ifBlank { getString(R.string.value_address_default) }
            cuisineTv.text = restaurant.primaryType.ifBlank { getString(R.string.value_cuisine_default) }
            // Track latest data for review button navigation
            if (restaurant.name.isNotBlank()) restaurantName = restaurant.name
            if (restaurant.address.isNotBlank()) restaurantAddress = restaurant.address
        }

        // Helper to update rating UI from reviews list
        fun updateRatingFromReviews(reviews: List<Review>) {
            if (reviews.isEmpty()) {
                ratingScoreTv.text = getString(R.string.rating_score_default)
                ratingStars.rating = 0f
                ratingCountTv.text = getString(R.string.rating_count_default)
                // Reset breakdown
                listOf(ratingCount5, ratingCount4, ratingCount3, ratingCount2, ratingCount1).forEach { it.text = "0" }
                listOf(progress5, progress4, progress3, progress2, progress1).forEach { it.progress = 0 }
                return
            }

            val total = reviews.size
            val avg = reviews.map { it.rating }.average()

            ratingScoreTv.text = String.format("%.1f", avg)
            ratingStars.rating = avg.toFloat()
            ratingCountTv.text = resources.getQuantityString(
                R.plurals.rating_count_format,
                total,
                total
            )

            // Compute rating breakdown
            val counts = IntArray(5)
            for (review in reviews) {
                val idx = review.rating.coerceIn(1, 5) - 1
                counts[idx]++
            }
            val maxCount = counts.max().coerceAtLeast(1)

            ratingCount5.text = counts[4].toString()
            ratingCount4.text = counts[3].toString()
            ratingCount3.text = counts[2].toString()
            ratingCount2.text = counts[1].toString()
            ratingCount1.text = counts[0].toString()

            progress5.progress = (counts[4] * 100) / maxCount
            progress4.progress = (counts[3] * 100) / maxCount
            progress3.progress = (counts[2] * 100) / maxCount
            progress2.progress = (counts[1] * 100) / maxCount
            progress1.progress = (counts[0] * 100) / maxCount
        }

        // Observe feed from repository and filter locally
        reviewRepo.observeFeed().observe(viewLifecycleOwner) { reviews ->
            val filtered = if (restaurantId.isNotBlank()) {
                reviews.filter { it.restaurantId == restaurantId }
            } else {
                reviews
            }
            reviewAdapter.submitList(filtered)
            updateRatingFromReviews(filtered)
        }

        // ViewModel for restaurant data
        val restaurantRepo = ServiceLocator.provideRestaurantRepository(requireContext())
        val factory = RestaurantViewModelFactory(restaurantRepo)
        val vm = ViewModelProvider(this, factory).get(RestaurantViewModel::class.java)

        if (restaurantId.isNotBlank()) {
            // Observe Room-backed LiveData for reactive updates
            vm.observeRestaurant(restaurantId).observe(viewLifecycleOwner) { restaurant ->
                if (restaurant != null) {
                    populateRestaurant(restaurant)
                }
            }

            // Also observe the direct fetch result as a fallback
            vm.restaurant.observe(viewLifecycleOwner) { restaurant ->
                if (restaurant != null) {
                    populateRestaurant(restaurant)
                }
            }

            // Kick off loading to ensure initial data exists in Room (fetches Firestore when needed)
            vm.loadRestaurant(restaurantId)
        } else {
            // fallback: no restaurantId provided, keep name from arg or placeholder
            if (restaurantName.isNullOrBlank()) {
                nameTv.text = getString(R.string.restaurant_name_placeholder)
            }
        }
    }
}
