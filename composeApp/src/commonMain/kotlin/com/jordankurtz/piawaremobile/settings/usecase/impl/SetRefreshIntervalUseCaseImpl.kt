package com.jordankurtz.piawaremobile.settings.usecase.impl

import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.SetRefreshIntervalUseCase
import kotlinx.coroutines.flow.first

class SetRefreshIntervalUseCaseImpl(private val settingsRepository: SettingsRepository) : SetRefreshIntervalUseCase {
    override suspend operator fun invoke(newRefreshInterval: Int) {
        val settings = settingsRepository.getSettings().first()
        settingsRepository.saveSettings(
            settings.copy(
                refreshInterval = newRefreshInterval
            )
        )
    }
}
