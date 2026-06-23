import SwiftUI
import ComposeApp

struct MapTabView: View {
    @Environment(AircraftBridge.self) private var aircraft

    /// When non-nil, a glass sidebar-reveal button is shown at top-leading.
    var onShowSidebar: (() -> Void)? = nil

    @Namespace private var glassNamespace

    var body: some View {
        ZStack {
            ComposeScreen { ScreenViewControllersKt.MapViewController() }
                .ignoresSafeArea()

            // All glass elements share one container so they sample the
            // Compose map backdrop consistently and don't go stale.
            GlassEffectContainer(spacing: 12) {
                // Sidebar reveal (iPad only, visible when sidebar is collapsed)
                if let onShowSidebar {
                    Button(action: onShowSidebar) {
                        Image(systemName: "sidebar.left")
                            .padding(12)
                    }
                    .buttonStyle(.glass)
                    .glassEffectID("sidebar", in: glassNamespace)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                    .padding(.leading, 16)
                    .padding(.top, 60)
                }

                // Map controls (top-right)
                VStack(spacing: 8) {
                    if !aircraft.aircraft.isEmpty {
                        Button {
                            KoinHelpersKt.fitMapToAircraft()
                        } label: {
                            Image(systemName: "airplane")
                                .padding(12)
                        }
                        .buttonStyle(.glass)
                        .glassEffectID("fit", in: glassNamespace)
                    }
                    Button {
                        ScreenViewControllersKt.toggleMapFollowUserLocation()
                    } label: {
                        Image(systemName: "location.fill")
                            .padding(12)
                    }
                    .buttonStyle(.glass)
                    .glassEffectID("follow", in: glassNamespace)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                .padding(.trailing, 16)
                .padding(.top, 60)

                // Aircraft count pill (bottom-left)
                Label("\(aircraft.numberOfPlanes)", systemImage: "airplane")
                    .font(.caption.weight(.medium))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .glassEffect(in: .capsule)
                    .glassEffectID("count", in: glassNamespace)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
                    .padding(.leading, 16)
                    .padding(.bottom, 16)
            }
        }
    }
}
