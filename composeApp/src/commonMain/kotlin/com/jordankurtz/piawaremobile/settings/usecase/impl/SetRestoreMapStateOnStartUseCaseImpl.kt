package com.jordankurtz.piawaremobile.settings.usecase.impl

import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.SetRestoreMapStateOnStartUseCase
import kotlinx.coroutines.flow.first

class SetRestoreMapStateOnStartUseCaseImpl(
    private val settingsRepository: SettingsRepository
) : SetRestoreMapStateOnStartUseCase {
    override suspend operator fun invoke(enabled: Boolean) {
        val currentSettings = settingsRepository.getSettings().first()
        settingsRepository.saveSettings(
            currentSettings.copy(restoreMapStateOnStart = enabled)
        )
    }
}
