# Offline Maps Background Downloads

**Date:** 2026-04-08  
**Scope:** Android Foreground Service + iOS BGContinuedProcessingTask with Live Activity  
**Background scenario:** App backgrounded (screen off / user switches apps). Downloads resume in the background but do not survive process termination.

---

## Goals

- Tile downloads continue when the user locks the screen or leaves the app
- Android shows a live progress notification with a cancel action
- iOS shows a Live Activity on the lock screen and Dynamic Island (iOS 26+)
- No behavior change when the app is in the foreground

## Non-Goals

- Rescheduling downloads after the app is force-quit or the device reboots (scenario B)
- Pre-iOS 26 background support (graceful no-op on older iOS is not needed; deployment target is raised)

---

## Architecture

The central change is retiring `DownloadScopeHolder` and replacing it with `BackgroundDownloadCoordinator` — an `expect/actual`-backed interface that owns the coroutine scope and drives the platform background mechanism.

```
OfflineMapsViewModel
    └── BackgroundDownloadCoordinator  (interface, Koin-injected)
            ├── [Android] AndroidBackgroundDownloadCoordinator
            │       └── OfflineDownloadForegroundService
            │               ├── Notification (live progress + Cancel action)
            │               └── OfflineDownloadEngine  (unchanged)
            └── [iOS] IosBackgroundDownloadCoordinator
                    ├── BGContinuedProcessingTask
                    ├── IosDownloadObserver (Kotlin interface → ObjC protocol)
                    │       └── DownloadActivityManager (Swift, manages Live Activity)
                    └── OfflineDownloadEngine  (unchanged)
```

`OfflineDownloadEngine`, `OfflineTileStore`, and `TileCache` are unchanged.  
`DownloadScopeHolder` is deleted.

---

## Common Layer (commonMain)

### `BackgroundDownloadCoordinator` interface

```kotlin
interface BackgroundDownloadCoordinator {
    val progress: StateFlow<DownloadProgress?>
    val isDownloading: StateFlow<Boolean>
    fun start(region: OfflineRegion, config: TileProviderConfig)
    fun cancel()
}
```

Both platform implementations:
- Hold their own `CoroutineScope(SupervisorJob() + ioDispatcher)`
- Collect `OfflineDownloadEngine.download()` and update `progress` / `isDownloading`
- Handle `CancellationException` by writing `PARTIAL` status to the store (same logic as today's `doDownload`)

### `IosDownloadObserver` interface (commonMain, iosMain used only)

Defined in `commonMain` so Kotlin/Native exposes it as an ObjC protocol:

```kotlin
interface IosDownloadObserver {
    fun onStarted(regionName: String, total: Long)
    fun onProgress(downloaded: Long, total: Long)
    fun onComplete(regionName: String)
    fun onFailed(regionName: String)
    fun onCancelled()
}
```

---

## ViewModel Changes

`OfflineMapsViewModel` constructor:
- Remove: `DownloadScopeHolder`, `DownloadEngine`
- Add: `BackgroundDownloadCoordinator`

Changes:
- `_isDownloading` and `_downloadProgress` are collected from `coordinator.isDownloading` / `coordinator.progress` in `viewModelScope`
- `startDownload()`, `retryDownload()`, `cancelDownload()` delegate to the coordinator
- `doDownload()` is deleted — cancellation/PARTIAL/FAILED logic moves into the coordinator
- `_regions` update-on-progress logic stays in the ViewModel, collecting `coordinator.progress` in `viewModelScope` — keeps the coordinator focused on platform concerns

---

## Android (androidMain)

### `AndroidBackgroundDownloadCoordinator`

- Koin `@Single`, injected with `Context`, `OfflineDownloadEngine`, `OfflineTileStore`
- `start()`: calls `context.startForegroundService(intent)` with region/config as extras; sets `isDownloading = true`
- `cancel()`: sends a cancel intent to the service
- `progress`: delegates to `OfflineDownloadForegroundService.progressFlow` (companion object `MutableStateFlow`)

### `OfflineDownloadForegroundService`

- `onStartCommand()`: calls `startForeground()` immediately with an indeterminate notification, then launches the download coroutine
- Coroutine collects `OfflineDownloadEngine.download()`:
  - Each emission updates `progressFlow` and calls `NotificationManager.notify()` with `setProgress(total, downloaded, false)`
  - On completion: posts a one-shot "Download complete" notification (auto-cancel), resets `progressFlow` to `null`, calls `stopSelf()`
  - On `CancellationException`: writes `PARTIAL` status, resets flow, stops self
  - On other exception: posts a persistent "Download failed" notification, resets flow, stops self

**Companion object:**
```kotlin
companion object {
    val progressFlow = MutableStateFlow<DownloadProgress?>(null)
}
```

### Notification spec

| State | Title | Body | Actions |
|---|---|---|---|
| Downloading | "Downloading [region name]" | "X / Y tiles" with `setProgress` | Cancel (pending intent) |
| Complete | "[region name] downloaded" | — | — (auto-cancel) |
| Failed | "[region name] download failed" | "Tap to retry" | — (opens app) |

- Channel: `offline_map_downloads`, importance `IMPORTANCE_LOW` (no sound/vibration)
- Foreground service type: `dataSync`

### Manifest additions

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

<service
    android:name=".map.offline.OfflineDownloadForegroundService"
    android:foregroundServiceType="dataSync" />
```

---

## iOS (iosMain + iosApp)

### Deployment target

Bump `IPHONEOS_DEPLOYMENT_TARGET` from `15.3` → `26.0` in `project.pbxproj`.

### BGContinuedProcessingTask

Task identifier: `com.jordankurtz.piawaremobile.offlinedownload`

**`Info.plist`:**
```xml
<key>BGTaskSchedulerPermittedIdentifiers</key>
<array>
    <string>com.jordankurtz.piawaremobile.offlinedownload</string>
</array>
```

**`iOSApp.swift`** registers the handler at app init:
```swift
BGTaskScheduler.shared.register(
    forTaskWithIdentifier: "com.jordankurtz.piawaremobile.offlinedownload",
    using: nil
) { task in
    guard let task = task as? BGContinuedProcessingTask else { return }
    task.expirationHandler = {
        coordinator.cancel()
    }
    // Download is already running; task just extends background time.
    // Mark complete when coordinator finishes.
}
```

**`IosBackgroundDownloadCoordinator.start()`** submits the request via Kotlin/Native ObjC interop:
```kotlin
val request = BGContinuedProcessingTaskRequest(identifier = TASK_IDENTIFIER)
BGTaskScheduler.shared.submitTaskRequest(request, error = null)
```

### Live Activity

**`OfflineMapActivityAttributes.swift`** (new file in iosApp):
```swift
struct OfflineMapActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        var downloaded: Int
        var total: Int
    }
    let regionName: String
}
```

UI:
- **Lock screen**: region name, `setProgress`-style bar, "X / Y tiles" label
- **Dynamic Island compact**: fraction indicator + "X/Y" text

**`DownloadActivityManager.swift`** implements `IosDownloadObserver` (ObjC protocol):
- `onStarted`: calls `Activity<OfflineMapActivityAttributes>.request(...)` to start the Live Activity
- `onProgress`: calls `activity.update(using: ContentState(downloaded:total:))`
- `onComplete` / `onFailed` / `onCancelled`: calls `activity.end(using:dismissalPolicy: .immediate)`

**`iOSApp.swift`** instantiates `DownloadActivityManager` and injects it into `IosBackgroundDownloadCoordinator` via Koin at startup.

### `IosBackgroundDownloadCoordinator`

- Koin `@Single`, injected with `OfflineDownloadEngine`, `OfflineTileStore`, `IODispatcher`
- `var observer: IosDownloadObserver?` — set by Swift at app init
- `start()`:
  1. Submits `BGContinuedProcessingTaskRequest`
  2. Calls `observer?.onStarted(regionName, total)`
  3. Launches download coroutine
- Coroutine progress: calls `observer?.onProgress(downloaded, total)` each emission
- On complete/fail/cancel: calls corresponding observer method + writes status to store

---

## Testing

### Unit tests (commonTest)
- `BackgroundDownloadCoordinatorTest`: mock `OfflineDownloadEngine` emitting a sequence of `DownloadProgress`, verify `progress` StateFlow emissions and final `DownloadStatus` written to store
- Cancellation: cancel mid-download, verify `PARTIAL` status written and `isDownloading` resets to false

### Desktop UI tests (desktopTest)
- No new UI components; existing `OfflineMapsScreen` tests cover the region list

### Android instrumented tests
- Smoke test: `OfflineDownloadForegroundService` starts and posts a notification (verify notification channel exists)

### iOS
- `IosDownloadObserver` is a pure interface; `DownloadActivityManager` is tested manually / via XCTest in iosApp

---

## File Summary

| File | Change |
|---|---|
| `commonMain/.../BackgroundDownloadCoordinator.kt` | New interface |
| `commonMain/.../IosDownloadObserver.kt` | New interface |
| `commonMain/.../OfflineMapsViewModel.kt` | Replace `DownloadScopeHolder`/`DownloadEngine` with coordinator |
| `commonMain/.../DownloadScopeHolder.kt` | Deleted |
| `androidMain/.../AndroidBackgroundDownloadCoordinator.kt` | New |
| `androidMain/.../OfflineDownloadForegroundService.kt` | New |
| `androidMain/AndroidManifest.xml` | Permissions + service declaration |
| `iosMain/.../IosBackgroundDownloadCoordinator.kt` | New |
| `iosApp/.../OfflineMapActivityAttributes.swift` | New |
| `iosApp/.../DownloadActivityManager.swift` | New |
| `iosApp/.../iOSApp.swift` | BGTask registration + observer injection |
| `iosApp/.../Info.plist` | `BGTaskSchedulerPermittedIdentifiers` |
| `iosApp/project.pbxproj` | Deployment target 15.3 → 26.0 |
