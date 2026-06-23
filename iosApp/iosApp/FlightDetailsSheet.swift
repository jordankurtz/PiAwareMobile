import SwiftUI
import ComposeApp

struct FlightDetailsSheet: View {
    @Environment(AircraftBridge.self) private var aircraft

    private var selectedAircraft: AircraftWithServers? {
        aircraft.aircraft.first { $0.aircraft.hex == aircraft.selectedHex }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                if let item = selectedAircraft {
                    VStack(alignment: .leading, spacing: 16) {

                        // MARK: Header
                        HStack(alignment: .center, spacing: 12) {
                            Image(systemName: "airplane")
                                .font(.largeTitle)
                                .foregroundStyle(.primary)
                            VStack(alignment: .leading, spacing: 2) {
                                if let callsign = item.aircraft.flight, !callsign.isEmpty {
                                    Text(callsign)
                                        .font(.title3)
                                        .fontWeight(.semibold)
                                }
                                Text(item.aircraft.hex.uppercased())
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                if let category = item.aircraft.category {
                                    Text(category)
                                        .font(.caption2)
                                        .foregroundStyle(.tertiary)
                                }
                            }
                            Spacer()
                        }
                        .padding(.horizontal)
                        .padding(.top, 4)

                        Divider()
                            .padding(.horizontal)

                        // MARK: Data grid
                        Grid(alignment: .leading, horizontalSpacing: 24, verticalSpacing: 10) {
                            GridRow {
                                StatCell(
                                    icon: "arrow.up",
                                    label: "Altitude",
                                    value: altitudeText(item.aircraft)
                                )
                                StatCell(
                                    icon: "speedometer",
                                    label: "Speed",
                                    value: speedText(item.aircraft)
                                )
                            }
                            GridRow {
                                StatCell(
                                    icon: "safari",
                                    label: "Track",
                                    value: trackText(item.aircraft)
                                )
                                StatCell(
                                    icon: "antenna.radiowaves.left.and.right",
                                    label: "Squawk",
                                    value: squawkText(item.aircraft)
                                )
                            }
                        }
                        .padding(.horizontal)

                        Divider()
                            .padding(.horizontal)

                        // MARK: Flight state section
                        Group {
                            switch aircraft.flightState {
                            case .notStarted:
                                EmptyView()
                            case .loading:
                                HStack {
                                    Spacer()
                                    ProgressView("Loading flight info…")
                                    Spacer()
                                }
                                .padding()
                            case .success(let flight):
                                FlightRouteView(flight: flight)
                            case .error(let message):
                                Text(message)
                                    .font(.callout)
                                    .foregroundStyle(.secondary)
                                    .padding()
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                        }
                    }
                    .padding(.bottom, 16)
                } else {
                    Text("No aircraft selected")
                        .foregroundStyle(.secondary)
                        .padding()
                }
            }
            .navigationTitle(selectedAircraft?.aircraft.flight?.nilIfEmpty ?? selectedAircraft?.aircraft.hex ?? "Aircraft")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    // MARK: - Formatters

    private func altitudeText(_ a: Aircraft) -> String {
        if let baro = a.altBaro, baro != "ground", !baro.isEmpty {
            return "\(baro) ft"
        }
        if let geom = a.altGeom {
            return "\(geom) ft"
        }
        return "—"
    }

    private func speedText(_ a: Aircraft) -> String {
        guard let gs = a.gs else { return "—" }
        return String(format: "%.0f kt", gs)
    }

    private func trackText(_ a: Aircraft) -> String {
        guard let track = a.track else { return "—" }
        return String(format: "%.0f°", track)
    }

    private func squawkText(_ a: Aircraft) -> String {
        a.squawk ?? "—"
    }
}

// MARK: - StatCell

private struct StatCell: View {
    let icon: String
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Label {
                Text(label)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            } icon: {
                Image(systemName: icon)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Text(value)
                .font(.body)
                .fontWeight(.medium)
        }
    }
}

// MARK: - FlightRouteView

private struct FlightRouteView: View {
    let flight: Flight

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Route")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .padding(.horizontal)

            HStack(spacing: 12) {
                if let origin = flight.origin {
                    AirportChip(ref: origin, times: AirportTimes(
                        scheduled: flight.scheduledOut,
                        estimated: flight.estimatedOut,
                        actual: flight.actualOut
                    ))
                }

                VStack(spacing: 4) {
                    Image(systemName: "airplane")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    if let pct = flight.progressPercent?.intValue {
                        Text("\(pct)%")
                            .font(.caption2)
                            .foregroundStyle(.tertiary)
                    }
                }

                if let dest = flight.destination {
                    AirportChip(ref: dest, times: AirportTimes(
                        scheduled: flight.scheduledIn,
                        estimated: flight.estimatedIn,
                        actual: flight.actualIn
                    ))
                }
            }
            .padding(.horizontal)

            // Operator + status
            VStack(alignment: .leading, spacing: 4) {
                if let op = flight.`operator`, !op.isEmpty {
                    HStack(spacing: 4) {
                        Text("Operator:")
                            .foregroundStyle(.secondary)
                        Text(op)
                    }
                    .font(.caption)
                }
                if !flight.status.isEmpty {
                    HStack(spacing: 4) {
                        Text("Status:")
                            .foregroundStyle(.secondary)
                        Text(flight.status)
                    }
                    .font(.caption)
                }
                if let reg = flight.registration, !reg.isEmpty {
                    HStack(spacing: 4) {
                        Text("Reg:")
                            .foregroundStyle(.secondary)
                        Text(reg)
                    }
                    .font(.caption)
                }
                if let type = flight.aircraftType, !type.isEmpty {
                    HStack(spacing: 4) {
                        Text("Type:")
                            .foregroundStyle(.secondary)
                        Text(type)
                    }
                    .font(.caption)
                }
            }
            .padding(.horizontal)
            .foregroundStyle(.primary)
        }
        .padding(.vertical, 4)
    }
}

// MARK: - AirportChip

private struct AirportTimes {
    let scheduled: KotlinInstant?
    let estimated: KotlinInstant?
    let actual: KotlinInstant?

    var displayTime: String? {
        if let a = actual { return a.formattedTime }
        if let e = estimated { return e.formattedTime }
        if let s = scheduled { return s.formattedTime }
        return nil
    }
}

private struct AirportChip: View {
    let ref: FlightAirportRef
    let times: AirportTimes

    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(ref.codeIcao ?? ref.codeIata ?? ref.code ?? "?")
                .font(.headline)
            if let city = ref.city, !city.isEmpty {
                Text(city)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            } else if let name = ref.name, !name.isEmpty {
                Text(name)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            if let time = times.displayTime {
                Text(time)
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(10)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 10))
    }
}

// MARK: - String helper

private extension String {
    var nilIfEmpty: String? { isEmpty ? nil : self }
}
