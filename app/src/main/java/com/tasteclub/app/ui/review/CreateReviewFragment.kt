package com.tasteclub.app.ui.review

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.widget.PlaceAutocompleteActivity
import com.squareup.picasso.Picasso
import com.tasteclub.app.data.remote.places.PlacesService
import com.tasteclub.app.databinding.FragmentCreateReviewBinding
import com.tasteclub.app.util.ServiceLocator
import java.io.IOException

/**
 * CreateReviewFragment - Create a new review
 */
class CreateReviewFragment : Fragment() {

    private var _binding: FragmentCreateReviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReviewViewModel by viewModels {
        ReviewViewModelFactory(
            ServiceLocator.provideReviewRepository(requireContext()),
            ServiceLocator.provideRestaurantRepository(requireContext()),
            ServiceLocator.providePlacesService(requireContext()),
            ServiceLocator.provideAuthRepository(requireContext())
        )
    }

    private lateinit var placesService: PlacesService
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var sessionToken: AutocompleteSessionToken? = null
    private var currentLocation: LatLng? = null

    // Store selected image bitmap (nullable)
    private var selectedImageBitmap: Bitmap? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCurrentLocation()
        }
    }

    // Camera permission launcher for taking photo
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_LONG).show()
        }
    }

    // Launch camera to get a Bitmap preview
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { onImageSelected(it) }
    }

    // Pick image from gallery
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleImageUri(it) }
    }

    private val autocompleteLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult

                val prediction = placesService.getPredictionFromIntent(data) ?: return@registerForActivityResult

                val placeId = prediction.placeId

                val placeName = prediction.getPrimaryText(null).toString()
                val placeAddress = prediction.getSecondaryText(null).toString()

                binding.restaurantName.text = placeName
                binding.restaurantAddress.text = placeAddress


                // Use new API to set both id and display text
                viewModel.setSelectedRestaurant(placeId, placeName, placeAddress)

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
            // Ensure current contents of the EditText and rating are pushed to the ViewModel
            viewModel.setReviewText(binding.reviewEditText.text.toString())
            viewModel.setRating(binding.ratingBar.rating)
            // Pass the selected image bitmap (may be null)
            viewModel.createReview(selectedImageBitmap)
        }

        // Photos: tap opens bottom sheet with camera/gallery options
        binding.addPhotosCard.setOnClickListener {
            showPhotoSourceOptions()
        }
    }

    private fun setupObservers() {
        viewModel.selectedRestaurantDisplay.observe(viewLifecycleOwner, Observer { display ->
            if (!display.isNullOrBlank()) {
                binding.changeText.text = display
            } else {
                // Fallback to default strings when nothing selected
                binding.changeText.text = getString(com.tasteclub.app.R.string.select_restaurant)
            }
        })

        // Observe separate name/address LiveData to populate the dedicated fields
        viewModel.selectedRestaurantName.observe(viewLifecycleOwner, Observer { name ->
            if (!name.isNullOrBlank()) {
                binding.restaurantName.text = name
            } else {
                binding.restaurantName.text = ""
            }
        })

        viewModel.selectedRestaurantAddress.observe(viewLifecycleOwner, Observer { address ->
            if (!address.isNullOrBlank()) {
                binding.restaurantAddress.text = address
            } else {
                binding.restaurantAddress.text = ""
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
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Failed to post review: ${it.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
                viewModel.resetCreateResult()
            }
        })
    }

    private fun showPhotoSourceOptions() {
        // Simple dialog for camera/gallery choice
        val options = arrayOf("Take photo", "Choose from gallery", "Remove photo")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Add photo")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch()
                    1 -> pickImageLauncher.launch("image/*")
                    2 -> removeSelectedImage()
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted, launch camera
                launchCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Show rationale dialog
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Camera Permission Required")
                    .setMessage("Camera permission is needed to take photos for your review.")
                    .setPositiveButton("Grant") { _, _ ->
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            else -> {
                // Request permission
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera() {
        takePictureLauncher.launch(null)
    }

    private fun handleImageUri(uri: Uri) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
            onImageSelected(bitmap)
        } catch (e: IOException) {
            Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onImageSelected(bitmap: Bitmap) {
        // Compress image before upload / attach to review
        val compressedBitmap = compressBitmap(bitmap, 1200, 1200)
        selectedImageBitmap = compressedBitmap
        // Show preview
        binding.selectedPhotoImageView.visibility = View.VISIBLE
        binding.addPhotosOverlay.visibility = View.GONE
        // Use Picasso to display the bitmap into ImageView (Picasso supports bitmaps via .load)
        Picasso.get()
            .load(android.net.Uri.parse(MediaStore.Images.Media.insertImage(requireContext().contentResolver, compressedBitmap, null, null)))
            .fit()
            .centerCrop()
            .into(binding.selectedPhotoImageView)
    }

    private fun removeSelectedImage() {
        selectedImageBitmap = null
        binding.selectedPhotoImageView.setImageDrawable(null)
        binding.selectedPhotoImageView.visibility = View.GONE
        binding.addPhotosOverlay.visibility = View.VISIBLE
    }

    /**
     * Compress bitmap to max dimensions while maintaining aspect ratio
     */
    private fun compressBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)

        if (ratio >= 1) {
            return bitmap
        }

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
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
