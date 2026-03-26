package com.jordankurtz.piawaremobile.aircraft.api

import com.jordankurtz.piawaremobile.settings.ServerType

interface AircraftDataSourceFactory {
    fun getDataSource(serverType: ServerType): AircraftDataSource
}
