package com.jordankurtz.piawaremobile.aircraft.api.impl

import com.jordankurtz.piawaremobile.model.FlightResponse
import com.jordankurtz.piawaremobile.settings.usecase.GetFlightAwareApiKeyUseCase
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AeroApiImplTest {

    private lateinit var getFlightAwareApiKeyUseCase: GetFlightAwareApiKeyUseCase
    private lateinit var aeroApi: AeroApiImpl
    private lateinit var mockEngine: MockEngine

    private val mockFlightResponse = FlightResponse(
        flights = emptyList(),
        links = null,
        numPages = 1
    )
    private val json = Json { ignoreUnknownKeys = true }
    private val mockResponseJson = json.encodeToString(mockFlightResponse)

    @BeforeTest
    fun setup() {
        getFlightAwareApiKeyUseCase = mock()
        mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(mockResponseJson),
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        aeroApi = AeroApiImpl(client, getFlightAwareApiKeyUseCase)
    }

    @Test
    fun `getFlight calls api with correct parameters`() = runTest {
        val ident = "SWA123"
        val identType = "designator"
        val start = "2023-01-01T00:00:00Z"
        val end = "2023-01-01T12:00:00Z"
        val maxPages = 5
        val apiKey = "test_api_key"

        everySuspend { getFlightAwareApiKeyUseCase() } returns apiKey

        val result = aeroApi.getFlight(
            ident = ident,
            identType = identType,
            start = start,
            end = end,
            maxPages = maxPages,
        )

        assertEquals(mockFlightResponse, result)

        val request = mockEngine.requestHistory.first()
        val expectedUrl = "https://aeroapi.flightaware.com/aeroapi/flights/$ident?ident_type=$identType&start=2023-01-01T00%3A00%3A00Z&end=2023-01-01T12%3A00%3A00Z&max_pages=$maxPages"
        assertEquals(expectedUrl, request.url.toString())
        assertEquals(apiKey, request.headers["x-apikey"])
    }
}
