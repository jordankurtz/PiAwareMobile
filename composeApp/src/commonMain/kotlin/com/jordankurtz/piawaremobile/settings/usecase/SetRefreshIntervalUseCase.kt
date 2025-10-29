package com.jordankurtz.piawaremobile.settings.usecase

interface SetRefreshIntervalUseCase {
    suspend operator fun invoke(newRefreshInterval: Int)
}
