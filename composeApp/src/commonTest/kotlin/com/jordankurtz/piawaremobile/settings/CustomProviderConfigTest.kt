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
                displayName = "My Tiles",
                urlTemplate = "https://example.com/{z}/{x}/{y}.png",
            )
        val json = Json.encodeToString(config)
        val decoded = Json.decodeFromString<CustomProviderConfig>(json)
        assertEquals(config, decoded)
    }
}
