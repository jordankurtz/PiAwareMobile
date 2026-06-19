package com.jordankurtz.piawaremobile.aircraft.usecase.impl

import com.jordankurtz.piawaremobile.aircraft.api.AeroApi
import com.jordankurtz.piawaremobile.aircraft.cache.FlightCacheRepo
import com.jordankurtz.piawaremobile.map.offline.BoundingBox
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.FlightResponse
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrefetchFlightsUseCaseImplTest {
    private val aeroApi: AeroApi = mock()
    private val flightCacheRepo: FlightCacheRepo = mock()
    private val useCase = PrefetchFlightsUseCaseImpl(aeroApi, flightCacheRepo)

    private val box = BoundingBox(minLat = 30.0, maxLat = 55.0, minLon = -10.0, maxLon = 30.0)

    @Test
    fun `returns success with flight count on happy path`() =
        runTest {
            val response = FlightResponse(flights = emptyList(), links = null, numPages = 1)
            everySuspend { flightCacheRepo.insertRegion(any(), any(), any()) } returns 1L
            everySuspend { aeroApi.searchFlights(query = any(), start = any(), end = any()) } returns response
            everySuspend { flightCacheRepo.bulkCacheFlights(any(), any()) } returns Unit
            everySuspend { flightCacheRepo.updateRegionFlightCount(any(), any()) } returns Unit

            val result = useCase("Europe", box, daysAhead = 3)

            assertTrue(result is Async.Success)
            assertEquals(0, result.data)
        }

    @Test
    fun `returns error when API throws`() =
        runTest {
            everySuspend { flightCacheRepo.insertRegion(any(), any(), any()) } returns 1L
            everySuspend {
                aeroApi.searchFlights(query = any(), start = any(), end = any())
            } throws RuntimeException("Network error")

            val result = useCase("Europe", box, daysAhead = 3)

            assertTrue(result is Async.Error)
        }

    @Test
    fun `builds correct latlong query from bounding box`() =
        runTest {
            val response = FlightResponse(flights = emptyList(), links = null, numPages = 1)
            everySuspend { flightCacheRepo.insertRegion(any(), any(), any()) } returns 1L
            everySuspend {
                aeroApi.searchFlights(
                    query = "-latlong \"30.0 -10.0 55.0 30.0\"",
                    start = any(),
                    end = any(),
                )
            } returns response
            everySuspend { flightCacheRepo.bulkCacheFlights(any(), any()) } returns Unit
            everySuspend { flightCacheRepo.updateRegionFlightCount(any(), any()) } returns Unit

            useCase("Europe", box, daysAhead = 3)

            verifySuspend(VerifyMode.exactly(1)) {
                aeroApi.searchFlights(
                    query = "-latlong \"30.0 -10.0 55.0 30.0\"",
                    start = any(),
                    end = any(),
                )
            }
        }
}
