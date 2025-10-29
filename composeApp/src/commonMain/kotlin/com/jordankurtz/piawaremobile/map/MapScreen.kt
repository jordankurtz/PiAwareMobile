package com.jordankurtz.piawaremobile.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawaremobile.Overlay
import com.jordankurtz.piawaremobile.aircraft.AircraftViewModel
import com.jordankurtz.piawaremobile.location.LocationViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MapScreen(
    mapViewModel: MapViewModel = koinViewModel(),
    locationViewModel: LocationViewModel = koinViewModel(),
    aircraftViewModel: AircraftViewModel = koinViewModel(),
) {
    val aircraft by aircraftViewModel.aircraft.collectAsState()
    val receiverLocations by aircraftViewModel.receiverLocations.collectAsState()
    val currentLocation by locationViewModel.currentLocation.collectAsState()
    val numberOfPlanes by aircraftViewModel.numberOfPlanes.collectAsState()

    LaunchedEffect(aircraft) {
        mapViewModel.onAircraftUpdated(aircraft)
    }

    LaunchedEffect(receiverLocations) {
        receiverLocations.forEach(mapViewModel::onReceiverLocation)
    }

    LaunchedEffect(currentLocation) {
        currentLocation?.let(mapViewModel::onUserLocationChanged)
    }

    LaunchedEffect(Unit) {
        locationViewModel.recenterMap.collect {
            mapViewModel.recenterOnLocation(it)
        }
    }

    Box {
        OpenStreetMap(state = mapViewModel.state)
        Overlay(numberOfPlanes, modifier = Modifier.align(Alignment.BottomEnd).padding(horizontal = 8.dp))
    }
}
