import ActivityKit
import BackgroundTasks
import ComposeApp

class DownloadActivityManager: IosDownloadObserver {
    private var activity: Activity<OfflineMapActivityAttributes>?
    private var currentTask: BGContinuedProcessingTask?
    private var lastTotal: Int = 0

    func setCurrentTask(_ task: BGContinuedProcessingTask) {
        currentTask = task
    }

    func onDownloadStarting(regionName: String) {
        let request = BGContinuedProcessingTaskRequest(
            identifier: "com.jordankurtz.piawaremobile.offlinedownload",
            title: "Downloading Offline Map",
            subtitle: regionName
        )
        try? BGTaskScheduler.shared.submit(request)

        let attributes = OfflineMapActivityAttributes(regionName: regionName)
        let contentState = OfflineMapActivityAttributes.ContentState(
            downloaded: 0,
            total: 0,
            status: .downloading
        )
        activity = try? Activity.request(attributes: attributes, contentState: contentState, pushType: nil)
    }

    func onProgress(downloaded: Int64, total: Int64) {
        lastTotal = Int(total)
        let state = OfflineMapActivityAttributes.ContentState(
            downloaded: Int(downloaded),
            total: Int(total),
            status: .downloading
        )
        Task { await self.activity?.update(using: state) }
    }

    func onComplete(regionName: String) {
        let finalState = OfflineMapActivityAttributes.ContentState(
            downloaded: lastTotal,
            total: lastTotal,
            status: .complete
        )
        let task = currentTask
        currentTask = nil
        Task {
            await self.activity?.end(using: finalState, dismissalPolicy: .after(.now + 5))
            self.activity = nil
            task?.setTaskCompleted(success: true)
        }
    }

    func onFailed(regionName: String) {
        let failedState = OfflineMapActivityAttributes.ContentState(
            downloaded: 0,
            total: 0,
            status: .failed
        )
        let task = currentTask
        currentTask = nil
        Task {
            await self.activity?.end(using: failedState, dismissalPolicy: .after(.now + 5))
            self.activity = nil
            task?.setTaskCompleted(success: false)
        }
    }

    func onCancelled() {
        let task = currentTask
        currentTask = nil
        Task {
            await self.activity?.end(using: nil, dismissalPolicy: .immediate)
            self.activity = nil
            task?.setTaskCompleted(success: false)
        }
    }
}
