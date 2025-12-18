package com.jordankurtz.piawaremobile.map.usecase.impl

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.map.repo.MapStateRepository
import com.jordankurtz.piawaremobile.map.usecase.GetSavedMapStateUseCase
import com.jordankurtz.piawaremobile.model.MapState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory(binds = [GetSavedMapStateUseCase::class])
class GetSavedMapStateUseCaseImpl(
    private val mapStateRepository: MapStateRepository,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : GetSavedMapStateUseCase {
    override suspend operator fun invoke(): MapState = withContext(ioDispatcher) {
        mapStateRepository.getSavedMapState()
    }
}
