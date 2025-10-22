package com.jordankurtz.piawaremobile.settings.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository.Companion.DEFAULT_REFRESH_INTERVAL
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsRepositoryImpl(
    val datastore: DataStore<Preferences>
): SettingsRepository {
    override fun getSettings(): Flow<Settings> {
        return datastore.data.map {preferences ->
            Settings(
                servers = preferences[SettingsRepository.SERVERS]?.let { Json.decodeFromString(it) } ?: emptyList(),
                refreshInterval = preferences[SettingsRepository.REFRESH_INTERVAL] ?: DEFAULT_REFRESH_INTERVAL,
                centerMapOnUserOnStart = preferences[SettingsRepository.CENTER_MAP_ON_USER_ON_START] ?: false,
                restoreMapStateOnStart = preferences[SettingsRepository.RESTORE_MAP_STATE_ON_START] ?: false,
                showReceiverLocations = preferences[SettingsRepository.SHOW_RECEIVER_LOCATIONS] ?: false,
                showUserLocationOnMap = preferences[SettingsRepository.SHOW_USER_LOCATION_ON_MAP] ?: false,
            )
        }
    }

    override suspend fun saveSettings(settings: Settings) {
        datastore.edit {preferences ->
            preferences[SettingsRepository.SERVERS] = settings.servers.let { Json.encodeToString(it) }
            preferences[SettingsRepository.REFRESH_INTERVAL] = settings.refreshInterval
            preferences[SettingsRepository.CENTER_MAP_ON_USER_ON_START] = settings.centerMapOnUserOnStart
            preferences[SettingsRepository.RESTORE_MAP_STATE_ON_START] = settings.restoreMapStateOnStart
            preferences[SettingsRepository.SHOW_RECEIVER_LOCATIONS] = settings.showReceiverLocations
            preferences[SettingsRepository.SHOW_USER_LOCATION_ON_MAP] = settings.showUserLocationOnMap
        }
    }
}
