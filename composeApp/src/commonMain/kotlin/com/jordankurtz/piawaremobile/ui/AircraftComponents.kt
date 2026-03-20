package com.jordankurtz.piawaremobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftInfo
import com.jordankurtz.piawaremobile.model.Flight
import com.jordankurtz.piawaremobile.model.Location
import com.jordankurtz.piawaremobile.model.bearingTo
import com.jordankurtz.piawaremobile.model.distanceTo
import org.jetbrains.compose.resources.stringResource
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.label_aircraft_type
import piawaremobile.composeapp.generated.resources.label_altitude
import piawaremobile.composeapp.generated.resources.label_description
import piawaremobile.composeapp.generated.resources.label_direction
import piawaremobile.composeapp.generated.resources.label_distance
import piawaremobile.composeapp.generated.resources.label_heading
import piawaremobile.composeapp.generated.resources.label_last_seen
import piawaremobile.composeapp.generated.resources.label_location
import piawaremobile.composeapp.generated.resources.label_registration
import piawaremobile.composeapp.generated.resources.label_signal
import piawaremobile.composeapp.generated.resources.label_speed
import piawaremobile.composeapp.generated.resources.label_squawk
import piawaremobile.composeapp.generated.resources.label_type
import piawaremobile.composeapp.generated.resources.label_vertical_speed
import piawaremobile.composeapp.generated.resources.label_wtc
import piawaremobile.composeapp.generated.resources.value_altitude_feet
import piawaremobile.composeapp.generated.resources.value_direction_with_cardinal
import piawaremobile.composeapp.generated.resources.value_distance_km
import piawaremobile.composeapp.generated.resources.value_heading_degrees
import piawaremobile.composeapp.generated.resources.value_last_seen_seconds
import piawaremobile.composeapp.generated.resources.value_location_coords
import piawaremobile.composeapp.generated.resources.value_signal_dbm
import piawaremobile.composeapp.generated.resources.value_speed_knots
import piawaremobile.composeapp.generated.resources.value_vertical_speed_fpm
import kotlin.math.roundToInt

/**
 * A labeled value display with the label on top and value below.
 */
@Composable
fun LabeledValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * A compact labeled value with the value first (larger) and unit/label after (smaller).
 */
@Composable
fun CompactLabeledValue(
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(value, style = MaterialTheme.typography.bodyMedium)
        Text(
            " $unit",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Primary aircraft details: Altitude, Heading, Speed
 */
@Composable
fun AircraftPrimaryDetails(
    aircraft: Aircraft,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        aircraft.altBaro?.let {
            LabeledValue(
                label = stringResource(Res.string.label_altitude),
                value = stringResource(Res.string.value_altitude_feet, it),
            )
        }
        aircraft.track?.let {
            LabeledValue(
                label = stringResource(Res.string.label_heading),
                value = stringResource(Res.string.value_heading_degrees, it.toString()),
            )
        }
        aircraft.gs?.let {
            LabeledValue(
                label = stringResource(Res.string.label_speed),
                value = stringResource(Res.string.value_speed_knots, it.toInt()),
            )
        }
    }
}

/**
 * Location details: Location coordinates, Distance, Direction
 */
@Composable
fun AircraftLocationDetails(
    aircraft: Aircraft,
    userLocation: Location?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (aircraft.lat != 0.0 && aircraft.lon != 0.0) {
            LabeledValue(
                label = stringResource(Res.string.label_location),
                value =
                    stringResource(
                        Res.string.value_location_coords,
                        aircraft.lat.round(4).toString(),
                        aircraft.lon.round(4).toString(),
                    ),
            )
            userLocation?.let { location ->
                val aircraftLocation = Location(aircraft.lat, aircraft.lon)
                val distance = location.distanceTo(aircraftLocation)
                val bearing = location.bearingTo(aircraftLocation)

                LabeledValue(
                    label = stringResource(Res.string.label_distance),
                    value = stringResource(Res.string.value_distance_km, distance.roundToInt()),
                )
                LabeledValue(
                    label = stringResource(Res.string.label_direction),
                    value =
                        stringResource(
                            Res.string.value_direction_with_cardinal,
                            bearing.roundToInt(),
                            bearing.toCardinalDirection(),
                        ),
                )
            }
        }
    }
}

/**
 * Secondary aircraft details: Vertical Speed, Squawk
 */
@Composable
fun AircraftSecondaryDetails(
    aircraft: Aircraft,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        aircraft.baroRate?.let {
            LabeledValue(
                label = stringResource(Res.string.label_vertical_speed),
                value = stringResource(Res.string.value_vertical_speed_fpm, it),
            )
        }
        aircraft.squawk?.let {
            LabeledValue(
                label = stringResource(Res.string.label_squawk),
                value = it,
            )
        }
    }
}

/**
 * Signal details: RSSI, Last Seen
 */
@Composable
fun AircraftSignalDetails(
    aircraft: Aircraft,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        aircraft.rssi?.let {
            LabeledValue(
                label = stringResource(Res.string.label_signal),
                value = stringResource(Res.string.value_signal_dbm, it.toString()),
            )
        }
        aircraft.seen?.let {
            LabeledValue(
                label = stringResource(Res.string.label_last_seen),
                value = stringResource(Res.string.value_last_seen_seconds, it.toString()),
            )
        }
    }
}

/**
 * Flight-specific details from FlightAware: Aircraft Type, Registration
 */
@Composable
fun FlightAircraftDetails(
    aircraft: Aircraft?,
    flight: Flight,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        aircraft?.baroRate?.let {
            LabeledValue(
                label = stringResource(Res.string.label_vertical_speed),
                value = stringResource(Res.string.value_vertical_speed_fpm, it),
            )
        }
        aircraft?.squawk?.let {
            LabeledValue(
                label = stringResource(Res.string.label_squawk),
                value = it,
            )
        }
        flight.aircraftType?.let {
            LabeledValue(
                label = stringResource(Res.string.label_aircraft_type),
                value = it,
            )
        }
        flight.registration?.let {
            LabeledValue(
                label = stringResource(Res.string.label_registration),
                value = it,
            )
        }
    }
}

/**
 * Complete aircraft details grid with all information.
 */
@Composable
fun AircraftDetailsGrid(
    aircraft: Aircraft,
    userLocation: Location?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        AircraftPrimaryDetails(aircraft = aircraft)

        Spacer(modifier = Modifier.height(8.dp))

        AircraftSecondaryDetails(aircraft = aircraft)

        Spacer(modifier = Modifier.height(8.dp))

        AircraftLocationDetails(aircraft = aircraft, userLocation = userLocation)

        Spacer(modifier = Modifier.height(8.dp))

        AircraftSignalDetails(aircraft = aircraft)
    }
}

/**
 * Aircraft info row showing ICAO type, description, and WTC.
 */
@Composable
fun AircraftInfoRow(
    info: AircraftInfo,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        info.icaoType?.let {
            LabeledValue(label = stringResource(Res.string.label_type), value = it)
        }
        info.typeDescription?.let {
            LabeledValue(label = stringResource(Res.string.label_description), value = it)
        }
        info.wtc?.let {
            LabeledValue(label = stringResource(Res.string.label_wtc), value = it)
        }
    }
}

// Extension functions

fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}

fun Double.toCardinalDirection(): String {
    val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val index = ((this / 45) + 0.5).toInt() % 8
    return directions[index]
}
