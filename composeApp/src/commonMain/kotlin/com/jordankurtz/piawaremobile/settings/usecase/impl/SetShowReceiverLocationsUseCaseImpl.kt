package com.jordankurtz.piawaremobile.settings.usecase.impl

import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.SetShowReceiverLocationsUseCase
import kotlinx.coroutines.flow.first

class SetShowReceiverLocationsUseCaseImpl(
    private val settingsRepository: SettingsRepository
) : SetShowReceiverLocationsUseCase {
    override suspend operator fun invoke(enabled: Boolean) {
        val currentSettings = settingsRepository.getSettings().first()
        settingsRepository.saveSettings(
            currentSettings.copy(showReceiverLocations = enabled)
        )
    }
}
