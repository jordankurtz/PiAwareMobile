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

    // MARK: - Mutations

    func addServer(name: String, address: String, type: ServerType) {
        vm.addServer(name: name, address: address, type: type)
    }

    func deleteServer(id: String) {
        let uuid = KotlinUuid.companion.parse(uuidString: id)
        vm.deleteServer(id: uuid)
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
