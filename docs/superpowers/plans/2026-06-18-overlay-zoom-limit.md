# Overlay Zoom Limit Setting — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a "Limit zoom to overlay" setting that, when on, clamps the map zoom to the intersection of the active overlays' native tile ranges, snapping the current zoom immediately.

**Architecture:** Extract overlay zoom range constants into a shared file so both `MapLibreMap.kt` (for `TileSetOptions`) and `MapViewModel` (for limit computation) reference the same values. Add `limitZoomToOverlay: Boolean` to `Settings` and wire it through the standard settings stack. In `MapViewModel.onSettingsLoaded`, compute the effective zoom range and snap the current zoom after every settings update.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, DataStore Preferences, Mokkery (mocking), kotlinx.coroutines test utilities (`StandardTestDispatcher`, `runTest`, `advanceUntilIdle`).

## Global Constraints

- All new code must pass `./gradlew ktlintCheck` and `./gradlew detekt`
- Unit tests live in `composeApp/src/commonTest/kotlin/...`
- Use `runTest(testDispatcher)` + `advanceUntilIdle()` for coroutine tests
- Use Mokkery (`mock()`, `everySuspend`, `verifySuspend`) for interface mocking
- No new files in `androidTest` or `desktopTest` needed (this is pure ViewModel/settings logic)

---

### Task 1: Extract overlay zoom range constants and wire into MapLibreMap

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/OverlayZoomRanges.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/MapLibreMap.kt`

**Interfaces:**
- Produces:
  - `OverlayZoomRanges.FAA_SECTIONAL: IntRange` = `8..12`
  - `OverlayZoomRanges.IFR_LOW: IntRange` = `7..12`
  - `OverlayZoomRanges.IFR_HIGH: IntRange` = `5..9`
  - `OverlayZoomRanges.AIRSPACE: IntRange` = `7..14`

- [ ] **Step 1: Create `OverlayZoomRanges.kt`**

```kotlin
package com.jordankurtz.piawaremobile.map

object OverlayZoomRanges {
    val FAA_SECTIONAL = 8..12
    val IFR_LOW = 7..12
    val IFR_HIGH = 5..9
    val AIRSPACE = 7..14
}
```

- [ ] **Step 2: Update `MapLibreMap.kt` to reference the constants**

Find the three `TileSetOptions(...)` calls for the raster overlays (around line 136, 146, 155) and replace the hardcoded integers:

```kotlin
// VFR Sectional — was: TileSetOptions(minZoom = 8, maxZoom = 12)
options = TileSetOptions(
    minZoom = OverlayZoomRanges.FAA_SECTIONAL.first,
    maxZoom = OverlayZoomRanges.FAA_SECTIONAL.last,
),

// IFR Low — was: TileSetOptions(minZoom = 7, maxZoom = 12)
options = TileSetOptions(
    minZoom = OverlayZoomRanges.IFR_LOW.first,
    maxZoom = OverlayZoomRanges.IFR_LOW.last,
),

// IFR High — was: TileSetOptions(minZoom = 5, maxZoom = 9)
options = TileSetOptions(
    minZoom = OverlayZoomRanges.IFR_HIGH.first,
    maxZoom = OverlayZoomRanges.IFR_HIGH.last,
),
```

For Airspace (the `rememberVectorSource` call around line 179):
```kotlin
options = TileSetOptions(
    minZoom = OverlayZoomRanges.AIRSPACE.first,
    maxZoom = OverlayZoomRanges.AIRSPACE.last,
),
```

- [ ] **Step 3: Run ktlint and confirm it passes**

```
./gradlew :composeApp:ktlintCheck
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/OverlayZoomRanges.kt \
        composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/MapLibreMap.kt
git commit -m "Extract overlay zoom range constants to OverlayZoomRanges"
```

---

### Task 2: Add `limitZoomToOverlay` to the settings data model and persistence layer

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/Settings.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/repo/SettingsRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/repo/SettingsRepositoryImpl.kt`

**Interfaces:**
- Produces: `Settings.limitZoomToOverlay: Boolean = false`
- Produces: `SettingsRepository.LIMIT_ZOOM_TO_OVERLAY: BooleanPreferencesKey`

- [ ] **Step 1: Add the field to `Settings.kt`**

Add after `showFaaIfrHigh`:
```kotlin
val limitZoomToOverlay: Boolean = false,
```

Full `Settings.kt` after edit:
```kotlin
data class Settings(
    val servers: List<Server> = emptyList(),
    val refreshInterval: Int = SettingsRepository.DEFAULT_REFRESH_INTERVAL,
    val centerMapOnUserOnStart: Boolean = false,
    val restoreMapStateOnStart: Boolean = false,
    val showReceiverLocations: Boolean = false,
    val showUserLocationOnMap: Boolean = false,
    val trailDisplayMode: TrailDisplayMode = TrailDisplayMode.NONE,
    val showMinimapTrails: Boolean = false,
    val openUrlsExternally: Boolean = false,
    val enableFlightAwareApi: Boolean = false,
    val flightAwareApiKey: String = "",
    val mapProviderId: String = TileProviders.DEFAULT.id,
    val defaultZoomLevel: Int = SettingsRepository.DEFAULT_ZOOM_LEVEL,
    val minZoomLevel: Int = SettingsRepository.MIN_ZOOM_LEVEL,
    val maxZoomLevel: Int = SettingsRepository.MAX_ZOOM_LEVEL,
    val apiKeys: Map<String, String> = emptyMap(),
    val customProviders: List<CustomProviderConfig> = emptyList(),
    val showFaaCharts: Boolean = false,
    val showAirspace: Boolean = false,
    val showFaaIfrLow: Boolean = false,
    val showFaaIfrHigh: Boolean = false,
    val limitZoomToOverlay: Boolean = false,
)
```

- [ ] **Step 2: Add the preference key to `SettingsRepository`**

Add after `SHOW_FAA_IFR_HIGH` in the companion object:
```kotlin
val LIMIT_ZOOM_TO_OVERLAY = booleanPreferencesKey("limitZoomToOverlay")
```

- [ ] **Step 3: Read the key in `SettingsRepositoryImpl.getSettings()`**

Add after the `showFaaIfrHigh` line inside the `Settings(...)` constructor call:
```kotlin
limitZoomToOverlay = preferences[SettingsRepository.LIMIT_ZOOM_TO_OVERLAY] ?: false,
```

- [ ] **Step 4: Write the key in `SettingsRepositoryImpl.saveSettings()`**

Add after `preferences[SettingsRepository.SHOW_FAA_IFR_HIGH] = settings.showFaaIfrHigh`:
```kotlin
preferences[SettingsRepository.LIMIT_ZOOM_TO_OVERLAY] = settings.limitZoomToOverlay
```

- [ ] **Step 5: Run unit tests to confirm nothing broke**

```
./gradlew :composeApp:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL, all existing tests green.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/Settings.kt \
        composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/repo/SettingsRepository.kt \
        composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/repo/SettingsRepositoryImpl.kt
git commit -m "Add limitZoomToOverlay field to Settings and persist to DataStore"
```

---

### Task 3: TDD `SettingsService.setLimitZoomToOverlay` and `SettingsViewModel.updateLimitZoomToOverlay`

**Files:**
- Modify: `composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/settings/SettingsServiceOverlayTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/usecase/SettingsService.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/usecase/impl/SettingsServiceImpl.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/SettingsViewModel.kt`

**Interfaces:**
- Consumes: `Settings.limitZoomToOverlay` (Task 2)
- Produces:
  - `SettingsService.setLimitZoomToOverlay(enabled: Boolean)`
  - `SettingsViewModel.updateLimitZoomToOverlay(enabled: Boolean)`

- [ ] **Step 1: Write failing tests in `SettingsServiceOverlayTest.kt`**

Add after the existing `setShowAirspace persists false` test:

```kotlin
@Test
fun `setLimitZoomToOverlay persists true`() =
    runTest(testDispatcher) {
        val slot = slot<Settings>()
        everySuspend { settingsRepository.saveSettings(capture(slot)) } returns Unit

        settingsService.setLimitZoomToOverlay(true)

        val saved = (slot.value as SlotCapture.Value.Present).value
        assertTrue(saved.limitZoomToOverlay)
    }

@Test
fun `setLimitZoomToOverlay persists false`() =
    runTest(testDispatcher) {
        everySuspend { settingsRepository.getSettings() } returns
            flowOf(Settings(limitZoomToOverlay = true))
        val slot = slot<Settings>()
        everySuspend { settingsRepository.saveSettings(capture(slot)) } returns Unit

        settingsService.setLimitZoomToOverlay(false)

        val saved = (slot.value as SlotCapture.Value.Present).value
        assertFalse(saved.limitZoomToOverlay)
    }
```

- [ ] **Step 2: Run to confirm tests fail**

```
./gradlew :composeApp:testDebugUnitTest --tests "*.SettingsServiceOverlayTest"
```

Expected: FAILED — `Unresolved reference: setLimitZoomToOverlay`

- [ ] **Step 3: Add method to `SettingsService` interface**

Add after `setShowFaaIfrHigh`:
```kotlin
suspend fun setLimitZoomToOverlay(enabled: Boolean)
```

- [ ] **Step 4: Implement in `SettingsServiceImpl`**

Add after `setShowFaaIfrHigh`:
```kotlin
override suspend fun setLimitZoomToOverlay(enabled: Boolean) =
    updateSetting { it.copy(limitZoomToOverlay = enabled) }
```

- [ ] **Step 5: Run tests to confirm they pass**

```
./gradlew :composeApp:testDebugUnitTest --tests "*.SettingsServiceOverlayTest"
```

Expected: BUILD SUCCESSFUL, all tests green.

- [ ] **Step 6: Add `updateLimitZoomToOverlay` to `SettingsViewModel`**

Add after `updateShowFaaIfrHigh`:
```kotlin
fun updateLimitZoomToOverlay(enabled: Boolean) =
    viewModelScope.launch {
        settingsService.setLimitZoomToOverlay(enabled)
    }
```

- [ ] **Step 7: Run all unit tests**

```
./gradlew :composeApp:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/settings/SettingsServiceOverlayTest.kt \
        composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/usecase/SettingsService.kt \
        composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/usecase/impl/SettingsServiceImpl.kt \
        composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/SettingsViewModel.kt
git commit -m "Add setLimitZoomToOverlay to SettingsService and SettingsViewModel"
```

---

### Task 4: TDD `MapViewModel` overlay zoom limit logic

**Files:**
- Create: `composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/map/MapViewModelOverlayZoomTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/MapViewModel.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/map/MapViewModelOverlayTest.kt`

**Interfaces:**
- Consumes:
  - `Settings.limitZoomToOverlay` (Task 2)
  - `OverlayZoomRanges.*` (Task 1)
  - `SettingsService.setLimitZoomToOverlay` (Task 3)
  - `FakeMapStateController.lastMinZoom`, `FakeMapStateController.lastMaxZoom`, `FakeMapStateController.zoom`
- Produces:
  - `MapViewModel._limitZoomToOverlay: MutableStateFlow<Boolean>`
  - `MapViewModel.limitZoomToOverlay: StateFlow<Boolean>`
  - `MapViewModel.toggleLimitZoomToOverlay()`
  - `MapViewModel.computeEffectiveZoomLimits(settings: Settings): IntRange` (private, tested via zoom limit side-effects)

- [ ] **Step 1: Update `MapViewModelOverlayTest.setUp` to mock the new method**

In the `setUp()` block, add after the existing `everySuspend` calls:
```kotlin
everySuspend { settingsService.setLimitZoomToOverlay(any()) } returns Unit
```

- [ ] **Step 2: Write failing tests in new `MapViewModelOverlayZoomTest.kt`**

Create the file:

```kotlin
package com.jordankurtz.piawaremobile.map

import com.jordankurtz.piawaremobile.map.debug.TileCacheStatsTracker
import com.jordankurtz.piawaremobile.map.usecase.GetSavedMapStateUseCase
import com.jordankurtz.piawaremobile.map.usecase.SaveMapStateUseCase
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.MapState
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SettingsService
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelOverlayZoomTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var settingsService: SettingsService
    private lateinit var loadSettingsUseCase: LoadSettingsUseCase
    private lateinit var getSavedMapStateUseCase: GetSavedMapStateUseCase
    private lateinit var saveMapStateUseCase: SaveMapStateUseCase

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsService = mock()
        loadSettingsUseCase = mock()
        getSavedMapStateUseCase = mock()
        saveMapStateUseCase = mock()
        everySuspend { getSavedMapStateUseCase.invoke() } returns MapState(0.0, 0.0, 8.0)
        everySuspend { saveMapStateUseCase.invoke(any(), any(), any()) } returns Unit
        everySuspend { settingsService.setShowFaaCharts(any()) } returns Unit
        everySuspend { settingsService.setShowAirspace(any()) } returns Unit
        everySuspend { settingsService.setShowFaaIfrLow(any()) } returns Unit
        everySuspend { settingsService.setShowFaaIfrHigh(any()) } returns Unit
        everySuspend { settingsService.setLimitZoomToOverlay(any()) } returns Unit
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        settings: Settings = Settings(),
        initialZoom: Double = 5.0,
    ): Pair<MapViewModel, FakeMapStateController> {
        every { loadSettingsUseCase.invoke() } returns flowOf(Async.Success(settings))
        val controller = FakeMapStateController()
        controller.zoom = initialZoom
        val vm = MapViewModel(
            providerConfigFlow = MutableStateFlow(TileProviders.DEFAULT),
            getSavedMapStateUseCase = getSavedMapStateUseCase,
            saveMapStateUseCase = saveMapStateUseCase,
            loadSettingsUseCase = loadSettingsUseCase,
            settingsService = settingsService,
            tileCacheStatsTracker = TileCacheStatsTracker(),
            mapStateController = controller,
        )
        return vm to controller
    }

    // --- limitZoomToOverlay state flow ---

    @Test
    fun `limitZoomToOverlay defaults to false`() =
        runTest(testDispatcher) {
            val (vm, _) = createViewModel()
            advanceUntilIdle()
            assertFalse(vm.limitZoomToOverlay.value)
        }

    @Test
    fun `limitZoomToOverlay reflects settings value true`() =
        runTest(testDispatcher) {
            val (vm, _) = createViewModel(Settings(limitZoomToOverlay = true))
            advanceUntilIdle()
            assertTrue(vm.limitZoomToOverlay.value)
        }

    @Test
    fun `toggleLimitZoomToOverlay calls setLimitZoomToOverlay with toggled value`() =
        runTest(testDispatcher) {
            val (vm, _) = createViewModel(Settings(limitZoomToOverlay = false))
            advanceUntilIdle()
            vm.toggleLimitZoomToOverlay()
            advanceUntilIdle()
            dev.mokkery.verifySuspend { settingsService.setLimitZoomToOverlay(true) }
        }

    // --- effective zoom limits when setting is off ---

    @Test
    fun `zoom limits use global min-max when limitZoomToOverlay is false`() =
        runTest(testDispatcher) {
            val (_, controller) = createViewModel(
                Settings(
                    limitZoomToOverlay = false,
                    minZoomLevel = 2,
                    maxZoomLevel = 14,
                    showFaaIfrHigh = true,
                ),
            )
            advanceUntilIdle()
            assertEquals(2.0, controller.lastMinZoom)
            assertEquals(14.0, controller.lastMaxZoom)
        }

    // --- effective zoom limits: single overlay ---

    @Test
    fun `zoom limits clamp to IFR High range when only IFR High active`() =
        runTest(testDispatcher) {
            val (_, controller) = createViewModel(
                Settings(
                    limitZoomToOverlay = true,
                    minZoomLevel = 1,
                    maxZoomLevel = 16,
                    showFaaIfrHigh = true,
                ),
            )
            advanceUntilIdle()
            // IFR_HIGH = 5..9, global = 1..16 → effective = 5..9
            assertEquals(5.0, controller.lastMinZoom)
            assertEquals(9.0, controller.lastMaxZoom)
        }

    @Test
    fun `zoom limits clamp to IFR Low range when only IFR Low active`() =
        runTest(testDispatcher) {
            val (_, controller) = createViewModel(
                Settings(
                    limitZoomToOverlay = true,
                    minZoomLevel = 1,
                    maxZoomLevel = 16,
                    showFaaIfrLow = true,
                ),
            )
            advanceUntilIdle()
            // IFR_LOW = 7..12
            assertEquals(7.0, controller.lastMinZoom)
            assertEquals(12.0, controller.lastMaxZoom)
        }

    @Test
    fun `zoom limits clamp to sectional range when only sectional active`() =
        runTest(testDispatcher) {
            val (_, controller) = createViewModel(
                Settings(
                    limitZoomToOverlay = true,
                    minZoomLevel = 1,
                    maxZoomLevel = 16,
                    showFaaCharts = true,
                ),
            )
            advanceUntilIdle()
            // FAA_SECTIONAL = 8..12
            assertEquals(8.0, controller.lastMinZoom)
            assertEquals(12.0, controller.lastMaxZoom)
        }

    @Test
    fun `zoom limits clamp to airspace range when only airspace active`() =
        runTest(testDispatcher) {
            val (_, controller) = createViewModel(
                Settings(
                    limitZoomToOverlay = true,
                    minZoomLevel = 1,
                    maxZoomLevel = 16,
                    showAirspace = true,
                ),
            )
            advanceUntilIdle()
            // AIRSPACE = 7..14
            assertEquals(7.0, controller.lastMinZoom)
            assertEquals(14.0, controller.lastMaxZoom)
        }

    // --- effective zoom limits: multiple overlays ---

    @Test
    fun `zoom limits intersect IFR High and IFR Low`() =
        runTest(testDispatcher) {
            val (_, controller) = createViewModel(
                Settings(
                    limitZoomToOverlay = true,
                    minZoomLevel = 1,
                    maxZoomLevel = 16,
                    showFaaIfrHigh = true,
                    showFaaIfrLow = true,
                ),
            )
            advanceUntilIdle()
            // IFR_HIGH=5..9, IFR_LOW=7..12 → intersection = 7..9
            assertEquals(7.0, controller.lastMinZoom)
            assertEquals(9.0, controller.lastMaxZoom)
        }

    // --- no active overlays ---

    @Test
    fun `zoom limits fall back to global when limitZoomToOverlay is true but no overlay active`() =
        runTest(testDispatcher) {
            val (_, controller) = createViewModel(
                Settings(
                    limitZoomToOverlay = true,
                    minZoomLevel = 3,
                    maxZoomLevel = 15,
                ),
            )
            advanceUntilIdle()
            assertEquals(3.0, controller.lastMinZoom)
            assertEquals(15.0, controller.lastMaxZoom)
        }

    // --- global min/max constrains overlay range ---

    @Test
    fun `effective limits are further bounded by global min and max`() =
        runTest(testDispatcher) {
            val (_, controller) = createViewModel(
                Settings(
                    limitZoomToOverlay = true,
                    minZoomLevel = 7,
                    maxZoomLevel = 8,  // global max narrows IFR High's 5..9
                    showFaaIfrHigh = true,
                ),
            )
            advanceUntilIdle()
            // IFR_HIGH=5..9 ∩ global=7..8 → 7..8
            assertEquals(7.0, controller.lastMinZoom)
            assertEquals(8.0, controller.lastMaxZoom)
        }

    // --- zoom snap ---

    @Test
    fun `zoom is snapped down to effective max when current zoom is above range`() =
        runTest(testDispatcher) {
            val (_, controller) = createViewModel(
                settings = Settings(
                    limitZoomToOverlay = true,
                    minZoomLevel = 1,
                    maxZoomLevel = 16,
                    showFaaIfrHigh = true,  // range 5..9
                ),
                initialZoom = 13.0,  // above IFR High max of 9
            )
            advanceUntilIdle()
            assertEquals(9.0, controller.zoom)
        }

    @Test
    fun `zoom is snapped up to effective min when current zoom is below range`() =
        runTest(testDispatcher) {
            val (_, controller) = createViewModel(
                settings = Settings(
                    limitZoomToOverlay = true,
                    minZoomLevel = 1,
                    maxZoomLevel = 16,
                    showFaaIfrHigh = true,  // range 5..9
                ),
                initialZoom = 3.0,  // below IFR High min of 5
            )
            advanceUntilIdle()
            assertEquals(5.0, controller.zoom)
        }

    @Test
    fun `zoom is not changed when already within effective range`() =
        runTest(testDispatcher) {
            val (_, controller) = createViewModel(
                settings = Settings(
                    limitZoomToOverlay = true,
                    minZoomLevel = 1,
                    maxZoomLevel = 16,
                    showFaaIfrHigh = true,  // range 5..9
                ),
                initialZoom = 7.0,
            )
            advanceUntilIdle()
            assertEquals(7.0, controller.zoom)
        }
}
```

- [ ] **Step 3: Run to confirm tests fail**

```
./gradlew :composeApp:testDebugUnitTest --tests "*.MapViewModelOverlayZoomTest"
```

Expected: FAILED — `Unresolved reference: limitZoomToOverlay` on `MapViewModel`

- [ ] **Step 4: Add `_limitZoomToOverlay` state flow and `toggleLimitZoomToOverlay` to `MapViewModel`**

Add these fields after `_showFaaIfrHigh`:
```kotlin
private val _limitZoomToOverlay = MutableStateFlow(false)
val limitZoomToOverlay: StateFlow<Boolean> = _limitZoomToOverlay
```

Add this function after `toggleFaaIfrHigh`:
```kotlin
fun toggleLimitZoomToOverlay() {
    viewModelScope.launch { settingsService.setLimitZoomToOverlay(!_limitZoomToOverlay.value) }
}
```

Add assignment in `onSettingsLoaded` after `_showFaaIfrHigh.value = settings.showFaaIfrHigh`:
```kotlin
_limitZoomToOverlay.value = settings.limitZoomToOverlay
```

- [ ] **Step 5: Add `computeEffectiveZoomLimits` private function to `MapViewModel`**

Add this function at the bottom of `MapViewModel`, before the closing `}`:

```kotlin
private fun computeEffectiveZoomLimits(settings: Settings): IntRange {
    if (!settings.limitZoomToOverlay) return settings.minZoomLevel..settings.maxZoomLevel

    val activeRanges =
        buildList {
            if (settings.showFaaCharts) add(OverlayZoomRanges.FAA_SECTIONAL)
            if (settings.showFaaIfrLow) add(OverlayZoomRanges.IFR_LOW)
            if (settings.showFaaIfrHigh) add(OverlayZoomRanges.IFR_HIGH)
            if (settings.showAirspace) add(OverlayZoomRanges.AIRSPACE)
        }

    if (activeRanges.isEmpty()) return settings.minZoomLevel..settings.maxZoomLevel

    val intersectionMin = activeRanges.maxOf { it.first }
    val intersectionMax = activeRanges.minOf { it.last }

    if (intersectionMin > intersectionMax) return settings.minZoomLevel..settings.maxZoomLevel

    return intersectionMin.coerceAtLeast(settings.minZoomLevel)..
        intersectionMax.coerceAtMost(settings.maxZoomLevel)
}
```

- [ ] **Step 6: Apply effective limits in `onSettingsLoaded`**

In `onSettingsLoaded`, replace the existing `mapStateController.setZoomLimits(...)` call and add the snap at the end of the function. The block currently looks like:

```kotlin
mapStateController.setZoomLimits(
    settings.minZoomLevel.toDouble(),
    settings.maxZoomLevel.toDouble(),
)
saveStateJob?.cancel()
if (settings.restoreMapStateOnStart) {
    startSaveMapStateJob()
}
if (!cameraInitialized) {
    cameraInitialized = true
    if (settings.restoreMapStateOnStart) {
        loadMapState(settings.minZoomLevel, settings.maxZoomLevel)
    } else {
        mapStateController.zoom = settings.defaultZoomLevel.toDouble()
    }
}
```

Replace it with:

```kotlin
val effectiveLimits = computeEffectiveZoomLimits(settings)
mapStateController.setZoomLimits(effectiveLimits.first.toDouble(), effectiveLimits.last.toDouble())
saveStateJob?.cancel()
if (settings.restoreMapStateOnStart) {
    startSaveMapStateJob()
}
if (!cameraInitialized) {
    cameraInitialized = true
    if (settings.restoreMapStateOnStart) {
        loadMapState(settings.minZoomLevel, settings.maxZoomLevel)
    } else {
        mapStateController.zoom = settings.defaultZoomLevel.toDouble()
    }
}
mapStateController.zoom = mapStateController.zoom.coerceIn(
    effectiveLimits.first.toDouble(),
    effectiveLimits.last.toDouble(),
)
```

- [ ] **Step 7: Run tests to confirm they pass**

```
./gradlew :composeApp:testDebugUnitTest --tests "*.MapViewModelOverlayZoomTest"
```

Expected: BUILD SUCCESSFUL, all 14 tests green.

- [ ] **Step 8: Run full test suite**

```
./gradlew :composeApp:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL, all existing tests still green.

- [ ] **Step 9: Commit**

```bash
git add composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/map/MapViewModelOverlayZoomTest.kt \
        composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/map/MapViewModelOverlayTest.kt \
        composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/MapViewModel.kt
git commit -m "TDD overlay zoom limit logic in MapViewModel"
```

---

### Task 5: Settings UI — toggle and string resources

**Files:**
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/ui/MainScreen.kt`

**Interfaces:**
- Consumes: `SettingsViewModel.updateLimitZoomToOverlay(Boolean)` (Task 3)

- [ ] **Step 1: Add string resources to `strings.xml`**

Add after the `zoom_max_title` string:
```xml
<string name="limit_zoom_to_overlay_title">Limit Zoom to Overlay</string>
<string name="limit_zoom_to_overlay_description">Constrains zoom to the range of active overlays</string>
```

- [ ] **Step 2: Add the toggle to `MainScreen.kt`**

In the map settings `SettingsGroup`, after the `SettingsNumberInput` for Max Zoom and its preceding `HorizontalDivider`, add:

```kotlin
HorizontalDivider()
SettingsSwitch(
    title = stringResource(Res.string.limit_zoom_to_overlay_title),
    description = stringResource(Res.string.limit_zoom_to_overlay_description),
    checked = settings.getValue()?.limitZoomToOverlay ?: false,
    onCheckedChange = viewModel::updateLimitZoomToOverlay,
)
```

This goes immediately before the existing `HorizontalDivider()` that precedes the Clear Map Cache `SettingsItem`.

- [ ] **Step 3: Run ktlint**

```
./gradlew :composeApp:ktlintCheck
```

Expected: BUILD SUCCESSFUL. If it fails on formatting, run:
```
./gradlew :composeApp:ktlintFormat
```
Then re-check.

- [ ] **Step 4: Run full unit test suite and desktop UI tests**

```
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/composeResources/values/strings.xml \
        composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/ui/MainScreen.kt
git commit -m "Add Limit Zoom to Overlay toggle to Settings UI"
```
