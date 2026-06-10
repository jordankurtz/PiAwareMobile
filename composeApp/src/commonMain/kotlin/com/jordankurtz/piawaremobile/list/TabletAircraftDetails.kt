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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import piawaremobile.composeapp.generated.resources.detected_by
import piawaremobile.composeapp.generated.resources.ic_arrow_back

@Composable
fun TabletAircraftDetails(
    aircraftWithServers: AircraftWithServers,
    flightDetails: Async<Flight>,
    userLocation: Location?,
    onClose: () -> Unit,
    onOpenFlightPage: () -> Unit,
) {
    val aircraft = aircraftWithServers.aircraft
    val info = aircraftWithServers.info

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_back),
                        contentDescription = stringResource(Res.string.back_to_list),
                    )
                }
                Column {
                    Text(
                        text = aircraft.flight?.trim() ?: aircraft.hex,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    info?.let {
                        if (it.subtitle.isNotEmpty()) {
                            Text(
                                text = it.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        if (aircraftWithServers.servers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text =
                    stringResource(
                        Res.string.detected_by,
                        aircraftWithServers.servers.joinToString(", ") { it.name },
                    ),
                modifier = Modifier.padding(start = 48.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(24.dp))
        FlightDetailsSection(
            aircraft = aircraft,
            flightDetails = flightDetails,
            onLoadFlightDetails = { },
            onOpenFlightPage = onOpenFlightPage,
        )
    }
}
