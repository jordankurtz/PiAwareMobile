package com.jordankurtz.piawaremobile.aircraft.usecase

interface LoadAircraftTypesUseCase {
    suspend operator fun invoke(servers: List<String>)
}
