package com.jordankurtz.piawaremobile.aircraft.api

import com.jordankurtz.piawaremobile.aircraft.api.impl.PiAwareDataSource
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.ICAOAircraftType
import com.jordankurtz.piawaremobile.model.PiAwareResponse
import com.jordankurtz.piawaremobile.model.Receiver
import com.jordankurtz.piawaremobile.settings.Server
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PiAwareDataSourceTest {
    private val piAwareApi: PiAwareApi = mock()
    private val dataSource = PiAwareDataSource(piAwareApi)
    private val server = Server(name = "Test", address = "test-host")

    @Test
    fun `getAircraft delegates to PiAwareApi`() =
        runTest {
            val expected = listOf(Aircraft(hex = "abc123", lat = 32.7, lon = -96.8))
            everySuspend { piAwareApi.getAircraft("test-host") } returns expected

            val result = dataSource.getAircraft(server)

            assertEquals(expected, result)
            verifySuspend { piAwareApi.getAircraft("test-host") }
        }

    @Test
    fun `getReceiverInfo delegates to getDump1090ReceiverInfo`() =
        runTest {
            val expected = Receiver(latitude = 32.7f, longitude = -96.8f)
            everySuspend { piAwareApi.getDump1090ReceiverInfo("test-host") } returns expected

            val result = dataSource.getReceiverInfo(server)

            assertEquals(expected, result)
        }

    @Test
    fun `getReceiverInfo returns null when API returns null`() =
        runTest {
            everySuspend { piAwareApi.getDump1090ReceiverInfo("test-host") } returns null

            val result = dataSource.getReceiverInfo(server)

            assertNull(result)
        }

    @Test
    fun `getAircraftTypes delegates to PiAwareApi`() =
        runTest {
            val expected = mapOf("A320" to ICAOAircraftType("Airbus A320", "L2J"))
            everySuspend { piAwareApi.getAircraftTypes("test-host") } returns expected

            val result = dataSource.getAircraftTypes(server)

            assertEquals(expected, result)
        }

    @Test
    fun `getAircraftInfo delegates to PiAwareApi`() =
        runTest {
            val expected = buildJsonObject { put("key", "value") }
            everySuspend { piAwareApi.getAircraftInfo("test-host", "A") } returns expected

            val result = dataSource.getAircraftInfo(server, "A")

            assertEquals(expected, result)
        }

    @Test
    fun `getHistory delegates to PiAwareApi getHistoryFile`() =
        runTest {
            val expected = PiAwareResponse(now = 1000.0, aircraft = emptyList())
            everySuspend { piAwareApi.getHistoryFile("test-host", 0) } returns expected

            val result = dataSource.getHistory(server, 0)

            assertEquals(expected, result)
        }
}
