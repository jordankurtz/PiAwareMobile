package com.jordankurtz.piawaremobile.aircraft.usecase.impl

import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepo
import com.jordankurtz.piawaremobile.aircraft.usecase.GetAircraftWithDetailsUseCase
import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory(binds = [GetAircraftWithDetailsUseCase::class])
class GetAircraftWithDetailsUseCaseImpl(
    private val aircraftRepo: AircraftRepo,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : GetAircraftWithDetailsUseCase {

    override suspend operator fun invoke(
        servers: List<String>,
        infoHost: String,
    ): List<Pair<Aircraft, AircraftInfo?>> = withContext(ioDispatcher) {
        val allAircraft = aircraftRepo.getAircraft(servers)

        allAircraft.map { aircraft ->
            async {
                val aircraftInfo = aircraftRepo.findAircraftInfo(
                    host = infoHost,
                    hex = aircraft.hex,
                )
                Pair(
                    first = aircraft,
                    second = aircraftInfo,
                )
            }
        }.awaitAll()
    }
}
