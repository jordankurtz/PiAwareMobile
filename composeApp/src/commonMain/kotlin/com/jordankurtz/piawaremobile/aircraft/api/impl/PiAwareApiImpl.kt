package com.jordankurtz.piawaremobile.aircraft.api.impl

import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.aircraft.api.PiAwareApi
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.ICAOAircraftType
import com.jordankurtz.piawaremobile.model.PiAwareResponse
import com.jordankurtz.piawaremobile.model.Receiver
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.json.JsonObject
import org.koin.core.annotation.Single

@Single(binds = [PiAwareApi::class])
class PiAwareApiImpl(private val httpClient: HttpClient) : PiAwareApi {

    private val aircraftInfoMap = mutableMapOf<String, JsonObject>()

    override suspend fun getAircraft(host: String): List<Aircraft> {
        return try {
            val response: PiAwareResponse =
                httpClient.get("http://$host/data/aircraft.json").body<PiAwareResponse>()
            response.aircraft
        } catch (e: Exception) {
            Logger.e("Error fetching aircraft", e)
            emptyList()
        }
    }

    override suspend fun getAircraftTypes(host: String): Map<String, ICAOAircraftType> {
        return try {
            httpClient.get("http://$host/db/aircraft_types/icao_aircraft_types.json")
                .body<Map<String, ICAOAircraftType>>()
        } catch (e: Exception) {
            Logger.e("Error fetching aircraft types", e)
            emptyMap()
        }
    }

    override suspend fun getAircraftInfo(host: String, bkey: String): JsonObject? {
        return "http://$host/db/$bkey.json".let { key ->
            aircraftInfoMap[key] ?: try {
                Logger.d("Making request to $key")
                httpClient.get(
                    key
                ).body<JsonObject>().also { aircraftInfoMap[key] = it }
            } catch (e: Exception) {
                Logger.e("Error fetching aircraft info", e)
                null
            }
        }
    }

    override suspend fun getDump1090ReceiverInfo(host: String): Receiver? {
        return try {
            httpClient.get("http://$host/data/receiver.json")
                .body<Receiver>()
        } catch (e: Exception) {
            Logger.e("Failed to get dump1090 receiver info", e)
            null
        }
    }

    override suspend fun getDump978ReceiverInfo(host: String): Receiver? {
        return try {
            httpClient.get("http://$host/data-978/receiver.json")
                .body<Receiver>()
        } catch (e: Exception) {
            Logger.e("Failed to get dump978 receiver info", e)
            null
        }
    }

    override suspend fun getHistoryFile(host: String, index: Int): List<Aircraft>? {
        return try {
            val response: PiAwareResponse =
                httpClient.get("http://$host/data/history_$index.json").body<PiAwareResponse>()
            response.aircraft
        } catch (e: Exception) {
            Logger.e("Error fetching history file $index", e)
            null
        }
    }
}
