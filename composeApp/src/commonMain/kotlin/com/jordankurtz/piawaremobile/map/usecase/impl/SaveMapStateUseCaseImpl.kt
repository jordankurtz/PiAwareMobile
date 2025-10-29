package com.jordankurtz.piawaremobile.map.usecase.impl

import com.jordankurtz.piawaremobile.map.repo.MapStateRepository
import com.jordankurtz.piawaremobile.map.usecase.SaveMapStateUseCase

class SaveMapStateUseCaseImpl(private val mapStateRepository: MapStateRepository) : SaveMapStateUseCase {
    override suspend operator fun invoke(scrollX: Double, scrollY: Double, zoom: Double) {
        mapStateRepository.saveMapState(scrollX, scrollY, zoom)
    }
}
