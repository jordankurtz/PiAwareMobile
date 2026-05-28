package com.jordankurtz.piawaremobile.map.usecase.impl

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.map.repo.MapStateRepository
import com.jordankurtz.piawaremobile.map.usecase.SaveMapStateUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory(binds = [SaveMapStateUseCase::class])
class SaveMapStateUseCaseImpl(
    private val mapStateRepository: MapStateRepository,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : SaveMapStateUseCase {
    override suspend operator fun invoke(
        latitude: Double,
        longitude: Double,
        zoom: Double,
    ) = withContext(ioDispatcher) {
        mapStateRepository.saveMapState(
            latitude = latitude,
            longitude = longitude,
            zoom = zoom,
        )
    }
}
