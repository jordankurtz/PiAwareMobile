import SwiftUI
import ComposeApp

struct MapTabView: View {
    var body: some View {
        ComposeScreen { ScreenViewControllersKt.MapViewController() }
            .ignoresSafeArea()
            .overlay(alignment: .bottomTrailing) {
                Button("", systemImage: "location.fill") {
                    ScreenViewControllersKt.toggleMapFollowUserLocation()
                }
                .buttonStyle(.glass)
                .padding(.bottom, 100)
                .padding(.trailing, 16)
            }
    }
}
