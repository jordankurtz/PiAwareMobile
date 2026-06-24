import SwiftUI
import ComposeApp

struct MapTabView: View {
    @Environment(AircraftBridge.self) private var aircraft

    /// Non-nil on iPad when the NavigationSplitView sidebar is collapsed.
    var onShowSidebar: (() -> Void)? = nil
    /// Non-nil on iPad — opens the Settings sheet from the map.
    var onShowSettings: (() -> Void)? = nil

    @Namespace private var topRightNamespace
    @Namespace private var topLeftNamespace

    var body: some View {
        ZStack {
            ComposeScreen { ScreenViewControllersKt.MapViewController() }
                .ignoresSafeArea()

            // Use VStack+HStack+Spacer so each GlassEffectContainer is
            // sized to its content, not to the full screen. A full-screen
            // container frame creates an internal backdrop layer that blocks
            // all map touches.
            VStack(spacing: 0) {
                HStack(alignment: .top, spacing: 0) {
                    // Sidebar toggle — top-left, own container.
                    if let onShowSidebar {
                        GlassEffectContainer {
                            Button(action: onShowSidebar) {
                                Image(systemName: "sidebar.left")
                                    .padding(12)
                            }
                            .buttonStyle(.glass)
                            .glassEffectID("sidebar", in: topLeftNamespace)
                        }
                    }

                    Spacer()

                    // Fit + follow + settings — top-right, shared container
                    // so morph animation works when fit button appears/disappears.
                    GlassEffectContainer(spacing: 8) {
                        VStack(spacing: 8) {
                            if !aircraft.aircraft.isEmpty {
                                Button {
                                    KoinHelpersKt.fitMapToAircraft()
                                } label: {
                                    Image(systemName: "airplane")
                                        .padding(12)
                                }
                                .buttonStyle(.glass)
                                .glassEffectID("fit", in: topRightNamespace)
                            }
                            Button {
                                ScreenViewControllersKt.toggleMapFollowUserLocation()
                            } label: {
                                Image(systemName: "location.fill")
                                    .padding(12)
                            }
                            .buttonStyle(.glass)
                            .glassEffectID("follow", in: topRightNamespace)
                            if let onShowSettings {
                                Button(action: onShowSettings) {
                                    Image(systemName: "gearshape")
                                        .padding(12)
                                }
                                .buttonStyle(.glass)
                                .glassEffectID("settings", in: topRightNamespace)
                            }
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 60)

                Spacer()

                HStack {
                    Label("\(aircraft.numberOfPlanes)", systemImage: "airplane")
                        .font(.caption.weight(.medium))
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .glassEffect(in: .capsule)

                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 16)
            }
        }
    }
}
