package com.jordankurtz.piawaremobile.aircraft.cache

import com.jordankurtz.piawaremobile.map.offline.BoundingBox
import com.jordankurtz.piawaremobile.model.Flight
import kotlin.time.Instant

interface FlightCacheRepo {
    suspend fun getCachedFlight(ident: String): CachedFlightEntry?

    suspend fun cacheFlight(flight: Flight)

    suspend fun bulkCacheFlights(
        flights: List<Flight>,
        regionId: Long,
    )

    suspend fun getAllRegions(): List<CachedRegion>

    suspend fun insertRegion(
        name: String,
        box: BoundingBox,
        daysAhead: Int,
    ): Long

    suspend fun updateRegionFlightCount(
        regionId: Long,
        count: Int,
    )

    suspend fun deleteRegion(regionId: Long)
}

data class CachedFlightEntry(
    val flight: Flight,
    val cachedAt: Instant,
)

data class CachedRegion(
    val id: Long,
    val name: String,
    val box: BoundingBox,
    val daysAhead: Int,
    val flightCount: Int,
    val cachedAt: Instant,
)
