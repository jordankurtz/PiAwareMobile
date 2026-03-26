package com.jordankurtz.piawaremobile.aircraft.api

import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.ICAOAircraftType
import com.jordankurtz.piawaremobile.model.PiAwareResponse
import com.jordankurtz.piawaremobile.model.Receiver
import com.jordankurtz.piawaremobile.settings.Server
import kotlinx.serialization.json.JsonObject

interface AircraftDataSource {
    suspend fun getAircraft(server: Server): List<Aircraft>

    suspend fun getReceiverInfo(server: Server): Receiver?

    suspend fun getAircraftTypes(server: Server): Map<String, ICAOAircraftType>

    suspend fun getAircraftInfo(
        server: Server,
        bkey: String,
    ): JsonObject?

    suspend fun getHistory(
        server: Server,
        index: Int,
    ): PiAwareResponse?
}
