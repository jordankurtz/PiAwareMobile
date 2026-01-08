package com.jordankurtz.piawaremobile.settings.usecase.impl

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.SetShowAircraftPathsUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory(binds = [SetShowAircraftPathsUseCase::class])
class SetShowAircraftPathsUseCaseImpl(
    private val settingsRepository: SettingsRepository,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher
) : SetShowAircraftPathsUseCase {
    override suspend operator fun invoke(enabled: Boolean) {
        withContext(ioDispatcher) {
            val currentSettings = settingsRepository.getSettings().first()
            settingsRepository.saveSettings(
                currentSettings.copy(showAircraftPaths = enabled)
            )
        }
    }
}
