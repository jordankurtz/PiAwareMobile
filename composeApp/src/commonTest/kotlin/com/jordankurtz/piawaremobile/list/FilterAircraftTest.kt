package com.jordankurtz.piawaremobile.list

import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftInfo
import com.jordankurtz.piawaremobile.model.AircraftWithServers
import kotlin.test.Test
import kotlin.test.assertEquals

class FilterAircraftTest {
    private fun aircraft(
        hex: String = "ABC123",
        flight: String? = null,
        squawk: String? = null,
        info: AircraftInfo? = null,
    ): AircraftWithServers =
        AircraftWithServers(
            aircraft = Aircraft(hex = hex, flight = flight, squawk = squawk),
            info = info,
        )

    private fun info(
        registration: String? = null,
        icaoType: String? = null,
        typeDescription: String? = null,
    ): AircraftInfo =
        AircraftInfo(
            registration = registration,
            icaoType = icaoType,
            typeDescription = typeDescription,
            wtc = null,
        )

    @Test
    fun emptyQueryReturnsAll() {
        val list = listOf(aircraft(hex = "A"), aircraft(hex = "B"))
        assertEquals(list, filterAircraft(list, ""))
        assertEquals(list, filterAircraft(list, "   "))
    }

    @Test
    fun filterByFlightNumber() {
        val list =
            listOf(
                aircraft(hex = "A1", flight = "DAL123"),
                aircraft(hex = "A2", flight = "SWA456"),
                aircraft(hex = "A3"),
            )
        val result = filterAircraft(list, "dal")
        assertEquals(1, result.size)
        assertEquals("A1", result[0].aircraft.hex)
    }

    @Test
    fun filterByHexCode() {
        val list =
            listOf(
                aircraft(hex = "ABC123"),
                aircraft(hex = "DEF456"),
            )
        val result = filterAircraft(list, "abc")
        assertEquals(1, result.size)
        assertEquals("ABC123", result[0].aircraft.hex)
    }

    @Test
    fun filterByRegistration() {
        val list =
            listOf(
                aircraft(hex = "A1", info = info(registration = "N12345")),
                aircraft(hex = "A2", info = info(registration = "G-ABCD")),
            )
        val result = filterAircraft(list, "n123")
        assertEquals(1, result.size)
        assertEquals("A1", result[0].aircraft.hex)
    }

    @Test
    fun filterByAircraftType() {
        val list =
            listOf(
                aircraft(hex = "A1", info = info(typeDescription = "Boeing 737-800")),
                aircraft(hex = "A2", info = info(typeDescription = "Airbus A320")),
            )
        val result = filterAircraft(list, "boeing")
        assertEquals(1, result.size)
        assertEquals("A1", result[0].aircraft.hex)
    }

    @Test
    fun filterByIcaoType() {
        val list =
            listOf(
                aircraft(hex = "A1", info = info(icaoType = "B738")),
                aircraft(hex = "A2", info = info(icaoType = "A320")),
            )
        val result = filterAircraft(list, "b738")
        assertEquals(1, result.size)
    }

    @Test
    fun filterBySquawk() {
        val list =
            listOf(
                aircraft(hex = "A1", squawk = "7700"),
                aircraft(hex = "A2", squawk = "1200"),
            )
        val result = filterAircraft(list, "7700")
        assertEquals(1, result.size)
        assertEquals("A1", result[0].aircraft.hex)
    }

    @Test
    fun noMatchReturnsEmpty() {
        val list =
            listOf(
                aircraft(hex = "ABC", flight = "DAL123"),
            )
        assertEquals(emptyList(), filterAircraft(list, "xyz"))
    }

    @Test
    fun matchesAcrossMultipleFields() {
        val a1 = aircraft(hex = "MATCH1", flight = "DAL999")
        val a2 = aircraft(hex = "OTHER", info = info(registration = "MATCH2"))
        val list = listOf(a1, a2)
        val result = filterAircraft(list, "match")
        assertEquals(2, result.size)
    }
}
