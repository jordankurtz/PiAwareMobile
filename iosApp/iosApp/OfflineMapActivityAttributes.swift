import ActivityKit
import Foundation

struct OfflineMapActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        var downloaded: Int
        var total: Int
    }

    var regionName: String
}
