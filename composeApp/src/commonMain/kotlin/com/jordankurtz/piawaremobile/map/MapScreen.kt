package com.jordankurtz.piawaremobile.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.jordankurtz.piawaremobile.Overlay
import com.jordankurtz.piawaremobile.aircraft.AircraftViewModel
import com.jordankurtz.piawaremobile.location.LocationViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.fit_to_aircraft
import piawaremobile.composeapp.generated.resources.follow_user_location
import piawaremobile.composeapp.generated.resources.ic_plane
import piawaremobile.composeapp.generated.resources.ic_user_location

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
    val followingAircraftHex by mapViewModel.followingAircraft.collectAsState()
    val isFollowingUser by mapViewModel.followingUserLocation.collectAsState()
    val showUserLocationOnMap by mapViewModel.showUserLocationOnMap.collectAsState()
    val flightDetails by aircraftViewModel.flightDetails.collectAsState()
    val aircraftTrails by aircraftViewModel.aircraftTrails.collectAsState()
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
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
        aircraftViewModel.selectAircraft(selectedAircraftHex)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        aircraftViewModel.onResume()
    }

    Box {
        OpenStreetMap(state = mapViewModel.state)
        Column(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (aircraft.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { mapViewModel.fitToAircraft(aircraft) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_plane),
                        contentDescription = stringResource(Res.string.fit_to_aircraft),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            if (showUserLocationOnMap) {
                FollowUserLocationFab(
                    isFollowing = isFollowingUser,
                    onClick = { mapViewModel.toggleFollowUserLocation() },
                )
            }
        }
        Overlay(
            numberOfPlanes,
            modifier = Modifier.align(Alignment.BottomEnd).padding(horizontal = 8.dp),
        )
    }

    val selectedAircraft =
        selectedAircraftHex?.let { hex ->
            aircraft.firstOrNull { it.aircraft.hex == hex }?.aircraft
        }

    FlightDetailsBottomSheet(
        aircraft = selectedAircraft,
        flightDetails = flightDetails,
        isFollowing = followingAircraftHex != null,
        onDismissRequest = { mapViewModel.onAircraftDeselected() },
        onOpenFlightPage = { aircraftViewModel.openFlightPage(selectedAircraftHex) },
        onFollowToggle = {
            if (followingAircraftHex != null) {
                mapViewModel.unfollowAircraft()
            } else {
                mapViewModel.followSelectedAircraft()
            }
        },
        sheetState = sheetState,
    )
}

@Composable
fun FollowUserLocationFab(
    isFollowing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor =
            if (isFollowing) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_user_location),
            contentDescription = stringResource(Res.string.follow_user_location),
            modifier = Modifier.size(24.dp),
        )
    }
}
