package com.jordankurtz.piawaremobile.settings

import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository

data class Settings (
    val servers: List<Server> = emptyList(),
    val refreshInterval: Int = SettingsRepository.DEFAULT_REFRESH_INTERVAL,
    val centerMapOnUserOnStart: Boolean = false,
    val restoreMapStateOnStart: Boolean = false,
    val showReceiverLocations: Boolean = false,
    val showUserLocationOnMap: Boolean = false,
    val openUrlsExternally: Boolean = false,
    val enableFlightAwareApi: Boolean = false,
    val flightAwareApiKey: String = ""
)
