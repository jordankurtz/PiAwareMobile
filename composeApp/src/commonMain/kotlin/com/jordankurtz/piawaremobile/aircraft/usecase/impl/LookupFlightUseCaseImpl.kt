package com.jordankurtz.piawaremobile.aircraft.usecase.impl

import com.jordankurtz.piawaremobile.aircraft.cache.FlightCacheRepo
import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepo
import com.jordankurtz.piawaremobile.aircraft.usecase.LookupFlightUseCase
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.Flight
import com.jordankurtz.piawaremobile.model.FlightResult
import org.koin.core.annotation.Single
import kotlin.time.Clock

@Single(binds = [LookupFlightUseCase::class])
class LookupFlightUseCaseImpl(
    private val aircraftRepo: AircraftRepo,
    private val flightCacheRepo: FlightCacheRepo,
) : LookupFlightUseCase {
    override suspend fun invoke(ident: String): Async<FlightResult> {
        val apiResult = aircraftRepo.lookupFlight(ident)
        if (apiResult is Async.Success) {
            val filteredFlight = filterFlights(apiResult.data.flights)
            if (filteredFlight != null) {
                flightCacheRepo.cacheFlight(filteredFlight)
                return Async.Success(
                    FlightResult(
                        flight = filteredFlight,
                        source = FlightResult.Source.LIVE,
                    ),
                )
            }
        }

        val cached = flightCacheRepo.getCachedFlight(ident)
        if (cached != null) {
            return Async.Success(
                FlightResult(
                    flight = cached.flight,
                    source = FlightResult.Source.CACHED,
                    cachedAt = cached.cachedAt,
                ),
            )
        }

        return if (apiResult is Async.Error) apiResult else Async.Error("Could not find flight for $ident")
    }

    private fun filterFlights(flights: List<Flight>): Flight? {
        val now = Clock.System.now()
        return flights
            .mapNotNull { flight ->
                val departureTime =
                    flight.scheduledOut
                        ?: flight.scheduledOff
                departureTime?.let { flight to it }
            }
            .filter { (_, departureTime) -> departureTime < now }
            .maxByOrNull { (_, departureTime) -> departureTime }
            ?.first
    }
}
