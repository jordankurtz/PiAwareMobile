package com.jordankurtz.piawaremobile.aircraft.repo

import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.PiAwareResponse
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
    fun `mergeHistoryResponses builds trails from history`() =
        runTest {
            val historyAircraft = Aircraft(hex = "abc123", lat = 32.5, lon = -96.5, seenPos = 5f)
            val responses =
                listOf(
                    PiAwareResponse(
                        now = 1000.0,
                        aircraft = listOf(historyAircraft),
                    ),
                    PiAwareResponse(
                        now = 1030.0,
                        aircraft = listOf(historyAircraft.copy(lat = 32.6, seenPos = 5f)),
                    ),
                )

            trailManager.mergeHistoryResponses(responses)

            // History positions are stored but not emitted until aircraft becomes current
            trailManager.updateTrailsFromAircraft(listOf(historyAircraft))

            val trails = trailManager.aircraftTrails.value
            assertTrue(trails.containsKey(historyAircraft.hex))
            assertTrue(trails[historyAircraft.hex]!!.positions.size >= 2)
        }

    @Test
    fun `mergeHistoryResponses deduplicates positions on repeated calls`() =
        runTest {
            val historyAircraft = Aircraft(hex = "abc123", lat = 32.5, lon = -96.5, seenPos = 5f)
            val responses =
                listOf(
                    PiAwareResponse(
                        now = 1000.0,
                        aircraft = listOf(historyAircraft),
                    ),
                    PiAwareResponse(
                        now = 1030.0,
                        aircraft = listOf(historyAircraft.copy(lat = 32.6, seenPos = 5f)),
                    ),
                )

            trailManager.mergeHistoryResponses(responses)
            trailManager.mergeHistoryResponses(responses) // Same data again

            trailManager.updateTrailsFromAircraft(listOf(historyAircraft))

            val trails = trailManager.aircraftTrails.value
            val positions = trails[historyAircraft.hex]!!.positions
            // 2 deduped history positions + 1 current = 3
            assertEquals(3, positions.size)
        }

    @Test
    fun `mergeHistoryResponses sorts positions by timestamp`() =
        runTest {
            val historyAircraft = Aircraft(hex = "abc123", lat = 32.5, lon = -96.5, seenPos = 0f)
            val responses =
                listOf(
                    PiAwareResponse(now = 1060.0, aircraft = listOf(historyAircraft.copy(lat = 32.6))),
                    PiAwareResponse(now = 1000.0, aircraft = listOf(historyAircraft.copy(lat = 32.5))),
                    PiAwareResponse(now = 1120.0, aircraft = listOf(historyAircraft.copy(lat = 32.7))),
                )

            trailManager.mergeHistoryResponses(responses)
            trailManager.updateTrailsFromAircraft(listOf(historyAircraft.copy(lat = 32.7)))

            val trails = trailManager.aircraftTrails.value
            val positions = trails[historyAircraft.hex]!!.positions

            assertEquals(3, positions.size)
            assertEquals(32.5, positions[0].latitude)
            assertEquals(32.6, positions[1].latitude)
            assertEquals(32.7, positions[2].latitude)
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
    fun `mergeHistoryResponses with empty list does not crash`() =
        runTest {
            trailManager.mergeHistoryResponses(emptyList())

            val trails = trailManager.aircraftTrails.value
            assertTrue(trails.isEmpty())
        }

    @Test
    fun `mergeHistoryResponses uses seenPos for position age`() =
        runTest {
            val aircraft = Aircraft(hex = "abc123", lat = 32.5, lon = -96.5, seenPos = 10f, seen = 20f)
            val responses =
                listOf(
                    PiAwareResponse(now = 1000.0, aircraft = listOf(aircraft)),
                )

            trailManager.mergeHistoryResponses(responses)
            trailManager.updateTrailsFromAircraft(listOf(aircraft))

            val trails = trailManager.aircraftTrails.value
            val positions = trails[aircraft.hex]!!.positions
            // History position timestamp should be now - seenPos = 1000 - 10 = 990
            assertEquals(990.0, positions.first().timestamp)
        }

    @Test
    fun `mergeHistoryResponses falls back to seen when seenPos is null`() =
        runTest {
            val aircraft = Aircraft(hex = "abc123", lat = 32.5, lon = -96.5, seenPos = null, seen = 15f)
            val responses =
                listOf(
                    PiAwareResponse(now = 1000.0, aircraft = listOf(aircraft)),
                )

            trailManager.mergeHistoryResponses(responses)
            trailManager.updateTrailsFromAircraft(listOf(aircraft))

            val trails = trailManager.aircraftTrails.value
            val positions = trails[aircraft.hex]!!.positions
            // History position timestamp should be now - seen = 1000 - 15 = 985
            assertEquals(985.0, positions.first().timestamp)
        }

    @Test
    fun `mergeHistoryResponses deduplicates by location after sort`() =
        runTest {
            val aircraft = Aircraft(hex = "abc123", lat = 32.5, lon = -96.5, seenPos = 0f)
            val responses =
                listOf(
                    PiAwareResponse(now = 1000.0, aircraft = listOf(aircraft)),
                    // Same location, different timestamp
                    PiAwareResponse(now = 1030.0, aircraft = listOf(aircraft)),
                )

            trailManager.mergeHistoryResponses(responses)
            trailManager.updateTrailsFromAircraft(listOf(aircraft))

            val trails = trailManager.aircraftTrails.value
            val positions = trails[aircraft.hex]!!.positions
            // Should deduplicate same lat/lon even with different timestamps
            assertEquals(1, positions.size)
        }

    @Test
    fun `mergeHistoryResponses skips responses with null timestamp`() =
        runTest {
            val historyAircraft = Aircraft(hex = "abc123", lat = 32.5, lon = -96.5, seenPos = 0f)
            val responses =
                listOf(
                    PiAwareResponse(now = null, aircraft = listOf(historyAircraft)),
                    PiAwareResponse(now = 1000.0, aircraft = listOf(historyAircraft)),
                )

            trailManager.mergeHistoryResponses(responses)
            trailManager.updateTrailsFromAircraft(listOf(historyAircraft))

            val trails = trailManager.aircraftTrails.value
            val positions = trails[historyAircraft.hex]!!.positions
            // Only 1 from history (null timestamp skipped), current position deduped (same lat/lon)
            assertEquals(1, positions.size)
        }
}
