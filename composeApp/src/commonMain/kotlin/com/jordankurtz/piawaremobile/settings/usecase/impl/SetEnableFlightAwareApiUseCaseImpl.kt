package com.jordankurtz.piawaremobile.settings.usecase.impl

import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.SetEnableFlightAwareApiUseCase
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory(binds = [SetEnableFlightAwareApiUseCase::class])
class SetEnableFlightAwareApiUseCaseImpl(
    private val settingsRepository: SettingsRepository
) : SetEnableFlightAwareApiUseCase {
    override suspend operator fun invoke(enabled: Boolean) {
        val settings = settingsRepository.getSettings().first()
        settingsRepository.saveSettings(settings.copy(enableFlightAwareApi = enabled))
    }
}
