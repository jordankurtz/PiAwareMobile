package com.jordankurtz.piawaremobile.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawaremobile.aircraft.AircraftViewModel
import com.jordankurtz.piawaremobile.location.LocationViewModel
import com.jordankurtz.piawaremobile.map.MiniMap
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftWithServers
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.Flight
import com.jordankurtz.piawaremobile.model.Location
import com.jordankurtz.piawaremobile.model.distanceTo
import com.jordankurtz.piawaremobile.ui.AircraftDetailsGrid
import com.jordankurtz.piawaremobile.ui.AircraftInfoRow
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.aircraft_list_collapse
import piawaremobile.composeapp.generated.resources.aircraft_list_empty
import piawaremobile.composeapp.generated.resources.aircraft_list_expand
import piawaremobile.composeapp.generated.resources.aircraft_list_loading_flight_details
import piawaremobile.composeapp.generated.resources.aircraft_list_progress_percent
import piawaremobile.composeapp.generated.resources.aircraft_list_retry_flight_details
import piawaremobile.composeapp.generated.resources.aircraft_list_route_arrow
import piawaremobile.composeapp.generated.resources.aircraft_list_stat_avg_altitude
import piawaremobile.composeapp.generated.resources.aircraft_list_stat_tracked
import piawaremobile.composeapp.generated.resources.aircraft_list_stat_with_position
import piawaremobile.composeapp.generated.resources.aircraft_list_title
import piawaremobile.composeapp.generated.resources.ic_expand_less
import piawaremobile.composeapp.generated.resources.ic_expand_more
import piawaremobile.composeapp.generated.resources.label_flight
import piawaremobile.composeapp.generated.resources.label_operator
import piawaremobile.composeapp.generated.resources.label_registration
import piawaremobile.composeapp.generated.resources.label_type
import piawaremobile.composeapp.generated.resources.open_in_flightaware
import piawaremobile.composeapp.generated.resources.unit_feet
import piawaremobile.composeapp.generated.resources.unit_heading
import piawaremobile.composeapp.generated.resources.unit_kilometers
import piawaremobile.composeapp.generated.resources.unit_knots
import piawaremobile.composeapp.generated.resources.unit_squawk
import kotlin.math.roundToInt

@Composable
fun AircraftListScreen(
    aircraftViewModel: AircraftViewModel = koinViewModel(),
    locationViewModel: LocationViewModel = koinViewModel(),
) {
    val aircraft by aircraftViewModel.aircraft.collectAsState()
    val userLocation by locationViewModel.currentLocation.collectAsState()
    val flightDetails by aircraftViewModel.flightDetails.collectAsState()
    var selectedFlightHex by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        ListHeader(aircraft = aircraft)

        if (aircraft.isEmpty()) {
            EmptyAircraftList()
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    items = aircraft,
                    key = { it.aircraft.hex },
                ) { aircraftWithServers ->
                    AircraftListItem(
                        aircraftWithServers = aircraftWithServers,
                        userLocation = userLocation,
                        flightDetails =
                            if (selectedFlightHex == aircraftWithServers.aircraft.hex) {
                                flightDetails
                            } else {
                                Async.NotStarted
                            },
                        onLoadFlightDetails = {
                            selectedFlightHex = aircraftWithServers.aircraft.hex
                            aircraftViewModel.openFlightInformation(aircraftWithServers.aircraft.hex)
                        },
                        onOpenFlightPage = {
                            aircraftViewModel.openFlightPage(aircraftWithServers.aircraft.hex)
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
internal fun ListHeader(aircraft: List<AircraftWithServers>) {
    val unitFeet = stringResource(Res.string.unit_feet)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.aircraft_list_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                StatItem(
                    label = stringResource(Res.string.aircraft_list_stat_tracked),
                    value = aircraft.size.toString(),
                )

                val withPosition = aircraft.count { it.aircraft.hasPosition }
                StatItem(
                    label = stringResource(Res.string.aircraft_list_stat_with_position),
                    value = withPosition.toString(),
                )

                val avgAltitude =
                    aircraft
                        .mapNotNull { it.aircraft.altBaro?.replace(",", "")?.toIntOrNull() }
                        .takeIf { it.isNotEmpty() }
                        ?.average()
                        ?.roundToInt()
                if (avgAltitude != null) {
                    StatItem(
                        label = stringResource(Res.string.aircraft_list_stat_avg_altitude),
                        value = "$avgAltitude$unitFeet",
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyAircraftList() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.aircraft_list_empty),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun AircraftListItem(
    aircraftWithServers: AircraftWithServers,
    userLocation: Location?,
    flightDetails: Async<Flight>,
    onLoadFlightDetails: () -> Unit,
    onOpenFlightPage: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val aircraft = aircraftWithServers.aircraft
    val info = aircraftWithServers.info

    LaunchedEffect(expanded) {
        if (expanded && !aircraft.flight.isNullOrBlank()) {
            onLoadFlightDetails()
        }
    }

    // Calculate distance
    val distance =
        if (aircraft.hasPosition && userLocation != null) {
            userLocation.distanceTo(Location(aircraft.lat, aircraft.lon))
        } else {
            null
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // Header row - flight number and expand icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = aircraft.flight?.trim() ?: aircraft.hex,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    info?.icaoType?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                // Second line: registration and type description
                info?.subtitle?.takeIf { it.isNotEmpty() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Icon(
                painter = painterResource(if (expanded) Res.drawable.ic_expand_less else Res.drawable.ic_expand_more),
                contentDescription =
                    stringResource(
                        if (expanded) Res.string.aircraft_list_collapse else Res.string.aircraft_list_expand,
                    ),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Stats row
        val unitFeet = stringResource(Res.string.unit_feet)
        val unitKnots = stringResource(Res.string.unit_knots)
        val unitHeading = stringResource(Res.string.unit_heading)
        val unitKm = stringResource(Res.string.unit_kilometers)
        val unitSquawk = stringResource(Res.string.unit_squawk)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left side stats
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                aircraft.altBaro?.let {
                    CompactStat(value = it, unit = unitFeet)
                }
                aircraft.gs?.let {
                    CompactStat(value = "${it.toInt()}", unit = unitKnots)
                }
                aircraft.track?.let {
                    CompactStat(value = "${it.toInt()}°", unit = unitHeading)
                }
            }

            // Right side stats
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                distance?.let {
                    CompactStat(value = "${it.roundToInt()}", unit = unitKm)
                }
                aircraft.squawk?.let {
                    CompactStat(value = it, unit = unitSquawk)
                }
            }
        }

        // Server info
        if (aircraftWithServers.servers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = aircraftWithServers.servers.joinToString(", ") { it.name },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Expanded content
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                // MiniMap
                MiniMap(
                    aircraft = aircraft,
                    userLocation = userLocation,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Details grid
                AircraftDetailsGrid(aircraft = aircraft, userLocation = userLocation)

                // Aircraft info
                info?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    AircraftInfoRow(it)
                }

                // Flight details section
                Spacer(modifier = Modifier.height(12.dp))
                FlightDetailsSection(
                    aircraft = aircraft,
                    flightDetails = flightDetails,
                    onLoadFlightDetails = onLoadFlightDetails,
                    onOpenFlightPage = onOpenFlightPage,
                )
            }
        }
    }
}

@Composable
private fun CompactStat(
    value: String,
    unit: String,
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(2.dp))
        Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun FlightDetailsSection(
    aircraft: Aircraft,
    flightDetails: Async<Flight>,
    onLoadFlightDetails: () -> Unit,
    onOpenFlightPage: () -> Unit,
) {
    Column {
        when (flightDetails) {
            Async.NotStarted -> {
                // Auto-trigger happens elsewhere; nothing to show
            }
            Async.Loading -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.aircraft_list_loading_flight_details))
                }
            }
            is Async.Error -> {
                Text(
                    text = flightDetails.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = onLoadFlightDetails) {
                    Text(stringResource(Res.string.aircraft_list_retry_flight_details))
                }
            }
            is Async.Success -> {
                val flight = flightDetails.data
                FlightInfo(flight = flight)
            }
        }

        // Always show "Open in FlightAware" when flight is available
        if (!aircraft.flight.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onOpenFlightPage,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.open_in_flightaware))
            }
        }
    }
}

@Composable
internal fun FlightInfo(flight: Flight) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.label_flight, flight.ident),
            style = MaterialTheme.typography.titleSmall,
        )

        // Route info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            flight.origin?.let { origin ->
                Column {
                    Text(origin.code ?: "", style = MaterialTheme.typography.titleMedium)
                    Text(origin.city ?: origin.name ?: "", style = MaterialTheme.typography.bodySmall)
                }
            }

            Text(stringResource(Res.string.aircraft_list_route_arrow), style = MaterialTheme.typography.titleLarge)

            flight.destination?.let { dest ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(dest.code ?: "", style = MaterialTheme.typography.titleMedium)
                    Text(dest.city ?: dest.name ?: "", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Progress bar
        flight.progressPercent?.let { progress ->
            if (progress in 0..100) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(Res.string.aircraft_list_progress_percent, progress),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Aircraft info from FlightAware
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            flight.aircraftType?.let {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(Res.string.label_type), style = MaterialTheme.typography.labelSmall)
                }
            }
            flight.registration?.let {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(Res.string.label_registration), style = MaterialTheme.typography.labelSmall)
                }
            }
            flight.operator?.let {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(Res.string.label_operator), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
