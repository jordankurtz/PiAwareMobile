package com.jordankurtz.piawaremobile.aircraft.repo

import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftTrail
import com.jordankurtz.piawaremobile.model.PiAwareResponse
import kotlinx.coroutines.flow.StateFlow

interface AircraftTrailManager {
    val aircraftTrails: StateFlow<Map<String, AircraftTrail>>

    suspend fun updateTrailsFromAircraft(aircraft: List<Aircraft>)

    suspend fun mergeHistoryResponses(responses: List<PiAwareResponse>)

    suspend fun clearTrails()
}
