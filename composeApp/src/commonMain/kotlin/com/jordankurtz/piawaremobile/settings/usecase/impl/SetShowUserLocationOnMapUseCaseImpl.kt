package com.jordankurtz.piawaremobile.settings.usecase.impl

import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.SetShowUserLocationOnMapUseCase
import kotlinx.coroutines.flow.first

class SetShowUserLocationOnMapUseCaseImpl(
    private val settingsRepository: SettingsRepository
) : SetShowUserLocationOnMapUseCase {
    override suspend operator fun invoke(enabled: Boolean) {
        val currentSettings = settingsRepository.getSettings().first()
        settingsRepository.saveSettings(
            currentSettings.copy(showUserLocationOnMap = enabled)
        )
    }
}
