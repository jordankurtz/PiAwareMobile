package com.jordankurtz.piawaremobile.extensions

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Instant

class TimeExtensionsTest {
    @Test
    fun formattedTimeContainsAmOrPm() {
        // 2024-01-15T14:30:00Z = 2:30 PM in UTC
        val instant = Instant.parse("2024-01-15T14:30:00Z")
        val formatted = instant.formattedTime
        assertTrue(
            formatted.contains("am") || formatted.contains("pm"),
            "Formatted time should contain am/pm: $formatted",
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
