package com.jordankurtz.piawaremobile.aircraft.api.impl

import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.aircraft.api.AircraftDataSource
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.ICAOAircraftType
import com.jordankurtz.piawaremobile.model.PiAwareResponse
import com.jordankurtz.piawaremobile.model.Receiver
import com.jordankurtz.piawaremobile.settings.Server
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import org.koin.core.annotation.Single

@Single
class ReadsbDataSource(
    private val httpClient: HttpClient,
) : AircraftDataSource {
    override val supportsHistory: Boolean = false

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

    override suspend fun getHistory(
        server: Server,
        index: Int,
    ): PiAwareResponse? {
        // readsb uses a trace API (/data/traces/) instead of history files.
        // Trace API support will be added in a follow-up issue.
        Logger.d("History files not supported for readsb server ${server.address}, use trace API instead")
        return null
    }
}
