package com.jordankurtz.piawaremobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
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
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AircraftListScreenTest {
    private val testAircraft =
        listOf(
            AircraftWithServers(
                aircraft = mockAircraft(hex = "A1B2C3", flight = "TST101  ", altBaro = "35000", gs = 450f),
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
                aircraft = mockAircraft(hex = "D4E5F6", flight = "TST202  ", altBaro = "28000", gs = 380f),
                info = null,
                servers = setOf(mockServer()),
            ),
            AircraftWithServers(
                aircraft = mockAircraft(hex = "789ABC", flight = null, altBaro = null, gs = null),
                info = null,
                servers = emptySet(),
            ),
        )

    @Test
    fun listHeaderDisplaysTrackedLabel() =
        runComposeUiTest {
            setContent {
                ListHeader(aircraft = testAircraft)
            }
            onNodeWithText("Tracked").assertIsDisplayed()
            onNodeWithText("With Position").assertIsDisplayed()
        }

    @Test
    fun listHeaderDisplaysTitle() =
        runComposeUiTest {
            setContent {
                ListHeader(aircraft = testAircraft)
            }
            onNodeWithText("Aircraft").assertIsDisplayed()
        }

    @Test
    fun tabletListPanelShowsAircraftItems() =
        runComposeUiTest {
            setContent {
                TabletAircraftListPanel(
                    aircraft = testAircraft,
                    selectedHex = null,
                    flightDetails = Async.NotStarted,
                    userLocation = null,
                    onAircraftSelected = {},
                    onOpenFlightPage = {},
                )
            }
            onNodeWithText("TST101").assertIsDisplayed()
            onNodeWithText("TST202").assertIsDisplayed()
            // Third aircraft has no flight, should show hex
            onNodeWithText("789ABC").assertIsDisplayed()
        }

    @Test
    fun tabletListPanelShowsAircraftType() =
        runComposeUiTest {
            setContent {
                TabletAircraftListPanel(
                    aircraft = testAircraft,
                    selectedHex = null,
                    flightDetails = Async.NotStarted,
                    userLocation = null,
                    onAircraftSelected = {},
                    onOpenFlightPage = {},
                )
            }
            onNodeWithText("A320").assertIsDisplayed()
        }

    @Test
    fun tabletListPanelFiresSelectionCallback() =
        runComposeUiTest {
            var selectedHex: String? = null
            setContent {
                TabletAircraftListPanel(
                    aircraft = testAircraft,
                    selectedHex = null,
                    flightDetails = Async.NotStarted,
                    userLocation = null,
                    onAircraftSelected = { selectedHex = it },
                    onOpenFlightPage = {},
                )
            }
            onNodeWithText("TST101").performClick()
            assertTrue(selectedHex == "A1B2C3")
        }

    @Test
    fun tabletListPanelShowsDetailsWhenSelected() =
        runComposeUiTest {
            setContent {
                TabletAircraftListPanel(
                    aircraft = testAircraft,
                    selectedHex = "A1B2C3",
                    flightDetails = Async.NotStarted,
                    userLocation = null,
                    onAircraftSelected = {},
                    onOpenFlightPage = {},
                )
            }
            // Should show detail view with flight name as headline
            onNodeWithText("TST101").assertIsDisplayed()
            // Registration subtitle
            onNodeWithText("N12345 - Airbus A320").assertIsDisplayed()
        }

    @Test
    fun tabletDetailsShowsBackButton() =
        runComposeUiTest {
            var closed = false
            setContent {
                TabletAircraftDetails(
                    aircraftWithServers = testAircraft[0],
                    flightDetails = Async.NotStarted,
                    userLocation = null,
                    onClose = { closed = true },
                    onOpenFlightPage = {},
                )
            }
            onNodeWithContentDescription("Back to list").performClick()
            assertTrue(closed)
        }

    @Test
    fun tabletDetailsShowsServerInfo() =
        runComposeUiTest {
            setContent {
                TabletAircraftDetails(
                    aircraftWithServers = testAircraft[0],
                    flightDetails = Async.NotStarted,
                    userLocation = null,
                    onClose = {},
                    onOpenFlightPage = {},
                )
            }
            onNodeWithText("Detected by: Test PiAware").assertIsDisplayed()
        }

    @Test
    fun flightInfoDisplaysRouteInfo() =
        runComposeUiTest {
            setContent {
                FlightInfo(flight = mockFlight())
            }
            onNodeWithText("KTST").assertIsDisplayed()
            onNodeWithText("Test City").assertIsDisplayed()
            onNodeWithText("KDST").assertIsDisplayed()
            onNodeWithText("Destination City").assertIsDisplayed()
        }

    @Test
    fun flightInfoDisplaysAircraftType() =
        runComposeUiTest {
            setContent {
                FlightInfo(flight = mockFlight())
            }
            onNodeWithText("A320").assertIsDisplayed()
            onNodeWithText("N12345").assertIsDisplayed()
            onNodeWithText("Test Airlines").assertIsDisplayed()
        }

    @Test
    fun flightInfoDisplaysProgress() =
        runComposeUiTest {
            setContent {
                FlightInfo(flight = mockFlight())
            }
            // String resource "%1$d%% complete" renders as "50% complete"
            onNodeWithText("50%", substring = true).assertIsDisplayed()
        }

    @Test
    fun flightDetailsSectionShowsLoadingState() =
        runComposeUiTest {
            setContent {
                FlightDetailsSection(
                    aircraft = mockAircraft(flight = "TST101"),
                    flightDetails = Async.Loading,
                    onLoadFlightDetails = {},
                    onOpenFlightPage = {},
                )
            }
            onNodeWithText("Loading flight details...").assertIsDisplayed()
        }

    @Test
    fun flightDetailsSectionShowsErrorWithRetry() =
        runComposeUiTest {
            var retried = false
            setContent {
                FlightDetailsSection(
                    aircraft = mockAircraft(flight = "TST101"),
                    flightDetails = Async.Error("Network error"),
                    onLoadFlightDetails = { retried = true },
                    onOpenFlightPage = {},
                )
            }
            onNodeWithText("Network error").assertIsDisplayed()
            onNodeWithText("Retry").performClick()
            assertTrue(retried)
        }

    @Test
    fun flightDetailsSectionShowsOpenInFlightAware() =
        runComposeUiTest {
            var opened = false
            setContent {
                FlightDetailsSection(
                    aircraft = mockAircraft(flight = "TST101"),
                    flightDetails = Async.NotStarted,
                    onLoadFlightDetails = {},
                    onOpenFlightPage = { opened = true },
                )
            }
            onNodeWithText("Open in FlightAware").performClick()
            assertTrue(opened)
        }

    @Test
    fun flightDetailsSectionHidesFlightAwareWhenNoFlight() =
        runComposeUiTest {
            setContent {
                FlightDetailsSection(
                    aircraft = mockAircraft(flight = null),
                    flightDetails = Async.NotStarted,
                    onLoadFlightDetails = {},
                    onOpenFlightPage = {},
                )
            }
            // onNodeWithText would throw if there are multiple, assertDoesNotExist checks absence
            onNodeWithText("Open in FlightAware").assertDoesNotExist()
        }
}
