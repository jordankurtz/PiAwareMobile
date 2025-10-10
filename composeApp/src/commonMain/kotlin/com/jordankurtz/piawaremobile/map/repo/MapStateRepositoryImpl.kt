package com.jordankurtz.piawaremobile.map.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.jordankurtz.piawaremobile.model.MapState
import com.russhwolf.settings.datastore.DataStoreSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class MapStateRepositoryImpl(val dataStore: DataStore<Preferences>) : MapStateRepository {

    private val settings by lazy {
        DataStoreSettings(dataStore)
    }

    // Define keys for storing the values
    companion object Companion {
        private const val KEY_SCROLL_X = "map_scroll_x"
        private const val KEY_SCROLL_Y = "map_scroll_y"
        private const val KEY_ZOOM = "map_zoom"

        // Define a default location (e.g., center of the US)
        val DEFAULT_STATE = MapState(scrollX = 0.5, scrollY = 0.5, zoom = 4.0)    }

    /**
     * Saves the map's current state to persistent settings.
     */
    override suspend fun saveMapState(scrollX: Double, scrollY: Double, zoom: Double) {
        withContext(Dispatchers.IO) {
            settings.putDouble(KEY_SCROLL_X, scrollX)
            settings.putDouble(KEY_SCROLL_Y, scrollY)
            settings.putDouble(KEY_ZOOM, zoom)
        }
    }

    /**
     * Loads the last saved map state from persistent settings.
     * Returns a default state if no state has been saved yet.
     */
    override suspend fun getSavedMapState(): MapState {
        return withContext(Dispatchers.IO) {
            val scrollX = settings.getDoubleOrNull(KEY_SCROLL_X)
            val scrollY = settings.getDoubleOrNull(KEY_SCROLL_Y)
            val zoom = settings.getDoubleOrNull(KEY_ZOOM)

            if (scrollX != null && scrollY != null && zoom != null) {
                return@withContext MapState(scrollX, scrollY, zoom)
            }

            DEFAULT_STATE
        }
    }
}
