//
//  BrowerVM.swift
//  DwebBrowser
//
//  Created by ui06 on 5/8/23.
//

import Combine
import Foundation
import SwiftUI

class BrowserArea: ObservableObject {
    @Published var frame: CGRect = .zero
}

class WebMonitor: ObservableObject{
    @Published var loadingProgress: Double = 0{
        willSet{
            if newValue >= 1.0{
                isLoadingDone = true
            }else {
                if isLoadingDone != false{
                    isLoadingDone = false
                }
            }
        }
    }
    @Published var isLoadingDone : Bool = false
}

class SelectedTab: ObservableObject {
    @Published var curIndex: Int = 0
}

class WndDragScale: ObservableObject {
    @Published var onWidth: CGFloat = 1 {
        didSet {
            if onWidth < 0 {
                onWidth = 0
            }
        }
    }
    
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
    @Published var createTabTapped = false
    @Published var showMoreMenu = false
    @Published var creatingDesktopLink = false
    @Published var isPresentingScanner = false
}

class ShiftAnimation: ObservableObject {
    @Published var snapshotImage: UIImage = UIImage()
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
