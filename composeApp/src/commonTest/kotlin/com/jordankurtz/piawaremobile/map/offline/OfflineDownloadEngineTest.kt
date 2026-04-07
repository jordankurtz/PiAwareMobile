package com.jordankurtz.piawaremobile.map.offline

import com.jordankurtz.piawaremobile.map.cache.TileCache
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineDownloadEngineTest {
    private lateinit var tileCache: TileCache
    private lateinit var offlineStore: OfflineTileStore
    private val testDispatcher = StandardTestDispatcher()

    private val tileBytes = byteArrayOf(1, 2, 3, 4, 5)

    // 1x1 degree box: 40-41°N, 75-74°W — yields 4 tiles at zoom 8
    private val testRegion =
        OfflineRegion(
            id = 1L,
            name = "Test",
            minZoom = 8,
            maxZoom = 8,
            minLat = 40.0,
            maxLat = 41.0,
            minLon = -75.0,
            maxLon = -74.0,
            providerId = "openstreetmap",
            createdAt = 1000L,
        )

    // Use zero delay so tests don't wait 1 second per tile
    private val config = TileProviders.OPENSTREETMAP.copy(requestDelayMs = 0L)

    @BeforeTest
    fun setUp() {
        tileCache = mock()
        offlineStore = mock()
    }

    private fun createEngine(httpClient: HttpClient) =
        OfflineDownloadEngine(
            tileCache = tileCache,
            offlineTileStore = offlineStore,
            httpClient = httpClient,
            ioDispatcher = testDispatcher,
        )

    private fun successHttpClient(): HttpClient =
        HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(
                        content = ByteReadChannel(tileBytes),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "image/png"),
                    )
                }
            }
        }

    @Test
    fun `download emits one progress event per tile`() =
        runTest(testDispatcher) {
            everySuspend { tileCache.get(any(), any(), any()) } returns null
            everySuspend { tileCache.put(any(), any(), any(), any()) } returns Unit
            everySuspend { offlineStore.isPinned(any(), any(), any()) } returns false
            everySuspend { offlineStore.pinTile(any(), any(), any(), any()) } returns Unit
            everySuspend { offlineStore.updateRegionStats(any(), any(), any()) } returns Unit

            val engine = createEngine(successHttpClient())
            val events = mutableListOf<DownloadProgress>()

            engine.download(testRegion, config).collect { events.add(it) }

            // 4 tiles in the 1x1 degree box at zoom 8
            assertEquals(4, events.size)
            assertEquals(4L, events.last().downloaded)
            assertEquals(4L, events.last().total)
        }

    @Test
    fun `download skips already-pinned tiles and does not fetch from network`() =
        runTest(testDispatcher) {
            // First tile (col=74, row=96 at zoom 8) is already pinned
            everySuspend { offlineStore.isPinned(any(), any(), any()) } returns false
            everySuspend { offlineStore.isPinned(zoomLevel = 8, col = 74, row = 96) } returns true
            everySuspend { tileCache.get(any(), any(), any()) } returns null
            everySuspend { tileCache.put(any(), any(), any(), any()) } returns Unit
            everySuspend { offlineStore.pinTile(any(), any(), any(), any()) } returns Unit
            everySuspend { offlineStore.updateRegionStats(any(), any(), any()) } returns Unit

            val engine = createEngine(successHttpClient())
            val events = mutableListOf<DownloadProgress>()
            engine.download(testRegion, config).collect { events.add(it) }

            // 4 total tiles, 1 skipped → 3 network fetches (puts to cache)
            verifySuspend(VerifyMode.exactly(3)) {
                tileCache.put(any(), any(), any(), any())
            }
            // Skipped tiles still count toward total
            assertEquals(4L, events.last().total)
        }

    @Test
    fun `download pins each tile after putting in cache`() =
        runTest(testDispatcher) {
            everySuspend { offlineStore.isPinned(any(), any(), any()) } returns false
            everySuspend { tileCache.get(any(), any(), any()) } returns null
            everySuspend { tileCache.put(any(), any(), any(), any()) } returns Unit
            everySuspend { offlineStore.pinTile(any(), any(), any(), any()) } returns Unit
            everySuspend { offlineStore.updateRegionStats(any(), any(), any()) } returns Unit

            val engine = createEngine(successHttpClient())
            engine.download(testRegion, config).collect {}

            verifySuspend(VerifyMode.exactly(4)) {
                offlineStore.pinTile(any(), any(), any(), any())
            }
        }

    @Test
    fun `download calls updateRegionStats once on completion`() =
        runTest(testDispatcher) {
            everySuspend { offlineStore.isPinned(any(), any(), any()) } returns false
            everySuspend { tileCache.get(any(), any(), any()) } returns null
            everySuspend { tileCache.put(any(), any(), any(), any()) } returns Unit
            everySuspend { offlineStore.pinTile(any(), any(), any(), any()) } returns Unit
            everySuspend { offlineStore.updateRegionStats(any(), any(), any()) } returns Unit

            val engine = createEngine(successHttpClient())
            engine.download(testRegion, config).collect {}

            verifySuspend(VerifyMode.exactly(1)) {
                offlineStore.updateRegionStats(id = 1L, tileCount = any(), sizeBytes = any())
            }
        }

    @Test
    fun `download skips tile silently on network error and still completes`() =
        runTest(testDispatcher) {
            val failingClient =
                HttpClient(MockEngine) {
                    engine {
                        addHandler { throw RuntimeException("Network error") }
                    }
                }
            everySuspend { offlineStore.isPinned(any(), any(), any()) } returns false
            everySuspend { tileCache.get(any(), any(), any()) } returns null
            everySuspend { tileCache.put(any(), any(), any(), any()) } returns Unit
            everySuspend { offlineStore.pinTile(any(), any(), any(), any()) } returns Unit
            everySuspend { offlineStore.updateRegionStats(any(), any(), any()) } returns Unit

            val engine = createEngine(failingClient)

            var completed = false
            engine.download(testRegion, config).collect {}
            completed = true

            assertTrue(completed)
            // No tiles should be cached on network error
            verifySuspend(VerifyMode.exactly(0)) {
                tileCache.put(any(), any(), any(), any())
            }
        }
}
