package com.jordankurtz.piawaremobile.settings.usecase.impl

import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.SetShowMinimapTrailsUseCase
import org.koin.core.annotation.Factory

@Factory
class SetShowMinimapTrailsUseCaseImpl(
    private val settingsRepository: SettingsRepository
) : SetShowMinimapTrailsUseCase {
    override suspend fun invoke(showMinimapTrails: Boolean) {
        settingsRepository.setShowMinimapTrails(showMinimapTrails)
    }
}
