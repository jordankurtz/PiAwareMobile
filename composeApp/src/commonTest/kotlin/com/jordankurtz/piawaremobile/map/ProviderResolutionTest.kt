package com.jordankurtz.piawaremobile.map

import com.jordankurtz.piawaremobile.settings.CustomProviderConfig
import com.jordankurtz.piawaremobile.settings.Settings
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ProviderResolutionTest {
    @Test
    fun resolvesBuiltInProvider() {
        val settings = Settings(mapProviderId = "openfreemap-bright")
        val result = resolveActiveProviderConfig(settings)
        assertEquals("openfreemap-bright", result.id)
    }

    @Test
    fun fallsBackToDefaultForUnknownId() {
        val settings = Settings(mapProviderId = "does_not_exist")
        val result = resolveActiveProviderConfig(settings)
        assertEquals(TileProviders.DEFAULT.id, result.id)
    }

    @Test
    fun injectsApiKeyForStadiaProvider() {
        val settings =
            Settings(
                mapProviderId = "stadia-alidade-smooth",
                apiKeys = mapOf("stadia" to "my-key"),
            )
        val result = resolveActiveProviderConfig(settings)
        assertContains(result.styleUrl, "my-key")
        assertFalse(result.styleUrl.contains("{api_key}"))
    }

    @Test
    fun stadiaGroupKeyAppliesToAllStadiaProviders() {
        val settings =
            Settings(
                apiKeys = mapOf("stadia" to "shared-key"),
            )
        listOf("stadia-alidade-smooth", "stadia-outdoors").forEach { id ->
            val result = resolveActiveProviderConfig(settings.copy(mapProviderId = id))
            assertContains(
                result.styleUrl,
                "shared-key",
                message = "Provider $id should use the shared stadia key",
            )
        }
    }

    @Test
    fun maptilerGroupKeyAppliesToAllMaptilerProviders() {
        val settings =
            Settings(
                apiKeys = mapOf("maptiler" to "mt-key"),
            )
        listOf("maptiler-streets", "maptiler-outdoor").forEach { id ->
            val result = resolveActiveProviderConfig(settings.copy(mapProviderId = id))
            assertContains(
                result.styleUrl,
                "mt-key",
                message = "Provider $id should use the shared maptiler key",
            )
        }
    }

    @Test
    fun fallsBackToDefaultWhenApiKeyNotConfigured() {
        val settings = Settings(mapProviderId = "stadia-alidade-smooth")
        val result = resolveActiveProviderConfig(settings)
        assertEquals(TileProviders.DEFAULT.id, result.id)
    }

    @Test
    fun fallsBackToDefaultWhenApiKeyIsBlank() {
        val settings =
            Settings(
                mapProviderId = "stadia-alidade-smooth",
                apiKeys = mapOf("stadia" to ""),
            )
        val result = resolveActiveProviderConfig(settings)
        assertEquals(TileProviders.DEFAULT.id, result.id)
    }

    @Test
    fun resolvesCustomProvider() {
        val custom =
            CustomProviderConfig(
                id = "my-custom",
                displayName = "My Style",
                styleUrl = "https://example.com/style.json",
            )
        val settings =
            Settings(
                mapProviderId = "my-custom",
                customProviders = listOf(custom),
            )
        val result = resolveActiveProviderConfig(settings)
        assertEquals("my-custom", result.id)
        assertEquals("https://example.com/style.json", result.styleUrl)
    }
}
