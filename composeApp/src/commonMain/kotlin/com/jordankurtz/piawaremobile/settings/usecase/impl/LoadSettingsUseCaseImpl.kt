package com.jordankurtz.piawaremobile.settings.usecase.impl

import com.jordankurtz.piawaremobile.extensions.async
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class LoadSettingsUseCaseImpl(private val settingsRepository: SettingsRepository) : LoadSettingsUseCase {
    override operator fun invoke(): Flow<Async<Settings>> {
        return settingsRepository.getSettings().distinctUntilChanged().async()
    }
}
