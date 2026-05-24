package com.jordankurtz.piawaremobile.map.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import com.jordankurtz.piawaremobile.model.MapState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [MapStateRepository::class])
class MapStateRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : MapStateRepository {
    companion object Companion {
        private val KEY_LATITUDE = doublePreferencesKey("map_latitude")
        private val KEY_LONGITUDE = doublePreferencesKey("map_longitude")
        private val KEY_ZOOM_LEVEL = doublePreferencesKey("map_zoom_level")

        // Center of the continental US, zoom 5
        val DEFAULT_STATE = MapState(latitude = 39.5, longitude = -98.35, zoom = 5.0)
    }

    /**
     * Saves the map's current state to persistent settings.
     */
    override suspend fun saveMapState(
        latitude: Double,
        longitude: Double,
        zoom: Double,
    ) {
        dataStore.edit { preferences ->
            preferences[KEY_LATITUDE] = latitude
            preferences[KEY_LONGITUDE] = longitude
            preferences[KEY_ZOOM_LEVEL] = zoom
        }
    }

    /**
     * Loads the last saved map state from persistent settings.
     * Returns a default state if no state has been saved yet.
     */
    override suspend fun getSavedMapState(): MapState {
        return dataStore.data.map { preferences ->
            val latitude = preferences[KEY_LATITUDE]
            val longitude = preferences[KEY_LONGITUDE]
            val zoom = preferences[KEY_ZOOM_LEVEL]

            if (latitude != null && longitude != null && zoom != null) {
                MapState(latitude, longitude, zoom)
            } else {
                DEFAULT_STATE
            }
        }.first()
    }
}
