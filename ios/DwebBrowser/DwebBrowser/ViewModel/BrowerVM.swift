//
//  BrowerVM.swift
//  DwebBrowser
//
//  Created by ui06 on 5/8/23.
//

import Foundation
import SwiftUI

class Web: ObservableObject{
    
}

class Home: ObservableObject{
    
}

class Page: Identifiable, ObservableObject, Hashable{
    var id = UUID()

    
    @Published var webStore: WebViewStore //= WebViewStore( webCache: WebCache.createItem())

    init(id: UUID = UUID(), webStore: WebViewStore) {
        self.id = id
        self.webStore = webStore
    }
    
    static func == (lhs: Page, rhs: Page) -> Bool {
        lhs.id == rhs.id
    }
    
    public func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
}

class BrowerVM: ObservableObject {
    @Published var showingOptions = true
    @Published var selectedTabIndex = 0
    @Published var addressBarOffset = 0.0
    
    @Published var pages = WebCacheStore().store.map{Page(webStore: WebViewStore(webCache: $0))}
    
    @Published var sharedResources = SharedSourcesVM()
    
    @Published var shrinkingSnapshot: UIImage? = nil
    
    @Published var shouldTakeSnapshot = false
    
    var addressBarHeight: CGFloat{
        showingOptions ? 0:60
    }
}

