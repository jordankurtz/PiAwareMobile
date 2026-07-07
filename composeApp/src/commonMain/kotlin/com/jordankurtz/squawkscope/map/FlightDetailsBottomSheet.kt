package com.jordankurtz.squawkscope.map

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jordankurtz.squawkscope.extensions.formattedTime
import com.jordankurtz.squawkscope.location.LocationViewModel
import com.jordankurtz.squawkscope.model.Aircraft
import com.jordankurtz.squawkscope.model.Async
import com.jordankurtz.squawkscope.model.Flight
import com.jordankurtz.squawkscope.model.FlightAirportRef
import com.jordankurtz.squawkscope.model.Location
import com.jordankurtz.squawkscope.squawk.SquawkCodes
import com.jordankurtz.squawkscope.squawk.SquawkSeverity
import com.jordankurtz.squawkscope.squawk.ui.SquawkInfoDialog
import com.jordankurtz.squawkscope.ui.AircraftLocationDetails
import com.jordankurtz.squawkscope.ui.AircraftPrimaryDetails
import com.jordankurtz.squawkscope.ui.AircraftSecondaryDetails
import com.jordankurtz.squawkscope.ui.AircraftSignalDetails
import com.jordankurtz.squawkscope.ui.AppTheme
import com.jordankurtz.squawkscope.ui.FlightAircraftDetails
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import squawkscope.composeapp.generated.resources.Res
import squawkscope.composeapp.generated.resources.aircraft_list_loading_flight_details
import squawkscope.composeapp.generated.resources.aircraft_list_progress_percent
import squawkscope.composeapp.generated.resources.flight_details_actual
import squawkscope.composeapp.generated.resources.flight_details_airport_name_code
import squawkscope.composeapp.generated.resources.flight_details_departure
import squawkscope.composeapp.generated.resources.flight_details_destination
import squawkscope.composeapp.generated.resources.flight_details_error
import squawkscope.composeapp.generated.resources.flight_details_estimated
import squawkscope.composeapp.generated.resources.flight_details_flight_number
import squawkscope.composeapp.generated.resources.flight_details_flight_progress
import squawkscope.composeapp.generated.resources.flight_details_hours_minutes_short
import squawkscope.composeapp.generated.resources.flight_details_minutes_diff
import squawkscope.composeapp.generated.resources.flight_details_minutes_short
import squawkscope.composeapp.generated.resources.flight_details_on_time
import squawkscope.composeapp.generated.resources.flight_details_remaining_time
import squawkscope.composeapp.generated.resources.flight_details_scheduled
import squawkscope.composeapp.generated.resources.follow_aircraft
import squawkscope.composeapp.generated.resources.ic_arrow_downward
import squawkscope.composeapp.generated.resources.open_in_flightaware
import squawkscope.composeapp.generated.resources.unfollow_aircraft
import kotlin.time.Instant

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
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
    if (aircraft != null) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            dragHandle = null,
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
@OptIn(ExperimentalMaterial3Api::class)
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
    val aircraftTabIndex = tabs.indexOf("Aircraft")
    var squawkForDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(flightDetails) {
        squawkForDialog = null
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DragHandle()

        when (flightDetails) {
            is Async.Error -> {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(Res.string.flight_details_error, flightDetails.message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlightDetailsActionButtons(
                        aircraft = aircraft,
                        isFollowing = isFollowing,
                        onFollowToggle = onFollowToggle,
                        onOpenFlightPage = onOpenFlightPage,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailsTab(aircraft, userLocation)
                }
            }

            Async.Loading -> {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.aircraft_list_loading_flight_details),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlightDetailsActionButtons(
                        aircraft = aircraft,
                        isFollowing = isFollowing,
                        onFollowToggle = onFollowToggle,
                        onOpenFlightPage = onOpenFlightPage,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailsTab(aircraft, userLocation)
                }
            }

            is Async.Success -> {
                val flight = flightDetails.data

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(Res.string.flight_details_flight_number, flight.ident),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    flight.operator?.let { operator ->
                        Text(
                            text = operator,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    FlightDetailsActionButtons(
                        aircraft = aircraft,
                        isFollowing = isFollowing,
                        onFollowToggle = onFollowToggle,
                        onOpenFlightPage = onOpenFlightPage,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                PillTabRow(
                    tabs = tabs,
                    selectedIndex = tabIndex,
                    onTabSelected = { tabIndex = it },
                )

                AnimatedContent(
                    targetState = tabIndex,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    label = "tab_content",
                ) { index ->
                    when (index) {
                        0 -> DetailsTab(aircraft, userLocation)
                        aircraftTabIndex -> AircraftTab(aircraft, flight, onSquawkClick = { squawkForDialog = it })
                        else -> RouteTab(flight)
                    }
                }
            }

            Async.NotStarted -> {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    FlightDetailsActionButtons(
                        aircraft = aircraft,
                        isFollowing = isFollowing,
                        onFollowToggle = onFollowToggle,
                        onOpenFlightPage = onOpenFlightPage,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                DetailsTab(aircraft, userLocation)
            }
        }
    }

    squawkForDialog?.let { squawk ->
        SquawkInfoDialog(
            squawk = squawk,
            onDismiss = { squawkForDialog = null },
        )
    }
}

@Composable
private fun DragHandle() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp)
                .semantics { contentDescription = "" },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier =
                Modifier
                    .width(32.dp)
                    .height(4.dp),
            shape = RoundedCornerShape(2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            content = {},
        )
    }
}

@Composable
fun FlightDetailsActionButtons(
    aircraft: Aircraft?,
    isFollowing: Boolean,
    onFollowToggle: () -> Unit,
    onOpenFlightPage: () -> Unit,
) {
    val compactPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (isFollowing) {
            FilledTonalButton(
                onClick = onFollowToggle,
                contentPadding = compactPadding,
            ) {
                Text(stringResource(Res.string.unfollow_aircraft))
            }
        } else {
            OutlinedButton(
                onClick = onFollowToggle,
                border = ButtonDefaults.outlinedButtonBorder(),
                contentPadding = compactPadding,
            ) {
                Text(stringResource(Res.string.follow_aircraft))
            }
        }
        if (!aircraft?.flight.isNullOrBlank()) {
            OutlinedButton(
                onClick = onOpenFlightPage,
                border = ButtonDefaults.outlinedButtonBorder(),
                contentPadding = compactPadding,
            ) {
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
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        MiniMap(
            aircraft = aircraft,
            userLocation = userLocation,
        )
        Spacer(modifier = Modifier.height(12.dp))

        aircraft?.let {
            Card(
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AircraftPrimaryDetails(
                    aircraft = it,
                    modifier = Modifier.padding(16.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AircraftLocationDetails(
                    aircraft = it,
                    userLocation = userLocation,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AircraftTab(
    aircraft: Aircraft?,
    flight: Flight,
    onSquawkClick: ((String) -> Unit)? = null,
) {
    val emergencyColor = AppTheme.colors.aircraftEmergency
    val cautionColor = AppTheme.colors.caution
    val defaultSquawkColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        FlightAircraftDetails(flight = flight)
        Spacer(modifier = Modifier.height(8.dp))
        aircraft?.let {
            val squawkColor =
                it.squawk?.let { code ->
                    when (SquawkCodes[code]?.severity) {
                        SquawkSeverity.EMERGENCY -> emergencyColor
                        SquawkSeverity.CAUTION -> cautionColor
                        else -> defaultSquawkColor
                    }
                } ?: defaultSquawkColor
            AircraftSecondaryDetails(
                aircraft = it,
                squawkValueColor = squawkColor,
                onSquawkClick = onSquawkClick,
            )
            Spacer(modifier = Modifier.height(8.dp))
            AircraftSignalDetails(aircraft = it)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun RouteTab(flight: Flight) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        flight.origin?.let {
            AirportInfo(
                title = stringResource(Res.string.flight_details_departure),
                airport = it,
                scheduledTime = flight.scheduledOut,
                actualTime = flight.actualOut,
                estimatedTime = flight.estimatedOut,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        FlightProgress(flight = flight)

        flight.destination?.let {
            Spacer(modifier = Modifier.height(8.dp))
            AirportInfo(
                title = stringResource(Res.string.flight_details_destination),
                airport = it,
                scheduledTime = flight.scheduledIn,
                actualTime = flight.actualIn,
                estimatedTime = flight.estimatedIn,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
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

    Card(modifier = Modifier.fillMaxWidth()) {
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

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(Res.string.aircraft_list_progress_percent, progress),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
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
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))

            airport.code?.let { code ->
                Text(text = code, style = MaterialTheme.typography.headlineMedium)
            }
            if (!airport.city.isNullOrBlank()) {
                Text(
                    text = airport.city,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            airport.name?.let { name ->
                if (name.isNotBlank()) {
                    Text(
                        text =
                            stringResource(
                                Res.string.flight_details_airport_name_code,
                                name,
                                airport.code.orEmpty(),
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            scheduledTime?.let { scheduled ->
                TimeRow(
                    formattedLabel = stringResource(Res.string.flight_details_scheduled, scheduled.formattedTime),
                    highlight = false,
                )
            }

            actualTime?.let { actual ->
                val diff = calculateTimeDifference(scheduledTime, actual)
                TimeRow(
                    formattedLabel = stringResource(Res.string.flight_details_actual, actual.formattedTime, diff),
                    highlight = scheduledTime != null,
                )
            }

            if (actualTime == null) {
                estimatedTime?.let { estimated ->
                    val diff = calculateTimeDifference(scheduledTime, estimated)
                    TimeRow(
                        formattedLabel =
                            stringResource(
                                Res.string.flight_details_estimated,
                                estimated.formattedTime,
                                diff,
                            ),
                        highlight = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeRow(
    formattedLabel: String,
    highlight: Boolean,
) {
    Text(
        text = formattedLabel,
        style = MaterialTheme.typography.bodyMedium,
        color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 2.dp),
    )
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

@Composable
private fun PillTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            tabs.forEachIndexed { index, title ->
                val selected = index == selectedIndex
                val tabColor =
                    if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
                val textColor =
                    if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                Surface(
                    shape = RoundedCornerShape(50),
                    color = tabColor,
                    modifier = Modifier.clickable { onTabSelected(index) },
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
