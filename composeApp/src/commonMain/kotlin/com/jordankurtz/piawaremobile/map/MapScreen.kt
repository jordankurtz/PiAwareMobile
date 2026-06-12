package com.jordankurtz.piawaremobile.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.jordankurtz.piawaremobile.Overlay
import com.jordankurtz.piawaremobile.aircraft.AircraftViewModel
import com.jordankurtz.piawaremobile.isDebugBuild
import com.jordankurtz.piawaremobile.location.LocationViewModel
import com.jordankurtz.piawaremobile.map.debug.TileCacheDebugOverlay
import com.jordankurtz.piawaremobile.map.ui.CompassFab
import com.jordankurtz.piawaremobile.map.ui.MapFab
import com.jordankurtz.piawaremobile.map.ui.OverlaySheet
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.fit_to_aircraft
import piawaremobile.composeapp.generated.resources.follow_user_location
import piawaremobile.composeapp.generated.resources.ic_plane
import piawaremobile.composeapp.generated.resources.ic_user_location
import piawaremobile.composeapp.generated.resources.show_overlays

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
    val activeProvider by mapViewModel.activeProvider.collectAsState()
    val selectedAircraftHex by mapViewModel.selectedAircraft.collectAsState()
    val followingAircraftHex by mapViewModel.followingAircraft.collectAsState()
    val isFollowingUser by mapViewModel.followingUserLocation.collectAsState()
    val showUserLocationOnMap by mapViewModel.showUserLocationOnMap.collectAsState()
    val showFaaCharts by mapViewModel.showFaaCharts.collectAsState()
    val showFaaIfrLow by mapViewModel.showFaaIfrLow.collectAsState()
    val showFaaIfrHigh by mapViewModel.showFaaIfrHigh.collectAsState()
    val showAirspace by mapViewModel.showAirspace.collectAsState()
    val showTfrs by mapViewModel.showTfrs.collectAsState()
    val openAipApiKey by mapViewModel.openAipApiKey.collectAsState()
    val anyOverlayActive = showFaaCharts || showFaaIfrLow || showFaaIfrHigh || showAirspace || showTfrs
    var showOverlaySheet by remember { mutableStateOf(false) }
    val tileStats by mapViewModel.tileStats.collectAsState()
    val currentZoom by mapViewModel.currentZoomLevel.collectAsState()
    val zoomSettings by mapViewModel.zoomSettings.collectAsState()
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

    var mapBearing by remember { mutableStateOf(0f) }

    MapLibreMap(
        controller = mapViewModel.mapStateController as MapLibreStateController,
        styleUrl = activeProvider.styleUrl,
        onBearingChanged = { mapBearing = it },
        faaChartsEnabled = showFaaCharts,
        faaIfrLowEnabled = showFaaIfrLow,
        faaIfrHighEnabled = showFaaIfrHigh,
        tfrsEnabled = showTfrs,
        airspaceEnabled = showAirspace,
        openAipApiKey = openAipApiKey,
        topStart = {
            Column {
                AnimatedVisibility(
                    visible = false,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.padding(16.dp),
                ) {
                    OfflineIndicator()
                }
                if (isDebugBuild) {
                    TileCacheDebugOverlay(
                        stats = tileStats,
                        currentZoom = currentZoom,
                        zoomSettings = zoomSettings,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                Overlay(
                    numberOfPlanes = numberOfPlanes,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        },
        topEnd = {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (aircraft.isNotEmpty()) {
                    MapFab(onClick = { mapViewModel.fitToAircraft(aircraft) }) {
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
                MapFab(
                    onClick = { showOverlaySheet = true },
                    active = anyOverlayActive,
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = stringResource(Res.string.show_overlays),
                        modifier = Modifier.size(24.dp),
                    )
                }
                CompassFab(
                    bearing = mapBearing,
                    onResetNorth = { mapViewModel.resetBearing() },
                )
            }
        },
    )

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

    if (showOverlaySheet) {
        OverlaySheet(
            showFaaCharts = showFaaCharts,
            showFaaIfrLow = showFaaIfrLow,
            showFaaIfrHigh = showFaaIfrHigh,
            showAirspace = showAirspace,
            showTfrs = showTfrs,
            hasOpenAipKey = openAipApiKey.isNotEmpty(),
            onToggleFaaCharts = { mapViewModel.toggleFaaCharts() },
            onToggleFaaIfrLow = { mapViewModel.toggleFaaIfrLow() },
            onToggleFaaIfrHigh = { mapViewModel.toggleFaaIfrHigh() },
            onToggleAirspace = { mapViewModel.toggleAirspace() },
            onToggleTfrs = { mapViewModel.toggleTfrs() },
            onDismiss = { showOverlaySheet = false },
        )
    }
}

@Composable
fun FollowUserLocationFab(
    isFollowing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MapFab(
        onClick = onClick,
        modifier = modifier,
        active = isFollowing,
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_user_location),
            contentDescription = stringResource(Res.string.follow_user_location),
            modifier = Modifier.size(24.dp),
        )
    }
}
