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
//    @Published private var _curIndex: Int = 0
//
//    var curIndex: Int {
//            get {
//                if !isValid(_curIndex) {
//                    _curIndex = 0 // 设置为合法的默认值
//                }
//                return _curIndex
//            }
//            set {
//                _curIndex = newValue
//            }
//        }
//    private func isValid(_ value: Int) -> Bool {
//        // 根据需要进行属性校验的逻辑
//        return value >= WebCacheMgr.shared.store.count
//    }
    
    @Published var curIndex:Int = 0
//    {
//        get{
//
//        }
//    }
}

class AddrBarOffset: ObservableObject {
    @Published var onX: CGFloat = 0
}

class AddressBarState: ObservableObject{
    @Published var isFocused = false
    @Published var inputText: String = ""
}

class TabState: ObservableObject {
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
