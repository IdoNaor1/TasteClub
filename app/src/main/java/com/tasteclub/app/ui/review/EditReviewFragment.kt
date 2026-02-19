package com.tasteclub.app.ui.review

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso
import com.tasteclub.app.R
import com.tasteclub.app.data.model.Review
import com.tasteclub.app.data.remote.firebase.FirestoreSource
import com.tasteclub.app.util.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * EditReviewFragment - Edit an existing review
 */
class EditReviewFragment : Fragment() {

    // Views
    private lateinit var restaurantNameTextView: TextView
    private lateinit var restaurantAddressTextView: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var reviewEditText: TextInputEditText
    private lateinit var selectedPhotoImageView: ImageView
    private lateinit var addPhotosCard: View
    private lateinit var updateReviewButton: View
    private lateinit var deleteReviewButton: View

    // Repositories/services
    private val reviewRepository by lazy { ServiceLocator.provideReviewRepository(requireContext()) }

    // Local state
    private var currentReview: Review? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_review, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupListeners()

        // Read nav arg: reviewId
        val reviewId = arguments?.getString("reviewId")
        if (reviewId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Missing review id", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        // Load review from remote (Firestore) then populate UI
        loadReview(reviewId)
    }

    private fun bindViews(root: View) {
        restaurantNameTextView = root.findViewById(R.id.restaurantName)
        restaurantAddressTextView = root.findViewById(R.id.restaurantAddress)
        ratingBar = root.findViewById(R.id.ratingBar)
        reviewEditText = root.findViewById(R.id.reviewEditText)
        selectedPhotoImageView = root.findViewById(R.id.selectedPhotoImageView)
        addPhotosCard = root.findViewById(R.id.addPhotosCard)
        updateReviewButton = root.findViewById(R.id.updateReviewButton)
        deleteReviewButton = root.findViewById(R.id.deleteReviewButton)
    }

    private fun setupListeners() {
        // Enable/disable update button based on simple validation
        val validator = {
            val ratingValid = ratingBar.rating >= 1f
            val textValid = !reviewEditText.text.isNullOrBlank()
            updateReviewButton.isEnabled = ratingValid && textValid
        }

        ratingBar.setOnRatingBarChangeListener { _, _, _ -> validator() }

        reviewEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { validator() }
            override fun afterTextChanged(s: Editable?) {}
        })

        updateReviewButton.setOnClickListener {
            val review = currentReview
            if (review == null) {
                Toast.makeText(requireContext(), "No review loaded", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            performUpdate(review)
        }

        deleteReviewButton.setOnClickListener {
            val review = currentReview
            if (review == null) {
                Toast.makeText(requireContext(), "No review loaded", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.delete_review_title))
                .setMessage(getString(R.string.delete_review_message))
                .setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                    dialog.dismiss()
                    performDelete(review.id)
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
                .show()
        }

        // Photo selection not implemented here; keep existing behavior (tapping could be wired later)
    }

    private fun loadReview(reviewId: String) {
        lifecycleScope.launch {
            try {
                val firestore = FirestoreSource()
                val review = withContext(Dispatchers.IO) { firestore.getReview(reviewId) }
                if (review == null) {
                    Toast.makeText(requireContext(), "Review not found", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                    return@launch
                }

                currentReview = review

                // Populate UI on main thread
                restaurantNameTextView.text = review.restaurantName
                restaurantAddressTextView.text = review.restaurantAddress
                ratingBar.rating = review.rating.toFloat()
                reviewEditText.setText(review.text)

                if (review.imageUrl.isNotBlank()) {
                    selectedPhotoImageView.visibility = View.VISIBLE
                    Picasso.get()
                        .load(review.imageUrl)
                        .placeholder(R.drawable.image_placeholder)
                        .error(R.drawable.image_placeholder)
                        .fit()
                        .centerCrop()
                        .into(selectedPhotoImageView)
                } else {
                    selectedPhotoImageView.visibility = View.GONE
                }

                // Ensure validation state updates
                updateReviewButton.isEnabled = ratingBar.rating >= 1f && !reviewEditText.text.isNullOrBlank()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load review: ${e.message}", Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
            }
        }
    }

    private fun performUpdate(existing: Review) {
        val newRating = ratingBar.rating.toInt()
        val newText = reviewEditText.text?.toString() ?: ""

        // Build updated review: keep user and restaurant fields
        val updated = existing.copy(
            rating = newRating,
            text = newText
            // imageUrl left unchanged; image upload flow not implemented here
        )

        updateReviewButton.isEnabled = false
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    reviewRepository.upsertReview(updated, null)
                }
                Toast.makeText(requireContext(), "Review updated", Toast.LENGTH_SHORT).show()
                // Navigate back to previous screen
                findNavController().popBackStack()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to update review: ${e.message}", Toast.LENGTH_LONG).show()
                updateReviewButton.isEnabled = true
            }
        }
    }

    private fun performDelete(reviewId: String) {
        deleteReviewButton.isEnabled = false
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    reviewRepository.deleteReview(reviewId)
                }
                Toast.makeText(requireContext(), getString(R.string.review_deleted), Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to delete review: ${e.message}", Toast.LENGTH_LONG).show()
                deleteReviewButton.isEnabled = true
            }
        }
    }
}
