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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.jordankurtz.piawaremobile.model.Location
import com.jordankurtz.piawaremobile.model.bearingTo
import com.jordankurtz.piawaremobile.model.distanceTo
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.ic_expand_less
import piawaremobile.composeapp.generated.resources.ic_expand_more
import kotlin.math.roundToInt

@Composable
fun AircraftListScreen(
    aircraftViewModel: AircraftViewModel = koinViewModel(),
    locationViewModel: LocationViewModel = koinViewModel()
) {
    val aircraft by aircraftViewModel.aircraft.collectAsState()
    val userLocation by locationViewModel.currentLocation.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        ListHeader(aircraft = aircraft)

        if (aircraft.isEmpty()) {
            EmptyAircraftList()
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    items = aircraft,
                    key = { it.aircraft.hex }
                ) { aircraftWithServers ->
                    AircraftListItem(
                        aircraftWithServers = aircraftWithServers,
                        userLocation = userLocation
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ListHeader(aircraft: List<AircraftWithServers>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Aircraft",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                StatItem(label = "Tracked", value = aircraft.size.toString())

                val withPosition = aircraft.count { it.aircraft.lat != 0.0 && it.aircraft.lon != 0.0 }
                StatItem(label = "With Position", value = withPosition.toString())

                val avgAltitude = aircraft
                    .mapNotNull { it.aircraft.altBaro?.replace(",", "")?.toIntOrNull() }
                    .takeIf { it.isNotEmpty() }
                    ?.average()
                    ?.roundToInt()
                if (avgAltitude != null) {
                    StatItem(label = "Avg Alt", value = "${avgAltitude}ft")
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyAircraftList() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No aircraft detected",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun AircraftListItem(
    aircraftWithServers: AircraftWithServers,
    userLocation: Location?
) {
    var expanded by remember { mutableStateOf(false) }
    val aircraft = aircraftWithServers.aircraft

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = aircraft.flight?.trim() ?: aircraft.hex,
                    style = MaterialTheme.typography.titleMedium
                )
                aircraftWithServers.info?.let { info ->
                    info.registration?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick stats
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                aircraft.altBaro?.let {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                        Text("ft", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                aircraft.gs?.let {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${it.toInt()}", style = MaterialTheme.typography.bodyMedium)
                        Text("kts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Icon(
                painter = painterResource(if (expanded) Res.drawable.ic_expand_less else Res.drawable.ic_expand_more),
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Server info
        if (aircraftWithServers.servers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = aircraftWithServers.servers.joinToString(", ") { it.name },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Expanded content
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                // MiniMap
                MiniMap(
                    aircraft = aircraft,
                    userLocation = userLocation
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Details grid
                AircraftDetailsGrid(aircraft = aircraft, userLocation = userLocation)

                // Aircraft info
                aircraftWithServers.info?.let { info ->
                    Spacer(modifier = Modifier.height(8.dp))
                    AircraftInfoRow(info)
                }
            }
        }
    }
}

@Composable
private fun AircraftDetailsGrid(aircraft: Aircraft, userLocation: Location?) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            aircraft.altBaro?.let {
                DetailItem(label = "Altitude", value = "$it ft")
            }
            aircraft.track?.let {
                DetailItem(label = "Heading", value = "$it°")
            }
            aircraft.gs?.let {
                DetailItem(label = "Speed", value = "${it.toInt()} kts")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            aircraft.baroRate?.let {
                DetailItem(label = "Vertical Speed", value = "$it fpm")
            }
            aircraft.squawk?.let {
                DetailItem(label = "Squawk", value = it)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            if (aircraft.lat != 0.0 && aircraft.lon != 0.0) {
                DetailItem(
                    label = "Location",
                    value = "(${aircraft.lat.round(4)}, ${aircraft.lon.round(4)})"
                )

                userLocation?.let { location ->
                    val aircraftLocation = Location(aircraft.lat, aircraft.lon)
                    val distance = location.distanceTo(aircraftLocation)
                    val bearing = location.bearingTo(aircraftLocation)

                    DetailItem(label = "Distance", value = "${distance.roundToInt()} km")
                    DetailItem(label = "Direction", value = "${bearing.roundToInt()}° ${bearing.toCardinalDirection()}")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            aircraft.rssi?.let {
                DetailItem(label = "Signal", value = "$it dBm")
            }
            aircraft.seen?.let {
                DetailItem(label = "Last Seen", value = "$it s ago")
            }
        }
    }
}

@Composable
private fun AircraftInfoRow(info: com.jordankurtz.piawaremobile.model.AircraftInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        info.icaoType?.let {
            DetailItem(label = "Type", value = it)
        }
        info.typeDescription?.let {
            DetailItem(label = "Description", value = it)
        }
        info.wtc?.let {
            DetailItem(label = "WTC", value = it)
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}

private fun Double.toCardinalDirection(): String {
    val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val index = ((this / 45) + 0.5).toInt() % 8
    return directions[index]
}
