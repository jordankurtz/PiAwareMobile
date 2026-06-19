package com.jordankurtz.piawaremobile.model

import kotlin.time.Instant

data class FlightResult(
    val flight: Flight,
    val source: Source,
    val cachedAt: Instant? = null,
) {
    enum class Source { LIVE, CACHED }
}
