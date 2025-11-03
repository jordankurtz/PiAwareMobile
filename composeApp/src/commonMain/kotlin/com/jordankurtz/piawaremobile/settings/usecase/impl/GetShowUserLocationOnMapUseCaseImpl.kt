package com.jordankurtz.piawaremobile.settings.usecase.impl

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.GetShowUserLocationOnMapUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Factory

@Factory(binds = [GetShowUserLocationOnMapUseCase::class])
class GetShowUserLocationOnMapUseCaseImpl(
    private val settingsRepository: SettingsRepository,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher
) : GetShowUserLocationOnMapUseCase {
    override fun invoke(): Flow<Boolean> {
        return settingsRepository.getSettings().map { it.showUserLocationOnMap }
    }
}
