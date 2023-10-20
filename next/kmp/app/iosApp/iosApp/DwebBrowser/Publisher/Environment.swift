//
//  BrowerVM.swift
//  DwebBrowser
//
//  Created by ui06 on 5/8/23.
//

import Combine
import Foundation
import SwiftUI

class KeyBoard: ObservableObject {
    @Published var height: CGFloat = 0
}

class SelectedTab: ObservableObject {
    @Published var curIndex: Int = 0
}

class AddressBarState: ObservableObject {
    @Published var isFocused = false
    @Published var inputText: String = ""
    @Published var shouldDisplay: Bool = true
    @Published var needRefreshOfIndex: Int = -1
    @Published var stopLoadingOfIndex: Int = -1

}

class ToolBarState: ObservableObject {
    @Published var shouldExpand = true
    @Published var canGoBack = false
    @Published var canGoForward = false
    @Published var goBackTapped = false
    @Published var goForwardTapped = false
    @Published var createTabTapped = false
}

class ShiftAnimation: ObservableObject {
    @Published var snapshotImage: UIImage = UIImage()
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

class TabGridState: ObservableObject {
    @Published var scale = 1.0
}

class DeleteCache: ObservableObject {
    @Published var cacheId = UUID()
}
