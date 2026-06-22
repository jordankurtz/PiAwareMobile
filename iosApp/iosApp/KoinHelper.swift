import Foundation
import ComposeApp

@MainActor
enum KoinHelper {
    static func makeAircraftBridge() -> AircraftBridge {
        AircraftBridge(vm: KoinHelpersKt.getAircraftViewModel())
    }

    static func makeLocationBridge() -> LocationBridge {
        LocationBridge(vm: KoinHelpersKt.getLocationViewModel())
    }

    static func makeSettingsBridge() -> SettingsBridge {
        SettingsBridge(vm: KoinHelpersKt.getSettingsViewModel())
    }
}
