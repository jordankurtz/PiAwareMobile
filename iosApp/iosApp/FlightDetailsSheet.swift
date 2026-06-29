import SwiftUI
import MapKit
import ComposeApp

struct FlightDetailsSheet: View {
    @Environment(AircraftBridge.self) private var aircraft
    @Environment(LocationBridge.self) private var location
    @Environment(SettingsBridge.self) private var settings
    @Environment(\.openURL) private var openURL
    private var resolvedTileURL: String {
        guard let s = settings.settings else {
            return "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        }
        let activeId = s.mapProviderId ?? "openstreetmap"
        let allBuiltIn = KoinHelpersKt.getBuiltInTileProviders() + KoinHelpersKt.getApiKeyTileProviders()
        if let provider = allBuiltIn.first(where: { $0.id == activeId }) {
            let keyGroup = provider.apiKeyGroup ?? provider.id
            let apiKey = s.apiKeys[keyGroup] ?? ""
            return provider.urlTemplate.replacingOccurrences(of: "{api_key}", with: apiKey)
        }
        if let custom = s.customProviders.first(where: { $0.id == activeId }) {
            return custom.urlTemplate
        }
        return "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
    }

    private var resolvedSubdomains: [String] {
        guard let s = settings.settings else { return [] }
        let activeId = s.mapProviderId ?? "openstreetmap"
        let allBuiltIn = KoinHelpersKt.getBuiltInTileProviders() + KoinHelpersKt.getApiKeyTileProviders()
        return allBuiltIn.first(where: { $0.id == activeId })?.subdomains ?? []
    }

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
                            actionButtons(aircraft: item.aircraft, flightIdent: flight.ident)
                            allContent(aircraft: item.aircraft, flight: flight)

                        case .loading:
                            HStack { Spacer(); ProgressView("Loading flight info…"); Spacer() }
                                .padding()
                            actionButtons(aircraft: item.aircraft, flightIdent: nil)
                            allContent(aircraft: item.aircraft, flight: nil)

                        case .error(let msg):
                            Label(msg, systemImage: "exclamationmark.triangle")
                                .font(.callout)
                                .foregroundStyle(.secondary)
                                .padding(.horizontal)
                            actionButtons(aircraft: item.aircraft, flightIdent: nil)
                            allContent(aircraft: item.aircraft, flight: nil)

                        case .notStarted:
                            actionButtons(aircraft: item.aircraft, flightIdent: nil)
                            allContent(aircraft: item.aircraft, flight: nil)
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
        if let op = flight.`operator`, !op.isEmpty {
            Text(op)
                .font(.subheadline).foregroundStyle(.secondary)
                .frame(maxWidth: .infinity)
                .padding(.top, 8)
        }
    }

    @ViewBuilder private func actionButtons(aircraft: Aircraft, flightIdent: String?) -> some View {
        let ident = (flightIdent?.nilIfBlank) ?? aircraft.flight?.nilIfBlank
        HStack(spacing: 8) {
            if let ident {
                Button("Open in FlightAware") {
                    if let url = URL(string: "https://www.flightaware.com/live/flight/\(ident)") {
                        openURL(url)
                    }
                }
                .buttonStyle(.bordered)
                .controlSize(.small)
            }
        }
        .padding(.horizontal)
    }

    // MARK: - All content (single scroll)

    @ViewBuilder private func allContent(aircraft: Aircraft, flight: Flight?) -> some View {
        if aircraft.hasPosition {
            MiniMapView(
                aircraft: aircraft,
                userCoordinate: location.coordinate,
                tileURL: resolvedTileURL,
                subdomains: resolvedSubdomains,
                showTrails: settings.settings?.showMinimapTrails == true
            )
            .padding(.horizontal)
        }

        // Primary stats: altitude, heading, speed
        HStack(spacing: 0) {
            if let alt = aircraft.altBaro {
                StatColumn(label: "Altitude", value: "\(alt) ft")
            } else if let alt = aircraft.altGeom {
                StatColumn(label: "Altitude", value: "\(alt) ft")
            }
            if let track = aircraft.track {
                StatColumn(label: "Heading", value: String(format: "%.0f°", Double(truncating: track)))
            }
            if let gs = aircraft.gs {
                StatColumn(label: "Speed", value: String(format: "%.0f kt", Double(truncating: gs)))
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal)

        // Location stats: position, distance, direction
        if aircraft.hasPosition {
            let coord = CLLocationCoordinate2D(latitude: aircraft.lat, longitude: aircraft.lon)
            HStack(spacing: 0) {
                StatColumn(label: "Position", value: String(format: "%.4f, %.4f", aircraft.lat, aircraft.lon))
                if let userCoord = location.coordinate {
                    let km = Int(CLLocation(latitude: coord.latitude, longitude: coord.longitude)
                        .distance(from: CLLocation(latitude: userCoord.latitude, longitude: userCoord.longitude)) / 1000)
                    let brg = bearing(from: userCoord, to: coord)
                    StatColumn(label: "Distance", value: "\(km) km")
                    StatColumn(label: "Direction", value: String(format: "%.0f° %@", brg, cardinal(brg)))
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.horizontal)
        }

        // ADS-B: vertical speed, squawk, signal, last seen
        VStack(spacing: 12) {
            HStack(spacing: 0) {
                if let baro = aircraft.baroRate {
                    StatColumn(label: "Vertical Speed", value: "\(Int(truncating: baro)) fpm")
                }
                if let squawk = aircraft.squawk {
                    StatColumn(label: "Squawk", value: squawk,
                               valueColor: ["7500", "7600", "7700"].contains(squawk) ? .red : nil)
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

        // Flight API: type, registration
        if let flight {
            if flight.aircraftType != nil || flight.registration != nil {
                HStack(spacing: 0) {
                    if let type = flight.aircraftType { StatColumn(label: "Type", value: type) }
                    if let reg = flight.registration { StatColumn(label: "Registration", value: reg) }
                }
                .frame(maxWidth: .infinity)
                .padding(.horizontal)
            }

            routeContent(flight: flight)
        }
    }

    @ViewBuilder private func routeContent(flight: Flight) -> some View {
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

// MARK: - Trail helpers

private class ColoredPolyline: MKPolyline {
    var segmentColor: UIColor = .systemBlue
}

private struct TrailSegment {
    let color: UIColor
    let coords: [CLLocationCoordinate2D]
}

private func trailSegments(_ positions: [AircraftPosition]) -> [TrailSegment] {
    guard positions.count >= 2 else { return [] }
    var result: [TrailSegment] = []
    var color = altitudeColor(for: positions[0].altitude)
    var coords = [CLLocationCoordinate2D(latitude: positions[0].latitude, longitude: positions[0].longitude)]

    for i in 1..<positions.count {
        let p = positions[i]
        let c = altitudeColor(for: p.altitude)
        let coord = CLLocationCoordinate2D(latitude: p.latitude, longitude: p.longitude)
        if c == color {
            coords.append(coord)
        } else {
            if coords.count >= 2 { result.append(TrailSegment(color: color, coords: coords)) }
            color = c
            coords = [CLLocationCoordinate2D(latitude: positions[i - 1].latitude, longitude: positions[i - 1].longitude), coord]
        }
    }
    if coords.count >= 2 { result.append(TrailSegment(color: color, coords: coords)) }
    return result
}

// MARK: - Altitude color (mirrors getColorForAltitude in MapHelpers.kt)

private func altitudeColor(for altBaro: String?) -> UIColor {
    if altBaro == "ground" { return UIColor(red: 139/255, green: 69/255, blue: 19/255, alpha: 1) }
    let alt = altBaro.flatMap { Int($0) } ?? 0
    let rgb: (Int, Int, Int)
    switch alt {
    case 0...250:       rgb = (255, 64, 0)
    case 251...500:     rgb = (255, 128, 0)
    case 501...750:     rgb = (255, 160, 0)
    case 751...1000:    rgb = (255, 192, 0)
    case 1001...1500:   rgb = (255, 224, 0)
    case 1501...2000:   rgb = (255, 255, 0)
    case 2001...3000:   rgb = (192, 255, 0)
    case 3001...4000:   rgb = (128, 255, 0)
    case 4001...5000:   rgb = (64, 255, 0)
    case 5001...6000:   rgb = (0, 255, 64)
    case 6001...7000:   rgb = (0, 255, 128)
    case 7001...8000:   rgb = (0, 255, 192)
    case 8001...9000:   rgb = (0, 255, 224)
    case 9001...10000:  rgb = (0, 255, 255)
    case 10001...15000: rgb = (0, 224, 255)
    case 15001...20000: rgb = (0, 192, 255)
    case 20001...25000: rgb = (0, 160, 255)
    case 25001...30000: rgb = (0, 128, 255)
    case 30001...35000: rgb = (0, 64, 255)
    case 35001...40000: rgb = (0, 0, 255)
    case 40001...45000: rgb = (64, 0, 255)
    case 45001...50000: rgb = (128, 0, 255)
    default:            rgb = (192, 0, 255)
    }
    return UIColor(red: CGFloat(rgb.0)/255, green: CGFloat(rgb.1)/255, blue: CGFloat(rgb.2)/255, alpha: 1)
}

// MARK: - MiniMapView

private struct MiniMapView: View {
    let aircraft: Aircraft
    let userCoordinate: CLLocationCoordinate2D?
    let tileURL: String
    let subdomains: [String]
    let showTrails: Bool

    @State private var trailPositions: [AircraftPosition] = []

    var body: some View {
        MiniMKMapView(
            coordinate: CLLocationCoordinate2D(latitude: aircraft.lat, longitude: aircraft.lon),
            userCoordinate: userCoordinate,
            heading: aircraft.track.map { Double(truncating: $0) } ?? 0,
            aircraftColor: altitudeColor(for: aircraft.altBaro),
            trail: showTrails ? trailPositions : [],
            tileURL: tileURL,
            subdomains: subdomains
        )
        .frame(height: 180)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .task(id: aircraft.hex) {
            trailPositions = []
            for await trail in KoinHelpersKt.getAircraftTrail(hex: aircraft.hex) {
                trailPositions = trail?.positions ?? []
            }
        }
    }
}

private struct MiniMKMapView: UIViewRepresentable {
    let coordinate: CLLocationCoordinate2D
    let userCoordinate: CLLocationCoordinate2D?
    let heading: Double
    let aircraftColor: UIColor
    let trail: [AircraftPosition]
    let tileURL: String
    let subdomains: [String]

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIView(context: Context) -> MKMapView {
        // Prime coordinator before addAnnotation so viewFor annotation: reads the correct values
        context.coordinator.aircraftColor = aircraftColor
        context.coordinator.heading = heading

        let mapView = MKMapView()
        mapView.delegate = context.coordinator
        mapView.isUserInteractionEnabled = false
        mapView.isRotateEnabled = false
        mapView.isPitchEnabled = false
        mapView.showsUserLocation = true

        let overlay = CustomTileOverlay(urlTemplate: tileURL, subdomains: subdomains)
        overlay.canReplaceMapContent = true
        mapView.addOverlay(overlay, level: .aboveLabels)

        let ann = MKPointAnnotation()
        ann.coordinate = coordinate
        mapView.addAnnotation(ann)
        context.coordinator.annotation = ann

        mapView.setRegion(fitRegion(aircraft: coordinate, user: userCoordinate), animated: false)
        return mapView
    }

    func updateUIView(_ mapView: MKMapView, context: Context) {
        context.coordinator.heading = heading
        context.coordinator.aircraftColor = aircraftColor

        if let ann = context.coordinator.annotation {
            ann.coordinate = coordinate
            mapView.setRegion(fitRegion(aircraft: coordinate, user: userCoordinate), animated: true)
            if let view = mapView.view(for: ann) {
                view.tintColor = aircraftColor
                view.transform = CGAffineTransform(rotationAngle: CGFloat((heading - 90) * .pi / 180))
            }
        }

        if let existing = mapView.overlays.compactMap({ $0 as? CustomTileOverlay }).first {
            if existing.urlTemplate != tileURL || existing.subdomains != subdomains {
                mapView.removeOverlay(existing)
                let overlay = CustomTileOverlay(urlTemplate: tileURL, subdomains: subdomains)
                overlay.canReplaceMapContent = true
                mapView.addOverlay(overlay, level: .aboveLabels)
            }
        }

        mapView.overlays.compactMap { $0 as? ColoredPolyline }.forEach { mapView.removeOverlay($0) }
        for segment in trailSegments(trail) {
            let poly = ColoredPolyline(coordinates: segment.coords, count: segment.coords.count)
            poly.segmentColor = segment.color
            mapView.addOverlay(poly, level: .aboveLabels)
        }
    }

    private func fitRegion(aircraft: CLLocationCoordinate2D, user: CLLocationCoordinate2D?) -> MKCoordinateRegion {
        guard let user else {
            return MKCoordinateRegion(center: aircraft, latitudinalMeters: 100_000, longitudinalMeters: 100_000)
        }
        let p1 = MKMapPoint(aircraft)
        let p2 = MKMapPoint(user)
        let width = abs(p1.x - p2.x)
        let height = abs(p1.y - p2.y)
        let padding = max(max(width, height) * 0.3, 100_000)
        let rect = MKMapRect(
            x: min(p1.x, p2.x) - padding,
            y: min(p1.y, p2.y) - padding,
            width: width + padding * 2,
            height: height + padding * 2
        )
        return MKCoordinateRegion(rect)
    }

    final class Coordinator: NSObject, MKMapViewDelegate {
        var annotation: MKPointAnnotation?
        var heading: Double = 0
        var aircraftColor: UIColor = .systemBlue

        func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
            if let poly = overlay as? ColoredPolyline {
                let r = MKPolylineRenderer(polyline: poly)
                r.strokeColor = poly.segmentColor.withAlphaComponent(0.85)
                r.lineWidth = 2.5
                return r
            }
            if let tile = overlay as? MKTileOverlay {
                return MKTileOverlayRenderer(tileOverlay: tile)
            }
            return MKOverlayRenderer(overlay: overlay)
        }

        func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
            guard !(annotation is MKUserLocation) else { return nil }
            let view = MKAnnotationView(annotation: annotation, reuseIdentifier: "aircraft")
            let config = UIImage.SymbolConfiguration(pointSize: 16, weight: .medium)
            view.image = UIImage(systemName: "airplane", withConfiguration: config)?
                .withRenderingMode(.alwaysTemplate)
            view.tintColor = aircraftColor
            view.transform = CGAffineTransform(rotationAngle: CGFloat((heading - 90) * .pi / 180))
            return view
        }
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
