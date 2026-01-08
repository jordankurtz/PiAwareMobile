package com.jordankurtz.piawaremobile.settings.usecase

interface SetShowMinimapTrailsUseCase {
    suspend operator fun invoke(showMinimapTrails: Boolean)
}
