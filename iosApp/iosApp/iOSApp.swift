import BackgroundTasks
import SwiftUI

@main
struct iOSApp: App {
    private let activityManager = DownloadActivityManager()

    init() {
        // BGTask handler must be registered before the app finishes launching.
        // The expirationHandler runs lazily (only when the task expires while
        // backgrounded), so accessing the coordinator there is safe — Koin is
        // already started by the time the task can expire.
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: "com.jordankurtz.piawaremobile.offlinedownload",
            using: nil
        ) { [activityManager] task in
            guard let task = task as? BGContinuedProcessingTask else { return }
            activityManager.setCurrentTask(task)
            task.expirationHandler = {
                IosCoordinatorProviderKt.getIosBackgroundDownloadCoordinator().cancel()
            }
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onAppear {
                    // Inject the Swift observer after Koin is initialised.
                    // Koin starts inside MainViewController which runs on first appearance.
                    IosCoordinatorProviderKt
                        .getIosBackgroundDownloadCoordinator()
                        .observer = activityManager
                }
        }
    }
}
