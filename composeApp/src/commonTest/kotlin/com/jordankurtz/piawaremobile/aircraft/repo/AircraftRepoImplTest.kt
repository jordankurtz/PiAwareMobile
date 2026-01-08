package com.jordankurtz.piawaremobile.aircraft.repo

import com.jordankurtz.piawaremobile.aircraft.api.AeroApi
import com.jordankurtz.piawaremobile.aircraft.api.PiAwareApi
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.FlightResponse
import com.jordankurtz.piawaremobile.model.ICAOAircraftType
import com.jordankurtz.piawaremobile.model.Links
import com.jordankurtz.piawaremobile.model.Receiver
import com.jordankurtz.piawaremobile.model.ReceiverType
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AircraftRepoImplTest {

    private lateinit var piAwareApi: PiAwareApi
    private lateinit var aeroApi: AeroApi
    private lateinit var repo: AircraftRepoImpl

    private val mockAircraft1 = Aircraft(hex = "a8b2c3", flight = "SWA123", lat = 32.7, lon = -96.8)
    private val mockAircraft2 = Aircraft(hex = "a1b2c3", flight = "DAL456", lat = 32.8, lon = -96.9)
    private val mockAircraftNoLocation = Aircraft(hex = "d4e5f6", flight = "UAL789", lat = 0.0, lon = 0.0)
    private val mockReceiver1090 = Receiver(latitude = 32.7f, longitude = -96.8f)
    private val mockReceiver978 = Receiver(latitude = 32.8f, longitude = -96.9f)
    private val mockAircraftTypes1 = mapOf("A320" to ICAOAircraftType("Airbus A320", "L2J"))
    private val mockAircraftTypes2 = mapOf("B738" to ICAOAircraftType("Boeing 737-800", "L2J"))
    private val mockAircraftInfo = buildJsonObject { put("i", "N12345"); put("t", "A320") }

    @BeforeTest
    fun setup() {
        piAwareApi = mock()
        aeroApi = mock()
        repo = AircraftRepoImpl(piAwareApi, aeroApi)
    }

    @Test
    fun `getAircraft returns merged aircraft list from multiple servers`() = runTest {
        everySuspend { piAwareApi.getAircraft("server1") } returns listOf(mockAircraft1)
        everySuspend { piAwareApi.getAircraft("server2") } returns listOf(mockAircraft2)

        val result = repo.getAircraft(listOf("server1", "server2"))

        assertEquals(listOf(mockAircraft1, mockAircraft2), result)
    }

    @Test
    fun `getAircraft handles one server failing`() = runTest {
        everySuspend { piAwareApi.getAircraft("server1") } returns listOf(mockAircraft1)
        everySuspend { piAwareApi.getAircraft("server2") } throws Exception("Network error")

        val result = repo.getAircraft(listOf("server1", "server2"))

        assertEquals(listOf(mockAircraft1), result)
    }

    @Test
    fun `getAircraft filters out aircraft with no location`() = runTest {
        everySuspend { piAwareApi.getAircraft("server1") } returns listOf(mockAircraft1, mockAircraftNoLocation)

        val result = repo.getAircraft(listOf("server1"))

        assertEquals(listOf(mockAircraft1), result)
    }

    @Test
    fun `getReceiverInfo returns dump1090 receiver info`() = runTest {
        everySuspend { piAwareApi.getDump1090ReceiverInfo("server1") } returns mockReceiver1090

        val result = repo.getReceiverInfo("server1", ReceiverType.DUMP_1090)

        assertEquals(mockReceiver1090, result)
    }

    @Test
    fun `getReceiverInfo returns dump978 receiver info`() = runTest {
        everySuspend { piAwareApi.getDump978ReceiverInfo("server1") } returns mockReceiver978

        val result = repo.getReceiverInfo("server1", ReceiverType.DUMP_978)

        assertEquals(mockReceiver978, result)
    }

    @Test
    fun `getReceiverInfo returns null when receiver info not found`() = runTest {
        everySuspend { piAwareApi.getDump1090ReceiverInfo("server1") } returns null
        everySuspend { piAwareApi.getDump978ReceiverInfo("server1") } returns null

        val result1090 = repo.getReceiverInfo("server1", ReceiverType.DUMP_1090)
        val result978 = repo.getReceiverInfo("server1", ReceiverType.DUMP_978)

        assertNull(result1090)
        assertNull(result978)
    }

    @Test
    fun `loadAircraftTypes merges types from multiple servers`() = runTest {
        everySuspend { piAwareApi.getAircraftTypes("server1") } returns mockAircraftTypes1
        everySuspend { piAwareApi.getAircraftTypes("server2") } returns mockAircraftTypes2
        everySuspend { piAwareApi.getAircraftInfo(any(), any()) } returns null // To simplify the test

        repo.loadAircraftTypes(listOf("server1", "server2"))
    }

    @Test
    fun `findAircraftInfo returns correct info`() = runTest {
        everySuspend { piAwareApi.getAircraftTypes("server1") } returns mockAircraftTypes1
        everySuspend { piAwareApi.getAircraftInfo("server1", "A") } returns buildJsonObject { put("8B2C3", mockAircraftInfo) }

        repo.loadAircraftTypes(listOf("server1"))
        val result = repo.findAircraftInfo("server1", "A8B2C3")

        assertNotNull(result)
        assertEquals("N12345", result.registration)
        assertEquals("A320", result.icaoType)
        assertEquals("Airbus A320", result.typeDescription)
    }

    @Test
    fun `findAircraftInfo performs recursive lookup`() = runTest {
        everySuspend { piAwareApi.getAircraftTypes("server1") } returns mockAircraftTypes1
        everySuspend { piAwareApi.getAircraftInfo("server1", "A") } returns buildJsonObject {
            putJsonArray("children") { add("A8") }
        }
        everySuspend { piAwareApi.getAircraftInfo("server1", "A8") } returns buildJsonObject { put("B2C3", mockAircraftInfo) }

        repo.loadAircraftTypes(listOf("server1"))
        val result = repo.findAircraftInfo("server1", "A8B2C3")

        assertNotNull(result)
        assertEquals("N12345", result.registration)
        assertEquals("A320", result.icaoType)
        assertEquals("Airbus A320", result.typeDescription)
    }

    @Test
    fun `lookupAircraftInfoRecursive performs recursive lookup`() = runTest {
        everySuspend { piAwareApi.getAircraftTypes("server1") } returns mockAircraftTypes1
        everySuspend { piAwareApi.getAircraftInfo("server1", "A") } returns buildJsonObject {
            putJsonArray("children") { add("A8") }
        }
        everySuspend { piAwareApi.getAircraftInfo("server1", "A8") } returns buildJsonObject { put("B2C3", mockAircraftInfo) }

        repo.loadAircraftTypes(listOf("server1"))
        val result = repo.lookupAircraftInfoRecursive("server1", "A8B2C3")

        assertNotNull(result)
        assertEquals("N12345", result.registration)
        assertEquals("A320", result.icaoType)
        assertEquals("Airbus A320", result.typeDescription)
    }

    @Test
    fun `lookupAircraftInfoRecursive handles lowercase hex`() = runTest {
        everySuspend { piAwareApi.getAircraftTypes("server1") } returns mockAircraftTypes1
        everySuspend { piAwareApi.getAircraftInfo("server1", "A") } returns buildJsonObject { put("8B2C3", mockAircraftInfo) }

        repo.loadAircraftTypes(listOf("server1"))
        val result = repo.lookupAircraftInfoRecursive("server1", "a8b2c3")

        assertNotNull(result)
        assertEquals("N12345", result.registration)
        assertEquals("A320", result.icaoType)
        assertEquals("Airbus A320", result.typeDescription)
    }

    @Test
    fun `lookupFlight returns success`() = runTest {
        val mockResponse = FlightResponse(flights = emptyList(), links = null, numPages = 1)
        everySuspend { aeroApi.getFlight(any(), any(), any(), any(), any(), any()) } returns mockResponse

        val result = repo.lookupFlight("SWA123")

        assertTrue(result is Async.Success)
        assertEquals(mockResponse, (result as Async.Success).data)
    }

    @Test
    fun `lookupFlight returns error on exception`() = runTest {
        val exception = Exception("Network error")
        everySuspend { aeroApi.getFlight(any(), any(), any(), any(), any(), any()) } throws exception

        val result = repo.lookupFlight("SWA123")

        assertTrue(result is Async.Error)
        assertEquals("Failed to fetch flight for ident SWA123", (result as Async.Error).message)
    }

    // Trails tests

    @Test
    fun `getAircraft adds positions to trails`() = runTest {
        everySuspend { piAwareApi.getAircraft("server1") } returns listOf(mockAircraft1, mockAircraft2)

        repo.getAircraft(listOf("server1"))

        val trails = repo.aircraftTrails.value
        assertEquals(2, trails.size)
        assertTrue(trails.containsKey(mockAircraft1.hex))
        assertTrue(trails.containsKey(mockAircraft2.hex))
        assertEquals(1, trails[mockAircraft1.hex]?.positions?.size)
        assertEquals(1, trails[mockAircraft2.hex]?.positions?.size)
    }

    @Test
    fun `getAircraft does not add duplicate positions to trails`() = runTest {
        everySuspend { piAwareApi.getAircraft("server1") } returns listOf(mockAircraft1)

        repo.getAircraft(listOf("server1"))
        repo.getAircraft(listOf("server1")) // Same position again

        val trails = repo.aircraftTrails.value
        // Should still only have 1 position since position didn't change
        assertEquals(1, trails[mockAircraft1.hex]?.positions?.size)
    }

    @Test
    fun `getAircraft adds new positions to trails when location changes`() = runTest {
        everySuspend { piAwareApi.getAircraft("server1") } returns listOf(mockAircraft1)
        repo.getAircraft(listOf("server1"))

        val movedAircraft = mockAircraft1.copy(lat = 33.0, lon = -97.0)
        everySuspend { piAwareApi.getAircraft("server1") } returns listOf(movedAircraft)
        repo.getAircraft(listOf("server1"))

        val trails = repo.aircraftTrails.value
        assertEquals(2, trails[mockAircraft1.hex]?.positions?.size)
    }

    @Test
    fun `getAircraft only includes current aircraft in trails`() = runTest {
        // First update with aircraft1 and aircraft2
        everySuspend { piAwareApi.getAircraft("server1") } returns listOf(mockAircraft1, mockAircraft2)
        repo.getAircraft(listOf("server1"))

        // Second update with only aircraft1 (aircraft2 is no longer visible)
        everySuspend { piAwareApi.getAircraft("server1") } returns listOf(mockAircraft1)
        repo.getAircraft(listOf("server1"))

        val trails = repo.aircraftTrails.value
        // Only current aircraft (aircraft1) should be in the emitted trails
        assertEquals(1, trails.size)
        assertTrue(trails.containsKey(mockAircraft1.hex))
        assertTrue(!trails.containsKey(mockAircraft2.hex))
    }

    @Test
    fun `clearTrails removes all trail data`() = runTest {
        everySuspend { piAwareApi.getAircraft("server1") } returns listOf(mockAircraft1, mockAircraft2)
        repo.getAircraft(listOf("server1"))

        repo.clearTrails()

        val trails = repo.aircraftTrails.value
        assertTrue(trails.isEmpty())
    }

    @Test
    fun `fetchAndMergeHistory fetches history files from server`() = runTest {
        val historyReceiver = mockReceiver1090.copy(history = 2)
        val historyAircraft = Aircraft(hex = "abc123", lat = 32.5, lon = -96.5)

        everySuspend { piAwareApi.getDump1090ReceiverInfo("server1") } returns historyReceiver
        everySuspend { piAwareApi.getHistoryFile("server1", 0) } returns listOf(historyAircraft)
        everySuspend { piAwareApi.getHistoryFile("server1", 1) } returns listOf(historyAircraft.copy(lat = 32.6))

        repo.fetchAndMergeHistory("server1")

        // After fetching history, getAircraft needs to be called to see the trails
        // The trails are stored but not emitted until currentAircraftHex includes them
        everySuspend { piAwareApi.getAircraft("server1") } returns listOf(historyAircraft)
        repo.getAircraft(listOf("server1"))

        val trails = repo.aircraftTrails.value
        assertTrue(trails.containsKey(historyAircraft.hex))
        // Should have positions from history + current
        assertTrue(trails[historyAircraft.hex]!!.positions.size >= 1)
    }

    @Test
    fun `fetchAndMergeHistory handles null receiver`() = runTest {
        everySuspend { piAwareApi.getDump1090ReceiverInfo("server1") } returns null

        repo.fetchAndMergeHistory("server1")

        // Should complete without error
        val trails = repo.aircraftTrails.value
        assertTrue(trails.isEmpty())
    }

    @Test
    fun `fetchAndMergeHistory handles null history count`() = runTest {
        everySuspend { piAwareApi.getDump1090ReceiverInfo("server1") } returns mockReceiver1090

        repo.fetchAndMergeHistory("server1")

        // Should complete without error when history is null
        val trails = repo.aircraftTrails.value
        assertTrue(trails.isEmpty())
    }

    @Test
    fun `fetchAndMergeHistory handles failed history file requests`() = runTest {
        val historyReceiver = mockReceiver1090.copy(history = 3)
        val historyAircraft = Aircraft(hex = "abc123", lat = 32.5, lon = -96.5)

        everySuspend { piAwareApi.getDump1090ReceiverInfo("server1") } returns historyReceiver
        everySuspend { piAwareApi.getHistoryFile("server1", 0) } returns listOf(historyAircraft)
        everySuspend { piAwareApi.getHistoryFile("server1", 1) } returns null // Failed request
        everySuspend { piAwareApi.getHistoryFile("server1", 2) } returns listOf(historyAircraft.copy(lat = 32.6))

        repo.fetchAndMergeHistory("server1")

        // Should still work with partial data
        everySuspend { piAwareApi.getAircraft("server1") } returns listOf(historyAircraft)
        repo.getAircraft(listOf("server1"))
        val trails = repo.aircraftTrails.value
        assertTrue(trails.containsKey(historyAircraft.hex))
    }
}
