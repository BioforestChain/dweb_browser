//
//  BrowerVM.swift
//  DwebBrowser
//
//  Created by ui06 on 5/8/23.
//

import Combine
import Foundation
import Observation
import SwiftUI

@Observable
class SelectedTab {
    var index: Int = 0
}

class WebMonitor: ObservableObject {
    @Published var loadingProgress: Double = 0 {
        willSet {
            if newValue >= 1.0 {
                isLoadingDone = true
            } else {
                if isLoadingDone != false {
                    isLoadingDone = false
                }
            }
        }
    }

    @Published var isLoadingDone: Bool = false
}

@Observable
class WndDragScale {
    var onWidth: CGFloat = 1 {
        didSet {
            if onWidth < 0 {
                onWidth = 0
            }
        }
    }

    func properValue(max value: CGFloat) -> CGFloat {
        min(value, max(value * 0.333, value * onWidth))
    }

    var addressbarHeight: CGFloat { properValue(max: maxAddressBarH) }
    var toolbarItemWidth: CGFloat { properValue(max: 28.0) }

    var scaledFont_8: Font { Font.system(size: 1.0 * max(5, onWidth * 8)) }
    var scaledFont_12: Font { Font.system(size: 1.0 * max(6, onWidth * 12)) }
    var scaledFont_16: Font { Font.system(size: 1.0 * max(8, onWidth * 16)) }
    var scaledFont_18: Font { Font.system(size: 1.0 * max(9, onWidth * 18)) }
    var scaledFont_20: Font { Font.system(size: 1.0 * max(10, onWidth * 20)) }
    var scaledFont_22: Font { Font.system(size: 1.0 * max(11, onWidth * 22)) }
}

class AddressBarState: ObservableObject {
    @Published var isFocused = false
    @Published var inputText: String = ""
    @Published var shouldDisplay: Bool = true
    @Published var needRefreshOfIndex: Int = -1
    @Published var stopLoadingOfIndex: Int = -1
    @Published var searchInputText: String? = nil
}

class ToolBarState: ObservableObject {
    @Published var shouldExpand = true
    @Published var createTabTapped = false
    @Published var showMoreMenu = false
    @Published var isPresentingScanner = false
}

class ShiftAnimation: ObservableObject {
    @Published var snapshotImage: UIImage = lightSnapshotImage
    @Published var progress: AnimationProgress = .invisible
}

class TracelessMode {
    static var shared = TracelessMode()
    private let tracelessKEY = "tracelessKEY"
    var isON: Bool {
        willSet {
            browserViewDataSource.trackModel = newValue
        }
    }

    private init() {
        isON = browserViewDataSource.trackModel
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
