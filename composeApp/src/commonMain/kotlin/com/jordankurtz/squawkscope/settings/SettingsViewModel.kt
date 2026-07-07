package com.jordankurtz.squawkscope.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jordankurtz.squawkscope.extensions.stateIn
import com.jordankurtz.squawkscope.map.TileProviderConfig
import com.jordankurtz.squawkscope.map.cache.TileCache
import com.jordankurtz.squawkscope.model.Async
import com.jordankurtz.squawkscope.settings.usecase.SettingsService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Suppress("TooManyFunctions")
@Single
class SettingsViewModel(
    private val settingsService: SettingsService,
    private val tileCache: TileCache,
) : ViewModel() {
    val settings: StateFlow<Async<Settings>>
        get() = _settings
    private val _settings = settingsService.loadSettings().stateIn(viewModelScope)

    fun addServer(
        name: String,
        address: String,
        type: ServerType,
    ) = viewModelScope.launch {
        settingsService.addServer(name, address, type)
    }

    fun editServer(server: Server) =
        viewModelScope.launch {
            settingsService.editServer(server)
        }

    fun deleteServer(id: Uuid) =
        viewModelScope.launch {
            settingsService.deleteServer(id)
        }

    fun updateRefreshInterval(refreshInterval: Int) =
        viewModelScope.launch {
            settingsService.setRefreshInterval(refreshInterval)
        }

    fun updateCenterMapOnUserOnStart(enabled: Boolean) =
        viewModelScope.launch {
            settingsService.setCenterMapOnUserOnStart(enabled)
        }

    fun updateRestoreMapStateOnStart(enabled: Boolean) =
        viewModelScope.launch {
            settingsService.setRestoreMapStateOnStart(enabled)
        }

    fun updateShowReceiverLocations(enabled: Boolean) =
        viewModelScope.launch {
            settingsService.setShowReceiverLocations(enabled)
        }

    fun updateShowUserLocationOnMap(enabled: Boolean) =
        viewModelScope.launch {
            settingsService.setShowUserLocationOnMap(enabled)
        }

    fun updateTrailDisplayMode(trailDisplayMode: TrailDisplayMode) =
        viewModelScope.launch {
            settingsService.setTrailDisplayMode(trailDisplayMode)
        }

    fun updateShowMinimapTrails(enabled: Boolean) =
        viewModelScope.launch {
            settingsService.setShowMinimapTrails(enabled)
        }

    fun updateOpenUrlsExternally(enabled: Boolean) =
        viewModelScope.launch {
            settingsService.setOpenUrlsExternally(enabled)
        }

    fun updateEnableFlightAwareApi(enabled: Boolean) =
        viewModelScope.launch {
            settingsService.setEnableFlightAwareApi(enabled)
        }

    fun updateFlightAwareApiKey(apiKey: String) =
        viewModelScope.launch {
            settingsService.setFlightAwareApiKey(apiKey)
        }

    fun updateMapProvider(config: TileProviderConfig) =
        viewModelScope.launch {
            settingsService.setMapProviderId(config.id)
        }

    fun updateDefaultZoomLevel(zoom: Int) =
        viewModelScope.launch {
            settingsService.setDefaultZoomLevel(zoom)
        }

    fun updateMinZoomLevel(zoom: Int) =
        viewModelScope.launch {
            settingsService.setMinZoomLevel(zoom)
        }

    fun updateMaxZoomLevel(zoom: Int) =
        viewModelScope.launch {
            settingsService.setMaxZoomLevel(zoom)
        }

    fun updateApiKey(
        providerId: String,
        key: String,
    ) = viewModelScope.launch {
        settingsService.setApiKey(providerId, key)
    }

    fun setApiKeyAndActivateProvider(
        keyGroup: String,
        key: String,
        provider: TileProviderConfig,
    ) = viewModelScope.launch {
        settingsService.setApiKey(keyGroup, key)
        settingsService.setMapProviderId(provider.id)
    }

    fun addCustomProvider(
        id: String,
        displayName: String,
        urlTemplate: String,
    ) = viewModelScope.launch {
        settingsService.addCustomProvider(id, displayName, urlTemplate)
    }

    fun deleteCustomProvider(id: String) =
        viewModelScope.launch {
            settingsService.deleteCustomProvider(id)
        }

    fun clearTileCache() =
        viewModelScope.launch {
            tileCache.clearAll()
        }
}
