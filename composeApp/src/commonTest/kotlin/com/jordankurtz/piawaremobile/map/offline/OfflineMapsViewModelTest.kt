package com.jordankurtz.piawaremobile.map.offline

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.matching
import dev.mokkery.mock
import dev.mokkery.verify
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
import com.jordankurtz.piawaremobile.map.TileProviderConfig as MapTileProviderConfig

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineMapsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val downloadScopeHolder = DownloadScopeHolder(testDispatcher)

    private lateinit var store: OfflineTileStore
    private lateinit var engine: DownloadEngine
    private lateinit var mapLibreOfflineApi: MapLibreOfflineApi
    private lateinit var thumbnailGenerator: ThumbnailGenerator
    private lateinit var thumbnailFileManager: ThumbnailFileManager
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
            providerId = "openfreemap-bright",
            createdAt = 1000L,
            status = DownloadStatus.COMPLETE,
            nativeRegionId = 42L,
        )

    private fun makeVm() =
        OfflineMapsViewModel(
            store,
            engine,
            mapLibreOfflineApi,
            downloadScopeHolder,
            thumbnailGenerator,
            thumbnailFileManager,
            testDispatcher,
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        store = mock()
        engine = mock()
        mapLibreOfflineApi = mock()
        thumbnailGenerator = mock()
        thumbnailFileManager = mock()
        everySuspend { store.resetStuckDownloads() } returns Unit
        everySuspend { store.markLegacyRasterRegionsFailed(any()) } returns Unit
        everySuspend { store.updateDownloadStatus(any(), any(), any()) } returns Unit
        everySuspend { store.updateRegionStats(any(), any(), any()) } returns Unit
        everySuspend { mapLibreOfflineApi.deleteRegion(any()) } returns Unit
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `regions are loaded on construction`() =
        runTest {
            everySuspend { store.getRegions() } returns listOf(savedRegion)

            vm = makeVm()
            advanceUntilIdle()

            assertEquals(listOf(savedRegion), vm.regions.value)
        }

    @Test
    fun `confirmDelete removes region and refreshes list`() =
        runTest {
            everySuspend { store.getRegions() } returns listOf(savedRegion)
            everySuspend { store.deleteRegion(any()) } returns Unit
            every { thumbnailFileManager.delete(any()) } returns Unit

            vm = makeVm()
            advanceUntilIdle()

            everySuspend { store.getRegions() } returns emptyList()
            vm.requestDeleteRegion(savedRegion)
            vm.confirmDelete()
            advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(1)) { mapLibreOfflineApi.deleteRegion(42L) }
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
            everySuspend { thumbnailGenerator.generate(any(), any(), any(), any()) } returns false
            every { thumbnailFileManager.thumbnailPath(any()) } returns "/cache/thumbnails/2.png"
            every { engine.download(any(), any()) } returns flowOf(progress1, progress2)

            vm = makeVm()
            advanceUntilIdle()

            val bounds = BoundingBox(minLat = 40.0, maxLat = 41.0, minLon = -75.0, maxLon = -74.0)
            vm.startDownload("Airport area", bounds, minZoom = 8, maxZoom = 12, viewportZoom = 10)

            assertTrue(vm.isDownloading.value)

            advanceUntilIdle()

            assertFalse(vm.isDownloading.value)
        }

    @Test
    fun `downloadProgress reflects latest progress event`() =
        runTest(testDispatcher) {
            everySuspend { store.getRegions() } returns emptyList()
            everySuspend { store.saveRegion(any()) } returns 1L
            everySuspend { thumbnailGenerator.generate(any(), any(), any(), any()) } returns false
            every { thumbnailFileManager.thumbnailPath(any()) } returns "/cache/thumbnails/1.png"
            val progress = DownloadProgress(regionId = 1L, downloaded = 5L, total = 10L)
            every { engine.download(any(), any()) } returns flowOf(progress)

            val localVm = makeVm()
            advanceUntilIdle()

            val collected = mutableListOf<DownloadProgress?>()
            val collectorJob =
                launch {
                    localVm.downloadProgress.collect { collected.add(it) }
                }

            assertNull(localVm.downloadProgress.value)

            localVm.startDownload(
                name = "Test",
                bounds = BoundingBox(minLat = 40.0, maxLat = 41.0, minLon = -75.0, maxLon = -74.0),
                minZoom = 8,
                maxZoom = 10,
                viewportZoom = 9,
            )
            advanceUntilIdle()

            // Progress should have been emitted and then cleared
            assertTrue(collected.any { it != null })
            assertNull(localVm.downloadProgress.value)
            collectorJob.cancel()
        }

    @Test
    fun `startDownload creates region with correct fields`() =
        runTest {
            everySuspend { store.getRegions() } returns emptyList()
            everySuspend { store.saveRegion(any()) } returns 2L
            everySuspend { thumbnailGenerator.generate(any(), any(), any(), any()) } returns false
            every { thumbnailFileManager.thumbnailPath(any()) } returns "/cache/thumbnails/2.png"
            every { engine.download(any(), any()) } returns flowOf()

            vm = makeVm()
            advanceUntilIdle()

            val bounds = BoundingBox(minLat = 40.0, maxLat = 41.0, minLon = -75.0, maxLon = -74.0)
            vm.startDownload("Airport area", bounds, minZoom = 8, maxZoom = 12, viewportZoom = 10)
            advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(1)) {
                store.saveRegion(
                    matching<OfflineRegion> { region ->
                        region.name == "Airport area" &&
                            region.minZoom == 8 &&
                            region.maxZoom == 12 &&
                            region.providerId == "openfreemap-bright"
                    },
                )
            }
        }

    @Test
    fun `startDownload uses provider id for region and download`() =
        runTest {
            val customProvider =
                MapTileProviderConfig(
                    id = "custom",
                    displayName = "Custom Provider",
                    styleUrl = "https://example.com/styles/custom",
                )
            everySuspend { store.getRegions() } returns emptyList()
            everySuspend { store.saveRegion(any()) } returns 3L
            everySuspend { thumbnailGenerator.generate(any(), any(), any(), any()) } returns false
            every { thumbnailFileManager.thumbnailPath(any()) } returns "/cache/thumbnails/3.png"
            every { engine.download(any(), any()) } returns flowOf()

            vm = makeVm()
            advanceUntilIdle()

            val bounds = BoundingBox(minLat = 40.0, maxLat = 41.0, minLon = -75.0, maxLon = -74.0)
            vm.startDownload(
                "Custom area",
                bounds,
                minZoom = 8,
                maxZoom = 12,
                viewportZoom = 10,
                provider = customProvider,
            )
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

            vm = makeVm()
            advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(1)) { store.resetStuckDownloads() }
        }

    @Test
    fun `cancelDownload writes PARTIAL status and reloads regions`() =
        runTest {
            val partialRegion = savedRegion.copy(status = DownloadStatus.PARTIAL)
            everySuspend { store.saveRegion(any()) } returns 1L
            everySuspend { thumbnailGenerator.generate(any(), any(), any(), any()) } returns false
            every { thumbnailFileManager.thumbnailPath(any()) } returns "/cache/thumbnails/1.png"
            val downloadFlow =
                flow<DownloadProgress> {
                    emit(DownloadProgress(regionId = 1L, downloaded = 5L, total = 10L))
                    awaitCancellation()
                }
            every { engine.download(any(), any()) } returns downloadFlow
            everySuspend { store.getRegions() } returns listOf(partialRegion)

            vm = makeVm()
            advanceUntilIdle()

            val bounds = BoundingBox(minLat = 40.0, maxLat = 41.0, minLon = -75.0, maxLon = -74.0)
            vm.startDownload("Home", bounds, minZoom = 8, maxZoom = 12, viewportZoom = 10)
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

            vm = makeVm()
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

            vm = makeVm()
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

            vm = makeVm()
            advanceUntilIdle()

            vm.requestDeleteRegion(partialRegion)
            advanceUntilIdle()

            assertEquals(partialRegion, vm.pendingDeleteRegion.value)
        }

    @Test
    fun `startDownload generates thumbnail and persists path`() =
        runTest {
            everySuspend { store.getRegions() } returns emptyList()
            everySuspend { store.saveRegion(any()) } returns 42L
            everySuspend { thumbnailGenerator.generate(any(), any(), any(), any()) } returns true
            every { thumbnailFileManager.thumbnailPath(42L) } returns "/cache/thumbnails/42.png"
            everySuspend { store.updateThumbnail(any(), any(), any()) } returns Unit
            every { engine.download(any(), any()) } returns flowOf()

            vm = makeVm()
            advanceUntilIdle()

            val bounds = BoundingBox(minLat = 47.0, maxLat = 48.0, minLon = -122.5, maxLon = -122.0)
            vm.startDownload("Test", bounds, minZoom = 8, maxZoom = 14, viewportZoom = 12)
            advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(1)) {
                thumbnailGenerator.generate(
                    bounds = bounds,
                    styleUrl = any(),
                    thumbnailZoom = 12,
                    outputPath = "/cache/thumbnails/42.png",
                )
            }
            verifySuspend(mode = VerifyMode.exactly(1)) {
                store.updateThumbnail(42L, 12, "/cache/thumbnails/42.png")
            }
        }

    @Test
    fun `startDownload gracefully handles thumbnail failure`() =
        runTest {
            everySuspend { store.getRegions() } returns emptyList()
            everySuspend { store.saveRegion(any()) } returns 42L
            everySuspend { thumbnailGenerator.generate(any(), any(), any(), any()) } returns false
            every { thumbnailFileManager.thumbnailPath(any()) } returns "/cache/thumbnails/42.png"
            every { engine.download(any(), any()) } returns flowOf()

            vm = makeVm()
            advanceUntilIdle()

            val bounds = BoundingBox(minLat = 47.0, maxLat = 48.0, minLon = -122.5, maxLon = -122.0)
            vm.startDownload("Test", bounds, minZoom = 8, maxZoom = 14, viewportZoom = 12)
            advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(0)) {
                store.updateThumbnail(any(), any(), any())
            }
        }

    @Test
    fun `onLoad auto-regenerates missing thumbnail for complete region`() =
        runTest {
            val regionWithMissingThumbnail =
                savedRegion.copy(
                    id = 5L,
                    thumbnailZoom = 12,
                    thumbnailPath = null,
                    status = DownloadStatus.COMPLETE,
                )
            everySuspend { store.getRegions() } returns listOf(regionWithMissingThumbnail)
            every { thumbnailFileManager.thumbnailPath(5L) } returns "/cache/thumbnails/5.png"
            everySuspend { thumbnailGenerator.generate(any(), any(), any(), any()) } returns true
            everySuspend { store.updateThumbnail(any(), any(), any()) } returns Unit

            vm = makeVm()
            advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(1)) {
                thumbnailGenerator.generate(
                    bounds = any(),
                    styleUrl = any(),
                    thumbnailZoom = 12,
                    outputPath = "/cache/thumbnails/5.png",
                )
            }
            verifySuspend(mode = VerifyMode.exactly(1)) {
                store.updateThumbnail(5L, 12, "/cache/thumbnails/5.png")
            }
        }

    @Test
    fun `onLoad skips regeneration for incomplete region`() =
        runTest {
            val downloadingRegion =
                savedRegion.copy(
                    id = 6L,
                    thumbnailZoom = 12,
                    thumbnailPath = null,
                    status = DownloadStatus.DOWNLOADING,
                )
            everySuspend { store.getRegions() } returns listOf(downloadingRegion)

            vm = makeVm()
            advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(0)) {
                thumbnailGenerator.generate(any(), any(), any(), any())
            }
        }

    @Test
    fun `confirmDelete deletes thumbnail file`() =
        runTest {
            val regionToDelete = savedRegion.copy(id = 3L)
            everySuspend { store.getRegions() } returns listOf(regionToDelete)
            everySuspend { store.deleteRegion(any()) } returns Unit
            every { thumbnailFileManager.delete(any()) } returns Unit

            vm = makeVm()
            advanceUntilIdle()

            everySuspend { store.getRegions() } returns emptyList()
            vm.requestDeleteRegion(regionToDelete)
            vm.confirmDelete()
            advanceUntilIdle()

            verify(mode = VerifyMode.exactly(1)) { thumbnailFileManager.delete(3L) }
        }
}
