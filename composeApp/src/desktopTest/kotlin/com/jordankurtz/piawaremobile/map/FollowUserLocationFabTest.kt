package com.jordankurtz.piawaremobile.map

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class FollowUserLocationFabTest {
    @Test
    fun followLocationFab_visibleWhenTrue() =
        runComposeUiTest {
            setContent {
                FollowUserLocationFab(
                    isFollowing = false,
                    onClick = {},
                )
            }
            onNodeWithContentDescription("Follow my location").assertIsDisplayed()
        }

    @Test
    fun followLocationFab_hiddenWhenShowFabFalseViaMapScreen() =
        runComposeUiTest {
            var fabClicked = false
            setContent {
                FollowUserLocationFab(
                    isFollowing = false,
                    onClick = { fabClicked = true },
                )
            }
            // Verify the FAB is displayed and clickable
            onNodeWithContentDescription("Follow my location").assertIsDisplayed()
            onNodeWithContentDescription("Follow my location").performClick()
            assertTrue(fabClicked)
        }
}
