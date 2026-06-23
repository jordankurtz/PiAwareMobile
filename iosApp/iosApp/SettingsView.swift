import SwiftUI
import ComposeApp

struct SettingsView: View {
    @Environment(SettingsBridge.self) private var settingsBridge

    @State private var showClearCacheConfirm = false

    var body: some View {
        @Bindable var bridge = settingsBridge
        NavigationStack {
            Form {
                // MARK: - Servers
                Section("Servers") {
                    NavigationLink("Servers") {
                        ServersView()
                            .environment(settingsBridge)
                    }
                }

                // MARK: - Map
                Section("Map") {
                    if let settings = settingsBridge.settings {
                        HStack {
                            Text("Refresh Interval")
                            Spacer()
                            Stepper(
                                "\(Int(settings.refreshInterval))s",
                                value: Binding(
                                    get: { Int(settings.refreshInterval) },
                                    set: { settingsBridge.updateRefreshInterval($0) }
                                ),
                                in: 1...60
                            )
                        }
                    } else {
                        HStack {
                            Text("Refresh Interval")
                            Spacer()
                            ProgressView()
                        }
                    }

                    Button(role: .destructive) {
                        showClearCacheConfirm = true
                    } label: {
                        Text("Clear Tile Cache")
                    }
                }

                // MARK: - Offline
                Section("Offline") {
                    NavigationLink("Offline Maps") {
                        ComposeScreen { ScreenViewControllersKt.OfflineMapsViewController() }
                            .navigationTitle("Offline Maps")
                            .navigationBarTitleDisplayMode(.inline)
                    }

                    NavigationLink("Flight Cache") {
                        ComposeScreen { ScreenViewControllersKt.FlightCacheViewController() }
                            .navigationTitle("Flight Cache")
                            .navigationBarTitleDisplayMode(.inline)
                    }
                }

                // MARK: - FlightAware
                Section("FlightAware") {
                    if let settings = settingsBridge.settings {
                        Toggle(
                            "Enable FlightAware API",
                            isOn: Binding(
                                get: { settings.enableFlightAwareApi },
                                set: { settingsBridge.updateEnableFlightAwareApi($0) }
                            )
                        )

                        if settings.enableFlightAwareApi {
                            HStack {
                                Text("API Key")
                                Spacer()
                                TextField(
                                    "API Key",
                                    text: Binding(
                                        get: { settings.flightAwareApiKey },
                                        set: { settingsBridge.updateFlightAwareApiKey($0) }
                                    )
                                )
                                .multilineTextAlignment(.trailing)
                                .autocorrectionDisabled()
                                .textInputAutocapitalization(.never)
                            }
                        }
                    } else {
                        ProgressView()
                    }
                }
            }
            .navigationTitle("Settings")
            .confirmationDialog(
                "Clear Tile Cache?",
                isPresented: $showClearCacheConfirm,
                titleVisibility: .visible
            ) {
                Button("Clear Cache", role: .destructive) {
                    settingsBridge.clearTileCache()
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("This will delete all cached map tiles. Downloaded offline regions will not be affected.")
            }
        }
    }
}
