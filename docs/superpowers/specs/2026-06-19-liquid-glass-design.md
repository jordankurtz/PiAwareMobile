# Liquid Glass on Apple Platforms

## Overview

Adopt Apple's native Liquid Glass UI on iOS, iPadOS, and macOS while keeping Android and Linux/Windows on Material 3. The implementation uses SwiftUI as the navigation shell on Apple platforms; Compose Multiplatform continues to render all screen content. Glass fidelity comes from iOS 26 / macOS 26 system APIs — not simulated in Compose.

---

## Strategy

`App.kt` in `commonMain` is unchanged and continues to serve Android and Linux/Windows JVM desktop. iOS stops calling `App()`. Instead, `iosMain` exposes per-screen `UIViewController` factories and a new SwiftUI `TabView` stitches them together. macOS gets a new Kotlin/Native target alongside the iOS targets; a new Xcode macOS target wraps it with a SwiftUI `NavigationSplitView` shell.

The JVM desktop build (`./gradlew run`) continues to represent Linux and Windows only. macOS ships exclusively as the new native Xcode target going forward.

---

## Section 1: What Gets Glass

- **Tab bar** — automatically glass on iOS 26 via SwiftUI `TabView`; no explicit modifier needed
- **Navigation bar / toolbar buttons** — automatically glass as system chrome on iOS 26 / macOS 26
- **Map FAB (center on user location)** — lifted from Compose to a SwiftUI `.overlay` with `.buttonStyle(.glass)`; hidden inside the Compose `MapScreen` on iOS via a parameter
- **Sub-screen navigation within tabs** (Settings → Flight Cache, Settings → Map Providers, etc.) — stays inside Compose using in-composable navigation state; these screens don't need glass chrome

---

## Section 2: commonMain Changes

No changes to screen composables. `MapScreen`, `AircraftListScreen`, `SettingsScreen`, and their sub-screens already exist as independent composables without navigation chrome embedded.

`Theme.kt` is unchanged. `MaterialTheme` continues to apply inside each Compose content pane on Apple platforms (colors, typography, and shapes still come from Material tokens for Compose-rendered widgets). SwiftUI glass overrides only the chrome layer — not Compose content.

---

## Section 3: iosMain — Koin Init & Screen Factories

`MainViewController.kt` is deleted and replaced with two new files.

**`KoinInitializer.kt`** (new):
```kotlin
fun startKoin() {
    Logger.addWriter(ConsoleLogger())
    Logger.addWriter(SentryLogger(BuildConfig.SENTRY_DSN))
    org.koin.core.context.startKoin {
        modules(AppModule().module)
    }
}
```

**`ScreenViewControllers.kt`** (new):
```kotlin
fun MapViewController(onCenterOnUserLocation: () -> Unit): UIViewController =
    ComposeUIViewController {
        Theme { MapScreen(showFab = false, onCenterOnUserLocation = onCenterOnUserLocation) }
    }

fun AircraftListViewController(): UIViewController =
    ComposeUIViewController { Theme { AircraftListScreen() } }

fun SettingsViewController(): UIViewController =
    ComposeUIViewController { Theme { SettingsScreen() } }
```

`MapScreen` gains a `showFab: Boolean = true` parameter. When `false` (iOS), it omits its internal FAB. The `onCenterOnUserLocation` callback is wired to the same `MapViewModel` action.

---

## Section 4: iOS SwiftUI Shell

The two existing Swift files are replaced.

**`iOSApp.swift`** — adds `init()` for Koin startup:
```swift
@main
struct iOSApp: App {
    init() {
        KoinInitializerKt.startKoin()
    }
    var body: some Scene {
        WindowGroup { PiAwareTabView() }
    }
}
```

**`ContentView.swift`** → deleted; replaced by two new files.

**`ComposeScreen.swift`** (new — reused by all tabs and macOS):
```swift
struct ComposeScreen: UIViewControllerRepresentable {
    let make: () -> UIViewController
    func makeUIViewController(context: Context) -> UIViewController { make() }
    func updateUIViewController(_: UIViewController, context: Context) {}
}
```

**`PiAwareTabView.swift`** (new):
```swift
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

**`MapTabView.swift`** (new — handles the glass FAB overlay):
```swift
struct MapTabView: View {
    @State private var centerTrigger = false

    var body: some View {
        ComposeScreen {
            MapViewControllerKt.MapViewController(onCenterOnUserLocation: {
                // callback captured by Compose ViewModel
            })
        }
        .ignoresSafeArea()
        .overlay(alignment: .bottomTrailing) {
            Button("Center", systemImage: "location.fill") {
                centerTrigger.toggle()
            }
            .buttonStyle(.glass)
            .padding(.bottom, 100)
            .padding(.trailing, 16)
        }
    }
}
```

The callback bridge between Swift and the Kotlin `MapViewModel` uses a closure captured at `MapViewController` creation time and dispatched to the ViewModel via a public `centerOnUserLocation()` method.

---

## Section 5: macOS Native Target

### KMP — `build.gradle.kts`

```kotlin
listOf(
    macosX64(),
    macosArm64(),
).forEach { macosTarget ->
    macosTarget.binaries.framework {
        baseName = "ComposeApp"
        isStatic = true
    }
}
```

KMP's default hierarchy template automatically creates `appleMain` (shared between `iosMain` and `macosMain`) and `macosMain`. Logger and Sentry setup shared between iOS and macOS moves to `appleMain`.

### `macosMain` — Screen Factories

```kotlin
fun MapNSViewController(onCenterOnUserLocation: () -> Unit): NSViewController =
    ComposeNSViewController {
        Theme { MapScreen(showFab = false, onCenterOnUserLocation = onCenterOnUserLocation) }
    }

fun AircraftListNSViewController(): NSViewController =
    ComposeNSViewController { Theme { AircraftListScreen() } }

fun SettingsNSViewController(): NSViewController =
    ComposeNSViewController { Theme { SettingsScreen() } }
```

### Swift macOS App — Xcode Target

A new macOS target is added to the existing `iosApp.xcodeproj` (or a sibling `macosApp.xcodeproj` if the workspace structure warrants it). It links the macOS `ComposeApp.framework`.

**`MacApp.swift`**:
```swift
@main
struct MacApp: App {
    init() { KoinInitializerKt.startKoin() }
    var body: some Scene {
        WindowGroup { PiAwareSplitView() }
    }
}
```

**`PiAwareSplitView.swift`**:
```swift
enum AppSection: String, CaseIterable, Identifiable {
    case map, aircraft, settings
    var id: String { rawValue }
    var title: String { /* Map / Aircraft / Settings */ }
    var icon: String { /* map / airplane / gearshape */ }
}

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
        }
    }
}
```

**`ComposeDetailView.swift`** — `NSViewControllerRepresentable` wrapper selecting the right Compose screen per sidebar selection.

Toolbar buttons placed in `.toolbar` use system-standard styling, which is automatically glass on macOS 26.

---

## Section 6: Android & Linux/Windows — Unchanged

`androidMain`, `desktopMain`, and `App.kt` in `commonMain` are not modified. Android keeps its Material `NavigationBar` + `Scaffold`. Linux and Windows keep the JVM desktop build. No platform detection branching is added to `commonMain`.

---

## Testing

- **Unit tests** (`commonTest`): no new tests needed; screen composables and ViewModels are unchanged
- **Desktop UI tests** (`desktopTest`): no new tests needed
- **iOS**: manual verification on iOS 26 simulator (or device) — tab bar glass, toolbar button glass, map FAB glass
- **macOS**: manual verification on macOS 26 — sidebar, toolbar buttons, detail pane Compose content
- **Android instrumented tests**: existing tests unchanged; confirm they still pass post-`MapScreen` parameter addition

---

## Out of Scope

- Simulating glass in Compose for non-Apple platforms
- Rewriting any screen in SwiftUI (all screen content stays in Compose)
- Sub-screen navigation (Settings → Flight Cache, etc.) glass treatment
- tvOS / watchOS targets
