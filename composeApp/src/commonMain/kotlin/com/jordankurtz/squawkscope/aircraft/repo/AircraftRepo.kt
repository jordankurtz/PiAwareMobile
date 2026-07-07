package com.jordankurtz.squawkscope.aircraft.repo

import com.jordankurtz.squawkscope.model.Aircraft
import com.jordankurtz.squawkscope.model.AircraftInfo
import com.jordankurtz.squawkscope.model.Async
import com.jordankurtz.squawkscope.model.FlightResponse
import com.jordankurtz.squawkscope.model.Receiver
import com.jordankurtz.squawkscope.model.ReceiverType
import com.jordankurtz.squawkscope.settings.Server

interface AircraftRepo {
    suspend fun getAircraftWithServers(servers: List<Server>): Map<Aircraft, Set<Server>>

    suspend fun loadAircraftTypes(servers: List<Server>)

    suspend fun findAircraftInfo(
        server: Server,
        hex: String,
    ): AircraftInfo?

    suspend fun getReceiverInfo(
        server: Server,
        receiverType: ReceiverType,
    ): Receiver?

    suspend fun lookupFlight(ident: String): Async<FlightResponse>

    suspend fun fetchAndMergeHistory(server: Server)
}
