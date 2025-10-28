package com.jordankurtz.piawaremobile.settings.usecase

interface SetCenterMapOnUserOnStartUseCase {
    suspend operator fun invoke(enabled: Boolean)
}
