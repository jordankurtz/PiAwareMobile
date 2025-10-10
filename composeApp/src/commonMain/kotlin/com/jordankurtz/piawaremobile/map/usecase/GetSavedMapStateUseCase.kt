package com.jordankurtz.piawaremobile.map.usecase

import com.jordankurtz.piawaremobile.map.repo.MapStateRepository
import com.jordankurtz.piawaremobile.model.MapState

class GetSavedMapStateUseCase(
    private val mapStateRepository: MapStateRepository
) {
    suspend operator fun invoke(): MapState {
        return mapStateRepository.getSavedMapState()
    }
}
