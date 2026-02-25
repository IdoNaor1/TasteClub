package com.tasteclub.app.data.local

import androidx.room.TypeConverter
import com.google.android.libraries.places.api.model.AddressComponents
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromAddressComponents(value: AddressComponents?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toAddressComponents(value: String?): AddressComponents? {
        // AddressComponents is an abstract type from the Places SDK and cannot be
        // reconstructed via Gson (Gson can't instantiate abstract classes). Returning
        // null here avoids crashes when Room reads cached restaurants. The raw JSON
        // is still stored in the DB (fromAddressComponents) if needed for debugging.
        if (value.isNullOrEmpty()) return null
        return null
    }
}
