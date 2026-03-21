package com.jordankurtz.piawaremobile.aircraft.repo

import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftPosition
import com.jordankurtz.piawaremobile.model.AircraftTrail
import com.jordankurtz.piawaremobile.model.PiAwareResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import kotlin.time.Clock

@Single(binds = [AircraftTrailManager::class])
class AircraftTrailManagerImpl : AircraftTrailManager {
    private val _aircraftTrails = MutableStateFlow<Map<String, AircraftTrail>>(emptyMap())
    override val aircraftTrails: StateFlow<Map<String, AircraftTrail>> = _aircraftTrails.asStateFlow()

    private val trailMutex = Mutex()
    private val trailPositions = mutableMapOf<String, MutableList<AircraftPosition>>()
    private var currentAircraftHex = setOf<String>()

    override suspend fun updateTrailsFromAircraft(aircraft: List<Aircraft>) {
        trailMutex.withLock {
            val timestamp = Clock.System.now().epochSeconds.toDouble()
            currentAircraftHex = aircraft.map { it.hex }.toSet()

            aircraft
                .filter { it.hasPosition }
                .forEach { plane ->
                    val positions = trailPositions.getOrPut(plane.hex) { mutableListOf() }
                    val newPosition =
                        AircraftPosition(
                            latitude = plane.lat,
                            longitude = plane.lon,
                            altitude = plane.altBaro,
                            timestamp = timestamp,
                        )
                    if (positions.lastOrNull()?.let {
                            it.latitude == newPosition.latitude && it.longitude == newPosition.longitude
                        } != true
                    ) {
                        positions.add(newPosition)
                    }
                }

            updateTrailsStateFlow()
        }
    }

    override suspend fun mergeHistoryResponses(responses: List<PiAwareResponse>) {
        trailMutex.withLock {
            responses.forEach { response ->
                val snapshotTime = response.now ?: return@forEach
                response.aircraft
                    .filter { it.hasPosition }
                    .forEach { aircraft ->
                        val positions = trailPositions.getOrPut(aircraft.hex) { mutableListOf() }
                        val positionAge = aircraft.seenPos ?: aircraft.seen ?: 0f
                        val newPosition =
                            AircraftPosition(
                                latitude = aircraft.lat,
                                longitude = aircraft.lon,
                                altitude = aircraft.altBaro,
                                timestamp = snapshotTime - positionAge,
                            )
                        positions.add(newPosition)
                    }
            }

            // Sort positions by timestamp and deduplicate
            trailPositions.forEach { (_, positions) ->
                val sorted = positions.sortedBy { it.timestamp }.distinctBy { it.timestamp }
                positions.clear()
                sorted.forEach { pos ->
                    val last = positions.lastOrNull()
                    if (last == null || last.latitude != pos.latitude || last.longitude != pos.longitude) {
                        positions.add(pos)
                    }
                }
            }

            updateTrailsStateFlow()
        }
    }

    override suspend fun clearTrails() {
        trailMutex.withLock {
            trailPositions.clear()
            currentAircraftHex = emptySet()
            _aircraftTrails.value = emptyMap()
        }
    }

    private fun updateTrailsStateFlow() {
        _aircraftTrails.value =
            trailPositions
                .filterKeys { currentAircraftHex.contains(it) }
                .mapValues { (hex, positions) ->
                    AircraftTrail(hex = hex, positions = positions.toList())
                }
    }
}
