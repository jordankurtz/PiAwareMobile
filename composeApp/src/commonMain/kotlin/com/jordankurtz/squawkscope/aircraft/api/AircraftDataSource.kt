package com.jordankurtz.squawkscope.aircraft.api

import com.jordankurtz.squawkscope.model.Aircraft
import com.jordankurtz.squawkscope.model.AircraftPosition
import com.jordankurtz.squawkscope.model.ICAOAircraftType
import com.jordankurtz.squawkscope.model.Receiver
import com.jordankurtz.squawkscope.settings.Server
import kotlinx.serialization.json.JsonObject

interface AircraftDataSource {
    suspend fun getAircraft(server: Server): List<Aircraft>

    suspend fun getReceiverInfo(server: Server): Receiver?

    suspend fun getDump978ReceiverInfo(server: Server): Receiver?

    suspend fun getAircraftTypes(server: Server): Map<String, ICAOAircraftType>

    suspend fun getAircraftInfo(
        server: Server,
        bkey: String,
    ): JsonObject?

    suspend fun fetchTrails(server: Server): Map<String, List<AircraftPosition>>
}
