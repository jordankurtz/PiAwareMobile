//
//  PiAwareWidgetsBundle.swift
//  PiAwareWidgets
//
//  Created by Jordan Kurtz on 4/8/26.
//  Copyright © 2026 orgName. All rights reserved.
//

import WidgetKit
import SwiftUI

@main
struct PiAwareWidgetsBundle: WidgetBundle {
    var body: some Widget {
        PiAwareWidgets()
        PiAwareWidgetsControl()
        PiAwareWidgetsLiveActivity()
    }
}
