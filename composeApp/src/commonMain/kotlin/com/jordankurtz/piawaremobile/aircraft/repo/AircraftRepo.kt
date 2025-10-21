package com.jordankurtz.piawaremobile.aircraft.repo

import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftInfo

interface AircraftRepo {
    suspend fun getAircraft(servers: List<String>): List<Aircraft>
    suspend fun loadAircraftTypes(servers: List<String>)
    suspend fun findAircraftInfo(host: String, hex: String): AircraftInfo?
}
