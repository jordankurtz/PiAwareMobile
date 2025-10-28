package com.jordankurtz.piawaremobile.settings.usecase

interface AddServerUseCase {
    suspend operator fun invoke(name: String, address: String)
}
