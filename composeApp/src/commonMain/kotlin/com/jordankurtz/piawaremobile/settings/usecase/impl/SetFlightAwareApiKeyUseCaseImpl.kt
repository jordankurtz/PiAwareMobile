package com.jordankurtz.piawaremobile.settings.usecase.impl

import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.SetFlightAwareApiKeyUseCase
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory(binds = [SetFlightAwareApiKeyUseCase::class])
class SetFlightAwareApiKeyUseCaseImpl(
    private val settingsRepository: SettingsRepository
) : SetFlightAwareApiKeyUseCase {
    override suspend operator fun invoke(apiKey: String) {
        val settings = settingsRepository.getSettings().first()
        settingsRepository.saveSettings(settings.copy(flightAwareApiKey = apiKey))
    }
}
