package com.tasteclub.app.data.remote.places

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.PlaceAutocomplete
import com.tasteclub.app.BuildConfig
import kotlinx.coroutines.tasks.await


class PlacesService(context: Context) {

    private val appContext = context.applicationContext
    private val placesClient: PlacesClient

    init {
        if (!Places.isInitialized()) {
            val apiKey = BuildConfig.PLACES_API_KEY
            if (apiKey.isEmpty()) {
                Log.e("Places test", "No api key")
            } else {
                Places.initializeWithNewPlacesApiEnabled(appContext, apiKey)
            }
        }
        placesClient = Places.createClient(appContext)
    }

    fun createSessionToken(): AutocompleteSessionToken =
        AutocompleteSessionToken.newInstance()

    fun getBoundsFromLocation(currentLocation: LatLng): RectangularBounds {
        // Create a roughly 5-10km box around the user
        val biasRange = 0.05 // approx 5km in degrees

        val southWest =
            LatLng(currentLocation.latitude - biasRange, currentLocation.longitude - biasRange)
        val northEast =
            LatLng(currentLocation.latitude + biasRange, currentLocation.longitude + biasRange)

        return RectangularBounds.newInstance(southWest, northEast)
    }

    /** New Autocomplete (UI) - create the intent */
    fun createAutocompleteIntent(
        sessionToken: AutocompleteSessionToken? = null,
        currentLocation: LatLng? = null
    ): Intent {
        return PlaceAutocomplete.createIntent(appContext) {
            if (currentLocation != null) {
                setLocationBias(getBoundsFromLocation(currentLocation))
            }
            setTypesFilter(listOf("restaurant"))
            if (sessionToken != null) {
                setAutocompleteSessionToken(sessionToken)
            }
        }
    }

    /** New Autocomplete (UI) - parse results */
    fun getPredictionFromIntent(data: Intent) =
        PlaceAutocomplete.getPredictionFromIntent(data)

    fun getResultStatusFromIntent(data: Intent): Status? =
        PlaceAutocomplete.getResultStatusFromIntent(data)

    suspend fun getPlaceDetails(
        placeId: String,
        sessionToken: AutocompleteSessionToken? = null,
    ): Place? {
        val fields = mutableListOf(
            Place.Field.ID,
            Place.Field.PHOTO_METADATAS,
            Place.Field.DISPLAY_NAME,
            Place.Field.ADDRESS_COMPONENTS,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION,
            Place.Field.PRIMARY_TYPE_DISPLAY_NAME,
        )

        return try {
            val request = FetchPlaceRequest.builder(placeId, fields)
                .setSessionToken(sessionToken)
                .build()

            val response = placesClient.fetchPlace(request).await()
            response.place
        } catch (e: Exception) {
            Log.e("PlacesService", "Error fetching place details: ${e.message}")
            null
        }
    }
}
