package com.tasteclub.app.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.tasteclub.app.R
import com.tasteclub.app.data.model.Review
import com.tasteclub.app.databinding.ItemReviewCardBinding
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * ReviewAdapter - Adapter for displaying review cards in RecyclerView
 * Uses ListAdapter with DiffUtil for efficient updates
 */
class ReviewAdapter : ListAdapter<Review, ReviewAdapter.ReviewViewHolder>(ReviewDiffCallback()) {

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
                Picasso.get()
                    .load(review.userProfileImageUrl)
                    .placeholder(R.drawable.ic_user_placeholder)
                    .error(R.drawable.ic_user_placeholder)
                    .fit()
                    .centerCrop()
                    .into(userAvatarImageView)

                // Restaurant info
                restaurantNameTextView.text = review.restaurantName
                restaurantAddressTextView.text = review.restaurantAddress

                // Load restaurant image with Picasso
                if (review.imageUrl.isNotEmpty()) {
                    restaurantImageView.visibility = View.VISIBLE
                    Picasso.get()
                        .load(review.imageUrl)
                        .placeholder(R.drawable.image_placeholder)
                        .error(R.drawable.image_placeholder)
                        .resize(800, 0)
                        .centerCrop()
                        .into(restaurantImageView)
                } else {
                    // Hide image if not available
                    restaurantImageView.visibility = View.GONE
                }

                // Star rating - Set filled/outline stars based on rating
                setupStarRating(review.rating)

                // Review text
                reviewTextView.text = review.text
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

    /**
     * DiffUtil callback for efficient list updates
     */
    private class ReviewDiffCallback : DiffUtil.ItemCallback<Review>() {
        override fun areItemsTheSame(oldItem: Review, newItem: Review): Boolean {
            // Compare unique identifiers
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Review, newItem: Review): Boolean {
            // Compare all content
            return oldItem == newItem
        }
    }
}

