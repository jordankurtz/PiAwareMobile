package com.jordankurtz.piawaremobile.aircraft.usecase.impl

import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepo
import com.jordankurtz.piawaremobile.aircraft.usecase.GetAircraftWithDetailsUseCase
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class GetAircraftWithDetailsUseCaseImpl(
    private val aircraftRepo: AircraftRepo
) : GetAircraftWithDetailsUseCase {

    override suspend operator fun invoke(servers: List<String>, infoHost: String): List<Pair<Aircraft, AircraftInfo?>> {
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
