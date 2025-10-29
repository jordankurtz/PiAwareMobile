package com.jordankurtz.piawaremobile.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.Location
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory

@Factory
class LocationViewModel(
    private val locationService: LocationService,
    private val loadSettingsUseCase: LoadSettingsUseCase,
) : ViewModel() {

    private val _locationState = MutableStateFlow<LocationState>(LocationState.Idle)
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _recenterMap = MutableSharedFlow<Location>()
    val recenterMap = _recenterMap.asSharedFlow()

    init {
        viewModelScope.launch {
            loadSettingsUseCase().collect {
                if (it is Async.Success) {
                    val settings = it.data
                    if (settings.showUserLocationOnMap) {
                        requestLocationPermission()
                    }
                    if (settings.centerMapOnUserOnStart) {
                        requestLocationPermission(true)
                    }
                }
            }
        }
    }

    private fun requestLocationPermission(recenter: Boolean = false) {
        _locationState.value = LocationState.RequestingPermission
        locationService.requestPermissions { granted ->
            if (granted) {
                _locationState.value = LocationState.PermissionGranted
                startLocationUpdates(recenter)
            } else {
                _locationState.value = LocationState.PermissionDenied
            }
        }
    }

    private fun startLocationUpdates(recenter: Boolean) {
        _locationState.value = LocationState.TrackingLocation
        var isFirstUpdate = true
        locationService.startLocationUpdates { location ->
            _currentLocation.value = location
            if (isFirstUpdate && recenter) {
                viewModelScope.launch { _recenterMap.emit(location) }
                isFirstUpdate = false
            }
        }
    }

    fun stopLocationUpdates() {
        locationService.stopLocationUpdates()
        _locationState.value = LocationState.Idle
        _currentLocation.value = null
    }
}
