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
    override suspend fun invoke(ident: String): Async<Flight> {
        val result = aircraftRepo.lookupFlight(ident)
        if (result is Async.Success) {
            val filteredFlight = filterFlights(result.data.flights)
            if (filteredFlight != null) {
                return Async.Success(filteredFlight)
            }
        }
        if (result is Async.Error) {
            return result
        }
        return Async.Error("Could not find flight for $ident")
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
