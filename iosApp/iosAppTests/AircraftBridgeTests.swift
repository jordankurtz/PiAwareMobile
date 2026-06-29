import XCTest
import ComposeApp
@testable import iosApp

/// Integration tests for the map-tap → flight sheet chain.
///
/// The chain relies on:
/// 1. AircraftViewModel being @Single in Koin (same instance shared by MapScreen
///    and the Swift bridge)
/// 2. The SKIE async sequence delivering selectAircraft updates to AircraftBridge
@MainActor
final class AircraftBridgeTests: XCTestCase {
    private var vm: AircraftViewModel!
    private var bridge: AircraftBridge!

    override func setUp() async throws {
        try await super.setUp()
        vm = SwiftBridgeKt.getAircraftViewModel()
        bridge = AircraftBridge(vm: vm)
        // Reset any selection from a prior test
        vm.selectAircraft(hex: nil)
        try await Task.sleep(for: .milliseconds(50))
    }

    override func tearDown() async throws {
        vm.selectAircraft(hex: nil)
        try await super.tearDown()
    }

    // MARK: - Koin singleton scope

    /// Regression test for the @Factory → @Single fix.
    /// If AircraftViewModel were @Factory, getAircraftViewModel() would return
    /// a new instance each call, breaking the map-tap → flight sheet chain.
    func testAircraftViewModelIsKoinSingleton() {
        let vm1 = SwiftBridgeKt.getAircraftViewModel()
        let vm2 = SwiftBridgeKt.getAircraftViewModel()
        XCTAssertIdentical(vm1, vm2, "AircraftViewModel must be @Single — the iOS bridge and MapScreen must share one instance")
    }

    // MARK: - SKIE observation chain

    func testBridgeReceivesSelectionFromViewModel() async throws {
        vm.selectAircraft(hex: "ABC123")
        try await Task.sleep(for: .milliseconds(100))
        XCTAssertEqual(bridge.selectedHex, "ABC123")
    }

    func testBridgeUpdatesWhenSelectionChanges() async throws {
        vm.selectAircraft(hex: "ABC123")
        try await Task.sleep(for: .milliseconds(100))

        vm.selectAircraft(hex: "DEF456")
        try await Task.sleep(for: .milliseconds(100))

        XCTAssertEqual(bridge.selectedHex, "DEF456")
    }

    func testDismissResetsSelectedHex() async throws {
        vm.selectAircraft(hex: "ABC123")
        try await Task.sleep(for: .milliseconds(100))

        bridge.dismissFlight()
        try await Task.sleep(for: .milliseconds(100))

        XCTAssertNil(bridge.selectedHex)
    }

    func testBridgeSelectAircraftRoutesToViewModel() async throws {
        // Calling bridge.selectAircraft should push through to the VM and back
        bridge.selectAircraft("ABC123")
        try await Task.sleep(for: .milliseconds(100))
        XCTAssertEqual(bridge.selectedHex, "ABC123")
    }
}
