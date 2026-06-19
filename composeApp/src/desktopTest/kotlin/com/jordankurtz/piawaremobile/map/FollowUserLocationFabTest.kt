package com.jordankurtz.piawaremobile.map

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

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
            setContent {
                val show = false
                if (show) {
                    FollowUserLocationFab(isFollowing = false, onClick = {})
                }
            }
            onNodeWithContentDescription("Follow my location").assertDoesNotExist()
        }
}
