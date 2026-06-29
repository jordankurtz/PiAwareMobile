import Foundation
import ComposeApp

@MainActor
enum KoinHelper {
    static func makeAircraftBridge() -> AircraftBridge {
        AircraftBridge(vm: SwiftBridgeKt.getAircraftViewModel())
    }

    static func makeLocationBridge() -> LocationBridge {
        LocationBridge(vm: SwiftBridgeKt.getLocationViewModel())
    }

    static func makeSettingsBridge() -> SettingsBridge {
        SettingsBridge(vm: SwiftBridgeKt.getSettingsViewModel())
    }

    static func makeOfflineMapsBridge() -> OfflineMapsBridge {
        OfflineMapsBridge(vm: SwiftBridgeKt.getOfflineMapsViewModel())
    }
}
