package com.jordankurtz.piawaremobile.settings.usecase.impl

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.SetRestoreMapStateOnStartUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory(binds = [SetRestoreMapStateOnStartUseCase::class])
class SetRestoreMapStateOnStartUseCaseImpl(
    private val settingsRepository: SettingsRepository,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher
) : SetRestoreMapStateOnStartUseCase {
    override suspend operator fun invoke(enabled: Boolean) {
        withContext(ioDispatcher) {
            val currentSettings = settingsRepository.getSettings().first()
            settingsRepository.saveSettings(
                currentSettings.copy(restoreMapStateOnStart = enabled)
            )
        }
    }
}
