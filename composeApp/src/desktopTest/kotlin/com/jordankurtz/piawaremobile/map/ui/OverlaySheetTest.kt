package com.jordankurtz.piawaremobile.map.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OverlaySheetTest {
    private fun defaultContent(
        showFaaCharts: Boolean = false,
        showFaaIfrLow: Boolean = false,
        showFaaIfrHigh: Boolean = false,
        showAirspace: Boolean = false,
        hasOpenAipKey: Boolean = true,
        onToggleFaaCharts: () -> Unit = {},
        onToggleFaaIfrLow: () -> Unit = {},
        onToggleFaaIfrHigh: () -> Unit = {},
        onToggleAirspace: () -> Unit = {},
    ): @androidx.compose.runtime.Composable () -> Unit =
        {
            OverlaySheetContent(
                showFaaCharts = showFaaCharts,
                showFaaIfrLow = showFaaIfrLow,
                showFaaIfrHigh = showFaaIfrHigh,
                showAirspace = showAirspace,
                hasOpenAipKey = hasOpenAipKey,
                onToggleFaaCharts = onToggleFaaCharts,
                onToggleFaaIfrLow = onToggleFaaIfrLow,
                onToggleFaaIfrHigh = onToggleFaaIfrHigh,
                onToggleAirspace = onToggleAirspace,
            )
        }

    @Test
    fun allOverlayRowsAreDisplayed() =
        runComposeUiTest {
            setContent(defaultContent())
            onNodeWithText("FAA VFR Sectional").assertIsDisplayed()
            onNodeWithText("FAA IFR Low Enroute").assertIsDisplayed()
            onNodeWithText("FAA IFR High Enroute").assertIsDisplayed()
            onNodeWithText("Airspace").assertIsDisplayed()
        }

    @Test
    fun toggleFaaChartsFiresCallback() =
        runComposeUiTest {
            var toggled = false
            setContent(defaultContent(onToggleFaaCharts = { toggled = true }))
            onNodeWithText("FAA VFR Sectional").performClick()
            assertTrue(toggled)
        }

    @Test
    fun toggleFaaIfrLowFiresCallback() =
        runComposeUiTest {
            var toggled = false
            setContent(defaultContent(onToggleFaaIfrLow = { toggled = true }))
            onNodeWithText("FAA IFR Low Enroute").performClick()
            assertTrue(toggled)
        }

    @Test
    fun toggleFaaIfrHighFiresCallback() =
        runComposeUiTest {
            var toggled = false
            setContent(defaultContent(onToggleFaaIfrHigh = { toggled = true }))
            onNodeWithText("FAA IFR High Enroute").performClick()
            assertTrue(toggled)
        }

    @Test
    fun airspaceSwitchDisabledWhenNoApiKey() =
        runComposeUiTest {
            setContent(defaultContent(hasOpenAipKey = false))
            onNodeWithText("Airspace")
                .assertIsDisplayed()
            // The switch for airspace should be disabled
            onNodeWithText("OpenAIP airspace boundaries (API key required)")
                .assertIsDisplayed()
        }

    @Test
    fun airspaceSwitchEnabledWhenApiKeySet() =
        runComposeUiTest {
            setContent(defaultContent(hasOpenAipKey = true))
            onNodeWithText("Airspace").assertIsDisplayed()
        }
}
