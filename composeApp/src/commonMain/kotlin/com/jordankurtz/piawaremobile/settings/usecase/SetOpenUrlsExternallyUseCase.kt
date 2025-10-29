package com.jordankurtz.piawaremobile.settings.usecase

interface SetOpenUrlsExternallyUseCase {
    suspend operator fun invoke(enabled: Boolean)
}
