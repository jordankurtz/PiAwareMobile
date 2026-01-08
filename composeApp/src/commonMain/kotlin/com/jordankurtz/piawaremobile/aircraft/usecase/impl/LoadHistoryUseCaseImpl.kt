package com.jordankurtz.piawaremobile.aircraft.usecase.impl

import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepo
import com.jordankurtz.piawaremobile.aircraft.usecase.LoadHistoryUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.koin.core.annotation.Single

@Single(binds = [LoadHistoryUseCase::class])
class LoadHistoryUseCaseImpl(
    private val aircraftRepo: AircraftRepo
) : LoadHistoryUseCase {

    private var hasBeenExecuted = false

    override suspend fun invoke(servers: List<String>) {
        if (hasBeenExecuted) return
        hasBeenExecuted = true

        if (servers.isEmpty()) return

        coroutineScope {
            servers.map { server ->
                async {
                    try {
                        aircraftRepo.fetchAndMergeHistory(server)
                    } catch (e: Exception) {
                        Logger.e("Failed to fetch history from server $server", e)
                    }
                }
            }.awaitAll()
        }
    }
}
