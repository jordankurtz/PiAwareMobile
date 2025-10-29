package com.jordankurtz.piawaremobile.settings.usecase.impl

import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.SetOpenUrlsExternallyUseCase
import kotlinx.coroutines.flow.first

class SetOpenUrlsExternallyUseCaseImpl(
    private val settingsRepository: SettingsRepository
) : SetOpenUrlsExternallyUseCase {
    override suspend operator fun invoke(enabled: Boolean) {
        val currentSettings = settingsRepository.getSettings().first()
        settingsRepository.saveSettings(
            currentSettings.copy(openUrlsExternally = enabled)
        )
    }
}
