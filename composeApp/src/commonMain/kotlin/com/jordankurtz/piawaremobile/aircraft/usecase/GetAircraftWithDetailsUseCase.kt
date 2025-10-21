package com.jordankurtz.piawaremobile.aircraft.usecase

import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepo
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * A use case that fetches the list of aircraft from multiple servers and
 * then enriches each aircraft with its detailed information.
 */
class GetAircraftWithDetailsUseCase(
    private val aircraftRepo: AircraftRepo
) {
    /**
     * Executes the use case.
     *
     * @param servers A list of server URLs to fetch aircraft from.
     * @param infoHost The primary server URL used to look up detailed aircraft info.
     *                 Often, one server is designated for these lookups.
     * @return A list of pairs, where each pair contains an [Aircraft] and its corresponding [AircraftInfo].
     */
    suspend operator fun invoke(servers: List<String>, infoHost: String): List<Pair<Aircraft, AircraftInfo?>> {
        val allAircraft = aircraftRepo.getAircraft(servers)

        return coroutineScope {
            allAircraft.map { aircraft ->
                async {
                    val aircraftInfo = aircraftRepo.findAircraftInfo(infoHost, aircraft.hex)
                    Pair(aircraft, aircraftInfo)
                }
            }.awaitAll()
        }
    }
}
