import SwiftUI
import ComposeApp

struct PiAwareTabView: View {
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @State private var aircraftBridge = KoinHelper.makeAircraftBridge()
    @State private var locationBridge = KoinHelper.makeLocationBridge()
    @State private var settingsBridge = KoinHelper.makeSettingsBridge()
    @State private var showSettings = false
    @State private var showFlightSheet = false

    var body: some View {
        Group {
            if horizontalSizeClass == .regular {
                iPadLayout
            } else {
                iPhoneLayout
            }
        }
        .environment(aircraftBridge)
        .environment(locationBridge)
        .environment(settingsBridge)
        .onChange(of: aircraftBridge.selectedHex) { _, newValue in
            showFlightSheet = newValue != nil
        }
        .sheet(isPresented: $showFlightSheet, onDismiss: {
            aircraftBridge.dismissFlight()
        }) {
            FlightDetailsSheet()
                .environment(aircraftBridge)
        }
    }

    // MARK: - iPhone

    private var iPhoneLayout: some View {
        TabView {
            Tab("Map", systemImage: "map") { MapTabView() }
            Tab("Aircraft", systemImage: "airplane") { AircraftListView() }
            Tab("Settings", systemImage: "gearshape") { SettingsView() }
        }
        .tabBarMinimizeBehavior(.onScrollDown)
    }

    // MARK: - iPad

    private var iPadLayout: some View {
        NavigationSplitView {
            AircraftListView()
                .toolbar {
                    ToolbarItem(placement: .primaryAction) {
                        Button {
                            showSettings = true
                        } label: {
                            Image(systemName: "gearshape")
                        }
                    }
                }
        } detail: {
            MapTabView()
                .toolbar(.hidden, for: .navigationBar)
                .ignoresSafeArea()
        }
        .sheet(isPresented: $showSettings) {
            NavigationStack {
                SettingsView()
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Done") { showSettings = false }
                        }
                    }
            }
        }
    }
}
