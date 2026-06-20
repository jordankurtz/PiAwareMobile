import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        KoinInitializerKt.doStartKoin()
    }

    var body: some Scene {
        WindowGroup {
            PiAwareTabView()
        }
    }
}
