package com.jordankurtz.piawaremobile.extensions

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Instant

class TimeExtensionsTest {
    @Test
    fun formattedTimeContainsDigits() {
        val instant = Instant.parse("2024-01-15T14:30:00Z")
        val formatted = instant.formattedTime
        assertTrue(
            formatted.any { it.isDigit() },
            "Formatted time should contain digits: $formatted",
        )
    }

    @Test
    fun formattedTimeContainsColon() {
        val instant = Instant.parse("2024-01-15T14:30:00Z")
        val formatted = instant.formattedTime
        assertTrue(formatted.contains(":"), "Formatted time should contain colon: $formatted")
    }

    @Test
    fun formattedTimeIsNotEmpty() {
        val instant = Instant.parse("2024-06-01T00:00:00Z")
        assertTrue(instant.formattedTime.isNotEmpty())
    }
}
