package com.tasteclub.app.ui.restaurant

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.squareup.picasso.Picasso
import com.tasteclub.app.R
import com.tasteclub.app.data.model.Restaurant
import com.tasteclub.app.data.model.Review
import com.tasteclub.app.ui.comment.CommentsBottomSheetFragment
import com.tasteclub.app.ui.feed.ReviewAdapter
import com.tasteclub.app.util.ServiceLocator
import kotlinx.coroutines.launch
import java.util.Locale

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
        val headerPhotoIv: ShapeableImageView = view.findViewById(R.id.restaurant_header_photo)
        val photosTitle: TextView = view.findViewById(R.id.photos_title)
        val photosScroll: HorizontalScrollView = view.findViewById(R.id.photos_scroll)
        val photosContainer: LinearLayout = view.findViewById(R.id.photos_container)

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
        headerPhotoIv.visibility = View.GONE

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
        val commentRepo = ServiceLocator.provideCommentRepository(requireContext())
        val currentUserId = authRepository.currentUserId() ?: ""

        // Local patched list — holds commentCounts fetched from Firestore
        val displayedReviews = mutableListOf<Review>()

        lateinit var reviewAdapter: ReviewAdapter
        reviewAdapter = ReviewAdapter(
            currentUserId = currentUserId,
            onLikeClick = { review ->
                if (currentUserId.isNotBlank()) {
                    lifecycleScope.launch {
                        try {
                            reviewRepo.toggleLike(review.id, currentUserId)
                        } catch (_: Exception) { }
                    }
                }
            },
            onUserClick = { userId ->
                val bundle = Bundle().apply {
                    putString("userId", userId)
                }
                findNavController().navigate(R.id.action_restaurant_detail_to_other_profile, bundle)
            },
            onCommentClick = { review ->
                val sheet = CommentsBottomSheetFragment.newInstance(review.id)
                sheet.onCommentCountChanged = { reviewId, newCount ->
                    val idx = displayedReviews.indexOfFirst { it.id == reviewId }
                    if (idx != -1) {
                        displayedReviews[idx] = displayedReviews[idx].copy(commentCount = newCount)
                        reviewAdapter.submitList(displayedReviews.toList())
                    }
                }
                sheet.show(childFragmentManager, "comments_${review.id}")
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
            if (restaurant.photoUrl.isBlank()) {
                Picasso.get().cancelRequest(headerPhotoIv)
                headerPhotoIv.setImageDrawable(null)
                headerPhotoIv.visibility = View.GONE
            } else {
                headerPhotoIv.visibility = View.VISIBLE
                try {
                    Picasso.get()
                        .load(restaurant.photoUrl)
                        .placeholder(R.drawable.image_placeholder)
                        .error(R.drawable.image_placeholder)
                        .fit()
                        .centerCrop()
                        .into(headerPhotoIv)
                } catch (_: IllegalArgumentException) {
                    headerPhotoIv.setImageResource(R.drawable.image_placeholder)
                }
            }
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

            ratingScoreTv.text = String.format(Locale.getDefault(), "%.1f", avg)
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

        fun Int.dp(): Int = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            resources.displayMetrics
        ).toInt()

        fun renderReviewPhotos(reviews: List<Review>) {
            val photoUrls = reviews.mapNotNull { it.imageUrl.takeIf(String::isNotBlank) }

            photosContainer.removeAllViews()

            if (photoUrls.isEmpty()) {
                photosTitle.visibility = View.GONE
                photosScroll.visibility = View.GONE
                return
            }

            photosTitle.visibility = View.VISIBLE
            photosScroll.visibility = View.VISIBLE
            photosTitle.text = getString(R.string.photos_title_count, photoUrls.size)

            val itemWidth = 160.dp()
            val itemHeight = 110.dp()
            val itemSpacing = 12.dp()
            val cornerRadius = 12.dp().toFloat()

            photoUrls.forEachIndexed { index, photoUrl ->
                val imageView = ShapeableImageView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(itemWidth, itemHeight).apply {
                        if (index < photoUrls.lastIndex) {
                            marginEnd = itemSpacing
                        }
                    }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    contentDescription = getString(
                        R.string.restaurant_photo_content_description,
                        index + 1
                    )
                    shapeAppearanceModel = shapeAppearanceModel
                        .toBuilder()
                        .setAllCornerSizes(cornerRadius)
                        .build()
                }

                try {
                    Picasso.get()
                        .load(photoUrl)
                        .placeholder(R.drawable.image_placeholder)
                        .error(R.drawable.image_placeholder)
                        .resize(800, 0)
                        .centerCrop()
                        .into(imageView)
                } catch (_: IllegalArgumentException) {
                    imageView.setImageResource(R.drawable.image_placeholder)
                }

                photosContainer.addView(imageView)
            }
        }

        if (restaurantId.isNotBlank()) {
            // Observe only this restaurant's reviews from Room
            reviewRepo.observeReviewsByRestaurant(restaurantId).observe(viewLifecycleOwner) { reviews ->
                // Preserve any commentCounts already patched in displayedReviews
                val knownCounts = displayedReviews.associate { it.id to it.commentCount }
                val merged = reviews.map { r ->
                    val known = knownCounts[r.id] ?: 0
                    if (known > 0) r.copy(commentCount = known) else r
                }
                displayedReviews.clear()
                displayedReviews.addAll(merged)
                reviewAdapter.submitList(displayedReviews.toList())
                updateRatingFromReviews(reviews)
                renderReviewPhotos(reviews)

                // Fetch real comment counts in background
                if (merged.isNotEmpty()) {
                    lifecycleScope.launch {
                        try {
                            val counts = commentRepo.getCommentCountsBatch(merged.map { it.id })
                            var changed = false
                            counts.forEach { (reviewId, count) ->
                                val idx = displayedReviews.indexOfFirst { it.id == reviewId }
                                if (idx != -1 && displayedReviews[idx].commentCount != count) {
                                    displayedReviews[idx] = displayedReviews[idx].copy(commentCount = count)
                                    changed = true
                                }
                            }
                            if (changed) reviewAdapter.submitList(displayedReviews.toList())
                        } catch (_: Exception) { }
                    }
                }
            }

            lifecycleScope.launch {
                try {
                    reviewRepo.refreshRestaurantReviewsPage(
                        restaurantId = restaurantId,
                        limit = 100
                    )
                } catch (_: Exception) {
                    // Keep showing cached Room data if remote refresh fails.
                }
            }
        } else {
            renderReviewPhotos(emptyList())
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
