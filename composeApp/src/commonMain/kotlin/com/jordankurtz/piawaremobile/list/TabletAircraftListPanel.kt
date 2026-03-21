package com.jordankurtz.piawaremobile.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawaremobile.model.AircraftWithServers
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.Flight
import com.jordankurtz.piawaremobile.model.Location
import com.jordankurtz.piawaremobile.model.distanceTo
import kotlin.math.roundToInt

@Composable
fun TabletAircraftListPanel(
    aircraft: List<AircraftWithServers>,
    selectedHex: String?,
    flightDetails: Async<Flight>,
    userLocation: Location?,
    onAircraftSelected: (String?) -> Unit,
    onOpenFlightPage: () -> Unit,
) {
    val selectedAircraft = aircraft.find { it.aircraft.hex == selectedHex }
    var searchQuery by remember { mutableStateOf("") }

    val filteredAircraft =
        remember(aircraft, searchQuery) {
            filterAircraft(aircraft, searchQuery)
        }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with stats
        ListHeader(aircraft = aircraft)

        if (selectedAircraft != null) {
            // Show selected aircraft details (no minimap)
            TabletAircraftDetails(
                aircraftWithServers = selectedAircraft,
                flightDetails = flightDetails,
                userLocation = userLocation,
                onClose = { onAircraftSelected(null) },
                onOpenFlightPage = onOpenFlightPage,
            )
        } else {
            AircraftSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
            )
            // Show aircraft list
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    items = filteredAircraft,
                    key = { it.aircraft.hex },
                ) { item ->
                    TabletAircraftListItem(
                        aircraftWithServers = item,
                        userLocation = userLocation,
                        isSelected = item.aircraft.hex == selectedHex,
                        onClick = { onAircraftSelected(item.aircraft.hex) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun TabletAircraftListItem(
    aircraftWithServers: AircraftWithServers,
    userLocation: Location?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val aircraft = aircraftWithServers.aircraft
    val info = aircraftWithServers.info

    val distance =
        if (aircraft.hasPosition && userLocation != null) {
            userLocation.distanceTo(Location(aircraft.lat, aircraft.lon))
        } else {
            null
        }

    val backgroundColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Flight info
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

            // Subtitle: registration and type
            info?.subtitle?.takeIf { it.isNotEmpty() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Right side info
        Column(horizontalAlignment = Alignment.End) {
            aircraft.altBaro?.let {
                Text(
                    text = "$it ft",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            distance?.let {
                Text(
                    text = "${it.roundToInt()} km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
