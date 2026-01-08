package com.jordankurtz.piawaremobile.aircraft.api

import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.ICAOAircraftType
import com.jordankurtz.piawaremobile.model.Receiver
import kotlinx.serialization.json.JsonObject

interface PiAwareApi {
    suspend fun getAircraft(host: String): List<Aircraft>
    suspend fun getAircraftTypes(host: String): Map<String, ICAOAircraftType>
    suspend fun getAircraftInfo(host: String, bkey: String): JsonObject?
    suspend fun getDump1090ReceiverInfo(host: String): Receiver?
    suspend fun getDump978ReceiverInfo(host: String): Receiver?
    suspend fun getHistoryFile(host: String, index: Int): List<Aircraft>?
}
