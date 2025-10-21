package com.jordankurtz.piawaremobile.settings.usecase

import com.jordankurtz.piawaremobile.extensions.async
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class LoadSettingsUseCase(private val settingsRepository: SettingsRepository) {
    operator fun invoke(): Flow<Async<Settings>> {
        return settingsRepository.getSettings().distinctUntilChanged().async()
    }
}
