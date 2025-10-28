package com.jordankurtz.piawaremobile.settings.usecase

interface SetShowReceiverLocationsUseCase {
    suspend operator fun invoke(enabled: Boolean)
}
