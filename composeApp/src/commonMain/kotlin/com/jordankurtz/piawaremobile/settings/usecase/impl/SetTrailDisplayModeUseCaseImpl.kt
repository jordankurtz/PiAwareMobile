package com.jordankurtz.piawaremobile.settings.usecase.impl

import com.jordankurtz.piawaremobile.settings.TrailDisplayMode
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.SetTrailDisplayModeUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [SetTrailDisplayModeUseCase::class])
class SetTrailDisplayModeUseCaseImpl(
    private val settingsRepository: SettingsRepository
) : SetTrailDisplayModeUseCase {
    override suspend fun invoke(trailDisplayMode: TrailDisplayMode) {
        settingsRepository.setTrailDisplayMode(trailDisplayMode)
    }
}
