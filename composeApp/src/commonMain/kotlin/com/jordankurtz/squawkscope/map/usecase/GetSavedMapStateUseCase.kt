package com.jordankurtz.squawkscope.map.usecase

import com.jordankurtz.squawkscope.model.MapState

interface GetSavedMapStateUseCase {
    suspend operator fun invoke(): MapState
}
