package com.jordankurtz.piawaremobile.settings.usecase

interface SetShowAircraftPathsUseCase {
    suspend operator fun invoke(enabled: Boolean)
}
