package com.jordankurtz.piawaremobile.aircraft.api.impl

import com.jordankurtz.piawaremobile.aircraft.api.AircraftDataSource
import com.jordankurtz.piawaremobile.aircraft.api.AircraftDataSourceFactory
import com.jordankurtz.piawaremobile.settings.ServerType
import org.koin.core.annotation.Single

@Single(binds = [AircraftDataSourceFactory::class])
class AircraftDataSourceFactoryImpl(
    private val piAwareDataSource: PiAwareDataSource,
    private val readsbDataSource: ReadsbDataSource,
) : AircraftDataSourceFactory {
    override fun getDataSource(serverType: ServerType): AircraftDataSource {
        return when (serverType) {
            ServerType.PIAWARE -> piAwareDataSource
            ServerType.READSB -> readsbDataSource
        }
    }
}
