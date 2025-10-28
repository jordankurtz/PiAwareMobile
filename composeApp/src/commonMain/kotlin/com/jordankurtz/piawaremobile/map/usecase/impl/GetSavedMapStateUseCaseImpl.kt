package com.jordankurtz.piawaremobile.map.usecase.impl

import com.jordankurtz.piawaremobile.map.repo.MapStateRepository
import com.jordankurtz.piawaremobile.map.usecase.GetSavedMapStateUseCase
import com.jordankurtz.piawaremobile.model.MapState

class GetSavedMapStateUseCaseImpl(
    private val mapStateRepository: MapStateRepository
) : GetSavedMapStateUseCase {
    override suspend operator fun invoke(): MapState {
        return mapStateRepository.getSavedMapState()
    }
}
