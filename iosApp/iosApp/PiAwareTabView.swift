import SwiftUI
import ComposeApp

struct PiAwareTabView: View {
    var body: some View {
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
    }
}
