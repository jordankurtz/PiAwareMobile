package com.jordankurtz.piawaremobile.aircraft.cache

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.jordankurtz.piawaremobile.map.cache.TileCacheDatabase
import com.jordankurtz.piawaremobile.map.offline.BoundingBox
import com.jordankurtz.piawaremobile.model.Flight
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FlightCacheRepoImplTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: FlightCacheRepoImpl

    @BeforeTest
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        TileCacheDatabase.Schema.create(driver)
        repo = FlightCacheRepoImpl(TileCacheDatabase(driver).flightCacheQueries, dispatcher)
    }

    @Test
    fun `getCachedFlight returns null when flight not in cache`() =
        runTest(dispatcher) {
            val result = repo.getCachedFlight("UNKNOWN")
            assertNull(result)
        }

    @Test
    fun `cacheFlight then getCachedFlight returns entry`() =
        runTest(dispatcher) {
            val flight = makeFlight("AA100")

            repo.cacheFlight(flight)
            val result = repo.getCachedFlight("AA100")

            assertNotNull(result)
            assertEquals("AA100", result.flight.ident)
        }

    @Test
    fun `cacheFlight preserves all mapped fields`() =
        runTest(dispatcher) {
            val flight = makeFlight("UA200", status = "En Route", type = "airline")

            repo.cacheFlight(flight)
            val result = repo.getCachedFlight("UA200")

            assertNotNull(result)
            assertEquals("En Route", result.flight.status)
            assertEquals("airline", result.flight.type)
            assertEquals("B738", result.flight.aircraftType)
        }

    @Test
    fun `getCachedFlight returns non-null cachedAt`() =
        runTest(dispatcher) {
            repo.cacheFlight(makeFlight("DL300"))
            val result = repo.getCachedFlight("DL300")
            assertNotNull(result)
            assertTrue(result.cachedAt.epochSeconds > 0)
        }

    @Test
    fun `getAllRegions is empty initially`() =
        runTest(dispatcher) {
            assertTrue(repo.getAllRegions().isEmpty())
        }

    @Test
    fun `insertRegion then getAllRegions returns the region`() =
        runTest(dispatcher) {
            val box = BoundingBox(minLat = 30.0, maxLat = 35.0, minLon = -100.0, maxLon = -95.0)

            repo.insertRegion(name = "Texas", box = box, daysAhead = 3)
            val regions = repo.getAllRegions()

            assertEquals(1, regions.size)
            assertEquals("Texas", regions[0].name)
            assertEquals(3, regions[0].daysAhead)
            assertEquals(30.0, regions[0].box.minLat)
        }

    @Test
    fun `deleteRegion removes region and orphaned prefetch flights`() =
        runTest(dispatcher) {
            val box = BoundingBox(minLat = 30.0, maxLat = 35.0, minLon = -100.0, maxLon = -95.0)
            val regionId = repo.insertRegion(name = "Texas", box = box, daysAhead = 3)
            repo.bulkCacheFlights(listOf(makeFlight("WN500")), regionId)

            repo.deleteRegion(regionId)

            assertTrue(repo.getAllRegions().isEmpty())
            assertNull(repo.getCachedFlight("WN500"), "Orphaned prefetch flight should be deleted with region")
        }

    @Test
    fun `deleteRegion does not remove individually cached flights`() =
        runTest(dispatcher) {
            val box = BoundingBox(minLat = 30.0, maxLat = 35.0, minLon = -100.0, maxLon = -95.0)
            val regionId = repo.insertRegion(name = "Texas", box = box, daysAhead = 3)

            repo.cacheFlight(makeFlight("WN600"))
            repo.bulkCacheFlights(listOf(makeFlight("WN600")), regionId)

            repo.deleteRegion(regionId)

            assertNotNull(repo.getCachedFlight("WN600"), "Individually cached flight should survive region deletion")
        }

    @Test
    fun `updateRegionFlightCount updates count in database`() =
        runTest(dispatcher) {
            val box = BoundingBox(minLat = 30.0, maxLat = 35.0, minLon = -100.0, maxLon = -95.0)
            val regionId = repo.insertRegion(name = "Texas", box = box, daysAhead = 3)

            repo.updateRegionFlightCount(regionId, 42)
            val regions = repo.getAllRegions()

            assertEquals(42, regions[0].flightCount)
        }
}

private fun makeFlight(
    ident: String,
    status: String = "Scheduled",
    type: String = "airline",
): Flight =
    Flight(
        ident = ident,
        identIcao = null,
        identIata = null,
        faFlightId = "$ident-FA",
        operator = null,
        operatorIcao = null,
        operatorIata = null,
        flightNumber = null,
        registration = null,
        atcIdent = null,
        inboundFaFlightId = null,
        codeshares = null,
        codesharesIata = null,
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
        status = status,
        aircraftType = "B738",
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
        type = type,
        scheduledOut = null,
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
        actualRunwayOn = null,
    )
