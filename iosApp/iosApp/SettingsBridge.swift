import Foundation
import ComposeApp
import Observation

@Observable @MainActor
final class SettingsBridge {
    let vm: SettingsViewModel
    init(vm: SettingsViewModel) { self.vm = vm }
}
