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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
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
import piawaremobile.composeapp.generated.resources.aircraft_list_clear_search
import piawaremobile.composeapp.generated.resources.aircraft_list_collapse
import piawaremobile.composeapp.generated.resources.aircraft_list_empty
import piawaremobile.composeapp.generated.resources.aircraft_list_expand
import piawaremobile.composeapp.generated.resources.aircraft_list_loading_flight_details
import piawaremobile.composeapp.generated.resources.aircraft_list_no_results
import piawaremobile.composeapp.generated.resources.aircraft_list_progress_percent
import piawaremobile.composeapp.generated.resources.aircraft_list_retry_flight_details
import piawaremobile.composeapp.generated.resources.aircraft_list_search_hint
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
    var searchQuery by remember { mutableStateOf("") }

    val filteredAircraft =
        remember(aircraft, searchQuery) {
            filterAircraft(aircraft, searchQuery)
        }

    Column(modifier = Modifier.fillMaxSize()) {
        ListHeader(aircraft = aircraft, filteredCount = filteredAircraft.size)
        AircraftSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
        )

        if (aircraft.isEmpty()) {
            EmptyAircraftList()
        } else if (filteredAircraft.isEmpty()) {
            NoSearchResults()
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    items = filteredAircraft,
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
internal fun ListHeader(
    aircraft: List<AircraftWithServers>,
    filteredCount: Int? = null,
) {
    val unitFeet = stringResource(Res.string.unit_feet)
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(Res.string.aircraft_list_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                val trackedValue =
                    if (filteredCount != null && filteredCount != aircraft.size) {
                        "$filteredCount / ${aircraft.size}"
                    } else {
                        aircraft.size.toString()
                    }
                StatItem(
                    label = stringResource(Res.string.aircraft_list_stat_tracked),
                    value = trackedValue,
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
            color = MaterialTheme.colorScheme.onSurface,
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = aircraft.flight?.trim() ?: aircraft.hex,
                        style = MaterialTheme.typography.titleSmall,
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                aircraft.altBaro?.let {
                    PrimaryCompactStat(value = it, unit = unitFeet)
                }
                aircraft.gs?.let {
                    PrimaryCompactStat(value = "${it.toInt()}", unit = unitKnots)
                }
                aircraft.track?.let {
                    SecondaryCompactStat(value = "${it.toInt()}°", unit = unitHeading)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                distance?.let {
                    SecondaryCompactStat(value = "${it.roundToInt()}", unit = unitKm)
                }
                aircraft.squawk?.let {
                    SecondaryCompactStat(value = it, unit = unitSquawk)
                }
            }
        }

        if (aircraftWithServers.servers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = aircraftWithServers.servers.joinToString(", ") { it.name },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                MiniMap(
                    aircraft = aircraft,
                    userLocation = userLocation,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        AircraftDetailsGrid(aircraft = aircraft, userLocation = userLocation)

                        info?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            AircraftInfoRow(it)
                        }
                    }
                }

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
private fun PrimaryCompactStat(
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
private fun SecondaryCompactStat(
    value: String,
    unit: String,
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            unit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
                FlightInfo(flight = flightDetails.data)
            }
        }

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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(Res.string.label_flight, flight.ident),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                flight.origin?.let { origin ->
                    Column {
                        Text(origin.code ?: "", style = MaterialTheme.typography.titleLarge)
                        Text(
                            origin.city ?: origin.name ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )

                flight.destination?.let { dest ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(dest.code ?: "", style = MaterialTheme.typography.titleLarge)
                        Text(
                            dest.city ?: dest.name ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

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

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                flight.aircraftType?.let {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(Res.string.label_type),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                flight.registration?.let {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(Res.string.label_registration),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                flight.operator?.let {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(Res.string.label_operator),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AircraftSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = {
            Text(
                stringResource(Res.string.aircraft_list_search_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(Res.string.aircraft_list_clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors =
            TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

@Composable
private fun NoSearchResults() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.aircraft_list_no_results),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun filterAircraft(
    aircraft: List<AircraftWithServers>,
    query: String,
): List<AircraftWithServers> {
    if (query.isBlank()) return aircraft
    val trimmed = query.trim().lowercase()
    return aircraft.filter { item ->
        item.aircraft.flight?.trim()?.lowercase()?.contains(trimmed) == true ||
            item.aircraft.hex.lowercase().contains(trimmed) ||
            item.info?.registration?.lowercase()?.contains(trimmed) == true ||
            item.info?.typeDescription?.lowercase()?.contains(trimmed) == true ||
            item.info?.icaoType?.lowercase()?.contains(trimmed) == true ||
            item.aircraft.squawk?.contains(trimmed) == true
    }
}
