package com.jordankurtz.piawaremobile.settings.usecase

import com.jordankurtz.piawaremobile.settings.TrailDisplayMode

interface SetTrailDisplayModeUseCase {
    suspend operator fun invoke(trailDisplayMode: TrailDisplayMode)
}
