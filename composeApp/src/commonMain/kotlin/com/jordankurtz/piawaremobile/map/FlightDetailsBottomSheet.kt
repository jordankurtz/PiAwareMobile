package com.jordankurtz.piawaremobile.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawaremobile.extensions.formattedTime
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.Flight
import com.jordankurtz.piawaremobile.model.FlightAirportRef
import com.jordankurtz.piawaremobile.model.Location
import com.jordankurtz.piawaremobile.model.bearingTo
import com.jordankurtz.piawaremobile.model.distanceTo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.flight_details_actual
import piawaremobile.composeapp.generated.resources.flight_details_airport_name_code
import piawaremobile.composeapp.generated.resources.flight_details_departure
import piawaremobile.composeapp.generated.resources.flight_details_destination
import piawaremobile.composeapp.generated.resources.flight_details_error
import piawaremobile.composeapp.generated.resources.flight_details_estimated
import piawaremobile.composeapp.generated.resources.flight_details_flight_number
import piawaremobile.composeapp.generated.resources.flight_details_flight_progress
import piawaremobile.composeapp.generated.resources.flight_details_hours_minutes_short
import piawaremobile.composeapp.generated.resources.flight_details_minutes_diff
import piawaremobile.composeapp.generated.resources.flight_details_minutes_short
import piawaremobile.composeapp.generated.resources.flight_details_on_time
import piawaremobile.composeapp.generated.resources.flight_details_remaining_time
import piawaremobile.composeapp.generated.resources.flight_details_scheduled
import piawaremobile.composeapp.generated.resources.ic_arrow_downward
import kotlin.math.roundToInt
import kotlin.time.Instant

@Composable
fun FlightDetailsBottomSheet(
    aircraft: Aircraft?,
    userLocation: Location?,
    flightDetails: Async<Flight>,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    ),
) {
    if (flightDetails !is Async.NotStarted) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (flightDetails) {
                    is Async.Error -> Text(text = stringResource(Res.string.flight_details_error, flightDetails.message))
                    Async.Loading -> CircularProgressIndicator()
                    is Async.Success -> {
                        val flight = flightDetails.data

                        Text(text = stringResource(Res.string.flight_details_flight_number, flight.ident), style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            aircraft?.altBaro?.let {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Altitude", style = MaterialTheme.typography.labelSmall)
                                    Text("$it ft", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                            aircraft?.track?.let {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Heading", style = MaterialTheme.typography.labelSmall)
                                    Text("$it°", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                            aircraft?.gs?.let {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Speed", style = MaterialTheme.typography.labelSmall)
                                    Text("$it kts", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            aircraft?.lat?.let { lat ->
                                aircraft.lon?.let { lon ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Location", style = MaterialTheme.typography.labelSmall)
                                        Text("(${lat.round(4)}, ${lon.round(4)})", style = MaterialTheme.typography.bodyLarge)
                                    }
                                    userLocation?.let {
                                        val aircraftLocation = Location(lat, lon)
                                        val distance = it.distanceTo(aircraftLocation)
                                        val bearing = it.bearingTo(aircraftLocation)
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Distance", style = MaterialTheme.typography.labelSmall)
                                            Text("${distance.roundToInt()} km", style = MaterialTheme.typography.bodyLarge)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Direction", style = MaterialTheme.typography.labelSmall)
                                            Text("${bearing.roundToInt()}° ${bearing.toCardinalDirection()}", style = MaterialTheme.typography.bodyLarge)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        MoreDetails(aircraft, flight)

                        flight.origin?.let {
                            AirportInfo(
                                title = stringResource(Res.string.flight_details_departure),
                                airport = it,
                                scheduledTime = flight.scheduledOut,
                                actualTime = flight.actualOut,
                                estimatedTime = flight.estimatedOut
                            )
                        }

                        FlightProgress(flight = flight)

                        flight.destination?.let {
                            AirportInfo(
                                title = stringResource(Res.string.flight_details_destination),
                                airport = it,
                                scheduledTime = flight.scheduledIn,
                                actualTime = flight.actualIn,
                                estimatedTime = flight.estimatedIn
                            )
                        }
                    }

                    Async.NotStarted -> {
                        // Do nothing
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreDetails(aircraft: Aircraft?, flight: Flight) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        aircraft?.baroRate?.let {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Vertical Speed", style = MaterialTheme.typography.labelSmall)
                Text("$it fpm", style = MaterialTheme.typography.bodyLarge)
            }
        }
        aircraft?.squawk?.let {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Squawk", style = MaterialTheme.typography.labelSmall)
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
        }
        flight.aircraftType?.let {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Aircraft Type", style = MaterialTheme.typography.labelSmall)
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
        }
        flight.registration?.let {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Registration", style = MaterialTheme.typography.labelSmall)
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        aircraft?.rssi?.let {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Signal Strength", style = MaterialTheme.typography.labelSmall)
                Text("$it dBm", style = MaterialTheme.typography.bodyLarge)
            }
        }
        aircraft?.seen?.let {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Last Seen", style = MaterialTheme.typography.labelSmall)
                Text("$it s ago", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun FlightProgress(flight: Flight) {
    val ete = flight.filedEte
    val progress = flight.progressPercent
    if (ete != null && progress != null && progress in 0..100) {
        val remainingSeconds = ete * (1.0 - progress / 100.0)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_downward),
                contentDescription = stringResource(Res.string.flight_details_flight_progress)
            )
            if (remainingSeconds > 0) {
                val remainingString = formatSecondsToHoursMinutes(remainingSeconds.toInt())
                if (remainingString.isNotBlank()) {
                    Text(
                        text = stringResource(Res.string.flight_details_remaining_time, remainingString),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
        LinearProgressIndicator(
            progress = { (progress / 100.0).toFloat() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AirportInfo(
    modifier: Modifier = Modifier,
    title: String,
    airport: FlightAirportRef,
    scheduledTime: Instant?,
    actualTime: Instant?,
    estimatedTime: Instant?
) {
    Column(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (!airport.city.isNullOrBlank()) {
            Text(text = airport.city)
        }
        Text(text = stringResource(Res.string.flight_details_airport_name_code, airport.name.orEmpty(), airport.code.orEmpty()))
        Spacer(modifier = Modifier.height(8.dp))

        scheduledTime?.let { scheduled ->
            Text(text = stringResource(Res.string.flight_details_scheduled, scheduled.formattedTime))
        }

        actualTime?.let { actual ->
            val diff = calculateTimeDifference(scheduledTime, actual)
            Text(text = stringResource(Res.string.flight_details_actual, actual.formattedTime, diff))
        }

        // Only show estimated if actual is not present
        if (actualTime == null) {
            estimatedTime?.let { estimated ->
                val diff = calculateTimeDifference(scheduledTime, estimated)
                Text(text = stringResource(Res.string.flight_details_estimated, estimated.formattedTime, diff))
            }
        }
    }
}


@Composable
private fun calculateTimeDifference(time1: Instant?, time2: Instant?): String {
    if (time1 == null || time2 == null) return ""
    val differenceInMinutes = (time2 - time1).inWholeMinutes
    return if (differenceInMinutes == 0L) {
        stringResource(Res.string.flight_details_on_time)
    } else {
        val sign = if (differenceInMinutes > 0) "+" else ""
        stringResource(Res.string.flight_details_minutes_diff, sign, differenceInMinutes)
    }
}

@Composable
private fun formatSecondsToHoursMinutes(seconds: Int): String {
    if (seconds <= 0) return ""
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> stringResource(Res.string.flight_details_hours_minutes_short, hours, minutes)
        minutes > 0 -> stringResource(Res.string.flight_details_minutes_short, minutes)
        else -> ""
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
