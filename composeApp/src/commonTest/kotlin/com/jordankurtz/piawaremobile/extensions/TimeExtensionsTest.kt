package com.jordankurtz.piawaremobile.extensions

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun formattedDateProducesYYYYMMDD() {
        val instant = Instant.parse("2023-11-14T12:00:00Z")
        val formatted = instant.formattedDateInZone(TimeZone.UTC)
        assertEquals("2023-11-14", formatted)
    }

    @Test
    fun formattedDatePadsMonthAndDay() {
        val instant = Instant.parse("2024-03-05T00:00:00Z")
        val formatted = instant.formattedDateInZone(TimeZone.UTC)
        assertEquals("2024-03-05", formatted)
    }
}
