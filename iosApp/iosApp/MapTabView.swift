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
                .modifier(GlassButtonModifier())
                .padding(.bottom, 100)
                .padding(.trailing, 16)
            }
    }
}

private struct GlassButtonModifier: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 26, *) {
            content.buttonStyle(.glass)
        } else {
            content.buttonStyle(.borderedProminent)
        }
    }
}
