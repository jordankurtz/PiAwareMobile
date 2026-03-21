package com.jordankurtz.piawaremobile.settings

import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository

data class Settings(
    val servers: List<Server> = emptyList(),
    val refreshInterval: Int = SettingsRepository.DEFAULT_REFRESH_INTERVAL,
    val centerMapOnUserOnStart: Boolean = false,
    val restoreMapStateOnStart: Boolean = false,
    val showReceiverLocations: Boolean = false,
    val showUserLocationOnMap: Boolean = false,
    val trailDisplayMode: TrailDisplayMode = TrailDisplayMode.NONE,
    val showMinimapTrails: Boolean = false,
    val openUrlsExternally: Boolean = false,
    val enableFlightAwareApi: Boolean = false,
    val flightAwareApiKey: String = "",
    val defaultZoomLevel: Int = DEFAULT_ZOOM_LEVEL,
    val minZoomLevel: Int = MIN_ZOOM_LEVEL,
    val maxZoomLevel: Int = MAX_ZOOM_LEVEL,
) {
    companion object {
        const val MIN_ZOOM_LEVEL = 1
        const val MAX_ZOOM_LEVEL = 16
        const val DEFAULT_ZOOM_LEVEL = 8
    }
}
