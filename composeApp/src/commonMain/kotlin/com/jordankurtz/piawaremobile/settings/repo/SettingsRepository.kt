package com.jordankurtz.piawaremobile.settings.repo

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
        const val DEFAULT_REFRESH_INTERVAL = 5
    }
}