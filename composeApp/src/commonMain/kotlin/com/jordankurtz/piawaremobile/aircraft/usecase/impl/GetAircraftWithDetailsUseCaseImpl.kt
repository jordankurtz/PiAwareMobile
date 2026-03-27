package com.jordankurtz.piawaremobile.aircraft.usecase.impl

import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepo
import com.jordankurtz.piawaremobile.aircraft.usecase.GetAircraftWithDetailsUseCase
import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.model.AircraftWithServers
import com.jordankurtz.piawaremobile.settings.Server
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
        servers: List<Server>,
        infoServer: Server,
    ): List<AircraftWithServers> =
        withContext(ioDispatcher) {
            val aircraftWithServers = aircraftRepo.getAircraftWithServers(servers)

            aircraftWithServers.map { (aircraft, serverSet) ->
                async {
                    val aircraftInfo =
                        aircraftRepo.findAircraftInfo(
                            server = infoServer,
                            hex = aircraft.hex,
                        )
                    AircraftWithServers(
                        aircraft = aircraft,
                        info = aircraftInfo,
                        servers = serverSet,
                    )
                }
            }.awaitAll()
        }
}
