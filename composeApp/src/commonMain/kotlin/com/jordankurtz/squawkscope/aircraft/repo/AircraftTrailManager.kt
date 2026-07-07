package com.jordankurtz.squawkscope.aircraft.repo

import com.jordankurtz.squawkscope.model.Aircraft
import com.jordankurtz.squawkscope.model.AircraftPosition
import com.jordankurtz.squawkscope.model.AircraftTrail
import kotlinx.coroutines.flow.StateFlow

interface AircraftTrailManager {
    val aircraftTrails: StateFlow<Map<String, AircraftTrail>>

    suspend fun updateTrailsFromAircraft(aircraft: List<Aircraft>)

    suspend fun mergeTrails(trails: Map<String, List<AircraftPosition>>)

    suspend fun clearTrails()
}
