package com.jordankurtz.piawaremobile.settings.usecase

import kotlin.uuid.Uuid

interface DeleteServerUseCase {
    suspend operator fun invoke(id: Uuid)
}
