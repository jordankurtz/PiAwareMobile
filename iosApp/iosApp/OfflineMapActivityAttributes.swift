import ActivityKit
import Foundation

enum DownloadStatus: String, Codable, Hashable {
    case downloading
    case complete
    case failed
}

struct OfflineMapActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        var downloaded: Int
        var total: Int
        var status: DownloadStatus
    }

    var regionName: String
}
