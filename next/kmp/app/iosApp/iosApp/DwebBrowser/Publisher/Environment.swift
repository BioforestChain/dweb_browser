//
//  BrowerVM.swift
//  DwebBrowser
//
//  Created by ui06 on 5/8/23.
//

import Combine
import Foundation
import SwiftUI
import DwebShared

class BrowserArea: ObservableObject {
    @Published var frame: CGRect = .zero
}

class SelectedTab: ObservableObject {
    @Published var curIndex: Int = 0
}

class WndDragScale: ObservableObject {
    @Published var onWidth: CGFloat = 1
    
    func properValue(floor: CGFloat, ceiling: CGFloat) -> CGFloat{
        min(ceiling, max(floor, ceiling * onWidth))
    }
    func scaledFont(maxSize: CGFloat = 18) -> Font{
        Font.system(size:  max(10, onWidth * maxSize))
    }
    func scaledFontSize(maxSize: CGFloat = 18) -> CGFloat{
        max(10, onWidth * maxSize)
    }

    var addressbarHeight: CGFloat { properValue(floor: minAddressBarH, ceiling: maxAddressBarH)}
    var toolbarHeight: CGFloat { properValue(floor: minToolBarH, ceiling: maxToolBarH)}
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
    @Published var canGoBack = false
    @Published var canGoForward = false
    @Published var goBackTapped = false
    @Published var goForwardTapped = false
    @Published var createTabTapped = false
    @Published var showMoreMenu = false
    @Published var creatingDesktopLink = false
}

class ShiftAnimation: ObservableObject {
    @Published var snapshotImage: UIImage = UIImage()
    @Published var progress: AnimationProgress = .invisible
}

class TracelessMode {
    static var shared = TracelessMode()
    private let tracelessKEY = "tracelessKEY"
    private let service = DwebBrowserIosSupport().browserService
    var isON: Bool {
        willSet {
            service.trackModel = newValue
        }
    }
    
    private init() {
        isON = service.trackModel
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
