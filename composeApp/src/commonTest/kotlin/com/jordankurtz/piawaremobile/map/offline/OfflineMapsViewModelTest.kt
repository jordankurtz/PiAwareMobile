package com.jordankurtz.piawaremobile.map.offline

import com.jordankurtz.piawaremobile.map.cache.TileCache
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.matching
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineMapsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val downloadScopeHolder = DownloadScopeHolder(testDispatcher)

    private lateinit var store: OfflineTileStore
    private lateinit var engine: DownloadEngine
    private lateinit var tileCache: TileCache
    private lateinit var vm: OfflineMapsViewModel

    private val savedRegion =
        OfflineRegion(
            id = 1L,
            name = "Home",
            minZoom = 8,
            maxZoom = 10,
            minLat = 40.0,
            maxLat = 41.0,
            minLon = -75.0,
            maxLon = -74.0,
            providerId = "openstreetmap",
            createdAt = 1000L,
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        store = mock()
        engine = mock()
        tileCache = mock()
        everySuspend { store.resetStuckDownloads() } returns Unit
        everySuspend { store.updateDownloadStatus(any(), any(), any()) } returns Unit
        everySuspend { store.updateRegionStats(any(), any(), any()) } returns Unit
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `regions are loaded on construction`() =
        runTest {
            everySuspend { store.getRegions() } returns listOf(savedRegion)

            vm = OfflineMapsViewModel(store, engine, tileCache, downloadScopeHolder, testDispatcher)
            advanceUntilIdle()

            assertEquals(listOf(savedRegion), vm.regions.value)
        }

    @Test
    fun `confirmDelete removes region and refreshes list`() =
        runTest {
            everySuspend { store.getRegions() } returns listOf(savedRegion)
            everySuspend { store.deleteRegion(any()) } returns Unit
            everySuspend { store.getExclusiveTilesForRegion(any()) } returns emptyList()
            everySuspend { store.getFreedBytesForRegion(any()) } returns 0L

            vm = OfflineMapsViewModel(store, engine, tileCache, downloadScopeHolder, testDispatcher)
            advanceUntilIdle()

            everySuspend { store.getRegions() } returns emptyList()
            vm.requestDeleteRegion(savedRegion)
            vm.confirmDelete()
            advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(1)) { store.deleteRegion(savedRegion.id) }
            assertEquals(emptyList(), vm.regions.value)
        }

    @Test
    fun `isDownloading is true while download is in progress`() =
        runTest {
            val progress1 = DownloadProgress(regionId = 2L, downloaded = 1L, total = 2L)
            val progress2 = DownloadProgress(regionId = 2L, downloaded = 2L, total = 2L)

            everySuspend { store.getRegions() } returns emptyList()
            everySuspend { store.saveRegion(any()) } returns 2L
            every { engine.download(any(), any()) } returns flowOf(progress1, progress2)

            vm = OfflineMapsViewModel(store, engine, tileCache, downloadScopeHolder, testDispatcher)
            advanceUntilIdle()

            val bounds = BoundingBox(minLat = 40.0, maxLat = 41.0, minLon = -75.0, maxLon = -74.0)
            vm.startDownload("Airport area", bounds, minZoom = 8, maxZoom = 12)

            assertTrue(vm.isDownloading.value)

            advanceUntilIdle()

            assertFalse(vm.isDownloading.value)
        }

    @Test
    fun `downloadProgress reflects latest progress event`() =
        runTest(testDispatcher) {
            everySuspend { store.getRegions() } returns emptyList()
            everySuspend { store.saveRegion(any()) } returns 1L
            val progress = DownloadProgress(regionId = 1L, downloaded = 5L, total = 10L)
            every { engine.download(any(), any()) } returns flowOf(progress)

            val vm = OfflineMapsViewModel(store, engine, tileCache, downloadScopeHolder, testDispatcher)
            advanceUntilIdle()

            val collected = mutableListOf<DownloadProgress?>()
            val collectorJob =
                launch {
                    vm.downloadProgress.collect { collected.add(it) }
                }

            assertNull(vm.downloadProgress.value)

            vm.startDownload(
                name = "Test",
                bounds = BoundingBox(minLat = 40.0, maxLat = 41.0, minLon = -75.0, maxLon = -74.0),
                minZoom = 8,
                maxZoom = 10,
            )
            advanceUntilIdle()

            // Progress should have been emitted and then cleared
            assertTrue(collected.any { it != null })
            assertNull(vm.downloadProgress.value)
            collectorJob.cancel()
        }

    @Test
    fun `startDownload creates region with correct fields`() =
        runTest {
            everySuspend { store.getRegions() } returns emptyList()
            everySuspend { store.saveRegion(any()) } returns 2L
            every { engine.download(any(), any()) } returns flowOf()

            vm = OfflineMapsViewModel(store, engine, tileCache, downloadScopeHolder, testDispatcher)
            advanceUntilIdle()

            val bounds = BoundingBox(minLat = 40.0, maxLat = 41.0, minLon = -75.0, maxLon = -74.0)
            vm.startDownload("Airport area", bounds, minZoom = 8, maxZoom = 12)
            advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(1)) {
                store.saveRegion(
                    matching<OfflineRegion> { region ->
                        region.name == "Airport area" &&
                            region.minZoom == 8 &&
                            region.maxZoom == 12 &&
                            region.providerId == "openstreetmap"
                    },
                )
            }
        }

    @Test
    fun `startDownload uses provider id for region and download`() =
        runTest {
            val customProvider =
                TileProviderConfig(
                    id = "custom",
                    urlTemplate = "https://example.com/{z}/{x}/{y}.png",
                    requestDelayMs = 0L,
                    avgTileSizeBytes = 10_000L,
                    userAgent = "Test",
                )
            everySuspend { store.getRegions() } returns emptyList()
            everySuspend { store.saveRegion(any()) } returns 3L
            every { engine.download(any(), any()) } returns flowOf()

            vm = OfflineMapsViewModel(store, engine, tileCache, downloadScopeHolder, testDispatcher)
            advanceUntilIdle()

            val bounds = BoundingBox(minLat = 40.0, maxLat = 41.0, minLon = -75.0, maxLon = -74.0)
            vm.startDownload("Custom area", bounds, minZoom = 8, maxZoom = 12, provider = customProvider)
            advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(1)) {
                store.saveRegion(
                    matching<OfflineRegion> { region -> region.providerId == "custom" },
                )
            }
        }

    @Test
    fun `resetStuckDownloads is called on construction`() =
        runTest {
            everySuspend { store.getRegions() } returns emptyList()

            vm = OfflineMapsViewModel(store, engine, tileCache, downloadScopeHolder, testDispatcher)
            advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(1)) { store.resetStuckDownloads() }
        }

    @Test
    fun `cancelDownload writes PARTIAL status and reloads regions`() =
        runTest {
            val partialRegion = savedRegion.copy(status = DownloadStatus.PARTIAL)
            everySuspend { store.saveRegion(any()) } returns 1L
            val downloadFlow =
                flow<DownloadProgress> {
                    emit(DownloadProgress(regionId = 1L, downloaded = 5L, total = 10L))
                    awaitCancellation()
                }
            every { engine.download(any(), any()) } returns downloadFlow
            everySuspend { store.getRegions() } returns listOf(partialRegion)

            vm = OfflineMapsViewModel(store, engine, tileCache, downloadScopeHolder, testDispatcher)
            advanceUntilIdle()

            val bounds = BoundingBox(minLat = 40.0, maxLat = 41.0, minLon = -75.0, maxLon = -74.0)
            vm.startDownload("Home", bounds, minZoom = 8, maxZoom = 12)
            advanceUntilIdle()

            vm.cancelDownload()
            advanceUntilIdle()

            verifySuspend(mode = VerifyMode.atLeast(1)) {
                store.updateRegionStats(1L, 10L, 0L)
            }
            verifySuspend(mode = VerifyMode.atLeast(1)) {
                store.updateDownloadStatus(1L, DownloadStatus.PARTIAL, any())
            }
            assertFalse(vm.isDownloading.value)
        }

    @Test
    fun `retryDownload starts a new download for existing region`() =
        runTest {
            val failedRegion = savedRegion.copy(status = DownloadStatus.FAILED)
            everySuspend { store.getRegions() } returns listOf(failedRegion)
            val progress = DownloadProgress(regionId = 1L, downloaded = 2L, total = 2L)
            every { engine.download(any(), any()) } returns flowOf(progress)

            vm = OfflineMapsViewModel(store, engine, tileCache, downloadScopeHolder, testDispatcher)
            advanceUntilIdle()

            vm.retryDownload(failedRegion)
            assertTrue(vm.isDownloading.value)

            advanceUntilIdle()
            assertFalse(vm.isDownloading.value)
            verifySuspend(mode = VerifyMode.atLeast(1)) {
                store.updateDownloadStatus(1L, DownloadStatus.DOWNLOADING, any())
            }
        }

    @Test
    fun `requestDeleteRegion is blocked for DOWNLOADING region`() =
        runTest {
            val downloadingRegion = savedRegion.copy(status = DownloadStatus.DOWNLOADING)
            everySuspend { store.getRegions() } returns listOf(downloadingRegion)

            vm = OfflineMapsViewModel(store, engine, tileCache, downloadScopeHolder, testDispatcher)
            advanceUntilIdle()

            vm.requestDeleteRegion(downloadingRegion)
            advanceUntilIdle()

            assertNull(vm.pendingDeleteRegion.value)
        }

    @Test
    fun `requestDeleteRegion is allowed for PARTIAL region`() =
        runTest {
            val partialRegion = savedRegion.copy(status = DownloadStatus.PARTIAL)
            everySuspend { store.getRegions() } returns listOf(partialRegion)
            everySuspend { store.getFreedBytesForRegion(any()) } returns 0L

            vm = OfflineMapsViewModel(store, engine, tileCache, downloadScopeHolder, testDispatcher)
            advanceUntilIdle()

            vm.requestDeleteRegion(partialRegion)
            advanceUntilIdle()

            assertEquals(partialRegion, vm.pendingDeleteRegion.value)
        }
}
