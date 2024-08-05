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

@Observable
class WebMonitor {
    var loadingProgress: Double = 0 {
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

    var isLoadingDone: Bool = false
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
    var scaledFont_28: Font { Font.system(size: 1.0 * max(32, onWidth * 28)) }
    var scaledFont_32: Font { Font.system(size: 1.0 * max(32, onWidth * 32)) }
    var scaledFont_42: Font { Font.system(size: 1.0 * max(42, onWidth * 42)) }
}

@Observable
class AddressBarState {
    var isFocused = false
    var inputText: String = ""
    var shouldDisplay: Bool = true
    var needRefreshOfIndex: Int = -1
    var stopLoadingOfIndex: Int = -1
}

@Observable
class ToolBarState {
    var tabsState = TabsStates.expanded
    var shouldCreateTab = false
    var showMoreMenu = false
    var isPresentingScanner = false
    var newTabUrl = emptyURL // 新打开的标签页

    enum TabsStates: Int {
        case shouldExpand
        case shouldShrink
        case expanded
        case shrinked
    }
}

@Observable
class ShiftAnimation {
    var snapshotImage: UIImage = lightSnapshotImage
    var progress: AnimationProgress = .invisible
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

@Observable
class PullingMenu {
    var isActived: Bool = false
}

@Observable
class OpeningLink {
    var clickedLink: URL = emptyURL
}

@Observable
class OuterSearch {
    var content = ""
    var shouldDoSearch = true
}

@Observable
class ResizeSheetState {
    var presenting = false
}

@Observable
class TabGridState {
    var scale = 1.0
}

@Observable
class DeleteCache {
    var cacheId = UUID()
}

enum LocalColorScheme: Int {
    case unspecified, light, dark
}

class ColorSchemeManager: ObservableObject {
    static let shared = ColorSchemeManager()
    @AppStorage("colorScheme") var colorScheme: LocalColorScheme = .unspecified {
        didSet {
            applyColorScheme()
        }
    }

    func applyColorScheme() {
        keyWindow?.overrideUserInterfaceStyle = UIUserInterfaceStyle(rawValue: colorScheme.rawValue)!
    }

    private var keyWindow: UIWindow? {
        if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene {
            if let window = windowScene.windows.first {
                return window
            }
        }
        return nil
    }
}
