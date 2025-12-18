package com.jordankurtz.piawaremobile.aircraft.usecase.impl

import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepo
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.Flight
import com.jordankurtz.piawaremobile.model.FlightResponse
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class LookupFlightUseCaseImplTest {

    private lateinit var aircraftRepo: AircraftRepo
    private lateinit var useCase: LookupFlightUseCaseImpl

    @BeforeTest
    fun setup() {
        aircraftRepo = mock()
        useCase = LookupFlightUseCaseImpl(aircraftRepo)
    }

    private fun createMockFlight(ident: String, scheduledOut: Instant?): Flight {
        return Flight(
            ident = ident,
            identIcao = null,
            identIata = null,
            faFlightId = "FA123",
            operator = null,
            operatorIcao = null,
            operatorIata = null,
            flightNumber = null,
            registration = null,
            atcIdent = null,
            inboundFaFlightId = null,
            codeshares = emptyList(),
            codesharesIata = emptyList(),
            blocked = false,
            diverted = false,
            cancelled = false,
            positionOnly = false,
            origin = null,
            destination = null,
            departureDelay = null,
            arrivalDelay = null,
            filedEte = null,
            progressPercent = null,
            status = "On Time",
            aircraftType = null,
            routeDistance = null,
            filedAirspeed = null,
            filedAltitude = null,
            route = null,
            baggageClaim = null,
            seatsCabinBusiness = null,
            seatsCabinCoach = null,
            seatsCabinFirst = null,
            gateOrigin = null,
            gateDestination = null,
            terminalOrigin = null,
            terminalDestination = null,
            type = "type",
            scheduledOut = scheduledOut,
            estimatedOut = null,
            actualOut = null,
            scheduledOff = null,
            estimatedOff = null,
            actualOff = null,
            scheduledOn = null,
            estimatedOn = null,
            actualOn = null,
            scheduledIn = null,
            estimatedIn = null,
            actualIn = null,
            foresightPredictionsAvailable = false,
            actualRunwayOff = null,
            actualRunwayOn = null
        )
    }

    @Test
    fun `invoke returns most recent past flight when successful`() = runTest {
        val now = Clock.System.now()
        val futureFlight = createMockFlight(ident = "SWA123", scheduledOut = now.plus(1.hours))
        val mostRecentPastFlight = createMockFlight(ident = "SWA123", scheduledOut = now.minus(1.hours))
        val olderPastFlight = createMockFlight(ident = "SWA123", scheduledOut = now.minus(30.hours))
        val flights = listOf(futureFlight, mostRecentPastFlight, olderPastFlight)
        val response = FlightResponse(flights = flights, links = null, numPages = 1)

        everySuspend { aircraftRepo.lookupFlight("SWA123") } returns Async.Success(response)

        val result = useCase.invoke("SWA123")

        assertTrue(result is Async.Success)
        assertEquals(mostRecentPastFlight, (result as Async.Success).data)
    }

    @Test
    fun `invoke returns error when no past flights are found`() = runTest {
        val now = Clock.System.now()
        val futureFlight = createMockFlight(ident = "SWA123", scheduledOut = now.plus(1.hours))
        val flights = listOf(futureFlight)
        val response = FlightResponse(flights = flights, links = null, numPages = 1)

        everySuspend { aircraftRepo.lookupFlight("SWA123") } returns Async.Success(response)

        val result = useCase.invoke("SWA123")

        assertTrue(result is Async.Error)
    }

    @Test
    fun `invoke returns error when lookup fails`() = runTest {
        val error = Async.Error("Network error")
        everySuspend { aircraftRepo.lookupFlight("SWA123") } returns error

        val result = useCase.invoke("SWA123")

        assertEquals(error, result)
    }
}
