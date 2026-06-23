import SwiftUI
import ComposeApp

struct MapTabView: View {
    @Environment(AircraftBridge.self) private var aircraft
    @Namespace private var glassNamespace

    var body: some View {
        ZStack {
            ComposeScreen { ScreenViewControllersKt.MapViewController() }
                .ignoresSafeArea()

            // Top-right controls share a GlassEffectContainer so they
            // sample the backdrop together and morph correctly when the
            // fit-to-aircraft button appears/disappears.
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
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
            .padding(.trailing, 16)
            .padding(.top, 60)

            // Aircraft count pill — isolated from the buttons above so it
            // doesn't interfere with their morph animation.
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
