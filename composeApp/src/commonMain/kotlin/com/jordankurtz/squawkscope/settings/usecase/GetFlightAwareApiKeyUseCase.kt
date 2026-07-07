package com.jordankurtz.squawkscope.settings.usecase

interface GetFlightAwareApiKeyUseCase {
    suspend operator fun invoke(): String
}
