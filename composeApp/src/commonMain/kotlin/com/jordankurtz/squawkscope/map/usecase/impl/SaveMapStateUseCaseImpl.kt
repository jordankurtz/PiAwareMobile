package com.jordankurtz.squawkscope.map.usecase.impl

import com.jordankurtz.squawkscope.di.annotations.IODispatcher
import com.jordankurtz.squawkscope.map.repo.MapStateRepository
import com.jordankurtz.squawkscope.map.usecase.SaveMapStateUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory(binds = [SaveMapStateUseCase::class])
class SaveMapStateUseCaseImpl(
    private val mapStateRepository: MapStateRepository,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : SaveMapStateUseCase {
    override suspend operator fun invoke(
        scrollX: Double,
        scrollY: Double,
        zoom: Double,
    ) = withContext(ioDispatcher) {
        mapStateRepository.saveMapState(
            scrollX = scrollX,
            scrollY = scrollY,
            zoom = zoom,
        )
    }
}
