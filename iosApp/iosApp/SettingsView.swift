import SwiftUI
import ComposeApp

struct SettingsView: View {
    @Environment(SettingsBridge.self) private var settingsBridge

    @State private var showClearCacheConfirm = false

    var body: some View {
        @Bindable var bridge = settingsBridge
        Form {
                // MARK: - Map

                Section("Map") {
                    NavigationLink("Map Providers") {
                        MapProvidersView()
                    }

                    if let settings = settingsBridge.settings {
                        Toggle(
                            "Center Map on User at Start",
                            isOn: Binding(
                                get: { settings.centerMapOnUserOnStart },
                                set: { settingsBridge.updateCenterMapOnUserOnStart($0) }
                            )
                        )

                        Toggle(
                            "Restore Map Position at Start",
                            isOn: Binding(
                                get: { settings.restoreMapStateOnStart },
                                set: { settingsBridge.updateRestoreMapStateOnStart($0) }
                            )
                        )

                        Picker(
                            "Trail Display",
                            selection: Binding(
                                get: { settings.trailDisplayMode },
                                set: { settingsBridge.updateTrailDisplayMode($0) }
                            )
                        ) {
                            Text("None").tag(TrailDisplayMode.none)
                            Text("Selected").tag(TrailDisplayMode.selected)
                            Text("All").tag(TrailDisplayMode.all)
                        }

                        Toggle(
                            "Show Trails in Minimap",
                            isOn: Binding(
                                get: { settings.showMinimapTrails },
                                set: { settingsBridge.updateShowMinimapTrails($0) }
                            )
                        )

                        Toggle(
                            "Show Receiver Locations",
                            isOn: Binding(
                                get: { settings.showReceiverLocations },
                                set: { settingsBridge.updateShowReceiverLocations($0) }
                            )
                        )

                        Toggle(
                            "Show My Location on Map",
                            isOn: Binding(
                                get: { settings.showUserLocationOnMap },
                                set: { settingsBridge.updateShowUserLocationOnMap($0) }
                            )
                        )

                        HStack {
                            Text("Default Zoom")
                            Spacer()
                            Stepper(
                                "\(Int(settings.defaultZoomLevel))",
                                value: Binding(
                                    get: { Int(settings.defaultZoomLevel) },
                                    set: { settingsBridge.updateDefaultZoomLevel($0) }
                                ),
                                in: 1...16
                            )
                        }

                        HStack {
                            Text("Min Zoom")
                            Spacer()
                            Stepper(
                                "\(Int(settings.minZoomLevel))",
                                value: Binding(
                                    get: { Int(settings.minZoomLevel) },
                                    set: { settingsBridge.updateMinZoomLevel($0) }
                                ),
                                in: 1...16
                            )
                        }

                        HStack {
                            Text("Max Zoom")
                            Spacer()
                            Stepper(
                                "\(Int(settings.maxZoomLevel))",
                                value: Binding(
                                    get: { Int(settings.maxZoomLevel) },
                                    set: { settingsBridge.updateMaxZoomLevel($0) }
                                ),
                                in: 1...16
                            )
                        }
                    } else {
                        ProgressView()
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
                        OfflineMapsView()
                    }
                }

                // MARK: - Servers

                Section("Servers") {
                    NavigationLink("Servers") {
                        ServersView()
                            .environment(settingsBridge)
                    }

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

                // MARK: - App

                Section("App") {
                    if let settings = settingsBridge.settings {
                        Toggle(
                            "Open URLs Externally",
                            isOn: Binding(
                                get: { settings.openUrlsExternally },
                                set: { settingsBridge.updateOpenUrlsExternally($0) }
                            )
                        )
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
