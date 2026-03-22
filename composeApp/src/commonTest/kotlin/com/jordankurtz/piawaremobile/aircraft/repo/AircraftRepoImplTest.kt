package com.jordankurtz.piawaremobile.aircraft.repo

import com.jordankurtz.piawaremobile.aircraft.api.AeroApi
import com.jordankurtz.piawaremobile.aircraft.api.PiAwareApi
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.FlightResponse
import com.jordankurtz.piawaremobile.model.ICAOAircraftType
import com.jordankurtz.piawaremobile.model.PiAwareResponse
import com.jordankurtz.piawaremobile.model.Receiver
import com.jordankurtz.piawaremobile.model.ReceiverType
import com.jordankurtz.piawaremobile.settings.Server
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
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
    private lateinit var trailManager: AircraftTrailManager
    private lateinit var repo: AircraftRepoImpl

    private val server1 = Server(name = "Server 1", address = "server1")
    private val server2 = Server(name = "Server 2", address = "server2")
    private val mockAircraft1 = Aircraft(hex = "a8b2c3", flight = "SWA123", lat = 32.7, lon = -96.8)
    private val mockAircraft2 = Aircraft(hex = "a1b2c3", flight = "DAL456", lat = 32.8, lon = -96.9)
    private val mockAircraftNoLocation = Aircraft(hex = "d4e5f6", flight = "UAL789", lat = 0.0, lon = 0.0)
    private val mockReceiver1090 = Receiver(latitude = 32.7f, longitude = -96.8f)
    private val mockReceiver978 = Receiver(latitude = 32.8f, longitude = -96.9f)
    private val mockAircraftTypes1 = mapOf("A320" to ICAOAircraftType("Airbus A320", "L2J"))
    private val mockAircraftTypes2 = mapOf("B738" to ICAOAircraftType("Boeing 737-800", "L2J"))
    private val mockAircraftInfo =
        buildJsonObject {
            put("i", "N12345")
            put("t", "A320")
        }

    @BeforeTest
    fun setup() {
        piAwareApi = mock()
        aeroApi = mock()
        trailManager = mock()
        everySuspend { trailManager.updateTrailsFromAircraft(any()) } returns Unit
        everySuspend { trailManager.mergeHistoryResponses(any()) } returns Unit
        repo = AircraftRepoImpl(piAwareApi, aeroApi, trailManager)
    }

    @Test
    fun `getAircraftWithServers returns merged aircraft from multiple servers with server tracking`() =
        runTest {
            everySuspend { piAwareApi.getAircraft("server1") } returns listOf(mockAircraft1)
            everySuspend { piAwareApi.getAircraft("server2") } returns listOf(mockAircraft2)

            val result = repo.getAircraftWithServers(listOf(server1, server2))

            assertEquals(2, result.size)
            assertEquals(setOf(server1), result[mockAircraft1])
            assertEquals(setOf(server2), result[mockAircraft2])
        }

    @Test
    fun `getAircraftWithServers tracks multiple servers for same aircraft`() =
        runTest {
            everySuspend { piAwareApi.getAircraft("server1") } returns listOf(mockAircraft1)
            everySuspend { piAwareApi.getAircraft("server2") } returns listOf(mockAircraft1)

            val result = repo.getAircraftWithServers(listOf(server1, server2))

            assertEquals(1, result.size)
            assertEquals(setOf(server1, server2), result[mockAircraft1])
        }

    @Test
    fun `getAircraftWithServers handles one server failing`() =
        runTest {
            everySuspend { piAwareApi.getAircraft("server1") } returns listOf(mockAircraft1)
            everySuspend { piAwareApi.getAircraft("server2") } throws Exception("Network error")

            val result = repo.getAircraftWithServers(listOf(server1, server2))

            assertEquals(1, result.size)
            assertEquals(setOf(server1), result[mockAircraft1])
        }

    @Test
    fun `getAircraftWithServers filters out aircraft with no location`() =
        runTest {
            everySuspend { piAwareApi.getAircraft("server1") } returns listOf(mockAircraft1, mockAircraftNoLocation)

            val result = repo.getAircraftWithServers(listOf(server1))

            assertEquals(1, result.size)
            assertTrue(result.containsKey(mockAircraft1))
            assertTrue(!result.containsKey(mockAircraftNoLocation))
        }

    @Test
    fun `getAircraftWithServers delegates trail update to trail manager`() =
        runTest {
            everySuspend { piAwareApi.getAircraft("server1") } returns listOf(mockAircraft1)

            repo.getAircraftWithServers(listOf(server1))

            verifySuspend(mode = VerifyMode.exactly(1)) {
                trailManager.updateTrailsFromAircraft(any())
            }
        }

    @Test
    fun `getReceiverInfo returns dump1090 receiver info`() =
        runTest {
            everySuspend { piAwareApi.getDump1090ReceiverInfo("server1") } returns mockReceiver1090

            val result = repo.getReceiverInfo("server1", ReceiverType.DUMP_1090)

            assertEquals(mockReceiver1090, result)
        }

    @Test
    fun `getReceiverInfo returns dump978 receiver info`() =
        runTest {
            everySuspend { piAwareApi.getDump978ReceiverInfo("server1") } returns mockReceiver978

            val result = repo.getReceiverInfo("server1", ReceiverType.DUMP_978)

            assertEquals(mockReceiver978, result)
        }

    @Test
    fun `getReceiverInfo returns null when receiver info not found`() =
        runTest {
            everySuspend { piAwareApi.getDump1090ReceiverInfo("server1") } returns null
            everySuspend { piAwareApi.getDump978ReceiverInfo("server1") } returns null

            val result1090 = repo.getReceiverInfo("server1", ReceiverType.DUMP_1090)
            val result978 = repo.getReceiverInfo("server1", ReceiverType.DUMP_978)

            assertNull(result1090)
            assertNull(result978)
        }

    @Test
    fun `loadAircraftTypes merges types from multiple servers`() =
        runTest {
            everySuspend { piAwareApi.getAircraftTypes("server1") } returns mockAircraftTypes1
            everySuspend { piAwareApi.getAircraftTypes("server2") } returns mockAircraftTypes2
            everySuspend { piAwareApi.getAircraftInfo(any(), any()) } returns null

            repo.loadAircraftTypes(listOf(server1, server2))
        }

    @Test
    fun `findAircraftInfo returns correct info`() =
        runTest {
            everySuspend { piAwareApi.getAircraftTypes("server1") } returns mockAircraftTypes1
            everySuspend {
                piAwareApi.getAircraftInfo("server1", "A")
            } returns buildJsonObject { put("8B2C3", mockAircraftInfo) }

            repo.loadAircraftTypes(listOf(server1))
            val result = repo.findAircraftInfo("server1", "A8B2C3")

            assertNotNull(result)
            assertEquals("N12345", result.registration)
            assertEquals("A320", result.icaoType)
            assertEquals("Airbus A320", result.typeDescription)
        }

    @Test
    fun `findAircraftInfo performs recursive lookup`() =
        runTest {
            everySuspend { piAwareApi.getAircraftTypes("server1") } returns mockAircraftTypes1
            everySuspend { piAwareApi.getAircraftInfo("server1", "A") } returns
                buildJsonObject {
                    putJsonArray("children") { add("A8") }
                }
            everySuspend {
                piAwareApi.getAircraftInfo("server1", "A8")
            } returns buildJsonObject { put("B2C3", mockAircraftInfo) }

            repo.loadAircraftTypes(listOf(server1))
            val result = repo.findAircraftInfo("server1", "A8B2C3")

            assertNotNull(result)
            assertEquals("N12345", result.registration)
            assertEquals("A320", result.icaoType)
            assertEquals("Airbus A320", result.typeDescription)
        }

    @Test
    fun `lookupAircraftInfoRecursive performs recursive lookup`() =
        runTest {
            everySuspend { piAwareApi.getAircraftTypes("server1") } returns mockAircraftTypes1
            everySuspend { piAwareApi.getAircraftInfo("server1", "A") } returns
                buildJsonObject {
                    putJsonArray("children") { add("A8") }
                }
            everySuspend {
                piAwareApi.getAircraftInfo("server1", "A8")
            } returns buildJsonObject { put("B2C3", mockAircraftInfo) }

            repo.loadAircraftTypes(listOf(server1))
            val result = repo.lookupAircraftInfoRecursive("server1", "A8B2C3")

            assertNotNull(result)
            assertEquals("N12345", result.registration)
            assertEquals("A320", result.icaoType)
            assertEquals("Airbus A320", result.typeDescription)
        }

    @Test
    fun `lookupAircraftInfoRecursive handles lowercase hex`() =
        runTest {
            everySuspend { piAwareApi.getAircraftTypes("server1") } returns mockAircraftTypes1
            everySuspend {
                piAwareApi.getAircraftInfo("server1", "A")
            } returns buildJsonObject { put("8B2C3", mockAircraftInfo) }

            repo.loadAircraftTypes(listOf(server1))
            val result = repo.lookupAircraftInfoRecursive("server1", "a8b2c3")

            assertNotNull(result)
            assertEquals("N12345", result.registration)
            assertEquals("A320", result.icaoType)
            assertEquals("Airbus A320", result.typeDescription)
        }

    @Test
    fun `lookupFlight returns success`() =
        runTest {
            val mockResponse = FlightResponse(flights = emptyList(), links = null, numPages = 1)
            everySuspend { aeroApi.getFlight(any(), any(), any(), any(), any(), any()) } returns mockResponse

            val result = repo.lookupFlight("SWA123")

            assertTrue(result is Async.Success)
            assertEquals(mockResponse, (result as Async.Success).data)
        }

    @Test
    fun `lookupFlight returns error on exception`() =
        runTest {
            val exception = Exception("Network error")
            everySuspend { aeroApi.getFlight(any(), any(), any(), any(), any(), any()) } throws exception

            val result = repo.lookupFlight("SWA123")

            assertTrue(result is Async.Error)
            assertEquals("Failed to fetch flight for ident SWA123", (result as Async.Error).message)
        }

    @Test
    fun `fetchAndMergeHistory delegates to trail manager`() =
        runTest {
            val historyReceiver = mockReceiver1090.copy(history = 2)
            val historyAircraft = Aircraft(hex = "abc123", lat = 32.5, lon = -96.5, seenPos = 5f)

            everySuspend { piAwareApi.getDump1090ReceiverInfo("server1") } returns historyReceiver
            everySuspend { piAwareApi.getHistoryFile("server1", 0) } returns
                PiAwareResponse(now = 1000.0, aircraft = listOf(historyAircraft))
            everySuspend { piAwareApi.getHistoryFile("server1", 1) } returns
                PiAwareResponse(now = 1030.0, aircraft = listOf(historyAircraft.copy(lat = 32.6, seenPos = 5f)))

            repo.fetchAndMergeHistory("server1")

            verifySuspend(mode = VerifyMode.exactly(1)) {
                trailManager.mergeHistoryResponses(any())
            }
        }

    @Test
    fun `fetchAndMergeHistory handles null receiver`() =
        runTest {
            everySuspend { piAwareApi.getDump1090ReceiverInfo("server1") } returns null

            repo.fetchAndMergeHistory("server1")

            verifySuspend(mode = VerifyMode.exactly(0)) {
                trailManager.mergeHistoryResponses(any())
            }
        }

    @Test
    fun `fetchAndMergeHistory handles null history count`() =
        runTest {
            everySuspend { piAwareApi.getDump1090ReceiverInfo("server1") } returns mockReceiver1090

            repo.fetchAndMergeHistory("server1")

            verifySuspend(mode = VerifyMode.exactly(0)) {
                trailManager.mergeHistoryResponses(any())
            }
        }

    @Test
    fun `fetchAndMergeHistory handles zero history count`() =
        runTest {
            everySuspend { piAwareApi.getDump1090ReceiverInfo("server1") } returns
                mockReceiver1090.copy(history = 0)

            repo.fetchAndMergeHistory("server1")

            verifySuspend(mode = VerifyMode.exactly(0)) {
                trailManager.mergeHistoryResponses(any())
            }
        }

    @Test
    fun `getAircraftWithServers keeps fresher aircraft data when seen from multiple servers`() =
        runTest {
            val staleAircraft = mockAircraft1.copy(seen = 10f, altBaro = "10000")
            val freshAircraft = mockAircraft1.copy(seen = 1f, altBaro = "20000")
            everySuspend { piAwareApi.getAircraft("server1") } returns listOf(staleAircraft)
            everySuspend { piAwareApi.getAircraft("server2") } returns listOf(freshAircraft)

            val result = repo.getAircraftWithServers(listOf(server1, server2))

            assertEquals(1, result.size)
            val aircraft = result.keys.first()
            assertEquals("20000", aircraft.altBaro)
            assertEquals(setOf(server1, server2), result[aircraft])
        }

    @Test
    fun `getAircraftWithServers returns empty map for empty server list`() =
        runTest {
            val result = repo.getAircraftWithServers(emptyList())

            assertTrue(result.isEmpty())
        }

    @Test
    fun `findAircraftInfo returns cached result on second call`() =
        runTest {
            everySuspend { piAwareApi.getAircraftTypes("server1") } returns mockAircraftTypes1
            everySuspend {
                piAwareApi.getAircraftInfo("server1", "A")
            } returns buildJsonObject { put("8B2C3", mockAircraftInfo) }

            repo.loadAircraftTypes(listOf(server1))
            val first = repo.findAircraftInfo("server1", "A8B2C3")
            val second = repo.findAircraftInfo("server1", "A8B2C3")

            assertNotNull(first)
            assertNotNull(second)
            assertEquals(first, second)
            // API should only be called once due to caching
            verifySuspend(mode = VerifyMode.exactly(1)) {
                piAwareApi.getAircraftInfo("server1", "A")
            }
        }

    @Test
    fun `loadAircraftTypes only loads once`() =
        runTest {
            everySuspend { piAwareApi.getAircraftTypes("server1") } returns mockAircraftTypes1

            repo.loadAircraftTypes(listOf(server1))
            repo.loadAircraftTypes(listOf(server1))

            verifySuspend(mode = VerifyMode.exactly(1)) {
                piAwareApi.getAircraftTypes("server1")
            }
        }
}
