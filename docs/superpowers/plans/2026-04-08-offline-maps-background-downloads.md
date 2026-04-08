# Offline Maps Background Downloads Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep offline tile downloads running when the user locks the screen or leaves the app — using Android Foreground Service with live progress notification and iOS BGContinuedProcessingTask with Live Activity (iOS 26+).

**Architecture:** Retire `DownloadScopeHolder` and introduce `BackgroundDownloadCoordinator` — an interface with a shared `BaseDownloadCoordinator` abstract class that owns the download coroutine and calls platform hooks. Android subclass starts/stops a Foreground Service; iOS subclass delegates to a Swift `DownloadActivityManager` via an `IosDownloadObserver` ObjC protocol.

**Tech Stack:** Kotlin Multiplatform, Koin (koin-annotations), Android Foreground Service, iOS BGContinuedProcessingTask (iOS 26), ActivityKit Live Activity, Mokkery (tests)

---

## File Map

| File | Action |
|---|---|
| `commonMain/.../map/offline/BackgroundDownloadCoordinator.kt` | Create — interface |
| `commonMain/.../map/offline/BaseDownloadCoordinator.kt` | Create — abstract class with download logic |
| `commonMain/.../map/offline/OfflineMapsViewModel.kt` | Modify — swap DownloadScopeHolder+DownloadEngine for coordinator |
| `commonMain/.../map/offline/DownloadScopeHolder.kt` | Delete |
| `commonTest/.../map/offline/BackgroundDownloadCoordinatorTest.kt` | Create |
| `commonTest/.../map/offline/OfflineMapsViewModelTest.kt` | Modify — use FakeBackgroundDownloadCoordinator |
| `androidMain/.../map/offline/AndroidBackgroundDownloadCoordinator.kt` | Create |
| `androidMain/.../map/offline/OfflineDownloadForegroundService.kt` | Create |
| `androidMain/.../map/offline/CancelDownloadReceiver.kt` | Create |
| `androidMain/AndroidManifest.xml` | Modify — permissions + service + receiver |
| `iosMain/.../map/offline/IosDownloadObserver.kt` | Create — interface (becomes ObjC protocol) |
| `iosMain/.../map/offline/IosBackgroundDownloadCoordinator.kt` | Create |
| `iosMain/.../map/offline/IosCoordinatorProvider.kt` | Create — Swift-accessible helper |
| `iosApp/iosApp/OfflineMapActivityAttributes.swift` | Create |
| `iosApp/iosApp/DownloadActivityManager.swift` | Create |
| `iosApp/iosApp/iOSApp.swift` | Modify — BGTask registration + observer injection |
| `iosApp/iosApp/Info.plist` | Modify — BGTaskSchedulerPermittedIdentifiers + NSSupportsLiveActivities |
| `iosApp/iosApp.xcodeproj/project.pbxproj` | Modify — deployment target 15.3 → 26.0 |

Package prefix for all Kotlin files: `com.jordankurtz.piawaremobile`

---

## Task 1: BackgroundDownloadCoordinator Interface + BaseDownloadCoordinator

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/BackgroundDownloadCoordinator.kt`
- Create: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/BaseDownloadCoordinator.kt`
- Create: `composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/map/offline/BackgroundDownloadCoordinatorTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `BackgroundDownloadCoordinatorTest.kt`:

```kotlin
package com.jordankurtz.piawaremobile.map.offline

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
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

    private val region = OfflineRegion(
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
            every { engine.download(any(), any()) } returns flowOf(
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
            every { engine.download(any(), any()) } returns flowOf(
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
            every { engine.download(any(), any()) } returns flow {
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

    // Test double
    class TestCoordinator(
        engine: DownloadEngine,
        store: OfflineTileStore,
        dispatcher: CoroutineDispatcher,
    ) : BaseDownloadCoordinator(engine, store, dispatcher) {
        val platformEvents = mutableListOf<String>()

        override fun onStartPlatform(regionName: String) { platformEvents.add("start:$regionName") }
        override fun onProgressPlatform(downloaded: Long, total: Long) { platformEvents.add("progress:$downloaded/$total") }
        override fun onCompletePlatform(regionName: String) { platformEvents.add("complete:$regionName") }
        override fun onFailedPlatform(regionName: String) { platformEvents.add("failed:$regionName") }
        override fun onCancelledPlatform() { platformEvents.add("cancelled") }
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

```
./gradlew :composeApp:testDebugUnitTest --tests "*.BackgroundDownloadCoordinatorTest" 2>&1 | tail -20
```

Expected: compilation error — `BackgroundDownloadCoordinator`, `BaseDownloadCoordinator` do not exist.

- [ ] **Step 3: Create BackgroundDownloadCoordinator.kt**

```kotlin
package com.jordankurtz.piawaremobile.map.offline

import kotlinx.coroutines.flow.StateFlow

interface BackgroundDownloadCoordinator {
    val progress: StateFlow<DownloadProgress?>
    val isDownloading: StateFlow<Boolean>

    fun start(region: OfflineRegion, config: TileProviderConfig)

    fun cancel()
}
```

- [ ] **Step 4: Create BaseDownloadCoordinator.kt**

```kotlin
package com.jordankurtz.piawaremobile.map.offline

import com.jordankurtz.logger.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

abstract class BaseDownloadCoordinator(
    private val engine: DownloadEngine,
    protected val store: OfflineTileStore,
    ioDispatcher: CoroutineDispatcher,
) : BackgroundDownloadCoordinator {

    protected val scope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val _progress = MutableStateFlow<DownloadProgress?>(null)
    override val progress: StateFlow<DownloadProgress?> = _progress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    override val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private var downloadJob: Job? = null

    override fun start(region: OfflineRegion, config: TileProviderConfig) {
        if (!_isDownloading.compareAndSet(expect = false, update = true)) return
        onStartPlatform(region.name)
        downloadJob = scope.launch { executeDownload(region, config) }
    }

    override fun cancel() {
        downloadJob?.cancel()
    }

    protected open fun onStartPlatform(regionName: String) {}
    protected open fun onProgressPlatform(downloaded: Long, total: Long) {}
    protected open fun onCompletePlatform(regionName: String) {}
    protected open fun onFailedPlatform(regionName: String) {}
    protected open fun onCancelledPlatform() {}

    private suspend fun executeDownload(region: OfflineRegion, config: TileProviderConfig) {
        var lastDownloaded = region.downloadedTileCount
        var lastTotal = region.tileCount
        try {
            store.updateDownloadStatus(region.id, DownloadStatus.DOWNLOADING, 0L)
            engine.download(region, config).collect { progress ->
                lastDownloaded = progress.downloaded
                lastTotal = progress.total
                store.updateDownloadStatus(region.id, DownloadStatus.DOWNLOADING, progress.downloaded)
                _progress.value = progress
                onProgressPlatform(progress.downloaded, progress.total)
                yield()
            }
            onCompletePlatform(region.name)
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                store.updateRegionStats(region.id, lastTotal, 0L)
                store.updateDownloadStatus(region.id, DownloadStatus.PARTIAL, lastDownloaded)
                onCancelledPlatform()
            }
            throw e
        } catch (e: Exception) {
            Logger.e("Download failed for region ${region.id}", e)
            onFailedPlatform(region.name)
        } finally {
            _isDownloading.value = false
            _progress.value = null
            downloadJob = null
        }
    }
}
```

- [ ] **Step 5: Run the tests to verify they pass**

```
./gradlew :composeApp:testDebugUnitTest --tests "*.BackgroundDownloadCoordinatorTest" 2>&1 | tail -20
```

Expected: all 7 tests pass.

- [ ] **Step 6: Run ktlint and detekt**

```
./gradlew ktlintFormat && ./gradlew ktlintCheck && ./gradlew detekt 2>&1 | tail -20
```

Expected: no errors.

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/BackgroundDownloadCoordinator.kt \
        composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/BaseDownloadCoordinator.kt \
        composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/map/offline/BackgroundDownloadCoordinatorTest.kt
git commit -m "Add BackgroundDownloadCoordinator interface and BaseDownloadCoordinator"
```

---

## Task 2: Refactor OfflineMapsViewModel

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/OfflineMapsViewModel.kt`
- Delete: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/DownloadScopeHolder.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/map/offline/OfflineMapsViewModelTest.kt`

- [ ] **Step 1: Replace OfflineMapsViewModelTest.kt**

The existing tests used `DownloadScopeHolder` + `DownloadEngine` directly. Replace the whole file — cancellation/PARTIAL logic is now tested in `BackgroundDownloadCoordinatorTest`, so those tests are removed from the ViewModel test. The ViewModel tests now verify delegation and region-list observation.

```kotlin
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

    private val savedRegion = OfflineRegion(
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

        override fun start(region: OfflineRegion, config: TileProviderConfig) {
            startCalls.add(StartCall(region, config))
            _isDownloading.value = true
        }

        override fun cancel() {
            cancelCalled = true
            _isDownloading.value = false
        }
    }
}
```

- [ ] **Step 2: Run tests to see which ones fail (expected: constructor mismatch)**

```
./gradlew :composeApp:testDebugUnitTest --tests "*.OfflineMapsViewModelTest" 2>&1 | tail -20
```

Expected: compilation errors because ViewModel still has old constructor.

- [ ] **Step 3: Rewrite OfflineMapsViewModel.kt**

```kotlin
package com.jordankurtz.piawaremobile.map.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.map.cache.TileCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import kotlin.time.Clock

@Factory
class OfflineMapsViewModel(
    private val store: OfflineTileStore,
    private val coordinator: BackgroundDownloadCoordinator,
    private val tileCache: TileCache,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _regions = MutableStateFlow<List<OfflineRegion>>(emptyList())
    val regions: StateFlow<List<OfflineRegion>> = _regions.asStateFlow()

    val downloadProgress: StateFlow<DownloadProgress?> = coordinator.progress
    val isDownloading: StateFlow<Boolean> = coordinator.isDownloading

    private val _pendingDeleteRegion = MutableStateFlow<OfflineRegion?>(null)
    val pendingDeleteRegion: StateFlow<OfflineRegion?> = _pendingDeleteRegion.asStateFlow()

    private val _pendingDeleteFreedBytes = MutableStateFlow(0L)
    val pendingDeleteFreedBytes: StateFlow<Long> = _pendingDeleteFreedBytes.asStateFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            store.resetStuckDownloads()
            _regions.value = store.getRegions()
        }
        viewModelScope.launch {
            coordinator.progress.collect { progress ->
                if (progress != null) {
                    _regions.value = _regions.value.map { r ->
                        if (r.id == progress.regionId) {
                            r.copy(downloadedTileCount = progress.downloaded, tileCount = progress.total)
                        } else {
                            r
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            var wasDownloading = false
            coordinator.isDownloading.collect { downloading ->
                if (wasDownloading && !downloading) {
                    withContext(ioDispatcher) {
                        _regions.value = store.getRegions()
                    }
                }
                wasDownloading = downloading
            }
        }
    }

    fun cancelDownload() {
        coordinator.cancel()
    }

    fun requestDeleteRegion(region: OfflineRegion) {
        if (region.status == DownloadStatus.DOWNLOADING) return
        _pendingDeleteRegion.value = region
        viewModelScope.launch(ioDispatcher) {
            _pendingDeleteFreedBytes.value = store.getFreedBytesForRegion(region.id)
        }
    }

    fun cancelDelete() {
        _pendingDeleteRegion.value = null
        _pendingDeleteFreedBytes.value = 0L
    }

    fun confirmDelete() {
        val region = _pendingDeleteRegion.value ?: return
        _pendingDeleteRegion.value = null
        viewModelScope.launch(ioDispatcher) {
            val exclusiveTiles = store.getExclusiveTilesForRegion(region.id)
            for ((zoom, col, row) in exclusiveTiles) {
                tileCache.delete(zoom, col, row)
            }
            store.deleteRegion(region.id)
            _regions.value = store.getRegions()
        }
    }

    fun startDownload(name: String, bounds: BoundingBox, minZoom: Int, maxZoom: Int) {
        if (coordinator.isDownloading.value) return
        viewModelScope.launch(ioDispatcher) {
            val region = OfflineRegion(
                name = name,
                minZoom = minZoom,
                maxZoom = maxZoom,
                minLat = bounds.minLat,
                maxLat = bounds.maxLat,
                minLon = bounds.minLon,
                maxLon = bounds.maxLon,
                providerId = TileProviders.OPENSTREETMAP.id,
                createdAt = Clock.System.now().toEpochMilliseconds(),
            )
            val regionId = store.saveRegion(region)
            val savedRegion = region.copy(id = regionId, status = DownloadStatus.DOWNLOADING)
            _regions.value = _regions.value + savedRegion
            coordinator.start(savedRegion, TileProviders.OPENSTREETMAP)
        }
    }

    fun retryDownload(region: OfflineRegion) {
        if (coordinator.isDownloading.value) return
        viewModelScope.launch(ioDispatcher) {
            _regions.value = _regions.value.map { r ->
                if (r.id == region.id) r.copy(status = DownloadStatus.DOWNLOADING) else r
            }
            coordinator.start(region, TileProviders.OPENSTREETMAP)
        }
    }
}
```

- [ ] **Step 4: Delete DownloadScopeHolder.kt**

```bash
git rm composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/DownloadScopeHolder.kt
```

- [ ] **Step 5: Run all unit tests**

```
./gradlew :composeApp:testDebugUnitTest 2>&1 | tail -30
```

Expected: all tests pass.

- [ ] **Step 6: Run ktlint and detekt**

```
./gradlew ktlintFormat && ./gradlew ktlintCheck && ./gradlew detekt 2>&1 | tail -20
```

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/OfflineMapsViewModel.kt \
        composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/map/offline/OfflineMapsViewModelTest.kt
git commit -m "Refactor OfflineMapsViewModel to use BackgroundDownloadCoordinator

Remove DownloadScopeHolder and DownloadEngine from ViewModel; delegate
download lifecycle to BackgroundDownloadCoordinator. Cancellation and
PARTIAL status logic now lives in BaseDownloadCoordinator."
```

---

## Task 3: Android Foreground Service + Coordinator

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/jordankurtz/piawaremobile/map/offline/AndroidBackgroundDownloadCoordinator.kt`
- Create: `composeApp/src/androidMain/kotlin/com/jordankurtz/piawaremobile/map/offline/OfflineDownloadForegroundService.kt`
- Create: `composeApp/src/androidMain/kotlin/com/jordankurtz/piawaremobile/map/offline/CancelDownloadReceiver.kt`
- Modify: `composeApp/src/androidMain/AndroidManifest.xml`

- [ ] **Step 1: Update AndroidManifest.xml**

Add after the existing `<uses-permission>` lines and inside `<application>`:

```xml
<!-- after existing permissions -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

```xml
<!-- inside <application>, after the <activity> block -->
<service
    android:name=".map.offline.OfflineDownloadForegroundService"
    android:foregroundServiceType="dataSync"
    android:exported="false" />

<receiver
    android:name=".map.offline.CancelDownloadReceiver"
    android:exported="false" />
```

- [ ] **Step 2: Create CancelDownloadReceiver.kt**

```kotlin
package com.jordankurtz.piawaremobile.map.offline

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CancelDownloadReceiver : BroadcastReceiver(), KoinComponent {
    private val coordinator: BackgroundDownloadCoordinator by inject()

    override fun onReceive(context: Context, intent: Intent) {
        coordinator.cancel()
    }
}
```

- [ ] **Step 3: Create OfflineDownloadForegroundService.kt**

```kotlin
package com.jordankurtz.piawaremobile.map.offline

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jordankurtz.piawaremobile.MainActivity
import com.jordankurtz.piawaremobile.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class OfflineDownloadForegroundService : Service() {

    private val coordinator: BackgroundDownloadCoordinator by inject()
    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val name = intent.getStringExtra(EXTRA_REGION_NAME) ?: "Map region"
                startForeground(NOTIFICATION_ID, buildProgressNotification(name, 0L, 0L))
                progressJob = serviceScope.launch {
                    coordinator.progress.collect { progress ->
                        if (progress != null) {
                            notificationManager.notify(
                                NOTIFICATION_ID,
                                buildProgressNotification(name, progress.downloaded, progress.total),
                            )
                        }
                    }
                }
            }
            ACTION_COMPLETE -> {
                val name = intent.getStringExtra(EXTRA_REGION_NAME) ?: "Map region"
                stopProgress()
                notificationManager.notify(COMPLETE_NOTIFICATION_ID, buildCompleteNotification(name))
                stopSelf()
            }
            ACTION_FAILED -> {
                val name = intent.getStringExtra(EXTRA_REGION_NAME) ?: "Map region"
                stopProgress()
                notificationManager.notify(COMPLETE_NOTIFICATION_ID, buildFailedNotification(name))
                stopSelf()
            }
            ACTION_CANCELLED -> {
                stopProgress()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun stopProgress() {
        progressJob?.cancel()
        progressJob = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Offline Map Downloads",
                NotificationManager.IMPORTANCE_LOW,
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildProgressNotification(name: String, downloaded: Long, total: Long) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_plane)
            .setContentTitle("Downloading $name")
            .setContentText(if (total > 0L) "$downloaded / $total tiles" else "Starting download\u2026")
            .setProgress(
                total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                downloaded.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                total == 0L,
            )
            .setOngoing(true)
            .addAction(
                0,
                "Cancel",
                PendingIntent.getBroadcast(
                    this,
                    0,
                    Intent(this, CancelDownloadReceiver::class.java),
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()

    private fun buildCompleteNotification(name: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_plane)
            .setContentTitle("$name downloaded")
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()

    private fun buildFailedNotification(name: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_plane)
            .setContentTitle("$name download failed")
            .setContentText("Tap to retry")
            .setContentIntent(openAppIntent())
            .build()

    private fun openAppIntent() =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    companion object {
        const val ACTION_START = "com.jordankurtz.piawaremobile.action.DOWNLOAD_START"
        const val ACTION_COMPLETE = "com.jordankurtz.piawaremobile.action.DOWNLOAD_COMPLETE"
        const val ACTION_FAILED = "com.jordankurtz.piawaremobile.action.DOWNLOAD_FAILED"
        const val ACTION_CANCELLED = "com.jordankurtz.piawaremobile.action.DOWNLOAD_CANCELLED"
        const val EXTRA_REGION_NAME = "region_name"
        const val CHANNEL_ID = "offline_map_downloads"
        const val NOTIFICATION_ID = 1001
        const val COMPLETE_NOTIFICATION_ID = 1002
    }
}
```

- [ ] **Step 4: Create AndroidBackgroundDownloadCoordinator.kt**

```kotlin
package com.jordankurtz.piawaremobile.map.offline

import android.content.Intent
import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.di.modules.ContextWrapper
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Single

@Single(binds = [BackgroundDownloadCoordinator::class])
class AndroidBackgroundDownloadCoordinator(
    private val contextWrapper: ContextWrapper,
    engine: DownloadEngine,
    store: OfflineTileStore,
    @IODispatcher ioDispatcher: CoroutineDispatcher,
) : BaseDownloadCoordinator(engine, store, ioDispatcher) {

    override fun onStartPlatform(regionName: String) {
        contextWrapper.context.startForegroundService(
            serviceIntent(OfflineDownloadForegroundService.ACTION_START) {
                putExtra(OfflineDownloadForegroundService.EXTRA_REGION_NAME, regionName)
            },
        )
    }

    override fun onCompletePlatform(regionName: String) {
        contextWrapper.context.startService(
            serviceIntent(OfflineDownloadForegroundService.ACTION_COMPLETE) {
                putExtra(OfflineDownloadForegroundService.EXTRA_REGION_NAME, regionName)
            },
        )
    }

    override fun onFailedPlatform(regionName: String) {
        contextWrapper.context.startService(
            serviceIntent(OfflineDownloadForegroundService.ACTION_FAILED) {
                putExtra(OfflineDownloadForegroundService.EXTRA_REGION_NAME, regionName)
            },
        )
    }

    override fun onCancelledPlatform() {
        contextWrapper.context.startService(
            serviceIntent(OfflineDownloadForegroundService.ACTION_CANCELLED),
        )
    }

    private fun serviceIntent(action: String, block: Intent.() -> Unit = {}): Intent =
        Intent(contextWrapper.context, OfflineDownloadForegroundService::class.java)
            .apply { this.action = action }
            .apply(block)
}
```

- [ ] **Step 5: Run unit + desktop tests**

```
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest 2>&1 | tail -30
```

Expected: all tests pass. (Service/coordinator are Android components not unit-tested here; the logic is covered by BackgroundDownloadCoordinatorTest.)

- [ ] **Step 6: Run ktlint and detekt**

```
./gradlew ktlintFormat && ./gradlew ktlintCheck && ./gradlew detekt 2>&1 | tail -20
```

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/jordankurtz/piawaremobile/map/offline/AndroidBackgroundDownloadCoordinator.kt \
        composeApp/src/androidMain/kotlin/com/jordankurtz/piawaremobile/map/offline/OfflineDownloadForegroundService.kt \
        composeApp/src/androidMain/kotlin/com/jordankurtz/piawaremobile/map/offline/CancelDownloadReceiver.kt \
        composeApp/src/androidMain/AndroidManifest.xml
git commit -m "Add Android foreground service for background tile downloads

AndroidBackgroundDownloadCoordinator starts OfflineDownloadForegroundService
on download start so downloads survive screen-off and app switching.
Notification shows live tile progress with a Cancel action."
```

---

## Task 4: iOS Kotlin — IosDownloadObserver + IosBackgroundDownloadCoordinator

**Files:**
- Create: `composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/map/offline/IosDownloadObserver.kt`
- Create: `composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/map/offline/IosBackgroundDownloadCoordinator.kt`
- Create: `composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/map/offline/IosCoordinatorProvider.kt`

- [ ] **Step 1: Create IosDownloadObserver.kt**

This interface compiles to an ObjC protocol that Swift implements.

```kotlin
package com.jordankurtz.piawaremobile.map.offline

interface IosDownloadObserver {
    fun onDownloadStarting(regionName: String)

    fun onProgress(
        downloaded: Long,
        total: Long,
    )

    fun onComplete(regionName: String)

    fun onFailed(regionName: String)

    fun onCancelled()
}
```

- [ ] **Step 2: Create IosBackgroundDownloadCoordinator.kt**

```kotlin
package com.jordankurtz.piawaremobile.map.offline

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Single

@Single(binds = [BackgroundDownloadCoordinator::class])
class IosBackgroundDownloadCoordinator(
    engine: DownloadEngine,
    store: OfflineTileStore,
    @IODispatcher ioDispatcher: CoroutineDispatcher,
) : BaseDownloadCoordinator(engine, store, ioDispatcher) {

    var observer: IosDownloadObserver? = null

    override fun onStartPlatform(regionName: String) {
        observer?.onDownloadStarting(regionName)
    }

    override fun onProgressPlatform(downloaded: Long, total: Long) {
        observer?.onProgress(downloaded, total)
    }

    override fun onCompletePlatform(regionName: String) {
        observer?.onComplete(regionName)
    }

    override fun onFailedPlatform(regionName: String) {
        observer?.onFailed(regionName)
    }

    override fun onCancelledPlatform() {
        observer?.onCancelled()
    }
}
```

- [ ] **Step 3: Create IosCoordinatorProvider.kt**

Top-level function accessible from Swift as `IosCoordinatorProviderKt.getIosBackgroundDownloadCoordinator()`.

```kotlin
package com.jordankurtz.piawaremobile.map.offline

import org.koin.mp.KoinPlatform

fun getIosBackgroundDownloadCoordinator(): IosBackgroundDownloadCoordinator =
    KoinPlatform.getKoin().get()
```

- [ ] **Step 4: Run unit + desktop tests**

```
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest 2>&1 | tail -20
```

Expected: all tests pass.

- [ ] **Step 5: Run ktlint and detekt**

```
./gradlew ktlintFormat && ./gradlew ktlintCheck && ./gradlew detekt 2>&1 | tail -20
```

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/map/offline/IosDownloadObserver.kt \
        composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/map/offline/IosBackgroundDownloadCoordinator.kt \
        composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/map/offline/IosCoordinatorProvider.kt
git commit -m "Add IosBackgroundDownloadCoordinator with IosDownloadObserver protocol"
```

---

## Task 5: iOS Swift — Live Activity, BGContinuedProcessingTask, App Entrypoint

**Files:**
- Create: `iosApp/iosApp/OfflineMapActivityAttributes.swift`
- Create: `iosApp/iosApp/DownloadActivityManager.swift`
- Modify: `iosApp/iosApp/iOSApp.swift`
- Modify: `iosApp/iosApp/Info.plist`
- Modify: `iosApp/iosApp.xcodeproj/project.pbxproj` (deployment target)

- [ ] **Step 1: Bump iOS deployment target to 26.0 in project.pbxproj**

In `iosApp/iosApp.xcodeproj/project.pbxproj`, replace all occurrences of:
```
IPHONEOS_DEPLOYMENT_TARGET = 15.3;
```
with:
```
IPHONEOS_DEPLOYMENT_TARGET = 26.0;
```

There are 4 occurrences (2 build configurations × 2 targets). Replace all.

- [ ] **Step 2: Update Info.plist**

Add these keys inside the root `<dict>` in `iosApp/iosApp/Info.plist`:

```xml
<key>BGTaskSchedulerPermittedIdentifiers</key>
<array>
    <string>com.jordankurtz.piawaremobile.offlinedownload</string>
</array>
<key>NSSupportsLiveActivities</key>
<true/>
```

- [ ] **Step 3: Create OfflineMapActivityAttributes.swift**

```swift
import ActivityKit
import Foundation

struct OfflineMapActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        var downloaded: Int
        var total: Int
    }

    var regionName: String
}
```

- [ ] **Step 4: Create DownloadActivityManager.swift**

```swift
import ActivityKit
import BackgroundTasks
import ComposeApp

class DownloadActivityManager: IosDownloadObserver {
    private var activity: Activity<OfflineMapActivityAttributes>?
    private var currentTask: BGContinuedProcessingTask?
    private var lastTotal: Int = 0

    func setCurrentTask(_ task: BGContinuedProcessingTask) {
        currentTask = task
    }

    func onDownloadStarting(regionName: String) {
        let request = BGContinuedProcessingTaskRequest(
            identifier: "com.jordankurtz.piawaremobile.offlinedownload"
        )
        try? BGTaskScheduler.shared.submit(request)

        let attributes = OfflineMapActivityAttributes(regionName: regionName)
        let contentState = OfflineMapActivityAttributes.ContentState(downloaded: 0, total: 0)
        activity = try? Activity.request(attributes: attributes, contentState: contentState, pushType: nil)
    }

    func onProgress(downloaded: Int64, total: Int64) {
        lastTotal = Int(total)
        let state = OfflineMapActivityAttributes.ContentState(
            downloaded: Int(downloaded),
            total: Int(total)
        )
        Task { await activity?.update(using: state) }
    }

    func onComplete(regionName: String) {
        let finalState = OfflineMapActivityAttributes.ContentState(
            downloaded: lastTotal,
            total: lastTotal
        )
        let task = currentTask
        currentTask = nil
        Task {
            await activity?.end(using: finalState, dismissalPolicy: .after(.now + 5))
            activity = nil
            task?.setTaskCompleted(success: true)
        }
    }

    func onFailed(regionName: String) {
        endActivity(success: false)
    }

    func onCancelled() {
        endActivity(success: false)
    }

    private func endActivity(success: Bool) {
        let task = currentTask
        currentTask = nil
        Task {
            await activity?.end(using: nil, dismissalPolicy: .immediate)
            activity = nil
            task?.setTaskCompleted(success: success)
        }
    }
}
```

- [ ] **Step 5: Update iOSApp.swift**

Replace the entire file:

```swift
import BackgroundTasks
import SwiftUI

@main
struct iOSApp: App {
    private let activityManager = DownloadActivityManager()

    init() {
        // BGTask handler must be registered before app finishes launching.
        // The expirationHandler runs lazily (only when the task expires while
        // backgrounded), so accessing the coordinator there is safe — Koin is
        // already started by the time the task can expire.
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: "com.jordankurtz.piawaremobile.offlinedownload",
            using: nil
        ) { [activityManager] task in
            guard let task = task as? BGContinuedProcessingTask else { return }
            activityManager.setCurrentTask(task)
            task.expirationHandler = {
                IosCoordinatorProviderKt.getIosBackgroundDownloadCoordinator().cancel()
            }
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onAppear {
                    // Inject the Swift observer after Koin is initialised
                    // (Koin starts inside MainViewController which runs on first appearance).
                    IosCoordinatorProviderKt
                        .getIosBackgroundDownloadCoordinator()
                        .observer = activityManager
                }
        }
    }
}
```

- [ ] **Step 6: Build the iOS target to verify compilation**

Open Xcode and build for a simulator, OR use xcodebuild:

```bash
cd iosApp && xcodebuild -scheme iosApp -destination "platform=iOS Simulator,name=iPhone 16" build 2>&1 | grep -E "error:|warning:|BUILD" | tail -30
```

Expected: `BUILD SUCCEEDED` with no errors.

- [ ] **Step 7: Run all Kotlin tests one final time**

```
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest 2>&1 | tail -30
```

Expected: all tests pass.

- [ ] **Step 8: Commit**

```bash
git add iosApp/iosApp/OfflineMapActivityAttributes.swift \
        iosApp/iosApp/DownloadActivityManager.swift \
        iosApp/iosApp/iOSApp.swift \
        iosApp/iosApp/Info.plist \
        iosApp/iosApp.xcodeproj/project.pbxproj
git commit -m "Add iOS Live Activity and BGContinuedProcessingTask for background downloads

Bump deployment target to iOS 26.0. DownloadActivityManager implements the
IosDownloadObserver protocol to manage a Live Activity (lock screen +
Dynamic Island) and mark BGContinuedProcessingTask complete when the
download finishes, fails, or is cancelled."
```

---

## Final rebase

Once all tasks are complete, squash the task commits into logical units if needed:

```bash
git rebase -i HEAD~5
```

Suggested final commits:
1. `Add BackgroundDownloadCoordinator interface and BaseDownloadCoordinator` (Task 1)
2. `Refactor OfflineMapsViewModel to use BackgroundDownloadCoordinator` (Task 2)
3. `Add Android foreground service for background tile downloads` (Task 3)
4. `Add iOS background download support with Live Activity` (Tasks 4 + 5)
