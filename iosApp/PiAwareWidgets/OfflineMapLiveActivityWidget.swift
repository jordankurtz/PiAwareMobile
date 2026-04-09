import ActivityKit
import SwiftUI
import WidgetKit

struct OfflineMapLiveActivityView: View {
    let context: ActivityViewContext<OfflineMapActivityAttributes>

    private var progress: Double {
        guard context.state.total > 0 else { return 0 }
        return Double(context.state.downloaded) / Double(context.state.total)
    }

    var body: some View {
        switch context.state.status {
        case .downloading:
            VStack(alignment: .leading, spacing: 8) {
                Text("Downloading \(context.attributes.regionName)")
                    .font(.headline)
                    .lineLimit(1)
                if context.state.total > 0 {
                    ProgressView(value: progress)
                } else {
                    ProgressView()
                }
                Text("\(context.state.downloaded) / \(context.state.total) tiles")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .padding()
        case .complete:
            VStack(alignment: .leading, spacing: 8) {
                Text("\(context.attributes.regionName) downloaded")
                    .font(.headline)
                    .lineLimit(1)
                ProgressView(value: 1.0)
                Text("\(context.state.total) tiles")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .padding()
        case .failed:
            VStack(alignment: .leading, spacing: 8) {
                Text("\(context.attributes.regionName) download failed")
                    .font(.headline)
                    .lineLimit(1)
                Text("Tap to retry")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .padding()
        }
    }
}

struct OfflineMapWidget: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: OfflineMapActivityAttributes.self) { context in
            OfflineMapLiveActivityView(context: context)
        } dynamicIsland: { context in
            let progress: Double = context.state.total > 0
                ? Double(context.state.downloaded) / Double(context.state.total)
                : 0.0

            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Gauge(value: progress) {
                        Image(systemName: "map")
                    }
                    .gaugeStyle(.accessoryCircularCapacity)
                    .frame(width: 44, height: 44)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(context.attributes.regionName)
                            .font(.caption)
                            .lineLimit(1)
                        switch context.state.status {
                        case .downloading:
                            Text("\(context.state.downloaded) / \(context.state.total) tiles")
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        case .complete:
                            Text("Downloaded")
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        case .failed:
                            Text("Download failed")
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
            } compactLeading: {
                Gauge(value: progress) {
                    Image(systemName: "map")
                }
                .gaugeStyle(.accessoryCircularCapacity)
                .frame(width: 20, height: 20)
            } compactTrailing: {
                switch context.state.status {
                case .downloading:
                    Text(context.state.total > 0 ? "\(Int(progress * 100))%" : "…")
                        .font(.caption2)
                        .monospacedDigit()
                case .complete:
                    Image(systemName: "checkmark")
                        .font(.caption2)
                case .failed:
                    Image(systemName: "xmark")
                        .font(.caption2)
                }
            } minimal: {
                Gauge(value: progress) {
                    Image(systemName: "map")
                }
                .gaugeStyle(.accessoryCircularCapacity)
            }
        }
    }
}
