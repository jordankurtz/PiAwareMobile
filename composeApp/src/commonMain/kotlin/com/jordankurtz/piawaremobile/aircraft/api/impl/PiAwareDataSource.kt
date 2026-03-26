package com.jordankurtz.piawaremobile.aircraft.api.impl

import com.jordankurtz.piawaremobile.aircraft.api.AircraftDataSource
import com.jordankurtz.piawaremobile.aircraft.api.PiAwareApi
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.ICAOAircraftType
import com.jordankurtz.piawaremobile.model.PiAwareResponse
import com.jordankurtz.piawaremobile.model.Receiver
import com.jordankurtz.piawaremobile.settings.Server
import kotlinx.serialization.json.JsonObject
import org.koin.core.annotation.Single

@Single
class PiAwareDataSource(
    private val piAwareApi: PiAwareApi,
) : AircraftDataSource {
    override suspend fun getAircraft(server: Server): List<Aircraft> {
        return piAwareApi.getAircraft(server.address)
    }

    override suspend fun getReceiverInfo(server: Server): Receiver? {
        return piAwareApi.getDump1090ReceiverInfo(server.address)
    }

    override suspend fun getAircraftTypes(server: Server): Map<String, ICAOAircraftType> {
        return piAwareApi.getAircraftTypes(server.address)
    }

    override suspend fun getAircraftInfo(
        server: Server,
        bkey: String,
    ): JsonObject? {
        return piAwareApi.getAircraftInfo(server.address, bkey)
    }

    override suspend fun getHistory(
        server: Server,
        index: Int,
    ): PiAwareResponse? {
        return piAwareApi.getHistoryFile(server.address, index)
    }
}
