package com.jordankurtz.piawaremobile.aircraft.repo

import com.jordankurtz.piawaremobile.aircraft.api.PiAwareApi
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.ICAOAircraftType
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

class AircraftRepoImplTest {

    private lateinit var piAwareApi: PiAwareApi
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
        repo = AircraftRepoImpl(piAwareApi)
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
}
