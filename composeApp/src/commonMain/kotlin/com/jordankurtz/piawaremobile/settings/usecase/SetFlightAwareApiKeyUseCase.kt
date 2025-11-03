package com.jordankurtz.piawaremobile.settings.usecase

interface SetFlightAwareApiKeyUseCase {
    suspend operator fun invoke(apiKey: String)
}
