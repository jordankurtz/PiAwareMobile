package com.jordankurtz.piawaremobile.settings.repo

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jordankurtz.piawaremobile.settings.Settings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings() : Flow<Settings>
    suspend fun saveSettings(settings: Settings)

    companion object {
        val SERVERS = stringPreferencesKey("servers")
        val REFRESH_INTERVAL = intPreferencesKey("refreshInterval")
        val CENTER_MAP_ON_USER_ON_START = booleanPreferencesKey("centerMapOnUserOnStart")
        val RESTORE_MAP_STATE_ON_START = booleanPreferencesKey("restoreMapStateOnStart")
        val SHOW_RECEIVER_LOCATIONS = booleanPreferencesKey("showReceiverLocations")
        val SHOW_USER_LOCATION_ON_MAP = booleanPreferencesKey("showUserLocation")
        val SHOW_AIRCRAFT_PATHS = booleanPreferencesKey("showAircraftPaths")
        val OPEN_URLS_EXTERNALLY = booleanPreferencesKey("openUrlsExternally")
        val ENABLE_FLIGHT_AWARE_API = booleanPreferencesKey("enableFlightAwareApi")
        val FLIGHT_AWARE_API_KEY = stringPreferencesKey("flightAwareApiKey")
        const val DEFAULT_REFRESH_INTERVAL = 5
    }
}
