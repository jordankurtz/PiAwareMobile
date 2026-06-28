package com.jordankurtz.piawaremobile

import com.jordankurtz.piawaremobile.aircraft.AircraftViewModel
import com.jordankurtz.piawaremobile.location.LocationViewModel
import com.jordankurtz.piawaremobile.map.MapViewModel
import com.jordankurtz.piawaremobile.map.TileProviders
import com.jordankurtz.piawaremobile.map.offline.BoundingBox
import com.jordankurtz.piawaremobile.map.offline.OfflineMapsViewModel
import com.jordankurtz.piawaremobile.settings.SettingsViewModel
import org.koin.mp.KoinPlatform

fun getAircraftViewModel(): AircraftViewModel = KoinPlatform.getKoin().get()
fun getLocationViewModel(): LocationViewModel = KoinPlatform.getKoin().get()
fun getSettingsViewModel(): SettingsViewModel = KoinPlatform.getKoin().get()
fun getMapViewModel(): MapViewModel = KoinPlatform.getKoin().get()
fun getOfflineMapsViewModel(): OfflineMapsViewModel = KoinPlatform.getKoin().get()

fun fitMapToAircraft() {
    val koin = KoinPlatform.getKoin()
    val aircraft = koin.get<AircraftViewModel>().aircraft.value
    koin.get<MapViewModel>().fitToAircraft(aircraft)
}

fun getBuiltInTileProviders() = TileProviders.BUILT_IN
fun getApiKeyTileProviders() = TileProviders.API_KEY_REQUIRED

fun updateMapProviderById(id: String) {
    val config = TileProviders.ALL.firstOrNull { it.id == id } ?: return
    KoinPlatform.getKoin().get<SettingsViewModel>().updateMapProvider(config)
}

fun setApiKeyAndActivate(keyGroup: String, key: String, providerId: String) {
    val config = TileProviders.ALL.firstOrNull { it.id == providerId } ?: return
    KoinPlatform.getKoin().get<SettingsViewModel>().setApiKeyAndActivateProvider(keyGroup, key, config)
}

fun addCustomTileProvider(id: String, name: String, urlTemplate: String) {
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
) = vm.startDownload(
    name = name,
    bounds = bounds,
    minZoom = minZoom,
    maxZoom = maxZoom,
    viewportZoom = viewportZoom,
)
