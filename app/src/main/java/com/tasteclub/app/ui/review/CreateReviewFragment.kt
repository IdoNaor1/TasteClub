package com.tasteclub.app.ui.review

import android.app.Activity
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.PlaceAutocompleteActivity
import com.tasteclub.app.data.remote.places.PlacesService


/**
 * CreateReviewFragment - Create a new review
 */
class CreateReviewFragment : Fragment() {

    private lateinit var placesService: PlacesService
    private var sessionToken: AutocompleteSessionToken? = null

    private val autocompleteLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult

                val prediction =
                    placesService.getPredictionFromIntent(data) ?: return@registerForActivityResult
                val tokenFromIntent = placesService.getSessionTokenFromIntent(data)

                val placeId = prediction.placeId

                placesService.getPlaceDetails(
                    placeId = placeId,
                    sessionToken = tokenFromIntent,
                    onSuccess = { place ->
                        // save to viewmodel / update UI
                    },
                    onError = { e ->
                        Toast.makeText(requireContext(), "Fetch details failed", Toast.LENGTH_SHORT)
                            .show()
                    }
                )

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

    private fun openAutocomplete() {
        sessionToken = placesService.createSessionToken()
        val intent = placesService.createAutocompleteIntent(sessionToken)
        autocompleteLauncher.launch(intent)
    }
}



