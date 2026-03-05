package com.tasteclub.app.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.tasteclub.app.R
import com.tasteclub.app.data.model.Review
import com.tasteclub.app.databinding.ItemReviewCardBinding
import com.tasteclub.app.ui.common.ReviewDiffCallback
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * ReviewAdapter - Adapter for displaying review cards in RecyclerView
 * Uses ListAdapter with shared ReviewDiffCallback for efficient updates
 */
class ReviewAdapter(
    private val currentUserId: String,
    private val onLikeClick: (Review) -> Unit,
    private val onRestaurantClick: ((restaurantId: String, restaurantName: String) -> Unit)? = null
) : ListAdapter<Review, ReviewAdapter.ReviewViewHolder>(ReviewDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for Review items
     */
    inner class ReviewViewHolder(
        private val binding: ItemReviewCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // Star ImageViews array for easy iteration
        private val starViews: List<ImageView> by lazy {
            listOf(
                binding.star1ImageView,
                binding.star2ImageView,
                binding.star3ImageView,
                binding.star4ImageView,
                binding.star5ImageView
            )
        }

        fun bind(review: Review) {
            with(binding) {
                // User info
                userNameTextView.text = review.userName
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                dateTextView.text = dateFormat.format(review.createdAt)

                // Load user avatar with Picasso
                if (!review.userProfileImageUrl.isNullOrBlank()) {
                    try {
                        Picasso.get()
                            .load(review.userProfileImageUrl)
                            .placeholder(R.drawable.ic_user_placeholder)
                            .error(R.drawable.ic_user_placeholder)
                            .fit()
                            .centerCrop()
                            .into(userAvatarImageView)
                    } catch (e: IllegalArgumentException) {
                        // In case Picasso rejects the path, fallback to placeholder
                        userAvatarImageView.setImageResource(R.drawable.ic_user_placeholder)
                    }
                } else {
                    userAvatarImageView.setImageResource(R.drawable.ic_user_placeholder)
                }

                // Restaurant info
                restaurantNameTextView.text = review.restaurantName
                restaurantAddressTextView.text = review.restaurantAddress

                // Restaurant name click -> navigate to restaurant detail
                restaurantNameTextView.setOnClickListener {
                    if (review.restaurantId.isNotBlank()) {
                        onRestaurantClick?.invoke(review.restaurantId, review.restaurantName)
                    }
                }

                // Load restaurant image with Picasso
                if (!review.imageUrl.isNullOrBlank()) {
                    restaurantImageView.visibility = View.VISIBLE
                    try {
                        Picasso.get()
                            .load(review.imageUrl)
                            .placeholder(R.drawable.image_placeholder)
                            .error(R.drawable.image_placeholder)
                            .resize(800, 0)
                            .centerCrop()
                            .into(restaurantImageView)
                    } catch (e: IllegalArgumentException) {
                        // If path invalid, show placeholder instead of crashing
                        restaurantImageView.setImageResource(R.drawable.image_placeholder)
                    }
                } else {
                    // Hide image if not available
                    restaurantImageView.visibility = View.GONE
                }

                // Star rating - Set filled/outline stars based on rating
                setupStarRating(review.rating)

                // Review text
                reviewTextView.text = review.text

                // Like button
                val isLiked = review.likedBy.contains(currentUserId)
                likeButton.setImageResource(
                    if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                )
                likeCountTextView.text = review.likedBy.size.toString()
                likeButton.setOnClickListener { onLikeClick(review) }
            }
        }

        /**
         * Setup star rating display based on rating value (1-5)
         */
        private fun setupStarRating(rating: Int) {
            starViews.forEachIndexed { index, imageView ->
                if (index < rating) {
                    // Filled star
                    imageView.setImageResource(R.drawable.ic_star_filled)
                } else {
                    // Outline star
                    imageView.setImageResource(R.drawable.ic_star_outline)
                }
            }
        }
    }
}
