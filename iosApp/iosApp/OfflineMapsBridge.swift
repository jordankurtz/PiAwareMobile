import Foundation
import ComposeApp

@MainActor
@Observable
final class OfflineMapsBridge {
    private let vm: OfflineMapsViewModel

    var regions: [OfflineRegion] = []
    var pendingDeleteRegion: OfflineRegion? = nil
    var pendingDeleteFreedBytes: Int64 = 0

    init(vm: OfflineMapsViewModel) {
        self.vm = vm
        Task { await observeRegions() }
        Task { await observePendingDelete() }
        Task { await observePendingDeleteBytes() }
    }

    func cancelDownload() { vm.cancelDownload() }
    func requestDeleteRegion(_ region: OfflineRegion) { vm.requestDeleteRegion(region: region) }
    func confirmDelete() { vm.confirmDelete() }
    func cancelDelete() { vm.cancelDelete() }
    func retryDownload(_ region: OfflineRegion) { vm.retryDownload(region: region) }

    func startDownload(
        name: String,
        bounds: BoundingBox,
        minZoom: Int32,
        maxZoom: Int32,
        viewportZoom: Int32
    ) {
        vm.startDownload(
            name: name,
            bounds: bounds,
            minZoom: minZoom,
            maxZoom: maxZoom,
            viewportZoom: viewportZoom
        )
    }

    private func observeRegions() async {
        for await value in vm.regions { regions = value }
    }

    private func observePendingDelete() async {
        for await value in vm.pendingDeleteRegion { pendingDeleteRegion = value }
    }

    private func observePendingDeleteBytes() async {
        for await value in vm.pendingDeleteFreedBytes { pendingDeleteFreedBytes = value }
    }
}
