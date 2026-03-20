package com.tasteclub.app.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.tasteclub.app.R
import com.tasteclub.app.data.model.Review
import com.tasteclub.app.databinding.ItemReviewCardBinding
import com.tasteclub.app.ui.common.ReviewDiffCallback
import com.tasteclub.app.data.repository.AuthRepository
import com.tasteclub.app.util.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * ReviewAdapter - Adapter for displaying review cards in RecyclerView
 * Uses ListAdapter with shared ReviewDiffCallback for efficient updates
 */
class ReviewAdapter(
    private val currentUserId: String,
    private val onLikeClick: (Review) -> Unit,
    private val onRestaurantClick: ((restaurantId: String, restaurantName: String) -> Unit)? = null,
    private val onUserClick: ((userId: String) -> Unit)? = null,
    private val onCommentClick: (Review) -> Unit = {}
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

        private var currentJob: kotlinx.coroutines.Job? = null

        fun bind(review: Review) {
            // Cancel any pending user resolution for recycled view
            currentJob?.cancel()

            with(binding) {
                val context = root.context
                
                // --- USER IDENTITY RESOLUTION ---
                // Reset to placeholder state first
                userNameTextView.text = "Loading..."
                userAvatarImageView.setImageResource(R.drawable.ic_user_placeholder)

                if (review.userId.isNotBlank()) {
                    val authRepo = ServiceLocator.provideAuthRepository(context)
                    
                    // Use View's lifecycle scope if possible, or create a scope
                    currentJob = CoroutineScope(Dispatchers.Main).launch {
                        val userInfo = withContext(Dispatchers.IO) {
                            authRepo.resolveUserDisplayInfo(review.userId)
                        }

                        if (userInfo != null) {
                            val (name, photoUrl) = userInfo
                            userNameTextView.text = name
                            
                            if (photoUrl.isNotBlank()) {
                                try {
                                    Picasso.get()
                                        .load(photoUrl)
                                        .placeholder(R.drawable.ic_user_placeholder)
                                        .error(R.drawable.ic_user_placeholder)
                                        .fit()
                                        .centerCrop()
                                        .into(userAvatarImageView)
                                } catch (e: Exception) {
                                    // ignore
                                }
                            }
                        } else {
                            // User deleted or not found
                            userNameTextView.text = "Deleted User"
                            userAvatarImageView.setImageResource(R.drawable.ic_user_placeholder)
                        }
                    }
                }
                
                // Existing date logic
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                dateTextView.text = dateFormat.format(review.createdAt)

                // Username / avatar click -> navigate to user profile
                val userClickListener = View.OnClickListener {
                    if (review.userId.isNotBlank()) {
                        onUserClick?.invoke(review.userId)
                    }
                }
                userNameTextView.setOnClickListener(userClickListener)
                userAvatarImageView.setOnClickListener(userClickListener)

                // Food image (ShapeableImageView — hidden when no image)
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
                        restaurantImageView.setImageResource(R.drawable.image_placeholder)
                    }
                } else {
                    restaurantImageView.visibility = View.GONE
                }

                // Review text
                reviewTextView.text = review.text

                // Restaurant info
                restaurantNameTextView.text = review.restaurantName
                restaurantAddressTextView.text = review.restaurantAddress

                // Restaurant name click -> navigate to restaurant detail
                restaurantNameTextView.setOnClickListener {
                    if (review.restaurantId.isNotBlank()) {
                        onRestaurantClick?.invoke(review.restaurantId, review.restaurantName)
                    }
                }

                // 5-star rating
                val starViews = listOf(
                    binding.star1ImageView,
                    binding.star2ImageView,
                    binding.star3ImageView,
                    binding.star4ImageView,
                    binding.star5ImageView
                )
                starViews.forEachIndexed { index, star ->
                    star.setImageResource(
                        if (index < review.rating) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                    )
                }

                // Like button
                val isLiked = review.likedBy.contains(currentUserId)
                likeButton.setImageResource(
                    if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                )
                likeCountTextView.text = review.likedBy.size.toString()
                likeButton.setOnClickListener { onLikeClick(review) }

                // Comment button
                val hasComments = review.commentCount > 0
                commentButton.setImageResource(
                    if (hasComments) R.drawable.ic_comment else R.drawable.ic_comment_outline
                )
                tvCommentCount.text = review.commentCount.toString()
                commentButton.setOnClickListener { onCommentClick(review) }
                tvCommentCount.setOnClickListener { onCommentClick(review) }
            }
        }
    }
}
