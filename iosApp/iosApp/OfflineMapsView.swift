import SwiftUI
import ComposeApp

struct OfflineMapsView: View {
    @Environment(OfflineMapsBridge.self) private var offlineMaps
    @Environment(SettingsBridge.self) private var settingsBridge

    private var resolvedTileURL: String {
        guard let s = settingsBridge.settings else {
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
        guard let s = settingsBridge.settings else { return [] }
        let activeId = s.mapProviderId ?? "openstreetmap"
        let allBuiltIn = KoinHelpersKt.getBuiltInTileProviders() + KoinHelpersKt.getApiKeyTileProviders()
        return allBuiltIn.first(where: { $0.id == activeId })?.subdomains ?? []
    }
    @State private var showDownloadSetup = false
    @State private var showRegionPicker = false
    @State private var pendingBounds: BoundingBox? = nil
    @State private var pendingName = ""
    @State private var pendingViewportZoom: Int32 = 10

    var body: some View {
        List {
            if offlineMaps.regions.isEmpty {
                VStack(spacing: 8) {
                    Text("No offline maps")
                        .font(.headline)
                    Text("Download a region to use maps without a connection.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .listRowBackground(Color.clear)
                .padding(.vertical, 32)
            } else {
                ForEach(offlineMaps.regions, id: \.id) { region in
                    OfflineRegionRow(
                        region: region,
                        onDelete: { offlineMaps.requestDeleteRegion(region) },
                        onRetry: { offlineMaps.retryDownload(region) },
                        onCancel: { offlineMaps.cancelDownload() }
                    )
                }
            }
        }
        .navigationTitle("Offline Maps")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button { showDownloadSetup = true } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showDownloadSetup) {
            DownloadSetupSheet(
                pendingName: $pendingName,
                pendingBounds: $pendingBounds,
                pendingViewportZoom: $pendingViewportZoom,
                onPickOnMap: {
                    showDownloadSetup = false
                    showRegionPicker = true
                },
                onStart: { minZoom, maxZoom in
                    guard let bounds = pendingBounds else { return }
                    offlineMaps.startDownload(
                        name: pendingName,
                        bounds: bounds,
                        minZoom: minZoom,
                        maxZoom: maxZoom,
                        viewportZoom: pendingViewportZoom,
                        providerId: settingsBridge.settings?.mapProviderId ?? "openstreetmap",
                        urlTemplate: resolvedTileURL
                    )
                    pendingName = ""
                    pendingBounds = nil
                    showDownloadSetup = false
                },
                onDismiss: {
                    pendingName = ""
                    pendingBounds = nil
                    showDownloadSetup = false
                }
            )
        }
        .fullScreenCover(isPresented: $showRegionPicker) {
            RegionPickerView(
                tileURL: resolvedTileURL,
                subdomains: resolvedSubdomains,
                onSelected: { bounds, viewportZoom in
                    pendingBounds = bounds
                    pendingViewportZoom = viewportZoom
                    showRegionPicker = false
                    showDownloadSetup = true
                },
                onDismiss: {
                    showRegionPicker = false
                    showDownloadSetup = true
                }
            )
        }
        .alert(
            "Delete Region?",
            isPresented: Binding(
                get: { offlineMaps.pendingDeleteRegion != nil },
                set: { if !$0 { offlineMaps.cancelDelete() } }
            )
        ) {
            Button("Delete", role: .destructive) { offlineMaps.confirmDelete() }
            Button("Cancel", role: .cancel) { offlineMaps.cancelDelete() }
        } message: {
            let mb = Int(offlineMaps.pendingDeleteFreedBytes / 1_048_576)
            Text(mb > 0 ? "This will free \(mb) MB." : "This region will be deleted.")
        }
    }
}

// MARK: - Region Row

private struct OfflineRegionRow: View {
    let region: OfflineRegion
    let onDelete: () -> Void
    let onRetry: () -> Void
    let onCancel: () -> Void

    private var fraction: Double {
        guard region.tileCount > 0 else { return 0 }
        return Double(region.downloadedTileCount) / Double(region.tileCount)
    }

    var body: some View {
        HStack(spacing: 12) {
            // Thumbnail
            Group {
                if let path = region.thumbnailPath {
                    AsyncImage(url: URL(fileURLWithPath: path)) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        Color.secondary.opacity(0.2)
                    }
                } else {
                    Image(systemName: "map")
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .background(Color.secondary.opacity(0.1))
                }
            }
            .frame(width: 64, height: 64)
            .clipShape(.rect(cornerRadius: 8))

            // Info
            VStack(alignment: .leading, spacing: 4) {
                Text(region.name).font(.headline)
                Text("Zoom \(region.minZoom)–\(region.maxZoom)")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)

                switch region.status {
                case .downloading:
                    ProgressView(value: fraction)
                    Text("\(region.downloadedTileCount) / \(region.tileCount) tiles")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                case .partial:
                    Text("\(region.downloadedTileCount) of \(region.tileCount) tiles downloaded")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                case .failed:
                    Text("Download failed")
                        .font(.caption)
                        .foregroundStyle(.red)
                case .complete:
                    let mb = Int(region.sizeBytes / 1_048_576)
                    Text("\(mb) MB")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                default:
                    EmptyView()
                }
            }

            Spacer()

            // Actions
            VStack(spacing: 8) {
                switch region.status {
                case .downloading:
                    Button(action: onCancel) {
                        Image(systemName: "xmark.circle")
                    }
                case .partial, .failed:
                    Button(action: onRetry) {
                        Image(systemName: "arrow.clockwise")
                    }
                    Button(role: .destructive, action: onDelete) {
                        Image(systemName: "trash")
                    }
                case .complete:
                    Button(role: .destructive, action: onDelete) {
                        Image(systemName: "trash")
                    }
                default:
                    EmptyView()
                }
            }
            .buttonStyle(.plain)
            .foregroundStyle(.secondary)
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Download Setup Sheet

private struct DownloadSetupSheet: View {
    @Binding var pendingName: String
    @Binding var pendingBounds: BoundingBox?
    @Binding var pendingViewportZoom: Int32
    let onPickOnMap: () -> Void
    let onStart: (Int32, Int32) -> Void
    let onDismiss: () -> Void

    @State private var minZoom: Double = 6
    @State private var maxZoom: Double = 12

    private var hasBounds: Bool { pendingBounds != nil }
    private var isValid: Bool {
        !pendingName.trimmingCharacters(in: .whitespaces).isEmpty && hasBounds
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Region") {
                    TextField("Name", text: $pendingName).autocorrectionDisabled()
                    Button(hasBounds ? "Region selected — change" : "Select region on map") {
                        onPickOnMap()
                    }
                }
                Section("Zoom Levels") {
                    VStack(alignment: .leading) {
                        Text("Min zoom: \(Int(minZoom))")
                        Slider(value: $minZoom, in: 1...18, step: 1)
                    }
                    VStack(alignment: .leading) {
                        Text("Max zoom: \(Int(maxZoom))")
                        Slider(value: $maxZoom, in: 1...18, step: 1)
                            .onChange(of: minZoom) { _, newMin in
                                if maxZoom < newMin { maxZoom = newMin }
                            }
                    }
                }
            }
            .navigationTitle("Download Region")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { onDismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Download") {
                        onStart(Int32(minZoom), Int32(maxZoom))
                    }
                    .disabled(!isValid)
                }
            }
        }
    }
}
