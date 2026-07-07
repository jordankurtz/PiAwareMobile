package com.jordankurtz.squawkscope.map.usecase.impl

import com.jordankurtz.squawkscope.di.annotations.IODispatcher
import com.jordankurtz.squawkscope.map.repo.MapStateRepository
import com.jordankurtz.squawkscope.map.usecase.GetSavedMapStateUseCase
import com.jordankurtz.squawkscope.model.MapState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory(binds = [GetSavedMapStateUseCase::class])
class GetSavedMapStateUseCaseImpl(
    private val mapStateRepository: MapStateRepository,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : GetSavedMapStateUseCase {
    override suspend operator fun invoke(): MapState =
        withContext(ioDispatcher) {
            mapStateRepository.getSavedMapState()
        }
}
