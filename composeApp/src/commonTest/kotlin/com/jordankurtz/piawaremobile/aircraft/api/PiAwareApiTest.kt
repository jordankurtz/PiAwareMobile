
package com.jordankurtz.piawaremobile.aircraft.api

import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.ICAOAircraftType
import com.jordankurtz.piawaremobile.model.PiAwareResponse
import com.jordankurtz.piawaremobile.model.Receiver
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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PiAwareApiTest {

    @Test
    fun `getAircraft returns aircraft list on success`() = runTest {
        // Arrange
        val mockAircraft = listOf(
            Aircraft(hex = "a8b2c3", flight = "SWA123", lat = 32.7, lon = -96.8)
        )
        val mockResponse = PiAwareResponse(aircraft = mockAircraft)
        val mockJson = Json.encodeToString(mockResponse)

        val mockEngine = MockEngine { request ->
            assertEquals("http://test-host/data/aircraft.json", request.url.toString())
            respond(
                content = mockJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }
        val piAwareApi = PiAwareApiImpl(httpClient)

        // Act
        val result = piAwareApi.getAircraft("test-host")

        // Assert
        assertEquals(mockAircraft, result)
    }

    @Test
    fun `getAircraft returns empty list on network error`() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            respond(
                content = "Error",
                status = HttpStatusCode.InternalServerError,
            )
        }
        val httpClient = HttpClient(mockEngine)
        val piAwareApi = PiAwareApiImpl(httpClient)

        // Act
        val result = piAwareApi.getAircraft("test-host")

        // Assert
        assertEquals(emptyList(), result)
    }

    @Test
    fun `getAircraftTypes returns aircraft types on success`() = runTest {
        // Arrange
        val mockAircraftTypes = mapOf(
            "A320" to ICAOAircraftType("Airbus A320", "L2J")
        )
        val mockJson = Json.encodeToString(mockAircraftTypes)

        val mockEngine = MockEngine { request ->
            assertEquals("http://test-host/db/aircraft_types/icao_aircraft_types.json", request.url.toString())
            respond(
                content = mockJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }
        val piAwareApi = PiAwareApiImpl(httpClient)

        // Act
        val result = piAwareApi.getAircraftTypes("test-host")

        // Assert
        assertEquals(mockAircraftTypes, result)
    }

    @Test
    fun `getAircraftTypes returns empty map on network error`() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            respond(
                content = "Error",
                status = HttpStatusCode.InternalServerError,
            )
        }
        val httpClient = HttpClient(mockEngine)
        val piAwareApi = PiAwareApiImpl(httpClient)

        // Act
        val result = piAwareApi.getAircraftTypes("test-host")

        // Assert
        assertEquals(emptyMap(), result)
    }

    @Test
    fun `getAircraftInfo returns aircraft info on success`() = runTest {
        // Arrange
        val mockAircraftInfo = buildJsonObject { put("key", "value") }
        val mockJson = Json.encodeToString(mockAircraftInfo)

        val mockEngine = MockEngine { request ->
            assertEquals("http://test-host/db/test-bkey.json", request.url.toString())
            respond(
                content = mockJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }
        val piAwareApi = PiAwareApiImpl(httpClient)

        // Act
        val result = piAwareApi.getAircraftInfo("test-host", "test-bkey")

        // Assert
        assertEquals(mockAircraftInfo, result)
    }

    @Test
    fun `getAircraftInfo returns cached aircraft info on second call`() = runTest {
        // Arrange
        val mockAircraftInfo = buildJsonObject { put("key", "value") }
        val mockJson = Json.encodeToString(mockAircraftInfo)
        var requestCount = 0

        val mockEngine = MockEngine { request ->
            requestCount++
            if (requestCount > 1) {
                error("Should not be called more than once")
            }
            assertEquals("http://test-host/db/test-bkey.json", request.url.toString())
            respond(
                content = mockJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }
        val piAwareApi = PiAwareApiImpl(httpClient)

        // Act
        val result1 = piAwareApi.getAircraftInfo("test-host", "test-bkey")
        val result2 = piAwareApi.getAircraftInfo("test-host", "test-bkey")

        // Assert
        assertEquals(mockAircraftInfo, result1)
        assertEquals(mockAircraftInfo, result2)
        assertEquals(1, requestCount)
    }

    @Test
    fun `getAircraftInfo returns null on network error`() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            respond(
                content = "Error",
                status = HttpStatusCode.InternalServerError,
            )
        }
        val httpClient = HttpClient(mockEngine)
        val piAwareApi = PiAwareApiImpl(httpClient)

        // Act
        val result = piAwareApi.getAircraftInfo("test-host", "test-bkey")

        // Assert
        assertNull(result)
    }

    @Test
    fun `getDump1090ReceiverInfo returns receiver info on success`() = runTest {
        // Arrange
        val mockReceiver = Receiver(latitude = 32.7f, longitude = -96.8f)
        val mockJson = Json.encodeToString(mockReceiver)

        val mockEngine = MockEngine { request ->
            assertEquals("http://test-host/data/receiver.json", request.url.toString())
            respond(
                content = mockJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }
        val piAwareApi = PiAwareApiImpl(httpClient)

        // Act
        val result = piAwareApi.getDump1090ReceiverInfo("test-host")

        // Assert
        assertEquals(mockReceiver, result)
    }

    @Test
    fun `getDump1090ReceiverInfo returns null on network error`() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            respond(
                content = "Error",
                status = HttpStatusCode.InternalServerError,
            )
        }
        val httpClient = HttpClient(mockEngine)
        val piAwareApi = PiAwareApiImpl(httpClient)

        // Act
        val result = piAwareApi.getDump1090ReceiverInfo("test-host")

        // Assert
        assertNull(result)
    }

    @Test
    fun `getDump978ReceiverInfo returns receiver info on success`() = runTest {
        // Arrange
        val mockReceiver = Receiver(latitude = 32.7f, longitude = -96.8f)
        val mockJson = Json.encodeToString(mockReceiver)

        val mockEngine = MockEngine { request ->
            assertEquals("http://test-host/data-978/receiver.json", request.url.toString())
            respond(
                content = mockJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }
        val piAwareApi = PiAwareApiImpl(httpClient)

        // Act
        val result = piAwareApi.getDump978ReceiverInfo("test-host")

        // Assert
        assertEquals(mockReceiver, result)
    }

    @Test
    fun `getDump978ReceiverInfo returns null on network error`() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            respond(
                content = "Error",
                status = HttpStatusCode.InternalServerError,
            )
        }
        val httpClient = HttpClient(mockEngine)
        val piAwareApi = PiAwareApiImpl(httpClient)

        // Act
        val result = piAwareApi.getDump978ReceiverInfo("test-host")

        // Assert
        assertNull(result)
    }
}
