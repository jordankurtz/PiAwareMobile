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
        private val KEY_SCROLL_X = doublePreferencesKey("map_scroll_x")
        private val KEY_SCROLL_Y = doublePreferencesKey("map_scroll_y")
        private val KEY_ZOOM = doublePreferencesKey("map_zoom")

        val DEFAULT_STATE = MapState(scrollX = 0.5, scrollY = 0.5, zoom = 4.0)
    }

    /**
     * Saves the map's current state to persistent settings.
     */
    override suspend fun saveMapState(
        scrollX: Double,
        scrollY: Double,
        zoom: Double,
    ) {
        dataStore.edit { preferences ->
            preferences[KEY_SCROLL_X] = scrollX
            preferences[KEY_SCROLL_Y] = scrollY
            preferences[KEY_ZOOM] = zoom
        }
    }

    /**
     * Loads the last saved map state from persistent settings.
     * Returns a default state if no state has been saved yet.
     */
    override suspend fun getSavedMapState(): MapState {
        return dataStore.data.map { preferences ->
            val scrollX = preferences[KEY_SCROLL_X]
            val scrollY = preferences[KEY_SCROLL_Y]
            val zoom = preferences[KEY_ZOOM]

            if (scrollX != null && scrollY != null && zoom != null) {
                MapState(scrollX, scrollY, zoom)
            } else {
                DEFAULT_STATE
            }
        }.first()
    }
}
