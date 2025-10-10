package com.jordankurtz.piawaremobile.aircraft.usecase

import com.jordankurtz.piawaremobile.aircraft.repo.AircraftRepo

/**
 * A use case dedicated to loading and caching the aircraft type definitions
 * from the servers. This should be called once before fetching aircraft details
 * to ensure the type information is available in the repository's cache.
 */
class LoadAircraftTypesUseCase(
    private val aircraftRepo: AircraftRepo
) {
    /**
     * Executes the use case.
     *
     * @param servers A list of server URLs from which to load the type definitions.
     */
    suspend operator fun invoke(servers: List<String>) {
        aircraftRepo.loadAircraftTypes(servers)
    }
}
