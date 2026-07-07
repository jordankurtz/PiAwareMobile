@file:Suppress("TooManyFunctions")

package com.jordankurtz.squawkscope

import com.jordankurtz.squawkscope.aircraft.AircraftViewModel
import com.jordankurtz.squawkscope.aircraft.usecase.GetAircraftTrailUseCase
import com.jordankurtz.squawkscope.location.LocationViewModel
import com.jordankurtz.squawkscope.map.MapViewModel
import com.jordankurtz.squawkscope.map.TileProviderConfig
import com.jordankurtz.squawkscope.map.TileProviders
import com.jordankurtz.squawkscope.map.offline.BoundingBox
import com.jordankurtz.squawkscope.map.offline.OfflineMapsViewModel
import com.jordankurtz.squawkscope.settings.SettingsViewModel
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform
import com.jordankurtz.squawkscope.map.offline.TileProviderConfig as OfflineTileProviderConfig
import com.jordankurtz.squawkscope.map.offline.TileProviders as OfflineTileProviders

fun getAircraftViewModel(): AircraftViewModel = KoinPlatform.getKoin().get()

fun getLocationViewModel(): LocationViewModel = KoinPlatform.getKoin().get()

fun getSettingsViewModel(): SettingsViewModel = KoinPlatform.getKoin().get()

fun getMapViewModel(): MapViewModel = KoinPlatform.getKoin().get()

fun getMapZoomLevel(): Int = getMapViewModel().currentZoomLevel.value

fun getOfflineMapsViewModel(): OfflineMapsViewModel = KoinPlatform.getKoin().get()

fun fitMapToAircraft() {
    val koin = KoinPlatform.getKoin()
    val aircraft = koin.get<AircraftViewModel>().aircraft.value
    koin.get<MapViewModel>().fitToAircraft(aircraft)
}

fun getAircraftTrail(hex: String) = KoinPlatform.getKoin().get<GetAircraftTrailUseCase>().invoke(hex)

fun getBuiltInTileProviders() = TileProviders.BUILT_IN

fun getApiKeyTileProviders() = TileProviders.API_KEY_REQUIRED

suspend fun resolveProviderDisplayName(provider: TileProviderConfig): String =
    provider.displayNameRes?.let { getString(it) } ?: provider.displayName.ifEmpty { provider.id }

fun updateMapProviderById(id: String) {
    val config = TileProviders.ALL.firstOrNull { it.id == id } ?: return
    KoinPlatform.getKoin().get<SettingsViewModel>().updateMapProvider(config)
}

fun setApiKeyAndActivate(
    keyGroup: String,
    key: String,
    providerId: String,
) {
    val config = TileProviders.ALL.firstOrNull { it.id == providerId } ?: return
    KoinPlatform.getKoin().get<SettingsViewModel>().setApiKeyAndActivateProvider(keyGroup, key, config)
}

fun addCustomTileProvider(
    id: String,
    name: String,
    urlTemplate: String,
) {
    KoinPlatform.getKoin().get<SettingsViewModel>().addCustomProvider(id, name, urlTemplate)
}

fun deleteCustomTileProvider(id: String) {
    KoinPlatform.getKoin().get<SettingsViewModel>().deleteCustomProvider(id)
}

fun startOfflineDownload(
    vm: OfflineMapsViewModel,
    name: String,
    bounds: BoundingBox,
    minZoom: Int,
    maxZoom: Int,
    viewportZoom: Int,
    providerId: String,
    urlTemplate: String,
) {
    val provider =
        OfflineTileProviderConfig(
            id = providerId,
            urlTemplate = urlTemplate,
            requestDelayMs = OfflineTileProviders.OPENSTREETMAP.requestDelayMs,
            avgTileSizeBytes = OfflineTileProviders.OPENSTREETMAP.avgTileSizeBytes,
            userAgent = OfflineTileProviders.OPENSTREETMAP.userAgent,
        )
    vm.startDownload(
        name = name,
        bounds = bounds,
        minZoom = minZoom,
        maxZoom = maxZoom,
        viewportZoom = viewportZoom,
        provider = provider,
    )
}
