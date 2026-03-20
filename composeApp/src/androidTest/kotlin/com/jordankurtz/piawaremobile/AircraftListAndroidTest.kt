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
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftInfo
import com.jordankurtz.piawaremobile.model.AircraftWithServers
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.Flight
import com.jordankurtz.piawaremobile.model.FlightAirportRef
import com.jordankurtz.piawaremobile.settings.Server
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AircraftListAndroidTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testServer = Server(name = "Test PiAware", address = "piaware.local")

    private val testAircraft =
        listOf(
            AircraftWithServers(
                aircraft = Aircraft(hex = "A1B2C3", flight = "TST101  ", altBaro = "35000"),
                info =
                    AircraftInfo(
                        registration = "N12345",
                        icaoType = "A320",
                        typeDescription = "Airbus A320",
                        wtc = "M",
                    ),
                servers = setOf(testServer),
            ),
            AircraftWithServers(
                aircraft = Aircraft(hex = "D4E5F6", flight = "TST202  "),
                info = null,
                servers = setOf(testServer),
            ),
        )

    private val testFlight =
        Flight(
            ident = "TST101",
            identIcao = "TST101",
            identIata = "TT101",
            actualRunwayOff = null,
            actualRunwayOn = null,
            faFlightId = "TST101-0001",
            operator = "Test Airlines",
            operatorIcao = "TST",
            operatorIata = "TT",
            flightNumber = "101",
            registration = "N12345",
            atcIdent = "TST101",
            inboundFaFlightId = null,
            codeshares = null,
            codesharesIata = null,
            blocked = false,
            diverted = false,
            cancelled = false,
            positionOnly = false,
            origin =
                FlightAirportRef(
                    code = "KTST",
                    codeIcao = "KTST",
                    codeIata = "TST",
                    codeLid = "KTST",
                    timezone = "America/Chicago",
                    name = "Test Origin Airport",
                    city = "Test City",
                    airportInfoUrl = "/airports/KTST",
                ),
            destination =
                FlightAirportRef(
                    code = "KDST",
                    codeIcao = "KDST",
                    codeIata = "DST",
                    codeLid = "KDST",
                    timezone = "America/Chicago",
                    name = "Test Destination Airport",
                    city = "Destination City",
                    airportInfoUrl = "/airports/KDST",
                ),
            departureDelay = null,
            arrivalDelay = null,
            filedEte = null,
            progressPercent = 50,
            status = "En Route",
            aircraftType = "A320",
            routeDistance = null,
            filedAirspeed = null,
            filedAltitude = null,
            route = null,
            baggageClaim = null,
            seatsCabinBusiness = null,
            seatsCabinCoach = null,
            seatsCabinFirst = null,
            gateOrigin = null,
            gateDestination = null,
            terminalOrigin = null,
            terminalDestination = null,
            type = "Airline",
            scheduledOut = null,
            estimatedOut = null,
            actualOut = null,
            scheduledOff = null,
            estimatedOff = null,
            actualOff = null,
            scheduledOn = null,
            estimatedOn = null,
            actualOn = null,
            scheduledIn = null,
            estimatedIn = null,
            actualIn = null,
            foresightPredictionsAvailable = false,
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
        assertTrue(closed)
    }

    @Test
    fun flightInfoRendersOnAndroid() {
        composeTestRule.setContent {
            FlightInfo(flight = testFlight)
        }
        composeTestRule.onNodeWithText("KTST").assertIsDisplayed()
        composeTestRule.onNodeWithText("KDST").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Airlines").assertIsDisplayed()
    }

    @Test
    fun flightDetailsSectionLoadingState() {
        composeTestRule.setContent {
            FlightDetailsSection(
                aircraft = Aircraft(hex = "A1B2C3", flight = "TST101"),
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
                aircraft = Aircraft(hex = "A1B2C3", flight = "TST101"),
                flightDetails = Async.Error("Network error"),
                onLoadFlightDetails = { retried = true },
                onOpenFlightPage = {},
            )
        }
        composeTestRule.onNodeWithText("Network error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").performClick()
        assertTrue(retried)
    }
}
