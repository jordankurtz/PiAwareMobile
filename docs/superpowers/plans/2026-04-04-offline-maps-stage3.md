# Offline Maps Stage 3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the offline maps feature end-to-end: fix the critical mock-bounds bug so downloads target the correct region, add live size estimates, track download status per region, keep downloads alive across navigation, and free tile storage when deleting a region.

**Architecture:** The coordinate conversion math lives in `MapHelpers.kt` (pure functions, easy to test). `MapRegionPickerContent` receives scroll/scale as parameters and computes real lat/lon from screen pixels. A single application-scoped `DownloadScope` (Koin `@Single`) replaces `viewModelScope` for downloads so they survive navigation. Download status is persisted as a `TEXT` column in `offline_region`, mapped to a `DownloadStatus` enum in Kotlin. Tile deletion on region delete cleans up exclusively-pinned tiles from disk via a new `TileCache.delete()` method.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Koin (annotation-based DI), SQLDelight 2.0.2, mapcompose (`MapState`, `scroll`, `scale`), Mokkery, `runTest` + `StandardTestDispatcher`

---

## File Structure

**New files:**
- `commonMain/.../map/offline/DownloadStatus.kt` — `enum class DownloadStatus { DOWNLOADING, COMPLETE, FAILED }`
- `commonMain/sqldelight/.../map/cache/2.sqm` — Migration adding `status` and `downloaded_tile_count` to `offline_region`
- `commonTest/.../map/offline/MapHelpersInverseTest.kt` — Unit tests for `invertProjection` and `screenToLatLon`
- `commonTest/.../map/offline/MapRegionPickerContentTest.kt` — Desktop UI test for region picker confirming real bounds

**Modified files:**
- `commonMain/.../map/MapHelpers.kt` — add `invertProjection`, `screenToLatLon`
- `commonMain/.../map/offline/OfflineRegion.kt` — add `status: DownloadStatus` field
- `commonMain/.../map/offline/OfflineTileStore.kt` — add `updateDownloadStatus`, `getExclusiveTileSizeBytes`, `deleteRegionAndFreeTiles`
- `commonMain/.../map/offline/SqlDelightOfflineTileStore.kt` — implement new methods
- `commonMain/.../map/cache/TileCache.kt` — add `delete(zoomLvl, col, row)`
- `commonMain/.../map/cache/FileTileCache.kt` — implement `delete`
- `commonMain/.../map/offline/OfflineDownloadEngine.kt` — set status to DOWNLOADING/COMPLETE/FAILED
- `commonMain/.../map/offline/OfflineMapsViewModel.kt` — inject `DownloadScope`, pass status updates, add confirmation delete
- `commonMain/.../map/offline/MapRegionPickerScreen.kt` — fix confirm button to use real coordinate conversion
- `commonMain/.../settings/ui/DownloadRegionDialog.kt` — live tile count/MB estimate
- `commonMain/.../settings/ui/OfflineMapsScreen.kt` — show download status per region, confirmation delete dialog
- `commonMain/.../sqldelight/.../TileCache.sq` — add `updateRegionStatus`, `selectExclusivelyPinnedTilesByRegion`, `sizeOfExclusivelyPinnedTiles`
- `jvmTest/.../map/offline/SqlDelightOfflineTileStoreTest.kt` — tests for new methods

---

## Task 1: Add `invertProjection` and `screenToLatLon` to MapHelpers

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/MapHelpers.kt`
- Create: `composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/map/MapHelpersInverseTest.kt`

### Background

`doProjection(lat, lon)` converts to normalized [0,1] map coordinates. The inverse is needed to convert screen pixels → lat/lon when the user confirms a selection box.

The math for the inverse:

**Lon from normX:**
```
normX = (6378137 * lon_rad - X0) / (-2 * X0)  where X0 = -20037508.342789248
lon_rad = (normX * (-2 * X0) + X0) / 6378137
lon = lon_rad * 180 / PI
```

**Lat from normY:**
```
normY = (yMerc - (-X0)) / (X0 - (-X0))  [min=-X0=+20037508, max=X0=-20037508]
      = (yMerc - 20037508) / (-40075016)
yMerc = 20037508 * (1 - 2 * normY)
yMerc = 3189068.5 * ln((1+sin(lat_rad)) / (1-sin(lat_rad)))
u = yMerc / 3189068.5
sin(lat) = (e^u - 1) / (e^u + 1)
lat = asin(sin(lat)) * 180 / PI
```

**Screen pixel to lat/lon (mapcompose viewport math):**

`MapState.scroll` gives the normalized [0,1] position of the viewport center. At `scale=s`, one normalized unit = `mapSize * s` screen pixels. So:
```
normX = scroll.x + (screenX - screenWidth/2) / (mapSize * scale)
normY = scroll.y + (screenY - screenHeight/2) / (mapSize * scale)
```
where `mapSize = TILE_SIZE * 2^MAX_LEVEL = 256 * 65536 = 16777216`.

- [ ] **Step 1: Write the failing tests**

Create `composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/map/MapHelpersInverseTest.kt`:

```kotlin
package com.jordankurtz.piawaremobile.map

import kotlin.test.Test
import kotlin.test.assertEquals

class MapHelpersInverseTest {
    private fun assertNearlyEqual(
        expected: Double,
        actual: Double,
        delta: Double = 1e-6,
        message: String = "",
    ) {
        val diff = kotlin.math.abs(expected - actual)
        if (diff > delta) {
            throw AssertionError(
                "$message expected=$expected actual=$actual diff=$diff (tolerance=$delta)"
            )
        }
    }

    @Test
    fun `invertProjection round-trips with doProjection for equator`() {
        val (normX, normY) = doProjection(0.0, 0.0)
        val (lat, lon) = invertProjection(normX, normY)
        assertNearlyEqual(0.0, lat, message = "lat")
        assertNearlyEqual(0.0, lon, message = "lon")
    }

    @Test
    fun `invertProjection round-trips for San Francisco`() {
        val lat0 = 37.7749
        val lon0 = -122.4194
        val (normX, normY) = doProjection(lat0, lon0)
        val (lat, lon) = invertProjection(normX, normY)
        assertNearlyEqual(lat0, lat, message = "lat")
        assertNearlyEqual(lon0, lon, message = "lon")
    }

    @Test
    fun `invertProjection round-trips for negative latitude`() {
        val lat0 = -33.8688
        val lon0 = 151.2093
        val (normX, normY) = doProjection(lat0, lon0)
        val (lat, lon) = invertProjection(normX, normY)
        assertNearlyEqual(lat0, lat, message = "lat")
        assertNearlyEqual(lon0, lon, message = "lon")
    }

    @Test
    fun `screenToLatLon returns scroll center when screen center passed`() {
        // scroll center (0.5, 0.5) = equator/prime meridian at scale=1
        val (lat, lon) = screenToLatLon(
            screenX = 500f, screenY = 400f,
            screenWidth = 1000f, screenHeight = 800f,
            scrollX = 0.5, scrollY = 0.5,
            scale = 1f,
        )
        assertNearlyEqual(0.0, lat, delta = 1e-4, message = "lat at center")
        assertNearlyEqual(0.0, lon, delta = 1e-4, message = "lon at center")
    }
}
```

- [ ] **Step 2: Run the tests to confirm they fail**

```
./gradlew :composeApp:testDebugUnitTest --tests "*.MapHelpersInverseTest" 2>&1 | tail -20
```
Expected: FAIL — `invertProjection` and `screenToLatLon` not found.

- [ ] **Step 3: Add `invertProjection` and `screenToLatLon` to MapHelpers.kt**

Add these imports at the top of `MapHelpers.kt`:
```kotlin
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.exp
```

Add these functions after `doProjection`:
```kotlin
/**
 * Inverse of [doProjection]: converts normalized [0,1] map coordinates back to (lat, lon) degrees.
 * Useful for mapping screen pixel positions back to geographic coordinates.
 */
fun invertProjection(normX: Double, normY: Double): Pair<Double, Double> {
    val halfRange = -X0 // 20037508.342789248
    val xMerc = normX * (2 * halfRange) - halfRange
    val lon = xMerc / 6378137.0 * (180.0 / PI)

    val yMerc = halfRange * (1.0 - 2.0 * normY)
    val u = yMerc / 3189068.5
    val sinLat = (exp(u) - 1.0) / (exp(u) + 1.0)
    val lat = asin(sinLat) * (180.0 / PI)

    return Pair(lat, lon)
}

/**
 * Converts a screen pixel position to geographic (lat, lon) degrees, given the current map
 * viewport state.
 *
 * @param scrollX Normalized [0,1] x-coordinate of the viewport center (from MapState.scroll.x)
 * @param scrollY Normalized [0,1] y-coordinate of the viewport center (from MapState.scroll.y)
 * @param scale Current map scale factor (from MapState.scale)
 */
fun screenToLatLon(
    screenX: Float,
    screenY: Float,
    screenWidth: Float,
    screenHeight: Float,
    scrollX: Double,
    scrollY: Double,
    scale: Float,
): Pair<Double, Double> {
    val normX = scrollX + (screenX - screenWidth / 2f) / (mapSize * scale)
    val normY = scrollY + (screenY - screenHeight / 2f) / (mapSize * scale)
    return invertProjection(normX, normY)
}
```

- [ ] **Step 4: Run the tests to confirm they pass**

```
./gradlew :composeApp:testDebugUnitTest --tests "*.MapHelpersInverseTest" 2>&1 | tail -20
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/MapHelpers.kt \
        composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/map/MapHelpersInverseTest.kt
git commit -m "Add invertProjection and screenToLatLon to MapHelpers"
```

---

## Task 2: Fix MapRegionPickerScreen coordinate conversion

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/MapRegionPickerScreen.kt`

### Background

`MapRegionPickerContent` has `screenWidth`, `screenHeight`, and `BoxBounds(left, top, right, bottom)` in screen pixels. The confirm button currently returns hardcoded SF Bay Area bounds (lines 281-290). We need to:
1. Add `scrollX: Double`, `scrollY: Double`, `scale: Float` parameters to `MapRegionPickerContent` (with defaults for tests)
2. In `MapRegionPickerScreen`, read `mapViewModel.state.scroll` and `mapViewModel.state.scale` and pass them in
3. Replace the mock bounds with `screenToLatLon` calls for each corner of `BoxBounds`

The `BoundingBox` constructor takes `(minLat, maxLat, minLon, maxLon)`. The box top edge = maxLat (north), bottom edge = minLat (south), left edge = minLon (west), right edge = maxLon (east).

The `MapState` is from mapcompose. `mapViewModel.state` is of type `ovh.plrapps.mapcompose.ui.state.MapState`. Access scroll as `mapViewModel.state.scroll.x` and `mapViewModel.state.scroll.y`, and scale as `mapViewModel.state.scale`.

Note: `scroll` and `scale` are Compose state, so reading them inside a `@Composable` is reactive.

- [ ] **Step 1: Add scroll/scale parameters to `MapRegionPickerContent`**

In `MapRegionPickerContent`, add three parameters after `mapLayer`:
```kotlin
@Composable
internal fun MapRegionPickerContent(
    onRegionSelected: (BoundingBox) -> Unit,
    onDismiss: () -> Unit,
    mapLayer: @Composable () -> Unit,
    scrollX: Double = 0.5,
    scrollY: Double = 0.5,
    scale: Float = 1f,
) {
```

- [ ] **Step 2: Replace the hardcoded bounds with real coordinate conversion**

Find the confirm button's `onClick` lambda (around line 280) and replace the mock body:

```kotlin
Button(
    onClick = {
        val b = bounds
        val (topLat, leftLon) = screenToLatLon(
            screenX = b.left, screenY = b.top,
            screenWidth = screenWidth, screenHeight = screenHeight,
            scrollX = scrollX, scrollY = scrollY, scale = scale,
        )
        val (bottomLat, rightLon) = screenToLatLon(
            screenX = b.right, screenY = b.bottom,
            screenWidth = screenWidth, screenHeight = screenHeight,
            scrollX = scrollX, scrollY = scrollY, scale = scale,
        )
        onRegionSelected(
            BoundingBox(
                minLat = bottomLat,
                maxLat = topLat,
                minLon = leftLon,
                maxLon = rightLon,
            ),
        )
    },
    modifier = Modifier.weight(1f),
) {
    Text(stringResource(Res.string.offline_maps_picker_confirm))
}
```

You also need to add the import at the top:
```kotlin
import com.jordankurtz.piawaremobile.map.screenToLatLon
```

- [ ] **Step 3: Pass scroll/scale from `MapRegionPickerScreen`**

In `MapRegionPickerScreen`, read the map state:
```kotlin
@Composable
fun MapRegionPickerScreen(
    onRegionSelected: (BoundingBox) -> Unit,
    onDismiss: () -> Unit,
    mapViewModel: MapViewModel = koinViewModel(),
) {
    val scrollX = mapViewModel.state.scroll.x
    val scrollY = mapViewModel.state.scroll.y
    val scale = mapViewModel.state.scale
    MapRegionPickerContent(
        onRegionSelected = onRegionSelected,
        onDismiss = onDismiss,
        mapLayer = {
            OpenStreetMap(
                state = mapViewModel.state,
                modifier = Modifier.fillMaxSize(),
            )
        },
        scrollX = scrollX,
        scrollY = scrollY,
        scale = scale,
    )
}
```

- [ ] **Step 4: Run unit and desktop tests**

```
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest 2>&1 | tail -30
```
Expected: all pass (existing tests still pass since they use default scroll/scale values).

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/MapRegionPickerScreen.kt
git commit -m "Fix MapRegionPickerScreen: compute real lat/lon bounds from viewport state"
```

---

## Task 3: Live size estimate in DownloadRegionDialog

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/ui/DownloadRegionDialog.kt`
- Create: `composeApp/src/desktopTest/kotlin/com/jordankurtz/piawaremobile/settings/ui/DownloadRegionDialogTest.kt`

### Background

`DownloadRegionDialog` currently shows a static string `offline_maps_dialog_estimate` = "Estimated size depends on zoom levels and area". We need to replace this with a live computed estimate.

`tileCount(bounds, minZoom, maxZoom): Long` is already in `TileCoordinateUtils.kt`. An average OSM tile is ~15 KB (this is a rough estimate — widely used approximation). So estimated MB = tileCount * 15_360 / 1_048_576.

The estimate should update live as the user moves the zoom sliders. Use `remember(selectedBounds, minZoom, maxZoom) { ... }` to recompute only when inputs change.

When `selectedBounds` is null, show a static prompt "Select a region to see an estimate" (add string resource `offline_maps_dialog_estimate_no_bounds`).

When bounds are set, show e.g. "~42 MB (2800 tiles)" using string resource `offline_maps_dialog_estimate_computed` with two format args (`%d MB`, `%d tiles`).

Add string resources to `composeApp/src/commonMain/composeResources/values/strings.xml`.

- [ ] **Step 1: Write the failing desktop test**

Create `composeApp/src/desktopTest/kotlin/com/jordankurtz/piawaremobile/settings/ui/DownloadRegionDialogTest.kt`:

```kotlin
package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.jordankurtz.piawaremobile.map.offline.BoundingBox
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class DownloadRegionDialogTest {
    @Test
    fun `estimate shows tile count and MB when bounds are set`() = runComposeUiTest {
        val bounds = BoundingBox(minLat = 40.0, maxLat = 41.0, minLon = -75.0, maxLon = -74.0)
        setContent {
            DownloadRegionDialog(
                onDismiss = {},
                onConfirm = { _, _, _ -> },
                selectedBounds = bounds,
            )
        }
        // The estimate node should mention "tiles" (exact numbers vary by zoom defaults)
        onNodeWithText("tiles", substring = true).assertExists()
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

```
./gradlew :composeApp:desktopTest --tests "*.DownloadRegionDialogTest" 2>&1 | tail -20
```
Expected: FAIL — the node with "tiles" doesn't exist (static string shown instead).

- [ ] **Step 3: Add string resources**

In `composeApp/src/commonMain/composeResources/values/strings.xml`, add:
```xml
<string name="offline_maps_dialog_estimate_no_bounds">Select a region on the map to see an estimate</string>
<string name="offline_maps_dialog_estimate_computed">~%1$d MB (%2$d tiles)</string>
```

Remove or keep the old `offline_maps_dialog_estimate` resource (it's still referenced until we update the code).

- [ ] **Step 4: Update DownloadRegionDialog to show live estimate**

In `DownloadRegionDialog.kt`, add these imports:
```kotlin
import androidx.compose.runtime.derivedStateOf
import com.jordankurtz.piawaremobile.map.offline.tileCount
import org.jetbrains.compose.resources.stringResource
import piawaremobile.composeapp.generated.resources.offline_maps_dialog_estimate_computed
import piawaremobile.composeapp.generated.resources.offline_maps_dialog_estimate_no_bounds
```

Add a constant for average tile bytes:
```kotlin
private const val AVG_TILE_BYTES = 15_360L // ~15 KB per tile (rough estimate)
```

Inside the composable, replace the static estimate `Text` with:
```kotlin
val estimateText by remember(selectedBounds, minZoom, maxZoom) {
    derivedStateOf {
        if (selectedBounds == null) {
            null // handled below
        } else {
            val count = tileCount(selectedBounds, minZoom.toInt(), maxZoom.toInt())
            val mb = (count * AVG_TILE_BYTES / (1024L * 1024L)).toInt()
            Pair(mb, count)
        }
    }
}

// Replace the old static Text with:
val estimate = estimateText
if (estimate == null) {
    Text(
        text = stringResource(Res.string.offline_maps_dialog_estimate_no_bounds),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
} else {
    Text(
        text = stringResource(Res.string.offline_maps_dialog_estimate_computed, estimate.first, estimate.second),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
```

- [ ] **Step 5: Run the tests**

```
./gradlew :composeApp:desktopTest --tests "*.DownloadRegionDialogTest" 2>&1 | tail -20
```
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/ui/DownloadRegionDialog.kt \
        composeApp/src/desktopTest/kotlin/com/jordankurtz/piawaremobile/settings/ui/DownloadRegionDialogTest.kt \
        composeApp/src/commonMain/composeResources/values/strings.xml
git commit -m "Show live tile count and MB estimate in DownloadRegionDialog"
```

---

## Task 4: Download status tracking

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/DownloadStatus.kt`
- Create: `composeApp/src/commonMain/sqldelight/com/jordankurtz/piawaremobile/map/cache/2.sqm`
- Modify: `composeApp/src/commonMain/sqldelight/com/jordankurtz/piawaremobile/map/cache/TileCache.sq`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/OfflineRegion.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/OfflineTileStore.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/SqlDelightOfflineTileStore.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/OfflineDownloadEngine.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/OfflineMapsViewModel.kt`
- Modify: `composeApp/src/jvmTest/kotlin/com/jordankurtz/piawaremobile/map/offline/SqlDelightOfflineTileStoreTest.kt`

### Background

Currently `OfflineRegion` has no status field. If a download is interrupted (crash, force-quit), the region stays in the list without any indicator it's incomplete. We add a `status` column to persist download state.

Status values: `DOWNLOADING` (set at start), `COMPLETE` (set at end), `FAILED` (set on exception).

The `OfflineDownloadEngine` needs access to `updateDownloadStatus` — this means the engine's `download()` method needs to set status. But since `OfflineDownloadEngine` already depends on `OfflineTileStore`, we just add a new method to the interface and call it from the engine.

The migration file `2.sqm` migrates schema version 1→2 (adds `status` and `downloaded_tile_count` columns). SQLDelight uses the `.sqm` filename to determine the schema version.

- [ ] **Step 1: Create `DownloadStatus.kt`**

```kotlin
package com.jordankurtz.piawaremobile.map.offline

enum class DownloadStatus {
    DOWNLOADING,
    COMPLETE,
    FAILED,
}
```

- [ ] **Step 2: Create migration `2.sqm`**

Create `composeApp/src/commonMain/sqldelight/com/jordankurtz/piawaremobile/map/cache/2.sqm`:
```sql
ALTER TABLE offline_region ADD COLUMN status TEXT NOT NULL DEFAULT 'COMPLETE';
ALTER TABLE offline_region ADD COLUMN downloaded_tile_count INTEGER NOT NULL DEFAULT 0;
```

- [ ] **Step 3: Add `updateRegionStatus` query to `TileCache.sq`**

At the end of `TileCache.sq`, add:
```sql
updateRegionStatus:
UPDATE offline_region SET status = ?, downloaded_tile_count = ? WHERE id = ?;
```

- [ ] **Step 4: Update `OfflineRegion.kt` to add `status` field**

Open `OfflineRegion.kt`. Add `status: DownloadStatus = DownloadStatus.COMPLETE` and `downloadedTileCount: Long = 0L` fields to the data class:
```kotlin
data class OfflineRegion(
    val id: Long = 0L,
    val name: String,
    val minZoom: Int,
    val maxZoom: Int,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
    val providerId: String,
    val createdAt: Long,
    val tileCount: Long = 0L,
    val sizeBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.COMPLETE,
    val downloadedTileCount: Long = 0L,
)
```

- [ ] **Step 5: Add `updateDownloadStatus` to `OfflineTileStore` interface**

```kotlin
suspend fun updateDownloadStatus(
    id: Long,
    status: DownloadStatus,
    downloadedTileCount: Long = 0L,
)
```

- [ ] **Step 6: Implement `updateDownloadStatus` in `SqlDelightOfflineTileStore`**

```kotlin
override suspend fun updateDownloadStatus(
    id: Long,
    status: DownloadStatus,
    downloadedTileCount: Long,
): Unit =
    withContext(ioDispatcher) {
        queries.updateRegionStatus(
            status = status.name,
            downloaded_tile_count = downloadedTileCount,
            id = id,
        )
    }
```

Also update `Offline_region.toOfflineRegion()` to map the new fields:
```kotlin
private fun Offline_region.toOfflineRegion() =
    OfflineRegion(
        id = id,
        name = name,
        minZoom = min_zoom.toInt(),
        maxZoom = max_zoom.toInt(),
        minLat = min_lat,
        maxLat = max_lat,
        minLon = min_lon,
        maxLon = max_lon,
        providerId = provider_id,
        createdAt = created_at,
        tileCount = tile_count,
        sizeBytes = size_bytes,
        status = DownloadStatus.entries.find { it.name == status } ?: DownloadStatus.COMPLETE,
        downloadedTileCount = downloaded_tile_count,
    )
```

- [ ] **Step 7: Add status setting to `OfflineDownloadEngine`**

In `OfflineDownloadEngine.download()`, the engine sets DOWNLOADING at the start of the loop and updates `downloadedTileCount` as tiles complete. Add these calls:

At the top of the `flow { ... }` block, before the tile loop:
```kotlin
withContext(ioDispatcher) {
    offlineTileStore.updateDownloadStatus(region.id, DownloadStatus.DOWNLOADING, 0L)
}
```

After `downloaded++` and the `emit(...)` line, update the count:
```kotlin
withContext(ioDispatcher) {
    offlineTileStore.updateDownloadStatus(region.id, DownloadStatus.DOWNLOADING, downloaded)
}
```

After `withContext(ioDispatcher) { offlineTileStore.updateRegionStats(...) }`:
```kotlin
withContext(ioDispatcher) {
    offlineTileStore.updateDownloadStatus(region.id, DownloadStatus.COMPLETE, downloaded)
}
```

Add a `try/catch` in the flow wrapping the entire loop to set FAILED status:
```kotlin
flow {
    try {
        // ... existing code ...
        withContext(ioDispatcher) {
            offlineTileStore.updateDownloadStatus(region.id, DownloadStatus.COMPLETE, downloaded)
        }
    } catch (e: Exception) {
        Logger.e("Download failed", e)
        withContext(ioDispatcher) {
            offlineTileStore.updateDownloadStatus(region.id, DownloadStatus.FAILED, downloaded)
        }
        throw e
    }
}
```

- [ ] **Step 8: Write the failing test for `updateDownloadStatus`**

In `SqlDelightOfflineTileStoreTest.kt`, add:
```kotlin
@Test
fun `updateDownloadStatus persists status and downloaded tile count`() =
    runTest(testDispatcher) {
        val id =
            store.saveRegion(
                OfflineRegion(
                    name = "Status",
                    minZoom = 8,
                    maxZoom = 12,
                    minLat = 0.0,
                    maxLat = 1.0,
                    minLon = 0.0,
                    maxLon = 1.0,
                    providerId = "osm",
                    createdAt = 1000L,
                ),
            )
        store.updateDownloadStatus(id, DownloadStatus.DOWNLOADING, 5L)
        val region = store.getRegion(id)
        assertEquals(DownloadStatus.DOWNLOADING, region?.status)
        assertEquals(5L, region?.downloadedTileCount)
    }
```

- [ ] **Step 9: Run all unit tests**

```
./gradlew :composeApp:testDebugUnitTest 2>&1 | tail -30
```
Expected: PASS. Note: The schema migration test (`SqlDelightOfflineTileStoreTest.setUp`) calls `TileCacheDatabase.Schema.create(driver)` which runs all migrations — verify this includes migration 2.

- [ ] **Step 10: Commit**

```bash
git add \
  composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/DownloadStatus.kt \
  composeApp/src/commonMain/sqldelight/com/jordankurtz/piawaremobile/map/cache/2.sqm \
  composeApp/src/commonMain/sqldelight/com/jordankurtz/piawaremobile/map/cache/TileCache.sq \
  composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/OfflineRegion.kt \
  composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/OfflineTileStore.kt \
  composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/SqlDelightOfflineTileStore.kt \
  composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/OfflineDownloadEngine.kt \
  composeApp/src/jvmTest/kotlin/com/jordankurtz/piawaremobile/map/offline/SqlDelightOfflineTileStoreTest.kt
git commit -m "Track download status per region (DOWNLOADING/COMPLETE/FAILED)"
```

---

## Task 5: Application-scoped download scope

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/DownloadScopeHolder.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/OfflineMapsViewModel.kt`

### Background

Downloads currently run in `viewModelScope`, so they stop when the user navigates away from settings. For offline maps, we want downloads to keep going in the background (within the app's lifetime). The simplest approach: a Koin `@Single` that holds a `CoroutineScope`. It lives until the Koin container is torn down (app death).

`OfflineMapsViewModel` gets this scope injected and uses it for the `launch { engine.download(...) }` call. It still uses `viewModelScope` for non-download coroutines (loading regions, deleting).

A GitHub issue should be filed for WorkManager migration (proper background work that survives app death).

- [ ] **Step 1: Create `DownloadScopeHolder`**

Create `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/DownloadScopeHolder.kt`:

```kotlin
package com.jordankurtz.piawaremobile.map.offline

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.annotation.Single

@Single
class DownloadScopeHolder {
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
```

Note: Koin with `@ComponentScan("com.jordankurtz.piawaremobile")` in `AppModule` will automatically pick this up since it's in the scanned package.

- [ ] **Step 2: Inject `DownloadScopeHolder` into `OfflineMapsViewModel`**

Update `OfflineMapsViewModel`:

```kotlin
@Factory
class OfflineMapsViewModel(
    private val store: OfflineTileStore,
    private val engine: DownloadEngine,
    private val downloadScopeHolder: DownloadScopeHolder,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
```

In `startDownload`, change:
```kotlin
// Before:
viewModelScope.launch {

// After:
downloadScopeHolder.scope.launch {
```

The `try/finally` in `startDownload` still resets `_isDownloading` and `_downloadProgress` — but now this runs in the download scope, not viewModelScope. The UI state flows are still updated from the download scope (they're thread-safe `StateFlow`).

Also update `_regions` refresh — keep that in `viewModelScope` since it drives UI:
```kotlin
// Inside the download finally block, refresh regions in viewModelScope:
viewModelScope.launch(ioDispatcher) { _regions.value = store.getRegions() }
```

Wait — the regions refresh was inside the try block (on success), and `finally` resets state. Keep it as:
```kotlin
try {
    // ... download ...
    // On success, refresh regions:
    withContext(ioDispatcher) { _regions.value = store.getRegions() }
} catch (e: Exception) {
    Logger.e("Download failed for region", e)
} finally {
    _isDownloading.value = false
    _downloadProgress.value = null
}
```

But since this now runs in `downloadScopeHolder.scope`, and `viewModelScope` may be cancelled if the user navigates away, we need `_regions.value = store.getRegions()` to not use `viewModelScope`. It's fine — `_regions` is a `MutableStateFlow` and updating it from any coroutine is safe.

- [ ] **Step 3: Update `OfflineMapsViewModelTest` to inject a `DownloadScopeHolder`**

In the ViewModel test, wherever `OfflineMapsViewModel(store, engine, ioDispatcher)` is constructed, add the new param:
```kotlin
private val downloadScopeHolder = DownloadScopeHolder()
// ...
OfflineMapsViewModel(store, engine, downloadScopeHolder, testDispatcher)
```

Note: `DownloadScopeHolder()` creates a real `CoroutineScope`. In tests, `runTest` controls the test coroutine but the download scope runs on `Dispatchers.Default`. To avoid this, override the scope in tests:

Actually the tests mock `DownloadEngine`, so the download flow is controlled by the mock. The `DownloadScopeHolder` scope just needs to exist. Use the test dispatcher to avoid real concurrency:

```kotlin
private val downloadScope = CoroutineScope(testDispatcher)
private val downloadScopeHolder = DownloadScopeHolder().also {
    // Can't override the scope after construction — create a test-friendly holder instead
}
```

Since `DownloadScopeHolder.scope` is a `val`, subclass won't work. The cleanest fix: make `DownloadScopeHolder` have a constructor parameter with a default:

```kotlin
@Single
class DownloadScopeHolder(
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
)
```

Then in tests:
```kotlin
private val downloadScopeHolder = DownloadScopeHolder(scope = CoroutineScope(testDispatcher))
```

- [ ] **Step 4: Run all unit tests**

```
./gradlew :composeApp:testDebugUnitTest 2>&1 | tail -30
```
Expected: PASS

- [ ] **Step 5: File a GitHub issue for WorkManager**

```bash
gh issue create \
  --title "Migrate offline downloads to WorkManager for true background execution" \
  --body "Downloads currently run in an application-scoped CoroutineScope, so they survive navigation but not app death. For a production implementation, WorkManager would allow downloads to continue even when the app is killed.

Discovered while implementing the application-scoped download scope in the offline maps feature."
```

- [ ] **Step 6: Commit**

```bash
git add \
  composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/DownloadScopeHolder.kt \
  composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/OfflineMapsViewModel.kt \
  composeApp/src/commonTest/kotlin/com/jordankurtz/piawaremobile/map/offline/OfflineMapsViewModelTest.kt
git commit -m "Use application-scoped CoroutineScope for downloads so they survive navigation"
```

---

## Task 6: Region deletion with freed storage and confirmation dialog

**Files:**
- Modify: `composeApp/src/commonMain/sqldelight/com/jordankurtz/piawaremobile/map/cache/TileCache.sq`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/cache/TileCache.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/cache/FileTileCache.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/OfflineTileStore.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/SqlDelightOfflineTileStore.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/OfflineMapsViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/ui/OfflineMapsScreen.kt`
- Modify: `composeApp/src/jvmTest/kotlin/com/jordankurtz/piawaremobile/map/offline/SqlDelightOfflineTileStoreTest.kt`

### Background

When a region is deleted, the `pinned_tile` rows cascade-delete. But the `tile` table (and the files on disk) aren't cleaned up. Tiles exclusively pinned to the deleted region should be removed.

A tile is "exclusively pinned" to region R if it appears in `pinned_tile` only for `region_id = R`.

The SQL query to find these tiles and their total size:

```sql
-- Tiles exclusively pinned to a specific region (not pinned by any other region)
selectExclusivelyPinnedTilesByRegion:
SELECT pt.zoom_level, pt.col, pt.row
FROM pinned_tile pt
WHERE pt.region_id = ?
  AND NOT EXISTS (
    SELECT 1 FROM pinned_tile pt2
    WHERE pt2.zoom_level = pt.zoom_level
      AND pt2.col = pt.col
      AND pt2.row = pt.row
      AND pt2.region_id != pt.region_id
  );

-- Total bytes of tiles exclusively pinned to a specific region
sizeOfExclusivelyPinnedTilesByRegion:
SELECT COALESCE(SUM(t.size_bytes), 0)
FROM tile t
WHERE EXISTS (
  SELECT 1 FROM pinned_tile pt
  WHERE pt.zoom_level = t.zoom_level
    AND pt.col = t.col
    AND pt.row = t.row
    AND pt.region_id = ?
    AND NOT EXISTS (
      SELECT 1 FROM pinned_tile pt2
      WHERE pt2.zoom_level = pt.zoom_level
        AND pt2.col = pt.col
        AND pt2.row = pt.row
        AND pt2.region_id != pt.region_id
    )
);
```

`TileCache` needs a `delete(zoomLvl, col, row)` method to remove a tile from disk + database. `FileTileCache` calls `cacheFileSystem.delete(tileKey)`, `queries.deleteCacheEntry(...)`, and `queries.deleteTile(...)`.

The ViewModel exposes `getFreedBytesForRegion(id): Long` and `deleteRegion(id)` now: first queries exclusive tiles + their size, deletes from cache, then deletes the region.

The UI shows a confirmation dialog: "Delete [name]? This will free ~X MB." with Cancel/Delete buttons.

- [ ] **Step 1: Add SQL queries to `TileCache.sq`**

```sql
selectExclusivelyPinnedTilesByRegion:
SELECT pt.zoom_level, pt.col, pt.row
FROM pinned_tile pt
WHERE pt.region_id = ?
  AND NOT EXISTS (
    SELECT 1 FROM pinned_tile pt2
    WHERE pt2.zoom_level = pt.zoom_level
      AND pt2.col = pt.col
      AND pt2.row = pt.row
      AND pt2.region_id != pt.region_id
  );

sizeOfExclusivelyPinnedTilesByRegion:
SELECT COALESCE(SUM(t.size_bytes), 0)
FROM tile t
WHERE EXISTS (
  SELECT 1 FROM pinned_tile pt
  WHERE pt.zoom_level = t.zoom_level
    AND pt.col = t.col
    AND pt.row = t.row
    AND pt.region_id = ?
    AND NOT EXISTS (
      SELECT 1 FROM pinned_tile pt2
      WHERE pt2.zoom_level = pt.zoom_level
        AND pt2.col = pt.col
        AND pt2.row = pt.row
        AND pt2.region_id != pt.region_id
    )
);
```

- [ ] **Step 2: Add `delete` to `TileCache` interface**

```kotlin
suspend fun delete(zoomLvl: Int, col: Int, row: Int)
```

- [ ] **Step 3: Implement `delete` in `FileTileCache`**

```kotlin
override suspend fun delete(zoomLvl: Int, col: Int, row: Int) {
    withContext(ioDispatcher) {
        val tileKey = tileKey(zoomLvl, col, row)
        cacheFileSystem.delete(tileKey)
        queries.deleteCacheEntry(zoomLvl.toLong(), col.toLong(), row.toLong())
        queries.deleteTile(zoomLvl.toLong(), col.toLong(), row.toLong())
    }
}
```

- [ ] **Step 4: Add `getExclusiveTilesForRegion` and `getFreedBytesForRegion` to `OfflineTileStore`**

```kotlin
suspend fun getExclusiveTilesForRegion(id: Long): List<Triple<Int, Int, Int>>

suspend fun getFreedBytesForRegion(id: Long): Long
```

- [ ] **Step 5: Implement in `SqlDelightOfflineTileStore`**

```kotlin
override suspend fun getExclusiveTilesForRegion(id: Long): List<Triple<Int, Int, Int>> =
    withContext(ioDispatcher) {
        queries.selectExclusivelyPinnedTilesByRegion(id).executeAsList().map {
            Triple(it.zoom_level.toInt(), it.col.toInt(), it.row.toInt())
        }
    }

override suspend fun getFreedBytesForRegion(id: Long): Long =
    withContext(ioDispatcher) {
        queries.sizeOfExclusivelyPinnedTilesByRegion(id).executeAsOne()
    }
```

- [ ] **Step 6: Update `OfflineMapsViewModel.deleteRegion` to clean up cache**

```kotlin
fun deleteRegion(id: Long) {
    if (_isDownloading.value) return
    viewModelScope.launch(ioDispatcher) {
        val exclusiveTiles = store.getExclusiveTilesForRegion(id)
        for ((zoom, col, row) in exclusiveTiles) {
            tileCache.delete(zoom, col, row)
        }
        store.deleteRegion(id)
        _regions.value = store.getRegions()
    }
}
```

This requires `tileCache: TileCache` as a constructor parameter:
```kotlin
@Factory
class OfflineMapsViewModel(
    private val store: OfflineTileStore,
    private val engine: DownloadEngine,
    private val tileCache: TileCache,
    private val downloadScopeHolder: DownloadScopeHolder,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
```

- [ ] **Step 7: Add `pendingDeleteRegion` state and `getFreedBytes` to `OfflineMapsViewModel`**

Add to the ViewModel:
```kotlin
private val _pendingDeleteRegion = MutableStateFlow<OfflineRegion?>(null)
val pendingDeleteRegion: StateFlow<OfflineRegion?> = _pendingDeleteRegion.asStateFlow()

fun requestDeleteRegion(region: OfflineRegion) {
    if (_isDownloading.value) return
    _pendingDeleteRegion.value = region
}

fun cancelDelete() {
    _pendingDeleteRegion.value = null
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
```

Remove the old `deleteRegion(id: Long)` function.

- [ ] **Step 8: Add confirmation dialog string resources**

In `strings.xml`:
```xml
<string name="offline_maps_delete_confirm_title">Delete Region?</string>
<string name="offline_maps_delete_confirm_message">Delete "%1$s"? This will free ~%2$d MB of storage.</string>
<string name="offline_maps_delete_confirm_delete">Delete</string>
<string name="offline_maps_delete_confirm_cancel">Cancel</string>
```

- [ ] **Step 9: Add confirmation dialog to `OfflineMapsScreen`**

In `OfflineMapsScreen` (the stateful composable wired to the ViewModel), add:

```kotlin
// Collect pending delete state
val pendingDelete by vm.pendingDeleteRegion.collectAsState()

// Show confirmation dialog when pending
pendingDelete?.let { region ->
    AlertDialog(
        onDismissRequest = onCancelDelete,
        title = { Text(stringResource(Res.string.offline_maps_delete_confirm_title)) },
        text = {
            Text(
                stringResource(
                    Res.string.offline_maps_delete_confirm_message,
                    region.name,
                    (region.sizeBytes / (1024 * 1024)).toInt(),
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirmDelete) {
                Text(stringResource(Res.string.offline_maps_delete_confirm_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelDelete) {
                Text(stringResource(Res.string.offline_maps_delete_confirm_cancel))
            }
        },
    )
}
```

Wire the callbacks in the stateful `OfflineMapsScreen` composable:
```kotlin
val onRequestDelete = remember(vm) { { region: OfflineRegion -> vm.requestDeleteRegion(region) } }
val onConfirmDelete = remember(vm) { { vm.confirmDelete() } }
val onCancelDelete = remember(vm) { { vm.cancelDelete() } }
```

Update `OfflineMapsScreenContent` to accept `onDeleteRegion: (OfflineRegion) -> Unit` (rename from `onDeleteRegion: (Long) -> Unit` → passes the whole region).

- [ ] **Step 10: Write tests for `getExclusiveTilesForRegion` and `getFreedBytesForRegion`**

In `SqlDelightOfflineTileStoreTest.kt`, add:

```kotlin
@Test
fun `getExclusiveTilesForRegion excludes shared tiles`() =
    runTest(testDispatcher) {
        // Need a tile in the tile table for size queries to work
        // SqlDelightOfflineTileStoreTest uses the real DB so we can insert tiles
        val queries = (store as SqlDelightOfflineTileStore).let {
            // Can't access queries directly — use store methods only.
            // Just test that exclusive tile count is correct via pinning.
        }
        val region1Id = store.saveRegion(baseRegion("R1"))
        val region2Id = store.saveRegion(baseRegion("R2"))

        // Tile (8, 10, 20) pinned by both regions
        store.pinTile(zoomLevel = 8, col = 10, row = 20, regionId = region1Id)
        store.pinTile(zoomLevel = 8, col = 10, row = 20, regionId = region2Id)
        // Tile (8, 11, 20) pinned only by region1
        store.pinTile(zoomLevel = 8, col = 11, row = 20, regionId = region1Id)

        val exclusive = store.getExclusiveTilesForRegion(region1Id)
        assertEquals(1, exclusive.size)
        assertEquals(Triple(8, 11, 20), exclusive[0])
    }

private fun baseRegion(name: String) = OfflineRegion(
    name = name, minZoom = 8, maxZoom = 12,
    minLat = 0.0, maxLat = 1.0, minLon = 0.0, maxLon = 1.0,
    providerId = "osm", createdAt = 1000L,
)
```

Note: `getFreedBytesForRegion` needs tiles in the `tile` table. Since `SqlDelightOfflineTileStoreTest` doesn't insert into `tile` directly (that's `TileCacheQueries`), test this with a value of 0 (no tiles in cache):

```kotlin
@Test
fun `getFreedBytesForRegion returns 0 when no tiles in cache`() =
    runTest(testDispatcher) {
        val id = store.saveRegion(baseRegion("R"))
        store.pinTile(zoomLevel = 8, col = 10, row = 20, regionId = id)
        val freed = store.getFreedBytesForRegion(id)
        assertEquals(0L, freed)
    }
```

- [ ] **Step 11: Run all tests**

```
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest 2>&1 | tail -30
```
Expected: PASS

- [ ] **Step 12: Commit**

```bash
git add \
  composeApp/src/commonMain/sqldelight/com/jordankurtz/piawaremobile/map/cache/TileCache.sq \
  composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/cache/TileCache.kt \
  composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/cache/FileTileCache.kt \
  composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/OfflineTileStore.kt \
  composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/SqlDelightOfflineTileStore.kt \
  composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/offline/OfflineMapsViewModel.kt \
  composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/ui/OfflineMapsScreen.kt \
  composeApp/src/commonMain/composeResources/values/strings.xml \
  composeApp/src/jvmTest/kotlin/com/jordankurtz/piawaremobile/map/offline/SqlDelightOfflineTileStoreTest.kt
git commit -m "Delete exclusive tiles from cache on region deletion, add confirmation dialog"
```

---

## Task 7: Show download status in OfflineMapsScreen region list

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/ui/OfflineMapsScreen.kt`
- Modify: `composeApp/src/desktopTest/kotlin/com/jordankurtz/piawaremobile/settings/ui/OfflineMapsScreenTest.kt`

### Background

Region list items currently show name, zoom range, and size (MB). With status tracking in place, DOWNLOADING regions should show a progress indicator + tile count, and FAILED regions should show an error indicator.

- [ ] **Step 1: Write the failing desktop test**

In the existing `OfflineMapsScreenTest.kt` (or create it if it doesn't exist), add:

```kotlin
@Test
fun `downloading region shows progress indicator`() = runComposeUiTest {
    val region = OfflineRegion(
        id = 1L, name = "My Region", minZoom = 8, maxZoom = 14,
        minLat = 37.0, maxLat = 38.0, minLon = -122.0, maxLon = -121.0,
        providerId = "osm", createdAt = 1000L,
        tileCount = 100L, sizeBytes = 1_500_000L,
        status = DownloadStatus.DOWNLOADING,
        downloadedTileCount = 40L,
    )
    setContent {
        OfflineMapsScreenContent(
            regions = listOf(region),
            onDeleteRegion = {},
        )
    }
    onNodeWithContentDescription("Downloading", substring = true).assertExists()
}
```

- [ ] **Step 2: Run to confirm failure**

```
./gradlew :composeApp:desktopTest --tests "*.OfflineMapsScreenTest" 2>&1 | tail -20
```

- [ ] **Step 3: Update `OfflineRegionItem` to show status**

In `OfflineMapsScreen.kt`, update `OfflineRegionItem` to show a downloading indicator when `region.status == DownloadStatus.DOWNLOADING`:

```kotlin
@Composable
private fun OfflineRegionItem(
    region: OfflineRegion,
    onDelete: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(region.name) },
        supportingContent = {
            Column {
                Text(
                    text = stringResource(Res.string.offline_maps_region_zoom, region.minZoom, region.maxZoom),
                )
                when (region.status) {
                    DownloadStatus.DOWNLOADING -> {
                        val fraction = if (region.tileCount > 0L) {
                            region.downloadedTileCount.toFloat() / region.tileCount.toFloat()
                        } else {
                            0f
                        }
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        )
                        Text(
                            text = "${region.downloadedTileCount} / ${region.tileCount} tiles",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    DownloadStatus.FAILED -> {
                        Text(
                            text = stringResource(Res.string.offline_maps_region_download_failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    DownloadStatus.COMPLETE -> {
                        Text(
                            text = stringResource(
                                Res.string.offline_maps_region_size,
                                (region.sizeBytes / (1024 * 1024)).toInt(),
                            ),
                        )
                    }
                }
            }
        },
        trailingContent = {
            if (region.status != DownloadStatus.DOWNLOADING) {
                IconButton(onClick = onDelete) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_delete),
                        contentDescription = stringResource(Res.string.offline_maps_region_delete),
                    )
                }
            }
        },
    )
}
```

Add string resource to `strings.xml`:
```xml
<string name="offline_maps_region_download_failed">Download failed — tap to retry</string>
```

Also add the `contentDescription` to the `LinearProgressIndicator` (Compose M3 doesn't have a built-in one, use a `Modifier.semantics`):

```kotlin
LinearProgressIndicator(
    progress = { fraction },
    modifier = Modifier
        .fillMaxWidth()
        .padding(top = 4.dp)
        .semantics { contentDescription = "Downloading" },
)
```

- [ ] **Step 4: Run desktop tests**

```
./gradlew :composeApp:desktopTest 2>&1 | tail -30
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add \
  composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/settings/ui/OfflineMapsScreen.kt \
  composeApp/src/commonMain/composeResources/values/strings.xml \
  composeApp/src/desktopTest/kotlin/com/jordankurtz/piawaremobile/settings/ui/OfflineMapsScreenTest.kt
git commit -m "Show download status (progress/failed/complete) per region in offline maps list"
```

---

## Task 8: Final checks and PR

**Files:**
- No code changes — verify, rebase, push, open PR

- [ ] **Step 1: Run full local test suite**

```bash
./gradlew ktlintCheck 2>&1 | tail -20
# If failures: ./gradlew ktlintFormat && recheck

./gradlew detekt 2>&1 | tail -20

./gradlew :composeApp:testDebugUnitTest 2>&1 | tail -30

./gradlew :composeApp:desktopTest 2>&1 | tail -30
```

All must pass.

- [ ] **Step 2: Interactive rebase to create logical commits**

Review all commits on the branch and squash/rename into clean, logical commits:
```bash
git log --oneline origin/main..HEAD
git rebase -i origin/main
```

Target commit structure (one per PR convention):
1. `Add TileCoordinateUtils: lat/lon to tile math and region enumeration`
2. `Add TileProviderConfig, DownloadProgress, and updateRegionStats to OfflineTileStore`
3. `Implement OfflineDownloadEngine: rate-limited tile download with pin-on-complete`
4. `Add OfflineMapsViewModel and wire OfflineMapsScreen to real data`
5. `Fix pinned tile expiry, add offlineHits stat`
6. `Add invertProjection and screenToLatLon; fix MapRegionPickerScreen to use real bounds`
7. `Show live tile count estimate in DownloadRegionDialog`
8. `Track download status per region (DOWNLOADING/COMPLETE/FAILED)`
9. `Keep downloads alive across navigation with application-scoped CoroutineScope`
10. `Free exclusive tiles on region deletion, add confirmation dialog`
11. `Show per-region download status in offline maps list`

- [ ] **Step 3: Force-push the rebased branch**

```bash
git push --force-with-lease origin jk/offline-maps-download
```

- [ ] **Step 4: Open PR targeting `main`**

```bash
gh pr create \
  --title "Offline maps: complete end-to-end feature" \
  --base main \
  --body "$(cat <<'EOF'
## Summary

- Fixes critical bug: `MapRegionPickerScreen` was returning hardcoded SF Bay Area bounds; now correctly converts screen pixels to lat/lon using viewport scroll/scale
- Adds live storage estimate in `DownloadRegionDialog` (~MB, tile count)
- Tracks download status per region (`DOWNLOADING`/`COMPLETE`/`FAILED`) via schema migration
- Downloads survive navigation (application-scoped `CoroutineScope`); WorkManager follow-up filed
- Frees exclusively-pinned tiles on region deletion with confirmation dialog
- Shows per-region download progress/status in the offline maps list

Closes #<issue-number>

## Test plan

- [ ] All unit tests pass (`./gradlew :composeApp:testDebugUnitTest`)
- [ ] All desktop UI tests pass (`./gradlew :composeApp:desktopTest`)
- [ ] ktlint and detekt pass
- [ ] Android instrumented tests run in CI

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

---

## Self-Review

### Spec coverage

- [x] Fix mock bounds bug → Tasks 1 + 2
- [x] Live estimate → Task 3
- [x] Download status tracking (schema + UI) → Tasks 4 + 7
- [x] Application-scoped download → Task 5
- [x] Freed tile cleanup + confirmation → Task 6
- [x] PR to `main` → Task 8

### Potential issues

1. **Task 4, `OfflineDownloadEngine`**: The engine now calls `updateDownloadStatus` on every tile (inside the loop). This is 1 DB write per tile — could be slow for large regions. Acceptable for correctness; a follow-up could batch or debounce these updates.

2. **Task 5, `DownloadScopeHolder` + `Dispatchers.Default`**: Uses `Dispatchers.Default` which is fine for CPU-light coordination. The actual I/O in the engine uses `withContext(ioDispatcher)` already.

3. **Task 6, test for `getExclusiveTilesForRegion`**: The test accesses `store` as `SqlDelightOfflineTileStore` — but `store` is typed as `OfflineTileStore`. Use a class-level property of the concrete type:
   ```kotlin
   private lateinit var store: SqlDelightOfflineTileStore
   ```
   (already the case in `setUp` — verify this is consistent throughout the test file).

4. **Task 6, `OfflineMapsViewModel` now takes `TileCache`**: The mock test needs to add a `TileCache` mock. Update `OfflineMapsViewModelTest`:
   ```kotlin
   private lateinit var tileCache: TileCache
   // In setUp:
   tileCache = mock()
   // In constructor:
   OfflineMapsViewModel(store, engine, tileCache, downloadScopeHolder, testDispatcher)
   ```

5. **Task 7, `ic_delete` drawable**: Check that `Res.drawable.ic_delete` exists. If the existing delete icon is named differently, update the reference.
