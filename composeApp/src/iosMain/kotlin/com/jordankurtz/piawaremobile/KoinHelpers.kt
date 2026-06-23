package com.jordankurtz.piawaremobile

import com.jordankurtz.piawaremobile.aircraft.AircraftViewModel
import com.jordankurtz.piawaremobile.location.LocationViewModel
import com.jordankurtz.piawaremobile.map.MapViewModel
import com.jordankurtz.piawaremobile.settings.SettingsViewModel
import org.koin.mp.KoinPlatform

fun getAircraftViewModel(): AircraftViewModel = KoinPlatform.getKoin().get()
fun getLocationViewModel(): LocationViewModel = KoinPlatform.getKoin().get()
fun getSettingsViewModel(): SettingsViewModel = KoinPlatform.getKoin().get()
fun getMapViewModel(): MapViewModel = KoinPlatform.getKoin().get()
