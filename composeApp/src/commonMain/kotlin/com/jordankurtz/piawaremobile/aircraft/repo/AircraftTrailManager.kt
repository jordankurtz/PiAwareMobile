package com.jordankurtz.piawaremobile.aircraft.repo

import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftPosition
import com.jordankurtz.piawaremobile.model.AircraftTrail
import kotlinx.coroutines.flow.StateFlow

interface AircraftTrailManager {
    val aircraftTrails: StateFlow<Map<String, AircraftTrail>>

    suspend fun updateTrailsFromAircraft(aircraft: List<Aircraft>)

    suspend fun mergeTrails(trails: Map<String, List<AircraftPosition>>)

    suspend fun clearTrails()
}
