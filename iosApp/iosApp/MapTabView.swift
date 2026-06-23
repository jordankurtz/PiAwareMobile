import SwiftUI
import ComposeApp

struct MapTabView: View {
    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            ComposeScreen { ScreenViewControllersKt.MapViewController() }
                .ignoresSafeArea()
            Button {
                ScreenViewControllersKt.toggleMapFollowUserLocation()
            } label: {
                Image(systemName: "location.fill")
                    .padding(12)
            }
            .buttonStyle(.glass)
            .padding(.trailing, 16)
            .padding(.bottom, 16)
        }
    }
}
