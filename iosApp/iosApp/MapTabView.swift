import SwiftUI
import MapKit
import ComposeApp

struct MapTabView: View {
    @Environment(AircraftBridge.self) private var aircraft
    @Environment(LocationBridge.self) private var location

    @State private var position: MapCameraPosition = .automatic
    @State private var followingUser: Bool = false
    @State private var showFlightDetails: Bool = false

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            Map(position: $position) {
                // User location annotation
                UserAnnotation()

                // Aircraft annotations
                ForEach(aircraft.aircraft, id: \.aircraft.hex) { item in
                    let coord = CLLocationCoordinate2D(
                        latitude: item.aircraft.lat,
                        longitude: item.aircraft.lon
                    )
                    Annotation(
                        item.aircraft.flight ?? item.aircraft.hex,
                        coordinate: coord
                    ) {
                        AircraftMarker(
                            aircraft: item.aircraft,
                            isSelected: item.aircraft.hex == aircraft.selectedHex
                        )
                        .onTapGesture {
                            aircraft.selectAircraft(item.aircraft.hex)
                        }
                    }
                }
            }
            .mapStyle(.standard)
            .mapControls {
                MapCompass()
                MapScaleView()
            }
            .backgroundExtensionEffect()
            .ignoresSafeArea()

            // Follow-location FAB — separate overlay layer, respects safe area
            Button {
                followingUser.toggle()
                if followingUser, let coord = location.coordinate {
                    withAnimation {
                        position = .camera(
                            MapCamera(centerCoordinate: coord, distance: 50_000)
                        )
                    }
                } else {
                    withAnimation {
                        position = .automatic
                    }
                }
            } label: {
                Image(systemName: followingUser ? "location.fill" : "location")
                    .padding(12)
            }
            .buttonStyle(.glass)
            .padding(.trailing, 16)
            .padding(.bottom, 16)
        }
        .sheet(isPresented: $showFlightDetails, onDismiss: {
            aircraft.dismissFlight()
        }) {
            FlightDetailsSheet()
                .presentationDetents([.medium, .large])
                .environment(aircraft)
        }
        .onChange(of: aircraft.selectedHex) { _, newValue in
            showFlightDetails = newValue != nil
        }
        .onChange(of: location.coordinate?.latitude) { _, _ in
            guard followingUser, let coord = location.coordinate else { return }
            position = .camera(MapCamera(centerCoordinate: coord, distance: 50_000))
        }
    }
}

// MARK: - AircraftMarker

private struct AircraftMarker: View {
    let aircraft: Aircraft
    let isSelected: Bool

    var body: some View {
        Image(systemName: "airplane")
            .rotationEffect(.degrees(aircraft.track.map(Double.init(truncating:)) ?? 0))
            .font(isSelected ? .title : .body)
            .foregroundStyle(isSelected ? .yellow : .white)
            .shadow(color: .black.opacity(0.5), radius: 2)
    }
}
