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
import com.tasteclub.app.util.toRelativeString
import de.hdodenhof.circleimageview.CircleImageView

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

        fun bind(comment: Comment) {
            tvUserName.text = comment.userName
            tvTimestamp.text = comment.createdAt.toRelativeString()
            tvCommentText.text = comment.text

            // Load avatar with Picasso
            if (comment.userImageUrl.isNotBlank()) {
                try {
                    Picasso.get()
                        .load(comment.userImageUrl)
                        .placeholder(R.drawable.ic_user_placeholder)
                        .error(R.drawable.ic_user_placeholder)
                        .fit()
                        .centerCrop()
                        .into(ivAvatar)
                } catch (e: IllegalArgumentException) {
                    ivAvatar.setImageResource(R.drawable.ic_user_placeholder)
                }
            } else {
                ivAvatar.setImageResource(R.drawable.ic_user_placeholder)
            }

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

