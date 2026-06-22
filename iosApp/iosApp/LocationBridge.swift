import Foundation
import ComposeApp
import Observation

@Observable @MainActor
final class LocationBridge {
    let vm: LocationViewModel
    init(vm: LocationViewModel) { self.vm = vm }
}
