package com.jordankurtz.piawaremobile.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SheetState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.jordankurtz.piawaremobile.extensions.formattedTime
import com.jordankurtz.piawaremobile.location.LocationViewModel
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.Flight
import com.jordankurtz.piawaremobile.model.FlightAirportRef
import com.jordankurtz.piawaremobile.model.Location
import com.jordankurtz.piawaremobile.ui.AircraftLocationDetails
import com.jordankurtz.piawaremobile.ui.AircraftPrimaryDetails
import com.jordankurtz.piawaremobile.ui.AircraftSecondaryDetails
import com.jordankurtz.piawaremobile.ui.AircraftSignalDetails
import com.jordankurtz.piawaremobile.ui.FlightAircraftDetails
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
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
import piawaremobile.composeapp.generated.resources.follow_aircraft
import piawaremobile.composeapp.generated.resources.ic_arrow_downward
import piawaremobile.composeapp.generated.resources.open_in_flightaware
import piawaremobile.composeapp.generated.resources.unfollow_aircraft
import kotlin.time.Instant

@Suppress("LongParameterList")
@Composable
fun FlightDetailsBottomSheet(
    aircraft: Aircraft?,
    flightDetails: Async<Flight>,
    isFollowing: Boolean = false,
    onDismissRequest: () -> Unit,
    onOpenFlightPage: () -> Unit,
    onFollowToggle: () -> Unit = {},
    sheetState: SheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        ),
    locationViewModel: LocationViewModel = koinViewModel(),
) {
    val userLocation by locationViewModel.currentLocation.collectAsState()
    if (flightDetails !is Async.NotStarted) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
        ) {
            FlightDetailsSheetContent(
                aircraft = aircraft,
                flightDetails = flightDetails,
                isFollowing = isFollowing,
                userLocation = userLocation,
                onFollowToggle = onFollowToggle,
                onOpenFlightPage = onOpenFlightPage,
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
fun FlightDetailsSheetContent(
    aircraft: Aircraft?,
    flightDetails: Async<Flight>,
    isFollowing: Boolean = false,
    userLocation: Location? = null,
    onFollowToggle: () -> Unit = {},
    onOpenFlightPage: () -> Unit = {},
) {
    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Details", "Aircraft", "Route")

    Column(
        modifier =
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (flightDetails) {
            is Async.Error ->
                Text(
                    text = stringResource(Res.string.flight_details_error, flightDetails.message),
                )
            Async.Loading -> CircularProgressIndicator()
            is Async.Success -> {
                val flight = flightDetails.data

                Text(
                    text = stringResource(Res.string.flight_details_flight_number, flight.ident),
                    style = MaterialTheme.typography.headlineSmall,
                )

                Spacer(modifier = Modifier.height(8.dp))
                FlightDetailsActionButtons(
                    aircraft = aircraft,
                    isFollowing = isFollowing,
                    onFollowToggle = onFollowToggle,
                    onOpenFlightPage = onOpenFlightPage,
                )

                Spacer(modifier = Modifier.height(8.dp))

                PrimaryTabRow(selectedTabIndex = tabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            text = { Text(title) },
                            selected = tabIndex == index,
                            onClick = { tabIndex = index },
                        )
                    }
                }

                when (tabIndex) {
                    0 -> DetailsTab(aircraft, userLocation)
                    1 -> AircraftTab(aircraft, flight)
                    2 -> RouteTab(flight)
                }
            }

            Async.NotStarted -> {
                // Do nothing
            }
        }
    }
}

@Composable
fun FlightDetailsActionButtons(
    aircraft: Aircraft?,
    isFollowing: Boolean,
    onFollowToggle: () -> Unit,
    onOpenFlightPage: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onFollowToggle) {
            Text(
                if (isFollowing) {
                    stringResource(Res.string.unfollow_aircraft)
                } else {
                    stringResource(Res.string.follow_aircraft)
                },
            )
        }
        if (!aircraft?.flight.isNullOrBlank()) {
            OutlinedButton(onClick = onOpenFlightPage) {
                Text(stringResource(Res.string.open_in_flightaware))
            }
        }
    }
}

@Composable
private fun DetailsTab(
    aircraft: Aircraft?,
    userLocation: Location?,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(16.dp))
        MiniMap(
            aircraft = aircraft,
            userLocation = userLocation,
        )
        Spacer(modifier = Modifier.height(16.dp))

        aircraft?.let {
            AircraftPrimaryDetails(aircraft = it)
            Spacer(modifier = Modifier.height(8.dp))
            AircraftLocationDetails(aircraft = it, userLocation = userLocation)
        }
    }
}

@Composable
private fun AircraftTab(
    aircraft: Aircraft?,
    flight: Flight,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(16.dp))
        FlightAircraftDetails(flight = flight)
        Spacer(modifier = Modifier.height(8.dp))
        aircraft?.let {
            AircraftSecondaryDetails(aircraft = it)
            Spacer(modifier = Modifier.height(8.dp))
            AircraftSignalDetails(aircraft = it)
        }
    }
}

@Composable
private fun RouteTab(flight: Flight) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(16.dp))
        flight.origin?.let {
            AirportInfo(
                title = stringResource(Res.string.flight_details_departure),
                airport = it,
                scheduledTime = flight.scheduledOut,
                actualTime = flight.actualOut,
                estimatedTime = flight.estimatedOut,
            )
        }

        FlightProgress(flight = flight)

        flight.destination?.let {
            AirportInfo(
                title = stringResource(Res.string.flight_details_destination),
                airport = it,
                scheduledTime = flight.scheduledIn,
                actualTime = flight.actualIn,
                estimatedTime = flight.estimatedIn,
            )
        }
    }
}

@Composable
fun MiniMap(
    aircraft: Aircraft?,
    userLocation: Location?,
    miniMapViewModel: MiniMapViewModel = koinViewModel(),
) {
    LaunchedEffect(key1 = aircraft, key2 = userLocation) {
        miniMapViewModel.updateMapState(aircraft = aircraft, location = userLocation)
    }

    Card {
        OpenStreetMap(
            state = miniMapViewModel.state,
            modifier = Modifier.height(height = 200.dp),
        )
    }
}

@Composable
fun FlightProgress(flight: Flight) {
    val ete = flight.filedEte
    val progress = flight.progressPercent
    if (ete != null && progress != null && progress in 0..100) {
        val remainingSeconds = ete * (1.0 - progress / 100.0)

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_downward),
                contentDescription = stringResource(Res.string.flight_details_flight_progress),
            )
            if (remainingSeconds > 0) {
                val remainingString = formatSecondsToHoursMinutes(remainingSeconds.toInt())
                if (remainingString.isNotBlank()) {
                    Text(
                        text = stringResource(Res.string.flight_details_remaining_time, remainingString),
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        }
        LinearProgressIndicator(
            progress = { (progress / 100.0).toFloat() },
            modifier = Modifier.fillMaxWidth(),
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
    estimatedTime: Instant?,
) {
    Column(
        modifier =
            modifier
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (!airport.city.isNullOrBlank()) {
            Text(text = airport.city)
        }
        Text(
            text =
                stringResource(
                    Res.string.flight_details_airport_name_code,
                    airport.name.orEmpty(),
                    airport.code.orEmpty(),
                ),
        )
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
private fun calculateTimeDifference(
    time1: Instant?,
    time2: Instant?,
): String {
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
