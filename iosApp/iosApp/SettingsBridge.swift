import Foundation
import ComposeApp
import Observation

// MARK: - SettingsBridge

@Observable @MainActor
final class SettingsBridge {
    let vm: SettingsViewModel

    private(set) var settings: Settings?

    private var observationTasks: [Task<Void, Never>] = []

    init(vm: SettingsViewModel) {
        self.vm = vm
        startObserving()
    }

    deinit {
        observationTasks.forEach { $0.cancel() }
    }

    // MARK: - Mutations

    func addServer(name: String, address: String, type: ServerType) {
        vm.addServer(name: name, address: address, type: type)
    }

    func deleteServer(id: String) {
        let uuid = KotlinUuid.companion.parse(uuidString: id)
        vm.deleteServer(id: uuid)
    }

    func editServer(id: String, name: String, address: String, type: ServerType) {
        guard let existing = settings?.servers.first(where: { $0.id.toHexDashString() == id }) else { return }
        vm.editServer(server: Server(id: existing.id, name: name, address: address, type: type))
    }

    func updateRefreshInterval(_ interval: Int) {
        vm.updateRefreshInterval(refreshInterval: Int32(interval))
    }

    func updateEnableFlightAwareApi(_ enabled: Bool) {
        vm.updateEnableFlightAwareApi(enabled: enabled)
    }

    func updateFlightAwareApiKey(_ key: String) {
        vm.updateFlightAwareApiKey(apiKey: key)
    }

    func updateOpenUrlsExternally(_ enabled: Bool) {
        vm.updateOpenUrlsExternally(enabled: enabled)
    }

    func updateCenterMapOnUserOnStart(_ enabled: Bool) {
        vm.updateCenterMapOnUserOnStart(enabled: enabled)
    }

    func updateRestoreMapStateOnStart(_ enabled: Bool) {
        vm.updateRestoreMapStateOnStart(enabled: enabled)
    }

    func updateShowReceiverLocations(_ enabled: Bool) {
        vm.updateShowReceiverLocations(enabled: enabled)
    }

    func updateShowUserLocationOnMap(_ enabled: Bool) {
        vm.updateShowUserLocationOnMap(enabled: enabled)
    }

    func updateTrailDisplayMode(_ mode: TrailDisplayMode) {
        vm.updateTrailDisplayMode(trailDisplayMode: mode)
    }

    func updateShowMinimapTrails(_ enabled: Bool) {
        vm.updateShowMinimapTrails(enabled: enabled)
    }

    func updateDefaultZoomLevel(_ zoom: Int) {
        vm.updateDefaultZoomLevel(zoom: Int32(zoom))
    }

    func updateMinZoomLevel(_ zoom: Int) {
        vm.updateMinZoomLevel(zoom: Int32(zoom))
    }

    func updateMaxZoomLevel(_ zoom: Int) {
        vm.updateMaxZoomLevel(zoom: Int32(zoom))
    }

    func clearTileCache() {
        vm.clearTileCache()
    }

    // MARK: - Private observation

    private func startObserving() {
        let vm = self.vm

        observationTasks.append(Task { [weak self] in
            for await value in vm.settings {
                guard let self else { return }
                switch onEnum(of: value) {
                case .notStarted, .loading, .error:
                    break
                case .success(let s):
                    if let loaded = s.data {
                        self.settings = loaded
                    }
                }
            }
        })
    }
}
