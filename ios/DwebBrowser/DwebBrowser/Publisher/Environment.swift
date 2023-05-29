//
//  BrowerVM.swift
//  DwebBrowser
//
//  Created by ui06 on 5/8/23.
//

import Foundation
import SwiftUI
import Combine

class BrowerVM: ObservableObject {
    var cancellables = Set<AnyCancellable>()
//    @Published var currentSnapshotImage: UIImage = UIImage.defaultSnapShotImage
    
    @Published var selectedTabIndex = 0
//    {
//        didSet{
//
//            currentSnapshotImage = UIImage.snapshotImage(from: WebCacheMgr.shared.store[selectedTabIndex].snapshotUrl)
//        }
//    }
}


class AddrBarOffset: ObservableObject {
    @Published var onX: CGFloat = 0
}

class TabState: ObservableObject {
    @Published var showTabGrid = true
    @Published var canGoBack = false
    @Published var canGoForward = false
    @Published var goBackTapped = false
    @Published var goForwardTapped = false
    
    var addressBarHeight: CGFloat{
        withAnimation(.easeInOut,{
            showTabGrid ? 0 : addressBarH
        })
    }
}

class Animation: ObservableObject{
    @Published var snapshotImage: UIImage = UIImage.defaultSnapShotImage
    @Published var progress: AnimationProgress = .invisible
    
    
}
