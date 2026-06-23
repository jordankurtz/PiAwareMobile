import SwiftUI
import ComposeApp

struct AircraftListView: View {
    @Environment(AircraftBridge.self) private var aircraft
    @State private var query = ""
    @State private var showFlightSheet = false

    private var filtered: [AircraftWithServers] {
        let trimmed = query.trimmingCharacters(in: .whitespaces).lowercased()
        guard !trimmed.isEmpty else { return aircraft.aircraft }
        return aircraft.aircraft.filter { item in
            let hex = item.aircraft.hex.lowercased()
            let callsign = (item.aircraft.flight ?? "").trimmingCharacters(in: .whitespaces).lowercased()
            return hex.contains(trimmed) || callsign.contains(trimmed)
        }
    }

    var body: some View {
        NavigationStack {
            List(filtered, id: \.aircraft.hex) { item in
                AircraftRow(item: item)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        aircraft.selectAircraft(item.aircraft.hex)
                        showFlightSheet = true
                    }
                    .listRowBackground(Color.clear)
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .navigationTitle("Aircraft")
            .navigationSubtitle("\(aircraft.numberOfPlanes) tracked")
            .searchable(text: $query, prompt: "Callsign or hex")
            .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
        }
        .sheet(isPresented: $showFlightSheet, onDismiss: {
            aircraft.dismissFlight()
        }) {
            FlightDetailsSheet()
                .environment(aircraft)
        }
    }
}

// MARK: - AircraftRow

private struct AircraftRow: View {
    let item: AircraftWithServers

    private var callsign: String {
        let flight = item.aircraft.flight?.trimmingCharacters(in: .whitespaces) ?? ""
        return flight.isEmpty ? item.aircraft.hex.uppercased() : flight
    }

    private var altitudeText: String {
        if let baro = item.aircraft.altBaro, baro != "ground", !baro.isEmpty {
            return "\(baro) ft"
        }
        if let geom = item.aircraft.altGeom {
            return "\(geom) ft"
        }
        return "—"
    }

    private var speedText: String {
        guard let gs = item.aircraft.gs else { return "—" }
        return String(format: "%.0f kt", gs)
    }

    private var trackDegrees: Double {
        Double(truncating: item.aircraft.track ?? 0)
    }

    var body: some View {
        HStack(spacing: 12) {
            // Tail icon rotated to track heading
            Image(systemName: "airplane")
                .font(.title2)
                .foregroundStyle(.secondary)
                .rotationEffect(.degrees(trackDegrees))

            VStack(alignment: .leading, spacing: 2) {
                Text(callsign)
                    .font(.headline)
                Text(item.aircraft.hex.uppercased())
                    .font(.caption)
                    .foregroundStyle(.tertiary)
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 2) {
                Text(altitudeText)
                    .font(.subheadline)
                    .monospacedDigit()
                Text(speedText)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .monospacedDigit()
            }
        }
        .padding(.vertical, 4)
    }
}
