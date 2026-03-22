package com.jordankurtz.piawaremobile.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.jordankurtz.piawaremobile.map.FollowUserLocationFab
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class FollowUserLocationFabTest {
    @Test
    fun fabRendersWithContentDescription() =
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
    fun fabFiresOnClickCallback() =
        runComposeUiTest {
            var clicked = false
            setContent {
                FollowUserLocationFab(
                    isFollowing = false,
                    onClick = { clicked = true },
                )
            }
            onNodeWithContentDescription("Follow my location").performClick()
            assertTrue(clicked)
        }

    @Test
    fun fabRendersWhenFollowingIsTrue() =
        runComposeUiTest {
            setContent {
                FollowUserLocationFab(
                    isFollowing = true,
                    onClick = {},
                )
            }
            onNodeWithContentDescription("Follow my location").assertIsDisplayed()
        }

    @Test
    fun fabRendersWhenFollowingIsFalse() =
        runComposeUiTest {
            setContent {
                FollowUserLocationFab(
                    isFollowing = false,
                    onClick = {},
                )
            }
            onNodeWithContentDescription("Follow my location").assertIsDisplayed()
        }
}
