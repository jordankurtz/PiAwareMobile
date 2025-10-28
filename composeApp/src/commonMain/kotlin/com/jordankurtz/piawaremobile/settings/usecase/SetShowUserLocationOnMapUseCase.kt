package com.jordankurtz.piawaremobile.settings.usecase

interface SetShowUserLocationOnMapUseCase {
    suspend operator fun invoke(enabled: Boolean)
}
