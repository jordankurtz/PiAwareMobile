import SwiftUI
import ComposeApp

struct PiAwareTabView: View {
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @State private var aircraftBridge = KoinHelper.makeAircraftBridge()
    @State private var locationBridge = KoinHelper.makeLocationBridge()
    @State private var settingsBridge = KoinHelper.makeSettingsBridge()
    @State private var offlineMapsBridge = KoinHelper.makeOfflineMapsBridge()
    @State private var showSettings = false
    @State private var showFlightSheet = false
    @State private var columnVisibility: NavigationSplitViewVisibility = .all

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
        .environment(offlineMapsBridge)
        .onChange(of: aircraftBridge.selectedHex) { _, newValue in
            showFlightSheet = newValue != nil
        }
        .sheet(isPresented: $showFlightSheet, onDismiss: {
            aircraftBridge.dismissFlight()
        }) {
            FlightDetailsSheet()
                .environment(aircraftBridge)
                .environment(locationBridge)
                .environment(settingsBridge)
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
        NavigationSplitView(columnVisibility: $columnVisibility) {
            AircraftListView()
        } detail: {
            MapTabView(
                onShowSidebar: columnVisibility != .all
                    ? { withAnimation { columnVisibility = .all } }
                    : nil,
                onShowSettings: { showSettings = true }
            )
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
