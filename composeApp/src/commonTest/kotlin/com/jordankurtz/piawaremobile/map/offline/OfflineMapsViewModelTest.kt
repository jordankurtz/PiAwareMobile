package com.jordankurtz.piawaremobile.map.offline

import com.jordankurtz.piawaremobile.map.cache.TileCache
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private lateinit var store: OfflineTileStore
    private lateinit var tileCache: TileCache
    private lateinit var fakeCoordinator: FakeBackgroundDownloadCoordinator
    private lateinit var vm: OfflineMapsViewModel

    private val savedRegion =
        OfflineRegion(
            id = 1L, name = "Home", minZoom = 8, maxZoom = 10,
            minLat = 40.0, maxLat = 41.0, minLon = -75.0, maxLon = -74.0,
            providerId = "openstreetmap", createdAt = 1000L,
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        store = mock()
        tileCache = mock()
        fakeCoordinator = FakeBackgroundDownloadCoordinator()
        everySuspend { store.resetStuckDownloads() } returns Unit
        everySuspend { store.updateDownloadStatus(any(), any(), any()) } returns Unit
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `regions are loaded on construction`() =
        runTest {
            everySuspend { store.getRegions() } returns listOf(savedRegion)

            vm = OfflineMapsViewModel(store, fakeCoordinator, tileCache, testDispatcher)
            advanceUntilIdle()

            assertEquals(listOf(savedRegion), vm.regions.value)
        }

    @Test
    fun `resetStuckDownloads is called on construction`() =
        runTest {
            everySuspend { store.getRegions() } returns emptyList()

            vm = OfflineMapsViewModel(store, fakeCoordinator, tileCache, testDispatcher)
            advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(1)) { store.resetStuckDownloads() }
        }

    @Test
    fun `isDownloading delegates to coordinator`() =
        runTest {
            everySuspend { store.getRegions() } returns emptyList()
            everySuspend { store.saveRegion(any()) } returns 2L

            vm = OfflineMapsViewModel(store, fakeCoordinator, tileCache, testDispatcher)
            advanceUntilIdle()

            vm.startDownload("Airport area", BoundingBox(40.0, 41.0, -75.0, -74.0), 8, 12)
            advanceUntilIdle()

            assertTrue(vm.isDownloading.value)

            fakeCoordinator._isDownloading.value = false
            advanceUntilIdle()

            assertFalse(vm.isDownloading.value)
        }

    @Test
    fun `downloadProgress delegates to coordinator`() =
        runTest {
            everySuspend { store.getRegions() } returns emptyList()
            everySuspend { store.saveRegion(any()) } returns 1L

            vm = OfflineMapsViewModel(store, fakeCoordinator, tileCache, testDispatcher)
            advanceUntilIdle()

            assertNull(vm.downloadProgress.value)

            val progress = DownloadProgress(regionId = 1L, downloaded = 5L, total = 10L)
            fakeCoordinator._progress.value = progress
            advanceUntilIdle()

            assertEquals(progress, vm.downloadProgress.value)
        }

    @Test
    fun `startDownload saves region and delegates to coordinator`() =
        runTest {
            everySuspend { store.getRegions() } returns emptyList()
            everySuspend { store.saveRegion(any()) } returns 2L

            vm = OfflineMapsViewModel(store, fakeCoordinator, tileCache, testDispatcher)
            advanceUntilIdle()

            vm.startDownload("Airport area", BoundingBox(40.0, 41.0, -75.0, -74.0), 8, 12)
            advanceUntilIdle()

            val started = fakeCoordinator.startCalls.first().region
            assertEquals("Airport area", started.name)
            assertEquals(8, started.minZoom)
            assertEquals(12, started.maxZoom)
            assertEquals("openstreetmap", started.providerId)
        }

    @Test
    fun `cancelDownload delegates to coordinator`() =
        runTest {
            everySuspend { store.getRegions() } returns emptyList()

            vm = OfflineMapsViewModel(store, fakeCoordinator, tileCache, testDispatcher)
            advanceUntilIdle()

            vm.cancelDownload()

            assertTrue(fakeCoordinator.cancelCalled)
        }

    @Test
    fun `retryDownload delegates to coordinator`() =
        runTest {
            val failedRegion = savedRegion.copy(status = DownloadStatus.FAILED)
            everySuspend { store.getRegions() } returns listOf(failedRegion)

            vm = OfflineMapsViewModel(store, fakeCoordinator, tileCache, testDispatcher)
            advanceUntilIdle()

            vm.retryDownload(failedRegion)
            advanceUntilIdle()

            assertEquals(failedRegion.id, fakeCoordinator.startCalls.first().region.id)
        }

    @Test
    fun `regions are updated as progress comes in`() =
        runTest {
            val region = savedRegion.copy(status = DownloadStatus.DOWNLOADING)
            everySuspend { store.getRegions() } returns listOf(region)
            everySuspend { store.saveRegion(any()) } returns region.id

            vm = OfflineMapsViewModel(store, fakeCoordinator, tileCache, testDispatcher)
            advanceUntilIdle()

            fakeCoordinator._progress.value = DownloadProgress(region.id, 5L, 10L)
            advanceUntilIdle()

            val updated = vm.regions.value.find { it.id == region.id }!!
            assertEquals(5L, updated.downloadedTileCount)
            assertEquals(10L, updated.tileCount)
        }

    @Test
    fun `regions reload from DB when download finishes`() =
        runTest {
            val completeRegion = savedRegion.copy(status = DownloadStatus.COMPLETE)
            everySuspend { store.getRegions() } returns listOf(completeRegion)

            vm = OfflineMapsViewModel(store, fakeCoordinator, tileCache, testDispatcher)
            fakeCoordinator._isDownloading.value = true
            advanceUntilIdle()

            fakeCoordinator._isDownloading.value = false
            advanceUntilIdle()

            assertEquals(listOf(completeRegion), vm.regions.value)
        }

    @Test
    fun `confirmDelete removes region and refreshes list`() =
        runTest {
            everySuspend { store.getRegions() } returns listOf(savedRegion)
            everySuspend { store.deleteRegion(any()) } returns Unit
            everySuspend { store.getExclusiveTilesForRegion(any()) } returns emptyList()
            everySuspend { store.getFreedBytesForRegion(any()) } returns 0L

            vm = OfflineMapsViewModel(store, fakeCoordinator, tileCache, testDispatcher)
            advanceUntilIdle()

            everySuspend { store.getRegions() } returns emptyList()
            vm.requestDeleteRegion(savedRegion)
            vm.confirmDelete()
            advanceUntilIdle()

            verifySuspend(mode = VerifyMode.exactly(1)) { store.deleteRegion(savedRegion.id) }
            assertEquals(emptyList(), vm.regions.value)
        }

    @Test
    fun `requestDeleteRegion is blocked for DOWNLOADING region`() =
        runTest {
            val downloadingRegion = savedRegion.copy(status = DownloadStatus.DOWNLOADING)
            everySuspend { store.getRegions() } returns listOf(downloadingRegion)

            vm = OfflineMapsViewModel(store, fakeCoordinator, tileCache, testDispatcher)
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

            vm = OfflineMapsViewModel(store, fakeCoordinator, tileCache, testDispatcher)
            advanceUntilIdle()

            vm.requestDeleteRegion(partialRegion)
            advanceUntilIdle()

            assertEquals(partialRegion, vm.pendingDeleteRegion.value)
        }

    // --- Test double ---

    class FakeBackgroundDownloadCoordinator : BackgroundDownloadCoordinator {
        val _progress = MutableStateFlow<DownloadProgress?>(null)
        val _isDownloading = MutableStateFlow(false)
        override val progress: StateFlow<DownloadProgress?> = _progress.asStateFlow()
        override val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

        data class StartCall(val region: OfflineRegion, val config: TileProviderConfig)

        val startCalls = mutableListOf<StartCall>()
        var cancelCalled = false

        override fun start(
            region: OfflineRegion,
            config: TileProviderConfig,
        ) {
            startCalls.add(StartCall(region, config))
            _isDownloading.value = true
        }

        override fun cancel() {
            cancelCalled = true
            _isDownloading.value = false
        }
    }
}
