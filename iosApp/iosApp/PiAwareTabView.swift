import SwiftUI
import ComposeApp

struct PiAwareTabView: View {
    var body: some View {
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
