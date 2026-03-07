package com.tasteclub.app.ui.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.tasteclub.app.R

/**
 * FilterSortBottomSheet - Bottom sheet for filter and sort options on the Discover screen.
 */
class FilterSortBottomSheet : BottomSheetDialogFragment() {

    /** Callback to apply selected filters */
    var onApply: ((minRating: Int, minReviewCount: Int, sortOption: DiscoverViewModel.SortOption) -> Unit)? = null

    /** Callback to clear all filters */
    var onClear: (() -> Unit)? = null

    // Current state (set before showing)
    var initialMinRating: Int = 0
    var initialMinReviewCount: Int = 0
    var initialSortOption: DiscoverViewModel.SortOption = DiscoverViewModel.SortOption.RELEVANCE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_filter_sort, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Sort chips
        val chipRelevance: Chip = view.findViewById(R.id.chipRelevance)
        val chipBestRated: Chip = view.findViewById(R.id.chipBestRated)
        val chipMostReviews: Chip = view.findViewById(R.id.chipMostReviews)
        val chipNewest: Chip = view.findViewById(R.id.chipNewest)

        // Rating chips
        val chipRatingAny: Chip = view.findViewById(R.id.chipRatingAny)
        val chipRating2: Chip = view.findViewById(R.id.chipRating2)
        val chipRating3: Chip = view.findViewById(R.id.chipRating3)
        val chipRating4: Chip = view.findViewById(R.id.chipRating4)

        // Review count chips
        val chipCountAny: Chip = view.findViewById(R.id.chipCountAny)
        val chipCount5: Chip = view.findViewById(R.id.chipCount5)
        val chipCount10: Chip = view.findViewById(R.id.chipCount10)
        val chipCount25: Chip = view.findViewById(R.id.chipCount25)

        // Set initial state — sort
        when (initialSortOption) {
            DiscoverViewModel.SortOption.RELEVANCE -> chipRelevance.isChecked = true
            DiscoverViewModel.SortOption.BEST_RATED -> chipBestRated.isChecked = true
            DiscoverViewModel.SortOption.MOST_REVIEWS -> chipMostReviews.isChecked = true
            DiscoverViewModel.SortOption.NEWEST -> chipNewest.isChecked = true
        }

        // Set initial state — rating
        when (initialMinRating) {
            2 -> chipRating2.isChecked = true
            3 -> chipRating3.isChecked = true
            4 -> chipRating4.isChecked = true
            else -> chipRatingAny.isChecked = true
        }

        // Set initial state — review count
        when (initialMinReviewCount) {
            5 -> chipCount5.isChecked = true
            10 -> chipCount10.isChecked = true
            25 -> chipCount25.isChecked = true
            else -> chipCountAny.isChecked = true
        }

        // Apply button
        view.findViewById<View>(R.id.btnApplyFilters).setOnClickListener {
            val sort = when {
                chipBestRated.isChecked -> DiscoverViewModel.SortOption.BEST_RATED
                chipMostReviews.isChecked -> DiscoverViewModel.SortOption.MOST_REVIEWS
                chipNewest.isChecked -> DiscoverViewModel.SortOption.NEWEST
                else -> DiscoverViewModel.SortOption.RELEVANCE
            }

            val rating = when {
                chipRating4.isChecked -> 4
                chipRating3.isChecked -> 3
                chipRating2.isChecked -> 2
                else -> 0
            }

            val reviewCount = when {
                chipCount25.isChecked -> 25
                chipCount10.isChecked -> 10
                chipCount5.isChecked -> 5
                else -> 0
            }

            onApply?.invoke(rating, reviewCount, sort)
            dismiss()
        }

        // Clear button
        view.findViewById<View>(R.id.btnClearFilters).setOnClickListener {
            onClear?.invoke()
            dismiss()
        }
    }
}

