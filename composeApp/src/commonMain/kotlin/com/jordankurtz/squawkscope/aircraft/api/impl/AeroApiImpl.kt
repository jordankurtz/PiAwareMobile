package com.jordankurtz.squawkscope.aircraft.api.impl

import com.jordankurtz.squawkscope.aircraft.api.AeroApi
import com.jordankurtz.squawkscope.model.FlightResponse
import com.jordankurtz.squawkscope.settings.usecase.GetFlightAwareApiKeyUseCase
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import org.koin.core.annotation.Factory

@Factory(binds = [AeroApi::class])
class AeroApiImpl(
    private val client: HttpClient,
    private val getFlightAwareApiKeyUseCase: GetFlightAwareApiKeyUseCase,
) : AeroApi {
    override suspend fun getFlight(
        ident: String,
        identType: String?,
        start: String?,
        end: String?,
        maxPages: Int?,
        cursor: String?,
    ): FlightResponse {
        return client.get("https://aeroapi.flightaware.com/aeroapi/flights/$ident") {
            header("x-apikey", getFlightAwareApiKeyUseCase())
            parameter("ident_type", identType)
            parameter("start", start)
            parameter("end", end)
            parameter("max_pages", maxPages)
            parameter("cursor", cursor)
        }.body()
    }
}
