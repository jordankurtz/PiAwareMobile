package com.jordankurtz.piawaremobile.extensions

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

val Instant.formattedTime: String
    get() {
        val localDateTime = toLocalDateTime(TimeZone.currentSystemDefault())

        // Define a custom format using the DSL
        val formatter =
            LocalDateTime.Format {
                amPmHour()
                char(':')
                minute()
                char(' ')
                amPmMarker("am", "pm")
            }
        return formatter.format(localDateTime)
    }

val Instant.formattedDate: String
    get() = formattedDateInZone(TimeZone.currentSystemDefault())

fun Instant.formattedDateInZone(timeZone: TimeZone): String {
    val localDateTime = toLocalDateTime(timeZone)
    val formatter =
        LocalDateTime.Format {
            year()
            char('-')
            monthNumber(padding = Padding.ZERO)
            char('-')
            day(padding = Padding.ZERO)
        }
    return formatter.format(localDateTime)
}
