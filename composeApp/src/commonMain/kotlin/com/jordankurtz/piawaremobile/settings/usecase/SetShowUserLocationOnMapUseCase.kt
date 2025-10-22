package com.jordankurtz.piawaremobile.settings.usecase

import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import kotlinx.coroutines.flow.first

class SetShowUserLocationOnMapUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        val currentSettings = settingsRepository.getSettings().first()
        settingsRepository.saveSettings(
            currentSettings.copy(showUserLocationOnMap = enabled)
        )
    }
}
