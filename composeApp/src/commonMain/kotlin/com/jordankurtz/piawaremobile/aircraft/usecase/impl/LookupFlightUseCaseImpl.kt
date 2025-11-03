package com.jordankurtz.piawaremobile.aircraft.usecase.impl

import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepo
import com.jordankurtz.piawaremobile.aircraft.usecase.LookupFlightUseCase
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.Flight
import org.koin.core.annotation.Single
import kotlin.time.Clock

@Single(binds = [LookupFlightUseCase::class])
class LookupFlightUseCaseImpl(
    private val aircraftRepo: AircraftRepo
) : LookupFlightUseCase {
    override suspend fun invoke(ident: String): Async<Flight?> {
        val result = aircraftRepo.lookupFlight(ident)
        if (result is Async.Success) {
            val filteredFlights = filterFlights(result.data.flights)
            return Async.Success(filteredFlights)
        }
        return Async.Success(null)
    }

    private fun filterFlights(flights: List<Flight>): Flight? {
        val now = Clock.System.now()
        val mostRecentFlight = flights
            .mapNotNull { flight ->
                val departureTime = flight.scheduledOut
                    ?: flight.scheduledOff
                departureTime?.let { flight to it }
            }
            .filter { (_, departureTime) -> departureTime < now }
            .maxByOrNull { (_, departureTime) -> departureTime }
            ?.first

        return mostRecentFlight
    }
}
