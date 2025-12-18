package com.jordankurtz.piawaremobile.extensions

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

val Instant.formattedTime: String
    get() {
        val localDateTime = toLocalDateTime(TimeZone.currentSystemDefault())

        // Define a custom format using the DSL
        val formatter = LocalDateTime.Format {
            amPmHour()
            char(':')
            minute()
            char(' ')
            amPmMarker("am", "pm")
        }
        return formatter.format(localDateTime)
    }
