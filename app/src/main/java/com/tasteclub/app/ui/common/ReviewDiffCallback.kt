package com.tasteclub.app.ui.common

import androidx.recyclerview.widget.DiffUtil
import com.tasteclub.app.data.model.Review

/**
 * Shared DiffUtil callback for Review items
 * Used by both Feed and My Posts adapters for efficient RecyclerView updates
 */
class ReviewDiffCallback : DiffUtil.ItemCallback<Review>() {
    override fun areItemsTheSame(oldItem: Review, newItem: Review): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Review, newItem: Review): Boolean {
        return oldItem == newItem
    }
}

