package com.jordankurtz.piawaremobile.aircraft.repo

import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftInfo
import com.jordankurtz.piawaremobile.model.AircraftTrail
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.FlightResponse
import com.jordankurtz.piawaremobile.model.Receiver
import com.jordankurtz.piawaremobile.model.ReceiverType
import kotlinx.coroutines.flow.StateFlow

interface AircraftRepo {
    suspend fun getAircraft(servers: List<String>): List<Aircraft>
    suspend fun loadAircraftTypes(servers: List<String>)
    suspend fun findAircraftInfo(host: String, hex: String): AircraftInfo?
    suspend fun getReceiverInfo(host: String, receiverType: ReceiverType): Receiver?
    suspend fun lookupFlight(ident: String): Async<FlightResponse>

    val aircraftTrails: StateFlow<Map<String, AircraftTrail>>
    suspend fun fetchAndMergeHistory(host: String)
    fun clearTrails()
}
