@file:Suppress("UnusedPrivateMember")

package com.jordankurtz.piawaremobile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jordankurtz.piawaremobile.Overlay
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftInfo
import com.jordankurtz.piawaremobile.model.Location
import com.jordankurtz.piawaremobile.settings.ui.AddServerDialog
import org.jetbrains.compose.ui.tooling.preview.Preview

private val previewAircraft =
    Aircraft(
        hex = "A1B2C3",
        lat = 40.0,
        lon = -100.0,
        flight = "TST101  ",
        altBaro = "35000",
        gs = 450f,
        track = 180f,
        squawk = "1200",
        rssi = -3.5f,
        seen = 1.2f,
    )

private val previewUserLocation = Location(latitude = 40.1, longitude = -100.1)

private val previewAircraftInfo =
    AircraftInfo(
        registration = "N12345",
        icaoType = "B738",
        typeDescription = "Boeing 737-800",
        wtc = "M",
    )

@Preview
@Composable
private fun OverlayPreview() {
    Theme {
        Overlay(
            numberOfPlanes = 42,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview
@Composable
private fun AircraftPrimaryDetailsPreview() {
    Theme {
        AircraftPrimaryDetails(
            aircraft = previewAircraft,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview
@Composable
private fun AircraftLocationDetailsPreview() {
    Theme {
        AircraftLocationDetails(
            aircraft = previewAircraft,
            userLocation = previewUserLocation,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview
@Composable
private fun AircraftSecondaryDetailsPreview() {
    Theme {
        AircraftSecondaryDetails(
            aircraft = previewAircraft,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview
@Composable
private fun AircraftSignalDetailsPreview() {
    Theme {
        AircraftSignalDetails(
            aircraft = previewAircraft,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview
@Composable
private fun AircraftInfoRowPreview() {
    Theme {
        AircraftInfoRow(
            info = previewAircraftInfo,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview
@Composable
private fun AircraftDetailsGridPreview() {
    Theme {
        AircraftDetailsGrid(
            aircraft = previewAircraft,
            userLocation = previewUserLocation,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview
@Composable
private fun LabeledValuePreview() {
    Theme {
        Column(modifier = Modifier.padding(16.dp)) {
            LabeledValue(label = "Altitude", value = "35,000 ft")
            LabeledValue(label = "Speed", value = "450 kts")
            LabeledValue(label = "Heading", value = "180°")
        }
    }
}

@Preview
@Composable
private fun AddServerDialogPreview() {
    Theme {
        AddServerDialog(
            onDismiss = {},
            onConfirm = { _, _ -> },
        )
    }
}
