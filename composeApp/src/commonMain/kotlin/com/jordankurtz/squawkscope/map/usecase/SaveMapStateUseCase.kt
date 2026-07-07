package com.jordankurtz.squawkscope.map.usecase

interface SaveMapStateUseCase {
    suspend operator fun invoke(
        scrollX: Double,
        scrollY: Double,
        zoom: Double,
    )
}
