package com.jordankurtz.piawaremobile.map

import com.jordankurtz.piawaremobile.settings.CustomProviderConfig
import com.jordankurtz.piawaremobile.settings.Settings
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ProviderResolutionTest {
    @Test
    fun resolvesBuiltInProvider() {
        val settings = Settings(mapProviderId = "openstreetmap")
        val result = resolveActiveProviderConfig(settings)
        assertEquals("openstreetmap", result.id)
    }

    @Test
    fun fallsBackToOpenStreetMapForUnknownId() {
        val settings = Settings(mapProviderId = "does_not_exist")
        val result = resolveActiveProviderConfig(settings)
        assertEquals("openstreetmap", result.id)
    }

    @Test
    fun injectsApiKeyForKeyGatedProvider() {
        val settings =
            Settings(
                mapProviderId = "stadia_toner",
                apiKeys = mapOf("stadia" to "my-key"),
            )
        val result = resolveActiveProviderConfig(settings)
        assertContains(result.urlTemplate, "my-key")
    }

    @Test
    fun groupKeyAppliesToAllProvidersInGroup() {
        val settings =
            Settings(
                apiKeys = mapOf("stadia" to "shared-key"),
            )
        listOf("stadia_toner", "stadia_watercolor", "stadia_alidade_smooth", "stadia_alidade_dark")
            .forEach { id ->
                val result = resolveActiveProviderConfig(settings.copy(mapProviderId = id))
                assertContains(
                    result.urlTemplate,
                    "shared-key",
                    message = "Provider $id should use the shared stadia key",
                )
            }
    }

    @Test
    fun usesBlankKeyWhenNotConfigured() {
        val settings = Settings(mapProviderId = "stadia_toner")
        val result = resolveActiveProviderConfig(settings)
        assertContains(result.urlTemplate, "api_key=")
        assertEquals(false, result.urlTemplate.contains("{api_key}"))
    }

    @Test
    fun resolvesCustomProvider() {
        val custom =
            CustomProviderConfig(
                id = "my-custom",
                displayName = "My Tiles",
                urlTemplate = "https://example.com/{z}/{x}/{y}.png",
            )
        val settings =
            Settings(
                mapProviderId = "my-custom",
                customProviders = listOf(custom),
            )
        val result = resolveActiveProviderConfig(settings)
        assertEquals("my-custom", result.id)
        assertEquals("https://example.com/{z}/{x}/{y}.png", result.urlTemplate)
    }
}
