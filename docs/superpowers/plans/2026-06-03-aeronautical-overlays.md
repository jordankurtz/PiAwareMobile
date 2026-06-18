# Aeronautical Chart Overlays Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add two independently toggleable aeronautical raster tile overlays to the map — FAA VFR Sectional Charts (no API key, US only) and OpenAIP Airspace (free API key required, global) — with on-map FAB toggles and Settings page controls.

**Architecture:** Raster tile overlays are rendered as `RasterSource`/`RasterLayer` pairs inside the `MaplibreComposeMap { }` block in `MapLibreMap.kt`, controlled by booleans flowing from `Settings` → `MapViewModel` → `MapScreen` → `MapLibreMap`. Toggle FABs on the map surface use the existing `MapFab` expect/actual. The OpenAIP API key lives in the existing `Settings.apiKeys` map under key `"openaip"`.

**Tech Stack:** Kotlin Multiplatform Compose, MapLibre Compose 0.12.1 (`RasterSource`, `RasterLayer`, `TileCoordinateSystem`, `TileSetOptions`), AndroidX DataStore, Koin, Mokkery (tests)

---

## Tile URLs

- **FAA VFR Sectional:** `https://tiles.arcgis.com/tiles/ssFJjBXIUyZDrSYZ/arcgis/rest/services/VFR_Sectional/MapServer/tile/{z}/{y}/{x}` — ArcGIS public layer, TMS coordinate order, no key
- **OpenAIP Airspace:** `https://api.tiles.openaip.net/api/data/airspaces/{z}/{x}/{y}.pbf?apiKey={key}` — MVT vector tiles, source-layer `"airspaces"`, free API key from openaip.net; styled with `FillLayer` + `LineLayer` color-coded by `icaoClass` (0=A … 6=G)

---

## File Map

| File | Change |
|------|--------|
| `settings/Settings.kt` | Add `showFaaCharts: Boolean = false`, `showAirspace: Boolean = false` |
| `settings/repo/SettingsRepository.kt` | Add two `booleanPreferencesKey` constants |
| `settings/repo/SettingsRepositoryImpl.kt` | Read/write new fields in `getSettings()` / `saveSettings()` |
| `settings/usecase/SettingsService.kt` | Add `setShowFaaCharts` and `setShowAirspace` signatures |
| `settings/usecase/impl/SettingsServiceImpl.kt` | Implement both with `updateSetting { }` |
| `settings/SettingsViewModel.kt` | Add `updateShowFaaCharts` and `updateShowAirspace` |
| `settings/ui/MainScreen.kt` | Add two `SettingsSwitch` + one `SettingsTextInput` for OpenAIP key in Map section |
| `map/MapViewModel.kt` | Add `SettingsService` param, two `StateFlow<Boolean>`, two toggle funs |
| `map/MapLibreMap.kt` | Add `faaChartsEnabled`, `airspaceEnabled`, `openAipApiKey` params; add raster layers |
| `map/MapScreen.kt` | Collect new state, pass to `MapLibreMap`, add two `MapFab` toggles in topEnd Column |
| `commonMain/composeResources/values/strings.xml` | Add 5 string resources |
| `commonTest/…/map/MapViewModelTest.kt` | Add `mock<SettingsService>()` to `createViewModel()` |
| `commonTest/…/map/MapViewModelOverlayTest.kt` | New: tests for overlay toggle behavior |
| `desktopTest/…/map/ui/OverlayFabTest.kt` | New: desktop UI test for overlay FABs |

---

## Task 1: Add overlay fields to Settings data class

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/Settings.kt`

- [ ] **Step 1: Add two fields to Settings**

Open `Settings.kt`. After `val customProviders: List<CustomProviderConfig> = emptyList(),` add:

```kotlin
    val showFaaCharts: Boolean = false,
    val showAirspace: Boolean = false,
```

Final class signature (last four fields):
```kotlin
data class Settings(
    // ... existing fields ...
    val apiKeys: Map<String, String> = emptyMap(),
    val customProviders: List<CustomProviderConfig> = emptyList(),
    val showFaaCharts: Boolean = false,
    val showAirspace: Boolean = false,
)
```

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew :composeApp:compileKotlinDesktop
```
Expected: BUILD SUCCESSFUL

---

## Task 2: Add DataStore keys and wire persistence

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/repo/SettingsRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/repo/SettingsRepositoryImpl.kt`

- [ ] **Step 1: Add keys to SettingsRepository companion**

In `SettingsRepository.kt`, add after `val CUSTOM_PROVIDERS_JSON`:
```kotlin
val SHOW_FAA_CHARTS = booleanPreferencesKey("showFaaCharts")
val SHOW_AIRSPACE = booleanPreferencesKey("showAirspace")
```

- [ ] **Step 2: Read new fields in getSettings()**

In `SettingsRepositoryImpl.kt`, inside the `Settings(...)` constructor call in `getSettings()`, add after `customProviders = ...`:
```kotlin
showFaaCharts = preferences[SettingsRepository.SHOW_FAA_CHARTS] ?: false,
showAirspace = preferences[SettingsRepository.SHOW_AIRSPACE] ?: false,
```

- [ ] **Step 3: Write new fields in saveSettings()**

In `SettingsRepositoryImpl.kt`, inside `saveSettings()`, add after the `CUSTOM_PROVIDERS_JSON` line:
```kotlin
preferences[SettingsRepository.SHOW_FAA_CHARTS] = settings.showFaaCharts
preferences[SettingsRepository.SHOW_AIRSPACE] = settings.showAirspace
```

- [ ] **Step 4: Compile check**

```bash
./gradlew :composeApp:compileKotlinDesktop
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/Settings.kt \
        composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/repo/SettingsRepository.kt \
        composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/repo/SettingsRepositoryImpl.kt
git commit -m "Add showFaaCharts and showAirspace to Settings with DataStore persistence"
```

---

## Task 3: Add service methods

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/usecase/SettingsService.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/usecase/impl/SettingsServiceImpl.kt`

- [ ] **Step 1: Write failing tests for new service methods**

Create `composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/settings/SettingsServiceOverlayTest.kt`:

```kotlin
package com.jordankurtz.piawaremobile.settings

import com.jordankurtz.piawaremobile.settings.repo.SettingsRepository
import com.jordankurtz.piawaremobile.settings.repo.SettingsRepositoryImpl
import com.jordankurtz.piawaremobile.testutil.FakeDataStore
import com.jordankurtz.piawaremobile.settings.usecase.impl.SettingsServiceImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsServiceOverlayTest {
    private val dataStore = FakeDataStore()
    private val repository: SettingsRepository = SettingsRepositoryImpl(dataStore)
    private val service = SettingsServiceImpl(repository, Dispatchers.Unconfined)

    @Test
    fun `setShowFaaCharts persists true`() = runTest {
        service.setShowFaaCharts(true)
        assertTrue(repository.getSettings().first().showFaaCharts)
    }

    @Test
    fun `setShowFaaCharts persists false`() = runTest {
        service.setShowFaaCharts(true)
        service.setShowFaaCharts(false)
        assertFalse(repository.getSettings().first().showFaaCharts)
    }

    @Test
    fun `setShowAirspace persists true`() = runTest {
        service.setShowAirspace(true)
        assertTrue(repository.getSettings().first().showAirspace)
    }

    @Test
    fun `setShowAirspace persists false`() = runTest {
        service.setShowAirspace(true)
        service.setShowAirspace(false)
        assertFalse(repository.getSettings().first().showAirspace)
    }
}
```

- [ ] **Step 2: Run tests — expect compile failure**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*.SettingsServiceOverlayTest"
```
Expected: compile error — `setShowFaaCharts` and `setShowAirspace` not defined

- [ ] **Step 3: Add signatures to SettingsService interface**

In `SettingsService.kt`, add after `suspend fun setMaxZoomLevel(zoom: Int)`:
```kotlin
suspend fun setShowFaaCharts(enabled: Boolean)

suspend fun setShowAirspace(enabled: Boolean)
```

- [ ] **Step 4: Implement in SettingsServiceImpl**

In `SettingsServiceImpl.kt`, add after `setMaxZoomLevel`:
```kotlin
override suspend fun setShowFaaCharts(enabled: Boolean) =
    updateSetting { it.copy(showFaaCharts = enabled) }

override suspend fun setShowAirspace(enabled: Boolean) =
    updateSetting { it.copy(showAirspace = enabled) }
```

- [ ] **Step 5: Run tests — expect pass**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*.SettingsServiceOverlayTest"
```
Expected: 4 tests PASS

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/usecase/SettingsService.kt \
        composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/usecase/impl/SettingsServiceImpl.kt \
        composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/settings/SettingsServiceOverlayTest.kt
git commit -m "Add setShowFaaCharts/setShowAirspace service methods with tests"
```

---

## Task 4: Wire MapViewModel overlay state and toggles

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/MapViewModel.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/map/MapViewModelTest.kt`
- Create: `composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/map/MapViewModelOverlayTest.kt`

- [ ] **Step 1: Write failing tests**

Create `composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/map/MapViewModelOverlayTest.kt`:

```kotlin
package com.jordankurtz.piawaremobile.map

import com.jordankurtz.piawaremobile.map.usecase.GetSavedMapStateUseCase
import com.jordankurtz.piawaremobile.map.usecase.SaveMapStateUseCase
import com.jordankurtz.piawaremobile.map.debug.TileCacheStatsTracker
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import com.jordankurtz.piawaremobile.settings.usecase.SettingsService
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class MapViewModelOverlayTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var settingsService: SettingsService
    private lateinit var loadSettingsUseCase: LoadSettingsUseCase
    private lateinit var viewModel: MapViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsService = mock<SettingsService>()
        loadSettingsUseCase = mock<LoadSettingsUseCase>()
        every { loadSettingsUseCase.invoke() } returns flowOf(Async.Success(Settings()))
        everySuspend { settingsService.setShowFaaCharts(any()) } returns Unit
        everySuspend { settingsService.setShowAirspace(any()) } returns Unit
        viewModel = MapViewModel(
            providerConfigFlow = MutableStateFlow(TileProviders.DEFAULT),
            getSavedMapStateUseCase = mock<GetSavedMapStateUseCase>().also {
                everySuspend { it.invoke() } returns SavedMapState(0.0, 0.0, 8.0)
            },
            saveMapStateUseCase = mock<SaveMapStateUseCase>(),
            loadSettingsUseCase = loadSettingsUseCase,
            settingsService = settingsService,
            tileCacheStatsTracker = TileCacheStatsTracker(),
            mapStateController = FakeMapStateController(),
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `showFaaCharts starts false from default settings`() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.showFaaCharts.value)
    }

    @Test
    fun `showAirspace starts false from default settings`() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.showAirspace.value)
    }

    @Test
    fun `toggleFaaCharts calls setShowFaaCharts with toggled value`() = runTest {
        advanceUntilIdle()
        viewModel.toggleFaaCharts()
        advanceUntilIdle()
        verifySuspend { settingsService.setShowFaaCharts(true) }
    }

    @Test
    fun `toggleAirspace calls setShowAirspace with toggled value`() = runTest {
        advanceUntilIdle()
        viewModel.toggleAirspace()
        advanceUntilIdle()
        verifySuspend { settingsService.setShowAirspace(true) }
    }

    @Test
    fun `showFaaCharts reflects settings value true`() = runTest {
        every { loadSettingsUseCase.invoke() } returns flowOf(Async.Success(Settings(showFaaCharts = true)))
        val vm = MapViewModel(
            providerConfigFlow = MutableStateFlow(TileProviders.DEFAULT),
            getSavedMapStateUseCase = mock<GetSavedMapStateUseCase>().also {
                everySuspend { it.invoke() } returns SavedMapState(0.0, 0.0, 8.0)
            },
            saveMapStateUseCase = mock<SaveMapStateUseCase>(),
            loadSettingsUseCase = loadSettingsUseCase,
            settingsService = settingsService,
            tileCacheStatsTracker = TileCacheStatsTracker(),
            mapStateController = FakeMapStateController(),
        )
        advanceUntilIdle()
        assertTrue(vm.showFaaCharts.value)
    }
}
```

- [ ] **Step 2: Run tests — expect compile failure**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*.MapViewModelOverlayTest"
```
Expected: compile error

- [ ] **Step 3: Update MapViewModel**

In `MapViewModel.kt`:

1. Add `settingsService: SettingsService` to the constructor after `loadSettingsUseCase`:
```kotlin
@Factory
class MapViewModel(
    private val providerConfigFlow: StateFlow<TileProviderConfig>,
    private val getSavedMapStateUseCase: GetSavedMapStateUseCase,
    private val saveMapStateUseCase: SaveMapStateUseCase,
    private val loadSettingsUseCase: LoadSettingsUseCase,
    private val settingsService: SettingsService,      // ADD THIS
    private val tileCacheStatsTracker: TileCacheStatsTracker,
    internal val mapStateController: MapStateController,
) : ViewModel() {
```

2. Add import: `import com.jordankurtz.piawaremobile.settings.usecase.SettingsService`

3. Add two StateFlows after `val tileStats`:
```kotlin
private val _showFaaCharts = MutableStateFlow(false)
val showFaaCharts: StateFlow<Boolean> = _showFaaCharts

private val _showAirspace = MutableStateFlow(false)
val showAirspace: StateFlow<Boolean> = _showAirspace
```

4. In `onSettingsLoaded()`, add after `_showUserLocationOnMap.value = settings.showUserLocationOnMap`:
```kotlin
_showFaaCharts.value = settings.showFaaCharts
_showAirspace.value = settings.showAirspace
```

5. Add toggle functions after `resetBearing()`:
```kotlin
fun toggleFaaCharts() {
    viewModelScope.launch { settingsService.setShowFaaCharts(!_showFaaCharts.value) }
}

fun toggleAirspace() {
    viewModelScope.launch { settingsService.setShowAirspace(!_showAirspace.value) }
}
```

- [ ] **Step 4: Fix MapViewModelTest — add mock settingsService**

In `MapViewModelTest.kt`, add:
```kotlin
private val settingsService: SettingsService = mock<SettingsService>()
```
(alongside the other `mock<>()` declarations)

And add to `createViewModel()`:
```kotlin
MapViewModel(
    providerConfigFlow = providerConfigFlow,
    getSavedMapStateUseCase = getSavedMapStateUseCase,
    saveMapStateUseCase = saveMapStateUseCase,
    loadSettingsUseCase = loadSettingsUseCase,
    settingsService = settingsService,         // ADD THIS
    tileCacheStatsTracker = TileCacheStatsTracker(),
    mapStateController = mapStateController,
)
```

Also add the import: `import com.jordankurtz.piawaremobile.settings.usecase.SettingsService` and `import dev.mokkery.mock`.

- [ ] **Step 5: Also check SavedMapState import in test**

`GetSavedMapStateUseCase` returns a `SavedMapState`. Check its package in `MapViewModelTest` and use the same import in `MapViewModelOverlayTest`. Look at:
```bash
grep "SavedMapState\|GetSavedMapState" composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/map/MapViewModelTest.kt | head -5
```
Mirror those imports in the new test file.

- [ ] **Step 6: Run all map tests**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "*.MapViewModel*"
```
Expected: all pass

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/MapViewModel.kt \
        composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/map/MapViewModelTest.kt \
        composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/map/MapViewModelOverlayTest.kt
git commit -m "Add overlay toggle state and methods to MapViewModel"
```

---

## Task 5: Add overlay layers to MapLibreMap

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/MapLibreMap.kt`

FAA sectionals use `RasterSource` + `RasterLayer` (charts are inherently raster images).
OpenAIP airspace uses `VectorSource` + `FillLayer` + `LineLayer` (MVT vector tiles, styled by `icaoClass`).

OpenAIP `icaoClass` integer mapping: 0=A, 1=B, 2=C, 3=D, 4=E, 5=F, 6=G
Source layer name in PBF tiles: `"airspaces"` (matches the endpoint path `/airspaces/{z}/{x}/{y}.pbf`)

- [ ] **Step 1: Add parameters to MapLibreMap**

In `MapLibreMap.kt`, add three parameters after `onBearingChanged`:
```kotlin
faaChartsEnabled: Boolean = false,
airspaceEnabled: Boolean = false,
openAipApiKey: String = "",
```

- [ ] **Step 2: Add imports**

Add to `MapLibreMap.kt` imports:
```kotlin
import androidx.compose.ui.graphics.Color
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.switch
import org.maplibre.compose.expressions.dsl.case
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.RasterLayer
import org.maplibre.compose.sources.TileCoordinateSystem
import org.maplibre.compose.sources.TileSetOptions
import org.maplibre.compose.sources.rememberRasterSource
import org.maplibre.compose.sources.rememberVectorSource
```

`const` is already imported from `org.maplibre.compose.expressions.dsl.const`.

- [ ] **Step 3: Add overlay layers inside MaplibreComposeMap block**

Inside the `MaplibreComposeMap { }` lambda, after `controller.paths.values.forEach { path -> PathLayer(path) }`, add:

```kotlin
if (faaChartsEnabled) {
    val faaSource =
        rememberRasterSource(
            tiles =
                listOf(
                    "https://tiles.arcgis.com/tiles/ssFJjBXIUyZDrSYZ/arcgis/rest/services/VFR_Sectional/MapServer/tile/{z}/{y}/{x}",
                ),
            options = TileSetOptions(tileCoordinateSystem = TileCoordinateSystem.TMS),
        )
    RasterLayer(
        id = "faa-sectional",
        source = faaSource,
        opacity = const(0.6f),
    )
}

if (airspaceEnabled && openAipApiKey.isNotEmpty()) {
    val airspaceSource =
        rememberVectorSource(
            tiles =
                listOf(
                    "https://api.tiles.openaip.net/api/data/airspaces/{z}/{x}/{y}.pbf?apiKey=$openAipApiKey",
                ),
            options = TileSetOptions(minZoom = 7, maxZoom = 14),
        )
    val airspaceColor =
        switch(
            feature.get("icaoClass"),
            arrayOf(
                case(0, const(Color(0xFF4169E1))), // A — dark blue
                case(1, const(Color(0xFF0047AB))), // B — cobalt blue
                case(2, const(Color(0xFF800080))), // C — magenta
                case(3, const(Color(0xFF1E90FF))), // D — dodger blue
                case(4, const(Color(0xFFDA70D6))), // E — orchid
                case(5, const(Color(0xFF808080))), // F — grey
            ),
            const(Color(0xFFFF4444)), // G/uncontrolled + restricted fallback — red
        )
    FillLayer(
        id = "openaip-airspace-fill",
        source = airspaceSource,
        sourceLayer = "airspaces",
        fillColor = airspaceColor,
        fillOpacity = const(0.15f),
    )
    LineLayer(
        id = "openaip-airspace-border",
        source = airspaceSource,
        sourceLayer = "airspaces",
        lineColor = airspaceColor,
        lineWidth = const(1.5f),
        lineOpacity = const(0.8f),
    )
}
```

- [ ] **Step 4: Compile check**

```bash
./gradlew :composeApp:compileKotlinDesktop
```
Expected: BUILD SUCCESSFUL. If `switch`/`case`/`feature` imports conflict with Kotlin keywords, use fully-qualified names or rename imports: `import org.maplibre.compose.expressions.dsl.switch as mapSwitch` etc.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/MapLibreMap.kt
git commit -m "Add FAA sectional (raster) and OpenAIP airspace (vector MVT) layers to MapLibreMap"
```

---

## Task 6: Wire MapScreen — collect state, add FABs, pass to map

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/MapScreen.kt`
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`

- [ ] **Step 1: Add string resources**

In `strings.xml`, add after `<string name="reset_north">Reset north</string>`:
```xml
<string name="show_faa_charts">FAA Sectional Charts</string>
<string name="show_airspace">Airspace</string>
```

- [ ] **Step 2: Collect overlay state in MapScreen**

In `MapScreen.kt`, after `val isFollowingUser by mapViewModel.followingUserLocation.collectAsState()`, add:
```kotlin
val showFaaCharts by mapViewModel.showFaaCharts.collectAsState()
val showAirspace by mapViewModel.showAirspace.collectAsState()
val openAipApiKey by remember {
    derivedStateOf { mapViewModel.activeProvider.value.let { _ ->
        // apiKeys flows with settings — derive via activeProvider settings flow not available here
        // Use a separate flow from the settings loaded in MapViewModel
        "" // populated below
    } }
}
```

Wait — the OpenAIP key is in `Settings.apiKeys["openaip"]` but `MapViewModel` doesn't expose it directly. Add a `StateFlow<String>` to `MapViewModel` instead:

In `MapViewModel.kt`, add:
```kotlin
private val _openAipApiKey = MutableStateFlow("")
val openAipApiKey: StateFlow<String> = _openAipApiKey
```

And in `onSettingsLoaded()`, add:
```kotlin
_openAipApiKey.value = settings.apiKeys["openaip"] ?: ""
```

Then in `MapScreen.kt`:
```kotlin
val showFaaCharts by mapViewModel.showFaaCharts.collectAsState()
val showAirspace by mapViewModel.showAirspace.collectAsState()
val openAipApiKey by mapViewModel.openAipApiKey.collectAsState()
```

Add imports to `MapScreen.kt`:
```kotlin
import piawaremobile.composeapp.generated.resources.show_faa_charts
import piawaremobile.composeapp.generated.resources.show_airspace
```

- [ ] **Step 3: Pass overlay params to MapLibreMap**

In `MapScreen.kt`, in the `MapLibreMap(...)` call, add:
```kotlin
faaChartsEnabled = showFaaCharts,
airspaceEnabled = showAirspace,
openAipApiKey = openAipApiKey,
```

- [ ] **Step 4: Add overlay toggle FABs to topEnd Column**

In `MapScreen.kt`, in the topEnd `Column`, after the `CompassFab(...)` call, add:
```kotlin
MapFab(
    onClick = { mapViewModel.toggleFaaCharts() },
    active = showFaaCharts,
) {
    Icon(
        painter = painterResource(Res.drawable.ic_plane),
        contentDescription = stringResource(Res.string.show_faa_charts),
        modifier = Modifier.size(20.dp),
    )
}
MapFab(
    onClick = { mapViewModel.toggleAirspace() },
    active = showAirspace,
) {
    Icon(
        painter = painterResource(Res.drawable.ic_plane),
        contentDescription = stringResource(Res.string.show_airspace),
        modifier = Modifier.size(20.dp),
    )
}
```

Note: `ic_plane` is a placeholder — replace with dedicated icons later. Both icons will render the plane SVG for now.

- [ ] **Step 5: Compile check + lint**

```bash
./gradlew :composeApp:compileKotlinDesktop ktlintCheck
```
If ktlint fails: `./gradlew ktlintFormat` then re-check.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/MapScreen.kt \
        composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/MapViewModel.kt \
        composeApp/src/commonMain/composeResources/values/strings.xml
git commit -m "Wire overlay FABs and state through MapScreen to MapLibreMap"
```

---

## Task 7: Add Settings UI for overlays and OpenAIP key

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/SettingsViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/ui/MainScreen.kt`
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`

- [ ] **Step 1: Add string resources**

In `strings.xml`, add after the `show_airspace` string from Task 6:
```xml
<string name="show_faa_charts_description">Overlay US VFR sectional charts on the map</string>
<string name="show_airspace_description">Show airspace classes and restrictions (OpenAIP key required)</string>
<string name="openaip_api_key_title">OpenAIP API Key</string>
```

- [ ] **Step 2: Add SettingsViewModel methods**

In `SettingsViewModel.kt`, add after `updateShowUserLocationOnMap`:
```kotlin
fun updateShowFaaCharts(enabled: Boolean) =
    viewModelScope.launch {
        settingsService.setShowFaaCharts(enabled)
    }

fun updateShowAirspace(enabled: Boolean) =
    viewModelScope.launch {
        settingsService.setShowAirspace(enabled)
    }

fun updateOpenAipKey(key: String) =
    viewModelScope.launch {
        settingsService.setApiKey("openaip", key)
    }
```

- [ ] **Step 3: Add toggle UI to MainScreen**

In `MainScreen.kt`, after the `showUserLocationOnMap` `SettingsSwitch` item (around line 213), add three new `item { }` blocks:

```kotlin
item {
    SettingsSwitch(
        title = stringResource(Res.string.show_faa_charts),
        description = stringResource(Res.string.show_faa_charts_description),
        checked = settings.getValue()?.showFaaCharts ?: false,
        onCheckedChange = viewModel::updateShowFaaCharts,
    )
}

item {
    SettingsSwitch(
        title = stringResource(Res.string.show_airspace),
        description = stringResource(Res.string.show_airspace_description),
        checked = settings.getValue()?.showAirspace ?: false,
        onCheckedChange = viewModel::updateShowAirspace,
    )
}

item {
    SettingsTextInput(
        title = stringResource(Res.string.openaip_api_key_title),
        value = settings.getValue()?.apiKeys?.get("openaip") ?: "",
        onValueChange = viewModel::updateOpenAipKey,
    )
}
```

Add imports to `MainScreen.kt`:
```kotlin
import piawaremobile.composeapp.generated.resources.show_faa_charts
import piawaremobile.composeapp.generated.resources.show_faa_charts_description
import piawaremobile.composeapp.generated.resources.show_airspace
import piawaremobile.composeapp.generated.resources.show_airspace_description
import piawaremobile.composeapp.generated.resources.openaip_api_key_title
```

- [ ] **Step 4: Run full check**

```bash
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest ktlintCheck
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/SettingsViewModel.kt \
        composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/ui/MainScreen.kt \
        composeApp/src/commonMain/composeResources/values/strings.xml
git commit -m "Add FAA charts and airspace overlay toggles and OpenAIP key to Settings UI"
```

---

## Task 8: Desktop UI test for overlay FABs

**Files:**
- Create: `composeApp/src/desktopTest/kotlin/com/jordankurtz/piawaremobile/map/ui/OverlayFabTest.kt`

- [ ] **Step 1: Write and run desktop UI tests**

Create `composeApp/src/desktopTest/kotlin/com/jordankurtz/piawaremobile/map/ui/OverlayFabTest.kt`:

```kotlin
package com.jordankurtz.piawaremobile.map.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OverlayFabTest {
    @Test
    fun faaChartsFabRendersInactive() =
        runComposeUiTest {
            setContent {
                MapFab(onClick = {}, active = false) { Text("FAA") }
            }
            onNodeWithText("FAA").assertIsDisplayed()
        }

    @Test
    fun faaChartsFabRendersActive() =
        runComposeUiTest {
            setContent {
                MapFab(onClick = {}, active = true) { Text("FAA") }
            }
            onNodeWithText("FAA").assertIsDisplayed()
        }

    @Test
    fun faaChartsFabClickFires() =
        runComposeUiTest {
            var clicked = false
            setContent {
                MapFab(onClick = { clicked = true }, active = false) { Text("FAA") }
            }
            onNodeWithText("FAA").performClick()
            assertTrue(clicked)
        }

    @Test
    fun airspaceFabClickFires() =
        runComposeUiTest {
            var clicked = false
            setContent {
                MapFab(onClick = { clicked = true }, active = false) { Text("AIR") }
            }
            onNodeWithText("AIR").performClick()
            assertTrue(clicked)
        }
}
```

- [ ] **Step 2: Run desktop tests**

```bash
./gradlew :composeApp:desktopTest --tests "*.OverlayFabTest"
```
Expected: 4 tests PASS

- [ ] **Step 3: Run full suite**

```bash
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest ktlintCheck
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/desktopTest/kotlin/com/jordankurtz/piawaremobile/map/ui/OverlayFabTest.kt
git commit -m "Add desktop UI tests for overlay toggle FABs"
```

---

## Task 9: Push branch and open PR

- [ ] **Step 1: Create and push branch**

```bash
git checkout -b aeronautical-overlays
git push -u origin aeronautical-overlays
```

(If already on a working branch, just push it.)

- [ ] **Step 2: Final verification**

```bash
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest ktlintCheck detekt
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Open PR**

Base branch: `compass-and-map-fab`
Title: `Add toggleable FAA sectional chart and OpenAIP airspace overlays`

---

## Self-Review Checklist

- [x] Settings persistence: `showFaaCharts` and `showAirspace` saved/loaded via DataStore — Tasks 1–2
- [x] Service methods with tests — Task 3
- [x] MapViewModel exposes state + toggles, `openAipApiKey` derived from settings — Task 4 + Task 6
- [x] RasterSource/RasterLayer in MapLibreMap — Task 5
- [x] On-map FAB toggles in MapScreen — Task 6
- [x] Settings UI toggles + OpenAIP key input — Task 7
- [x] Desktop UI tests — Task 8
- [x] OpenAIP layer gracefully skipped when key is empty (`airspaceEnabled && openAipApiKey.isNotEmpty()`) — Task 5
- [x] `TileCoordinateSystem.TMS` used for FAA ArcGIS tiles (row/col ordering) — Task 5
- [x] Existing `MapViewModelTest` updated for new constructor param — Task 4
