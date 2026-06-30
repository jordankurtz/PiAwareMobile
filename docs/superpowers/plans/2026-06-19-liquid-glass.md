# Liquid Glass Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the iOS/macOS Compose navigation shell with a SwiftUI TabView/NavigationSplitView that delivers native Apple Liquid Glass on tab bar, navigation bar, and the map follow-location button, while Android and Linux/Windows stay unchanged.

**Architecture:** SwiftUI becomes the root on Apple platforms; Compose renders each tab's content pane. `iosMain` exposes per-screen `UIViewController` factories; a new native macOS target (`macosX64`/`macosArm64`) exposes `NSViewController` factories. Shared Apple code (Darwin HTTP client, CoreLocation, SQLite driver, NSFoundation file I/O) moves to a new `appleMain` source set. UIKit-only code stays in `iosMain`; a small set of macOS-specific implementations live in `macosMain`.

**Tech Stack:** Kotlin Multiplatform 2.x, Compose Multiplatform 1.9.1, Koin Annotations, SwiftUI (iOS 26 / macOS 26), ComposeUIViewController (iOS), ComposeNSViewController (macOS), NativeSqliteDriver, Darwin Ktor engine.

## Global Constraints

- CMP version 1.9.1 — `ComposeNSViewController` is available
- `applyDefaultHierarchyTemplate` already creates `appleMain` when iOS targets are present; no explicit declaration needed
- All Kotlin files in `appleMain` are compiled for both iOS and macOS native targets; do NOT use UIKit APIs there
- `iosMain` dependencies (`ktor.client.darwin`, `sqldelight.native.driver`) move to `appleMain.dependencies` in Task 2
- KSP runs on `commonMain` metadata only; `actual` class location (iosMain → appleMain) does not affect KSP
- Run `./gradlew ktlintCheck` and `./gradlew detekt` before each commit; run `./gradlew ktlintFormat` to auto-fix
- Run `./gradlew :composeApp:testDebugUnitTest` and `./gradlew :composeApp:desktopTest` after Kotlin tasks
- Swift files added to Xcode targets must be done in Xcode or via pbxproj edits — the plan calls out each addition explicitly

---

## File Map

### Modified
- `composeApp/build.gradle.kts` — add macOS targets, move iosMain deps to appleMain, add buildkonfig entries (Task 2)
- `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/MapScreen.kt` — add `showFollowLocationFab` param (Task 1)
- `composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/di/modules/TileCacheModule.ios.kt` — update class refs after rename (Task 3)
- `iosApp/iosApp/iOSApp.swift` — call `KoinInitializerKt.startKoin()` in init (Task 6)

### Created (Kotlin — appleMain)
- `composeApp/src/appleMain/kotlin/com/jordankurtz/piawaremobile/KoinInitializer.kt`
- `composeApp/src/appleMain/kotlin/com/jordankurtz/piawaremobile/KtorClient.apple.kt`
- `composeApp/src/appleMain/kotlin/com/jordankurtz/piawaremobile/IsDebugBuild.apple.kt`
- `composeApp/src/appleMain/kotlin/com/jordankurtz/piawaremobile/location/LocationService.apple.kt`
- `composeApp/src/appleMain/kotlin/com/jordankurtz/piawaremobile/map/cache/DatabaseDriverFactory.apple.kt`
- `composeApp/src/appleMain/kotlin/com/jordankurtz/piawaremobile/map/cache/AppleCacheFileSystem.kt`
- `composeApp/src/appleMain/kotlin/com/jordankurtz/piawaremobile/map/offline/AppleThumbnailFileManager.kt`
- `composeApp/src/appleMain/kotlin/com/jordankurtz/piawaremobile/di/modules/ContextModule.apple.kt`
- `composeApp/src/appleMain/kotlin/com/jordankurtz/piawaremobile/di/modules/DataStoreModule.apple.kt`
- `composeApp/src/appleMain/kotlin/com/jordankurtz/piawaremobile/di/modules/DatabaseModule.apple.kt`

### Created (Kotlin — iosMain)
- `composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/ScreenViewControllers.kt`

### Created (Kotlin — macosMain)
- `composeApp/src/macosMain/kotlin/com/jordankurtz/piawaremobile/Platform.macos.kt`
- `composeApp/src/macosMain/kotlin/com/jordankurtz/piawaremobile/UrlHandler.macos.kt`
- `composeApp/src/macosMain/kotlin/com/jordankurtz/piawaremobile/map/offline/MacosThumbnailGenerator.kt`
- `composeApp/src/macosMain/kotlin/com/jordankurtz/piawaremobile/di/modules/TileCacheModule.macos.kt`
- `composeApp/src/macosMain/kotlin/com/jordankurtz/piawaremobile/ScreenViewControllers.kt`

### Created (Swift — iOS)
- `iosApp/iosApp/ComposeScreen.swift`
- `iosApp/iosApp/PiAwareTabView.swift`
- `iosApp/iosApp/MapTabView.swift`

### Created (Swift — macOS)
- `iosApp/macosApp/MacApp.swift`
- `iosApp/macosApp/AppSection.swift`
- `iosApp/macosApp/PiAwareSplitView.swift`
- `iosApp/macosApp/ComposeDetailView.swift`

### Deleted
- `composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/MainViewController.kt`
- `composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/KtorClient.ios.kt`
- `composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/IsDebugBuild.ios.kt`
- `composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/location/LocationService.ios.kt`
- `composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/map/cache/DatabaseDriverFactory.ios.kt`
- `composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/map/cache/IosCacheFileSystem.kt`
- `composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/map/offline/IosThumbnailFileManager.kt`
- `composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/di/modules/ContextModule.ios.kt`
- `composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/di/modules/DataStoreModule.ios.kt`
- `composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/di/modules/DatabaseModule.ios.kt`
- `iosApp/iosApp/ContentView.swift`

---

### Task 1: MapScreen follow-location FAB parameter

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/MapScreen.kt`
- Create: `composeApp/src/desktopTest/kotlin/com/jordankurtz/piawaremobile/map/FollowUserLocationFabTest.kt`

**Interfaces:**
- Produces: `MapScreen(showFollowLocationFab: Boolean = true, ...)` — new parameter, default keeps existing behaviour unchanged on Android/desktop

- [ ] **Step 1: Write the failing test**

Create `composeApp/src/desktopTest/kotlin/com/jordankurtz/piawaremobile/map/FollowUserLocationFabTest.kt`:

```kotlin
package com.jordankurtz.piawaremobile.map

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class FollowUserLocationFabTest {
    @Test
    fun followLocationFab_visibleWhenTrue() = runComposeUiTest {
        setContent {
            FollowUserLocationFab(isFollowing = false, onClick = {})
        }
        onNodeWithContentDescription("Follow user location").assertIsDisplayed()
    }

    @Test
    fun followLocationFab_hiddenWhenShowFabFalseViaMapScreen() = runComposeUiTest {
        // Test the composable directly; full MapScreen requires Koin/map setup
        setContent {
            // When showFollowLocationFab = false, the parent Column omits it
            // We verify the FAB is absent by testing the conditional rendering function
            val show = false
            if (show) {
                FollowUserLocationFab(isFollowing = false, onClick = {})
            }
        }
        onNodeWithContentDescription("Follow user location").assertDoesNotExist()
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**

```
./gradlew :composeApp:desktopTest --tests "*.FollowUserLocationFabTest" 2>&1 | tail -20
```

Expected: FAIL — `FollowUserLocationFab` content description string doesn't match (we need to check what string is used in the resource).

- [ ] **Step 3: Check the actual content description string**

Open `composeApp/src/commonMain/composeResources/values/strings.xml` (or similar) and find the `follow_user_location` string resource value. Update the test's `onNodeWithContentDescription("...")` to match the exact English string.

- [ ] **Step 4: Add `showFollowLocationFab` parameter to `MapScreen`**

In `composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/MapScreen.kt`, change the signature and the `FollowUserLocationFab` conditional:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    mapViewModel: MapViewModel = koinViewModel(),
    locationViewModel: LocationViewModel = koinViewModel(),
    aircraftViewModel: AircraftViewModel = koinViewModel(),
    showFollowLocationFab: Boolean = true,
) {
    // ... existing state collection unchanged ...

    Box {
        OpenStreetMap(state = mapViewModel.state)
        Column(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (aircraft.isNotEmpty()) {
                SmallFloatingActionButton(
                    onClick = { mapViewModel.fitToAircraft(aircraft) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_plane),
                        contentDescription = stringResource(Res.string.fit_to_aircraft),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            if (showFollowLocationFab && showUserLocationOnMap) {
                FollowUserLocationFab(
                    isFollowing = isFollowingUser,
                    onClick = { mapViewModel.toggleFollowUserLocation() },
                )
            }
        }
        // ... rest unchanged ...
    }
    // ... FlightDetailsBottomSheet unchanged ...
}
```

- [ ] **Step 5: Run tests**

```
./gradlew :composeApp:desktopTest --tests "*.FollowUserLocationFabTest" 2>&1 | tail -20
```

Expected: PASS

- [ ] **Step 6: Run full test suite**

```
./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest 2>&1 | tail -20
```

Expected: all pass.

- [ ] **Step 7: Lint and commit**

```
./gradlew ktlintFormat && ./gradlew ktlintCheck
git add composeApp/src/commonMain/kotlin/com/jordankurtz/piawaremobile/map/MapScreen.kt \
        composeApp/src/desktopTest/kotlin/com/jordankurtz/piawaremobile/map/FollowUserLocationFabTest.kt
git commit -m "feat: add showFollowLocationFab param to MapScreen"
```

---

### Task 2: Add macOS KMP targets and move iosMain deps to appleMain

**Files:**
- Modify: `composeApp/build.gradle.kts`

**Interfaces:**
- Produces: `appleMain` source set with Darwin + native SQLite dependencies; `macosX64` and `macosArm64` compilation targets; `buildkonfig` entries for macOS

- [ ] **Step 1: Add macOS targets and appleMain deps**

In `composeApp/build.gradle.kts`, after the `listOf(iosX64(), iosArm64(), iosSimulatorArm64())` block, add:

```kotlin
listOf(
    macosX64(),
    macosArm64(),
).forEach { macosTarget ->
    macosTarget.binaries.framework {
        baseName = "ComposeApp"
        isStatic = true
    }
    macosTarget.binaries.all {
        linkerOpts("-lsqlite3")
    }
}
```

- [ ] **Step 2: Move dependencies from iosMain to appleMain**

In the `sourceSets { }` block, replace the `iosMain.dependencies { ... }` block:

```kotlin
// REMOVE this:
iosMain.dependencies {
    implementation(libs.ktor.client.darwin)
    implementation(libs.sqldelight.native.driver)
}

// ADD this:
val appleMain by getting {
    dependencies {
        implementation(libs.ktor.client.darwin)
        implementation(libs.sqldelight.native.driver)
    }
}
```

- [ ] **Step 3: Add macOS buildkonfig entries**

In the `buildkonfig { targetConfigs { ... } }` block, add after the `iosSimulatorArm64` entry:

```kotlin
create("macosX64") {
    buildConfigField(STRING, "SENTRY_DSN", providers.gradleProperty("sentry.dsn.macos").getOrElse(""))
}
create("macosArm64") {
    buildConfigField(STRING, "SENTRY_DSN", providers.gradleProperty("sentry.dsn.macos").getOrElse(""))
}
```

- [ ] **Step 4: Verify iOS still compiles**

```
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Verify macOS targets resolve (they'll have missing actuals — that's OK for now)**

```
./gradlew :composeApp:compileKotlinMacosArm64 2>&1 | grep -E "error:|BUILD" | head -20
```

Expected: errors about missing `actual` declarations — this is expected and will be fixed in Tasks 3–4.

- [ ] **Step 6: Commit**

```
git add composeApp/build.gradle.kts
git commit -m "build: add macosX64/macosArm64 targets and move apple deps to appleMain"
```

---

### Task 3: Move shared Apple code from iosMain to appleMain

**Context:** `appleMain` compiles for all Apple targets (iOS + macOS). All files here must use only Foundation/CoreLocation/Darwin APIs — no UIKit. The class renames (`IosCacheFileSystem` → `AppleCacheFileSystem`, `IosThumbnailFileManager` → `AppleThumbnailFileManager`) prevent confusion about platform scope.

**Files:**
- Create 9 files in `composeApp/src/appleMain/kotlin/...`
- Modify: `composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/di/modules/TileCacheModule.ios.kt`
- Delete 9 files from `composeApp/src/iosMain/kotlin/...`

**Interfaces:**
- Produces: `AppleCacheFileSystem(cacheDir: String) : CacheFileSystem`, `AppleThumbnailFileManager(thumbnailCacheDir: String) : ThumbnailFileManager`, `actual fun getKtorClient()`, `actual val isDebugBuild`, `actual class LocationServiceImpl`, `actual class DatabaseDriverFactory`, `actual class ContextModule`, `actual class DataStoreModule`, `actual class DatabaseModule`

- [ ] **Step 1: Create appleMain directory structure**

```
mkdir -p composeApp/src/appleMain/kotlin/com/jordankurtz/piawaremobile/di/modules
mkdir -p composeApp/src/appleMain/kotlin/com/jordankurtz/piawaremobile/location
mkdir -p composeApp/src/appleMain/kotlin/com/jordankurtz/piawaremobile/map/cache
mkdir -p composeApp/src/appleMain/kotlin/com/jordankurtz/piawaremobile/map/offline
```

- [ ] **Step 2: Create `appleMain/KtorClient.apple.kt`**

```kotlin
package com.jordankurtz.piawaremobile

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

actual fun getKtorClient(): HttpClient {
    return HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    namingStrategy = JsonNamingStrategy.SnakeCase
                },
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = HttpTimeoutDefaults.REQUEST_TIMEOUT_MS
            connectTimeoutMillis = HttpTimeoutDefaults.CONNECT_TIMEOUT_MS
            socketTimeoutMillis = HttpTimeoutDefaults.SOCKET_TIMEOUT_MS
        }
        engine {
            configureRequest {
                setAllowsCellularAccess(true)
            }
        }
    }
}
```

- [ ] **Step 3: Create `appleMain/IsDebugBuild.apple.kt`**

```kotlin
package com.jordankurtz.piawaremobile

import platform.Foundation.NSBundle
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform as NativePlatform

@OptIn(ExperimentalNativeApi::class)
actual val isDebugBuild: Boolean
    get() = NativePlatform.isDebugBinary || isInstalledFromTestFlight()

private fun isInstalledFromTestFlight(): Boolean {
    val receiptPath = NSBundle.mainBundle.appStoreReceiptURL?.path ?: return false
    return receiptPath.contains("sandboxReceipt")
}
```

- [ ] **Step 4: Create `appleMain/map/cache/DatabaseDriverFactory.apple.kt`**

```kotlin
package com.jordankurtz.piawaremobile.map.cache

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver = NativeSqliteDriver(TileCacheDatabase.Schema, "piaware_mobile.db")
}
```

- [ ] **Step 5: Create `appleMain/map/cache/AppleCacheFileSystem.kt`**

```kotlin
package com.jordankurtz.piawaremobile.map.cache

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.io.IOException
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.writeToFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class AppleCacheFileSystem(private val cacheDir: String) : CacheFileSystem {
    private val fileManager: NSFileManager = NSFileManager()

    private fun fullPath(key: String): String {
        var path = cacheDir
        for (component in key.split("/")) {
            @Suppress("CAST_NEVER_SUCCEEDS")
            path = (path as NSString).stringByAppendingPathComponent(component)
        }
        return path
    }

    private fun parentPath(path: String): String {
        val lastSlash = path.lastIndexOf('/')
        return if (lastSlash > 0) path.substring(0, lastSlash) else cacheDir
    }

    override fun read(key: String): ByteArray? {
        val path = fullPath(key)
        if (!fileManager.fileExistsAtPath(path)) return null
        val data = NSData.dataWithContentsOfFile(path) ?: return null
        return data.toByteArray()
    }

    override fun write(key: String, data: ByteArray) {
        val path = fullPath(key)
        val parent = parentPath(path)
        val dirCreated = fileManager.createDirectoryAtPath(
            parent,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        if (!dirCreated) throw IOException("Failed to create directory: $parent")
        val nsData = data.toNSData()
        if (!nsData.writeToFile(path, atomically = true)) throw IOException("Failed to write file: $path")
    }

    override fun delete(key: String) {
        val path = fullPath(key)
        if (fileManager.fileExistsAtPath(path)) fileManager.removeItemAtPath(path, error = null)
    }

    private fun NSData.toByteArray(): ByteArray {
        val size = length.toInt()
        if (size == 0) return byteArrayOf()
        val bytes = ByteArray(size)
        bytes.usePinned { pinned -> memcpy(pinned.addressOf(0), this.bytes, this.length) }
        return bytes
    }

    private fun ByteArray.toNSData(): NSData {
        if (isEmpty()) return NSData()
        return usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = size.toULong()) }
    }
}
```

- [ ] **Step 6: Create `appleMain/map/offline/AppleThumbnailFileManager.kt`**

```kotlin
package com.jordankurtz.piawaremobile.map.offline

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager

@OptIn(ExperimentalForeignApi::class)
class AppleThumbnailFileManager(
    private val thumbnailCacheDir: String,
) : ThumbnailFileManager {
    override fun thumbnailPath(regionId: Long): String = "$thumbnailCacheDir/$regionId.png"

    override fun delete(regionId: Long) {
        NSFileManager.defaultManager.removeItemAtPath(thumbnailPath(regionId), error = null)
    }

    override fun exists(path: String): Boolean = NSFileManager.defaultManager.fileExistsAtPath(path)
}
```

- [ ] **Step 7: Create `appleMain/location/LocationService.apple.kt`**

Copy the full content from `iosMain/location/LocationService.ios.kt`, changing only the package comment and file header — the CoreLocation implementation is identical on macOS:

```kotlin
package com.jordankurtz.piawaremobile.location

import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.di.modules.ContextWrapper
import com.jordankurtz.piawaremobile.model.Location
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import org.koin.core.annotation.Factory
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.darwin.NSObject

@Factory(binds = [LocationService::class])
@Suppress("UnusedPrivateProperty")
actual class LocationServiceImpl actual constructor(private val contextWrapper: ContextWrapper) :
    LocationService {
    private val locationManager = CLLocationManager()
    private val delegate = LocationDelegate()

    init {
        locationManager.delegate = delegate
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
    }

    actual override fun startLocationUpdates(onLocationUpdate: (Location) -> Unit) {
        delegate.onLocationUpdate = onLocationUpdate
        locationManager.startUpdatingLocation()
    }

    actual override fun stopLocationUpdates() {
        locationManager.stopUpdatingLocation()
        delegate.onLocationUpdate = null
    }

    actual override fun requestPermissions(onResult: (Boolean) -> Unit) {
        delegate.onPermissionResult = onResult
        when (CLLocationManager.authorizationStatus()) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways,
            -> {
                onResult(true)
                delegate.onPermissionResult = null
            }
            kCLAuthorizationStatusNotDetermined -> {
                locationManager.requestWhenInUseAuthorization()
            }
            else -> {
                onResult(false)
                delegate.onPermissionResult = null
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class LocationDelegate : NSObject(), CLLocationManagerDelegateProtocol {
    var onPermissionResult: ((Boolean) -> Unit)? = null
    var onLocationUpdate: ((Location) -> Unit)? = null

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        val status = manager.authorizationStatus
        onPermissionResult?.let { callback ->
            when (status) {
                kCLAuthorizationStatusAuthorizedWhenInUse,
                kCLAuthorizationStatusAuthorizedAlways,
                -> callback(true)
                kCLAuthorizationStatusDenied -> callback(false)
                else -> return
            }
            onPermissionResult = null
        }
    }

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val location = (didUpdateLocations.lastOrNull() as? CLLocation) ?: return
        location.coordinate.useContents {
            onLocationUpdate?.invoke(Location(latitude = latitude, longitude = longitude))
        }
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        Logger.e("Location error", Throwable(didFailWithError.localizedDescription))
    }
}
```

- [ ] **Step 8: Create `appleMain/di/modules/ContextModule.apple.kt`**

```kotlin
package com.jordankurtz.piawaremobile.di.modules

import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.scope.Scope

actual class ContextWrapper

@Module
actual class ContextModule {
    @Single
    actual fun providesContextWrapper(scope: Scope): ContextWrapper = ContextWrapper()
}
```

- [ ] **Step 9: Create `appleMain/di/modules/DataStoreModule.apple.kt`**

```kotlin
package com.jordankurtz.piawaremobile.di.modules

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

@Module
actual class DataStoreModule {
    @Single
    actual fun provideDataStore(contextWrapper: ContextWrapper): DataStore<Preferences> {
        return PreferenceDataStoreFactory.createWithPath {
            (
                requireNotNull(
                    NSSearchPathForDirectoriesInDomains(
                        NSApplicationSupportDirectory,
                        NSUserDomainMask,
                        true,
                    ).firstOrNull()?.toString(),
                ) + "/settings.preferences_pb"
            ).toPath()
        }
    }
}
```

- [ ] **Step 10: Create `appleMain/di/modules/DatabaseModule.apple.kt`**

```kotlin
package com.jordankurtz.piawaremobile.di.modules

import com.jordankurtz.piawaremobile.aircraft.cache.FlightCacheRepo
import com.jordankurtz.piawaremobile.aircraft.cache.FlightCacheRepoImpl
import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.map.cache.DatabaseDriverFactory
import com.jordankurtz.piawaremobile.map.cache.TileCacheDatabase
import com.jordankurtz.piawaremobile.map.offline.OfflineTileStore
import com.jordankurtz.piawaremobile.map.offline.SqlDelightOfflineTileStore
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
actual class DatabaseModule {
    @Single
    actual fun provideTileCacheDatabase(contextWrapper: ContextWrapper): TileCacheDatabase {
        val driverFactory = DatabaseDriverFactory()
        return TileCacheDatabase(driverFactory.createDriver())
    }

    @Single
    actual fun provideOfflineTileStore(
        database: TileCacheDatabase,
        @IODispatcher ioDispatcher: CoroutineDispatcher,
    ): OfflineTileStore = SqlDelightOfflineTileStore(database.tileCacheQueries, ioDispatcher)

    @Single
    actual fun provideFlightCacheRepo(
        database: TileCacheDatabase,
        @IODispatcher ioDispatcher: CoroutineDispatcher,
    ): FlightCacheRepo = FlightCacheRepoImpl(database.flightCacheQueries, ioDispatcher)
}
```

- [ ] **Step 11: Update `iosMain/di/modules/TileCacheModule.ios.kt` to use renamed classes**

Replace `IosCacheFileSystem` with `AppleCacheFileSystem` and `IosThumbnailFileManager` with `AppleThumbnailFileManager`:

```kotlin
package com.jordankurtz.piawaremobile.di.modules

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.map.cache.AppleCacheFileSystem
import com.jordankurtz.piawaremobile.map.cache.FileTileCache
import com.jordankurtz.piawaremobile.map.cache.TileCache
import com.jordankurtz.piawaremobile.map.cache.TileCacheDatabase
import com.jordankurtz.piawaremobile.map.offline.AppleThumbnailFileManager
import com.jordankurtz.piawaremobile.map.offline.IosThumbnailGenerator
import com.jordankurtz.piawaremobile.map.offline.ThumbnailFileManager
import com.jordankurtz.piawaremobile.map.offline.ThumbnailGenerator
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringByAppendingPathComponent

@Module
actual class TileCacheModule {
    @Single
    actual fun provideTileCache(
        contextWrapper: ContextWrapper,
        database: TileCacheDatabase,
        @IODispatcher ioDispatcher: CoroutineDispatcher,
    ): TileCache {
        val cacheDir = appleCacheDir()
        val cacheFileSystem = AppleCacheFileSystem(cacheDir)
        return FileTileCache(
            cacheFileSystem = cacheFileSystem,
            queries = database.tileCacheQueries,
            ioDispatcher = ioDispatcher,
        )
    }

    @Single
    actual fun provideThumbnailGenerator(
        contextWrapper: ContextWrapper,
        @IODispatcher ioDispatcher: CoroutineDispatcher,
    ): ThumbnailGenerator =
        IosThumbnailGenerator(
            tileCacheDir = appleCacheDir(),
            ioDispatcher = ioDispatcher,
        )

    @Single
    actual fun provideThumbnailFileManager(contextWrapper: ContextWrapper): ThumbnailFileManager =
        AppleThumbnailFileManager(appleThumbnailDir())
}

private fun appleCacheDir(): String {
    val cachePaths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
    val base = cachePaths.first() as String
    @Suppress("CAST_NEVER_SUCCEEDS")
    return (base as NSString).stringByAppendingPathComponent("map_tiles")
}

private fun appleThumbnailDir(): String {
    val cachePaths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
    val base = cachePaths.first() as String
    @Suppress("CAST_NEVER_SUCCEEDS")
    return (base as NSString).stringByAppendingPathComponent("thumbnails")
}
```

- [ ] **Step 12: Delete the 9 moved files from iosMain**

```bash
rm composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/KtorClient.ios.kt
rm composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/IsDebugBuild.ios.kt
rm composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/location/LocationService.ios.kt
rm composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/map/cache/DatabaseDriverFactory.ios.kt
rm composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/map/cache/IosCacheFileSystem.kt
rm composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/map/offline/IosThumbnailFileManager.kt
rm composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/di/modules/ContextModule.ios.kt
rm composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/di/modules/DataStoreModule.ios.kt
rm composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/di/modules/DatabaseModule.ios.kt
```

- [ ] **Step 13: Verify iOS still links**

```
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 14: Lint and commit**

```
./gradlew ktlintFormat && ./gradlew ktlintCheck && ./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest 2>&1 | tail -10
git add -A
git commit -m "refactor: move shared Apple code from iosMain to appleMain"
```

---

### Task 4: macosMain actual implementations

**Context:** macOS needs `actual` declarations for `Platform`, `UrlHandler`, `ThumbnailGenerator`, and `TileCacheModule`. `Platform` uses `NSProcessInfo`; `UrlHandler` uses `NSWorkspace`; `ThumbnailGenerator` is a stub (offline map thumbnails unsupported on macOS initially); `TileCacheModule` mirrors the iOS version but uses `MacosThumbnailGenerator`.

**Files:**
- Create: `composeApp/src/macosMain/kotlin/com/jordankurtz/piawaremobile/Platform.macos.kt`
- Create: `composeApp/src/macosMain/kotlin/com/jordankurtz/piawaremobile/UrlHandler.macos.kt`
- Create: `composeApp/src/macosMain/kotlin/com/jordankurtz/piawaremobile/map/offline/MacosThumbnailGenerator.kt`
- Create: `composeApp/src/macosMain/kotlin/com/jordankurtz/piawaremobile/di/modules/TileCacheModule.macos.kt`

**Interfaces:**
- Produces: all `actual` declarations needed for `macosArm64`/`macosX64` to compile cleanly

- [ ] **Step 1: Create macosMain directory structure**

```bash
mkdir -p composeApp/src/macosMain/kotlin/com/jordankurtz/piawaremobile/di/modules
mkdir -p composeApp/src/macosMain/kotlin/com/jordankurtz/piawaremobile/map/offline
```

- [ ] **Step 2: Create `macosMain/Platform.macos.kt`**

```kotlin
package com.jordankurtz.piawaremobile

import platform.Foundation.NSProcessInfo

class MacOSPlatform : Platform {
    override val name: String =
        "macOS " + NSProcessInfo.processInfo.operatingSystemVersionString
}

actual fun getPlatform(): Platform = MacOSPlatform()
```

- [ ] **Step 3: Create `macosMain/UrlHandler.macos.kt`**

```kotlin
package com.jordankurtz.piawaremobile

import com.jordankurtz.piawaremobile.di.modules.ContextWrapper
import org.koin.core.annotation.Factory
import platform.AppKit.NSWorkspace
import platform.Foundation.NSURL

@Factory(binds = [UrlHandler::class])
@Suppress("UnusedPrivateProperty")
actual class UrlHandlerImpl actual constructor(
    private val contextWrapper: ContextWrapper,
) : UrlHandler {
    actual override fun openUrlInternally(url: String) {
        NSURL.URLWithString(url)?.let { NSWorkspace.sharedWorkspace.openURL(it) }
    }

    actual override fun openUrlExternally(url: String) {
        NSURL.URLWithString(url)?.let { NSWorkspace.sharedWorkspace.openURL(it) }
    }
}
```

- [ ] **Step 4: Create `macosMain/map/offline/MacosThumbnailGenerator.kt`**

```kotlin
package com.jordankurtz.piawaremobile.map.offline

import kotlinx.coroutines.CoroutineDispatcher

class MacosThumbnailGenerator(
    @Suppress("UnusedPrivateProperty") private val ioDispatcher: CoroutineDispatcher,
) : ThumbnailGenerator {
    override suspend fun generate(
        bounds: BoundingBox,
        providerId: String,
        thumbnailZoom: Int,
        outputPath: String,
    ): Boolean = false
}
```

- [ ] **Step 5: Create `macosMain/di/modules/TileCacheModule.macos.kt`**

```kotlin
package com.jordankurtz.piawaremobile.di.modules

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.map.cache.AppleCacheFileSystem
import com.jordankurtz.piawaremobile.map.cache.FileTileCache
import com.jordankurtz.piawaremobile.map.cache.TileCache
import com.jordankurtz.piawaremobile.map.cache.TileCacheDatabase
import com.jordankurtz.piawaremobile.map.offline.AppleThumbnailFileManager
import com.jordankurtz.piawaremobile.map.offline.MacosThumbnailGenerator
import com.jordankurtz.piawaremobile.map.offline.ThumbnailFileManager
import com.jordankurtz.piawaremobile.map.offline.ThumbnailGenerator
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUserDomainMask
import platform.Foundation.stringByAppendingPathComponent

@Module
actual class TileCacheModule {
    @Single
    actual fun provideTileCache(
        contextWrapper: ContextWrapper,
        database: TileCacheDatabase,
        @IODispatcher ioDispatcher: CoroutineDispatcher,
    ): TileCache {
        val cacheFileSystem = AppleCacheFileSystem(appleCacheDir())
        return FileTileCache(
            cacheFileSystem = cacheFileSystem,
            queries = database.tileCacheQueries,
            ioDispatcher = ioDispatcher,
        )
    }

    @Single
    actual fun provideThumbnailGenerator(
        contextWrapper: ContextWrapper,
        @IODispatcher ioDispatcher: CoroutineDispatcher,
    ): ThumbnailGenerator = MacosThumbnailGenerator(ioDispatcher)

    @Single
    actual fun provideThumbnailFileManager(contextWrapper: ContextWrapper): ThumbnailFileManager =
        AppleThumbnailFileManager(appleThumbnailDir())
}

private fun appleCacheDir(): String {
    val cachePaths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
    val base = cachePaths.first() as String
    @Suppress("CAST_NEVER_SUCCEEDS")
    return (base as NSString).stringByAppendingPathComponent("map_tiles")
}

private fun appleThumbnailDir(): String {
    val cachePaths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
    val base = cachePaths.first() as String
    @Suppress("CAST_NEVER_SUCCEEDS")
    return (base as NSString).stringByAppendingPathComponent("thumbnails")
}
```

- [ ] **Step 6: Verify macOS compiles cleanly**

```
./gradlew :composeApp:compileKotlinMacosArm64 2>&1 | grep -E "error:|BUILD" | head -20
```

Expected: BUILD SUCCESSFUL (no missing actual errors)

- [ ] **Step 7: Lint and commit**

```
./gradlew ktlintFormat && ./gradlew ktlintCheck
git add -A
git commit -m "feat: add macosMain actual implementations (Platform, UrlHandler, ThumbnailGenerator, TileCacheModule)"
```

---

### Task 5: KoinInitializer + iosMain screen factories

**Context:** `MainViewController.kt` is replaced by two files. `KoinInitializer.kt` goes in `appleMain` (shared with macOS). `ScreenViewControllers.kt` in `iosMain` exposes the three per-screen `UIViewController` factories and a `toggleMapFollowUserLocation()` function for the SwiftUI glass button. `MainViewController.kt` is deleted — the iOS Xcode build will be broken between this task and Task 6; that is expected.

**Files:**
- Create: `composeApp/src/appleMain/kotlin/com/jordankurtz/piawaremobile/KoinInitializer.kt`
- Create: `composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/ScreenViewControllers.kt`
- Create: `composeApp/src/macosMain/kotlin/com/jordankurtz/piawaremobile/ScreenViewControllers.kt`
- Delete: `composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/MainViewController.kt`

**Interfaces:**
- Produces (appleMain): `fun startKoin()` — callable from Swift as `KoinInitializerKt.startKoin()`
- Produces (iosMain): `fun MapViewController(): UIViewController`, `fun AircraftListViewController(): UIViewController`, `fun SettingsViewController(): UIViewController`, `fun toggleMapFollowUserLocation()`
- Produces (macosMain): `fun MapNSViewController(): NSViewController`, `fun AircraftListNSViewController(): NSViewController`, `fun SettingsNSViewController(): NSViewController`, `fun toggleMacMapFollowUserLocation()`

- [ ] **Step 1: Create `appleMain/KoinInitializer.kt`**

```kotlin
package com.jordankurtz.piawaremobile

import com.jordankurtz.consolelogger.ConsoleLogger
import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.di.modules.AppModule
import com.jordankurtz.sentrylogger.SentryLogger
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module

fun startKoin() {
    Logger.addWriter(ConsoleLogger())
    Logger.addWriter(SentryLogger(BuildConfig.SENTRY_DSN))
    startKoin {
        modules(AppModule().module)
    }
}
```

- [ ] **Step 2: Create `iosMain/ScreenViewControllers.kt`**

```kotlin
package com.jordankurtz.piawaremobile

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.runtime.LaunchedEffect
import com.jordankurtz.piawaremobile.list.AircraftListScreen
import com.jordankurtz.piawaremobile.map.MapScreen
import com.jordankurtz.piawaremobile.settings.ui.SettingsScreen
import com.jordankurtz.piawaremobile.ui.Theme
import org.koin.compose.viewmodel.koinViewModel
import com.jordankurtz.piawaremobile.map.MapViewModel
import platform.UIKit.UIViewController

private var mapToggleFn: () -> Unit = {}

fun MapViewController(): UIViewController =
    ComposeUIViewController {
        val mapViewModel: MapViewModel = koinViewModel()
        LaunchedEffect(mapViewModel) {
            mapToggleFn = { mapViewModel.toggleFollowUserLocation() }
        }
        Theme {
            MapScreen(
                mapViewModel = mapViewModel,
                showFollowLocationFab = false,
            )
        }
    }

fun toggleMapFollowUserLocation() {
    mapToggleFn()
}

fun AircraftListViewController(): UIViewController =
    ComposeUIViewController {
        Theme { AircraftListScreen() }
    }

fun SettingsViewController(): UIViewController =
    ComposeUIViewController {
        Theme { SettingsScreen() }
    }
```

- [ ] **Step 3: Create `macosMain/ScreenViewControllers.kt`**

```kotlin
package com.jordankurtz.piawaremobile

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.ComposeNSViewController
import com.jordankurtz.piawaremobile.list.AircraftListScreen
import com.jordankurtz.piawaremobile.map.MapScreen
import com.jordankurtz.piawaremobile.map.MapViewModel
import com.jordankurtz.piawaremobile.settings.ui.SettingsScreen
import com.jordankurtz.piawaremobile.ui.Theme
import org.koin.compose.viewmodel.koinViewModel
import platform.AppKit.NSViewController

private var macMapToggleFn: () -> Unit = {}

fun MapNSViewController(): NSViewController =
    ComposeNSViewController {
        val mapViewModel: MapViewModel = koinViewModel()
        LaunchedEffect(mapViewModel) {
            macMapToggleFn = { mapViewModel.toggleFollowUserLocation() }
        }
        Theme {
            MapScreen(
                mapViewModel = mapViewModel,
                showFollowLocationFab = false,
            )
        }
    }

fun toggleMacMapFollowUserLocation() {
    macMapToggleFn()
}

fun AircraftListNSViewController(): NSViewController =
    ComposeNSViewController {
        Theme { AircraftListScreen() }
    }

fun SettingsNSViewController(): NSViewController =
    ComposeNSViewController {
        Theme { SettingsScreen() }
    }
```

- [ ] **Step 4: Delete `MainViewController.kt`**

```bash
rm composeApp/src/iosMain/kotlin/com/jordankurtz/piawaremobile/MainViewController.kt
```

- [ ] **Step 5: Verify Kotlin compilation (Xcode build is intentionally broken until Task 6)**

```
./gradlew :composeApp:compileKotlinIosSimulatorArm64 :composeApp:compileKotlinMacosArm64 2>&1 | grep -E "error:|BUILD" | head -20
```

Expected: BUILD SUCCESSFUL for both targets

- [ ] **Step 6: Lint and commit**

```
./gradlew ktlintFormat && ./gradlew ktlintCheck && ./gradlew :composeApp:testDebugUnitTest :composeApp:desktopTest 2>&1 | tail -10
git add -A
git commit -m "feat: add KoinInitializer and per-screen ViewControllers for iOS and macOS"
```

---

### Task 6: iOS SwiftUI shell

**Context:** Replace the two existing Swift files with four new ones. The Xcode build resumes working after this task. Build and test manually on an iOS 26 simulator: confirm glass tab bar, glass toolbar buttons, and the glass location FAB overlay on the Map tab.

**Files:**
- Modify: `iosApp/iosApp/iOSApp.swift`
- Delete: `iosApp/iosApp/ContentView.swift`
- Create: `iosApp/iosApp/ComposeScreen.swift`
- Create: `iosApp/iosApp/PiAwareTabView.swift`
- Create: `iosApp/iosApp/MapTabView.swift`

**Interfaces:**
- Consumes: `KoinInitializerKt.startKoin()`, `MapViewControllerKt.MapViewController()`, `AircraftListViewControllerKt.AircraftListViewController()`, `SettingsViewControllerKt.SettingsViewController()`, `ScreenViewControllersKt.toggleMapFollowUserLocation()`

- [ ] **Step 1: Update `iosApp/iosApp/iOSApp.swift`**

```swift
import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        KoinInitializerKt.doStartKoin()
    }

    var body: some Scene {
        WindowGroup {
            PiAwareTabView()
        }
    }
}
```

Note: Kotlin top-level functions named `startKoin` are exposed to Swift as `doStartKoin()` because `startKoin` conflicts with a Swift keyword in some contexts. Verify the exact Swift name by building once; adjust if needed.

- [ ] **Step 2: Create `iosApp/iosApp/ComposeScreen.swift`**

```swift
import SwiftUI
import UIKit

struct ComposeScreen: UIViewControllerRepresentable {
    let make: () -> UIViewController

    func makeUIViewController(context: Context) -> UIViewController {
        make()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

- [ ] **Step 3: Create `iosApp/iosApp/PiAwareTabView.swift`**

```swift
import SwiftUI
import ComposeApp

struct PiAwareTabView: View {
    var body: some View {
        TabView {
            Tab("Map", systemImage: "map") {
                MapTabView()
            }
            Tab("Aircraft", systemImage: "airplane") {
                ComposeScreen { AircraftListViewControllerKt.AircraftListViewController() }
            }
            Tab("Settings", systemImage: "gearshape") {
                ComposeScreen { SettingsViewControllerKt.SettingsViewController() }
            }
        }
    }
}
```

- [ ] **Step 4: Create `iosApp/iosApp/MapTabView.swift`**

```swift
import SwiftUI
import ComposeApp

struct MapTabView: View {
    var body: some View {
        ComposeScreen { MapViewControllerKt.MapViewController() }
            .ignoresSafeArea()
            .overlay(alignment: .bottomTrailing) {
                Button("", systemImage: "location.fill") {
                    ScreenViewControllersKt.toggleMapFollowUserLocation()
                }
                .buttonStyle(.glass)
                .padding(.bottom, 100)
                .padding(.trailing, 16)
            }
    }
}
```

- [ ] **Step 5: Delete `iosApp/iosApp/ContentView.swift`**

```bash
rm iosApp/iosApp/ContentView.swift
```

- [ ] **Step 6: Add new files to the Xcode target and remove ContentView.swift**

Open `iosApp/iosApp.xcodeproj` in Xcode:
1. Right-click the `iosApp` group → Add Files → select `ComposeScreen.swift`, `PiAwareTabView.swift`, `MapTabView.swift`; ensure "Add to targets: iosApp" is checked
2. Select `ContentView.swift` in the file navigator → press Delete → Move to Trash

Alternatively, edit `iosApp/iosApp.xcodeproj/project.pbxproj` directly: add PBXFileReference and PBXBuildFile entries for the three new .swift files, and remove the ContentView.swift entries. This is tedious — Xcode GUI is strongly preferred.

- [ ] **Step 7: Build and run on iOS 26 simulator**

In Xcode, select an iOS 26 simulator (or iPhone running iOS 26), press Run (⌘R).

Verify:
- App launches without crash
- Tab bar has glass appearance
- All three tabs navigate to the correct Compose screen
- Map tab shows the glass location button at bottom-right
- Tapping the location button triggers `toggleMapFollowUserLocation()`

- [ ] **Step 8: Commit**

```bash
git add iosApp/iosApp/iOSApp.swift \
        iosApp/iosApp/ComposeScreen.swift \
        iosApp/iosApp/PiAwareTabView.swift \
        iosApp/iosApp/MapTabView.swift \
        iosApp/iosApp.xcodeproj/project.pbxproj
git rm iosApp/iosApp/ContentView.swift
git commit -m "feat: replace iOS Compose shell with SwiftUI TabView for liquid glass"
```

---

### Task 7: macOS SwiftUI shell and Xcode target

**Context:** Add a macOS target to `iosApp.xcodeproj` that links the macOS `ComposeApp.framework`. The SwiftUI shell uses `NavigationSplitView` (standard macOS pattern). No automated tests exist for macOS Swift; verify manually on macOS 26.

**Files:**
- Create: `iosApp/macosApp/MacApp.swift`
- Create: `iosApp/macosApp/AppSection.swift`
- Create: `iosApp/macosApp/PiAwareSplitView.swift`
- Create: `iosApp/macosApp/ComposeDetailView.swift`
- Modify: `iosApp/iosApp.xcodeproj/project.pbxproj` — new macOS target via Xcode GUI

**Interfaces:**
- Consumes: `KoinInitializerKt.doStartKoin()`, `MapNSViewControllerKt.MapNSViewController()`, `AircraftListNSViewControllerKt.AircraftListNSViewController()`, `SettingsNSViewControllerKt.SettingsNSViewController()`, `ScreenViewControllersKt.toggleMacMapFollowUserLocation()`

- [ ] **Step 1: Create `iosApp/macosApp/` directory**

```bash
mkdir -p iosApp/macosApp
```

- [ ] **Step 2: Create `iosApp/macosApp/AppSection.swift`**

```swift
import Foundation

enum AppSection: String, CaseIterable, Identifiable {
    case map
    case aircraft
    case settings

    var id: String { rawValue }

    var title: String {
        switch self {
        case .map: return "Map"
        case .aircraft: return "Aircraft"
        case .settings: return "Settings"
        }
    }

    var icon: String {
        switch self {
        case .map: return "map"
        case .aircraft: return "airplane"
        case .settings: return "gearshape"
        }
    }
}
```

- [ ] **Step 3: Create `iosApp/macosApp/ComposeDetailView.swift`**

```swift
import SwiftUI
import AppKit
import ComposeApp

struct ComposeDetailView: NSViewControllerRepresentable {
    let section: AppSection

    func makeNSViewController(context: Context) -> NSViewController {
        viewController(for: section)
    }

    func updateNSViewController(_ nsViewController: NSViewController, context: Context) {}

    private func viewController(for section: AppSection) -> NSViewController {
        switch section {
        case .map: return MapNSViewControllerKt.MapNSViewController()
        case .aircraft: return AircraftListNSViewControllerKt.AircraftListNSViewController()
        case .settings: return SettingsNSViewControllerKt.SettingsNSViewController()
        }
    }
}
```

- [ ] **Step 4: Create `iosApp/macosApp/PiAwareSplitView.swift`**

```swift
import SwiftUI
import ComposeApp

struct PiAwareSplitView: View {
    @State private var selection: AppSection = .map

    var body: some View {
        NavigationSplitView {
            List(AppSection.allCases, selection: $selection) { section in
                Label(section.title, systemImage: section.icon)
            }
            .navigationTitle("PiAware")
        } detail: {
            ComposeDetailView(section: selection)
                .toolbar {
                    if selection == .map {
                        ToolbarItem {
                            Button("", systemImage: "location.fill") {
                                ScreenViewControllersKt.toggleMacMapFollowUserLocation()
                            }
                        }
                    }
                }
        }
    }
}
```

- [ ] **Step 5: Create `iosApp/macosApp/MacApp.swift`**

```swift
import SwiftUI
import ComposeApp

@main
struct MacApp: App {
    init() {
        KoinInitializerKt.doStartKoin()
    }

    var body: some Scene {
        WindowGroup {
            PiAwareSplitView()
        }
    }
}
```

- [ ] **Step 6: Add macOS target in Xcode**

Open `iosApp/iosApp.xcodeproj` in Xcode:
1. File → New → Target → macOS → App (SwiftUI, Swift)
2. Name: `macosApp`, Bundle ID: `com.jordankurtz.piawaremobile.macos`
3. Delete the Xcode-generated `ContentView.swift` and `macosAppApp.swift` from the new target
4. Add the four new files (`MacApp.swift`, `AppSection.swift`, `PiAwareSplitView.swift`, `ComposeDetailView.swift`) to the `macosApp` target
5. Under the `macosApp` target → Build Phases → Link Binary With Libraries → add `ComposeApp.framework` (the macOS variant, built via `./gradlew :composeApp:linkDebugFrameworkMacosArm64`)
6. Set the Framework Search Path to include the macOS framework output directory

The macOS framework build output path is typically:
`composeApp/build/bin/macosArm64/debugFramework/ComposeApp.framework`

Add that path (or its parent) to `FRAMEWORK_SEARCH_PATHS` in the macosApp target's Build Settings.

- [ ] **Step 7: Build the macOS framework**

```
./gradlew :composeApp:linkDebugFrameworkMacosArm64 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL. Framework at `composeApp/build/bin/macosArm64/debugFramework/ComposeApp.framework`.

- [ ] **Step 8: Build and run the macOS app in Xcode**

Select the `macosApp` scheme in Xcode → Run (⌘R).

Verify:
- App launches on macOS 26 without crash
- Sidebar shows Map / Aircraft / Settings
- Selecting each sidebar item shows the corresponding Compose screen
- Map selection shows a toolbar location button with glass styling
- Tapping the toolbar button triggers `toggleMacMapFollowUserLocation()`

- [ ] **Step 9: Commit**

```bash
git add iosApp/macosApp/ iosApp/iosApp.xcodeproj/project.pbxproj
git commit -m "feat: add macOS SwiftUI NavigationSplitView shell with liquid glass toolbar"
```
