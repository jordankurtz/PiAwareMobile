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
    fun `getDump978ReceiverInfo returns null`() =
        runTest {
            val mockEngine =
                MockEngine {
                    error("Should not make any HTTP requests")
                }
            val dataSource = createDataSource(mockEngine)

            val result = dataSource.getDump978ReceiverInfo(server)

            assertNull(result)
        }

    @Test
    fun `fetchTrails fetches traces for all aircraft with position`() =
        runTest {
            val aircraft = listOf(Aircraft(hex = "abc123", lat = 32.7, lon = -96.8))
            val aircraftJson = Json.encodeToString(PiAwareResponse(aircraft = aircraft))
            val traceJson = """{"icao":"abc123","timestamp":1000.0,"trace":[[5.0,32.7,-96.8,35000]]}"""

            val mockEngine =
                MockEngine { request ->
                    when (request.url.toString()) {
                        "http://readsb-host/data/aircraft.json" ->
                            respond(
                                content = aircraftJson,
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        "http://readsb-host/data/traces/ab/abc123.json" ->
                            respond(
                                content = traceJson,
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        else -> error("Unexpected URL: ${request.url}")
                    }
                }
            val dataSource = createDataSource(mockEngine)

            val result = dataSource.fetchTrails(server)

            assertEquals(1, result.size)
            val positions = result["abc123"]!!
            assertEquals(1, positions.size)
            assertEquals(32.7, positions[0].latitude)
            assertEquals(-96.8, positions[0].longitude)
            assertEquals(1005.0, positions[0].timestamp) // 1000.0 + 5.0
            assertEquals("35000", positions[0].altitude)
        }

    @Test
    fun `fetchTrails constructs trace URL using two-char hex prefix`() =
        runTest {
            val aircraft = listOf(Aircraft(hex = "abc123", lat = 32.7, lon = -96.8))
            val aircraftJson = Json.encodeToString(PiAwareResponse(aircraft = aircraft))
            val traceJson = """{"icao":"abc123","timestamp":1000.0,"trace":[]}"""

            var traceUrl = ""
            val mockEngine =
                MockEngine { request ->
                    if (request.url.toString().contains("traces")) {
                        traceUrl = request.url.toString()
                    }
                    respond(
                        content = if (request.url.toString().contains("aircraft")) aircraftJson else traceJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val dataSource = createDataSource(mockEngine)

            dataSource.fetchTrails(server)

            assertEquals("http://readsb-host/data/traces/ab/abc123.json", traceUrl)
        }

    @Test
    fun `fetchTrails handles ground altitude in trace entry`() =
        runTest {
            val aircraft = listOf(Aircraft(hex = "abc123", lat = 32.7, lon = -96.8))
            val aircraftJson = Json.encodeToString(PiAwareResponse(aircraft = aircraft))
            val traceJson = """{"icao":"abc123","timestamp":1000.0,"trace":[[0.0,32.7,-96.8,"ground"]]}"""

            val mockEngine =
                MockEngine { request ->
                    respond(
                        content = if (request.url.toString().contains("aircraft")) aircraftJson else traceJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val dataSource = createDataSource(mockEngine)

            val result = dataSource.fetchTrails(server)

            assertEquals("ground", result["abc123"]?.first()?.altitude)
        }

    @Test
    fun `fetchTrails handles null altitude in trace entry`() =
        runTest {
            val aircraft = listOf(Aircraft(hex = "abc123", lat = 32.7, lon = -96.8))
            val aircraftJson = Json.encodeToString(PiAwareResponse(aircraft = aircraft))
            val traceJson = """{"icao":"abc123","timestamp":1000.0,"trace":[[0.0,32.7,-96.8,null]]}"""

            val mockEngine =
                MockEngine { request ->
                    respond(
                        content = if (request.url.toString().contains("aircraft")) aircraftJson else traceJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val dataSource = createDataSource(mockEngine)

            val result = dataSource.fetchTrails(server)

            assertNull(result["abc123"]?.first()?.altitude)
        }

    @Test
    fun `fetchTrails skips aircraft without position`() =
        runTest {
            val aircraft =
                listOf(
                    // no position — hasPosition is false when lat == 0.0 && lon == 0.0
                    Aircraft(hex = "abc123", lat = 0.0, lon = 0.0),
                    Aircraft(hex = "def456", lat = 32.7, lon = -96.8),
                )
            val aircraftJson = Json.encodeToString(PiAwareResponse(aircraft = aircraft))
            val traceJson = """{"icao":"def456","timestamp":1000.0,"trace":[]}"""

            var requestCount = 0
            val mockEngine =
                MockEngine { request ->
                    if (request.url.toString().contains("traces")) requestCount++
                    respond(
                        content = if (request.url.toString().contains("aircraft")) aircraftJson else traceJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val dataSource = createDataSource(mockEngine)

            dataSource.fetchTrails(server)

            assertEquals(1, requestCount) // only def456 fetched, not abc123
        }

    @Test
    fun `fetchTrails returns empty map when aircraft fetch fails`() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(content = "Error", status = HttpStatusCode.InternalServerError)
                }
            val dataSource = createDataSource(mockEngine)

            val result = dataSource.fetchTrails(server)

            assertEquals(emptyMap(), result)
        }

    @Test
    fun `fetchTrails skips failed trace fetches and returns successful ones`() =
        runTest {
            val aircraft =
                listOf(
                    Aircraft(hex = "abc123", lat = 32.7, lon = -96.8),
                    Aircraft(hex = "def456", lat = 33.0, lon = -97.0),
                )
            val aircraftJson = Json.encodeToString(PiAwareResponse(aircraft = aircraft))
            val traceJson = """{"icao":"abc123","timestamp":1000.0,"trace":[[0.0,32.7,-96.8,35000]]}"""

            val mockEngine =
                MockEngine { request ->
                    when {
                        request.url.toString().contains("aircraft") ->
                            respond(
                                content = aircraftJson,
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        request.url.toString().contains("abc123") ->
                            respond(
                                content = traceJson,
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        else -> respond(content = "Error", status = HttpStatusCode.InternalServerError)
                    }
                }
            val dataSource = createDataSource(mockEngine)

            val result = dataSource.fetchTrails(server)

            assertEquals(1, result.size)
            assertNotNull(result["abc123"])
        }
}
