import SwiftUI
import ComposeApp

/// Thin SwiftUI wrapper around the Compose MapProvidersScreen.
/// The full provider list, custom provider CRUD, and API keys are handled inside Compose.
struct MapProvidersView: View {
    var body: some View {
        ComposeScreen { ScreenViewControllersKt.MapProvidersViewController() }
            .navigationTitle("Map Provider")
            .navigationBarTitleDisplayMode(.inline)
            .ignoresSafeArea()
    }
}
