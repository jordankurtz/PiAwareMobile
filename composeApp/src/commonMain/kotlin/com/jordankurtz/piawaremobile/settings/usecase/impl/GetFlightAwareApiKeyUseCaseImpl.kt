package com.jordankurtz.piawaremobile.settings.usecase.impl

import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.usecase.GetFlightAwareApiKeyUseCase
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Factory

@Factory(binds = [GetFlightAwareApiKeyUseCase::class])
class GetFlightAwareApiKeyUseCaseImpl(
    private val settingsRepository: SettingsRepository
) : GetFlightAwareApiKeyUseCase {
    override suspend operator fun invoke(): String {
        return settingsRepository.getSettings().first().flightAwareApiKey
    }
}
