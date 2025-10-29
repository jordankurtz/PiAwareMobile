package com.jordankurtz.piawaremobile.settings.usecase.impl

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.SetRefreshIntervalUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory(binds = [SetRefreshIntervalUseCase::class])
class SetRefreshIntervalUseCaseImpl(
    private val settingsRepository: SettingsRepository,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher
) : SetRefreshIntervalUseCase {
    override suspend operator fun invoke(newRefreshInterval: Int) {
        withContext(ioDispatcher) {
            val settings = settingsRepository.getSettings().first()
            settingsRepository.saveSettings(
                settings.copy(
                    refreshInterval = newRefreshInterval
                )
            )
        }
    }
}
