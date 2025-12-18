package com.jordankurtz.piawaremobile.settings.usecase

interface GetFlightAwareApiKeyUseCase {
    suspend operator fun invoke(): String
}
