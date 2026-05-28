import Foundation
import CoreGraphics
import CoreLocation
import MapLibre
import UIKit

// MARK: - Offline Downloads

@objcMembers public class MapLibreOfflineController: NSObject {
    public static let shared = MapLibreOfflineController()
    private let storage = MLNOfflineStorage.shared

    public func startDownload(
        styleUrl: String,
        minLat: Double, maxLat: Double,
        minLon: Double, maxLon: Double,
        minZoom: Int, maxZoom: Int,
        nativeId: Int64,
        completion: @escaping (NSError?) -> Void
    ) {
        guard let styleURL = URL(string: styleUrl) else {
            completion(NSError(domain: "MapLibreBridge", code: -1,
                               userInfo: [NSLocalizedDescriptionKey: "Invalid style URL"]))
            return
        }
        let bounds = MLNCoordinateBounds(
            sw: CLLocationCoordinate2D(latitude: minLat, longitude: minLon),
            ne: CLLocationCoordinate2D(latitude: maxLat, longitude: maxLon)
        )
        let region = MLNTilePyramidOfflineRegion(
            styleURL: styleURL,
            bounds: bounds,
            fromZoomLevel: Double(minZoom),
            toZoomLevel: Double(maxZoom)
        )
        let context = nativeId.toData()
        storage.addPack(for: region, withContext: context) { pack, error in
            if let error = error {
                completion(error as NSError)
                return
            }
            pack?.resume()
            completion(nil)
        }
    }

    public func observeProgress(
        nativeId: Int64,
        onProgress: @escaping (Int64, Int64) -> Void,
        onComplete: @escaping () -> Void,
        onError: @escaping (String) -> Void
    ) -> MapLibreObservationToken {
        let center = NotificationCenter.default

        let progressObs = center.addObserver(
            forName: .MLNOfflinePackProgressChanged, object: nil, queue: nil
        ) { notification in
            guard let pack = notification.object as? MLNOfflinePack,
                  pack.context.toInt64() == nativeId else { return }
            let downloaded = Int64(pack.progress.countOfResourcesCompleted)
            let total = Int64(pack.progress.countOfResourcesExpected)
            onProgress(downloaded, total)
            if pack.state == .complete { onComplete() }
        }

        let errorObs = center.addObserver(
            forName: .MLNOfflinePackError, object: nil, queue: nil
        ) { notification in
            guard let pack = notification.object as? MLNOfflinePack,
                  pack.context.toInt64() == nativeId else { return }
            let error = notification.userInfo?[MLNOfflinePackUserInfoKey.error] as? NSError
            onError(error?.localizedDescription ?? "Download error")
        }

        return MapLibreObservationToken(
            progressObserver: progressObs,
            errorObserver: errorObs,
            center: center
        )
    }

    public func removePack(nativeId: Int64, completion: @escaping (NSError?) -> Void) {
        let pack = storage.packs?
            .compactMap { $0 as? MLNOfflinePack }
            .first { $0.context.toInt64() == nativeId }
        guard let pack = pack else {
            completion(nil)
            return
        }
        storage.removePack(pack) { error in
            completion(error as NSError?)
        }
    }
}

@objcMembers public class MapLibreObservationToken: NSObject {
    private var progressObserver: Any?
    private var errorObserver: Any?
    private let center: NotificationCenter

    init(progressObserver: Any, errorObserver: Any, center: NotificationCenter) {
        self.progressObserver = progressObserver
        self.errorObserver = errorObserver
        self.center = center
    }

    public func cancel() {
        if let obs = progressObserver { center.removeObserver(obs) }
        if let obs = errorObserver { center.removeObserver(obs) }
        progressObserver = nil
        errorObserver = nil
    }
}

// MARK: - Thumbnails

@objcMembers public class MapLibreThumbnailController: NSObject {
    public static func generateSnapshot(
        styleUrl: String,
        minLat: Double, maxLat: Double,
        minLon: Double, maxLon: Double,
        zoomLevel: Double,
        outputPath: String,
        completion: @escaping (Bool) -> Void
    ) {
        guard let styleURL = URL(string: styleUrl) else { completion(false); return }
        let bounds = MLNCoordinateBounds(
            sw: CLLocationCoordinate2D(latitude: minLat, longitude: minLon),
            ne: CLLocationCoordinate2D(latitude: maxLat, longitude: maxLon)
        )
        let center = CLLocationCoordinate2D(
            latitude: (minLat + maxLat) / 2,
            longitude: (minLon + maxLon) / 2
        )
        let camera = MLNMapCamera(lookingAtCenter: center, altitude: 0, pitch: 0, heading: 0)
        let size = CGSize(width: 256, height: 256)
        let options = MLNMapSnapshotOptions(styleURL: styleURL, camera: camera, size: size)
        options.zoomLevel = zoomLevel
        options.coordinateBounds = bounds

        let snapshotter = MLNMapSnapshotter(options: options)
        snapshotter.start { snapshot, error in
            guard let snapshot = snapshot, error == nil else { completion(false); return }
            guard let pngData = snapshot.image.pngData() else { completion(false); return }
            let url = URL(fileURLWithPath: outputPath)
            do {
                try FileManager.default.createDirectory(
                    at: url.deletingLastPathComponent(),
                    withIntermediateDirectories: true
                )
                try pngData.write(to: url)
                completion(true)
            } catch {
                completion(false)
            }
        }
    }
}

// MARK: - Helpers

private extension Int64 {
    func toData() -> Data {
        var value = self
        return Data(bytes: &value, count: MemoryLayout<Int64>.size)
    }
}

private extension Data {
    func toInt64() -> Int64 {
        guard count >= MemoryLayout<Int64>.size else { return 0 }
        return withUnsafeBytes { $0.load(as: Int64.self) }
    }
}
