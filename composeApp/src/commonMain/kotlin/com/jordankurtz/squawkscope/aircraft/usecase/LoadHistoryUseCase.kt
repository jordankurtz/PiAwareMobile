package com.jordankurtz.squawkscope.aircraft.usecase

import com.jordankurtz.squawkscope.settings.Server

interface LoadHistoryUseCase {
    suspend operator fun invoke(servers: List<Server>)
}
