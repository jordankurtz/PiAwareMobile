package com.jordankurtz.piawaremobile.map.offline

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
class BackgroundDownloadCoordinatorTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var store: OfflineTileStore
    private lateinit var engine: DownloadEngine
    private lateinit var coordinator: TestCoordinator

    private val region =
        OfflineRegion(
            id = 1L, name = "Home", minZoom = 8, maxZoom = 10,
            minLat = 40.0, maxLat = 41.0, minLon = -75.0, maxLon = -74.0,
            providerId = "openstreetmap", createdAt = 1000L,
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        store = mock()
        engine = mock()
        everySuspend { store.updateDownloadStatus(any(), any(), any()) } returns Unit
        everySuspend { store.updateRegionStats(any(), any(), any()) } returns Unit
        coordinator = TestCoordinator(engine, store, testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `isDownloading is true while download is running`() =
        runTest {
            every { engine.download(any(), any()) } returns
                flowOf(
                    DownloadProgress(1L, 1L, 2L),
                    DownloadProgress(1L, 2L, 2L),
                )

            coordinator.start(region, TileProviders.OPENSTREETMAP)
            assertTrue(coordinator.isDownloading.value)

            advanceUntilIdle()
            assertFalse(coordinator.isDownloading.value)
        }

    @Test
    fun `progress is null after download completes`() =
        runTest {
            every { engine.download(any(), any()) } returns flowOf(DownloadProgress(1L, 1L, 1L))

            coordinator.start(region, TileProviders.OPENSTREETMAP)
            advanceUntilIdle()

            assertNull(coordinator.progress.value)
        }

    @Test
    fun `onStartPlatform called when download begins`() =
        runTest {
            every { engine.download(any(), any()) } returns flowOf()

            coordinator.start(region, TileProviders.OPENSTREETMAP)
            advanceUntilIdle()

            assertTrue(coordinator.platformEvents.contains("start:Home"))
        }

    @Test
    fun `onProgressPlatform called for each emission`() =
        runTest {
            every { engine.download(any(), any()) } returns
                flowOf(
                    DownloadProgress(1L, 1L, 2L),
                    DownloadProgress(1L, 2L, 2L),
                )

            coordinator.start(region, TileProviders.OPENSTREETMAP)
            advanceUntilIdle()

            assertTrue(coordinator.platformEvents.contains("progress:1/2"))
            assertTrue(coordinator.platformEvents.contains("progress:2/2"))
        }

    @Test
    fun `onCompletePlatform called on success`() =
        runTest {
            every { engine.download(any(), any()) } returns flowOf()

            coordinator.start(region, TileProviders.OPENSTREETMAP)
            advanceUntilIdle()

            assertTrue(coordinator.platformEvents.contains("complete:Home"))
        }

    @Test
    fun `cancel writes PARTIAL status and calls onCancelledPlatform`() =
        runTest {
            every { engine.download(any(), any()) } returns
                flow {
                    emit(DownloadProgress(1L, 5L, 10L))
                    awaitCancellation()
                }

            coordinator.start(region, TileProviders.OPENSTREETMAP)
            advanceUntilIdle()

            coordinator.cancel()
            advanceUntilIdle()

            assertTrue(coordinator.platformEvents.contains("cancelled"))
            assertFalse(coordinator.isDownloading.value)
            assertNull(coordinator.progress.value)
            verifySuspend(mode = VerifyMode.atLeast(1)) {
                store.updateRegionStats(region.id, 10L, 0L)
            }
            verifySuspend(mode = VerifyMode.atLeast(1)) {
                store.updateDownloadStatus(region.id, DownloadStatus.PARTIAL, 5L)
            }
        }

    @Test
    fun `onFailedPlatform called and isDownloading resets when engine throws`() =
        runTest {
            every { engine.download(any(), any()) } returns
                flow {
                    emit(DownloadProgress(1L, 1L, 2L))
                    throw RuntimeException("network error")
                }

            coordinator.start(region, TileProviders.OPENSTREETMAP)
            advanceUntilIdle()

            assertTrue(coordinator.platformEvents.contains("failed:Home"))
            assertFalse(coordinator.isDownloading.value)
            assertNull(coordinator.progress.value)
        }

    @Test
    fun `second start call is ignored while download is running`() =
        runTest {
            every { engine.download(any(), any()) } returns flow { awaitCancellation() }

            coordinator.start(region, TileProviders.OPENSTREETMAP)
            coordinator.start(region, TileProviders.OPENSTREETMAP)
            advanceUntilIdle()

            assertEquals(1, coordinator.platformEvents.count { it.startsWith("start:") })
        }

    class TestCoordinator(
        engine: DownloadEngine,
        store: OfflineTileStore,
        dispatcher: CoroutineDispatcher,
    ) : BaseDownloadCoordinator(engine, store, dispatcher) {
        val platformEvents = mutableListOf<String>()

        override fun onStartPlatform(regionName: String) {
            platformEvents.add("start:$regionName")
        }

        override fun onProgressPlatform(
            downloaded: Long,
            total: Long,
        ) {
            platformEvents.add("progress:$downloaded/$total")
        }

        override fun onCompletePlatform(regionName: String) {
            platformEvents.add("complete:$regionName")
        }

        override fun onFailedPlatform(regionName: String) {
            platformEvents.add("failed:$regionName")
        }

        override fun onCancelledPlatform() {
            platformEvents.add("cancelled")
        }
    }
}
