package com.jordankurtz.piawaremobile.settings.usecase

import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.Settings
import kotlinx.coroutines.flow.Flow

interface LoadSettingsUseCase {
    operator fun invoke(): Flow<Async<Settings>>
}
