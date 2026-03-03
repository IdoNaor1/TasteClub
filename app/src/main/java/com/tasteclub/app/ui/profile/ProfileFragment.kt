package com.tasteclub.app.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso
import com.tasteclub.app.R
import com.tasteclub.app.databinding.FragmentProfileBinding
import com.tasteclub.app.util.ServiceLocator
import java.io.IOException

/**
 * ProfileFragment - Displays user profile and settings
 *
 * Features:
 * - View profile information (name, email, profile picture)
 * - Display user statistics (review count, followers, following)
 * - Edit profile (name, email)
 * - Change profile picture (camera or gallery)
 * - Manage email preferences
 * - Logout with confirmation
 */
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(
            ServiceLocator.provideAuthRepository(requireContext()),
            ServiceLocator.provideReviewRepository(requireContext())
        )
    }

    private lateinit var loadingProgressBar: android.widget.ProgressBar

    // Camera permission launcher
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(
                context,
                "Camera permission is required to take photos",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Activity result launchers for image selection
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { handleImageSelected(it) }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { handleImageUri(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)

        setupClickListeners()
        observeViewModel()
    }

    /**
     * Setup click listeners for all interactive elements
     */
    private fun setupClickListeners() {
        binding.apply {
            // Edit profile picture
            editProfileImageButton.setOnClickListener {
                showPhotoUploadOptions()
            }

            // Edit profile (name, email)
            editProfileRow.setOnClickListener {
                showEditProfileDialog()
            }

            // Email preferences
            emailPreferencesRow.setOnClickListener {
                showEmailPreferencesDialog()
            }

            // Logout
            logoutButton.setOnClickListener {
                showLogoutConfirmation()
            }

            // Footer links (placeholder toasts for Phase 3)
            termsLink.setOnClickListener {
                Toast.makeText(context, "Terms of Service", Toast.LENGTH_SHORT).show()
            }

            privacyLink.setOnClickListener {
                Toast.makeText(context, "Privacy Policy", Toast.LENGTH_SHORT).show()
            }

            helpLink.setOnClickListener {
                Toast.makeText(context, "Help Center", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Observe ViewModel LiveData
     */
    private fun observeViewModel() {
        // Observe current user
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.apply {
                    userName.text = it.userName
                    userEmail.text = it.email

                    // Load profile image
                    if (it.profileImageUrl.isNotEmpty()) {
                        Picasso.get()
                            .load(it.profileImageUrl)
                            .placeholder(R.drawable.ic_user_placeholder)
                            .error(R.drawable.ic_user_placeholder)
                            .into(profileImage)
                    } else {
                        profileImage.setImageResource(R.drawable.ic_user_placeholder)
                    }
                }
            }
        }

        // Observe review count
        viewModel.reviewCount.observe(viewLifecycleOwner) { count ->
            binding.reviewCountText.text = count.toString()
        }

        // Observe profile state
        viewModel.profileState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProfileViewModel.ProfileState.Loading -> {
                    loadingProgressBar.visibility = View.VISIBLE
                }
                is ProfileViewModel.ProfileState.Uploading -> {
                    loadingProgressBar.visibility = View.VISIBLE
                }
                is ProfileViewModel.ProfileState.Success -> {
                    loadingProgressBar.visibility = View.GONE
                    Toast.makeText(context, R.string.profile_updated, Toast.LENGTH_SHORT).show()
                    viewModel.resetState()
                }
                is ProfileViewModel.ProfileState.Error -> {
                    loadingProgressBar.visibility = View.GONE
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                    viewModel.resetState()
                }
                is ProfileViewModel.ProfileState.UploadSuccess -> {
                    loadingProgressBar.visibility = View.GONE
                    Toast.makeText(context, R.string.profile_picture_updated, Toast.LENGTH_SHORT).show()
                    viewModel.resetState()
                }
                else -> {
                    loadingProgressBar.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Show photo upload options dialog
     */
    private fun showPhotoUploadOptions() {
        val options = arrayOf(
            getString(R.string.take_photo),
            getString(R.string.choose_from_gallery),
            getString(R.string.cancel)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setItems(options) { dialog, which ->
                when (options[which]) {
                    getString(R.string.take_photo) -> checkCameraPermissionAndLaunch()
                    getString(R.string.choose_from_gallery) -> pickImageLauncher.launch("image/*")
                    else -> dialog.dismiss()
                }
            }
            .show()
    }

    /**
     * Check camera permission and launch camera if granted
     */
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
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Camera Permission Required")
                    .setMessage("Camera permission is needed to take photos for your profile picture.")
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

    /**
     * Launch camera to take picture
     */
    private fun launchCamera() {
        takePictureLauncher.launch(null)
    }

    /**
     * Handle image URI from gallery
     */
    private fun handleImageUri(uri: Uri) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
            handleImageSelected(bitmap)
        } catch (e: IOException) {
            Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handle selected/captured image bitmap
     */
    private fun handleImageSelected(bitmap: Bitmap) {
        // Compress image before upload
        val compressedBitmap = compressBitmap(bitmap, 800, 800)
        viewModel.updateProfilePicture(compressedBitmap)
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

    /**
     * Show edit profile dialog
     */
    private fun showEditProfileDialog() {
        val currentUser = viewModel.currentUser.value ?: return

        val dialog = EditProfileDialog.newInstance(
            currentUser.userName,
            currentUser.email
        )
        dialog.setOnProfileUpdatedListener(object : EditProfileDialog.OnProfileUpdatedListener {
            override fun onProfileUpdated(name: String, email: String) {
                viewModel.updateProfile(name, email)
            }
        })
        dialog.show(childFragmentManager, "EditProfileDialog")
    }

    /**
     * Show email preferences dialog
     */
    private fun showEmailPreferencesDialog() {
        val dialog = EmailPreferencesDialog()
        dialog.show(childFragmentManager, "EmailPreferencesDialog")
    }

    /**
     * Show logout confirmation dialog
     */
    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.logout_confirmation_title)
            .setMessage(R.string.logout_confirmation_message)
            .setPositiveButton(R.string.logout) { _, _ ->
                performLogout()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Perform logout and navigate to login screen
     */
    private fun performLogout() {
        viewModel.logout()

        // Navigate to login and clear back stack
        findNavController().navigate(
            R.id.action_global_login
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

