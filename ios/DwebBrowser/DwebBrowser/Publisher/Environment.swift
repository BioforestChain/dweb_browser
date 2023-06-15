//
//  BrowerVM.swift
//  DwebBrowser
//
//  Created by ui06 on 5/8/23.
//

import Foundation
import SwiftUI
import Combine

class SelectedTab: ObservableObject {
    @Published var curIndex:Int = 0
}

class AddrBarOffset: ObservableObject {
    @Published var onX: CGFloat = 0
}

class AddressBarState: ObservableObject{
    @Published var isFocused = false
    @Published var inputText: String = ""
}

class ToolBarState: ObservableObject {
    @Published var showTabGrid = true
    @Published var canGoBack = false
    @Published var canGoForward = false
    @Published var goBackTapped = false
    @Published var goForwardTapped = false

    var addressBarHeight: CGFloat{
        showTabGrid ? 0 : addressBarH
    }
}

class Animation: ObservableObject{
    @Published var snapshotImage: UIImage = UIImage.defaultSnapShotImage
    @Published var progress: AnimationProgress = .invisible
}

class TraceLessMode{
    static var shared = TraceLessMode()
    private let tracelessKEY = "tracelessKEY"
    var isON: Bool{
        willSet{
            UserDefaults.standard.setValue(newValue, forKey: tracelessKEY)
        }
    }
    
    private init(){
        isON = UserDefaults.standard.bool(forKey: tracelessKEY)
    }
}


class OpeningLink: ObservableObject{
    @Published var clickedLink: URL = testURL
}

class ShowSheet: ObservableObject{
    @Published var should: Bool = false
}
