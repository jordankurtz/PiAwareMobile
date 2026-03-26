package com.jordankurtz.piawaremobile.aircraft.api

import com.jordankurtz.piawaremobile.aircraft.api.impl.AircraftDataSourceFactoryImpl
import com.jordankurtz.piawaremobile.aircraft.api.impl.PiAwareDataSource
import com.jordankurtz.piawaremobile.aircraft.api.impl.ReadsbDataSource
import com.jordankurtz.piawaremobile.settings.ServerType
import dev.mokkery.mock
import io.ktor.client.HttpClient
import kotlin.test.Test
import kotlin.test.assertSame

class AircraftDataSourceFactoryTest {
    private val piAwareApi: PiAwareApi = mock()
    private val piAwareDataSource = PiAwareDataSource(piAwareApi)
    private val readsbDataSource = ReadsbDataSource(HttpClient())
    private val factory = AircraftDataSourceFactoryImpl(piAwareDataSource, readsbDataSource)

    @Test
    fun `getDataSource returns PiAwareDataSource for PIAWARE type`() {
        val result = factory.getDataSource(ServerType.PIAWARE)
        assertSame(piAwareDataSource, result)
    }

    @Test
    fun `getDataSource returns ReadsbDataSource for READSB type`() {
        val result = factory.getDataSource(ServerType.READSB)
        assertSame(readsbDataSource, result)
    }
}
