package com.tasteclub.app.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.tasteclub.app.R
import com.tasteclub.app.data.model.Comment
import com.tasteclub.app.util.ServiceLocator
import com.tasteclub.app.util.toRelativeString
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentAdapter(
    private val currentUserId: String?,
    private val onDeleteClick: (Comment) -> Unit
) : ListAdapter<Comment, CommentAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: CircleImageView = itemView.findViewById(R.id.ivCommentAvatar)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val tvCommentText: TextView = itemView.findViewById(R.id.tvCommentText)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        private var currentJob: Job? = null

        fun bind(comment: Comment) {
            // Cancel any pending user resolution job
            currentJob?.cancel()

            // --- USER RESOLUTION ---
            tvUserName.text = "Loading..."
            ivAvatar.setImageResource(R.drawable.ic_user_placeholder)

            val context = itemView.context
            val authRepo = ServiceLocator.provideAuthRepository(context)

            currentJob = CoroutineScope(Dispatchers.Main).launch {
                val userInfo = withContext(Dispatchers.IO) {
                    authRepo.resolveUserDisplayInfo(comment.userId)
                }

                if (userInfo != null) {
                    tvUserName.text = userInfo.first
                    if (userInfo.second.isNotBlank()) {
                         try {
                            Picasso.get()
                                .load(userInfo.second)
                                .placeholder(R.drawable.ic_user_placeholder)
                                .error(R.drawable.ic_user_placeholder)
                                .fit()
                                .centerCrop()
                                .into(ivAvatar)
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                } else {
                    tvUserName.text = "Deleted User"
                }
            }

            // Timestamp & Text
            tvTimestamp.text = comment.createdAt.toRelativeString()
            tvCommentText.text = comment.text

            // Show delete button only for the current user's own comments
            if (comment.userId == currentUserId) {
                btnDelete.visibility = View.VISIBLE
                btnDelete.setOnClickListener { onDeleteClick(comment) }
            } else {
                btnDelete.visibility = View.GONE
                btnDelete.setOnClickListener(null)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Comment, newItem: Comment) =
            oldItem == newItem
    }
}
