package com.jordankurtz.piawaremobile.aircraft.api.impl

import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.aircraft.api.AircraftDataSource
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftPosition
import com.jordankurtz.piawaremobile.model.ICAOAircraftType
import com.jordankurtz.piawaremobile.model.PiAwareResponse
import com.jordankurtz.piawaremobile.model.ReadsbTraceResponse
import com.jordankurtz.piawaremobile.model.Receiver
import com.jordankurtz.piawaremobile.settings.Server
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonObject
import org.koin.core.annotation.Single

@Single
class ReadsbDataSource(
    private val httpClient: HttpClient,
) : AircraftDataSource {
    override suspend fun getAircraft(server: Server): List<Aircraft> {
        return try {
            val response: PiAwareResponse =
                httpClient.get("http://${server.address}/data/aircraft.json").body()
            response.aircraft
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("Error fetching aircraft from readsb server ${server.address}", e)
            emptyList()
        }
    }

    override suspend fun getReceiverInfo(server: Server): Receiver? {
        return try {
            httpClient.get("http://${server.address}/data/receiver.json").body()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("Error fetching receiver info from readsb server ${server.address}", e)
            null
        }
    }

    override suspend fun getDump978ReceiverInfo(server: Server): Receiver? {
        // readsb does not support dump978 UAT receivers
        return null
    }

    override suspend fun getAircraftTypes(server: Server): Map<String, ICAOAircraftType> {
        return try {
            httpClient.get("http://${server.address}/db/aircraft_types/icao_aircraft_types.json").body()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("Error fetching aircraft types from readsb server ${server.address}", e)
            emptyMap()
        }
    }

    override suspend fun getAircraftInfo(
        server: Server,
        bkey: String,
    ): JsonObject? {
        return try {
            httpClient.get("http://${server.address}/db/$bkey.json").body()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("Error fetching aircraft info from readsb server ${server.address}", e)
            null
        }
    }

    override suspend fun fetchTrails(server: Server): Map<String, List<AircraftPosition>> {
        val aircraft =
            try {
                httpClient.get("http://${server.address}/data/aircraft.json")
                    .body<PiAwareResponse>().aircraft
                    .filter { it.hasPosition }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e("Error fetching aircraft from readsb server ${server.address}", e)
                return emptyMap()
            }

        return coroutineScope {
            aircraft.map { plane ->
                async { fetchTrace(server, plane.hex) }
            }.awaitAll()
                .filterNotNull()
                .toTrails()
        }
    }

    private suspend fun fetchTrace(
        server: Server,
        hex: String,
    ): ReadsbTraceResponse? {
        return try {
            httpClient.get("http://${server.address}/data/traces/${hex.take(2)}/$hex.json").body()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e("Error fetching trace for $hex from readsb server ${server.address}", e)
            null
        }
    }

    private fun List<ReadsbTraceResponse>.toTrails(): Map<String, List<AircraftPosition>> {
        val result = mutableMapOf<String, MutableList<AircraftPosition>>()
        forEach { response ->
            val positions =
                response.trace.map { entry ->
                    AircraftPosition(
                        latitude = entry.latitude,
                        longitude = entry.longitude,
                        altitude = entry.altitude,
                        timestamp = response.timestamp + entry.timeOffset,
                    )
                }
            if (positions.isNotEmpty()) {
                result[response.icao] = positions.toMutableList()
            }
        }
        return result
    }
}
