package com.jordankurtz.piawaremobile.map.usecase

import com.jordankurtz.piawaremobile.model.MapState

interface GetSavedMapStateUseCase {
    suspend operator fun invoke(): MapState
}
