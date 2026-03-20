package com.jordankurtz.piawaremobile.settings.usecase

import com.jordankurtz.piawaremobile.settings.Server

interface EditServerUseCase {
    suspend operator fun invoke(server: Server)
}
