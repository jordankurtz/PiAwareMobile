package com.jordankurtz.piawaremobile.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    fun followLocationFab_hiddenWhenConditionFalse() =
        runComposeUiTest {
            var showFab by mutableStateOf(true)
            setContent {
                if (showFab) {
                    FollowUserLocationFab(isFollowing = false, onClick = {})
                }
            }
            onNodeWithContentDescription("Follow my location").assertIsDisplayed()
            showFab = false
            waitForIdle()
            onNodeWithContentDescription("Follow my location").assertDoesNotExist()
        }
}
