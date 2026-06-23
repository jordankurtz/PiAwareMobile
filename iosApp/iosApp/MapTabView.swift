import SwiftUI
import ComposeApp

struct MapTabView: View {
    @Environment(AircraftBridge.self) private var aircraft

    /// Non-nil on iPad when the NavigationSplitView sidebar is collapsed.
    var onShowSidebar: (() -> Void)? = nil

    @Namespace private var topRightNamespace
    @Namespace private var topLeftNamespace

    var body: some View {
        ZStack {
            ComposeScreen { ScreenViewControllersKt.MapViewController() }
                .ignoresSafeArea()

            // Sidebar toggle — shown at top-left when sidebar is hidden.
            // Its own container; top-left and top-right are not "nearby."
            if let onShowSidebar {
                GlassEffectContainer {
                    Button(action: onShowSidebar) {
                        Image(systemName: "sidebar.left")
                            .padding(12)
                    }
                    .buttonStyle(.glass)
                    .glassEffectID("sidebar", in: topLeftNamespace)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                .padding(.leading, 16)
                .padding(.top, 60)
            }

            // Fit + follow buttons share a container — they are nearby and
            // the fit button morphs in/out, so they must share a backdrop.
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
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
            .padding(.trailing, 16)
            .padding(.top, 60)

            // Count pill — spatially isolated (bottom-left), standalone glass.
            Label("\(aircraft.numberOfPlanes)", systemImage: "airplane")
                .font(.caption.weight(.medium))
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .glassEffect(in: .capsule)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
                .padding(.leading, 16)
                .padding(.bottom, 16)
        }
    }
}
