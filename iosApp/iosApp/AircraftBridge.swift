import Foundation
import ComposeApp
import Observation

@Observable @MainActor
final class AircraftBridge {
    let vm: AircraftViewModel
    init(vm: AircraftViewModel) { self.vm = vm }
}
