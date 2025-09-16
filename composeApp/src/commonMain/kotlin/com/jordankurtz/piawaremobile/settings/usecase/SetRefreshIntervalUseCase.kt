package com.jordankurtz.piawaremobile.settings.usecase

import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import kotlinx.coroutines.flow.first

class SetRefreshIntervalUseCase(private val settingsRepository: SettingsRepository) {
    suspend operator fun invoke(newRefreshInterval: Int) {
        val settings = settingsRepository.getSettings().first()
        settingsRepository.saveSettings(
            settings.copy(
                refreshInterval = newRefreshInterval
            )
        )
    }
}