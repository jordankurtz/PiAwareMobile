package com.jordankurtz.squawkscope.aircraft.api

import com.jordankurtz.squawkscope.model.Aircraft
import com.jordankurtz.squawkscope.model.ICAOAircraftType
import com.jordankurtz.squawkscope.model.PiAwareResponse
import com.jordankurtz.squawkscope.model.Receiver
import kotlinx.serialization.json.JsonObject

interface PiAwareApi {
    suspend fun getAircraft(host: String): List<Aircraft>

    suspend fun getAircraftTypes(host: String): Map<String, ICAOAircraftType>

    suspend fun getAircraftInfo(
        host: String,
        bkey: String,
    ): JsonObject?

    suspend fun getDump1090ReceiverInfo(host: String): Receiver?

    suspend fun getDump978ReceiverInfo(host: String): Receiver?

    suspend fun getHistoryFile(
        host: String,
        index: Int,
    ): PiAwareResponse?
}
