package com.jordankurtz.piawaremobile.settings.usecase

interface SetRestoreMapStateOnStartUseCase {
    suspend operator fun invoke(enabled: Boolean)
}
