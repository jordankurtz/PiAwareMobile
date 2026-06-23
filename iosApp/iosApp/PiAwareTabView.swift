import SwiftUI

struct PiAwareTabView: View {
    @State private var aircraftBridge = KoinHelper.makeAircraftBridge()
    @State private var locationBridge = KoinHelper.makeLocationBridge()
    @State private var settingsBridge = KoinHelper.makeSettingsBridge()

    var body: some View {
        TabView {
            Tab("Map", systemImage: "map") {
                MapTabView()
                    .environment(aircraftBridge)
                    .environment(locationBridge)
            }
            Tab("Aircraft", systemImage: "airplane") {
                AircraftListView()
                    .environment(aircraftBridge)
            }
            Tab("Settings", systemImage: "gearshape") {
                SettingsView()
                    .environment(settingsBridge)
            }
        }
        .tabBarMinimizeBehavior(.onScrollDown)
    }
}
