package com.jordankurtz.piawaremobile.settings.usecase.impl

import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.SetCenterMapOnUserOnStartUseCase
import kotlinx.coroutines.flow.first

class SetCenterMapOnUserOnStartUseCaseImpl(
    private val settingsRepository: SettingsRepository
) : SetCenterMapOnUserOnStartUseCase {
    override suspend operator fun invoke(enabled: Boolean) {
        val currentSettings = settingsRepository.getSettings().first()
        settingsRepository.saveSettings(
            currentSettings.copy(centerMapOnUserOnStart = enabled)
        )
    }
}
