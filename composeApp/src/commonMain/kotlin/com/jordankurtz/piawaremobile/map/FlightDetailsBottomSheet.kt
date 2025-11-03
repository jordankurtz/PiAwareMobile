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
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.Flight
import com.jordankurtz.piawaremobile.model.FlightAirportRef
import org.jetbrains.compose.resources.painterResource
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.ic_arrow_downward
import kotlin.time.Instant

@Composable
fun FlightDetailsBottomSheet(
    flightDetails: Async<Flight?>,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
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
                    is Async.Error -> Text(text = "Error: ${flightDetails.message}")
                    Async.Loading -> CircularProgressIndicator()
                    is Async.Success<*> -> {
                        val flight = flightDetails.data as? Flight
                        if (flight != null) {
                            Text(text = "Flight ${flight.ident}", style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))

                            flight.origin?.let {
                                AirportInfo(
                                    title = "Departure",
                                    airport = it,
                                    scheduledTime = flight.scheduledOut,
                                    actualTime = flight.actualOut,
                                    estimatedTime = flight.estimatedOut
                                )
                            }

                            FlightProgress(flight = flight)

                            flight.destination?.let {
                                AirportInfo(
                                    title = "Destination",
                                    airport = it,
                                    scheduledTime = flight.scheduledIn,
                                    actualTime = flight.actualIn,
                                    estimatedTime = flight.estimatedIn
                                )
                            }
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
                contentDescription = "Flight progress"
            )
            if (remainingSeconds > 0) {
                val remainingString = formatSecondsToHoursMinutes(remainingSeconds.toInt())
                if (remainingString.isNotBlank()) {
                    Text(
                        text = "$remainingString remaining",
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
        Text(text = "${airport.name} (${airport.code})")
        Spacer(modifier = Modifier.height(8.dp))

        scheduledTime?.let { scheduled ->
            Text(text = "Scheduled: ${scheduled.formattedTime}")
        }

        actualTime?.let { actual ->
            val diff = calculateTimeDifference(scheduledTime, actual)
            Text(text = "Actual: ${actual.formattedTime} $diff")
        }

        // Only show estimated if actual is not present
        if (actualTime == null) {
            estimatedTime?.let { estimated ->
                val diff = calculateTimeDifference(scheduledTime, estimated)
                Text(text = "Estimated: ${estimated.formattedTime} $diff")
            }
        }
    }
}


private fun calculateTimeDifference(time1: Instant?, time2: Instant?): String {
    if (time1 == null || time2 == null) return ""
    return try {
        val differenceInMinutes = (time2 - time1).inWholeMinutes
        if (differenceInMinutes == 0L) {
            "(on time)"
        } else {
            val sign = if (differenceInMinutes > 0) "+" else ""
            "($sign${differenceInMinutes}m)"
        }
    } catch (e: Exception) {
        ""
    }
}

private fun formatSecondsToHoursMinutes(seconds: Int): String {
    if (seconds <= 0) return ""
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> ""
    }
}
