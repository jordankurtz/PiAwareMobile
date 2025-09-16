package com.jordankurtz.piawaremobile.api

import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftInfo
import com.jordankurtz.piawaremobile.model.ICAOAircraftType
import com.jordankurtz.piawaremobile.model.PiAwareResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class PiAwareApi(private val httpClient: HttpClient) {

    private val aircraftInfoMap = mutableMapOf<String, JsonObject>()

    suspend fun getAircraft(host: String): List<Aircraft> {
        return try {
            val response: PiAwareResponse =
                httpClient.get("http://$host/data/aircraft.json").body<PiAwareResponse>()
            response.aircraft
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAircraftTypes(host: String): Map<String, ICAOAircraftType> {
        return try {
            httpClient.get("http://$host/db/aircraft_types/icao_aircraft_types.json")
                .body<Map<String, ICAOAircraftType>>()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun getAircraftInfo(host: String, bkey: String): JsonObject? {
        return "http://$host/db/$bkey.json".let { key ->
            aircraftInfoMap[key] ?: try {
                println("Making request to $key")
                httpClient.get(
                    key
                ).body<JsonObject>().also { aircraftInfoMap[key] = it }
            } catch (e: Exception) {
                null
            }
        }
    }
}
