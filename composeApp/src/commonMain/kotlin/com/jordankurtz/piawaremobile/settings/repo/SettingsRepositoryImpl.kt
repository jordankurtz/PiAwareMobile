package com.jordankurtz.piawaremobile.settings.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.jordankurtz.piawaremobile.map.TileProviders
import com.jordankurtz.piawaremobile.settings.CustomProviderConfig
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.TrailDisplayMode
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository.Companion.DEFAULT_REFRESH_INTERVAL
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Single(binds = [SettingsRepository::class])
class SettingsRepositoryImpl(
    private val datastore: DataStore<Preferences>,
) : SettingsRepository {
    override fun getSettings(): Flow<Settings> {
        return datastore.data.map { preferences ->
            Settings(
                servers = preferences[SettingsRepository.SERVERS]?.let { Json.decodeFromString(it) } ?: emptyList(),
                refreshInterval = preferences[SettingsRepository.REFRESH_INTERVAL] ?: DEFAULT_REFRESH_INTERVAL,
                centerMapOnUserOnStart = preferences[SettingsRepository.CENTER_MAP_ON_USER_ON_START] ?: false,
                restoreMapStateOnStart = preferences[SettingsRepository.RESTORE_MAP_STATE_ON_START] ?: false,
                showReceiverLocations = preferences[SettingsRepository.SHOW_RECEIVER_LOCATIONS] ?: false,
                showUserLocationOnMap = preferences[SettingsRepository.SHOW_USER_LOCATION_ON_MAP] ?: false,
                trailDisplayMode =
                    preferences[SettingsRepository.TRAIL_DISPLAY_MODE]?.let {
                        try {
                            TrailDisplayMode.valueOf(it)
                        } catch (_: IllegalArgumentException) {
                            null
                        }
                    } ?: TrailDisplayMode.NONE,
                showMinimapTrails = preferences[SettingsRepository.SHOW_MINIMAP_TRAILS] ?: false,
                openUrlsExternally = preferences[SettingsRepository.OPEN_URLS_EXTERNALLY] ?: false,
                enableFlightAwareApi = preferences[SettingsRepository.ENABLE_FLIGHT_AWARE_API] ?: false,
                flightAwareApiKey = preferences[SettingsRepository.FLIGHT_AWARE_API_KEY] ?: "",
                mapProviderId = preferences[SettingsRepository.MAP_PROVIDER_ID] ?: TileProviders.OPENSTREETMAP.id,
                defaultZoomLevel =
                    preferences[SettingsRepository.DEFAULT_ZOOM_LEVEL_KEY]
                        ?: SettingsRepository.DEFAULT_ZOOM_LEVEL,
                minZoomLevel =
                    preferences[SettingsRepository.MIN_ZOOM_LEVEL_KEY]
                        ?: SettingsRepository.MIN_ZOOM_LEVEL,
                maxZoomLevel =
                    preferences[SettingsRepository.MAX_ZOOM_LEVEL_KEY]
                        ?: SettingsRepository.MAX_ZOOM_LEVEL,
                apiKeys =
                    preferences[SettingsRepository.API_KEYS_JSON]?.let {
                        try {
                            Json.decodeFromString(it)
                        } catch (_: Exception) {
                            emptyMap()
                        }
                    } ?: emptyMap(),
                customProviders =
                    preferences[SettingsRepository.CUSTOM_PROVIDERS_JSON]?.let {
                        try {
                            Json.decodeFromString(it)
                        } catch (_: Exception) {
                            emptyList()
                        }
                    } ?: emptyList(),
            )
        }
    }

    override suspend fun saveSettings(settings: Settings) {
        datastore.edit { preferences ->
            preferences[SettingsRepository.SERVERS] = settings.servers.let { Json.encodeToString(it) }
            preferences[SettingsRepository.REFRESH_INTERVAL] = settings.refreshInterval
            preferences[SettingsRepository.CENTER_MAP_ON_USER_ON_START] = settings.centerMapOnUserOnStart
            preferences[SettingsRepository.RESTORE_MAP_STATE_ON_START] = settings.restoreMapStateOnStart
            preferences[SettingsRepository.SHOW_RECEIVER_LOCATIONS] = settings.showReceiverLocations
            preferences[SettingsRepository.SHOW_USER_LOCATION_ON_MAP] = settings.showUserLocationOnMap
            preferences[SettingsRepository.TRAIL_DISPLAY_MODE] = settings.trailDisplayMode.name
            preferences[SettingsRepository.SHOW_MINIMAP_TRAILS] = settings.showMinimapTrails
            preferences[SettingsRepository.OPEN_URLS_EXTERNALLY] = settings.openUrlsExternally
            preferences[SettingsRepository.ENABLE_FLIGHT_AWARE_API] = settings.enableFlightAwareApi
            preferences[SettingsRepository.FLIGHT_AWARE_API_KEY] = settings.flightAwareApiKey
            preferences[SettingsRepository.MAP_PROVIDER_ID] = settings.mapProviderId
            preferences[SettingsRepository.DEFAULT_ZOOM_LEVEL_KEY] = settings.defaultZoomLevel
            preferences[SettingsRepository.MIN_ZOOM_LEVEL_KEY] = settings.minZoomLevel
            preferences[SettingsRepository.MAX_ZOOM_LEVEL_KEY] = settings.maxZoomLevel
            preferences[SettingsRepository.API_KEYS_JSON] = Json.encodeToString(settings.apiKeys)
            preferences[SettingsRepository.CUSTOM_PROVIDERS_JSON] = Json.encodeToString(settings.customProviders)
        }
    }

    override suspend fun setTrailDisplayMode(trailDisplayMode: TrailDisplayMode) {
        datastore.edit { preferences ->
            preferences[SettingsRepository.TRAIL_DISPLAY_MODE] = trailDisplayMode.name
        }
    }

    override suspend fun setShowMinimapTrails(showMinimapTrails: Boolean) {
        datastore.edit { preferences ->
            preferences[SettingsRepository.SHOW_MINIMAP_TRAILS] = showMinimapTrails
        }
    }
}
