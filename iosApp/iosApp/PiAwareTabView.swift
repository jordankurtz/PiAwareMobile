import SwiftUI
import ComposeApp

struct PiAwareTabView: View {
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @State private var aircraftBridge = KoinHelper.makeAircraftBridge()
    @State private var locationBridge = KoinHelper.makeLocationBridge()
    @State private var settingsBridge = KoinHelper.makeSettingsBridge()
    @State private var showSettings = false

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
        HStack(spacing: 0) {
            // Map takes remaining width (~60%)
            ZStack(alignment: .topTrailing) {
                MapTabView()
                Button {
                    showSettings = true
                } label: {
                    Image(systemName: "gearshape")
                        .padding(12)
                }
                .buttonStyle(.glass)
                .padding(.top, 60)
                .padding(.trailing, 16)
            }

            Divider()

            // Aircraft list panel (~40%)
            AircraftListView()
                .frame(width: 380)
        }
        .ignoresSafeArea()
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
