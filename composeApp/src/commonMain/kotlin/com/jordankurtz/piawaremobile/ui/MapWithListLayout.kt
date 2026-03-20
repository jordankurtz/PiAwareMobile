package com.jordankurtz.piawaremobile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawaremobile.Overlay
import com.jordankurtz.piawaremobile.aircraft.AircraftViewModel
import com.jordankurtz.piawaremobile.list.TabletAircraftListPanel
import com.jordankurtz.piawaremobile.location.LocationViewModel
import com.jordankurtz.piawaremobile.map.MapViewModel
import com.jordankurtz.piawaremobile.map.OpenStreetMap
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.ic_settings
import piawaremobile.composeapp.generated.resources.settings_title

@Composable
fun MapWithListLayout(
    onSettingsClick: () -> Unit,
    aircraftViewModel: AircraftViewModel = koinViewModel(),
    mapViewModel: MapViewModel = koinViewModel(),
    locationViewModel: LocationViewModel = koinViewModel(),
) {
    val aircraft by aircraftViewModel.aircraft.collectAsState()
    val selectedHex by aircraftViewModel.selectedAircraftHex.collectAsState()
    val flightDetails by aircraftViewModel.flightDetails.collectAsState()
    val userLocation by locationViewModel.currentLocation.collectAsState()
    val receiverLocations by aircraftViewModel.receiverLocations.collectAsState()
    val numberOfPlanes by aircraftViewModel.numberOfPlanes.collectAsState()
    val aircraftTrails by aircraftViewModel.aircraftTrails.collectAsState()
    val mapSelectedHex by mapViewModel.selectedAircraft.collectAsState()

    // Sync aircraft updates to map
    LaunchedEffect(aircraft) {
        mapViewModel.onAircraftUpdated(aircraft)
    }

    // Sync trails to map
    LaunchedEffect(aircraftTrails) {
        mapViewModel.onAircraftTrailsUpdated(aircraftTrails)
    }

    // Sync receiver locations to map
    LaunchedEffect(receiverLocations) {
        receiverLocations.forEach(mapViewModel::onReceiverLocation)
    }

    // Sync user location to map
    LaunchedEffect(userLocation) {
        userLocation?.let(mapViewModel::onUserLocationChanged)
    }

    // Handle map recenter requests
    LaunchedEffect(Unit) {
        locationViewModel.recenterMap.collect {
            mapViewModel.recenterOnLocation(it)
        }
    }

    // Sync map selection -> AircraftViewModel
    LaunchedEffect(mapSelectedHex) {
        if (mapSelectedHex != selectedHex) {
            aircraftViewModel.selectAircraft(mapSelectedHex)
        }
    }

    // Sync AircraftViewModel selection -> Map (when selecting from list)
    LaunchedEffect(selectedHex) {
        if (selectedHex != mapSelectedHex) {
            mapViewModel.syncSelection(selectedHex)
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // Map takes 60% of width
        Box(
            modifier =
                Modifier
                    .weight(0.6f)
                    .fillMaxHeight(),
        ) {
            OpenStreetMap(state = mapViewModel.state)
            Overlay(
                numberOfPlanes,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(horizontal = 8.dp),
            )
            // Settings button
            IconButton(
                onClick = onSettingsClick,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                colors =
                    IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    ),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_settings),
                    contentDescription = stringResource(Res.string.settings_title),
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        VerticalDivider()

        // List panel takes 40% of width
        Surface(
            modifier =
                Modifier
                    .weight(0.4f)
                    .fillMaxHeight(),
        ) {
            TabletAircraftListPanel(
                aircraft = aircraft,
                selectedHex = selectedHex,
                flightDetails = flightDetails,
                userLocation = userLocation,
                onAircraftSelected = { hex ->
                    aircraftViewModel.selectAircraft(hex)
                    mapViewModel.syncSelection(hex)
                },
                onOpenFlightPage = {
                    aircraftViewModel.openFlightPage(selectedHex)
                },
            )
        }
    }
}
