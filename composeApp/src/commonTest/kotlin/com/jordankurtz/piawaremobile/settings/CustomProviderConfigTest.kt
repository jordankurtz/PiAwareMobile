package com.jordankurtz.piawaremobile.settings

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomProviderConfigTest {
    @Test
    fun serializationRoundTrip() {
        val config =
            CustomProviderConfig(
                id = "abc-123",
                displayName = "My Style",
                styleUrl = "https://example.com/style.json",
            )
        val json = Json.encodeToString(config)
        val decoded = Json.decodeFromString<CustomProviderConfig>(json)
        assertEquals(config, decoded)
    }

    @Test
    fun malformedApiKeysJsonFallsBackToEmpty() {
        val result: Map<String, String> =
            try {
                Json.decodeFromString("not valid json")
            } catch (_: Exception) {
                emptyMap()
            }
        assertEquals(emptyMap(), result)
    }

    @Test
    fun malformedCustomProvidersJsonFallsBackToEmpty() {
        val result: List<CustomProviderConfig> =
            try {
                Json.decodeFromString("not valid json")
            } catch (_: Exception) {
                emptyList()
            }
        assertEquals(emptyList(), result)
    }
}
