package com.tasteclub.app.ui.review

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.squareup.picasso.Picasso
import com.tasteclub.app.R
import com.tasteclub.app.data.model.Review
import com.tasteclub.app.ui.common.ReviewDiffCallback
import java.text.SimpleDateFormat
import java.util.*

/**
 * MyPostsAdapter - RecyclerView adapter for displaying user's own reviews
 * Uses shared Review data model and ReviewDiffCallback from common package
 *
 * Key differences from Feed:
 * - Uses item_my_post_card layout (no user header, has edit/delete buttons)
 * - Full date format instead of relative time
 * - Edit and delete click handlers
 */
class MyPostsAdapter(
    private val currentUserId: String,
    private val onEditClick: (Review) -> Unit,
    private val onDeleteClick: (Review) -> Unit,
    private val onLikeClick: (Review) -> Unit,
    private val onRestaurantClick: ((restaurantId: String, restaurantName: String) -> Unit)? = null
) : ListAdapter<Review, MyPostsAdapter.MyPostViewHolder>(ReviewDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyPostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_post_card, parent, false)
        return MyPostViewHolder(view, currentUserId, onEditClick, onDeleteClick, onLikeClick, onRestaurantClick)
    }

    override fun onBindViewHolder(holder: MyPostViewHolder, position: Int) {
        val review = getItem(position)
        holder.bind(review)
    }

    /**
     * ViewHolder for My Post items
     */
    class MyPostViewHolder(
        itemView: View,
        private val currentUserId: String,
        private val onEditClick: (Review) -> Unit,
        private val onDeleteClick: (Review) -> Unit,
        private val onLikeClick: (Review) -> Unit,
        private val onRestaurantClick: ((restaurantId: String, restaurantName: String) -> Unit)? = null
    ) : RecyclerView.ViewHolder(itemView) {

        private val restaurantImageView: ImageView = itemView.findViewById(R.id.restaurantImageView)
        private val editFab: FloatingActionButton = itemView.findViewById(R.id.editFab)
        private val restaurantNameTextView: MaterialButton = itemView.findViewById(R.id.restaurantNameTextView)
        private val restaurantAddressTextView: TextView = itemView.findViewById(R.id.restaurantAddressTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val reviewTextView: TextView = itemView.findViewById(R.id.reviewTextView)
        private val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteButton)
        private val likeButton: ImageView = itemView.findViewById(R.id.likeButton)
        private val likeCountTextView: TextView = itemView.findViewById(R.id.likeCountTextView)

        private val star1ImageView: ImageView = itemView.findViewById(R.id.star1ImageView)
        private val star2ImageView: ImageView = itemView.findViewById(R.id.star2ImageView)
        private val star3ImageView: ImageView = itemView.findViewById(R.id.star3ImageView)
        private val star4ImageView: ImageView = itemView.findViewById(R.id.star4ImageView)
        private val star5ImageView: ImageView = itemView.findViewById(R.id.star5ImageView)

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(review: Review) {
            // Set restaurant details
            restaurantNameTextView.text = review.restaurantName
            restaurantAddressTextView.text = review.restaurantAddress

            // Restaurant name click -> navigate to restaurant detail
            restaurantNameTextView.setOnClickListener {
                if (review.restaurantId.isNotBlank()) {
                    onRestaurantClick?.invoke(review.restaurantId, review.restaurantName)
                }
            }

            // Format date as "Jan 15, 2026"
            val date = Date(review.createdAt)
            dateTextView.text = dateFormat.format(date)

            // Set review text
            reviewTextView.text = review.text

            // Load restaurant image
            if (review.imageUrl.isNotBlank()) {
                Picasso.get()
                    .load(review.imageUrl)
                    .placeholder(R.drawable.image_placeholder)
                    .error(R.drawable.image_placeholder)
                    .fit()
                    .centerCrop()
                    .into(restaurantImageView)
            } else {
                restaurantImageView.setImageResource(R.drawable.image_placeholder)
            }

            // Set star rating
            setStarRating(review.rating)

            // Setup click listeners
            editFab.setOnClickListener {
                onEditClick(review)
            }

            deleteButton.setOnClickListener {
                onDeleteClick(review)
            }

            // Like button
            val isLiked = review.likedBy.contains(currentUserId)
            likeButton.setImageResource(
                if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )
            likeCountTextView.text = review.likedBy.size.toString()
            likeButton.setOnClickListener { onLikeClick(review) }
        }

        /**
         * Update star rating display based on rating value
         */
        private fun setStarRating(rating: Int) {
            val stars = listOf(
                star1ImageView,
                star2ImageView,
                star3ImageView,
                star4ImageView,
                star5ImageView
            )

            stars.forEachIndexed { index, starImageView ->
                if (index < rating) {
                    starImageView.setImageResource(R.drawable.ic_star_filled)
                } else {
                    starImageView.setImageResource(R.drawable.ic_star_outline)
                }
            }
        }
    }
}
