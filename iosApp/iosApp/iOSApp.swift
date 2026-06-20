import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        KoinInitializerKt.startKoin()
    }

    var body: some Scene {
        WindowGroup {
            PiAwareTabView()
        }
    }
}
