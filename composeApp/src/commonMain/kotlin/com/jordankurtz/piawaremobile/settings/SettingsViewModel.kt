package com.jordankurtz.piawaremobile.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jordankurtz.piawaremobile.extensions.stateIn
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.usecase.AddServerUseCase
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetCenterMapOnUserOnStartUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetRefreshIntervalUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetRestoreMapStateOnStartUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetShowReceiverLocationsUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SetShowUserLocationOnMapUseCase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    loadSettingsUseCase: LoadSettingsUseCase,
    private val addServerUseCase: AddServerUseCase,
    private val setRefreshIntervalUseCase: SetRefreshIntervalUseCase,
    private val setCenterMapOnUserOnStartUseCase: SetCenterMapOnUserOnStartUseCase,
    private val setRestoreMapStateOnStartUseCase: SetRestoreMapStateOnStartUseCase,
    private val setShowReceiverLocationsUseCase: SetShowReceiverLocationsUseCase,
    private val setShowUserLocationOnMapUseCase: SetShowUserLocationOnMapUseCase,
) : ViewModel() {

    val settings: StateFlow<Async<Settings>>
        get() = _settings
    private val _settings = loadSettingsUseCase().stateIn(viewModelScope)

    fun addServer(name: String, address: String) = viewModelScope.launch {
        addServerUseCase(name, address)
    }

    fun updateRefreshInterval(refreshInterval: Int) = viewModelScope.launch {
        setRefreshIntervalUseCase(refreshInterval)
    }

    fun updateCenterMapOnUserOnStart(enabled: Boolean) = viewModelScope.launch {
        setCenterMapOnUserOnStartUseCase(enabled)
    }

    fun updateRestoreMapStateOnStart(enabled: Boolean) = viewModelScope.launch {
        setRestoreMapStateOnStartUseCase(enabled)
    }

    fun updateShowReceiverLocations(enabled: Boolean) = viewModelScope.launch {
        setShowReceiverLocationsUseCase(enabled)
    }

    fun updateShowUserLocationOnMap(enabled: Boolean) = viewModelScope.launch {
        setShowUserLocationOnMapUseCase(enabled)
    }
}
