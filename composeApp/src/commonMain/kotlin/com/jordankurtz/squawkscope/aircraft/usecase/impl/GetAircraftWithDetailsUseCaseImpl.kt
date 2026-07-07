package com.jordankurtz.squawkscope.aircraft.usecase.impl

import com.jordankurtz.squawkscope.aircraft.repo.AircraftRepo
import com.jordankurtz.squawkscope.aircraft.usecase.GetAircraftWithDetailsUseCase
import com.jordankurtz.squawkscope.di.annotations.IODispatcher
import com.jordankurtz.squawkscope.model.AircraftWithServers
import com.jordankurtz.squawkscope.settings.Server
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
