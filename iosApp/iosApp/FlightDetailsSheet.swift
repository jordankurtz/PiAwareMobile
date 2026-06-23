import SwiftUI

struct FlightDetailsSheet: View {
    @Environment(AircraftBridge.self) private var aircraft

    var body: some View {
        Text("Flight details — Task H")
            .padding()
    }
}
