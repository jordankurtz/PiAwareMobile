import SwiftUI
import MapKit
import CoreLocation
import ComposeApp  // BoundingBox

// MARK: - RegionPickerView

struct RegionPickerView: View {
    let tileURL: String
    let subdomains: [String]
    let initialCenter: CLLocationCoordinate2D?
    let initialZoom: Int
    let onSelected: (BoundingBox, Int32) -> Void
    let onDismiss: () -> Void

    @State private var visibleRegion: MKCoordinateRegion?
    @State private var isBoxMode = false
    @State private var boxRect = CGRect.zero
    @State private var screenSize = CGSize.zero

    var body: some View {
        ZStack {
            TileMapView(
                urlTemplate: tileURL,
                subdomains: subdomains,
                initialCenter: initialCenter,
                initialZoom: initialZoom,
                isInteractionEnabled: !isBoxMode,
                onRegionChange: { visibleRegion = $0 }
            )
            .ignoresSafeArea()

            if !boxRect.isEmpty {
                selectionOverlay
            }

            if isBoxMode && !boxRect.isEmpty {
                HandleOverlayView(boxRect: $boxRect, screenSize: screenSize)
                    .ignoresSafeArea()
            }

            controls
        }
        .ignoresSafeArea()
        .onGeometryChange(for: CGSize.self, of: \.size) { size in
            if screenSize != size {
                screenSize = size
                if boxRect.isEmpty { initBox(in: size) }
            }
        }
    }

    // MARK: - Dimming + border overlay

    private var selectionOverlay: some View {
        Canvas { ctx, size in
            var path = Path()
            path.addRect(CGRect(origin: .zero, size: size))
            path.addRect(boxRect)
            ctx.fill(path, with: .color(.black.opacity(0.4)), style: FillStyle(eoFill: true))
            ctx.stroke(
                Path(boxRect),
                with: .color(.white),
                style: StrokeStyle(lineWidth: 2)
            )
        }
        .ignoresSafeArea()
        .allowsHitTesting(false)
    }

    // MARK: - Mode toggle + action bar

    private var controls: some View {
        VStack {
            HStack {
                Spacer()
                Button {
                    withAnimation(.easeInOut(duration: 0.2)) { isBoxMode.toggle() }
                } label: {
                    Image(systemName: isBoxMode ? "map" : "crop")
                        .padding(8)
                }
                .glassEffect()
                .padding(.top, 60)
                .padding(.trailing, 16)
            }

            Spacer()

            HStack(spacing: 16) {
                Button("Cancel", action: onDismiss)
                    .frame(maxWidth: .infinity)
                    .buttonStyle(.bordered)
                Button("Save Region") {
                    confirmSelection()
                }
                .frame(maxWidth: .infinity)
                .buttonStyle(.borderedProminent)
                .disabled(visibleRegion == nil)
            }
            .padding(.horizontal)
            .padding(.vertical, 12)
            .background(.ultraThinMaterial)
        }
    }

    // MARK: - Helpers

    private func initBox(in size: CGSize) {
        let fraction = 0.6
        let w = size.width * fraction
        let h = size.height * fraction
        boxRect = CGRect(
            x: (size.width - w) / 2,
            y: (size.height - h) / 2,
            width: w,
            height: h
        )
    }

    private func confirmSelection() {
        guard let region = visibleRegion, !boxRect.isEmpty, screenSize != .zero else { return }

        let latTop = region.center.latitude + region.span.latitudeDelta / 2.0
        let lonLeft = region.center.longitude - region.span.longitudeDelta / 2.0
        let latPerPx = region.span.latitudeDelta / screenSize.height
        let lonPerPx = region.span.longitudeDelta / screenSize.width

        let maxLat = latTop - boxRect.minY * latPerPx
        let minLat = latTop - boxRect.maxY * latPerPx
        let minLon = lonLeft + boxRect.minX * lonPerPx
        let maxLon = lonLeft + boxRect.maxX * lonPerPx

        let box = BoundingBox(
            minLat: min(minLat, maxLat),
            maxLat: max(minLat, maxLat),
            minLon: min(minLon, maxLon),
            maxLon: max(minLon, maxLon)
        )

        let latSpan = abs(maxLat - minLat)
        let zoom = Int(log2(360.0 / max(latSpan, 1e-4))).clamped(to: 1...18)

        onSelected(box, Int32(zoom))
    }
}

// MARK: - TileMapView

private struct TileMapView: UIViewRepresentable {
    let urlTemplate: String
    let subdomains: [String]
    let initialCenter: CLLocationCoordinate2D?
    let initialZoom: Int
    let isInteractionEnabled: Bool
    let onRegionChange: (MKCoordinateRegion) -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(onRegionChange: onRegionChange)
    }

    func makeUIView(context: Context) -> MKMapView {
        let mapView = MKMapView()
        mapView.delegate = context.coordinator
        mapView.showsUserLocation = false
        mapView.isRotateEnabled = false
        mapView.isPitchEnabled = false
        let overlay = CustomTileOverlay(urlTemplate: urlTemplate, subdomains: subdomains)
        overlay.canReplaceMapContent = true
        mapView.addOverlay(overlay, level: .aboveLabels)
        if let center = initialCenter {
            let degreesPerTile = 360.0 / pow(2.0, Double(initialZoom))
            let span = MKCoordinateSpan(latitudeDelta: degreesPerTile * 3, longitudeDelta: degreesPerTile * 3)
            mapView.setRegion(MKCoordinateRegion(center: center, span: span), animated: false)
        }
        return mapView
    }

    func updateUIView(_ mapView: MKMapView, context: Context) {
        mapView.isUserInteractionEnabled = isInteractionEnabled
        context.coordinator.onRegionChange = onRegionChange

        if let existing = mapView.overlays.compactMap({ $0 as? CustomTileOverlay }).first {
            if existing.urlTemplate != urlTemplate || existing.subdomains != subdomains {
                mapView.removeOverlay(existing)
                let overlay = CustomTileOverlay(urlTemplate: urlTemplate, subdomains: subdomains)
                overlay.canReplaceMapContent = true
                mapView.addOverlay(overlay, level: .aboveLabels)
            }
        }
    }

    final class Coordinator: NSObject, MKMapViewDelegate {
        var onRegionChange: (MKCoordinateRegion) -> Void

        init(onRegionChange: @escaping (MKCoordinateRegion) -> Void) {
            self.onRegionChange = onRegionChange
        }

        func mapViewDidChangeVisibleRegion(_ mapView: MKMapView) {
            onRegionChange(mapView.region)
        }

        func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
            if let tile = overlay as? MKTileOverlay {
                return MKTileOverlayRenderer(tileOverlay: tile)
            }
            return MKOverlayRenderer(overlay: overlay)
        }
    }
}

// MARK: - CustomTileOverlay

final class CustomTileOverlay: MKTileOverlay {
    let subdomains: [String]

    init(urlTemplate: String, subdomains: [String]) {
        self.subdomains = subdomains
        super.init(urlTemplate: urlTemplate)
    }

    override func url(forTilePath path: MKTileOverlayPath) -> URL {
        var s = urlTemplate ?? ""
        if !subdomains.isEmpty {
            let sub = subdomains[Int(path.x + path.y) % subdomains.count]
            s = s.replacingOccurrences(of: "{s}", with: sub)
        } else {
            s = s.replacingOccurrences(of: "{s}", with: "")
        }
        s = s
            .replacingOccurrences(of: "{z}", with: "\(path.z)")
            .replacingOccurrences(of: "{x}", with: "\(path.x)")
            .replacingOccurrences(of: "{y}", with: "\(path.y)")
        return URL(string: s) ?? super.url(forTilePath: path)
    }
}

// MARK: - HandleOverlayView

private struct HandleOverlayView: View {
    @Binding var boxRect: CGRect
    let screenSize: CGSize

    private let minSide: CGFloat = 80
    private let handleSize: CGFloat = 22

    @State private var moveStartRect: CGRect = .zero
    @State private var isDraggingBox = false

    var body: some View {
        ZStack {
            Color.clear
                .contentShape(Rectangle())
                .frame(
                    width: max(0, boxRect.width - handleSize * 2),
                    height: max(0, boxRect.height - handleSize * 2)
                )
                .position(x: boxRect.midX, y: boxRect.midY)
                .gesture(boxTranslateGesture)

            ForEach(BoxHandle.allCases, id: \.self) { handle in
                Circle()
                    .fill(Color.white)
                    .overlay(Circle().strokeBorder(Color(white: 0.7), lineWidth: 1))
                    .frame(width: handleSize, height: handleSize)
                    .position(handle.position(in: boxRect))
                    .gesture(
                        DragGesture(minimumDistance: 0, coordinateSpace: .global)
                            .onChanged { applyHandleDrag(handle: handle, at: $0.location) }
                    )
            }
        }
        .frame(width: screenSize.width, height: screenSize.height)
    }

    private var boxTranslateGesture: some Gesture {
        DragGesture(minimumDistance: 0)
            .onChanged { value in
                if !isDraggingBox {
                    isDraggingBox = true
                    moveStartRect = boxRect
                }
                let dx = value.translation.width
                let dy = value.translation.height
                let newX = (moveStartRect.minX + dx).clamped(to: 0...(screenSize.width - moveStartRect.width))
                let newY = (moveStartRect.minY + dy).clamped(to: 0...(screenSize.height - moveStartRect.height))
                boxRect = CGRect(origin: CGPoint(x: newX, y: newY), size: moveStartRect.size)
            }
            .onEnded { _ in isDraggingBox = false }
    }

    private func applyHandleDrag(handle: BoxHandle, at location: CGPoint) {
        let x = location.x.clamped(to: 0...screenSize.width)
        let y = location.y.clamped(to: 0...screenSize.height)
        let b = boxRect
        let minX = b.minX, minY = b.minY, maxX = b.maxX, maxY = b.maxY

        boxRect = handle.updatedRect(
            from: boxRect,
            x: x, y: y,
            minX: minX, minY: minY, maxX: maxX, maxY: maxY,
            minSide: minSide
        )
    }
}

// MARK: - BoxHandle

private enum BoxHandle: CaseIterable, Hashable {
    case topLeft, topCenter, topRight
    case midLeft, midRight
    case bottomLeft, bottomCenter, bottomRight

    func position(in rect: CGRect) -> CGPoint {
        switch self {
        case .topLeft:      return CGPoint(x: rect.minX, y: rect.minY)
        case .topCenter:    return CGPoint(x: rect.midX, y: rect.minY)
        case .topRight:     return CGPoint(x: rect.maxX, y: rect.minY)
        case .midLeft:      return CGPoint(x: rect.minX, y: rect.midY)
        case .midRight:     return CGPoint(x: rect.maxX, y: rect.midY)
        case .bottomLeft:   return CGPoint(x: rect.minX, y: rect.maxY)
        case .bottomCenter: return CGPoint(x: rect.midX, y: rect.maxY)
        case .bottomRight:  return CGPoint(x: rect.maxX, y: rect.maxY)
        }
    }

    func updatedRect(
        from b: CGRect,
        x: CGFloat, y: CGFloat,
        minX: CGFloat, minY: CGFloat, maxX: CGFloat, maxY: CGFloat,
        minSide: CGFloat
    ) -> CGRect {
        switch self {
        case .topLeft:
            let nx = min(x, maxX - minSide), ny = min(y, maxY - minSide)
            return CGRect(x: nx, y: ny, width: maxX - nx, height: maxY - ny)
        case .topCenter:
            let ny = min(y, maxY - minSide)
            return CGRect(x: minX, y: ny, width: b.width, height: maxY - ny)
        case .topRight:
            let nx = max(x, minX + minSide), ny = min(y, maxY - minSide)
            return CGRect(x: minX, y: ny, width: nx - minX, height: maxY - ny)
        case .midLeft:
            let nx = min(x, maxX - minSide)
            return CGRect(x: nx, y: minY, width: maxX - nx, height: b.height)
        case .midRight:
            return CGRect(x: minX, y: minY, width: max(x, minX + minSide) - minX, height: b.height)
        case .bottomLeft:
            let nx = min(x, maxX - minSide)
            return CGRect(x: nx, y: minY, width: maxX - nx, height: max(y, minY + minSide) - minY)
        case .bottomCenter:
            return CGRect(x: minX, y: minY, width: b.width, height: max(y, minY + minSide) - minY)
        case .bottomRight:
            return CGRect(x: minX, y: minY, width: max(x, minX + minSide) - minX, height: max(y, minY + minSide) - minY)
        }
    }
}

// MARK: - Helpers

private extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        Swift.min(Swift.max(self, range.lowerBound), range.upperBound)
    }
}
