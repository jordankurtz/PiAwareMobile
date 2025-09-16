package com.jordankurtz.piawareviewer.settings.usecase

import com.jordankurtz.piawareviewer.extensions.async
import com.jordankurtz.piawareviewer.model.Async
import com.jordankurtz.piawareviewer.settings.Settings
import com.jordankurtz.piawareviewer.settings.repo.SettingsRepository
import kotlinx.coroutines.flow.Flow

class LoadSettingsUseCase(private val settingsRepository: SettingsRepository) {
    operator fun invoke(): Flow<Async<Settings>> {
        return settingsRepository.getSettings().async()
    }
}