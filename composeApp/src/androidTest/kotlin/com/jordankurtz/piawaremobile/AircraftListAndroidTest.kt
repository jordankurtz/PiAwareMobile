package com.jordankurtz.piawaremobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jordankurtz.piawaremobile.list.FlightDetailsSection
import com.jordankurtz.piawaremobile.list.FlightInfo
import com.jordankurtz.piawaremobile.list.ListHeader
import com.jordankurtz.piawaremobile.list.TabletAircraftDetails
import com.jordankurtz.piawaremobile.list.TabletAircraftListPanel
import com.jordankurtz.piawaremobile.model.AircraftInfo
import com.jordankurtz.piawaremobile.model.AircraftWithServers
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.testutil.mockAircraft
import com.jordankurtz.piawaremobile.testutil.mockFlight
import com.jordankurtz.piawaremobile.testutil.mockServer
import org.junit.Rule
import org.junit.Test

class AircraftListAndroidTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testAircraft =
        listOf(
            AircraftWithServers(
                aircraft = mockAircraft(hex = "A1B2C3", flight = "TST101  ", altBaro = "35000"),
                info =
                    AircraftInfo(
                        registration = "N12345",
                        icaoType = "A320",
                        typeDescription = "Airbus A320",
                        wtc = "M",
                    ),
                servers = setOf(mockServer()),
            ),
            AircraftWithServers(
                aircraft = mockAircraft(hex = "D4E5F6", flight = "TST202  "),
                info = null,
                servers = setOf(mockServer()),
            ),
        )

    @Test
    fun listHeaderRendersOnAndroid() {
        composeTestRule.setContent {
            ListHeader(aircraft = testAircraft)
        }
        composeTestRule.onNodeWithText("Aircraft").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tracked").assertIsDisplayed()
    }

    @Test
    fun tabletListPanelRendersAircraftOnAndroid() {
        composeTestRule.setContent {
            TabletAircraftListPanel(
                aircraft = testAircraft,
                selectedHex = null,
                flightDetails = Async.NotStarted,
                userLocation = null,
                onAircraftSelected = {},
                onOpenFlightPage = {},
            )
        }
        composeTestRule.onNodeWithText("TST101").assertIsDisplayed()
        composeTestRule.onNodeWithText("TST202").assertIsDisplayed()
    }

    @Test
    fun tabletListPanelSelectionShowsDetails() {
        composeTestRule.setContent {
            TabletAircraftListPanel(
                aircraft = testAircraft,
                selectedHex = "A1B2C3",
                flightDetails = Async.NotStarted,
                userLocation = null,
                onAircraftSelected = {},
                onOpenFlightPage = {},
            )
        }
        composeTestRule.onNodeWithText("TST101").assertIsDisplayed()
        composeTestRule.onNodeWithText("N12345 - Airbus A320").assertIsDisplayed()
    }

    @Test
    fun tabletDetailsBackButtonWorks() {
        var closed = false
        composeTestRule.setContent {
            TabletAircraftDetails(
                aircraftWithServers = testAircraft[0],
                flightDetails = Async.NotStarted,
                userLocation = null,
                onClose = { closed = true },
                onOpenFlightPage = {},
            )
        }
        composeTestRule.onNodeWithContentDescription("Back to list").performClick()
        assert(closed)
    }

    @Test
    fun flightInfoRendersOnAndroid() {
        composeTestRule.setContent {
            FlightInfo(flight = mockFlight())
        }
        composeTestRule.onNodeWithText("KTST").assertIsDisplayed()
        composeTestRule.onNodeWithText("KDST").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Airlines").assertIsDisplayed()
    }

    @Test
    fun flightDetailsSectionLoadingState() {
        composeTestRule.setContent {
            FlightDetailsSection(
                aircraft = mockAircraft(flight = "TST101"),
                flightDetails = Async.Loading,
                onLoadFlightDetails = {},
                onOpenFlightPage = {},
            )
        }
        composeTestRule.onNodeWithText("Loading flight details...").assertIsDisplayed()
    }

    @Test
    fun flightDetailsSectionErrorWithRetry() {
        var retried = false
        composeTestRule.setContent {
            FlightDetailsSection(
                aircraft = mockAircraft(flight = "TST101"),
                flightDetails = Async.Error("Network error"),
                onLoadFlightDetails = { retried = true },
                onOpenFlightPage = {},
            )
        }
        composeTestRule.onNodeWithText("Network error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").performClick()
        assert(retried)
    }
}
