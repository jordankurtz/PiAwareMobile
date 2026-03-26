package com.jordankurtz.piawaremobile.aircraft.api

import com.jordankurtz.piawaremobile.aircraft.api.impl.ReadsbDataSource
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.ICAOAircraftType
import com.jordankurtz.piawaremobile.model.PiAwareResponse
import com.jordankurtz.piawaremobile.model.Receiver
import com.jordankurtz.piawaremobile.settings.Server
import com.jordankurtz.piawaremobile.settings.ServerType
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ReadsbDataSourceTest {
    private val server = Server(name = "Readsb", address = "readsb-host", type = ServerType.READSB)

    private fun createDataSource(mockEngine: MockEngine): ReadsbDataSource {
        val httpClient =
            HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json()
                }
            }
        return ReadsbDataSource(httpClient)
    }

    @Test
    fun `getAircraft returns aircraft list on success`() =
        runTest {
            val mockAircraft = listOf(Aircraft(hex = "abc123", lat = 32.7, lon = -96.8))
            val mockResponse = PiAwareResponse(aircraft = mockAircraft)
            val mockJson = Json.encodeToString(mockResponse)

            val mockEngine =
                MockEngine { request ->
                    assertEquals("http://readsb-host/data/aircraft.json", request.url.toString())
                    respond(
                        content = mockJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val dataSource = createDataSource(mockEngine)

            val result = dataSource.getAircraft(server)

            assertEquals(mockAircraft, result)
        }

    @Test
    fun `getAircraft returns empty list on error`() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(content = "Error", status = HttpStatusCode.InternalServerError)
                }
            val dataSource = createDataSource(mockEngine)

            val result = dataSource.getAircraft(server)

            assertEquals(emptyList(), result)
        }

    @Test
    fun `getReceiverInfo returns receiver on success`() =
        runTest {
            val mockReceiver = Receiver(latitude = 32.7f, longitude = -96.8f)
            val mockJson = Json.encodeToString(mockReceiver)

            val mockEngine =
                MockEngine { request ->
                    assertEquals("http://readsb-host/data/receiver.json", request.url.toString())
                    respond(
                        content = mockJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val dataSource = createDataSource(mockEngine)

            val result = dataSource.getReceiverInfo(server)

            assertEquals(mockReceiver, result)
        }

    @Test
    fun `getReceiverInfo returns null on error`() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(content = "Error", status = HttpStatusCode.InternalServerError)
                }
            val dataSource = createDataSource(mockEngine)

            val result = dataSource.getReceiverInfo(server)

            assertNull(result)
        }

    @Test
    fun `getAircraftTypes returns types on success`() =
        runTest {
            val mockTypes = mapOf("A320" to ICAOAircraftType("Airbus A320", "L2J"))
            val mockJson = Json.encodeToString(mockTypes)

            val mockEngine =
                MockEngine { request ->
                    assertEquals(
                        "http://readsb-host/db/aircraft_types/icao_aircraft_types.json",
                        request.url.toString(),
                    )
                    respond(
                        content = mockJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val dataSource = createDataSource(mockEngine)

            val result = dataSource.getAircraftTypes(server)

            assertEquals(mockTypes, result)
        }

    @Test
    fun `getAircraftTypes returns empty map on error`() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(content = "Error", status = HttpStatusCode.InternalServerError)
                }
            val dataSource = createDataSource(mockEngine)

            val result = dataSource.getAircraftTypes(server)

            assertEquals(emptyMap(), result)
        }

    @Test
    fun `getAircraftInfo returns json on success`() =
        runTest {
            val mockJson = """{"i":"N12345","t":"A320"}"""

            val mockEngine =
                MockEngine { request ->
                    assertEquals("http://readsb-host/db/A8.json", request.url.toString())
                    respond(
                        content = mockJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val dataSource = createDataSource(mockEngine)

            val result = dataSource.getAircraftInfo(server, "A8")

            assertNotNull(result)
            assertEquals("N12345", result["i"]?.let { Json.decodeFromJsonElement(it) })
        }

    @Test
    fun `getAircraftInfo returns null on error`() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(content = "Error", status = HttpStatusCode.InternalServerError)
                }
            val dataSource = createDataSource(mockEngine)

            val result = dataSource.getAircraftInfo(server, "A8")

            assertNull(result)
        }

    @Test
    fun `getHistory returns null for readsb servers`() =
        runTest {
            val mockEngine =
                MockEngine {
                    error("Should not make any HTTP requests")
                }
            val dataSource = createDataSource(mockEngine)

            val result = dataSource.getHistory(server, 0)

            assertNull(result)
        }
}
