package com.jordankurtz.piawaremobile.map.usecase.impl

import com.jordankurtz.piawaremobile.map.repo.MapStateRepository
import com.jordankurtz.piawaremobile.map.usecase.SaveMapStateUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [SaveMapStateUseCase::class])
class SaveMapStateUseCaseImpl(private val mapStateRepository: MapStateRepository) : SaveMapStateUseCase {
    override suspend operator fun invoke(scrollX: Double, scrollY: Double, zoom: Double) {
        mapStateRepository.saveMapState(scrollX, scrollY, zoom)
    }
}
