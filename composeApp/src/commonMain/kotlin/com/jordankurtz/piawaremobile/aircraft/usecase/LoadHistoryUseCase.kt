package com.jordankurtz.piawaremobile.aircraft.usecase

import com.jordankurtz.piawaremobile.settings.Server

interface LoadHistoryUseCase {
    suspend operator fun invoke(servers: List<Server>)
}
