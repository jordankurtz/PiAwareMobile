import SwiftUI
import MapKit
import ComposeApp

struct FlightDetailsSheet: View {
    @Environment(AircraftBridge.self) private var aircraft
    @Environment(LocationBridge.self) private var location
    @Environment(\.openURL) private var openURL
    @State private var selectedTab = 0

    private var item: AircraftWithServers? {
        aircraft.aircraft.first { $0.aircraft.hex == aircraft.selectedHex }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                if let item {
                    VStack(alignment: .leading, spacing: 16) {
                        switch aircraft.flightState {
                        case .success(let flight):
                            flightHeader(flight)
                            actionButtons(aircraft: item.aircraft, flight: flight)
                            tabPicker
                            tabContent(aircraft: item.aircraft, flight: flight)

                        case .loading:
                            HStack { Spacer(); ProgressView("Loading flight info…"); Spacer() }
                                .padding()
                            bareContent(aircraft: item.aircraft)

                        case .error(let msg):
                            Label(msg, systemImage: "exclamationmark.triangle")
                                .font(.callout)
                                .foregroundStyle(.secondary)
                                .padding(.horizontal)
                            bareContent(aircraft: item.aircraft)

                        case .notStarted:
                            bareContent(aircraft: item.aircraft)
                        }
                    }
                    .padding(.bottom, 20)
                } else {
                    Text("No aircraft selected").foregroundStyle(.secondary).padding()
                }
            }
            .navigationTitle(
                item?.aircraft.flight?.nilIfBlank ??
                item?.aircraft.hex.uppercased() ?? "Aircraft"
            )
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    // MARK: - Headers & buttons

    @ViewBuilder private func flightHeader(_ flight: Flight) -> some View {
        VStack(spacing: 2) {
            Text(flight.ident)
                .font(.title2).fontWeight(.semibold)
            if let op = flight.operator_, !op.isEmpty {
                Text(op).font(.subheadline).foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 8)
    }

    @ViewBuilder private func actionButtons(aircraft: Aircraft, flight: Flight) -> some View {
        HStack(spacing: 8) {
            if !flight.ident.isEmpty {
                Button("Open in FlightAware") {
                    if let url = URL(string: "https://www.flightaware.com/live/flight/\(flight.ident)") {
                        openURL(url)
                    }
                }
                .buttonStyle(.bordered)
                .controlSize(.small)
            }
        }
        .padding(.horizontal)
    }

    // MARK: - Tab selector

    private var tabPicker: some View {
        Picker("", selection: $selectedTab) {
            Text("Details").tag(0)
            Text("Aircraft").tag(1)
            Text("Route").tag(2)
        }
        .pickerStyle(.segmented)
        .padding(.horizontal)
    }

    @ViewBuilder private func tabContent(aircraft: Aircraft, flight: Flight) -> some View {
        switch selectedTab {
        case 0: detailsTab(aircraft: aircraft)
        case 1: aircraftTab(aircraft: aircraft, flight: flight)
        default: routeTab(flight: flight)
        }
    }

    // MARK: - Details tab

    @ViewBuilder private func bareContent(aircraft: Aircraft) -> some View {
        detailsTab(aircraft: aircraft)
    }

    @ViewBuilder private func detailsTab(aircraft: Aircraft) -> some View {
        VStack(spacing: 12) {
            if aircraft.hasPosition {
                MiniMapView(aircraft: aircraft)
                    .padding(.horizontal)
            }
            primaryStats(aircraft: aircraft)
            locationStats(aircraft: aircraft)
        }
    }

    @ViewBuilder private func primaryStats(aircraft: Aircraft) -> some View {
        let a = aircraft
        HStack(spacing: 0) {
            if let alt = a.altBaro {
                StatColumn(label: "Altitude", value: "\(alt) ft")
            } else if let alt = a.altGeom {
                StatColumn(label: "Altitude", value: "\(alt) ft")
            }
            if let track = a.track {
                StatColumn(label: "Heading", value: String(format: "%.0f°", Double(truncating: track)))
            }
            if let gs = a.gs {
                StatColumn(label: "Speed", value: String(format: "%.0f kt", Double(truncating: gs)))
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal)
    }

    @ViewBuilder private func locationStats(aircraft: Aircraft) -> some View {
        if aircraft.hasPosition {
            let coord = CLLocationCoordinate2D(latitude: aircraft.lat, longitude: aircraft.lon)
            HStack(spacing: 0) {
                StatColumn(
                    label: "Position",
                    value: String(format: "%.4f, %.4f", aircraft.lat, aircraft.lon)
                )
                if let userCoord = location.coordinate {
                    let userLoc = CLLocation(latitude: userCoord.latitude, longitude: userCoord.longitude)
                    let acLoc = CLLocation(latitude: coord.latitude, longitude: coord.longitude)
                    let km = Int(acLoc.distance(from: userLoc) / 1000)
                    let brg = bearing(from: userCoord, to: coord)
                    StatColumn(label: "Distance", value: "\(km) km")
                    StatColumn(label: "Direction", value: String(format: "%.0f° %@", brg, cardinal(brg)))
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.horizontal)
        }
    }

    // MARK: - Aircraft tab

    @ViewBuilder private func aircraftTab(aircraft: Aircraft, flight: Flight) -> some View {
        VStack(spacing: 12) {
            HStack(spacing: 0) {
                if let type = flight.aircraftType { StatColumn(label: "Type", value: type) }
                if let reg = flight.registration { StatColumn(label: "Registration", value: reg) }
            }
            .frame(maxWidth: .infinity)
            HStack(spacing: 0) {
                if let baro = aircraft.baroRate {
                    let rate = Int(truncating: baro)
                    StatColumn(label: "Vertical Speed", value: "\(rate) fpm")
                }
                if let squawk = aircraft.squawk {
                    let emergency = ["7500", "7600", "7700"].contains(squawk)
                    StatColumn(label: "Squawk", value: squawk, valueColor: emergency ? .red : nil)
                }
            }
            .frame(maxWidth: .infinity)
            HStack(spacing: 0) {
                if let rssi = aircraft.rssi {
                    StatColumn(label: "Signal", value: String(format: "%.1f dBm", Double(truncating: rssi)))
                }
                if let seen = aircraft.seen {
                    StatColumn(label: "Last Seen", value: String(format: "%.0f s", Double(truncating: seen)))
                }
            }
            .frame(maxWidth: .infinity)
        }
        .padding(.horizontal)
    }

    // MARK: - Route tab

    @ViewBuilder private func routeTab(flight: Flight) -> some View {
        VStack(spacing: 12) {
            if let origin = flight.origin {
                AirportCard(
                    role: "Departure",
                    airport: origin,
                    scheduled: flight.scheduledOut,
                    estimated: flight.estimatedOut,
                    actual: flight.actualOut
                )
            }

            if let pct = flight.progressPercent, let ete = flight.filedEte {
                FlightProgressView(progressPercent: Int(truncating: pct), filedEte: Int(truncating: ete))
            }

            if let destination = flight.destination {
                AirportCard(
                    role: "Destination",
                    airport: destination,
                    scheduled: flight.scheduledIn,
                    estimated: flight.estimatedIn,
                    actual: flight.actualIn
                )
            }

            if !flight.status.isEmpty {
                HStack {
                    Text("Status:").foregroundStyle(.secondary)
                    Text(flight.status)
                }
                .font(.caption)
                .padding(.horizontal)
            }
        }
        .padding(.horizontal)
    }

    // MARK: - Helpers

    private func bearing(from: CLLocationCoordinate2D, to: CLLocationCoordinate2D) -> Double {
        let lat1 = from.latitude * .pi / 180
        let lat2 = to.latitude * .pi / 180
        let dLon = (to.longitude - from.longitude) * .pi / 180
        let x = sin(dLon) * cos(lat2)
        let y = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        let b = atan2(x, y) * 180 / .pi
        return (b + 360).truncatingRemainder(dividingBy: 360)
    }

    private func cardinal(_ bearing: Double) -> String {
        let dirs = ["N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"]
        return dirs[Int((bearing + 22.5) / 45) % 8]
    }
}

// MARK: - MiniMapView

private struct MiniMapView: View {
    let aircraft: Aircraft

    private var coordinate: CLLocationCoordinate2D {
        CLLocationCoordinate2D(latitude: aircraft.lat, longitude: aircraft.lon)
    }

    private var cameraPosition: MapCameraPosition {
        .camera(MapCamera(centerCoordinate: coordinate, distance: 100_000))
    }

    var body: some View {
        Map(initialPosition: cameraPosition) {
            Annotation("", coordinate: coordinate) {
                let angle = Double(truncating: aircraft.track ?? 0) - 90
                Image(systemName: "airplane")
                    .font(.body)
                    .rotationEffect(.degrees(angle))
                    .foregroundStyle(.blue)
            }
        }
        .frame(height: 180)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .disabled(true)
    }
}

// MARK: - StatColumn

private struct StatColumn: View {
    let label: String
    let value: String
    var valueColor: Color? = nil

    var body: some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.body).fontWeight(.medium)
                .foregroundStyle(valueColor.map { AnyShapeStyle($0) } ?? AnyShapeStyle(Color.primary))
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - AirportCard

private struct AirportCard: View {
    let role: String
    let airport: FlightAirportRef
    let scheduled: KotlinInstant?
    let estimated: KotlinInstant?
    let actual: KotlinInstant?

    private var code: String { airport.codeIcao ?? airport.codeIata ?? airport.code ?? "?" }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(role)
                .font(.caption)
                .foregroundStyle(.secondary)
                .textCase(.uppercase)

            Text(code).font(.title2).fontWeight(.bold)

            if let city = airport.city, !city.isEmpty {
                Text(city).font(.subheadline).foregroundStyle(.secondary)
            } else if let name = airport.name, !name.isEmpty {
                Text(name).font(.subheadline).foregroundStyle(.secondary)
            }

            Divider()

            if let sched = scheduled {
                TimeRow(label: "Scheduled", time: sched.formattedTime, diff: nil, highlighted: false)
            }
            if let act = actual {
                TimeRow(
                    label: "Actual",
                    time: act.formattedTime,
                    diff: timeDiff(scheduled, act),
                    highlighted: true
                )
            } else if let est = estimated {
                TimeRow(
                    label: "Estimated",
                    time: est.formattedTime,
                    diff: timeDiff(scheduled, est),
                    highlighted: false
                )
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 12))
    }

    private func timeDiff(_ a: KotlinInstant?, _ b: KotlinInstant) -> String? {
        guard let a else { return nil }
        let diffMin = (b.epochSeconds - a.epochSeconds) / 60
        if diffMin == 0 { return "On time" }
        let sign = diffMin > 0 ? "+" : ""
        return "\(sign)\(diffMin) min"
    }
}

private struct TimeRow: View {
    let label: String
    let time: String
    let diff: String?
    let highlighted: Bool

    var body: some View {
        HStack {
            Text(label).foregroundStyle(.secondary)
            Text(time).foregroundStyle(highlighted ? Color.accentColor : Color.primary)
            if let diff {
                Text(diff)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
        }
        .font(.caption)
    }
}

// MARK: - FlightProgressView

private struct FlightProgressView: View {
    let progressPercent: Int
    let filedEte: Int

    private var remainingSeconds: Int {
        Int(Double(filedEte) * (1.0 - Double(progressPercent) / 100.0))
    }

    private var remainingText: String {
        let hours = remainingSeconds / 3600
        let minutes = (remainingSeconds % 3600) / 60
        if hours > 0 { return "\(hours)h \(minutes)m remaining" }
        if minutes > 0 { return "\(minutes)m remaining" }
        return ""
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text("\(progressPercent)%").font(.caption).foregroundStyle(.secondary)
                Spacer()
                if !remainingText.isEmpty {
                    Text(remainingText).font(.caption).foregroundStyle(.secondary)
                }
            }
            ProgressView(value: Double(progressPercent) / 100.0)
        }
    }
}

// MARK: - String helper

private extension String {
    var nilIfBlank: String? { trimmingCharacters(in: .whitespaces).isEmpty ? nil : self }
}
