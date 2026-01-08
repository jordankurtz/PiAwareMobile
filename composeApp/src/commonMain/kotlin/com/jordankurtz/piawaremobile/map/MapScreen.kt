package com.jordankurtz.piawaremobile.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
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

@OptIn(ExperimentalMaterial3Api::class)
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
    val selectedAircraftHex by mapViewModel.selectedAircraft.collectAsState()
    val flightDetails by aircraftViewModel.flightDetails.collectAsState()
    val aircraftTrails by aircraftViewModel.aircraftTrails.collectAsState()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    LaunchedEffect(aircraft) {
        mapViewModel.onAircraftUpdated(aircraft)
    }

    LaunchedEffect(aircraftTrails) {
        mapViewModel.onAircraftTrailsUpdated(aircraftTrails)
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

    LaunchedEffect(selectedAircraftHex) {
        aircraftViewModel.openFlightInformation(selectedAircraftHex)
    }

    Box {
        OpenStreetMap(state = mapViewModel.state)
        Overlay(
            numberOfPlanes,
            modifier = Modifier.align(Alignment.BottomEnd).padding(horizontal = 8.dp)
        )
    }

    val selectedAircraft = selectedAircraftHex?.let { hex ->
        aircraft.firstOrNull { it.first.hex == hex }?.first
    }

    FlightDetailsBottomSheet(
        aircraft = selectedAircraft,
        flightDetails = flightDetails,
        onDismissRequest = { mapViewModel.onAircraftDeselected() },
        sheetState = sheetState
    )
}
