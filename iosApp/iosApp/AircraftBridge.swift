import Foundation
import ComposeApp
import Observation

// MARK: - FlightState

enum FlightState {
    case notStarted
    case loading
    case success(Flight)
    case error(String)
}

// MARK: - AircraftBridge

@Observable @MainActor
final class AircraftBridge {
    let vm: AircraftViewModel

    private(set) var aircraft: [AircraftWithServers] = []
    private(set) var numberOfPlanes: Int = 0
    private(set) var selectedHex: String?
    private(set) var flightState: FlightState = .notStarted

    private var observationTasks: [Task<Void, Never>] = []

    init(vm: AircraftViewModel) {
        self.vm = vm
        startObserving()
    }

    nonisolated deinit {
        observationTasks.forEach { $0.cancel() }
    }

    // MARK: - Mutations

    func selectAircraft(_ hex: String?) {
        vm.selectAircraft(hex: hex)
    }

    func dismissFlight() {
        vm.onFlightDetailsDismissed()
    }

    // MARK: - Private observation

    private func startObserving() {
        let vm = self.vm

        observationTasks.append(Task { [weak self] in
            for await value in vm.aircraft {
                guard let self else { return }
                self.aircraft = value
            }
        })

        observationTasks.append(Task { [weak self] in
            for await value in vm.numberOfPlanes {
                guard let self else { return }
                self.numberOfPlanes = value.intValue
            }
        })

        observationTasks.append(Task { [weak self] in
            for await value in vm.selectedAircraftHex {
                guard let self else { return }
                self.selectedHex = value
            }
        })

        observationTasks.append(Task { [weak self] in
            for await value in vm.flightDetails {
                guard let self else { return }
                switch onEnum(of: value) {
                case .notStarted:
                    self.flightState = .notStarted
                case .loading:
                    self.flightState = .loading
                case .success(let s):
                    if let flight = s.data {
                        self.flightState = .success(flight)
                    }
                case .error(let e):
                    self.flightState = .error(e.message)
                }
            }
        })
    }
}
