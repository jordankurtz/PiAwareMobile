import SwiftUI
import ComposeApp

struct MapTabView: View {
    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            ComposeScreen { ScreenViewControllersKt.MapViewController() }
                .ignoresSafeArea()
            // Temporary — remove guard after Task B bumps deployment target
            if #available(iOS 26, *) {
                Button {
                    ScreenViewControllersKt.toggleMapFollowUserLocation()
                } label: {
                    Image(systemName: "location.fill")
                        .padding(12)
                }
                .buttonStyle(.glass)
                .padding(.trailing, 16)
                .padding(.bottom, 16)
            } else {
                Button {
                    ScreenViewControllersKt.toggleMapFollowUserLocation()
                } label: {
                    Image(systemName: "location.fill")
                        .padding(12)
                }
                .buttonStyle(.borderedProminent)
                .padding(.trailing, 16)
                .padding(.bottom, 16)
            }
        }
    }
}
