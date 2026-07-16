package com.jordankurtz.squawkscope.settings.usecase.impl

import com.jordankurtz.squawkscope.settings.usecase.GetFlightAwareApiKeyUseCase
import com.jordankurtz.squawkscope.settings.usecase.SettingsService
import org.koin.core.annotation.Factory

@Factory(binds = [GetFlightAwareApiKeyUseCase::class])
class GetFlightAwareApiKeyUseCaseImpl(
    private val settingsService: SettingsService,
) : GetFlightAwareApiKeyUseCase {
    override suspend operator fun invoke(): String = settingsService.getFlightAwareApiKey()
}
