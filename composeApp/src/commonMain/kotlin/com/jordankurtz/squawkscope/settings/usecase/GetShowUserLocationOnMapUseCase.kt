package com.jordankurtz.squawkscope.settings.usecase

import kotlinx.coroutines.flow.Flow

interface GetShowUserLocationOnMapUseCase {
    operator fun invoke(): Flow<Boolean>
}
