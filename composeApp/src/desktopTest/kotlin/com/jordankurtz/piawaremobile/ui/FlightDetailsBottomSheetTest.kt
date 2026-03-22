package com.jordankurtz.piawaremobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.jordankurtz.piawaremobile.map.FlightDetailsActionButtons
import com.jordankurtz.piawaremobile.testutil.mockAircraft
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class FlightDetailsBottomSheetTest {
    @Test
    fun followButtonShownWhenNotFollowing() =
        runComposeUiTest {
            setContent {
                FlightDetailsActionButtons(
                    aircraft = mockAircraft(),
                    isFollowing = false,
                    onFollowToggle = {},
                    onOpenFlightPage = {},
                )
            }
            onNodeWithText("Follow").assertIsDisplayed()
            onNodeWithText("Unfollow").assertDoesNotExist()
        }

    @Test
    fun unfollowButtonShownWhenFollowing() =
        runComposeUiTest {
            setContent {
                FlightDetailsActionButtons(
                    aircraft = mockAircraft(),
                    isFollowing = true,
                    onFollowToggle = {},
                    onOpenFlightPage = {},
                )
            }
            onNodeWithText("Unfollow").assertIsDisplayed()
            onNodeWithText("Follow").assertDoesNotExist()
        }

    @Test
    fun followToggleCallbackFired() =
        runComposeUiTest {
            var toggled = false
            setContent {
                FlightDetailsActionButtons(
                    aircraft = mockAircraft(),
                    isFollowing = false,
                    onFollowToggle = { toggled = true },
                    onOpenFlightPage = {},
                )
            }
            onNodeWithText("Follow").performClick()
            assertTrue(toggled)
        }

    @Test
    fun flightAwareButtonHiddenWhenNoFlightIdent() =
        runComposeUiTest {
            setContent {
                FlightDetailsActionButtons(
                    aircraft = mockAircraft(flight = null),
                    isFollowing = false,
                    onFollowToggle = {},
                    onOpenFlightPage = {},
                )
            }
            onNodeWithText("Open in FlightAware").assertDoesNotExist()
        }

    @Test
    fun flightAwareButtonShownWhenFlightIdentExists() =
        runComposeUiTest {
            setContent {
                FlightDetailsActionButtons(
                    aircraft = mockAircraft(flight = "TST101"),
                    isFollowing = false,
                    onFollowToggle = {},
                    onOpenFlightPage = {},
                )
            }
            onNodeWithText("Open in FlightAware").assertIsDisplayed()
        }
}
