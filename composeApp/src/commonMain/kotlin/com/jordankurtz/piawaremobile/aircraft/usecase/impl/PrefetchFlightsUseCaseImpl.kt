package com.jordankurtz.piawaremobile.aircraft.usecase.impl

import com.jordankurtz.piawaremobile.aircraft.api.AeroApi
import com.jordankurtz.piawaremobile.aircraft.cache.FlightCacheRepo
import com.jordankurtz.piawaremobile.aircraft.usecase.PrefetchFlightsUseCase
import com.jordankurtz.piawaremobile.map.offline.BoundingBox
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.Flight
import kotlinx.coroutines.CancellationException
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

@Single(binds = [PrefetchFlightsUseCase::class])
class PrefetchFlightsUseCaseImpl(
    private val aeroApi: AeroApi,
    private val flightCacheRepo: FlightCacheRepo,
) : PrefetchFlightsUseCase {
    override suspend fun invoke(
        name: String,
        box: BoundingBox,
        daysAhead: Int,
    ): Async<Int> =
        try {
            val regionId = flightCacheRepo.insertRegion(name, box, daysAhead)
            val now = Clock.System.now()
            val end = now.plus(daysAhead.days)
            val query = "-latlong \"${box.minLat} ${box.minLon} ${box.maxLat} ${box.maxLon}\""

            val flights = mutableListOf<Flight>()
            var cursor: String? = null
            do {
                val response =
                    aeroApi.searchFlights(
                        query = query,
                        start = now.toString(),
                        end = end.toString(),
                        maxPages = 1,
                        cursor = cursor,
                    )
                flights.addAll(response.flights)
                cursor = response.links?.next
            } while (cursor != null)

            flightCacheRepo.bulkCacheFlights(flights, regionId)
            flightCacheRepo.updateRegionFlightCount(regionId, flights.size)
            Async.Success(flights.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Async.Error("Failed to prefetch flights: ${e.message}", e)
        }
}
