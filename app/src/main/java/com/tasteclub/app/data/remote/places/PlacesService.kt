package com.tasteclub.app.data.remote.places

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.PlaceAutocomplete
import com.tasteclub.app.BuildConfig


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

    /** New Autocomplete (UI) - create the intent */
    fun createAutocompleteIntent(
        sessionToken: AutocompleteSessionToken? = null
    ): Intent {
        return PlaceAutocomplete.createIntent(appContext) {
            if (sessionToken != null) {
                setAutocompleteSessionToken(sessionToken)
            }
            // Optional configuration if your SDK version supports it:
            // setCountries(listOf("IL"))
            // setTypesFilter(listOf("restaurant"))
        }
    }

    /** New Autocomplete (UI) - parse results */
    fun getPredictionFromIntent(data: Intent) =
        PlaceAutocomplete.getPredictionFromIntent(data)

    fun getResultStatusFromIntent(data: Intent): Status? =
        PlaceAutocomplete.getResultStatusFromIntent(data)

    fun getSessionTokenFromIntent(data: Intent): AutocompleteSessionToken? =
        PlaceAutocomplete.getSessionTokenFromIntent(data)

    /** Fetch full place details by placeId (optional sessionToken recommended) */
    fun getPlaceDetails(
        placeId: String,
        sessionToken: AutocompleteSessionToken? = null,
        onSuccess: (Place) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val builder = FetchPlaceRequest.builder(placeId, listOf(
            Place.Field.ID,
            Place.Field.PHOTO_METADATAS,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.PRIMARY_TYPE_DISPLAY_NAME
            //Place.Field.INTERNATIONAL_PHONE_NUMBER
            //Place.Field.PRICE_LEVEL

        ),)
        if (sessionToken != null) builder.sessionToken = sessionToken

        placesClient.fetchPlace(builder.build())
            .addOnSuccessListener { res -> onSuccess(res.place) }
            .addOnFailureListener { ex -> onError(ex) }
    }
}

