package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "findora_session")

data class ApproxLocation(
    val label: String = "Midtown New York",
    val latitude: Double = 40.7527,
    val longitude: Double = -73.9772
)

class SessionManager(private val context: Context) {

    private object Keys {
        val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        val LOCATION_LABEL = stringPreferencesKey("location_label")
        val LOCATION_LAT = doublePreferencesKey("location_lat")
        val LOCATION_LNG = doublePreferencesKey("location_lng")
    }

    val currentUserId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.CURRENT_USER_ID]
    }

    val approxLocation: Flow<ApproxLocation> = context.dataStore.data.map { prefs ->
        ApproxLocation(
            label = prefs[Keys.LOCATION_LABEL] ?: "Midtown New York",
            latitude = prefs[Keys.LOCATION_LAT] ?: 40.7527,
            longitude = prefs[Keys.LOCATION_LNG] ?: -73.9772
        )
    }

    suspend fun setCurrentUserId(userId: String?) {
        context.dataStore.edit { prefs ->
            if (userId == null) {
                prefs.remove(Keys.CURRENT_USER_ID)
            } else {
                prefs[Keys.CURRENT_USER_ID] = userId
            }
        }
    }

    suspend fun setApproxLocation(label: String, lat: Double, lng: Double) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LOCATION_LABEL] = label
            prefs[Keys.LOCATION_LAT] = lat
            prefs[Keys.LOCATION_LNG] = lng
        }
    }
}
