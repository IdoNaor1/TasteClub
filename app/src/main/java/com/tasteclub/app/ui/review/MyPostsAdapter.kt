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
import com.tasteclub.app.util.ServiceLocator
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MyPostsAdapter - RecyclerView adapter for displaying user's own reviews
 */
class MyPostsAdapter(
    private val currentUserId: String,
    private val onEditClick: (Review) -> Unit,
    private val onDeleteClick: (Review) -> Unit,
    private val onLikeClick: (Review) -> Unit,
    private val onRestaurantClick: ((restaurantId: String, restaurantName: String) -> Unit)? = null,
    private val onCommentClick: (Review) -> Unit = {}
) : ListAdapter<Review, MyPostsAdapter.MyPostViewHolder>(ReviewDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyPostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_post_card, parent, false)
        return MyPostViewHolder(view, currentUserId, onEditClick, onDeleteClick, onLikeClick, onRestaurantClick, onCommentClick)
    }

    override fun onBindViewHolder(holder: MyPostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MyPostViewHolder(
        itemView: View,
        private val currentUserId: String,
        private val onEditClick: (Review) -> Unit,
        private val onDeleteClick: (Review) -> Unit,
        private val onLikeClick: (Review) -> Unit,
        private val onRestaurantClick: ((restaurantId: String, restaurantName: String) -> Unit)? = null,
        private val onCommentClick: (Review) -> Unit = {}
    ) : RecyclerView.ViewHolder(itemView) {

        private val restaurantImageView: ImageView = itemView.findViewById(R.id.restaurantImageView)
        private val editFab: FloatingActionButton = itemView.findViewById(R.id.editFab)
        // restaurantNameTextView is now a TextView (was MaterialButton)
        private val restaurantNameTextView: TextView = itemView.findViewById(R.id.restaurantNameTextView)
        private val restaurantAddressTextView: TextView = itemView.findViewById(R.id.restaurantAddressTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val reviewTextView: TextView = itemView.findViewById(R.id.reviewTextView)
        private val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteButton)
        private val likeButton: ImageView = itemView.findViewById(R.id.likeButton)
        private val likeCountTextView: TextView = itemView.findViewById(R.id.likeCountTextView)
        private val commentButton: ImageView = itemView.findViewById(R.id.commentButton)
        private val tvCommentCount: TextView = itemView.findViewById(R.id.tvCommentCount)

        private val star1ImageView: ImageView = itemView.findViewById(R.id.star1ImageView)
        private val star2ImageView: ImageView = itemView.findViewById(R.id.star2ImageView)
        private val star3ImageView: ImageView = itemView.findViewById(R.id.star3ImageView)
        private val star4ImageView: ImageView = itemView.findViewById(R.id.star4ImageView)
        private val star5ImageView: ImageView = itemView.findViewById(R.id.star5ImageView)

        private var currentJob: kotlinx.coroutines.Job? = null

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(review: Review) {
            // Cancel pending jobs
            currentJob?.cancel()

            // Resolve User Info (even for my posts, to be consistent and safe)
            // Although "My Posts" implies "Me", data consistency is better if we fetch.
            // But for "My Posts" optimization, we KNOW the user is the current logged in user.
            // We can skip the async fetch and use the injected current user details if we passed them,
            // but the adapter only knows currentUserId string.
            // Let's use the same async pattern for consistency.

            val context = itemView.context
            val authRepo = ServiceLocator.provideAuthRepository(context)

            // Views might be missing in My Posts card
            val userNameTextView = itemView.findViewById<TextView>(R.id.userNameTextView)
            val userAvatarImageView = itemView.findViewById<ImageView>(R.id.userAvatarImageView)

            userNameTextView?.text = "Loading..."
            userAvatarImageView?.setImageResource(R.drawable.ic_user_placeholder)

            currentJob = CoroutineScope(Dispatchers.Main).launch {
                 val userInfo = withContext(Dispatchers.IO) {
                    authRepo.resolveUserDisplayInfo(review.userId)
                 }

                 if (userInfo != null) {
                    userNameTextView?.text = userInfo.first
                    if (userInfo.second.isNotBlank()) {
                        try {
                            userAvatarImageView?.let {
                                Picasso.get().load(userInfo.second)
                                 .placeholder(R.drawable.ic_user_placeholder)
                                 .error(R.drawable.ic_user_placeholder)
                                 .fit().centerCrop().into(it)
                            }
                        } catch(e: Exception) {}
                    }
                 } else {
                    userNameTextView?.text = "Deleted User"
                 }
            }


            restaurantNameTextView.text = review.restaurantName
            restaurantAddressTextView.text = review.restaurantAddress

            // Restaurant name click -> navigate to restaurant detail
            restaurantNameTextView.setOnClickListener {
                if (review.restaurantId.isNotBlank()) {
                    onRestaurantClick?.invoke(review.restaurantId, review.restaurantName)
                }
            }

            dateTextView.text = dateFormat.format(Date(review.createdAt))
            reviewTextView.text = review.text

            // Load food image
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

            // Star rating
            setStarRating(review.rating)

            editFab.setOnClickListener { onEditClick(review) }
            deleteButton.setOnClickListener { onDeleteClick(review) }

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

        private fun setStarRating(rating: Int) {
            val stars = listOf(star1ImageView, star2ImageView, star3ImageView, star4ImageView, star5ImageView)
            stars.forEachIndexed { index, star ->
                star.setImageResource(
                    if (index < rating) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                )
            }
        }
    }
}
