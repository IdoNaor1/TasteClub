package com.tasteclub.app.ui.review

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.widget.PlaceAutocompleteActivity
import com.tasteclub.app.data.model.Restaurant
import com.tasteclub.app.data.remote.places.PlacesService
import com.tasteclub.app.databinding.FragmentCreateReviewBinding
import com.tasteclub.app.util.ServiceLocator
import kotlinx.coroutines.launch

/**
 * CreateReviewFragment - Create a new review
 */
class CreateReviewFragment : Fragment() {

    private var _binding: FragmentCreateReviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReviewViewModel by viewModels {
        ReviewViewModelFactory(ServiceLocator.provideReviewRepository(requireContext()))
    }

    private lateinit var placesService: PlacesService
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var sessionToken: AutocompleteSessionToken? = null
    private var currentLocation: LatLng? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCurrentLocation()
        }
    }

    private val autocompleteLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult

                val prediction = placesService.getPredictionFromIntent(data) ?: return@registerForActivityResult

                val tokenFromIntent = placesService.getSessionTokenFromIntent(data)

                val placeId = prediction.placeId

                // Launch coroutine to fetch place details
                viewModel.viewModelScope.launch {
                    val place = placesService.getPlaceDetails(placeId, tokenFromIntent)
                    if (place != null) {
                        val restaurant = Restaurant(
                            id = place.id ?: "",
                            name = place.displayName ?: "",
                            address = place.formattedAddress ?: "",
                            //photoUrl = place.photoMetadatas?.firstOrNull()?.let { it.photoReference } ?: "",
                            categories = listOf(place.primaryTypeDisplayName ?: "")
                        )
                        viewModel.setSelectedRestaurant(restaurant)
                    } else {
                        Toast.makeText(requireContext(), "Fetch details failed", Toast.LENGTH_SHORT).show()
                    }
                }

            } else if (result.resultCode == PlaceAutocompleteActivity.RESULT_ERROR) {
                val data = result.data ?: return@registerForActivityResult
                val status = placesService.getResultStatusFromIntent(data)
                Toast.makeText(
                    requireContext(),
                    status?.statusMessage ?: "Autocomplete error",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        placesService = ServiceLocator.providePlacesService(requireContext())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        getCurrentLocation()
        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        binding.backButton.setOnClickListener {
            // Navigate back
            requireActivity().onBackPressed()
        }

        binding.changeText.setOnClickListener {
            openAutocomplete()
        }

        binding.ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            viewModel.setRating(rating)
        }

        binding.reviewEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                viewModel.setReviewText(binding.reviewEditText.text.toString())
            }
        }

        binding.postReviewButton.setOnClickListener {
            viewModel.createReview()
        }

        // TODO: Add photos functionality
    }

    private fun setupObservers() {
        viewModel.selectedRestaurant.observe(viewLifecycleOwner, Observer { restaurant ->
            if (restaurant != null) {
                binding.restaurantName.text = restaurant.name
                binding.restaurantAddress.text = restaurant.address
                binding.changeText.text = getString(com.tasteclub.app.R.string.change_restaurant)
            } else {
                binding.restaurantName.text = getString(com.tasteclub.app.R.string.restaurant_name_placeholder)
                binding.restaurantAddress.text = getString(com.tasteclub.app.R.string.restaurant_address_placeholder)
                binding.changeText.text = getString(com.tasteclub.app.R.string.select_restaurant)
            }
        })

        viewModel.rating.observe(viewLifecycleOwner, Observer { rating ->
            binding.ratingBar.rating = rating
        })

        viewModel.reviewText.observe(viewLifecycleOwner, Observer { text ->
            if (binding.reviewEditText.text.toString() != text) {
                binding.reviewEditText.setText(text)
            }
        })

        viewModel.isCreating.observe(viewLifecycleOwner, Observer { isCreating ->
            binding.postReviewButton.isEnabled = !isCreating
            binding.postReviewButton.text = if (isCreating) "Posting..." else getString(com.tasteclub.app.R.string.post_review)
        })

        viewModel.createResult.observe(viewLifecycleOwner, Observer { result ->
            result?.let {
                if (it.isSuccess) {
                    Toast.makeText(requireContext(), "Review posted successfully!", Toast.LENGTH_SHORT).show()
                    // Navigate back or to feed
                    requireActivity().onBackPressed()
                } else {
                    Toast.makeText(requireContext(), "Failed to post review: ${it.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
                viewModel.resetCreateResult()
            }
        })
    }

    private fun openAutocomplete() {
        sessionToken = placesService.createSessionToken()
        val intent = placesService.createAutocompleteIntent(sessionToken, currentLocation)
        autocompleteLauncher.launch(intent)
    }

    private fun getCurrentLocation() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        currentLocation = LatLng(location.latitude, location.longitude)
                        // You can now use the currentLocation variable
                    }
                }
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
