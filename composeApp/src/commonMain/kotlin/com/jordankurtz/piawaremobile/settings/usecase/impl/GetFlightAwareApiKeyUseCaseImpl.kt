package com.jordankurtz.piawaremobile.settings.usecase.impl

import com.jordankurtz.piawaremobile.settings.usecase.GetFlightAwareApiKeyUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SettingsService
import org.koin.core.annotation.Factory

@Factory(binds = [GetFlightAwareApiKeyUseCase::class])
class GetFlightAwareApiKeyUseCaseImpl(
    private val settingsService: SettingsService,
) : GetFlightAwareApiKeyUseCase {
    override suspend operator fun invoke(): String {
        return settingsService.getFlightAwareApiKey()
    }
}
