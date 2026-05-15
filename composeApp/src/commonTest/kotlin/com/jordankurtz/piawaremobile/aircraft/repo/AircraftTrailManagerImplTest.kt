package com.jordankurtz.piawaremobile.aircraft.repo

import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftPosition
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AircraftTrailManagerImplTest {
    private lateinit var trailManager: AircraftTrailManagerImpl

    private val aircraft1 = Aircraft(hex = "a8b2c3", flight = "SWA123", lat = 32.7, lon = -96.8)
    private val aircraft2 = Aircraft(hex = "a1b2c3", flight = "DAL456", lat = 32.8, lon = -96.9)
    private val aircraftNoLocation = Aircraft(hex = "d4e5f6", flight = "UAL789", lat = 0.0, lon = 0.0)

    @BeforeTest
    fun setup() {
        trailManager = AircraftTrailManagerImpl()
    }

    @Test
    fun `updateTrailsFromAircraft adds positions for aircraft with location`() =
        runTest {
            trailManager.updateTrailsFromAircraft(listOf(aircraft1, aircraft2))

            val trails = trailManager.aircraftTrails.value
            assertEquals(2, trails.size)
            assertTrue(trails.containsKey(aircraft1.hex))
            assertTrue(trails.containsKey(aircraft2.hex))
            assertEquals(1, trails[aircraft1.hex]?.positions?.size)
            assertEquals(1, trails[aircraft2.hex]?.positions?.size)
        }

    @Test
    fun `updateTrailsFromAircraft does not add duplicate positions`() =
        runTest {
            trailManager.updateTrailsFromAircraft(listOf(aircraft1))
            trailManager.updateTrailsFromAircraft(listOf(aircraft1)) // Same position

            val trails = trailManager.aircraftTrails.value
            assertEquals(1, trails[aircraft1.hex]?.positions?.size)
        }

    @Test
    fun `updateTrailsFromAircraft adds new positions when location changes`() =
        runTest {
            trailManager.updateTrailsFromAircraft(listOf(aircraft1))

            val movedAircraft = aircraft1.copy(lat = 33.0, lon = -97.0)
            trailManager.updateTrailsFromAircraft(listOf(movedAircraft))

            val trails = trailManager.aircraftTrails.value
            assertEquals(2, trails[aircraft1.hex]?.positions?.size)
        }

    @Test
    fun `updateTrailsFromAircraft only includes current aircraft in trails`() =
        runTest {
            trailManager.updateTrailsFromAircraft(listOf(aircraft1, aircraft2))
            trailManager.updateTrailsFromAircraft(listOf(aircraft1)) // aircraft2 gone

            val trails = trailManager.aircraftTrails.value
            assertEquals(1, trails.size)
            assertTrue(trails.containsKey(aircraft1.hex))
            assertTrue(!trails.containsKey(aircraft2.hex))
        }

    @Test
    fun `updateTrailsFromAircraft filters out aircraft with no location`() =
        runTest {
            trailManager.updateTrailsFromAircraft(listOf(aircraft1, aircraftNoLocation))

            val trails = trailManager.aircraftTrails.value
            assertEquals(1, trails.size)
            assertTrue(trails.containsKey(aircraft1.hex))
        }

    @Test
    fun `clearTrails removes all trail data`() =
        runTest {
            trailManager.updateTrailsFromAircraft(listOf(aircraft1, aircraft2))
            trailManager.clearTrails()

            val trails = trailManager.aircraftTrails.value
            assertTrue(trails.isEmpty())
        }

    @Test
    fun `updateTrailsFromAircraft with empty list clears current aircraft`() =
        runTest {
            trailManager.updateTrailsFromAircraft(listOf(aircraft1))
            trailManager.updateTrailsFromAircraft(emptyList())

            val trails = trailManager.aircraftTrails.value
            assertTrue(trails.isEmpty())
        }

    @Test
    fun `mergeTrails adds positions from input map`() =
        runTest {
            val aircraft = listOf(Aircraft(hex = "abc123", lat = 32.7, lon = -96.8))
            trailManager.updateTrailsFromAircraft(aircraft)

            trailManager.mergeTrails(
                mapOf(
                    "abc123" to listOf(
                        AircraftPosition(latitude = 32.5, longitude = -96.5, altitude = null, timestamp = 100.0),
                        AircraftPosition(latitude = 32.6, longitude = -96.6, altitude = "35000", timestamp = 200.0),
                    ),
                ),
            )

            val trail = trailManager.aircraftTrails.value["abc123"]!!
            assertTrue(trail.positions.any { it.timestamp == 100.0 })
            assertTrue(trail.positions.any { it.timestamp == 200.0 })
        }

    @Test
    fun `mergeTrails deduplicates positions with identical timestamps`() =
        runTest {
            val aircraft = listOf(Aircraft(hex = "abc123", lat = 32.7, lon = -96.8))
            trailManager.updateTrailsFromAircraft(aircraft)

            val position = AircraftPosition(latitude = 32.5, longitude = -96.5, altitude = null, timestamp = 100.0)
            trailManager.mergeTrails(mapOf("abc123" to listOf(position)))
            trailManager.mergeTrails(mapOf("abc123" to listOf(position)))

            val trail = trailManager.aircraftTrails.value["abc123"]!!
            assertEquals(1, trail.positions.count { it.timestamp == 100.0 })
        }

    @Test
    fun `mergeTrails sorts positions by timestamp`() =
        runTest {
            val aircraft = listOf(Aircraft(hex = "abc123", lat = 32.7, lon = -96.8))
            trailManager.updateTrailsFromAircraft(aircraft)

            trailManager.mergeTrails(
                mapOf(
                    "abc123" to listOf(
                        AircraftPosition(latitude = 32.6, longitude = -96.6, altitude = null, timestamp = 200.0),
                        AircraftPosition(latitude = 32.5, longitude = -96.5, altitude = null, timestamp = 100.0),
                    ),
                ),
            )

            val positions = trailManager.aircraftTrails.value["abc123"]!!.positions
            assertTrue(positions[0].timestamp < positions[1].timestamp)
        }

    @Test
    fun `mergeTrails deduplicates consecutive identical locations`() =
        runTest {
            val aircraft = listOf(Aircraft(hex = "abc123", lat = 32.7, lon = -96.8))
            trailManager.updateTrailsFromAircraft(aircraft)

            trailManager.mergeTrails(
                mapOf(
                    "abc123" to listOf(
                        AircraftPosition(latitude = 32.5, longitude = -96.5, altitude = null, timestamp = 100.0),
                        AircraftPosition(latitude = 32.5, longitude = -96.5, altitude = null, timestamp = 200.0),
                        AircraftPosition(latitude = 32.6, longitude = -96.6, altitude = null, timestamp = 300.0),
                    ),
                ),
            )

            val positions = trailManager.aircraftTrails.value["abc123"]!!.positions
            // 32.5/-96.5 appears twice in input; consecutive same-location dedup removes one.
            // Plus the current position (32.7/-96.8) added by updateTrailsFromAircraft = 3 total.
            assertEquals(3, positions.size)
        }

    @Test
    fun `mergeTrails with empty map does not crash`() =
        runTest {
            trailManager.mergeTrails(emptyMap())
        }

    @Test
    fun `mergeTrails accumulates across multiple calls`() =
        runTest {
            val aircraft = listOf(Aircraft(hex = "abc123", lat = 32.7, lon = -96.8))
            trailManager.updateTrailsFromAircraft(aircraft)

            trailManager.mergeTrails(
                mapOf("abc123" to listOf(AircraftPosition(32.5, -96.5, null, 100.0))),
            )
            trailManager.mergeTrails(
                mapOf("abc123" to listOf(AircraftPosition(32.6, -96.6, null, 200.0))),
            )

            val trail = trailManager.aircraftTrails.value["abc123"]!!
            assertTrue(trail.positions.size >= 2)
        }
}
