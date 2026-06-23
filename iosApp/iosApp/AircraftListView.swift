import SwiftUI
import ComposeApp

struct AircraftListView: View {
    @Environment(AircraftBridge.self) private var aircraft
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @State private var query = ""

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
        // On iPad, NavigationSplitView provides the navigation context.
        // On iPhone, we need our own NavigationStack.
        if horizontalSizeClass == .regular {
            listContent
        } else {
            NavigationStack { listContent }
        }
    }

    private var listContent: some View {
        List(filtered, id: \.aircraft.hex) { item in
            AircraftRow(item: item)
                .contentShape(Rectangle())
                .onTapGesture {
                    aircraft.selectAircraft(item.aircraft.hex)
                }
        }
        .listStyle(.sidebar)
        .navigationTitle("Aircraft")
        .navigationSubtitle("\(aircraft.numberOfPlanes) tracked")
        .searchable(text: $query, prompt: "Callsign or hex")
    }
}

// MARK: - AircraftRow

private struct AircraftRow: View {
    let item: AircraftWithServers

    private var callsign: String {
        let flight = item.aircraft.flight?.trimmingCharacters(in: .whitespaces) ?? ""
        return flight.isEmpty ? item.aircraft.hex.uppercased() : flight
    }

    private var subtitle: String {
        var parts: [String] = []
        if let reg = item.info?.registration { parts.append(reg) }
        if let type = item.info?.icaoType { parts.append(type) }
        if parts.isEmpty { parts.append(item.aircraft.hex.uppercased()) }
        return parts.joined(separator: " · ")
    }

    private var altitudeText: String {
        if let baro = item.aircraft.altBaro {
            if baro.lowercased() == "ground" { return "Ground" }
            if !baro.isEmpty { return "\(baro) ft" }
        }
        if let geom = item.aircraft.altGeom { return "\(geom) ft" }
        return "—"
    }

    private var speedText: String {
        guard let gs = item.aircraft.gs else { return "—" }
        return String(format: "%.0f kt", gs)
    }

    private var verticalRate: Int? {
        guard let rate = item.aircraft.baroRate else { return nil }
        let intRate = rate.intValue
        return abs(intRate) >= 100 ? intRate : nil
    }

    private var trackDegrees: Double {
        Double(truncating: item.aircraft.track ?? 0)
    }

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "airplane")
                .font(.title2)
                .foregroundStyle(.secondary)
                .rotationEffect(.degrees(trackDegrees))
                .frame(width: 28)

            VStack(alignment: .leading, spacing: 2) {
                Text(callsign)
                    .font(.headline)
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 2) {
                Text(altitudeText)
                    .font(.subheadline)
                    .monospacedDigit()
                if let rate = verticalRate {
                    Text("\(rate > 0 ? "↑" : "↓") \(Swift.abs(rate)) fpm")
                        .font(.caption)
                        .foregroundStyle(rate > 0 ? Color.green : Color.orange)
                        .monospacedDigit()
                } else {
                    Text(speedText)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .monospacedDigit()
                }
            }
        }
        .padding(.vertical, 4)
    }
}
