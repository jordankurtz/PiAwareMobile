package com.jordankurtz.piawaremobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Layers
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawaremobile.Overlay
import com.jordankurtz.piawaremobile.aircraft.AircraftViewModel
import com.jordankurtz.piawaremobile.isDebugBuild
import com.jordankurtz.piawaremobile.list.TabletAircraftListPanel
import com.jordankurtz.piawaremobile.location.LocationViewModel
import com.jordankurtz.piawaremobile.map.FollowUserLocationFab
import com.jordankurtz.piawaremobile.map.MapLibreMap
import com.jordankurtz.piawaremobile.map.MapLibreStateController
import com.jordankurtz.piawaremobile.map.MapViewModel
import com.jordankurtz.piawaremobile.map.debug.TileCacheDebugOverlay
import com.jordankurtz.piawaremobile.map.ui.CompassFab
import com.jordankurtz.piawaremobile.map.ui.MapFab
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.fit_to_aircraft
import piawaremobile.composeapp.generated.resources.ic_plane
import piawaremobile.composeapp.generated.resources.ic_settings
import piawaremobile.composeapp.generated.resources.settings_title
import piawaremobile.composeapp.generated.resources.show_airspace
import piawaremobile.composeapp.generated.resources.show_faa_charts

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
    val tileStats by mapViewModel.tileStats.collectAsState()
    val currentZoom by mapViewModel.currentZoomLevel.collectAsState()
    val zoomSettings by mapViewModel.zoomSettings.collectAsState()
    val aircraftTrails by aircraftViewModel.aircraftTrails.collectAsState()
    val mapSelectedHex by mapViewModel.selectedAircraft.collectAsState()
    val activeProvider by mapViewModel.activeProvider.collectAsState()
    val showUserLocationOnMap by mapViewModel.showUserLocationOnMap.collectAsState()
    val isFollowingUser by mapViewModel.followingUserLocation.collectAsState()
    val showFaaCharts by mapViewModel.showFaaCharts.collectAsState()
    val showAirspace by mapViewModel.showAirspace.collectAsState()
    val openAipApiKey by mapViewModel.openAipApiKey.collectAsState()
    var mapBearing by remember { mutableStateOf(0f) }

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
            MapLibreMap(
                controller = mapViewModel.mapStateController as MapLibreStateController,
                styleUrl = activeProvider.styleUrl,
                onBearingChanged = { mapBearing = it },
                faaChartsEnabled = showFaaCharts,
                airspaceEnabled = showAirspace,
                openAipApiKey = openAipApiKey,
                topStart = {
                    Column {
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
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.size(40.dp),
                            colors =
                                IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_settings),
                                contentDescription = stringResource(Res.string.settings_title),
                                modifier = Modifier.size(24.dp),
                            )
                        }
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
                            onClick = { mapViewModel.toggleFaaCharts() },
                            active = showFaaCharts,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = stringResource(Res.string.show_faa_charts),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        MapFab(
                            onClick = { mapViewModel.toggleAirspace() },
                            active = showAirspace,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Air,
                                contentDescription = stringResource(Res.string.show_airspace),
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
        }

        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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
                },
                onOpenFlightPage = {
                    aircraftViewModel.openFlightPage(selectedHex)
                },
            )
        }
    }
}
