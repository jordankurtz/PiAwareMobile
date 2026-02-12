package com.jordankurtz.piawaremobile.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawaremobile.model.AircraftWithServers
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.Flight
import com.jordankurtz.piawaremobile.model.Location
import com.jordankurtz.piawaremobile.ui.AircraftDetailsGrid
import com.jordankurtz.piawaremobile.ui.AircraftInfoRow
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.back_to_list
import piawaremobile.composeapp.generated.resources.ic_arrow_back

@Composable
fun TabletAircraftDetails(
    aircraftWithServers: AircraftWithServers,
    flightDetails: Async<Flight>,
    userLocation: Location?,
    onClose: () -> Unit,
    onOpenFlightPage: () -> Unit
) {
    val aircraft = aircraftWithServers.aircraft
    val info = aircraftWithServers.info

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_back),
                        contentDescription = stringResource(Res.string.back_to_list)
                    )
                }
                Column {
                    Text(
                        text = aircraft.flight?.trim() ?: aircraft.hex,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    info?.let {
                        val subtitle = buildList {
                            it.registration?.let { reg -> add(reg) }
                            it.typeDescription?.let { desc -> add(desc) }
                        }.joinToString(" - ")
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Aircraft details grid (no MiniMap - main map is visible)
        AircraftDetailsGrid(aircraft = aircraft, userLocation = userLocation)

        // Aircraft info row
        info?.let {
            Spacer(modifier = Modifier.height(16.dp))
            AircraftInfoRow(it)
        }

        // Server info
        if (aircraftWithServers.servers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Detected by: ${aircraftWithServers.servers.joinToString(", ") { it.name }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Flight details section
        Spacer(modifier = Modifier.height(24.dp))
        FlightDetailsSection(
            aircraft = aircraft,
            flightDetails = flightDetails,
            onLoadFlightDetails = { /* retry handled by re-selecting */ },
            onOpenFlightPage = onOpenFlightPage
        )
    }
}
