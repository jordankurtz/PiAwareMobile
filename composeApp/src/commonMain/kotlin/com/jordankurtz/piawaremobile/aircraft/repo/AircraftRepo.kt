package com.jordankurtz.piawaremobile.aircraft.repo

import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftInfo
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.FlightResponse
import com.jordankurtz.piawaremobile.model.Receiver
import com.jordankurtz.piawaremobile.model.ReceiverType
import com.jordankurtz.piawaremobile.settings.Server

interface AircraftRepo {
    suspend fun getAircraftWithServers(servers: List<Server>): Map<Aircraft, Set<Server>>

    suspend fun loadAircraftTypes(servers: List<Server>)

    suspend fun findAircraftInfo(
        host: String,
        hex: String,
    ): AircraftInfo?

    suspend fun getReceiverInfo(
        host: String,
        receiverType: ReceiverType,
    ): Receiver?

    suspend fun lookupFlight(ident: String): Async<FlightResponse>

    suspend fun fetchAndMergeHistory(host: String)
}
