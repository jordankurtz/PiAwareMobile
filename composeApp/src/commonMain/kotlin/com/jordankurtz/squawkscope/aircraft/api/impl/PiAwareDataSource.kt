package com.jordankurtz.squawkscope.aircraft.api.impl

import com.jordankurtz.squawkscope.aircraft.api.AircraftDataSource
import com.jordankurtz.squawkscope.aircraft.api.PiAwareApi
import com.jordankurtz.squawkscope.model.Aircraft
import com.jordankurtz.squawkscope.model.AircraftPosition
import com.jordankurtz.squawkscope.model.ICAOAircraftType
import com.jordankurtz.squawkscope.model.PiAwareResponse
import com.jordankurtz.squawkscope.model.Receiver
import com.jordankurtz.squawkscope.settings.Server
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import org.koin.core.annotation.Single

@Single
class PiAwareDataSource(
    private val piAwareApi: PiAwareApi,
) : AircraftDataSource {
    override suspend fun getAircraft(server: Server): List<Aircraft> = piAwareApi.getAircraft(server.address)

    override suspend fun getReceiverInfo(server: Server): Receiver? = piAwareApi.getDump1090ReceiverInfo(server.address)

    override suspend fun getDump978ReceiverInfo(server: Server): Receiver? =
        piAwareApi.getDump978ReceiverInfo(server.address)

    override suspend fun getAircraftTypes(server: Server): Map<String, ICAOAircraftType> =
        piAwareApi.getAircraftTypes(server.address)

    override suspend fun getAircraftInfo(
        server: Server,
        bkey: String,
    ): JsonObject? = piAwareApi.getAircraftInfo(server.address, bkey)

    override suspend fun fetchTrails(server: Server): Map<String, List<AircraftPosition>> {
        val receiver = piAwareApi.getDump1090ReceiverInfo(server.address) ?: return emptyMap()
        val historyCount = receiver.history ?: return emptyMap()
        if (historyCount <= 0) return emptyMap()

        return coroutineScope {
            (0 until historyCount)
                .map { index ->
                    async { fetchHistoryWithRetry(server.address, index) }
                }.awaitAll()
                .filterNotNull()
                .toTrails()
        }
    }

    private suspend fun fetchHistoryWithRetry(
        host: String,
        index: Int,
        maxRetries: Int = 3,
    ): PiAwareResponse? {
        repeat(maxRetries) { attempt ->
            val result = piAwareApi.getHistoryFile(host, index)
            if (result != null) return result
            if (attempt < maxRetries - 1) delay((attempt + 1) * 500L)
        }
        return null
    }

    private fun List<PiAwareResponse>.toTrails(): Map<String, List<AircraftPosition>> {
        val result = mutableMapOf<String, MutableList<AircraftPosition>>()
        forEach { response ->
            val snapshotTime = response.now ?: return@forEach
            response.aircraft
                .filter { it.hasPosition }
                .forEach { aircraft ->
                    val positionAge = aircraft.seenPos ?: aircraft.seen ?: 0f
                    result.getOrPut(aircraft.hex) { mutableListOf() }.add(
                        AircraftPosition(
                            latitude = aircraft.lat,
                            longitude = aircraft.lon,
                            altitude = aircraft.altBaro,
                            timestamp = snapshotTime - positionAge,
                        ),
                    )
                }
        }
        return result
    }
}
