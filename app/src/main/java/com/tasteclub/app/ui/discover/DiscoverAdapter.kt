package com.tasteclub.app.ui.discover

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.tasteclub.app.R
import com.tasteclub.app.data.model.Restaurant
import com.tasteclub.app.data.model.Review
import com.tasteclub.app.data.model.User
import com.tasteclub.app.databinding.ItemDiscoverRestaurantBinding
import com.tasteclub.app.databinding.ItemDiscoverReviewBinding
import com.tasteclub.app.databinding.ItemDiscoverUserBinding
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * DiscoverAdapter - A multi-type RecyclerView adapter for the Discover screen.
 * Handles section headers, restaurant cards, user cards, and review cards.
 */
class DiscoverAdapter(
    private val currentUserId: String,
    private val onRestaurantClick: (restaurantId: String, restaurantName: String) -> Unit,
    private val onUserClick: (userId: String) -> Unit,
    private val onLikeClick: (review: Review) -> Unit,
    private val onCommentClick: (review: Review) -> Unit
) : ListAdapter<DiscoverAdapter.DiscoverItem, RecyclerView.ViewHolder>(DiscoverDiffCallback()) {

    companion object {
        private const val TYPE_SECTION_HEADER = 0
        private const val TYPE_RESTAURANT = 1
        private const val TYPE_USER = 2
        private const val TYPE_REVIEW = 3
    }

    /**
     * Sealed class representing all possible item types in the Discover list.
     */
    sealed class DiscoverItem {
        data class SectionHeader(val title: String) : DiscoverItem()
        data class RestaurantItem(val restaurant: Restaurant) : DiscoverItem()
        data class UserItem(val user: User) : DiscoverItem()
        data class ReviewItem(val review: Review) : DiscoverItem()
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DiscoverItem.SectionHeader -> TYPE_SECTION_HEADER
            is DiscoverItem.RestaurantItem -> TYPE_RESTAURANT
            is DiscoverItem.UserItem -> TYPE_USER
            is DiscoverItem.ReviewItem -> TYPE_REVIEW
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SECTION_HEADER -> {
                val view = inflater.inflate(R.layout.item_section_header, parent, false)
                SectionHeaderViewHolder(view)
            }
            TYPE_RESTAURANT -> {
                val binding = ItemDiscoverRestaurantBinding.inflate(inflater, parent, false)
                RestaurantViewHolder(binding)
            }
            TYPE_USER -> {
                val binding = ItemDiscoverUserBinding.inflate(inflater, parent, false)
                UserViewHolder(binding)
            }
            TYPE_REVIEW -> {
                val binding = ItemDiscoverReviewBinding.inflate(inflater, parent, false)
                ReviewViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is DiscoverItem.SectionHeader -> (holder as SectionHeaderViewHolder).bind(item)
            is DiscoverItem.RestaurantItem -> (holder as RestaurantViewHolder).bind(item.restaurant)
            is DiscoverItem.UserItem -> (holder as UserViewHolder).bind(item.user)
            is DiscoverItem.ReviewItem -> (holder as ReviewViewHolder).bind(item.review)
        }
    }

    // ---- ViewHolders ----

    inner class SectionHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleView: TextView = itemView.findViewById(R.id.sectionTitle)

        fun bind(item: DiscoverItem.SectionHeader) {
            titleView.text = item.title
        }
    }

    inner class RestaurantViewHolder(
        private val binding: ItemDiscoverRestaurantBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(restaurant: Restaurant) {
            binding.restaurantName.text = restaurant.name
            binding.restaurantAddress.text = restaurant.address

            // Cuisine type tag
            if (restaurant.primaryType.isNotBlank()) {
                binding.cuisineTag.visibility = View.VISIBLE
                binding.cuisineTag.text = restaurant.primaryType
            } else {
                binding.cuisineTag.visibility = View.GONE
            }

            // Rating
            val rating = restaurant.averageRating
            binding.ratingText.text = if (rating > 0) String.format("%.1f", rating) else "—"

            // Restaurant photo
            loadImage(restaurant.photoUrl, binding.restaurantImage, R.drawable.image_placeholder)

            binding.root.setOnClickListener {
                onRestaurantClick(restaurant.id, restaurant.name)
            }
        }
    }

    inner class UserViewHolder(
        private val binding: ItemDiscoverUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.userName.text = user.userName

            if (user.bio.isNotBlank()) {
                binding.userBio.text = user.bio
                binding.userBio.visibility = View.VISIBLE
            } else {
                binding.userBio.visibility = View.GONE
            }

            // User avatar
            loadImage(user.profileImageUrl, binding.userAvatar, R.drawable.ic_user_placeholder)

            binding.root.setOnClickListener {
                onUserClick(user.uid)
            }
        }
    }

    inner class ReviewViewHolder(
        private val binding: ItemDiscoverReviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(review: Review) {
            binding.reviewUserName.text = review.userName

            // Date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            binding.reviewDate.text = dateFormat.format(review.createdAt)

            // Review text
            if (review.text.isNotBlank()) {
                binding.reviewText.visibility = View.VISIBLE
                binding.reviewText.text = review.text
            } else {
                binding.reviewText.visibility = View.GONE
            }

            // Food image
            if (review.imageUrl.isNotBlank()) {
                binding.reviewImage.visibility = View.VISIBLE
                loadImage(review.imageUrl, binding.reviewImage, R.drawable.image_placeholder)
            } else {
                binding.reviewImage.visibility = View.GONE
            }

            // Restaurant info
            binding.reviewRestaurantName.text = review.restaurantName

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

            // Restaurant name click -> navigate to restaurant detail
            binding.reviewRestaurantName.setOnClickListener {
                if (review.restaurantId.isNotBlank()) {
                    onRestaurantClick(review.restaurantId, review.restaurantName)
                }
            }

            // User avatar
            loadImage(review.userProfileImageUrl, binding.reviewUserAvatar, R.drawable.ic_user_placeholder)

            // Username / avatar click -> navigate to user profile
            val userClickListener = View.OnClickListener {
                if (review.userId.isNotBlank()) {
                    onUserClick(review.userId)
                }
            }
            binding.reviewUserName.setOnClickListener(userClickListener)
            binding.reviewUserAvatar.setOnClickListener(userClickListener)

            // Likes
            val isLiked = review.likedBy.contains(currentUserId)
            binding.likeCount.text = review.likedBy.size.toString()
            binding.likeIcon.setImageResource(
                if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )
            binding.likeIcon.setOnClickListener { onLikeClick(review) }

            // Comments
            val hasComments = review.commentCount > 0
            binding.commentCount.text = review.commentCount.toString()
            binding.commentIcon.setImageResource(
                if (hasComments) R.drawable.ic_comment else R.drawable.ic_comment_outline
            )
            binding.commentIcon.setOnClickListener { onCommentClick(review) }
            binding.commentCount.setOnClickListener { onCommentClick(review) }

            // Review card itself is not clickable
            binding.root.isClickable = false
            binding.root.isFocusable = false
        }
    }

    // ---- Helpers ----

    private fun loadImage(url: String?, imageView: android.widget.ImageView, placeholder: Int) {
        if (!url.isNullOrBlank()) {
            try {
                Picasso.get()
                    .load(url)
                    .placeholder(placeholder)
                    .error(placeholder)
                    .fit()
                    .centerCrop()
                    .into(imageView)
            } catch (e: IllegalArgumentException) {
                imageView.setImageResource(placeholder)
            }
        } else {
            imageView.setImageResource(placeholder)
        }
    }

    // ---- DiffCallback ----

    class DiscoverDiffCallback : DiffUtil.ItemCallback<DiscoverItem>() {
        override fun areItemsTheSame(oldItem: DiscoverItem, newItem: DiscoverItem): Boolean {
            return when {
                oldItem is DiscoverItem.SectionHeader && newItem is DiscoverItem.SectionHeader ->
                    oldItem.title == newItem.title
                oldItem is DiscoverItem.RestaurantItem && newItem is DiscoverItem.RestaurantItem ->
                    oldItem.restaurant.id == newItem.restaurant.id
                oldItem is DiscoverItem.UserItem && newItem is DiscoverItem.UserItem ->
                    oldItem.user.uid == newItem.user.uid
                oldItem is DiscoverItem.ReviewItem && newItem is DiscoverItem.ReviewItem ->
                    oldItem.review.id == newItem.review.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: DiscoverItem, newItem: DiscoverItem): Boolean {
            return oldItem == newItem
        }
    }
}
