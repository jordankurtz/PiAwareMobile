package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.jordankurtz.piawaremobile.map.TileProviders
import com.jordankurtz.piawaremobile.settings.CustomProviderConfig
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MapProvidersScreenTest {
    @Test
    fun builtInProviderRowDisplaysName() =
        runComposeUiTest {
            setContent {
                BuiltInProviderRow(
                    config = TileProviders.OPENFREEMAP_BRIGHT,
                    isSelected = true,
                    onClick = {},
                )
            }
            onNodeWithText("OpenFreeMap Bright").assertIsDisplayed()
        }

    @Test
    fun apiKeyProviderRowShowsKeyRequiredBadgeWhenNotConfigured() =
        runComposeUiTest {
            setContent {
                ApiKeyProviderRow(
                    config = TileProviders.STADIA_ALIDADE_SMOOTH,
                    isSelected = false,
                    hasKey = false,
                    onClick = {},
                )
            }
            onNodeWithText("Stadia Alidade Smooth").assertIsDisplayed()
            onNodeWithText("API key required").assertIsDisplayed()
        }

    @Test
    fun apiKeyProviderRowShowsConfiguredBadgeWhenKeyPresent() =
        runComposeUiTest {
            setContent {
                ApiKeyProviderRow(
                    config = TileProviders.STADIA_ALIDADE_SMOOTH,
                    isSelected = false,
                    hasKey = true,
                    onClick = {},
                )
            }
            onNodeWithText("Key configured").assertIsDisplayed()
        }

    @Test
    fun apiKeyBottomSheetSaveDisabledWhenEmpty() =
        runComposeUiTest {
            setContent {
                ApiKeyBottomSheet(
                    providerName = "Stadia Toner",
                    keyInfo = "Get a free key at stadiamaps.com",
                    onSave = {},
                    onDismiss = {},
                )
            }
            onNodeWithText("Save").assertIsNotEnabled()
        }

    @Test
    fun apiKeyBottomSheetSaveEnabledWhenKeyEntered() =
        runComposeUiTest {
            setContent {
                ApiKeyBottomSheet(
                    providerName = "Stadia Toner",
                    keyInfo = "Get a free key at stadiamaps.com",
                    onSave = {},
                    onDismiss = {},
                )
            }
            onNodeWithText("API key").performTextInput("my-secret-key")
            onNodeWithText("Save").assertIsEnabled()
        }

    @Test
    fun apiKeyBottomSheetCallsOnSaveWithEnteredKey() =
        runComposeUiTest {
            var savedKey = ""
            setContent {
                ApiKeyBottomSheet(
                    providerName = "Stadia Toner",
                    keyInfo = "Get a free key at stadiamaps.com",
                    onSave = { savedKey = it },
                    onDismiss = {},
                )
            }
            onNodeWithText("API key").performTextInput("my-key")
            onNodeWithText("Save").performClick()
            assertEquals("my-key", savedKey)
        }

    @Test
    fun customProviderBottomSheetSaveDisabledWhenEmpty() =
        runComposeUiTest {
            setContent {
                AddCustomProviderBottomSheet(
                    onSave = { _, _ -> },
                    onDismiss = {},
                )
            }
            onNodeWithText("Save").assertIsNotEnabled()
        }

    @Test
    fun customProviderBottomSheetSaveEnabledWithValidUrl() =
        runComposeUiTest {
            setContent {
                AddCustomProviderBottomSheet(
                    onSave = { _, _ -> },
                    onDismiss = {},
                )
            }
            onNodeWithText("Name").performTextInput("My Tiles")
            onNodeWithText("Style JSON URL (MapLibre format)")
                .performTextInput("https://tiles.example.com/style.json")
            onNodeWithText("Save").assertIsEnabled()
        }

    @Test
    fun customProviderBottomSheetSaveDisabledWithEmptyName() =
        runComposeUiTest {
            setContent {
                AddCustomProviderBottomSheet(
                    onSave = { _, _ -> },
                    onDismiss = {},
                )
            }
            onNodeWithText("Style JSON URL (MapLibre format)")
                .performTextInput("https://tiles.example.com/style.json")
            onNodeWithText("Save").assertIsNotEnabled()
        }

    @Test
    fun customProviderRowDisplaysName() =
        runComposeUiTest {
            val config =
                CustomProviderConfig(
                    id = "c1",
                    displayName = "My Tiles",
                    styleUrl = "https://tiles.example.com/style.json",
                )
            setContent {
                CustomProviderRow(
                    config = config,
                    isSelected = false,
                    onClick = {},
                    onDelete = {},
                )
            }
            onNodeWithText("My Tiles").assertIsDisplayed()
        }
}
