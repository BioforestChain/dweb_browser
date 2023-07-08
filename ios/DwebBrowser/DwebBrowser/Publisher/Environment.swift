//
//  BrowerVM.swift
//  DwebBrowser
//
//  Created by ui06 on 5/8/23.
//

import Combine
import Foundation
import SwiftUI

class SelectedTab: ObservableObject {
    @Published var curIndex: Int = 0
}

class AddressBarState: ObservableObject {
    @Published var isFocused = false
    @Published var inputText: String = ""
}

class ToolBarState: ObservableObject {
    @Published var shouldExpand = false
    @Published var canGoBack = false
    @Published var canGoForward = false
    @Published var goBackTapped = false
    @Published var goForwardTapped = false

    var addressBarHeight: CGFloat {
        shouldExpand ? addressBarH : 0
    }
}

class ShiftAnimation: ObservableObject {
    @Published var snapshotImage: UIImage = .defaultSnapShotImage
    @Published var progress: AnimationProgress = .invisible
}

class TraceLessMode {
    static var shared = TraceLessMode()
    private let tracelessKEY = "tracelessKEY"
    var isON: Bool {
        willSet {
            UserDefaults.standard.setValue(newValue, forKey: tracelessKEY)
        }
    }

    private init() {
        isON = UserDefaults.standard.bool(forKey: tracelessKEY)
    }
}

class OpeningLink: ObservableObject {
    @Published var clickedLink: URL = emptyURL
}

class ShowSheet: ObservableObject {
    @Published var should: Bool = false
}

class TabGridState: ObservableObject {
    @Published var scale = 1.0
    @Published var opacity = 1.0
}
