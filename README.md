# PiAware Flight Tracker

A cross-platform flight tracking app built with [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) that displays live ADS-B data from [PiAware](https://flightaware.com/adsb/piaware/) receivers.

## Features

- **Live aircraft map** with real-time position updates on OpenStreetMap tiles
- **Aircraft list** with callsign, altitude, speed, heading, squawk, and signal strength
- **Flight trails** colored by altitude, with configurable display modes
- **Multiple receiver support** - connect to several PiAware devices simultaneously
- **FlightAware API integration** - optional flight details lookup (origin, destination, progress)
- **Tablet layout** with side-by-side map and list panels
- **Configurable settings** - refresh interval, zoom levels, trail display, receiver locations

## Platforms

| Platform | Status |
|----------|--------|
| Android  | Supported (API 24+) |
| iOS      | Supported |
| Desktop  | Supported (macOS, Windows, Linux) |

## Prerequisites

- JDK 17+
- Android SDK (API 24+) for Android builds
- Xcode 15+ for iOS builds

## Building

```bash
# Android debug APK
./gradlew :composeApp:assembleDebug

# Desktop application
./gradlew :composeApp:run

# iOS (via Xcode)
open iosApp/iosApp.xcodeproj
```

## Testing

```bash
# Unit tests
./gradlew :composeApp:testDebugUnitTest

# Desktop UI tests
./gradlew :composeApp:desktopTest

# Lint and static analysis
./gradlew ktlintCheck detekt

# Code coverage report
./gradlew :composeApp:koverXmlReportDebug
```

## Project Structure

```
composeApp/src/
  commonMain/     Shared Kotlin code (UI, business logic, data layer)
  androidMain/    Android-specific implementations
  iosMain/        iOS-specific implementations
  desktopMain/    Desktop/JVM-specific implementations
  commonTest/     Shared unit tests
  desktopTest/    Desktop Compose UI tests
  androidTest/    Android instrumented tests
iosApp/           Xcode project wrapper
logger/           Logging abstraction module
sentry-logger/    Sentry error reporting module
console-logger/   Console logging module
```

## Tech Stack

- **Kotlin Multiplatform** with Compose Multiplatform for shared UI
- **Ktor** for HTTP networking
- **Koin** for dependency injection (with KSP annotation processing)
- **kotlinx.serialization** for JSON parsing
- **DataStore** for persistent settings
- **MapCompose** for tiled map rendering
- **Mokkery** + **Turbine** for testing

## Setup

1. Clone the repository
2. Add at least one PiAware server address in Settings > Servers
3. The app connects to your PiAware receiver's HTTP API (default port 8080) to fetch aircraft data

For FlightAware API integration, obtain an [AeroAPI key](https://flightaware.com/aeroapi/) and enter it in Settings.

## License

This project is licensed under the GNU General Public License v2.0 - see the [LICENSE](LICENSE) file for details.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on how to contribute.
