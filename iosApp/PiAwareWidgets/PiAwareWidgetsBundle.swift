import SwiftUI
import WidgetKit

@main
struct PiAwareWidgetsBundle: WidgetBundle {
    var body: some Widget {
        PiAwareWidgets()
        PiAwareWidgetsControl()
        OfflineMapWidget()
    }
}
