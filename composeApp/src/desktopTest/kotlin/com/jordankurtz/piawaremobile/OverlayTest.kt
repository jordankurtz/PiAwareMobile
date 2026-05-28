package com.jordankurtz.piawaremobile

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OverlayTest {
    @Test
    fun showsPlaneCount() =
        runComposeUiTest {
            setContent {
                Overlay(
                    numberOfPlanes = 7,
                    modifier = androidx.compose.ui.Modifier,
                )
            }
            onNodeWithText("7 planes").assertIsDisplayed()
        }
}
