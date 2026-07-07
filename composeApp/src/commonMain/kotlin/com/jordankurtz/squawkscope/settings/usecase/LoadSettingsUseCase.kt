package com.jordankurtz.squawkscope.settings.usecase

import com.jordankurtz.squawkscope.model.Async
import com.jordankurtz.squawkscope.settings.Settings
import kotlinx.coroutines.flow.Flow

interface LoadSettingsUseCase {
    operator fun invoke(): Flow<Async<Settings>>
}
