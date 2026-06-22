import Foundation
import CoreLocation
import ComposeApp
import Observation

// MARK: - LocationBridge

@Observable @MainActor
final class LocationBridge {
    let vm: LocationViewModel

    private(set) var coordinate: CLLocationCoordinate2D?

    private var observationTasks: [Task<Void, Never>] = []

    init(vm: LocationViewModel) {
        self.vm = vm
        startObserving()
    }

    // MARK: - Private observation

    private func startObserving() {
        let vm = self.vm

        observationTasks.append(Task { [weak self] in
            for await location in vm.currentLocation {
                guard let self else { return }
                if let location {
                    self.coordinate = CLLocationCoordinate2D(
                        latitude: location.latitude,
                        longitude: location.longitude
                    )
                } else {
                    self.coordinate = nil
                }
            }
        })
    }
}
