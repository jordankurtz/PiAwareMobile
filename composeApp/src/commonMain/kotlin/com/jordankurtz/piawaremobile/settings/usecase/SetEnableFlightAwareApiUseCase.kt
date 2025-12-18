package com.jordankurtz.piawaremobile.settings.usecase

interface SetEnableFlightAwareApiUseCase {
    suspend operator fun invoke(enabled: Boolean)
}
