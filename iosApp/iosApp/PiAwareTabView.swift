import SwiftUI
import ComposeApp

struct PiAwareTabView: View {
    var body: some View {
        if #available(iOS 26, *) {
            TabView {
                Tab("Map", systemImage: "map") {
                    MapTabView()
                }
                Tab("Aircraft", systemImage: "airplane") {
                    ComposeScreen { ScreenViewControllersKt.AircraftListViewController() }
                }
                Tab("Settings", systemImage: "gearshape") {
                    ComposeScreen { ScreenViewControllersKt.SettingsViewController() }
                }
            }
            .tabBarMinimizeBehavior(.onScrollDown)
        } else if #available(iOS 18, *) {
            TabView {
                Tab("Map", systemImage: "map") {
                    MapTabView()
                }
                Tab("Aircraft", systemImage: "airplane") {
                    ComposeScreen { ScreenViewControllersKt.AircraftListViewController() }
                }
                Tab("Settings", systemImage: "gearshape") {
                    ComposeScreen { ScreenViewControllersKt.SettingsViewController() }
                }
            }
        } else {
            TabView {
                MapTabView()
                    .tabItem { Label("Map", systemImage: "map") }
                ComposeScreen { ScreenViewControllersKt.AircraftListViewController() }
                    .tabItem { Label("Aircraft", systemImage: "airplane") }
                ComposeScreen { ScreenViewControllersKt.SettingsViewController() }
                    .tabItem { Label("Settings", systemImage: "gearshape") }
            }
        }
    }
}
