package com.jordankurtz.piawaremobile.map.usecase

import com.jordankurtz.piawaremobile.map.repo.MapStateRepository

class SaveMapStateUseCase(private val mapStateRepository: MapStateRepository) {
    suspend operator fun invoke(scrollX: Double, scrollY: Double, zoom: Double) {
        mapStateRepository.saveMapState(scrollX, scrollY, zoom)
    }
}
