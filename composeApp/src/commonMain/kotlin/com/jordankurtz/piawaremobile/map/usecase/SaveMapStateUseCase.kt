package com.jordankurtz.piawaremobile.map.usecase

interface SaveMapStateUseCase {
    suspend operator fun invoke(
        latitude: Double,
        longitude: Double,
        zoom: Double,
    )
}
