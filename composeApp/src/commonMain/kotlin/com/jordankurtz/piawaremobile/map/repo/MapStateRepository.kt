package com.jordankurtz.piawaremobile.map.repo

import com.jordankurtz.piawaremobile.model.MapState

interface MapStateRepository {
    /**
     * Saves the map's current state (latitude, longitude, and zoom) to persistent storage.
     */
    suspend fun saveMapState(scrollX: Double, scrollY: Double, zoom: Double)

    /**
     * Loads the last saved map state from persistent storage.
     *
     * @return The saved [MapState], or a default state if no state has been previously saved.
     */
    suspend fun getSavedMapState(): MapState
}
