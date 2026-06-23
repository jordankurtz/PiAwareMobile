import SwiftUI
import ComposeApp

struct MapTabView: View {
    @Environment(AircraftBridge.self) private var aircraft

    var body: some View {
        ZStack {
            ComposeScreen { ScreenViewControllersKt.MapViewController() }
                .ignoresSafeArea()

            // Top-right: fit to aircraft + follow location
            VStack(spacing: 8) {
                if !aircraft.aircraft.isEmpty {
                    Button {
                        KoinHelpersKt.fitMapToAircraft()
                    } label: {
                        Image(systemName: "airplane")
                            .padding(12)
                    }
                    .buttonStyle(.glass)
                }
                Button {
                    ScreenViewControllersKt.toggleMapFollowUserLocation()
                } label: {
                    Image(systemName: "location.fill")
                        .padding(12)
                }
                .buttonStyle(.glass)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
            .padding(.trailing, 16)
            .padding(.top, 60)

            // Bottom-left: aircraft count pill
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
