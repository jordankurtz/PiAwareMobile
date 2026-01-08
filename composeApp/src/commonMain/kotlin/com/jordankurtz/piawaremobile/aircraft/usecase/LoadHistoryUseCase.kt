package com.jordankurtz.piawaremobile.aircraft.usecase

interface LoadHistoryUseCase {
    suspend operator fun invoke(servers: List<String>)
}
